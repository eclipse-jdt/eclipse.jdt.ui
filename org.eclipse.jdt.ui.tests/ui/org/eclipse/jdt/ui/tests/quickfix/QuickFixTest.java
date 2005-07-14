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
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.NewCUCompletionUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

/**
  */
public class QuickFixTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(SerialVersionQuickFixTest.allTests());
		suite.addTest(UtilitiesTest.allTests());
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
		suite.addTest(ConvertIterableLoopQuickFixTest.allTests());
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
	
	
	public static void assertExpectedExistInProposals(List actualProposals, String[] expecteds) throws CoreException, BadLocationException {
		ArrayList actuals= new ArrayList(actualProposals.size());
		for (int i= 0; i < actualProposals.size(); i++) {
			Object curr= actualProposals.get(i);
			if (curr instanceof CUCorrectionProposal) {
				actuals.add(getPreviewContent((CUCorrectionProposal) curr));
			} else if (curr instanceof NewCUCompletionUsingWizardProposal) {
				actuals.add(getWizardPreviewContent((NewCUCompletionUsingWizardProposal) curr));
			}
		}
		StringAsserts.assertExpectedExistInProposals((String[]) actuals.toArray(new String[actuals.size()]), expecteds);
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


	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot) throws CoreException {
		return collectCorrections(cu, astRoot, 1, null);
	}
	
	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems) throws CoreException {
		return collectCorrections(cu, astRoot, nProblems, null);
	}


	protected static final ArrayList collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems, AssistContext context) throws CoreException {
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
		return collectCorrections(cu, problems[0], context);
	}
	
	protected static final ArrayList collectCorrections2(ICompilationUnit cu, int nProblems) throws CoreException {
		
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
		return collectCorrections(cu, problems[0], null);
	}
	
	protected static final ArrayList collectCorrections(ICompilationUnit cu, IProblem curr, IInvocationContext context) throws CoreException {
		int offset= curr.getSourceStart();
		int length= curr.getSourceEnd() + 1 - offset;
		if (context == null) {
			context= new AssistContext(cu, offset, length);
		}
		
		ProblemLocation problem= new ProblemLocation(offset, length, curr.getID(), curr.getArguments(), true);
		ArrayList proposals= collectCorrections(context, problem);
		if (!proposals.isEmpty()) {
			assertCorrectContext(context, problem);
		}
		
		return proposals;
	}
	
	protected static ArrayList collectCorrections(IInvocationContext context, IProblemLocation problem) throws CoreException {
		ArrayList proposals= new ArrayList();
		IStatus status= JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { problem }, proposals);
		assertStatusOk(status);
		return proposals;
	}
	
	public static void assertStatusOk(IStatus status) throws CoreException {
		if (!status.isOK()) {
			if (status.getException() == null) {  // find a status with an exception
				IStatus[] children= status.getChildren();
				for (int i= 0; i < children.length; i++) {
					IStatus child= children[i];
					if (child.getException() != null) {
						throw new CoreException(child);
					}
				}
			}
		}
	}
	
	
	protected static final ArrayList collectAssists(IInvocationContext context, Class[] filteredTypes) throws CoreException {
		ArrayList proposals= new ArrayList();
		IStatus status= JavaCorrectionProcessor.collectAssists(context, new IProblemLocation[0], proposals);
		assertStatusOk(status);
		
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
	
	protected static final ArrayList collectAssists(IInvocationContext context, boolean includeLinkedRename) throws CoreException {
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
	
	protected static String getWizardPreviewContent(NewCUCompletionUsingWizardProposal newCUWizard) throws CoreException, BadLocationException {
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		IType createdType= newCUWizard.getCreatedType();
		assertTrue("Nothing created", createdType.exists());
		String preview= createdType.getCompilationUnit().getSource();
		
		IJavaElement parent= createdType.getParent();
		if (parent instanceof IType) {
			createdType.delete(true, null);
		} else {
			JavaProjectHelper.delete(parent);
		}
		StringBuffer res= new StringBuffer();
		IDocument doc= new Document(preview);
		int nLines= doc.getNumberOfLines();
		for (int i= 0; i < nLines; i++) {
			IRegion lineInformation= doc.getLineInformation(i);
			res.append(doc.get(lineInformation.getOffset(), lineInformation.getLength()));
			if (i != nLines - 1) {
				res.append('\n');
			}
		}
		return res.toString();
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
