package org.eclipse.jdt.ui.tests.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
  */
public class CoreTests extends TestCase {

	public static Test suite() {
		
		TestSuite suite= new TestSuite();
		suite.addTest(new TestSuite(AddImportTest.class));
		suite.addTest(new TestSuite(AddUnimplementedMethodsTest.class));
		//suite.addTest(new TestSuite(AllTypesCacheTest.suite.class));
		suite.addTest(new TestSuite(BindingsNameTest.class));
		suite.addTest(new TestSuite(ClassPathDetectorTest.class));
		suite.addTest(new TestSuite(HierarchicalASTVisitorTest.class));
		suite.addTest(new TestSuite(ImportOrganizeTest.class));
		suite.addTest(new TestSuite(JavaModelUtilTest.class));
		//suite.addTest(new TestSuite(NameProposerTest.class));
		suite.addTest(new TestSuite(TextBufferTest.class));
		suite.addTest(new TestSuite(TypeInfoTest.class));		
		return suite;
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

	public static void assertEqualString(String str1, String str2) {	
		int diffPos= getDiffPos(str1, str2);
		if (diffPos != -1) {
			int diffAhead= Math.max(0, diffPos - printRange);
			int diffAfter= Math.min(str1.length(), diffPos + printRange);
			
			String diffStr= str1.substring(diffAhead, diffPos) + '^' + str1.substring(diffPos, diffAfter);
			assertTrue("Content not as expected: is\n" + str1 + "\nDiffers at pos " + diffPos + ": " + diffStr + "\nexpected:\n" + str2, false);
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
	
	
}
