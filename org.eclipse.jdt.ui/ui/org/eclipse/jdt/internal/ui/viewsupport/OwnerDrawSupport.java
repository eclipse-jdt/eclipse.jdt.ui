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

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;

public class OwnerDrawSupport {
	//private static final String PREF="org.eclipse.jdt.ui.coloredlabels"; //$NON-NLS-1$
	
	private final StructuredViewer fViewer;
	private final Point fBoundOffset;

	private OwnerDrawSupport(StructuredViewer viewer) {
		this(viewer, new Point(2, 2));
	}
	
	private OwnerDrawSupport(StructuredViewer viewer, Point boundOffset) {
		fViewer= viewer;
		fBoundOffset= boundOffset;
	}
		
	private Listener fPaintListener= new Listener() {
		public void handleEvent(Event event) {
			performPaint(event);
		}
	};
	
	public void installOwnerDraw() {
		Control control= fViewer.getControl();
		control.addListener(SWT.PaintItem, fPaintListener);
	}
	
	public void uninstallOwnerDraw() {
		Control control= fViewer.getControl();
		control.removeListener(SWT.PaintItem, fPaintListener);
	}
	
	private void performPaint(Event event) {
		if ((event.detail & SWT.SELECTED) != 0) {
			return;
		}
		
		IBaseLabelProvider labelProvider= fViewer.getLabelProvider();
		if (labelProvider instanceof IRichLabelProvider) {
			IRichLabelProvider richLabelProvider= (IRichLabelProvider) labelProvider;
			Widget item= event.item;
			ColoredString richLabel= richLabelProvider.getRichTextLabel(item.getData());
			if (richLabel != null) {
				if (item instanceof TreeItem) {
					Rectangle bounds= ((TreeItem) item).getBounds();
					processRichLabel(event.gc, bounds.x + fBoundOffset.x, bounds.y + fBoundOffset.y, richLabel);
				} else if (item instanceof TableItem) {
					Rectangle bounds= ((TableItem) item).getBounds();
					processRichLabel(event.gc, bounds.x +2, bounds.y + 2, richLabel);
				}
			}
		}
	}

	private void processRichLabel(GC gc, int x, int y, ColoredString richLabel) {
		String text= richLabel.getString();
		
		int offsetProcessed= 0;
		Iterator ranges= richLabel.getRanges();
		while (ranges.hasNext()) {
			ColoredString.ColorRange curr= (ColoredString.ColorRange) ranges.next();
			if (offsetProcessed < curr.offset) {
				x= draw(gc, x, y, text.substring(offsetProcessed, curr.offset), 0, 0);
			}
			x= draw(gc, x, y, text.substring(curr.offset, curr.offset + curr.length), curr.foregroundColor, curr.backgroundColor);
			offsetProcessed= curr.offset + curr.length;
		}
	}

	private int draw(GC gc, int x, int y, String text, int foregroundColor, int backgroundColor) {
		Point p= gc.textExtent(text);
		if (foregroundColor > 0 || backgroundColor > 0) {
			gc.setForeground(getForegroundColor(gc, foregroundColor));
			gc.drawText(text, x, y, SWT.NONE);
		}
		return x + p.x;
	}
	
	private Color getForegroundColor(GC gc, int color) {
		switch (color) {
			case 0:
				return gc.getDevice().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
			case 1:
				return gc.getDevice().getSystemColor(SWT.COLOR_DARK_GRAY);
			case 2:
				return gc.getDevice().getSystemColor(SWT.COLOR_DARK_BLUE);
			case 3:
				return gc.getDevice().getSystemColor(SWT.COLOR_GRAY);
			case 4:
				return gc.getDevice().getSystemColor(SWT.COLOR_DARK_RED);
			case 5:
				return gc.getDevice().getSystemColor(SWT.COLOR_DARK_CYAN);
			default:
				return gc.getDevice().getSystemColor(SWT.COLOR_RED);
		}
	}
	
	public static void install(StructuredViewer viewer) {
		String preference= PreferenceConstants.getPreference(AppearancePreferencePage.PREF_COLORED_LABELS, null);
		if (preference != null && Boolean.valueOf(preference).booleanValue()) {
			OwnerDrawSupport support= new OwnerDrawSupport(viewer);
			support.installOwnerDraw();
		}
		
		/*String property= System.getProperty(PREF, "false"); //$NON-NLS-1$
		if (Boolean.valueOf(property).booleanValue()) {
			OwnerDrawSupport support= new OwnerDrawSupport(viewer);
			support.installOwnerDraw();
			return;
		}
		int index= property.indexOf(':');
		if (index != -1) {
			try {
				int x= Integer.parseInt(property.substring(0, index));
				int y= Integer.parseInt(property.substring(index + 1));
				OwnerDrawSupport support= new OwnerDrawSupport(viewer, new Point(x, y));
				support.installOwnerDraw();
			} catch (NumberFormatException e) {
				JavaPlugin.logErrorMessage("Invalid arguments for " + PREF); //$NON-NLS-1$
			}
		}*/
	}

}
