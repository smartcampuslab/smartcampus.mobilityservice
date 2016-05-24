package eu.trentorise.smartcampus.mobility.controller.extensions.request;


public class EvaluableRequest {

	private String type;

	private String formula;
	
	private Boolean promoted;
	private Integer iteration;
	
	private Boolean wheelchair;
	
	public EvaluableRequest() {
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getPromoted() {
		return promoted;
	}

	public void setPromoted(Boolean promoted) {
		this.promoted = promoted;
	}

	public Integer getIteration() {
		return iteration;
	}

	public void setIteration(Integer iteration) {
		this.iteration = iteration;
	}

	public Boolean getWheelchair() {
		return wheelchair;
	}

	public void setWheelchair(Boolean wheelchair) {
		this.wheelchair = wheelchair;
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}

		
	
}
