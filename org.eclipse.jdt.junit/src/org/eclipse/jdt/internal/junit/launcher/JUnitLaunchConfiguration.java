/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jface.util.Assert;

/**
 * Launch configuration delegate for a plain JUnit test.
 */
public class JUnitLaunchConfiguration extends JUnitBaseLaunchConfiguration  {

	public static final String ID_JUNIT_APPLICATION= "org.eclipse.jdt.junit.launchconfig"; //$NON-NLS-1$
	/**
	 * Add a VMRunner with a class path that includes org.eclipse.jdt.junit plugin.
	 * In addition it adds the port for the RemoteTestRunner as an argument
	 */
	protected VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration, IType[] testTypes, int port, String runMode) throws CoreException {
		String[] classPath= createClassPath(configuration);	
		String progArgs= getProgramArguments(configuration);
		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("org.eclipse.jdt.internal.junit.runner.RemoteTestRunner", classPath); //$NON-NLS-1$
		String testName= configuration.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, ""); //$NON-NLS-1$
		
		// insert the program arguments
		Vector argv= new Vector(10);
		ExecutionArguments execArgs = new ExecutionArguments("", progArgs); //$NON-NLS-1$
		String[] pa= execArgs.getProgramArgumentsArray();
		for (int i= 0; i < pa.length; i++) {
			argv.add(pa[i]);
		}
	
		argv.add("-version"); //$NON-NLS-1$
		argv.add("3"); //$NON-NLS-1$
		
		argv.add("-port"); //$NON-NLS-1$
		argv.add(Integer.toString(port));
		//argv("-debugging");
				
		if (keepAlive(configuration) && runMode.equals(ILaunchManager.DEBUG_MODE))
			argv.add(0, "-keepalive"); //$NON-NLS-1$
		
		// a testname was specified just run the single test
		if (testName.length() > 0) {
			argv.add("-test"); //$NON-NLS-1$
			argv.add(testTypes[0].getFullyQualifiedName()+":"+testName);			 //$NON-NLS-1$
		} else if (testTypes.length > 1) {
			String fileName= createTestNamesFile(testTypes);
			argv.add("-testNameFile"); //$NON-NLS-1$
			argv.add(fileName);
		} else {
			argv.add("-classNames"); //$NON-NLS-1$
			for (int i= 0; i < testTypes.length; i++) 
				argv.add(testTypes[i].getFullyQualifiedName());
		}
		String[] args= new String[argv.size()];
		argv.copyInto(args);
		vmConfig.setProgramArguments(args);
		return vmConfig;
	}

	private String createTestNamesFile(IType[] testTypes) throws CoreException {
		try {
			File file= File.createTempFile("testNames", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			BufferedWriter bw= null;
			try {
				bw= new BufferedWriter(new FileWriter(file));
				for (int i= 0; i < testTypes.length; i++) {
					String testName= testTypes[i].getFullyQualifiedName();
					bw.write(testName);
					bw.newLine();
				}
			} finally {
				if (bw != null) {
					bw.close();
				}
			}
			return file.getAbsolutePath();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, JUnitPlugin.PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}
	
	private String[] createClassPath(ILaunchConfiguration configuration) throws CoreException {
		URL runtimeURL= Platform.getBundle("org.eclipse.jdt.junit.runtime").getEntry("/"); //$NON-NLS-1$ //$NON-NLS-2$
		URL url= Platform.getBundle(JUnitPlugin.PLUGIN_ID).getEntry("/"); //$NON-NLS-1$
		
		String[] cp= getClasspath(configuration);
		String[] classPath= null;
		
		try {
			if (BootLoader.inDevelopmentMode()) {
				// we first try the bin output folder
				List junitEntries= new ArrayList();
				
				try {
					junitEntries.add(Platform.asLocalURL(new URL(url, "bin")).getFile()); //$NON-NLS-1$
				} catch (IOException e3) {
					try {
						junitEntries.add(Platform.asLocalURL(new URL(url, "junitsupport.jar")).getFile()); //$NON-NLS-1$
					} catch (IOException e4) {
						// fall through
					}
				}
				try {
					junitEntries.add(Platform.asLocalURL(new URL(runtimeURL, "bin")).getFile()); //$NON-NLS-1$
				} catch (IOException e1) {
					try {
						junitEntries.add(Platform.asLocalURL(new URL(runtimeURL, "junitruntime.jar")).getFile()); //$NON-NLS-1$
					} catch (IOException e4) {
						// fall through
					}
				}
				Assert.isTrue(junitEntries.size() == 2, "Required JARs available"); //$NON-NLS-1$
				
				classPath= new String[cp.length + junitEntries.size()];
				Object[] jea= junitEntries.toArray();
				System.arraycopy(cp, 0, classPath, 0, cp.length);
				System.arraycopy(jea, 0, classPath, cp.length, jea.length);
			} else {
				classPath= new String[cp.length + 2];
				System.arraycopy(cp, 0, classPath, 2, cp.length);
				classPath[0]= Platform.asLocalURL(new URL(url, "junitsupport.jar")).getFile(); //$NON-NLS-1$
				classPath[1]= Platform.asLocalURL(new URL(runtimeURL, "junitruntime.jar")).getFile(); //$NON-NLS-1$
			}
		} catch (IOException e) {
			JUnitPlugin.log(e); // TODO abort run and inform user
		}
		return classPath;
	}		
}
