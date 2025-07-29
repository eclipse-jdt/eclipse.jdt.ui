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

import java.util.Objects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * An {@link IEditorInput} that carries a JDT handle that can be transformed into an JavaElement
 *
 * @see ClassFileEditorInputFactory#createElement(IMemento)
 * @see ClassFileEditor#transformEditorInput(IEditorInput)
 */
public class HandleEditorInput implements IEditorInput, IPersistableElement {

	private final String handleIdentifier;
	private final String name;

	public HandleEditorInput(String handleIdentifier, String name) {
		Assert.isNotNull(handleIdentifier);
		Assert.isNotNull(name);
		this.handleIdentifier= handleIdentifier;
		this.name= name;
	}

	public IJavaElement getElement() throws CoreException {
		IJavaElement element= JavaCore.create(handleIdentifier);
		if (element == null) {
			throw new CoreException(Status.error("Unable to restore Java element from: " + handleIdentifier)); //$NON-NLS-1$
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
		return JavaPluginImages.DESC_OBJS_CFILE;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IPersistableElement getPersistable() {
		return this;
	}

	@Override
	public String getToolTipText() {
		return name;
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString(ClassFileEditorInputFactory.KEY, handleIdentifier);
		memento.putString(ClassFileEditorInputFactory.KEY_NAME, name);
	}

	@Override
	public String getFactoryId() {
		return ClassFileEditorInputFactory.ID;
	}

	public String getHandleIdentifier() {
		return handleIdentifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(handleIdentifier, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HandleEditorInput)) {
			return false;
		}
		HandleEditorInput other= (HandleEditorInput) obj;
		return Objects.equals(handleIdentifier, other.handleIdentifier) && Objects.equals(name, other.name);
	}
}
