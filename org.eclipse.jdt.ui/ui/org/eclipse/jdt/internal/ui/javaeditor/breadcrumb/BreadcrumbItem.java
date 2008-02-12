/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.breadcrumb;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.jdt.internal.ui.util.PixelConverter;


/**
 * An item in a breadcrumb viewer.
 * <p>The item shows a label and an image. It also has the ability
 * to expand, that is to open a drop down menu.</p>
 * <p>The drop down allows to select any child of the items input element.
 * The item shows the label and icon of its data element, if any.</p>
 * 
 * @since 3.4
 */
class BreadcrumbItem extends Item {

	private ILabelProvider fLabelProvider;
	private ITreeContentProvider fContentProvider;

	private Object fInput;

	private final BreadcrumbViewer fParent;
	private Composite fContainer;

	private BreadcrumbItemDetails fDetailsBlock;
	private BreadcrumbItemDropDown fExpandBlock;
	private Label fSpacer;

	/**
	 * A new breadcrumb item which is shown inside the given viewer.
	 * 
	 * @param viewer the items viewer
	 * @param parent the container containing the item
	 */
	public BreadcrumbItem(BreadcrumbViewer viewer, Composite parent) {
		super(parent, SWT.NONE);

		fParent= viewer;

		fContainer= new Composite(parent, SWT.NONE);
		fContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		GridLayout layout= new GridLayout(3, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		fContainer.setLayout(layout);
		fContainer.setBackground(parent.getBackground());

		fExpandBlock= new BreadcrumbItemDropDown(this, fContainer);
		if (fLabelProvider != null)
			fExpandBlock.setLabelProvider(fLabelProvider);

		if (fContentProvider != null)
			fExpandBlock.setContentProvider(fContentProvider);

		fDetailsBlock= new BreadcrumbItemDetails(this, fContainer);

		fSpacer= new Label(fContainer, SWT.VERTICAL | SWT.SEPARATOR);
		GridData data= new GridData(SWT.BEGINNING, SWT.TOP, false, false);
		data.heightHint= new PixelConverter(parent).convertHeightInCharsToPixels(1) + 10;
		fSpacer.setLayoutData(data);
		fSpacer.setVisible(false);
	}

	/**
	 * @return the viewer showing this item
	 */
	public BreadcrumbViewer getViewer() {
		return fParent;
	}

	/**
	 * @param contentProvider the content provider to use
	 */
	public void setContentProvider(ITreeContentProvider contentProvider) {
		fContentProvider= contentProvider;
		if (fExpandBlock != null)
			fExpandBlock.setContentProvider(contentProvider);
	}

	/**
	 * @param labelProvider the label provider to use
	 */
	public void setLabelProvider(ILabelProvider labelProvider) {
		fLabelProvider= labelProvider;
		if (fExpandBlock != null)
			fExpandBlock.setLabelProvider(labelProvider);
	}

	/**
	 * Set the input element. The item allows to select any
	 * children of its input element.
	 * 
	 * @param input the input element for this item
	 */
	public void setInput(Object input) {
		if (fInput == input)
			return;

		fInput= input;
		refresh();
	}

	/**
	 * @return the input element of this item
	 */
	Object getInput() {
		return fInput;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#setData(java.lang.Object)
	 */
	public void setData(Object data) {
		if (data == getData())
			return;

		if (data != null && data.equals(getData()))
			return;

		super.setData(data);
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		fContainer.dispose();
		super.dispose();
	}

	/**
	 * Should this item show a text label.
	 * 
	 * @param enabled true if it should
	 */
	void setShowText(boolean enabled) {
		fDetailsBlock.setTextVisible(enabled);
	}

	/**
	 * Does this item show a text label?
	 * 
	 * @return true if it does.
	 */
	boolean isShowText() {
		return fDetailsBlock.isTextVisible();
	}

	/**
	 * @return the width of this item 
	 */
	int getWidth() {
		return fSpacer.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + fDetailsBlock.getWidth() + 19; //9 + 2 * 5 (arrowWidth + 2 * horizontalSpacing)
	}

	/**
	 * Sets whether this item has to be marked as
	 * selected or not.
	 * 
	 * @param selected true if marked as selected
	 */
	void setSelected(boolean selected) {
		fDetailsBlock.setSelected(selected);
	}

	/**
	 * Sets whether this item has the keyboard focus.
	 * 
	 * @param enabled if it does, false otherwise
	 */
	void setFocus(boolean enabled) {
		fDetailsBlock.setFocus(enabled);
	}

	/**
	 * @return true if this item has the keyboard focus
	 */
	boolean hasFocus() {
		return fDetailsBlock.hasFocus();
	}
	
	/**
	 * Set the label and the image of this item
	 * to the given values.
	 * 
	 * @param text the items label
	 * @param image the items image
	 */
	void update(String text, Image image) {
		fDetailsBlock.setContent(text, image, text);
	}

	/**
	 * Redraw this item, retrieves new labels
	 * from its label provider.
	 */
	void refresh() {
		if (getData() != null) {
			String text= fLabelProvider.getText(getData());
			Image image= fLabelProvider.getImage(getData());

			fDetailsBlock.setContent(text, image, text);
			fDetailsBlock.setVisible(true);
			fSpacer.setVisible(true);
		} else {
			fDetailsBlock.setVisible(false);
			fSpacer.setVisible(false);
		}

		if (fContentProvider.hasChildren(fInput)) {
			fExpandBlock.setEnabled(true);
		} else {
			fExpandBlock.setEnabled(false);
			fSpacer.setVisible(false);
		}
	}

	/**
	 * Expand this item, shows the drop down menu.
	 * Initialize the filter with the given text. <code>null</code>
	 * for default filter text.
	 * 
	 * @param filterText the text to use to initialize the filter text or <code>null</code>
	 * @param selectItem true to select the item in the drop down, false to select the filter text 
	 */
	void openDropDownMenu(String filterText, boolean selectItem) {
		fExpandBlock.showMenu(filterText, selectItem);
	}

	/**
	 * @return true if this item is expanded
	 */
	boolean isMenuShown() {
		return fExpandBlock.isMenuShown();
	}

}
