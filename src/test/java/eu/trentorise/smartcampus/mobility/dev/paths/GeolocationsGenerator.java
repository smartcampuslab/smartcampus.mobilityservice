package eu.trentorise.smartcampus.mobility.dev.paths;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Activity;
import eu.trentorise.smartcampus.mobility.geolocation.model.Coords;
import eu.trentorise.smartcampus.mobility.geolocation.model.Location;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

@Component
public class GeolocationsGenerator {

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;		


	/**
	 * 
	 * @param id trackedInstance id
	 * @param timeJitter percentage of timestamp change of a point, relative to the previous one
	 * @param spaceJitter percentage of distance change of a point, relative to the previous one
	 * @param modTime apply timeJitter changes only every modTime points
	 * @param modSpace apply spaceJitter changes only every modSpace points
	 * @param timeShift if true, shift every timestamp to have the itinerary start time set at 'now'
	 * @return
	 * @throws Exception
	 */	
	public List<Location> generate(String id, double timeJitter, double spaceJitter, int modTime, int modSpace, boolean timeShift) throws Exception {
		TrackedInstance ti = getItinerary(id);
		
		long shift = timeShift ? (System.currentTimeMillis() - ti.getItinerary().getData().getStartime()) : 0;
		
		List<Location> locations = generateGeolocation(ti.getItinerary().getData().getLeg(), shift, ti.getClientId());
		
		addTimeJitter(locations, timeJitter, modTime);
		addSpaceJitter(locations, spaceJitter, modSpace);
		respeed(locations);

		return locations;
	}
	
	private TrackedInstance getItinerary(String id) {
		Criteria criteria = new Criteria("_id").is(id);
		Query query = new Query(criteria);
		
		TrackedInstance ti = template.findOne(query, TrackedInstance.class, "trackedInstances");
		
		return ti;
	}
	
	private List<Location> generateGeolocation(List<Leg> legs, long timeShift, String travelId) {
		List<Location> result = Lists.newArrayList();
		boolean first = true;
		for (Leg leg : legs) {
			List<Location> legPoints = generateGeolocation(leg, timeShift, travelId, first);
			first = !first;
			result.addAll(legPoints);
		}

		return result;
	}
	
	private List<Location> generateGeolocation(Leg leg, long timeShift, String travelId, boolean first) {
		String activity = convertTType(leg.getTransport().getType());
		double speed = getTTypeSpeed(leg.getTransport().getType());
		List<Location> result = Lists.newArrayList();
		long time = leg.getStartime();
//		System.out.println(new Date(time));
		
		List<double[]> coords = PolylineUtils.decode(leg.getLegGeometery().getPoints());
		
		long timeIncrease = (leg.getEndtime() - time) / coords.size();
		time += timeShift;

		
		Location lastCoords = buildLocation(coords.get(0), activity, first ? 0 : speed, time, travelId);
		result.add(lastCoords);
		
		for (int i = 1 ; i < coords.size(); i++) {
			double[] coord = coords.get(i);
			
			double d = GamificationHelper.harvesineDistance(lastCoords.getCoords().getLatitude(), lastCoords.getCoords().getLongitude(), coord[0], coord[1]);
			long dt = (long)(3.6 * 1E6 * d / speed);
			time += dt;
			double ns = (1000.0 * d / ((double) dt / 1000)) * 3.6;
			
			Location geo = buildLocation(coords.get(i), activity, speed, time, travelId);
			
			lastCoords = geo;
			result.add(geo);
		}

		removeSameLocations(result);
		return result;
	}
	
	private Location buildLocation(double[] coord, String activity, double speed, long time, String travelId) {
		Location geo = new Location();
		Coords c = new Coords();
		c.setLatitude(coord[0]);
		c.setLongitude(coord[1]);
		c.setAccuracy(100L);
		
		c.setSpeed(speed);
		geo.setCoords(c);
		
		Activity a = new Activity();
		a.setConfidence(100);
		a.setType(activity);
		geo.setActivity(a);
		
		geo.setTimestamp(new Date(time));
		
		geo.setIs_moving(true);
		geo.setUuid(Long.toHexString(time) + "-" + Long.toHexString(System.currentTimeMillis()));
		
		Map<String, Object> extra = Maps.newTreeMap();
		extra.put("idTrip", travelId);
		geo.setExtras(extra);
		
		return geo;
	}
	
	private void removeSameLocations(List<Location> locations) {
		Location lastLocation = locations.get(0);
		List<Location> toRemove = Lists.newArrayList();
		for (int i = 1; i < locations.size(); i++) {
			if (locations.get(i).getTimestamp().getTime() <= lastLocation.getTimestamp().getTime()) {
				System.err.print("");
			}
			double d = GamificationHelper.harvesineDistance(lastLocation.getCoords().getLatitude(), lastLocation.getCoords().getLongitude(), locations.get(i).getCoords().getLatitude(), locations.get(i).getCoords().getLongitude());
			if (d < 1E-4) {
				toRemove.add(lastLocation);
			}
			lastLocation = locations.get(i);
		}
		locations.removeAll(toRemove);
	}
	
	private void addTimeJitter(List<Location> locations, double speedVariationPercent, int modTime) {
		long lastTime = locations.get(0).getTimestamp().getTime();
		long incTime = 0;
		for (int i = 1; i < locations.size(); i++) {
			Location location = locations.get(i);
			if (i % modTime != 0) {
				lastTime = location.getTimestamp().getTime();
				continue;
			}
			
			long currentTime = location.getTimestamp().getTime() + incTime;
			
			double variation = Math.random() * 2 * speedVariationPercent - speedVariationPercent;
			long deltaTime = currentTime - lastTime;
			
			if (deltaTime < 0) {
				System.err.print("");
			}
			
			long time = (long)(deltaTime * (1 + variation)) + lastTime;
			
			if (time < lastTime) {
				System.err.print("");
			}
			
			lastTime = time;
			incTime += (long)(deltaTime * variation);
			
			location.setTimestamp(new Date(time));
		}
	}
	
	private void addSpaceJitter(List<Location> locations, double spaceVariationPercent, int modSpace) {
		Location lastLocation = locations.get(0);
		for (int i = 1; i < locations.size(); i++) {
			Location location = locations.get(i);
			
			if (i % modSpace != 0) {
				lastLocation = location;
				continue;
			}			
			
			double variationX = Math.random() * 2 * spaceVariationPercent - spaceVariationPercent;
			double variationY = Math.random() * 2 * spaceVariationPercent - spaceVariationPercent;

			double dx = location.getCoords().getLongitude() - lastLocation.getCoords().getLongitude();
			double dy = location.getCoords().getLatitude() - lastLocation.getCoords().getLatitude();
			
			double lon = lastLocation.getCoords().getLongitude() + dx * (1 + variationX);
			double lat = lastLocation.getCoords().getLatitude() + dy * (1 + variationY);
			
			location.getCoords().setLatitude(lat);
			location.getCoords().setLongitude(lon);
		}
	}	
	
	private void respeed(List<Location> locations) {
		Location lastLocation = locations.get(0);
		for (int i = 1; i < locations.size(); i++) {
			double d = GamificationHelper.harvesineDistance(lastLocation.getCoords().getLatitude(), lastLocation.getCoords().getLongitude(), locations.get(i).getCoords().getLatitude(), locations.get(i).getCoords().getLongitude());
			long t = locations.get(i).getTimestamp().getTime() - lastLocation.getTimestamp().getTime();
			if (d != 0 && t != 0) {
				double speed = (1000.0 * d / ((double) t / 1000)) * 3.6;
//				System.out.println(locations.get(i).getCoords().getSpeed() + " / " + speed);
				locations.get(i).getCoords().setSpeed(speed);
				if (speed < 0 || speed > 100) {
					System.err.print("");
				}
			} else {
				System.err.print("");
			}
			lastLocation = locations.get(i);
		}
	}
	
	private String convertTType(TType tt) {
		if (tt.equals(TType.WALK)) {
			return "on_foot";
		}
		if (tt.equals(TType.BICYCLE) || tt.equals(TType.SHAREDBIKE) || tt.equals(TType.SHAREDBIKE_WITHOUT_STATION)) {
			return "on_bicycle";
		}
		// TODO: no transit: bus/train
		if (tt.equals(TType.CAR) || tt.equals(TType.CARWITHPARKING) || tt.equals(TType.BUS)|| tt.equals(TType.TRAIN) || tt.equals(TType.TRANSIT) || tt.equals(TType.GONDOLA) ) {
			return "in_vehicle";
		}		
		return "";
		
	}	
	
	private double getTTypeSpeed(TType tt) {
		if (tt.equals(TType.WALK)) {
			return 3;
		}
		if (tt.equals(TType.BICYCLE) || tt.equals(TType.SHAREDBIKE) || tt.equals(TType.SHAREDBIKE_WITHOUT_STATION)) {
			return 15;
		}
		// TODO: no transit: bus/train
		if (tt.equals(TType.CAR) || tt.equals(TType.CARWITHPARKING) || tt.equals(TType.BUS)|| tt.equals(TType.TRAIN) || tt.equals(TType.TRANSIT) || tt.equals(TType.GONDOLA) ) {
			return 30;
		}		
		return 0;
		
	}	
	

}
