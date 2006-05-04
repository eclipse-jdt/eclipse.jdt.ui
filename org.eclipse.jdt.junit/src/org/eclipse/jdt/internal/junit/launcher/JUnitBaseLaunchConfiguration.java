/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.variables.VariablesPlugin;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.IJUnitStatusConstants;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

/**
 * Abstract launch configuration delegate for a JUnit test.
 */
public abstract class JUnitBaseLaunchConfiguration extends AbstractJavaLaunchConfigurationDelegate {

	public static final String NO_DISPLAY_ATTR = JUnitPlugin.PLUGIN_ID + ".NO_DISPLAY"; //$NON-NLS-1$

	public static final String RUN_QUIETLY_MODE = "runQuietly"; //$NON-NLS-1$
	
	public static final String PORT_ATTR= JUnitPlugin.PLUGIN_ID+".PORT"; //$NON-NLS-1$
	/**
	 * The single test type, or "" iff running a launch container.
	 */
	public static final String TESTTYPE_ATTR= JUnitPlugin.PLUGIN_ID+".TESTTYPE"; //$NON-NLS-1$
	/**
	 * The test method, or "" iff running the whole test type.
	 */
	public static final String TESTNAME_ATTR= JUnitPlugin.PLUGIN_ID+".TESTNAME"; //$NON-NLS-1$
	public static final String ATTR_KEEPRUNNING = JUnitPlugin.PLUGIN_ID+ ".KEEPRUNNING_ATTR"; //$NON-NLS-1$
	/**
	 * The launch container, or "" iff running a single test type.
	 */
	public static final String LAUNCH_CONTAINER_ATTR= JUnitPlugin.PLUGIN_ID+".CONTAINER"; //$NON-NLS-1$

	/**
	 * The name of the prioritized tests
	 */
	public static final String FAILURES_FILENAME_ATTR= JUnitPlugin.PLUGIN_ID+".FAILURENAMES"; //$NON-NLS-1$

	public static final String TEST_KIND_ATTR = JUnitPlugin.PLUGIN_ID+".TEST_KIND"; //$NON-NLS-1$
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor pm) throws CoreException {		
		if (mode.equals(RUN_QUIETLY_MODE)) {
			launch.setAttribute(NO_DISPLAY_ATTR, "true"); //$NON-NLS-1$
			mode = ILaunchManager.RUN_MODE;
		}
			
		TestSearchResult testTypes = findTestTypes(configuration, pm);
		IVMInstall install= getVMInstall(configuration);
		IVMRunner runner = install.getVMRunner(mode);
		if (runner == null) {
			abort(Messages.format(JUnitMessages.JUnitBaseLaunchConfiguration_error_novmrunner, new String[]{install.getId()}), null, IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST); 
		}
		
		int port= SocketUtil.findFreePort();
		VMRunnerConfiguration runConfig= launchTypes(configuration, mode, testTypes, port);
		setDefaultSourceLocator(launch, configuration);
		
		launch.setAttribute(PORT_ATTR, Integer.toString(port));
		launch.setAttribute(TESTTYPE_ATTR, testTypes.getTypes()[0].getHandleIdentifier());
		runner.run(runConfig, launch, pm);		
	}

	/**
	 * @param configuration
	 * @param pm
	 * @return The types representing tests to be launched for this
	 *         configuration
	 * @throws CoreException
	 */
	protected TestSearchResult findTestTypes(ILaunchConfiguration configuration, IProgressMonitor pm) throws CoreException {
		IJavaProject javaProject= getJavaProject(configuration);
		if ((javaProject == null) || !javaProject.exists()) {
			informAndAbort(JUnitMessages.JUnitBaseLaunchConfiguration_error_invalidproject, null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); 
		}
		if (!TestSearchEngine.hasTestCaseType(javaProject)) {
			informAndAbort(JUnitMessages.JUnitBaseLaunchConfiguration_error_junitnotonpath, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
		}
		final ITestSearchExtent testTarget= testSearchTarget(configuration, javaProject, pm);
		TestSearchResult searchResult= TestKindRegistry.getDefault().getTestTypes(configuration, testTarget);
		if (searchResult.isEmpty()) {
			informAndAbort(JUnitMessages.JUnitBaseLaunchConfiguration_error_notests, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); 
		}
		return searchResult;
	}

	protected void informAndAbort(String message, Throwable exception, int code) throws CoreException {
		IStatus status= new Status(IStatus.INFO, JUnitPlugin.PLUGIN_ID, code, message, exception);
		if (showStatusMessage(status))
			throw new CoreException(status);
		abort(message, exception, code);
	}

	protected VMRunnerConfiguration launchTypes(ILaunchConfiguration configuration,
					String mode, TestSearchResult tests, int port) throws CoreException {
		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) 
			workingDirName = workingDir.getAbsolutePath();
		
		// Program & VM args
		String vmArgs= getVMArguments(configuration);
		ExecutionArguments execArgs = new ExecutionArguments(vmArgs, ""); //$NON-NLS-1$
		String[] envp= getEnvironment(configuration);

		VMRunnerConfiguration runConfig= createVMRunner(configuration, tests, port, mode);
		runConfig.setVMArguments(execArgs.getVMArgumentsArray());
		runConfig.setWorkingDirectory(workingDirName);
		runConfig.setEnvironment(envp);

		Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
		runConfig.setVMSpecificAttributesMap(vmAttributesMap);

		String[] bootpath = getBootpath(configuration);
		runConfig.setBootClassPath(bootpath);
		
		return runConfig;
	}

	public ITestSearchExtent testSearchTarget(ILaunchConfiguration configuration, IJavaProject javaProject, IProgressMonitor pm) throws CoreException {
		String containerHandle = configuration.getAttribute(LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$
		if (containerHandle.length() != 0) {
			return containerTestTarget(containerHandle, pm);
		}
		String testTypeName= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null);
		return singleTypeTarget(javaProject, performStringSubstitution(testTypeName));
	}
	
	protected String performStringSubstitution(String testTypeName) throws CoreException {
		return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(testTypeName);
		}

	protected ITestSearchExtent containerTestTarget(final String containerHandle, final IProgressMonitor pm) {
		return new ContainerTestSearchExtent(pm, containerHandle);
	}

	public ITestSearchExtent singleTypeTarget(final IJavaProject javaProject, final String testName) throws CoreException {
		IType type = null;
		try {
			type = javaProject.findType(testName);
		} catch (JavaModelException jme) {
			testTypeDoesNotExist();
		}
		if (type == null) {
			testTypeDoesNotExist();
		}
		return new SingleTypeTestSearchExtent(type);
	}
	
	private boolean showStatusMessage(final IStatus status) {
		final boolean[] success= new boolean[] { false };
		getDisplay().syncExec(
				new Runnable() {
					public void run() {
						Shell shell= JUnitPlugin.getActiveWorkbenchShell();
						if (shell == null)
							shell= getDisplay().getActiveShell();
						if (shell != null) {
							MessageDialog.openInformation(shell, JUnitMessages.JUnitBaseLaunchConfiguration_dialog_title, status.getMessage());
							success[0]= true;
						}
					}
				}
		);
		return success[0];
	}
	
	private Display getDisplay() {
		Display display;
		display= Display.getCurrent();
		if (display == null)
			display= Display.getDefault();
		return display;		
	}
	
	private void testTypeDoesNotExist() throws CoreException {
		abort("Test type does not exist", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.launcher.ITestFindingAbortHandler#abort(java.lang.String, java.lang.Throwable, int)
	 */
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, JUnitPlugin.PLUGIN_ID, code, message, exception));
	}
	
	/**
	 * Override to create a custom VMRunnerConfiguration for a launch configuration.
	 */
	protected abstract VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration, TestSearchResult testTypes, int port, String runMode) throws CoreException;

	protected boolean keepAlive(ILaunchConfiguration config) {
		try {
			return config.getAttribute(ATTR_KEEPRUNNING, false);
		} catch(CoreException e) {
		}
		return false;
	}

	public List getBasicArguments(ILaunchConfiguration configuration, int port, String runMode, TestSearchResult result) throws CoreException {
		ArrayList argv = new ArrayList();
		argv.add("-version"); //$NON-NLS-1$
		argv.add("3"); //$NON-NLS-1$

		argv.add("-port"); //$NON-NLS-1$
		argv.add(Integer.toString(port));

		if (keepAlive(configuration) && runMode.equals(ILaunchManager.DEBUG_MODE))
			argv.add(0, "-keepalive"); //$NON-NLS-1$

		String testLoaderId = result.getTestKind().getLoaderClassName();
		argv.add("-testLoaderClass"); //$NON-NLS-1$
		argv.add(testLoaderId);
		
		// TODO: allow the TestKind to add new options to the command line:
		// result.getTestKind().addArguments(configuration, argv)
		
		return argv;
	}
	
	public String defaultTestLoaderClass() {
		return JUnit3TestLoader.class.getName();
	}
}
