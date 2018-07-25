package eu.trentorise.smartcampus.mobility.controller.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningPolicy;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;
import eu.trentorise.smartcampus.mobility.controller.extensions.compilable.CompilablePolicyData;
import eu.trentorise.smartcampus.mobility.controller.extensions.compilable.VelocityCompiler;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerService;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@RequestMapping(value = "/policies")
@ApiIgnore
public class PoliciesController {

	@Autowired
	private DomainStorage domainStorage;

	@Autowired
	private SmartPlannerService plannerService;

	@RequestMapping(method = RequestMethod.POST, value = "/compiled")
	public @ResponseBody CompilablePolicyData saveCompiledPolicy(@RequestBody(required=false) CompilablePolicyData policy, @RequestParam(required = false) Boolean compileOnly, HttpServletResponse response)
			throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		// System.out.println(mapper.writeValueAsString(policy));

		if (policy.getName() == null || policy.getDescription() == null) {
			if (compileOnly == null || !compileOnly) {
				response.addHeader("error_msg", "\"Nome\" e \"Descrizione\" sono obbligatori.");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return policy;
			}
		}
		if (policy.getGroups() != null) {
			HashSet<String> groupNames = Sets.newHashSet();
			for (PlanningResultGroup group : policy.getGroups()) {
				if (groupNames.contains(group.getName())) {
					response.addHeader("error_msg", "I nomi dei gruppi devono essere differenti tra loro.");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return policy;
				}
				groupNames.add(group.getName());
			}
		}
		try {
			VelocityCompiler velo = new VelocityCompiler();
			velo.compile(policy);
			velo.check(policy);
			if (compileOnly == null || !compileOnly) {
				domainStorage.savePolicy(policy);
				response.addHeader("msg", "Politica salvata.");
			}
		} catch (Exception e) {
			response.addHeader("error_msg", e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return policy;
	}
	
	
	@RequestMapping(method = RequestMethod.POST, value = "/compile")
	public @ResponseBody CompilablePolicyData compile(@RequestBody(required=false) CompilablePolicyData policy, @RequestParam(required = false) Boolean generate, @RequestParam(required = false) Boolean evaluate, @RequestParam(required = false) Boolean extract, @RequestParam(required = false) Boolean filter, HttpServletResponse response)
			throws Exception {
		try {
			VelocityCompiler velo = new VelocityCompiler();
			velo.compile(policy, generate, evaluate, extract, filter);
			velo.check(policy);
		} catch (Exception e) {
			response.addHeader("error_msg", e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return policy;		
	}
	
	

	@RequestMapping(method = RequestMethod.GET, value = "/compiled/{name}")
	public @ResponseBody CompilablePolicyData getCompiledPolicy(@PathVariable String name, HttpServletResponse response) throws Exception {
		Map<String, Object> query = Maps.newTreeMap();
		query.put("name", name);
		CompilablePolicyData result = domainStorage.searchDomainObject(query, CompilablePolicyData.class);
		if (result == null) {
			response.addHeader("error_msg", "Politica non trovata.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		return result;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/compiled/{name}")
	public @ResponseBody void deleteCompiledPolicy(@PathVariable String name, HttpServletResponse response) throws Exception {
		Criteria criteria = new Criteria("name").is(name);
		domainStorage.deleteDomainObject(criteria, CompilablePolicyData.class);
	}

	// /////////////

	@RequestMapping(method = RequestMethod.GET, value = "/")
	public @ResponseBody List<Map> list(@RequestParam(required = false) Boolean draft, HttpServletResponse response) throws Exception {
		List<Map> result = Lists.newArrayList();
		List<PlanningPolicy> pols = Lists.newArrayList(plannerService.getPolicies(draft).values());
		Collections.sort(pols, new Comparator<PlanningPolicy>() {

			@Override
			public int compare(PlanningPolicy o1, PlanningPolicy o2) {
				boolean b1 = (o1.getDraft() == null || o1.getDraft());
				boolean b2 = (o2.getDraft() == null || o2.getDraft());
				return (b1 ? 1 : 0) - (b2 ? 1 : 0);
			}
		});
		for (PlanningPolicy policy : pols) {
			Map<String, Object> policyMap = Maps.newTreeMap();
			policyMap.put("name", policy.getName());
			policyMap.put("description", policy.getDescription());
			policyMap.put("draft", policy.getDraft());
			policyMap.put("editable", true);
			policyMap.put("policyType", policy.getPolicyType());
			result.add(policyMap);
			// map.put(policy.getName(), policyMap);
		}
		return result;
	}

	/*
	 * // TODO: only "scripted" now
	 * 
	 * @RequestMapping(method = RequestMethod.GET, value = "/scripted/{name}")
	 * public @ResponseBody ScriptedPolicy listScripted(@PathVariable String
	 * name, HttpServletResponse response) throws Exception { Map <String,
	 * Object> query = Maps.newTreeMap(); query.put("name", name);
	 * ScriptedPolicy result = domainStorage.searchDomainObject(query,
	 * ScriptedPolicy.class); if (result == null) {
	 * response.addHeader("error_msg", "Politica non trovata.");
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); } return result;
	 * }
	 * 
	 * // TODO: only "scripted" now
	 * 
	 * @RequestMapping(method = RequestMethod.GET, value = "/parametric/{name}")
	 * public @ResponseBody ParametricPolicyRequest listParametric(@PathVariable
	 * String name, HttpServletResponse response) throws Exception { Map
	 * <String, Object> query = Maps.newTreeMap(); query.put("name", name);
	 * ParametricPolicyRequest result = domainStorage.searchDomainObject(query,
	 * ParametricPolicyRequest.class); if (result == null) {
	 * response.addHeader("error_msg", "Politica non trovata.");
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); } return result;
	 * }
	 * 
	 * 
	 * 
	 * @RequestMapping(method = RequestMethod.DELETE, value =
	 * "/scripted/{name}") public @ResponseBody void delete(@PathVariable String
	 * name, HttpServletResponse response) throws Exception { Criteria criteria
	 * = new Criteria("name").is(name);
	 * domainStorage.deleteDomainObject(criteria, ScriptedPolicy.class); }
	 * 
	 * 
	 * @RequestMapping(method = RequestMethod.POST, value = "/parametric")
	 * public @ResponseBody void savePolicy(@RequestBody(required=false) ParametricPolicyRequest
	 * policy, HttpServletResponse response) throws Exception { ObjectMapper
	 * mapper = new ObjectMapper();
	 * System.out.println(mapper.writeValueAsString(policy)); if
	 * (policy.getName() == null || policy.getDescription() == null) {
	 * response.addHeader("error_msg",
	 * "\"Nome\" e \"Descrizione\" sono obbligatori.");
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; } try {
	 * domainStorage.savePolicy(policy);
	 * 
	 * response.addHeader("msg", "Politica salvata."); } catch (Exception e) {
	 * response.addHeader("error_msg", e.getMessage());
	 * response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); } }
	 * 
	 * // @RequestMapping(method = RequestMethod.PUT, value = "/scripted",
	 * consumes = {MediaType.APPLICATION_JSON_VALUE,
	 * MediaType.APPLICATION_XML_VALUE}) // public @ResponseBody void
	 * updatePolicy(@RequestBody(required=false) ScriptedPolicy policy, HttpServletResponse
	 * response) throws Exception { // if (policy.getName() == null) { //
	 * response.addHeader("error_msg", "\"Nome\" è obbligatorio."); //
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // return; // }
	 * // domainStorage.savePolicy(policy, true); // response.addHeader("msg",
	 * "Politica salvata."); // }
	 * 
	 * 
	 * @RequestMapping(method = RequestMethod.POST, value = "/scripted",
	 * consumes = {MediaType.APPLICATION_JSON_VALUE,
	 * MediaType.APPLICATION_XML_VALUE}) public @ResponseBody void
	 * saveScripted(@RequestBody(required=false) ScriptedPolicy policy, HttpServletResponse
	 * response) throws Exception { if (policy.getName() == null ||
	 * policy.getDescription() == null) { response.addHeader("error_msg",
	 * "\"Nome\" e \"Descrizione\" sono obbligatori.");
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
	 * 
	 * try { CompilerConfiguration conf = CompilerConfiguration.DEFAULT;
	 * Compiler compiler = new Compiler(conf);
	 * 
	 * compiler.compile("1", ScriptedPlanningPolicy.IMPORTS +
	 * policy.getGeneratePlanRequests()); compiler.compile("2",
	 * ScriptedPlanningPolicy.IMPORTS + policy.getEvaluatePlanResults());
	 * compiler.compile("3", ScriptedPlanningPolicy.IMPORTS +
	 * policy.getExtractItinerariesFromPlanResults()); compiler.compile("4",
	 * ScriptedPlanningPolicy.IMPORTS + policy.getFilterAndSortItineraries());
	 * 
	 * if (!domainStorage.savePolicy(policy, false)) {
	 * response.addHeader("error_msg", "Politica già  esistente.");
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
	 * response.addHeader("msg", "Politica salvata.");
	 * 
	 * } catch (MultipleCompilationErrorsException e) { for (Object o :
	 * e.getErrorCollector().getErrors()) { Message msg = (Message)o;
	 * StringWriter sw = new StringWriter(); PrintWriter pw = new
	 * PrintWriter(sw, true); msg.write(pw); response.addHeader("error_msg",
	 * sw.getBuffer().toString());
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); } } }
	 * 
	 * @RequestMapping(method = RequestMethod.PUT, value = "/scripted", consumes
	 * = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	 * public @ResponseBody void updateScripted(@RequestBody(required=false) ScriptedPolicy
	 * policy, HttpServletResponse response) throws Exception { if
	 * (policy.getName() == null || policy.getDescription() == null) {
	 * response.addHeader("error_msg",
	 * "\"Nome\" e \"Descrizione\" sono obbligatori.");
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
	 * 
	 * try { CompilerConfiguration conf = CompilerConfiguration.DEFAULT;
	 * Compiler compiler = new Compiler(conf);
	 * 
	 * compiler.compile("1", ScriptedPlanningPolicy.IMPORTS +
	 * policy.getGeneratePlanRequests()); compiler.compile("2",
	 * ScriptedPlanningPolicy.IMPORTS + policy.getEvaluatePlanResults());
	 * compiler.compile("3", ScriptedPlanningPolicy.IMPORTS +
	 * policy.getExtractItinerariesFromPlanResults()); compiler.compile("4",
	 * ScriptedPlanningPolicy.IMPORTS + policy.getFilterAndSortItineraries());
	 * 
	 * domainStorage.savePolicy(policy, true); response.addHeader("msg",
	 * "Politica salvata.");
	 * 
	 * } catch (MultipleCompilationErrorsException e) { for (Object o :
	 * e.getErrorCollector().getErrors()) { Message msg = (Message)o;
	 * StringWriter sw = new StringWriter(); PrintWriter pw = new
	 * PrintWriter(sw, true); msg.write(pw); response.addHeader("error_msg",
	 * sw.getBuffer().toString());
	 * response.setStatus(HttpServletResponse.SC_BAD_REQUEST); } } }
	 */

	@RequestMapping("/console")
	public String viewConsole() {
		return "policiesconsole";
	}

}
