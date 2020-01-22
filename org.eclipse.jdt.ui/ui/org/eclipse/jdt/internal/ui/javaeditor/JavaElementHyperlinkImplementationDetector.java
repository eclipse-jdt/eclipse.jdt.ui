/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;


/**
 * Java element implementation hyperlink detector for types and methods.
 *
 * @since 3.5
 */
public class JavaElementHyperlinkImplementationDetector extends JavaElementHyperlinkDetector {

	@Override
	protected void addHyperlinks(List<IHyperlink> hyperlinksCollector, IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, JavaEditor editor) {
		if (canOpenImplementation(element) && SelectionConverter.canOperateOn(editor)) {
			hyperlinksCollector.add(new JavaElementImplementationHyperlink(wordRegion, openAction, element, qualify, editor));
		}
	}

	public static boolean canOpenImplementation(IJavaElement element) {
		return element.getElementType() == IJavaElement.METHOD || isImplementableType(element);
	}

	private static boolean isImplementableType(IJavaElement element) {
		if (element.getElementType() == IJavaElement.TYPE) {
			IType type= (IType) element;
			try {
				if (type.isClass() || type.isInterface() && !type.isAnnotation()) {
					return true;
				}
			} catch (JavaModelException e) {
				// cannot check the type
			}
		}
		return false;
	}
}
