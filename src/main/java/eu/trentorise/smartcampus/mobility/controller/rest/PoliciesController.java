package eu.trentorise.smartcampus.mobility.controller.rest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.tools.Compiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.ScriptedPlanningPolicy;
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
	public @ResponseBody List<Map> list(@RequestParam(required=false) Boolean draft, HttpServletResponse response) throws Exception {
		List<Map> result = Lists.newArrayList();
		List<PlanningPolicy> pols = Lists.newArrayList(plannerService.getPolicies(draft).values());
		Collections.sort(pols, new Comparator<PlanningPolicy>() {

			@Override
			public int compare(PlanningPolicy o1, PlanningPolicy o2) {
				boolean b1 = (o1.getDraft() == null || o1.getDraft());
				boolean b2 = (o2.getDraft() == null || o2.getDraft());
				return (b1?1:0) - (b2?1:0);
			}
		});
		for (PlanningPolicy policy: pols) {
			Map<String, Object> policyMap = Maps.newTreeMap();
			policyMap.put("name", policy.getName());
			policyMap.put("description", policy.getDescription());
			policyMap.put("draft", policy.getDraft());
			result.add(policyMap);
//			map.put(policy.getName(), policyMap);
		}
		return result;
	}

	// TODO: only "scripted" now
	@RequestMapping(method = RequestMethod.GET, value = "/{name}")
	public @ResponseBody ScriptedPolicy list(@PathVariable String name, HttpServletResponse response) throws Exception {
		Map <String, Object> query = Maps.newTreeMap();
		query.put("name", name);
		ScriptedPolicy result = domainStorage.searchDomainObject(query, ScriptedPolicy.class);
		if (result == null) {
			response.addHeader("error_msg", "Politica non trovata.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		return result;
	}	
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{name}")
	public @ResponseBody void delete(@PathVariable String name, HttpServletResponse response) throws Exception {
		Criteria criteria = new Criteria("name").is(name);
		domainStorage.deleteDomainObject(criteria, ScriptedPolicy.class);
	}	
	
	
	@RequestMapping(method = RequestMethod.POST, value = "/parametric")
	public @ResponseBody void savePolicy(@RequestBody ParametricPolicy policy, HttpServletResponse response) throws Exception {
		if (policy.getName() == null) {
			response.addHeader("error_msg", "\"Nome\" è obbligatorio.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		domainStorage.savePolicy(policy);
		
	}
	
//	@RequestMapping(method = RequestMethod.PUT, value = "/scripted", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
//	public @ResponseBody void updatePolicy(@RequestBody ScriptedPolicy policy, HttpServletResponse response) throws Exception {
//		if (policy.getName() == null) {
//			response.addHeader("error_msg", "\"Nome\" è obbligatorio.");
//			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//			return;
//		}
//		domainStorage.savePolicy(policy, true);
//		response.addHeader("msg", "Politica salvata.");
//	}	
	
	
	@RequestMapping(method = RequestMethod.POST, value = "/scripted", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody void saveScripted(@RequestBody ScriptedPolicy policy, HttpServletResponse response) throws Exception {
		if (policy.getName() == null) {
			response.addHeader("error_msg", "\"Nome\" è obbligatorio.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}		
		
		try {
		CompilerConfiguration conf = CompilerConfiguration.DEFAULT;
		Compiler compiler = new Compiler(conf);

		compiler.compile("1", ScriptedPlanningPolicy.IMPORTS + policy.getGeneratePlanRequests());
		compiler.compile("2", ScriptedPlanningPolicy.IMPORTS + policy.getEvaluatePlanResults());
		compiler.compile("3", ScriptedPlanningPolicy.IMPORTS + policy.getExtractItinerariesFromPlanResults());
		compiler.compile("4", ScriptedPlanningPolicy.IMPORTS + policy.getFilterAndSortItineraries());
		
		if (!domainStorage.savePolicy(policy, false)) {
			response.addHeader("error_msg", "Politica già  esistente.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);	
			return;
		}
		response.addHeader("msg", "Politica salvata.");
		
		} catch (MultipleCompilationErrorsException e) {
			for (Object o : e.getErrorCollector().getErrors()) {
				Message msg = (Message)o;
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw, true);
				msg.write(pw);
				response.addHeader("error_msg", sw.getBuffer().toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			}
		}
	}	
	
	@RequestMapping(method = RequestMethod.PUT, value = "/scripted", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody void updateScripted(@RequestBody ScriptedPolicy policy, HttpServletResponse response) throws Exception {
		if (policy.getName() == null) {
			response.addHeader("error_msg", "\"Nome\" è obbligatorio.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}		
		
//		try {
//		CompilerConfiguration conf = CompilerConfiguration.DEFAULT;
//		Compiler compiler = new Compiler(conf);
//		
//		compiler.compile("1", ScriptedPlanningPolicy.IMPORTS + policy.getGeneratePlanRequests());
//		compiler.compile("2", ScriptedPlanningPolicy.IMPORTS + policy.getEvaluatePlanResults());
//		compiler.compile("3", ScriptedPlanningPolicy.IMPORTS + policy.getExtractItinerariesFromPlanResults());
//		compiler.compile("4", ScriptedPlanningPolicy.IMPORTS + policy.getFilterAndSortItineraries());
		
		domainStorage.savePolicy(policy, true);
		response.addHeader("msg", "Politica salvata.");
		
//		plannerService.addPolicy(policy);
//		} catch (MultipleCompilationErrorsException e) {
//			for (Object o : e.getErrorCollector().getErrors()) {
//				Message msg = (Message)o;
//				msg.write(response.getWriter());
//			}
//		}
	}		
	
	@RequestMapping("/console")
	public String vewConsole() {
		return "/planningconsole";
	}		
	
	
	
}
