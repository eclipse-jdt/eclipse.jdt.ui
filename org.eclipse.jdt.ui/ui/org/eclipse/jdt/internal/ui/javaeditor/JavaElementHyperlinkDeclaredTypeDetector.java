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
import org.eclipse.jdt.core.Signature;

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
	protected IHyperlink createHyperlink(IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		try {
			if ((element.getElementType() == IJavaElement.FIELD || element.getElementType() == IJavaElement.LOCAL_VARIABLE) && !isPrimitive(element) && SelectionConverter.canOperateOn(editor)) {
				return new JavaElementDeclaredTypeHyperlink(wordRegion, openAction, element, qualify);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	/**
	 * Checks whether the declared type is a primitive type.
	 * 
	 * @param element the java element
	 * @return <code>true</code> if the declared type is a primitive type, <code> false</code>
	 *         otherwise
	 * @throws JavaModelException if this element does not exist or if an exception occurs while
	 *             accessing its corresponding resource.
	 */
	private boolean isPrimitive(IJavaElement element) throws JavaModelException {
		String typeSignature= null;
		if (element instanceof ILocalVariable) {
			typeSignature= ((ILocalVariable)element).getTypeSignature();
		} else if (element instanceof IField) {
			try {
				typeSignature= ((IField)element).getTypeSignature();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				return false;
			}
		}
		return Signature.getTypeSignatureKind(Signature.getElementType(typeSignature)) == Signature.BASE_TYPE_SIGNATURE;
	}
}
