/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

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


	@Override
	protected void addHyperlinks(List<IHyperlink> hyperlinksCollector, IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		try {
			if (element.getElementType() == IJavaElement.FIELD || element.getElementType() == IJavaElement.LOCAL_VARIABLE) {
				String typeSignature= getTypeSignature(element);
				if (!JavaModelUtil.isPrimitive(typeSignature) && SelectionConverter.canOperateOn(editor)) {
					if (Signature.getTypeSignatureKind(typeSignature) == Signature.INTERSECTION_TYPE_SIGNATURE) {
						String[] bounds= Signature.getIntersectionTypeBounds(typeSignature);
						qualify|= bounds.length >= 2;
						for (String bound : bounds) {
							hyperlinksCollector.add(new JavaElementDeclaredTypeHyperlink(wordRegion, openAction, element, bound, qualify));
						}
					} else {
						hyperlinksCollector.add(new JavaElementDeclaredTypeHyperlink(wordRegion, openAction, element, qualify));
					}
				}
			}
		} catch (JavaModelException e) {
			if (e.getStatus().getCode() != IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST) {
				JavaPlugin.log(e);
			}
		}
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
