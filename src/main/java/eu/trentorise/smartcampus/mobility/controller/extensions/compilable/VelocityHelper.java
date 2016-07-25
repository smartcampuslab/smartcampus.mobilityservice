package eu.trentorise.smartcampus.mobility.controller.extensions.compilable;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

public class VelocityHelper {

	public String toString(Object o) {
		if (o == null) {
			return null;
		} else if (o instanceof TType) {
			return "TType." + o;
		} else if (o instanceof RType) {
			return "RType." + o;
		} else if (o instanceof SortType) {
			return "SortType." + o;
		} else {
			return o.toString();
		}
	}
	
}
