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

package org.eclipse.text.reconcilerpipe;

import org.eclipse.jface.text.IDocument;

/**
 * Adapts an IDocument to an ITextModel.
 *
 * @since 3.0
 */
public class TextModelAdapter implements ITextModel {

	private IDocument fDocument;

	/**
	 * Creates a text model adapter for the given document.
	 * 
	 * @param document
	 */
	public TextModelAdapter(IDocument document) {
		fDocument= document;
	}

	/**
	 * Returns this model's document.
	 *
	 * @return the model's input document
	 */
	public IDocument getDocument() {
		return fDocument;
	}
}
