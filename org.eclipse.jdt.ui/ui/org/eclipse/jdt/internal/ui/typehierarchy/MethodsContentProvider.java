/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

/**
 * Content provider used for the method view.
 * Allows also seeing methods inherited from base classes.
 */
public class MethodsContentProvider implements IStructuredContentProvider, IElementChangedListener, ITypeHierarchyLifeCycleListener {
	
	private static final String[] UNSTRUCTURED= new String[] { IBasicPropertyConstants.P_TEXT, IBasicPropertyConstants.P_IMAGE };
	protected static final Object[] NO_ELEMENTS = new Object[0];
		
	private boolean fShowInheritedMethods;
	
	protected IType fInputType;
	private TypeHierarchyLifeCycle fHierarchyLifeCycle;
	
	private TableViewer fViewer;
	
	public MethodsContentProvider() {
		fHierarchyLifeCycle= new TypeHierarchyLifeCycle(true);
		fHierarchyLifeCycle.addChangedListener(this);
		fShowInheritedMethods= false;
		fViewer= null;
	}
	
	/**
	 * Turn on / off showing of inherited methods
	 */
	public void showInheritedMethods(boolean show) throws JavaModelException {	
		if (show != fShowInheritedMethods) {
			fShowInheritedMethods= show;
			if (!show) {
				fHierarchyLifeCycle.freeHierarchy();
			} else {
				fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInputType);
			}
			if (fViewer != null) {
				fViewer.refresh();
			}
		}
	}
	
	/**
	 * Returns true if inherited methods are shown
	 */
	public boolean isShowInheritedMethods() {
		return fShowInheritedMethods;
	}
	
	/**
	 * Returns the current input type
	 */
	public IType getInputType() {
		return fInputType;
	}
	
	private void addAll(Object[] arr, List res) {
		if (arr != null) {
			for (int j= 0; j < arr.length; j++) {
				res.add(arr[j]);
			}
		}
	}		

	/**
	 * @see IStructuredContentProvider#getElements
	 */		
	public Object[] getElements(Object element) {
		if (element instanceof IType) {
			IType type= (IType)element;
			List res= new ArrayList();
			try {
				if (fShowInheritedMethods) {
					ITypeHierarchy hierarchy= fHierarchyLifeCycle.getHierarchy();
					IType[] allSupertypes= hierarchy.getAllSupertypes(type);
					// sort in from last to first: elements with same name
					// will show up in hierarchy order 
					for (int i= allSupertypes.length - 1; i >= 0; i--) {
						addAll(allSupertypes[i].getMethods(), res);
						addAll(allSupertypes[i].getInitializers(), res);
						addAll(allSupertypes[i].getFields(), res);
					}
				}
				addAll(type.getMethods(), res);
				addAll(type.getInitializers(), res);
				addAll(type.getFields(), res);				
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
			return res.toArray();
		}
		return NO_ELEMENTS;
	}		
	
	
	/**
	 * @see IContentProvider#inputChanged
	 */
	public void inputChanged(Viewer part, Object oldInput, Object newInput) {
		if (part instanceof TableViewer) {
			fViewer= (TableViewer)part;
		} else {
			fViewer= null;
		}
		
		if (oldInput == null && newInput != null) {
			JavaCore.addElementChangedListener(this); 
		} else if (oldInput != null && newInput == null) {
			JavaCore.removeElementChangedListener(this); 
		}
		if (newInput instanceof IType) {
			fInputType= (IType)newInput;
		} else if (newInput instanceof TypeHierarchyViewPart) {
			Object input= ((TypeHierarchyViewPart)newInput).getInput();
			if (input instanceof IType) {
				fInputType= (IType)input;
			}
		} else {
			fInputType= null;
		}	
		if (fShowInheritedMethods) {
			try {
				fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInputType);
			} catch (JavaModelException e) {
				fInputType= null;
				JavaPlugin.log(e.getStatus());
			}
		}		
	}
	
	/**
	 * @see IContentProvider#isDeleted
	 */
	public boolean isDeleted(Object obj) {
		try {
			if (obj instanceof IJavaElement) {
				IJavaElement elem= (IJavaElement)obj;
				return !(elem.exists() && JavaModelUtil.isOnBuildPath(elem));
			}
		} catch (JavaModelException e) {
			// dont handle here
		}			
		return false;
	}

	/**
	 * @see IContentProvider#dispose
	 */	
	public void dispose() {
		// just to get sure that everything gets released
		fHierarchyLifeCycle.freeHierarchy();
		fHierarchyLifeCycle.removeChangedListener(this);
		JavaCore.removeElementChangedListener(this);
	}	

	/**
	 * @see IElementChangedListener#elementChanged
	 */
	public void elementChanged(ElementChangedEvent event) {
		if (fInputType != null && fViewer != null) {
			try {
				if (fShowInheritedMethods) {
					if (fHierarchyLifeCycle.getHierarchy() != null) {
						processDeltaWithHierarchy(event.getDelta());
					}
				} else {
					processDeltaNoHierarchy(event.getDelta());
				}
			} catch(JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
	}

	/*
	 * handle deltas when fShowInheritedMethods is disabled
	 * returns true if the problem has been completly handled
	 * fViewer != null
	 */		
	private boolean processDeltaNoHierarchy(IJavaElementDelta delta) throws JavaModelException {
		IJavaElement element= delta.getElement();
		// try to limit the recursive search
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				if (!fInputType.getJavaProject().equals(element)) {
					return false;
				}
				break;
			case IJavaElement.PACKAGE_FRAGMENT:
				if (!fInputType.getPackageFragment().equals(element)) {
					return false;
				} 
				break;
			case IJavaElement.COMPILATION_UNIT:
				ICompilationUnit cu= fInputType.getCompilationUnit();
				if (cu == null || !cu.equals(element)) {
					return false;
				}
				break;
			case IJavaElement.CLASS_FILE:
				IClassFile cf= fInputType.getClassFile();
				if (cf == null || !cf.equals(element)) {
					return false;
				}
				break;
			case IJavaElement.TYPE:
				if (fInputType.equals(element)) {
					return processChangeOnInput(delta);
				}
				return false;
		}
		IJavaElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			if (processDeltaNoHierarchy(children[i])) {
				return true;
			}	
		}
		return false;
	}
		
	/*
	 * handle deltas when fShowInheritedMethods is enabled	
	 * returns true if the problem has been completly handled
	 * fViewer != null
	 */					
	private boolean processDeltaWithHierarchy(IJavaElementDelta delta) throws JavaModelException {
		IJavaElement element= delta.getElement();
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				if (!fHierarchyLifeCycle.getHierarchy().getType().getJavaProject().equals(element)) {
					return false;
				}
				break;	
			case IJavaElement.TYPE:
				if (fHierarchyLifeCycle.getHierarchy().contains((IType)element)) {
					return processChangeOnInput(delta);
				}
				return false;
		}
		IJavaElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			if (processDeltaWithHierarchy(children[i])) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * returns true if the problem has been completly handled
	 * fViewer != null
	 */	
	private boolean processChangeOnInput(IJavaElementDelta delta) throws JavaModelException {
		IType type= (IType)delta.getElement();
		switch (delta.getKind()) {
			case IJavaElementDelta.REMOVED:
			case IJavaElementDelta.ADDED:
				fViewer.refresh();
				return true;
			case IJavaElementDelta.CHANGED:
				int flags= delta.getFlags();
				if ((flags & IJavaElementDelta.F_CHILDREN) != 0) {
					int nChildren= delta.getAffectedChildren().length;
					IJavaElementDelta[] added= delta.getAddedChildren();
					if (added.length > 0) {
						fViewer.add(collectElements(added));
						nChildren-= added.length;
					}
					IJavaElementDelta[] removed= delta.getRemovedChildren();
					if (removed.length > 0) {
						fViewer.remove(collectElements(removed));
						nChildren-= removed.length;
					}
					if (nChildren > 0) {
						fViewer.update(collectElements(delta.getChangedChildren()), UNSTRUCTURED);
					}
					return false;
				}
				fViewer.update(type, UNSTRUCTURED);
				return false;
	
		}
		return false;
	}
		
	private Object[] collectElements(IJavaElementDelta[] deltas) {
		int nElements= deltas.length;
		Object[] elements= new Object[nElements];
		for (int i= 0; i < nElements; i++) {
			elements[i]= deltas[i].getElement();
		}
		return elements;
	}		

	/**
	 * @see ITypeHierarchyChangedListener#typeHierarchyChanged
	 */
	public void typeHierarchyChanged(TypeHierarchyLifeCycle th) {
		checkedSyncExec(new Runnable() {
			public void run() {
				try {
				 	fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInputType);
				 	if (fViewer != null) {
				 		fViewer.refresh();
				 	}
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
				}
			}
		});
	}
	
	private void checkedSyncExec(Runnable r) {
		if (fViewer != null && !fViewer.getControl().isDisposed()) {
			Display d= fViewer.getControl().getDisplay();
			if (d != null) {
				d.syncExec(r);
			}
		}
	}			
}