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

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IPersistableElement;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;

/**
 * An editor input that is returned in case of an {@link JavaModelException} is thrown while trying to gather the actual input
 */
class ExceptionalEditorInput implements IClassFileEditorInput {


	private String fIdentifier;
	private Exception fException;
	private IPersistableElement fElement;

	public ExceptionalEditorInput(String identifier, IPersistableElement element, Exception modelException) {
		fIdentifier= identifier;
		fElement= element;
		fException= modelException;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageDescriptor.getMissingImageDescriptor();
	}

	@Override
	public String getName() {
		return fIdentifier;
	}

	@Override
	public IPersistableElement getPersistable() {
		return fElement;
	}

	@Override
	public String getToolTipText() {
		return String.format("Error while gathering input for %s: %s", fIdentifier, fException); //$NON-NLS-1$
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IPersistableElement.class) {
			return adapter.cast(getPersistable());
		}
		return null;
	}

	@Override
	public IClassFile getClassFile() {
		if (fException instanceof RuntimeException rte) {
			throw rte;
		}
		throw new RuntimeException(getToolTipText(), fException);
	}
}
