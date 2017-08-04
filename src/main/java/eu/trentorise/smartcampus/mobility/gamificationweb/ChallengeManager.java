package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.stringtemplate.v4.ST;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamification.model.ChallengeConcept;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengeStructure;

@Component
public class ChallengeManager {

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	@Qualifier("mongoTemplate")
	MongoTemplate template;

	private Map<String, ChallengeStructure> challengeStructureMap;

	private Map<String, List> challengeDictionaryMap;
	private Map<String, String> challengeReplacements;

	// private VelocityEngine engine;

	@PostConstruct
	private void init() throws Exception {
		// template.dropCollection(ChallengeStructure.class);

		challengeStructureMap = Maps.newTreeMap();

		List list = mapper.readValue(Resources.getResource("challenges/challenges.json"), List.class);
		for (Object o : list) {
			ChallengeStructure challenge = mapper.convertValue(o, ChallengeStructure.class);

			String key = challenge.getName() + "#" + challenge.getCounterName();
			challengeStructureMap.put(key, challenge);

			template.save(challenge);
		}

		challengeDictionaryMap = mapper.readValue(Resources.getResource("challenges/challenges_dictionary.json"), Map.class);
		challengeReplacements = mapper.readValue(Resources.getResource("challenges/challenges_replacements.json"), Map.class);
	}

	// private void initVelocity() {
	// Properties props = new Properties();
	// props.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
	// props.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
	// props.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE, IncludeRelativePath.class.getName());
	//
	// Velocity.init();
	// engine = new VelocityEngine();
	// engine.init(props);
	// }

	public List<ChallengeConcept> parse(String data) throws Exception {
		List<ChallengeConcept> concepts = Lists.newArrayList();

		Map playerMap = mapper.readValue(data, Map.class);
		if (playerMap.containsKey("state")) {
			Map stateMap = mapper.convertValue(playerMap.get("state"), Map.class);
			if (stateMap.containsKey("ChallengeConcept")) {
				List conceptList = mapper.convertValue(stateMap.get("ChallengeConcept"), List.class);
				for (Object o : conceptList) {
					ChallengeConcept concept = mapper.convertValue(o, ChallengeConcept.class);
					concepts.add(concept);
				}
			}
		}
		return concepts;
	}

	public String fillDescription(ChallengeConcept challenge, String lang) throws Exception {
		String description = null;
		String name = challenge.getModelName();
		String counterName = (String) challenge.getFields().get("counterName");

		String counterNameA = null;
		String counterNameB = null;
		if (counterName != null) {
			String counterNames[] = ((String) challenge.getFields().get("counterName")).split("_");
			counterNameA = counterNames[0];
			if (counterNames.length == 2) {
				counterNameB = counterNames[1];

				if (counterNameA.startsWith("No")) {
					counterNameA = counterNameA.replace("No", "");
					counterNameB = "No" + counterNameB;
				}

			}
		}

		ChallengeStructure challengeStructure = challengeStructureMap.getOrDefault(name + "#" + counterName, null);

		if (challengeStructure == null) {
			challengeStructure = challengeStructureMap.getOrDefault(name + "#_" + counterNameB, null);
		}

		if (challengeStructure != null) {
//			System.err.println(challengeStructure);

			description = fillDescription(challengeStructure, counterNameA, counterNameB, challenge, lang);
			
			for (String key: challengeReplacements.keySet()) {
				description = description.replaceAll(key, challengeReplacements.get(key));
			}			
			
//			System.err.println("\t" + description);
//			System.err.println("________________________");
		} else {
			System.err.println(name + " / " + counterName);
			return "";
		}


		
		return description;

		// String counterNames[] = ((String)challenge.getFields().get("counterName")).split("_");
		// if (counterNames != null && counterNames.length > 2) {
		// System.err.println("!!!");
		// }
		//
		// String counterNameA = null;
		// String counterNameB = null;
		// if (counterNames != null) {
		// counterNameA = counterNames[0];
		// if (counterNames.length == 2) {
		// counterNameB = counterNames[1];
		// }
		// }

	}

	private String fillDescription(ChallengeStructure structure, String counterNameA, String counterNameB, ChallengeConcept challenge, String lang) throws Exception {
		// VelocityContext context = new VelocityContext();
		// for (String field: challenge.getFields().keySet()) {
		// context.put(field, challenge.getFields().get(field));
		// }
		//
		// context.put("counterNameA", counterNameA);
		// context.put("counterNameB", counterNameB);
		//
		// Template template = engine.getTemplate(structure.getDescription());
		//
		// StringWriter sw = new StringWriter();
		//
		// template.merge(context, sw);
		//
		// sw.flush();
		// sw.close();
		//
		// return sw.getBuffer().toString();

		ST st = new ST(structure.getDescription().get(lang));

		boolean negative = counterNameB != null && counterNameB.startsWith("No");

		for (String field : challenge.getFields().keySet()) {
			Object o = challenge.getFields().get(field);
			st.add(field, o instanceof Number ? ((Number) o).intValue() : (o instanceof String ? instantiateWord(o.toString(), negative, lang) : o));
		}

		st.add("counterNameA", instantiateWord(counterNameA, negative, lang));
		st.add("counterNameB", instantiateWord(counterNameB, negative, lang));

		return st.render();
	}

	private String instantiateWord(String word, boolean negative, String lang) {
		if (word != null) {
			List versions = challengeDictionaryMap.get(word.toLowerCase());
			if (versions != null) {
				Optional<Map> result = versions.stream().filter(x -> negative == (Boolean) ((Map) x).get("negative")).findFirst();
				if (result.isPresent()) {
					return (String)((Map)((Map) result.get()).get("word")).get(lang);
				}
			}
		}
		return word;
	}

}
