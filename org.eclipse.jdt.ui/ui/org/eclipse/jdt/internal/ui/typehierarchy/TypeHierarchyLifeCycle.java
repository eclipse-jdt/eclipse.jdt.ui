/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

/**
 * Manages a type hierarchy, to keep it refreshed, and to allow it to be shared.
 */
public class TypeHierarchyLifeCycle implements ITypeHierarchyChangedListener, IElementChangedListener {
	
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
			JavaCore.removeElementChangedListener(this);
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
	
	private void fireChange(IType[] changedTypes) {
		for (int i= fChangeListeners.size()-1; i>=0; i--) {
			ITypeHierarchyLifeCycleListener curr= (ITypeHierarchyLifeCycleListener) fChangeListeners.get(i);
			curr.typeHierarchyChanged(this, changedTypes);
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
		boolean hierachyCreationNeeded= (fHierarchy == null || !type.equals(fHierarchy.getType()));
		
		if (hierachyCreationNeeded || fHierarchyRefreshNeeded) {
			IRunnableWithProgress op= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					try {
						doHierarchyRefresh(type, pm);
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
	
	private void doHierarchyRefresh(IType type, IProgressMonitor pm) throws JavaModelException {
		boolean hierachyCreationNeeded= (fHierarchy == null || !type.equals(fHierarchy.getType()));
		if (hierachyCreationNeeded) {
			if (fHierarchy != null) {
				fHierarchy.removeTypeHierarchyChangedListener(this);
				JavaCore.removeElementChangedListener(this);
			}
			if (fIsSuperTypesOnly) {
				fHierarchy= type.newSupertypeHierarchy(pm);
			} else {
				fHierarchy= type.newTypeHierarchy(pm);
			}
			fHierarchy.addTypeHierarchyChangedListener(this);
			JavaCore.addElementChangedListener(this);
		} else {
			fHierarchy.refresh(pm);
		}
	}		
	
	/*
	 * @see ITypeHierarchyChangedListener#typeHierarchyChanged
	 */
	public void typeHierarchyChanged(ITypeHierarchy typeHierarchy) {
	 	fHierarchyRefreshNeeded= true;

	}		

	/*
	 * @see IElementChangedListener#elementChanged(ElementChangedEvent)
	 */
	public void elementChanged(ElementChangedEvent event) {
		IJavaElement elem= event.getDelta().getElement();
		if (elem instanceof IWorkingCopy && ((IWorkingCopy)elem).isWorkingCopy()) {
			return;
		}
		if (fHierarchyRefreshNeeded) {
			fireChange(null);
		} else {
			ArrayList changedTypes= new ArrayList();
			processDelta(event.getDelta(), changedTypes);
			fireChange((IType[]) changedTypes.toArray(new IType[changedTypes.size()]));
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
				if (delta.getKind() == IJavaElementDelta.CHANGED && delta.getFlags() == IJavaElementDelta.F_CONTENT) {
					try {
						IType[] types= ((ICompilationUnit) element).getAllTypes();
						for (int i= 0; i < types.length; i++) {
							processTypeDelta(types[i], changedTypes);
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				} else {
					processChildrenDelta(delta, changedTypes);
				}
				break;
			case IJavaElement.CLASS_FILE:	
				if (delta.getKind() == IJavaElementDelta.CHANGED && delta.getFlags() == IJavaElementDelta.F_CONTENT) {
					try {
						IType type= ((IClassFile) element).getType();
						processTypeDelta(type, changedTypes);
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				} else {
					processChildrenDelta(delta, changedTypes);
				}
				break;				
		}
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