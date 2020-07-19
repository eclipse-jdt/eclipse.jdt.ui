/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.custom.StyleRange;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager.HighlightedPosition;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager.Highlighting;
import org.eclipse.jdt.internal.ui.text.JavaPresentationReconciler;


/**
 * Semantic highlighting presenter - UI thread implementation.
 *
 * @since 3.0
 */
public class SemanticHighlightingPresenter extends SemanticHighlightingPresenterCore
	implements ITextPresentationListener, ITextInputListener, IDocumentListener {

	/** The source viewer this semantic highlighting reconciler is installed on */
	private JavaSourceViewer fSourceViewer;
	/** The background presentation reconciler */
	private JavaPresentationReconciler fPresentationReconciler;

	/**
	 * Creates and returns a new highlighted position with the given offset, length and highlighting.
	 * <p>
	 * NOTE: Also called from background thread.
	 * </p>
	 *
	 * @param offset The offset
	 * @param length The length
	 * @param highlighting The highlighting
	 * @return The new highlighted position
	 */
	public HighlightedPosition createHighlightedPosition(int offset, int length, Highlighting highlighting) {
		// TODO: reuse deleted positions
		return new HighlightedPosition(offset, length, highlighting, fPositionUpdater);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingPresenterCore#addPositionForEvent()
	 */
	@Override
	protected void addPositionForEvent(DocumentEvent event, String category, int offset, int length, Object highlighting) {
		addPositionFromUI(offset, length, (Highlighting)highlighting);
	}


	/**
	 * Create a text presentation in the background.
	 * <p>
	 * NOTE: Called from background thread.
	 * </p>
	 *
	 * @param addedPositions the added positions
	 * @param removedPositions the removed positions
	 * @return the text presentation or <code>null</code>, if reconciliation should be canceled
	 */
	public TextPresentation createPresentation(List<Position> addedPositions, List<Position> removedPositions) {
		JavaSourceViewer sourceViewer= fSourceViewer;
		JavaPresentationReconciler presentationReconciler= fPresentationReconciler;
		if (sourceViewer == null || presentationReconciler == null)
			return null;

		if (isCanceled())
			return null;

		IDocument document= sourceViewer.getDocument();
		if (document == null)
			return null;

		int minStart= Integer.MAX_VALUE;
		int maxEnd= Integer.MIN_VALUE;
		for (Position position : removedPositions) {
			int offset= position.getOffset();
			minStart= Math.min(minStart, offset);
			maxEnd= Math.max(maxEnd, offset + position.getLength());
		}
		for (Position position : addedPositions) {
			int offset= position.getOffset();
			minStart= Math.min(minStart, offset);
			maxEnd= Math.max(maxEnd, offset + position.getLength());
		}

		if (minStart < maxEnd)
			try {
				return presentationReconciler.createRepairDescription(new Region(minStart, maxEnd - minStart), document);
			} catch (RuntimeException e) {
				// Assume concurrent modification from UI thread
			}

		return null;
	}

	/**
	 * Create a runnable for updating the presentation.
	 * <p>
	 * NOTE: Called from background thread.
	 * </p>
	 * @param textPresentation the text presentation
	 * @param addedPositions the added positions
	 * @param removedPositions the removed positions
	 * @return the runnable or <code>null</code>, if reconciliation should be canceled
	 */
	public Runnable createUpdateRunnable(final TextPresentation textPresentation, List<Position> addedPositions, List<Position> removedPositions) {
		if (fSourceViewer == null || textPresentation == null)
			return null;

		// TODO: do clustering of positions and post multiple fast runnables
		final HighlightedPosition[] added= new SemanticHighlightingManager.HighlightedPosition[addedPositions.size()];
		addedPositions.toArray(added);
		final SemanticHighlightingManager.HighlightedPosition[] removed= new SemanticHighlightingManager.HighlightedPosition[removedPositions.size()];
		removedPositions.toArray(removed);

		if (isCanceled())
			return null;

		Runnable runnable= () -> updatePresentation(textPresentation, added, removed);
		return runnable;
	}

	/**
	 * Invalidate the presentation of the positions based on the given added positions and the existing deleted positions.
	 * Also unregisters the deleted positions from the document and patches the positions of this presenter.
	 * <p>
	 * NOTE: Indirectly called from background thread by UI runnable.
	 * </p>
	 * @param textPresentation the text presentation or <code>null</code>, if the presentation should computed in the UI thread
	 * @param addedPositions the added positions
	 * @param removedPositions the removed positions
	 */
	public void updatePresentation(TextPresentation textPresentation, HighlightedPosition[] addedPositions, HighlightedPosition[] removedPositions) {
		if (fSourceViewer == null)
			return;

//		checkOrdering("added positions: ", Arrays.asList(addedPositions)); //$NON-NLS-1$
//		checkOrdering("removed positions: ", Arrays.asList(removedPositions)); //$NON-NLS-1$
//		checkOrdering("old positions: ", fPositions); //$NON-NLS-1$

		// TODO: double-check consistency with document.getPositions(...)
		// TODO: reuse removed positions
		if (isCanceled())
			return;

		IDocument document= fSourceViewer.getDocument();
		if (document == null)
			return;

		String positionCategory= getPositionCategory();

		List<HighlightedPosition> removedPositionsList= Arrays.asList(removedPositions);

		try {
			synchronized (fPositionLock) {
				List<Position> oldPositions= fPositions;
				int newSize= Math.max(fPositions.size() + addedPositions.length - removedPositions.length, 10);

				/*
				 * The following loop is a kind of merge sort: it merges two List<Position>, each
				 * sorted by position.offset, into one new list. The first of the two is the
				 * previous list of positions (oldPositions), from which any deleted positions get
				 * removed on the fly. The second of two is the list of added positions. The result
				 * is stored in newPositions.
				 */
				List<Position> newPositions= new ArrayList<>(newSize);
				Position position= null;
				Position addedPosition= null;
				for (int i= 0, j= 0, n= oldPositions.size(), m= addedPositions.length; i < n || position != null || j < m || addedPosition != null;) {
					// loop variant: i + j < old(i + j)

					// a) find the next non-deleted Position from the old list
					while (position == null && i < n) {
						position= oldPositions.get(i++);
						if (position.isDeleted() || contain(removedPositionsList, position)) {
							document.removePosition(positionCategory, position);
							position= null;
						}
					}

					// b) find the next Position from the added list
					if (addedPosition == null && j < m) {
						addedPosition= addedPositions[j++];
						document.addPosition(positionCategory, addedPosition);
					}

					// c) merge: add the next of position/addedPosition with the lower offset
					if (position != null) {
						if (addedPosition != null)
							if (position.getOffset() <= addedPosition.getOffset()) {
								newPositions.add(position);
								position= null;
							} else {
								newPositions.add(addedPosition);
								addedPosition= null;
							}
						else {
							newPositions.add(position);
							position= null;
						}
					} else if (addedPosition != null) {
						newPositions.add(addedPosition);
						addedPosition= null;
					}
				}
				fPositions= newPositions;
			}
		} catch (BadPositionCategoryException | BadLocationException e) {
			// Should not happen
			JavaPlugin.log(e);
		}
//		checkOrdering("new positions: ", fPositions); //$NON-NLS-1$

		if (textPresentation != null)
			fSourceViewer.changeTextPresentation(textPresentation, false);
		else
			fSourceViewer.invalidateTextPresentation();
	}

//	private void checkOrdering(String s, List positions) {
//		Position previous= null;
//		for (int i= 0, n= positions.size(); i < n; i++) {
//			Position current= (Position) positions.get(i);
//			if (previous != null && previous.getOffset() + previous.getLength() > current.getOffset())
//				return;
//		}
//	}

	/**
	 * Insert the given position in <code>fPositions</code>, s.t. the offsets remain in linear order.
	 *
	 * @param position The position for insertion
	 */
	private void insertPosition(Position position) {
		int i= computeIndexAfterOffset(fPositions, position.getOffset());
		fPositions.add(i, position);
	}

	/**
	 * Returns the index of the first position with an offset greater than the given offset.
	 *
	 * @param positions the positions, must be ordered by offset and must not overlap
	 * @param offset the offset
	 * @return the index of the last position with an offset greater than the given offset
	 */
	private int computeIndexAfterOffset(List<Position> positions, int offset) {
		int i= -1;
		int j= positions.size();
		while (j - i > 1) {
			int k= (i + j) >> 1;
			Position position= positions.get(k);
			if (position.getOffset() > offset)
				j= k;
			else
				i= k;
		}
		return j;
	}

	/*
	 * @see org.eclipse.jface.text.ITextPresentationListener#applyTextPresentation(org.eclipse.jface.text.TextPresentation)
	 */
	@Override
	public void applyTextPresentation(TextPresentation textPresentation) {
		IRegion region= textPresentation.getExtent();
		int i= computeIndexAtOffset(fPositions, region.getOffset()), n= computeIndexAtOffset(fPositions, region.getOffset() + region.getLength());
		if (n - i > 2) {
			List<StyleRange> ranges= new ArrayList<>(n - i);
			for (; i < n; i++) {
				HighlightedPosition position= (HighlightedPosition) fPositions.get(i);
				if (!position.isDeleted())
					ranges.add(position.createStyleRange());
			}
			StyleRange[] array= new StyleRange[ranges.size()];
			array= ranges.toArray(array);
			textPresentation.replaceStyleRanges(array);
		} else {
			for (; i < n; i++) {
				HighlightedPosition position= (HighlightedPosition) fPositions.get(i);
				if (!position.isDeleted())
					textPresentation.replaceStyleRange(position.createStyleRange());
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
	 */
	@Override
	public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
		setCanceled(true);
		releaseDocument(oldInput);
		resetState();
	}

	/*
	 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
	 */
	@Override
	public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
		manageDocument(newInput);
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		setCanceled(true);
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
	}

	/**
	 * @return Returns <code>true</code> iff the current reconcile is canceled.
	 * <p>
	 * NOTE: Also called from background thread.
	 * </p>
	 */
	@Override
	public boolean isCanceled() {
		IDocument document= fSourceViewer != null ? fSourceViewer.getDocument() : null;
		if (document == null)
			return fIsCanceled;

		synchronized (getLockObject(document)) {
			return fIsCanceled;
		}
	}

	/**
	 * Set whether or not the current reconcile is canceled.
	 *
	 * @param isCanceled <code>true</code> iff the current reconcile is canceled
	 */
	@Override
	public void setCanceled(boolean isCanceled) {
		IDocument document= fSourceViewer != null ? fSourceViewer.getDocument() : null;
		if (document == null) {
			fIsCanceled= isCanceled;
			return;
		}

		synchronized (getLockObject(document)) {
			fIsCanceled= isCanceled;
		}
	}

	/**
	 * @param document the document
	 * @return the document's lock object
	 */
	private Object getLockObject(IDocument document) {
		if (document instanceof ISynchronizable) {
			Object lock= ((ISynchronizable)document).getLockObject();
			if (lock != null)
				return lock;
		}
		return document;
	}

	/**
	 * Install this presenter on the given source viewer and background presentation
	 * reconciler.
	 *
	 * @param sourceViewer the source viewer
	 * @param backgroundPresentationReconciler the background presentation reconciler,
	 * 	can be <code>null</code>, in that case {@link SemanticHighlightingPresenter#createPresentation(List, List)}
	 * 	should not be called
	 */
	public void install(JavaSourceViewer sourceViewer, JavaPresentationReconciler backgroundPresentationReconciler) {
		fSourceViewer= sourceViewer;
		fPresentationReconciler= backgroundPresentationReconciler;

		fSourceViewer.prependTextPresentationListener(this);
		fSourceViewer.addTextInputListener(this);
		manageDocument(fSourceViewer.getDocument());
	}

	/**
	 * Uninstall this presenter.
	 */
	public void uninstall() {
		setCanceled(true);

		if (fSourceViewer != null) {
			fSourceViewer.removeTextPresentationListener(this);
			releaseDocument(fSourceViewer.getDocument());
			invalidateTextPresentation();
			resetState();

			fSourceViewer.removeTextInputListener(this);
			fSourceViewer= null;
		}
	}

	/**
	 * Invalidate text presentation of positions with the given highlighting.
	 *
	 * @param highlighting The highlighting
	 */
	public void highlightingStyleChanged(Highlighting highlighting) {
		for (Position fPosition : fPositions) {
			HighlightedPosition position= (HighlightedPosition) fPosition;
			if (position.getHighlighting() == highlighting)
				fSourceViewer.invalidateTextPresentation(position.getOffset(), position.getLength());
		}
	}

	/**
	 * Invalidate text presentation of all positions.
	 */
	private void invalidateTextPresentation() {
		for (Position position : fPositions) {
			fSourceViewer.invalidateTextPresentation(position.getOffset(), position.getLength());
		}
	}

	/**
	 * Add a position with the given range and highlighting unconditionally, only from UI thread.
	 * The position will also be registered on the document. The text presentation is not
	 * invalidated.
	 *
	 * @param offset The range offset
	 * @param length The range length
	 * @param highlighting the highlighting
	 */
	private void addPositionFromUI(int offset, int length, Highlighting highlighting) {
		Position position= createHighlightedPosition(offset, length, highlighting);
		synchronized (fPositionLock) {
			insertPosition(position);
		}

		IDocument document= fSourceViewer.getDocument();
		if (document == null)
			return;
		String positionCategory= getPositionCategory();
		try {
			document.addPosition(positionCategory, position);
		} catch (BadLocationException | BadPositionCategoryException e) {
			// Should not happen
			JavaPlugin.log(e);
		}
	}

	/**
	 * Reset to initial state.
	 */
	private void resetState() {
		synchronized (fPositionLock) {
			fPositions.clear();
		}
	}

	/**
	 * Start managing the given document.
	 *
	 * @param document The document
	 */
	private void manageDocument(IDocument document) {
		if (document != null) {
			document.addPositionCategory(getPositionCategory());
			document.addPositionUpdater(fPositionUpdater);
			document.addDocumentListener(this);
		}
	}

	/**
	 * Stop managing the given document.
	 *
	 * @param document The document
	 */
	private void releaseDocument(IDocument document) {
		if (document != null) {
			document.removeDocumentListener(this);
			document.removePositionUpdater(fPositionUpdater);
			try {
				document.removePositionCategory(getPositionCategory());
			} catch (BadPositionCategoryException e) {
				// Should not happen
				JavaPlugin.log(e);
			}
		}
	}

	/**
	 * @return The semantic reconciler position's category.
	 */
	@Override
	public String getPositionCategory() {
		return toString();
	}
}
