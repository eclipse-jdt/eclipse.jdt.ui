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

package org.eclipse.jdt.internal.corext.refactoring.generics;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class GenericContainers {
	/*
	 * TODO: models for container categories:
	 * - array
	 * - map
	 * - collection, iterator, enumeration
	 */
	
	public static final String JAVA_UTIL_MAP= "java.util.Map"; //$NON-NLS-1$
	public static final String JAVA_UTIL_COLLECTION= "java.util.Collection"; //$NON-NLS-1$
	public static final String JAVA_UTIL_ITERATOR= "java.util.Iterator"; //$NON-NLS-1$
	public static final String JAVA_UTIL_ENUMERATION= "java.util.Enumeration"; //$NON-NLS-1$
	
	private final IJavaProject fProject;
	
	private IType fCollectionType;
	private IType fMapType;
	private IType fIteratorType;
	private IType fEnumerationType;
	private IType[] fAllCollectionTypes;
	private IType[] fAllMapTypes;
	private IType[] fAllIteratorTypes;
	private IType[] fAllEnumerationTypes;

	public static GenericContainers create(IJavaProject project, IProgressMonitor pm) throws JavaModelException {
		return new GenericContainers(project, pm);
	}

	private GenericContainers(IJavaProject project, IProgressMonitor pm) throws JavaModelException {
		fProject= project;
		fCollectionType= fProject.findType(JAVA_UTIL_COLLECTION);
		fMapType= fProject.findType(JAVA_UTIL_MAP);
		fIteratorType= fProject.findType(JAVA_UTIL_ITERATOR);
		fEnumerationType= fProject.findType(JAVA_UTIL_ENUMERATION);
//		fJavaLangObject= fProject.findType("java.lang.Object"); //$NON-NLS-1$
//		fCloneableType= fProject.findType("java.lang.Cloneable"); //$NON-NLS-1$
//		fSerializableType= fProject.findType("java.io.Serializable"); //$NON-NLS-1$
		
		fAllCollectionTypes= fCollectionType.newTypeHierarchy(pm).getAllSubtypes(fCollectionType);
		fAllMapTypes= fMapType.newTypeHierarchy(pm).getAllSubtypes(fMapType);
		fAllIteratorTypes= fIteratorType.newTypeHierarchy(pm).getAllSubtypes(fIteratorType);
		fAllEnumerationTypes= fEnumerationType.newTypeHierarchy(pm).getAllSubtypes(fEnumerationType);
		//TODO: Prune subtypes with instantiated type parameters?
		// Or just create more constraining constraints for them?
		//TODO: java.util.Iterable for JRE >= 1.5?
	}

	public IType[] getContainerTypes() {
		ArrayList containerTypes= new ArrayList();
		containerTypes.addAll(Arrays.asList(fAllCollectionTypes));
		containerTypes.addAll(Arrays.asList(fAllMapTypes));
		containerTypes.addAll(Arrays.asList(fAllIteratorTypes));
		containerTypes.addAll(Arrays.asList(fAllEnumerationTypes));
		return (IType[]) containerTypes.toArray(new IType[containerTypes.size()]);
	}
	
	


}
