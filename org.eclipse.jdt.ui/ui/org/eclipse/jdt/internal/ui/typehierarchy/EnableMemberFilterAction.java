/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.JavaUIAction;

/**
 * Action enable / disable member filtering
 */
public class EnableMemberFilterAction extends JavaUIAction {

	private TypeHierarchyViewPart fView;	
	
	public static final String RESOURCE_PREFIX= "EnableMemberFilterAction.";
	
	public EnableMemberFilterAction(TypeHierarchyViewPart v, boolean initValue) {
		super(JavaPlugin.getResourceBundle(), RESOURCE_PREFIX);
		setImageDescriptors("lcl16", "impl_co.gif");
		//setImageDescriptor(JavaPluginImages.DESC_LCL_MEMBER_FILTER);
		fView= v;
		valueChanged(initValue);
	}

	/**
	 * @see Action#actionPerformed
	 */		
	public void run() {
		valueChanged(isChecked());
	}
	
	private void valueChanged(boolean on) {
		setChecked(on);
		fView.enableMemberFilter(on);
		if (on) {
			setToolTipText(JavaPlugin.getResourceString(RESOURCE_PREFIX + "tooltip.checked"));
		} else {
			setToolTipText(JavaPlugin.getResourceString(RESOURCE_PREFIX + "tooltip.unchecked"));
		}
	}
	
}