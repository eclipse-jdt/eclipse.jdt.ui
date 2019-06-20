/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.viewsupport.ProblemTableViewer;

/**
 * Special problem table viewer to handle logical packages.
 */
class PackagesViewTableViewer extends ProblemTableViewer implements IPackagesViewViewer {

	public PackagesViewTableViewer(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void mapElement(Object element, Widget item) {
		if (element instanceof LogicalPackage && item instanceof Item) {
			LogicalPackage cp= (LogicalPackage) element;
			for (IPackageFragment fragment : cp.getFragments()) {
				fResourceToItemsMapper.addToMap(fragment, (Item)item);
			}
		}
		super.mapElement(element, item);
	}

	@Override
	public void unmapElement(Object element, Widget item) {
		if (element instanceof LogicalPackage && item instanceof Item) {
			LogicalPackage cp= (LogicalPackage) element;
			for (IPackageFragment fragment : cp.getFragments()) {
				fResourceToItemsMapper.removeFromMap(fragment, (Item)item);
			}
		}
		super.unmapElement(element, item);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#getFilteredChildren(java.
	 * lang.Object)
	 */
	@Override
	protected Object[] getFilteredChildren(Object parent) {

		Object[] result= getRawChildren(parent);
		List<Object> list= new ArrayList<>();
		if (result != null) {
			Object[] toBeFiltered= new Object[1];
			for (Object object : result) {
				if(object instanceof LogicalPackage) {
					if(selectLogicalPackage((LogicalPackage)object))
						list.add(object);
				} else {
					toBeFiltered[0]= object;
					if (filter(toBeFiltered).length == 1)
						list.add(object);
				}
			}
		}
		return list.toArray();
	}

	private boolean selectLogicalPackage(LogicalPackage logicalPackage) {
		return filter(logicalPackage.getFragments()).length > 0;
	}

	// --------- see IPackagesViewViewer ----------

	@Override
	public Widget doFindItem(Object element){
		return super.doFindItem(element);
	}

	@Override
	public Widget doFindInputItem(Object element){
		return super.doFindInputItem(element);
	}

	@Override
	public List getSelectionFromWidget(){
		return super.getSelectionFromWidget();
	}

	@Override
	public void doUpdateItem(Widget item, Object element, boolean fullMap){
		super.doUpdateItem(item, element, fullMap);
	}

	@Override
	public void internalRefresh(Object element){
		super.internalRefresh(element);
	}

	@Override
	public void setSelectionToWidget(List l, boolean reveal){
		super.setSelectionToWidget(l, reveal);
	}
}
