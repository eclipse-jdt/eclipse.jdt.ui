/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Content provider which provides package fragments for hierarchical
 * Package Explorer layout.
 * 
 * @since 2.1
 */
public class PackageFragmentProvider implements IPropertyChangeListener {

	private TreeViewer fViewer;
	private boolean fFoldPackages;
	
	public PackageFragmentProvider() {
		fFoldPackages= arePackagesFoldedInHierarchicalLayout();
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}
			
	
	/**
	 * Returns the hierarchical packages inside a given fragment or root.
	 * @param parent The parent package fragment root
	 * @param fragment The package to get the children for or 'null' to get the children of the root.
	 * @param result Collection where the resulting elements are added
	 * @throws JavaModelException
	 */
	public void getHierarchicalPackageChildren(IPackageFragmentRoot parent, IPackageFragment fragment, Collection result) throws JavaModelException {
		IJavaElement[] children= parent.getChildren();
		String prefix= fragment != null ? fragment.getElementName() + '.' : ""; //$NON-NLS-1$
		int prefixLen= prefix.length();
		for (int i= 0; i < children.length; i++) {
			IPackageFragment curr= (IPackageFragment) children[i];
			String name= curr.getElementName();
			if (name.startsWith(prefix) && name.length() > prefixLen && name.indexOf('.', prefixLen) == -1) {
				if (fFoldPackages) {
					curr= getFolded(children, curr);
				}
				result.add(curr);
			} else if (fragment == null && curr.isDefaultPackage()) {
				result.add(curr);
			}
		}
	}
	
	/**
	 * Returns the hierarchical packages inside a given folder.
	 * @param folder The parent folder
	 * @param result Collection where the resulting elements are added
	 * @throws JavaModelException
	 */
	public void getHierarchicalPackagesInFolder(IFolder folder, Collection result) throws CoreException {
		IResource[] resources= folder.members();
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			if (resource instanceof IFolder) {
				IFolder curr= (IFolder) resource;
				IJavaElement element= JavaCore.create(curr);
				if (element instanceof IPackageFragment) {
					if (fFoldPackages) {
						IPackageFragment fragment= (IPackageFragment) element;
						IPackageFragmentRoot root= (IPackageFragmentRoot) fragment.getParent();
						element= getFolded(root.getChildren(), fragment);
					}
					result.add(element);	
				} 
			}	
		}
	}

	public Object getHierarchicalPackageParent(IPackageFragment child) {
		String name= child.getElementName();
		IPackageFragmentRoot parent= (IPackageFragmentRoot) child.getParent();
		int index= name.lastIndexOf('.');
		if (index != -1) {
			String realParentName= name.substring(0, index);
			IPackageFragment element= parent.getPackageFragment(realParentName);
			if (element.exists()) {
				try {
					if (fFoldPackages && isEmpty(element) && findSinglePackageChild(element, parent.getChildren()) != null) {
						return getHierarchicalPackageParent(element);
					}
				} catch (JavaModelException e) {
					// ignore
				}
				return element;
			} else { // bug 65240
				IResource resource= element.getResource();
				if (resource != null) {
					return resource;
				}
			}
		}
		if (parent.getResource() instanceof IProject) {
			return parent.getJavaProject();
		}
		return parent;
	}
	
	private static IPackageFragment getFolded(IJavaElement[] children, IPackageFragment pack) throws JavaModelException {
		while (isEmpty(pack)) {
			IPackageFragment collapsed= findSinglePackageChild(pack, children);
			if (collapsed == null) {
				return pack;
			}
			pack= collapsed;
		}
		return pack;
	}
		
	private static boolean isEmpty(IPackageFragment fragment) throws JavaModelException {
		return !fragment.containsJavaResources() && fragment.getNonJavaResources().length == 0;
	}
	
	private static IPackageFragment findSinglePackageChild(IPackageFragment fragment, IJavaElement[] children) {
		String prefix= fragment.getElementName() + '.';
		int prefixLen= prefix.length();
		IPackageFragment found= null;
		for (int i= 0; i < children.length; i++) {
			IJavaElement element= children[i];
			String name= element.getElementName();
			if (name.startsWith(prefix) && name.length() > prefixLen && name.indexOf('.', prefixLen) == -1) {
				if (found == null) {
					found= (IPackageFragment) element;
				} else {
					return null;
				}
			}
		}
		return found;
	}

	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
	}

	/**
	 * Called when the view is closed and opened.
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer= (TreeViewer)viewer;
	}
	
	
	public void processDelta(IJavaElementDelta delta) {

		int kind = delta.getKind();
		final IJavaElement element = delta.getElement();

		if (element instanceof IPackageFragment) {

			if (kind == IJavaElementDelta.REMOVED) {

				postRunnable(new Runnable() {
					public void run() {
						Control ctrl = fViewer.getControl();
						if (ctrl != null && !ctrl.isDisposed()) {
							if (!fFoldPackages)
								 fViewer.remove(element);
							else
								refreshGrandParent(element);
						}
					}
				});
				return;

			} else if (kind == IJavaElementDelta.ADDED) {

				final Object parent = getHierarchicalPackageParent((IPackageFragment) element);
				if (parent != null) {
					postRunnable(new Runnable() {
						public void run() {
							Control ctrl = fViewer.getControl();
							if (ctrl != null && !ctrl.isDisposed()) {
								if (!fFoldPackages)
									 fViewer.add(parent, element);
								else
									refreshGrandParent(element);
							}
						}
					});
				}
				return;
			} 
		}
	}

	// XXX: needs to be revisited - might be a performance issue
	private void refreshGrandParent(final IJavaElement element) {
		if (element instanceof IPackageFragment) {
			Object gp= getGrandParent((IPackageFragment)element);
			if (gp instanceof IJavaElement) {
				IJavaElement el = (IJavaElement) gp;
				if(el.exists())
					fViewer.refresh(gp);
			} else if (gp instanceof IFolder) {
				IFolder folder= (IFolder)gp;
				if (folder.exists())
					fViewer.refresh(folder);
			}
		}
	}

	private Object getGrandParent(IPackageFragment element) {
		Object parent= getHierarchicalPackageParent(element);
		if (parent instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
			if (root.getResource() instanceof IProject)
				return root.getJavaProject();
			return root;
		} else if (parent instanceof IPackageFragment) {
			return getHierarchicalPackageParent((IPackageFragment) parent);
		}
		return parent;
	}
	
	private void postRunnable(final Runnable r) {
		Control ctrl= fViewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {

			Display currentDisplay= Display.getCurrent();
			if (currentDisplay != null && currentDisplay.equals(ctrl.getDisplay()))
				ctrl.getDisplay().syncExec(r);
			else
				ctrl.getDisplay().asyncExec(r);
		}
	}

	/*
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (arePackagesFoldedInHierarchicalLayout() != fFoldPackages){
			fFoldPackages= arePackagesFoldedInHierarchicalLayout();
			if (fViewer != null && !fViewer.getControl().isDisposed()) {
				fViewer.getControl().setRedraw(false);
				Object[] expandedObjects= fViewer.getExpandedElements();
				fViewer.refresh();	
				fViewer.setExpandedElements(expandedObjects);
				fViewer.getControl().setRedraw(true);
			}
		}
	}

	private boolean arePackagesFoldedInHierarchicalLayout(){
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER);
	}
}
