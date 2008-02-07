/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.SimpleStyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerColumn;

public class ColoringLabelProvider extends SimpleStyledCellLabelProvider implements ILabelProvider {
	
	private ILabelProvider fLabelProvider;
	private ColorRegistry fRegistry;
	
	public ColoringLabelProvider(ILabelProvider labelProvider) {
		fLabelProvider= labelProvider;
		fRegistry= JFaceResources.getColorRegistry();
	}
	
	public void initialize(ColumnViewer viewer, ViewerColumn column) {
		ColoredViewersManager.install(this);
		setOwnerDrawEnabled(ColoredViewersManager.showColoredLabels());
		
		super.initialize(viewer, column);
	}
	
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		fLabelProvider.addListener(listener);
	}
	
	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		fLabelProvider.removeListener(listener);
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
	
	protected LabelPresentationInfo getLabelPresentationInfo(Object element) {
		Image image= fLabelProvider.getImage(element);
		Font defaultFont= null;
		
		if (fLabelProvider instanceof IFontProvider) {
			IFontProvider fontProvider= (IFontProvider) fLabelProvider;
			defaultFont= fontProvider.getFont(element);
		}
		Color foreground= null;
		Color background= null;
		if (fLabelProvider instanceof IColorProvider) {
			IColorProvider colorProvider= (IColorProvider) fLabelProvider;
			foreground= colorProvider.getForeground(element);
			background= colorProvider.getBackground(element);
		}
		String text;
		StyleRange[] ranges;
		ColoredString label= getRichTextLabel(element);
		if (label != null) {
			text= label.getString();
			ranges= label.getStyleRanges(fRegistry);
		} else {
			text= fLabelProvider.getText(element);
			ranges= new StyleRange[0];
		}
		return new LabelPresentationInfo(text, ranges, image, defaultFont, foreground, background);
	}
	
	
	private ColoredString getRichTextLabel(Object element) {
		if (fLabelProvider instanceof IRichLabelProvider) {
			// get a rich label 
			IRichLabelProvider richLabelProvider= (IRichLabelProvider) fLabelProvider;
			ColoredString richLabel= richLabelProvider.getRichTextLabel(element);
			if (richLabel != null) {
				return richLabel;
			}
		}
		return null;
	}

	public Image getImage(Object element) {
		return fLabelProvider.getImage(element);
	}

	public String getText(Object element) {
		return fLabelProvider.getText(element);
	}

}
