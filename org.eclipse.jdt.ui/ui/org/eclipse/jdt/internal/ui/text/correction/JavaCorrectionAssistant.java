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

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Iterator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;


public class JavaCorrectionAssistant extends ContentAssistant {

	private ITextViewer fViewer;
	private IEditorPart fEditor;
	private Position fPosition;
		
	private QuickAssistLightBulbUpdater fLightBulbUpdater;
	
	/**
	 * Constructor for JavaCorrectionAssistant.
	 */
	public JavaCorrectionAssistant(IEditorPart editor) {
		super();
		Assert.isNotNull(editor);
		fEditor= editor;
		
		JavaCorrectionProcessor processor= new JavaCorrectionProcessor(this); 
		
		setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		setContentAssistProcessor(processor, IJavaPartitions.JAVA_STRING);
		setContentAssistProcessor(processor, IJavaPartitions.JAVA_CHARACTER);
		setContentAssistProcessor(processor, IJavaPartitions.JAVA_DOC);
		setContentAssistProcessor(processor, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
		setContentAssistProcessor(processor, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
	
		enableAutoActivation(false);
		enableAutoInsert(false);
		
		setContextInformationPopupOrientation(CONTEXT_INFO_ABOVE);
		setInformationControlCreator(getInformationControlCreator());

		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		IColorManager manager= textTools.getColorManager();

		IPreferenceStore store=  JavaPlugin.getDefault().getPreferenceStore();

		Color c= getColor(store, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, manager);
		setProposalSelectorForeground(c);
		
		c= getColor(store, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, manager);
		setProposalSelectorBackground(c);
	}
	
	public IEditorPart getEditor() {
		return fEditor;
	}
	
		
	private IInformationControlCreator getInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, new HTMLTextPresenter());
			}
		};
	}	
	
	private static Color getColor(IPreferenceStore store, String key, IColorManager manager) {
		RGB rgb= PreferenceConverter.getColor(store, key);
		return manager.getColor(rgb);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistant#install(org.eclipse.jface.text.ITextViewer)
	 */
	public void install(ITextViewer textViewer) {
		super.install(textViewer);
		fViewer= textViewer;
		
		fLightBulbUpdater= new QuickAssistLightBulbUpdater(fEditor, textViewer);
		fLightBulbUpdater.install();
	}
	


	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ContentAssistant#uninstall()
	 */
	public void uninstall() {
		if (fLightBulbUpdater != null) {
			fLightBulbUpdater.uninstall();
			fLightBulbUpdater= null;
		}
		super.uninstall();
	}

	/**
	 * Show completions at caret position. If current
	 * position does not contain quick fixes look for
	 * next quick fix on same line by moving from left
	 * to right and restarting at end of line if the
	 * beginning of the line is reached.
	 * 
	 * @see org.eclipse.jface.text.contentassist.IContentAssistant#showPossibleCompletions()
	 */
	public String showPossibleCompletions() {
		if (fViewer == null || fViewer.getDocument() == null)
			// Let superclass deal with this
			return super.showPossibleCompletions();
		
		Point selectedRange= fViewer.getSelectedRange();
		fPosition= null;
		
		if (selectedRange.y == 0) {
			int invocationOffset= computeOffsetWithCorrection(selectedRange.x);
			if (invocationOffset != -1) {
				storePosition();
				fViewer.setSelectedRange(invocationOffset, 0);
				fViewer.revealRange(invocationOffset, 0);	
			}
		}
		return super.showPossibleCompletions();
	}

	/**
	 * Find offset which contains corrections.
	 * Search on same line by moving from left
	 * to right and restarting at end of line if the
	 * beginning of the line is reached.
	 * 
	 * @return an offset where corrections are available or -1 if none
	 */
	private int computeOffsetWithCorrection(int initalOffset) {
		IRegion lineInfo= null;
		try {
			lineInfo= fViewer.getDocument().getLineInformationOfOffset(initalOffset);
		} catch (BadLocationException ex) {
			return -1;
		}
		int startOffset= lineInfo.getOffset();
		int endOffset= startOffset + lineInfo.getLength();
		
		int result= computeOffsetWithCorrection(startOffset, endOffset, initalOffset);
		if (result > 0 && result != initalOffset)
			return result;
		else
			return -1;
	}

	/**
	 * @return the best matching offset with corrections or -1 if nothing is found
	 */
	private int computeOffsetWithCorrection(int startOffset, int endOffset, int initialOffset) {
		IAnnotationModel model= JavaUI.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());

		int invocationOffset= -1;
		int offsetOfFirstProblem= Integer.MAX_VALUE;

		Iterator iter= model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annot= (Annotation) iter.next();
			if (JavaCorrectionProcessor.isQuickFixableType(annot)) {
				Position pos= model.getPosition(annot);
				if (isIncluded(pos, startOffset, endOffset)) {
					if (JavaCorrectionProcessor.hasCorrections(annot)) {
						offsetOfFirstProblem= Math.min(offsetOfFirstProblem, pos.getOffset());
						invocationOffset= computeBestOffset(invocationOffset, pos, initialOffset);
						if (initialOffset == invocationOffset)
							return initialOffset;
					}
				}
			}
		}
		if (initialOffset < offsetOfFirstProblem && offsetOfFirstProblem != Integer.MAX_VALUE)
			return offsetOfFirstProblem;
		else
			return invocationOffset;
	}

	private boolean isIncluded(Position pos, int lineStart, int lineEnd) {
		return (pos != null) && (pos.getOffset() >= lineStart && (pos.getOffset() +  pos.getLength() <= lineEnd));
	}

	/**
	 * Computes and returns the invocation offset given a new
	 * position, the initial offset and the best invocation offset
	 * found so far.
	 * <p>
	 * The closest offset to the left of the initial offset is the
	 * best. If there is no offset on the left, the closest on the
	 * right is the best.</p>
	 */
	private int computeBestOffset(int invocationOffset, Position pos, int initalOffset) {
		int newOffset= pos.offset;
		if (newOffset <= initalOffset && initalOffset <= newOffset + pos.length)
			return initalOffset;

		if (invocationOffset < 0)
			return newOffset;

		if (newOffset <= initalOffset && invocationOffset >= initalOffset)
			return newOffset;

		if (newOffset <= initalOffset && invocationOffset < initalOffset)
			return Math.max(invocationOffset, newOffset);

		if (invocationOffset <= initalOffset)
			return invocationOffset;
		
		return Math.max(invocationOffset, newOffset);
	}

	/* 
	 * @see org.eclipse.jface.text.contentassist.ContentAssistant#possibleCompletionsClosed()
	 */
	protected void possibleCompletionsClosed() {
		super.possibleCompletionsClosed();
		restorePosition();
	}

	private void storePosition() {
		int initalOffset= fViewer.getSelectedRange().x;
		int length= fViewer.getSelectedRange().y;
		fPosition= new Position(initalOffset, length);
	}

	private void restorePosition() {
		if (fPosition != null && !fPosition.isDeleted() && fViewer.getDocument() != null) {
			fViewer.setSelectedRange(fPosition.offset, fPosition.length);
			fViewer.revealRange(fPosition.offset, fPosition.length);
		}
		fPosition= null;
	}
	
	/**
	 * Returns true if the last invoked completion was called with an updated offset.
	 */
	public boolean isUpdatedOffset() {
		return fPosition != null;
	}
	
}
