/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

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
	protected VMRunnerConfiguration configureVM(IType[] testTypes, int port) throws CoreException {
		String[] classPath= createClassPath(testTypes[0]);	
		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("org.eclipse.jdt.internal.junit.runner.RemoteTestRunner", classPath);
	
		String [] args= new String[] {
			"-port", Integer.toString(port),
		//	"-debugging", 			
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
	
	private String[] createClassPath(IType type) throws CoreException {
		URL url= JUnitPlugin.getDefault().getDescriptor().getInstallURL();
		String[] cp = JavaRuntime.computeDefaultRuntimeClassPath(type.getJavaProject());
		String[] classPath= new String[cp.length + 2];
		System.arraycopy(cp, 0, classPath, 2, cp.length);
		try {
			classPath[0]= Platform.asLocalURL(new URL(url, "junitsupport.jar")).getFile();
			classPath[1]= Platform.asLocalURL(new URL(url, "bin")).getFile();
		} catch (MalformedURLException e) {
			JUnitPlugin.log(e); // TO DO abort run and inform user
		} catch (IOException e) {
			JUnitPlugin.log(e); // TO DO abort run and inform user
		}
		return classPath;
	}
}