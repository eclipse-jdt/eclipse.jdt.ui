/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Content provider used for the method view.
 * Allows also seeing methods inherited from base classes.
 */
public class MethodsContentProvider implements IStructuredContentProvider {
	
	private static final Object[] NO_ELEMENTS = new Object[0];
		
	private boolean fShowInheritedMethods;
	private TypeHierarchyLifeCycle fHierarchyLifeCycle;
	private TableViewer fViewer;
	
	public MethodsContentProvider(TypeHierarchyLifeCycle lifecycle) {
		fHierarchyLifeCycle= lifecycle;
		fShowInheritedMethods= false;
		fViewer= null;
	}
	
	/**
	 * Turn on / off showing of inherited methods
	 */
	public void showInheritedMethods(boolean show) throws JavaModelException {	
		if (show != fShowInheritedMethods) {
			fShowInheritedMethods= show;
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
	public void inputChanged(Viewer input, Object oldInput, Object newInput) {
		Assert.isTrue(input instanceof TableViewer);
	
		fViewer= (TableViewer) input;
	}
	
	/*
	 * @see IContentProvider#dispose
	 */	
	public void dispose() {
	}	

}