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
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
  */
public class QuickFixTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(UnresolvedTypesQuickFixTest.allTests());
		suite.addTest(UnresolvedVariablesQuickFixTest.allTests());
		suite.addTest(UnresolvedMethodsQuickFixTest.allTests());
		suite.addTest(ReturnTypeQuickFixTest.allTests());
		suite.addTest(LocalCorrectionsQuickFixTest.allTests());
		suite.addTest(ReorgQuickFixTest.allTests());
		suite.addTest(ModifierCorrectionsQuickFixTest.allTests());
		suite.addTest(AssistQuickFixTest.allTests());
		suite.addTest(MarkerResolutionTest.allTests());
		suite.addTest(JavadocQuickFixTest.allTests());
		return new ProjectTestSetup(suite);
	}

	
	public QuickFixTest(String name) {
		super(name);
	}
	
	public static void assertCorrectLabels(List proposals) {
		for (int i= 0; i < proposals.size(); i++) {
			ICompletionProposal proposal= (ICompletionProposal) proposals.get(i);
			String name= proposal.getDisplayString();
			if (name == null || name.length() == 0 || name.charAt(0) == '!' || name.indexOf("{0}") != -1 || name.indexOf("{1}") != -1) {
				assertTrue("wrong proposal label: " + name, false);
			}
			if (proposal.getImage() == null) {
				assertTrue("wrong proposal image", false);
			}			
		}
	}
	
	public static void assertCorrectContext(IInvocationContext context, ProblemLocation problem) {
		if (problem.getProblemId() != 0) {
			if (!JavaCorrectionProcessor.hasCorrections(context.getCompilationUnit(), problem.getProblemId())) {
				assertTrue("Problem type not marked with lightbulb: " + problem, false);
			}
		}
	}	
	
	
	public static void assertNumberOf(String name, int nProblems, int nProblemsExpected) {
		assertTrue("Wrong number of " + name + ", is: " + nProblems + ", expected: " + nProblemsExpected, nProblems == nProblemsExpected);
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
		
	public static AssistContext getCorrectionContext(ICompilationUnit cu, int offset, int length) {
		AssistContext context= new AssistContext(cu, offset, length);
		return context;
	}


	protected final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot) {
		return collectCorrections(cu, astRoot, 1);
	}


	protected final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems) {
		IProblem[] problems= astRoot.getProblems();
		if (problems.length != nProblems) {
			StringBuffer buf= new StringBuffer("Wrong number of problems, is: ");
			buf.append(problems.length).append(", expected: ").append(nProblems).append('\n');
			for (int i= 0; i < problems.length; i++) {
				buf.append(problems[i]);
				buf.append('[').append(problems[i].getSourceStart()).append(" ,").append(problems[i].getSourceEnd()).append(']');
				buf.append('\n');
			}
			assertTrue(buf.toString(), false);

		}
		return collectCorrections(cu, problems[0]);
	}
	
	protected final ArrayList collectCorrections2(ICompilationUnit cu, int nProblems) throws JavaModelException {
		
		final ArrayList problemsList= new ArrayList();
		IProblemRequestor requestor= new IProblemRequestor() {
			public void acceptProblem(IProblem problem) {
				problemsList.add(problem);
			}
			public void beginReporting() {
				problemsList.clear();
			}
			public void endReporting() {}
			public boolean isActive() {	return true;}
		};
		
		ICompilationUnit wc= cu.getWorkingCopy(new WorkingCopyOwner() {}, requestor, null);
		try {
			wc.reconcile(true, null);
		} finally {
			wc.discardWorkingCopy();
		}
		
		IProblem[] problems= (IProblem[]) problemsList.toArray(new IProblem[problemsList.size()]);
		if (problems.length != nProblems) {
			StringBuffer buf= new StringBuffer("Wrong number of problems, is: ");
			buf.append(problems.length).append(", expected: ").append(nProblems).append('\n');
			for (int i= 0; i < problems.length; i++) {
				buf.append(problems[i]);
				buf.append('[').append(problems[i].getSourceStart()).append(" ,").append(problems[i].getSourceEnd()).append(']');
				buf.append('\n');
			}
			assertTrue(buf.toString(), false);

		}
		return collectCorrections(cu, problems[0]);
	}
		
	protected final ArrayList collectCorrections(ICompilationUnit cu, IProblem curr) {
			
		int offset= curr.getSourceStart();
		int length= curr.getSourceEnd() + 1 - offset;
		
		ProblemLocation problem= new ProblemLocation(offset, length, curr.getID(), curr.getArguments());
		AssistContext context= new AssistContext(cu, offset, length);
		assertCorrectContext(context, problem);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  new ProblemLocation[] { problem }, proposals);
		return proposals;
	}


	protected final ArrayList collectAssists(IInvocationContext context, boolean includeLinkedRename) {
		ArrayList proposals= new ArrayList();
		JavaCorrectionProcessor.collectAssists(context, null, proposals);
		if (!includeLinkedRename) {
			for (Iterator iter= proposals.iterator(); iter.hasNext(); ) {
				if (iter.next() instanceof LinkedNamesAssistProposal) {
					iter.remove();
				}
			}
		}
		return proposals;
	}	
	
	
}
