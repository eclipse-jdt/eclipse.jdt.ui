/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.util.Assert;
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
import org.eclipse.jdt.internal.ui.preferences.*;

/**
 * Content provider used for the method view.
 * Allows also seeing methods inherited from base classes.
 */
public class MethodsContentProvider implements IStructuredContentProvider, ITypeHierarchyLifeCycleListener {
	
	private static final String[] UNSTRUCTURED= new String[] { IBasicPropertyConstants.P_TEXT, IBasicPropertyConstants.P_IMAGE };
	private static final Object[] NO_ELEMENTS = new Object[0];
		
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

	/*
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
	
	
	/*
	 * @see IContentProvider#inputChanged
	 */
	public void inputChanged(Viewer part, Object oldInput, Object newInput) {
		Assert.isTrue(part instanceof TableViewer);
	
		fViewer= (TableViewer)part;
		
		if (newInput instanceof IType) {
			fInputType= (IType) newInput;
		} else {
			fInputType= null;
		}	
		try {
			fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInputType);
		} catch (JavaModelException e) {
			fInputType= null;
			JavaPlugin.log(e.getStatus());
		}		
	}
	
	/*
	 * @see IContentProvider#dispose
	 */	
	public void dispose() {
		// just to get sure that everything gets released
		fHierarchyLifeCycle.freeHierarchy();
		fHierarchyLifeCycle.removeChangedListener(this);
	}	

	/*
	 * @see ITypeHierarchyChangedListener#typeHierarchyChanged
	 */
	public void typeHierarchyChanged(final TypeHierarchyLifeCycle lifeCycle, final IType[] changedTypes) {
		if (fViewer != null && !fViewer.getControl().isDisposed()) {
			Display d= fViewer.getControl().getDisplay();
			if (d != null) {
				d.asyncExec(new Runnable() {
					public void run() {
						doTypeHierarchyChanged(lifeCycle, changedTypes);
					}
				});
			}
		}
	}
	
	private void doTypeHierarchyChanged(TypeHierarchyLifeCycle lifeCycle, IType[] changedTypes) {
		if (fViewer == null) {
			return; // runing async, be prepared fro everything
		}
		
		try {
			if (changedTypes == null) {
			 	fHierarchyLifeCycle.ensureRefreshedTypeHierarchy(fInputType);
	 			fViewer.refresh();
			} else {
				if (fShowInheritedMethods || AppearancePreferencePage.showOverrideIndicators()) {
					fViewer.refresh();
				} else {
					for (int i= 0; i < changedTypes.length; i++) {
						if (changedTypes[i].equals(fInputType)) {
							fViewer.refresh();
							return;
						}
					}
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}
	}	
}