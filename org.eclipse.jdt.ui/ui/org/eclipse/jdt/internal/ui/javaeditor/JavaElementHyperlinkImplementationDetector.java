/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Java element implementation hyperlink detector for methods.
 * 
 * @since 3.5
 */
public class JavaElementHyperlinkImplementationDetector extends JavaElementHyperlinkDetector {

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlinkDetector#createHyperlink(org.eclipse.jface.text.IRegion, org.eclipse.jdt.ui.actions.SelectionDispatchAction, org.eclipse.jdt.core.IJavaElement, boolean, org.eclipse.ui.texteditor.ITextEditor)
	 * @since 3.5
	 */
	protected IHyperlink createHyperlink(IRegion wordRegion, SelectionDispatchAction openAction, IJavaElement element, boolean qualify, ITextEditor editor) {
		if (element.getElementType() == IJavaElement.METHOD && canBeOverridden((IMethod)element)) {
			return new JavaElementImplementationHyperlink(wordRegion, openAction, element, qualify, editor);
		}
		return null;
	}

	/**
	 * Checks whether a method can be overridden.
	 * 
	 * @param method the method
	 * @return <code>false</code> if the method is final, static, or a constructor, or if its declaring
	 *         class is final, or in case of an exception, <code>true</code> otherwise
	 */
	private boolean canBeOverridden(IMethod method) {
		try {
			return !(JdtFlags.isPrivate(method) || JdtFlags.isFinal(method) || JdtFlags.isStatic(method) || method.isConstructor() || JdtFlags.isFinal((IMember)method.getParent()));
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}
}
