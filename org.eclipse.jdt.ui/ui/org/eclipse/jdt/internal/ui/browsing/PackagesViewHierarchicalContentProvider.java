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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * 
 * XXX: not yet reviewed - part of experimental logical packages view
 * 
 * Tree content provider for the hierarchical layout in the packages view.
 * <p>
 * XXX: The standard Java browsing part content provider needs and calls
 * the browsing part/view. This class currently doesn't need to do so
 * but might be required to later.
 * </p>
 */
class PackagesViewHierarchicalContentProvider extends LogicalPackgesContentProvider implements ITreeContentProvider, IElementChangedListener {
	private boolean fProjectViewState= true;
	
	public PackagesViewHierarchicalContentProvider(StructuredViewer viewer){
		super(viewer);
	}

	/**
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
							fMapToCompoundElement= new HashMap();
							fProjectViewState= true;
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

							return createCompoundElements((IPackageFragment[]) list.toArray(new IPackageFragment[list.size()]));
						}

					case IJavaElement.PACKAGE_FRAGMENT_ROOT :
						{
							IPackageFragmentRoot root= (IPackageFragmentRoot) parentElement;
							fProjectViewState= false;
		
							//create new element mapping
							fMapToCompoundElement= new HashMap();
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
				LogicalPackage compoundEl= (LogicalPackage) parentElement;
				IPackageFragment[] elements= compoundEl.getFragments();
				for (int i= 0; i < elements.length; i++) {
					IPackageFragment fragment= elements[i];
					IPackageFragment[] objects= findNextLevelChildrenByElementName((IPackageFragmentRoot) fragment.getParent(), fragment);
					children.addAll(Arrays.asList(objects));
				}
				return createCompoundElements((IPackageFragment[]) children.toArray(new IPackageFragment[children.size()]));
			}

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return new Object[0];
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
					if (!"".equals(fragmentname) && name.startsWith(fragmentname) && !name.equals(fragmentname)) {//$NON-NLS-1$
						String tail= name.substring(fragmentname.length() + 1); 
						if (!"".equals(tail) && (tail.indexOf(".") == -1)) { //$NON-NLS-1$ //$NON-NLS-2$   
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

	/**
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
					else
						return pkgFragment;
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
					else
						return pkgFragment;
				} else
					return fragment.getJavaProject();
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
			if (root.isArchive()) {
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


	/**
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

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
	}

	/**
	 * Called when the view is closed and opened. When newInput is null
	 * removes the element map and decouples ElementChangedListener.
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(newInput!= null){
			JavaCore.addElementChangedListener(this);
		} else {
			JavaCore.removeElementChangedListener(this);
		}
	}
	
	/**
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		try {
			processDelta(event.getDelta());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}
	
	private void processDelta(IJavaElementDelta delta) throws JavaModelException {

		int kind = delta.getKind();
		final IJavaElement element = delta.getElement();

		if (element instanceof IPackageFragment) {
			final IPackageFragment frag = (IPackageFragment) element;

			//if fragment was in compound element refresh,
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
				if (logicalPkg != null)
					postRefresh(logicalPkg);
				else
					postRefresh(frag);
				return;
			}
		}
		
		processAffectedChildren(delta);
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
		
		if(fMapToCompoundElement==null){
			postAdd(frag, parent);
			return;	
		}

		String key= getKey(frag);
		Object object= fMapToCompoundElement.get(key);

		//if fragment must be added to an existing CompoundElement
		if (object instanceof LogicalPackage){
			LogicalPackage element= (LogicalPackage) object;
			if (element.belongs(frag)){
				element.add(frag);
			}

		//if a new CompoundElement must be created
		} else if (object instanceof IPackageFragment){
			IPackageFragment iPackageFrament= (IPackageFragment) object;
			if (!iPackageFrament.equals(frag)){
				LogicalPackage element= new LogicalPackage(iPackageFrament);
				element.add(frag);
				fMapToCompoundElement.put(key, element);

								
				if (parent instanceof IPackageFragmentRoot){
					IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
					if (fProjectViewState){
						postRefresh(root.getJavaProject());	
					} else postRefresh(root);
				} else { 
					postAdd(element, parent);
					postRemove(iPackageFrament);
				}

			}
		} else {
			fMapToCompoundElement.put(key, frag);

			if (parent instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
				if (fProjectViewState) {
					postRefresh(root.getJavaProject());
				} else
					postRefresh(root);
			} else {
				postAdd(frag, parent);
			}
		}
	}

	private void removeElement(IPackageFragment frag) {

		if (fMapToCompoundElement == null) {
			postRemove(frag);
			return;
		}

		String key= getKey(frag);
		Object object= fMapToCompoundElement.get(key);

		if (object instanceof LogicalPackage) {
			LogicalPackage element= (LogicalPackage) object;
			element.remove(frag);
			if (element.getFragments().length == 1) {
				IPackageFragment fragment= element.getFragments()[0];
				fMapToCompoundElement.put(key, fragment);
				postRemove(element);
				Object parent= getParent(fragment);
				if (parent instanceof IPackageFragmentRoot) {
					IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
					parent= root.getJavaProject();
				}
				postAdd(fragment, parent);
			}
		} else if (object instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment) object;
			if (fragment.equals(frag)) {
				fMapToCompoundElement.remove(key);
				postRemove(frag);
			}
		}
	}
	
}
