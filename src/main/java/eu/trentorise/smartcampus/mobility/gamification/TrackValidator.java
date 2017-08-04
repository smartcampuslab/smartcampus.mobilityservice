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

package eu.trentorise.smartcampus.mobility.gamification;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.Geolocation;
import eu.trentorise.smartcampus.mobility.geolocation.model.TrackSplit;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.ERROR_TYPE;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.Interval;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.MODE_TYPE;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus.TRIP_TYPE;
import eu.trentorise.smartcampus.mobility.security.Circle;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

/**
 * @author raman
 *
 */
public class TrackValidator {

	private static final int WALK_SPEED_THRESHOLD = 20; // km/h
	private static final int BIKE_SPEED_THRESHOLD = 65; // km/h

	private static final int WALK_AVG_SPEED_THRESHOLD = 15; // km/h
	private static final int BIKE_AVG_SPEED_THRESHOLD = 27; // km/h

	public static final double VALIDITY_THRESHOLD = 80; // %
	public static final double ACCURACY_THRESHOLD = 150; // meters
	public static final int COVERAGE_THRESHOLD = 80; // %
	public static final double DISTANCE_THRESHOLD = 250; // meters
	public static final long DATA_HOLE_THRESHOLD = 10*60; // seconds

	/**
	 * Preprocess tracked data: spline, remove outstanding points, remove potentially erroneous start / stop points 
	 * @param points
	 * @return
	 */
	public static List<Geolocation> preprocessTrack(List<Geolocation> points) {
		if (points.size() > 4) points.remove(0);
		if (points.size() > 2) GamificationHelper.removeOutliers(points);
//		if (points.size() > 3) removeNoise(points);
		points = GamificationHelper.transform(points);
		return points;
	}
	
	/**
	 * Remove intermediate points. Compare distances between point and two successors. If the angle formed by
	 * 3 points is sharp and distance shift is small, remove intermediate
	 * @param points
	 */
	public static void removeNoise(List<Geolocation> points) {
		int i = 0;
		while (i < points.size()-3) {
			double dist = GamificationHelper.harvesineDistance(points.get(i), points.get(i+2));
			double dist1 = GamificationHelper.harvesineDistance(points.get(i), points.get(i+1));
			if (dist1 > 2*dist && dist < 0.05) {
				points.remove(i+1);
			} else {
				i++;
			}
		}
	}

	/**
	 * Preprocess and prevalidate track data.
	 * Check for data 'holes'
	 * Basic validation: minimal length and points, game area. 
	 * @param track
	 * @param status
	 * @param areas
	 * @return preprocessed track data
	 */
	public static List<Geolocation> prevalidate(TrackedInstance track, ValidationStatus status, List<Circle> areas) {
		// check data present
		if (track.getGeolocationEvents() == null || track.getGeolocationEvents().size() <= 1) {
			status.setError(ERROR_TYPE.NO_DATA);
			status.setValidationOutcome(TravelValidity.INVALID);
			return Collections.emptyList();
		}
		List<Geolocation> points = new ArrayList<Geolocation>(track.getGeolocationEvents());
		Collections.sort(points, (o1, o2) -> (int)(o1.getRecorded_at().getTime() - o2.getRecorded_at().getTime()));
		// check for data holes. If there is missing data, set status to PENDING and stop
//		for (int i = 1; i < points.size(); i++) {
//			long interval = points.get(i).getRecorded_at().getTime() - points.get(i-1).getRecorded_at().getTime();
//			if (interval > DATA_HOLE_THRESHOLD * 1000) {
//				double dist = GamificationHelper.harvesineDistance(points.get(i), points.get(i-1));
//				// if moving faster than 1 m/s through out the interval then data is missing
//				if (dist*1000000 > interval) {
//					status.setError(ERROR_TYPE.DATA_HOLE);
//					status.setValidationOutcome(TravelValidity.PENDING);
//					return Collections.emptyList();
//				}
//			}
//		}
		
		// preprocess
		status.computeAccuracy(points);
		points = preprocessTrack(points);
		status.updateMetrics(points);

		if (points.size() < 2) {
			status.setValidationOutcome(TravelValidity.INVALID);
			status.setError(ERROR_TYPE.TOO_SHORT);
			return points;
		}
		
		// check if the track is in the area of interest
		boolean inRange = true;
		if (areas != null && !areas.isEmpty()) {
			if (!points.isEmpty()) {
				inRange &= GamificationHelper.inAreas(areas, points.get(0));
				inRange &= GamificationHelper.inAreas(areas, points.get(points.size() - 1));
			}
		}	
		if (!inRange) {
			status.setValidationOutcome(TravelValidity.INVALID);
			status.setError(ERROR_TYPE.OUT_OF_AREA);
		}
		// min distance 
		else if (status.getDistance() < DISTANCE_THRESHOLD) {
			status.setValidationOutcome(TravelValidity.INVALID);
			status.setError(ERROR_TYPE.TOO_SHORT);
		}
		
		return points;
	}
	
	/**
	 * Validate free tracking: train. Take reference train shapes as input.
	 * Preprocess track data; check if contains more than 2 points; split into blocks with 20km/h - 3 mins for stop - at least 1 min fast track; 
	 * match fragments against reference trac with 150m error and 80% coverage. Consider VALID if at least 90% of length is matched. If no
	 * fast fragment found consider PENDING with TOO_SLOW error. Otherwise consider PENDING.
	 * @param track
	 * @param referenceTracks
	 * @return
	 */
	public static ValidationStatus validateFreeTrain(TrackedInstance track, List<List<Geolocation>> referenceTracks, List<Circle> areas) {
		ValidationStatus status = new ValidationStatus();
		
		// set parameters
		status.setTripType(TRIP_TYPE.FREE);
		status.setModeType(MODE_TYPE.TRAIN);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setMatchThreshold(ACCURACY_THRESHOLD);
		status.setCoverageThreshold(COVERAGE_THRESHOLD);

		// basic validation
		List<Geolocation> points = prevalidate(track, status, areas);
		if (status.getValidationOutcome() != null) {
			return status;
		}
		
		// split track into pieces. For train consider 15km/h threshold
		TrackSplit trackSplit = TrackSplit.fastSplit(points, 15, 3*60*1000, 1*60*1000);
		if (trackSplit.getFastIntervals().isEmpty()) {
			status.setValidationOutcome(TravelValidity.PENDING);
			status.setError(ERROR_TYPE.TOO_SLOW);
			return status;
		}
		status.updateFastSplit(trackSplit);

		// compute matches checking each fast fragment against available reference train tracks
		// check max coverage for each fragment
		// if overall distance coverage is high enough, set trip valid 
		double matchedDistance = 0;
		for (Interval interval: status.getIntervals()) {
			List<Geolocation> subtrack = trackSplit.getTrack().subList(interval.getStart(), interval.getEnd());
			int effectiveLength = subtrack.size();
			int invalid = effectiveLength;
			double subtrackPrecision = 0;
			for (List<Geolocation> ref: referenceTracks) {
				invalid = Math.min(trackMatch(subtrack, ref, status.getMatchThreshold()),invalid); 
				subtrackPrecision = 100.0 * (effectiveLength-invalid) / (effectiveLength);
				if (subtrackPrecision >= status.getValidityThreshold()) break;
			}
			interval.setMatch(subtrackPrecision);
			if (subtrackPrecision >= status.getValidityThreshold()) {
				status.setMatchedIntervals(status.getMatchedIntervals()+1);
				matchedDistance += interval.getDistance();
			}
		}
		double coverage = 100.0 * matchedDistance / status.getDistance();
		status.setValidationOutcome(coverage >= COVERAGE_THRESHOLD ? TravelValidity.VALID : TravelValidity.PENDING);
		
		return status;
	}
	
	public static ValidationStatus validateFreeWalk(TrackedInstance track, List<Circle> areas) {
		ValidationStatus status = new ValidationStatus();
		
		// set parameters
		status.setTripType(TRIP_TYPE.FREE);
		status.setModeType(MODE_TYPE.WALK);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setCoverageThreshold(COVERAGE_THRESHOLD);

		// basic validation
		List<Geolocation> points = prevalidate(track, status, areas);
		if (status.getValidationOutcome() != null) {
			return status;
		}
		
		
		// split track into pieces. For walk consider 20km/h threshold
		TrackSplit trackSplit = TrackSplit.slowSplit(points, WALK_SPEED_THRESHOLD, 20*1000, 30*1000);
		// if no slow intervals or no fast intervals and speed is high, invalid
		if (trackSplit.getSlowIntervals().isEmpty() || trackSplit.getFastIntervals().isEmpty()) {
			if (status.getAverageSpeed() > WALK_AVG_SPEED_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.INVALID);
				status.setError(ERROR_TYPE.TOO_FAST);
				return status;
			}	
		}
		status.updateSlowSplit(trackSplit, true);
		// distance should be non-trivial
		double distance = status.getEffectiveDistances().get(MODE_TYPE.WALK);
		// min distance 
		if (distance < DISTANCE_THRESHOLD) {
			// check the average speed of fast part.
			int fastPart = trackSplit.getSlowIntervals().isEmpty() ? 0 : trackSplit.getSlowIntervals().getFirst()[0];
			if (getFragmentEffectiveAverage(points, fastPart, points.size()) > WALK_AVG_SPEED_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.INVALID);
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
				return status;
			}
		}
		status.setValidationOutcome(TravelValidity.VALID);
		
		return status;
	}
	
	public static ValidationStatus validateFreeBike(TrackedInstance track, List<Circle> areas) {
		ValidationStatus status = new ValidationStatus();
		
		// set parameters
		status.setTripType(TRIP_TYPE.FREE);
		status.setModeType(MODE_TYPE.BIKE);
		status.setValidityThreshold(VALIDITY_THRESHOLD);
		status.setCoverageThreshold(COVERAGE_THRESHOLD);

		// basic validation
		List<Geolocation> points = prevalidate(track, status, areas);
		if (status.getValidationOutcome() != null) {
			return status;
		}
		
		// split track into pieces. For walk consider 20km/h threshold
		TrackSplit trackSplit = TrackSplit.slowSplit(points, BIKE_SPEED_THRESHOLD, 20*1000, 30*1000);
		// if no slow intervals or no fast intervals and speed is high, invalid
		if (trackSplit.getSlowIntervals().isEmpty() || trackSplit.getFastIntervals().isEmpty()) {
			if (status.getAverageSpeed() > BIKE_AVG_SPEED_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.INVALID);
				status.setError(ERROR_TYPE.TOO_FAST);
				return status;
			}	
		}
		status.updateSlowSplit(trackSplit, true);
		// distance should be non-trivial
		double distance = status.getEffectiveDistances().get(MODE_TYPE.BIKE);
		// min distance 
		if (distance < DISTANCE_THRESHOLD) {
			// check the average speed of fast part.
			int fastPart = trackSplit.getSlowIntervals().isEmpty() ? 0 : trackSplit.getSlowIntervals().getFirst()[0];
			if (getFragmentEffectiveAverage(points, fastPart, points.size()) > BIKE_AVG_SPEED_THRESHOLD) {
				status.setValidationOutcome(TravelValidity.INVALID);
				status.setError(ERROR_TYPE.DOES_NOT_MATCH);
				return status;
			}
		}
		status.setValidationOutcome(TravelValidity.VALID);
		
		return status;
	}
	
	private static final double getFragmentEffectiveAverage(List<Geolocation> points, int start, int end) {
		double realTime = 0;
		double remainingDistance = 0;
		double prevDist = 0;
		for (int i = start+1; i < end; i++) {
			double d = GamificationHelper.harvesineDistance(points.get(i), points.get(i - 1));
			remainingDistance += d;
			// effective average
			long t = points.get(i).getRecorded_at().getTime() - points.get(i-1).getRecorded_at().getTime();
			if (t > 0) {
				d += prevDist;
				double pointSpeed = (1000.0 * d / ((double) t / 1000)) * 3.6;
				// not still
				if (pointSpeed > 0.1 && pointSpeed < 144) {
					realTime += t;
				}
				prevDist = 0;
			} else {
				prevDist = d;
			}
		}
		realTime = realTime * 0.001;
		return ( remainingDistance / ((double) realTime)) * 3.6;
		
	}
	
	/**
	 * Verify the trac1 matches track2 with a given precision using Hausdorff-based analysis.
	 * @param track1 track to match
	 * @param track2 track with which to perform match
	 * @param error in meters
	 * @return true if the Hausdorff distance is less than specified precision
	 */
	public static int trackMatch(List<Geolocation> track1, List<Geolocation> track2, double error) {
		// normalize distance: in km and along coordinate
		final double distance = error / 1000 / 2 / Math.sqrt(2);
		
		// consider track1 is shorter and is used as a reference for matrix construction
		// identify matrix coordinates
		double[] nw = new double[]{Double.MIN_VALUE, Double.MAX_VALUE}, se = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
		track1.forEach(g -> {
			nw[0] = Math.max(nw[0], g.getLatitude()); nw[1] = Math.min(nw[1], g.getLongitude());
			se[0] = Math.min(se[0], g.getLatitude()); se[1] = Math.max(se[1], g.getLongitude());
		});
		
		// add extra row/col to matrix 
		int width = 2 + (int) Math.ceil(GamificationHelper.harvesineDistance(nw[0],nw[1], nw[0], se[1]) / distance);
		int height = 2 + (int) Math.ceil(GamificationHelper.harvesineDistance(nw[0],nw[1], se[0], nw[1]) / distance);
		
//		System.err.println("nw = "+Arrays.toString(nw));
//		System.err.println("se = "+Arrays.toString(se));
//		System.err.println("width = "+width);
//		System.err.println("height = "+height);
		
		
		// represent the cells with values to avoid sparse matrix traversal
		Map<Integer,Integer> matrix = new HashMap<>();
		// fill in the matrix from test track
		track1.forEach(g -> {
			int row = row(se[0], g.getLatitude(), g.getLongitude(), distance);
			int col = col(nw[1], g.getLatitude(), g.getLongitude(), distance);
			if (row >= height || col >= width) return; // should never happen
			matrix.put(row*width+col, matrix.getOrDefault(row*width+col,0)-1); // only first track present
		});
		int matching = 0;
		// fill in the matrix from reference track. include also additional cells
		for (Geolocation g : track2) {
			if (g.getLatitude() > nw[0] || g.getLatitude() < se[0] || g.getLongitude() < nw[1] || g.getLongitude() > se[1]) continue;
			
			int row = row(se[0], g.getLatitude(), g.getLongitude(), distance);
			int col = col(nw[1], g.getLatitude(), g.getLongitude(), distance);
			
			if (row >= height || col >= width) continue; // should never happen
						
			int idx = row*width+col;
			if ( matrix.getOrDefault(idx, 0) == 0)  matrix.put(idx, 2); // only 2nd track present
			else if ( matrix.getOrDefault(idx, 0) < 0)  matrix.put(idx, 3); // both present
			matching++;
		}
		if (matching == 0) return track1.size();
			
		int invalidCount = 0;
		
		// check the cells with the test track items only: check 8 neighbors
		for (Integer idx : matrix.keySet()) {
			if (matrix.get(idx) < 0 &&
				matrix.getOrDefault(idx-width, 0) < 2 && matrix.getOrDefault(idx+width, 0) < 2 &&
				matrix.getOrDefault(idx-width-1, 0) < 2 && matrix.getOrDefault(idx+width-1, 0) < 2 &&
				matrix.getOrDefault(idx-width+1, 0) < 2 && matrix.getOrDefault(idx+width+1, 0) < 2 &&
				matrix.getOrDefault(idx-1, 0) < 2 && matrix.getOrDefault(idx+1, 0) < 2
				) 
			{
				invalidCount -= matrix.get(idx);
				//return false;
			}
		}
		return invalidCount;
	}
	private static int col(double w, double lat, double lon, double distance) {
		return (int) Math.ceil(GamificationHelper.harvesineDistance(lat, w, lat, lon) / distance);
	}
	private static int row(double s, double lat, double lon, double distance) {
		return (int) Math.ceil(GamificationHelper.harvesineDistance(s, lon, lat, lon) / distance);
	}

	public static List<List<Geolocation>> parseShape(InputStream is) {
		return new BufferedReader(new InputStreamReader(is))
		.lines()
		.map(s -> s.split(","))
		.filter(a -> !a[0].isEmpty() && !a[0].equals("shape_id"))
		.collect(Collectors.groupingBy(a -> a[0]))
		.values()
		.stream()
		.map(list -> {
			return list.stream()
					.sorted((a,b) -> Integer.parseInt(a[3]) - Integer.parseInt(b[3]))
					.map(a -> new Geolocation(Double.parseDouble(a[1]), Double.parseDouble(a[2]), null))
					.collect(Collectors.toList());
		})
		.map(track -> fillTrace(track, 100.0 / 1000 / 2 / Math.sqrt(2)))
		.collect(Collectors.toList());
		
	}
	
	private static List<Geolocation> fillTrace(List<Geolocation> track, double distance) {
		List<Geolocation> res = new LinkedList<>();
		res.add(track.get(0));
		// preprocess the reference track: add extra points 
		for (int i = 1; i < track.size(); i++) {
			double d = GamificationHelper.harvesineDistance(track.get(i), track.get(i-1));
			for (int j = 1; j * distance < d; j++) {
				double lng = track.get(i-1).getLongitude() + (j*distance)*(track.get(i).getLongitude() - track.get(i-1).getLongitude()) / d; 
				double lat = track.get(i-1).getLatitude() + (j*distance)*(track.get(i).getLatitude() - track.get(i-1).getLatitude()) / d; 
				res.add(new Geolocation(lat, lng, null));
			}
			res.add(track.get(i));
		}
//		String poly = GamificationHelper.encodePoly(res);
//		System.err.println(poly);
		
		return res;
	}

}
