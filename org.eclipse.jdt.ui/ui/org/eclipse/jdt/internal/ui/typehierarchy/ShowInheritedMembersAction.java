/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
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
	
	private void valueChanged(boolean on) {
		fMethodsViewer.showInheritedMethods(on);
	}
	
}