/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

/**
 * manages a type hierarchy, to keep it refreshed, and to allow it to be shared
 */
public class TypeHierarchyLifeCycle implements ITypeHierarchyChangedListener {
	
	private static final String CREATE_ERROR_PREFIX= "TypeHierarchyLifeCycle.error.createhierarchy.";
	
	private boolean fHierarchyRefreshNeeded;
	private ITypeHierarchy fHierarchy;
	private boolean fIsSuperTypesOnly;
	
	private List fChangeListeners;
	
	public TypeHierarchyLifeCycle() {
		this(false);
	}	
	
	public TypeHierarchyLifeCycle(boolean isSuperTypesOnly) {
		fHierarchy= null;
		fIsSuperTypesOnly= isSuperTypesOnly;
		fChangeListeners= new ArrayList(2);
	}
	
	public ITypeHierarchy getHierarchy() {
		return fHierarchy;
	}
	
	public IType getInput() {
		if (fHierarchy != null) {
			return fHierarchy.getType();
		}
		return null;
	}	
	
	public void freeHierarchy() {
		if (fHierarchy != null) {
			fHierarchy.removeTypeHierarchyChangedListener(this);
			fHierarchy= null;
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
	
	private void fireChange() {
		for (int i= fChangeListeners.size()-1; i>=0; i--) {
			((ITypeHierarchyLifeCycleListener)fChangeListeners.get(i)).typeHierarchyChanged(this);
		}
	}
	
	public void ensureRefreshedTypeHierarchy() throws JavaModelException {
		if (fHierarchy != null) {
			ensureRefreshedTypeHierarchy(fHierarchy.getType());
		}
	}
	
	public void ensureRefreshedTypeHierarchy(final IType type) throws JavaModelException {
		if (type == null) {
			freeHierarchy();
			return;
		}
		final boolean hierachyCreationNeeded= (fHierarchy == null || !type.equals(fHierarchy.getType()));
		
		if (hierachyCreationNeeded || fHierarchyRefreshNeeded) {
			IRunnableWithProgress op= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						if (hierachyCreationNeeded) {
							if (fHierarchy != null) {
								fHierarchy.removeTypeHierarchyChangedListener(TypeHierarchyLifeCycle.this);
							}
							IJavaProject jproject= type.getJavaProject();
							if (fIsSuperTypesOnly) {
								fHierarchy= type.newSupertypeHierarchy(pm);
							} else {
								fHierarchy= type.newTypeHierarchy(pm);
							}
							fHierarchy.addTypeHierarchyChangedListener(TypeHierarchyLifeCycle.this);
						} else {
							fHierarchy.refresh(pm);
						}
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			
			try {
				new BusyIndicatorRunnableContext().run(true, false, op);
			} catch (InvocationTargetException e) {
				Throwable th= e.getTargetException();
				if (th instanceof JavaModelException) {
					throw (JavaModelException)th;
				} else {
					throw new JavaModelException(th, IStatus.ERROR);
				}
			} catch (InterruptedException e) {
				// Not cancelable.
			}
			
			fHierarchyRefreshNeeded= false;
		}
	}
	
	/**
	 * @see ITypeHierarchyChangedListener#typeHierarchyChanged
	 */
	public void typeHierarchyChanged(ITypeHierarchy typeHierarchy) {
	 	fHierarchyRefreshNeeded= true;
	 	fireChange();
	}		

}