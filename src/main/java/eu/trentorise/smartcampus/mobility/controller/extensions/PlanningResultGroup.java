package eu.trentorise.smartcampus.mobility.controller.extensions;

public class PlanningResultGroup {

	private String name;
	private Integer maxEntries = Integer.MAX_VALUE;
	
	public static final PlanningResultGroup FAKE_GROUP = new PlanningResultGroup(); 
	
	public PlanningResultGroup() {
	}
			
	public PlanningResultGroup(String name, Integer maxEntries) {
		super();
		this.name = name;
		this.maxEntries = maxEntries;
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
	
	
	
}
