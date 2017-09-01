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

import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;

/**
 * @author raman
 *
 */
public class ExtTrackedInstance extends TrackedInstance {

	Boolean switchValidity;
	Long estimatedScore;

	public Boolean getSwitchValidity() {
		return switchValidity;
	}

	public void setSwitchValidity(Boolean switchValidity) {
		this.switchValidity = switchValidity;
	}

	public Long getEstimatedScore() {
		return estimatedScore;
	}

	public void setEstimatedScore(Long estimatedScore) {
		this.estimatedScore = estimatedScore;
	}

}
