package p;

import java.util.Enumeration;

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
