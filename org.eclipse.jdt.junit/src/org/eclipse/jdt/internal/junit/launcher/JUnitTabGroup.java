package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.internal.debug.ui.launcher.JavaArgumentsTab;
import org.eclipse.jdt.internal.debug.ui.launcher.JavaEnvironmentTab;

public class JUnitTabGroup extends AbstractLaunchConfigurationTabGroup {
	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog, String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[4];
		tabs[0]= new JUnitMainTab();
		tabs[0].setLaunchConfigurationDialog(dialog);
		tabs[1]= new JavaArgumentsTab(); 
		tabs[1].setLaunchConfigurationDialog(dialog);
		tabs[2]= new JavaEnvironmentTab();
		tabs[2].setLaunchConfigurationDialog(dialog);
		tabs[3]= new CommonTab();
		tabs[3].setLaunchConfigurationDialog(dialog);
		setTabs(tabs);
	}

	/**
	 * @see ILaunchConfigurationTabGroup#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config); 
		config.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String)null);
		//config.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, (String)null);
	}
}
