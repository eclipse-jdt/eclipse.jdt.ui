/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.breadcrumb;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerRow;

/**
 * A viewer row for the breadcrumb viewer.
 * 
 * @since 3.4
 */
class BreadcrumbViewerRow extends ViewerRow {

	private Color fForeground;
	private Font fFont;
	private Color fBackground;

	private final BreadcrumbItem fItem;
	private final BreadcrumbViewer fViewer;

	public BreadcrumbViewerRow(BreadcrumbViewer viewer, BreadcrumbItem item) {
		fViewer= viewer;
		fItem= item;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#clone()
	 */
	public Object clone() {
		return new BreadcrumbViewerRow(fViewer, fItem);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getBackground(int)
	 */
	public Color getBackground(int columnIndex) {
		return fBackground;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getBounds(int)
	 */
	public Rectangle getBounds(int columnIndex) {
		return getBounds();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getBounds()
	 */
	public Rectangle getBounds() {
		return fItem.getBounds();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getColumnCount()
	 */
	public int getColumnCount() {
		return 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getControl()
	 */
	public Control getControl() {
		return fViewer.getControl();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getElement()
	 */
	public Object getElement() {
		return fItem.getData();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getFont(int)
	 */
	public Font getFont(int columnIndex) {
		return fFont;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getForeground(int)
	 */
	public Color getForeground(int columnIndex) {
		return fForeground;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getImage(int)
	 */
	public Image getImage(int columnIndex) {
		return fItem.getImage();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getItem()
	 */
	public Widget getItem() {
		return fItem;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getNeighbor(int, boolean)
	 */
	public ViewerRow getNeighbor(int direction, boolean sameLevel) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getText(int)
	 */
	public String getText(int columnIndex) {
		return fItem.getText();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#getTreePath()
	 */
	public TreePath getTreePath() {
		return new TreePath(new Object[] { getElement() });
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#setBackground(int, org.eclipse.swt.graphics.Color)
	 */
	public void setBackground(int columnIndex, Color color) {
		fBackground= color;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#setFont(int, org.eclipse.swt.graphics.Font)
	 */
	public void setFont(int columnIndex, Font font) {
		fFont= font;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#setForeground(int, org.eclipse.swt.graphics.Color)
	 */
	public void setForeground(int columnIndex, Color color) {
		fForeground= color;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#setImage(int, org.eclipse.swt.graphics.Image)
	 */
	public void setImage(int columnIndex, Image image) {
		fItem.setImage(image);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerRow#setText(int, java.lang.String)
	 */
	public void setText(int columnIndex, String text) {
		fItem.setText(text);
	}

}
