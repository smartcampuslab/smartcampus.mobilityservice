package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.RType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class PlanningResultGroup {

	private String name;
	private Integer maxEntries = Integer.MAX_VALUE;
	private RType rType = null;
	
	public static final PlanningResultGroup FAKE_GROUP = new PlanningResultGroup(); 
	
	public PlanningResultGroup() {
	}
			
	public PlanningResultGroup(String name, Integer maxEntries, RType rType) {
		super();
		this.name = name;
		this.maxEntries = maxEntries;
		this.rType = rType;
	}
		
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getMaxEntries() {
		return maxEntries;
	}
	public void setMaxEntries(Integer maxEntries) {
		this.maxEntries = maxEntries;
	}
	public RType getRType() {
		return rType;
	}
	public void setRType(RType rType) {
		this.rType = rType;
	}

	@Override
	public String toString() {
		return name + ":" + maxEntries;
	}
	
}
