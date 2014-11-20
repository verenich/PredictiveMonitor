package org.processmining.plugins.predictive_monitor.bpmn;

import java.util.ArrayList;
import java.util.Set;

public class Pattern {

	private ArrayList<String> items;
	private double earlinessDegree;
	private Set<Integer> sequencesID;

	public ArrayList<String> getItems() {
		return items;
	}

	public void setItems(ArrayList<String> items) {
		this.items = items;
	}

	public double getEarlinessDegree() {
		return earlinessDegree;
	}

	public void setEarlinessDegree(double earlinessDegree) {
		this.earlinessDegree = earlinessDegree;
	}

	public Set<Integer> getSequencesID() {
		return sequencesID;
	}

	public void setSequencesID(Set<Integer> sequencesID) {
		this.sequencesID = sequencesID;
	}




	
	
	
	
}
