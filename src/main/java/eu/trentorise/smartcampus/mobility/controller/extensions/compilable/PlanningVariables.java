package eu.trentorise.smartcampus.mobility.controller.extensions.compilable;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.util.List;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest;

public class PlanningVariables {

	private TType ttype;
	private RType rtype;
	
	private double[] from;
	private double[] to;
	
	private boolean promoted;
	private int iteration;
	
	private boolean wheelchair;
	
	private int resultNumber;
	
	
	public PlanningVariables() {
	}
	
	public PlanningVariables(PlanningRequest pr) {
		ttype = pr.getType();
		rtype = pr.getRouteType();
		promoted = pr.isPromoted();
		iteration = pr.getIteration();
		if (pr.getItinerary() != null) {
			resultNumber = pr.getItinerary().size();
			
		}
		if (pr.getOriginalRequest() != null) {
			if (pr.getOriginalRequest().getFrom() != null) {
				from = new double[]{Double.parseDouble(pr.getOriginalRequest().getFrom().getLat()), Double.parseDouble(pr.getOriginalRequest().getFrom().getLon())};
			}
			if (pr.getOriginalRequest().getTo() != null) {
				to = new double[]{Double.parseDouble(pr.getOriginalRequest().getTo().getLat()), Double.parseDouble(pr.getOriginalRequest().getTo().getLon())};
			}			
		}
	}
	
	
	
	
	
	public TType getTtype() {
		return ttype;
	}

	public void setTtype(TType ttype) {
		this.ttype = ttype;
	}

	public RType getRtype() {
		return rtype;
	}

	public void setRtype(RType rtype) {
		this.rtype = rtype;
	}

	public double[] getFrom() {
		return from;
	}

	public void setFrom(double[] from) {
		this.from = from;
	}

	public double[] getTo() {
		return to;
	}

	public void setTo(double[] to) {
		this.to = to;
	}

	public boolean isPromoted() {
		return promoted;
	}

	public void setPromoted(boolean promoted) {
		this.promoted = promoted;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public boolean isWheelchair() {
		return wheelchair;
	}

	public void setWheelchair(boolean wheelchair) {
		this.wheelchair = wheelchair;
	}

	public int getResultNumber() {
		return resultNumber;
	}

	public void setResultNumber(int resultNumber) {
		this.resultNumber = resultNumber;
	}

	public double fromdistance(List<Number> coords2) {
		return Math.sqrt(Math.pow(coords2.get(0).doubleValue() - from[0] , 2) + Math.pow(coords2.get(1).doubleValue() - from[1] , 2));
	}
	
	public double todistance(double[] coords2) {
		return Math.sqrt(Math.pow(coords2[0] - to[0] , 2) + Math.pow(coords2[1] - to[1] , 2));
	}
	
//	public double distance(double[] coords1, double[] coords2) {
//		return Math.sqrt(Math.pow(coords2[0] - coords1[0] , 2) + Math.pow(coords2[1] - coords1[1] , 2));
//	}
	
	public double distance(List<Number> coords1, List<Number> coords2) {
		return Math.sqrt(Math.pow(coords2.get(0).doubleValue() - coords1.get(0).doubleValue() , 2) + Math.pow(coords2.get(1).doubleValue() - coords1.get(1).doubleValue() , 2));
	}	
	
	
}
