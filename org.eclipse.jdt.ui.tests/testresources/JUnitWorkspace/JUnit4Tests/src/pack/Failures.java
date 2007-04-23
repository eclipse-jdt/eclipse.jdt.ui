package pack;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

public class Failures {
	
	@Test
	public void testNasty() throws Exception {
		fail("</failure>");
	}
	
	@Ignore @Test
	public void ignored() throws Exception {
		fail("should not happen");
	}

	@Test
	public void testError() throws Exception {
		throw new IllegalStateException("</failure>");
	}
	
	@Test(expected=IllegalStateException.class)
	public void errorExpected() throws Exception {
		throw new IllegalStateException("expected");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void errorExpectedOther() throws Exception {
		throw new IllegalStateException("xx");
	}
	
	@Test()
	public void compareTheStuff() throws Exception {
		assertEquals("\nHello World.\n\n", "\n\nHello my friend.");
	}
	
	@Test
	public void testCompareNull() throws Exception {
		assertEquals("Hello World.", null);
	}
}
