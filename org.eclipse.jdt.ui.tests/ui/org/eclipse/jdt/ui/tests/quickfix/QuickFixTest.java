package org.eclipse.jdt.ui.tests.quickfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

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
		suite.addTest(new TestSuite(ReorgQuickFixTest.class));
		suite.addTest(new TestSuite(ModifierCorrectionsQuickFixTest.class));
		suite.addTest(new TestSuite(AssistQuickFixTest.class));
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
	
	public static void assertCorrectContext(CorrectionContext context) {
		if (context.getProblemId() != 0) {
			assertTrue("Problem type not marked with lightbulb", JavaCorrectionProcessor.hasCorrections(context.getProblemId()));
		}
	}	
	
	
	public static void assertNumberOf(String name, int nProblems, int nProblemsExpected) {
		assertTrue("Wrong number of " + name + ", is: " + nProblems + ", expected: " + nProblemsExpected, nProblems == nProblemsExpected);
	}
	
	
	public static void assertEqualStringsIgnoreOrder(String[] str1, String[] str2) {
		int nUnmatched= 0;
		
		loop1: for (int i= 0; i < str1.length; i++) {
			String s1= str1[i];
			for (int k= 0; k < str2.length; k++) {
				String s2= str2[k];
				if (s2 != null && s2.equals(s1)) {
					str2[k]= null;
					str1[i]= null;
					continue loop1;
				}
			}
			nUnmatched++;
		}
		if (nUnmatched > 0) {
			if (nUnmatched == 1) {
				for (int i= 0; i < str1.length; i++) {
					if (str1[i] != null) {
						for (int k= 0; k < str2.length; k++) {
							if (str2[k] != null) {
								assertEqualString(str1[i], str2[k]);
							}
						}
					}
				}
			}
			
			StringBuffer buf= new StringBuffer();
			buf.append("Content not as expected: Content is: \n");
			for (int i= 0; i < str1.length; i++) {
				String s1= str1[i];
				if (s1 != null) {
					buf.append(s1);
					buf.append("\n");
				}
			}
			buf.append("Expected contents: \n");
			for (int i= 0; i < str2.length; i++) {
				String s2= str2[i];
				if (s2 != null) {
					buf.append(s2);
					buf.append("\n");
				}
			}
			assertTrue(buf.toString(), false);
		}				
	}
	
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
	
	private static final int printRange= 6;
	
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
	
	public static VariableDeclarationFragment findFieldDeclaration(TypeDeclaration typeDecl, String fieldName) {
		FieldDeclaration[] fields= typeDecl.getFields();
		for (int i= 0; i < fields.length; i++) {
			List list= fields[i].fragments();
			for (int k= 0; k < list.size(); k++) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment) list.get(k);
				if (fieldName.equals(fragment.getName().getIdentifier())) {
					return fragment;
				}				
			}
		}
		return null;
	}	
	
	public static CorrectionContext getCorrectionContext(ICompilationUnit cu, IProblem problem) {
		CorrectionContext context= new CorrectionContext(cu);
		context.initialize(problem.getSourceStart(), problem.getSourceEnd() - problem.getSourceStart() + 1, problem.getID(), problem.getArguments());
		return context;
	}
	
	public static CorrectionContext getCorrectionContext(ICompilationUnit cu, int offset, int length) {
		CorrectionContext context= new CorrectionContext(cu);
		context.initialize(offset, length, 0, null);
		return context;
	}	
	
	
}
