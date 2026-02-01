/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import java.util.List;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Tracks dirty (modified) lines in a document to support "format edited lines" functionality.
 * This class maintains a set of line numbers that have been modified since the last format/save,
 * automatically adjusting them when lines are inserted or deleted to prevent race conditions.
 *
 * @since 3.40
 */
public class DocumentDirtyTracker implements IDocumentListener {

	/** WeakHashMap to associate trackers with documents without requiring API changes */
	private static final WeakHashMap<IDocument, DocumentDirtyTracker> trackers = new WeakHashMap<>();

	/** Set of dirty line numbers, maintained in sorted order */
	private final TreeSet<Integer> dirtyLines = new TreeSet<>();

	/** The document being tracked */
	private final IDocument document;

	/**
	 * Gets or creates a DocumentDirtyTracker for the given document.
	 *
	 * @param document the document to track
	 * @return the tracker for this document
	 */
	public static synchronized DocumentDirtyTracker get(IDocument document) {
		if (document == null) {
			throw new IllegalArgumentException("Document cannot be null"); //$NON-NLS-1$
		}
		return trackers.computeIfAbsent(document, DocumentDirtyTracker::new);
	}

	/**
	 * Private constructor - use {@link #get(IDocument)} instead.
	 *
	 * @param document the document to track
	 */
	private DocumentDirtyTracker(IDocument document) {
		this.document = document;
		document.addDocumentListener(this);
	}

	/**
	 * Returns the dirty regions (ranges of consecutive dirty lines).
	 * This method is safe to call concurrently and returns regions that are
	 * always valid for the current document state.
	 *
	 * @return array of regions representing dirty lines, or null if no lines are dirty
	 */
	public synchronized IRegion[] getDirtyRegions() {
		if (dirtyLines.isEmpty()) {
			return null;
		}

		List<IRegion> regions = new ArrayList<>();
		Integer startLine = null;
		Integer previousLine = null;

		for (Integer line : dirtyLines) {
			if (startLine == null) {
				// Start of a new region
				startLine = line;
				previousLine = line;
			} else if (line == previousLine + 1) {
				// Consecutive line - extend the current region
				previousLine = line;
			} else {
				// Gap found - close current region and start a new one
				try {
					regions.add(createRegion(startLine, previousLine));
				} catch (BadLocationException e) {
					// Line was deleted or invalid - skip this region
				}
				startLine = line;
				previousLine = line;
			}
		}

		// Add the last region
		if (startLine != null) {
			try {
				regions.add(createRegion(startLine, previousLine));
			} catch (BadLocationException e) {
				// Line was deleted or invalid - skip this region
			}
		}

		return regions.isEmpty() ? null : regions.toArray(new IRegion[regions.size()]);
	}

	/**
	 * Creates a region from a range of line numbers.
	 *
	 * @param startLine the first line (inclusive)
	 * @param endLine the last line (inclusive)
	 * @return the region covering these lines
	 * @throws BadLocationException if the lines are invalid
	 */
	private IRegion createRegion(int startLine, int endLine) throws BadLocationException {
		IRegion startLineInfo = document.getLineInformation(startLine);
		IRegion endLineInfo = document.getLineInformation(endLine);

		int offset = startLineInfo.getOffset();
		int length = endLineInfo.getOffset() + endLineInfo.getLength() - offset;

		return new Region(offset, length);
	}

	/**
	 * Clears all dirty line markers. Should be called after successful formatting.
	 */
	public synchronized void clearDirtyLines() {
		dirtyLines.clear();
	}

	/**
	 * Marks specific lines as dirty.
	 *
	 * @param lines the line numbers to mark as dirty
	 */
	public synchronized void markLinesDirty(int... lines) {
		for (int line : lines) {
			if (line >= 0) {
				dirtyLines.add(line);
			}
		}
	}

	@Override
	public synchronized void documentAboutToBeChanged(DocumentEvent event) {
		// Nothing to do before change
	}

	@Override
	public synchronized void documentChanged(DocumentEvent event) {
		try {
			// Get the line numbers affected by this change
			int offset = event.getOffset();
			int length = event.getLength();
			String text = event.getText();

			int startLine = document.getLineOfOffset(offset);
			int linesAdded = text != null ? countLines(text) : 0;
			int linesRemoved = length > 0 ? document.getLineOfOffset(offset + length) - startLine : 0;

			// Mark changed lines as dirty
			for (int i = 0; i <= linesAdded; i++) {
				dirtyLines.add(startLine + i);
			}

			// Adjust line numbers if lines were added or removed
			int netChange = linesAdded - linesRemoved;
			if (netChange != 0) {
				adjustLineNumbers(startLine + 1, netChange);
			}

		} catch (BadLocationException e) {
			// If we can't determine the line, ignore this change
		}
	}

	/**
	 * Adjusts line numbers after a given line when lines are inserted or deleted.
	 *
	 * @param afterLine the line after which to adjust
	 * @param delta the number of lines added (positive) or removed (negative)
	 */
	private void adjustLineNumbers(int afterLine, int delta) {
		// Get all lines after the change point
		TreeSet<Integer> linesToAdjust = new TreeSet<>(dirtyLines.tailSet(afterLine));

		// Remove and re-add with adjusted line numbers
		dirtyLines.removeAll(linesToAdjust);
		for (Integer line : linesToAdjust) {
			int newLine = line + delta;
			if (newLine >= 0) {
				dirtyLines.add(newLine);
			}
		}
	}

	/**
	 * Counts the number of newline characters in a string.
	 *
	 * @param text the text to analyze
	 * @return the number of newlines (0 if text is null or empty)
	 */
	private int countLines(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}

		int count = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				count++;
			}
		}
		return count;
	}

	/**
	 * Removes the tracker from the document (cleanup).
	 * Should be called when the document is no longer needed.
	 */
	public synchronized void dispose() {
		document.removeDocumentListener(this);
		synchronized (DocumentDirtyTracker.class) {
			trackers.remove(document);
		}
	}
}
