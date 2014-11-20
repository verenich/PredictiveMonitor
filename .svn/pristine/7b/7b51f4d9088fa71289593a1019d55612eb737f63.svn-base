package org.processmining.plugins.predictive_monitor.bpmn;

import java.util.ArrayList;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class ClusterManager {

	public XTrace getCentroid(XLog cluster){
		//XTrace output = null;
		double minDist = Double.MAX_VALUE;
		XTrace minP = null;
		int position1 = 0;
		for(XTrace p1 : cluster){
			double totalDist = 0d;
			int position2 = 0;
			for (final XTrace p2 : cluster) {
				// sum up the distance to all points p2 | p2!=p1
				if (position1 != position2) {
					totalDist += this.getDistance(p1, p2);
				}
				position2 ++;
			}
			// if the current distance is lower that the min, take it as new min
			if (totalDist < minDist) {
				minDist = totalDist;
				minP = p1;
			}
			position1 ++;
		}
		return minP;
	}
	
	public double getDistance(XTrace t1, XTrace t2){
		double out = 0;
		ArrayList<String> encode1 = new ArrayList<String>();
		for(XEvent e1 : t1){
			encode1.add(XConceptExtension.instance().extractName(e1));
		}
		ArrayList<String> encode2 = new ArrayList<String>();
		for(XEvent e2 : t2){
			encode1.add(XConceptExtension.instance().extractName(e2));
		}
		EditDistance edit = new EditDistance(encode1, encode2);
		
		out = edit.computeNormalizedEditDistance();
		return out;
	}
}
