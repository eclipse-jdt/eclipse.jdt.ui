package org.eclipse.jdt.ui.tests.quickfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
  */
public class QuickFixTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new TestSuite(UnresolvedTypesQuickFixTest.class));
		suite.addTest(new TestSuite(UnresolvedVariablesQuickFixTest.class));
		suite.addTest(new TestSuite(UnresolvedMethodsQuickFixTest.class));
		suite.addTest(new TestSuite(ReturnTypeQuickFixTest.class));
		suite.addTest(new TestSuite(LocalCorrectionsQuickFixTest.class));
		suite.addTest(new TestSuite(UnresolvedMethodsQuickFixTest.class));
		suite.addTest(new TestSuite(ReorgQuickFixTest.class));
		suite.addTest(new TestSuite(MarkerResolutionTest.class));
		return suite;
	}

	
	public QuickFixTest(String name) {
		super(name);
	}
	
	public static void assertCorrectLabels(List proposals) {
		for (int i= 0; i < proposals.size(); i++) {
			ICompletionProposal proposal= (ICompletionProposal) proposals.get(i);
			String name= proposal.getDisplayString();
			if (name == null || name.length() == 0 || name.charAt(0) == '!' || name.indexOf('{') != -1) {
				assertTrue("wrong proposal label: " + name, false);
			}
			if (proposal.getImage() == null) {
				assertTrue("wrong proposal image", false);
			}			
		}
	}
	
	
	public static void assertNumberOf(String name, int nProblems, int nProblemsExpected) {
		assertTrue("Wrong number of " + name + ", is: " + nProblems + ", expected: " + nProblemsExpected, nProblems == nProblemsExpected);
	}
	
	private static final int printRange= 6;
	
	public static void assertEqualString(String str1, String str2) {	
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
	
	public static TypeDeclaration findTypeDeclaration(CompilationUnit astRoot, String simpleTypeName) {
		List types= astRoot.types();
		for (int i= 0; i < types.size(); i++) {
			TypeDeclaration elem= (TypeDeclaration) types.get(i);
			if (simpleTypeName.equals(elem.getName().getIdentifier())) {
				return elem;
			}
		}
		return null;
	}
	
	public static MethodDeclaration findMethodDeclaration(TypeDeclaration typeDecl, String methodName) {
		MethodDeclaration[] methods= typeDecl.getMethods();
		for (int i= 0; i < methods.length; i++) {
			if (methodName.equals(methods[i].getName().getIdentifier())) {
				return methods[i];
			}
		}
		return null;
	}	
	
}
