/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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


import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.JarEntryEditorInputFactory;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

/**
 * An EditorInput for a JarEntryFile.
 */
public class JarEntryEditorInput implements IStorageEditorInput {

	private final IStorage fJarEntryFile;

	public JarEntryEditorInput(IStorage jarEntryFile) {
		Assert.isNotNull(jarEntryFile);
		fJarEntryFile= jarEntryFile;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof JarEntryEditorInput))
			return false;
		JarEntryEditorInput other= (JarEntryEditorInput) obj;
		return fJarEntryFile.equals(other.fJarEntryFile);
	}

	@Override
	public int hashCode() {
		return fJarEntryFile.hashCode();
	}

	/*
	 * @see IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		if (fJarEntryFile instanceof IJarEntryResource) {
			return new IPersistableElement() {
				@Override
				public void saveState(IMemento memento) {
					JarEntryEditorInputFactory.saveState(memento, (IJarEntryResource) fJarEntryFile);
				}

				@Override
				public String getFactoryId() {
					return JarEntryEditorInputFactory.FACTORY_ID;
				}
			};
		} else {
			return null;
		}
	}

	/*
	 * @see IEditorInput#getName()
	 */
	@Override
	public String getName() {
		return fJarEntryFile.getName();
	}

	/*
	 * @see IEditorInput#getContentType()
	 */
	public String getContentType() {
		return fJarEntryFile.getFullPath().getFileExtension();
	}

	/*
	 * @see IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		if (fJarEntryFile instanceof IJarEntryResource) {
			IJarEntryResource jarEntry= (IJarEntryResource)fJarEntryFile;
			IPackageFragmentRoot root= jarEntry.getPackageFragmentRoot();
			IPath fullPath= root.getPath().append(fJarEntryFile.getFullPath());
			return BasicElementLabels.getPathLabel(fullPath, root.isExternal());
		}

		IPath fullPath= fJarEntryFile.getFullPath();
		if (fullPath == null)
			return null;
		return BasicElementLabels.getPathLabel(fullPath, false);
	}

	/*
	 * @see IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
		return registry.getImageDescriptor(getContentType());
	}

	/*
	 * @see IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		// JAR entries can't be deleted
		return true;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	/*
	 * see IStorageEditorInput#getStorage()
	 */
	 @Override
	public IStorage getStorage() {
	 	return fJarEntryFile;
	 }
}


