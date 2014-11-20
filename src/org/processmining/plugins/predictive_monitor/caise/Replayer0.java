package org.processmining.plugins.predictive_monitor.caise;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ltl2aut.automaton.Automaton;
import ltl2aut.automaton.Transition;
import ltl2aut.formula.DefaultParser;
import ltl2aut.formula.conjunction.ConjunctionFactory;
import ltl2aut.formula.conjunction.ConjunctionTreeLeaf;
import ltl2aut.formula.conjunction.ConjunctionTreeNode;
import ltl2aut.formula.conjunction.DefaultTreeFactory;
import ltl2aut.formula.conjunction.GroupedTreeConjunction;
import ltl2aut.formula.conjunction.TreeFactory;
import ltl2aut.ltl.SyntaxParserException;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.in.XMxmlGZIPParser;
import org.deckfour.xes.in.XMxmlParser;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;
import org.processmining.plugins.declareminer.Watch;

import weka_predictions.data_predictions.Result;

public class Replayer0 {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String inputLogFileName = "/home/coderus/TKDE/case_study/BPI2011_20.xes";
		String outputFileName = "/home/coderus/TKDE/case_study/outputBPI2012_caise_gap1.txt";
		double minConfidenceThreshold = 0.5;
		double minSupportThreshold = 2;
		
		File output = new File(outputFileName);
		PrintWriter pw = new PrintWriter(output);
		
		XLog log = null;

		if(inputLogFileName.toLowerCase().contains("mxml.gz")){
			XMxmlGZIPParser parser = new XMxmlGZIPParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}else if(inputLogFileName.toLowerCase().contains("mxml")){
			XMxmlParser parser = new XMxmlParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if(inputLogFileName.toLowerCase().contains("xes.gz")){
			XesXmlGZIPParser parser = new XesXmlGZIPParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}else if(inputLogFileName.toLowerCase().contains("xes")){
			XesXmlParser parser = new XesXmlParser();
			if(parser.canParse(new File(inputLogFileName))){
				try {
					log = parser.parse(new File(inputLogFileName)).get(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		//CONFIGURAZIONE
		//String formulaLTL = "<>(\"ca-125 using meia\")";
		//String formulaLTL = "(<>(\"order rate\"))/\\(<>(\"ca-125 using meia\"))";
		//String formulaLTL = "(<>(\"ca-125 using meia\"))";
		//String formulaLTL = "(<>(\"O2 saturation\"))";
		//String formulaLTL = "[]((\"calcium\")->(<>(\"Calcium - urgent\")))";
		//int traceIndex = 44;
		//int traceIndex = 11;

		String[] confFormulas = new String[1];
		confFormulas[0]="(  <>(\"tumor marker CA-19.9\") ) \\/ ( <> (\"ca-125 using meia\") )  ";
//		confFormulas[1]="!(  <>(\"First outpatient consultation\"))";
//		confFormulas[2]="(  (! (\"Calcium - urgent\")) U (\"calcium\")) ";
//		//confFormulas[3]="( [](    ((\"calcium\") -> (\"Calcium - urgent\")) /\\  ((\"glucose\") -> (\"Glucose - urgent\"))   ) )";
//		Formula dFormula1 = new DataCondFormula("[A.Age>35][][]","histological examination - biopsies nno","squamous cell carcinoma using eia",DeclareTemplate.Response);
//		Formula dFormula2 = new DataCondFormula("['A.Producer code'=='CLRE'][][]","assumption laboratory","MRI abdomen",DeclareTemplate.Precedence);
//		confFormulas[3]="(  <>(\"order rate\"))";
//		confFormulas[4]="(  (! (\"histological examination - biopsies nno\")) U (\"cytology - ectocervix -\"))";
//		Formula dFormula3 = new DataCondFormula("[A.Diagnosis=='maligniteit vulva'][][]","ca-125 using meia","order rate",DeclareTemplate.Responded_Existence);
//		confFormulas[5] = "(  <>(\"bicarbonate\") ) ";
//		confFormulas[6]="( [](    ((\"calcium\") -> (\"Calcium - urgent\")) /\\  ((\"glucose\") -> (\"Glucose - urgent\"))   ) )";

		Vector<Formula> formulaVector = new Vector<Formula>();
		for (String lTLFormula : confFormulas) {
			SimpleFormula formula = new SimpleFormula(lTLFormula);
			formulaVector.add(formula);
		}
		
			LogReaderAndReplayer replayer = null;
		try {
			replayer = new LogReaderAndReplayer(log);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		DataSnapshotListener listener = new DataSnapshotListener(replayer.getDataTypes(), replayer.getActivityLabels());

		int f =0;
		for (Formula formula : formulaVector) {
			Vector<Formula> formulas = new Vector<Formula>();
			formulas.add(formula);
		
		//pw.println("Formula\tTraceId\teventPosition\teventIndex\tpredictedValue\tactualValue\tConfidence\tSupport\tsimilarityThreshold");
		pw.println("Formula\tTraceId\teventIndex\tpredictedValue\tactualValue\tConfidence\tInitTime\tTraceTime");
		pw.flush();
		
		double acc = 0.0;
					double tot = 0.0;
					double maybe = 0.0;
					double tp = 0.0;
					double tn = 0.0;
					double fp = 0.0;
					double fn = 0.0;
					double earliness = 0.0;

					double initTime = 0.0;
					double predTime = 0.0;
					
					double totalLogEventTime = 0.0;
					double totalLogEventNumber = 0.0;
					double numberEv = 0;
					double secs = 0;
		
	//	boolean start = false;
			for(XTrace trace : log){
			//	int x = 0;
			//	int y = trace.size()-1;
				//int eventIndex = 1;//(int)(Math.random()*(y-x))+x;
				tot++;
				Watch watch = new Watch();
				watch.start();
				long time = 0;
						
						
				int traceSize = trace.size();
				
				
			
		//		if(((XAttributeLiteral) trace.getAttributes().get("concept:name")).getValue().equals("00001100")){
		//			start = true;
		//		}
			//	if(start){
			
			if(trace.size()>0){

				System.out.println("Trace: "+XConceptExtension.instance().extractName(trace));



				boolean found = false;
				int currPrefIndex;
				int traceGap = 1;
				
				for (currPrefIndex = 0; currPrefIndex < trace.size() && !found; currPrefIndex+=traceGap) {
								System.out.println(currPrefIndex);
								Watch watch_ev = new Watch();
								watch_ev.start();
								long time_ev = 0;
								
								totalLogEventNumber ++;

				
				List<String> currentEvents = new Vector<String>();
								int i = 0;
								for(XEvent event : trace){
									currentEvents.add(event.getAttributes().get(XConceptExtension.KEY_NAME).toString());
									i++;
									if(i>currPrefIndex){
										break;
									}
								}
			
				
					Vector<String> currentVariables = new Vector<String>();
								for(String attribute : trace.get(currPrefIndex).getAttributes().keySet()){
									currentVariables.add(attribute);
								}

								Map<String, String> variables = new HashMap<String, String>();

								XAttributeMap traceAttr = trace.getAttributes();
								for(String attribute : traceAttr.keySet()){
									//if(!attribute.contains("date"))
									variables.put(attribute, traceAttr.get(attribute).toString());
								}
								

					i= 0;
					for(XEvent e : trace){
						XAttributeMap eventAttr = e.getAttributes();
						for(String attribute : eventAttr.keySet()){
							//	if(!attribute.contains("date"))
							variables.put(attribute, eventAttr.get(attribute).toString());
						}
						i++;
						if(i>currPrefIndex){
							break;
						}
					}
					
					numberEv ++;
					
					String ltlFormula = formula.getLTLFormula();
					List<ltl2aut.formula.Formula> formulaeParsed = new ArrayList<ltl2aut.formula.Formula>();
					try {
						formulaeParsed.add(new DefaultParser(ltlFormula).parse());
					} catch (SyntaxParserException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
					ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction
							.getFactory(treeFactory);
					GroupedTreeConjunction conjunction = conjunctionFactory.instance(formulaeParsed);
					Automaton aut = conjunction.getAutomaton().op.reduce();
					ExecutableAutomaton execAut = new ExecutableAutomaton(aut);
					execAut.ini();
					PossibleNodes current = execAut.currentState();
					boolean violated = false;
					for(XEvent e : trace){
						String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();
						violated = true;
						current = execAut.currentState();
						//	XAttributeMap eventAttr = e.getAttributes();
						//	String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();
						if(current!=null && !(current.get(0)==null)){
							for (Transition out : current.output()) {
								if (out.parses(label)) {
									violated = false;
									break;
								}
							}
						}
						if(!violated){
							execAut.next(label);
						}else{
							break;
						}
					}
					current = execAut.currentState();
					if(!violated){
						if(!current.isAccepting()){
							violated = true;
						}
					}

					Watch w = new Watch();
					w.start();
					Map<String, Result> result = OperationalSupport.provideOperationalSupport(formulas,currentEvents, currentVariables, variables, trace.getAttributes().get(XConceptExtension.KEY_NAME).toString());
					secs = secs + w.msecs();
					time = watch.msecs();
					
					totalLogEventTime = totalLogEventTime+time_ev;

					Result prediction = null;
					
					boolean key = result.keySet().iterator().hasNext();
					if(key ==true){
					
					
									prediction = result.get(result.keySet().iterator().next());
									System.out.println("confidence: "+prediction.getConfidence()+ " prediction: "+ prediction.isSatisfied());
									if (prediction.getConfidence()>minConfidenceThreshold && prediction.getSupport()>minSupportThreshold) {

										System.out.print("Predicted: "+ prediction.isSatisfied());
										System.out.println(" --- Real: "+ !violated);


										if (prediction.isSatisfied()==!violated)
											acc++;
										if (prediction.isSatisfied()){
											if (!violated)
												tp++;
											else
												fp++;
										} else {
											if (violated)
												tn++;
											else
												fn++;

										}
										earliness = earliness + (((double)currPrefIndex)/((double)trace.size()));
										initTime = prediction.getInitializationTime();
										predTime = predTime+time;

										pw.print(formula.getLTLFormula()+"\t");
										pw.flush();
										pw.print(XConceptExtension.instance().extractName(trace)+"\t");
										pw.flush();

										pw.print(currPrefIndex+"\t");
										pw.flush();

										pw.print(prediction.isSatisfied()+"\t");
										pw.flush();

										pw.print(!violated+"\t");
										pw.flush();

										pw.print(prediction.getConfidence()+"\t");
										pw.flush();

										pw.print(prediction.getInitializationTime()+"\t");
										pw.flush();
										//long t2 = System.currentTimeMillis();
										pw.print(time+"\t");
										pw.flush();


										pw.println("");

										found = true;

									}
									}
					

					}
					
												if (!found) {
								
								System.out.print("Predicted: maybe");
								//System.out.println(" --- Real: "+ !violated);

								pw.print(formula.getLTLFormula()+"\t");
								pw.flush();
								pw.print(XConceptExtension.instance().extractName(trace)+"\t");
								pw.flush();

								pw.print(currPrefIndex+"\t");
								pw.flush();

								pw.print("maybe\t");
								pw.flush();

								//pw.print(!violated+"\t");
								//pw.flush();

								pw.print("\t");
								pw.flush();

								pw.print("\t");
								pw.flush();
								//long t2 = System.currentTimeMillis();
								pw.print(time+"\t");
								pw.flush();

								pw.println("");
								maybe++;

							}
							
							
					
				}
	//		}
			}
			pw.println("CORRECT:\t"+acc);
					pw.println("MAYBE:\t"+maybe);
					pw.println("TRUE POSITIVE:\t"+tp);
					pw.println("TRUE NEGATIVE:\t"+tn);
					pw.println("FALSE POSITIVE:\t"+fp);
					pw.println("FALSE NEGATIVE:\t"+fn);
					pw.println("TOTAL:\t"+tot);
					acc = acc / (tot - maybe);
					pw.println("ACCURACY:\t"+acc);
					double tpr = tp/(tp+fn);
					pw.println("TPR:\t"+tpr);
					pw.println("FPR:\t"+fp/(fp+tn));
					double ppv = tp/(tp+fp);
					pw.println("PPV:\t"+ppv);
					pw.println("F1:\t"+(2*(ppv*tpr)/(ppv+tpr)));
					pw.println("EARLINESS AVG:\t"+earliness/(tot-maybe));
					pw.println("FAILURE RATE:\t"+maybe/tot);
					pw.println("INIT TIME:\t"+initTime);
					pw.println("TOATL TIME (ON TRACES) FOR THE PREDICTIVE EVENT :\t"+predTime);
					pw.println("AVG TIME (ON TRACES) FOR THE PREDICTIVE EVENT :\t"+predTime/(tot-maybe));
					
					pw.println("TOTAL LOG EVENT TIME:\t"+totalLogEventTime);
					pw.println("TOTAL NUMBER OF LOG EVENTS:\t"+totalLogEventNumber);
					pw.println("AVG LOG EVENT TIME:\t"+totalLogEventTime/totalLogEventNumber);
					secs = secs-initTime;
					secs = secs/ numberEv;
					pw.println("Average Prediction Time: "+secs);
					pw.close();

		}


	}

}