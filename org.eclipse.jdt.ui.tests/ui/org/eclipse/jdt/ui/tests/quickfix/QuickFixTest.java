/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jens Reimann - add method to convert a collection of previews
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalPositionGroup.Proposal;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.GetterSetterCorrectionSubProcessor.SelfEncapsulateFieldProposal;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.ReorgCorrectionsSubProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewCUUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameRefactoringProposal;
import org.eclipse.jdt.internal.ui.text.template.contentassist.SurroundWithTemplateProposal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 */
public class QuickFixTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite(QuickFixTest.class.getName());
		suite.addTest(QuickFixTest9.suite());
		suite.addTest(QuickFixTest18.suite());
		suite.addTest(QuickFixTest12.suite());
		suite.addTest(SerialVersionQuickFixTest.suite());
		suite.addTest(UtilitiesTest.suite());
		suite.addTest(UnresolvedTypesQuickFixTest.suite());
		suite.addTest(UnresolvedVariablesQuickFixTest.suite());
		suite.addTest(UnresolvedMethodsQuickFixTest.suite());
		suite.addTest(UnresolvedMethodsQuickFixTest18.suite());
		suite.addTest(ReturnTypeQuickFixTest.suite());
		suite.addTest(LocalCorrectionsQuickFixTest.suite());
		suite.addTest(LocalCorrectionsQuickFixTest17.suite());
		suite.addTest(LocalCorrectionsQuickFixTest18.suite());
		suite.addTest(TypeMismatchQuickFixTests.suite());
		suite.addTest(ReorgQuickFixTest.suite());
		suite.addTest(ModifierCorrectionsQuickFixTest.suite());
		suite.addTest(ModifierCorrectionsQuickFixTest17.suite());
		suite.addTest(ModifierCorrectionsQuickFixTest9.suite());
		suite.addTest(GetterSetterQuickFixTest.suite());
		suite.addTest(AssistQuickFixTest.suite());
		suite.addTest(AssistQuickFixTest17.suite());
		suite.addTest(AssistQuickFixTest18.suite());
		suite.addTest(AssistQuickFixTest12.suite());
		suite.addTest(ChangeNonStaticToStaticTest.suite());
		suite.addTest(MarkerResolutionTest.suite());
		suite.addTest(JavadocQuickFixTest.suite());
		suite.addTest(ConvertForLoopQuickFixTest.suite());
		suite.addTest(ConvertIterableLoopQuickFixTest.suite());
		suite.addTest(AdvancedQuickAssistTest.suite());
		suite.addTest(AdvancedQuickAssistTest17.suite());
		suite.addTest(AdvancedQuickAssistTest18.suite());
		suite.addTest(CleanUpTestCase.suite());
		suite.addTest(QuickFixEnablementTest.suite());
		suite.addTest(SurroundWithTemplateTest.suite());
		suite.addTest(TypeParameterMismatchTest.suite());
		suite.addTest(PropertiesFileQuickAssistTest.suite());
		suite.addTest(NullAnnotationsQuickFixTest.suite());
		suite.addTest(NullAnnotationsQuickFixTest18.suite());
		suite.addTest(NullAnnotationsQuickFixTest18Mix.suite());
		suite.addTest(AnnotateAssistTest15.suite());
		suite.addTest(AnnotateAssistTest18.suite());
		suite.addTest(TypeAnnotationQuickFixTest.suite());

		return new ProjectTestSetup(suite);
	}


	public QuickFixTest(String name) {
		super(name);
	}

	public static void assertCorrectLabels(List<? extends ICompletionProposal> proposals) {
		for (int i= 0; i < proposals.size(); i++) {
			ICompletionProposal proposal= proposals.get(i);
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
			if (!JavaCorrectionProcessor.hasCorrections(context.getCompilationUnit(), problem.getProblemId(), problem.getMarkerType())) {
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

	public static void assertEqualStringsIgnoreOrder(Collection<String> actuals, Collection<String> expecteds) {
		String[] act= actuals.toArray(new String[actuals.size()]);
		String[] exp= expecteds.toArray(new String[actuals.size()]);
		StringAsserts.assertEqualStringsIgnoreOrder(act, exp);
	}

	public static void assertExpectedExistInProposals(List<IJavaCompletionProposal> actualProposals, String[] expecteds) throws CoreException, BadLocationException {
		StringAsserts.assertExpectedExistInProposals(getPreviewContents(actualProposals), expecteds);
	}

	protected static void assertProposalPreviewEquals(String expected, String proposalName, List<IJavaCompletionProposal> proposals) throws CoreException, BadLocationException {
		final ICompletionProposal proposal= findProposalByName(proposalName, proposals);
		assertNotNull("proposal \""+proposalName+"\" not found", proposal);
		assertEquals(expected, getProposalPreviewContent(proposal));
	}

	public static void assertCommandIdDoesNotExist(List<? extends ICompletionProposal> actualProposals, String commandId) {
		assertTrue(findProposalByCommandId(commandId, actualProposals) == null);
	}

	public static void assertProposalDoesNotExist(List<? extends ICompletionProposal> actualProposals, String proposalName) {
		assertTrue(findProposalByName(proposalName, actualProposals) == null);
	}

	public static void assertProposalExists(List<? extends ICompletionProposal> actualProposals, String proposalName) {
		assertTrue(findProposalByName(proposalName, actualProposals) != null);
	}

	public static TypeDeclaration findTypeDeclaration(CompilationUnit astRoot, String simpleTypeName) {
		List<AbstractTypeDeclaration> types= astRoot.types();
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
			List<VariableDeclarationFragment> list= fields[i].fragments();
			for (int k= 0; k < list.size(); k++) {
				VariableDeclarationFragment fragment= list.get(k);
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


	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot) throws CoreException {
		return collectCorrections(cu, astRoot, 1, null);
	}

	/**
	 * Bad design: only collects corrections for the <b>first</b> problem!
	 * @param cu
	 * @param astRoot
	 * @param nProblems
	 * @return
	 * @throws CoreException
	 */
	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems) throws CoreException {
		return collectCorrections(cu, astRoot, nProblems, null);
	}

	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems, int problem) throws CoreException {
		return collectCorrections(cu, astRoot, nProblems, problem, null);
	}

	/**
	 * Bad design: only collects corrections for the <b>first</b> problem!
	 * @param cu
	 * @param astRoot
	 * @param nProblems
	 * @param context
	 * @return
	 * @throws CoreException
	 */
	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems, AssistContext context) throws CoreException {
		return collectCorrections(cu, astRoot, nProblems, 0, context);
	}

	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems, int problem, AssistContext context) throws CoreException {
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(nProblems, problems);

		return collectCorrections(cu, problems[problem], context);
	}

	protected static final ArrayList<ICompletionProposal> collectAllCorrections(ICompilationUnit cu, CompilationUnit astRoot, int nProblems) throws CoreException {
		IProblem[] problems= astRoot.getProblems();
		assertNumberOfProblems(nProblems, problems);
		
		ArrayList<ICompletionProposal> corrections= new ArrayList<>();
		for (int i= 0; i < nProblems; i++) {
			corrections.addAll(collectCorrections(cu, problems[i], null));
		}
		return corrections;
	}


	protected static void assertNumberOfProblems(int nProblems, IProblem[] problems) {
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
	}

	/**
	 * Bad design: only collects corrections for the <b>first</b> problem!
	 * @param cu
	 * @param nProblems
	 * @return
	 * @throws CoreException
	 */
	protected static final ArrayList<IJavaCompletionProposal> collectCorrections2(ICompilationUnit cu, int nProblems) throws CoreException {

		final ArrayList<IProblem> problemsList= new ArrayList<>();
		final IProblemRequestor requestor= new IProblemRequestor() {
			@Override
			public void acceptProblem(IProblem problem) {
				problemsList.add(problem);
			}
			@Override
			public void beginReporting() {
				problemsList.clear();
			}
			@Override
			public void endReporting() {}
			@Override
			public boolean isActive() {	return true;}
		};

		WorkingCopyOwner workingCopyOwner= new WorkingCopyOwner() {
			@Override
			public IProblemRequestor getProblemRequestor(ICompilationUnit workingCopy) {
				return requestor;
			}
		};
		ICompilationUnit wc= cu.getWorkingCopy(workingCopyOwner, null);
		try {
			wc.reconcile(ICompilationUnit.NO_AST, true, true, wc.getOwner(), null);
		} finally {
			wc.discardWorkingCopy();
		}

		IProblem[] problems= problemsList.toArray(new IProblem[problemsList.size()]);
		assertNumberOfProblems(nProblems, problems);

		return collectCorrections(cu, problems[0], null);
	}

	protected static final ArrayList<IJavaCompletionProposal> collectCorrections(ICompilationUnit cu, IProblem curr, IInvocationContext context) throws CoreException {
		int offset= curr.getSourceStart();
		int length= curr.getSourceEnd() + 1 - offset;
		if (context == null) {
			context= new AssistContext(cu, offset, length);
		}

		ProblemLocation problem= new ProblemLocation(curr);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);
		if (!proposals.isEmpty()) {
			assertCorrectContext(context, problem);
		}

		return proposals;
	}

	protected static ArrayList<IJavaCompletionProposal> collectCorrections(IInvocationContext context, IProblemLocation problem) throws CoreException {
		ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
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


	protected static final ArrayList<IJavaCompletionProposal> collectAssists(IInvocationContext context, Class<?>[] filteredTypes) throws CoreException {
		ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
		IStatus status= JavaCorrectionProcessor.collectAssists(context, new IProblemLocation[0], proposals);
		assertStatusOk(status);

		if (!proposals.isEmpty()) {
			assertTrue("should be marked as 'has assist'", JavaCorrectionProcessor.hasAssists(context));
		}


		if (filteredTypes != null && filteredTypes.length > 0) {
			for (Iterator<IJavaCompletionProposal> iter= proposals.iterator(); iter.hasNext(); ) {
				if (isFiltered(iter.next(), filteredTypes)) {
					iter.remove();
				}
			}
		}
		return proposals;
	}

	private static boolean isFiltered(Object curr, Class<?>[] filteredTypes) {
		for (int k = 0; k < filteredTypes.length; k++) {
			if (filteredTypes[k].isInstance(curr)) {
				return true;
			}
		}
		return false;
	}

	protected static final ArrayList<IJavaCompletionProposal> collectAssists(IInvocationContext context, boolean includeLinkedRename) throws CoreException {
		Class<?>[] filteredTypes= includeLinkedRename ? null : new Class[] { LinkedNamesAssistProposal.class, RenameRefactoringProposal.class };
		return collectAssists(context, filteredTypes);
	}

	protected static CompilationUnit getASTRoot(ICompilationUnit cu) {
		return ASTResolving.createQuickFixAST(cu, null);
	}

	public static void addPreviewAndExpected(List<IJavaCompletionProposal> proposals, StringBuffer expected, ArrayList<String> expecteds, ArrayList<String> previews) throws CoreException {
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(expecteds.size());
		previews.add(getPreviewContent(proposal));
		expecteds.add(expected.toString());
	}


	protected static String[] getPreviewContents(List<IJavaCompletionProposal> proposals) throws CoreException, BadLocationException {
		String[] res= new String[proposals.size()];
		for (int i= 0; i < proposals.size(); i++) {
			res[i]=getProposalPreviewContent(proposals.get(i));
		}
		return res;
	}


	private static String getProposalPreviewContent(ICompletionProposal curr) throws CoreException, BadLocationException {
		String previewContent = null;
		if (curr instanceof ReorgCorrectionsSubProcessor.ClasspathFixCorrectionProposal) {
			// ignore
		} else if (curr instanceof CUCorrectionProposal) {
			previewContent= getPreviewContent((CUCorrectionProposal) curr);
		} else if (curr instanceof NewCUUsingWizardProposal) {
			previewContent= getWizardPreviewContent((NewCUUsingWizardProposal) curr);
		} else if (curr instanceof SurroundWithTemplateProposal) {
			previewContent= getTemplatePreviewContent((SurroundWithTemplateProposal) curr);
		} else if (curr instanceof SelfEncapsulateFieldProposal) {
			previewContent= getSEFPreviewContent((SelfEncapsulateFieldProposal) curr);
		}
		return previewContent;
	}
	
	private static String getSEFPreviewContent(SelfEncapsulateFieldProposal sefp) throws CoreException {
		ICompilationUnit compilationUnit= sefp.getField().getCompilationUnit();
		TextFileChange change= sefp.getChange((IFile) compilationUnit.getResource());
		if (change != null) {
			return change.getPreviewContent(null);
		}
		return "";
	}


	private static String getTemplatePreviewContent(SurroundWithTemplateProposal proposal) {
		return proposal.getPreviewContent();
	}

	protected static String getPreviewContent(CUCorrectionProposal proposal) throws CoreException {
		return proposal.getPreviewContent();
	}

	protected static String[] getAllPreviewContent(Collection<? extends IJavaCompletionProposal> proposals) throws CoreException {
		ArrayList<String> result = new ArrayList<>(proposals.size());
		for ( IJavaCompletionProposal proposal: proposals) {
			if ( proposal instanceof CUCorrectionProposal) {
				result.add(getPreviewContent((CUCorrectionProposal) proposal));
			}
		}
		return result.toArray(new String[result.size()]);
	}

	protected static String[] getAllDisplayStrings(ArrayList<IJavaCompletionProposal> proposals) {
		return proposals.stream()
				.map(proposal -> proposal.getDisplayString())
				.filter(displayString -> displayString != null && !displayString.isEmpty())
				.toArray(String[]::new);
	}

	protected static String getWizardPreviewContent(NewCUUsingWizardProposal newCUWizard) throws CoreException, BadLocationException {
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

	protected static void assertNumberOfProposals(List<? extends ICompletionProposal> proposals, int expectedProposals) {
		if (proposals.size() != expectedProposals) {
			StringBuffer buf= new StringBuffer();
			buf.append("Wrong number of proposals, is: ").append(proposals.size()). append(", expected: ").append(expectedProposals).append('\n');
			for (int i= 0; i < proposals.size(); i++) {
				ICompletionProposal curr= proposals.get(i);
				buf.append(" - ").append(curr.getDisplayString()).append('\n');
				if (curr instanceof CUCorrectionProposal) {
					appendSource(((CUCorrectionProposal) curr), buf);
				}
			}
			assertTrue(buf.toString(), false);
		}
	}

	protected static ICommandAccess findProposalByCommandId(String commandId, List<? extends ICompletionProposal> proposals) {
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof ICommandAccess) {
				if (commandId.equals(((ICommandAccess) curr).getCommandId())) {
					return (ICommandAccess) curr;
				}
			}
		}
		return null;
	}

	protected static ICompletionProposal findProposalByName(String name, List<? extends ICompletionProposal> proposals) {
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof ICompletionProposal && name.equals(((ICompletionProposal)curr).getDisplayString()))
				return (ICompletionProposal)curr;
		}
		return null;
	}


	private static void appendSource(CUCorrectionProposal proposal, StringBuffer buf) {
		try {
			buf.append(proposal.getPreviewContent());
		} catch (CoreException e) {
			// ignore
		}
	}

	protected static void assertNoErrors(IInvocationContext context) {
		IProblem[] problems= context.getASTRoot().getProblems();
		for (int i= 0; i < problems.length; i++) {
			if (problems[i].isError()) {
				assertTrue("source has error: " + problems[i].getMessage(), false);
			}
		}
	}

	public static String getPreviewsInBufAppend(ICompilationUnit cu) throws CoreException, BadLocationException {
		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		if (proposals.isEmpty()) {
			return null;
		}
		return getPreviewsInBufAppend(cu, proposals);
	}


	protected static String getPreviewsInBufAppend(ICompilationUnit cu, List<IJavaCompletionProposal> proposals) throws CoreException, BadLocationException {
		StringBuffer buf= new StringBuffer();
		String[] previewContents= getPreviewContents(proposals);

		buf.append("public void testX() throws Exception {\n");
		buf.append("IPackageFragment pack1= fSourceFolder.createPackageFragment(\"").append(cu.getParent().getElementName()).append("\", false, null);\n");
		buf.append("StringBuffer buf= new StringBuffer();\n");
		wrapInBufAppend(cu.getBuffer().getContents(), buf);
		buf.append("ICompilationUnit cu= pack1.createCompilationUnit(\"").append(cu.getElementName()).append("\", buf.toString(), false, null);\n\n");
		buf.append("CompilationUnit astRoot= getASTRoot(cu);\n");
		buf.append("ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);\n\n");
		buf.append("assertCorrectLabels(proposals);\n");

		buf.append("assertNumberOfProposals(proposals, ").append(previewContents.length).append(");\n\n");
		buf.append("String[] expected= new String[").append(previewContents.length).append("];\n");

		for (int i= 0; i < previewContents.length; i++) {
			String curr= previewContents[i];
			if (curr == null) {
				continue;
			}

			buf.append("buf= new StringBuffer();\n");
			wrapInBufAppend(curr, buf);
			buf.append("expected[" + i + "]= buf.toString();\n\n");
		}

		buf.append("assertExpectedExistInProposals(proposals, expected);\n");
		buf.append("}\n");
		return buf.toString();
	}


	private static void wrapInBufAppend(String curr, StringBuffer buf) {
		buf.append("buf.append(\"");

		int last= curr.length() - 1;
		for (int k= 0; k <= last ; k++) {
			char ch= curr.charAt(k);
			if (ch == '\n') {
				buf.append("\\n\");\n");
				if (k < last) {
					buf.append("buf.append(\"");
				}
			} else if (ch == '\r') {
				// ignore
			} else if (ch == '\t') {
				buf.append("    "); // 4 spaces
			} else if (ch == '"' || ch == '\\') {
				buf.append('\\').append(ch);
			} else {
				buf.append(ch);
			}
		}
		if (buf.length() > 0 && buf.charAt(buf.length() - 1) != '\n') {
			buf.append("\\n\");\n");
		}
	}

	protected void assertLinkedChoices(ICompletionProposal proposal, String linkedGroup, String[] expectedChoices) {
		assertTrue("Not a LinkedCorrectionProposal", proposal instanceof LinkedCorrectionProposal);
		LinkedCorrectionProposal linkedProposal = (LinkedCorrectionProposal)proposal;

		LinkedProposalModel linkedProposalModel = linkedProposal.getLinkedProposalModel();
		LinkedProposalPositionGroup positionGroup = linkedProposalModel.getPositionGroup(linkedGroup, false);
		Proposal[] choices = positionGroup.getProposals();
		assertEquals("Not same number of choices", expectedChoices.length, choices.length);
		Arrays.sort(expectedChoices);
		List<String> sortedChoices= Arrays.stream(choices)
									.map(Proposal::getDisplayString)
									.sorted()
									.collect(Collectors.toList());
		for (int i=0; i<expectedChoices.length; i++) {
			assertEquals("Unexpected choice", expectedChoices[i], sortedChoices.get(i));
		}
	}
}
