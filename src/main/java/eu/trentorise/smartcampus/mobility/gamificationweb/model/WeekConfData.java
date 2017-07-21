package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class WeekConfData {

	private String weekNum = "";
	private String weekTheme = "";
	private String weekThemeEng = "";
	private boolean challenges = false;
	private boolean prizes = false;
	private boolean prizesLast = false;
	private boolean isActual = false;
	
	public String getWeekNum() {
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

	public boolean isActual() {
		return isActual;
	}

	public void setWeekNum(String weekNum) {
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

	public void setActual(boolean isActual) {
		this.isActual = isActual;
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

	public WeekConfData(String weekNum, String weekTheme, String weekThemeEng, boolean challenges, boolean prizes, boolean prizesLast,
			boolean isAcutal) {
		super();
		this.weekNum = weekNum;
		this.weekTheme = weekTheme;
		this.weekThemeEng = weekThemeEng;
		this.challenges = challenges;
		this.prizes = prizes;
		this.prizesLast = prizesLast;
		this.isActual = isAcutal;
	}

	@Override
	public String toString() {
		return "WeekConfData [weekNum=" + weekNum + ", weekTheme=" + weekTheme + ", challenges=" + challenges
				+ ", prizes=" + prizes + ", prizesLast=" + prizesLast + ", isAcutal=" + isActual + "]";
	}


}
