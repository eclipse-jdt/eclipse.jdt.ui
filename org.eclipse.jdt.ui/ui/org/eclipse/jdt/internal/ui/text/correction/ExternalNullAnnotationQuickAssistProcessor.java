/*******************************************************************************
 * Copyright (c) 2015 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jdt.internal.corext.fix.ExternalNullAnnotationChangeProposals;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;


/**
 * Alternate, stripped down quick assist processor for the annotate class file action.
 *
 * @see <a href="https://bugs.eclipse.org/458200">[null] "Annotate" proposals for adding external
 *      null annotations to library classes</a>
 * @since 3.11
 */
public class ExternalNullAnnotationQuickAssistProcessor implements IQuickAssistProcessor {

	public static final String ANNOTATE_MEMBER_ID= "org.eclipse.jdt.ui.annotate.nullAnnotateMember.assist"; //$NON-NLS-1$

	private JavaCorrectionAssistant fAssistant;

	private String fErrorMessage;

	public ExternalNullAnnotationQuickAssistProcessor(JavaCorrectionAssistant javaCorrectionAssistant) {
		fAssistant= javaCorrectionAssistant;
	}

	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		return false; // irrelevant on class files
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		IEditorPart part= fAssistant.getEditor();
		IClassFile classFile= ((IClassFileEditorInput) part.getEditorInput()).getClassFile();
		return JavaCore.ENABLED.equals(classFile.getJavaProject().getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true));
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext quickAssistContext) {
		ICompilationUnit cu= null;
		try {
			// preparation similar to JavaCorrectionProcessor:
			IEditorPart part= fAssistant.getEditor();

			IClassFile classFile= ((IClassFileEditorInput) part.getEditorInput()).getClassFile();
			cu= classFile.getWorkingCopy((WorkingCopyOwner) null, null);
			if (cu == null || classFile.getSource() == null)
				return null;

			ISourceViewer viewer= quickAssistContext.getSourceViewer();
			int documentOffset= quickAssistContext.getOffset();

			int length= viewer != null ? viewer.getSelectedRange().y : 0;
			AssistContext context= new AssistContext(cu, viewer, part, documentOffset, length);

			ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();

			// shortcut (we only have one processor):
			ExternalNullAnnotationChangeProposals.collectExternalAnnotationProposals(context.getCompilationUnit(),
					context.getCoveringNode(), documentOffset, proposals);

			return proposals.toArray(new IJavaCompletionProposal[proposals.size()]);

		} catch (JavaModelException e) {
			fErrorMessage= e.getMessage();
			JavaPlugin.log(e);
			return null;
		} finally {
			try {
				if (cu != null)
					cu.discardWorkingCopy();
			} catch (JavaModelException e) {
				// ignore
			}
		}
	}

	/**
	 * API for regular assist processor working on a source file / compilation unit.
	 * @param context the invocation context
	 * @param proposals list of proposals to which new proposals may be added
	 */
	public static void getAnnotateProposals(IInvocationContext context, List<ICommandAccess> proposals) {
		List<IJavaCompletionProposal> eeaList= new ArrayList<>();
		ExternalNullAnnotationChangeProposals.collectExternalAnnotationProposals(context.getCompilationUnit(),
				context.getCoveringNode(), context.getSelectionOffset(), eeaList);
		for (IJavaCompletionProposal proposal : eeaList)
			proposals.add((ICommandAccess) proposal);
	}

	public static boolean canAssist(IInvocationContext context) {
		ICompilationUnit cu= context.getCompilationUnit();
		if (cu != null) {
			IJavaProject javaProject= cu.getJavaProject();
			return JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, true))
					&& ExternalNullAnnotationChangeProposals.hasAnnotationPathInWorkspace(javaProject, cu);
		}
		return false;
	}
}
