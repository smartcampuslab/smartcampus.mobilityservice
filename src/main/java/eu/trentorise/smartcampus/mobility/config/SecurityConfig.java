package eu.trentorise.smartcampus.mobility.config;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.token.JdbcTokenStore;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import eu.trentorise.smartcampus.mobility.security.CustomAuthenticationProvider;
import eu.trentorise.smartcampus.mobility.security.FixedSerialVersionUUIDJdbcTokenStore;
import eu.trentorise.smartcampus.resourceprovider.filter.ResourceAuthenticationProvider;
import eu.trentorise.smartcampus.resourceprovider.filter.ResourceFilter;
import eu.trentorise.smartcampus.resourceprovider.jdbc.JdbcServices;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@ComponentScan("eu.trentorise.smartcampus.resourceprovider")
public class SecurityConfig {

	@Value("${jdbc.driver}")
	private String jdbcDriver;
	@Value("${jdbc.url}")
	private String jdbcUrl;
	@Value("${jdbc.user}")
	private String jdbcUser;
	@Value("${jdbc.password}")
	private String jdbcPassword;
	
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
	    auth
	    .authenticationProvider(getCustomAuthenticationProvider())
	    .authenticationProvider(getResourceAuthenticationProvider());

	}	
	
	@Bean(name="dataSource")
	public BasicDataSource getDataSource() {
		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName(jdbcDriver);
		ds.setUrl(jdbcUrl);
		ds.setUsername(jdbcUser);
		ds.setPassword(jdbcPassword);
		
		return ds;
	}
	
	@Bean(name="authServices")
	public JdbcServices getAuthServices() {
		return new JdbcServices(getDataSource());
	}
	
	@Bean
	public JdbcTokenStore getTokenStore() {
		return new FixedSerialVersionUUIDJdbcTokenStore(getDataSource());
	}
	
	@Bean
	public ResourceAuthenticationProvider getResourceAuthenticationProvider() {
		ResourceAuthenticationProvider rap = new ResourceAuthenticationProvider();
		rap.setTokenStore(getTokenStore());
		rap.setAuthServices(getAuthServices());
		return rap;
	}
	
	@Bean
	public CustomAuthenticationProvider getCustomAuthenticationProvider() {
		return new CustomAuthenticationProvider();
	}
	
	public SavedRequestAwareAuthenticationSuccessHandler getSavedRequestAwareAuthenticationSuccessHandler() {
		SavedRequestAwareAuthenticationSuccessHandler h = new SavedRequestAwareAuthenticationSuccessHandler();
		h.setUseReferer(true);
		return h;
		
	}
	
    @Bean(name="oauthAuthenticationEntryPoint")
    public OAuth2AuthenticationEntryPoint getOAuth2AuthenticationEntryPoint() {
    	return new OAuth2AuthenticationEntryPoint();
    }
    
    @Bean(name="oAuth2AccessDeniedHandler")
    public OAuth2AccessDeniedHandler getOAuth2AccessDeniedHandler() {
    	return new OAuth2AccessDeniedHandler();
    }    
    
//    @Bean(name="resourceFilter")
//    public ResourceFilter getResourceFilter() throws Exception {
//    	ResourceFilter rf = new ResourceFilter();
//    	rf.setAuthenticationManager(authenticationManager());
//    	return rf;
//    }  
    
    @Configuration
    @EnableWebSecurity
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    @Order(2)                                                        
    public static class ServiceSecurityConfig extends WebSecurityConfigurerAdapter {
    
    	@Override
    	protected void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    		
    		http.antMatcher("/servicedata/**").authorizeRequests().antMatchers("/servicedata/**").hasRole("SERVICE").and().httpBasic();
    	}    	
    	
    }    
    
    @Configuration
    @EnableWebSecurity
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    @Order(3)                                                        
    public static class HttpSecurityConfig extends WebSecurityConfigurerAdapter {
    
    	@Override
    	protected void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		http.rememberMe();		
    		
//    		http.antMatcher("/gamification/console/**").authorizeRequests().antMatchers("/gamification/console/**").fullyAuthenticated().and()
//    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();	
//    		
//    		http.antMatcher("/policies/console/**").authorizeRequests().antMatchers("/policies/console/**").hasRole("CONSOLE").and()
//    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();		
//    		
//    		http.antMatcher("/web/notification/**").authorizeRequests().antMatchers("/web/notification/**").fullyAuthenticated().and()
//    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();	    		
    		
    		http.authorizeRequests().antMatchers("/gamification/console/**").fullyAuthenticated().and()
    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();	
    		
    		http.authorizeRequests().antMatchers("/policies/console/**").hasRole("CONSOLE").and()
    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();		
    		
    		http.authorizeRequests().antMatchers("/web/notification/**").fullyAuthenticated().and()
    		.formLogin().loginPage("/login").permitAll().and().logout().permitAll();	
    		
    	}    	
    	
    }
    
    @Configuration
    @EnableWebSecurity
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    @Order(1)
	public static class OAuthSecurityConfig extends WebSecurityConfigurerAdapter {
    	
        @Bean(name="resourceFilter")
        public ResourceFilter getResourceFilter() throws Exception {
        	ResourceFilter rf = new ResourceFilter();
        	rf.setAuthenticationManager(authenticationManager());
        	return rf;
        }     	
    	
    	@Override
    	public void configure(HttpSecurity http) throws Exception {
    		http.csrf().disable();
    		
    		http
    			.authorizeRequests()
    				.antMatchers("/itinerary/**","/gamification/geolocations","/gamification/freetracking/**","/gamification/freetracking/**")
    					.fullyAuthenticated()
    		.and()
    		.addFilterBefore(getResourceFilter(), RequestHeaderAuthenticationFilter.class);    		
    		
    		
    	}        	
    }    
    
	
}
