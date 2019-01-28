package eu.trentorise.smartcampus.mobility.config;

import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.google.common.collect.Maps;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;

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
public class MobilityConfig implements WebMvcConfigurer {

	
	private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {
		"classpath:/META-INF/resources/", "classpath:/resources/",
		"classpath:/static/", "classpath:/public/" };	

	@Value("${statlogging.dbname}")
	private String logDB;
	
	
	public MobilityConfig() {
		super();
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
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
		template.indexOps("trackedInstances").ensureIndex(new Index("time", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("userId", Direction.ASC));
		template.indexOps("trackedInstances").ensureIndex(new Index("appId", Direction.ASC));
//		template.setWriteConcern(new WriteConcern(1).withJournal(false).withWTimeout(200, TimeUnit.MILLISECONDS));
		template.setWriteConcern(new WriteConcern(1).withWTimeout(200, TimeUnit.MILLISECONDS));
		return template;
	}
	
	@Bean(name = "basicPoliciesMap")
	public Map<String, PlanningPolicy> getBasicPoliciesMap() {
		Map<String, PlanningPolicy> result = Maps.newTreeMap();
		result.put("default", new TrentoPlanningPolicy() );
		result.put( "Dummy", new DummyPlanningPolicy());
		result.put("Nessuna", new DummyPlanningPolicy() );
		result.put("Trento", new TrentoPlanningPolicy() );
		result.put( "Rovereto", new RoveretoPlanningPolicy());
		result.put( "New Trento", new NewTrentoPlanningPolicy());
		return result;
	}

	@Bean(name = "messageSource")
	public ResourceBundleMessageSource getResourceBundleMessageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("Messages");
		return source;
	}
	
	@Bean
	public ExecutorService getExecutors() {
		return Executors.newCachedThreadPool();
	}


	 @Override
	 public void addResourceHandlers(ResourceHandlerRegistry registry) {
		 registry.addResourceHandler("/**").addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS);	
//		 registry
//		      .addResourceHandler("/avatar/**")
//		      .addResourceLocations("file:///" + imagesDir)
//		      .setCachePeriod(3600);
////		      .resourceChain(true);
////		      .addResolver(new PathResourceResolver());
	 }
	 
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("PUT", "DELETE", "GET", "POST").allowedOrigins("*");
	} 	 
	
//	@Bean
//	public FileTemplateResolver  svgTemplateResolver() {
//		FileTemplateResolver  svgTemplateResolver = new FileTemplateResolver ();
//		svgTemplateResolver.setPrefix("/public/images/gamification/");
//		svgTemplateResolver.setSuffix(".svg");
//		svgTemplateResolver.setTemplateMode("XML");
//		svgTemplateResolver.setCharacterEncoding("UTF-8");
//		svgTemplateResolver.setOrder(0);
//
//		return svgTemplateResolver;
//	}	
	 
	@Bean
	public LocaleResolver localeResolver()
	{
	    SessionLocaleResolver slr = new SessionLocaleResolver();
	    slr.setDefaultLocale(Locale.ITALIAN);
	    return slr;
	}
	
	@Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
	
//	@Bean
//	public OncePerRequestFilter noContentFilter() {
//		return new CheckHeaderFilter();
//	}		
//	
//	
//	private class CheckHeaderFilter extends OncePerRequestFilter {
//
//		@Override
//		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//
//			String appId = request.getHeader("appId");
//			if (appId != null && !appId.isEmpty()) {
//				AppInfo app = MobilityConfig.this.appSetup.findAppById(appId);
//				if (app == null) {
//					response.sendError(HttpServletResponse.SC_FORBIDDEN);
//				} else if (app.getGameId() != null) {
//					GameInfo game = gameSetup.findGameById(app.getGameId());
//					if (game == null || game.getSend() == null || !game.getSend()) {
//						response.sendError(HttpServletResponse.SC_FORBIDDEN);
//					}
//				}
//			}
//
//			filterChain.doFilter(request, response);
//		}
//	}
	
	
}
