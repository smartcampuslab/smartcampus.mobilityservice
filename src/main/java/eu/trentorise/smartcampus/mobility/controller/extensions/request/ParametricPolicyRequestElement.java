package eu.trentorise.smartcampus.mobility.controller.extensions.request;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class ParametricPolicyRequestElement {

	public enum ParametricPolicyRequestType {
		generate, modify, evaluate, filter, group;
	}
	
	private String name;
	
	private EvaluableRequest condition;
	private SettableRequest action;
	
	private ParametricPolicyRequestType type;
	
	private boolean enabled = true;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public EvaluableRequest getCondition() {
		return condition;
	}
	public void setCondition(EvaluableRequest condition) {
		this.condition = condition;
	}
	public SettableRequest getAction() {
		return action;
	}
	public void setAction(SettableRequest action) {
		this.action = action;
	}
	public ParametricPolicyRequestType getType() {
		return type;
	}
	public void setType(ParametricPolicyRequestType type) {
		this.type = type;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	
	
}
