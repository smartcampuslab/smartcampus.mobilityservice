package eu.trentorise.smartcampus.mobility.processor;

import java.util.Arrays;

public class Station
{
	private String name;
	private String address;
	private String id;
	private int bikes;
	private int slots;
	private int totalSlots;
	private double[] position;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getBikes() {
		return bikes;
	}
	public void setBikes(int bikes) {
		this.bikes = bikes;
	}
	public int getSlots() {
		return slots;
	}
	public void setSlots(int slots) {
		this.slots = slots;
	}
	public int getTotalSlots() {
		return totalSlots;
	}
	public void setTotalSlots(int totalSlots) {
		this.totalSlots = totalSlots;
	}
	public double[] getPosition() {
		return position;
	}
	public void setPosition(double[] position) {
		this.position = position;
	}
	@Override
	public String toString() {
		return "Station [id=" + id + ", name=" + name + ", address=" + address
				+ ", bikes=" + bikes + ", slots=" + slots + ", totalSlots="
				+ totalSlots + ", position=" + Arrays.toString(position) + "]";
	}
}
