/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Toggles horizontaol / vertical layout of the type hierarchy
 */
public class ToggleOrientationAction extends Action {

	private TypeHierarchyViewPart fView;	
	
	public ToggleOrientationAction(TypeHierarchyViewPart v, boolean initHorizontal) {
		super(TypeHierarchyMessages.getString("ToggleOrientationAction.label")); //$NON-NLS-1$
		setDescription(TypeHierarchyMessages.getString("ToggleOrientationAction.description")); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getString("ToggleOrientationAction.tooltip")); //$NON-NLS-1$
		
		JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$

		fView= v;
		setChecked(initHorizontal);
	}

	/*
	 * @see Action#actionPerformed
	 */		
	public void run() {
		fView.setOrientation(isChecked()); // will toggle the checked state
	}

	/*
	 * @see Action#setChecked
	 */		
	public void setChecked(boolean checked) {
		if (checked) {
			setToolTipText(TypeHierarchyMessages.getString("ToggleOrientationAction.tooltip.checked")); //$NON-NLS-1$
		} else {
			setToolTipText(TypeHierarchyMessages.getString("ToggleOrientationAction.tooltip.unchecked")); //$NON-NLS-1$
		}
		super.setChecked(checked);
	}	
	
}