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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.PrimitiveType;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

/**
 * Java element method return type hyperlink detector.
 * 
 * @since 3.7
 */
public class JavaElementHyperlinkReturnTypeDetector extends JavaElementHyperlinkDetector {


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlinkDetector#createHyperlink(org.eclipse.jface.text.IRegion, org.eclipse.jdt.ui.actions.SelectionDispatchAction, org.eclipse.jdt.core.IJavaElement, boolean, org.eclipse.jdt.internal.ui.javaeditor.JavaEditor)
	 */
	protected IHyperlink createHyperlink(IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		try {
			if (element.getElementType() == IJavaElement.METHOD && !isPrimitive((IMethod)element) && SelectionConverter.canOperateOn(editor)) {
				return new JavaElementReturnTypeHyperlink(wordRegion, openAction, (IMethod)element, qualify);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Checks whether the return type is a primitive type.
	 * 
	 * @param method the method to check
	 * @return <code>true</code> if the return type is a primitive type, <code> false</code>
	 *         otherwise
	 * @throws JavaModelException if this element does not exist or if an exception occurs while
	 *             accessing its corresponding resource.
	 */
	private boolean isPrimitive(IMethod method) throws JavaModelException {
		String returnType= method.getReturnType();
		return (PrimitiveType.toCode(Signature.toString(returnType)) != null);
	}
}
