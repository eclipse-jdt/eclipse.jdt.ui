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

package org.eclipse.jdt.internal.ui.text.spelling;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension4;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Reconcile strategy for spell checking comments.
 * 
 * @since 3.1
 */
public class JavaSpellingReconcileStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	/**
	 * Spelling problem collector that forwards {@link SpellingProblem}s as
	 * {@link IProblem}s to the {@link IProblemRequestor}.
	 */
	private class SpellingProblemCollector implements ISpellingProblemCollector {

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
		 */
		public void accept(SpellingProblem problem) {
			if (fRequester != null) {
				try {
					int line= fDocument.getLineOfOffset(problem.getOffset()) + 1;
					String word= fDocument.get(problem.getOffset(), problem.getLength());
					boolean dictionaryMatch= false;
					boolean sentenceStart= false;
					if (problem instanceof JavaSpellingProblem) {
						dictionaryMatch= ((JavaSpellingProblem)problem).isDictionaryMatch();
						sentenceStart= ((JavaSpellingProblem) problem).isSentenceStart();
					}
					CoreSpellingProblem iProblem= new CoreSpellingProblem(problem.getOffset(), problem.getOffset() + problem.getLength() - 1, line, problem.getMessage(), word, dictionaryMatch, sentenceStart, fDocument, fEditor.getEditorInput().getName());
					fRequester.acceptProblem(iProblem);
				} catch (BadLocationException x) {
					// drop this SpellingProblem
				}
			}
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginReporting()
		 */
		public void beginReporting() {
			if (fRequester != null)
				fRequester.beginReporting();
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endReporting()
		 */
		public void endReporting() {
			if (fRequester != null)
				fRequester.endReporting();
		}
	}

	/** The id of the problem */
	public static final int SPELLING_PROBLEM_ID= 0x80000000;
	
	/** The text editor to operate on. */
	private ITextEditor fEditor;

	/** The document to operate on. */
	private IDocument fDocument;

	/** The progress monitor. */
	private IProgressMonitor fProgressMonitor;
	
	/** The problem requester. */
	private IProblemRequestor fRequester;
	
	/** The spelling problem collector. */
	private ISpellingProblemCollector fCollector= new SpellingProblemCollector();

	/**
	 * Creates a new comment reconcile strategy.
	 * 
	 * @param editor the text editor to operate on
	 */
	public JavaSpellingReconcileStrategy(ITextEditor editor) {
		fEditor= editor;
		updateProblemRequester();
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
	 */
	public void initialReconcile() {
		reconcile(new Region(0, fDocument.getLength()));
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion,org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile(subRegion);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion region) {
		if (fRequester != null) {
			try {
				SpellingContext context= new SpellingContext();
				context.setContentType(getContentType());
				EditorsUI.getSpellingService().check(fDocument, context, fCollector, fProgressMonitor);
			} catch (CoreException x) {
				// swallow exception
			}
		}
	}

	/**
	 * Returns the content type of the underlying editor input.
	 * 
	 * @return the content type of the underlying editor input or
	 *         <code>null</code> if none could be determined
	 * @throws CoreException if reading or accessing the underlying store fails
	 */
	private IContentType getContentType() throws CoreException {
		IDocumentProvider documentProvider= fEditor.getDocumentProvider();
		if (documentProvider instanceof IDocumentProviderExtension4) {
			IContentDescription desc= ((IDocumentProviderExtension4) documentProvider).getContentDescription(fEditor.getEditorInput());
			if (desc != null)
				return desc.getContentType();
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		fDocument= document;
		updateProblemRequester();
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor= monitor;
	}

	/**
	 * Update the problem requester based on the current editor
	 */
	private void updateProblemRequester() {
		IAnnotationModel model= fEditor.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
		fRequester= (model instanceof IProblemRequestor) ? (IProblemRequestor) model : null;
	}
}
