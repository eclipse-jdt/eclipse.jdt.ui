/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

/**
 * An {@link IEditorInput} that carries a JDT handle that can be transformed into an JavaElement
 */
public class HandleEditorInput implements IEditorInput, IPersistableElement {

	private String fHandleIdentifier;

	public HandleEditorInput(String handleIdentifier) {
		fHandleIdentifier= handleIdentifier;
	}

	public IJavaElement getElement() throws CoreException {
		IJavaElement element= JavaCore.create(fHandleIdentifier);
		if (element == null) {
			throw new CoreException(Status.error("Handle not found: " + fHandleIdentifier)); //$NON-NLS-1$
		}
		return element;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return "Java Handle Identifier Input"; //$NON-NLS-1$
	}

	@Override
	public IPersistableElement getPersistable() {
		return this;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString(ClassFileEditorInputFactory.KEY, fHandleIdentifier);

	}

	@Override
	public String getFactoryId() {
		return ClassFileEditorInputFactory.ID;
	}

}
