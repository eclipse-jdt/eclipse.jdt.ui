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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import org.eclipse.jdt.internal.corext.Assert;

/* package */ class Updater implements IDocumentListener {

	protected UndoMemento undo= new UndoMemento();
	
	public static Updater createUndoUpdater() {
		return new Updater();
	}

	public static DoUpdater createDoUpdater() {
		return new DoUpdater();
	}
		
	public static class DoUpdater extends Updater {
		private TextEdit fActiveEdit;
		private TreeIterationInfo fIterationInfo= new TreeIterationInfo();
		
		public void push(TextEdit[] edits) {
			fIterationInfo.push(edits);
		}
		public void setIndex(int index) {
			fIterationInfo.setIndex(index);
		}
		public void pop() {
			fIterationInfo.pop();
		}
		public void setActiveEdit(TextEdit edit) {
			fActiveEdit= edit;
		}
		public void documentChanged(DocumentEvent event) {
			fActiveEdit.update(event, fIterationInfo);
		}
	}

	protected Updater() {
	}

	public void documentAboutToBeChanged(DocumentEvent event) {
		int offset= event.getOffset();
		int currentLength= event.getLength();
		String currentText= null;
		try {
			currentText= event.getDocument().get(offset, currentLength);
		} catch (BadLocationException cannotHappen) {
			Assert.isTrue(false, "Can't happen"); //$NON-NLS-1$
		}

		String newText= event.getText();
		int newLength= newText.length();

		if (currentLength > 0 && newLength == 0) {
			// Delete edit
			undo.add(SimpleTextEdit.createInsert(offset, currentText));
		} else if (currentLength == 0 && newLength > 0) {
			// insert edit
			undo.add(SimpleTextEdit.createDelete(offset, newLength));
		} else {
			// replace edit
			undo.add(SimpleTextEdit.createReplace(offset, newLength, currentText));
		}
	}
	
	public void documentChanged(DocumentEvent event) {
	}	
}
