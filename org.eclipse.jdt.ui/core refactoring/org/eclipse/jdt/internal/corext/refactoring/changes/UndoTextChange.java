/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.UndoMemento;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

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

