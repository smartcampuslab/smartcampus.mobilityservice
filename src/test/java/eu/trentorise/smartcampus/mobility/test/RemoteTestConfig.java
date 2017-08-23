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

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.Mongo;
import com.mongodb.MongoException;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.gamification.statistics.StatisticsBuilder;
import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.processor.alerts.AlertNotifier;
import eu.trentorise.smartcampus.mobility.service.AlertSender;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerHelper;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
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
import it.sayservice.platform.smartplanner.data.message.otpbeans.BikeStation;
import it.sayservice.platform.smartplanner.data.message.otpbeans.Stop;

/**
 * @author raman
 *
 */
//@Configuration
public class RemoteTestConfig {

	@Bean(name="mongoTemplate")
	public MongoTemplate getDomainMongo() throws UnknownHostException, MongoException {
		return new MongoTemplate(new Mongo("127.0.0.1",37017), "mobility-domain");
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
	StatisticsBuilder getStatBuilder(){
		return new StatisticsBuilder();
	}
	
	@Bean
	public AlertNotifier getAlertNotifier() {
		return new AlertNotifier() {
			@Override
			public void notifyStrike(String userId, String clientId, String appId, AlertStrike alert, String name) {}
			@Override
			public void notifyRoad(String userId, String clientId, String appId, AlertRoad alert, String name) {}
			@Override
			public void notifyParking(String userId, String clientId, String appId, AlertParking alert, String name) {}
			@Override
			public void notifyDelay(String userId, String clientId, String appId, AlertDelay alert,String name) {}
			@Override
			public void notifyAccident(String userId, String clientId, String appId, AlertAccident alert, String name) {}
			@Override
			public void notifyAnnouncement(Announcement announcement, String appId) {}
		};
	}
	
	@Bean
	public SmartPlannerHelper getSmartPlannerHelper() {
		return new SmartPlannerHelper() {
			@Override
			public String transitTimes(String agencyId, String routeId, Long from, Long to) throws Exception { return null; }
			@Override
			public String extendedTransitTimes(String agencyId, String routeId, Long from, Long to) throws Exception { return null; }			
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
			public RecurrentJourney planRecurrent(RecurrentJourneyParameters parameters) throws Exception { return null; }
			@Override
			public List<Itinerary> planSingleJourney(SingleJourney journeyRequest, String policyId) throws Exception { return null; }			
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
			public InputStream extendedRoutesDB(String appId) throws Exception {
				return null;
			}			
			@Override
			public String getVersions() throws Exception {
				return null;
			}
			@Override
			public String getTaxiStations(double latitude, double longitude, double radius) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getAllTaxiStations() throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getAgencyTaxiStations(String agencyId) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public Map<String, PlanningPolicy> getPolicies(Boolean draft) {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getTaxiAgencyContacts() throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public String getTaxiAgencyContacts(String agencyId) throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public InputStream gtfs(String agencyId) throws Exception {
				return null;
			}
			@Override
			public String bikeStations() throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
			@Override
			public void addBikeSharingStations(List<BikeStation> stations) throws Exception {
				// TODO Auto-generated method stub
				
			}
			
		};
	}
}
