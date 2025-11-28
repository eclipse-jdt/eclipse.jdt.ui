/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.util;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Version;

import org.eclipse.jdt.junit.JUnitCore;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

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
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;


/**
 * Custom Search engine for suite() methods
 */
public class CoreTestSearchEngine {

	private static final String JUNIT_PLATFORM_SUITE_API_PREFIX= BuildPathSupport.JUNIT_PLATFORM_SUITE_API;
	private static final String JUNIT_PLATFORM_COMMONS_PREFIX= BuildPathSupport.JUNIT_PLATFORM_COMMONS;
	private static final String JAR_EXTENSION= ".jar"; //$NON-NLS-1$

	public static boolean isTestOrTestSuite(IType declaringType) throws CoreException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(declaringType);
		return testKind.getFinder().isTest(declaringType);
	}

	public static boolean isAccessibleClass(IType type, String testKindId) throws JavaModelException {
		int flags= type.getFlags();
		if (Flags.isInterface(flags)) {
			return false;
		}
		IJavaElement parent= type.getParent();
		while (true) {
			if (parent instanceof ICompilationUnit || parent instanceof IClassFile) {
				return true;
			}
			if (!(parent instanceof IType)) {
				return false;
			}
			if (TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKindId)) {
				// A nested/inner class need not be public in JUnit 5.
				if (Flags.isPrivate(flags)) {
					return false;
				}
				// If the inner class is non-static, it must be annotated with @Nested.
				if (!Flags.isStatic(flags) && !type.getAnnotation("Nested").exists()) { //$NON-NLS-1$
					return false;
				}
			} else if (!Flags.isStatic(flags) || !Flags.isPublic(flags)) {
				return false;
			}
			flags= ((IType) parent).getFlags();
			parent= parent.getParent();
		}
	}

	public static boolean isAccessibleClass(IType type) throws JavaModelException {
		return isAccessibleClass(type, null);
	}

	public static boolean isAccessibleClass(ITypeBinding type) { // not used
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

	public static boolean hasJUnit4TestAnnotation(IJavaProject project) {
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

	public static boolean hasJUnit5TestAnnotation(IJavaProject project) {
		return hasJUnitJupiterTestAnnotation(project, 1, // we check JUnit 5 platform bundles, they range in [1.0,2.0)
				JUnitCore.JUNIT3_CONTAINER_PATH, JUnitCore.JUNIT4_CONTAINER_PATH, JUnitCore.JUNIT6_CONTAINER_PATH);
	}

	public static boolean hasJUnit6TestAnnotation(IJavaProject project) {
		return hasJUnitJupiterTestAnnotation(project, 6,
				JUnitCore.JUNIT3_CONTAINER_PATH, JUnitCore.JUNIT4_CONTAINER_PATH, JUnitCore.JUNIT5_CONTAINER_PATH);
	}

	private static boolean hasJUnitJupiterTestAnnotation(IJavaProject project, int junitMajorVersion, IPath... disallowedJunitContainerPaths) {
		try {
			if (project != null) {
				String junitBundlePrefix = JUNIT_PLATFORM_COMMONS_PREFIX;
				IType type= project.findType(JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME);
				if (type == null) {
					junitBundlePrefix = JUNIT_PLATFORM_SUITE_API_PREFIX;
					type= project.findType(JUnitCorePlugin.JUNIT5_SUITE_ANNOTATION_NAME);
				}
				if (type != null) {
					// check if we have the right JUnit JUpiter version
					Version version = null;
					String filename= type.getPath().lastSegment();
					boolean isJar = filename.endsWith(JAR_EXTENSION);
					// Try matching the library name, this should be enough for many cases.
					if (isJar && (filename.startsWith(junitBundlePrefix + "_") || filename.startsWith(junitBundlePrefix + "-"))) { //$NON-NLS-1$ //$NON-NLS-2$
						String versionString = filename.substring(junitBundlePrefix.length() + 1, filename.length() - JAR_EXTENSION.length());
						try {
							version = Version.parseVersion(versionString);
						} catch (IllegalArgumentException e) {
							/*
							 * Some JUnit distributions use different naming, ignore those. See: https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2665
							 * We will try parsing a jar manifest below.
							 */
						}
					}
					if (isJar && version == null) {
						File jarFilePath = null;
						IPath jarPathFromType = type.getPath();
						IFile jarFileInWs = ResourcesPlugin.getWorkspace().getRoot().getFile(jarPathFromType);
						if (jarFileInWs.exists()) {
							jarFilePath= jarFileInWs.getLocation().toFile();
						} else {
							jarFilePath= jarPathFromType.toFile();
						}

						try (JarFile jarFile= new JarFile(jarFilePath)) {
							Manifest manifest= jarFile.getManifest();
							if (manifest != null) {
								Attributes attributes= manifest.getMainAttributes();
								String versionString= attributes.getValue("Specification-Version"); //$NON-NLS-1$
								if (versionString != null) {
									version= Version.parseVersion(versionString);
								}
							}
						} catch (Throwable e) {
							JUnitCorePlugin.log(Status.warning("Failed to determine JUnit version: " + jarFilePath, e)); //$NON-NLS-1$
						}
					}
					if (version != null && version.getMajor() != junitMajorVersion) {
						return false;
					}
					// @Testable/@Suite annotations are not accessible if the JUnit classpath container is set to JUnit 3 or JUnit 4
					// (although it may resolve to a JUnit 5 JAR)
					IPackageFragmentRoot root= (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
					IClasspathEntry cpEntry= root.getRawClasspathEntry();
					IPath entryPath= cpEntry.getPath();
					return !Arrays.asList(disallowedJunitContainerPaths).contains(entryPath);
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
		for (IType superInterface : superInterfaces) {
			if (JUnitCorePlugin.TEST_INTERFACE_NAME.equals(superInterface.getFullyQualifiedName('.'))) {
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
		for (ITypeBinding curr : interfaces) {
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
		if (!JUnitCorePlugin.SIMPLE_TEST_INTERFACE_NAME.equals(Signature.getSimpleName(Signature.toString(method.getReturnType())))) {
			return false;
		}
		return true;
	}

	public static IRegion getRegion(IJavaElement element) throws JavaModelException {
		IRegion result= JavaCore.newRegion();
		if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
			// for projects only add the contained source folders
			IPackageFragmentRoot[] roots= ((IJavaProject) element).getPackageFragmentRoots();
			for (IPackageFragmentRoot root : roots) {
				if (!root.isArchive()) {
					result.add(root);
				}
			}
		} else {
			result.add(element);
		}
		return result;
	}

	public static void findTestImplementorClasses(ITypeHierarchy typeHierarchy, IType testInterface, IRegion region, Set<IType> result)
			throws JavaModelException {
		IType[] subtypes= typeHierarchy.getAllSubtypes(testInterface);
		for (IType type : subtypes) {
			int cachedFlags= typeHierarchy.getCachedFlags(type);
			if (!Flags.isInterface(cachedFlags) && !Flags.isAbstract(cachedFlags) // do the cheaper tests first
				&& region.contains(type) && CoreTestSearchEngine.isAccessibleClass(type)) {
				result.add(type);
			}
		}
	}

	private static class SuiteMethodTypesCollector extends SearchRequestor {

		private Collection<IType> fResult;

		public SuiteMethodTypesCollector(Collection<IType> result) {
			fResult= result;
		}

		@Override
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

	public static void findSuiteMethods(IJavaElement element, Set<IType> result, IProgressMonitor pm) throws CoreException {
		// fix for bug 36449 JUnit should constrain tests to selected project
		// [JUnit]
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { element }, IJavaSearchScope.SOURCES);

		SearchRequestor requestor= new SuiteMethodTypesCollector(result);
		int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_ERASURE_MATCH;
		SearchPattern suitePattern= SearchPattern.createPattern("suite() Test", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, matchRule); //$NON-NLS-1$
		SearchParticipant[] participants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		new SearchEngine().search(suitePattern, participants, scope, requestor, pm);
	}
}
