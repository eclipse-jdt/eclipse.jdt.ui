package org.eclipse.jdt.internal.junit.launcher;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.debug.ui.JavaDebugUI;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;

import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;

import org.eclipse.jdt.internal.debug.ui.DebugUIMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.SocketUtil;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

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
		return verifyAndLaunch(configuration, mode, true);
	}

	/**
	 * @see ILaunchConfigurationDelegate#verify(ILaunchConfiguration, String)
	 */
	public void verify(ILaunchConfiguration configuration, String mode) throws CoreException {
		verifyAndLaunch(configuration, mode, false);
	}

	/**
	 * This delegate can initialize defaults for context objects that are IJavaElements
	 * (ICompilationUnits or IClassFiles), IFiles, and IEditorInputs.  The job of this method
	 * is to get an IJavaElement for the context then call the method that does the real work.
	 * 
	 * @see ILaunchConfigurationDelegate#initializeDefaults(ILaunchConfigurationWorkingCopy, Object)
	 */
	public void initializeDefaults(ILaunchConfigurationWorkingCopy configuration, Object object) {
		if (object instanceof IJavaElement) {
			initializeDefaults(configuration, (IJavaElement)object);
		} else if (object instanceof IFile) {
			IJavaElement javaElement = JavaCore.create((IFile)object);
			initializeDefaults(configuration, javaElement);			
		} else if (object instanceof IEditorInput) {
			IJavaElement javaElement = (IJavaElement) ((IEditorInput)object).getAdapter(IJavaElement.class);
			if (javaElement != null)
				initializeDefaults(configuration, javaElement);
		}
	}
	
	/**
	 * Attempt to initialize default attribute values on the specified working copy by
	 * retrieving the values from persistent storage on the resource associated with the
	 * specified IJavaElement.  If any of the required attributes cannot be found in
	 * persistent storage, this is taken to mean that there are no persisted defaults for 
	 * the IResource, and the working copy is initialized entirely from context and
	 * hard-coded defaults.
	 */
	protected void initializeDefaults(ILaunchConfigurationWorkingCopy workingCopy, IJavaElement javaElement) {
		
		// First look for a default config for this config type and the specified resource
		try {
			IResource resource = javaElement.getUnderlyingResource();
			if (resource != null) {
				String configTypeID = workingCopy.getType().getIdentifier();
				boolean foundDefault = getLaunchManager().initializeFromDefaultLaunchConfiguration(resource, workingCopy, configTypeID);
				if (foundDefault) {
					initializeFromContextJavaProject(workingCopy, javaElement);
					initializeFromContextTestClassAndName(workingCopy, javaElement);
					return;
				}
			}
		} catch (JavaModelException jme) {			
		} catch (CoreException ce) {			
		}
				
		// If no default config was found, initialize all attributes we can from the specified 
		// context object and from 'hard-coded' defaults known to this delegate
		initializeFromContextJavaProject(workingCopy, javaElement);
		initializeFromContextTestClassAndName(workingCopy, javaElement);
		initializeFromDefaultVM(workingCopy);
		initializeFromDefaultContainer(workingCopy);
		initializeFromDefaultPerspectives(workingCopy);	
		initializeFromDefaultBuild(workingCopy);				
	}
	
	/**
	 * Set the java project attribute on the working copy based on the IJavaElement.
	 */
	protected void initializeFromContextJavaProject(ILaunchConfigurationWorkingCopy workingCopy, IJavaElement javaElement) {
		IJavaProject javaProject = javaElement.getJavaProject();
		if ((javaProject == null) || !javaProject.exists()) {
			return;
		}
		workingCopy.setAttribute(JavaDebugUI.PROJECT_ATTR, javaProject.getElementName());		
	}
	
	/**
	 * Set the main type & name attributes on the working copy based on the IJavaElement
	 */
	protected void initializeFromContextTestClassAndName(ILaunchConfigurationWorkingCopy workingCopy, IJavaElement javaElement) {
		try {
			// we only do a search for compilation units or class files or 
			// or source references
			if ((javaElement instanceof ICompilationUnit) || 
				(javaElement instanceof ISourceReference) ||
				(javaElement instanceof IClassFile)) {
		
				IType[] types = TestSearchEngine.findTests(new BusyIndicatorRunnableContext(), new Object[] {javaElement});
				if ((types == null) || (types.length < 1)) {
					return;
				}
				// Simply grab the first main type found in the searched element
				String fullyQualifiedName = types[0].getFullyQualifiedName();
				workingCopy.setAttribute(JavaDebugUI.MAIN_TYPE_ATTR, fullyQualifiedName);
				String name = types[0].getElementName();
				workingCopy.rename(generateUniqueNameFrom(name));
			}	
		} catch (InterruptedException ie) {
		} catch (InvocationTargetException ite) {
		}			
	}
	
	/**
	 * Set the VM attributes on the working copy based on the workbench default VM.
	 */
	protected void initializeFromDefaultVM(ILaunchConfigurationWorkingCopy workingCopy) {
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		IVMInstallType vmInstallType = vmInstall.getVMInstallType();
		String vmInstallTypeID = vmInstallType.getId();
		workingCopy.setAttribute(JavaDebugUI.VM_INSTALL_TYPE_ATTR, vmInstallTypeID);

		String vmInstallID = vmInstall.getId();
		workingCopy.setAttribute(JavaDebugUI.VM_INSTALL_ATTR, vmInstallID);		
	}
	
	/**
	 * Set the default storage location of the working copy to local.
	 */
	protected void initializeFromDefaultContainer(ILaunchConfigurationWorkingCopy workingCopy) {
		workingCopy.setContainer(null);		
	}
	
	/**
	 * Set the default perspectives for Run & Debug to the DebugPerspective.
	 */
	protected void initializeFromDefaultPerspectives(ILaunchConfigurationWorkingCopy workingCopy) {
		workingCopy.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String)null);
		workingCopy.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, (String)null);				
	}
	
	/**
	 * Set the default 'build before launch' value.
	 */
	protected void initializeFromDefaultBuild(ILaunchConfigurationWorkingCopy workingCopy) {
		workingCopy.setAttribute(JavaDebugUI.BUILD_BEFORE_LAUNCH_ATTR, true);				
	}
		
	/**
	 * Verifies the given configuration can be launched, and attempts the
	 * launch as specified by the <code>launch</code> parameter.
	 * 
	 * @param configuration the configuration to validate and launch
	 * @param mode the mode in which to launch
	 * @param doLaunch whether to launch the configuration after validation
	 *  is complete
	 * @return the result launch or <code>null</code> if the launch
	 *  is not performed.
	 * @exception CoreException if the configuration is invalid or
	 *  if launching fails.
	 */
	protected ILaunch verifyAndLaunch(ILaunchConfiguration configuration, String mode, boolean doLaunch) throws CoreException {
		
		// Java project
		String projectName = configuration.getAttribute(JavaDebugUI.PROJECT_ATTR, (String)null);
		if ((projectName == null) || (projectName.trim().length() < 1)) {
			abort("No project specified", null, JavaDebugUI.UNSPECIFIED_PROJECT);
		}			
		IJavaProject javaProject = getJavaModel().getJavaProject(projectName);
		if ((javaProject == null) || !javaProject.exists()) {
			abort("Invalid project specified", null, JavaDebugUI.NOT_A_JAVA_PROJECT);
		}
		
		// test type
		String testTypeName = configuration.getAttribute(JavaDebugUI.MAIN_TYPE_ATTR, (String)null);
		if ((testTypeName == null) || (testTypeName.trim().length() < 1)) {
			abort("No test type specified", null, JavaDebugUI.UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		IType testType = null;
		try {
			testType = findType(javaProject, testTypeName);
		} catch (JavaModelException jme) {
			abort("Test type does not exist", null, JavaDebugUI.UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
		if (testType == null) {
			abort("Test type does not exist", null, JavaDebugUI.UNSPECIFIED_MAIN_TYPE); //$NON-NLS-1$
		}
				
		// VM install type
		String vmInstallTypeId = configuration.getAttribute(JavaDebugUI.VM_INSTALL_TYPE_ATTR, (String)null);
		if (vmInstallTypeId == null) {
			abort(DebugUIMessages.getString("JavaApplicationLaunchConfigurationDelegate.JRE_Type_not_specified._2"), null, JavaDebugUI.UNSPECIFIED_VM_INSTALL_TYPE); //$NON-NLS-1$
		}		
		IVMInstallType type = JavaRuntime.getVMInstallType(vmInstallTypeId);
		if (type == null) {
			abort(MessageFormat.format(DebugUIMessages.getString("JavaApplicationLaunchConfigurationDelegate.VM_Install_type_does_not_exist"), new String[] {vmInstallTypeId}), null, JavaDebugUI.VM_INSTALL_TYPE_DOES_NOT_EXIST); //$NON-NLS-1$
		}
		
		// VM
		String vmInstallId = configuration.getAttribute(JavaDebugUI.VM_INSTALL_ATTR, (String)null);
		if (vmInstallId == null) {
			abort(DebugUIMessages.getString("JavaApplicationLaunchConfigurationDelegate.JRE_not_specified._3"), null, JavaDebugUI.UNSPECIFIED_VM_INSTALL); //$NON-NLS-1$
		}
		IVMInstall install = type.findVMInstall(vmInstallId);
		if (install == null) {
			abort(MessageFormat.format(DebugUIMessages.getString("JavaApplicationLaunchConfigurationDelegate.JRE_{0}_does_not_exist._4"), new String[]{vmInstallId}), null, JavaDebugUI.VM_INSTALL_DOES_NOT_EXIST); //$NON-NLS-1$
		}		
		IVMRunner runner = install.getVMRunner(mode);
		if (runner == null) {
			abort(MessageFormat.format(DebugUIMessages.getString("JavaApplicationLaunchConfigurationDelegate.Internal_error__JRE_{0}_does_not_specify_a_VM_Runner._5"), new String[]{vmInstallId}), null, JavaDebugUI.VM_RUNNER_DOES_NOT_EXIST); //$NON-NLS-1$
		}
		
		// Working directory
		String workingDir = configuration.getAttribute(JavaDebugUI.WORKING_DIRECTORY_ATTR, (String)null);
		if ((workingDir != null) && (workingDir.trim().length() > 0)) {
			File dir = new File(workingDir);
			if (!dir.isDirectory()) {
				abort(MessageFormat.format(DebugUIMessages.getString("JavaApplicationLaunchConfiguration.Working_directory_does_not_exist"), new String[] {workingDir}), null, JavaDebugUI.WORKING_DIRECTORY_DOES_NOT_EXIST); //$NON-NLS-1$
			}
		}
		
		// If we were just verifying, we're done
		if (!doLaunch) {
			return null;
		}
		
		// Build before launch
		boolean build = configuration.getAttribute(JavaDebugUI.BUILD_BEFORE_LAUNCH_ATTR, false);
		if (build) {
			// TODO internal API use
			if (!DebugUIPlugin.saveAndBuild()) {
				return null;
			}			
		}
		
		// Program & VM args
		//String pgmArgs = configuration.getAttribute(JavaDebugUI.VM_ARGUMENTS_ATTR, "");	//$NON-NLS-1$
		String vmArgs = configuration.getAttribute(JavaDebugUI.PROGRAM_ARGUMENTS_ATTR, ""); //$NON-NLS-1$
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
		List bootpathList = configuration.getAttribute(JavaDebugUI.BOOTPATH_ATTR, (List)null);
		if (bootpathList != null) {
			String[] bootpath = new String[bootpathList.size()];
			bootpathList.toArray(bootpath);
			runConfig.setBootClassPath(bootpath);
		}
		
		// Get the configuration's perspective id's
		String runPerspID = configuration.getAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, (String)null);
		String debugPerspID = configuration.getAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, (String)null);
				
		// Launch the configuration
		VMRunnerResult result = runner.run(runConfig);
		
		// Persist config info as default values on the launched resource
		IResource resource = null;
		try {
			resource = testType.getUnderlyingResource();
		} catch (CoreException ce) {			
		}		
		if (resource != null) {
			getLaunchManager().setDefaultLaunchConfiguration(resource, configuration);
		}
		
		// Create & return Launch
		ISourceLocator sourceLocator = new JavaUISourceLocator(javaProject);
		Launch launch = new Launch(configuration, mode, sourceLocator, result.getProcesses(), result.getDebugTarget());
		launch.setAttribute(PORT_ATTR, Integer.toString(port));
		launch.setAttribute(TESTTYPE_ATTR, testType.getHandleIdentifier());
		return launch;
	}	
	
	/**
	 * Convenience method to set a persistent property on the specified IResource
	 */
	protected void persistAttribute(QualifiedName qualName, IResource resource, String value) {
		try {
			resource.setPersistentProperty(qualName, value);
		} catch (CoreException ce) {	
		}
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
	 * Construct a new config name using the name of the given config as a starting point.
	 * The new name is guaranteed not to collide with any existing config name.
	 */
	protected String generateUniqueNameFrom(String startingName) {
		String newName = startingName;
		int index = 1;
		while (getLaunchManager().isExistingLaunchConfigurationName(newName)) {
			StringBuffer buffer = new StringBuffer(startingName);
			buffer.append(" (");
			buffer.append(String.valueOf(index));
			buffer.append(')');	
			index++;
			newName = buffer.toString();		
		}		
		return newName;
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