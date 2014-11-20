package org.processmining.plugins.predictive_monitor.bpmn.sequencemining;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class StringSequenceDatabaseCMSpade {

	private Map<String, Integer> alphabetMap;
	private ca.pfv.spmf_predictions.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase sequenceDB;
	
	
	public StringSequenceDatabaseCMSpade() {
		alphabetMap = new HashMap<String, Integer>();
		sequenceDB = null;
	}


	public StringSequenceDatabaseCMSpade(Map<String, Integer> alphabetMap, ca.pfv.spmf_predictions.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase sequenceDB) {
		this.alphabetMap = alphabetMap;
		this.sequenceDB = sequenceDB;
	}


	public Map<String, Integer> getAlphabetMap() {
		return alphabetMap;
	}


	public ca.pfv.spmf_predictions.algorithms.sequentialpatterns.spade_spam_AGP.dataStructures.database.SequenceDatabase getSequenceDB() {
		return sequenceDB;
	}
	
	public String getMappedString(int index){
		String searchedString = null;
		for (Iterator iterator = alphabetMap.keySet().iterator(); iterator.hasNext() && searchedString==null;) {
			String mappedString = (String) iterator.next();
			Integer mappedIndex = alphabetMap.get(mappedString);
			if (mappedIndex.intValue()==index)
				searchedString = mappedString;
			
		}
		return searchedString;
	}
	
	
	

}