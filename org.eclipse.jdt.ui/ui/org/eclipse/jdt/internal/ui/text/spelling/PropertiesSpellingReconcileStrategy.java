/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.spelling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.ProblemAnnotation;

/**
 * Reconcile strategy for spell checking comments.
 *
 * @since 3.1
 */
public class PropertiesSpellingReconcileStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	/**
	 * Spelling problem collector that forwards {@link SpellingProblem}s as
	 * {@link IProblem}s to the {@link org.eclipse.jdt.core.IProblemRequestor}.
	 */
	private class SpellingProblemCollector implements ISpellingProblemCollector {

		/** Annotation model */
		private IAnnotationModel fAnnotationModel;

		/** Annotations to add */
		private Map fAddAnnotations;

		/**
		 * Initializes this collector with the given annotation model.
		 *
		 * @param annotationModel the annotation model
		 */
		public SpellingProblemCollector(IAnnotationModel annotationModel) {
			fAnnotationModel= annotationModel;
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
		 */
		public void accept(SpellingProblem problem) {
			try {
				int line= fDocument.getLineOfOffset(problem.getOffset()) + 1;
				String word= fDocument.get(problem.getOffset(), problem.getLength());
				boolean dictionaryMatch= false;
				boolean sentenceStart= false;
				if (problem instanceof JavaSpellingProblem) {
					dictionaryMatch= ((JavaSpellingProblem)problem).isDictionaryMatch();
					sentenceStart= ((JavaSpellingProblem) problem).isSentenceStart();
				}
				// see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=81514
				IEditorInput editorInput= fEditor.getEditorInput();
				if (editorInput != null) {
					CoreSpellingProblem iProblem= new CoreSpellingProblem(problem.getOffset(), problem.getOffset() + problem.getLength() - 1, line, problem.getMessage(), word, dictionaryMatch, sentenceStart, fDocument, editorInput.getName());
					fAddAnnotations.put(new ProblemAnnotation(iProblem, null), new Position(problem.getOffset(), problem.getLength()));
				}
			} catch (BadLocationException x) {
				// drop this SpellingProblem
			}
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
		 */
		public void beginCollecting() {
			fAddAnnotations= new HashMap();
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting()
		 */
		public void endCollecting() {
			List removeAnnotations= new ArrayList();
			for (Iterator iter= fAnnotationModel.getAnnotationIterator(); iter.hasNext();) {
				Annotation annotation= (Annotation) iter.next();
				if (ProblemAnnotation.SPELLING_ANNOTATION_TYPE.equals(annotation.getType()))
					removeAnnotations.add(annotation);
			}

			if (fAnnotationModel instanceof IAnnotationModelExtension)
				((IAnnotationModelExtension) fAnnotationModel).replaceAnnotations((Annotation[]) removeAnnotations.toArray(new Annotation[removeAnnotations.size()]), fAddAnnotations);
			else {
				for (Iterator iter= removeAnnotations.iterator(); iter.hasNext();)
					fAnnotationModel.removeAnnotation((Annotation) iter.next());
				for (Iterator iter= fAddAnnotations.keySet().iterator(); iter.hasNext();) {
					Annotation annotation= (Annotation) iter.next();
					fAnnotationModel.addAnnotation(annotation, (Position) fAddAnnotations.get(annotation));
				}
			}

			fAddAnnotations= null;
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
	
	/**
	 * The spelling context containing the Java properties
	 * content type.
	 * <p>
	 * Since his reconcile strategy is for the Properties File
	 * editor which normally edits Java properties files we always
	 * use the Java properties file content type for performance
	 * reasons.
	 * </p>
	 * @since 3.2
	 */
	private SpellingContext fSpellingContext;


	/**
	 * Creates a new comment reconcile strategy.
	 *
	 * @param editor the text editor to operate on
	 */
	public PropertiesSpellingReconcileStrategy(ITextEditor editor) {
		fEditor= editor;
		fSpellingContext= new SpellingContext();
		fSpellingContext.setContentType(Platform.getContentTypeManager().getContentType("org.eclipse.jdt.core.javaProperties")); //$NON-NLS-1$
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
		IAnnotationModel model= getAnnotationModel();
		if (model == null)
			return;

		PropertiesSpellingReconcileStrategy.SpellingProblemCollector collector= new SpellingProblemCollector(model);
		EditorsUI.getSpellingService().check(fDocument, fSpellingContext, collector, fProgressMonitor);

	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		fDocument= document;
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor= monitor;
	}

	/**
	 * Returns the annotation model of the underlying editor input.
	 *
	 * @return the annotation model of the underlying editor input or
	 *         <code>null</code> if none could be determined
	 */
	private IAnnotationModel getAnnotationModel() {
		IDocumentProvider documentProvider= fEditor.getDocumentProvider();
		if (documentProvider == null)
			return null;
		return documentProvider.getAnnotationModel(fEditor.getEditorInput());
	}
}
