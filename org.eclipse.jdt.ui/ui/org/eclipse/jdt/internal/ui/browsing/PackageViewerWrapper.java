/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
	Daniel Megert - Initial API
**********************************************************************/

package org.eclipse.jdt.internal.ui.browsing;

import java.util.List;

import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IPackageFragment;

/**
 * Wrapper who transfers listneres and filters and to which clients
 * can refer.
 * 
 * @deprecated needs to be replaced by a manager who handles transfer of listneres and filters
 */
class PackageViewerWrapper extends StructuredViewer {

	private StructuredViewer fViewer;
	private ListenerList fListenerList;
	private ListenerList fSelectionChangedListenerList;
	private ListenerList fPostSelectionChangedListenerList;

	public PackageViewerWrapper() {
		fListenerList= new ListenerList();
		fPostSelectionChangedListenerList= new ListenerList();
		fSelectionChangedListenerList= new ListenerList();
	}

	public void setViewer(StructuredViewer viewer) {
		Assert.isNotNull(viewer);

		StructuredViewer oldViewer= fViewer;
		this.fViewer= viewer;

		transferFilters(oldViewer);
		transferListeners();
	}

	StructuredViewer getViewer(){
		return fViewer;
	}

	private void transferFilters(StructuredViewer oldViewer) {
		//set filters
		if (oldViewer != null) {
			ViewerFilter[] filters= oldViewer.getFilters();
			for (int i= 0; i < filters.length; i++) {
				ViewerFilter filter= filters[i];
				fViewer.addFilter(filter);
			}
		}
	}

	private void transferListeners() {

		Object[] listeners= fPostSelectionChangedListenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			Object object= listeners[i];
			ISelectionChangedListener listener= (ISelectionChangedListener)object;
			fViewer.addPostSelectionChangedListener(listener);
		}

		listeners= fSelectionChangedListenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			Object object= listeners[i];
			ISelectionChangedListener listener= (ISelectionChangedListener)object;
			fViewer.addSelectionChangedListener(listener);
		}

		// Add all other listeners
		listeners= fListenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			Object object= listeners[i];

			if (object instanceof IOpenListener) {
				IOpenListener listener= (IOpenListener) object;
				addOpenListener(listener);
			} else if (object instanceof HelpListener) {
				HelpListener listener= (HelpListener) object;
				addHelpListener(listener);
			} else if (object instanceof IDoubleClickListener) {
				IDoubleClickListener listener= (IDoubleClickListener) object;
				addDoubleClickListener(listener);
			}
		}
	}

	public void reveal(Object element) {
		fViewer.reveal(element);
	}

	/**
	 * @see org.eclipse.jface.viewers.StructuredViewer#setSelectionToWidget(java.util.List, boolean)
	 */
	protected void setSelectionToWidget(List l, boolean reveal) {
	}

	/**
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	public Control getControl() {
		return fViewer.getControl();
	}

	/**
	 * @see org.eclipse.jface.viewers.StructuredViewer#addFilter(org.eclipse.jface.viewers.ViewerFilter)
	 */
	public void addFilter(ViewerFilter filter) {
		fViewer.addFilter(filter);
	}

	/**
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	public void refresh() {
		fViewer.refresh();
	}

	/**
	 * @see org.eclipse.jface.viewers.StructuredViewer#removeFilter(org.eclipse.jface.viewers.ViewerFilter)
	 */
	public void removeFilter(ViewerFilter filter) {
		fViewer.removeFilter(filter);
	}

	/**
	 * @see org.eclipse.jface.viewers.StructuredViewer#addPostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListenerList.add(listener);
		fViewer.addPostSelectionChangedListener(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#removePostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		fViewer.removePostSelectionChangedListener(listener);
		fPostSelectionChangedListenerList.remove(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListenerList.add(listener);
		fViewer.addSelectionChangedListener(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fViewer.removeSelectionChangedListener(listener);
		fSelectionChangedListenerList.remove(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		return fViewer.getSelection();
	}

	/*
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	public void setSelection(ISelection selection, boolean reveal) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel= (IStructuredSelection) selection;

			//try and give the two a common super class
			IContentProvider provider= fViewer.getContentProvider();
			if (provider instanceof LogicalPackgesContentProvider) {
				LogicalPackgesContentProvider fprovider= (LogicalPackgesContentProvider) provider;

				Object object= sel.getFirstElement();
				if (object instanceof IPackageFragment) {
					IPackageFragment pkgFragment= (IPackageFragment)object;
					LogicalPackage logicalPkg= fprovider.findLogicalPackage(pkgFragment);
					if (logicalPkg != null)
						object= logicalPkg;
					else
						object= pkgFragment;
				}
				if (object != null)	
					fViewer.setSelection(new StructuredSelection(object), reveal);
				else
					fViewer.setSelection(StructuredSelection.EMPTY, reveal);
			}
		} else
			fViewer.setSelection(selection, reveal);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#addDoubleClickListener(org.eclipse.jface.viewers.IDoubleClickListener)
	 */
	public void addDoubleClickListener(IDoubleClickListener listener) {
		fViewer.addDoubleClickListener(listener);
		fListenerList.add(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#addOpenListener(org.eclipse.jface.viewers.IOpenListener)
	 */
	public void addOpenListener(IOpenListener listener) {
		fViewer.addOpenListener(listener);
		fListenerList.add(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#refresh(boolean)
	 */
	public void refresh(boolean updateLabels) {
		fViewer.refresh(updateLabels);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#refresh(java.lang.Object, boolean)
	 */
	public void refresh(Object element, boolean updateLabels) {
		fViewer.refresh(element, updateLabels);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#refresh(java.lang.Object)
	 */
	public void refresh(Object element) {
		fViewer.refresh(element);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#removeDoubleClickListener(org.eclipse.jface.viewers.IDoubleClickListener)
	 */
	public void removeDoubleClickListener(IDoubleClickListener listener) {
		fViewer.removeDoubleClickListener(listener);
		fListenerList.remove(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#removeOpenListener(org.eclipse.jface.viewers.IOpenListener)
	 */
	public void removeOpenListener(IOpenListener listener) {
		fViewer.removeOpenListener(listener);
		fListenerList.remove(listener);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#resetFilters()
	 */
	public void resetFilters() {
		fViewer.resetFilters();
	}

	/*
	 * @see org.eclipse.jface.viewers.ContentViewer#setContentProvider(org.eclipse.jface.viewers.IContentProvider)
	 */
	public void setContentProvider(IContentProvider contentProvider) {
		fViewer.setContentProvider(contentProvider);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#setSorter(org.eclipse.jface.viewers.ViewerSorter)
	 */
	public void setSorter(ViewerSorter sorter) {
		fViewer.setSorter(sorter);
	}

	public void setUseHashlookup(boolean enable) {
		fViewer.setUseHashlookup(enable);
	}

	public void addHelpListener(HelpListener listener) {
		fViewer.addHelpListener(listener);
		fListenerList.add(listener);
	}

	public void removeHelpListener(HelpListener listener) {
		fListenerList.remove(listener);
		fViewer.removeHelpListener(listener);
	}

	// --------- simply delegate to wrapped viewer ---------
	public Widget testFindItem(Object element) {
		return fViewer.testFindItem(element);
	}

	public void update(Object element, String[] properties) {
		fViewer.update(element, properties);
	}

	public void update(Object[] elements, String[] properties) {
		fViewer.update(elements, properties);
	}

	public IContentProvider getContentProvider() {
		return fViewer.getContentProvider();
	}

	public Object getInput() {
		return fViewer.getInput();
	}

	public IBaseLabelProvider getLabelProvider() {
		return fViewer.getLabelProvider();
	}

	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		fViewer.setLabelProvider(labelProvider);
	}

	public Object getData(String key) {
		return fViewer.getData(key);
	}

	public Item scrollDown(int x, int y) {
		return fViewer.scrollDown(x, y);
	}

	public Item scrollUp(int x, int y) {
		return fViewer.scrollUp(x, y);
	}

	public void setData(String key, Object value) {
		fViewer.setData(key, value);
	}

	public void setSelection(ISelection selection) {
		fViewer.setSelection(selection);
	}

	public boolean equals(Object obj) {
		return fViewer.equals(obj);
	}

	public int hashCode() {
		return fViewer.hashCode();
	}

	public String toString() {
		return fViewer.toString();
	}

	public void setViewerInput(Object input){
		fViewer.setInput(input);
	}

	// need to provide implementation for abstract methods
	protected Widget doFindInputItem(Object element) {return null;}
	protected Widget doFindItem(Object element) {return null;}
	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {}
	protected List getSelectionFromWidget() {return null;}
	protected void internalRefresh(Object element) {}
}
