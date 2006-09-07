/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

import org.osgi.framework.Bundle;

/**
 * Launch configuration delegate for a plain JUnit test.
 */
public class JUnitLaunchConfiguration extends JUnitBaseLaunchConfiguration  {

	public static final String ID_JUNIT_APPLICATION= "org.eclipse.jdt.junit.launchconfig"; //$NON-NLS-1$
	/**
	 * Add a VMRunner with a class path that includes org.eclipse.jdt.junit plugin.
	 * In addition it adds the port for the RemoteTestRunner as an argument
	 */
	protected VMRunnerConfiguration createVMRunner(ILaunchConfiguration configuration, TestSearchResult testTypes, int port, String runMode) throws CoreException {
		String[] classPath = createClassPath(configuration, testTypes.getTestKind());
		VMRunnerConfiguration vmConfig= new VMRunnerConfiguration("org.eclipse.jdt.internal.junit.runner.RemoteTestRunner", classPath); //$NON-NLS-1$
		String[] args= getVMArgs(configuration, testTypes, port, runMode);
		vmConfig.setProgramArguments(args);
		return vmConfig;
	}

	private String[] getVMArgs(ILaunchConfiguration configuration, TestSearchResult result, int port, String runMode) throws CoreException {
		String progArgs= getProgramArguments(configuration);
		String testName= configuration.getAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, ""); //$NON-NLS-1$
		String testFailureNames= configuration.getAttribute(JUnitBaseLaunchConfiguration.FAILURES_FILENAME_ATTR, ""); //$NON-NLS-1$
		
		// insert the program arguments
		ArrayList argv= new ArrayList(10);
		ExecutionArguments execArgs = new ExecutionArguments("", progArgs); //$NON-NLS-1$
		String[] pa= execArgs.getProgramArgumentsArray();
		for (int i= 0; i < pa.length; i++) {
			argv.add(pa[i]);
		}
	
		argv.addAll(getBasicArguments(configuration, port, runMode, result));
		
		IType[] testTypes = result.getTypes();
		
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
		if (testFailureNames.length() > 0) {
			argv.add("-testfailures"); //$NON-NLS-1$
			argv.add(testFailureNames);			
		}

		return (String[]) argv.toArray(new String[argv.size()]);
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
	
	private String[] createClassPath(ILaunchConfiguration configuration, ITestKind kind) throws CoreException {
		String[] cp= getClasspath(configuration);
				
		List junitEntries = new ClasspathLocalizer(Platform.inDevelopmentMode()).localizeClasspath(kind);
				
		String[] classPath= new String[cp.length + junitEntries.size()];
		Object[] jea= junitEntries.toArray();
		System.arraycopy(cp, 0, classPath, 0, cp.length);
		System.arraycopy(jea, 0, classPath, cp.length, jea.length);
		return classPath;
	}		
}

class ClasspathLocalizer {
	
	private boolean fInDevelopmentMode;

	protected ClasspathLocalizer() {
		this(false);
	}

	public ClasspathLocalizer(boolean inDevelopmentMode) {
		fInDevelopmentMode = inDevelopmentMode;
	}

	public List localizeClasspath(ITestKind kind) {
		JUnitRuntimeClasspathEntry[] entries= kind.getClasspathEntries();
		List junitEntries= new ArrayList();
		
		for (int i= 0; i < entries.length; i++) {
			try {
				addEntry(junitEntries, entries[i]);
			} catch (IOException e) {
				Assert.isTrue(false, entries[i].getPluginId() + " is available (required JAR)"); //$NON-NLS-1$
			}
		}
		return junitEntries;
	}

	private void addEntry(List junitEntries, final JUnitRuntimeClasspathEntry entry) throws IOException, MalformedURLException {
		String entryString= entryString(entry);
		if (entryString != null)
			junitEntries.add(entryString);
	}

	private String entryString(final JUnitRuntimeClasspathEntry entry) throws IOException, MalformedURLException {
		if (inDevelopmentMode()) {
			try {
				return localURL(entry.developmentModeEntry());
			} catch (IOException e3) {
				// fall through and try default
			}
		}
		return localURL(entry);
	}

	private boolean inDevelopmentMode() {
		return fInDevelopmentMode;
	}
	
	private String localURL(JUnitRuntimeClasspathEntry jar) throws IOException, MalformedURLException {
		Bundle bundle= JUnitPlugin.getDefault().getBundle(jar.getPluginId());
		URL url;
		if (jar.getPluginRelativePath() == null)
			url= bundle.getEntry("/"); //$NON-NLS-1$
		else
			url= bundle.getEntry(jar.getPluginRelativePath());
		if (url == null)
			throw new IOException();
		return FileLocator.toFileURL(url).getFile();
	}
}

