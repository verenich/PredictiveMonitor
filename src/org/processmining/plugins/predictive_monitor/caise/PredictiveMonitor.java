package org.processmining.plugins.predictive_monitor.caise;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.bind.DatatypeConverter;

import ltl2aut.automaton.Automaton;
import ltl2aut.automaton.Transition;
import ltl2aut.formula.DefaultParser;
import ltl2aut.formula.conjunction.ConjunctionFactory;
import ltl2aut.formula.conjunction.ConjunctionTreeLeaf;
import ltl2aut.formula.conjunction.ConjunctionTreeNode;
import ltl2aut.formula.conjunction.DefaultTreeFactory;
import ltl2aut.formula.conjunction.GroupedTreeConjunction;
import ltl2aut.formula.conjunction.TreeFactory;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.operationalsupport.provider.AbstractProvider;
import org.processmining.operationalsupport.provider.Provider;
import org.processmining.operationalsupport.server.OSService;
import org.processmining.operationalsupport.session.Session;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;

import weka_predictions.data_predictions.Result;

/**
 * 
 * @author Fabrizio Maria Maggi
 * 
 */

@Plugin(name = "Preidctive Monitor", parameterLabels = { "Operational Support Service", "Log"}, returnLabels = { "Predictive Monitor" }, returnTypes = { PredictiveMonitor.class }, userAccessible = true)
public class PredictiveMonitor extends AbstractProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3042748916288208677L;
	private XLog log;
	private boolean all = false;
	private double similThresh = 0.5;
	private double minNumbTraces = 30;

	//private File integrate = new File("C:/Users/Fabrizio/Desktop/integrate.rdf");
	//PrintWriter pw = new PrintWriter(new FileWriter(integrate));
	//private WeightedDeclareModel wmodel;
	//private String[] constraints;
	////private String[] forms;
	//private String formula;
	//private final Hashtable formulaTable;

	//private final Hashtable hash = new Hashtable();

	//private Xes2RecTraceTranslator traceTranslator;
	//private RecEngine recProxy;

	/*
	 * IMPLEMENTARE METODI ACCEPT SIMPLE E GET NAME
	 * 
	 * public <R, L> R simple(final Session session, final XLog emptylog, final
	 * String langauge, final L query, final boolean false true se hai fatto)
	 */

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "F.M. Maggi et al.", email = "F.M.Maggi@ut.ee", uiLabel = "Predictive Monitor C", pack = "PredictiveMonitor")
	@PluginVariant(variantLabel = "Predictive Monitor C", requiredParameterLabels = { 0, 1 })
	public static Provider registerServiceProviderAUI(final UIPluginContext context, OSService service, XLog log) {
		return registerServiceProviderA(context, service, log);
		//In the final version, it will use a translator plug-in Climb->Event Calculus, and then simply call the other variant!

	}

	//	@PluginVariant(variantLabel = "Default settings", requiredParameterLabels = { 0,1 })
	public static Provider registerServiceProviderA(final PluginContext context, OSService service, XLog log) {

		try {
			PredictiveMonitor provider = new PredictiveMonitor(service,log);
			context.getFutureResult(0).setLabel(context.getFutureResult(0).getLabel() + " on port " + service);
			return provider;
		} catch (Exception e) {
			context.log(e);
		}
		return null;
	}

	public PredictiveMonitor(OSService owner, XLog log) throws Exception {
		super(owner);
		this.log = log;
	}

	protected String convert(XTrace trace, int pos) {
		XExtendedEvent ev = XExtendedEvent.wrap(trace.get(pos));
		return "" + ev.getTimestamp().getTime();
	}

	public String getName() {
		return "Mobucon LTL";
	}

	public boolean accept(final Session session, final List<String> modelLanguages, final List<String> queryLanguages,
			Object model) {

		//Hashtable replays = new Hashtable();
		//session.put("replays", replays);
		//		try {
		//			SAXBuilder sb = new SAXBuilder();
		//			DOMOutputter outputter = new DOMOutputter();
		//			Document xesDocument = sb.build(new StringReader((String)model));
		//			XMLOutputter xmlOutput = new XMLOutputter();
		//			File modelFile = File.createTempFile("model", ".xml");
		//			modelFile.deleteOnExit();
		//			xmlOutput.output(xesDocument, new FileWriter(modelFile));
		//
		//
		//		} catch (Exception ex) {
		//			ex.printStackTrace();
		//		}
		return true;
	}

	public <R, L> R simple(final Session session, final XLog availableItems, final String langauge, final L query,
			final boolean done) throws Exception {

		if ((query == null) || !(query instanceof String)) {
			return null;
		}
		//Map<String,Result> output = new HashMap<String, Result>();

		XTrace lastTrace;
		XTrace trace = session.getCompleteTrace();
		lastTrace = session.getLastTrace();
		if (lastTrace.size() == 0) {
			lastTrace = session.getCompleteTrace();
		}
		Vector<Formula> formulas = (Vector<Formula>) session.getObject("formulas");

		//String requiredPrefix = "Enter Loan Application;Retrieve Applicant Data;Length receipt;";

		ArrayList<String> requiredPrefix = new ArrayList<String>();
		for(XEvent event : lastTrace){
			requiredPrefix.add(XConceptExtension.instance().extractName(event));
		}
		File arff = File.createTempFile("classification", ".arff");

		//String outputFileName = "C:\\Users\\fabrizio\\Desktop\\loan.arff";
		Vector<TracePrefix> traceIdPrefixDisc = new Vector<TracePrefix>();
		Vector<TracePrefix> traceIdPrefixCh = new Vector<TracePrefix>();

		int traceIndex = 0;
		for(XTrace t : log){
			ArrayList<String> chPrefixList = new ArrayList<String>();
			double maxSimil = -1;
			String checkPref = "";
			String closestPrefix = "";
			for(XEvent e: t){
				String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();
				checkPref = checkPref+label+";";
				chPrefixList.add(label);
				EditDistance ed = new EditDistance(requiredPrefix, chPrefixList);
				double similarity = ed.computeNormalizedSimilarity();
				if(similarity>maxSimil){
					maxSimil = similarity;
					closestPrefix = checkPref;
				}				
			}
			TracePrefix prefix = new TracePrefix(maxSimil,closestPrefix,traceIndex); 
			if(maxSimil>=similThresh){
				traceIdPrefixCh.add(prefix);
			}else{
				traceIdPrefixDisc.add(prefix);
			}
			traceIndex ++;
		}
		int discovTraces = traceIdPrefixCh.size();
		if(discovTraces<((int)minNumbTraces)){
			Collections.sort(traceIdPrefixDisc);
			for(TracePrefix p : traceIdPrefixDisc){
				if(discovTraces==((int)minNumbTraces)){
					break;
				}
				traceIdPrefixCh.add(p);
				discovTraces++;
			}
		}

		System.out.println(traceIdPrefixCh.size());
		LogReaderAndReplayer replayer = new LogReaderAndReplayer(log);
		DataSnapshotListener listener = new DataSnapshotListener(replayer.getDataTypes(), replayer.getActivityLabels());


		String inputActivity =  XConceptExtension.instance().extractName(lastTrace.get(lastTrace.size()-1));
		//String[] formulaLTL = {"<>(\"Notify Eligibility\")"}; 
		//String globalFormula = "";
		for (Formula formula : formulas) {
			String ltlFormula = formula.getLTLFormula();
			List<ltl2aut.formula.Formula> formulaeParsed = new ArrayList<ltl2aut.formula.Formula>();
			formulaeParsed.add(new DefaultParser(ltlFormula).parse());
			TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
			ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction
					.getFactory(treeFactory);
			GroupedTreeConjunction conjunction = conjunctionFactory.instance(formulaeParsed);
			Automaton aut = conjunction.getAutomaton().op.reduce();
			ExecutableAutomaton execAut = new ExecutableAutomaton(aut);
			execAut.ini();
			//	PossibleNodes current = execAut.currentState();

			boolean violated = true;

			String relationName = "@relation data";

			ArrayList<String> datalines = new ArrayList<String>();

			//	HashMap<String, String> dataValues = new HashMap<String, String>();
			String currentPrefix = "";
			HashMap<String, ArrayList<String>> attributeTypes = new HashMap<String, ArrayList<String>>();
			HashMap<String, ArrayList<String>> tempAttributeTypes = new HashMap<String, ArrayList<String>>();
			for(TracePrefix pr : traceIdPrefixCh){
				XTrace t = log.get(pr.getTraceId());
				tempAttributeTypes = new HashMap<String, ArrayList<String>>();
				currentPrefix = "";
				//String dataValues = "";
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
						//	dataValues.put(attribute, traceAttr.get(attribute).toString());
					}
				}


				for(XEvent e : t){

					String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();
					currentPrefix = currentPrefix+label+";";
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
							//if(all || (label.equals(inputActivity) && (currentPrefix.equals(requiredPrefix)))){
							if(all || ((currentPrefix.equals(pr.getPrefix())))){
								for(String attrib : tempAttributeTypes.keySet()){
									attributeTypes.put(attrib, tempAttributeTypes.get(attrib));
								}

							}


						}
					}
				}

			}
			//boolean firstOccurrenceofInputAct = true;
			try{
				PrintWriter pw = new PrintWriter(new FileWriter(arff));
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
				pw.println("@attribute conformant {yes,no}");
				HashMap<String, String> dataValues = new HashMap<String, String>();
				ArrayList<HashMap<String, String>> dataPerTrace = new ArrayList<HashMap<String, String>>();
				pw.println("");
				pw.println("@data");
				boolean found = false;
				for(TracePrefix pr : traceIdPrefixCh){
					XTrace t = log.get(pr.getTraceId());
					Set<Integer> fulfillments = new HashSet<Integer>();
					found = false;
					currentPrefix = "";
					//	firstOccurrenceofInputAct = true;
					dataValues = new HashMap<String, String>();
					execAut.ini();
					PossibleNodes current = null;
					XAttributeMap traceAttr = t.getAttributes();
					for(String attribute : traceAttr.keySet()){
						if(!attribute.equals("Activity code") &&!attribute.equals("creator")&&!attribute.contains(":")&& !attribute.equals("description")){
							dataValues.put(attribute, traceAttr.get(attribute).toString());
						}
					}
					String line = "";
					int index = 0;
					for(XEvent e : t){

						String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();

						if(formula instanceof DataCondFormula){
							DataCondFormula dataFormula = (DataCondFormula)formula;
							DeclareTemplate template = dataFormula.getTemplate();
							String activation = null;
							switch (template) {
								case Response:
									activation = dataFormula.getParam1();
									break;
								case Precedence:
									activation = dataFormula.getParam2();
									break;
								case Responded_Existence:
									activation = dataFormula.getParam1();
									break;
							}
							fulfillments.add(index);
						}
						currentPrefix = currentPrefix+label+";";
						violated = true;
						current = execAut.currentState();
						XAttributeMap eventAttr = e.getAttributes();
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
						}
						for(String attribute : eventAttr.keySet()){
							if(!attribute.equals("Activity code")&&!attribute.contains(":")){
								dataValues.put(attribute, eventAttr.get(attribute).toString());
							}
						}


						//if(label.equals(inputActivity) && currentPrefix.equals(requiredPrefix)){
						if(currentPrefix.equals(pr.getPrefix())){
							found = true;
						}

						if(!all){
							//if(label.equals(inputActivity) && currentPrefix.equals(requiredPrefix)){
							if(currentPrefix.equals(pr.getPrefix())){
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
							}
						}
						index ++;
					}

					current = execAut.currentState();

					if(all){

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

					}

					if(!violated){
						if(formula instanceof DataCondFormula){
							DataCondFormula dataFormula = (DataCondFormula)formula;
							DeclareTemplate template = dataFormula.getTemplate();
							String activation = null;
							switch (template) {
								case Response:
									ResponseInfo info = new ResponseInfo();
									violated = info.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam2(), trace, fulfillments, ((DataCondFormula) formula).getDataCondition());
									break;
								case Precedence:
									PrecedenceInfo info2 = new PrecedenceInfo();
									violated = info2.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam1(), trace, fulfillments, ((DataCondFormula) formula).getDataCondition());
									break;
								case Responded_Existence:
									RespondedExistenceInfo info3 = new RespondedExistenceInfo();
									violated = info3.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam2(), trace, fulfillments, ((DataCondFormula) formula).getDataCondition());
									break;
							}
						}	
					}	

					if(found && !violated && !line.equals("")){
						if(current.isAccepting()){
							pw.println(line+", yes");
						}else{
							pw.println(line+", no");
						}
					}

				}


				pw.flush();
				pw.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		Map<String, Result> suggestions = Predictor.suggestInput(arff.getAbsolutePath(), (Vector<String> )session.getObject("currentVariables"), (Map<String, String>) session.getObject("variables"));



		return (R) suggestions;
	}


}