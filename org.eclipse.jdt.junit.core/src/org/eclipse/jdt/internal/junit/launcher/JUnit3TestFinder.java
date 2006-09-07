/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

public class JUnit3TestFinder implements ITestFinder {
	
	public void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}
		
		if (pm == null)
			pm= new NullProgressMonitor();
		
		pm.beginTask(JUnitMessages.TestSearchEngine_message_searching, 10);
		try {
			if (element instanceof IType) {
				if (isTest((IType) element)) {
					result.add(element);
				}
			} else if (element instanceof ICompilationUnit) {
				IType[] types= ((ICompilationUnit) element).getAllTypes();
				for (int i= 0; i < types.length; i++) {
					IType type= types[i];
					if (isTest(types[i])) {
						result.add(type);
					}
				}
			} else {
				findTestCases(element, result, new SubProgressMonitor(pm, 7));
				if (pm.isCanceled()) {
					return;
				}
				searchSuiteMethods(element, result, new SubProgressMonitor(pm, 3));
			}
			if (pm.isCanceled()) {
				return;
			}
		} finally {
			pm.done();
		}
	}

	private static void findTestCases(IJavaElement element, Set result, IProgressMonitor pm) throws JavaModelException {
		IJavaProject javaProject= element.getJavaProject();

		IType testCaseType= javaProject.findType(JUnitPlugin.TEST_INTERFACE_NAME);
		if (testCaseType == null)
			return;

		IRegion region= getRegion(element);
		ITypeHierarchy typeHierarchy= javaProject.newTypeHierarchy(testCaseType, region, pm);
		IType[] subtypes= typeHierarchy.getAllSubtypes(testCaseType);
		for (int i= 0; i < subtypes.length; i++) {
			IType type= subtypes[i];
			int cachedFlags= typeHierarchy.getCachedFlags(type);
			if (!Flags.isInterface(cachedFlags) && !Flags.isAbstract(cachedFlags) // do the cheaper tests first
					&& region.contains(type) && TestSearchEngine.isAccessibleClass(type)) {
				result.add(type);
			}
		}
	}

	private static IRegion getRegion(IJavaElement element) throws JavaModelException {
		IRegion result= JavaCore.newRegion();
		if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
			// for projects only add the contained source folders
			IPackageFragmentRoot[] roots= ((IJavaProject) element).getPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (!roots[i].isArchive()) {
					result.add(roots[i]);
				}
			}
		} else {
			result.add(element);
		}
		return result;
	}
	
	private static class JUnitSearchResultCollector extends SearchRequestor {

		private Collection fResult;
		
		public JUnitSearchResultCollector(Collection result) {
			fResult= result;
		}

		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			Object enclosingElement= match.getElement();
			if (!(enclosingElement instanceof IMethod))
				return;

			IMethod method= (IMethod) enclosingElement;
			if (!Flags.isStatic(method.getFlags()) || !Flags.isPublic(method.getFlags())) {
				return;
			}
			
			IType declaringType= ((IMethod) enclosingElement).getDeclaringType();
			if (!TestSearchEngine.isAccessibleClass(declaringType)) {
				return;
			}
			fResult.add(declaringType);
		}
	}

	private static void searchSuiteMethods(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		// fix for bug 36449 JUnit should constrain tests to selected project
		// [JUnit]
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { element }, IJavaSearchScope.SOURCES);
		
		SearchRequestor requestor= new JUnitSearchResultCollector(result);
		int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_ERASURE_MATCH;
		SearchPattern suitePattern= SearchPattern.createPattern("suite() Test", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, matchRule); //$NON-NLS-1$
		SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		new SearchEngine().search(suitePattern, participants, scope, requestor, pm);
	}	


	

	
	
	public boolean isTest(ITypeBinding type) throws JavaModelException {
		if (!type.isClass() || !Modifier.isPublic(type.getModifiers())) {
			return false;
		}
		
		IMethodBinding[] declaredMethods= type.getDeclaredMethods();
		for (int i= 0; i < declaredMethods.length; i++) {
			IMethodBinding curr= declaredMethods[i];
			if ("suite".equals(curr.getName())) { //$NON-NLS-1$
				int flags= curr.getModifiers();
				if (Modifier.isPublic(flags) && Modifier.isStatic(flags) && curr.getParameterTypes().length == 0 &&
						JUnitPlugin.SIMPLE_TEST_INTERFACE_NAME.equals(curr.getReturnType().getQualifiedName())) {
					return true;
				}
			}
		}

		if (TestSearchEngine.isTestImplementor(type)) {
			return true;
		}
		
		return false;
	}

	public boolean isTest(IType type) throws JavaModelException {
		return TestSearchEngine.isAccessibleClass(type) && (hasSuiteMethod(type) || isTestImplementor(type));
	}
	
	private boolean hasSuiteMethod(IType type) throws JavaModelException {
		IMethod method= type.getMethod("suite", new String[0]); //$NON-NLS-1$
		if (!method.exists())
			return false;

		if (!Flags.isStatic(method.getFlags()) || !Flags.isPublic(method.getFlags())) {
			return false;
		}
		if (!Signature.getSimpleName(Signature.toString(method.getReturnType())).equals(JUnitPlugin.SIMPLE_TEST_INTERFACE_NAME)) {
			return false;
		}
		return true;
	}
	
	private boolean isTestImplementor(IType type) throws JavaModelException {
		if (!Flags.isAbstract(type.getFlags()) && TestSearchEngine.isTestImplementor(type)) {
			return true;
		}
		return false;
	}
}
