/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.reconciler.IReconcilableModel;

/**
 * Adapts an <code>IDocument</code> to a <code>IReconcilableModel</code>.
 *
 * @since 3.0
 */
public class DocumentAdapter implements IReconcilableModel {

	private IDocument fDocument;

	/**
	 * Creates a text model adapter for the given document.
	 * 
	 * @param document
	 */
	public DocumentAdapter(IDocument document) {
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
