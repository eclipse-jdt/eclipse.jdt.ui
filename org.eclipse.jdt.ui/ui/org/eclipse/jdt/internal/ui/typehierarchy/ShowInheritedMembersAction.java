/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.JavaUIAction;

/**
 * Action to show / hide inherited members in the method view
 * Depending in the action state a different label provider is installed in the viewer
 */
public class ShowInheritedMembersAction extends JavaUIAction {
	
	private MethodsViewer fMethodsViewer;
	
	private static final String RESOURCE_PREFIX= "ShowInheritedMembersAction.";
	
	/** 
	 * Creates the action.
	 */
	public ShowInheritedMembersAction(MethodsViewer viewer, boolean initValue) {
		super(JavaPlugin.getResourceBundle(), RESOURCE_PREFIX);
		//setImageDescriptor(JavaPluginImages.DESC_LCL_SHOW_INHERITED);
		setImageDescriptors("lcl16", "inher_co.gif");
		fMethodsViewer= viewer;
		
		setChecked(initValue);
		valueChanged(initValue);
	}
	

	/**
	 * @see Action#actionPerformed
	 */	
	public void run() {
		valueChanged(isChecked());
	}
	
	public void updateState() {
		setChecked(fMethodsViewer.isShowInheritedMethods());
	}
	
	
	private void valueChanged(boolean on) {
		fMethodsViewer.showInheritedMethods(on);
		if (on) {
			setToolTipText(JavaPlugin.getResourceString(RESOURCE_PREFIX + "tooltip.checked"));
		} else {
			setToolTipText(JavaPlugin.getResourceString(RESOURCE_PREFIX + "tooltip.unchecked"));
		}
	}
	
}