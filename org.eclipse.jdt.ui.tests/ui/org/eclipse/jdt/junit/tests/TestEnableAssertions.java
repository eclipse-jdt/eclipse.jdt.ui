/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     John Kaplan (johnkaplantech@gmail.com) - initial API and implementation
 *     		(report 45408: Enable assertions during unit tests [JUnit])
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import junit.framework.TestCase;

import org.eclipse.jdt.testplugin.util.DialogCheck;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;

import org.eclipse.jdt.internal.junit.launcher.AssertionVMArg;
import org.eclipse.jdt.internal.junit.launcher.JUnitTabGroup;
import org.eclipse.jdt.internal.junit.ui.JUnitPreferencePage;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TestEnableAssertions extends TestCase {
	private static final String configName = "NoOneWouldEverThinkOfUsingANameLikeThis"; //$NON-NLS-1$

	public void testAssertionsOffByDefault() {
		assertFalse(AssertionVMArg.getEnableAssertionsPreference());
	}

	public void testEnableAssertionsInWizard() {
		JUnitPreferencePage page = new JUnitPreferencePage();

		page.createControl(DialogCheck.getShell());

		page.setAssertionCheckBoxSelection(true);
		page.performOk();

		assertTrue(AssertionVMArg.getEnableAssertionsPreference());

		page.setAssertionCheckBoxSelection(false);
		page.performOk();

		assertFalse(AssertionVMArg.getEnableAssertionsPreference());
	}

	public void testJUnitTabGroupSetDefaults() {
		IWorkbenchPart activePart= JavaPlugin.getActiveWorkbenchWindow().getPartService().getActivePart();
		if (activePart != null) {
			ISelectionProvider selectionProvider= activePart.getSite().getSelectionProvider();
			if (selectionProvider != null) {
				// make sure there's no active selection, otherwise JUnitLaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
				// can fail because it tries to initialize attributes on a selection 
				selectionProvider.setSelection(new StructuredSelection());
			}
		}
		
		JUnitTabGroup testSubject= new JUnitTabGroup();
		tabGroupSetDefaultTester(testSubject);
	}

	/* can't test this here because can't access pde classes from jdt plugin test
	public void testAbstractPDELaunchConfigurationTabGroupSetDefaults()
	{
		EclipseApplicationLauncherTabGroup testSubject =
			new EclipseApplicationLauncherTabGroup();
		tabGroupSetDefaultTester(testSubject);
	}*/

	protected void tabGroupSetDefaultTester(ILaunchConfigurationTabGroup testSubject) {
		boolean originalPreference= AssertionVMArg.getEnableAssertionsPreference();
		try {
			AssertionVMArg.setEnableAssertionsPreference(false);
			ILaunchConfigurationWorkingCopy wcFalse= getNewConfigWorkingCopy();
			testSubject.createTabs(null, null);
			testSubject.setDefaults(wcFalse);
			assertFalse("Enable assertions argument should not be enabled", getAssertArgEnabled(wcFalse));

			AssertionVMArg.setEnableAssertionsPreference(true);
			ILaunchConfigurationWorkingCopy wcTrue= getNewConfigWorkingCopy();
			testSubject.setDefaults(wcTrue);
			assertTrue("Enable assertions argument should be enabled", getAssertArgEnabled(wcTrue));
		} catch (CoreException err) {
			throw new RuntimeException(err);
		} finally {
			AssertionVMArg.setEnableAssertionsPreference(originalPreference);
		}
	}

	protected ILaunchConfigurationWorkingCopy getNewConfigWorkingCopy() throws CoreException {
		ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType= lm.getLaunchConfigurationType("org.eclipse.jdt.junit.launchconfig"); //$NON-NLS-1$
		String computedName= DebugPlugin.getDefault().getLaunchManager().generateLaunchConfigurationName(configName);
		return configType.newInstance(null, computedName);
	}

	protected boolean getAssertArgEnabled(ILaunchConfigurationWorkingCopy wc) throws CoreException {
		String vmArgs= wc.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""); //$NON-NLS-1$
		String[] argArray= DebugPlugin.parseArguments(vmArgs);
		int assertArgIndex= AssertionVMArg.findAssertEnabledArg(argArray);
		return (assertArgIndex != AssertionVMArg.ASSERT_ARG_NOT_FOUND);
	}
}
