/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.ProblemTreeViewer;
	
/**
 * Special problem tree viewer to handle logical packages.
 */
public class PackagesViewTreeViewer extends ProblemTreeViewer implements IPackagesViewViewer{

	public PackagesViewTreeViewer(Composite parent, int style) {
		super(parent, style);
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#mapElement(java.lang.Object, org.eclipse.swt.widgets.Widget)
	 */
	public void mapElement(Object element, Widget item) {
		if (element instanceof LogicalPackage && item instanceof Item) {
			LogicalPackage cp= (LogicalPackage) element;
			IPackageFragment[] fragments= cp.getFragments();
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				fResourceToItemsMapper.addToMap(fragment, (Item) item);
			}
		}
		super.mapElement(element, item);		
	}

	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#unmapElement(java.lang.Object, org.eclipse.swt.widgets.Widget)
	 */
	public void unmapElement(Object element, Widget item) {

		if (element instanceof LogicalPackage && item instanceof Item) {
			LogicalPackage cp= (LogicalPackage) element;
			IPackageFragment[] fragments= cp.getFragments();
			for (int i= 0; i < fragments.length; i++) {
				IPackageFragment fragment= fragments[i];
				fResourceToItemsMapper.removeFromMap((Object)fragment, (Item)item);
			}	
		}
		super.unmapElement(element, item);
	}
	
	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#getFilteredChildren(java.lang.Object)
	 */
	protected Object[] getFilteredChildren(Object parent) {
		List list= new ArrayList();
		Object[] result= getRawChildren(parent);
		if (result != null)	{
			Object[] toBeFiltered= new Object[1];
			for (int i= 0; i < result.length; i++) {
				Object object= result[i];
				toBeFiltered[0]= object;
				if(object instanceof LogicalPackage) {	 
					if(filterLogicalPackages((LogicalPackage)object))
						list.add(object);	
				} else if (isEssential(object) || filter(toBeFiltered).length == 1)
					list.add(object);
			}
		}
		return list.toArray();
	}
	
	
	/*
	 * @see org.eclipse.jface.viewers.StructuredViewer#filter(java.lang.Object[])
	 * @since 3.0
	 */
	protected Object[] filter(Object[] elements) {
		ViewerFilter[] filters= getFilters();
		if (filters == null || filters.length == 0)
			return elements;
		
		ArrayList filtered= new ArrayList(elements.length);
		Object root= getRoot();
		for (int i= 0; i < elements.length; i++) {
			boolean add= true;
			if (!isEssential(elements[i])) {
				for (int j = 0; j < filters.length; j++) {
					add= filters[j].select(this, root,
						elements[i]);
					if (!add)
						break;
				}
			}
			if (add)
				filtered.add(elements[i]);
		}
		return filtered.toArray();
	}
	
	/*
	 * @see AbstractTreeViewer#isExpandable(java.lang.Object)
	 */
	public boolean isExpandable(Object parent) {
		Object[] children= ((ITreeContentProvider)getContentProvider()).getChildren(parent);
		Object[] toBeFiltered= new Object[1];
		for (int i = 0; i < children.length; i++) {
			Object object= children[i];
			
			if (isEssential(object))
				return true;
			
			toBeFiltered[0]= object;
			Object[] filtered= filter(toBeFiltered);
			if (filtered.length > 0)
				return true;
		}
		return false;
	}
	
	private boolean isEssential(Object object) {
		try {
			if (object instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) object;
				return !fragment.isDefaultPackage() && fragment.hasSubpackages();
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		
		return false;
	}

	private boolean filterLogicalPackages(LogicalPackage logicalPackage) {
		IPackageFragment[] fragments= logicalPackage.getFragments();
		Object[] toBeFiltered= new Object[1];
		for (int i= 0; i < fragments.length; i++) {
			IPackageFragment fragment= fragments[i];
			toBeFiltered[0]= fragment;
			if(isEssential(fragment) || filter(toBeFiltered).length != 0)
				return true;
		}
		return false;
	}
		
	// --------- see: IPackagesViewViewer ----------
	
	public Widget doFindItem(Object element) {
		return super.doFindItem(element);
	}

	public Widget doFindInputItem(Object element) {
		return super.doFindInputItem(element);
	}

	public List getSelectionFromWidget() {
		return super.getSelectionFromWidget();
	}
	
	public void doUpdateItem(Widget item, Object element, boolean fullMap){
		super.doUpdateItem(item, element, fullMap);
	}
	
	public void internalRefresh(Object element){
		super.internalRefresh(element);
	}
	
	public void setSelectionToWidget(List l, boolean reveal){
		super.setSelectionToWidget(l, reveal);	
	}
}
