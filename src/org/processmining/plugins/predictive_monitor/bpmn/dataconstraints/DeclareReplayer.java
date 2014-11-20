package org.processmining.plugins.predictive_monitor.bpmn.dataconstraints;

import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.predictive_monitor.bpmn.DataSnapshotListener;

public interface DeclareReplayer {
	
	public void process(int eventIndex, String event, XTrace trace, String traceId,DataSnapshotListener listener);

}
