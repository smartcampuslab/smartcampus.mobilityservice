package eu.trentorise.smartcampus.mobility.controller.extensions.model;

public class RemoveItinerariesPolicy {

	private Integer maxEndtimeFromMaxTime;
	private Integer maxEndtimeFromMinTime;

	private Integer maxDurationFixed;
	private Integer maxDurationCoefficient;

	private Integer maxDistanceCoefficient;

	private Integer promotedToKeep;

	public Integer getMaxEndtimeFromMaxTime() {
		return maxEndtimeFromMaxTime;
	}

	public void setMaxEndtimeFromMaxTime(Integer maxEndtimeFromMaxTime) {
		this.maxEndtimeFromMaxTime = maxEndtimeFromMaxTime;
	}

	public Integer getMaxEndtimeFromMinTime() {
		return maxEndtimeFromMinTime;
	}

	public void setMaxEndtimeFromMinTime(Integer maxEndtimeFromMinTime) {
		this.maxEndtimeFromMinTime = maxEndtimeFromMinTime;
	}

	public Integer getMaxDurationFixed() {
		return maxDurationFixed;
	}

	public void setMaxDurationFixed(Integer maxDurationFixed) {
		this.maxDurationFixed = maxDurationFixed;
	}

	public Integer getMaxDurationCoefficient() {
		return maxDurationCoefficient;
	}

	public void setMaxDurationCoefficient(Integer maxDurationCoefficient) {
		this.maxDurationCoefficient = maxDurationCoefficient;
	}

	public Integer getMaxDistanceCoefficient() {
		return maxDistanceCoefficient;
	}

	public void setMaxDistanceCoefficient(Integer maxDistanceCoefficient) {
		this.maxDistanceCoefficient = maxDistanceCoefficient;
	}

	public Integer getPromotedToKeep() {
		return promotedToKeep;
	}

	public void setPromotedToKeep(Integer promotedToKeep) {
		this.promotedToKeep = promotedToKeep;
	}

}
