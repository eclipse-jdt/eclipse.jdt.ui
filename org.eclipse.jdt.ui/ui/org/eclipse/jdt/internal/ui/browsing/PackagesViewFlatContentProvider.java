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
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
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

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * XXX: not yet reviewed - part of experimental logical packages view
 * 
 * Table content provider for the hierarchical layout in the packages view.
 * <p> 
 * XXX: The standard Java browsing part content provider needs and calls
 * the browsing part/view. This class currently doesn't need to do so
 * but might be required to later.
 * </p>
 */
class PackagesViewFlatContentProvider extends LogicalPackgesContentProvider implements IStructuredContentProvider, IElementChangedListener, IPropertyChangeListener{
	PackagesViewFlatContentProvider(StructuredViewer viewer) {
		super(viewer);
	}
	
	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		
		if(parentElement instanceof IJavaElement){
			IJavaElement element= (IJavaElement) parentElement;

			int type= element.getElementType();
			
			try {
				switch (type) {
					case IJavaElement.JAVA_PROJECT :
						IJavaProject project= (IJavaProject) element;
						IPackageFragment[] children= getPackageFragments(project.getPackageFragments());
						if(isInCompoundState()) {
							fMapToCompoundElement= new HashMap();
							return createCompoundElements(children);	
						} else	return children;
				
					case IJavaElement.PACKAGE_FRAGMENT_ROOT :
						fMapToCompoundElement= new HashMap();
						IPackageFragmentRoot root= (IPackageFragmentRoot) element;
						return root.getChildren();
						
					case IJavaElement.PACKAGE_FRAGMENT :
						//no children in flat view
						break;
				
					default :
						//do nothing, empty array returned
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			
		}
		return new Object[0];
	}
	
	/**
	 * Weeds out packageFragments from external jars
	 */
	private IPackageFragment[] getPackageFragments(IPackageFragment[] iPackageFragments) {
		List list= new ArrayList();
		for (int i= 0; i < iPackageFragments.length; i++) {
			IPackageFragment fragment= iPackageFragments[i];
			IJavaElement el= fragment.getParent();
			if (el instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot) el;
				if(root.isArchive() && root.isExternal())
					continue;
			}
			list.add(fragment);
		}
		return (IPackageFragment[]) list.toArray(new IPackageFragment[list.size()]);
	}
	
	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
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
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput != null) {
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

		int kind= delta.getKind();
		final IJavaElement element= delta.getElement();

		if (element instanceof IPackageFragment) {
			final IPackageFragment frag= (IPackageFragment) element;

			if (kind == IJavaElementDelta.REMOVED) {
				removeElement(frag);

			} else if (kind == IJavaElementDelta.ADDED) {
				addElement(frag);

			} else if (kind == IJavaElementDelta.CHANGED) {
				//just refresh 
				if (element instanceof IPackageFragment) {
					IPackageFragment pkgFragment= (IPackageFragment)element;
					LogicalPackage logicalPkg= findLogicalPackage(pkgFragment);
					if (logicalPkg != null)
						postRefresh(logicalPkg);
					else
						postRefresh(pkgFragment);
				} else
					postRefresh(element);
			}
			//in this view there will be no children of PackageFragment to refresh
			return;
		}
		processAffectedChildren(delta);
	}


	private void processAffectedChildren(IJavaElementDelta delta) throws JavaModelException {
		IJavaElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			IJavaElementDelta elementDelta= children[i];
			processDelta(elementDelta);
		}
	}


	private void postAdd(final Object child) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl = fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					((TableViewer)fViewer).add(child);
				}
			}
		});
	}


	private void postRemove(final Object object) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl = fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					((TableViewer)fViewer).remove(object);
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
	
	private void removeElement(IPackageFragment frag) {
		String key= getKey(frag);
		Object object= fMapToCompoundElement.get(key);
	
		if(object instanceof LogicalPackage){
			LogicalPackage element= (LogicalPackage) object;
			element.remove(frag);
			if(element.getFragments().length == 1){
				IPackageFragment fragment= element.getFragments()[0];
				fMapToCompoundElement.put(key, fragment);		
				//@Improve: is this correct?
				postRemove(element);
				postAdd(fragment);
				//postRefresh(fragment.getJavaProject());
			} return;
		} else {
			fMapToCompoundElement.remove(key);	
			postRemove(frag);	
		}
	}
	

	private void postRefresh(final Object element) {
		postRunnable(new Runnable() {
			public void run() {
				Control ctrl= fViewer.getControl();
				if (ctrl != null && !ctrl.isDisposed()) {
					fViewer.refresh(element);
				}
			}
		});
	}
	
	private void addElement(IPackageFragment frag) {
		String key= getKey(frag);
		Object object= fMapToCompoundElement.get(key);
	
		if(object instanceof LogicalPackage){
			LogicalPackage element= (LogicalPackage) object;		
			if(element.belongs(frag))
				element.add(frag);
			return;	
		} else if(object instanceof IPackageFragment){
			//must create a new CompoundElement
			IPackageFragment iPackageFrament= (IPackageFragment) object;
			if(!iPackageFrament.equals(frag)){
				LogicalPackage element= new LogicalPackage(iPackageFrament);
				element.add(frag);
				fMapToCompoundElement.put(key, element);
				//@Improve: is this correct?
				postRemove(iPackageFrament);
				postAdd(element); 
				//postRefresh(element.getParentProject());
				return;
			}
		} else {
			fMapToCompoundElement.put(key, frag);
			postAdd(frag);
		}
	}
}
