/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.actions.*;

/**
 * Action used to enable / disable method filter properties
 */
public class MemberFilterAction extends Action {

	private int fFilterProperty;
	private MemberFilterActionGroup fFilterActionGroup;

	private boolean fCheckedIsFiltering;
	
	public MemberFilterAction(MemberFilterActionGroup actionGroup, String title, int property, String contextHelpId, boolean initValue, boolean checkedIsFiltering) {
		super(title);
		fFilterActionGroup= actionGroup;
		fFilterProperty= property;
		fCheckedIsFiltering= checkedIsFiltering;
		
		WorkbenchHelp.setHelp(this, contextHelpId);

		setFilter(initValue);
	}
	
	/**
	 * Returns this action's filter property.
	 */
	public int getFilterProperty() {
		return fFilterProperty;
	}
	
	/*
	 * @see Action#actionPerformed
	 */
	public void run() {	
		fFilterActionGroup.setMemberFilter(fFilterProperty, isFilterSet());
	}
	
	public void setFilter(boolean doFilter) {
		if (fCheckedIsFiltering) {
			setChecked(doFilter);
		} else {
			setChecked(!doFilter);
		}
	}
	
	public boolean isFilterSet() {
		if (fCheckedIsFiltering) {
			return isChecked();
		} else {
			return !isChecked();
		}
	}
	
}