package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PlanningRequest {

	public enum SmartplannerParameter  {
		maxWalkDistance, maxTotalWalkDistance, extraTransport, maxChanges;
	}
	
	private TType type;
	private RType routeType;
	private String request;
	private String plan;
	private PlanningResultGroup group;
	private List<Itinerary> itinerary;
	private SingleJourney originalRequest;
	private int itineraryNumber;
	private Boolean retryOnEmpty = false;
	private boolean wheelChair = false;
	
	private boolean promoted = false;
	private boolean derived = false;
	
	private PlanningRequest parentRequest;
	
	private Map<SmartplannerParameter, Object> smartplannerParameters;
	
	private int iteration;
	
	public PlanningRequest() {
		itinerary = Lists.newArrayList();
		smartplannerParameters = Maps.newTreeMap();
	}
	
	public void setSmartplannerParameter(SmartplannerParameter property, Object value) {
		smartplannerParameters.put(property, value);
	}
	
	public TType getType() {
		return type;
	}
	public void setType(TType type) {
		this.type = type;
	}
	public RType getRouteType() {
		return routeType;
	}
	public void setRouteType(RType routeType) {
		this.routeType = routeType;
	}
	public String getRequest() {
		return request;
	}
	public void setRequest(String request) {
		this.request = request;
	}
	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public PlanningResultGroup getGroup() {
		return group;
	}

	public void setGroup(PlanningResultGroup group) {
		this.group = group;
	}

	public List<Itinerary> getItinerary() {
		return itinerary;
	}

	public void setItinerary(List<Itinerary> itinerary) {
		this.itinerary = itinerary;
	}
	public SingleJourney getOriginalRequest() {
		return originalRequest;
	}
	public void setOriginalRequest(SingleJourney originalRequest) {
		this.originalRequest = originalRequest;
	}

	public int getItineraryNumber() {
		return itineraryNumber;
	}

	public void setItineraryNumber(int itineraryNumber) {
		this.itineraryNumber = itineraryNumber;
	}

	public Boolean getRetryOnEmpty() {
		return retryOnEmpty;
	}

	public void setRetryOnEmpty(Boolean retryOnFail) {
		this.retryOnEmpty = retryOnFail;
	}
	
	public boolean isWheelChair() {
		return wheelChair;
	}

	public void setWheelChair(boolean wheelChair) {
		this.wheelChair = wheelChair;
	}

	public boolean isPromoted() {
		return promoted;
	}

	public void setPromoted(boolean promoted) {
		this.promoted = promoted;
	}

	public boolean isDerived() {
		return derived;
	}

	public void setDerived(boolean derived) {
		this.derived = derived;
	}

	public Map<SmartplannerParameter, Object> getSmartplannerParameters() {
		return smartplannerParameters;
	}

	public void setSmartplannerParameters(Map<SmartplannerParameter, Object> smartplannerParameters) {
		this.smartplannerParameters = smartplannerParameters;
	}

	public PlanningRequest getParentRequest() {
		return parentRequest;
	}

	public void setParentRequest(PlanningRequest parentRequest) {
		this.parentRequest = parentRequest;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}
	
	@Override
	public String toString() {
		return type.toString();
	}
	
	
}
