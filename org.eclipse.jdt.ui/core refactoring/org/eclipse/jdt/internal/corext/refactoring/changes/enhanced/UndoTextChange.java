/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes.enhanced;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.enhanced.UndoMemento;

abstract class UndoTextChange extends AbstractTextChange {

	private UndoMemento fUndos;

	public UndoTextChange(String name, int changeKind, UndoMemento undos) {
		super(name, changeKind);
		fUndos= undos;
	}
	
	protected void addTextEdits(TextBufferEditor editor, boolean copy) throws CoreException {
		editor.add(fUndos);
	}	
}

