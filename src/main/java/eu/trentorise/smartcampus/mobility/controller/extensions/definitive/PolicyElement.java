package eu.trentorise.smartcampus.mobility.controller.extensions.definitive;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyElement {

	public enum ParametricPolicyRequestType {
		generate, modify, evaluate, filter, group;
	}

	private String name;
	private PolicyCondition condition;
	private PolicyAction action;

	private ParametricPolicyRequestType type; // ???

	private boolean enabled = true;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PolicyCondition getCondition() {
		return condition;
	}

	public void setCondition(PolicyCondition condition) {
		this.condition = condition;
	}

	public PolicyAction getAction() {
		return action;
	}

	public void setAction(PolicyAction action) {
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
