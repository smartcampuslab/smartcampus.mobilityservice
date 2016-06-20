package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.RType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import eu.trentorise.smartcampus.mobility.controller.extensions.definitive.SortType;

@JsonIgnoreProperties(ignoreUnknown=true)
public class PlanningResultGroup {

	private String name;
	private Integer maxEntries = Integer.MAX_VALUE;
//	private SortType sortType = SortType.fastest;
	private SortType sortType = null;
	
	public static final PlanningResultGroup FAKE_GROUP = new PlanningResultGroup(); 
	
	public PlanningResultGroup() {
	}
			
//	public PlanningResultGroup(String name, Integer maxEntries) {
//		super();
//		this.name = name;
//		this.maxEntries = maxEntries;
//	}
	
	public PlanningResultGroup(String name, Integer maxEntries, SortType sortType) {
		super();
		this.name = name;
		this.maxEntries = maxEntries;
		this.sortType = sortType;
	}
		
	public PlanningResultGroup(String name, Integer maxEntries, RType rType) {
		super();
		this.name = name;
		this.maxEntries = maxEntries;
		this.sortType = SortType.convertType(rType);
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

	public SortType getSortType() {
		return sortType;
	}

	public void setSortType(SortType sortType) {
		this.sortType = sortType;
	}
	
	
	
}
