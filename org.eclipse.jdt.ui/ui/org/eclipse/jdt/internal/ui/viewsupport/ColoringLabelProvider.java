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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledStringBuilder;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.viewers.StyledStringBuilder.Styler;

public class ColoringLabelProvider extends DecoratingStyledCellLabelProvider implements ILabelProvider {
	
	//TODO remove after IBuild > 20080227
	static class VoidLD extends BaseLabelProvider  implements ILabelDecorator {
		public Image decorateImage(Image image, Object element) {
			return null;
		}

		public String decorateText(String text, Object element) {
			return null;
		}
	}

	public static final Styler HIGHLIGHT_STYLE= StyledStringBuilder.createColorRegistryStyler(null, ColoredViewersManager.HIGHLIGHT_BG_COLOR_NAME);
	public static final Styler HIGHLIGHT_WRITE_STYLE= StyledStringBuilder.createColorRegistryStyler(null, ColoredViewersManager.HIGHLIGHT_WRITE_BG_COLOR_NAME);
	
	public ColoringLabelProvider(IStyledLabelProvider labelProvider) {
		this(labelProvider, new VoidLD(), null);
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
	
	protected StyleRange prepareStyleRange(StyleRange styleRange, boolean applyColors) {
		if (!applyColors && styleRange.background != null) {
			styleRange= super.prepareStyleRange(styleRange, applyColors);
			styleRange.borderStyle= SWT.BORDER_DOT;
			return styleRange;
		}
		return super.prepareStyleRange(styleRange, applyColors);
	}
	
	public String getText(Object element) {
		return getStyledText(element).toString();
	}

	public static StyledStringBuilder decorateStyledString(StyledStringBuilder string, String decorated, Styler color) {
		String label= string.toString();
		int originalStart= decorated.indexOf(label);
		if (originalStart == -1) {
			return new StyledStringBuilder(decorated); // the decorator did something wild
		}
		if (originalStart > 0) {
			StyledStringBuilder newString= new StyledStringBuilder(decorated.substring(0, originalStart), color);
			newString.append(string);
			string= newString;
		}
		if (decorated.length() > originalStart + label.length()) { // decorator appended something
			return string.append(decorated.substring(originalStart + label.length()), color);
		}
		return string; // no change
	}

}
