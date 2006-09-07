/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.junit.launcher.JUnit3TestFinder;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

public class SuiteClassesContentProvider implements IStructuredContentProvider {
		
	public Object[] getElements(Object parent) {
		if (! (parent instanceof IPackageFragment))
			return new Object[0];
		IPackageFragment pack= (IPackageFragment) parent;
		if (! pack.exists())
			return new Object[0];
		try {
			HashSet result= new HashSet();
			new JUnit3TestFinder().findTestsInContainer(pack, result, null);
			return result.toArray();
		} catch (CoreException e) {
			JUnitPlugin.log(e);
			return new Object[0];
		}
	}
	
	public void dispose() {
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
