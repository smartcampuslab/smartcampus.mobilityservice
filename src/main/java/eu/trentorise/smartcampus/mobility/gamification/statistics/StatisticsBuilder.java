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

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult.TravelValidity;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

@Component
public class StatisticsBuilder {

	private static final String GLOBAL_STATISTICS = "globalStatistics";

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
	
	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;
	
	public StatisticsGroup computeStatistics(String userId, String start, String from, String to, AggregationGranularity granularity) throws Exception {
		StatisticsGroup result = statsByGranularity(userId, from, to, granularity);
//		result.setGlobalStats(getGlobalStatistics(userId, start).getStats());
		return result;
	}
	
	public StatisticsGroup computeStatistics(String userId, long start, long from, long to, AggregationGranularity granularity) throws Exception {
		String fromDay = sdf.format(new Date(from));
		String toDay = sdf.format(new Date(to));
		String startDay = sdf.format(new Date(start));
		
		return computeStatistics(userId, startDay, fromDay, toDay, granularity);
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
		Multimap<Map<String, String>, TrackedInstance> byWeek = mergeByGranularity(byDay, granularity, start, sdf.format(new Date()));
		Map<Map<String, String>, Map<String,Double>> rangeSum = statsByRanges(byWeek);
		
		Map<String, Object> result = Maps.newTreeMap();
		for (Map<String, String> range: rangeSum.keySet()) {
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
	
	private Map<String, Long> convertRange(Map<String, String> range) throws Exception {
		Map<String, Long> result = Maps.newTreeMap();
		for (String key: range.keySet()) {
			result.put(key, sdf.parse(range.get(key)).getTime());
		}
		return result;
	}
	
	private StatisticsGroup statsByGranularity(String userId, String from, String to, AggregationGranularity granularity) throws Exception {
		List<TrackedInstance> instances = find(userId, from, to);
		Multimap<String, TrackedInstance> byDay = groupByDay(instances);
		Multimap<Map<String, String>, TrackedInstance> byWeek = mergeByGranularity(byDay, granularity, from, to);
		Map<Map<String, String>, Map<String,Double>> rangeSum = statsByRanges(byWeek);
		
		Map<String, String> outside = outside(userId, from, to);
		
		StatisticsGroup result = new StatisticsGroup();
		if (outside.containsKey("before")) {
			result.setFirstBefore(sdf.parse(outside.get("before")).getTime());
		}
		if (outside.containsKey("after")) {
			result.setFirstAfter(sdf.parse(outside.get("after")).getTime());
		}
		
		List<StatisticsAggregation> aggregations = Lists.newArrayList();
		for (Map<String, String> range: rangeSum.keySet()) {
			StatisticsAggregation aggregation = new StatisticsAggregation();
			aggregation.setFrom(sdf.parse(range.get("from")).getTime());
			aggregation.setTo(endOfDay(range.get("to")));
			aggregation.setData(rangeSum.get(range));
			aggregations.add(aggregation);
		}
		Collections.sort(aggregations);
		result.setStats(aggregations);
		return result;
	}	
	
	private List<TrackedInstance> findAll(String userId) {
		Criteria criteria = new Criteria("userId").is(userId).and("validationResult.travelValidity").is(TravelValidity.VALID);
		Query query = new Query(criteria);
		query.fields().include("validationResult.distance").include("day").include("freeTrackingTransport").include("itinerary");
		
		List<TrackedInstance> result = template.find(query, TrackedInstance.class, "trackedInstances");
		
		result = result.stream().filter(x -> x.getDay() != null).collect(Collectors.toList());
		
		return result;
	}	
	
	private List<TrackedInstance> find(String userId, String from, String to) {
		Criteria criteria = new Criteria("userId").is(userId).and("validationResult.travelValidity").is(TravelValidity.VALID);
		criteria = criteria.andOperator(Criteria.where("day").gte(from).lte(to));
		Query query = new Query(criteria);
		query.fields().include("validationResult.distance").include("day").include("freeTrackingTransport").include("itinerary");
		
		List<TrackedInstance> result = template.find(query, TrackedInstance.class, "trackedInstances");
		
		result = result.stream().filter(x -> x.getDay() != null).collect(Collectors.toList());
		
		return result;
	}
	
	private Map<String, String> outside(String userId, String from, String to) {
		Map<String, String> result = Maps.newTreeMap();
		
		Criteria criteria = new Criteria("userId").is(userId).and("validationResult.distance").gt(0.0); // .and("validationResult.valid").is(true)
		criteria = criteria.and("day").lt(from);
		Query query = new Query(criteria);
		query.with(new Sort(Sort.Direction.DESC, "day"));
		query.fields().include("day");
		
		TrackedInstance before = template.findOne(query, TrackedInstance.class, "trackedInstances");
		if (before != null) {
			result.put("before", before.getDay());
		}
		
		criteria = new Criteria("userId").is(userId).and("validationResult.distance").gt(0.0); // .and("validationResult.valid").is(true)
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
	
	private Multimap<Map<String, String>, TrackedInstance> mergeByGranularity(Multimap<String, TrackedInstance> byDay, AggregationGranularity granularity, String from, String to) throws Exception {
		Multimap<Map<String, String>, TrackedInstance> result = ArrayListMultimap.create(); 
		Multimap<Map<String, String>, String> weekDays = ArrayListMultimap.create();

		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		for (String day: byDay.keySet()) {
			Map<String, String> range = buildRanges(day, granularity, from, to);
			weekDays.put(range, day);
		}
		
		for (Map<String, String> range: weekDays.keySet()) {
			for (String day: weekDays.get(range)) {
				result.putAll(range, byDay.get(day));
			}
		}

		return result;
	}	
	
	private Map<Map<String, String>, Map<String,Double>> statsByRanges(Multimap<Map<String, String>, TrackedInstance> group) {
		Map<Map<String, String>, Map<String,Double>> result = Maps.newHashMap();
		for (Map<String, String> groupKey: group.keys()) {
			Map<String, Double> statByRange = Maps.newTreeMap();
			for (TrackedInstance ti: group.get(groupKey)) {
				Map<String, Double> dist = null;
				if (ti.getFreeTrackingTransport() != null) {
					dist = computeFreeTrackingDistances(ti.getValidationResult().getDistance(), ti.getFreeTrackingTransport());
					statByRange.put("free tracking", statByRange.getOrDefault("free tracking", 0.0) + 1);
//					if (ti.getEstimatedScore() != null) {
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
//					}
					
				}
				if (ti.getItinerary() != null) {
					dist = computePlannedJourneyDistances(ti.getItinerary());
					statByRange.put("planned", statByRange.getOrDefault("planned", 0.0) + 1);
//					if (ti.getEstimatedScore() != null) {					
//						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
//					}
				}
				for (String key: dist.keySet()) {
					statByRange.put(key, statByRange.getOrDefault(key, 0.0) + dist.get(key));
//					statByRange.put("max " + key, Math.max(statByRange.getOrDefault("max " + key, 0.0),dist.get(key)));
				}
			}
			result.put(groupKey, statByRange);
		}
		return result;
	}	
	
	private Map<Map<String, String>, Map<String,Double>> computeGlobalStatistics(Multimap<Map<String, String>, TrackedInstance> group) {
		Map<Map<String, String>, Map<String,Double>> result = Maps.newHashMap();
		for (Map<String, String> groupKey: group.keys()) {
			Map<String, Double> statByRange = Maps.newTreeMap();
			for (TrackedInstance ti: group.get(groupKey)) {
				Map<String, Double> dist = null;
				if (ti.getFreeTrackingTransport() != null) {
					dist = computeFreeTrackingDistances(ti.getValidationResult().getDistance(), ti.getFreeTrackingTransport());
					statByRange.put("free tracking", statByRange.getOrDefault("free tracking", 0.0) + 1);
					if (ti.getEstimatedScore() != null) {
						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
					}
					
				}
				if (ti.getItinerary() != null) {
					dist = computePlannedJourneyDistances(ti.getItinerary());
					statByRange.put("planned", statByRange.getOrDefault("planned", 0.0) + 1);
					if (ti.getEstimatedScore() != null) {					
						statByRange.put("score", statByRange.getOrDefault("score", 0.0) + ti.getEstimatedScore());
					}
				}
				for (String key: dist.keySet()) {
					statByRange.put(key, statByRange.getOrDefault(key, 0.0) + dist.get(key));
					statByRange.put("max " + key, Math.max(statByRange.getOrDefault("max " + key, 0.0), dist.get(key)));
				}
			}
			result.put(groupKey, statByRange);
		}
		return result;
	}	
	
	
	
	
	private Map<String, Double> computePlannedJourneyDistances(ItineraryObject itinerary) {
		Map<String, Double> result = Maps.newTreeMap();
		itinerary.getData().getLeg().stream().forEach(x -> result.put(GamificationHelper.convertTType(x.getTransport().getType()), result.getOrDefault(GamificationHelper.convertTType(x.getTransport().getType()), 0.0) + x.getLength() / 1000));
		return result;
	}
	
	private Map<String, Double> computeFreeTrackingDistances(double distance, String ttype) {
		Map<String, Double> result = Maps.newTreeMap();
		result.put(ttype, distance);
		return result;
	}	
	
	private Map<String, String> buildRanges(String day, AggregationGranularity granularity, String from, String to) throws Exception {
		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTime(sdf.parse(day));
		return buildRanges(c.getTimeInMillis(), granularity, sdf.parse(from).getTime(), sdf.parse(to).getTime());
	}
	
	private Map<String, String> buildRanges(long day, AggregationGranularity granularity, long fromTime, long toTime) throws Exception {
		 Map<String, String> result = Maps.newTreeMap();
		
		Calendar c = new GregorianCalendar();
		c.setFirstDayOfWeek(Calendar.MONDAY);
		c.setTimeInMillis(day);
		
		long from = 0;
		long to = 0;
		
		switch (granularity) {
			case day:
				from = day;
				to = day;
				break;
			case week:
				c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
//				c.add(Calendar.DAY_OF_YEAR, -2); // saturday?
				from = c.getTimeInMillis();
				c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
//				c.add(Calendar.DAY_OF_YEAR, -2) // friday?
				to = c.getTimeInMillis();
				break;
			case month:
				c.set(Calendar.DAY_OF_MONTH, 1);
				from = c.getTimeInMillis();
				c.add(Calendar.MONTH, 1);
				c.add(Calendar.DAY_OF_YEAR, -1);
				to = c.getTimeInMillis();
				break;
			case total:
				from = fromTime;
				to = toTime;
		}
		
		result.put("from", sdf.format(new Date(from)));
		result.put("to", sdf.format(new Date(to)));

		return result;
	}	
	
	private long endOfDay(String day) throws Exception {
		Calendar c = new GregorianCalendar();
		c.setTime(sdf.parse(day));
		c.add(Calendar.DAY_OF_YEAR, 1);
		c.add(Calendar.SECOND, -1);
		return c.getTimeInMillis();
	}

}
