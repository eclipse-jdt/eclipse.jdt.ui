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
package org.eclipse.jdt.internal.junit.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * Custom Search engine for suite() methods
 */
public class TestSearchEngine {

	private class JUnitSearchResultCollector extends SearchRequestor {
		List fList;
		Set fFailed= new HashSet();
		Set fMatches= new HashSet();
		
		public JUnitSearchResultCollector(List list) {
			fList= list;
		}
		
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			Object enclosingElement= match.getElement();
			if (!(enclosingElement instanceof IMethod)) 
				return;
			
			IMethod method= (IMethod)enclosingElement;		
			
			IType declaringType= method.getDeclaringType();
			if (fMatches.contains(declaringType) || fFailed.contains(declaringType))
				return;
			if (!hasSuiteMethod(declaringType) && !isTestType(declaringType)) {
				fFailed.add(declaringType);
				return;
			}
			fMatches.add(declaringType);
		}
		
		public void endReporting() {
			fList.addAll(fMatches);
		}
	}
	
	private List searchMethod(IProgressMonitor pm, final IJavaSearchScope scope) throws CoreException {
		final List typesFound= new ArrayList(200);	
		searchMethod(typesFound, scope, pm);
		return typesFound;	
	}

	private List searchMethod(final List v, IJavaSearchScope scope, final IProgressMonitor progressMonitor) throws CoreException {		
		SearchRequestor requestor= new JUnitSearchResultCollector(v);
		SearchPattern suitePattern= SearchPattern.createPattern("suite() Test", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE); //$NON-NLS-1$
		SearchPattern testPattern= SearchPattern.createPattern("test*() void", IJavaSearchConstants.METHOD , IJavaSearchConstants.DECLARATIONS, SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE); //$NON-NLS-1$
		SearchPattern pattern= SearchPattern.createOrPattern(suitePattern, testPattern);
		SearchParticipant[] participants= new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		new SearchEngine().search(pattern, participants, scope, requestor, progressMonitor); 
		return v;
	}
	
	public static IType[] findTests(IRunnableContext context, final Object[] elements) throws InvocationTargetException, InterruptedException {
		final Set result= new HashSet();
		
			if (elements.length > 0) {
				IRunnableWithProgress runnable= new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) throws InterruptedException {
						doFindTests(elements, result, pm);
					}
				};
				context.run(true, true, runnable);			
			}
			return (IType[]) result.toArray(new IType[result.size()]) ;
	}

	public static IType[] findTests(final Object[] elements) throws InvocationTargetException, InterruptedException{
		final Set result= new HashSet();
	
		if (elements.length > 0) {
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InterruptedException {
					doFindTests(elements, result, pm);
				}
			};
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);			
		}
		return (IType[]) result.toArray(new IType[result.size()]) ;
	}

	public static void doFindTests(Object[] elements, Set result, IProgressMonitor pm) throws InterruptedException {
		int nElements= elements.length;
		pm.beginTask(JUnitMessages.getString("TestSearchEngine.message.searching"), nElements);  //$NON-NLS-1$
		try {
			for (int i= 0; i < nElements; i++) {
				try {
					collectTypes(elements[i], new SubProgressMonitor(pm, 1), result);
				} catch (CoreException e) {
					JUnitPlugin.log(e.getStatus());
				}
				if (pm.isCanceled()) {
					throw new InterruptedException();
				}
			}
		} finally {
			pm.done();
		}
	}

	private static void collectTypes(Object element, IProgressMonitor pm, Set result) throws CoreException/*, InvocationTargetException*/ {
		element= computeScope(element);
		while((element instanceof IJavaElement) && !(element instanceof ICompilationUnit) && (element instanceof ISourceReference)) {
			if(element instanceof IType) {
				if (hasSuiteMethod((IType)element) || isTestType((IType)element)) {
					result.add(element);
					return;
				}
			}
			element= ((IJavaElement)element).getParent();
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)element;
			IType[] types= cu.getAllTypes();
			for (int i= 0; i < types.length; i++) {
				if (hasSuiteMethod(types[i])  || isTestType(types[i]))
					result.add(types[i]);
			}
		} 
		else if (element instanceof IJavaElement) {
			List found= searchSuiteMethods(pm, (IJavaElement)element);
			result.addAll(found);
		}
	}

	private static Object computeScope(Object element) throws JavaModelException {
		if (element instanceof IFileEditorInput)
			element= ((IFileEditorInput)element).getFile();
		if (element instanceof IResource)
			element= JavaCore.create((IResource)element);
		if (element instanceof IClassFile) {
			IClassFile cf= (IClassFile)element;
			element= cf.getType();
		}
		return element;
	}
	
	private static List searchSuiteMethods(IProgressMonitor pm, IJavaElement element) throws CoreException {	
		// fix for bug 36449  JUnit should constrain tests to selected project [JUnit] 
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { element },
				IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES);
		TestSearchEngine searchEngine= new TestSearchEngine(); 
		return searchEngine.searchMethod(pm, scope);
	}
		
	public static boolean hasSuiteMethod(IType type) throws JavaModelException {
		IMethod method= type.getMethod("suite", new String[0]); //$NON-NLS-1$
		if (method == null || !method.exists()) 
			return false;
		
		if (!Flags.isStatic(method.getFlags()) ||	
			!Flags.isPublic(method.getFlags()) ||			
			!Flags.isPublic(method.getDeclaringType().getFlags())) { 
			return false;
		}
		if (!Signature.getSimpleName(Signature.toString(method.getReturnType())).equals(JUnitPlugin.SIMPLE_TEST_INTERFACE_NAME)) {
			return false;
		}
		return true;
	}
	
	private static boolean isTestType(IType type) throws JavaModelException {
		if (Flags.isAbstract(type.getFlags())) 
			return false;
		if (!Flags.isPublic(type.getFlags())) 
			return false;
		
		IType[] interfaces= type.newSupertypeHierarchy(null).getAllSuperInterfaces(type);
		for (int i= 0; i < interfaces.length; i++)
			if(interfaces[i].getFullyQualifiedName().equals(JUnitPlugin.TEST_INTERFACE_NAME))
				return true;
		return false;
	}

	public static boolean isTestImplementor(IType type) throws JavaModelException {
		ITypeHierarchy typeHier= type.newSupertypeHierarchy(null);
		IType[] superInterfaces= typeHier.getAllInterfaces();
		for (int i= 0; i < superInterfaces.length; i++) {
			if (superInterfaces[i].getFullyQualifiedName().equals(JUnitPlugin.TEST_INTERFACE_NAME))
				return true;
		}
		return false;
	}

	public static boolean isTestOrTestSuite(IType type) throws JavaModelException {
		return hasSuiteMethod(type) || isTestType(type);
	}

}
