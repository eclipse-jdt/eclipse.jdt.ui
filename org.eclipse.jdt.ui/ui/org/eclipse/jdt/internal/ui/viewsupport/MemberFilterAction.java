/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Action used to enable / disable method filter properties
 */
public class MemberFilterAction extends Action {

	private int fFilterProperty;
	private MemberFilterActionGroup fFilterActionGroup;
	
	private String fCheckedTooltip;
	private String fUncheckedTooltip;
	
	public MemberFilterAction(MemberFilterActionGroup actionGroup, String title, int property, String contextHelpId, boolean initValue) {
		super(title);
		fFilterActionGroup= actionGroup;
		fFilterProperty= property;
		
		fCheckedTooltip= ""; //$NON-NLS-1$
		fUncheckedTooltip= ""; //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, contextHelpId);

		setChecked(initValue);
	}
	
	/**
	 * Returns this action's filter property.
	 */
	public int getFilterProperty() {
		return fFilterProperty;
	}
	
	/**
	 * Sets the unchecked-tooltip text.
	 * @param uncheckedTooltip The unchecked-tooltip text
	 */
	public void setToolTipUnchecked(String uncheckedTooltip) {
		fUncheckedTooltip= uncheckedTooltip;
		updateToolTip(isChecked());
	}


	/**
	 * Sets the checked-tooltip text.
	 * @param checkedTooltip The checked tooltip text
	 */
	public void setToolTipChecked(String checkedTooltip) {
		fCheckedTooltip= checkedTooltip;
		updateToolTip(isChecked());		
	}	
	
	/*
	 * @see Action#actionPerformed
	 */
	public void run() {	
		fFilterActionGroup.setMemberFilter(fFilterProperty, isChecked());
	}
	
	private void updateToolTip(boolean on) {
		if (on) {
			setToolTipText(fCheckedTooltip);
		} else {
			setToolTipText(fUncheckedTooltip);
		}
	}
		
	/*
	 * @see Action#setChecked
	 */	
	public void setChecked(boolean on) {
		updateToolTip(on);
		super.setChecked(on);
	}
}