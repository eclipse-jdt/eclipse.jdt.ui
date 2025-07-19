/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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


import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * The factory which is capable of recreating class file editor
 * inputs stored in a memento.
 */
public class ClassFileEditorInputFactory implements IElementFactory {

	public final static String ID=  "org.eclipse.jdt.ui.ClassFileEditorInputFactory"; //$NON-NLS-1$
	public final static String KEY= "org.eclipse.jdt.ui.ClassFileIdentifier"; //$NON-NLS-1$

	public ClassFileEditorInputFactory() {
	}

	/*
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	@Override
	public IAdaptable createElement(IMemento memento) {
		String identifier= memento.getString(KEY);
		if (identifier == null) {
			return new ExceptionalEditorInput("memento data", new IPersistableElement() { //$NON-NLS-1$

				@Override
				public void saveState(IMemento m) {
					//nothing to save...
				}

				@Override
				public String getFactoryId() {
					return ID;
				}
			}, new IllegalStateException(String.format("No %s present in memento", KEY))); //$NON-NLS-1$
		}
		IJavaElement element= JavaCore.create(identifier);
		try {
			if (!element.exists() && element instanceof IOrdinaryClassFile) {
				/*
				 * Let's try to find the class file,
				 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=83221
				 */
				IOrdinaryClassFile cf= (IOrdinaryClassFile)element;
				IType type= cf.getType();
				IJavaProject project= element.getJavaProject();
				if (project != null) {
					type= project.findType(type.getFullyQualifiedName());
					if (type != null) {
						return EditorUtility.getEditorInput(type.getParent());
					}
				}
				return new OrdinaryClassFileEditorInput(cf);
			}
			return EditorUtility.getEditorInput(element);
		} catch (JavaModelException x) {
			return new ExceptionalEditorInput(identifier, new IPersistableElement() {

				@Override
				public void saveState(IMemento m) {
					m.putString(KEY, identifier);

				}

				@Override
				public String getFactoryId() {
					return ID;
				}
			}, x);
		}
	}

	public static void saveState(IMemento memento, InternalClassFileEditorInput input) {
		IClassFile c= input.getClassFile();
		memento.putString(KEY, c.getHandleIdentifier());
	}
}
