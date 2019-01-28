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

package eu.trentorise.smartcampus.mobility.processor.alerts;

import eu.trentorise.smartcampus.mobility.model.Announcement;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertAccident;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertRoad;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertStrike;

/**
 * @author raman
 *
 */
public interface AlertNotifier {

	/**
	 * @param userId
	 * @param clientId
	 * @param alert
	 * @param name 
	 */
	void notifyStrike(String userId, String clientId, String appId, AlertStrike alert, String name);

	/**
	 * @param userId
	 * @param clientId
	 * @param alert
	 * @param name 
	 */
	void notifyDelay(String userId, String clientId, String appId, AlertDelay alert, String name);

	/**
	 * @param userId
	 * @param clientId
	 * @param alert
	 * @param name 
	 */
	void notifyParking(String userId, String clientId, String appId, AlertParking alert, String name);

	/**
	 * @param userId
	 * @param clientId
	 * @param alert
	 * @param name 
	 */
	void notifyAccident(String userId, String clientId, String appId, AlertAccident alert, String name);

	/**
	 * @param userId
	 * @param clientId
	 * @param alert
	 * @param name
	 */
	void notifyRoad(String userId, String clientId, String appId, AlertRoad alert, String name);
	

	/**
	 * 
	 * @param announcement
	 * @param appId
	 */
	void notifyAnnouncement(Announcement announcement, String appId);
	

}
