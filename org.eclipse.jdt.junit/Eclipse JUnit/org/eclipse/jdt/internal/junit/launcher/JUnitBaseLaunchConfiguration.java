package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
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
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.SocketUtil;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;
import org.eclipse.jdt.launching.sourcelookup.JavaSourceLocator;

/**
 * Abstract launch configuration delegate for a JUnit test.
 */
public abstract class JUnitBaseLaunchConfiguration implements ILaunchConfigurationDelegate {
	public static final String PORT_ATTR= JUnitPlugin.PLUGIN_ID+".PORT";
	public static final String TESTTYPE_ATTR= JUnitPlugin.PLUGIN_ID+".TESTTYPE";
	
	/**
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String)
	 */
	public ILaunch launch(ILaunchConfiguration configuration, String mode) throws CoreException {		
		// Java project
		String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);
		if ((projectName == null) || (projectName.trim().length() < 1)) {
			abort("No project specified", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);
		}			
		IJavaProject javaProject = getJavaModel().getJavaProject(projectName);
		if ((javaProject == null) || !javaProject.exists()) {
			abort("Invalid project specified", null, IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT);
		}
		
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
				
		// VM install type
		String vmInstallTypeId = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String)null);
		if (vmInstallTypeId == null) {
			abort("JRE type not specified", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_VM_INSTALL_TYPE);
		}		
		IVMInstallType type = JavaRuntime.getVMInstallType(vmInstallTypeId);
		if (type == null) {
			abort(MessageFormat.format("JRE type {0} does not exist.", new String[] {vmInstallTypeId}), null, IJavaLaunchConfigurationConstants.ERR_VM_INSTALL_TYPE_DOES_NOT_EXIST);
		}
		
		// VM
		String vmInstallId = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL, (String)null);
		if (vmInstallId == null) {
			abort("JRE not specified.", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_VM_INSTALL); 
		}
		IVMInstall install = type.findVMInstall(vmInstallId);
		if (install == null) {
			abort(MessageFormat.format("JRE {0} does not exist.", new String[]{vmInstallId}), null, IJavaLaunchConfigurationConstants.ERR_VM_INSTALL_DOES_NOT_EXIST);
		}		
		IVMRunner runner = install.getVMRunner(mode);
		if (runner == null) {
			abort(MessageFormat.format("Internal error: JRE {0} does not specify a VM Runner.", new String[]{vmInstallId}), null, IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST);
		}
		
		// Working directory
		String workingDir = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String)null);
		if ((workingDir != null) && (workingDir.trim().length() > 0)) {
			File dir = new File(workingDir);
			if (!dir.isDirectory()) {
				abort(MessageFormat.format("Working directory does not exist: {0}", new String[] {workingDir}), null, IJavaLaunchConfigurationConstants.ERR_WORKING_DIRECTORY_DOES_NOT_EXIST);
			}
		}
		
		// Program & VM args
		String vmArgs = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, ""); 
		ExecutionArguments execArgs = new ExecutionArguments(vmArgs, "");
		
		// Classpath
//		List classpathList = configuration.getAttribute(JavaDebugUI.CLASSPATH_ATTR, (List)null);
//		String[] classpath;
//		if (classpathList == null) {
//			classpath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
//		} else {
//			classpath = new String[classpathList.size()];
//			classpathList.toArray(classpath);
//		}
		
		// Create VM config
		IType types[]= { testType };
		int port= SocketUtil.findUnusedLocalPort(4000, 5000);  

		VMRunnerConfiguration runConfig = createVMRunner(configuration, types, port, mode);

		//runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
		runConfig.setVMArguments(execArgs.getVMArgumentsArray());
		runConfig.setWorkingDirectory(workingDir);

		// Bootpath
		List bootpathList = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_BOOTPATH, (List)null);
		if (bootpathList != null) {
			String[] bootpath = new String[bootpathList.size()];
			bootpathList.toArray(bootpath);
			runConfig.setBootClassPath(bootpath);
		}
				
		// Launch the configuration
		VMRunnerResult result = runner.run(runConfig);
		
		if (result == null) {
			return null;
		}
		// Create & return Launch
		ISourceLocator sourceLocator = new JavaSourceLocator(javaProject);
		Launch launch = new Launch(configuration, mode, sourceLocator, result.getProcesses(), result.getDebugTarget());
		launch.setAttribute(PORT_ATTR, Integer.toString(port));
		launch.setAttribute(TESTTYPE_ATTR, testType.getHandleIdentifier());
		return launch;
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
	 * Convenience method to return the launch manager.
	 * 
	 * @return the launch manager
	 */
	private ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
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
}