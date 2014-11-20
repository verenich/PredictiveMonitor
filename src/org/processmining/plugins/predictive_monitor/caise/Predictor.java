package org.processmining.plugins.predictive_monitor.caise;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import weka_predictions.classifiers.trees.j48.J48_predictions;
import weka_predictions.core.Instances;
import weka_predictions.data_predictions.Result;


public class Predictor {
	
	public static Map<String, Result> suggestInput(String inputFilePath, Vector<String> currentVariables, Map<String, String> variables){
		Map<String, Result> results = new HashMap<String, Result>();
		 try {
			 	Thread.sleep(1000);
		 		BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
				Instances train = new Instances(reader);
				reader.close();
				// setting class attribute
				train.setClassIndex(train.numAttributes() - 1);
				
				
				String[] options = new String[3];
				options[0] = "-p";   
				options[1] = "-C";
				options[2] = "0.6";
				J48_predictions tree = new J48_predictions();     // new instance of tree
				tree.setOptions(options);     // set the options
				tree.buildClassifier(train);   // build classifier
				String graphString = tree.graph();

				
				results = tree.computeConditionInfo(currentVariables, variables);
/*				for (String string : results.keySet()) {
					System.out.println(string + " "+results.get(string).prettyString());
				}*/
				System.out. println("SIMPLIFIED PRETTY STRING");
				if (results!=null && !results.isEmpty()){
					for (String string : results.keySet()) {
						Result currResult = results.get(string);
						System.out.println(string + " "+currResult.simplifiedPrettyString());
					}		
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		 
		 return results;
				 
	}
	
	

}
