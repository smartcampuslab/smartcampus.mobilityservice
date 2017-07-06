/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.gamification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

@Component
public class GamificationValidator {

	private static final String ON_FOOT = "on_foot";
	private static final String ON_BICYCLE = "on_bicycle";
	private static final String IN_VEHICLE = "in_vehicle";
	private static final String WALKING = "walking";
	private static final String RUNNING = "running";
	private static final String UNKNOWN = "unknown";
	private static final String EMPTY = "unknown";
	private static final double SPACE_ERROR = 0.1;

	private static final Logger logger = LoggerFactory.getLogger(GamificationValidator.class);

	private static final List<TType> FAST_TRANSPORTS = Lists.newArrayList(TType.BUS, TType.CAR, TType.GONDOLA, TType.SHUTTLE, TType.TRAIN, TType.TRANSIT);
	private static final Set<String> WALKLIKE = Sets.newHashSet(ON_FOOT, WALKING, RUNNING, UNKNOWN, EMPTY);

	public Map<String, Object> computePlannedJourneyScore(Itinerary itinerary, boolean log) {
		Map<String, Object> data = Maps.newTreeMap();

		String parkName = null; // name of the parking
		String startBikesharingName = null; // name of starting bike sharing
											// station
		String endBikesharingName = null; // name of ending bike sharing station
		boolean pnr = false; // (park-n-ride)
		boolean bikeSharing = false;
		double bikeDist = 0; // km
		double walkDist = 0; // km
		double trainDist = 0; // km
		double busDist = 0; // km
		double carDist = 0; // km
		double transitDist = 0;

		logger.debug("Analyzing itinerary for gamification.");
		if (itinerary != null) {
			for (Leg leg : itinerary.getLeg()) {
				if (leg.getTransport().getType().equals(TType.CAR)) {
					carDist += leg.getLength() / 1000;
					if (leg.getTo().getStopId() != null) {
						if (leg.getTo().getStopId().getExtra() != null) {
							if (leg.getTo().getStopId().getExtra().containsKey("parkAndRide")) {
								pnr |= (Boolean) leg.getTo().getStopId().getExtra().get("parkAndRide");
							}
						}
						parkName = leg.getTo().getStopId().getId();
					}
				} else if (leg.getTransport().getType().equals(TType.BICYCLE)) {
					bikeDist += leg.getLength() / 1000;
					if (leg.getFrom().getStopId() != null && leg.getFrom().getStopId().getAgencyId() != null) {
						if (leg.getFrom().getStopId().getAgencyId().startsWith("BIKE_SHARING")) {
						bikeSharing = true;
						startBikesharingName = leg.getFrom().getStopId().getId();
					}
					}
					if (leg.getTo().getStopId() != null && leg.getTo().getStopId().getAgencyId() != null) {
						if (leg.getTo().getStopId().getAgencyId().startsWith("BIKE_SHARING")) {
						bikeSharing = true;
						endBikesharingName = leg.getTo().getStopId().getId();
						}
					}
				} else if (leg.getTransport().getType().equals(TType.WALK)) {
					walkDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRAIN)) {
					trainDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.BUS)) {
					busDist += leg.getLength() / 1000;
				} else if (leg.getTransport().getType().equals(TType.TRANSIT)) {
					transitDist += leg.getLength() / 1000;
				}
			}
		}

		if (log) {
			logger.debug("Analysis results:");
			logger.debug("Distances [walk = " + walkDist + ", bike = " + bikeDist + ", train = " + trainDist + ", bus = " + busDist + ", car = " + carDist + "]");
			logger.debug("Park and ride = " + pnr + " , Bikesharing = " + bikeSharing);
			logger.debug("Park = " + parkName);
			logger.debug("Bikesharing = " + startBikesharingName + " / " + endBikesharingName);
		}

		Double score = 0.0;
		// score += (walkDist < 0.1 ? 0 : Math.min(3.5, walkDist)) * 10; Rovereto
		score += (walkDist < 0.25 ? 0 : Math.min(3.5, walkDist)) * 10;
		score += Math.min(7, bikeDist) * 5;

		double busTrainTransitDist = busDist + trainDist;
		if (busTrainTransitDist > 0) {
			score += (busTrainTransitDist > 0 && busTrainTransitDist < 1) ? 10 : ((busTrainTransitDist > 1 && busTrainTransitDist < 5) ? 15 : (busTrainTransitDist >= 5 && busTrainTransitDist < 10) ? 20
					: (busTrainTransitDist >= 10 && busTrainTransitDist < 30) ? 30 : 40);
		}
		
		// Trento only
		if (transitDist > 0) {
			score += 25;
		}

		boolean zeroImpact = (busDist + carDist + trainDist + transitDist == 0 && walkDist + bikeDist > 0);
//		Rovereto
//		if (zeroImpact && itinerary.isPromoted()) {
//			score *= 1.7;
//		} else {
//			if (zeroImpact) {
//				score *= 1.5;
//			}
//			if (itinerary.isPromoted()) {
//				score *= 1.2;
//			}
//		}
		
		
		if (pnr) {
			score += 10;
		}
		if (zeroImpact) {
			score *= 1.5;
		}

		if (bikeDist > 0) {
			data.put("bikeDistance", bikeDist);
		}
		if (walkDist > 0) {
			data.put("walkDistance", walkDist);
		}
		if (busDist > 0) {
			data.put("busDistance", busDist);
		}
		if (trainDist > 0) {
			data.put("trainDistance", trainDist);
		}
		if (transitDist > 0) {
			data.put("transitDistance", transitDist);
		}		
		if (carDist > 0) {
			data.put("carDistance", carDist);
		}
		if (bikeSharing) {
			data.put("bikesharing", bikeSharing);
		}
		if (parkName != null) {
			data.put("park", parkName);
		}
		if (startBikesharingName != null) {
			data.put("startBike", startBikesharingName);
		}
		if (endBikesharingName != null) {
			data.put("endBike", endBikesharingName);
		}
		if (pnr) {
			data.put("p+r", pnr);
		}
		data.put("sustainable", itinerary.isPromoted());
		data.put("zeroimpact", zeroImpact);
		data.put("estimatedScore", Math.round(score));

		return data;
	}
	
	public Map<String, Object> computeFreeTrackingScore(Collection<Geolocation> geolocationEvents, String ttype) {
		Map<String, Object> result = Maps.newTreeMap();
		Double score = 0.0;
		double distance = 0; 		
		
		if (geolocationEvents != null & geolocationEvents.size() >= 2) {

			List<Geolocation> points = new ArrayList<Geolocation>(geolocationEvents);
			
			Collections.sort(points, new Comparator<Geolocation>() {

				@Override
				public int compare(Geolocation o1, Geolocation o2) {
					return (int) (o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime());
				}

			});
			
			points = GamificationHelper.transform(points);
			
			for (int i = 1; i < points.size(); i++) {
				double d = GamificationHelper.harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude());
//				System.out.println(points.get(i - 1).getLatitude() + "," + points.get(i - 1).getLongitude() + " / " +  points.get(i).getLatitude() + "," +  points.get(i).getLongitude() + " = " + d);
				distance += d; 
			}

			if ("walk".equals(ttype)) {
				result.put("walkDistance", distance);
				score = (distance < 0.25 ? 0 : Math.min(3.5, distance)) * 10;
			}
			if ("bike".equals(ttype)) {
				result.put("bikeDistance", distance);
				score += Math.min(7, distance) * 5;
			}
			
			// always zero impact
			score *= 1.5;
		}

		result.put("estimatedScore", Math.round(score));
		return result;
	}	
	

	// TODO: remove?
	public long computeEstimatedGameScore(Itinerary itinerary, boolean log) {
		Long score = (Long) (computePlannedJourneyScore(itinerary, log).get("estimatedScore"));
		itinerary.getCustomData().put("estimatedScore", score);
		return score;
	}



	public ValidationResult validatePlannedJourney(ItineraryObject itinerary, Collection<Geolocation> geolocations) throws Exception {

		boolean legWalkOnly = true;
		boolean geolocationWalkOnly = true;

		Set<String> geolocationModes = Sets.newHashSet();
		Set<String> legsModes = Sets.newHashSet();

		ValidationResult vr = new ValidationResult();
		vr.setGeoLocationsN(geolocations.size());
		vr.setGeoActivities(geolocationModes);
		vr.setLegsActivities(legsModes);

		List<List<Geolocation>> legPositions = Lists.newArrayList();
		List<List<Geolocation>> matchedPositions = Lists.newArrayList();
		for (Leg leg : itinerary.getData().getLeg()) {
			legPositions.addAll(GamificationHelper.splitList(GamificationHelper.decodePoly(leg)));

			TType tt = leg.getTransport().getType();
			if (FAST_TRANSPORTS.contains(tt)) {
				// onLeg.setActivity_type(IN_VEHICLE);
				legsModes.add(IN_VEHICLE);
				legWalkOnly = false;
			} else if (tt.equals(TType.BICYCLE)) {
				// onLeg.setActivity_type(ON_BICYCLE);
				legsModes.add(ON_BICYCLE);
				legWalkOnly = false;
			} else if (tt.equals(TType.WALK)) {
				// onLeg.setActivity_type(ON_FOOT);
				legsModes.add(ON_FOOT);
			}
		}

		vr.setLegsLocationsN(legPositions.size());

		double distance = 0;
		long time = 0;		
		
		List<Geolocation> points = new ArrayList<Geolocation>(geolocations);
		for (int i = 1; i < points.size(); i++) {
			double d = GamificationHelper.harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude());
			distance += d;
		}
		time = points.get(points.size() - 1).getRecorded_at().getTime() - points.get(0).getRecorded_at().getTime();
		vr.setDistance(distance);
		vr.setTime(time);
		
		for (Geolocation geolocation : geolocations) {

			if (geolocation.getAccuracy() != null && geolocation.getActivity_confidence() != null && geolocation.getAccuracy() > SPACE_ERROR * 1000 * 2 && geolocation.getActivity_confidence() < 50) {
				continue;
			}

			double lat = geolocation.getLatitude();
			double lon = geolocation.getLongitude();

			if (geolocation.getActivity_type() != null && !geolocation.getActivity_type().isEmpty()) {
				if (WALKLIKE.contains(geolocation.getActivity_type())) {
					geolocationModes.addAll(WALKLIKE);
				} else {
					geolocationModes.add(geolocation.getActivity_type());
				}

				if (geolocation.getActivity_type().equals(IN_VEHICLE) && geolocation.getActivity_confidence() > 50) {
					geolocationWalkOnly = false;
				}
			}
			if (geolocation.getActivity_type() == null) {
				geolocationModes.addAll(WALKLIKE);
			}

			List<Geolocation> toRemove = null;
			for (List<Geolocation> poss : legPositions) {
				for (Geolocation pos : poss) {
					double d = GamificationHelper.harvesineDistance(lat, lon, pos.getLatitude(), pos.getLongitude());
					double t = Math.abs(pos.getRecorded_at().getTime() - geolocation.getRecorded_at().getTime());
					if (d <= Math.max(SPACE_ERROR, geolocation.getAccuracy() != null ? ((double) geolocation.getAccuracy() / 10000) : 0)) {
						toRemove = poss;
						break;
					}
				}
				if (toRemove != null) {
					break;
				}
			}
			if (toRemove != null) {
				legPositions.remove(toRemove);
				matchedPositions.add(toRemove);
			}

		}

		SetView<String> diffModes = Sets.difference(legsModes, geolocationModes);

		vr.setMatchedLocationsN(matchedPositions.size());
		vr.setMatchedLocations(vr.getMatchedLocationsN() > Math.ceil(vr.getLegsLocationsN() / 2));
		vr.setTooFewPoints(vr.getGeoLocationsN() < 2);
		vr.setMatchedActivities(diffModes.size() == 0);
		vr.setTooFast(legWalkOnly & !geolocationWalkOnly);
		
		boolean valid = vr.getMatchedActivities() && vr.getMatchedLocations() && !vr.getTooFast();
		// TODO temporary
		vr.setTravelValidity(valid ? TravelValidity.VALID : TravelValidity.INVALID);

		return vr;
	}

	public ValidationResult validateFreeTracking(Collection<Geolocation> geolocations, String ttype) throws Exception {
		if (geolocations == null || ttype == null) {
			return null;
		}
		ValidationResult vr = new ValidationResult();
		vr.reset();

		double averageSpeed = 0;
		double maxSpeed = 0;

		List<Geolocation> points = new ArrayList<Geolocation>(geolocations);
		Collections.sort(points, new Comparator<Geolocation>() {

			@Override
			public int compare(Geolocation o1, Geolocation o2) {
				return (int) (o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime());
			}

		});

		int tooFastCountTotal = 0;
		double distance = 0;
		long time = 0;
		double validatedDistance = 0;
		long validatedTime = 0; 		
		
		int origPointsSize = points.size();
		if (points.size() >= 2) {
//			logger.debug("Original track points (remove outliers): " + points.size());
//			removeOutliers(points);
//			logger.debug("Transformed track points (remove outliers): " + points.size());			
		
//			if (points.size() >= 2) {
			logger.debug("Original track points (transform): " + points.size());
			points = GamificationHelper.transform(points);
			logger.debug("Transformed track points (transform): " + points.size());
//			}
		
			int tooFastCount = 0;
			List<Geolocation> fastToRemove = Lists.newArrayList();
			if (points.size() >= 2) {
				for (int i = 1; i < points.size(); i++) {
					double d = GamificationHelper.harvesineDistance(points.get(i).getLatitude(), points.get(i).getLongitude(), points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude());
//					System.out.println(points.get(i - 1).getLatitude() + "," + points.get(i - 1).getLongitude() + " / " +  points.get(i).getLatitude() + "," +  points.get(i).getLongitude() + " = " + d);
					
					long t = points.get(i).getRecorded_at().getTime() - points.get(i - 1).getRecorded_at().getTime();
					if (t > 0) {
						double s = (1000.0 * d / ((double) t / 1000)) * 3.6;
						maxSpeed = Math.max(maxSpeed, s);
						if (isMaximumTooFast(s, ttype)) {
							fastToRemove.add(points.get(i));
							tooFastCount++;
							tooFastCountTotal = Math.max(tooFastCountTotal, tooFastCount);
						} else {
							tooFastCount = 0;
							validatedDistance += d;
							validatedTime += t;							}
						distance += d;
					}
				}
				
				time = points.get(points.size() - 1).getRecorded_at().getTime() - points.get(0).getRecorded_at().getTime();
				averageSpeed = (1000.0 * distance / ((double) time / 1000)) * 3.6;
			}
		}

		vr.setTooFast(false);
		if ("walk".equals(ttype)) {
			if (averageSpeed > 15) {
				vr.setTooFast(true);
			}
		}
		if ("bike".equals(ttype)) {
			if (averageSpeed > 27) {
				vr.setTooFast(true);
			}
		}
		if (tooFastCountTotal > 3) {
			vr.setTooFast(true);
		}

		vr.setGeoLocationsN(points.size());
		boolean valid = !vr.getTooFast() && origPointsSize >= 2;
		// TODO temporary
		vr.setTravelValidity(valid ? TravelValidity.VALID : TravelValidity.INVALID);		
		
		vr.setAverageSpeed(averageSpeed);
		vr.setTooFewPoints(vr.getGeoLocationsN() < 2);
		vr.setMaxSpeed(maxSpeed);
		vr.setDistance(distance);
		vr.setTime(time);
		vr.setValidatedDistance(validatedDistance);
		vr.setValidatedTime(validatedTime);
		return vr;
	}

	private boolean isMaximumTooFast(double speed, String ttype) {
		if ("walk".equals(ttype)) {
			if (speed > 20) {
				return true;
			}
		}
		if ("bike".equals(ttype)) {
			if (speed > 65) {
				return true;
			}
		}
		return false;
	}
	
//	public static boolean checkItineraryCompletion(ItineraryObject itinerary, Collection<Geolocation> geolocations) throws Exception {
//	if (itinerary == null) {
//		return false;
//	}
//	if (geolocations.size() > 1) {
//		boolean started = false;
//		boolean ended = false;
//
//		double fromLat = Double.parseDouble(itinerary.getData().getFrom().getLat());
//		double fromLon = Double.parseDouble(itinerary.getData().getFrom().getLon());
//		double toLat = Double.parseDouble(itinerary.getData().getTo().getLat());
//		double toLon = Double.parseDouble(itinerary.getData().getTo().getLon());
//		long startTime = itinerary.getData().getStartime();
//		long endTime = itinerary.getData().getEndtime();
//
//		for (Geolocation geolocation : geolocations) {
//			double lat = geolocation.getLatitude();
//			double lon = geolocation.getLongitude();
//			long time = geolocation.getCreated_at().getTime();
//			if (!started) {
//				double fromD = harvesineDistance(lat, lon, fromLat, fromLon);
//				long fromT = Math.abs(time - startTime);
//				started = fromD <= SPACE_ERROR & fromT <= TIME_ERROR;
//			}
//			if (!ended) {
//				double toD = harvesineDistance(geolocation.getLatitude(), geolocation.getLongitude(), toLat, toLon);
//				long toT = Math.abs(time - endTime);
//				ended = toD <= SPACE_ERROR & toT <= TIME_ERROR;
//			}
//			if (started && ended) {
//				return true;
//			}
//		}
//	}
//
//	return false;
//}	
	

}
