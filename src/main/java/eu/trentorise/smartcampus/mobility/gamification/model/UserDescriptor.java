package eu.trentorise.smartcampus.mobility.gamification.model;

public class UserDescriptor implements Comparable<UserDescriptor> {

	private String userId;
	private int valid;
	private int total;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public int getValid() {
		return valid;
	}
	public void setValid(int valid) {
		this.valid = valid;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	@Override
	public int compareTo(UserDescriptor o) {
		try {
			return Integer.parseInt(userId) - (Integer.parseInt(o.userId));
		} catch (Exception e) {
			return 0;
		}
	}
	
	
}
