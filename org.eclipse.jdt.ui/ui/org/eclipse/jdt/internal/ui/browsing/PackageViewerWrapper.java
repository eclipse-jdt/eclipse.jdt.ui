/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.browsing;

import java.util.List;

import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.IPackageFragment;

/**
 * Wrapper who transfers listeners and filters and to which clients
 * can refer.
 *
 * @deprecated needs to be replaced by a manager who handles transfer of listeners and filters
 */
@Deprecated
class PackageViewerWrapper extends StructuredViewer {

	private StructuredViewer fViewer;
	private ListenerList<Object> fListenerList;
	private ListenerList<ISelectionChangedListener> fSelectionChangedListenerList;
	private ListenerList<ISelectionChangedListener> fPostSelectionChangedListenerList;

	@Deprecated
	public PackageViewerWrapper() {
		fListenerList= new ListenerList<>(ListenerList.IDENTITY);
		fPostSelectionChangedListenerList= new ListenerList<>(ListenerList.IDENTITY);
		fSelectionChangedListenerList= new ListenerList<>(ListenerList.IDENTITY);
	}

	@Deprecated
	public void setViewer(StructuredViewer viewer) {
		Assert.isNotNull(viewer);

		StructuredViewer oldViewer= fViewer;
		fViewer= viewer;

		if (fViewer.getContentProvider() != null)
			super.setContentProvider(fViewer.getContentProvider());
		transferFilters(oldViewer);
		transferListeners();
	}

	@Deprecated
	StructuredViewer getViewer(){
		return fViewer;
	}

	private void transferFilters(StructuredViewer oldViewer) {
		//set filters
		if (oldViewer != null) {
			for (ViewerFilter filter : oldViewer.getFilters()) {
				fViewer.addFilter(filter);
			}
		}
	}

	private void transferListeners() {

		for (Object object : fPostSelectionChangedListenerList.getListeners()) {
			ISelectionChangedListener listener= (ISelectionChangedListener)object;
			fViewer.addPostSelectionChangedListener(listener);
		}

		for (Object object : fSelectionChangedListenerList.getListeners()) {
			ISelectionChangedListener listener= (ISelectionChangedListener)object;
			fViewer.addSelectionChangedListener(listener);
		}

		// Add all other listeners
		for (Object object : fListenerList.getListeners()) {
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

	@Deprecated
	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel= (IStructuredSelection) selection;

			//try and give the two a common super class
			IContentProvider provider= getContentProvider();
			if (provider instanceof LogicalPackagesProvider) {
				LogicalPackagesProvider fprovider= (LogicalPackagesProvider) provider;

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

	@Deprecated
	@Override
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListenerList.add(listener);
		fViewer.addPostSelectionChangedListener(listener);
	}

	@Deprecated
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListenerList.add(listener);
		fViewer.addSelectionChangedListener(listener);
	}

	@Deprecated
	@Override
	public void addDoubleClickListener(IDoubleClickListener listener) {
		fViewer.addDoubleClickListener(listener);
		fListenerList.add(listener);
	}

	@Deprecated
	@Override
	public void addOpenListener(IOpenListener listener) {
		fViewer.addOpenListener(listener);
		fListenerList.add(listener);
	}

	@Deprecated
	@Override
	public void addHelpListener(HelpListener listener) {
		fViewer.addHelpListener(listener);
		fListenerList.add(listener);
	}

	@Deprecated
	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fViewer.removeSelectionChangedListener(listener);
		fSelectionChangedListenerList.remove(listener);
	}

	@Deprecated
	@Override
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		fViewer.removePostSelectionChangedListener(listener);
		fPostSelectionChangedListenerList.remove(listener);
	}

	@Deprecated
	@Override
	public void removeHelpListener(HelpListener listener) {
		fListenerList.remove(listener);
		fViewer.removeHelpListener(listener);
	}

	@Deprecated
	@Override
	public void removeDoubleClickListener(IDoubleClickListener listener) {
		fViewer.removeDoubleClickListener(listener);
		fListenerList.remove(listener);
	}

	@Deprecated
	@Override
	public void removeOpenListener(IOpenListener listener) {
		fViewer.removeOpenListener(listener);
		fListenerList.remove(listener);
	}

	// --------- simply delegate to wrapped viewer ---------
	@Deprecated
	@Override
	public Control getControl() {
		return fViewer.getControl();
	}

	@Deprecated
	@Override
	public void addFilter(ViewerFilter filter) {
		fViewer.addFilter(filter);
	}

	@Deprecated
	@Override
	public void setFilters(ViewerFilter... filters) {
		fViewer.setFilters(filters);
	}

	@Deprecated
	@Override
	public ViewerFilter[] getFilters() {
		return fViewer.getFilters();
	}

	@Deprecated
	@Override
	public void refresh() {
		fViewer.refresh();
	}

	@Deprecated
	@Override
	public void removeFilter(ViewerFilter filter) {
		fViewer.removeFilter(filter);
	}

	@Deprecated
	@Override
	public ISelection getSelection() {
		return fViewer.getSelection();
	}

	@Deprecated
	@Override
	public void refresh(boolean updateLabels) {
		fViewer.refresh(updateLabels);
	}

	@Deprecated
	@Override
	public void refresh(Object element, boolean updateLabels) {
		fViewer.refresh(element, updateLabels);
	}

	@Deprecated
	@Override
	public void refresh(Object element) {
		fViewer.refresh(element);
	}

	@Deprecated
	@Override
	public void resetFilters() {
		fViewer.resetFilters();
	}

	@Deprecated
	@Override
	public void reveal(Object element) {
		fViewer.reveal(element);
	}

	@Deprecated
	@Override
	public void setContentProvider(IContentProvider contentProvider) {
		fViewer.setContentProvider(contentProvider);
	}

	@Deprecated
	@Override
	public void setSorter(ViewerSorter sorter) {
		fViewer.setSorter(sorter);
	}

	@Deprecated
	@Override
	public void setComparator(ViewerComparator comparator) {
		fViewer.setComparator(comparator);
	}

	@Deprecated
	@Override
	public void setUseHashlookup(boolean enable) {
		fViewer.setUseHashlookup(enable);
	}

	@Deprecated
	@Override
	public Widget testFindItem(Object element) {
		return fViewer.testFindItem(element);
	}

	@Deprecated
	@Override
	public void update(Object element, String[] properties) {
		fViewer.update(element, properties);
	}

	@Deprecated
	@Override
	public void update(Object[] elements, String[] properties) {
		fViewer.update(elements, properties);
	}

	@Deprecated
	@Override
	public IContentProvider getContentProvider() {
		return fViewer.getContentProvider();
	}

	@Deprecated
	@Override
	public Object getInput() {
		return fViewer.getInput();
	}

	@Deprecated
	@Override
	public IBaseLabelProvider getLabelProvider() {
		return fViewer.getLabelProvider();
	}

	@Deprecated
	@Override
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		fViewer.setLabelProvider(labelProvider);
	}

	@Deprecated
	@Override
	public Object getData(String key) {
		return fViewer.getData(key);
	}

	@Deprecated
	@Override
	public Item scrollDown(int x, int y) {
		return fViewer.scrollDown(x, y);
	}

	@Deprecated
	@Override
	public Item scrollUp(int x, int y) {
		return fViewer.scrollUp(x, y);
	}

	@Deprecated
	@Override
	public void setData(String key, Object value) {
		fViewer.setData(key, value);
	}

	@Deprecated
	@Override
	public void setSelection(ISelection selection) {
		fViewer.setSelection(selection);
	}

	@Deprecated
	@Override
	public boolean equals(Object obj) {
		return fViewer.equals(obj);
	}

	@Deprecated
	@Override
	public int hashCode() {
		return fViewer.hashCode();
	}

	@Deprecated
	@Override
	public String toString() {
		return fViewer.toString();
	}

	@Deprecated
	public void setViewerInput(Object input){
		fViewer.setInput(input);
	}

	// need to provide implementation for abstract methods
	@Deprecated
	@Override
	protected Widget doFindInputItem(Object element) {
		return ((IPackagesViewViewer) fViewer).doFindInputItem(element);
	}

	@Deprecated
	@Override
	protected Widget doFindItem(Object element) {
		return ((IPackagesViewViewer)fViewer).doFindItem(element);
	}

	@Deprecated
	@Override
	protected void doUpdateItem(Widget item, Object element, boolean fullMap) {
		((IPackagesViewViewer)fViewer).doUpdateItem(item, element, fullMap);
	}

	@Deprecated
	@Override
	protected List getSelectionFromWidget() {
		return ((IPackagesViewViewer)fViewer).getSelectionFromWidget();
	}

	@Deprecated
	@Override
	protected void internalRefresh(Object element) {
		((IPackagesViewViewer)fViewer).internalRefresh(element);
	}

	@Deprecated
	@Override
	protected void setSelectionToWidget(List l, boolean reveal) {
		((IPackagesViewViewer) fViewer).setSelectionToWidget(l, reveal);
	}

	@Deprecated
	@Override
	public ViewerComparator getComparator() {
		return fViewer.getComparator();
	}

	@Deprecated
	@Override
	public IElementComparer getComparer() {
		return fViewer.getComparer();
	}

	@Deprecated
	@Override
	public ViewerSorter getSorter() {
		return fViewer.getSorter();
	}

	@Deprecated
	@Override
	public void setComparer(IElementComparer comparer) {
		fViewer.setComparer(comparer);
	}

	@Deprecated
	@Override
	public void addDragSupport(int operations, Transfer[] transferTypes, DragSourceListener listener) {
		fViewer.addDragSupport(operations, transferTypes, listener);
	}

	@Deprecated
	@Override
	public void addDropSupport(int operations, Transfer[] transferTypes, DropTargetListener listener) {
		fViewer.addDropSupport(operations, transferTypes, listener);
	}

	@Deprecated
	@Override
	public Widget[] testFindItems(Object element) {
		return fViewer.testFindItems(element);
	}


}
