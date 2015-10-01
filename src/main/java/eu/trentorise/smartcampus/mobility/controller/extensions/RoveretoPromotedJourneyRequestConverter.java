package eu.trentorise.smartcampus.mobility.controller.extensions;

import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Position;
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
		}
	}
	
	@Override
	public void promoteJourney(List<PlanRequest> requests) {
		for (PlanRequest req: requests) {
			if (!isPromoted(req.getOriginalRequest())) {
				continue;
			}
			for (Itinerary it: req.getItinerary()) {
				it.setPromoted(true);
			}
		}
	}	
	
	private boolean isPromoted(SingleJourney journeyRequest) {
	boolean promoted = false;
	for (Position pos : positions) {
		promoted |= (journeyRequest.getTo().getLat().equals(pos.getLat()) && journeyRequest.getTo().getLon().equals(pos.getLon()));
	}	
	return promoted;
	}
	
}
