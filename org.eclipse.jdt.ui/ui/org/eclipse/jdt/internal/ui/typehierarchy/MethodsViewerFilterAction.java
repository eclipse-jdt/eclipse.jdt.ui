/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.typehierarchy;


import java.util.ResourceBundle;

import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.internal.ui.actions.JavaUIAction;

/**
 * Action used to enable / disable method filter properties
 */
public class MethodsViewerFilterAction extends JavaUIAction {

	private MethodsViewerFilter fFilter;
	private int fFilterProperty;
	private StructuredViewer fViewer;
	
	public MethodsViewerFilterAction(StructuredViewer viewer, MethodsViewerFilter filter,
		ResourceBundle bundle, String resourcePrefix, int property, boolean initValue) {
		super(bundle, resourcePrefix);
		fFilter= filter;
		fViewer= viewer;
		fFilterProperty= property;
		setChecked(initValue);
		valueChanged(initValue);
	}
	
	/**
	 * @see Action#actionPerformed
	 */
	public void run() {	
		valueChanged(isChecked());
	}
	
	private void valueChanged(boolean on) {
		if (on) {
			fFilter.removeFilter(fFilterProperty);
		} else {
			fFilter.addFilter(fFilterProperty);
		}
		fViewer.refresh();
	}
}