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
package org.eclipse.jdt.ui.search;

import org.eclipse.swt.graphics.Image;

import org.eclipse.ui.PartInitException;

import org.eclipse.search.ui.text.Match;

/**
 * This interface serves to display elements that a search participant has contributed to a search
 * result.
 * Clients may implement this interface.
 * Each <code>ISearchUIParticipant</code> is associated with a particular <code>IQueryParticipant</code>. The ISearchUIParticipant
 * will only be asked to handle elements and matches which its <code>IQueryParticipant</code> contributed to the 
 * search result. If two search participants report matches against the same element, one of them will
 * be chosen to handle the element.
 * 
 */
public interface ISearchUIParticipant {

	/**
	 * Returns the image for the label of the given element. 
	 *
	 * @param element the element for which to provide the label image
	 * @return the image used to label the element, or <code>null</code>
	 *   if there is no image for the given object
	 */
	public Image getImage(Object element);

	/**
	 * Returns the text for the label of the given element.
	 *
	 * @param element the element for which to provide the label text
	 * @return the text string used to label the element, or <code>null</code>
	 *   if there is no text label for the given object
	 */
	public String getText(Object element);

	/**
	 * Opens an editor on the given element and selects the given range of text.
	 * The location of matches are automatically updated when a file is edited
	 * through the file buffer infrastructure (see {@link org.eclipse.core.filebuffers.ITextFileBufferManager}). 
	 * When a file buffer is saved, the current positions are written back to the 
	 * match.
	 * 
	 * @param match
	 *            The match to show
	 * @param currentOffset
	 *            The current start offset of the match
	 * @param currentLength
	 *            The current length of the selection
	 * @throws PartInitException
	 *             If an editor can't be opened.
	 */
	void showMatch(Match match, int currentOffset, int currentLength) throws PartInitException;
}
