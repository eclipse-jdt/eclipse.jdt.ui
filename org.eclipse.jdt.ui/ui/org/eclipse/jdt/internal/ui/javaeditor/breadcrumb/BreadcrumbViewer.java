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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerLabel;

import org.eclipse.ui.forms.FormColors;


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
public abstract class BreadcrumbViewer extends StructuredViewer {
	
	private final Composite fContainer;
	private final ArrayList fBreadcrumbItems;

	private Image fGradientBackground;
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
		fBreadcrumbItems= new ArrayList();

		fContainer= new Composite(parent, SWT.NONE);
		GridData layoutData= new GridData(SWT.FILL, SWT.TOP, true, false);
		fContainer.setLayoutData(layoutData);
		fContainer.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				e.doit= true;
			}
		});
		fContainer.setBackgroundMode(SWT.INHERIT_DEFAULT);

		fContainer.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				int height= fContainer.getClientArea().height;
				
				if (fGradientBackground == null || fGradientBackground.getBounds().height != height) {
					Image image= createGradientImage(height, event.display);
					fContainer.setBackgroundImage(image);
					
					if (fGradientBackground != null)
						fGradientBackground.dispose();
					fGradientBackground= image;
				}				
			}
		});
		fContainer.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fGradientBackground.dispose();
			}
		});

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
	
	/**
	 * Return a new tree content provider which will be used for the drop
	 * down menu for the given root element.
	 * 
	 * @param root the root element of the drop down menu
	 * @return a new tree content provider
	 */
	public abstract ITreeContentProvider createDropDownContentProvider(Object root);

	/**
	 * Return a new label provider which will be used for the drop
	 * down menu for the given root element.
	 * 
	 * @param root the root element of the drop down menu
	 * @return a new label provider
	 */
	public abstract ILabelProvider createDropDownLabelProvider(Object root);

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	public Control getControl() {
		return fContainer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#getRoot()
	 */
	protected Object getRoot() {
		if (fBreadcrumbItems.isEmpty())
			return null;

		return ((BreadcrumbItem) fBreadcrumbItems.get(0)).getData();
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
			if (fBreadcrumbItems.size() == 0)
				return;

			BreadcrumbItem item= (BreadcrumbItem) fBreadcrumbItems.get(fBreadcrumbItems.size() - 1);
			if (item.getData() == null) {
				if (fBreadcrumbItems.size() < 2)
					return;

				item= (BreadcrumbItem) fBreadcrumbItems.get(fBreadcrumbItems.size() - 2);
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
		for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fBreadcrumbItems.get(i);
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
			while (lastIndex < fBreadcrumbItems.size()) {
				BreadcrumbItem item= (BreadcrumbItem) fBreadcrumbItems.remove(fBreadcrumbItems.size() - 1);
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
	
		for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fBreadcrumbItems.get(i);
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
				for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
					BreadcrumbItem item1= (BreadcrumbItem) fBreadcrumbItems.get(i);
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
		
		for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fBreadcrumbItems.get(i);
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
			for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
				BreadcrumbItem listItem= (BreadcrumbItem) fBreadcrumbItems.get(i);
				listItem.setFocus(false);
			}
		}
		
		fireSelectionChanged(new SelectionChangedEvent(this, getSelection()));
	}

	/**
	 * @return number of items shown in the viewer
	 */
	int getItemCount() {
		return fBreadcrumbItems.size();
	}

	/**
	 * @param index the index of the item
	 * @return the item ad the given <code>index</code>
	 */
	BreadcrumbItem getItem(int index) {
		return (BreadcrumbItem) fBreadcrumbItems.get(index);
	}
	
	/**
	 * @param item the item to search
	 * @return the index of the item or -1 if not found
	 */
	int getIndexOfItem(BreadcrumbItem item) {
		for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
			BreadcrumbItem pItem= (BreadcrumbItem) fBreadcrumbItems.get(i);
			if (pItem == item)
				return i;
		}

		return -1;
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
		fireOpen(new OpenEvent(this, new StructuredSelection(element)));
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

		int index= fBreadcrumbItems.indexOf(fSelectedItem);
		if (next) {
			if (index == fBreadcrumbItems.size() - 1) {
				BreadcrumbItem current= (BreadcrumbItem) fBreadcrumbItems.get(index);
				current.openDropDownMenu();
			} else {
				BreadcrumbItem nextItem= (BreadcrumbItem) fBreadcrumbItems.get(index + 1);
				selectItem(nextItem);
			}
		} else {
			if (index == 1) {
				BreadcrumbItem root= (BreadcrumbItem) fBreadcrumbItems.get(0);
				root.openDropDownMenu();
			} else {
				selectItem((BreadcrumbItem) fBreadcrumbItems.get(index - 1));
			}
		}
	}
	
	/**
	 * Generates the parent chain of the given element.
	 * 
	 * @param element element to build the parent chain for
	 * @return the first index of an item in fBreadcrumbItems which is not part of the chain
	 */
	private int buildItemChain(Object element) {
		if (element == null)
			return 0;
		
		ITreeContentProvider contentProvider= (ITreeContentProvider) getContentProvider();
		Object parent= contentProvider.getParent(element);
	
		int index= buildItemChain(parent);
	
		BreadcrumbItem item;
		if (index < fBreadcrumbItems.size()) {
			item= (BreadcrumbItem) fBreadcrumbItems.get(index);
			if (item.getData() != null)
				unmapElement(item.getData());
		} else {
			item= createItem();
			fBreadcrumbItems.add(item);
		}
	
		if (item == fSelectedItem && element != item.getData()) {
			selectItem(null);
		}
		item.setData(element);
		if (parent == null) {
			//don't show the models root
			item.setDetailsVisible(false);
		}
		
		mapElement(element, item);
	
		return index + 1;
	}

	/**
	 * @return new instance of a breadcrumb item
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
			while (currentWidth > width && index < fBreadcrumbItems.size() - 1) {
				BreadcrumbItem viewer= (BreadcrumbItem) fBreadcrumbItems.get(index);
				if (viewer.isShowText()) {
					viewer.setShowText(false);
					currentWidth= getCurrentWidth();
					requiresLayout= true;
				}
	
				index++;
			}
	
		} else if (currentWidth < width) {
	
			int index= fBreadcrumbItems.size() - 1;
			while (currentWidth < width && index >= 0) {
	
				BreadcrumbItem viewer= (BreadcrumbItem) fBreadcrumbItems.get(index);
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
		for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
			BreadcrumbItem viewer= (BreadcrumbItem) fBreadcrumbItems.get(i);
			result+= viewer.getWidth();
		}
	
		return result;
	}

	private boolean containsAny(Object[] elements) {
		for (int i= 0, size= fBreadcrumbItems.size(); i < size; i++) {
			BreadcrumbItem item= (BreadcrumbItem) fBreadcrumbItems.get(i);
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

	/**
	 * The image to use for the breadcrumb background as 
	 * specified in https://bugs.eclipse.org/bugs/show_bug.cgi?id=221477
	 * 
	 * @param height the height of the image to create
	 * @param display the current display
	 * @return the image for the breadcrumb background
	 */
	private Image createGradientImage(int height, Display display) {
		int width= 50;

		Image result= new Image(display, width, height);

		GC gc= new GC(result);

		Color colorA= createColor(SWT.COLOR_WIDGET_BACKGROUND, SWT.COLOR_LIST_BACKGROUND, 75, display);
		Color colorB= createColor(SWT.COLOR_WIDGET_BACKGROUND, SWT.COLOR_LIST_BACKGROUND, 50, display);
		Color colorC= createColor(SWT.COLOR_WIDGET_BACKGROUND, SWT.COLOR_LIST_BACKGROUND, 25, display);
		Color colorD= createColor(SWT.COLOR_WIDGET_BACKGROUND, SWT.COLOR_LIST_BACKGROUND, 45, display);
		Color colorE= createColor(SWT.COLOR_WIDGET_BACKGROUND, SWT.COLOR_LIST_BACKGROUND, 80, display);
		Color colorF= createColor(SWT.COLOR_WIDGET_BACKGROUND, SWT.COLOR_LIST_BACKGROUND, 70, display);
		Color colorG= createColor(SWT.COLOR_WIDGET_BACKGROUND, SWT.COLOR_WHITE, 45, display);
		Color colorH= createColor(SWT.COLOR_WIDGET_NORMAL_SHADOW, SWT.COLOR_LIST_BACKGROUND, 35, display);

		try {
			drawLine(width, 0, colorA, gc);
			drawLine(width, 1, colorB, gc);
			drawLine(width, 2, colorC, gc);
			drawLine(width, 3, colorC, gc);

			gc.setForeground(colorD);
			gc.setBackground(colorE);
			gc.fillGradientRectangle(0, 4, width, 4 + 8, true);

			gc.setBackground(colorE);
			gc.fillRectangle(0, 4 + 9, width, height - 4);

			drawLine(width, height - 3, colorF, gc);
			drawLine(width, height - 2, colorG, gc);
			drawLine(width, height - 1, colorH, gc);

		} finally {
			gc.dispose();

			colorA.dispose();
			colorB.dispose();
			colorC.dispose();
			colorD.dispose();
			colorE.dispose();
			colorF.dispose();
			colorG.dispose();
			colorH.dispose();
		}

		return result;
	}

	private void drawLine(int width, int position, Color color, GC gc) {
		gc.setForeground(color);
		gc.drawLine(0, position, width, position);
	}

	private Color createColor(int color1, int color2, int ratio, Display display) {
		RGB rgb1= display.getSystemColor(color1).getRGB();
		RGB rgb2= display.getSystemColor(color2).getRGB();

		RGB blend= FormColors.blend(rgb2, rgb1, ratio);

		return new Color(display, blend);
	}

}
