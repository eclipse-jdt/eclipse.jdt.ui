/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
  */
public class QuickFixTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(SerialVersionQuickFixTest.allTests());
		suite.addTest(UnresolvedTypesQuickFixTest.allTests());
		suite.addTest(UnresolvedVariablesQuickFixTest.allTests());
		suite.addTest(UnresolvedMethodsQuickFixTest.allTests());
		suite.addTest(ReturnTypeQuickFixTest.allTests());
		suite.addTest(LocalCorrectionsQuickFixTest.allTests());
		suite.addTest(TypeMismatchQuickFixTests.allTests());
		suite.addTest(ReorgQuickFixTest.allTests());
		suite.addTest(ModifierCorrectionsQuickFixTest.allTests());
		suite.addTest(AssistQuickFixTest.allTests());
		suite.addTest(MarkerResolutionTest.allTests());
		suite.addTest(JavadocQuickFixTest.allTests());
		suite.addTest(ConvertForLoopQuickFixTest.allTests());
		suite.addTest(AdvancedQuickAssistTest.allTests());
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
				assertTrue("Problem type not marked with light bulb: " + problem, false);
			}
		}
	}	
	
	
	public static void assertNumberOf(String name, int nProblems, int nProblemsExpected) {
		assertTrue("Wrong number of " + name + ", is: " + nProblems + ", expected: " + nProblemsExpected, nProblems == nProblemsExpected);
	}
	
	
	public static void assertEqualString(String actual, String expected) {	
		StringAsserts.assertEqualString(actual, expected);
	}
	
	public static void assertEqualStringIgnoreDelim(String actual, String expected) throws IOException {
		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}	
	
	public static void assertEqualStringsIgnoreOrder(String[] actuals, String[] expecteds) {
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expecteds);			
	}
	
	public static void assertExpectedExistInProposals(List actualProposals, String[] expecteds) throws CoreException {
		String[] actuals= new String[actualProposals.size()];
		for (int i= 0; i < actualProposals.size(); i++) {
			actuals[i]= getPreviewContent((CUCorrectionProposal) actualProposals.get(i));
		}
		StringAsserts.assertExpectedExistInProposals(actuals, expecteds);
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


	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot) {
		return collectCorrections(cu, astRoot, 1);
	}


	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems) {
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
	
	protected static final ArrayList collectCorrections2(ICompilationUnit cu, int nProblems) throws JavaModelException {
		
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
			wc.reconcile(ICompilationUnit.NO_AST, true, wc.getOwner(), null);
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
		
	protected static final ArrayList collectCorrections(ICompilationUnit cu, IProblem curr) {
			
		int offset= curr.getSourceStart();
		int length= curr.getSourceEnd() + 1 - offset;
		
		ProblemLocation problem= new ProblemLocation(offset, length, curr.getID(), curr.getArguments(), true);
		AssistContext context= new AssistContext(cu, offset, length);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  new ProblemLocation[] { problem }, proposals);
		if (!proposals.isEmpty()) {
			assertCorrectContext(context, problem);
		}
		
		return proposals;
	}


	protected static final ArrayList collectAssists(IInvocationContext context, Class[] filteredTypes) {
		ArrayList proposals= new ArrayList();
		JavaCorrectionProcessor.collectAssists(context, null, proposals);
		
		if (!proposals.isEmpty()) {
			assertTrue("should be marked as 'has assist'", JavaCorrectionProcessor.hasAssists(context));
		}
		
		
		if (filteredTypes != null && filteredTypes.length > 0) {
			for (Iterator iter= proposals.iterator(); iter.hasNext(); ) {
				if (isFiltered(iter.next(), filteredTypes)) {
					iter.remove();
				}
			}
		}
		return proposals;
	}
	
	private static boolean isFiltered(Object curr, Class[] filteredTypes) {
		for (int k = 0; k < filteredTypes.length; k++) {
			if (filteredTypes[k].isInstance(curr)) {
				return true;
			}
		}
		return false;
	}
	
	protected static final ArrayList collectAssists(IInvocationContext context, boolean includeLinkedRename) {
		Class[] filteredTypes= includeLinkedRename ? null : new Class[] { LinkedNamesAssistProposal.class };
		return collectAssists(context, filteredTypes);
	}
	
	protected static CompilationUnit getASTRoot(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}
	
	protected static String getPreviewContent(CUCorrectionProposal proposal) throws CoreException {
		return proposal.getPreviewContent();
	}
	
	protected static void assertNumberOfProposals(List proposals, int expectedProposals) {
		if (proposals.size() != expectedProposals) {
			StringBuffer buf= new StringBuffer();
			buf.append("Wrong number of proposals, is: ").append(proposals.size()). append(", expected: ").append(expectedProposals).append('\n');
			for (int i= 0; i < proposals.size(); i++) {
				ICompletionProposal curr= (ICompletionProposal) proposals.get(i);
				buf.append(" - ").append(curr.getDisplayString()).append('\n');
				if (curr instanceof CUCorrectionProposal) {
					appendSource(((CUCorrectionProposal) curr), buf);
				}
			}
			assertTrue(buf.toString(), false);
		}
	}
	
	private static void appendSource(CUCorrectionProposal proposal, StringBuffer buf) {
		
	}
	
	protected static void assertNoErrors(IInvocationContext context) {
		IProblem[] problems= context.getASTRoot().getProblems();
		for (int i= 0; i < problems.length; i++) {
			if (problems[i].isError()) {
				assertTrue("source has error: " + problems[i].getMessage(), false);
			}
		}
	}
	
}
