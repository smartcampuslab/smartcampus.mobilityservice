package eu.trentorise.smartcampus.mobility.gamification.statistics;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import eu.trentorise.smartcampus.mobility.gamification.TrackValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationStatus;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

@Component
public class StatisticsBuilder {

	private static final String GLOBAL_STATISTICS = "globalStatistics";

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;
	
	private StatisticsGroup computeStatistics(String userId, String from, String to, AggregationGranularity granularity) throws Exception {
		StatisticsGroup result = statsByGranularity(userId, from, to, granularity);
//		result.setGlobalStats(getGlobalStatistics(userId, start).getStats());
		return result;
	}
	
	public StatisticsGroup computeStatistics(String userId, long from, long to, AggregationGranularity granularity) throws Exception {
		String fromDay = sdf.format(new Date(from));
		String toDay = sdf.format(new Date(to));
		
		return computeStatistics(userId, fromDay, toDay, granularity);
	}	
	
	public GlobalStatistics getGlobalStatistics(String userId, String start, boolean dates) throws Exception {
		Criteria criteria = new Criteria("userId").is(userId);
		criteria = criteria.and("updateTime").gt(System.currentTimeMillis() - 1000 * 60 * 60);
		Query query = new Query(criteria);
		
		GlobalStatistics statistics = template.findOne(query, GlobalStatistics.class, GLOBAL_STATISTICS);
		if (statistics == null) {
			statistics = new GlobalStatistics();
			statistics.setUserId(userId);
			statistics.setUpdateTime(System.currentTimeMillis());
			statistics.setStats(computeGlobalStatistics(userId, start, dates));
			template.save(statistics, GLOBAL_STATISTICS);
		}
		
		return statistics;
	}
	
	private Map<AggregationGranularity, Map<String, Object>> computeGlobalStatistics(String userId, String start, boolean dates) throws Exception {
		Map<AggregationGranularity, Map<String, Object>> result = Maps.newHashMap();
		for (AggregationGranularity granularity: AggregationGranularity.values()) {
			result.put(granularity, computeGlobalStatistics(userId, start, granularity, dates));
		}
		
		return result;
	}
	
	private Map<String, Object> computeGlobalStatistics(String userId, String start, AggregationGranularity granularity, boolean dates) throws Exception {
		List<TrackedInstance> instances = findAll(userId);
		Multimap<String, TrackedInstance> byDay = groupByDay(instances);
		Multimap<Range, TrackedInstance> byWeek = mergeByGranularity(byDay, granularity, start, sdf.format(new Date()));
		Map<Range, Map<String,Double>> rangeSum = statsByRanges(byWeek);
		
		Map<String, Object> result = Maps.newTreeMap();
		for (Range range: rangeSum.keySet()) {
			Map<String,Double> value = rangeSum.get(range);
			for (String key: value.keySet()) {
				if (value.get(key) > (Double)result.getOrDefault("max " + key, 0.0)) {
					result.put("max " + key, Math.max((Double)result.getOrDefault("max " + key, 0.0), value.get(key)));
					if (dates) {
						result.put("date max " + key, convertRange(range));
					}
				}
			}
		}
		
		return result;
	}
	
	private Map<String, Long> convertRange(Range range) throws Exception {
		Map<String, Long> result = Maps.newTreeMap();
		result.put(range.from, sdf.parse(range.from).getTime());
		result.put(range.to, sdf.parse(range.to).getTime());
		return result;
	}
	
	private StatisticsGroup statsByGranularity(String userId, String from, String to, AggregationGranularity granularity) throws Exception {
		List<TrackedInstance> instances = find(userId, from, to);
		Multimap<String, TrackedInstance> byDay = groupByDay(instances);
		Multimap<Range, TrackedInstance> byWeek = mergeByGranularity(byDay, granularity, from, to);
		Map<Range, Map<String,Double>> rangeSum = statsByRanges(byWeek);
		
		Map<String, String> outside = outside(userId, from, to);
		
		StatisticsGroup result = new StatisticsGroup();
		if (outside.containsKey("before")) {
			result.setFirstBefore(sdf.parse(outside.get("before")).getTime());
		}
		if (outside.containsKey("after")) {
			result.setFirstAfter(sdf.parse(outside.get("after")).getTime());
		}
		
		List<StatisticsAggregation> aggregations = Lists.newArrayList();
		for (Range range: rangeSum.keySet()) {
			StatisticsAggregation aggregation = new StatisticsAggregation();
			aggregation.setFrom(sdf.parse(range.from).getTime());
			aggregation.setTo(endOfDay(range.to));
			aggregation.setData(rangeSum.get(range));
			aggregations.add(aggregation);
		}
		Collections.sort(aggregations);
		result.setStats(aggregations);
		return result;
	}	
	
	private List<TrackedInstance> findAll(String userId) {
		Criteria criteria = new Criteria("userId").is(userId);//.and("validationResult.validationStatus.validationOutcome").is(TravelValidity.VALID);
		criteria.orOperator(
				new Criteria("validationResult.validationStatus.validationOutcome").is(TravelValidity.VALID).and("changedValidity").is(null),
				new Criteria("changedValidity").is(TravelValidity.VALID));
		Query query = new Query(criteria);
		query.fields().include("validationResult.validationStatus").include("day").include("freeTrackingTransport").include("itinerary");
		
		List<TrackedInstance> result = template.find(query, TrackedInstance.class, "trackedInstances");
		
		result = result.stream().filter(x -> x.getDay() != null).collect(Collectors.toList());
		
		return result;
	}	
	
	private List<TrackedInstance> find(String userId, String from, String to) {
		Criteria criteria = new Criteria("userId").is(userId);
		criteria.orOperator(
				new Criteria("validationResult.validationStatus.validationOutcome").is(TravelValidity.VALID).and("changedValidity").is(null),
				new Criteria("changedValidity").is(TravelValidity.VALID));
		criteria = criteria.andOperator(Criteria.where("day").gte(from).lte(to));
		Query query = new Query(criteria);
		query.fields().include("validationResult.validationStatus").include("day").include("freeTrackingTransport");
		
		List<TrackedInstance> result = template.find(query, TrackedInstance.class, "trackedInstances");
		
		result = result.stream().filter(x -> x.getDay() != null).collect(Collectors.toList());
		
		return result;
	}
	
	private Map<String, String> outside(String userId, String from, String to) {
		Map<String, String> result = Maps.newTreeMap();
		
		Criteria criteria = new Criteria("userId").is(userId).and("validationResult.validationStatus.distance").gt(0.0); // .and("validationResult.valid").is(true)
		criteria = criteria.and("day").lt(from);
		Query query = new Query(criteria);
		query.with(new Sort(Sort.Direction.DESC, "day"));
		query.fields().include("day");
		
		TrackedInstance before = template.findOne(query, TrackedInstance.class, "trackedInstances");
		if (before != null) {
			result.put("before", before.getDay());
		}
		
		criteria = new Criteria("userId").is(userId).and("validationResult.validationStatus.distance").gt(0.0); // .and("validationResult.valid").is(true)
		criteria = criteria.and("day").gt(to);
		query = new Query(criteria);
		query.with(new Sort(Sort.Direction.ASC, "day"));
		query.fields().include("day");		
		
		TrackedInstance after = template.findOne(query, TrackedInstance.class, "trackedInstances");
		
		if (after != null) {
			result.put("after", after.getDay());
		}		
		
		return result;
	}	
	
	
	private Multimap<String, TrackedInstance> groupByDay(List<TrackedInstance> instances) {
		Multimap<String, TrackedInstance> result = Multimaps.index(instances, TrackedInstance::getDay);
		return result;
	}
	
	private Multimap<Range, TrackedInstance> mergeByGranularity(Multimap<String, TrackedInstance> byDay, AggregationGranularity granularity, String from, String to) throws Exception {
		Multimap<Range, TrackedInstance> result = ArrayListMultimap.create(); 
		Multimap<Range, String> weekDays = ArrayListMultimap.create();

		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		for (String day: byDay.keySet()) {
			Range range = buildRanges(day, granularity, from, to);
			weekDays.put(range, day);
		}
		
		for (Range range: weekDays.keySet()) {
			for (String day: weekDays.get(range)) {
				result.putAll(range, byDay.get(day));
			}
		}

		return result;
	}	
	
	private Map<Range, Map<String,Double>> statsByRanges(Multimap<Range, TrackedInstance> group) {
		Map<Range, Map<String,Double>> result = Maps.newHashMap();
		for (Range groupKey: group.keys()) {
			Map<String, Double> statByRange = Maps.newTreeMap();
			for (TrackedInstance ti: group.get(groupKey)) {
				Map<String, Double> dist = null;
				Double val = ti.getValidationResult().getDistance();
				if (val == null) {
					val = 0.0;
				}
				if (ti.getFreeTrackingTransport() != null) {
					dist = computeFreeTrackingDistances(val, ti.getFreeTrackingTransport());
//					statByRange.put("free tracking", statByRange.getOrDefault("free tracking", 0.0) + 1);
//					if (ti.getEstimatedScore() != null) {
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
//					}
					
				}
				if (ti.getItinerary() != null) {
					dist = computePlannedJourneyDistances(ti.getValidationResult().getValidationStatus());
//					statByRange.put("planned", statByRange.getOrDefault("planned", 0.0) + 1);
//					if (ti.getEstimatedScore() != null) {					
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
//					}
				}
				if (dist != null) {
					for (String key : dist.keySet()) {
						statByRange.put(key, statByRange.getOrDefault(key, 0.0) + dist.get(key));
						// statByRange.put("max " + key, Math.max(statByRange.getOrDefault("max " + key, 0.0),dist.get(key)));
					}
				}
			}
			result.put(groupKey, statByRange);
		}
		return result;
	}	
	
//	private Map<Map<String, String>, Map<String,Double>> computeGlobalStatistics(Multimap<Map<String, String>, TrackedInstance> group) {
//		Map<Map<String, String>, Map<String,Double>> result = Maps.newHashMap();
//		for (Map<String, String> groupKey: group.keys()) {
//			Map<String, Double> statByRange = Maps.newTreeMap();
//			for (TrackedInstance ti: group.get(groupKey)) {
//				Map<String, Double> dist = null;
//				if (ti.getFreeTrackingTransport() != null) {
//					dist = computeFreeTrackingDistances(ti.getValidationResult().getDistance(), ti.getFreeTrackingTransport());
//					statByRange.put("free tracking", statByRange.getOrDefault("free tracking", 0.0) + 1);
//					if (ti.getScore() != null) {
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getScore());
//					}
//					
//				}
//				if (ti.getItinerary() != null) {
//					dist = computePlannedJourneyDistances(ti.getItinerary());
//					statByRange.put("planned", statByRange.getOrDefault("planned", 0.0) + 1);
//					if (ti.getScore() != null) {					
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getScore());
//					}
//				}
//				for (String key: dist.keySet()) {
//					statByRange.put(key, statByRange.getOrDefault(key, 0.0) + dist.get(key));
//					statByRange.put("max " + key, Math.max(statByRange.getOrDefault("max " + key, 0.0), dist.get(key)));
//				}
//			}
//			result.put(groupKey, statByRange);
//		}
//		return result;
//	}		
	
	private Map<String, Double> computePlannedJourneyDistances(ValidationStatus vs) {
		Map<String, Double> result = Maps.newTreeMap();
		if (vs.getPlannedDistances() != null && !vs.getPlannedDistances().isEmpty()) {
			vs.getPlannedDistances().entrySet().forEach(entry -> result.put(TrackValidator.toModeString(entry.getKey()), entry.getValue()));
		}
		return result;
	}
	
	private Map<String, Double> computeFreeTrackingDistances(Double distance, String ttype) {
		Map<String, Double> result = Maps.newTreeMap();
		result.put(GamificationHelper.convertFreetrackingType(ttype), distance);
		return result;
	}	
	
	private Range buildRanges(String day, AggregationGranularity granularity, String from, String to) throws Exception {
		switch (granularity) {
			case day:
				return new Range(day, day);
			case week: {
				Range range = new Range();
				Calendar c = Calendar.getInstance();
				c.setFirstDayOfWeek(Calendar.MONDAY);
				c.setTimeInMillis(sdf.parse(day).getTime());
				c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				range.from = sdf.format(new Date(c.getTimeInMillis()));
				c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				range.to = sdf.format(new Date(c.getTimeInMillis()));
				return range;
			}	
			case month: {
				Range range = new Range();
				Calendar c = Calendar.getInstance();
				c.setFirstDayOfWeek(Calendar.MONDAY);
				c.set(Calendar.DAY_OF_MONTH, 1);
				range.from = sdf.format(new Date(c.getTimeInMillis()));
				c.add(Calendar.MONTH, 1);
				c.add(Calendar.DAY_OF_YEAR, -1);
				range.to = sdf.format(new Date(c.getTimeInMillis()));
				return range;
			}	
			case total:
			default:
				return new Range(from, to);
		}
	}	
	
	private long endOfDay(String day) throws Exception {
		Calendar c = new GregorianCalendar();
		c.setTime(sdf.parse(day));
		c.add(Calendar.DAY_OF_YEAR, 1);
		c.add(Calendar.SECOND, -1);
		return c.getTimeInMillis();
	}
	
	private static class Range {
		String from, to;

		public Range() {}

		public Range(String from, String to) {
			super();
			this.from = from;
			this.to = to;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((to == null) ? 0 : to.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Range other = (Range) obj;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (to == null) {
				if (other.to != null)
					return false;
			} else if (!to.equals(other.to))
				return false;
			return true;
		}
		
		
	}

}
