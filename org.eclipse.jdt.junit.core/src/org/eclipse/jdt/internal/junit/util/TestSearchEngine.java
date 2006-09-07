/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * Custom Search engine for suite() methods
 */
public class TestSearchEngine {

	public static boolean isTestOrTestSuite(IType declaringType) throws CoreException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(declaringType);
		return testKind.getFinder().isTest(declaringType);
	}


	public static IType[] findTests(IRunnableContext context, final IJavaElement element, final ITestKind testKind) throws InvocationTargetException, InterruptedException {
		final Set result= new HashSet();

		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InterruptedException, InvocationTargetException {		
				try {
					testKind.getFinder().findTestsInContainer(element, result, pm);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		context.run(true, true, runnable);
		return (IType[]) result.toArray(new IType[result.size()]);
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
	
	public static boolean isAccessibleClass(ITypeBinding type) throws JavaModelException {
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
			return javaProject != null && javaProject.findType(JUnitPlugin.TEST_SUPERCLASS_NAME) != null;
		} catch (JavaModelException e) {
			// not available
		}
		return false;
	}

	public static boolean hasTestAnnotation(IJavaProject project) {
		try {
			return project != null && project.findType(JUnitPlugin.JUNIT4_ANNOTATION_NAME) != null;
		} catch (JavaModelException e) {
			// not available
		}
		return false;
	}
	
	public static boolean isTestImplementor(IType type) throws JavaModelException {
		ITypeHierarchy typeHier= type.newSupertypeHierarchy(null);
		IType[] superInterfaces= typeHier.getAllInterfaces();
		for (int i= 0; i < superInterfaces.length; i++) {
			if (JUnitPlugin.TEST_INTERFACE_NAME.equals(superInterfaces[i].getFullyQualifiedName('.'))) {
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
			if (JUnitPlugin.TEST_INTERFACE_NAME.equals(curr.getQualifiedName()) || isTestImplementor(curr)) {
				return true;
			}
		}
		return false;
	}
}
