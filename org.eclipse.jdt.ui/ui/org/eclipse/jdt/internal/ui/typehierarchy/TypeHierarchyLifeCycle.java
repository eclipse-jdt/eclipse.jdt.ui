/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Manages a type hierarchy, to keep it refreshed, and to allow it to be shared.
 */
public class TypeHierarchyLifeCycle implements ITypeHierarchyChangedListener, IElementChangedListener {

	private boolean fHierarchyRefreshNeeded;
	private ITypeHierarchy fHierarchy;
	private IJavaElement fInputElement;
	private boolean fIsSuperTypesOnly;

	private List fChangeListeners;

	public TypeHierarchyLifeCycle() {
		this(false);
	}

	public TypeHierarchyLifeCycle(boolean isSuperTypesOnly) {
		fHierarchy= null;
		fInputElement= null;
		fIsSuperTypesOnly= isSuperTypesOnly;
		fChangeListeners= new ArrayList(2);
	}

	public ITypeHierarchy getHierarchy() {
		return fHierarchy;
	}

	public IJavaElement getInputElement() {
		return fInputElement;
	}


	public void freeHierarchy() {
		if (fHierarchy != null) {
			fHierarchy.removeTypeHierarchyChangedListener(this);
			JavaCore.removeElementChangedListener(this);
			fHierarchy= null;
			fInputElement= null;
		}
	}

	public void removeChangedListener(ITypeHierarchyLifeCycleListener listener) {
		fChangeListeners.remove(listener);
	}

	public void addChangedListener(ITypeHierarchyLifeCycleListener listener) {
		if (!fChangeListeners.contains(listener)) {
			fChangeListeners.add(listener);
		}
	}

	private void fireChange(IType[] changedTypes) {
		for (int i= fChangeListeners.size()-1; i>=0; i--) {
			ITypeHierarchyLifeCycleListener curr= (ITypeHierarchyLifeCycleListener) fChangeListeners.get(i);
			curr.typeHierarchyChanged(this, changedTypes);
		}
	}

	public void ensureRefreshedTypeHierarchy(final IJavaElement element, IRunnableContext context) throws InvocationTargetException, InterruptedException {
		if (element == null || !element.exists()) {
			freeHierarchy();
			return;
		}
		boolean hierachyCreationNeeded= (fHierarchy == null || !element.equals(fInputElement));

		if (hierachyCreationNeeded || fHierarchyRefreshNeeded) {

			IRunnableWithProgress op= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException, InterruptedException {
					try {
						doHierarchyRefresh(element, pm);
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					} catch (OperationCanceledException e) {
						throw new InterruptedException();
					}
				}
			};
			fHierarchyRefreshNeeded= true;
			context.run(true, true, op);
			fHierarchyRefreshNeeded= false;
		}
	}

	private ITypeHierarchy createTypeHierarchy(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		if (element.getElementType() == IJavaElement.TYPE) {
			IType type= (IType) element;
			if (fIsSuperTypesOnly) {
				return type.newSupertypeHierarchy(pm);
			} else {
				return type.newTypeHierarchy(pm);
			}
		} else {
			IRegion region= JavaCore.newRegion();
			if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
				// for projects only add the contained source folders
				IPackageFragmentRoot[] roots= ((IJavaProject) element).getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					if (!roots[i].isExternal()) {
						region.add(roots[i]);
					}
				}
			} else if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				IPackageFragmentRoot[] roots= element.getJavaProject().getPackageFragmentRoots();
				String name= element.getElementName();
				for (int i= 0; i < roots.length; i++) {
					IPackageFragment pack= roots[i].getPackageFragment(name);
					if (pack.exists()) {
						region.add(pack);
					}
				}
			} else {
				region.add(element);
			}
			IJavaProject jproject= element.getJavaProject();
			return jproject.newTypeHierarchy(region, pm);
		}
	}


	public synchronized void doHierarchyRefresh(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		boolean hierachyCreationNeeded= (fHierarchy == null || !element.equals(fInputElement));
		// to ensure the order of the two listeners always remove / add listeners on operations
		// on type hierarchies
		if (fHierarchy != null) {
			fHierarchy.removeTypeHierarchyChangedListener(this);
			JavaCore.removeElementChangedListener(this);
		}
		if (hierachyCreationNeeded) {
			fHierarchy= createTypeHierarchy(element, pm);
			if (pm != null && pm.isCanceled()) {
				throw new OperationCanceledException();
			}
			fInputElement= element;
		} else {
			fHierarchy.refresh(pm);
		}
		fHierarchy.addTypeHierarchyChangedListener(this);
		JavaCore.addElementChangedListener(this);
		fHierarchyRefreshNeeded= false;
	}

	/*
	 * @see ITypeHierarchyChangedListener#typeHierarchyChanged
	 */
	public void typeHierarchyChanged(ITypeHierarchy typeHierarchy) {
	 	fHierarchyRefreshNeeded= true;
 		fireChange(null);
	}

	/*
	 * @see IElementChangedListener#elementChanged(ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		if (fChangeListeners.isEmpty()) {
			return;
		}

		if (fHierarchyRefreshNeeded) {
			return;
		} else {
			ArrayList changedTypes= new ArrayList();
			processDelta(event.getDelta(), changedTypes);
			if (changedTypes.size() > 0) {
				fireChange((IType[]) changedTypes.toArray(new IType[changedTypes.size()]));
			}
		}
	}

	/*
	 * Assume that the hierarchy is intact (no refresh needed)
	 */
	private void processDelta(IJavaElementDelta delta, ArrayList changedTypes) {
		IJavaElement element= delta.getElement();
		switch (element.getElementType()) {
			case IJavaElement.TYPE:
				processTypeDelta((IType) element, changedTypes);
				processChildrenDelta(delta, changedTypes); // (inner types)
				break;
			case IJavaElement.JAVA_MODEL:
			case IJavaElement.JAVA_PROJECT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaElement.PACKAGE_FRAGMENT:
				processChildrenDelta(delta, changedTypes);
				break;
			case IJavaElement.COMPILATION_UNIT:
				ICompilationUnit cu= (ICompilationUnit)element;
				if (!JavaModelUtil.isPrimary(cu)) {
					return;
				}

				if (delta.getKind() == IJavaElementDelta.CHANGED && isPossibleStructuralChange(delta.getFlags())) {
					try {
						if (cu.exists()) {
							IType[] types= cu.getAllTypes();
							for (int i= 0; i < types.length; i++) {
								processTypeDelta(types[i], changedTypes);
							}
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				} else {
					processChildrenDelta(delta, changedTypes);
				}
				break;
			case IJavaElement.CLASS_FILE:
				if (delta.getKind() == IJavaElementDelta.CHANGED) {
					IType type= ((IClassFile) element).getType();
					processTypeDelta(type, changedTypes);
				} else {
					processChildrenDelta(delta, changedTypes);
				}
				break;
		}
	}

	private boolean isPossibleStructuralChange(int flags) {
		return (flags & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_FINE_GRAINED)) == IJavaElementDelta.F_CONTENT;
	}

	private void processTypeDelta(IType type, ArrayList changedTypes) {
		if (getHierarchy().contains(type)) {
			changedTypes.add(type);
		}
	}

	private void processChildrenDelta(IJavaElementDelta delta, ArrayList changedTypes) {
		IJavaElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			processDelta(children[i], changedTypes); // recursive
		}
	}


}
