/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit5.runner;

import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import org.eclipse.jdt.internal.junit.runner.ITestLoader;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

public class JUnit5TestLoader implements ITestLoader {

	private Launcher fLauncher= LauncherFactory.create();

	@Override
	public ITestReference[] loadTests(Class[] testClasses, String testName, String[] failureNames, String[] packages, String uniqueId, RemoteTestRunner listener) {
		ITestReference[] refs= new ITestReference[0];
		if (uniqueId != null && !uniqueId.trim().isEmpty()) {
			refs= new ITestReference[1];
			refs[0]= createUniqueIdTest(uniqueId);
		} else if (packages != null) {
			refs= new ITestReference[packages.length];
			for (int i= 0; i < packages.length; i++) {
				refs[i]= createTest(packages[i]);
			}
		} else {
			refs= new ITestReference[testClasses.length];
			for (int i= 0; i < testClasses.length; i++) {
				refs[i]= createTest(testClasses[i], testName);
			}
		}
		return refs;
	}

	private ITestReference createTest(Class<?> clazz, String testName) {
		if (clazz == null) {
			return null;
		}
		if (testName != null) {
			return createFilteredTest(clazz, testName);
		}
		return createUnfilteredTest(clazz);
	}

	private ITestReference createFilteredTest(Class<?> clazz, String testName) {
		LauncherDiscoveryRequest request= LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectMethod(clazz.getName() + "#" + testName)).build(); //$NON-NLS-1$
		return new JUnit5TestReference(request, fLauncher);
	}

	private ITestReference createUnfilteredTest(Class<?> clazz) {
		LauncherDiscoveryRequest request= LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(clazz)).build();
		return new JUnit5TestReference(request, fLauncher);
	}

	private ITestReference createTest(String pkg) {
		if (pkg == null) {
			return null;
		}
		String pattern;
		if (pkg.equals("<default>")) { //$NON-NLS-1$
			pkg= ""; //$NON-NLS-1$
			pattern= "^[^.]+$"; //$NON-NLS-1$
		} else {
			pattern= "^" + pkg + "\\.[^.]+$"; //$NON-NLS-1$//$NON-NLS-2$
		}
		LauncherDiscoveryRequest request= LauncherDiscoveryRequestBuilder.request()
				.selectors(DiscoverySelectors.selectPackage(pkg))
				.filters(ClassNameFilter.includeClassNamePatterns(pattern))
				.build();

		return new JUnit5TestReference(request, fLauncher);
	}

	private ITestReference createUniqueIdTest(String uniqueId) {
		LauncherDiscoveryRequest request= LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectUniqueId(uniqueId)).build();
		return new JUnit5TestReference(request, fLauncher);
	}
}
