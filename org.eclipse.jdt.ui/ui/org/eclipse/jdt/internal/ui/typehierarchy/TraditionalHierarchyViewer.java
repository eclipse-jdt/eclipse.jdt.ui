/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A TypeHierarchyViewer that looks like the type hierarchy view of VA/Java:
 * Starting form Object down to the element in focus, then all subclasses from
 * this element.
 * Used by the TypeHierarchyViewPart which has to provide a TypeHierarchyLifeCycle
 * on construction (shared type hierarchy)
 */
public class TraditionalHierarchyViewer extends TypeHierarchyViewer {
	
	private static final String TITLE= "TraditionalHierarchyViewer.title";
	private static final String TITLE_FILTERED= "TraditionalHierarchyViewer.filtered.title";
	
	public TraditionalHierarchyViewer(Composite parent, TypeHierarchyLifeCycle lifeCycle, IWorkbenchPart part) {
		super(parent, new TraditionalHierarchyContentProvider(lifeCycle), part);
	}
	
	/**
	 * @see TypeHierarchyViewer#getTitle
	 */		
	public String getTitle() {
		String key;
		if (getHierarchyContentProvider().getMemberFilter() != null) {
			key= TITLE_FILTERED;
		} else {
			key= TITLE;
		}
		return JavaPlugin.getResourceString(key);
	}

	/**
	 * @see TypeHierarchyViewer#updateContent
	 */		
	public void updateContent() {
		refresh();
		if (getHierarchyContentProvider().getMemberFilter() == null) {
			expandToLevel(((TraditionalHierarchyContentProvider)getContentProvider()).getExpandLevel());
		} else {
			expandAll();
		}		
	}	

	/**
	 * Content provider for the supertype hierarchy
	 */	
	private static class TraditionalHierarchyContentProvider extends TypeHierarchyContentProvider {
		
		// the hierarchy up to the input type
		private IType[] fSuperTypesList;
		private IType fInput;
	
		public TraditionalHierarchyContentProvider(TypeHierarchyLifeCycle provider) {
			super(provider);
			fSuperTypesList= null;
			fInput= null;
		}
		
		public int getExpandLevel() {
			if (fSuperTypesList != null) {
				return fSuperTypesList.length + 2;
			}
			return 2;
		}
	
		/**
		 * @see TypeHierarchyContentProvider.getElements
		 */
		public Object[] getElements(Object parent) {
			updateSuperTypesList();
			IType[] types;
			if (fSuperTypesList == null || fSuperTypesList.length == 0) {
				return new IType[] { getInputType() };
			} else {
				return new IType[] { fSuperTypesList[fSuperTypesList.length-1] };
			}
		}
	
		/**
		 * @see TypeHierarchyContentProvider.getTypesInHierarchy
		 */	
		protected final IType[] getTypesInHierarchy(IType type) {
			updateSuperTypesList();
			IType subTypeFromList= subTypeOf(type);
			if (subTypeFromList != null) {
				return new IType[] { subTypeFromList };
			} else {
				return getHierarchy().getSubtypes(type);
			}
		}				
		
		private IType subTypeOf(IType type) {
			if (fSuperTypesList != null) {
				int index= fSuperTypesList.length - 1;
				while (index >= 0 && !fSuperTypesList[index].equals(type)) {
					index--;
				}
				if (index > 0) {
					return fSuperTypesList[index - 1];
				} else if (index == 0) {
					return getInputType();
				}
			}
			return null;
		}	
		
		private void updateSuperTypesList() {
			IType input= getInputType();
			if (input != null) {
				if (!input.equals(fInput)) {
					fInput= input;
					fSuperTypesList= getHierarchy().getAllSuperclasses(input);
				}
			} else {
				fInput= null;
				fSuperTypesList= null;
			}
		}		
		
	}
}