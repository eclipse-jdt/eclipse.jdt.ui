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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.UndoMemento;

abstract class UndoTextChange extends AbstractTextChange {

	private UndoMemento fUndos;

	public UndoTextChange(String name, int changeKind, UndoMemento undos) {
		super(name, changeKind);
		fUndos= undos;
	}
	
	protected void addTextEdits(LocalTextEditProcessor editor) throws CoreException {
		editor.add(fUndos);
	}	
}

