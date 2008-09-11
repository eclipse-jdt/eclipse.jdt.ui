/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Kaplan (johnkaplantech@gmail.com) - initial API and implementation
 *     		(report 45408: Enable assertions during unit tests [JUnit])
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.JUnitPreferencesConstants;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Utilities to manipulate virtual machine arguments to enable assertions
 * according to user preferences.
 */
public class AssertionVMArg {

	private static final String LONG_VM_ARG_TEXT = "-enableassertions"; //$NON-NLS-1$
	private static final String SHORT_VM_ARG_TEXT = "-ea"; //$NON-NLS-1$

	public static final int ASSERT_ARG_NOT_FOUND = -1;

	/**
	 * Sets default VM args in launch configuration to enable assertions if
	 * user preference indicates such, or blank if not.
	 *
	 * @param config the launch configuration to default
	 */
	public static void setArgDefault(ILaunchConfigurationWorkingCopy config) {
		String argText= getEnableAssertionsPreference() ? SHORT_VM_ARG_TEXT : ""; //$NON-NLS-1$
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, argText);
	}

	public static String enableAssertInArgString(String currentArgs) {
		String[] argArray= DebugPlugin.parseArguments(currentArgs);
		boolean assertAlreadyEnabled= (findAssertEnabledArg(argArray) != ASSERT_ARG_NOT_FOUND);
		String result= currentArgs;
		// logic here is: if assertion is already enabled, take no action
		// if assertion is not already enabled, enable based on user preference
		if (!assertAlreadyEnabled && getEnableAssertionsPreference()) {
			result= setAssertInArgString(currentArgs);
		}

		return result;
	}

	public static int findAssertEnabledArg(String[] argArray) {
		int assertArgIndex= ASSERT_ARG_NOT_FOUND;

		for (int i= 0; i < argArray.length; ++i) {
			String arg= argArray[i].toLowerCase();
			if (arg.startsWith(SHORT_VM_ARG_TEXT) || arg.startsWith(LONG_VM_ARG_TEXT)) {
				assertArgIndex= i;
				break;
			}
		}

		return assertArgIndex;
	}

	public static String setAssertInArgString(String currentArgs) {
		// add argument to end (with separating space)
		// ..if there are VM arguments already,
		// ..otherwise, default args to single enabling arg
		return (currentArgs.length() == 0)
			? SHORT_VM_ARG_TEXT
			: currentArgs + " " + SHORT_VM_ARG_TEXT; //$NON-NLS-1$
	}

	public static boolean getEnableAssertionsPreference() {
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(JUnitPreferencesConstants.ENABLE_ASSERTIONS);
	}

	public static void setEnableAssertionsPreference(boolean preference) {
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
		store.setValue(JUnitPreferencesConstants.ENABLE_ASSERTIONS, preference);
	}

	/* not needed unless you're manipulating already entered configurations
	public static String removeAssertFromArgString(String currentArgs, String assertArg)
	{
		currentArgs.indexOf(assertArg);
	}

	public static void syncAssertionVMArgInConfigs()
	{
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType= lm.getLaunchConfigurationType(JUnitLaunchConfiguration.ID_JUNIT_APPLICATION);
		try {
			ILaunchConfiguration[] configs= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			for (int i=0; i<configs.length; ++i) {
				ILaunchConfiguration config = configs[i];
				String vmArgs = config.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""); //$NON-NLS-1$
				String[] argArray = DebugPlugin.parseArguments(vmArgs);
				int assertArgIndex = findAssertEnabledArg(argArray);
				boolean assertAlreadyEnabled = (assertArgIndex != ASSERT_ARG_NOT_FOUND);
				if (assertAlreadyEnabled != getEnableAssertionsPreference()) {
					if (assertAlreadyEnabled && !getEnableAssertionsPreference()) {
						// remove the assertion argument
						vmArgs = removeAssertFromArgString(vmArgs, argArray[assertArgIndex]);
					}
					else if (!assertAlreadyEnabled && getEnableAssertionsPreference()) {
						// add the assertion argument
						vmArgs = setAssertInArgString(vmArgs);
					}
					// save whatever change was made
					ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
					wc.setAttribute(
						IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
					wc.doSave();
				}
			}
		} catch (CoreException e) {
			JUnitPlugin.log(e);
		}

	}*/
}
