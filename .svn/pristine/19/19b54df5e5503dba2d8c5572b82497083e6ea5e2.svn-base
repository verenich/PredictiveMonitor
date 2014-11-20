package org.processmining.plugins.predictive_monitor.bpmn;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.operationalsupport.client.SessionHandle;
import org.processmining.operationalsupport.messages.reply.ResponseSet;

import weka_predictions.data_predictions.Result;

public class OperationalSupportRandomForest {
	
	public static SessionHandle handle=null;
	
	public static void createHandle(){

		//	handle.setModel(DeclareLanguage.INSTANCE, referenceXML);
		}
	
	public static Map<String, Result> provideOperationalSupport(XTrace currentTrace, int eventIndex, String traceID, Vector<Formula> formulas, Vector<String> currentVariables, Map<String, String> variables, Map<String,Integer>configuration){
//		Classifier classifier = new Classifier();
//		String filePath = classifier.classifyTraces(formula, currentEvents);
//		Predictor predictor = new Predictor();
//		Map<String, Result> suggestions = predictor.suggestInput(filePath, currentVariables, variables);
		
		
		Map<String, Result> analysis = new HashMap<String, Result>();

		Hashtable<String,XTrace> partialTraces = new Hashtable<String,XTrace>();

		try {
			String piID = traceID;	
			
			Hashtable handles = new Hashtable();
			HashMap<String, Object> properties = new HashMap<String,Object>();
			properties.put("formulas", formulas);
			properties.put("currentVariables", currentVariables);
			properties.put("variables", variables);
			properties.put("index", new Integer(eventIndex));
			properties.put("configuration",configuration);
			//ResponseSet result = null;
			try {
				handle = SessionHandle.create("localhost",1202, DeclareMonitorQuery.INSTANCE,properties);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			XTrace partialTrace;
		//	String eventName = "nome attivita";
			if (partialTraces.containsKey(piID)) {
				partialTrace = (XTrace) partialTraces.get(piID);
			} else {
				//partialTrace = new XTraceImpl(new XAttributeMapImpl());
				partialTrace = currentTrace;
			}
			boolean done = false;
			if(partialTrace.get(partialTrace.size()-1).equals("complete")){
				done = true;
			}

			/*	if(currentEvents.get(currentEvents.size()-1).equals("complete")){
				done = true;
			}*/
			int i = 0;
			for(XEvent event : partialTrace){
				//XEvent completeEvent = new XEventImpl();
				//XConceptExtension.instance().assignName(completeEvent, eventName.toLowerCase());
				handle.addEvent(event);
				i++;
				//partialTrace.add(completeEvent);
				if(i>eventIndex){
					break;
				}
			}
			
			XConceptExtension.instance().assignName(partialTrace,piID);
			partialTraces.put(piID, partialTrace);
			XLog emptyLog = XFactoryRegistry.instance().currentDefault().createLog();
			ResponseSet<Map<String, Object>> result = handle.simple(piID, emptyLog ,done);
			
			for (String provider : result) {
				for (Map<String, Object> r : result.getResponses(provider)) {
					for(String key : r.keySet()){
						analysis.put(key, (Result)r.get(key));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
		return analysis;
	}

}
