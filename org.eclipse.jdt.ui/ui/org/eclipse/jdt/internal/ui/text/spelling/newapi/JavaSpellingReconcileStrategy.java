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

package org.eclipse.jdt.internal.ui.text.spelling.newapi;

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

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Reconcile strategy for spell checking comments.
 * 
 * @since 3.1
 */
public class JavaSpellingReconcileStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	/**
	 * Spelling problem to be accepted by problem requesters.
	 */
	private class CoreSpellingProblem implements IProblem {

		/** The end offset of the problem */
		private int fSourceEnd= 0;

		/** The line number of the problem */
		private int fLineNumber= 1;

		/** The start offset of the problem */
		private int fSourceStart= 0;

		/** The description of the problem */
		private String fMessage;

		/** The misspelled word */
		private String fWord;

		/** Was the word found in the dictionary? */
		private boolean fMatch;

		/** Does the word start a new sentence? */
		private boolean fSentence;

		/**
		 * Initialize with the given parameters.
		 * 
		 * @param start the start offset
		 * @param end the end offset
		 * @param line the line
		 * @param message the message
		 * @param word the word
		 * @param match <code>true</code> iff the word was found in the dictionary
		 * @param sentence <code>true</code> iff the word starts a sentence
		 */
		public CoreSpellingProblem(int start, int end, int line, String message, String word, boolean match, boolean sentence) {
			super();
			fSourceStart= start;
			fSourceEnd= end;
			fLineNumber= line;
			fMessage= message;
			fWord= word;
			fMatch= match;
			fSentence= sentence;
		}
		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#getArguments()
		 */
		public String[] getArguments() {

			String prefix= ""; //$NON-NLS-1$
			String postfix= ""; //$NON-NLS-1$

			try {

				IRegion line= fDocument.getLineInformationOfOffset(fSourceStart);

				prefix= fDocument.get(line.getOffset(), fSourceStart - line.getOffset());
				postfix= fDocument.get(fSourceEnd + 1, line.getOffset() + line.getLength() - fSourceEnd);

			} catch (BadLocationException exception) {
				// Do nothing
			}
			return new String[] { fWord, prefix, postfix, fSentence ? Boolean.toString(true) : Boolean.toString(false), fMatch ? Boolean.toString(true) : Boolean.toString(false) };
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#getID()
		 */
		public int getID() {
			return SPELLING_PROBLEM_ID;
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#getMessage()
		 */
		public String getMessage() {
			return fMessage;
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#getOriginatingFileName()
		 */
		public char[] getOriginatingFileName() {
			return fEditor.getEditorInput().getName().toCharArray();
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#getSourceEnd()
		 */
		public int getSourceEnd() {
			return fSourceEnd;
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#getSourceLineNumber()
		 */
		public int getSourceLineNumber() {
			return fLineNumber;
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#getSourceStart()
		 */
		public int getSourceStart() {
			return fSourceStart;
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#isError()
		 */
		public boolean isError() {
			return false;
		}

		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#isWarning()
		 */
		public boolean isWarning() {
			return true;
		}
		
		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#setSourceStart(int)
		 */
		public void setSourceStart(int sourceStart) {
			fSourceStart= sourceStart;
		}
		
		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#setSourceEnd(int)
		 */
		public void setSourceEnd(int sourceEnd) {
			fSourceEnd= sourceEnd;
		}
		
		/*
		 * @see org.eclipse.jdt.core.compiler.IProblem#setSourceLineNumber(int)
		 */
		public void setSourceLineNumber(int lineNumber) {
			fLineNumber= lineNumber;
		}
	}

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
					CoreSpellingProblem iProblem= new CoreSpellingProblem(problem.getOffset(), problem.getOffset() + problem.getLength() - 1, line, problem.getMessage(), word, dictionaryMatch, sentenceStart);
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
			SpellingContext context= new SpellingContext();
			context.setContentType(getContentType());
			EditorsUI.getSpellingService().check(fDocument, context, fCollector, fProgressMonitor);
		}
	}

	/**
	 * Returns the content type of the underlying editor input.
	 * 
	 * @return the content type of the underlying editor input or
	 *         <code>null</code> if none could be determined
	 */
	private IContentType getContentType() {
		IDocumentProvider documentProvider= fEditor.getDocumentProvider();
		if (documentProvider instanceof IDocumentProviderExtension4) {
			try {
				IContentDescription desc= ((IDocumentProviderExtension4) documentProvider).getContentDescription(fEditor.getEditorInput());
				if (desc != null)
					return desc.getContentType();
			} catch (CoreException x) {
				JavaPlugin.log(x.getStatus());
			}
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
