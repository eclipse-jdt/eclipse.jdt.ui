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
package org.eclipse.text.edits;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

public abstract class SimpleTextEdit extends TextEdit {

	protected SimpleTextEdit(int offset, int length) {
		super(offset, length);
	}
	
	/**
	 * Copy constructor
	 */
	protected SimpleTextEdit(SimpleTextEdit other) {
		super(other);
	}
	
	/**
	 * Returns the new text inserted at the range denoted
	 * by this edit. 
	 * 
	 * @return the edit's text.
	 */
	public abstract String getText();
	
	
	/* non Java-doc
	 * @see TextEdit#doPerform
	 */
	/* package */ void perform(IDocument document) throws PerformEditException {
		String text= getText(); 		
		if (text != null)
			performReplace(document, text);
	}
	
	/* package */ void update(DocumentEvent event, TreeIterationInfo info) {
		markChildrenAsDeleted();
		super.update(event, info);
	}	
}

