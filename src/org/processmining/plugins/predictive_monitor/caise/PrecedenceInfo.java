package org.processmining.plugins.predictive_monitor.caise;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.jeval.EvaluationException;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

public class PrecedenceInfo {

	public boolean checkDataConditions(boolean eventTypes, DataSnapshotListener listener, String targetName, XTrace trace, Set<Integer> fulfillments, String condition){
		boolean violated = false;
		String traceId = trace.getAttributes().get("concept:name").toString();		
		listener.openTrace(trace.getAttributes(), traceId,new HashSet<String>());

		int index = 0;
		for (XEvent event : trace) {
			listener.processEvent(event.getAttributes(), index);
			index++;
		}
		listener.closeTrace(trace.getAttributes(), traceId);

		for(Integer pos : fulfillments){
			XEvent activation = trace.get(pos);
			XAttributeMap actAttributes = activation.getAttributes();
			String activationName = actAttributes.get(XConceptExtension.KEY_NAME).toString();
			if(eventTypes){
				activationName = actAttributes.get(XConceptExtension.KEY_NAME).toString()+"-"+actAttributes.get(XLifecycleExtension.KEY_TRANSITION);
			}
			boolean found = false;
			for(int c=pos; c>=0; c--){
				XEvent event = trace.get(c);
				if(!containsEventType(targetName)){
					if(eventTypes){
						targetName = targetName+"-complete";
					}
				}
				String eventName = (XConceptExtension.instance().extractName(event));
				if(eventTypes){
					eventName = (XConceptExtension.instance().extractName(event))+"-"+event.getAttributes().get(XLifecycleExtension.KEY_TRANSITION);
				}
				
				if(eventName.equals(targetName)){
					Map<String, String> variables = new HashMap<String, String>();
					List<Pair<Integer, Map<String, Object>>> listAct = listener.getInstances().get(activationName).get(traceId);
					Map<String, Object> mapAct = null;
					for(Pair<Integer, Map<String, Object>> pair: listAct){
						if(pair.getFirst()==pos){
							mapAct = pair.getSecond();
							break;
						}
					}
					List<Pair<Integer, Map<String, Object>>> listTarg = listener.getInstances().get(targetName).get(traceId);
					Map<String, Object> mapTarg = null;
					for(Pair<Integer, Map<String, Object>> pair: listTarg){
						if(pair.getFirst()==c){
							mapTarg = pair.getSecond();
							break;
						}
					}
					for(String variable : mapAct.keySet()){
						variables.put("A."+variable, mapAct.get(variable).toString());
					}
					for(String variable : mapTarg.keySet()){
						variables.put("T."+variable, mapTarg.get(variable).toString());
					}
					try {
						if(Evaluator_predictions.evaluateExpression(variables, condition)){
							found = true;
						}
					} catch (EvaluationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if(found){
					break;
				}
			}
			if(!found){
				violated = true;
				break;
			}
		}
		return violated;
	}

	protected boolean containsEventType(String activityName){
		return (!(!activityName.contains("-assign")&&!activityName.contains("-ate_abort")&&
				!activityName.contains("-suspend")&&!activityName.contains("-complete")&&
				!activityName.contains("-autoskip")&&!activityName.contains("-manualskip")&&
				!activityName.contains("pi_abort")&&!activityName.contains("-reassign")&&!
				activityName.contains("-resume")&&!activityName.contains("-schedule")&&
				!activityName.contains("-start")&&!activityName.contains("-unknown")&&
				!activityName.contains("-withdraw")));
	}


}
