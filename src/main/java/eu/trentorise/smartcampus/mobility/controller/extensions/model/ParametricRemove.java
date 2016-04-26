package eu.trentorise.smartcampus.mobility.controller.extensions.model;

public class ParametricRemove {

	private int maxEndtimeFromMaxTime;
	private int maxEndtimeFromMinTime;

	private int maxDurationFixed;
	private double maxDurationCoefficient = 1;
	
	private int maxDurationFromMaxDuration;

	private double maxDistanceCoefficient = 1;

	private int promotedToKeep = 2;

	public int getMaxEndtimeFromMaxTime() {
		return maxEndtimeFromMaxTime;
	}

	public void setMaxEndtimeFromMaxTime(int maxEndtimeFromMaxTime) {
		this.maxEndtimeFromMaxTime = maxEndtimeFromMaxTime;
	}

	public int getMaxEndtimeFromMinTime() {
		return maxEndtimeFromMinTime;
	}

	public void setMaxEndtimeFromMinTime(int maxEndtimeFromMinTime) {
		this.maxEndtimeFromMinTime = maxEndtimeFromMinTime;
	}

	public int getMaxDurationFixed() {
		return maxDurationFixed;
	}

	public void setMaxDurationFixed(int maxDurationFixed) {
		this.maxDurationFixed = maxDurationFixed;
	}

	public double getMaxDurationCoefficient() {
		return maxDurationCoefficient;
	}

	public void setMaxDurationCoefficient(double maxDurationCoefficient) {
		this.maxDurationCoefficient = maxDurationCoefficient;
	}

	public int getMaxDurationFromMaxDuration() {
		return maxDurationFromMaxDuration;
	}

	public void setMaxDurationFromMaxDuration(int maxDurationFromMaxDuration) {
		this.maxDurationFromMaxDuration = maxDurationFromMaxDuration;
	}

	public double getMaxDistanceCoefficient() {
		return maxDistanceCoefficient;
	}

	public void setMaxDistanceCoefficient(double maxDistanceCoefficient) {
		this.maxDistanceCoefficient = maxDistanceCoefficient;
	}

	public int getPromotedToKeep() {
		return promotedToKeep;
	}

	public void setPromotedToKeep(int promotedToKeep) {
		this.promotedToKeep = promotedToKeep;
	}

	
}
