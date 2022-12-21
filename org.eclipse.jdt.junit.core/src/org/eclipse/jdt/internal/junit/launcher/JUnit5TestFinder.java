/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;

public class JUnit5TestFinder implements ITestFinder {

	private static class Annotation {

		private static final Annotation RUN_WITH= new Annotation("org.junit.runner.RunWith"); //$NON-NLS-1$

		private static final Annotation TEST_4= new Annotation("org.junit.Test"); //$NON-NLS-1$

		private static final Annotation SUITE= new Annotation("org.junit.platform.suite.api.Suite"); //$NON-NLS-1$

		private static final Annotation TESTABLE= new Annotation(JUnitCorePlugin.JUNIT5_TESTABLE_ANNOTATION_NAME);

		private static final Annotation NESTED= new Annotation(JUnitCorePlugin.JUNIT5_JUPITER_NESTED_ANNOTATION_NAME);

		private final String fName;

		private Annotation(String name) {
			fName= name;
		}

		String getName() {
			return fName;
		}

		boolean annotatesAtLeastOneInnerClass(ITypeBinding type) {
			if (type == null) {
				return false;
			}
			if (annotatesDeclaredTypes(type)) {
				return true;
			}
			ITypeBinding superClass= type.getSuperclass();
			if (annotatesAtLeastOneInnerClass(superClass)) {
				return true;
			}
			ITypeBinding[] interfaces= type.getInterfaces();
			for (ITypeBinding intf : interfaces) {
				if (annotatesAtLeastOneInnerClass(intf)) {
					return true;
				}
			}
			return false;
		}

		private boolean annotatesDeclaredTypes(ITypeBinding type) {
			ITypeBinding[] declaredTypes= type.getDeclaredTypes();
			for (ITypeBinding declaredType : declaredTypes) {
				if (isNestedClass(declaredType)) {
					return true;
				}
			}
			return false;
		}

		private boolean isNestedClass(ITypeBinding type) {
			int modifiers= type.getModifiers();
			if (type.isInterface() || Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers)) {
				return false;
			}
			if (annotates(type.getAnnotations())) {
				return true;
			}
			return false;
		}

		boolean annotatesTypeOrSuperTypes(ITypeBinding type) {
			while (type != null) {
				if (annotates(type.getAnnotations())) {
					return true;
				}
				type= type.getSuperclass();
			}
			return false;
		}

		boolean annotatesAtLeastOneMethod(ITypeBinding type) {
			if (type == null) {
				return false;
			}
			if (annotatesDeclaredMethods(type)) {
				return true;
			}
			ITypeBinding superClass= type.getSuperclass();
			if (annotatesAtLeastOneMethod(superClass)) {
				return true;
			}
			ITypeBinding[] interfaces= type.getInterfaces();
			for (ITypeBinding intf : interfaces) {
				if (annotatesAtLeastOneMethod(intf)) {
					return true;
				}
			}
			return false;
		}

		private boolean annotatesDeclaredMethods(ITypeBinding type) {
			IMethodBinding[] declaredMethods= type.getDeclaredMethods();
			for (IMethodBinding curr : declaredMethods) {
				if (annotates(curr.getAnnotations())) {
					return true;
				}
			}
			return false;
		}

		// See JUnitLaunchConfigurationTab#isAnnotatedWithTestable also.
		private boolean annotates(IAnnotationBinding[] annotations) {
			for (IAnnotationBinding annotation : annotations) {
				if (annotation == null) {
					continue;
				}
				if (matchesName(annotation.getAnnotationType())) {
					return true;
				}
				if (TESTABLE.getName().equals(fName) || NESTED.getName().equals(fName)) {
					Set<ITypeBinding> hierarchy= new HashSet<>();
					if (matchesNameInAnnotationHierarchy(annotation, hierarchy)) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean matchesName(ITypeBinding annotationType) {
			if (annotationType != null) {
				String qualifiedName= annotationType.getQualifiedName();
				if (qualifiedName.equals(fName)) {
					return true;
				}
			}
			return false;
		}

		private boolean matchesNameInAnnotationHierarchy(IAnnotationBinding annotation, Set<ITypeBinding> hierarchy) {
			ITypeBinding type= annotation.getAnnotationType();
			if (type != null) {
				for (IAnnotationBinding annotationBinding : type.getAnnotations()) {
					if (annotationBinding != null) {
						ITypeBinding annotationType= annotationBinding.getAnnotationType();
						if (annotationType != null && hierarchy.add(annotationType)) {
							if (matchesName(annotationType) || matchesNameInAnnotationHierarchy(annotationBinding, hierarchy)) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}
	}

	@Override
	public void findTestsInContainer(IJavaElement element, Set<IType> result, IProgressMonitor pm) throws CoreException {
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}

		if (element instanceof IType) {
			IType type= (IType) element;
			if (internalIsTest(type, pm)) {
				result.add(type);
				return;
			}
		}

		var subMonitor = SubMonitor.convert(pm, JUnitMessages.JUnit5TestFinder_searching_description, 4);

		IRegion region= CoreTestSearchEngine.getRegion(element);
		ITypeHierarchy hierarchy= JavaCore.newTypeHierarchy(region, null, subMonitor.split(1));
		IType[] allClasses= hierarchy.getAllClasses();

		// search for all types with references to RunWith and Test and all subclasses
		for (IType type : allClasses) {
			if (region.contains(type) && internalIsTest(type, pm)) {
				addTypeAndSubtypes(type, result, hierarchy);
			}
		}

		// add all classes implementing JUnit 3.8's Test interface in the region
		IType testInterface= element.getJavaProject().findType(JUnitCorePlugin.TEST_INTERFACE_NAME);
		if (testInterface != null) {
			CoreTestSearchEngine.findTestImplementorClasses(hierarchy, testInterface, region, result);
		}

		//JUnit 4.3 can also run JUnit-3.8-style public static Test suite() methods:
		CoreTestSearchEngine.findSuiteMethods(element, result, subMonitor.split(1));
	}

	private void addTypeAndSubtypes(IType type, Set<IType> result, ITypeHierarchy hierarchy) {
		if (result.add(type)) {
			IType[] subclasses= hierarchy.getSubclasses(type);
			for (IType subclasse : subclasses) {
				addTypeAndSubtypes(subclasse, result, hierarchy);
			}
		}
	}

	@Override
	public boolean isTest(IType type) throws JavaModelException {
		return internalIsTest(type, null);
	}

	private boolean internalIsTest(IType type, IProgressMonitor monitor) throws JavaModelException {
		if (CoreTestSearchEngine.isAccessibleClass(type, TestKindRegistry.JUNIT5_TEST_KIND_ID)) {
			if (CoreTestSearchEngine.hasSuiteMethod(type)) { // since JUnit 4.3.1
				return true;
			}
			ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
			if (type.getCompilationUnit() != null) {
				parser.setSource(type.getCompilationUnit());
			} else if (!isAvailable(type.getSourceRange())) { // class file with no source
				parser.setProject(type.getJavaProject());
				IBinding[] bindings= parser.createBindings(new IJavaElement[] { type }, monitor);
				if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
					ITypeBinding binding= (ITypeBinding) bindings[0];
					return isTest(binding);
				}
				return false;
			} else {
				parser.setSource(type.getClassFile());
			}
			parser.setFocalPosition(0);
			parser.setResolveBindings(true);
			CompilationUnit root= (CompilationUnit) parser.createAST(monitor);
			ASTNode node= root.findDeclaringNode(type.getKey());
			if (node instanceof TypeDeclaration || node instanceof RecordDeclaration) {
				ITypeBinding binding= ((AbstractTypeDeclaration) node).resolveBinding();
				if (binding != null) {
					return isTest(binding);
				}
			}
		}
		return false;

	}

	private static boolean isAvailable(ISourceRange range) {
		return range != null && range.getOffset() != -1;
	}


	private boolean isTest(ITypeBinding binding) {
		if (Modifier.isAbstract(binding.getModifiers()))
			return false;

		if (Annotation.RUN_WITH.annotatesTypeOrSuperTypes(binding)
				|| Annotation.SUITE.annotatesTypeOrSuperTypes(binding)
				|| Annotation.TEST_4.annotatesAtLeastOneMethod(binding)
				|| Annotation.TESTABLE.annotatesAtLeastOneMethod(binding)
				|| Annotation.TESTABLE.annotatesTypeOrSuperTypes(binding)
				|| Annotation.NESTED.annotatesAtLeastOneInnerClass(binding)) {
			return true;
		}
		return CoreTestSearchEngine.isTestImplementor(binding);
	}
}
