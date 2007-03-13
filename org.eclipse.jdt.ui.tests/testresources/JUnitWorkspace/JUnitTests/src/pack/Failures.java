package pack;

import junit.framework.TestCase;

public class Failures extends TestCase {

	public void testNasty() throws Exception {
		fail("</failure>");
	}

	public void testError() throws Exception {
		throw new IllegalStateException("</failure>");
	}
	
	public void testCompare() throws Exception {
		assertEquals("\nHello World.\n\n", "\n\nHello my friend.");
	}
	
	public void testCompareNull() throws Exception {
		assertEquals("Hello World.", null);
	}
}
