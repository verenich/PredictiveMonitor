package org.processmining.plugins.predictive_monitor.bpmn;

public class Pair<F,S> {
	F first;
	S second;
	public Pair(F f, S s) {
		first = f;
		second = s;
	}
	public F getFirst() { return first; }
	public S getSecond() { return second; }
	
	public String toString() {
		return String.format("(%s,%s)", first, second);
	}
}
