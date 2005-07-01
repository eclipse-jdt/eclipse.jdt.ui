package org.eclipse.jdt.junit.tests;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.junit.ui.ITraceDisplay;
import org.eclipse.jdt.internal.junit.ui.TextualTrace;

public class WrappingUnitTest extends TestCase {
	public void test00wrapSecondLine() throws Exception {
		TextualTrace trace = new TextualTrace("12345\n1234512345",
				new String[0]);
		trace.display(new ITraceDisplay() {
			public void addTraceLine(int lineType, String label) {
				assertEquals("12345", label);
			}
		}, 5);
	}
}
