package eu.trentorise.smartcampus.mobility.util;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;




public class ScriptingHelper {
	
	Binding binding;
	GroovyShell shell;

	public ScriptingHelper() {
		binding = new Binding();
		shell = new GroovyShell(binding);
	}
	
	public void test(SingleJourney singleJourney) {
		try {
		binding.setVariable("singleJourney", singleJourney);
		System.out.println(shell.evaluate("\"$singleJourney.routeType\""));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
