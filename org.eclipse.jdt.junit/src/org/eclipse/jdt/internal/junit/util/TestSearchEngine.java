/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;

/**
 * Custom Search engine for suite() methods.
 *
 * @see CoreTestSearchEngine
 */
public class TestSearchEngine extends CoreTestSearchEngine {

	public static Set<IType> findTests(IRunnableContext context, IJavaElement element, ITestKind testKind) throws InvocationTargetException, InterruptedException {
		var result= new HashSet<IType>();
		context.run(true, true, (progressMonitor) -> {
			ITestFinder finder= testKind.getFinder();
			try {
				finder.findTestsInContainer(element, result, progressMonitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		});
		return result;
	}

	public static Set<String> findTestMethods(IRunnableContext context, IJavaProject javaProject, IType type, TestKind testKind) throws InvocationTargetException, InterruptedException {
		var result= new HashSet<String>();
		context.run(true, true, (progressMonitor) -> {
			String message= Messages.format(JUnitMessages.TestSearchEngine_search_message_progress_monitor, type.getElementName());
			SubMonitor subMonitor= SubMonitor.convert(progressMonitor, message, 1);
			try {
				collectMethodNames(type, javaProject, testKind.getId(), result, subMonitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		});
		return result;
	}

	private static void collectMethodNames(IType type, IJavaProject javaProject, String testKindId, Set<String> methodNames, IProgressMonitor monitor) throws JavaModelException {
		if (type == null) {
			return;
		}

		SubMonitor subMonitor= SubMonitor.convert(monitor, 3);

		collectDeclaredMethodNames(type, javaProject, testKindId, methodNames);
		subMonitor.split(1);

		String superclassName= type.getSuperclassName();
		IType superType= getResolvedType(superclassName, type, javaProject);
		collectMethodNames(superType, javaProject, testKindId, methodNames, subMonitor.split(1));

		String[] superInterfaceNames= type.getSuperInterfaceNames();
		subMonitor.setWorkRemaining(superInterfaceNames.length);
		for (String interfaceName : superInterfaceNames) {
			superType= getResolvedType(interfaceName, type, javaProject);
			collectMethodNames(superType, javaProject, testKindId, methodNames, subMonitor.split(1));
		}
	}

	private static IType getResolvedType(String typeName, IType type, IJavaProject javaProject) throws JavaModelException {
		IType resolvedType= null;
		if (typeName != null) {
			int pos= typeName.indexOf('<');
			if (pos != -1) {
				typeName= typeName.substring(0, pos);
			}
			String[][] resolvedTypeNames= type.resolveType(typeName);
			if (resolvedTypeNames != null && resolvedTypeNames.length > 0) {
				String[] resolvedTypeName= resolvedTypeNames[0];
				resolvedType= javaProject.findType(resolvedTypeName[0], resolvedTypeName[1]); // secondary types not found by this API
			}
		}
		return resolvedType;
	}

	private static void collectDeclaredMethodNames(IType type, IJavaProject javaProject, String testKindId, Set<String> methodNames) throws JavaModelException {
		for (IMethod method : type.getMethods()) {
			if (!isCandidate(method)) {
				continue;
			}
			// Only include public, no-arg methods that start with "test" and have void return type:
			if (isJUnit3StyleTestMethod(method)) {
				methodNames.add(method.getElementName());
			}
			if (isAnnotatedTestMethod(method, javaProject, testKindId)) {
				methodNames.add(method.getElementName() + JUnitStubUtility.getParameterTypes(method, false));
			}
		}
	}

	/** Returns the <code>@Test</code> annotation of the method, qualified or not, or <code>null</code>. */
	private static IAnnotation findTestAnnotation(IMethod method) throws JavaModelException {
		for (IAnnotation annotation : method.getAnnotations()) {
			String name= annotation.getElementName();
			// a qualified name must be the one of a JUnit annotation, not of another framework such as TestNG
			if ("Test".equals(name) || JUnitCorePlugin.JUNIT4_ANNOTATION_NAME.equals(name) //$NON-NLS-1$
					|| JUnitCorePlugin.JUNIT5_JUPITER_TEST_ANNOTATION_NAME.equals(name)) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Tells whether the project has JUnit (any version) on its classpath.
	 *
	 * @param project the project
	 * @return <code>true</code> if it has
	 */
	public static boolean hasJUnit(IJavaProject project) {
		return hasTestCaseType(project) || hasJUnit4TestAnnotation(project) || hasJUnit5TestAnnotation(project)
				|| hasJUnit6TestAnnotation(project);
	}

	private static final String JUPITER_TEST_FACTORY_ANNOTATION_NAME= "org.junit.jupiter.api.TestFactory"; //$NON-NLS-1$

	private static final String JUPITER_TEST_TEMPLATE_ANNOTATION_NAME= "org.junit.jupiter.api.TestTemplate"; //$NON-NLS-1$

	private static final String[] JUPITER_TEST_ANNOTATION_NAMES= { JUPITER_TEST_FACTORY_ANNOTATION_NAME, JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME,
			JUPITER_TEST_TEMPLATE_ANNOTATION_NAME, "org.junit.jupiter.params.ParameterizedTest", "org.junit.jupiter.api.RepeatedTest" }; //$NON-NLS-1$ //$NON-NLS-2$

	private static boolean isCandidate(IMethod method) throws JavaModelException {
		int flags= method.getFlags();
		return !(Flags.isStatic(flags) || Flags.isPrivate(flags) || Flags.isAbstract(flags));
	}

	/**
	 * Tells whether the method is public, has no argument, returns void and is named like a test of
	 * JUnit 3.
	 *
	 * @param method the method
	 * @return <code>true</code> if it is
	 * @throws JavaModelException if the method cannot be inspected
	 */
	public static boolean isJUnit3StyleTestMethod(IMethod method) throws JavaModelException {
		return Flags.isPublic(method.getFlags())
				&& method.getNumberOfParameters() == 0
				&& method.getElementName().startsWith("test") //$NON-NLS-1$
				&& Signature.SIG_VOID.equals(method.getReturnType());
	}

	private static boolean isAnnotatedTestMethod(IMethod method, IJavaProject javaProject, String testKindId) throws JavaModelException {
		if (TestKindRegistry.JUNIT3_TEST_KIND_ID.equals(testKindId)) {
			return false;
		}
		if (findTestAnnotation(method) != null) {
			return true;
		}
		if (TestKindRegistry.JUNIT4_TEST_KIND_ID.equals(testKindId)) {
			return false;
		}
		// JUnit Jupiter: one of its annotations, or one of the same name which cannot be resolved, such as while editing the
		// imports (the project can declare an annotation of the same name)
		for (String qualifiedName : JUPITER_TEST_ANNOTATION_NAMES) {
			String name= qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
			if (method.getAnnotation(name).exists()) {
				IType annotationType= resolveAnnotationType(name, method.getDeclaringType(), javaProject);
				if (annotationType == null || qualifiedName.equals(annotationType.getFullyQualifiedName())) {
					return true;
				}
			}
		}
		return isAnnotatedWithTestable(method, method.getDeclaringType(), javaProject);
	}

	/**
	 * Tells whether the given method is a test method that the test runner of the given JUnit test
	 * kind runs: a method annotated with a test annotation (JUnit 4 and later, with the signature
	 * that the runner of the annotation needs), or a public no-arg
	 * void method whose name starts with <code>test</code> in a concrete class implementing
	 * <code>junit.framework.Test</code>. The test finders of all the kinds accept such a class, and
	 * the runners of all the kinds run its methods. Static, private and abstract methods are never
	 * tests.
	 * <p>
	 * The declaring class is not checked to be an instantiable test class, use
	 * {@link ITestFinder#isTest(IType)} for this.
	 * </p>
	 *
	 * @param method the method to check
	 * @param javaProject the project used to resolve annotation types
	 * @param testKindId the id of the test kind, see {@link TestKindRegistry}, or <code>null</code>
	 *            if the project has no test kind
	 * @param isConcreteTestImplementor whether the declaring class is a concrete implementor of
	 *            <code>junit.framework.Test</code>, see {@link #isConcreteTestImplementor(IType)}:
	 *            a caller which checks many methods of a class finds it out once, and only if
	 *            {@link #isJUnit3StyleTestMethod(IMethod)} is true for one of them
	 * @return <code>true</code> if the method is a test method
	 * @throws JavaModelException if the method cannot be inspected
	 */
	public static boolean isTestMethod(IMethod method, IJavaProject javaProject, String testKindId, boolean isConcreteTestImplementor) throws JavaModelException {
		if (testKindId == null || !isCandidate(method)) {
			return false;
		}
		return (isAnnotatedTestMethod(method, javaProject, testKindId) && isRunnableTestAnnotation(method))
				|| (isConcreteTestImplementor && isJUnit3StyleTestMethod(method));
	}

	/**
	 * Checks that a method annotated with <code>@Test</code> is one that the runner of this
	 * annotation runs: <code>org.junit.Test</code> needs a public, void, no-arg method,
	 * <code>org.junit.jupiter.api.Test</code> a void method (the other modifiers are checked
	 * elsewhere). Another annotation named <code>Test</code> makes a test only if it is composed from
	 * a Jupiter test annotation: a void method for <code>@Test</code> and the test templates, such as
	 * <code>@ParameterizedTest</code>, and a method returning the tests for <code>@TestFactory</code>.
	 * The methods with other Jupiter annotations are not checked.
	 */
	private static boolean isRunnableTestAnnotation(IMethod method) throws JavaModelException {
		IAnnotation annotation= findTestAnnotation(method);
		if (annotation == null) {
			return true;
		}
		boolean isVoid= Signature.SIG_VOID.equals(method.getReturnType());
		String written= annotation.getElementName();
		// a qualified name needs no resolution
		String[][] resolved= written.indexOf('.') > 0 ? new String[][] { { "", written } } : method.getDeclaringType().resolveType(written); //$NON-NLS-1$
		if (resolved == null) {
			// not resolved, for example while editing the imports: accept it if JUnit is available
			// (the method is neither private, static nor abstract)
			IJavaProject project= method.getJavaProject();
			boolean isJupiter= hasJUnit5TestAnnotation(project) || hasJUnit6TestAnnotation(project);
			boolean isJUnit4Method= Flags.isPublic(method.getFlags()) && method.getNumberOfParameters() == 0;
			return isVoid && hasJUnit(project) && (isJupiter || isJUnit4Method);
		}
		for (String[] candidate : resolved) { // several with imports on demand
			String name= candidate[0].isEmpty() ? candidate[1] : candidate[0] + '.' + candidate[1];
			if (JUnitCorePlugin.JUNIT4_ANNOTATION_NAME.equals(name)) {
				return isVoid && Flags.isPublic(method.getFlags()) && method.getNumberOfParameters() == 0;
			} else if (JUnitCorePlugin.JUNIT5_JUPITER_TEST_ANNOTATION_NAME.equals(name)) {
				return isVoid;
			}
		}
		// a composed annotation of the package, which can be named Test too
		IType context= method.getDeclaringType();
		IJavaProject project= method.getJavaProject();
		if (isOrIsMetaAnnotatedWith(annotation, context, project, JUPITER_TEST_FACTORY_ANNOTATION_NAME)) {
			return !isVoid;
		}
		return isVoid && (isOrIsMetaAnnotatedWith(annotation, context, project, JUnitCorePlugin.JUNIT5_JUPITER_TEST_ANNOTATION_NAME)
				|| isOrIsMetaAnnotatedWith(annotation, context, project, JUPITER_TEST_TEMPLATE_ANNOTATION_NAME));
	}

	/**
	 * Tells whether an annotation is the annotation of the given name, or a composed annotation which is
	 * meta-annotated with it, directly or through other annotations.
	 *
	 * @param annotation the annotation of a program element
	 * @param context the type to resolve the annotation in
	 * @param javaProject the project used to resolve annotation types
	 * @param annotationName the fully qualified name of the annotation to look for
	 * @return <code>true</code> if it is, or is meta-annotated with it
	 * @throws JavaModelException if the annotation types cannot be inspected
	 */
	public static boolean isOrIsMetaAnnotatedWith(IAnnotation annotation, IType context, IJavaProject javaProject, String annotationName) throws JavaModelException {
		IType annotationType= resolveAnnotationType(annotation.getElementName(), context, javaProject);
		return annotationType != null
				&& (annotationName.equals(annotationType.getFullyQualifiedName()) || isMetaAnnotatedWith(annotationType, javaProject, annotationName, new HashSet<>()));
	}

	/** Resolves the name of an annotation; the one of a binary type is already qualified, and cannot be resolved in it. */
	private static IType resolveAnnotationType(String name, IType context, IJavaProject javaProject) throws JavaModelException {
		String[][] names= context.resolveType(name);
		if (names == null || names.length == 0) {
			return name.indexOf('.') > 0 ? javaProject.findType(name) : null;
		}
		for (String[] candidate : names) { // several with imports on demand
			IType found= javaProject.findType(candidate[0], candidate[1]);
			if (found == null) {
				// a secondary type, which this API does not find, such as an annotation declared in the file of the test class
				ICompilationUnit unit= context.getCompilationUnit();
				found= unit != null && candidate[0].equals(context.getPackageFragment().getElementName()) && unit.getType(candidate[1]).exists()
						? unit.getType(candidate[1])
						: javaProject.findType(candidate[0], candidate[1], (IProgressMonitor) null);
			}
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/**
	 * Tells whether the method is annotated with the <code>@Test</code> of JUnit 4, whose runner runs it
	 * even if a subclass overrides it without the annotation.
	 *
	 * @param method the method
	 * @return <code>true</code> if it is
	 * @throws JavaModelException if the annotation cannot be resolved
	 */
	public static boolean hasJUnit4TestAnnotation(IMethod method) throws JavaModelException {
		IAnnotation annotation= findTestAnnotation(method);
		if (annotation == null) {
			return false;
		}
		IType annotationType= resolveAnnotationType(annotation.getElementName(), method.getDeclaringType(), method.getJavaProject());
		return annotationType != null && JUnitCorePlugin.JUNIT4_ANNOTATION_NAME.equals(annotationType.getFullyQualifiedName());
	}

	private static boolean isMetaAnnotatedWith(IType annotationType, IJavaProject javaProject, String annotationName, Set<IType> visited) throws JavaModelException {
		for (IAnnotation annotation : annotationType.getAnnotations()) {
			IType metaType= resolveAnnotationType(annotation.getElementName(), annotationType, javaProject);
			if (metaType != null && visited.add(metaType)
					&& (annotationName.equals(metaType.getFullyQualifiedName()) || isMetaAnnotatedWith(metaType, javaProject, annotationName, visited))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tells whether the given class is concrete and implements <code>junit.framework.Test</code>.
	 *
	 * @param type the class
	 * @return <code>true</code> if it is
	 * @throws JavaModelException if the class cannot be inspected
	 */
	public static boolean isConcreteTestImplementor(IType type) throws JavaModelException {
		return !Flags.isAbstract(type.getFlags()) && isTestImplementor(type);
	}

	// See JUnit5TestFinder.Annotation#annotates also.
	private static boolean isAnnotatedWithTestable(IMethod method, IType declaringType, IJavaProject javaProject) throws JavaModelException {
		for (IAnnotation annotation : method.getAnnotations()) {
			// composed and qualified annotations, whose types may be binary
			if (isOrIsMetaAnnotatedWith(annotation, declaringType, javaProject, JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME)) {
				return true;
			}
		}
		return false;
	}
}
