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
package org.eclipse.jdt.internal.ui.browsing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Tree content provider for the hierarchical layout in the packages view.
 * <p>
 * XXX: The standard Java browsing part content provider needs and calls
 * the browsing part/view. This class currently doesn't need to do so
 * but might be required to later.
 * </p>
 */
class PackagesViewHierarchicalContentProvider extends LogicalPackagesProvider implements ITreeContentProvider {
	
	public PackagesViewHierarchicalContentProvider(StructuredViewer viewer){
		super(viewer);
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
					case IJavaElement.JAVA_PROJECT :
						{

							//create new element mapping
							fMapToLogicalPackage.clear();
							fMapToPackageFragments.clear();
							IJavaProject project= (IJavaProject) parentElement;

							IPackageFragment[] topLevelChildren= getTopLevelChildrenByElementName(project.getPackageFragments());
							List list= new ArrayList();
							for (int i= 0; i < topLevelChildren.length; i++) {
								IPackageFragment fragment= topLevelChildren[i];

								IJavaElement el= fragment.getParent();
								if (el instanceof IPackageFragmentRoot) {
									IPackageFragmentRoot root= (IPackageFragmentRoot) el;
									if (!root.isArchive() || !root.isExternal())
										list.add(fragment);
								}
							}

							return combineSamePackagesIntoLogialPackages((IPackageFragment[]) list.toArray(new IPackageFragment[list.size()]));
						}

					case IJavaElement.PACKAGE_FRAGMENT_ROOT :
						{
							IPackageFragmentRoot root= (IPackageFragmentRoot) parentElement;
		
							//create new element mapping
							fMapToLogicalPackage.clear();
							fMapToPackageFragments.clear();
							IResource resource= root.getUnderlyingResource();
							IPackageFragment[] fragments= new IPackageFragment[0];
							if (root.isArchive()) {
								IJavaElement[] els= root.getChildren();
								fragments= getTopLevelChildrenByElementName(els);

							} else if (resource != null && resource instanceof IFolder) {
								fragments= getTopLevelChildrenByElementName(root.getChildren());
							}
							addFragmentsToMap(fragments);
							return fragments;
						}

					case IJavaElement.PACKAGE_FRAGMENT :
						{
							IPackageFragment packageFragment= (IPackageFragment) parentElement;
							IPackageFragment[] fragments= new IPackageFragment[0];
							IJavaElement parent= packageFragment.getParent();
							if (parent instanceof IPackageFragmentRoot) {
								IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
								fragments= findNextLevelChildrenByElementName(root, packageFragment);
							}

							addFragmentsToMap(fragments);
							return fragments;
						}
				}
				
			//@Improve: rewrite using concatenate	
			} else if (parentElement instanceof LogicalPackage) {

				List children= new ArrayList();
				LogicalPackage logicalPackage= (LogicalPackage) parentElement;
				IPackageFragment[] elements= logicalPackage.getFragments();
				for (int i= 0; i < elements.length; i++) {
					IPackageFragment fragment= elements[i];
					IPackageFragment[] objects= findNextLevelChildrenByElementName((IPackageFragmentRoot) fragment.getParent(), fragment);
					children.addAll(Arrays.asList(objects));
				}
				return combineSamePackagesIntoLogialPackages((IPackageFragment[]) children.toArray(new IPackageFragment[children.size()]));
			}

		} catch (JavaModelException e) {
			return NO_CHILDREN;
		}
		return NO_CHILDREN;
	}
	
		private IPackageFragment[] findNextLevelChildrenByElementName(IPackageFragmentRoot parent, IPackageFragment fragment) {
		List list= new ArrayList();
		try {

			IJavaElement[] children= parent.getChildren();
			String fragmentname= fragment.getElementName();
			for (int i= 0; i < children.length; i++) {
				IJavaElement element= children[i];
				if (element instanceof IPackageFragment) {
					IPackageFragment frag= (IPackageFragment) element;

					String name= element.getElementName();
					if (name.length() > fragmentname.length() && name.charAt(fragmentname.length()) == '.' && frag.exists() && !IPackageFragment.DEFAULT_PACKAGE_NAME.equals(fragmentname) && name.startsWith(fragmentname) && !name.equals(fragmentname)) {
						String tail= name.substring(fragmentname.length() + 1); 
						if (!IPackageFragment.DEFAULT_PACKAGE_NAME.equals(tail) && tail.indexOf(".") == -1) { //$NON-NLS-1$
							list.add(frag);
						}
					}
				}
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return (IPackageFragment[]) list.toArray(new IPackageFragment[list.size()]);
	}
	
	private IPackageFragment[] getTopLevelChildrenByElementName(IJavaElement[] elements){
		List topLevelElements= new ArrayList();
		for (int i= 0; i < elements.length; i++) {
			IJavaElement iJavaElement= elements[i];
			//if the name of the PackageFragment is the top level package it will contain no "." separators
			if((iJavaElement.getElementName().indexOf(".")==-1) && (iJavaElement instanceof IPackageFragment)){ //$NON-NLS-1$
				topLevelElements.add(iJavaElement);
			}
		}	
		return (IPackageFragment[]) topLevelElements.toArray(new IPackageFragment[topLevelElements.size()]);
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		
		try {
			if (element instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) element;
				if(!fragment.exists())
					return null;		
				Object parent= getHierarchicalParent(fragment);
				if(parent instanceof IPackageFragment) {
					IPackageFragment pkgFragment= (IPackageFragment)parent;
					LogicalPackage logicalPkg= findLogicalPackage(pkgFragment);
					if (logicalPkg != null) 
						return logicalPkg;
					else {
						LogicalPackage lp= createLogicalPackage(pkgFragment);
						if(lp == null)
							return pkgFragment;
						else return lp;
					}
				} 
				return parent;
				
			} else if(element instanceof LogicalPackage){
				LogicalPackage el= (LogicalPackage) element;
				IPackageFragment fragment= el.getFragments()[0];
				Object parent= getHierarchicalParent(fragment);

				if(parent instanceof IPackageFragment){
					IPackageFragment pkgFragment= (IPackageFragment) parent;
					LogicalPackage logicalPkg= findLogicalPackage(pkgFragment);
					if (logicalPkg != null)
						return logicalPkg;
					else {
						LogicalPackage lp= createLogicalPackage(pkgFragment);
						if(lp == null)
							return pkgFragment;
						else return lp;
					}
				} else
					return fragment.getJavaProject();
			} 

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	/*
	 * Check if the given IPackageFragment should be the member of a
	 * LogicalPackage and if so creates the LogicalPackage and adds it to the
	 * map.
	 */
	private LogicalPackage createLogicalPackage(IPackageFragment pkgFragment) {
		if(!fInputIsProject)
			return null;
		
		List fragments= new ArrayList();
		try {
			IPackageFragmentRoot[] roots= pkgFragment.getJavaProject().getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				IPackageFragmentRoot root= roots[i];
				IPackageFragment fragment= root.getPackageFragment(pkgFragment.getElementName());
				if(fragment.exists() && !fragment.equals(pkgFragment))
					fragments.add(fragment);
			}
			if(!fragments.isEmpty()) {
				LogicalPackage logicalPackage= new LogicalPackage(pkgFragment);
				fMapToLogicalPackage.put(getKey(pkgFragment), logicalPackage);
				Iterator iter= fragments.iterator();
				while(iter.hasNext()){
					IPackageFragment f= (IPackageFragment)iter.next();
					if(logicalPackage.belongs(f)){
						logicalPackage.add(f);
						fMapToLogicalPackage.put(getKey(f), logicalPackage);
					}
				}
				
				return logicalPackage;
			}
				
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		
		return null;
	}

	private Object getHierarchicalParent(IPackageFragment fragment) throws JavaModelException {
		IJavaElement parent= fragment.getParent();

		if ((parent instanceof IPackageFragmentRoot) && parent.exists()) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
			if (root.isArchive() || !fragment.exists()) {
				return findNextLevelParentByElementName(fragment, root);
			} else {
				IResource resource= fragment.getUnderlyingResource();
				if ((resource != null) && (resource instanceof IFolder)) {
					IFolder folder= (IFolder) resource;
					IResource res= folder.getParent();

					IJavaElement el= JavaCore.create(res);
					return el;
				}
			}
		}
		return parent;
	}

	
	private Object findNextLevelParentByElementName(IJavaElement child, IJavaElement parent) {
		String name= child.getElementName();
		
		if(name.indexOf(".")==-1) //$NON-NLS-1$  
			return parent;
		
		try {
			String realParentName= child.getElementName().substring(0,name.lastIndexOf(".")); //$NON-NLS-1$  
			IJavaElement[] children= new IJavaElement[0];
			
			if(parent instanceof IPackageFragmentRoot){
				IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
				children= root.getChildren();	
			} else if(parent instanceof IJavaProject){
				IJavaProject project= (IJavaProject) parent;
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
			if(fragment.isDefaultPackage() || !fragment.exists())
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

	protected void processDelta(IJavaElementDelta delta) throws JavaModelException {

		int kind = delta.getKind();
		final IJavaElement element = delta.getElement();

		if (isClassPathChange(delta)) {
			Object input= fViewer.getInput();
			if (input != null) {
				if (fInputIsProject && input.equals(element.getJavaProject())) {
					postRefresh(input);
					return;
				} else if (!fInputIsProject && input.equals(element)) {
					if (element.exists())
						postRefresh(input);
					else
						postRemove(input);
					return;
				}
			}
		}
		
		if (kind == IJavaElementDelta.REMOVED) {
			Object input= fViewer.getInput();
			if (input != null && input.equals(element)) {
					postRemove(input);
					return;
				}
		}

		if (element instanceof IPackageFragment) {
			final IPackageFragment frag = (IPackageFragment) element;

			//if fragment was in LogicalPackage refresh,
			//otherwise just remove
			if (kind == IJavaElementDelta.REMOVED) {
				removeElement(frag);
				return;

			} else if (kind == IJavaElementDelta.ADDED) {

				Object parent= getParent(frag);
				addElement(frag, parent);
				return;

			} else if (kind == IJavaElementDelta.CHANGED) {
				//just refresh
				LogicalPackage logicalPkg= findLogicalPackage(frag);
				//in case changed object is filtered out
				if (logicalPkg != null)
					postRefresh(findElementToRefresh(logicalPkg));
				else
					postRefresh(findElementToRefresh(frag));
				return;
			}
		}
		
		processAffectedChildren(delta);
	}
	
	private Object findElementToRefresh(Object object) {
		Object toBeRefreshed= object;
		if (fViewer.testFindItem(object) == null) {
			 Object parent= getParent(object);
			 if(parent instanceof IPackageFragmentRoot && fInputIsProject)
			 	parent= ((IPackageFragmentRoot)parent).getJavaProject();
			 
			if(parent != null)
				toBeRefreshed= parent;
		}
		return toBeRefreshed;
	}

	private void processAffectedChildren(IJavaElementDelta delta) throws JavaModelException {
		IJavaElementDelta[] affectedChildren = delta.getAffectedChildren();
		for (int i = 0; i < affectedChildren.length; i++) {
			if (!(affectedChildren[i] instanceof ICompilationUnit)) {
				processDelta(affectedChildren[i]);
			}
		}
	}

	private void postAdd(final Object child, final Object parent) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl = fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					((TreeViewer)fViewer).add(parent, child);
				}
			}
		});
	}

	private void postRemove(final Object object) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl = fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					((TreeViewer)fViewer).remove(object);
				}
			}
		});
	}
	
	private void postRefresh(final Object object) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					((TreeViewer) fViewer).refresh(object);
				}
			}
		});
	}
	
	private void postRunnable(final Runnable r) {
		Control ctrl= fViewer.getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
		//	fBrowsingPart.setProcessSelectionEvents(false);
			try {
				Display currentDisplay= Display.getCurrent();
				if (currentDisplay != null && currentDisplay.equals(ctrl.getDisplay()))
					ctrl.getDisplay().syncExec(r);
				else				
					ctrl.getDisplay().asyncExec(r);
			} finally {
		//		fBrowsingPart.setProcessSelectionEvents(true);
			}
		}
	}
	
	private void addElement(IPackageFragment frag, Object parent) {
		
		String key= getKey(frag);
		LogicalPackage lp= (LogicalPackage)fMapToLogicalPackage.get(key);

		//if fragment must be added to an existing LogicalPackage
		if (lp != null && lp.belongs(frag)){
			lp.add(frag);
			return;
		}

		//if a new LogicalPackage must be created
		IPackageFragment iPackageFragment= (IPackageFragment)fMapToPackageFragments.get(key);
		if (iPackageFragment!= null && !iPackageFragment.equals(frag)){
			lp= new LogicalPackage(iPackageFragment);
			lp.add(frag);
			//add new LogicalPackage to LogicalPackages map
			fMapToLogicalPackage.put(key, lp);

			//determin who to refresh					
			if (parent instanceof IPackageFragmentRoot){
				IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
				if (fInputIsProject){
					postRefresh(root.getJavaProject());	
				} else {
					postRefresh(root);	
				}
			} else { 
				//@Improve: Shoud this be replaced by a refresh?
				postAdd(lp, parent);
				postRemove(iPackageFragment);
			}

		} 
		//if this is a new Package Fragment
		else {
			fMapToPackageFragments.put(key, frag);

			//determin who to refresh
			if (parent instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
				if (fInputIsProject) {
					postAdd(frag, root.getJavaProject());
				} else
					postAdd(frag, root);
			} else {
				postAdd(frag, parent);
			}
		}
	}

	private void removeElement(IPackageFragment frag) {

		String key= getKey(frag);
		LogicalPackage lp= (LogicalPackage)fMapToLogicalPackage.get(key);

		if(lp != null){	
			lp.remove(frag);
			//if the LogicalPackage needs to revert back to a PackageFragment
			//remove it from the LogicalPackages map and add the PackageFragment
			//to the PackageFragment map
			if (lp.getFragments().length == 1) {
				IPackageFragment fragment= lp.getFragments()[0];
				fMapToPackageFragments.put(key, fragment);
				fMapToLogicalPackage.remove(key);
				
				//remove the LogicalPackage frome viewer
				postRemove(lp);
				
				Object parent= getParent(fragment);
				if (parent instanceof IPackageFragmentRoot) {
					parent= ((IPackageFragmentRoot)parent).getJavaProject();
				}
				postAdd(fragment, parent);
			}

		} else {
			//remove the fragment from the fragment map and viewer
			IPackageFragment fragment= (IPackageFragment) fMapToPackageFragments.get(key);
			if (fragment!= null && fragment.equals(frag)) {
				fMapToPackageFragments.remove(key);
				postRemove(frag);
			}
		}
	}
}
