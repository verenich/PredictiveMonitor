package org.processmining.plugins.predictive_monitor.bpmn;

import java.util.List;


public class EditDistance {

    
    private List<String> initialString;
    private List<String> finalString;
    private Entry entryTable[][];

   
    public EditDistance(List<String> x, List<String> y) {
        this.initialString = x;
        this.finalString = y;
    }

    public int computeEditDistance() {
        entryTable = new Entry[finalString.size() + 1][initialString.size() + 1];
        entryTable[0][0] = new Entry(0, Operation.NOOP); // initial state
        
        for (int i = 0; i < finalString.size() + 1; i++) {
            for (int j = 0; j < initialString.size() + 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }

                int minCost = Integer.MAX_VALUE;
                Operation minCostOp = null;

                if (i > 0 && j > 0 && initialString.get(j - 1).equals(finalString.get(i - 1))) {
                    int c = entryTable[i - 1][j - 1].getCost() + Operation.OK.cost;
                    if (c < minCost) {
                        minCost = c;
                        minCostOp = Operation.OK;
                    }
                }

                if (i > 0 && j > 0) {
                    int c = entryTable[i - 1][j - 1].getCost() + Operation.REPLACE.cost;
                    if (c < minCost) {
                        minCost = c;
                        minCostOp = Operation.REPLACE;
                    }
                }

                if (i > 0) {
                    int c = entryTable[i - 1][j].getCost() + Operation.INSERT.cost;
                    if (c < minCost) {
                        minCost = c;
                        minCostOp = Operation.INSERT;
                    }
                }

                if (j > 0) {
                    int c = entryTable[i][j - 1].getCost() + Operation.DELETE.cost;
                    if (c < minCost) {
                        minCost = c;
                        minCostOp = Operation.DELETE;
                    }
                }

                entryTable[i][j] = new Entry(minCost, minCostOp);
            }
        }
        
        return entryTable[finalString.size()][initialString.size()].getCost();
    }
    
    public double computeNormalizedEditDistance() {
    	int editDistance = computeEditDistance();
    	int max = Math.max(initialString.size(),finalString.size());
    	double normalizedEditDistance = 0.0;
    	if (max>0)
    		normalizedEditDistance = ((double) editDistance)/(initialString.size()+finalString.size());
		return normalizedEditDistance;
    }

    public double computeNormalizedSimilarity() {
    	double normalizedEditDistance = computeNormalizedEditDistance();
		return (1-normalizedEditDistance);
    }


}
