package eu.trentorise.smartcampus.mobility.dev.paths;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.smartcampus.mobility.geolocation.model.Location;

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableConfigurationProperties
public class GenerateGeolocationsTest {

	@Autowired
	private GeolocationsGenerator generator;

	@Test
	public void generation() throws Exception {
		List<Location> locations = generator.generate("5714ed86e4b0a36b3ea40b67", 0.1, 0.01, 1, 1, false);
		
		ObjectMapper mapper = new ObjectMapper();
		
		System.err.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(locations));
		
	}
	
}
