package eu.trentorise.smartcampus.mobility.test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import eu.trentorise.smartcampus.mobility.gamification.GamificationValidator;
import eu.trentorise.smartcampus.mobility.gamification.model.TrackedInstance;
import eu.trentorise.smartcampus.mobility.geolocation.model.ValidationResult;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableConfigurationProperties
//@BenchmarkOptions(callgc = false, benchmarkRounds = 1000, warmupRounds = 100, concurrency = BenchmarkOptions.CONCURRENCY_AVAILABLE_CORES)
public class ValidationPerformanceTest  {  // extends AbstractBenchmark {

	private static final int PAGE_SIZE = 100;
	private static final int THREAD_N = 10;
	private static final int TASK_N = 1000;
	
	@Autowired
	@Qualifier("mongoTemplate")
	private MongoTemplate template;		
	
	@Autowired
	private GamificationValidator validator;
	
	private ExecutorService executorService = Executors.newFixedThreadPool(THREAD_N);
	
	private File file;
	
	@Before
	public void init() throws Exception {
		file = new File("src/test/resources/bench.csv");
		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();
	}

	
	@Test
	public void test2() throws Exception {
		List<TrackedInstance> results = null;
		
		int page = 0;
		do {
			Query query = new Query();
			query.limit(PAGE_SIZE).skip(page * PAGE_SIZE);
			
			results = template.find(query, TrackedInstance.class, "trackedInstances");		
			
			for (TrackedInstance trackedInstance: results) {
				validate(trackedInstance);
			}
			
			page++;
		} while (results != null && results.size() == PAGE_SIZE);
	}
	
	
	private void validate(TrackedInstance trackedInstance) throws Exception {
		
		ValidationResult validation = validator.validateFreeTracking(trackedInstance.getGeolocationEvents(), trackedInstance.getFreeTrackingTransport(), "trentoplaygo");
		if (validation == null) {
			return;
		}
		
		Callable<Long> task = new Callable<Long>() {
	        @Override
	        public Long call() {
				long start = System.currentTimeMillis();
				try {
					validator.validateFreeTracking(trackedInstance.getGeolocationEvents(), trackedInstance.getFreeTrackingTransport(), "trentoplaygo");
				} catch (Exception e) {
					e.printStackTrace();
				}
				long duration = System.currentTimeMillis() - start;
				return duration;
	        }
	    };
	    
	    List<Callable<Long>> tasks = Collections.nCopies(TASK_N, task);
	    
	    long start = System.currentTimeMillis();
	    List<Future<Long>> futures = executorService.invokeAll(tasks);
		
	    
	    long parallelDuration = 0;
	    for (Future<Long> future : futures) {
	        parallelDuration += future.get();
	    }	    
	    
	    long duration = System.currentTimeMillis() - start;
	    
	    String type = trackedInstance.getFreeTrackingTransport() == null ? "planned" : trackedInstance.getFreeTrackingTransport();
	    String line = duration + "," + trackedInstance.getId() + "," + trackedInstance.getGeolocationEvents().size() + "," + type + "," + validation.getTravelValidity() + "," + trackedInstance.getId();
	    
	    Files.append(line + "\n", file, Charsets.UTF_8);
	    
	}
	
}
