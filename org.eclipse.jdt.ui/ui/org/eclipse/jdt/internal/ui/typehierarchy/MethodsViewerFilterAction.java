/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;


import java.util.ResourceBundle;

import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;

/**
 * Action used to enable / disable method filter properties
 */
public class MethodsViewerFilterAction extends JavaUIAction {
	
	private String fPrefix;

	private MethodsViewerFilter fFilter;
	private int fFilterProperty;
	private StructuredViewer fViewer;
	
	public MethodsViewerFilterAction(StructuredViewer viewer, MethodsViewerFilter filter,
		ResourceBundle bundle, String resourcePrefix, int property, boolean initValue) {
		super(bundle, resourcePrefix);
		fFilter= filter;
		fViewer= viewer;
		fFilterProperty= property;
		fPrefix= resourcePrefix;
		
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
			fFilter.addFilter(fFilterProperty);
			setToolTipText(JavaPlugin.getResourceString(fPrefix + "tooltip.checked"));
		} else {
			fFilter.removeFilter(fFilterProperty);
			setToolTipText(JavaPlugin.getResourceString(fPrefix + "tooltip.unchecked"));
		}
		fViewer.refresh();
	}
}