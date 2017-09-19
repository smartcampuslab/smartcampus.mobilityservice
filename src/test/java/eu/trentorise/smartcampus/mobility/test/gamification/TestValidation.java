/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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

package eu.trentorise.smartcampus.mobility.test.gamification;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.gamification.GamificationValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.ERROR_TYPE;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.MODE_TYPE;
import eu.trentorise.smartcampus.mobility.test.RemoteTestConfig;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.TType;

/**
 * @author raman
 *
 */
//@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations="classpath:application.properties")
@ContextConfiguration(classes = {RemoteTestConfig.class})
public class TestValidation {

	public static final double VALIDITY_THRESHOLD = 80; // %
	public static final double ACCURACY_THRESHOLD = 150; // meters
	public static final int COVERAGE_THRESHOLD = 80; // %
	public static final double DISTANCE_THRESHOLD = 250; // meters
	public static final long DATA_HOLE_THRESHOLD = 10*60; // seconds

	@Autowired
	private MongoTemplate template;

	@Autowired
	private GamificationValidator validator;
	
	private static final String APP_ID = "trentoplaygo";
	
	private static final Set<TType> PT = Sets.newHashSet(TType.BUS, TType.TRAIN, TType.TRANSIT, TType.GONDOLA, TType.CABLE_CAR, TType.GONDOLA);
	
	public void plannedStats() {
		Map<String, Integer> modalities = new HashMap<>();
		Query q = Query.query(Criteria.where("itinerary").exists(true).and("day").gte("2016/09/10"));
		q.fields().include("itinerary.data.leg.transport");
		List<TrackedInstance> planned = template.find(q, TrackedInstance.class, "trackedInstances");
		for (TrackedInstance i : planned) {
			List<Leg> legs = i.getItinerary().getData().getLeg();
			Map<TType, Long> map = legs.stream().collect(Collectors.groupingBy(l -> PT.contains(l.getTransport().getType()) ? TType.TRANSIT : l.getTransport().getType(), Collectors.counting()));
			if (map.size() == 1) {
				String type = map.keySet().iterator().next().toString();
				modalities.put(type, modalities.getOrDefault(type, 0) + 1);
			} else {
				map.remove(TType.WALK);
				if (map.size() == 1) {
					TType ttype = map.keySet().iterator().next();
					if (map.get(ttype) > 1) {
						modalities.put("multi", modalities.getOrDefault("multi", 0) + 1);
					} else {
						String type = "MIX_"+ttype.toString();
						modalities.put(type, modalities.getOrDefault(type, 0) + 1);
					}
				} else {
					modalities.put("multi", modalities.getOrDefault("multi", 0) + 1);
				}
			}
		}
		System.err.print(modalities);;
	}

	@Test
	public void validateWalks() throws Exception {
		Query q = Query.query(Criteria
				.where("itinerary").is(null)
				.and("day").gte("2016/09/10")
				.and("freeTrackingTransport").is("walk")
//				.and("id").is("57e000b7e4b04d79183e537d") //57ed0066e4b068507da19bbf
				);
		q.fields().include("id");
		List<ExtTrackedInstance> found = template.find(q, ExtTrackedInstance.class, "trackedInstances");
		
		System.err.println("COUNT = "+found.size());
		
		int falseNegative = 0, falsePositive = 0, tooShort = 0;
		for (ExtTrackedInstance t : found) {
			ExtTrackedInstance track = template.findOne(Query.query(Criteria.where("id").is(t.getId())), ExtTrackedInstance.class, "trackedInstances");
			
			ValidationStatus result = validator.validateFreeTracking(track.getGeolocationEvents(), "walk", APP_ID).getValidationStatus();
//			System.err.println(result);

			if (track.getEstimatedScore() != null && track.getEstimatedScore() > 0 && result.getValidationOutcome().equals(TravelValidity.INVALID) && (track.getValidationResult().isValid() || Boolean.TRUE.equals(track.getSwitchValidity())) && (result.getError().equals(ERROR_TYPE.TOO_SHORT))) {
				tooShort++;
			}
			if (track.getEstimatedScore() != null && track.getEstimatedScore() > 0 && result.getValidationOutcome().equals(TravelValidity.INVALID) && (track.getValidationResult().isValid() || Boolean.TRUE.equals(track.getSwitchValidity())) && (!result.getError().equals(ERROR_TYPE.TOO_SHORT))) {
				System.err.println("--------NEGATIVE------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falseNegative++;
			}
			if (track.getEstimatedScore() != null && track.getEstimatedScore() > 0 && result.getValidationOutcome().equals(TravelValidity.VALID) && (!track.getValidationResult().isValid() || Boolean.FALSE.equals(track.getSwitchValidity())) && 100.0*result.getEffectiveDistances().get(MODE_TYPE.WALK)/result.getDistance() > 80) {
				System.err.println("--------POSITIVE------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falsePositive++;
			}
		};
		System.err.println(falseNegative);
		System.err.println(falsePositive);
		System.err.println(tooShort);
	}
	
	@Test
	public void validateBikes() throws Exception {
		Query q = Query.query(Criteria
				.where("itinerary").is(null)
				.and("day").gte("2016/09/10")
				.and("freeTrackingTransport").is("bike")
				.and("id").is("57fa6c13e4b0c734dc226f8b") //57ed0066e4b068507da19bbf
				);
		q.fields().include("id");
		List<TrackedInstance> found = template.find(q, TrackedInstance.class, "trackedInstances");
		
		System.err.println("COUNT = "+found.size());
		
		int falseNegative = 0, falsePositive = 0, tooShort = 0;
		for (TrackedInstance t : found) {
			ExtTrackedInstance track = template.findOne(Query.query(Criteria.where("id").is(t.getId())), ExtTrackedInstance.class, "trackedInstances");
			
			ValidationStatus result = validator.validateFreeTracking(track.getGeolocationEvents(), "bike", APP_ID).getValidationStatus();
//			System.err.println(result);

			if (track.getEstimatedScore() != null && track.getEstimatedScore() > 0 && result.getValidationOutcome().equals(TravelValidity.INVALID) && (track.getValidationResult().isValid() || Boolean.TRUE.equals(track.getSwitchValidity())) && (result.getError().equals(ERROR_TYPE.TOO_SHORT))) {
				tooShort++;
			}
			if (track.getEstimatedScore() != null && track.getEstimatedScore() > 0 && result.getValidationOutcome().equals(TravelValidity.INVALID) && (track.getValidationResult().isValid() || Boolean.TRUE.equals(track.getSwitchValidity())) && (!result.getError().equals(ERROR_TYPE.TOO_SHORT))) {
				System.err.println("--------NEGATIVE------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falseNegative++;
			}
			if (track.getEstimatedScore() != null && track.getEstimatedScore() > 0 && result.getValidationOutcome().equals(TravelValidity.VALID) && (!track.getValidationResult().isValid() || Boolean.FALSE.equals(track.getSwitchValidity())) && 100.0*result.getEffectiveDistances().get(MODE_TYPE.BIKE)/result.getDistance() > 80) {
				System.err.println("--------POSITIVE------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falsePositive++;
			}
		};
		System.err.println(falseNegative);
		System.err.println(falsePositive);
		System.err.println(tooShort);
	}	
	
	@Test
	public void validateTrains() throws Exception {
		Query q = Query.query(Criteria
				.where("itinerary").exists(false)
				.and("day").gte("2016/09/10")
//				.and("itinerary.data.leg.transport.type").is("TRAIN")
				.and("id").is("59bb5be79045ea160d8aed8e")
				);
		q.fields().include("id");
		List<TrackedInstance> planned = template.find(q, TrackedInstance.class, "trackedInstances");
		
		System.err.println("COUNT = "+planned.size());
		int falseNegative = 0, falsePositive = 0;
		
		for (TrackedInstance t : planned) {
			ExtTrackedInstance track = template.findOne(Query.query(Criteria.where("id").is(t.getId())), ExtTrackedInstance.class, "trackedInstances");
//			if (!isSinglePT(track)) continue;
			
			ValidationStatus result = validator.validateFreeTracking(track.getGeolocationEvents(), "train", APP_ID).getValidationStatus();
//			System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
			System.err.print(result);
			
			if (result.getValidationOutcome().equals(TravelValidity.PENDING) && track.getValidationResult().isValid()) {
				System.err.println("---------NEGATIVE--------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falseNegative++;
			}
			if (result.getValidationOutcome().equals(TravelValidity.VALID) && !track.getValidationResult().isValid() && !Boolean.TRUE.equals(track.getSwitchValidity())) {
				System.err.println("---------POSITIVE--------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falsePositive++;
			}
			
//			List<Geolocation> points = preprocessTrack(track);
//			long duration = points.size() < 2 ? 0: (points.get(points.size()-1).getRecorded_at().getTime() - points.get(0).getRecorded_at().getTime()) / 1000;
//			float distance = 0;
//			int effectiveTrackLength = points.size() / 2 + 1;
//			for (int i = 1; i < points.size(); i++) {
//				distance += 1000.0*GamificationHelper.harvesineDistance(points.get(i), points.get(i-1));
//			}
//
//			int accuratePoints = 0;			
//			for (Geolocation g : track.getGeolocationEvents()) {
//				if (g.getAccuracy() < ACCURACY_THRESHOLD) accuratePoints++;				
//			}
//
//			float timeFreq = (float)(duration > 0 ? 60.0 * effectiveTrackLength / duration : 0); // per minute
//			float distFreq =  (float)(distance > 0 ? 1000.0 * effectiveTrackLength / distance : 0); // per km
//			float accuracy = (float) (track.getGeolocationEvents().size() > 0 ? (100.0 * accuratePoints / track.getGeolocationEvents().size()) : 0);
//			
//			String print =  "----------------------------\n" +
//							String.format("%s - %s - %s:\n", track.getUserId(), track.getDay(), track.getId()) + 
//							String.format("      stats (duration / length / points / dist freq. / time freq. / accuracy): %d / %.0f / %d / %.4f / %.4f / %.2f", duration, (float)distance, (int) effectiveTrackLength, distFreq, timeFreq, accuracy);
//						
//			if (points.size() <= 2) {
////				System.err.println(print);
////				System.err.println(String.format("%s - %s - %s: INVALID LENGTH (VALUATED AS: %b / %s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
//				return;
//			}
//			// split: 20km/h and 3 mins for stop
//			TrackSplit trackSplit = new TrackSplit(points, 20, 3*60*1000, 1*60*1000);
//			if (trackSplit.getFastIntervals().isEmpty()) {
//				System.err.println(print);
//				System.err.println(String.format("%s - %s - %s: TOO SLOW (VALUATED AS: %b / %s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
//				return;
//			}
////			int[] first = trackSplit.getFastIntervals().getFirst();
////			int[] last = trackSplit.getFastIntervals().getLast();
////			points = points.subList(first[0], first[1]);
//
//			Map<List<Geolocation>, Double> trackPrecision = new LinkedHashMap<>();
//			int matchedFragments = 0;
//			
//			for (List<Geolocation> subtrack: trackSplit.fastFragments()) {
//				int effectiveLength = subtrack.size() / 2 + 1;
//				int invalid = effectiveLength;
//				double subtrackPrecision = 0;
//				for (List<Geolocation> ref: tracks) {
//					invalid = Math.min(trackMatch(subtrack, ref, ACCURACY_THRESHOLD),invalid); 
//					subtrackPrecision = 100.0 * (effectiveLength-invalid) / (effectiveLength);
//					if (subtrackPrecision > VALIDITY_THRESHOLD) break;
//				}
//				trackPrecision.put(subtrack, subtrackPrecision);
//				if (subtrackPrecision > VALIDITY_THRESHOLD) matchedFragments++;
//			}
//			
////			if (matchedFragments != trackPrecision.size()) {
//			if ((!track.getValidationResult().isValid() || track.getSwitchValidity() != null) && matchedFragments > 0) {
//				System.err.println(print);
//				final double refDistance = distance;
//				final int totalPoints = points.size();
//				trackPrecision.keySet().forEach(f -> {
//					double fdistance = 0;
//					for (int i = 1; i < f.size(); i++) {
//						fdistance += 1000.0*GamificationHelper.harvesineDistance(f.get(i), f.get(i-1));
//					}
//
//					System.err.println(String.format("      ------- %s - %s: %.2f (%.2f%% of points, %.2f%% of distance)",DT_FORMATTER.format(f.get(0).getRecorded_at()), DT_FORMATTER.format(f.get(f.size()-1).getRecorded_at()), trackPrecision.get(f), (float)(100.0*f.size()/totalPoints), fdistance/refDistance*100));
//				});
//				System.err.println(String.format("      VALIDITY = %d / %d (VALUATED AS: %b / %s)",  matchedFragments, trackPrecision.size(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
//			}
//			
		}
		System.err.println(falseNegative);
		System.err.println(falsePositive);
	}

	@Test
	public void validateBuses() throws Exception {
		Query q = Query.query(Criteria
				.where("freeTrackingTransport").is("bus")
				.and("day").gte("2017/09/09")
//				.and("id").is("59b987f59045ea472f1892b8")
				);
		q.fields().include("id");
		List<TrackedInstance> planned = template.find(q, TrackedInstance.class, "trackedInstances");
		
		System.err.println("COUNT = "+planned.size());
		int falseNegative = 0, falsePositive = 0;
		
		for (TrackedInstance t : planned) {
			ExtTrackedInstance track = template.findOne(Query.query(Criteria.where("id").is(t.getId())), ExtTrackedInstance.class, "trackedInstances");
			if (!Boolean.TRUE.equals(track.getComplete())) continue;
			
			ValidationStatus result = validator.validateFreeTracking(track.getGeolocationEvents(), "bus", APP_ID).getValidationStatus();
			
			if (result.getValidationOutcome().equals(TravelValidity.PENDING) && track.getValidationResult().getValidationStatus().equals(TravelValidity.VALID) && (track.getValidationResult().isValid() || TravelValidity.VALID.equals(track.getChangedValidity()))) {
				System.err.println("---------NEGATIVE--------");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getChangedValidity()));
				System.err.println(result);
				falseNegative++;
			}
			if (result.getValidationOutcome().equals(TravelValidity.VALID) && (TravelValidity.INVALID.equals(track.getChangedValidity()))) {
				
				System.err.println("---------POSITIVE--------");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getChangedValidity()));
				System.err.println(result);
				falsePositive++;
			}
		}
		System.err.println(falseNegative);
		System.err.println(falsePositive);
	}
	
	@Test
	public void validateBusMatrix() {
		assertTrue("Bus data descriptior should exist", validator.BUS_DESCRIPTOR != null);
		assertTrue("Bus data descriptior dimensions should be set", validator.BUS_DESCRIPTOR.getWidth() > 0 && validator.BUS_DESCRIPTOR.getHeight() > 0);
		assertTrue("Bus data descriptior matrix should be initialized", validator.BUS_DESCRIPTOR.getStopMatrix().size() > 0);
	}
	
	@Test
	public void validatePlanned() throws Exception {
		Query q = Query.query(Criteria
				.where("itinerary").exists(true)
				.and("day").gte("2016/09/10")
				.and("id").is("59b8c0ad9045ea472f18912b")
				);
		q.fields().include("id");
		List<TrackedInstance> planned = template.find(q, TrackedInstance.class, "trackedInstances");
		
		int falsePending = 0, falseNegative = 0, falsePositive = 0, total = 0;
		
		for (TrackedInstance t : planned) {
			ExtTrackedInstance track = template.findOne(Query.query(Criteria.where("id").is(t.getId())), ExtTrackedInstance.class, "trackedInstances");
			if (track.getItinerary().getData().getLeg().size() <= 1) continue;
			
			total++;
			
			ValidationStatus result = validator.validatePlannedJourney(track.getItinerary(), track.getGeolocationEvents(), APP_ID).getValidationStatus();
			
			if (result.getValidationOutcome().equals(TravelValidity.VALID) && (!track.getValidationResult().isValid() && track.getSwitchValidity() == null || Boolean.FALSE.equals(track.getSwitchValidity()))) {
				System.err.println("---------POSITIVE --------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falsePositive++;
			}
			if (result.getValidationOutcome().equals(TravelValidity.PENDING) && track.getValidationResult().isValid()) {
//				System.err.println("---------PENDING--------\n");
//				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
//				System.err.println(result);
				falsePending++;
			}
			if (result.getValidationOutcome().equals(TravelValidity.INVALID) && (track.getValidationResult().isValid() || Boolean.TRUE.equals(track.getSwitchValidity()))) {
				System.err.println("---------NEGATIVE--------\n");
				System.err.println(String.format("%s - %s - %s: validated as (%s /%s)", track.getUserId(), track.getDay(), track.getId(), track.getValidationResult().isValid(), ""+track.getSwitchValidity()));
				System.err.println(result);
				falseNegative++;
			}
		}
		System.err.println("COUNT = "+total);
		System.err.println(falsePositive);
		System.err.println(falsePending);
		System.err.println(falseNegative);
	}
	
	
	/**
	 * @param track
	 * @return
	 */
	private boolean isSinglePT(TrackedInstance i) {
		List<Leg> legs = i.getItinerary().getData().getLeg();
		Map<TType, Long> map = legs.stream().collect(Collectors.groupingBy(l -> l.getTransport().getType(), Collectors.counting()));
		map.remove(TType.WALK);
		return map.size() == 1 && map.values().iterator().next() == 1; 
	}

}
