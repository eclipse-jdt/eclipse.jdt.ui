/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;

/**
 * Action used to enable / disable method filter properties
 */
public class MethodsViewerFilterAction extends Action {

	private MethodsViewerFilter fFilter;
	private int fFilterProperty;
	private StructuredViewer fViewer;
	
	private String fCheckedTooltip;
	private String fUncheckedTooltip;
	
	
	public MethodsViewerFilterAction(StructuredViewer viewer, MethodsViewerFilter filter, String title, int property, boolean initValue) {
		super(title);
		fFilter= filter;
		fViewer= viewer;
		fFilterProperty= property;
		
		fCheckedTooltip= ""; //$NON-NLS-1$
		fUncheckedTooltip= ""; //$NON-NLS-1$
		
		setChecked(initValue);
		valueChanged(initValue);
	}
	
	/**
	 * Sets the unchecked-tooltip text
	 * @param uncheckedTooltip The unchecked-tooltip text
	 */
	public void setToolTipUnchecked(String uncheckedTooltip) {
		fUncheckedTooltip= uncheckedTooltip;
		if (!isChecked()) {
			setToolTipText(uncheckedTooltip);
		}
	}


	/**
	 * Sets the checked-tooltip text
	 * @param checkedTooltip The checked tooltip text
	 */
	public void setToolTipChecked(String checkedTooltip) {
		fCheckedTooltip= checkedTooltip;
		if (isChecked()) {
			setToolTipText(checkedTooltip);
		}		
	}	
	
	/**
	 * @see Action#actionPerformed
	 */
	public void run() {	
		valueChanged(isChecked());
	}
	
	public void updateState() {
		setChecked(fFilter.hasFilter(fFilterProperty));
	}		
	
	
	private void valueChanged(boolean on) {
		if (on) {
			fFilter.addFilter(fFilterProperty);
			setToolTipText(fCheckedTooltip);
		} else {
			fFilter.removeFilter(fFilterProperty);
			setToolTipText(fUncheckedTooltip);
		}
		fViewer.refresh();
	}



}