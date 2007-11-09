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
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;

/** 
 * The JarEntryEditorInputFactory is used to save and recreate {@link JarEntryEditorInput}s.
 * 
 * @see IMemento
 * @see IElementFactory
 */
public class JarEntryEditorInputFactory implements IElementFactory {

	public static final String FACTORY_ID= "org.eclipse.jdt.ui.internal.JarEntryEditorInputFactory"; //$NON-NLS-1$
	private static final String KEY_ROOT= "packageFragmentRoot"; //$NON-NLS-1$
	private static final String KEY_PATH= "path"; //$NON-NLS-1$

	/**
	 * Public constructor for extension point.  
	 */
	public JarEntryEditorInputFactory() {
	}

	/*
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	public IAdaptable createElement(IMemento memento) {
	
		String rootIdentifier= memento.getString(KEY_ROOT);
		String pathIdentifier= memento.getString(KEY_PATH);
		if (rootIdentifier != null && pathIdentifier != null) {
			IJavaElement restoredRoot= JavaCore.create(rootIdentifier);
			if (!(restoredRoot instanceof IPackageFragmentRoot))
				return null;
			
			IPackageFragmentRoot root= (IPackageFragmentRoot) restoredRoot;
			String[] pathSegments= new Path(pathIdentifier).segments();
			try {
				Object[] children= root.getNonJavaResources();
				int depth= pathSegments.length;
				segments: for (int i= 0; i < depth; i++) {
					String name= pathSegments[i];
					for (int j= 0; j < children.length; j++) {
						Object child= children[j];
						if (child instanceof IJarEntryResource) {
							IJarEntryResource jarEntryResource= (IJarEntryResource) child;
							if (name.equals(jarEntryResource.getName())) {
								boolean isFile= jarEntryResource.isFile();
								if (isFile) {
									if (i == depth - 1) {
										return new JarEntryEditorInput(jarEntryResource);
									} else {
										return null; // got a file for a directory name
									}
								} else {
									children= jarEntryResource.getChildren();
									continue segments;
								}
							}
						}
					}
					return null; // no child found on this level
				}
			} catch (JavaModelException e) {
				return null;
			}
		}
		return null;
	}
	
	/*
	 * @see IPersistableElement#saveState(IMemento)
	 */
	public static void saveState(IMemento memento, IJarEntryResource jarEntryResource) {
		memento.putString(KEY_ROOT, jarEntryResource.getPackageFragmentRoot().getHandleIdentifier());
		memento.putString(KEY_PATH, jarEntryResource.getFullPath().toString());
	}
}
