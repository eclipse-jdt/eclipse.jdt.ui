/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.javaeditor.JarEntryEditorInput;

public class JavaFileLinkHelper implements ILinkHelper {

	public void activateEditor(IWorkbenchPage page, IStructuredSelection selection) {
		if (selection == null || selection.isEmpty())
			return;
		Object element= selection.getFirstElement();
		IEditorPart part= EditorUtility.isOpenInEditor(element);
		if (part != null) {
			page.bringToTop(part);
			if (element instanceof IJavaElement)
				EditorUtility.revealInEditor(part, (IJavaElement) element);
		}

	}

	public IStructuredSelection findSelection(IEditorInput input) {
		Object javaElement= null;
		if (input instanceof IClassFileEditorInput)
			javaElement= ((IClassFileEditorInput) input).getClassFile();
		else if (input instanceof IFileEditorInput) {
			IFile file= ((IFileEditorInput) input).getFile();
			javaElement= JavaCore.create(file);
		} else if (input instanceof JarEntryEditorInput)
			javaElement= ((JarEntryEditorInput) input).getStorage();

		return (javaElement != null) ? new StructuredSelection(javaElement) : StructuredSelection.EMPTY;
	}

}
