package eu.trentorise.smartcampus.mobility.controller.extensions.compilable;

import it.sayservice.platform.smartplanner.data.message.RType;

public enum SortType {
		fastest(RType.fastest),
		healthy(RType.healthy),
		leastWalking(RType.leastWalking),
		leastChanges(RType.leastChanges),
		greenest(RType.greenest),
		safest(RType.safest),
		fastestAndCheapest(null);	
		
		private RType matchingRType;
		
		private SortType(RType matchingRType) {
			this.matchingRType = matchingRType;
		}

		public static SortType convertType(RType rType) {
			for (SortType st: SortType.values()) {
				if (st.matchingRType == rType) {
					return st;
				}
			}
			return null;
		}
	
}
