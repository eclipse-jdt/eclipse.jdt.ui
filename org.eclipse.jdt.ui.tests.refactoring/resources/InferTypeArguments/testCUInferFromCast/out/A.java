package p;

import java.util.Enumeration;

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
