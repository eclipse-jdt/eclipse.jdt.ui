/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class JUnitLaunchShortcut implements ILaunchShortcut {
	
	/**
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IJavaElement element= null;
		IEditorInput input = editor.getEditorInput();
		element = (IJavaElement) input.getAdapter(IJavaElement.class);

		if (element != null) {
			searchAndLaunch(new Object[] {element}, mode);
		} 
	}
	
	/**
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			searchAndLaunch(((IStructuredSelection)selection).toArray(), mode);
		} 
	}

	protected void searchAndLaunch(Object[] search, String mode) {
		if (search != null) {
			if (search.length == 0) {
				MessageDialog.openInformation(getShell(), JUnitMessages.getString("LaunchTestAction.dialog.title"), JUnitMessages.getString("LaunchTestAction.message.notests")); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}	
			if (search[0] instanceof IJavaElement) {
				IJavaElement element= (IJavaElement)search[0];
				if (element.getElementType() < IJavaElement.COMPILATION_UNIT) {
					launchContainer(element, mode);
					return;
				}
				if (element.getElementType() == IJavaElement.METHOD) {
					launchMethod((IMethod)element, mode);
					return;
				}
			}
			// launch a CU or type
			launchType(search, mode);
		}
	}
	
	protected void launchType(Object[] search, String mode) {
		IType[] types= null;
		try {
			types= TestSearchEngine.findTests(search); 
		} catch (InterruptedException e) {
			JUnitPlugin.log(e);
			return;
		} catch (InvocationTargetException e) {
			JUnitPlugin.log(e);
			return;
		}
		IType type= null;
		if (types.length == 0) {
			MessageDialog.openInformation(getShell(), JUnitMessages.getString("LaunchTestAction.dialog.title"), JUnitMessages.getString("LaunchTestAction.message.notests")); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (types.length > 1) {
			type= chooseType(types, mode);
		} else {
			type= types[0];
		}
		if (type != null) {
			launch(type, mode);
		}
	}

	private void launchContainer(IJavaElement container, String mode) {
		String handleIdentifier= container.getHandleIdentifier();
		ILaunchConfiguration config = findLaunchConfiguration(
			mode, 
			container, 
			handleIdentifier, 
			"",  //$NON-NLS-1$
			"" //$NON-NLS-1$
		);
		if (config == null) {
			config = createConfiguration(
				container.getJavaProject(),
				container.getElementName(),
				"", //$NON-NLS-1$
				handleIdentifier,
				"" //$NON-NLS-1$
			);
		}
		launchConfiguration(mode, config);
	}
	
	private void launch(IType type, String mode) {
		String fullyQualifiedName= type.getFullyQualifiedName();
		ILaunchConfiguration config = findLaunchConfiguration(
			mode, 
			type, 
			"",  //$NON-NLS-1$
			fullyQualifiedName, 
			"" //$NON-NLS-1$
		);
		if (config == null) {
			config= createConfiguration(
				type.getJavaProject(),
				type.getElementName(),
				fullyQualifiedName,
				"", //$NON-NLS-1$
				"" //$NON-NLS-1$
			);
		}
		launchConfiguration(mode, config);
	}

	private void launchMethod(IMethod method, String mode) {
		IType declaringType= method.getDeclaringType();
		String fullyQualifiedName= declaringType.getFullyQualifiedName();
		ILaunchConfiguration config = findLaunchConfiguration(
			mode, 
			method, 
			"",  //$NON-NLS-1$
			fullyQualifiedName, 
			method.getElementName()
		);

		if (config == null) {
			config= createConfiguration(
				method.getJavaProject(),
				declaringType.getElementName()+"."+method.getElementName(), //$NON-NLS-1$
				fullyQualifiedName,
				"", //$NON-NLS-1$
				method.getElementName()
			);
		}
		launchConfiguration(mode, config);
	}


	protected void launchConfiguration(String mode, ILaunchConfiguration config) {
		if (config != null) {
			DebugUITools.launch(config, mode);
		}
	}
	
	/**
	 * Prompts the user to select a type
	 * 
	 * @return the selected type or <code>null</code> if none.
	 */
	protected IType chooseType(IType[] types, String mode) {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_POST_QUALIFIED));
		dialog.setElements(types);
		dialog.setTitle(JUnitMessages.getString("LaunchTestAction.dialog.title2")); //$NON-NLS-1$
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectTestToRun"));  //$NON-NLS-1$
		} else {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectTestToDebug")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		if (dialog.open() == Window.OK) {
			return (IType)dialog.getFirstResult();
		}
		return null;
	}
	
	private ILaunchConfiguration findLaunchConfiguration(String mode, IJavaElement element, String container, String testClass, String testName) {
		ILaunchConfigurationType configType= getJUnitLaunchConfigType();
		List candidateConfigs= Collections.EMPTY_LIST;
		try {
			ILaunchConfiguration[] configs= getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs= new ArrayList(configs.length);
			for (int i= 0; i < configs.length; i++) {
				ILaunchConfiguration config= configs[i];
				if ((config.getAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, "").equals(container)) && //$NON-NLS-1$
					(config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "").equals(testClass)) && //$NON-NLS-1$
					(config.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR,"").equals(testName)) &&   //$NON-NLS-1$
					(config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(element.getJavaProject().getElementName()))) {  //$NON-NLS-1$
						candidateConfigs.add(config);
				}
			}
		} catch (CoreException e) {
			JUnitPlugin.log(e);
		}
		
		// If there are no existing configs associated with the IType, create one.
		// If there is exactly one config associated with the IType, return it.
		// Otherwise, if there is more than one config associated with the IType, prompt the
		// user to choose one.
		int candidateCount= candidateConfigs.size();
		if (candidateCount < 1) {
			return null;
		} else if (candidateCount == 1) {
			return (ILaunchConfiguration) candidateConfigs.get(0);
		} else {
			// Prompt the user to choose a config.  A null result means the user
			// cancelled the dialog, in which case this method returns null,
			// since cancelling the dialog should also cancel launching anything.
			ILaunchConfiguration config= chooseConfiguration(candidateConfigs, mode);
			if (config != null) {
				return config;
			}
		}
		return null;
	}

	/**
	 * Show a selection dialog that allows the user to choose one of the specified
	 * launch configurations.  Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 */
	protected ILaunchConfiguration chooseConfiguration(List configList, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(JUnitMessages.getString("LaunchTestAction.message.selectConfiguration")); //$NON-NLS-1$
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectDebugConfiguration")); //$NON-NLS-1$
		} else {
			dialog.setMessage(JUnitMessages.getString("LaunchTestAction.message.selectRunConfiguration")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		int result= dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration)dialog.getFirstResult();
		}
		return null;		
	}
	

	protected ILaunchConfiguration createConfiguration(
			IJavaProject project, String name, String mainType, 
			String container, String testName) {
				
		ILaunchConfiguration config= null;
		try {
			ILaunchConfigurationType configType= getJUnitLaunchConfigType();
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(name)); 
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainType);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getElementName());
			wc.setAttribute(JUnitBaseLaunchConfiguration.ATTR_KEEPRUNNING, false);
			wc.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, container);
			if (testName.length() > 0)
				wc.setAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, testName);	
			config= wc.doSave();		
		} catch (CoreException ce) {
			JUnitPlugin.log(ce);
		}
		return config;
	}
	
	/**
	 * Returns the local java launch config type
	 */
	protected ILaunchConfigurationType getJUnitLaunchConfigType() {
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		return lm.getLaunchConfigurationType(JUnitLaunchConfiguration.ID_JUNIT_APPLICATION);		
	}	
	
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	/**
	 * Convenience method to get the window that owns this action's Shell.
	 */
	protected Shell getShell() {
		return JUnitPlugin.getActiveWorkbenchShell();
	}
}
