/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
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
	
	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaElement) {
				IJavaElement iJavaElement= (IJavaElement) parentElement;
				int type= iJavaElement.getElementType();

				switch (type) {
					case IJavaElement.JAVA_PROJECT: {
						IJavaProject project= (IJavaProject)iJavaElement;
						IProject proj= project.getProject();					
												
						List children= new ArrayList();
						
						IPackageFragmentRoot defaultroot= project.getPackageFragmentRoot(proj);
						if(defaultroot.exists()) {
							IJavaElement[] els= defaultroot.getChildren();
							children= getTopLevelChildrenByElementName(els);
						}
						return filter(children.toArray());
					}
				
					case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
							IPackageFragmentRoot root= (IPackageFragmentRoot) parentElement;
							if (root.exists()) {
								IResource resource= root.getUnderlyingResource();
								if (root.isArchive()) {
									IJavaElement[] els= root.getChildren();
									return filter(getTopLevelChildrenByElementName(els).toArray());

								} else if (resource instanceof IFolder) {
									IFolder folder= (IFolder) resource;
									IResource[] reses= folder.members();

									List children= getFolders(reses);
									IPackageFragment defaultPackage= root.getPackageFragment(""); //$NON-NLS-1$
									if(defaultPackage.exists())
										children.add(defaultPackage);

									return filter(children.toArray());
								}
							}
							break;
						}
					case IJavaElement.PACKAGE_FRAGMENT: {
							IPackageFragment packageFragment = (IPackageFragment) parentElement;
							if (!packageFragment.isDefaultPackage()) {
								IResource resource = packageFragment.getUnderlyingResource();
								if (resource != null && resource instanceof IFolder) {
									IFolder folder = (IFolder) resource;
									IResource[] reses = folder.members();
									Object[] children = getFolders(reses).toArray();
									return filter(children);
									//if the resource is null, maybe member of an archive
								} else {
									IJavaElement parent = packageFragment.getParent();
									if (parent instanceof IPackageFragmentRoot) {
										IPackageFragmentRoot root = (IPackageFragmentRoot) parent;
										Object[] children = findNextLevelChildrenByElementName(root, packageFragment);
										return filter(children);
									}
								}
							}
							break;
						}
					default :
						// do nothing
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return new Object[0];
	}

	private Object[] filter(Object[] children) {
		if (fFoldPackages) {
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IPackageFragment) {
					IPackageFragment fragment = (IPackageFragment) children[i];
					if(!fragment.isDefaultPackage())
						children[i] = getBottomPackage(fragment);
				}
			}
		}
		return children;
	}
	
	private Object getBottomPackage(IPackageFragment iPackageFragment) {
		try {
			if(isEmpty(iPackageFragment))
				return findChildrenToBeCompounded(iPackageFragment);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return iPackageFragment;
	}
	
	private IPackageFragment findChildrenToBeCompounded(IPackageFragment fragment) throws JavaModelException {

		Object[] children = getChildren(fragment);
		if ((children.length == 1) && (children[0] instanceof IPackageFragment)) {
			if (isEmpty((IPackageFragment) children[0]))
				return findChildrenToBeCompounded((IPackageFragment) children[0]);
			else return (IPackageFragment)children[0];
		}
		return fragment;
	}
	
	private boolean isEmpty(IPackageFragment fragment) throws JavaModelException {
		return (!fragment.containsJavaResources()) && (fragment.getNonJavaResources().length==0);
	}

	
	private Object[] findNextLevelChildrenByElementName(IPackageFragmentRoot parent, IPackageFragment fragment) {
		List list= new ArrayList();
		try {

			IJavaElement[] children= parent.getChildren();
			String fragmentname= fragment.getElementName();
			for (int i= 0; i < children.length; i++) {
				IJavaElement element= children[i];
				if (element instanceof IPackageFragment) {
					IPackageFragment frag= (IPackageFragment) element;

					String name= element.getElementName();
					if (!"".equals(fragmentname) && name.startsWith(fragmentname) && !name.equals(fragmentname)) { //$NON-NLS-1$
						String tail= name.substring(fragmentname.length() + 1);
						if (!"".equals(tail) && (tail.indexOf(".") == -1)) {//$NON-NLS-1$ //$NON-NLS-2$
							list.add(frag);
						}
					}
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return list.toArray();

	}
	
	private List getTopLevelChildrenByElementName(IJavaElement[] elements){
		List topLevelElements= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			IJavaElement iJavaElement= elements[i];
			//if the name of the PackageFragment is the top level package it will contain no "." separators
			if((iJavaElement.getElementName().indexOf(".")==-1) && (iJavaElement instanceof IPackageFragment)){ //$NON-NLS-1$
				topLevelElements.add(iJavaElement);
			}
		}	
		return topLevelElements;
	}

	private List getFolders(IResource[] resources) throws JavaModelException {
		List list= new ArrayList();
		for (int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			
			if (resource instanceof IFolder) {
				IFolder folder= (IFolder) resource;
				IJavaElement element= JavaCore.create(folder);
				
				if (element instanceof IPackageFragment) {
					list.add(element);	
				} 
			}	
		}
		return list;
	}


	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {

		if (element instanceof IPackageFragment) {
			IPackageFragment frag = (IPackageFragment) element;
			//@Changed: a fix, before: if(frag.exists() && isEmpty(frag))
		
			return filterParent(getActualParent(frag));
		}
		return null;
	}

	public Object getActualParent(IPackageFragment fragment) {
		try {

			if (fragment.exists()) {
				IJavaElement parent = fragment.getParent();

				if ((parent instanceof IPackageFragmentRoot) && parent.exists()) {
					IPackageFragmentRoot root = (IPackageFragmentRoot) parent;
					if (root.isArchive()) {
						return findNextLevelChildrenByElementName(fragment, root);
					} else {

						IResource resource = fragment.getUnderlyingResource();
						if ((resource != null) && (resource instanceof IFolder)) {
							IFolder folder = (IFolder) resource;
							IResource res = folder.getParent();

							IJavaElement el = JavaCore.create(res);
							return el;
						}
					}
					return parent;
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	private Object filterParent(Object parent) {
		if (fFoldPackages && (parent!=null)) {
			try {
				if (parent instanceof IPackageFragment) {
					IPackageFragment fragment = (IPackageFragment) parent;
					if (isEmpty(fragment) && hasSingleChild(fragment)) {
						return filterParent(getActualParent(fragment));
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return parent;
	}

	private boolean hasSingleChild(IPackageFragment fragment) {
		return getChildren(fragment).length==1;
	}


	private Object findNextLevelChildrenByElementName(IJavaElement child, IJavaElement parent) {
		String name= child.getElementName();
		
		if(name.indexOf(".")==-1) //$NON-NLS-1$
			return parent;
		
		try {
			String realParentName= child.getElementName().substring(0,name.lastIndexOf(".")); //$NON-NLS-1$
			IJavaElement[] children= new IJavaElement[0];			
			
			if(parent instanceof IPackageFragmentRoot){
				IPackageFragmentRoot root = (IPackageFragmentRoot) parent;
				children= root.getChildren();
			} else if (parent instanceof IJavaProject) {
				IJavaProject project = (IJavaProject) parent;
				children= project.getPackageFragments();
			}
			
			for (int i= 0; i < children.length; i++) {
				IJavaElement element= children[i];
				if(element.getElementName().equals(realParentName))
					return element;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		
		return parent;
	}


	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		
		if (element instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment) element;
			if(fragment.isDefaultPackage())
				return false;
		}
		return getChildren(element).length > 0;
	}

	/*
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
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
	
	/*
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		processDelta(event.getDelta());
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

				final Object parent = getParent(element);
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
			}
		}
	}

	private Object getGrandParent(IPackageFragment element) {

		Object parent= findNextLevelChildrenByElementName(element, element.getParent());
		if (parent instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
			if(isRootProject(root))
				return root.getJavaProject();
			else return root;
		}

		Object grandParent= getParent(parent);
		if(grandParent==null){
			return parent;
		}
		return grandParent;
	}

	private boolean isRootProject(IPackageFragmentRoot root) {
		if (IPackageFragmentRoot.DEFAULT_PACKAGEROOT_PATH.equals(root.getElementName()))
			return true;
		return false;
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
		if(arePackagesFoldedInHierarchicalLayout() != fFoldPackages){
			fFoldPackages= arePackagesFoldedInHierarchicalLayout();
			fViewer.getControl().setRedraw(false);
			Object[] expandedObjects= fViewer.getExpandedElements();
			fViewer.refresh();	
			fViewer.setExpandedElements(expandedObjects);
			fViewer.getControl().setRedraw(true);
		}
	}

	private boolean arePackagesFoldedInHierarchicalLayout(){
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER);
	}
}
