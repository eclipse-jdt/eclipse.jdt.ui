/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSScanner;


public class NLSScannerTester extends TestCase {


	public NLSScannerTester(String name) {
		super(name);
	}
	
	public static void main (String[] args) {
		junit.textui.TestRunner.run (suite());
	}
	
	public static Test suite() {
		return new TestSuite(NLSScannerTester.class);
	}
	
	private void printDisabledMessage(String msg){
		System.out.println("\nTest " + getName() + " disabled (" + msg + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public void test0() throws Exception{
		String text= "fred"; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		assertEquals("empty", true, l.length == 0); //$NON-NLS-1$
	}
	
	public void test1() throws Exception{
//		String text= "fred\"x\"";
//		List l= NLSScanner.scan(text);
//		assertEquals("non empty", false, l.isEmpty());
//		assertEquals("1 line", 1, l.size());
		printDisabledMessage("Scanner does not handle strings in the first line"); //$NON-NLS-1$
	}
	
	public void test1a() throws Exception{
		String text= "fred\n\"x\""; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		assertEquals("non empty", false, l.length == 0); //$NON-NLS-1$
		assertEquals("1 line", 1,l.length); //$NON-NLS-1$
	}
	
	public void test2() throws Exception{
//		String text= "fred\"x\" \"xx\"";
//		NLSLine[] l= NLSScanner.scan(text);
//		assertEquals("non empty", false, l.isEmpty());
//		assertEquals("2 line", 2,l.length);
		printDisabledMessage("Scanner does not handle strings in the first line"); //$NON-NLS-1$
	}
	
	public void test2a() throws Exception{
		String text= "fred\n\"x\" \"xx\""; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		assertEquals("non empty", false, l.length == 0); //$NON-NLS-1$
		assertEquals("1 lines", 1,l.length); //$NON-NLS-1$
	}
	
	public void test3() throws Exception{
//		String text= "fred\"x\"\n \"xx\"";
//		NLSLine[] l= NLSScanner.scan(text);
//		assertEquals("non empty", false, l.length == 0);
//		assertEquals("2 lines", 2,l.length);
		printDisabledMessage("Scanner does not handle strings in the first line"); //$NON-NLS-1$
	}


	public void test4() throws Exception{
		String text= "fred\n \"xx\""; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		assertEquals("non empty", false, l.length == 0); //$NON-NLS-1$
		assertEquals("1 line", 1,l.length); //$NON-NLS-1$
	}
	
	public void test5() throws Exception{
		String text= "\n \"xx\""; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("1 string", 1, line.size()); //$NON-NLS-1$
	}	
	
	public void test6() throws Exception{
		String text= "\n \"xx\" \"dff\""; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("2 string", 2, line.size()); //$NON-NLS-1$
	}	
	
	public void test7() throws Exception{
		String text= "\n \"xx\" \n\"dff\""; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("1 string A", 1, line.size()); //$NON-NLS-1$
		
		line= l[1];
		assertEquals("1 string B", 1, line.size()); //$NON-NLS-1$
	}	


	public void test8() throws Exception{
		String text= "\n \"xx\" \n\"dff\" \"ccc\""; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("1 string A", 1, line.size()); //$NON-NLS-1$
		
		line= l[1];
		assertEquals("2 strings B", 2, line.size()); //$NON-NLS-1$
	}
	
	public void test9() throws Exception{
		String text= "fred\n \"xx\"" + NLSElement.createTagText(1) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		NLSElement el= line.get(0);
		assertEquals("has tag", true, el.hasTag()); //$NON-NLS-1$
	}


	public void test10() throws Exception{
		String text= "fred\n \"xx\"\n"; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		NLSElement el= line.get(0);
		assertEquals("has tag", false, el.hasTag()); //$NON-NLS-1$
	}
	
	public void test11() throws Exception{
		String text= 
				"\n\"x\" \"y\"" //$NON-NLS-1$
				+ NLSElement.createTagText(2) 
				+ NLSElement.createTagText(1) 
				+ "\n"; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("2 strings", 2, line.size()); //$NON-NLS-1$
		
		NLSElement el= line.get(0);
		assertEquals("0 has tag", true, el.hasTag()); //$NON-NLS-1$
		
		el= line.get(1);
		assertEquals("1 has tag", true, el.hasTag()); //$NON-NLS-1$
	}
	
	public void test12() throws Exception{
		String text= 
				"\n\"x\" \"y\"" //$NON-NLS-1$
				+ NLSElement.createTagText(1) 
				+ NLSElement.createTagText(2) 
				+ "\n"; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("2 strings", 2, line.size()); //$NON-NLS-1$
		
		NLSElement el= line.get(0);
		assertEquals("0 has tag", true, el.hasTag()); //$NON-NLS-1$
		
		el= line.get(1);
		assertEquals("1 has tag", true, el.hasTag()); //$NON-NLS-1$
	}
	
	public void test13() throws Exception{
		String text= 
				"\n\"x\" \"y\"" //$NON-NLS-1$
				+ NLSElement.createTagText(1) 
				+ "\n"; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("2 strings", 2, line.size()); //$NON-NLS-1$
		
		NLSElement el= line.get(0);
		assertEquals("0 has tag", true, el.hasTag()); //$NON-NLS-1$
		
		el= line.get(1);
		assertEquals("1 has no tag", false, el.hasTag()); //$NON-NLS-1$
	}
	
	public void test14() throws Exception{
		String text= 
				"\n\"x\" \"y\"" //$NON-NLS-1$
				+ NLSElement.createTagText(2) 
				+ "\n"; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("2 strings", 2, line.size()); //$NON-NLS-1$
		
		NLSElement el= line.get(0);
		assertEquals("0 has no tag", false, el.hasTag()); //$NON-NLS-1$
		
		el= line.get(1);
		assertEquals("1 has tag", true, el.hasTag()); //$NON-NLS-1$
	}

	//regression test for bug 12600
	public void test54() throws Exception{
		String text= 
				"\n\"x\"" //$NON-NLS-1$
				+ NLSElement.TAG_PREFIX + 1 
				+ "\n"; //$NON-NLS-1$
		NLSLine[] l= NLSScanner.scan(text);
		NLSLine line=l[0];
		assertEquals("1 strings", 1, line.size()); //$NON-NLS-1$
		
		NLSElement el= line.get(0);
		assertEquals("0 has no tag", false, el.hasTag()); //$NON-NLS-1$
		
	}
				
}


