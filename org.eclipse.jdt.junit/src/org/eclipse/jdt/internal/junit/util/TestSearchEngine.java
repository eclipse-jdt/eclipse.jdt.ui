/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;


/**
 * Custom Search engine for suite() methods
 * @see CoreTestSearchEngine
 */
public class TestSearchEngine extends CoreTestSearchEngine {

	public static Set<IType> findTests(IRunnableContext context, final IJavaElement element, final ITestKind testKind) throws InvocationTargetException, InterruptedException {
		final Set<IType> result= new HashSet<>();

		IRunnableWithProgress runnable= progressMonitor -> {
			try {
				testKind.getFinder().findTestsInContainer(element, result, progressMonitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
		context.run(true, true, runnable);
		return result;
	}

	public static Set<String> findTestMethods(IRunnableContext context, final IJavaProject javaProject, IType type, TestKind testKind) throws InvocationTargetException, InterruptedException {
		final Set<String> result= new HashSet<>();

		IRunnableWithProgress runnable= progressMonitor -> {
			try {
				String message= Messages.format(JUnitMessages.TestSearchEngine_search_message_progress_monitor, type.getElementName());
				SubMonitor subMonitor= SubMonitor.convert(progressMonitor, message, 1);

				collectMethodNames(type, javaProject, testKind.getId(), result, subMonitor.split(1));
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
		context.run(true, true, runnable);
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
		IMethod[] methods= type.getMethods();
		for (IMethod method : methods) {
			String methodName= method.getElementName();
			int flags= method.getFlags();
			// Only include public, non-static, no-arg methods that return void and start with "test":
			if (Modifier.isPublic(flags) && !Modifier.isStatic(flags) &&
					method.getNumberOfParameters() == 0 && Signature.SIG_VOID.equals(method.getReturnType()) &&
					methodName.startsWith("test")) { //$NON-NLS-1$
				methodNames.add(methodName);
			}
			boolean isJUnit3= TestKindRegistry.JUNIT3_TEST_KIND_ID.equals(testKindId);
			boolean isJUnit5= TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(testKindId);
			if (!isJUnit3 && !Modifier.isPrivate(flags) && !Modifier.isStatic(flags)) {
				IAnnotation annotation= method.getAnnotation("Test"); //$NON-NLS-1$
				if (annotation.exists()) {
					methodNames.add(methodName + JUnitStubUtility.getParameterTypes(method, false));
				} else if (isJUnit5) {
					boolean hasAnyTestAnnotation= method.getAnnotation("TestFactory").exists() //$NON-NLS-1$
							|| method.getAnnotation("Testable").exists() //$NON-NLS-1$
							|| method.getAnnotation("TestTemplate").exists() //$NON-NLS-1$
							|| method.getAnnotation("ParameterizedTest").exists() //$NON-NLS-1$
							|| method.getAnnotation("RepeatedTest").exists(); //$NON-NLS-1$
					if (hasAnyTestAnnotation || isAnnotatedWithTestable(method, type, javaProject)) {
						methodNames.add(methodName + JUnitStubUtility.getParameterTypes(method, false));
					}
				}
			}
		}
	}

	// See JUnit5TestFinder.Annotation#annotates also.
	private static boolean isAnnotatedWithTestable(IMethod method, IType declaringType, IJavaProject javaProject) throws JavaModelException {
		for (IAnnotation annotation : method.getAnnotations()) {
			IType annotationType= getResolvedType(annotation.getElementName(), declaringType, javaProject);
			if (annotationType != null) {
				if (matchesTestable(annotationType)) {
					return true;
				}
				Set<IType> hierarchy= new HashSet<>();
				if (matchesTestableInAnnotationHierarchy(annotationType, javaProject, hierarchy)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean matchesTestable(IType annotationType) {
		return annotationType != null && JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME.equals(annotationType.getFullyQualifiedName());
	}

	private static boolean matchesTestableInAnnotationHierarchy(IType annotationType, IJavaProject javaProject, Set<IType> hierarchy) throws JavaModelException {
		if (annotationType != null) {
			for (IAnnotation annotation : annotationType.getAnnotations()) {
				IType annType= getResolvedType(annotation.getElementName(), annotationType, javaProject);
				if (annType != null && hierarchy.add(annType)) {
					if (matchesTestable(annType) || matchesTestableInAnnotationHierarchy(annType, javaProject, hierarchy)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
