/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import java.util.HashSet;
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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.IJUnitStatusConstants;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

/**
 * @deprecated Extend {@link JUnitLaunchConfigurationDelegate} instead
 */
public abstract class JUnitBaseLaunchConfiguration extends AbstractJavaLaunchConfigurationDelegate {

	public static final String NO_DISPLAY_ATTR = JUnitLaunchConfigurationConstants.ATTR_NO_DISPLAY;

	public static final String RUN_QUIETLY_MODE =JUnitLaunchConfigurationConstants.MODE_RUN_QUIETLY_MODE;
	
	public static final String PORT_ATTR= JUnitLaunchConfigurationConstants.ATTR_PORT;
	/**
	 * The single test type, or "" iff running a launch container.
	 */
	public static final String TESTTYPE_ATTR= JUnitPlugin.PLUGIN_ID+".TESTTYPE"; //$NON-NLS-1$
	/**
	 * The test method, or "" iff running the whole test type.
	 */
	public static final String TESTNAME_ATTR= JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME;
	public static final String ATTR_KEEPRUNNING = JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING;
	/**
	 * The launch container, or "" iff running a single test type.
	 */
	public static final String LAUNCH_CONTAINER_ATTR= JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER;

	/**
	 * The name of the prioritized tests
	 */
	public static final String FAILURES_FILENAME_ATTR= JUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES;
	public static final String TEST_KIND_ATTR = JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND;
	
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

	protected final TestSearchResult findTestTypes(ILaunchConfiguration configuration, IProgressMonitor pm) throws CoreException {
		IJavaProject javaProject= getJavaProject(configuration);
		if ((javaProject == null) || !javaProject.exists()) {
			informAndAbort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_invalidproject, null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); 
		}
		if (!TestSearchEngine.hasTestCaseType(javaProject)) {
			informAndAbort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junitnotonpath, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
		}

		ITestKind testKind= JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		if (testKind.isNull()) {
			testKind= TestKindRegistry.getDefault().getKind(TestKindRegistry.JUNIT3_TEST_KIND_ID); // backward compatible for launch configurations with no runner
		}
				
		boolean isJUnit4Configuration= TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKind.getId());
		if (isJUnit4Configuration && ! TestSearchEngine.hasTestAnnotation(javaProject)) {
			informAndAbort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junit4notonpath, null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
		}
		
		IJavaElement testTarget= getTestTarget(configuration, javaProject);
		HashSet result= new HashSet();
		testKind.getFinder().findTestsInContainer(testTarget, result, pm);
		if (result.isEmpty()) {
			String msg= Messages.format(JUnitMessages.JUnitLaunchConfigurationDelegate_error_notests_kind, testKind.getDisplayName());
			informAndAbort(msg, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); 
		}
		IType[] types= (IType[]) result.toArray(new IType[result.size()]);
		return new TestSearchResult(types, testKind);
	}

	private void informAndAbort(String message, Throwable exception, int code) throws CoreException {
		IStatus status= new Status(IStatus.INFO, JUnitPlugin.PLUGIN_ID, code, message, exception);
		if (showStatusMessage(status))
			throw new CoreException(status);
		abort(message, exception, code);
	}

	private final VMRunnerConfiguration launchTypes(ILaunchConfiguration configuration, String mode, TestSearchResult tests, int port) throws CoreException {
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

	private final IJavaElement getTestTarget(ILaunchConfiguration configuration, IJavaProject javaProject) throws CoreException {
		String containerHandle = configuration.getAttribute(LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$
		if (containerHandle.length() != 0) {
			 IJavaElement element= JavaCore.create(containerHandle);
			 if (element == null || !element.exists()) {
				 informAndAbort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_input_element_deosn_not_exist, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); 
			 }
			 return element;
		}
		String testTypeName= configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ""); //$NON-NLS-1$
		if (testTypeName.length() != 0) {
			testTypeName= performStringSubstitution(testTypeName);
			IType type= javaProject.findType(testTypeName);
			if (type != null) {
				return type;
			}
		}
		informAndAbort(JUnitMessages.JUnitLaunchConfigurationDelegate_input_type_does_not_exist, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
		return null; // not reachable
	}
	
	private final String performStringSubstitution(String testTypeName) throws CoreException {
		return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(testTypeName);
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
							MessageDialog.openInformation(shell, JUnitMessages.JUnitLaunchConfigurationDelegate_dialog_title, status.getMessage());
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

	protected List getBasicArguments(ILaunchConfiguration configuration, int port, String runMode, TestSearchResult result) throws CoreException {
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
}
