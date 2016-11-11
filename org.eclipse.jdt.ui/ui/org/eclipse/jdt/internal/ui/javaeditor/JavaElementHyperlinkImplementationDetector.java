/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		int elementType= element.getElementType();
		try {
			if ((elementType == IJavaElement.METHOD || (elementType == IJavaElement.TYPE && ((IType) element).isInterface())) && SelectionConverter.canOperateOn(editor)) {
				hyperlinksCollector.add(new JavaElementImplementationHyperlink(wordRegion, openAction, element, qualify, editor));
			}
		} catch (JavaModelException e) {
			// do nothing
		}
	}
}
