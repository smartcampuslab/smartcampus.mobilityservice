package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WeekConfData {

	private int weekNum = 0;
	private String weekTheme = "";
	private String weekThemeEng = "";
	private boolean challenges = false;
	private boolean prizes = false;
	private boolean prizesLast = false;
	private String weekStart, weekEnd;
	
	private static final SimpleDateFormat SDF_WEEK_DATE = new SimpleDateFormat("yyyy-MM-dd");
	
	public int getWeekNum() {
		return weekNum;
	}

	public String getWeekTheme() {
		return weekTheme;
	}

	public boolean isChallenges() {
		return challenges;
	}

	public boolean isPrizes() {
		return prizes;
	}

	public boolean isPrizesLast() {
		return prizesLast;
	}

	public void setWeekNum(int weekNum) {
		this.weekNum = weekNum;
	}

	public void setWeekTheme(String weekTheme) {
		this.weekTheme = weekTheme;
	}

	public void setChallenges(boolean challenges) {
		this.challenges = challenges;
	}

	public void setPrizes(boolean prizes) {
		this.prizes = prizes;
	}

	public void setPrizesLast(boolean prizesLast) {
		this.prizesLast = prizesLast;
	}

	public String getWeekThemeEng() {
		return weekThemeEng;
	}

	public void setWeekThemeEng(String weekThemeEng) {
		this.weekThemeEng = weekThemeEng;
	}

	public WeekConfData() {
		// TODO Auto-generated constructor stub
	}

	public WeekConfData(int weekNum, String weekTheme, String weekThemeEng, boolean challenges, boolean prizes, boolean prizesLast,
			String weekStart, String weekEnd) {
		super();
		this.weekNum = weekNum;
		this.weekTheme = weekTheme;
		this.weekThemeEng = weekThemeEng;
		this.challenges = challenges;
		this.prizes = prizes;
		this.prizesLast = prizesLast;
		this.weekStart = weekStart;
		this.weekEnd = weekEnd;
	}

	public String getWeekStart() {
		return weekStart;
	}

	public void setWeekStart(String weekStart) {
		this.weekStart = weekStart;
	}

	public String getWeekEnd() {
		return weekEnd;
	}

	public void setWeekEnd(String weekEnd) {
		this.weekEnd = weekEnd;
	}

	@Override
	public String toString() {
		return "WeekConfData [weekNum=" + weekNum + ", weekTheme=" + weekTheme + ", challenges=" + challenges
				+ ", prizes=" + prizes + ", prizesLast=" + prizesLast + ", weekStart=" + weekStart + "]";
	}

	public boolean currentWeek() {
		String currDate = SDF_WEEK_DATE.format(new Date());
		return currDate.compareTo(weekEnd) <= 0 && currDate.compareTo(weekStart) >= 0;
	}

}
