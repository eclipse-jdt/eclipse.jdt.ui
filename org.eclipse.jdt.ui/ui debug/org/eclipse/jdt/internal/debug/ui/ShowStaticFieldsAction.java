package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * An view filter action that shows/hides static fields in a view
 */
public class ShowStaticFieldsAction extends ToggleFilterAction {

	/**
	 * The filter this action applies to the viewer
	 */
	StaticFilter fStaticFilter;

	private static final String fgPrefix= "static_variables_action";

	class StaticFilter extends ViewerFilter {

		/**
		 * @see ViewerFilter
		 */
		public boolean isFilterProperty(Object p1, Object p2) {
			return false;
		}
		
		/**
		 * @see ViewerFilter
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IAdaptable) {
				IJavaVariable var= (IJavaVariable) ((IAdaptable) element).getAdapter(IJavaVariable.class);
				if (var != null) {
					if (element.equals(fViewer.getInput())) {
						//never filter out the root
						return true;
					}
					try {
						return !var.isStatic();
					} catch (DebugException e) {
						return true;
					}
				}
			}
			return true;
		}

	}

	public ShowStaticFieldsAction() {
		fStaticFilter= new StaticFilter();
	}

	/**
	 * @see ToggleFilterAction
	 */
	protected String getPrefix() {
		return fgPrefix;
	}

	/**
	 * @see ToggleFilterAction
	 */
	protected ViewerFilter getViewerFilter() {
		return fStaticFilter;
	}

}
