package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ChallengesData implements Comparable<ChallengesData> {
	
	private String challId = "";
	private String challDesc = "";
	private String challCompleteDesc = "";
	private int challTarget = 0;
	private int status = 0;
	private double row_status = 0L;
	private String type = "";
	private Boolean active = false;
	private Boolean success = false;
	private long startDate = 0L;
	private long endDate = 0L;
	private int daysToEnd = 0;
	private int bonus = 0;
	private long challCompletedDate = 0;
	
	private OtherAttendeeData otherAttendeeData;
	
	public ChallengesData(){
		super();
	}

	public ChallengesData(String challId, String challDesc, String challCompleteDesc, int challTarget, int status, double row_status,
			String type, Boolean active, Boolean success, long startDate, long endDate, int daysToEnd, int bonus, long challCompletedDate) {
		super();
		this.challId = challId;
		this.challDesc = challDesc;
		this.challCompleteDesc = challCompleteDesc;
		this.challTarget = challTarget;
		this.status = status;
		this.row_status = row_status;
		this.type = type;
		this.active = active;
		this.success = success;
		this.startDate = startDate;
		this.endDate = endDate;
		this.daysToEnd = daysToEnd;
		this.bonus = bonus;
		this.challCompletedDate = challCompletedDate;
	}

	public String getChallId() {
		return challId;
	}

	public String getChallDesc() {
		return challDesc;
	}

	public int getChallTarget() {
		return challTarget;
	}

	public void setChallId(String challId) {
		this.challId = challId;
	}

	public void setChallDesc(String challDesc) {
		this.challDesc = challDesc;
	}

	public void setChallTarget(int challTarget) {
		this.challTarget = challTarget;
	}
	
	public String getType() {
		return type;
	}

	public int getStatus() {
		return status;
	}

	public Boolean getActive() {
		return active;
	}

	public Boolean getSuccess() {
		return success;
	}

	public long getStartDate() {
		return startDate;
	}

	public long getEndDate() {
		return endDate;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

	public String getChallCompleteDesc() {
		return challCompleteDesc;
	}

	public void setChallCompleteDesc(String challCompleteDesc) {
		this.challCompleteDesc = challCompleteDesc;
	}

	public int getDaysToEnd() {
		return daysToEnd;
	}

	public void setDaysToEnd(int daysToEnd) {
		this.daysToEnd = daysToEnd;
	}

	public double getRow_status() {
		return row_status;
	}

	public void setRow_status(double row_status) {
		this.row_status = row_status;
	}
	
	public int getBonus() {
		return bonus;
	}

	public void setBonus(int bonus) {
		this.bonus = bonus;
	}

	public long getChallCompletedDate() {
		return challCompletedDate;
	}

	public void setChallCompletedDate(long challCompletedDate) {
		this.challCompletedDate = challCompletedDate;
	}

	public OtherAttendeeData getOtherAttendeeData() {
		return otherAttendeeData;
	}

	public void setOtherAttendeeData(OtherAttendeeData competitionData) {
		this.otherAttendeeData = competitionData;
	}

	@Override
	public String toString() {
		ToStringBuilder tsb = new ToStringBuilder(this, ToStringStyle.JSON_STYLE);
		return tsb.build();
	}

	@Override
	public int compareTo(ChallengesData o) {
		return new Long(startDate).compareTo(new Long(o.startDate));
	}

	
	
}
