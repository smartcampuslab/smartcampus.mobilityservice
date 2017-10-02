package eu.trentorise.smartcampus.mobility.test;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.smartcampus.mobility.service.SmartPlannerService;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Position;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PlanningTest {

	@Autowired
	private SmartPlannerService service;
	
//	@Value("${otp.url}")
//	private String otpURL;	

	@Test
	public void testPlan() throws Exception {
		SingleJourney req = new SingleJourney();
		req.setDate("09/22/2017"); // MM/DD/YYYY
		req.setDepartureTime("11:00am"); // am/pm
		req.setFrom(new Position("46.06697814655873,11.150484681129456")); // FBK
		req.setTo(new Position("46.066118375326724,11.11427754163742")); // Sanseverino
		req.setResultsNumber(3);
		req.setRouteType(RType.fastest);
		req.setTransportTypes(new TType[] {TType.BUS});
		req.setWheelchair(false);
		
		List<Itinerary> result = service.planSingleJourney(req, "Trento");
		
		ObjectMapper mapper = new ObjectMapper();
		System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
	}
	
}

