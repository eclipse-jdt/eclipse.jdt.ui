/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Johannes Utzig <mail@jutzig.de> - [JUnit] Update test suite wizard for JUnit 4: @RunWith(Suite.class)... - https://bugs.eclipse.org/155828
 *     Red Hat Inc. - Update test suite wizard for JUnit5 (@Suite support)
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.launcher.JUnit3TestFinder;
import org.eclipse.jdt.internal.junit.launcher.JUnit4TestFinder;
import org.eclipse.jdt.internal.junit.launcher.JUnit5TestFinder;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

public class SuiteClassesContentProvider implements IStructuredContentProvider {

	private boolean fIncludeJunit4Tests;
	private boolean fIncludeJunit5Tests;

	public SuiteClassesContentProvider() {
		this(false, false);
	}

	public SuiteClassesContentProvider(boolean includeJunit4Tests, boolean includeJunit5Tests) {
		this.fIncludeJunit4Tests= includeJunit4Tests;
		this.fIncludeJunit5Tests= includeJunit5Tests;
	}

	@Override
	public Object[] getElements(Object parent) {
		if (! (parent instanceof IPackageFragment))
			return new Object[0];
		IPackageFragment pack= (IPackageFragment) parent;
		if (! pack.exists())
			return new Object[0];
		Set<IType> tests= getTests(pack);
		IType[] result= tests.toArray(new IType[tests.size()]);
		Arrays.sort(result, new Comparator<IType>() {
			private Collator fCollator= Collator.getInstance();
			@Override
			public int compare(IType t1, IType t2) {
				return fCollator.compare(t1.getElementName(), t2.getElementName());
			}
		});
		return result;
	}

	public Set<IType> getTests(IPackageFragment pack) {
		try {
			HashSet<IType> result= new HashSet<>();
			if (isIncludeJunit5Tests()) {
				new JUnit5TestFinder().findTestsInContainer(pack, result, null);
			} else if (isIncludeJunit4Tests()) {
				new JUnit4TestFinder().findTestsInContainer(pack, result, null);
			} else {
				new JUnit3TestFinder().findTestsInContainer(pack, result, null);
			}
			return result;
		} catch (CoreException e) {
			JUnitPlugin.log(e);
			return Collections.emptySet();
		}
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	public void setIncludeJunit4Tests(boolean includeJunit4Tests) {
		fIncludeJunit4Tests= includeJunit4Tests;
	}

	public boolean isIncludeJunit4Tests() {
		return fIncludeJunit4Tests;
	}

	public void setIncludeJunit5Tests(boolean includeJunit5Tests) {
		fIncludeJunit5Tests= includeJunit5Tests;
	}

	public boolean isIncludeJunit5Tests() {
		return fIncludeJunit5Tests;
	}
}
