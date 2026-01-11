/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Class file considered as editor input.
 */
public class InternalClassFileEditorInput implements IClassFileEditorInput, IPersistableElement, IPathEditorInput {

	private IClassFile fClassFile;

	private volatile IPath fPath;

	public InternalClassFileEditorInput(IClassFile classFile) {
		fClassFile= classFile;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof InternalClassFileEditorInput))
			return false;
		InternalClassFileEditorInput other= (InternalClassFileEditorInput) obj;
		return fClassFile.equals(other.fClassFile);
	}

	@Override
	public int hashCode() {
		return fClassFile.hashCode();
	}

	@Override
	public IClassFile getClassFile() {
		return fClassFile;
	}

	@Override
	public IPersistableElement getPersistable() {
		return this;
	}

	@Override
	public String getName() {
		return fClassFile.getElementName();
	}

	@Override
	public String getToolTipText() {
		if (fClassFile instanceof IOrdinaryClassFile) {
			return ((IOrdinaryClassFile) fClassFile).getType().getFullyQualifiedName();
		} else if (fClassFile instanceof IModularClassFile) {
			try {
				return fClassFile.getModule().getElementName();
			} catch (JavaModelException e) {
				return e.getMessage();
			}
		}
		throw new IllegalStateException("input must be either IClassFile or IModularClassFile");  //$NON-NLS-1$
	}

	@Override
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

	@Override
	public boolean exists() {
		return fClassFile.exists();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IClassFile.class
				|| adapter == IJavaElement.class) {
			return (T) fClassFile;
		}
		return null;
	}

	@Override
	public void saveState(IMemento memento) {
		ClassFileEditorInputFactory.saveState(memento, this);
	}

	@Override
	public String getFactoryId() {
		return ClassFileEditorInputFactory.ID;
	}

	@Override
	public IPath getPath() {
		if (fPath == null)
			fPath= writeToTempFile(fClassFile);
		return fPath;
	}

	private static IPath writeToTempFile(IClassFile classFile) {
		try {
			File file= File.createTempFile(classFile.getElementName(), ".class"); //$NON-NLS-1$
			file.deleteOnExit();
			byte[] bytes= classFile.getBytes();
			Files.write(file.toPath(), bytes);
			return new Path(file.toString());
		} catch (IOException e) {
			JavaPlugin.log(e);
		} catch (JavaModelException ex) {
			if (!ex.isDoesNotExist()) {
				JavaPlugin.log(ex.getStatus());
			}
		}
		return IPath.EMPTY;
	}

}


