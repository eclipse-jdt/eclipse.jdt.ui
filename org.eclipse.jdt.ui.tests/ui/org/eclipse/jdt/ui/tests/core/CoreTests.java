/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
  */
public class CoreTests extends TestCase {

	public static Test suite() {
		
		TestSuite suite= new TestSuite();
		suite.addTest(AddImportTest.allTests());
		suite.addTest(AddUnimplementedMethodsTest.allTests());
		suite.addTest(AddUnimplementedConstructorsTest.allTests());
		suite.addTest(AllTypesCacheTest.allTests());
		suite.addTest(BindingsNameTest.allTests());
		suite.addTest(ClassPathDetectorTest.allTests());
		suite.addTest(HierarchicalASTVisitorTest.allTests());
		suite.addTest(ImportOrganizeTest.allTests());
		suite.addTest(JavaModelUtilTest.allTests());
		suite.addTest(NameProposerTest.allTests());
		suite.addTest(TextEditTests.allTests());
		suite.addTest(PrimaryWorkingCopyTest.allTests());
		suite.addTest(TypeInfoTest.allTests());	
		suite.addTest(CallHierarchyTest.allTests());
		suite.addTest(ScopeAnalyzerTest.allTests());
		suite.addTest(PartialASTTest.allTests());
		suite.addTest(CodeCompletionTest.allTests());
		return new ProjectTestSetup(suite);
	}

	public CoreTests(String name) {
		super(name);
	}
	
	private static final int printRange= 6;

	private static int getDiffPos(String str1, String str2) {
		int len1= Math.min(str1.length(), str2.length());
		
		int diffPos= -1;
		for (int i= 0; i < len1; i++) {
			if (str1.charAt(i) != str2.charAt(i)) {
				diffPos= i;
				break;
			}
		}
		if (diffPos == -1 && str1.length() != str2.length()) {
			diffPos= len1;
		}
		return diffPos;
	}

	public static void assertEqualString(String current, String expected) {	
		int diffPos= getDiffPos(current, expected);
		if (diffPos != -1) {
			int diffAhead= Math.max(0, diffPos - printRange);
			int diffAfter= Math.min(current.length(), diffPos + printRange);
			
			String diffStr= current.substring(diffAhead, diffPos) + '^' + current.substring(diffPos, diffAfter);
			assertTrue("Content not as expected: is\n" + current + "\nDiffers at pos " + diffPos + ": " + diffStr + "\nexpected:\n" + expected, false);
		}
	}
	
	public static void assertEqualStringIgnoreDelim(String str1, String str2) throws IOException {
		BufferedReader read1= new BufferedReader(new StringReader(str1));
		BufferedReader read2= new BufferedReader(new StringReader(str2));
		
		int line= 1;
		do {
			String s1= read1.readLine();
			String s2= read2.readLine();
			
			if (s1 == null || !s1.equals(s2)) {
				if (s1 == null && s2 == null) {
					return;
				}
				String diffStr= (s1 == null) ? s2 : s1;
				assertTrue("Content not as expected: is\n" + str1 + "\nDiffers at line " + line + ": " + diffStr + "\nexpected:\n" + str2, false);
			}
			line++;
		} while (true);
	}	
	
	public static void assertEqualStringsIgnoreOrder(String[] str1, String[] str2) {
		ArrayList list1= new ArrayList(Arrays.asList(str1));
		ArrayList list2= new ArrayList(Arrays.asList(str2));
		
		for (int i= list1.size() - 1; i >= 0; i--) {
			if (list2.remove(list1.get(i))) {
				list1.remove(i);
			}
		}
		
		int n1= list1.size();
		int n2= list2.size();
		
		if (n1 + n2 > 0) {
			if (n1 == 1 && n2 == 1) {
				assertEqualString((String) list1.get(0), (String) list2.get(0));
			}
			
			StringBuffer buf= new StringBuffer();
			buf.append("Content not as expected: Content is: \n");
			for (int i= 0; i < n1; i++) {
				String s1= (String) list1.get(i);
				if (s1 != null) {
					buf.append(s1);
					buf.append("\n");
				}
			}
			buf.append("Expected contents: \n");
			for (int i= 0; i < n2; i++) {
				String s2= (String) list2.get(i);
				if (s2 != null) {
					buf.append(s2);
					buf.append("\n");
				}
			}
			assertTrue(buf.toString(), false);
		}				
	}
	
	public static void assertNumberOf(String name, int is, int expected) {
		assertTrue("Wrong number of " + name + ", is: " + is + ", expected: " + expected, is == expected);
	}
	
}
