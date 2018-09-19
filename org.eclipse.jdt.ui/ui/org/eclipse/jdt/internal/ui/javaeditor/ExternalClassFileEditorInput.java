/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.core.resources.IFile;

import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;


/**
 * Editor input for .class files on the file system.
 */
public class ExternalClassFileEditorInput extends FileEditorInput implements IClassFileEditorInput {

	private IClassFile fClassFile;

	ExternalClassFileEditorInput(IFile file) {
		super(file);
		refresh();
	}

	/*
	 * @see IClassFileEditorInput#getClassFile()
	 */
	@Override
	public IClassFile getClassFile() {
		return fClassFile;
	}

	/**
	 * Refreshes this input element. Workaround for non-updating class file elements.
	 */
	public void refresh() {
		Object element= JavaCore.create(getFile());
		if (element instanceof IClassFile)
			fClassFile= (IClassFile) element;
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IClassFile.class)
			return (T) fClassFile;
		return super.getAdapter(adapter);
	}

}
