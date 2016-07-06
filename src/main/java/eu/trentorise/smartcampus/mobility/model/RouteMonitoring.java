package eu.trentorise.smartcampus.mobility.model;


public class RouteMonitoring {

	protected String clientId;
	
	protected String agencyId;
	protected String routeId;
	
	protected ItineraryRecurrency recurrency;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getAgencyId() {
		return agencyId;
	}

	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
	}

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public ItineraryRecurrency getRecurrency() {
		return recurrency;
	}

	public void setRecurrency(ItineraryRecurrency recurrency) {
		this.recurrency = recurrency;
	}
	
}
