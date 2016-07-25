package eu.trentorise.smartcampus.mobility.controller.extensions.compilable;

import java.util.List;

import com.google.common.collect.Lists;

public class PolicyFilter {

	private List<String> formulas;
	private List<Boolean> enabled;
	private SortType sortType = SortType.fastest;
	private int keep = 2;
	
	public PolicyFilter() {
		formulas = Lists.newArrayList();
	}
	
	public List<String> getFormulas() {
		return formulas;
	}
	public void setFormulas(List<String> formulas) {
		this.formulas = formulas;
	}
	public List<Boolean> getEnabled() {
		return enabled;
	}

	public void setEnabled(List<Boolean> enabled) {
		this.enabled = enabled;
	}

	public SortType getSortType() {
		return sortType;
	}
	public void setSortType(SortType sortType) {
		this.sortType = sortType;
	}
	public int getKeep() {
		return keep;
	}
	public void setKeep(int keep) {
		this.keep = keep;
	}
	
	
	
}
