/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

/**
 * Java element variable declaration type hyperlink detector.
 * 
 * @since 3.7
 */
public class JavaElementHyperlinkDeclaredTypeDetector extends JavaElementHyperlinkDetector {


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlinkDetector#createHyperlink(org.eclipse.jface.text.IRegion, org.eclipse.jdt.ui.actions.SelectionDispatchAction, org.eclipse.jdt.core.IJavaElement, boolean, org.eclipse.jdt.internal.ui.javaeditor.JavaEditor)
	 */
	protected IHyperlink createHyperlink(IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		if ((element.getElementType() == IJavaElement.FIELD || element.getElementType() == IJavaElement.LOCAL_VARIABLE) && SelectionConverter.canOperateOn(editor)) {
			return new JavaElementDeclaredTypeHyperlink(wordRegion, openAction, element, qualify);
		}
		return null;
	}
}
