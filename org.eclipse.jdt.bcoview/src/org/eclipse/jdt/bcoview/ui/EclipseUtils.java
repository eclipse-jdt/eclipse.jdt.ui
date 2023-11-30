/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.ui;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

@SuppressWarnings("restriction")
public class EclipseUtils {

	/**
	 * don't call me ;)
	 */
	private EclipseUtils() {
		super();
	}

	/**
	 * @return current active editor in workbench
	 */
	public static IEditorPart getActiveEditor() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				return page.getActiveEditor();
			}
		}
		return null;
	}

	public static IJavaElement getJavaInput(IEditorPart part) {
		IJavaElement input = part.getAdapter(IJavaElement.class);
		if (input != null) {
			return input;
		}
		IEditorInput editorInput = part.getEditorInput();
		if (editorInput != null) {
			input = editorInput.getAdapter(IJavaElement.class);
		}
		if (input != null) {
			return input;
		}
		input = EditorUtility.getEditorInputJavaElement(part, false);
		return input;
	}

	public static void selectInEditor(ITextEditor editor, int offset, int length) {
		IEditorPart active = getActiveEditor();
		if (active != editor) {
			editor.getSite().getPage().activate(editor);
		}
		editor.selectAndReveal(offset, length);
	}

	/**
	 * @param selectionProvider non null
	 * @return TextSelection or null, if provider does not provide TextSelection's
	 */
	public static ITextSelection getSelection(ISelectionProvider selectionProvider) {
		ISelection selection = selectionProvider.getSelection();
		if (selection instanceof ITextSelection) {
			return (ITextSelection) selection;
		}
		return null;
	}

	/**
	 * @param resource can be null
	 * @return full package name in default java notation (with dots) or empty string
	 */
	public static String getJavaPackageName(IJavaElement resource) {
		String name;
		if (resource == null) {
			return ""; //$NON-NLS-1$
		}
		name = resource.getElementName();
		if (name == null) {
			return ""; //$NON-NLS-1$
		}
		int type = resource.getElementType();
		if (type == IJavaElement.PACKAGE_FRAGMENT || type == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
			return name;
		}
		IJavaElement ancestor = resource.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		if (ancestor != null) {
			return ancestor.getElementName();
		}
		return ""; //$NON-NLS-1$
	}

}
