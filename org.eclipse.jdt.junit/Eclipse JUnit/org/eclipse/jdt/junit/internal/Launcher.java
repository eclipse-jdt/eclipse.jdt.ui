/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.junit.internal.BaseLauncher;


/**
 * A launcher for running JUnit (Eclipse Plugin) Test classes. 
 * Uses JDI to launch a vm in debug mode.
 */
public class Launcher extends BaseLauncher implements IExecutableExtension{

	private static IConfigurationElement fgConfigElement;
	/**
	 * @see BaseLauncher#configureVM(IType[], int)
	 */
	public VMRunnerConfiguration configureVM(IType[] testTypes, int port) throws InvocationTargetException {
		String[] classPath= LauncherUtil.createClassPath(testTypes[0]);
		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("org.eclipse.core.launcher.UIMain", classPath);
		
		String pluginsPathFileName= pluginsPathFileName= LauncherUtil.createPluginsPathFile(testTypes[0], fgConfigElement.getAttribute("pluginID"));
		String testPluginID= LauncherUtil.getPluginRegistryModel(testTypes[0].getJavaProject()).getPlugins()[0].getId();

		IJavaProject project= testTypes[0].getJavaProject();
			
		String[] args= new String[] {
			"-application", fgConfigElement.getAttribute("application"),
			"-data", LauncherUtil.getTempWorkSpaceLocation(),
			"-plugins", pluginsPathFileName,
			"-dev", "bin",
			"-consolelog",
			"-port", Integer.toString(port), 
			"-testPluginName", testPluginID,
			"-debugging",
			"-classNames"
		};
	
		String[] classNames= new String[testTypes.length];
		for (int i= 0; i < classNames.length; i++) {
			classNames[i]= testTypes[i].getFullyQualifiedName();
		}

		String[] programArguments= new String[args.length + classNames.length];
		System.arraycopy(args, 0, programArguments, 0, args.length);
		System.arraycopy(classNames, 0, programArguments, args.length, classNames.length);
				
		vmConfig.setProgramArguments(programArguments);
		return vmConfig;
	}
	/**
	 * @see IExecutableExtension#setInitializationData(IConfigurationElement, String, Object)
	 */
	public void setInitializationData(IConfigurationElement config, String arg0, Object arg1)
		throws CoreException {
		
		fgConfigElement= config;
	}

}