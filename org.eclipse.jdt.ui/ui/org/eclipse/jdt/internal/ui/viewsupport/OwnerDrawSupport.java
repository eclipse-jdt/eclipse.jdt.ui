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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;

public abstract class OwnerDrawSupport implements Listener {
	
	private static final int X_OFFSET= 2; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=178008 
	private static final int Y_OFFSET= 2; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=178008
	
	private TextLayout fTextLayout;
	private final Control fControl;
	
	public OwnerDrawSupport(Control control) {
		fControl= control;
		fTextLayout= new TextLayout(control.getDisplay());
		
		control.addListener(SWT.PaintItem, this);
		control.addListener(SWT.EraseItem, this);
		control.addListener(SWT.Dispose, this);
	}
	
	public abstract ColoredString getColoredLabel(Item item);
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	public void handleEvent(Event event) {
		if (event.type == SWT.PaintItem) {
			performPaint(event);
		} else if (event.type == SWT.EraseItem) {
			performErase(event);
		} else if (event.type == SWT.Dispose) {
			dispose();
		}
	}
	
	private void performErase(Event event) {
		event.detail &= ~SWT.FOREGROUND;
	}
			
	private void performPaint(Event event) {
		Item item= (Item) event.item;
		GC gc= event.gc;
		Image image= item.getImage();
		if (image != null) {
			gc.drawImage(item.getImage(), event.x, event.y);
		}
		ColoredString coloredLabel= getColoredLabel(item);
		boolean isSelected= (event.detail & SWT.SELECTED) != 0 && fControl.isFocusControl();
		if (item instanceof TreeItem) {
			TreeItem treeItem= (TreeItem) item;
			Rectangle bounds= treeItem.getBounds();
			Font font= treeItem.getFont(0);
			processColoredLabel(coloredLabel, gc, bounds.x + X_OFFSET, bounds.y + Y_OFFSET, isSelected, font);
			if ((event.detail & SWT.FOCUSED) != 0) {
				gc.drawFocus(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		} else if (item instanceof TableItem) {
			TableItem tableItem= (TableItem) item;
			Rectangle bounds= tableItem.getBounds();
			Font font= tableItem.getFont(0);
			processColoredLabel(coloredLabel, gc, bounds.x + X_OFFSET, bounds.y + Y_OFFSET, isSelected, font);
			if ((event.detail & SWT.FOCUSED) != 0) {
				gc.drawFocus(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}
	}
	
	private void processColoredLabel(ColoredString richLabel, GC gc, int x, int y, boolean isSelected, Font font) {
		String text= richLabel.getString();
		fTextLayout.setText(text);
		fTextLayout.setFont(font);
		if (isSelected) {
			// no styles when element is selected
			fTextLayout.draw(gc, x, y, 0, text.length() - 1, null, null);
		} else {
			// apply the styled ranges
			Display display= (Display) gc.getDevice();
			Iterator ranges= richLabel.getRanges();
			while (ranges.hasNext()) {
				ColoredString.StyleRange curr= (ColoredString.StyleRange) ranges.next();
				ColoredString.Style style= curr.style;
				if (style != null) {
					Color foreground= style.getForeground(display);
					TextStyle textStyle= new TextStyle(null, foreground, null);
					fTextLayout.setStyle(textStyle, curr.offset, curr.offset + curr.length - 1);
				}
			}
			fTextLayout.draw(gc, x, y);
		}
	}
	
	public void dispose() {
    	if (fTextLayout != null) {
    		fTextLayout.dispose();
    		fTextLayout= null;
    	}
    	if (!fControl.isDisposed()) {
			fControl.removeListener(SWT.PaintItem, this);
			fControl.removeListener(SWT.EraseItem, this);
			fControl.removeListener(SWT.Dispose, this);
    	}
	}
	
	private static class StructuredViewerOwnerDrawController {
		private static final String COLORED_LABEL_KEY= "coloredlabel"; //$NON-NLS-1$
		
		private StructuredViewer fViewer;
		private OwnerDrawSupport fOwnerDrawSupport;
		
		private StructuredViewerOwnerDrawController(StructuredViewer viewer) {
			fViewer= viewer;
			fOwnerDrawSupport= null;
			
			class PropertyChangeListener implements IPropertyChangeListener, DisposeListener, Runnable {
				public void widgetDisposed(DisposeEvent e) {
		    	  	PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
				}
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(AppearancePreferencePage.PREF_COLORED_LABELS)) {
						Display.getDefault().asyncExec(this);
					}
				}
				public void run() {
					performOwnerDrawPreferenceChanged();
				}
			}
			PropertyChangeListener listener= new PropertyChangeListener();
			PreferenceConstants.getPreferenceStore().addPropertyChangeListener(listener);
			fViewer.getControl().addDisposeListener(listener); // will uninstall itself on dispose
		}
		
		protected final void performOwnerDrawPreferenceChanged() {
			Control control= fViewer.getControl();
			if (!control.isDisposed()) {
				if (showColoredLabels()) {
					installOwnerDraw();
				} else {
					uninstallOwnerDraw();
				}
			}
		}
		
		protected void installOwnerDraw() {
			if (fOwnerDrawSupport != null)
				return; // already installed
			
			fOwnerDrawSupport= new OwnerDrawSupport(fViewer.getControl()) { // will install itself as listeners
				public ColoredString getColoredLabel(Item item) {
					return getColoredLabelForView(item);
				}
			};
		}
		
		protected void uninstallOwnerDraw() {
			if (fOwnerDrawSupport == null)
				return; // not installed

			fOwnerDrawSupport.dispose(); // removes itself as listener
			fViewer.setInput(fViewer.getInput()); // make sure all items are rebuilt
		}
		
		protected ColoredString getColoredLabelForView(Item item) {
			IBaseLabelProvider labelProvider= fViewer.getLabelProvider();
			ColoredString oldLabel= (ColoredString) item.getData(COLORED_LABEL_KEY);
			if (oldLabel != null && oldLabel.getString().equals(item.getText())) {
				// avoid accesses to the label provider if possible
				return oldLabel;
			}
			ColoredString newLabel= null;
			if (labelProvider instanceof IRichLabelProvider) {
				newLabel= ((IRichLabelProvider) labelProvider).getRichTextLabel(item.getData());
			}
			if (newLabel == null) {
				newLabel= new ColoredString(item.getText()); // fallback. Should never happen.
			}
			item.setData(COLORED_LABEL_KEY, newLabel); // cache the result
			return newLabel;
		}
	}
			
	public static boolean showColoredLabels() {
		String preference= PreferenceConstants.getPreference(AppearancePreferencePage.PREF_COLORED_LABELS, null);
		return preference != null && Boolean.valueOf(preference).booleanValue();
	}
	
	public static void install(StructuredViewer viewer) {
		StructuredViewerOwnerDrawController support= new StructuredViewerOwnerDrawController(viewer);
		if (showColoredLabels()) {
			support.installOwnerDraw();
		}
	}

}
