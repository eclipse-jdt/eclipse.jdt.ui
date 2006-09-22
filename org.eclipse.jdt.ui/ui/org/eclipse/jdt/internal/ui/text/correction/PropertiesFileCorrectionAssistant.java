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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * PropertiesFileCorrectionAssistant.
 *
 * @since 3.1
 */
public class PropertiesFileCorrectionAssistant extends QuickAssistAssistant {

	private ITextViewer fViewer;
	private ITextEditor fEditor;
	private Position fPosition;


	/**
	 * Constructor for PropertiesFileCorrectionAssistant.
	 */
	public PropertiesFileCorrectionAssistant(ITextEditor editor) {
		Assert.isNotNull(editor);
		fEditor= editor;

		setQuickAssistProcessor(new PropertiesFileCorrectionProcessor(this));

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

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistant#install(org.eclipse.jface.text.ITextViewer)
	 */
	public void install(ISourceViewer sourceViewer) {
		super.install(sourceViewer);
		fViewer= sourceViewer;
	}



	/*
	 * @see org.eclipse.jface.text.contentassist.ContentAssistant#uninstall()
	 */
	public void uninstall() {
		fViewer= null;
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
			return super.showPossibleQuickAssists();

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
		return super.showPossibleQuickAssists();
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
		IAnnotationModel model= null;
		IEditorInput input= fEditor.getEditorInput();
		if (!(input instanceof IStorageEditorInput))
			return -1;

		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= null;
		try {
			path= ((IStorageEditorInput)input).getStorage().getFullPath();
			if (path == null)
				return -1;
			manager.connect(path, null);
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			return -1;
		}

		try {
			model= manager.getTextFileBuffer(path).getAnnotationModel();

			int invocationOffset= -1;
			int offsetOfFirstProblem= Integer.MAX_VALUE;

			Iterator iter= model.getAnnotationIterator();
			while (iter.hasNext()) {
				Annotation annot= (Annotation) iter.next();
				if (JavaCorrectionProcessor.isQuickFixableType(annot)) {
					Position pos= model.getPosition(annot);
					if (isIncluded(pos, startOffset, endOffset)) {
						if (PropertiesFileCorrectionProcessor.hasCorrections(annot)) {
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
		} finally {
			try {
				manager.disconnect(path, null);
			} catch (CoreException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
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
