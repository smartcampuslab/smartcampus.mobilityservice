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
package eu.trentorise.smartcampus.mobility.controller.rest;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Leg;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.controller.extensions.compilable.SortType;

public class ItinerarySorter {

	public static void sortDisjoined(List<Itinerary> itineraries, Comparator<Itinerary> comparator) {
		List<Itinerary> promoted = Lists.newArrayList();
		List<Itinerary> notPromoted = Lists.newArrayList();
		for (Itinerary it: itineraries) {
			if (it.isPromoted()) {
				promoted.add(it);
			} else {
				notPromoted.add(it);
			}
		}
		Collections.sort(promoted, comparator);
		Collections.sort(notPromoted, comparator);
		itineraries.clear();
		itineraries.addAll(promoted);
		itineraries.addAll(notPromoted);
	}	
	
	public static void sort(List<Itinerary> itineraries, Comparator<Itinerary> comparator) {
		Collections.sort(itineraries, comparator);
	}
	
	public static Comparator<Itinerary> comparatorBySortType(SortType criterion) {
		if (criterion != null) {
			switch (criterion) {
			case fastest:
				return fastestComparator();
			case greenest:
				return greenComparator();
			case healthy:
				return healthyrComparator();
			case leastChanges:
				return leastChangesComparator();
			case leastWalking:
				return lessWalkingComparator();
			case fastestAndCheapest:
				return fastestAndCheapestComparator();
			}
		}
		
		return dummyComparator();
	}	
	
	public static Comparator<Itinerary> comparatorByRouteType(RType criterion) {
		if (criterion != null) {
			switch (criterion) {
			case fastest:
				return fastestComparator();
			case greenest:
				return greenComparator();
			case healthy:
				return healthyrComparator();
			case leastChanges:
				return leastChangesComparator();
			case leastWalking:
				return lessWalkingComparator();
			}
		}
		
		return dummyComparator();
	}
	
	public static Comparator<Itinerary> dummyComparator() {
		return new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return 0;
			}
		};
	}	
	
	public static Comparator<Itinerary> fastestComparator() {
		return new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return (int) (o1.getEndtime() - o2.getEndtime());
			}
		};
	}
	
	public static Comparator<Itinerary> greenComparator() {
		return new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeGreenness(o1) - computeGreenness(o2);
			}

			private int computeGreenness(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += leg.getLegGeometery().getLength() * leg.getTransport().getType().getGreen();
				}
				return h;
			}

		};
	}	
	
	public static Comparator<Itinerary> healthyrComparator() {
		return new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeHealthiness(o2) - computeHealthiness(o1);
			}

			private int computeHealthiness(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += leg.getLegGeometery().getLength() * leg.getTransport().getType().getHealth();
				}
				return h;
			}

		};
	}	
	
	public static Comparator<Itinerary> leastChangesComparator() {
		return new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return (int) (o1.getLeg().size() - o2.getLeg().size());
			}
		};
	}	
	
	public static Comparator<Itinerary> lessWalkingComparator() {
		return new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeWalking(o1) - computeWalking(o2);
			}

			private int computeWalking(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += (leg.getTransport().getType().equals(TType.WALK) ? leg.getLegGeometery().getLength() : 0);
				}
				return h;
			}

		};
	}		
	
	public static Comparator<Itinerary> fastestAndCheapestComparator() {
		return new Comparator<Itinerary>() {
			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				long o1End = getModifiedEndTime(o1);
				long o2End = getModifiedEndTime(o2);
				
				return (int) (o1End - o2End);
			}
		};
	}	
	
	private static long getModifiedEndTime(Itinerary it) {
		ObjectMapper mapper = new ObjectMapper();
		long endTime = it.getEndtime();
		for (Leg leg: it.getLeg()) {
			if (!leg.getTransport().getType().equals(TType.CAR)) {
				continue;
			}
			if (leg.getTo().getStopId() != null && leg.getTo().getStopId().getExtra() != null && leg.getTo().getStopId().getExtra().containsKey("costData")) {
				Map<String, String> costData = mapper.convertValue(leg.getTo().getStopId().getExtra().get("costData"), Map.class);
				Double fixedCost = costData.containsKey("fixedCost")?Double.parseDouble(costData.get("fixedCost")):0;
				endTime += (long)(8 * 1000 * 60 * fixedCost);
			}
			if (leg.getTo().getStopId() != null && leg.getTo().getStopId().getExtra() != null && leg.getTo().getStopId().getExtra().containsKey("searchTime")) {
				Map<String, String> searchTime = mapper.convertValue(leg.getTo().getStopId().getExtra().get("searchTime"), Map.class);
				Integer max = searchTime.containsKey("searchTime")?Integer.parseInt(searchTime.get("max")):0;
				endTime += max * 1000 * 60;
			}			
		}
		
		return endTime;
	}	
	
	
	
	public static void sort(List<Itinerary> itineraries, RType criterion) {
		if (criterion != null) {
			switch (criterion) {
			case fastest:
				sortFaster(itineraries);
				break;
			case greenest:
				sortByGreener(itineraries);
				break;
			case healthy:
				sortByHealthier(itineraries);
				break;
			case leastChanges:
				sortByLeastChanges(itineraries);
				break;
			case leastWalking:
				sortByLessWalking(itineraries);
				break;
			case safest:
				break;
			}
		}
	}

	private static void sortFaster(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return (int) (o1.getEndtime() - o2.getEndtime());
			}
		});
	}

	private static void sortByGreener(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeGreenness(o1) - computeGreenness(o2);
			}

			private int computeGreenness(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += leg.getLegGeometery().getLength() * leg.getTransport().getType().getGreen();
				}
				return h;
			}

		});
	}

	private static void sortByHealthier(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeHealthiness(o2) - computeHealthiness(o1);
			}

			private int computeHealthiness(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += leg.getLegGeometery().getLength() * leg.getTransport().getType().getHealth();
				}
				return h;
			}

		});
	}

	private static void sortByLeastChanges(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return (int) (o1.getLeg().size() - o2.getLeg().size());
			}
		});
	}

	private static void sortByLessWalking(List<Itinerary> itineraries) {
		Collections.sort(itineraries, new Comparator<Itinerary>() {

			@Override
			public int compare(Itinerary o1, Itinerary o2) {
				return computeWalking(o1) - computeWalking(o2);
			}

			private int computeWalking(Itinerary itinerary) {
				int h = 0;
				for (Leg leg : itinerary.getLeg()) {
					h += (leg.getTransport().getType().equals(TType.WALK) ? leg.getLegGeometery().getLength() : 0);
				}
				return h;
			}

		});
	}
	
	
	

}
