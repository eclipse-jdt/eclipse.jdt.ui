/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.core.IType;
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
		String[] classPath = LauncherUtil.createClassPath(testTypes[0]);	
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
}