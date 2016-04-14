package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Position;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;

public class RoveretoPromotedJourneyRequestConverter implements PromotedJourneyRequestConverter {

	private String places[] = new String[] { "45.889568,11.043297" };
//	private String places[] = new String[] { "45.890068,11.043747" };
	
	private List<Position> positions;
	
	private static Log logger = LogFactory.getLog(RoveretoPromotedJourneyRequestConverter.class);

	public RoveretoPromotedJourneyRequestConverter() throws IOException {
		positions = Lists.newArrayList();
		for (String place : places) {
			Position p = new Position(place);
			positions.add(p);
		}
	}
	
	public void modifyRequest(SingleJourney request) {
		if (isPromoted(request)) {
			for (int i = 0; i < request.getTransportTypes().length; i++) {
				if (request.getTransportTypes()[i].equals(TType.CAR)) {
					logger.info("Promoted trip by car, changing to park and ride.");
					request.getTransportTypes()[i] = TType.PARK_AND_RIDE;
				}
			}
		}
	}
	
	public void processRequests(List<PlanRequest> requests, int iteration) {
		for (PlanRequest pr: requests) {
			pr.setRequest("");
			if (pr.getType().equals(TType.CARWITHPARKING)) {
				pr.setRequest(pr.getRequest() + "&extraTransport=WALK");
			}
			if (pr.getType().equals(TType.TRANSIT) || pr.getType().equals(TType.BUS)) {
				if (pr.getRouteType().equals(RType.leastWalking) && iteration == 0) {
					pr.setRequest(pr.getRequest() + "&maxWalkDistance=500");
					pr.setRetryOnFail(true);
				} else {
					pr.setRequest(pr.getRequest() + "&maxWalkDistance=1000");
					pr.setRetryOnFail(false);
				}
			}			
			if (pr.getType().equals(TType.SHAREDBIKE) || pr.getType().equals(TType.SHAREDBIKE_WITHOUT_STATION) || pr.getType().equals(TType.SHAREDCAR) || pr.getType().equals(TType.SHAREDCAR_WITHOUT_STATION)
					|| pr.getType().equals(TType.CARWITHPARKING) || pr.getType().equals(TType.PARK_AND_RIDE)) {
				pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=1250");
			}
			if (pr.getType().equals(TType.SHAREDBIKE) || pr.getType().equals(TType.SHAREDBIKE_WITHOUT_STATION) || pr.getType().equals(TType.BICYCLE)) {
				pr.setRouteType(RType.safest);
			}
			if (pr.getType().equals(TType.WALK) && pr.getValue() != 0) {
				if (pr.getRouteType().equals(RType.leastWalking)) {
					pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=500");
				} else {
					pr.setRequest(pr.getRequest() + "&maxTotalWalkDistance=1000");
				}
			}
		}
	}
	
	@Override
	public void promoteJourney(List<PlanRequest> requests) {
	}		
	
//	@Override
//	public void promoteJourney(List<PlanRequest> requests) {
//		for (PlanRequest req: requests) {
//			if (!isPromoted(req.getOriginalRequest())) {
//				continue;
//			}
//			for (Itinerary it: req.getItinerary()) {
//				it.setPromoted(true);
//			}
//		}
//	}	
	
//	@Override
//	public void promoteJourney(List<PlanRequest> requests) {
//		for (PlanRequest req: requests) {
//			for (Itinerary it: req.getItinerary()) {
//				for (Leg leg: it.getLeg()) {
//					if (leg.getTransport() != null && "116".equals(leg.getTransport().getAgencyId()) && isPromoted(req.getOriginalRequest())) {
//						it.setPromoted(true);
//						break;
//					}
//				}
//			}
//		}
//	}		
	
	private boolean isPromoted(SingleJourney journeyRequest) {
	boolean promoted = false;
//	for (Position pos : positions) {
//		promoted |= (journeyRequest.getTo().getLat().equals(pos.getLat()) && journeyRequest.getTo().getLon().equals(pos.getLon()));
//	}	
	return promoted;
	}
	
}
