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
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

public abstract class SimpleTextEdit extends TextEdit {

	public static SimpleTextEdit createReplace(int offset, int length, String text) {
		return new ReplaceEdit(offset, length, text);
	}

	public static SimpleTextEdit createInsert(int offset, String text) {
		return new InsertEdit(offset, text);
	}
	
	public static SimpleTextEdit createDelete(int offset, int length) {
		return new DeleteEdit(offset, length);
	}
	
	public SimpleTextEdit(int offset, int length) {
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
	protected void perform(IDocument document) throws PerformEditException {
		String text= getText(); 		
		if (text != null)
			performReplace(document, text);
	}
	
	/* non Java-doc
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String text= getText();
		if (text == null)
			return super.toString() +" NOP"; //$NON-NLS-1$
		return super.toString() + " <<" + text; //$NON-NLS-1$
	}
	
	/* package */ void update(DocumentEvent event, TreeIterationInfo info) {
		markChildrenAsDeleted();
		super.update(event, info);
	}	
}

