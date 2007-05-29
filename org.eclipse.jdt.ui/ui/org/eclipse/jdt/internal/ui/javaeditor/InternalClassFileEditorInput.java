/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;


import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * Class file considered as editor input.
 */
public class InternalClassFileEditorInput implements IClassFileEditorInput, IPersistableElement {

	private IClassFile fClassFile;

	public InternalClassFileEditorInput(IClassFile classFile) {
		fClassFile= classFile;
	}

	/*
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof InternalClassFileEditorInput))
			return false;
		InternalClassFileEditorInput other= (InternalClassFileEditorInput) obj;
		return fClassFile.equals(other.fClassFile);
	}

	/*
	 * @see Object#hashCode
	 */
	public int hashCode() {
		return fClassFile.hashCode();
	}

	/*
	 * @see IClassFileEditorInput#getClassFile()
	 */
	public IClassFile getClassFile() {
		return fClassFile;
	}

	/*
	 * @see IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return this;
	}

	/*
	 * @see IEditorInput#getName()
	 */
	public String getName() {
		return fClassFile.getElementName();
	}

	/*
	 * @see IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return fClassFile.getType().getFullyQualifiedName();
	}

	/*
	 * @see IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		try {
			if (fClassFile.isClass())
				return JavaPluginImages.DESC_OBJS_CFILECLASS;
			return JavaPluginImages.DESC_OBJS_CFILEINT;
		} catch (JavaModelException e) {
			// fall through
		}
		return JavaPluginImages.DESC_OBJS_CFILE;
	}

	/*
	 * @see IEditorInput#exists()
	 */
	public boolean exists() {
		return fClassFile.exists();
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IClassFile.class)
			return fClassFile;
		return fClassFile.getAdapter(adapter);
	}

	/*
	 * @see IPersistableElement#saveState(IMemento)
	 */
	public void saveState(IMemento memento) {
		ClassFileEditorInputFactory.saveState(memento, this);
	}

	/*
	 * @see IPersistableElement#getFactoryId()
	 */
	public String getFactoryId() {
		return ClassFileEditorInputFactory.ID;
	}
}


