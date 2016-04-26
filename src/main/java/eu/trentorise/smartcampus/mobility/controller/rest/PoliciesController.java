package eu.trentorise.smartcampus.mobility.controller.rest;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.controller.extensions.model.ParametricPolicy;
import eu.trentorise.smartcampus.mobility.service.SmartPlannerService;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;

@Controller
@RequestMapping(value = "/policies")
public class PoliciesController {

	@Autowired
	private DomainStorage domainStorage;
	
	@Autowired
	private SmartPlannerService plannerService;
	
	@RequestMapping(method = RequestMethod.POST, value = "/policy")
	public @ResponseBody void notify(@RequestBody ParametricPolicy policy, HttpServletResponse response) throws Exception {
		domainStorage.savePolicies(policy);
		plannerService.addPolicy(policy);
	}
	
}
