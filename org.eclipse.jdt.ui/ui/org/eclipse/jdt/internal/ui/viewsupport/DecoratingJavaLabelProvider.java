/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.graphics.Color;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IColorProvider;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.packageview.HierarchicalDecorationContext;

public class DecoratingJavaLabelProvider extends DecoratingLabelProvider implements IColorProvider {
	
	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * with problem and override indicuator with the workbench decorator (label
	 * decorator extension point).
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider) {
		this(labelProvider, true);
	}

	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * (if enabled with problem indicator) with the workbench
	 * decorator (label decorator extension point).
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider, boolean errorTick) {
		this(labelProvider, errorTick, true);
	}
	
	/**
	 * Decorating label provider for Java. Combines a JavaUILabelProvider
	 * (if enabled with problem indicator) with the workbench
	 * decorator (label decorator extension point).
	 */
	public DecoratingJavaLabelProvider(JavaUILabelProvider labelProvider, boolean errorTick, boolean flatPackageMode) {
		super(labelProvider, PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
		if (errorTick) {
			labelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
		}
		setFlatPackageMode(flatPackageMode);
	}
	
	/**
	 * Tells the label decorator if the view presents packages flat or hierarchical.
	 * @param enable If set, packages are presented in flat mode.
	 */
	public void setFlatPackageMode(boolean enable) {
		if (enable) {
			setDecorationContext(DecorationContext.DEFAULT_CONTEXT);
		} else {
			setDecorationContext(HierarchicalDecorationContext.CONTEXT);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		// label provider is a JavaUILabelProvider
		return ((IColorProvider) getLabelProvider()).getForeground(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	public Color getBackground(Object element) {
		// label provider is a JavaUILabelProvider
		return ((IColorProvider) getLabelProvider()).getBackground(element);
	}

}
