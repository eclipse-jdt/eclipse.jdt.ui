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

package org.eclipse.jdt.internal.corext.refactoring.genericize;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

public class GenericContainers {
	
	public static final String JAVA_UTIL_MAP= "java.util.Map"; //$NON-NLS-1$
	public static final String JAVA_UTIL_COLLECTION= "java.util.Collection"; //$NON-NLS-1$
	
	private final IJavaProject fProject;
	
	private IType fCollectionType;
	private IType fMapType;

	public static GenericContainers create(IJavaProject project, IProgressMonitor pm) throws JavaModelException {
		return new GenericContainers(project, pm);
	}

	private GenericContainers(IJavaProject project, IProgressMonitor pm) throws JavaModelException {
		fProject= project;
		fCollectionType= fProject.findType(JAVA_UTIL_COLLECTION);
		fMapType= fProject.findType(JAVA_UTIL_MAP);
//		fJavaLangObject= fProject.findType("java.lang.Object"); //$NON-NLS-1$
//		fCloneableType= fProject.findType("java.lang.Cloneable"); //$NON-NLS-1$
//		fSerializableType= fProject.findType("java.io.Serializable"); //$NON-NLS-1$
		
		ITypeHierarchy collectionHierarchy= fCollectionType.newTypeHierarchy(pm);
	}
	
	


}
