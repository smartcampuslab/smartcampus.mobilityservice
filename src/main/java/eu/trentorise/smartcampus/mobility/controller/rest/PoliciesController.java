package eu.trentorise.smartcampus.mobility.controller.rest;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.model.ParametricPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.model.ScriptedPolicy;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerService;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;

@Controller
@RequestMapping(value = "/policies")
public class PoliciesController {

	@Autowired
	private DomainStorage domainStorage;
	
	@Autowired
	private SmartPlannerService plannerService;
	
	@RequestMapping(method = RequestMethod.GET, value = "/")
	public @ResponseBody Map<String, String> list(HttpServletResponse response) throws Exception {
		Map<String, String> map = Maps.newTreeMap();
		for (PlanningPolicy policy: plannerService.getPolicies().values()) {
			map.put(policy.getName(), policy.getDescription());
		}
		return map;
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/parametric")
	public @ResponseBody void notify(@RequestBody ParametricPolicy policy, HttpServletResponse response) throws Exception {
		domainStorage.savePolicy(policy);
		plannerService.addPolicy(policy);
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/scripted", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody void notify(@RequestBody ScriptedPolicy policy, HttpServletResponse response) throws Exception {
		
//		try {
//		CompilerConfiguration conf = CompilerConfiguration.DEFAULT;
//		Compiler compiler = new Compiler(conf);
//		
//		compiler.compile("1", ScriptedPlanningPolicy.IMPORTS + policy.getGeneratePlanRequests());
//		compiler.compile("2", ScriptedPlanningPolicy.IMPORTS + policy.getEvaluatePlanResults());
//		compiler.compile("3", ScriptedPlanningPolicy.IMPORTS + policy.getExtractItinerariesFromPlanResults());
//		compiler.compile("4", ScriptedPlanningPolicy.IMPORTS + policy.getFilterAndSortItineraries());
		
		domainStorage.savePolicy(policy);
		plannerService.addPolicy(policy);
//		} catch (MultipleCompilationErrorsException e) {
//			for (Object o : e.getErrorCollector().getErrors()) {
//				Message msg = (Message)o;
//				msg.write(response.getWriter());
//			}
//		}
	}	
	
}
