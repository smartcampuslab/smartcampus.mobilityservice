package eu.trentorise.smartcampus.mobility.config;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.common.io.Resources;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mongodb.MongoClient;

import eu.trentorise.smartcampus.mobility.controller.extensions.DummyPlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.NewTrentoPlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.RoveretoPlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.TrentoPlanningPolicy;

@Configuration
@EnableWebMvc
@ComponentScan("eu.trentorise.smartcampus.mobility")
@PropertySource("classpath:application.yml")
@EnableAsync
@EnableScheduling
@Order(value = 0)
public class MobilityConfig extends WebMvcConfigurerAdapter {

	
	private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
		"classpath:/META-INF/resources/", "classpath:/resources/",
		"classpath:/static/", "classpath:/public/" };	

	@Value("${statlogging.dbname}")
	private String logDB;
	
	@Value("${gamification.mail.host}")
	private String host;
	@Value("${gamification.mail.port}")
	private String port;
	@Value("${gamification.mail.protocol}")
	private String protocol;
	@Value("${gamification.mail.username}")
	private String username;
	@Value("${gamification.mail.password}")
	private String password;	

	public MobilityConfig() {
		super();
		Unirest.setObjectMapper(new ObjectMapper() {
			private com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return mapper.readValue(value, valueType);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object value) {
				try {
					return mapper.writeValueAsString(value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});		
		
		Unirest.setTimeouts(10000, 45000);		
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	@Bean
	public JavaMailSender getJavaMailSender() throws IOException {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(Integer.parseInt(port));
		sender.setProtocol(protocol);
		sender.setUsername(protocol);
		sender.setPassword(password);
		
		Properties props = new Properties();
		props.load(Resources.asByteSource(Resources.getResource("javamail.properties")).openBufferedStream());
		
		sender.setJavaMailProperties(props);
		return sender;
	}
	
	@Bean
	MongoClient getMongoClient() {
		return new MongoClient("localhost", 27017);
	}

	@Bean(name = "logMongoTemplate")
	public MongoTemplate getLogMongoTemplate() throws UnknownHostException {
//		MongoTemplate template = new MongoTemplate(new Mongo("localhost", 17017), logDB);
		MongoTemplate template = new MongoTemplate(getMongoClient(), logDB);
		return template;
	}

	@Bean(name = "mongoTemplate")
	@Primary
	public MongoTemplate getDomainMongoTemplate() throws UnknownHostException {
//		MongoTemplate template = new MongoTemplate(new Mongo("localhost", 17017), "mobility-domain");
		MongoTemplate template = new MongoTemplate(getMongoClient(), "mobility-domain");
		template.indexOps("trackedInstances").ensureIndex(new Index("day", Direction.ASC));
		return template;
	}
	
	@Bean(name = "basicPoliciesMap")
	public Map<String, PlanningPolicy> getBasicPoliciesMap() {
		return ArrayUtils.toMap(new Object[][] { { "default", new TrentoPlanningPolicy() }, { "Dummy", new DummyPlanningPolicy() }, { "Nessuna", new DummyPlanningPolicy() },
				{ "Trento", new TrentoPlanningPolicy() }, { "Rovereto", new RoveretoPlanningPolicy() }, { "New Trento", new NewTrentoPlanningPolicy() }, });
	}

	@Bean
	public ExecutorService getExecutors() {
		return Executors.newCachedThreadPool();
	}


	 @Override
	 public void addResourceHandlers(ResourceHandlerRegistry registry) {
		 registry.addResourceHandler("/**").addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);		 
	 }
	 
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	} 	 
	 
	
}
