/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.codemanipulation.IUndoTextEdits;
import org.eclipse.jdt.internal.core.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.core.codemanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

abstract class UndoTextChange extends AbstractTextChange {

	private IUndoTextEdits fUndoTextEdits;

	public UndoTextChange(String name, int changeKind, IUndoTextEdits undos) {
		super(name, changeKind);
		fUndoTextEdits= undos;
	}
	
	protected void addTextEdits(TextBufferEditor editor, boolean copy) throws CoreException {
		fUndoTextEdits.addTo(editor);
	}	
}

