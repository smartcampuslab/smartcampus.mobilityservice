package eu.trentorise.smartcampus.mobility.util;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;

@Component
public class ConfigUtils {

	@Autowired
	@Value("${weeklyDataDir}")
	private String weeklyDataDir;		
	
	private List<WeekConfData> weekConfData = null;
	
	private static final Logger logger = Logger.getLogger(ConfigUtils.class);
	
	// Method used to read a week conf data file and store all values in a list of WeekConfData object
	public List<WeekConfData> getWeekConfData() throws Exception {
		if (weekConfData != null) {
			return weekConfData;
		}
		
		synchronized(this) {
			if (weekConfData != null) return weekConfData;
			
			String src = weeklyDataDir + "/game_week_configuration.csv";
			String cvsSplitBy = ",";
			weekConfData = Lists.newArrayList();

//			List<String> lines = Resources.readLines(Resources.getResource(src), Charsets.UTF_8);
			List<String> lines = Resources.readLines(new File(src).toURI().toURL(), Charsets.UTF_8);

			for (int i = 1; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line.trim().isEmpty()) continue;
				
				// use comma as separator
				String[] weekConfValues = line.split(cvsSplitBy);
				int weekNum = Integer.parseInt(weekConfValues[0]);
				String weekTheme = weekConfValues[1];
				String weekThemeEng = weekConfValues[2];
				String areChallenges = weekConfValues[3];
				String arePrizes = weekConfValues[4];
				String arePrizesLast = weekConfValues[5];
				String actualWeek = weekConfValues[6];
				String actualWeekEnd = weekConfValues[7];
				logger.debug(String.format("Week conf file: week num %s, theme %s, challenges %s, prizes %s, prizes last %s, actual week %s", weekNum, weekTheme, areChallenges, arePrizes, arePrizesLast,
						actualWeek));
				// value conversion from string to boolean
				Boolean areChall = (areChallenges.compareTo("Y") == 0) ? true : false;
				Boolean arePriz = (arePrizes.compareTo("Y") == 0) ? true : false;
				Boolean arePrizLast = (arePrizesLast.compareTo("Y") == 0) ? true : false;
				WeekConfData wconf = new WeekConfData(weekNum, weekTheme, weekThemeEng, areChall, arePriz, arePrizLast, actualWeek, actualWeekEnd);
				weekConfData.add(wconf);
			}
			
			return weekConfData;
		}
	}	
	
	public WeekConfData getWeek(long timestamp) {
		try {
			for (WeekConfData week : getWeekConfData()) {
				if (week.isWeek(timestamp)) {
					return week;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;		
	}
	
	public WeekConfData getCurrentWeekConf() {
		try {
			for (WeekConfData week : getWeekConfData()) {
				if (week.currentWeek()) {
					return week;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}	
	
	public WeekConfData getPreviousWeekConf() {
		try {
			for (WeekConfData week : getWeekConfData()) {
				if (week.previousWeek()) {
					return week;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}		
	
}
