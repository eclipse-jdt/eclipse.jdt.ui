/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
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
	@Override
	protected IHyperlink createHyperlink(IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		try {
			if ((element.getElementType() == IJavaElement.FIELD || element.getElementType() == IJavaElement.LOCAL_VARIABLE) && !JavaModelUtil.isPrimitive(getTypeSignature(element))
					&& SelectionConverter.canOperateOn(editor)) {
				return new JavaElementDeclaredTypeHyperlink(wordRegion, openAction, element, qualify);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Returns the type signature of the element.
	 * 
	 * @param element an instance of <code>ILocalVariable</code> or <code>IField</code>
	 * @return the type signature of the element
	 * @throws JavaModelException if this element does not exist or if an exception occurs while
	 *             accessing its corresponding resource.
	 */
	static String getTypeSignature(IJavaElement element) throws JavaModelException {
		if (element instanceof ILocalVariable) {
			return ((ILocalVariable)element).getTypeSignature();
		} else if (element instanceof IField) {
			return ((IField)element).getTypeSignature();
		}
		throw new IllegalArgumentException();
	}
}
