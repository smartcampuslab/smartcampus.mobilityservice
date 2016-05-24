package eu.trentorise.smartcampus.mobility.util;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.request.PlanningRequestConditionFormulaVariables;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import it.sayservice.platform.smartplanner.data.message.Position;
import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.journey.SingleJourney;

public class ScriptingHelper {
	
	private static final String IMPORTS = "import it.sayservice.platform.smartplanner.data.message.RType;\n"
			+ "import it.sayservice.platform.smartplanner.data.message.TType;\n"
			+ "import it.sayservice.platform.smartplanner.data.message.Position;\n";
			
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
	
	public boolean evaluateConditionFormula(PlanningRequest pr, String script) {
		PlanningRequestConditionFormulaVariables er = new PlanningRequestConditionFormulaVariables(pr);
		String modScript = script;
		modScript = modScript.replaceAll("(\\#[\\w]*)\\-\\>(\\[[^\\]]*\\])", "#distance((List)$1,$2)");

		modScript = modScript.replace("#", "er.");
		
		for (TType tt : TType.values()) {
			modScript = modScript.replace(tt.toString() + " ", "TType." + tt + " ");
		}
		for (RType rt : RType.values()) {
			modScript = modScript.replace(rt.toString() + " ", "RType." + rt + " ");
		}		
		binding.setVariable("er", er);
		
		System.out.println(modScript);
		return (Boolean)shell.evaluate(IMPORTS + script);
	}
	
	public static void main(String[] args) {
		ScriptingHelper sh = new ScriptingHelper();
		PlanningRequest pr = new PlanningRequest();
		Position pos = new Position("40,20");
		SingleJourney sj = new SingleJourney();
		sj.setFrom(pos);
		pr.setType(TType.TRANSIT);
		pr.setRouteType(RType.fastest);
		pr.setOriginalRequest(sj);
		
//		String script = "#.ttype == TRANSIT && #.rtype == fastest && >>([41,20]) < 2";
		String script = " #ttype == TRANSIT && #rtype == fastest && #from->[41,20] < 2";
		
		System.out.println(sh.evaluateConditionFormula(pr, script));
	}
	
}
