/*******************************************************************************
 * Copyright (c) 2006, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
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
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;


public class JUnit4TestFinder implements ITestFinder {

	private static class Annotation {

		private static final Annotation RUN_WITH= new Annotation("org.junit.runner.RunWith"); //$NON-NLS-1$
		private static final Annotation TEST= new Annotation("org.junit.Test"); //$NON-NLS-1$

		private final String fName;

		private Annotation(String name) {
			fName= name;
		}

		public String getName() {
			return fName;
		}

		private boolean annotates(IAnnotationBinding[] annotations) {
			for (IAnnotationBinding annotation : annotations) {
				ITypeBinding annotationType= annotation.getAnnotationType();
				if (annotationType != null && (annotationType.getQualifiedName().equals(fName))) {
					return true;
				}
			}
			return  false;
		}

		public boolean annotatesTypeOrSuperTypes(ITypeBinding type) {
			while (type != null) {
				if (annotates(type.getAnnotations())) {
					return true;
				}
				type= type.getSuperclass();
			}
			return false;
		}

		public boolean annotatesAtLeastOneMethod(ITypeBinding type) {
			while (type != null) {
				IMethodBinding[] declaredMethods= type.getDeclaredMethods();
				for (IMethodBinding curr : declaredMethods) {
					if (annotates(curr.getAnnotations())) {
						return true;
					}
				}
				type= type.getSuperclass();
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
			if (internalIsTest((IType) element, pm)) {
				result.add((IType) element);
				return;
			}
		}

		SubMonitor subMon= SubMonitor.convert(pm, JUnitMessages.JUnit4TestFinder_searching_description, 4);
		try {

			IRegion region= CoreTestSearchEngine.getRegion(element);
			ITypeHierarchy hierarchy= JavaCore.newTypeHierarchy(region, null, subMon.newChild(1));
			IType[] allClasses= hierarchy.getAllClasses();

			// filter out anonymous classes which have no name
			List<IType> nonAnonymousClasses= new ArrayList<>();
			for (IType t : allClasses) {
				if (!t.getElementName().isEmpty()) {
					nonAnonymousClasses.add(t);
				}
			}
			IType[] filteredClasses= nonAnonymousClasses.toArray(new IType[0]);

			// search for all types with references to RunWith and Test and all subclasses
			HashSet<IType> candidates= new HashSet<>(filteredClasses.length);
			SearchRequestor requestor= new AnnotationSearchRequestor(hierarchy, candidates);

			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(filteredClasses, IJavaSearchScope.SOURCES);
			int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			SearchPattern runWithPattern= SearchPattern.createPattern(Annotation.RUN_WITH.getName(), IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);
			SearchPattern testPattern= SearchPattern.createPattern(Annotation.TEST.getName(), IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);

			SearchPattern annotationsPattern= SearchPattern.createOrPattern(runWithPattern, testPattern);
			SearchParticipant[] searchParticipants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
			new SearchEngine().search(annotationsPattern, searchParticipants, scope, requestor, subMon.newChild(2));

			// find all classes in the region
			for (IType curr : candidates) {
				if (!Flags.isAbstract(curr.getFlags()) && CoreTestSearchEngine.isAccessibleClass(curr) && region.contains(curr)) {
					result.add(curr);
				}
			}

			// add all classes implementing JUnit 3.8's Test interface in the region
			IType testInterface= element.getJavaProject().findType(JUnitCorePlugin.TEST_INTERFACE_NAME);
			if (testInterface != null) {
				CoreTestSearchEngine.findTestImplementorClasses(hierarchy, testInterface, region, result);
			}

			//JUnit 4.3 can also run JUnit-3.8-style public static Test suite() methods:
			CoreTestSearchEngine.findSuiteMethods(element, result, subMon.newChild(1));
		} finally {
			subMon.done();
		}
	}

	private static class AnnotationSearchRequestor extends SearchRequestor {

		private final Collection<IType> fResult;
		private final ITypeHierarchy fHierarchy;

		public AnnotationSearchRequestor(ITypeHierarchy hierarchy, Collection<IType> result) {
			fHierarchy= hierarchy;
			fResult= result;
		}

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
				Object element= match.getElement();
				if (element instanceof IType || element instanceof IMethod) {
					IMember member= (IMember) element;
					IType type= member.getElementType() == IJavaElement.TYPE ? (IType) member : member.getDeclaringType();
					addTypeAndSubtypes(type);
				}
			}
		}

		private void addTypeAndSubtypes(IType type) {
			if (fResult.add(type)) {
				IType[] subclasses= fHierarchy.getSubclasses(type);
				for (IType subclasse : subclasses) {
					addTypeAndSubtypes(subclasse);
				}
			}
		}
	}

	@Override
	public boolean isTest(IType type) throws JavaModelException {
		return internalIsTest(type, null);
	}

	private boolean internalIsTest(IType type, IProgressMonitor monitor) throws JavaModelException {
		if (CoreTestSearchEngine.isAccessibleClass(type)) {
			if (CoreTestSearchEngine.hasSuiteMethod(type)) { // since JUnit 4.3.1
				return true;
			}
			ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
			/* TODO: When bug 156352 is fixed:
			parser.setProject(type.getJavaProject());
			IBinding[] bindings= parser.createBindings(new IJavaElement[] { type }, monitor);
			if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
				ITypeBinding binding= (ITypeBinding) bindings[0];
				return isTest(binding);
			}*/

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

		if (Annotation.RUN_WITH.annotatesTypeOrSuperTypes(binding) || Annotation.TEST.annotatesAtLeastOneMethod(binding)) {
			return true;
		}
		return CoreTestSearchEngine.isTestImplementor(binding);
	}
}
