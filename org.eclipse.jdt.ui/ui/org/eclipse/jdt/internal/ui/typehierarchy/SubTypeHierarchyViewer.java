/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A viewer including the content provider for the subtype hierarchy.
 * Used by the TypeHierarchyViewPart which has to provide a TypeHierarchyLifeCycle
 * on construction (shared type hierarchy)
 */
public class SubTypeHierarchyViewer extends TypeHierarchyViewer {
	
	private static final String TITLE= "SubTypeHierarchyViewer.title";
	private static final String TITLE_FILTERED= "SubTypeHierarchyViewer.filtered.title";
	
	public SubTypeHierarchyViewer(Composite parent, TypeHierarchyLifeCycle lifeCycle, IWorkbenchPart part) {
		super(parent, new SubTypeHierarchyContentProvider(lifeCycle), part);
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
			expandToLevel(2);
		} else {
			expandAll();
		}
	}
	
	/**
	 * Content provider for the subtype hierarchy
	 */
	private static class SubTypeHierarchyContentProvider extends TypeHierarchyContentProvider {
		public SubTypeHierarchyContentProvider(TypeHierarchyLifeCycle lifeCycle) {
			super(lifeCycle);
		}
		
		protected final IType[] getTypesInHierarchy(IType type) {
			ITypeHierarchy hierarchy= getHierarchy();
			if (hierarchy != null) {
				return hierarchy.getSubtypes(type);
			} else {
				return new IType[0];
			}
		}
	}	
}