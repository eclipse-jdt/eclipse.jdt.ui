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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerLabel;


/**
 * A breadcrumb viewer shows a the parent chain of its input element
 * in a list. Each breadcrumb item of that list can be expanded and a sibling
 * of the element presented by the breadcrumb item can be selected.
 * <p>
 * Content providers for breadcrumb viewers must implement the
 * <code>ITreeContentProvider</code> interface.
 * </p>
 * <p>
 * Label providers for breadcrumb viewers must implement the <code>ILabelProvider</code>
 * interface.
 * </p>
 * 
 * @since 3.4
 */
public class BreadcrumbViewer extends StructuredViewer {
	
	private final Composite fContainer;
	private final ArrayList fTreeItems;

	private BreadcrumbItem fSelectedItem;

	/**
	 * Create a new <code>BreadcrumbViewer</code>.
	 * <p>
	 * Style is one of:
	 * <ul>
	 * <li>SWT.NONE</li>
	 * <li>SWT.VERTICAL</li>
	 * <li>SWT.HORIZONTAL</li>
	 * </ul>
	 * 
	 * @param parent the container for the viewer
	 * @param style the style flag used for this viewer
	 */
	public BreadcrumbViewer(Composite parent, int style) {
		fTreeItems= new ArrayList();

		fContainer= new Composite(parent, SWT.NONE);
		GridData layoutData= new GridData(SWT.FILL, SWT.TOP, true, false);
		fContainer.setLayoutData(layoutData);

		hookControl(fContainer);
		
		int columns= 1000;
		if ((SWT.VERTICAL & style) != 0) {
			columns= 1;
		}
		
		GridLayout gridLayout= new GridLayout(columns, false);
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		gridLayout.verticalSpacing= 0;
		gridLayout.horizontalSpacing= 0;
		fContainer.setLayout(gridLayout);

		fContainer.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				refresh();
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	public Control getControl() {
		return fContainer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#reveal(java.lang.Object)
	 */
	public void reveal(Object element) {
		//all elements are always visible
	}

	/**
	 * Transfers the keyboard focus into the viewer.
	 */
	public void setFocus() {
		fContainer.setFocus();

		if (fSelectedItem != null) {
			fSelectedItem.setFocus(true);
		} else {
			if (fTreeItems.size() == 0)
				return;

			BreadcrumbItem item= (BreadcrumbItem) fTreeItems.get(fTreeItems.size() - 1);
			if (item.getData() == null) {
				if (fTreeItems.size() < 2)
					return;

				item= (BreadcrumbItem) fTreeItems.get(fTreeItems.size() - 2);
			}
			item.setFocus(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#update(java.lang.Object[], java.lang.String[])
	 */
	public void update(Object[] elements, String[] properties) {
		fContainer.setRedraw(false);
		try {
			super.update(elements, properties);
			
			if (containsAny(elements)) {
				fContainer.layout(true, true);
				if (updateSize())
					fContainer.layout(true, true);
			}
		} finally {
			fContainer.setRedraw(true);
		}
	}
	
	/**
	 * @return true if the any of the items in the viewer is expanded
	 */
	public boolean isDropDownOpen() {
		for (int i= 0, size= fTreeItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fTreeItems.get(i);
			if (item.isMenuShown())
				return true;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#assertContentProviderType(org.eclipse.jface.viewers.IContentProvider)
	 */
	protected void assertContentProviderType(IContentProvider provider) {
		super.assertContentProviderType(provider);
		Assert.isTrue(provider instanceof ITreeContentProvider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object, java.lang.Object)
	 */
	protected void inputChanged(Object fInput, Object oldInput) {
		if (fContainer.isDisposed())
			return;

		fContainer.setRedraw(false);
		try {
			int lastIndex= buildItemChain(fInput);

			if (((ITreeContentProvider) getContentProvider()).hasChildren(fInput)) {
				BreadcrumbItem item;
				if (lastIndex < fTreeItems.size()) {
					item= (BreadcrumbItem) fTreeItems.get(lastIndex);
					if (item.getData() != null)
						unmapElement(item.getData());
				} else {
					item= createItem();
					fTreeItems.add(item);
				}

				if (item == fSelectedItem) {
					selectItem(null);
				}
				item.setInput(fInput);
				item.setData(null);
				lastIndex++;
			}

			while (lastIndex < fTreeItems.size()) {
				BreadcrumbItem item= (BreadcrumbItem) fTreeItems.remove(fTreeItems.size() - 1);
				if (item == fSelectedItem) {
					selectItem(null);
				}
				if (item.getData() != null)
					unmapElement(item.getData());
				item.dispose();
			}
			
			updateSize();
			fContainer.layout(true, true);
		} finally {
			fContainer.setRedraw(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindInputItem(java.lang.Object)
	 */
	protected Widget doFindInputItem(Object element) {
		if (element == null)
			return null;
		
		if (element == getInput() || element.equals(getInput()))
			return doFindItem(element);
	
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#doFindItem(java.lang.Object)
	 */
	protected Widget doFindItem(Object element) {
		if (element == null)
			return null;
	
		for (int i= 0, size= fTreeItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fTreeItems.get(i);
			if (item.getData() == element || element.equals(item.getData()))
				return item;
		}
	
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#doUpdateItem(org.eclipse.swt.widgets.Widget, java.lang.Object, boolean)
	 */
	protected void doUpdateItem(Widget widget, Object element, boolean fullMap) {
		if (widget instanceof BreadcrumbItem) {
			final BreadcrumbItem item= (BreadcrumbItem) widget;
	
			// remember element we are showing
			if (fullMap) {
				associate(element, item);
			} else {
				Object data= item.getData();
				if (data != null) {
					unmapElement(data, item);
				}
				item.setData(element);
				mapElement(element, item);
			}
	
			ViewerLabel label= new ViewerLabel(item.getText(), item.getImage());
			buildLabel(label, element);
			
			item.update(label.getText(), label.getImage());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#getSelectionFromWidget()
	 */
	protected List getSelectionFromWidget() {
		if (fSelectedItem == null)
			return Collections.EMPTY_LIST;
	
		if (fSelectedItem.getData() == null)
			return Collections.EMPTY_LIST;
	
		ArrayList result= new ArrayList();
		result.add(fSelectedItem.getData());
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#internalRefresh(java.lang.Object)
	 */
	protected void internalRefresh(Object element) {
	
		fContainer.setRedraw(false);
		try {
			BreadcrumbItem item= (BreadcrumbItem) doFindItem(element);
			if (item == null) {
				for (int i= 0, size= fTreeItems.size(); i < size; i++) {
					BreadcrumbItem item1= (BreadcrumbItem) fTreeItems.get(i);
					item1.refresh();
				}
			} else {
				item.refresh();
			}
			if (updateSize())
				fContainer.layout(true, true);
		} finally {
			fContainer.setRedraw(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#setSelectionToWidget(java.util.List, boolean)
	 */
	protected void setSelectionToWidget(List l, boolean reveal) {
		BreadcrumbItem focusItem= null;
		
		for (int i= 0, size= fTreeItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fTreeItems.get(i);
			if (item.hasFocus())
				focusItem= item;
			
			item.setSelected(false);
		}
		
		if (l == null)
			return;
	
		for (Iterator iterator= l.iterator(); iterator.hasNext();) {
			Object element= iterator.next();
			BreadcrumbItem item= (BreadcrumbItem) doFindItem(element);
			if (item != null) {
				item.setSelected(true);
				fSelectedItem= item;
				if (item == focusItem) {
					item.setFocus(true);
				}
			}
		}
	}

	/**
	 * Set a single selection to the given item. <code>null</code>
	 * to deselect all.
	 * 
	 * @param item the item to select or <code>null</code>
	 */
	void selectItem(BreadcrumbItem item) {
		if (fSelectedItem != null)
			fSelectedItem.setSelected(false);

		fSelectedItem= item;
		setSelectionToWidget(getSelection(), false);

		if (item != null) {
			setFocus();
		} else {
			for (int i= 0, size= fTreeItems.size(); i < size; i++) {
				BreadcrumbItem listItem= (BreadcrumbItem) fTreeItems.get(i);
				listItem.setFocus(false);
			}
		}
		
		fireSelectionChanged(new SelectionChangedEvent(this, getSelection()));
	}

	/**
	 * Notify all double click listeners
	 */
	void fireDoubleClick() {
		fireDoubleClick(new DoubleClickEvent(this, getSelection()));
	}
	
	/**
	 * Notify all open listeners
	 */
	void fireOpen() {
		fireOpen(new OpenEvent(this, getSelection()));
	}

	/**
	 * The given element was selected from a drop down menu.
	 * 
	 * @param element the selected element
	 */
	void fireMenuSelection(Object element) {
		setInput(element);
		BreadcrumbItem item= (BreadcrumbItem) doFindItem(element);
		selectItem(item);
		BreadcrumbItem leaf= (BreadcrumbItem) fTreeItems.get(fTreeItems.size() - 1);
		if (leaf != item)
			leaf.openDropDownMenu(null);
		
	}
	
	/**
	 * Set selection to the next element if <code>next</code> is true, to
	 * the previous otherwise, if possible.
	 * 
	 * @param next should the next or the previous elmeent be selected
	 */
	void doTraverse(boolean next) {
		if (fSelectedItem == null)
			return;

		int index= fTreeItems.indexOf(fSelectedItem);
		if (next) {
			if (index == fTreeItems.size() - 1)
				return;

			BreadcrumbItem nextItem= (BreadcrumbItem) fTreeItems.get(index + 1);
			if (index == fTreeItems.size() - 2 && nextItem.getData() == null) {
				nextItem.openDropDownMenu(null);
			} else {
				selectItem(nextItem);
			}
		} else {
			if (index == 0)
				return;

			selectItem((BreadcrumbItem) fTreeItems.get(index - 1));
		}
	}
	
	/**
	 * Generates the parent chain of the given element.
	 * 
	 * @param element element to build the parent chain for
	 * @return the first index of an item in fTreeItems which is not part of the chain
	 */
	private int buildItemChain(Object element) {
		ITreeContentProvider contentProvider= (ITreeContentProvider) getContentProvider();
		Object parent= contentProvider.getParent(element);
		if (parent == null)
			return 0;
	
		int index= buildItemChain(parent);
	
		BreadcrumbItem item;
		if (index < fTreeItems.size()) {
			item= (BreadcrumbItem) fTreeItems.get(index);
			if (item.getData() != null)
				unmapElement(item.getData());
		} else {
			item= createItem();
			fTreeItems.add(item);
		}
	
		if (item == fSelectedItem && element != item.getData()) {
			selectItem(null);
		}
		item.setInput(parent);
		item.setData(element);
		mapElement(element, item);
	
		return index + 1;
	}

	/**
	 * @return new instance of a list tree item
	 */
	private BreadcrumbItem createItem() {
		BreadcrumbItem result= new BreadcrumbItem(this, fContainer);
	
		result.setLabelProvider((ILabelProvider) getLabelProvider());
		result.setContentProvider((ITreeContentProvider) getContentProvider());
	
		return result;
	}

	/**
	 * Update the size of the items such that all items are visible, if possible
	 * 
	 * @return true if any item has changed, false otherwise
	 */
	private boolean updateSize() {
		int width= fContainer.getClientArea().width;
	
		int currentWidth= getCurrentWidth();
	
		boolean requiresLayout= false;
	
		if (currentWidth > width) {
			int index= 0;
			while (currentWidth > width && index < fTreeItems.size() - 1) {
				BreadcrumbItem viewer= (BreadcrumbItem) fTreeItems.get(index);
				if (viewer.isShowText()) {
					viewer.setShowText(false);
					currentWidth= getCurrentWidth();
					requiresLayout= true;
				}
	
				index++;
			}
	
		} else if (currentWidth < width) {
	
			int index= fTreeItems.size() - 1;
			while (currentWidth < width && index >= 0) {
	
				BreadcrumbItem viewer= (BreadcrumbItem) fTreeItems.get(index);
				if (!viewer.isShowText()) {
					viewer.setShowText(true);
					currentWidth= getCurrentWidth();
					if (currentWidth > width) {
						viewer.setShowText(false);
						index= 0;
					} else {
						requiresLayout= true;
					}
				}
	
				index--;
			}
		}
	
		return requiresLayout;
	}

	/**
	 * @return the width of all items in the list
	 */
	private int getCurrentWidth() {
		int result= 0;
		for (int i= 0, size= fTreeItems.size(); i < size; i++) {
			BreadcrumbItem viewer= (BreadcrumbItem) fTreeItems.get(i);
			result+= viewer.getWidth();
		}
	
		return result;
	}

	private boolean containsAny(Object[] elements) {
		for (int i= 0, size= fTreeItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fTreeItems.get(i);
			Object child= item.getData();
			if (contains(elements, child))
				return true;
		}

		return false;
	}

	private boolean contains(Object[] elements, Object element) {
		if (element == null)
			return false;

		for (int i= 0; i < elements.length; i++) {
			Object changedElement= elements[i];
			if (changedElement == element)
				return true;

			if (element.equals(changedElement))
				return true;
		}
		return false;
	}

}
