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

package eu.trentorise.smartcampus.mobility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import it.sayservice.platform.smartplanner.data.message.otpbeans.TransitTimeTable;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import eu.trentorise.smartcampus.mobility.model.Timetable;

/**
 * @author raman
 *
 */
public class TestTransitTimeConverter {

	@Test
	public void testConversion() throws JsonParseException, JsonMappingException, IOException {
		TransitTimeTable ttt = new ObjectMapper().readValue(getClass().getResourceAsStream("/transittimes.json"), TransitTimeTable.class);
		assertNotNull(ttt);
		
		Timetable timetable = Timetable.fromTransitTimeTable(ttt);
		assertNotNull(timetable);
		assertEquals(ttt.getStopsId(), timetable.getStopIds());
		assertEquals(ttt.getStops(), timetable.getStopNames());
		assertEquals(ttt.getTripIds().get(0).size(), timetable.getTrips().size());
	}
}
