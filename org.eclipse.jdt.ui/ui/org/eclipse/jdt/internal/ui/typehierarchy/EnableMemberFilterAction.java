/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action enable / disable member filtering
 */
public class EnableMemberFilterAction extends Action {

	private TypeHierarchyViewPart fView;	
	
	public EnableMemberFilterAction(TypeHierarchyViewPart v, boolean initValue) {
		super(TypeHierarchyMessages.getString("EnableMemberFilterAction.label")); //$NON-NLS-1$
		setDescription(TypeHierarchyMessages.getString("EnableMemberFilterAction.description")); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getString("EnableMemberFilterAction.tooltip")); //$NON-NLS-1$
		
		JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$

		fView= v;
		setChecked(initValue);
	}

	/*
	 * @see Action#actionPerformed
	 */		
	public void run() {
		fView.enableMemberFilter(isChecked());
	}

	/*
	 * @see Action#actionPerformed
	 */			
	public void setChecked(boolean checked) {
		if (checked) {
			setToolTipText(TypeHierarchyMessages.getString("EnableMemberFilterAction.tooltip.checked")); //$NON-NLS-1$
		} else {
			setToolTipText(TypeHierarchyMessages.getString("EnableMemberFilterAction.tooltip.unchecked")); //$NON-NLS-1$
		}
		super.setChecked(checked);
	}
	
}