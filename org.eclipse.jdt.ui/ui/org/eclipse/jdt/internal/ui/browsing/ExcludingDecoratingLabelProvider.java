/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

/**
 * Excludes a decorator from the decorator manager from being called.
 * 
 * @since 2.1
 */
class ExcludingDecoratingLabelProvider extends DecoratingLabelProvider {
	
	private String fExcludedDecoratorId;

	public ExcludingDecoratingLabelProvider(ILabelProvider provider, ILabelDecorator decorator, String excludedDecoratorId) {
		super(provider, decorator);
		fExcludedDecoratorId= excludedDecoratorId;
	}


	/*
	 * @see ILabelProvider#getImage
	 */
	public Image getImage(Object element) {
		IDecoratorManager decoratorMgr= PlatformUI.getWorkbench().getDecoratorManager();
		boolean isDecoratorEnabled= decoratorMgr.getEnabled(fExcludedDecoratorId);

		if (isDecoratorEnabled)
			try {
				decoratorMgr.setEnabled(fExcludedDecoratorId, false);
			} catch (CoreException e) {
				// continue
			}
		
		Image image= super.getImage(element);
		
		if (isDecoratorEnabled)
			try {
				decoratorMgr.setEnabled(fExcludedDecoratorId, true);
			} catch (CoreException e) {
				// continue
			}

		return image;
	}

	/*
	 * @see ILabelProvider#getText
	 */
	public String getText(Object element) {
		IDecoratorManager decoratorMgr= PlatformUI.getWorkbench().getDecoratorManager();
		boolean isDecoratorEnabled= decoratorMgr.getEnabled(fExcludedDecoratorId);

		if (isDecoratorEnabled)
			try {
				decoratorMgr.setEnabled(fExcludedDecoratorId, false);
			} catch (CoreException e) {
				// continue
			}

		String text= super.getText(element);

		if (isDecoratorEnabled)
			try {
				decoratorMgr.setEnabled(fExcludedDecoratorId, true);
			} catch (CoreException e) {
				// continue
			}

		return text;
	}
}
