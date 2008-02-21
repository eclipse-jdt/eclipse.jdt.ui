/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;

import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * This annotation hover shows the description of the
 * selected java annotation.
 *
 * XXX: Currently this problem hover only works for
 *		Java problems.
 *		see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=62081
 *
 * @since 3.0
 */
public class ProblemHover extends AbstractAnnotationHover {

	public ProblemHover() {
		super(false);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover#calculateProposals(org.eclipse.jface.text.source.Annotation, org.eclipse.jface.text.Position)
	 * @since 3.4
	 */
	protected ICompletionProposal[] getCompletionProposals(Annotation annotation, Position position) {
		ProblemLocation location= new ProblemLocation(position.getOffset(), position.getLength(), (IJavaAnnotation) annotation);
		ICompilationUnit cu= ((IJavaAnnotation) annotation).getCompilationUnit();

		IInvocationContext context= new AssistContext(cu, location.getOffset(), location.getLength());
		if (!SpellingAnnotation.TYPE.equals(annotation.getType()) && !hasProblem(context.getASTRoot().getProblems(), location))
			return new ICompletionProposal[0];

		ArrayList proposals= new ArrayList();
		JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { location }, proposals);
		Collections.sort(proposals, new CompletionProposalComparator());

		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private static boolean hasProblem(IProblem[] problems, IProblemLocation location) {
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (problem.getID() == location.getProblemId() && problem.getSourceStart() == location.getOffset())
				return true;
		}
		return false;
	}

}
