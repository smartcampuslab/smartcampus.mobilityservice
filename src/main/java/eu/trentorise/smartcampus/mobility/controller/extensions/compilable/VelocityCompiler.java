package eu.trentorise.smartcampus.mobility.controller.extensions.compilable;

import it.sayservice.platform.smartplanner.data.message.RType;
import it.sayservice.platform.smartplanner.data.message.TType;

import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.implement.IncludeRelativePath;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.tools.Compiler;

import com.google.common.collect.Lists;

import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningRequest;
import eu.trentorise.smartcampus.mobility.controller.extensions.PlanningResultGroup;

public class VelocityCompiler {

	private VelocityEngine engine;

	public VelocityCompiler() {
		Properties props = new Properties();
		props.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		props.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		props.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE, IncludeRelativePath.class.getName());

		Velocity.init();
		engine = new VelocityEngine();
		engine.init(props);
		// engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		// engine.setProperty("classpath.resource.loader.class",
		// ClasspathResourceLoader.class.getName());

	}

	public void compile(CompilablePolicyData policy) throws Exception {
		if (!policy.isModifiedGenerate()) {
			String result = compileGenerate(policy.getCreate(), policy.getModify(), policy.getGroups());
			// System.out.println(result);
			policy.setGenerateCode(result);
		}

		if (!policy.isModifiedEvaluate()) {
			String result = compileEvaluate(policy.getEvaluate());
			// System.out.println(result);
			policy.setEvaluateCode(result);
		}

		if (!policy.isModifiedExtract()) {
			String result = compileExtract(policy.getExtract());
			// System.out.println(result);
			policy.setExtractCode(result);
		}

		if (!policy.isModifiedFilter()) {
			String result = compileFilter(policy.getFilter());
			// System.out.println(result);

			policy.setFilterCode(result);
		}

	}

	public void compile(CompilablePolicyData policy, boolean generate, boolean evaluate, boolean extract, boolean filter) throws Exception {
		if (generate) {
			String result = compileGenerate(policy.getCreate(), policy.getModify(), policy.getGroups());
			policy.setGenerateCode(result);
		}

		if (evaluate) {
			String result = compileEvaluate(policy.getEvaluate());
			policy.setEvaluateCode(result);
		}

		if (extract) {
			String result = compileExtract(policy.getExtract());
			policy.setExtractCode(result);
		}

		if (filter) {
			String result = compileFilter(policy.getFilter());
			policy.setFilterCode(result);
		}

	}

	public void check(CompilablePolicyData policy) {
		try {
			CompilerConfiguration conf = CompilerConfiguration.DEFAULT;
			Compiler compiler = new Compiler(conf);

			if (policy.getGenerateCode() != null) {
				compiler.compile("GENERATE", policy.getGenerateCode());
			}
			if (policy.getEvaluateCode() != null) {
				compiler.compile("EVALUATE", policy.getEvaluateCode());
			}
			if (policy.getExtractCode() != null) {
				compiler.compile("EXTRACT", policy.getExtractCode());
			}
			if (policy.getFilterCode() != null) {
				compiler.compile("FILTER", policy.getFilterCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String compileGenerate(List<PolicyElement> pecs, List<PolicyElement> pems, List<PlanningResultGroup> prgs) throws Exception {
		VelocityContext context = new VelocityContext();
		context.put("pecs", pecs);
		context.put("pems", pems);
		context.put("prgs", prgs);

		return velocityMerge("templates/generate.vm", context);
	}

	private String compileEvaluate(List<PolicyElement> pees) throws Exception {
		VelocityContext context = new VelocityContext();
		context.put("pees", pees);

		return velocityMerge("templates/evaluate.vm", context);
	}

	private String compileExtract(List<PolicyElement> pees) throws Exception {
		VelocityContext context = new VelocityContext();
		context.put("pees", pees); // ???

		return velocityMerge("templates/extract.vm", context);
	}

	private String compileFilter(PolicyFilter pf) throws Exception {
		VelocityContext context = new VelocityContext();
		context.put("pf", pf);

		return velocityMerge("templates/filter.vm", context);
	}

	private String velocityMerge(String templateName, VelocityContext context) throws Exception {
		context.put("helper", new VelocityHelper());

		Template template = engine.getTemplate(templateName);

		StringWriter sw = new StringWriter();

		template.merge(context, sw);

		sw.flush();
		sw.close();

		return sw.getBuffer().toString();
	}

	public static void main(String[] args) throws Exception {
		List<PolicyElement> pecs = Lists.newArrayList();
		List<PolicyElement> pems = Lists.newArrayList();
		List<PolicyElement> pefs = Lists.newArrayList();
		List<PlanningResultGroup> prgs = Lists.newArrayList();
		PlanningResultGroup prg = new PlanningResultGroup("G1", 1, RType.fastest);
		prgs.add(prg);

		{
			PolicyCondition pc = new PolicyCondition();
			pc.setFormula("ttype == TType.BUS");
			PolicyAction pa = new PolicyAction();
			pa.setNewTType(TType.CAR);
			pa.setNewRType(RType.greenest);
			pa.setPlanningResultGroupName(prg.getName());

			PlanningRequest pr = new PlanningRequest();
			pr.setType(TType.BUS);

			PolicyElement pe = new PolicyElement();
			pe.setCondition(pc);
			pe.setAction(pa);
			pecs.add(pe);
		}
		{
			PolicyCondition pc = new PolicyCondition();
			pc.setFormula("ttype == TType.BUS");
			PolicyAction pa = new PolicyAction();
			pa.setNewTType(TType.GONDOLA);
			pa.setPlanningResultGroupName(prg.getName());

			PlanningRequest pr = new PlanningRequest();
			pr.setType(TType.BUS);

			PolicyElement pe = new PolicyElement();
			pe.setCondition(pc);
			pe.setAction(pa);
			pecs.add(pe);
		}

		{
			PolicyCondition pc = new PolicyCondition();
			pc.setFormula("ttype == TType.CAR");
			PolicyAction pa = new PolicyAction();
			pa.setNewTType(TType.PARK_AND_RIDE);
			pa.setNewRType(RType.fastest);
			pa.setPlanningResultGroupName(prg.getName());

			// Map<PlanningRequest.SmartplannerParameter, Object> spp =
			// Maps.newTreeMap();
			// spp.put(SmartplannerParameter.extraTransport, TType.WALK);
			// pa.setSmartplannerParameters(spp);

			pa.setExtraTransport(TType.WALK);

			PlanningRequest pr = new PlanningRequest();
			pr.setType(TType.BUS);

			PolicyElement pe = new PolicyElement();
			pe.setCondition(pc);
			pe.setAction(pa);
			pems.add(pe);

		}

		CompilablePolicyData cp = new CompilablePolicyData();
		cp.setCreate(pecs);
		cp.setModify(pems);
		cp.setGroups(prgs);

		cp.setEvaluate(pems);

		PolicyFilter pf = new PolicyFilter();
		pf.getFormulas().add("endtime > maxTime + (1000 * 60 * 30)");
		cp.setFilter(pf);

		VelocityCompiler velo = new VelocityCompiler();
		velo.compile(cp);
		velo.check(cp);
		// String res = velo.compileGenerate(pecs, pems, prgs);
		// System.out.println(res);
	}

	// public static void main(String[] args) throws Exception {
	//
	// Velocity.init();
	// VelocityEngine ve = new VelocityEngine();
	// ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
	// ve.setProperty("classpath.resource.loader.class",
	// ClasspathResourceLoader.class.getName());
	// VelocityContext context = new VelocityContext();
	//
	// PlanningRequest pr = new PlanningRequest();
	// pr.setType(TType.BICYCLE);
	//
	// context.put("pr", pr);
	//
	//
	// Template template = ve.getTemplate("templates/generate.vm");
	//
	//
	// StringWriter sw = new StringWriter();
	//
	// template.merge(context, sw);
	//
	// sw.flush();
	// sw.close();
	//
	// System.out.println(sw.getBuffer());
	//
	// }

}
