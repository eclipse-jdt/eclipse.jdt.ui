/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A TypeHierarchyViewer that looks like the type hierarchy view of VA/Java:
 * Starting form Object down to the element in focus, then all subclasses from
 * this element.
 * Used by the TypeHierarchyViewPart which has to provide a TypeHierarchyLifeCycle
 * on construction (shared type hierarchy)
 */
public class TraditionalHierarchyViewer extends TypeHierarchyViewer {
	
	public TraditionalHierarchyViewer(Composite parent, TypeHierarchyLifeCycle lifeCycle, ILabelProvider lprovider, IWorkbenchPart part) {
		super(parent, new TraditionalHierarchyContentProvider(lifeCycle), lprovider, part);
	}
	
	/*
	 * @see TypeHierarchyViewer#getTitle
	 */	
	public String getTitle() {
		if (isMethodFiltering()) {
			return TypeHierarchyMessages.getString("TraditionalHierarchyViewer.filtered.title"); //$NON-NLS-1$
		} else {
			return TypeHierarchyMessages.getString("TraditionalHierarchyViewer.title"); //$NON-NLS-1$
		}
	}

	/*
	 * @see TypeHierarchyViewer#updateContent
	 */		
	public void updateContent() {
		refresh();
		expandAll();
	}	

	/**
	 * Content provider for the 'traditional' type hierarchy.
	 */	
	private static class TraditionalHierarchyContentProvider extends TypeHierarchyContentProvider {
		
			
		public TraditionalHierarchyContentProvider(TypeHierarchyLifeCycle provider) {
			super(provider);
		}
		
	
		/*
		 * @see TypeHierarchyContentProvider.getElements
		 */
		public Object[] getElements(Object parent) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				IType input= hierarchy.getType();
				if (input == null) {
					// opened on a region
					return hierarchy.getRootClasses();	
				} else {
					try {
						if (input.isInterface()) {
							return new Object[] { input };
						} else {
							return hierarchy.getRootClasses(); // will be java.lang.Object
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				}
			}
			return NO_ELEMENTS;
		}
	
		/*
		 * @see TypeHierarchyContentProvider.getTypesInHierarchy
		 */	
		protected final IType[] getTypesInHierarchy(IType type) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				return hierarchy.getSubtypes(type);
			}
			return new IType[0];
		}				
	}
}