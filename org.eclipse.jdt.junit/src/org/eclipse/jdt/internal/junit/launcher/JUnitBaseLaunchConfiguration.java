package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.SocketUtil;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.sourcelookup.JavaSourceLocator;

/**
 * Abstract launch configuration delegate for a JUnit test.
 */
public abstract class JUnitBaseLaunchConfiguration extends AbstractJavaLaunchConfigurationDelegate {

	public static final String PORT_ATTR= JUnitPlugin.PLUGIN_ID+".PORT"; //$NON-NLS-1$
	public static final String TESTTYPE_ATTR= JUnitPlugin.PLUGIN_ID+".TESTTYPE"; //$NON-NLS-1$
	public static final String ATTR_KEEPRUNNING = JUnitPlugin.PLUGIN_ID+ ".KEEPRUNNING_ATTR"; //$NON-NLS-1$
	/**
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor pm) throws CoreException {		
		IJavaProject javaProject= getJavaProject(configuration);
		if ((javaProject == null) || !javaProject.exists()) {
			abort(JUnitMessages.getString("JUnitBaseLaunchConfiguration.error.invalidproject"), null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT); //$NON-NLS-1$
		}
		IType testType = getTestType(configuration, javaProject);
		IVMInstall install= getVMInstall(configuration);
		IVMRunner runner = install.getVMRunner(mode);
		if (runner == null) {
			abort(MessageFormat.format(JUnitMessages.getString("JUnitBaseLaunchConfiguration.error.novmrunner"), new String[]{install.getId()}), null, IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST); //$NON-NLS-1$
		}
		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) 
			workingDirName = workingDir.getAbsolutePath();
		
		// Program & VM args
		String vmArgs= getVMArguments(configuration);
		ExecutionArguments execArgs = new ExecutionArguments(vmArgs, ""); //$NON-NLS-1$
		
		// Create VM config
		IType types[]= { testType };
		int port= SocketUtil.findUnusedLocalPort("", 5000, 15000);   //$NON-NLS-1$

		VMRunnerConfiguration runConfig= createVMRunner(configuration, types, port, mode);
		runConfig.setVMArguments(execArgs.getVMArgumentsArray());
		runConfig.setWorkingDirectory(workingDirName);

		String[] bootpath = getBootpath(configuration);
		runConfig.setBootClassPath(bootpath);

				
		//  set default source locator if none specified
		String id= configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String)null);
		if (id == null) {
			ISourceLocator sourceLocator = new JavaSourceLocator(javaProject);
			launch.setSourceLocator(sourceLocator);
		}
		
		launch.setAttribute(PORT_ATTR, Integer.toString(port));
		launch.setAttribute(TESTTYPE_ATTR, testType.getHandleIdentifier());
		runner.run(runConfig, launch, pm);		
	}

	public IType getTestType(ILaunchConfiguration configuration, IJavaProject javaProject) throws CoreException {
		// test type
		String testTypeName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
		if ((testTypeName == null) || (testTypeName.trim().length() < 1)) {
			abort("No test type specified", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		IType testType = null;
		try {
			testType = findType(javaProject, testTypeName);
		} catch (JavaModelException jme) {
			abort("Test type does not exist", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		if (testType == null) {
			abort("Test type does not exist", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		return testType;
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
		String pathStr= mainTypeName.replace('.', '/') + ".java"; //$NON-NLS-1$
		IJavaElement javaElement= javaProject.findElement(new Path(pathStr));
		if (javaElement == null) {
			// try to find it as inner type
			String qualifier= Signature.getQualifier(mainTypeName);
			if (qualifier.length() > 0) {
				IType type= findType(javaProject, qualifier); // recursive!
				if (type != null) {
					IType res= type.getType(Signature.getSimpleName(mainTypeName));
					if (res.exists()) {
						return res;
					}
				}
			}
		} else if (javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
			String simpleName= Signature.getSimpleName(mainTypeName);
			return ((ICompilationUnit) javaElement).getType(simpleName);
		} else if (javaElement.getElementType() == IJavaElement.CLASS_FILE) {
			return ((IClassFile) javaElement).getType();
		}
		return null;		
	}
	
	/**
	 * Convenience method to get the java model.
	 */
	private IJavaModel getJavaModel() {
		return JavaCore.create(getWorkspaceRoot());
	}

	/**
	 * Convenience method to get the workspace root.
	 */
	private IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
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