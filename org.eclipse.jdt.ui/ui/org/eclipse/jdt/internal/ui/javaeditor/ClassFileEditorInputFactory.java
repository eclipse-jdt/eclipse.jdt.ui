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

import org.eclipse.jdt.core.IClassFile;

/**
 * The factory which is capable of recreating class file editor
 * inputs stored in a memento.
 */
public class ClassFileEditorInputFactory implements IElementFactory {

	public final static String ID=  "org.eclipse.jdt.ui.ClassFileEditorInputFactory"; //$NON-NLS-1$
	public final static String KEY= "org.eclipse.jdt.ui.ClassFileIdentifier"; //$NON-NLS-1$
	public final static String KEY_NAME= KEY + ".name"; //$NON-NLS-1$

	public ClassFileEditorInputFactory() {
	}

	@Override
	public IAdaptable createElement(IMemento memento) {
		String identifier= memento.getString(KEY);
		if (identifier == null) {
			return null;
		}
		String name= memento.getString(KEY_NAME);
		return new HandleEditorInput(identifier, name == null? "" : name); //$NON-NLS-1$
	}

	public static void saveState(IMemento memento, InternalClassFileEditorInput input) {
		IClassFile c= input.getClassFile();
		memento.putString(KEY, c.getHandleIdentifier());
		memento.putString(KEY_NAME, input.getName());
	}
}
