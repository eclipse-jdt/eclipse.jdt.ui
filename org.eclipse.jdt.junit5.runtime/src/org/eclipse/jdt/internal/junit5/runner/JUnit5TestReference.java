/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit5.runner;

import java.util.Set;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.eclipse.jdt.internal.junit.runner.TestExecution;
import org.eclipse.jdt.internal.junit.runner.TestIdMap;

public class JUnit5TestReference implements ITestReference {

	private LauncherDiscoveryRequest fRequest;

	private Launcher fLauncher;

	private TestPlan fTestPlan;

	private RemoteTestRunner fRemoteTestRunner;

	public JUnit5TestReference(LauncherDiscoveryRequest request, Launcher launcher, RemoteTestRunner remoteTestRunner) {
		fRequest= request;
		fLauncher= launcher;
		fRemoteTestRunner= remoteTestRunner;
		fTestPlan= fLauncher.discover(fRequest);
	}

	@Override
	public int countTestCases() {
		return (int) fTestPlan.countTestIdentifiers(TestIdentifier::isTest);
	}

	@Override
	public void sendTree(IVisitsTestTrees notified) {
		for (TestIdentifier root : fTestPlan.getRoots()) {
			for (TestIdentifier child : fTestPlan.getChildren(root)) {
				sendTree(notified, child);
			}
		}
	}

	private void sendTree(IVisitsTestTrees notified, TestIdentifier testIdentifier) {
		JUnit5Identifier identifier= new JUnit5Identifier(testIdentifier);
		String parentId= getParentId(testIdentifier, fTestPlan);
		if (testIdentifier.isTest()) {
			notified.visitTreeEntry(identifier, false, 1, false, parentId);
		} else {
			Set<TestIdentifier> children= fTestPlan.getChildren(testIdentifier);
			notified.visitTreeEntry(identifier, true, children.size(), false, parentId);
			for (TestIdentifier child : children) {
				sendTree(notified, child);
			}
		}
	}

	/**
	 * @param testIdentifier the test identifier whose parent id is required
	 * @param testPlan the test plan containing the test
	 * @return the parent id from {@link TestIdMap} if the parent is present, otherwise
	 *         <code>"-1"</code>
	 */
	private String getParentId(TestIdentifier testIdentifier, TestPlan testPlan) {
		// Same as JUnit5TestListener.getParentId(TestIdentifier testIdentifier, TestPlan testPlan)
		return testPlan.getParent(testIdentifier).map(parent -> fRemoteTestRunner.getTestId(new JUnit5Identifier(parent))).orElse("-1"); //$NON-NLS-1$
	}

	@Override
	public void run(TestExecution execution) {
		boolean foundMethodThatAvoidsRedundantDiscovery;
		try {
			fLauncher.getClass().getMethod("execute", TestPlan.class, TestExecutionListener[].class); //$NON-NLS-1$
			foundMethodThatAvoidsRedundantDiscovery= true;
		} catch (NoSuchMethodException e) {
			foundMethodThatAvoidsRedundantDiscovery= false;
		}
		if (foundMethodThatAvoidsRedundantDiscovery) {
			fLauncher.execute(fTestPlan, new JUnit5TestListener(execution.getListener(), fRemoteTestRunner));
		} else {
			fLauncher.execute(fRequest, new JUnit5TestListener(execution.getListener(), fRemoteTestRunner));
		}
	}

	@Override
	public ITestIdentifier getIdentifier() { // not used
		return new JUnit5Identifier(fTestPlan.getRoots().iterator().next());
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JUnit5TestReference))
			return false;

		JUnit5TestReference ref= (JUnit5TestReference) obj;
		return (ref.fRequest.equals(fRequest));
	}

	@Override
	public int hashCode() {
		return fRequest.hashCode();
	}

	@Override
	public String toString() {
		return fRequest.toString();
	}

}
