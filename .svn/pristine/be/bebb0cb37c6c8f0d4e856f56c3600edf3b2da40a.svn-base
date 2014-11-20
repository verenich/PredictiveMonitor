package org.processmining.plugins.predictive_monitor.bpmn;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.DatatypeConverter;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.predictive_monitor.bpmn.sequencemining.SequenceMiner;

import weka_predictions.core.Attribute;
import weka_predictions.core.DenseInstance;
import weka_predictions.core.Instance;

public class ArffBuilder {
	private static HashMap<String, ArrayList<String>> attributeTypes = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String inputLogFilePath = "./input/originalWithoutGroupString.xes";
		String inputLogFilePath = "./input/loan_test_wo_age.mxml";
		double minSup = 0.5;
		int minLength = 2;
		

		try {
			XLog log = XLogReader.openLog(inputLogFilePath);
			Formula formula = new SimpleFormula("<>(\"request_complex_accepted\")");
			Vector<Formula> formulas = new Vector<Formula>();
			formulas.add(formula);

			XTrace trace = log.get(2);
			XTrace currentTrace = (XTrace) trace.clone();
			for (int i = 2; i > 0; i--) {
				currentTrace.remove(currentTrace.size()-i);
			}

			ArrayList patterns = (ArrayList<Pattern>) SequenceMiner.mineFrequentPatternsMaxSPWithHoles(log, minSup, minLength);

			HashMap<String, Boolean> histTraceSatisfaction = new HashMap<String, Boolean>();
			LogReaderAndReplayer replayer = new LogReaderAndReplayer(log);
			DataSnapshotListener listener = new DataSnapshotListener(replayer.getDataTypes(), replayer.getActivityLabels());	
			for (XTrace hTrace : log) {
				histTraceSatisfaction.put(XConceptExtension.instance().extractName(hTrace), FormulaVerificator.isFormulaVerified(log, listener, hTrace, formulas));
			}
			writeArffFile(log, patterns, histTraceSatisfaction);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public static HashMap<String, String> generateDataValues(XTrace trace, HashMap<String, ArrayList<String>> attributeTypes){
		HashMap<String, String>dataValues = new HashMap<String, String>();
		XAttributeMap traceAttr = trace.getAttributes();
		for (String attribute : attributeTypes.keySet()) {
			dataValues.put(attribute,null);
			if (traceAttr.containsKey(attribute)){
				//it's a trace attribute
				if(!attribute.equals("Activity code") &&!attribute.equals("creator")&&!attribute.contains(":")&& !attribute.equals("description")){
					dataValues.put(attribute, traceAttr.get(attribute).toString());
				}
			} else {
				for (XEvent xEvent : trace) {
					XAttributeMap eventAttr = xEvent.getAttributes();
					if (eventAttr.containsKey(attribute))
						if(!attribute.equals("Activity code")&&!attribute.contains(":")){
							dataValues.put(attribute, eventAttr.get(attribute).toString());
					}
				}
			}	
		}
/*		for(String attribute : traceAttr.keySet()){
			if(!attribute.equals("Activity code") &&!attribute.equals("creator")&&!attribute.contains(":")&& !attribute.equals("description")){
				dataValues.put(attribute, traceAttr.get(attribute).toString());
			}
		}
		for (XEvent xEvent : trace) {
			XAttributeMap eventAttr = xEvent.getAttributes();
			for(String attribute : eventAttr.keySet()){
				if(!attribute.equals("Activity code")&&!attribute.contains(":")){
					dataValues.put(attribute, eventAttr.get(attribute).toString());
				}
			}
		}
*/
		return dataValues;
	}

	public static ArrayList<Integer> generatePatternVector(XTrace trace, ArrayList<Pattern> patterns){
		ArrayList<Integer> patternVector = PatternManager.getPatternsContainedInTrace(trace, patterns);
		return patternVector;
	}

	public static void writeAttributeNames(PrintWriter pw, HashMap<String, ArrayList<String>> attributeTypes, ArrayList<Pattern> patterns){
		String relationName = "@relation data";

		pw.println(relationName);
		pw.println("");
		for(String attribute : attributeTypes.keySet()){
			if(!attribute.equals("Activity code")){
				if(attributeTypes.get(attribute).iterator().next().equals("numeric")){
					pw.println("@attribute "+ "\""+ attribute+"\"" + " numeric");
				}else if(attributeTypes.get(attribute).iterator().next().equals("date")){
					pw.println("@attribute "+ "\""+ attribute+"\"" + " date");
				}else{
					pw.print("@attribute "+ "\""+attribute+"\"" + " {");
					Iterator<String> it = attributeTypes.get(attribute).iterator();
					while(it.hasNext()){
						String enumValue = it.next();
						if(it.hasNext()){
							pw.print("\""+enumValue+"\"" + ",");
						}else{
							pw.println("\""+enumValue+"\""+ "}");	
						}
					}
				}
			}
		}

		/*			for (Pattern pattern : patterns) {
				pw.println("@attribute \\"+pattern.getItems()+"\\ numeric");
			}*/
		for (int i = 0; i < patterns.size(); i++) {
			Pattern pattern = patterns.get(i);
			String patternId = "P"+i;
			pw.println("@attribute "+patternId+" {true, false}");
		}

		pw.println("@attribute conformant {yes,no}");	
		pw.println("");
		pw.println("@data");

	}


	public static String getDataValuesString (HashMap<String, ArrayList<String>> attributeTypes, HashMap<String, String> dataValues){
		String line = "";
		Iterator<String> it = attributeTypes.keySet().iterator();

		while(it.hasNext()){
			String attribute = it.next();
			if(dataValues.get(attribute)!=null){
				if(it.hasNext()){
					if(attributeTypes.get(attribute).iterator().next().equals("numeric")||attributeTypes.get(attribute).iterator().next().equals("date")){ 
						line = line +dataValues.get(attribute)+",";
					}else{
						line = line + "\""+dataValues.get(attribute)+"\""+",";
					}
				}else{
					if(attributeTypes.get(attribute).iterator().next().equals("numeric")||attributeTypes.get(attribute).iterator().next().equals("date")){ 
						line = line + dataValues.get(attribute);
					}else{
						line = line + "\""+dataValues.get(attribute)+"\"";
					}

				}
			}else{
				if(it.hasNext()){
					line = line + "?,";
				}else{
					line = line + "?";
				}
			}
		}
		return line;
	}

	public static String getCFPatternValuesString(ArrayList<Integer> patterns){
		String line = "";
		for (Integer integer : patterns) {
			if (integer>0)
				line+="true,";
			else
				line+="false,";
		}
		return line;
	}

	public static void writeDataValues(PrintWriter pw, HashMap<String, ArrayList<String>> attributeTypes, HashMap<String, String> dataValues, ArrayList<Integer> patterns, String satisfied){
		String line = getDataValuesString(attributeTypes, dataValues)+",";
		line =  line+getCFPatternValuesString(patterns);
		line  = line+ satisfied;
		System.out.println(line);
		pw.println(line);

	}

	public static String getPrefixString( XTrace trace){
		String tracePrefix = "";
		for(XEvent e : trace){

			String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();
			tracePrefix = tracePrefix+label+";";
		}
		return tracePrefix;
	}



	public static HashMap<String, ArrayList<String>> generateAttributeTypes(XTrace t, HashMap<String, ArrayList<String>> attributeTypes){
		HashMap<String, ArrayList<String>> tempAttributeTypes = new HashMap<String, ArrayList<String>>();

		XAttributeMap traceAttr = t.getAttributes();
		for(String attribute : traceAttr.keySet()){
			if(!attribute.equals("Activity code") &&!attribute.equals("creator")&&!attribute.contains(":")&& !attribute.equals("description")){
				ArrayList<String> value = null;
				if(attributeTypes.get(attribute)==null){
					try{
						if(!attribute.equals("Activity code")){
							new Integer(traceAttr.get(attribute).toString());
							value = new ArrayList<String>();
							value.add("numeric");
						}
					}catch(NumberFormatException ex){
						try{
							if(!attribute.equals("Activity code")){
								new Double(traceAttr.get(attribute).toString());
								value = new ArrayList<String>();
								value.add("numeric");
							}
						}catch(NumberFormatException exc){
							try {
								DatatypeConverter.parseDateTime(traceAttr.get(attribute).toString());
								value = new ArrayList<String>();
								value.add("date");
							} catch (IllegalArgumentException e) {
								value = new ArrayList<String>();
								value.add(traceAttr.get(attribute).toString());
							}

						}
					}
				}else{
					value = attributeTypes.get(attribute);
					if(!value.contains(traceAttr.get(attribute).toString())){
						value.add(traceAttr.get(attribute).toString());
					}
				}
				attributeTypes.put(attribute, value);

			}
		}
		for(XEvent e : t){

			String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();
			XAttributeMap eventAttr = e.getAttributes();


			for(String attribute : eventAttr.keySet()){
				if(!attribute.equals("Activity code")&&!attribute.contains(":")){
					ArrayList<String> value = null;
					if(attributeTypes.get(attribute)==null){
						try{
							if(!attribute.equals("Activity code")){
								new Integer(eventAttr.get(attribute).toString());
								value = new ArrayList<String>();
								value.add("numeric");
							}
						}catch(NumberFormatException ex){
							try{
								if(!attribute.equals("Activity code")){
									new Double(eventAttr.get(attribute).toString());
									value = new ArrayList<String>();
									value.add("numeric");
								}
							}catch(NumberFormatException exc){
								try {
									DatatypeConverter.parseDateTime(eventAttr.get(attribute).toString());
									value = new ArrayList<String>();
									value.add("date");
								} catch (IllegalArgumentException exce) {
									value = new ArrayList<String>();
									value.add(eventAttr.get(attribute).toString());
								}
							}
						}
					}else{
						value = attributeTypes.get(attribute);
						if(!value.contains(eventAttr.get(attribute).toString())){
							value.add(eventAttr.get(attribute).toString());
						}
					}
					tempAttributeTypes.put(attribute, value);
				}
				for(String attrib : tempAttributeTypes.keySet()){
					attributeTypes.put(attrib, tempAttributeTypes.get(attrib));
				}
			}
		}
		return attributeTypes;

	}
	
	



	public static HashMap<String, ArrayList<String>> getAttributeTypes() {
		return attributeTypes;
	}


	public static File writeArffFile(XLog prefixTraceLog, ArrayList<Pattern> patterns, HashMap<String, Boolean> histTraceSatisfaction){
		File arff = null;

		try {

			arff = File.createTempFile("classification", ".arff");
			PrintWriter pw = new PrintWriter(new FileWriter(arff));		

			attributeTypes = new HashMap<String, ArrayList<String>>();
			for (XTrace prefixTrace : prefixTraceLog) {
				attributeTypes = generateAttributeTypes(prefixTrace, attributeTypes);
			}

			writeAttributeNames(pw, attributeTypes, patterns);		

			for (XTrace histPrefixTrace : prefixTraceLog) {

				HashMap<String, String> dataValues = generateDataValues(histPrefixTrace, attributeTypes);
				ArrayList<Integer> patternVector = generatePatternVector(histPrefixTrace, patterns);
				
				boolean violated = histTraceSatisfaction.get(XConceptExtension.instance().extractName(histPrefixTrace));
				String satisfied = null;
				if (violated)
					satisfied = "no";
				else
					satisfied = "yes";
				writeDataValues(pw, attributeTypes, dataValues, patternVector, satisfied);
				//}
			}
			pw.flush();
			pw.close();			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arff;

	}

/*	public static File writeArffFile(DataSnapshotListener listener,  XLog prefixTraceLog, XTrace currentTrace, ArrayList<Pattern> patterns,  HashMap<String, Boolean> histTraceSatisfaction){
		File arffFile = null;
		Vector<TracePrefix> histTracePrefixes = new Vector<TracePrefix>();
		int traceIndex = 0;
		for (Iterator iterator = log.iterator(); iterator.hasNext();) {
			XTrace histTrace = (XTrace) iterator.next();
			System.out.print(traceIndex+" ");
						for (XEvent xEvent : histTrace) {
				System.out.print(XConceptExtension.instance().extractName(xEvent)+" ");
			}
			System.out.println();

			TracePrefix histPrefix = PatternManager.getSimilarPrefix(currentTrace, histTrace, traceIndex);
			histTracePrefixes.add(histPrefix);
			traceIndex++;
		}

		arffFile = writeArffFile(listener, formulas, histTracePrefixes, log, minSup, minLength, patterns);
		return arffFile;
	}*/



/*	public static Instance getTraceInstance(XTrace currentTrace, ArrayList<Pattern> patterns, HashMap<String, ArrayList<String>> attributeTypes, HashMap<String, String> variables){

		//HashMap<String, ArrayList<String>> attributeTypes = generateAttributeTypes(currentTrace);
		//HashMap<String, String> dataValues = generateDataValues(currentTrace, attributeTypes);
		Instance instance = new DenseInstance(attributeTypes.size()+patterns.size()+1);
		
		HashMap<String, String> dataValues = new HashMap<String, String>();
		for (String attribute : attributeTypes.keySet()) {
			if (variables.get(attribute)!=null)
				dataValues.put(attribute, variables.get(attribute));
			else
				dataValues.put(attribute, null);
		}

		
		
		int index = 0;
		for (String attribute : attributeTypes.keySet()) {
			System.out.println(instance.numAttributes()+" "+instance);
			//instance.setValue(new Attribute(attribute), Double.parseDouble(dataValues.get(attribute)));
			
			String attrValue = dataValues.get(attribute);

			if (attributeTypes.get(attribute).get(0).equalsIgnoreCase("numeric")){
				Attribute attr = new Attribute(attribute,index);
				if (attrValue!=null)
					instance.setValue(attr, Double.parseDouble(attrValue));
				else 
					instance.setMissing(attr);

			}
			else {
				if (attributeTypes.get(attribute).get(0).equalsIgnoreCase("date")){
					Attribute attr = new Attribute(attribute,"yyyy-MM-dd'T'HH:mm:ssXXX",index);	
					
					if (attrValue!=null)
						try {
							instance.setValue(attr, attr.parseDate(attrValue));
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
				else {
					List<String> values = new ArrayList();
					for (String enumType : attributeTypes.get(attribute)) {
						values.add(enumType);
					}
					System.out.println(values.size()+" "+values);
					System.out.println("ATTR_VALUE"+attrValue);
					if (attrValue!=null && !values.contains(attrValue)) {
						//values.add(attrValue);
						System.out.println("MISSING ENUM "+attrValue);
						attrValue = null;
					}
					Attribute attr = new Attribute(attribute,values,index);			
					if (attrValue!=null)
						instance.setValue(attr, attrValue);
					else 
						instance.setMissing(attr);
				}
				List<String> values = null;
				Attribute attr = new Attribute(attribute, values, index);
				if (attrValue!=null)
					instance.setValue(attr, attrValue);
				}
			}
			index++;
		}
		ArrayList<Integer> patternVector = generatePatternVector(currentTrace, patterns);
		for (int i = 0; i < patternVector.size(); i++) {
			

			Integer pattern = patternVector.get(i);
			String patternId = "P"+i;
			String attrValue;
			if (pattern>0)
				attrValue="true";
			else
				attrValue="false";
			List<String> values = new ArrayList();
			values.add("true");
			values.add("false");
			for (String enumType : attributeTypes.get(patternId)) {
				values.add(enumType);
			}
			if (!values.contains(attrValue))
				values.add(attrValue);
			//List<String> values = null;
			Attribute attr = new Attribute(patternId,values,index);			
			if (attrValue!=null)
				instance.setValue(attr, attrValue);
			//Attribute attr = new Attribute(patternId,index);
			//instance.setValue(attr, value);
			index++;
		}
		
		List<String> values = new ArrayList();
		values.add("yes");
		values.add("no");
		Attribute attr = new Attribute("conformant",values,index);			
		
		System.out.println(instance.numAttributes()+" "+instance);
		return instance;

	}*/
	
	public static Instance getTraceInstance(XTrace currentTrace, ArrayList<Pattern> patterns, HashMap<String, ArrayList<String>> attributeTypes, HashMap<String, String> variables){
		Instance instance = new DenseInstance(attributeTypes.size()+patterns.size()+1);
		
		HashMap<String, String> dataValues = new HashMap<String, String>();
		for (String attribute : attributeTypes.keySet()) {
			if (variables.get(attribute)!=null)
				dataValues.put(attribute, variables.get(attribute));
			else
				dataValues.put(attribute, null);
		}

		
		
		int index = 0;
		for (String attribute : attributeTypes.keySet()) {
			System.out.println(instance.numAttributes()+" "+instance);
			
			String attrValue = dataValues.get(attribute);

			if (attributeTypes.get(attribute).get(0).equalsIgnoreCase("numeric")){
				Attribute attr = new Attribute(attribute,index);
				if (attrValue!=null)
					instance.setValue(attr, Double.parseDouble(attrValue));
				else 
					instance.setMissing(attr);

			}
			else {
				if (attributeTypes.get(attribute).get(0).equalsIgnoreCase("date")){
					Attribute attr = new Attribute(attribute,"yyyy-MM-dd'T'HH:mm:ssXXX",index);						
					if (attrValue!=null){
						try {
							instance.setValue(attr, attr.parseDate(attrValue));
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				else {
					List<String> values = new ArrayList();
					for (String enumType : attributeTypes.get(attribute)) {
						values.add(enumType);
					}
					System.out.println(values.size()+" "+values);
					System.out.println("ATTR_VALUE "+attrValue);
					if (attrValue!=null && !values.contains(attrValue)) {
						//values.add(attrValue);
						System.out.println("MISSING ENUM "+attrValue);
						attrValue = null;
					}
					Attribute attr = new Attribute(attribute,values,index);			
					if (attrValue!=null)
						instance.setValue(attr, attrValue);
					else 
						instance.setMissing(attr);
				}
			}
			index++;
		}
		ArrayList<Integer> patternVector = generatePatternVector(currentTrace, patterns);
		for (int i = 0; i < patternVector.size(); i++) {
			

			Integer pattern = patternVector.get(i);
			String patternId = "P"+i;
			String attrValue;
			if (pattern>0)
				attrValue="true";
			else
				attrValue="false";
			List<String> values = new ArrayList();
			values.add("true");
			values.add("false");

			Attribute attr = new Attribute(patternId,values,index);			
			if (attrValue!=null)
				instance.setValue(attr, attrValue);

			index++;
		}
		
		
		System.out.println(instance.numAttributes()+" "+instance);
		return instance;

	}
	
	private static XTrace getPrefixedTrace(XTrace completeTrace, String prefixString){
		String[] prefixes = prefixString.split(";");
		Vector<String> prefixVector = new Vector<String>();
		for (int i = 0; i < prefixes.length; i++) {
			prefixVector.add(prefixes[i]);
		}
		 XFactory fR = XFactoryRegistry.instance().currentDefault();
		XTrace prefixTrace = fR.createTrace();
		prefixTrace.setAttributes(completeTrace.getAttributes());
		Iterator<XEvent> it = completeTrace.iterator();
		boolean end = false;
		while (it.hasNext() && !end){
			XEvent event = it.next();
			if (prefixVector.contains(XConceptExtension.instance().extractName(event))){
				XEvent pEvent = (XEvent) event.clone();
				prefixTrace.add(pEvent);
			} else 
				end = true;
		}
		return prefixTrace;

	}
	
	public static boolean isUmbalanced(XLog log, HashMap<String, Boolean> histTraceSatisfaction, double threshold){
		boolean umbalanced = false;
		int positive = 0;
		int negative = 0;
		for (XTrace trace : log) {
			String key = XConceptExtension.instance().extractName(trace);
			if (histTraceSatisfaction.get(key))
				positive++;
			else 
				negative++;
		}
		int tot = positive + negative;
		double max = (double)Math.max(positive, negative);

		if (max/tot>=threshold) 
			umbalanced = true;
		return umbalanced;
	}
	
	public static List<XLog> filterClusterLog(List<XLog> logs, HashMap<String, Boolean> histTraceSatisfaction, double threshold){
		ArrayList<XLog> filteredClusterLogs = new ArrayList<XLog>();
		for (XLog log : logs) {
			if (!isUmbalanced(log, histTraceSatisfaction, threshold)){
				filteredClusterLogs.add(log);
			}
			
		}
		return filteredClusterLogs;
	}


	


}
