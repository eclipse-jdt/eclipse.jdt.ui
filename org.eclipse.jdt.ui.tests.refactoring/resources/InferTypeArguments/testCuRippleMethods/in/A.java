package p;

import java.util.Enumeration;
import java.util.Vector;

public class A {
	private void createTestList(TestCollector collector) {
		Enumeration each= collector.collectTests();
		while (each.hasMoreElements()) {
			String s= (String) each.nextElement();
		}
	}
}

interface TestCollector {
	public Enumeration collectTests();
}

class Collector implements TestCollector {
	public Enumeration collectTests() {
		Vector v= new Vector();
		v.add("Test1");
		return v.elements();
	}
	
}