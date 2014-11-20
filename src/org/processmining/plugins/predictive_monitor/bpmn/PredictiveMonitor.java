package org.processmining.plugins.predictive_monitor.bpmn;

/* dropbox version*/
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.factory.XFactoryRegistry;
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
import org.processmining.plugins.declareminer.Watch;
import org.processmining.plugins.guidetreeminer_predictions.ClusterLogOutput;
import org.processmining.plugins.guidetreeminer_predictions.GuideTreeMinerInput;
import org.processmining.plugins.guidetreeminer_predictions.MineGuideTree;
import org.processmining.plugins.guidetreeminer_predictions.types.AHCJoinType;
import org.processmining.plugins.guidetreeminer_predictions.types.DistanceMetricType;
import org.processmining.plugins.guidetreeminer_predictions.types.GTMFeature;
import org.processmining.plugins.guidetreeminer_predictions.types.GTMFeatureType;
import org.processmining.plugins.guidetreeminer_predictions.types.LearningAlgorithmType;
import org.processmining.plugins.guidetreeminer_predictions.types.SimilarityDistanceMetricType;
import org.processmining.plugins.guidetreeminer_predictions.types.SimilarityMetricType;
import org.processmining.plugins.predictive_monitor.bpmn.sequencemining.DiscriminativeSequenceMiner;
import org.processmining.plugins.predictive_monitor.bpmn.sequencemining.SequenceMiner;

import weka_predictions.classifiers.trees.RandomForest_predictions;
import weka_predictions.classifiers.trees.j48.J48_predictions;
import weka_predictions.data_predictions.Result;


/**
 *
 * @author Fabrizio Maria Maggi
 *
 */

@Plugin(name = "Predictive Monitor", parameterLabels = { "Operational Support Service", "Log"}, returnLabels = { "Predictive Monitor" }, returnTypes = { PredictiveMonitor.class }, userAccessible = true)
public class PredictiveMonitor extends AbstractProvider {

	/**
	 *
	 */
	private static final long serialVersionUID = 3042748916288208677L;
	private XLog log;

	// classifier
	private boolean randomForest = false;

	// pattern
	private boolean usePatterns = false;
	private boolean withHoles = true; //allow gaps in prefix
	private boolean discriminative = false; //if true, only patternMinimumSupport is considered
	private double patternMinimumSupport=0.3;
	private int minimumPatternLength=1;
	private int maximumPatternLength=4;
	private double discriminativeMinimumSupport=0.3; //minimum support for a pattern to be discriminant (% of traces where it occurs)

	//trace prefix
	private  int minPrefixLength = 1;
	private  int maxPrefixLength = 25;
	private  int prefixGap = 1;

	//clustering
	private boolean traceClustering = true;
	private GTMFeatureType clusteringType = GTMFeatureType.WholeTrace;
	private boolean frequencyTraceClustering = true; //if true, ignore clusteringType
	private int clusterNumber = 50;

	//	private ArrayList<Pattern> patterns = null;
	//HashMap<String, ArrayList<String>> attributeTypes = null;
	private RandomForest_predictions rF = null;
	private J48_predictions tree = null;
	//private String arffPath = null;
	private HashMap<String, String> currentVariables = null;

	private HashMap<Integer,XLog> clusters = null;
	private HashMap<Integer,J48_predictions> trees = null;
	private HashMap<Integer,RandomForest_predictions> rForests = null;
	private HashMap<Integer, ArrayList<Pattern>> patternsMap = null;
	private HashMap<Integer, String> arffPathMap = null;
	private HashMap<Integer, HashMap<String, ArrayList<String>> > attributeTypeMap = null;
	private HashMap<Integer,XTrace> centroids = null;
	private TraceClusterer tClust = null;
	private long initTime = 0;

	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "F.M. Maggi et al.", email = "F.M.Maggi@ut.ee", uiLabel = "Predictive Monitor B", pack = "PredictiveMonitor")
	@PluginVariant(variantLabel = "Predictive Monitoring B", requiredParameterLabels = { 0, 1 })
	public static Provider registerServiceProviderAUI(final UIPluginContext context, OSService service, XLog log) {
		return registerServiceProviderA(context, service, log);
	}

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
		return "Predictive Monitor";
	}

	public boolean accept(final Session session, final List<String> modelLanguages, final List<String> queryLanguages,
			Object model) {
		String pFile = "./output/patterns";

		boolean newConfiguration = true;

		Map<String,Integer>configuration =(HashMap<String, Integer>) session.getObject("configuration");
		if (configuration.get("newConfiguration")!=null){
			if (configuration.get("newConfiguration") >0)
				newConfiguration = true;
			else
				newConfiguration = false;
		}


		//XLog prefixTraceLog = computePrefixTraceLog(log, prefixLength);


		if(newConfiguration){

			Watch initWatch = new Watch();
			initWatch.start();

			LogReaderAndReplayer replayer = null;
			try {
				replayer = new LogReaderAndReplayer(log);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			DataSnapshotListener listener = new DataSnapshotListener(replayer.getDataTypes(), replayer.getActivityLabels());

			Vector<Formula> formulas = (Vector<Formula>) session.getObject("formulas");
			HashMap<String, Boolean> histTraceSatisfaction = FormulaVerificator.computeTracesFormulaSatisfaction(log, listener, formulas);
			XLog clusterLog = null;

			if (configuration.get("maxPrefixLength")!=null)
				maxPrefixLength = configuration.get("maxPrefixLength");
			if (configuration.get("prefixGap")!=null)
				prefixGap = configuration.get("prefixGap");
			if (configuration.get("clusterNumber")!=null)
				clusterNumber = configuration.get("clusterNumber");


			List<XLog> logs = null;
			clusters = new HashMap<Integer, XLog>();
			trees = new HashMap<Integer, J48_predictions>();
			rForests = new HashMap<Integer, RandomForest_predictions>();
			patternsMap = new HashMap<Integer, ArrayList<Pattern>>();
			arffPathMap = new HashMap<Integer, String>();
			attributeTypeMap = new HashMap<Integer, HashMap<String, ArrayList<String>>>();
			centroids = new HashMap<Integer, XTrace>();

			GuideTreeMinerInput input = new GuideTreeMinerInput();
			//XLog lig = computePrefixTraceLog(log,5,25);
			//	GTMFeature feature = new GTMFeature();
			if (traceClustering){
				if (frequencyTraceClustering) {
					tClust = new TraceClusterer();
					XLog prefixLog = computePrefixTraceLog(log,minPrefixLength,maxPrefixLength,prefixGap);
					HashMap<Integer, ArrayList<Integer>> instanceMap = tClust.clusterTraceByFrequency(prefixLog, clusterNumber);
					logs = new ArrayList<XLog>();
					for (Integer integer : instanceMap.keySet()) {
						logs.add(tClust.computeXLogCluster(instanceMap.get(integer)));
					}
				} else {
					if(clusteringType.equals(GTMFeatureType.Alphabet)){
						input.setFeatureType(GTMFeatureType.Alphabet);
						input.setFeature(GTMFeature.IE);
						//input.setFeature(GTMFeature.TR);
						input.addFeature(GTMFeature.IE);
						input.setSimilarityMetricType(SimilarityMetricType.FScore);
						input.setSimilarityDistanceMetricType(SimilarityDistanceMetricType.Similarity);
						//input.addFeature(GTMFeature.TR);
						input.setBaseFeatures(true);
						clusterLog = (XLog) log.clone();
						input.setNumberOfClusters(clusterNumber);
						input.setLearningAlgorithmType(LearningAlgorithmType.AHC);
						input.setAhcJoinType(AHCJoinType.MinVariance);
						input.setIsOutputClusterLogs(true);
					}else if(clusteringType.equals(GTMFeatureType.WholeTrace)){
						input.setFeatureType(GTMFeatureType.WholeTrace);
						clusterLog = computePrefixTraceLog(log,minPrefixLength,maxPrefixLength,prefixGap);
						input.setNumberOfClusters(clusterNumber);
						input.setDistanceMetricType(DistanceMetricType.GenericEditDistance);
						input.setLearningAlgorithmType(LearningAlgorithmType.AHC);
						input.setAhcJoinType(AHCJoinType.MinVariance);
						input.setIsOutputClusterLogs(true);
					}

					MineGuideTree guide = new MineGuideTree();
					Object[] patt = guide.mine(input, clusterLog);
					logs = ((ClusterLogOutput) patt[1]).clusterLogList();
				}
			} else {
				logs.add(computePrefixTraceLog(log, minPrefixLength, maxPrefixLength,prefixGap));
			}

			List<XLog> filteredLogs = ArffBuilder.filterClusterLog(logs, histTraceSatisfaction, 0.8);
			int clusterNumber = 1;
			for(XLog l : filteredLogs){
				ArrayList<Pattern> patterns = new ArrayList<Pattern>();
				if (usePatterns){
					if(withHoles){
						patterns = (ArrayList<Pattern>) SequenceMiner.mineFrequentPatternsCMSpadeWithHoles(l, patternMinimumSupport, minimumPatternLength,maximumPatternLength);
					}else{
						patterns = (ArrayList<Pattern>) SequenceMiner.mineFrequentPatternsPrefixWithoutHoles(l, patternMinimumSupport, minimumPatternLength);
					}
					printPatterns(patterns, pFile);
					// ADD PATTERNS TO VARIABLES

					if (discriminative){
						String discrPFile = "./output/discr_patterns";
						patterns = (ArrayList<Pattern>) DiscriminativeSequenceMiner.mineDiscriminativePatterns(l, patterns, histTraceSatisfaction,discriminativeMinimumSupport, discriminativeMinimumSupport);
						printPatterns(patterns, discrPFile);
					}
				}

				File arff = ArffBuilder.writeArffFile(l, patterns,histTraceSatisfaction);
				HashMap<String, ArrayList<String>> attributeTypes = ArffBuilder.getAttributeTypes();
				//arffPath = arff.getAbsolutePath();
				if (randomForest){
					rF=Predictor.trainRandomForest(arff.getAbsolutePath());
					rForests.put(clusterNumber, rF);
				} else {
					tree = Predictor.trainDecisionTree(arff.getAbsolutePath());
					trees.put(clusterNumber, tree);
				}

				ClusterManager manager = new ClusterManager();
				patternsMap.put(clusterNumber, patterns);
				arffPathMap.put(clusterNumber, arff.getAbsolutePath());
				attributeTypeMap.put(clusterNumber, attributeTypes);
				if (!frequencyTraceClustering)
					centroids.put(clusterNumber, manager.getCentroid(l));
				clusters.put(clusterNumber, l);
				clusterNumber++;
			}
			initTime = initWatch.msecs();
		}
		if (randomForest){
			session.put("classifier", rF);
			session.put("rForests", rForests);
			session.put("arffPathMap", arffPathMap);
		} else {
			session.put("classifier", tree);
			session.put("trees", trees);
		}

		session.put("tClust", tClust);
		session.put("clusters", clusters);
		session.put("centroids", centroids);
		session.put("initTime", new Long(initTime));

		session.put("patternsMap", patternsMap);
		session.put("attributeTypeMap", attributeTypeMap);
		return true;
	}

	private void printPatterns(ArrayList<Pattern> patterns, String filePath){
		try {
			FileWriter fW = new FileWriter(new File(filePath));
			for (Pattern pattern : patterns) {
				for (String item : pattern.getItems()) {
					fW.write(item+" ");
					System.out.print(item+" ");
				}
				fW.write("\n");
				System.out.println();

			}
			fW.flush();
			fW.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	public <R, L> R simple(final Session session, final XLog availableItems, final String langauge, final L query,
			final boolean done) throws Exception {

		if ((query == null) || !(query instanceof String)) {
			return null;
		}

		//List<Pattern> minedPatterns = null;
		XTrace lastTrace;
		//XTrace trace = session.getCompleteTrace();
		lastTrace = session.getLastTrace();
		if (lastTrace.size() == 0) {
			lastTrace = session.getCompleteTrace();
		}

		//File arff = (File) session.getObject("arff");

		//	ArrayList<Pattern> patterns = (ArrayList<Pattern>) session.getObject("patterns");
		//HashMap<String, ArrayList<String>> attributeTypes = (HashMap<String, ArrayList<String>>) session.getObject("attributeTypes");
		//String arffPath = (String) session.getObject("arffPath");




		HashMap<Integer,XLog> clusters = (HashMap<Integer,XLog>) session.getObject("clusters");
		HashMap<Integer,J48_predictions> trees = (HashMap<Integer,J48_predictions>) session.getObject("trees");
		HashMap<Integer,RandomForest_predictions> rForests = (HashMap<Integer,RandomForest_predictions>) session.getObject("rForests");
		HashMap<Integer,ArrayList<Pattern>> patternsMap = (HashMap<Integer,ArrayList<Pattern>>) session.getObject("patternsMap");
		HashMap<Integer,String> arffPathMap = (HashMap<Integer,String>) session.getObject("arffPathMap");
		HashMap<Integer,HashMap<String, ArrayList<String>>> attributeTypeMap = (HashMap<Integer,HashMap<String, ArrayList<String>>> ) session.getObject("attributeTypeMap");
		HashMap<Integer,XTrace> centroids = (HashMap<Integer,XTrace>) session.getObject("centroids");
		Long initTime = (Long) session.getObject("initTime");



		int cl = -1;
		TraceClusterer tClust = (TraceClusterer) session.getObject("tClust");
		if (tClust!=null){
			cl = tClust.clusterTrace(lastTrace, clusters);
			System.out.println("CLUSTER "+cl);
		} else {
			int j = 1;
			ArrayList<String> current = new ArrayList<String>();
			//		XTrace tr = trace.get(0);
			for (XEvent e : lastTrace) {
				current.add(XConceptExtension.instance().extractName(e));
			}
			double mindist = 0;
			cl = 0;
			for(Integer cluster: clusters.keySet()){
				ArrayList<String> paragone = new ArrayList<String>();
				for (XEvent e : centroids.get(cluster)) {
					paragone.add(XConceptExtension.instance().extractName(e));
				}
				EditDistance edit = new EditDistance(current, paragone);
				double currdist = edit.computeNormalizedSimilarity();
				if(currdist>mindist){
					mindist = currdist;
					cl = j;
				}
				j++;
			}
		}

		ArrayList<Pattern> patterns = patternsMap.get(cl);
		HashMap <String, String> variables = session.getObject("variables");

		Map<String, Result> suggestions = null;
		if(!randomForest) {

			ArrayList<Integer> patternVector = ArffBuilder.generatePatternVector(lastTrace, patterns);
			for (int i = 0; i < patternVector.size(); i++) {
				Integer pattern = patternVector.get(i);
				String patternId = "P"+i;
				String value = null;
				if(pattern>0)
					value="true";
				else
					value="false";
				variables.put(patternId, value);
			}

			//	MyJ48 tree = (MyJ48) session.getObject("classifier");
			J48_predictions tree = trees.get(cl);
			suggestions = Predictor.makePredictionDecisionTree(tree,  (Vector<String> )session.getObject("currentVariables"),variables);
			//suggestions =Predictor.makePredictionDecisionTree(arff.getAbsolutePath(), (Vector<String> )session.getObject("currentVariables"), (Map<String, String>) session.getObject("variables"));
		}else{
			weka_predictions.core.Instance instance = ArffBuilder.getTraceInstance(lastTrace, patterns, attributeTypeMap.get(cl), variables);
			//RandomForest rF = (RandomForest) session.getObject("classifier");
			//suggestions = Predictor.makePredictionRandomForest(rF, arffPath, instance);
			RandomForest_predictions rF = rForests.get(cl);
			suggestions = Predictor.makePredictionRandomForest(rF, arffPathMap.get(cl), instance);
		}
		setInitializationTime(suggestions, initTime);
		return (R) suggestions;
	}

	private void setInitializationTime(Map<String, Result> predictions, long initializationTime){
		for (String predictionKey : predictions.keySet()) {
			Result prediction = predictions.get(predictionKey);
			prediction.setInitializationTime(initializationTime);
			//predictions.put(predictionKey, prediction);
		}
	}


	public XLog computePrefixTraceLog(XLog log, int prefixLength){
		XLog prefixTraceLog  = XFactoryRegistry.instance().currentDefault().createLog();
		prefixTraceLog.setAttributes(log.getAttributes());
		for (XTrace trace : log) {
			prefixTraceLog.add(getPrefixTrace(trace, prefixLength));
		}
		return prefixTraceLog;
	}

	private XTrace getPrefixTrace(XTrace trace, int prefixLength ){
		XTrace prefixTrace = XFactoryRegistry.instance().currentDefault().createTrace(trace.getAttributes());
		int i=0;
		for (Iterator iterator = trace.iterator(); iterator.hasNext() && i<prefixLength;) {
			XEvent event = (XEvent) iterator.next();
			prefixTrace.add(event);
			i++;
		}

		return prefixTrace;
	}

	public XLog computePrefixTraceLog(XLog log, int minPrefixLength, int maxPrefixLength, int gap){
		XLog prefixTraceLog  = XFactoryRegistry.instance().currentDefault().createLog();
		prefixTraceLog.setAttributes(log.getAttributes());
		for (XTrace trace : log) {
			for (int pL = minPrefixLength; pL <= maxPrefixLength; pL = pL+gap) {
				prefixTraceLog.add(getPrefixTrace(trace, pL));
			}

		}
		return prefixTraceLog;
	}

}