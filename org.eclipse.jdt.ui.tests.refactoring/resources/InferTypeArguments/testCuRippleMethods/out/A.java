package p;

import java.util.Enumeration;
import java.util.Vector;

public class A {
	private void createTestList(TestCollector collector) {
		Enumeration<String> each= collector.collectTests();
		while (each.hasMoreElements()) {
			String s= each.nextElement();
		}
	}
}

interface TestCollector {
	public Enumeration<String> collectTests();
}

class Collector implements TestCollector {
	public Enumeration<String> collectTests() {
		Vector<String> v= new Vector<String>();
		v.add("Test1");
		return v.elements();
	}
	
}