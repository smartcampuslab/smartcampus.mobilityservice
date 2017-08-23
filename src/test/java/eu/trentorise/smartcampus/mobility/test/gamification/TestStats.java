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

package eu.trentorise.smartcampus.mobility.test.gamification;

import static org.junit.Assert.*;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;

import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.trentorise.smartcampus.mobility.gamification.statistics.AggregationGranularity;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsBuilder;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsGroup;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.storage.ItineraryObject;
import eu.trentorise.smartcampus.mobility.storage.RecurrentJourneyObject;
import eu.trentorise.smartcampus.mobility.test.RemoteTestConfig;
import eu.trentorise.smartcampus.mobility.test.TestConfig;

/**
 * @author raman
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RemoteTestConfig.class})
public class TestStats {

	private final static String USER = "8";

	@Autowired
	StatisticsBuilder statBuilder;
	
	@Before
	public void init() {
	}
	
	@Test
	public void testStats() throws Exception {
		StatisticsGroup 
		stats = statBuilder.computeStatistics(USER, 0, System.currentTimeMillis(), AggregationGranularity.total);
		assertTrue("Total stats should not be empty", stats.getStats().size() > 0);
		stats = statBuilder.computeStatistics(USER, 0, System.currentTimeMillis(), AggregationGranularity.month);
		assertTrue("Total stats should not be empty", stats.getStats().size() > 0);
		stats = statBuilder.computeStatistics(USER, 0, System.currentTimeMillis(), AggregationGranularity.week);
		assertTrue("Total stats should not be empty", stats.getStats().size() > 0);
		stats = statBuilder.computeStatistics(USER, 0, System.currentTimeMillis(), AggregationGranularity.day);
		assertTrue("Total stats should not be empty", stats.getStats().size() > 0);
		
	}

}
