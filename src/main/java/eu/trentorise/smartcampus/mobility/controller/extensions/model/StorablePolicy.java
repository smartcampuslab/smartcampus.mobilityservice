package eu.trentorise.smartcampus.mobility.controller.extensions.model;

public interface StorablePolicy {

	String getName();
	void setName(String name);
	String getDescription();
	void setDescription(String description);
	void setDraft(Boolean draft);
	Boolean getDraft();
	
}
