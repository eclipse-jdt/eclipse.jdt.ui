/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;

import sun.security.krb5.internal.i;
import sun.security.krb5.internal.crypto.e;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;


 /**
 * A launcher for running JUnit Test classes. 
 * Uses JDI to launch a vm in debug mode.
 */
public class Launcher extends BaseLauncher {

	/**
	 * @see BaseLauncher#configureVM(IType[], int)
	 */
	public VMRunnerConfiguration configureVM(IType[] testTypes, int port) throws InvocationTargetException {
		String[] classPath= createClassPath(testTypes[0]);	
		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration(TestRunner.class.getName(), classPath);
	
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
	
	protected static String[] createClassPath(IType type) throws InvocationTargetException {
		URL url= JUnitPlugin.getDefault().getDescriptor().getInstallURL();
		try {
			String[] cp = JavaRuntime.computeDefaultRuntimeClassPath(type.getJavaProject());
			String[] classPath= new String[cp.length + 2];
			System.arraycopy(cp, 0, classPath, 2, cp.length);
			classPath[0]= Platform.asLocalURL(new URL(url, "junitsupport.jar")).getFile();
			classPath[1]= Platform.asLocalURL(new URL(url, "bin")).getFile();
			return classPath;
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

}