/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.browsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.JavaPlugin;

class LogicalPackgesContentProvider implements IPropertyChangeListener {

	protected Map fMapToCompoundElement;
	protected boolean fCompoundState;
	protected StructuredViewer fViewer;
	
	public LogicalPackgesContentProvider(StructuredViewer viewer){
		fViewer= viewer;
		fCompoundState= isInCompoundState();
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);	
	}
	
	protected void addFragmentsToMap(IPackageFragment[] children) {
		//this is important because it clears all compoundelements out of the map
		for (int i= 0; i < children.length; i++) {
			IPackageFragment fragment= children[i];
			String key= getKey(fragment);
			fMapToCompoundElement.put(key, fragment);
		}	
	}

	protected String getKey(IPackageFragment fragment) {
		return fragment.getElementName()+fragment.getJavaProject().getElementName();
	}

	/**
	 * Returns the logical package for the given package fragment
	 * or <code>null</code> if it is not grouped by logical package.
	 * 
	 * @param fragment the package fragment
	 * @return the logical package
	 */
	public LogicalPackage findLogicalPackage(IPackageFragment fragment) {
		Assert.isNotNull(fragment);
		
		if(fMapToCompoundElement == null)	
			return null;
		
		return (LogicalPackage)fMapToCompoundElement.get(getKey(fragment));
	}

	/* 
	 * @param children
	 * @return List new list of elements with packages with the same name now
	 * grouped together in compound elements.
	 */
	protected Object[] createCompoundElements(IPackageFragment[] children) {

		if (!fCompoundState)
			return children;

		List newChildren= new ArrayList();

		for (int i= 0; i < children.length; i++) {
			IPackageFragment fragment= (IPackageFragment) children[i];
			String key= getKey(fragment);
			Object object= fMapToCompoundElement.get(key);

			if (object instanceof LogicalPackage) {
				LogicalPackage element= (LogicalPackage) object;
				if (element.belongs(fragment)) {
					element.add(fragment);
				}
				if (!newChildren.contains(element))
					newChildren.add(element);

			} else if (object instanceof IPackageFragment) {
				IPackageFragment frag= (IPackageFragment) object;
				if (!fragment.equals(frag)) {
					LogicalPackage el= new LogicalPackage(frag);
					el.add(fragment);
					newChildren.remove(frag);
					newChildren.add(el);
					fMapToCompoundElement.put(key, el);
				} else
					newChildren.add(frag);
			} else if (object == null) {
				fMapToCompoundElement.put(key, fragment);
				newChildren.add(fragment);
			}
		}
		return newChildren.toArray();
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if(fCompoundState == isInCompoundState())
			return;
		else fCompoundState= isInCompoundState();
		
		if(fViewer instanceof TreeViewer){
			TreeViewer viewer= (TreeViewer) fViewer;
			Object[] expandedObjects= viewer.getExpandedElements();	
			viewer.refresh();
			viewer.setExpandedElements(expandedObjects);
		} else fViewer.refresh();
	}

	protected boolean isInCompoundState() {
		// XXX: for now we don't off a preference might become a view menu entry
		//		return AppearancePreferencePage.compoundPackagesInPackagesView();
		return true;

	}
}
