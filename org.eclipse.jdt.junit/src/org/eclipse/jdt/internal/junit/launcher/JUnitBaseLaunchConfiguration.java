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

 
import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

/**
 * Abstract launch configuration delegate for a JUnit test.
 */
public abstract class JUnitBaseLaunchConfiguration extends AbstractJavaLaunchConfigurationDelegate {

	public static final String PORT_ATTR= JUnitPlugin.PLUGIN_ID+".PORT"; //$NON-NLS-1$
	public static final String TESTTYPE_ATTR= JUnitPlugin.PLUGIN_ID+".TESTTYPE"; //$NON-NLS-1$
	public static final String TESTNAME_ATTR= JUnitPlugin.PLUGIN_ID+".TESTNAME"; //$NON-NLS-1$
	public static final String ATTR_KEEPRUNNING = JUnitPlugin.PLUGIN_ID+ ".KEEPRUNNING_ATTR"; //$NON-NLS-1$
	public static final String LAUNCH_CONTAINER_ATTR= JUnitPlugin.PLUGIN_ID+".CONTAINER"; //$NON-NLS-1$
	/**
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor pm) throws CoreException {		
		IJavaProject javaProject= getJavaProject(configuration);
		if ((javaProject == null) || !javaProject.exists()) {
			abort(JUnitMessages.getString("JUnitBaseLaunchConfiguration.error.invalidproject"), null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); //$NON-NLS-1$ //$NON-NLS-2$
		}
		IType[] testTypes = getTestTypes(configuration, javaProject, pm);
		if (testTypes.length == 0) {
			abort(JUnitMessages.getString("JUnitBaseLaunchConfiguration.error.notests"), null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		IVMInstall install= getVMInstall(configuration);
		IVMRunner runner = install.getVMRunner(mode);
		if (runner == null) {
			abort(MessageFormat.format(JUnitMessages.getString("JUnitBaseLaunchConfiguration.error.novmrunner"), new String[]{install.getId()}), null, IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST); //$NON-NLS-1$
		}
		
		int port= SocketUtil.findUnusedLocalPort("", 5000, 15000);   //$NON-NLS-1$
		VMRunnerConfiguration runConfig= launchTypes(configuration, mode, testTypes, port);
		setDefaultSourceLocator(launch, configuration);
		
		launch.setAttribute(PORT_ATTR, Integer.toString(port));
		launch.setAttribute(TESTTYPE_ATTR, testTypes[0].getHandleIdentifier());
		runner.run(runConfig, launch, pm);		
	}

	protected VMRunnerConfiguration launchTypes(ILaunchConfiguration configuration,
					String mode, IType[] tests, int port) throws CoreException {
		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) 
			workingDirName = workingDir.getAbsolutePath();
		
		// Program & VM args
		String vmArgs= getVMArguments(configuration);
		ExecutionArguments execArgs = new ExecutionArguments(vmArgs, ""); //$NON-NLS-1$
				
		VMRunnerConfiguration runConfig= createVMRunner(configuration, tests, port, mode);
		runConfig.setVMArguments(execArgs.getVMArgumentsArray());
		runConfig.setWorkingDirectory(workingDirName);
		
		String[] bootpath = getBootpath(configuration);
		runConfig.setBootClassPath(bootpath);
		String[][] bootpathInfo = getBootpathExt(configuration);
		runConfig.setPrependBootClassPath(bootpathInfo[0]);
		runConfig.setMainBootClassPath(bootpathInfo[1]);
		runConfig.setAppendBootClassPath(bootpathInfo[2]);
		
		return runConfig;
	}

	public IType[] getTestTypes(ILaunchConfiguration configuration, IJavaProject javaProject, IProgressMonitor pm) throws CoreException {
		String testTypeName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
		if (pm == null)
			pm= new NullProgressMonitor();
		
		String containerHandle = configuration.getAttribute(LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$
		if (containerHandle.length() == 0) {
			return findSingleTest(javaProject, testTypeName);
		}
		else 
			return findTestsInContainer(javaProject, containerHandle, pm);
	}
	/**
	 * @inheritdoc 
	 * @param javaProject
	 * @param containerHandle
	 * @param pm
	 * @return
	 */
	private IType[] findTestsInContainer(IJavaProject javaProject, String containerHandle, IProgressMonitor pm) {
		IJavaElement container= JavaCore.create(containerHandle);
		Set result= new HashSet();
		try {
			TestSearchEngine.doFindTests(new Object[]{container}, result, pm);
		} catch (InterruptedException e) {
		}
		return (IType[]) result.toArray(new IType[result.size()]) ;
	}


	public IType[] findSingleTest(IJavaProject javaProject, String testName) throws CoreException {
		IType type = null;
		try {
			type = findType(javaProject, testName);
		} catch (JavaModelException jme) {
			abort("Test type does not exist", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		if (type == null) {
			abort("Test type does not exist", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		return new IType[]{type};
	}
	
	/**
	 * Throws a core exception with the given message and optional
	 * exception. The exception's status code will indicate an error.
	 * 
	 * @param message error message
	 * @param exception cause of the error, or <code>null</code>
	 * @exception CoreException with the given message and underlying
	 *  exception
	 */
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, JUnitPlugin.PLUGIN_ID, code, message, exception));
	}
	
	/**
	 * Find the specified (fully-qualified) type name in the specified java project.
	 */
	private IType findType(IJavaProject javaProject, String mainTypeName) throws JavaModelException {
		return javaProject.findType(mainTypeName);
	}
	
	/**
	 * Override to create a custom VMRunnerConfiguration for a launch configuration.
	 */
	protected abstract VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration, IType[] testTypes, int port, String runMode) throws CoreException;

	protected boolean keepAlive(ILaunchConfiguration config) {
		try {
			return config.getAttribute(ATTR_KEEPRUNNING, false);
		} catch(CoreException e) {
		}
		return false;
	}
}
