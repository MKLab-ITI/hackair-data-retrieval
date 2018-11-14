package pojo.requests;

import java.util.ArrayList;

import pojo.mongo.HackairRecordFlickr;

public class DraxisRequest {

	private String owner;
	private String action;
	private String service;
	private ArrayList<HackairRecordFlickr> msg;

	public DraxisRequest(String service, ArrayList<HackairRecordFlickr> msg) {
		this.owner = "CERTH";
		this.action = "finished";
		this.service = service;
		this.msg = msg;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public ArrayList<HackairRecordFlickr> getMsg() {
		return msg;
	}

	public void setMsg(ArrayList<HackairRecordFlickr> msg) {
		this.msg = msg;
	}
}
