/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Utzig <mail@jutzig.de> - [JUnit] Update test suite wizard for JUnit 4: @RunWith(Suite.class)... - https://bugs.eclipse.org/155828
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
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

public class SuiteClassesContentProvider implements IStructuredContentProvider {

	private boolean fIncludeJunit4Tests;

	public SuiteClassesContentProvider() {
		this(false);
	}
	
	public SuiteClassesContentProvider(boolean includeJunit4Tests) {
		this.fIncludeJunit4Tests = includeJunit4Tests;
	}

	public Object[] getElements(Object parent) {
		if (! (parent instanceof IPackageFragment))
			return new Object[0];
		IPackageFragment pack= (IPackageFragment) parent;
		if (! pack.exists())
			return new Object[0];
		Object[] result= getTests(pack).toArray();
		Arrays.sort(result, new Comparator() {
			private Collator fCollator= Collator.getInstance();
			public int compare(Object o1, Object o2) {
				if (o1 instanceof IType && o2 instanceof IType) {
					IType t1= (IType) o1;
					IType t2= (IType) o2;
					return fCollator.compare(t1.getElementName(), t2.getElementName());
				}
				return 0;
			}
		});
		return result;
	}

	public Set getTests(IPackageFragment pack) {
		try {
			HashSet result= new HashSet();
			
			if (isIncludeJunit4Tests()) {
				new JUnit4TestFinder().findTestsInContainer(pack, result, null);
			} else {
				new JUnit3TestFinder().findTestsInContainer(pack, result, null);
			}
			
			return result;
		} catch (CoreException e) {
			JUnitPlugin.log(e);
			return Collections.EMPTY_SET;
		}
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	
	public void setIncludeJunit4Tests(boolean includeJunit4Tests) {
		fIncludeJunit4Tests= includeJunit4Tests;
	}
	
	public boolean isIncludeJunit4Tests() {
		return fIncludeJunit4Tests;
	}
}
