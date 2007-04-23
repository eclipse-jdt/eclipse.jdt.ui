/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.SourceRange;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;


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
		
		private boolean annotates(IAnnotationBinding[] annotations) throws JavaModelException {
			for (int i= 0; i < annotations.length; i++) {
				ITypeBinding annotationType= annotations[i].getAnnotationType();
				if (annotationType != null && (annotationType.getQualifiedName().equals(fName))) {
					return true;
				}
			}
			return  false;
		}
		
		public boolean annotatesTypeOrSuperTypes(ITypeBinding type) throws JavaModelException {
			while (type != null) {
				if (annotates(type.getAnnotations())) {
					return true;
				}
				type= type.getSuperclass();
			}
			return false;
		}
		
		public boolean annotatesAtLeastOneMethod(ITypeBinding type) throws JavaModelException {
			while (type != null) {
				IMethodBinding[] declaredMethods= type.getDeclaredMethods();
				for (int i= 0; i < declaredMethods.length; i++) {
					IMethodBinding curr= declaredMethods[i];
					if (annotates(curr.getAnnotations())) {
						return true;
					}
				}
				type= type.getSuperclass();
			}
			return false;
		}
	}

	public void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
		if (element == null || result == null) {
			throw new IllegalArgumentException();
		}
		
		if (element instanceof IType) {
			if (internalIsTest((IType) element, pm)) {
				result.add(element);
				return;
			}
		}

		if (pm == null)
			pm= new NullProgressMonitor();
		
		try {
			pm.beginTask(JUnitMessages.JUnit4TestFinder_searching_description, 4);
			
			IRegion region= TestSearchEngine.getRegion(element);
			ITypeHierarchy hierarchy= JavaCore.newTypeHierarchy(region, null, new SubProgressMonitor(pm, 1));
			IType[] allClasses= hierarchy.getAllClasses();
			
			// search for all types with references to RunWith and Test and all subclasses
			HashSet candidates= new HashSet(allClasses.length);
			SearchRequestor requestor= new AnnotationSearchRequestor(hierarchy, candidates);
			
			IJavaSearchScope scope= SearchEngine.createJavaSearchScope(allClasses, IJavaSearchScope.SOURCES);
			int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
			SearchPattern runWithPattern= SearchPattern.createPattern(Annotation.RUN_WITH.getName(), IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.REFERENCES, matchRule);
			SearchPattern testPattern= SearchPattern.createPattern(Annotation.TEST.getName(), IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.REFERENCES, matchRule);
			
			// TODO: Core bug (no results with OR pattern):
//			SearchPattern annotationsPattern= SearchPattern.createOrPattern(runWithPattern, testPattern);
//			SearchParticipant[] searchParticipants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
//			new SearchEngine().search(annotationsPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 2));

			SearchParticipant[] searchParticipants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
			new SearchEngine().search(runWithPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 1));
			new SearchEngine().search(testPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 1));
			
			// find all classes in the region
			for (Iterator iterator= candidates.iterator(); iterator.hasNext();) {
				IType curr= (IType) iterator.next();
				if (TestSearchEngine.isAccessibleClass(curr) && !Flags.isAbstract(curr.getFlags()) && region.contains(curr)) {
					result.add(curr);
				}
			}
			
			// add all classes implementing JUnit 3.8's Test interface in the region
			IType testInterface= element.getJavaProject().findType(JUnitPlugin.TEST_INTERFACE_NAME);
			if (testInterface != null) {
				TestSearchEngine.findTestImplementorClasses(hierarchy, testInterface, region, result);
			}
			
			//JUnit 4.3 can also run JUnit-3.8-style public static Test suite() methods: 
			TestSearchEngine.findSuiteMethods(element, result, new SubProgressMonitor(pm, 1));
		} finally {
			pm.done();
		}
	}

	private static class AnnotationSearchRequestor extends SearchRequestor {
		
		private final Collection fResult;
		private final ITypeHierarchy fHierarchy;

		public AnnotationSearchRequestor(ITypeHierarchy hierarchy, Collection result) {
			fHierarchy= hierarchy;
			fResult= result;
		}

		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
				Object element= match.getElement();
				if (element instanceof IType || element instanceof IMethod) {
					IMember member= (IMember) element;
					if (member.getNameRange().getOffset() > match.getOffset()) {
						IType type= member.getElementType() == IJavaElement.TYPE ? (IType) member : member.getDeclaringType();
						addTypeAndSubtypes(type);
					}
				}
			}
		}

		private void addTypeAndSubtypes(IType type) {
			if (fResult.add(type)) {
				IType[] subclasses= fHierarchy.getSubclasses(type);
				for (int i= 0; i < subclasses.length; i++) {
					addTypeAndSubtypes(subclasses[i]);
				}
			}
		}
	}

	public boolean isTest(IType type) throws JavaModelException {
		return internalIsTest(type, null);
	}
	
	private boolean internalIsTest(IType type, IProgressMonitor monitor) throws JavaModelException {
		if (TestSearchEngine.isAccessibleClass(type)) {
			if (TestSearchEngine.hasSuiteMethod(type)) { // since JUnit 4.3.1
				return true;
			}
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			/* TODO: When bug 156352 is fixed:
			parser.setProject(type.getJavaProject());
			IBinding[] bindings= parser.createBindings(new IJavaElement[] { type }, monitor);
			if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
				ITypeBinding binding= (ITypeBinding) bindings[0];
				return isTest(binding);
			}*/
			
			if (type.getCompilationUnit() != null) {
				parser.setSource(type.getCompilationUnit());
			} else if (! SourceRange.isAvailable(type.getSourceRange())) { // class file with no source
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
			if (node instanceof TypeDeclaration) {
				ITypeBinding binding= ((TypeDeclaration) node).resolveBinding();
				if (binding != null) {
					return isTest(binding);
				}
			}
		}
		return false;
		
	}
	
	
	private boolean isTest(ITypeBinding binding) throws JavaModelException {
		if (Modifier.isAbstract(binding.getModifiers()))
			return false;
		
		if (Annotation.RUN_WITH.annotatesTypeOrSuperTypes(binding) || Annotation.TEST.annotatesAtLeastOneMethod(binding)) {
			return true;
		}
		return TestSearchEngine.isTestImplementor(binding);
	}
}
