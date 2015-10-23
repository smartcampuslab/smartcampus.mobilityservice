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

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.alerts.Alert;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourneyParameters;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Stop;

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.Mongo;
import com.mongodb.MongoException;

import eu.trentorise.smartcampus.mobility.processor.alerts.AlertNotifier;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;

/**
 * @author raman
 *
 */
@Configuration
public class TestConfig {

	@Bean(name="domainMongoTemplate")
	public MongoTemplate getDomainMongo() throws UnknownHostException, MongoException {
		return new MongoTemplate(new Mongo(), "test-mobility");
	}
	
	@Bean
	public DomainStorage getDomainStorage() {
		return new DomainStorage();
	} 
	
	@Bean
	public AlertSender getAlertSender() {
		return new AlertSender();
	}
	
	@Bean
	public AlertNotifier getAlertNotifier() {
		return new AlertNotifier() {
			@Override
			public void notifyStrike(String userId, String clientId, AlertStrike alert, String name) {}
			@Override
			public void notifyRoad(String userId, String clientId, AlertRoad alert, String name) {}
			@Override
			public void notifyParking(String userId, String clientId, AlertParking alert, String name) {}
			@Override
			public void notifyDelay(String userId, String clientId, AlertDelay alert,String name) {}
			@Override
			public void notifyAccident(String userId, String clientId, AlertAccident alert, String name) {}
		};
	}
	
	@Bean
	public SmartPlannerHelper getSmartPlannerHelper() {
		return new SmartPlannerHelper() {
			@Override
			public String transitTimes(String agencyId, String routeId, Long from, Long to) throws Exception { return null; }
			@Override
			public List<Stop> stops(String agencyId, double lat, double lng,double radius, Integer page, Integer count) throws Exception { return null; }
			@Override
			public String stops(String agencyId, String routeId, double latitude,double longitude, double radius) throws Exception { return null; }
			@Override
			public String stops(String agencyId, String routeId) throws Exception { return null; }
			@Override
			public String stopTimetable(String agencyId, String stopId,Integer maxResults) throws Exception { return null; }
			@Override
			public String stopTimetable(String agencyId, String routeId, String stopId)	throws Exception { return null; }
			@Override
			public void sendAlert(Alert alert) throws Exception { }
			@Override
			public String routes(String agencyId) throws Exception { return null; }
			@Override
			public String roadInfoByAgency(String agencyId, Long from, Long to) throws Exception { return null; }
			@Override
			public RecurrentJourney replanRecurrent(RecurrentJourneyParameters parameters, RecurrentJourney oldJourney)	throws Exception { return null; }
			@Override
			public List<Itinerary> planSingleJourney(SingleJourney journeyRequest, int iteration)throws Exception { return null; }
			@Override
			public RecurrentJourney planRecurrent(RecurrentJourneyParameters parameters) throws Exception { return null; }
			@Override
			public String parkingsByAgency(String agencyId) throws Exception { return null; }
			@Override
			public String delays(String agencyId, String routeId, Long from, Long to) throws Exception { return null; }
			@Override
			public String bikeSharingByAgency(String agencyId) throws Exception { return null; }
			@Override
			public InputStream routesDB(String appId) throws Exception {
				return null;
			}
			@Override
			public String getVersions() throws Exception {
				return null;
			}
		};
	}
}
