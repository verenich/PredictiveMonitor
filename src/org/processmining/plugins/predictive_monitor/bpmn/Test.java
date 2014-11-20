package org.processmining.plugins.predictive_monitor.bpmn;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.predictive_monitor.bpmn.sequencemining.SequenceMiner;

import weka_predictions.core.Instance;
import weka_predictions.data_predictions.Result;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputLogFilePath = "./input/loan_test_wo_age.mxml";

		double minSup = 0.5;
		int minLength = 2;		
		try {
			XLog logOriginal = XLogReader.openLog(inputLogFilePath);
			XLog log = (XLog) logOriginal.clone();
			log.remove(0);
			
			XLog traceLog = (XLog) logOriginal.clone();
			for (int i = 1; i < traceLog.size(); i++) {
				traceLog.remove(i);
			}
			
			XTrace currentTrace = traceLog.get(0);
			for (int i = 4; i > 0; i--) {
				currentTrace.remove(currentTrace.size()-i);
			}
			
			//Formula formula = new SimpleFormula("<>(\"request_complex_accepted\")");
			//Formula formula = new DataCondFormula("[salary>1000][][]","retrieve_u_data","request_denied",DeclareTemplate.Response);
			Formula formula = new DataCondFormula("[salary>1000][T.length>25][0,40,d]","retrieve_u_data","request_rejected",DeclareTemplate.Response);
			
			Vector<Formula> formulas = new Vector<Formula>();
			formulas.add(formula);
				
			
			//XTrace currentTrace = (XTrace) trace.clone();

			ArrayList patterns = (ArrayList<Pattern>) SequenceMiner.mineFrequentPatternsMaxSPWithHoles(log, minSup, minLength);

			HashMap<String, Boolean> histTraceSatisfaction = new HashMap<String, Boolean>();
			LogReaderAndReplayer replayer = new LogReaderAndReplayer(log);
			DataSnapshotListener listener = new DataSnapshotListener(replayer.getDataTypes(), replayer.getActivityLabels());	
			for (XTrace hTrace : log) {
				histTraceSatisfaction.put(XConceptExtension.instance().extractName(hTrace), FormulaVerificator.isFormulaVerified(log, listener, hTrace, formulas));
			}
			
			//File arff = ArffBuilder.writeArffFile(listener, formulas, currentTrace, log, minSup, minLength, patterns);
			File arff = ArffBuilder.writeArffFile(log, patterns, histTraceSatisfaction);
			HashMap<String, ArrayList<String>> attributeTypes = ArffBuilder.getAttributeTypes();
			
			
			HashMap<String, String> variables = new HashMap<String, String>();
			XAttributeMap traceAttr = currentTrace.getAttributes();
			for (String attr : traceAttr.keySet()) {
				if(!attr.equals("Activity code") &&!attr.equals("creator")&&!attr.contains(":")&& !attr.equals("description")){
					variables.put(attr, traceAttr.get(attr).toString());
				}
			}
			//ArrayList<Pattern> patterns = (ArrayList<Pattern>) SequenceMiner.mineFrequentPatternsMaxSPWithHoles(log, minSup, minLength);
			Instance instance = ArffBuilder.getTraceInstance(currentTrace, patterns, attributeTypes, variables);

			
			Map<String, Result> suggestions = Predictor.makePredictionRandomForest(arff.getAbsolutePath(), instance);
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}


}
