package com.hp.csbu.cc.middleware;

import java.util.List;

public class CatalogV3 {
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public List getEndPoints() {
		return endPoints;
	}
	public void setEndPoints(List endPoints) {
		this.endPoints = endPoints;
	}
	String id;
	String type;
	List endPoints;
    
}


