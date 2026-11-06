package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.text.correction.ReorgCorrectionsSubProcessor;

public class QuickFixLTSMethodsTests {

	@Test
	public void testIsLTS() {
		boolean isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("8");
		assertEquals("Testing if version 8 is Lts", false, isLTS); // This version doesn't exist
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("1.8");
		assertEquals("Testing if version 1.8 is Lts", true, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("10");
		assertEquals("Testing if version 10 is Lts", false, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("15");
		assertEquals("Testing if Version 15 is Lts", false, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("17");
		assertEquals("Testing if Version 17 is Lts", true, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("21");
		assertEquals("Testing if Version 21 is Lts", true, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("22");
		assertEquals("Testing if Version 22 is Lts", false, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("25");
		assertEquals("Testing if Version 25 is Lts", true, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS("54");
		assertEquals("Testing non existing version if is Lts", false, isLTS);
		isLTS = ReorgCorrectionsSubProcessor.isVersionLTS(null);
		assertEquals("Testing null if is Lts", false, isLTS);
	}

	@Test
	public void testgetNextLTS() {
		String nextLTS = ReorgCorrectionsSubProcessor.getNextLTS(JavaCore.VERSION_1_2);
		assertEquals("Version passed: " + JavaCore.VERSION_1_2 + " to getNextLTS", JavaCore.VERSION_1_8, nextLTS);
		nextLTS = ReorgCorrectionsSubProcessor.getNextLTS(JavaCore.VERSION_1_8);
		assertEquals("Version passed: " + JavaCore.VERSION_1_8 + " to getNextLTS", JavaCore.VERSION_1_8, nextLTS);
		nextLTS = ReorgCorrectionsSubProcessor.getNextLTS(JavaCore.VERSION_9);
		assertEquals("Version passed: " + JavaCore.VERSION_9 + " to getNextLTS", JavaCore.VERSION_11, nextLTS);
		nextLTS = ReorgCorrectionsSubProcessor.getNextLTS(null);
		assertEquals("Version passed: null to getNextLTS", null, nextLTS);
		nextLTS = ReorgCorrectionsSubProcessor.getNextLTS("42");
		assertEquals("Version passed: 42 (nonexisting) to getNextLTS", null, nextLTS);
		nextLTS = ReorgCorrectionsSubProcessor.getNextLTS("12");
		assertEquals("Version passed: " + JavaCore.VERSION_12 + " to getNextLTS", JavaCore.VERSION_17, nextLTS);
		nextLTS = ReorgCorrectionsSubProcessor.getNextLTS("17");
		assertEquals("Version passed: " + JavaCore.VERSION_17 + " to getNextLTS", JavaCore.VERSION_17, nextLTS);
		nextLTS = ReorgCorrectionsSubProcessor.getNextLTS("22");
		assertEquals("Version passed: " + JavaCore.VERSION_22 + " to getNextLTS", JavaCore.VERSION_25, nextLTS);

	}

}
