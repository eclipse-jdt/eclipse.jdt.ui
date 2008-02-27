/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ViewerColumn;

public class ColoringLabelProvider extends DecoratingStyledCellLabelProvider implements ILabelProvider {
	
	public ColoringLabelProvider(IStyledLabelProvider labelProvider) {
		this(labelProvider, null, null);
	}
	
	public ColoringLabelProvider(IStyledLabelProvider labelProvider, ILabelDecorator decorator, IDecorationContext decorationContext) {
		super(labelProvider, decorator, decorationContext);
	}

	public void initialize(ColumnViewer viewer, ViewerColumn column) {
		ColoredViewersManager.install(this);
		setOwnerDrawEnabled(ColoredViewersManager.showColoredLabels());
		
		super.initialize(viewer, column);
	}
		
	public void dispose() {
		super.dispose();
		ColoredViewersManager.uninstall(this);
	}
	
	public void refresh() {
		ColumnViewer viewer= getViewer();
		
		if (viewer == null) {
			return;
		}
		boolean showColoredLabels= ColoredViewersManager.showColoredLabels();
		if (showColoredLabels != isOwnerDrawEnabled()) {
			setOwnerDrawEnabled(showColoredLabels);
			viewer.refresh();
		} else if (showColoredLabels) {
			viewer.refresh();
		}
	}
	
	public String getText(Object element) {
		return getStyledText(element).toString();
	}

}
