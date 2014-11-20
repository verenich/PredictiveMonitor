package org.processmining.plugins.predictive_monitor.bpmn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.declareminer.ExecutableAutomaton;
import org.processmining.plugins.declareminer.PossibleNodes;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.AlternatePrecedenceAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.AlternateResponseAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.ChainPrecedenceAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.ChainResponseAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.NotChainPrecedenceAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.NotChainResponseAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.NotPrecedenceAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.NotRespondedExistenceAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.NotResponseAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.PrecedenceAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.RespondedExistenceAnalyzer;
import org.processmining.plugins.predictive_monitor.bpmn.dataconstraints.ResponseAnalyzer;

public class FormulaVerificator {
	
	public static HashMap<String, Boolean> computeTracesFormulaSatisfaction(XLog log, DataSnapshotListener listener, Vector<Formula> formulas){
		HashMap<String, Boolean> traceFormulaSatisfaction = new HashMap<String, Boolean>();
		for (XTrace trace : log) {
			boolean verified = isFormulaVerified(log, listener, trace, formulas);
			traceFormulaSatisfaction.put(XConceptExtension.instance().extractName(trace), new Boolean(verified));
		}
		return traceFormulaSatisfaction;
		
	}

	public static boolean isTraceViolated(DataSnapshotListener listener, Formula formula, XTrace trace){

		String ltlFormula = formula.getLTLFormula();
		List<ltl2aut.formula.Formula> formulaeParsed = new ArrayList<ltl2aut.formula.Formula>();
		boolean violated = true;
		try {
			formulaeParsed.add(new DefaultParser(ltlFormula).parse());
			TreeFactory<ConjunctionTreeNode, ConjunctionTreeLeaf> treeFactory = DefaultTreeFactory.getInstance();
			ConjunctionFactory<? extends GroupedTreeConjunction> conjunctionFactory = GroupedTreeConjunction
					.getFactory(treeFactory);
			GroupedTreeConjunction conjunction = conjunctionFactory.instance(formulaeParsed);
			Automaton aut = conjunction.getAutomaton().op.reduce();
			ExecutableAutomaton execAut = new ExecutableAutomaton(aut);
			execAut.ini();
	//		int index = 0;
			PossibleNodes current = null;
		//	Set<Integer> fulfillments = new HashSet<Integer>();

			if (formula instanceof SimpleFormula){
				for(XEvent e : trace){

					String label = ((XAttributeLiteral) e.getAttributes().get("concept:name")).getValue();

//					if(formula instanceof DataCondFormula){
//						DataCondFormula dataFormula = (DataCondFormula)formula;
//						DeclareTemplate template = dataFormula.getTemplate();
//						String activation = null;
//						switch (template) {
//							case Response:
//								activation = dataFormula.getParam1();
//								break;
//							case Precedence:
//								activation = dataFormula.getParam2();
//								break;
//							case Responded_Existence:
//								activation = dataFormula.getParam1();
//								break;
//							case Chain_Response:
//								activation = dataFormula.getParam1();
//								break;
//							case Alternate_Response:
//								activation = dataFormula.getParam1();
//								break;
//							case NotChain_Response:
//								activation = dataFormula.getParam1();
//								break;
//							case NotResponse:
//								activation = dataFormula.getParam1();
//								break;
//							case Alternate_Precedence:
//								activation = dataFormula.getParam2();
//								break;
//							case Chain_Precedence:
//								activation = dataFormula.getParam2();
//								break;
//							case NotChain_Precedence:
//								activation = dataFormula.getParam2();
//								break;
//							case NotPrecedence:
//								activation = dataFormula.getParam2();
//								break;
//							case NotResponded_Existence:
//								activation = dataFormula.getParam1();
//								break;
//
//						}
//						fulfillments.add(index);
//					}
					violated = true;
					current = execAut.currentState();
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

					current = execAut.currentState();

				}


				if(!violated){
					if(current.isAccepting()){
						violated = false;
					}else{
						violated = true;
					}
				}

			} else {

				//if(!violated){
				if(formula instanceof DataCondFormula){
					DataCondFormula dataFormula = (DataCondFormula)formula;
					DeclareTemplate template = dataFormula.getTemplate();
					String activation = null;
					switch (template) {
						case Response:
							ResponseAnalyzer info = new ResponseAnalyzer();
							violated = info.checkDataConditions(false, listener,((DataCondFormula) formula).getParam1(), ((DataCondFormula) formula).getParam2(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case Chain_Response:
							ChainResponseAnalyzer infoch = new ChainResponseAnalyzer();
							violated = infoch.checkDataConditions(false, listener,((DataCondFormula) formula).getParam1(), ((DataCondFormula) formula).getParam2(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case Alternate_Response:
							AlternateResponseAnalyzer infoalt = new AlternateResponseAnalyzer();
							violated = infoalt.checkDataConditions(false, listener,((DataCondFormula) formula).getParam1(), ((DataCondFormula) formula).getParam2(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case NotChain_Response:
							NotChainResponseAnalyzer infonotch = new NotChainResponseAnalyzer();
							violated = infonotch.checkDataConditions(false, listener,((DataCondFormula) formula).getParam1(), ((DataCondFormula) formula).getParam2(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case NotResponse:
							NotResponseAnalyzer infonot = new NotResponseAnalyzer();
							violated = infonot.checkDataConditions(false, listener,((DataCondFormula) formula).getParam1(), ((DataCondFormula) formula).getParam2(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case Precedence:
							PrecedenceAnalyzer info2 = new PrecedenceAnalyzer();
							violated = info2.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam2(), ((DataCondFormula) formula).getParam1(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case Alternate_Precedence:
							AlternatePrecedenceAnalyzer infoaltpre = new AlternatePrecedenceAnalyzer();
							violated = infoaltpre.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam2(), ((DataCondFormula) formula).getParam1(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case Chain_Precedence:
							ChainPrecedenceAnalyzer infochpre = new ChainPrecedenceAnalyzer();
							violated = infochpre.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam2(), ((DataCondFormula) formula).getParam1(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case NotChain_Precedence:
							NotChainPrecedenceAnalyzer infonotchpre = new NotChainPrecedenceAnalyzer();
							violated = infonotchpre.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam2(), ((DataCondFormula) formula).getParam1(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case NotPrecedence:
							NotPrecedenceAnalyzer infonotpre = new NotPrecedenceAnalyzer();
							violated = infonotpre.checkDataConditions(false, listener, ((DataCondFormula) formula).getParam2(), ((DataCondFormula) formula).getParam1(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case Responded_Existence:
							RespondedExistenceAnalyzer info3 = new RespondedExistenceAnalyzer();
							violated = info3.checkDataConditions(false, listener,((DataCondFormula) formula).getParam1(), ((DataCondFormula) formula).getParam2(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
						case NotResponded_Existence:
							NotRespondedExistenceAnalyzer info3not = new NotRespondedExistenceAnalyzer();
							violated = info3not.checkDataConditions(false, listener,((DataCondFormula) formula).getParam1(), ((DataCondFormula) formula).getParam2(), trace, ((DataCondFormula) formula).getDataCondition());
							break;
					}
				}	
			}	



		} catch (SyntaxParserException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return violated;
	}

	public static boolean isFormulaVerified(XLog log, DataSnapshotListener listener, XTrace trace, Vector<Formula> formulas){
		boolean violated = false;
		for (Formula formula : formulas) {	
			violated = violated || isTraceViolated(listener, formula, trace);
		}
		return !violated;
	}
}
