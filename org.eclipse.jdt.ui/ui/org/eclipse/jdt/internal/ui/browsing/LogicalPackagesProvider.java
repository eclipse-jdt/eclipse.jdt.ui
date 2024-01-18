/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

abstract class LogicalPackagesProvider implements IPropertyChangeListener, IElementChangedListener, IContentProvider {

	protected static final Object[] NO_CHILDREN= new Object[0];

	protected Map<String, LogicalPackage> fMapToLogicalPackage;
	protected Map<String, IPackageFragment> fMapToPackageFragments;
	protected boolean fCompoundState;
	protected StructuredViewer fViewer;
	protected boolean fInputIsProject;

	public LogicalPackagesProvider(StructuredViewer viewer){
		fViewer= viewer;
		fCompoundState= isInCompoundState();
		fInputIsProject= true;
		fMapToLogicalPackage= new HashMap<>();
		fMapToPackageFragments= new HashMap<>();
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * Adds the given fragments to the internal map.
	 * Existing fragments will be replaced by the new ones.
	 *
	 * @param packageFragments the package fragments to add
	 */
	protected void addFragmentsToMap(IPackageFragment[] packageFragments) {
		for (IPackageFragment fragment : packageFragments) {
			String key= getKey(fragment);
			fMapToPackageFragments.put(key, fragment);
		}
	}

	protected String getKey(IPackageFragment fragment) {
		return fragment.getElementName() + fragment.getJavaProject().getElementName();
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
		if (isInCompoundState())
			return fMapToLogicalPackage.get(getKey(fragment));
		else
			return null;
	}

	/**
	 * Combines packages with same names into a logical package which will
	 * be added to the resulting array. If a package is not yet in this content
	 * provider then the package fragment is added to the resulting array.
	 *
	 * @param packageFragments the package fragments to combine
	 * @return an array with combined (logical) packages and package fragments
	 */
	protected Object[] combineSamePackagesIntoLogialPackages(IPackageFragment[] packageFragments) {

		if (!fCompoundState)
			return packageFragments;

		List<IAdaptable> newChildren= new ArrayList<>();

		for (IPackageFragment fragment : packageFragments) {
			if (fragment == null)
				continue;

			LogicalPackage lp= findLogicalPackage(fragment);

			if (lp != null) {
				if (lp.belongs(fragment)) {
					lp.add(fragment);
				}
				if(!newChildren.contains(lp))
					newChildren.add(lp);

			} else {
				String key= getKey(fragment);
				IPackageFragment frag= fMapToPackageFragments.get(key);
				if (frag != null && !fragment.equals(frag)) {
					lp= new LogicalPackage(frag);
					lp.add(fragment);
					newChildren.remove(frag);
					newChildren.add(lp);
					fMapToLogicalPackage.put(key, lp);
					fMapToPackageFragments.remove(key);
				} else {
					fMapToPackageFragments.put(key, fragment);
					newChildren.add(fragment);
				}
			}
		}
		return newChildren.toArray();
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (fCompoundState == isInCompoundState())
			return;
		else
			fCompoundState= isInCompoundState();

		if (!isInCompoundState()) {
			fMapToLogicalPackage.clear();
			fMapToPackageFragments.clear();
		}

		if(fViewer instanceof TreeViewer){
			TreeViewer viewer= (TreeViewer) fViewer;
			Object[] expandedObjects= viewer.getExpandedElements();
			viewer.refresh();
			viewer.setExpandedElements(expandedObjects);
		} else
			fViewer.refresh();
	}

	protected boolean isInCompoundState() {
		// XXX: for now we don't offer a preference might become a view menu entry
		//		return AppearancePreferencePage.logicalPackagesInPackagesView();
		return true;
	}

	@Override
	public void dispose(){
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		fMapToLogicalPackage= null;
		fMapToPackageFragments= null;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput != null) {
			JavaCore.addElementChangedListener(this);
		} else {
			JavaCore.removeElementChangedListener(this);
		}
		fInputIsProject= (newInput instanceof IJavaProject);

		if(viewer instanceof StructuredViewer)
			fViewer= (StructuredViewer)viewer;
	}

	abstract protected void processDelta(IJavaElementDelta delta) throws JavaModelException;

	/*
	 * @since 3.0
	 */
	protected boolean isClassPathChange(IJavaElementDelta delta) {

		// need to test the flags only for package fragment roots
		if (delta.getElement().getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT)
			return false;

		int flags= delta.getFlags();
		return (delta.getKind() == IJavaElementDelta.CHANGED &&
			((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) ||
			 ((flags & IJavaElementDelta.F_REORDER) != 0));
	}

	/*
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	@Override
	public void elementChanged(ElementChangedEvent event) {
		try {
			processDelta(event.getDelta());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

}
