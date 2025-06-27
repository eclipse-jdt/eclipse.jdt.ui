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

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;


/**
 * Java element open in new tab hyperlink detector for types and methods.
 *
 * @since 3.5
 */
public class JavaElementOpenInNewHyperlinkDetector extends JavaElementHyperlinkDetector {

	@Override
	protected void addHyperlinks(List<IHyperlink> hyperlinksCollector, IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		if (isDeclaredInCurrentEditor(element)) {
			hyperlinksCollector.add(new JavaElementOpenInNewImplementationHyperlink(wordRegion, element, editor));
		}
	}

	private boolean isDeclaredInCurrentEditor(IJavaElement TypeOrMethod) {
		if (TypeOrMethod.getElementType() == IJavaElement.METHOD || TypeOrMethod.getElementType() == IJavaElement.TYPE) {
			IEditorPart existingEditr= EditorUtility.isOpenInEditor(TypeOrMethod);
			if (existingEditr != null) {
				return true;

			}
		}
		return false;
	}
}
