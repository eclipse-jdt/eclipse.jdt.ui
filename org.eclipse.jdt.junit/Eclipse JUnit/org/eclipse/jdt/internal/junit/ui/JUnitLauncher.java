/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

 /**
 * A launcher for running JUnit Test classes. 
 */
public class JUnitLauncher extends JUnitBaseLauncherDelegate {
	/*
	 * @see JUnitBaseLauncherDelegate#configureVM(IType[], int)
	 */
	protected VMRunnerConfiguration configureVM(IType[] testTypes, int port, String runMode) throws CoreException {
		String[] classPath= createClassPath(testTypes[0]);	
		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("org.eclipse.jdt.internal.junit.runner.RemoteTestRunner", classPath);
	
		Vector argv= new Vector(10);
		argv.add("-port");
		argv.add(Integer.toString(port));
		//argv("-debugging");
		argv.add("-classNames");
				
		if (JUnitPreferencePage.getKeepJUnitAlive() && runMode.equals(ILaunchManager.DEBUG_MODE))
			argv.add(0, "-keepalive");
			
		for (int i= 0; i < testTypes.length; i++) 
			argv.add(testTypes[i].getFullyQualifiedName());
	
		String[] args= new String[argv.size()];
		argv.copyInto(args);
		vmConfig.setProgramArguments(args);
		return vmConfig;
	}
	
	private String[] createClassPath(IType type) throws CoreException {
		URL url= JUnitPlugin.getDefault().getDescriptor().getInstallURL();
		String[] cp= JavaRuntime.computeDefaultRuntimeClassPath(type.getJavaProject());
		String[] classPath= new String[cp.length + 2];
		System.arraycopy(cp, 0, classPath, 2, cp.length);
		try {
			// assumption is that the output folder is called bin!
			classPath[0]= Platform.asLocalURL(new URL(url, "bin")).getFile();
			classPath[1]= Platform.asLocalURL(new URL(url, "junitsupport.jar")).getFile();
		} catch (MalformedURLException e) {
			JUnitPlugin.log(e); // TO DO abort run and inform user
		} catch (IOException e) {
			JUnitPlugin.log(e); // TO DO abort run and inform user
		}
		return classPath;
	}
}