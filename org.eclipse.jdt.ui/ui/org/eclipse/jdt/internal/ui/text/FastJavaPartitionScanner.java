/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text;


import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * This scanner recognizes the JavaDoc comments, Java multi line comments, Java single line comments,
 * Java strings and Java characters.
 */
public class FastJavaPartitionScanner extends AbstractFastJavaPartitionScanner {
	public FastJavaPartitionScanner(boolean emulate) {
		super(emulate);
	}

	public FastJavaPartitionScanner() {
	    super(false);
	}

	public FastJavaPartitionScanner(IJavaProject javaProject) {
		super(javaProject);
	}

	@Override
	protected void setJavaProject() {
		if (getJavaProject() == null) {
			IWorkbenchPage page= null;
			try {
				page= JavaPlugin.getActivePage();
			} catch (IllegalStateException e) {
				//do nothing
			}
			if (page != null) {
				IEditorPart part= page.getActiveEditor();
				if (part != null) {
					IEditorInput editorInput= part.getEditorInput();
					if (editorInput != null) {
						setJavaProject(EditorUtility.getJavaProject(editorInput));
					}
				}
				if (getJavaProject() == null) {
					ISelection selection= page.getSelection();
					if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
						Object obj= ((IStructuredSelection) selection).getFirstElement();
						if (obj instanceof IJavaElement) {
							setJavaProject(((IJavaElement) obj).getJavaProject());
						}
					}
				}
			}
		}
	}
}
