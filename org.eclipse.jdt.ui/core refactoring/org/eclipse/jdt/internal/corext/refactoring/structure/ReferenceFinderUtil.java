/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

public class ReferenceFinderUtil {

	private ReferenceFinderUtil(){
		//no instances
	}

	//----- referenced types -

	public static IType[] getTypesReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchMatch[] results= getTypeReferencesIn(elements, null, pm);
		Set referencedTypes= extractElements(results, IJavaElement.TYPE);
		return (IType[]) referencedTypes.toArray(new IType[referencedTypes.size()]);
	}

	public static IType[] getTypesReferencedIn(IJavaElement[] elements, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		SearchMatch[] results= getTypeReferencesIn(elements, owner, pm);
		Set referencedTypes= extractElements(results, IJavaElement.TYPE);
		return (IType[]) referencedTypes.toArray(new IType[referencedTypes.size()]);
	}

	private static SearchMatch[] getTypeReferencesIn(IJavaElement[] elements, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		List referencedTypes= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedTypes.addAll(getTypeReferencesIn(elements[i], owner, new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchMatch[]) referencedTypes.toArray(new SearchMatch[referencedTypes.size()]);
	}

	private static List getTypeReferencesIn(IJavaElement element, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		CollectingSearchRequestor requestor= new CollectingSearchRequestor();
		SearchEngine engine= owner != null ? new SearchEngine(owner) : new SearchEngine();
		engine.searchDeclarationsOfReferencedTypes(element, requestor, pm);
		return requestor.getResults();
	}

	//----- referenced fields ----

	public static IField[] getFieldsReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchMatch[] results= getFieldReferencesIn(elements, null, pm);
		Set referencedFields= extractElements(results, IJavaElement.FIELD);
		return (IField[]) referencedFields.toArray(new IField[referencedFields.size()]);
	}

	public static IField[] getFieldsReferencedIn(IJavaElement[] elements, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		SearchMatch[] results= getFieldReferencesIn(elements, owner, pm);
		Set referencedFields= extractElements(results, IJavaElement.FIELD);
		return (IField[]) referencedFields.toArray(new IField[referencedFields.size()]);
	}

	private static SearchMatch[] getFieldReferencesIn(IJavaElement[] elements, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getFieldReferencesIn(elements[i], owner, new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchMatch[]) referencedFields.toArray(new SearchMatch[referencedFields.size()]);
	}

	private static List getFieldReferencesIn(IJavaElement element, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		CollectingSearchRequestor requestor= new CollectingSearchRequestor();
		SearchEngine engine= owner != null ? new SearchEngine(owner) : new SearchEngine();
		engine.searchDeclarationsOfAccessedFields(element, requestor, pm);
		return requestor.getResults();
	}

	//----- referenced methods ----

	public static IMethod[] getMethodsReferencedIn(IJavaElement[] elements, IProgressMonitor pm) throws JavaModelException {
		SearchMatch[] results= getMethodReferencesIn(elements, null, pm);
		Set referencedMethods= extractElements(results, IJavaElement.METHOD);
		return (IMethod[]) referencedMethods.toArray(new IMethod[referencedMethods.size()]);
	}

	public static IMethod[] getMethodsReferencedIn(IJavaElement[] elements, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		SearchMatch[] results= getMethodReferencesIn(elements, owner, pm);
		Set referencedMethods= extractElements(results, IJavaElement.METHOD);
		return (IMethod[]) referencedMethods.toArray(new IMethod[referencedMethods.size()]);
	}

	private static SearchMatch[] getMethodReferencesIn(IJavaElement[] elements, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		List referencedMethods= new ArrayList();
		pm.beginTask("", elements.length); //$NON-NLS-1$
		for (int i = 0; i < elements.length; i++) {
			referencedMethods.addAll(getMethodReferencesIn(elements[i], owner, new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (SearchMatch[]) referencedMethods.toArray(new SearchMatch[referencedMethods.size()]);
	}

	private static List getMethodReferencesIn(IJavaElement element, WorkingCopyOwner owner, IProgressMonitor pm) throws JavaModelException {
		CollectingSearchRequestor requestor= new CollectingSearchRequestor();
		SearchEngine engine= owner != null ? new SearchEngine(owner) : new SearchEngine();
		engine.searchDeclarationsOfSentMessages(element, requestor, pm);
		return requestor.getResults();
	}

	public static ITypeBinding[] getTypesReferencedInDeclarations(MethodDeclaration[] methods) {
		Set typesUsed= new HashSet();
		for (int i= 0; i < methods.length; i++) {
			typesUsed.addAll(getTypesUsedInDeclaration(methods[i]));
		}
		return (ITypeBinding[]) typesUsed.toArray(new ITypeBinding[typesUsed.size()]);
	}

	//set of ITypeBindings
	public static Set getTypesUsedInDeclaration(MethodDeclaration methodDeclaration) {
		if (methodDeclaration == null)
			return new HashSet(0);
		Set result= new HashSet();
		ITypeBinding binding= null;
		Type returnType= methodDeclaration.getReturnType2();
		if (returnType != null) {
			binding = returnType.resolveBinding();
			if (binding != null)
				result.add(binding);
		}

		for (Iterator iter= methodDeclaration.parameters().iterator(); iter.hasNext();) {
			binding = ((SingleVariableDeclaration)iter.next()).getType().resolveBinding();
			if (binding != null)
				result.add(binding);
		}

		for (Iterator iter= methodDeclaration.thrownExceptions().iterator(); iter.hasNext();) {
			binding = ((Name)iter.next()).resolveTypeBinding();
			if (binding != null)
				result.add(binding);
		}
		return result;
	}

	/// private helpers
	private static Set extractElements(SearchMatch[] searchResults, int elementType) {
		Set elements= new HashSet();
		for (int i= 0; i < searchResults.length; i++) {
			IJavaElement el= SearchUtils.getEnclosingJavaElement(searchResults[i]);
			if (el.exists() && el.getElementType() == elementType)
				elements.add(el);
		}
		return elements;
	}
}
