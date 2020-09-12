/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Sebastian Davids <sdavids@gmx.de> - bug 48696
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;

import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

public class JUnitQuickFixProcessor implements IQuickFixProcessor {

	private static final HashSet<String> ASSERT_METHOD_NAMES= new HashSet<>();

	public JUnitQuickFixProcessor() {
		ASSERT_METHOD_NAMES.add("fail"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("assertTrue"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("assertFalse"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("assertEquals"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("assertNotNull"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("assertNull"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("assertSame"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("assertNotSame"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("failNotEquals"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("failSame"); //$NON-NLS-1$
		ASSERT_METHOD_NAMES.add("failNotSame"); //$NON-NLS-1$
	}

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return problemId == IProblem.UndefinedType || problemId ==  IProblem.UndefinedMethod;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(final IInvocationContext context, IProblemLocation[] locations)  {
		ArrayList<IJavaCompletionProposal> res= null;
		for (IProblemLocation problem : locations) {
			int id= problem.getProblemId();
			if (IProblem.UndefinedType == id) {
				res= getAddJUnitToBuildPathProposals(context, problem, res);
			} else if (id == IProblem.UndefinedMethod) {
				String currentPreferenceValue= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
				if (!currentPreferenceValue.contains("org.junit.Assert.*")) { //$NON-NLS-1$
					res= getAddAssertImportProposals(context, problem, res);
				}
			}
		}
		if (res == null || res.isEmpty()) {
			return null;
		}
		return res.toArray(new IJavaCompletionProposal[res.size()]);
	}

	private ArrayList<IJavaCompletionProposal> getAddAssertImportProposals(IInvocationContext context, IProblemLocation problem, ArrayList<IJavaCompletionProposal> proposals) {
		String[] args= problem.getProblemArguments();
		if (args.length > 1) {
			String methodName= args[1];
			if (ASSERT_METHOD_NAMES.contains(methodName) && isInsideJUnit4Test(context)) {
				if (proposals == null) {
					proposals= new ArrayList<>();
				}
				proposals.add(new AddAssertProposal(context.getASTRoot(), methodName, 9));
				proposals.add(new AddAssertProposal(context.getASTRoot(), "*", 10)); //$NON-NLS-1$
			}
		}
		return proposals;
	}

	private ArrayList<IJavaCompletionProposal> getAddJUnitToBuildPathProposals(IInvocationContext context, IProblemLocation location, ArrayList<IJavaCompletionProposal> proposals) {
		try {
			ICompilationUnit unit= context.getCompilationUnit();
			String qualifiedName= null;
			String qualifiedName1= null;

			String s= unit.getBuffer().getText(location.getOffset(), location.getLength());
			switch (s) {
			case "TestCase": //$NON-NLS-1$
			case "TestSuite": //$NON-NLS-1$
				qualifiedName= "junit.framework." + s; //$NON-NLS-1$
				break;
			case "RunWith": //$NON-NLS-1$
				qualifiedName= "org.junit.runner.RunWith"; //$NON-NLS-1$
				break;
			case "Test": //$NON-NLS-1$
				ASTNode node= location.getCoveredNode(context.getASTRoot());
				if (node != null && node.getLocationInParent() == MarkerAnnotation.TYPE_NAME_PROPERTY) {
					qualifiedName= "org.junit.Test"; //$NON-NLS-1$
					qualifiedName1= "org.junit.jupiter.api.Test"; //$NON-NLS-1$
				} else {
					qualifiedName= "junit.framework.Test"; //$NON-NLS-1$
				}
				break;
			case "TestFactory": //$NON-NLS-1$
				qualifiedName= "org.junit.jupiter.api.TestFactory"; //$NON-NLS-1$
				break;
			case "Testable": //$NON-NLS-1$
				qualifiedName= "org.junit.platform.commons.annotation.Testable"; //$NON-NLS-1$
				break;
			case "TestTemplate": //$NON-NLS-1$
				qualifiedName= "org.junit.jupiter.api.TestTemplate"; //$NON-NLS-1$
				break;
			case "ParameterizedTest": //$NON-NLS-1$
				qualifiedName= "org.junit.jupiter.params.ParameterizedTest"; //$NON-NLS-1$
				break;
			case "RepeatedTest": //$NON-NLS-1$
				qualifiedName= "org.junit.jupiter.api.RepeatedTest"; //$NON-NLS-1$
				break;
			default:
				break;
			}
			IJavaProject javaProject= unit.getJavaProject();
			if (!foundInProjectClasspath(javaProject, qualifiedName) || !foundInProjectClasspath(javaProject, qualifiedName1)) {
				if (qualifiedName != null) {
					proposals= addProposal(context, proposals, qualifiedName, javaProject);
				}
				if (qualifiedName1 != null) {
					proposals= addProposal(context, proposals, qualifiedName1, javaProject);
				}
			} else {
				return proposals;
			}
		} catch (JavaModelException e) {
		    JUnitPlugin.log(e.getStatus());
		}
		return proposals;
	}

	private ArrayList<IJavaCompletionProposal> addProposal(IInvocationContext context, ArrayList<IJavaCompletionProposal> proposals, String qualifiedName, IJavaProject javaProject) {
		ClasspathFixProposal[] fixProposals= ClasspathFixProcessor.getContributedFixImportProposals(javaProject, qualifiedName, null);
		for (ClasspathFixProposal fixProposal : fixProposals) {
			if (proposals == null)
				proposals= new ArrayList<>();
			proposals.add(new JUnitClasspathFixCorrectionProposal(javaProject, fixProposal, getImportRewrite(context.getASTRoot(), qualifiedName)));
		}
		return proposals;
	}

	private boolean foundInProjectClasspath(IJavaProject javaProject, String qualifiedName) throws JavaModelException {
		return qualifiedName != null && javaProject.findType(qualifiedName) != null;
	}

	private ImportRewrite getImportRewrite(CompilationUnit astRoot, String typeToImport) {
		if (typeToImport != null) {
			ImportRewrite importRewrite= CodeStyleConfiguration.createImportRewrite(astRoot, true);
			importRewrite.addImport(typeToImport);
			return importRewrite;
		}
		return null;
	}

	private boolean isInsideJUnit4Test(IInvocationContext context) {
		if (!JUnitStubUtility.is50OrHigher(context.getCompilationUnit().getJavaProject())) {
			return false;
		}

		ASTNode node= context.getCoveringNode();
		while (node != null && !(node instanceof BodyDeclaration)) {
			node= node.getParent();
		}
		if (node instanceof MethodDeclaration) {
			IMethodBinding binding= ((MethodDeclaration) node).resolveBinding();
			if (binding != null) {
				IAnnotationBinding[] annotations= binding.getAnnotations();
				for (IAnnotationBinding annotation : annotations) {
					final ITypeBinding annotationType= annotation.getAnnotationType();
					if (annotationType != null && JUnitCorePlugin.JUNIT4_ANNOTATION_NAME.equals(annotationType.getQualifiedName()))
						return true;
				}
			}
		}
		return false;
	}

	private static class JUnitClasspathFixCorrectionProposal implements IJavaCompletionProposal {

		private final ClasspathFixProposal fClasspathFixProposal;
		private final ImportRewrite fImportRewrite;
		private final IJavaProject fJavaProject;

		public JUnitClasspathFixCorrectionProposal(IJavaProject javaProject, ClasspathFixProposal cpfix, ImportRewrite rewrite) {
			fJavaProject= javaProject;
			fClasspathFixProposal= cpfix;
			fImportRewrite= rewrite;
		}

		protected Change createChange() throws CoreException {
			Change change= fClasspathFixProposal.createChange(null);
			if (fImportRewrite != null) {
				TextFileChange cuChange= new TextFileChange("Add import", (IFile) fImportRewrite.getCompilationUnit().getResource()); //$NON-NLS-1$
				cuChange.setEdit(fImportRewrite.rewriteImports(null));

				CompositeChange composite= new CompositeChange(getDisplayString());
				composite.add(change);
				composite.add(cuChange);
				return composite;
			}
			return change;
		}

		@Override
		public void apply(IDocument document) {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, true, monitor -> {
					try {
						Change change= createChange();
						change.initializeValidationData(new NullProgressMonitor());
						PerformChangeOperation op= new PerformChangeOperation(change);
						op.setUndoManager(RefactoringCore.getUndoManager(), getDisplayString());
						op.setSchedulingRule(fJavaProject.getProject().getWorkspace().getRoot());
						op.run(monitor);
					} catch (CoreException e1) {
						throw new InvocationTargetException(e1);
					} catch (OperationCanceledException e1) {
						throw new InterruptedException();
					}
				});
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, JUnitPlugin.getActiveWorkbenchShell(), JUnitMessages.JUnitQuickFixProcessor_apply_problem_title, JUnitMessages.JUnitQuickFixProcessor_apply_problem_description);
			} catch (InterruptedException e) {

			}
		}

		@Override
		public String getAdditionalProposalInfo() {
			return fClasspathFixProposal.getAdditionalProposalInfo();
		}

		@Override
		public int getRelevance() {
			return fClasspathFixProposal.getRelevance();
		}

		@Override
		public IContextInformation getContextInformation() {
			return null;
		}

		@Override
		public String getDisplayString() {
			return fClasspathFixProposal.getDisplayString();
		}

		@Override
		public Image getImage() {
			return fClasspathFixProposal.getImage();
		}

		@Override
		public Point getSelection(IDocument document) {
			return null;
		}
	}

	private static class AddAssertProposal implements IJavaCompletionProposal {

		private final CompilationUnit fAstRoot;
		private final String fMethodName;
		private final int fRelevance;

		public AddAssertProposal(CompilationUnit astRoot, String methodName, int relevance) {
			fAstRoot= astRoot;
			fMethodName= methodName;
			fRelevance= relevance;
		}

		@Override
		public int getRelevance() {
			return fRelevance;
		}

		@Override
		public void apply(IDocument document) {
			try {
				ImportRewrite rewrite= CodeStyleConfiguration.createImportRewrite(fAstRoot, true);
				rewrite.addStaticImport("org.junit.Assert", fMethodName, true); //$NON-NLS-1$
				TextEdit edit= rewrite.rewriteImports(null);
				edit.apply(document);
			} catch (BadLocationException | MalformedTreeException | CoreException e) {
			}
		}

		@Override
		public String getAdditionalProposalInfo() {
			return Messages.format(JUnitMessages.JUnitQuickFixProcessor_add_assert_info, BasicElementLabels.getJavaElementName("org.junit.Assert." + fMethodName)); //$NON-NLS-1$
		}

		@Override
		public IContextInformation getContextInformation() {
			return null;
		}

		@Override
		public String getDisplayString() {
			return Messages.format(JUnitMessages.JUnitQuickFixProcessor_add_assert_description, BasicElementLabels.getJavaElementName("org.junit.Assert." + fMethodName)); //$NON-NLS-1$
		}

		@Override
		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_IMPDECL);
		}

		@Override
		public Point getSelection(IDocument document) {
			return null;
		}
	}

}
