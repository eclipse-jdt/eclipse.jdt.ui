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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

public class SuiteClassesContentProvider implements IStructuredContentProvider {
		
	public Object[] getElements(Object parent) {
		if (! (parent instanceof IPackageFragment))
			return new Object[0];
		IPackageFragment pack= (IPackageFragment) parent;
		if (! pack.exists())
			return new Object[0];
		try {
			ICompilationUnit[] cuArray= pack.getCompilationUnits();
			List typesArrayList= new ArrayList();
			for (int i= 0; i < cuArray.length; i++) {
				ICompilationUnit cu= cuArray[i];
				IType[] types= cu.getTypes();
				for (int j= 0; j < types.length; j++) {
					IType type= types[j];
					if (type.isClass() && ! Flags.isAbstract(type.getFlags()) && TestSearchEngine.isTestImplementor(type))	
						typesArrayList.add(types[j]);
				}
			}
			return typesArrayList.toArray();
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
			return new Object[0];
		}
	}
	
	public void dispose() {
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
