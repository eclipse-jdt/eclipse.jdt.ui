/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.util;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.junit.JUnitCore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;


/**
 * Custom Search engine for suite() methods
 */
public class CoreTestSearchEngine {

	public static boolean isTestOrTestSuite(IType declaringType) throws CoreException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(declaringType);
		return testKind.getFinder().isTest(declaringType);
	}

	public static boolean isAccessibleClass(IType type) throws JavaModelException {
		int flags= type.getFlags();
		if (Flags.isInterface(flags)) {
			return false;
		}
		IJavaElement parent= type.getParent();
		while (true) {
			if (parent instanceof ICompilationUnit || parent instanceof IClassFile) {
				return true;
			}
			if (!(parent instanceof IType) || !Flags.isStatic(flags) || !Flags.isPublic(flags)) {
				return false;
			}
			flags= ((IType) parent).getFlags();
			parent= parent.getParent();
		}
	}

	public static boolean isAccessibleClass(ITypeBinding type) {
		if (type.isInterface()) {
			return false;
		}
		int modifiers= type.getModifiers();
		while (true) {
			if (type.getDeclaringMethod() != null) {
				return false;
			}
			ITypeBinding declaringClass= type.getDeclaringClass();
			if (declaringClass == null) {
				return true;
			}
			if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
				return false;
			}
			modifiers= declaringClass.getModifiers();
			type= declaringClass;
		}
	}

	public static boolean hasTestCaseType(IJavaProject javaProject) {
		try {
			return javaProject != null && javaProject.findType(JUnitCorePlugin.TEST_SUPERCLASS_NAME) != null;
		} catch (JavaModelException e) {
			// not available
		}
		return false;
	}

	public static boolean hasTestAnnotation(IJavaProject project) {
		try {
			if (project != null) {
				IType type= project.findType(JUnitCorePlugin.JUNIT4_ANNOTATION_NAME);
				if (type != null) {
					// @Test annotation is not accessible if the JUnit classpath container is set to JUnit 3
					// (although it may resolve to a JUnit 4 JAR)
					IPackageFragmentRoot root= (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					IClasspathEntry cpEntry= root.getRawClasspathEntry();
					return ! JUnitCore.JUNIT3_CONTAINER_PATH.equals(cpEntry.getPath());
				}
			}
		} catch (JavaModelException e) {
			// not available
		}
		return false;
	}

	public static boolean isTestImplementor(IType type) throws JavaModelException {
		ITypeHierarchy typeHier= type.newSupertypeHierarchy(null);
		IType[] superInterfaces= typeHier.getAllInterfaces();
		for (int i= 0; i < superInterfaces.length; i++) {
			if (JUnitCorePlugin.TEST_INTERFACE_NAME.equals(superInterfaces[i].getFullyQualifiedName('.'))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isTestImplementor(ITypeBinding type) {
		ITypeBinding superType= type.getSuperclass();
		if (superType != null && isTestImplementor(superType)) {
			return true;
		}
		ITypeBinding[] interfaces= type.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			ITypeBinding curr= interfaces[i];
			if (JUnitCorePlugin.TEST_INTERFACE_NAME.equals(curr.getQualifiedName()) || isTestImplementor(curr)) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasSuiteMethod(IType type) throws JavaModelException {
		IMethod method= type.getMethod("suite", new String[0]); //$NON-NLS-1$
		if (!method.exists())
			return false;

		if (!Flags.isStatic(method.getFlags()) || !Flags.isPublic(method.getFlags())) {
			return false;
		}
		if (!Signature.getSimpleName(Signature.toString(method.getReturnType())).equals(JUnitCorePlugin.SIMPLE_TEST_INTERFACE_NAME)) {
			return false;
		}
		return true;
	}

	public static IRegion getRegion(IJavaElement element) throws JavaModelException {
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

	public static void findTestImplementorClasses(ITypeHierarchy typeHierarchy, IType testInterface, IRegion region, Set result)
			throws JavaModelException {
		IType[] subtypes= typeHierarchy.getAllSubtypes(testInterface);
		for (int i= 0; i < subtypes.length; i++) {
			IType type= subtypes[i];
			int cachedFlags= typeHierarchy.getCachedFlags(type);
			if (!Flags.isInterface(cachedFlags) && !Flags.isAbstract(cachedFlags) // do the cheaper tests first
					&& region.contains(type) && CoreTestSearchEngine.isAccessibleClass(type)) {
				result.add(type);
			}
		}
	}

	private static class SuiteMethodTypesCollector extends SearchRequestor {

		private Collection fResult;

		public SuiteMethodTypesCollector(Collection result) {
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
			if (!CoreTestSearchEngine.isAccessibleClass(declaringType)) {
				return;
			}
			fResult.add(declaringType);
		}
	}

	public static void findSuiteMethods(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		// fix for bug 36449 JUnit should constrain tests to selected project
		// [JUnit]
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { element }, IJavaSearchScope.SOURCES);

		SearchRequestor requestor= new SuiteMethodTypesCollector(result);
		int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_ERASURE_MATCH;
		SearchPattern suitePattern= SearchPattern.createPattern("suite() Test", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, matchRule); //$NON-NLS-1$
		SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		new SearchEngine().search(suitePattern, participants, scope, requestor, pm);
	}

	
// --- copied from org.eclipse.jdt.internal.corext.util.JavaModelUtil: ---
	/**
	 * @param version1 the first version
	 * @param version2 the second version
	 * @return <code>true</code> iff version1 is less than version2
	 */
	public static boolean isVersionLessThan(String version1, String version2) {
		if (JavaCore.VERSION_CLDC_1_1.equals(version1)) {
			version1= JavaCore.VERSION_1_1 + 'a';
		}
		if (JavaCore.VERSION_CLDC_1_1.equals(version2)) {
			version2= JavaCore.VERSION_1_1 + 'a';
		}
		return version1.compareTo(version2) < 0;
	}


	public static boolean is50OrHigher(String compliance) {
		return !isVersionLessThan(compliance, JavaCore.VERSION_1_5);
	}

	/**
	 * Checks if the given project or workspace has source compliance 5.0 or greater.
	 *
	 * @param project the project to test or <code>null</code> to test the workspace settings
	 * @return <code>true</code> if the given project or workspace has source compliance 5.0 or greater.
	 */
	public static boolean is50OrHigher(IJavaProject project) {
		String source= project != null ? project.getOption(JavaCore.COMPILER_SOURCE, true) : JavaCore.getOption(JavaCore.COMPILER_SOURCE);
		return is50OrHigher(source);
	}
// ---

}
