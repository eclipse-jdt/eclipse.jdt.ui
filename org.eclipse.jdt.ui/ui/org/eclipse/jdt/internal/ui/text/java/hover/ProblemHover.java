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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;

import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;


/**
 * This annotation hover shows the description of the
 * selected java annotation.
 *
 * XXX: Currently this problem hover only works for Java and spelling problems,
 *		see https://bugs.eclipse.org/bugs/show_bug.cgi?id=62081
 *
 * @since 3.0
 */
public class ProblemHover extends AbstractAnnotationHover {

	protected static class ProblemInfo extends AnnotationInfo {

		private static final ICompletionProposal[] NO_PROPOSALS= new ICompletionProposal[0];

		public ProblemInfo(Annotation annotation, Position position, ITextViewer textViewer) {
			super(annotation, position, textViewer);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover.AnnotationInfo#getCompletionProposals()
		 */
		@Override
		public ICompletionProposal[] getCompletionProposals() {
			if (annotation instanceof IJavaAnnotation) {
				ICompletionProposal[] result= getJavaAnnotationFixes((IJavaAnnotation) annotation);
				if (result.length > 0)
					return result;
			}

			if (annotation instanceof MarkerAnnotation)
				return getMarkerAnnotationFixes((MarkerAnnotation) annotation);

			return NO_PROPOSALS;
		}

		private ICompletionProposal[] getJavaAnnotationFixes(IJavaAnnotation javaAnnotation) {
			ProblemLocation location= new ProblemLocation(position.getOffset(), position.getLength(), javaAnnotation);
			ICompilationUnit cu= javaAnnotation.getCompilationUnit();
			if (cu == null)
				return NO_PROPOSALS;

			ISourceViewer sourceViewer= null;
			if (viewer instanceof ISourceViewer)
				sourceViewer= (ISourceViewer) viewer;

			IInvocationContext context= new AssistContext(cu, sourceViewer, location.getOffset(), location.getLength(), SharedASTProviderCore.WAIT_ACTIVE_ONLY);
			if (!SpellingAnnotation.TYPE.equals(javaAnnotation.getType()) && !hasProblem(context.getASTRoot().getProblems(), location))
				return NO_PROPOSALS;

			ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
			JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
			Collections.sort(proposals, new CompletionProposalComparator());

			return proposals.toArray(new ICompletionProposal[proposals.size()]);
		}

		private static boolean hasProblem(IProblem[] problems, IProblemLocation location) {
			for (IProblem problem : problems) {
				if (problem.getID() == location.getProblemId() && problem.getSourceStart() == location.getOffset())
					return true;
			}
			return false;
		}

		private ICompletionProposal[] getMarkerAnnotationFixes(MarkerAnnotation markerAnnotation) {
			if (markerAnnotation.isQuickFixableStateSet() && !markerAnnotation.isQuickFixable())
				return NO_PROPOSALS;

			IMarker marker= markerAnnotation.getMarker();

			ICompilationUnit cu= getCompilationUnit(marker);
			if (cu == null)
				return NO_PROPOSALS;

			IEditorInput input= EditorUtility.getEditorInput(cu);
			if (input == null)
				return NO_PROPOSALS;

			IAnnotationModel model= JavaUI.getDocumentProvider().getAnnotationModel(input);
			if (model == null)
				return NO_PROPOSALS;

			ISourceViewer sourceViewer= null;
			if (viewer instanceof ISourceViewer)
				sourceViewer= (ISourceViewer) viewer;

			AssistContext context= new AssistContext(cu, sourceViewer, position.getOffset(), position.getLength());

			ArrayList<IJavaCompletionProposal> proposals= new ArrayList<>();
			JavaCorrectionProcessor.collectProposals(context, model, new Annotation[] { markerAnnotation }, true, false, proposals);
			Collections.sort(proposals, new CompletionProposalComparator());

			return proposals.toArray(new ICompletionProposal[proposals.size()]);
		}

		private static ICompilationUnit getCompilationUnit(IMarker marker) {
			IResource res= marker.getResource();
			if (res instanceof IFile && res.isAccessible()) {
				IJavaElement element= JavaCore.create((IFile) res);
				if (element instanceof ICompilationUnit)
					return (ICompilationUnit) element;
			}
			return null;
		}

	}

	public ProblemHover() {
		super(false);
	}

	@Override
	protected AnnotationInfo createAnnotationInfo(Annotation annotation, Position position, ITextViewer textViewer) {
		return new ProblemInfo(annotation, position, textViewer);
	}
}
