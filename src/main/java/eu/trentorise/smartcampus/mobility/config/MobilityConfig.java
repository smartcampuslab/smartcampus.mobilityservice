package eu.trentorise.smartcampus.mobility.config;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.mongodb.Mongo;

import eu.trentorise.smartcampus.mobility.controller.extensions.DummyPlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.NewTrentoPlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.RoveretoPlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.TrentoPlanningPolicy;

@Configuration
@EnableWebMvc
@ComponentScan("eu.trentorise.smartcampus.mobility")
@PropertySource("classpath:mobility.properties")
@EnableAsync
@EnableScheduling
@Order(value=0)
public class MobilityConfig extends WebMvcConfigurerAdapter {

//	 @Value("${mongo.dbname:smart-planner-15x}")
//	 private String mongoDbName;	
//	
//	public @Bean
//	MongoDbFactory mongoDbFactory() throws Exception {
//		return new SimpleMongoDbFactory(new MongoClient(), mongoDbName);
//	}	
	
	@Value("${statlogging.dbname}")
	private String logDB;	
	
	public MobilityConfig() {
		super();
	}
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}	
	
	@Bean(name="logMongoTemplate")
	public MongoTemplate getLogMongoTemplate() throws UnknownHostException {
		MongoTemplate template = new MongoTemplate(new Mongo(), logDB);
		return template;
	}	
	
	@Bean(name="domainMongoTemplate")
	public MongoTemplate getDomainMongoTemplate() throws UnknownHostException {
		MongoTemplate template = new MongoTemplate(new Mongo(), "mobility-domain");
		return template;
	}
	
	@Bean(name="basicPoliciesMap")
	public Map<String, PlanningPolicy> getBasicPoliciesMap() {
		return ArrayUtils.toMap(new Object[][] {
				{"default" , new TrentoPlanningPolicy()},
				{"Dummy" , new DummyPlanningPolicy()},
				{"Nessuna" , new DummyPlanningPolicy()},
				{"Trento" , new TrentoPlanningPolicy()},
				{"Rovereto" , new RoveretoPlanningPolicy()},
				{"New Trento" , new NewTrentoPlanningPolicy()},
		});
	}
	
	@Bean
	public ExecutorService getExecutors() {
		return Executors.newCachedThreadPool();
	}
	
	@Bean
	public ViewResolver getViewResolver() {
		ContentNegotiatingViewResolver viewResolver = new ContentNegotiatingViewResolver();

		ViewResolver resolver = new InternalResourceViewResolver();
		((InternalResourceViewResolver)resolver).setPrefix("/WEB-INF/jsp/");
		((InternalResourceViewResolver)resolver).setSuffix(".jsp");
		
		viewResolver.setViewResolvers(Collections.singletonList(resolver));
		
		return resolver;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
//		registry.addResourceHandler("/web/img/**").addResourceLocations("/img/");
//		registry.addResourceHandler("/web/css/**").addResourceLocations("/css/");
//		registry.addResourceHandler("/web/lib/**").addResourceLocations("/lib/");
//		registry.addResourceHandler("/web/js/**").addResourceLocations("/js/");
//		registry.addResourceHandler("/web/fonts/**").addResourceLocations("/fonts/");
//		registry.addResourceHandler("/web/templates/**").addResourceLocations("/templates/");
		
		registry.addResourceHandler("/img/**").addResourceLocations("img/");
		registry.addResourceHandler("/css/**").addResourceLocations("css/");
		registry.addResourceHandler("/lib/**").addResourceLocations("lib/");
		registry.addResourceHandler("/js/**").addResourceLocations("js/");
		registry.addResourceHandler("/fonts/**").addResourceLocations("fonts/");
		registry.addResourceHandler("/templates/**").addResourceLocations("templates/");		
	}	
	
	
	
}
