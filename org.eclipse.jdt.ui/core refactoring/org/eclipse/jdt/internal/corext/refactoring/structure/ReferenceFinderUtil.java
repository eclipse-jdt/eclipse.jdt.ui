/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;

public class ReferenceFinderUtil {
	
	//no instances
	private ReferenceFinderUtil(){
	}

	//----- referenced types -
	
	public static IType[] getTypesReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchResult[] results= getTypeReferencesIn(elements, pm);
		Set referencedTypes= extractElements(results, IJavaElement.TYPE);
		return (IType[]) referencedTypes.toArray(new IType[referencedTypes.size()]);	
	}
	
	private static SearchResult[] getTypeReferencesIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedTypes= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedTypes.addAll(getTypeReferencesIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchResult[]) referencedTypes.toArray(new SearchResult[referencedTypes.size()]);
	}
	
	private static List getTypeReferencesIn(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfReferencedTypes(ResourcesPlugin.getWorkspace(), element, collector);
		return collector.getResults();
	}
	
	//----- referenced fields ----
	
	public static IField[] getFieldsReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchResult[] results= getFieldReferencesIn(elements, pm);
		Set referencedFields= extractElements(results, IJavaElement.FIELD);
		return (IField[]) referencedFields.toArray(new IField[referencedFields.size()]);
	}

	private static SearchResult[] getFieldReferencesIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getFieldReferencesIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchResult[]) referencedFields.toArray(new SearchResult[referencedFields.size()]);
	}
	
	private static List getFieldReferencesIn(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfAccessedFields(ResourcesPlugin.getWorkspace(), element, collector);
		return collector.getResults();
	}
	
	//----- referenced methods ----
	
	public static IMethod[] getMethodsReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchResult[] results= getMethodReferencesIn(elements, pm);
		Set referencedMethods= extractElements(results, IJavaElement.METHOD);
		return (IMethod[]) referencedMethods.toArray(new IMethod[referencedMethods.size()]);
	}
	
	private static SearchResult[] getMethodReferencesIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedMethods= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedMethods.addAll(getMethodReferencesIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchResult[]) referencedMethods.toArray(new SearchResult[referencedMethods.size()]);
	}
	
	private static List getMethodReferencesIn(IJavaElement element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfSentMessages(ResourcesPlugin.getWorkspace(), element, collector);
		return collector.getResults();
	}
	
	public static ITypeBinding[] getTypesReferencedInDeclarations(MethodDeclaration[] methods) throws JavaModelException{
		Set typesUsed= new HashSet();
		for (int i= 0; i < methods.length; i++) {
			typesUsed.addAll(getTypesUsedInDeclaration(methods[i]));
		}
		return (ITypeBinding[]) typesUsed.toArray(new ITypeBinding[typesUsed.size()]);
	}
		
	//set of ITypeBindings
	public static Set getTypesUsedInDeclaration(MethodDeclaration methodDeclaration) throws JavaModelException {
		if (methodDeclaration == null)
			return new HashSet(0);
		Set result= new HashSet();	
		result.add(methodDeclaration.getReturnType().resolveBinding());
				
		for (Iterator iter= methodDeclaration.parameters().iterator(); iter.hasNext();) {
			result.add(((SingleVariableDeclaration) iter.next()).getType().resolveBinding()); 
		}
			
		for (Iterator iter= methodDeclaration.thrownExceptions().iterator(); iter.hasNext();) {
			result.add(((Name) iter.next()).resolveTypeBinding());
		}
		return result;
	}
		
	/// private helpers 	
	private static Set extractElements(SearchResult[] searchResults, int elementType){
		Set elements= new HashSet();
		for (int i= 0; i < searchResults.length; i++) {
			IJavaElement el= searchResults[i].getEnclosingElement();
			if (el.exists() && el.getElementType() == elementType)
				elements.add(el);
		}
		return elements;
	}	
}
