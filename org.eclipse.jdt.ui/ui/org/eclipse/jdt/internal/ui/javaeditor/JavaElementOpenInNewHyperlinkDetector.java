/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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

import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;


/**
 * Java element open in new tab hyperlink detector for types and methods.
 */
public class JavaElementOpenInNewHyperlinkDetector extends JavaElementHyperlinkDetector {

	@Override
	protected void addHyperlinks(List<IHyperlink> hyperlinksCollector, IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		if (isDeclaredInCurrentEditor(element, editor)) {
			hyperlinksCollector.add(new JavaElementOpenInNewImplementationHyperlink(wordRegion, element, editor, qualify));
		}
	}

	private boolean isDeclaredInCurrentEditor(IJavaElement typeOrMethod, IEditorPart editor) {
		int type= typeOrMethod.getElementType();
		if (type != IJavaElement.METHOD && type != IJavaElement.TYPE) {
			return false;
		}

		IJavaElement editorElement= EditorUtility.getEditorInputJavaElement(editor, false);
		if (editorElement instanceof ITypeRoot editorTypeRoot) {

			ITypeRoot elementTypeRoot= (ITypeRoot) typeOrMethod.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (elementTypeRoot == null) {
				elementTypeRoot= (ITypeRoot) typeOrMethod.getAncestor(IJavaElement.CLASS_FILE);
			}
			return elementTypeRoot.equals(editorTypeRoot);
		}
		return false;
	}
}
