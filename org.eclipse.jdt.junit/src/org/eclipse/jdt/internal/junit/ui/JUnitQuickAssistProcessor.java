/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

public class JUnitQuickAssistProcessor implements IQuickAssistProcessor {

	private static final String JUNIT4_IGNORE_ANNOTATION = "org.junit.Ignore"; //$NON-NLS-1$
	private static final String JUNIT5_DISABLED_ANNOTATION = "org.junit.jupiter.api.Disabled"; //$NON-NLS-1$
	private static final String JUNIT4_TEST_ANNOTATION = "org.junit.Test"; //$NON-NLS-1$
	private static final String JUNIT5_TEST_ANNOTATION = "org.junit.jupiter.api.Test"; //$NON-NLS-1$

	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ASTNode coveringNode = context.getCoveringNode();
		if (coveringNode == null) {
			return false;
		}

		MethodDeclaration methodDecl = getMethodDeclaration(coveringNode);
		if (methodDecl == null) {
			return false;
		}

		return isJUnitTestMethod(methodDecl);
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		ASTNode coveringNode = context.getCoveringNode();
		if (coveringNode == null) {
			return null;
		}

		MethodDeclaration methodDecl = getMethodDeclaration(coveringNode);
		if (methodDecl == null) {
			return null;
		}

		if (!isJUnitTestMethod(methodDecl)) {
			return null;
		}

		List<IJavaCompletionProposal> proposals = new ArrayList<>();

		boolean hasDisabledAnnotation = hasAnnotation(methodDecl, JUNIT5_DISABLED_ANNOTATION);
		boolean hasIgnoreAnnotation = hasAnnotation(methodDecl, JUNIT4_IGNORE_ANNOTATION);

		if (hasDisabledAnnotation || hasIgnoreAnnotation) {
			// Offer to remove the annotation
			String annotationToRemove = hasDisabledAnnotation ? JUNIT5_DISABLED_ANNOTATION : JUNIT4_IGNORE_ANNOTATION;
			proposals.add(new RemoveAnnotationProposal(context, methodDecl, annotationToRemove));
		} else {
			// Offer to add the appropriate annotation based on JUnit version
			if (hasAnnotation(methodDecl, JUNIT5_TEST_ANNOTATION)) {
				// JUnit 5 test
				proposals.add(new AddAnnotationProposal(context, methodDecl, JUNIT5_DISABLED_ANNOTATION, "Disabled"));
			} else if (hasAnnotation(methodDecl, JUNIT4_TEST_ANNOTATION)) {
				// JUnit 4 test
				proposals.add(new AddAnnotationProposal(context, methodDecl, JUNIT4_IGNORE_ANNOTATION, "Ignore"));
			}
		}

		if (proposals.isEmpty()) {
			return null;
		}

		return proposals.toArray(new IJavaCompletionProposal[proposals.size()]);
	}

	private MethodDeclaration getMethodDeclaration(ASTNode node) {
		while (node != null && !(node instanceof MethodDeclaration)) {
			node = node.getParent();
		}
		return (MethodDeclaration) node;
	}

	private boolean isJUnitTestMethod(MethodDeclaration methodDecl) {
		IMethodBinding binding = methodDecl.resolveBinding();
		if (binding == null) {
			return false;
		}

		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			ITypeBinding annotationType = annotation.getAnnotationType();
			if (annotationType != null) {
				String qualifiedName = annotationType.getQualifiedName();
				if (JUNIT4_TEST_ANNOTATION.equals(qualifiedName) || 
					JUNIT5_TEST_ANNOTATION.equals(qualifiedName) ||
					JUnitCorePlugin.JUNIT4_ANNOTATION_NAME.equals(qualifiedName)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean hasAnnotation(MethodDeclaration methodDecl, String annotationQualifiedName) {
		IMethodBinding binding = methodDecl.resolveBinding();
		if (binding == null) {
			return false;
		}

		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			ITypeBinding annotationType = annotation.getAnnotationType();
			if (annotationType != null && annotationQualifiedName.equals(annotationType.getQualifiedName())) {
				return true;
			}
		}

		return false;
	}
}
