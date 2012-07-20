/*******************************************************************************
 * Copyright (c) 2011, 2012 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - [quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.NullAnnotationsFix;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.NullAnnotationsCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

/**
 * Quick Fixes for null-annotation related problems.
 */
public class NullAnnotationsCorrectionProcessor {

	public static void addReturnAndArgumentTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);

		boolean isArgumentProblem= NullAnnotationsFix.isComplainingAboutArgument(selectedNode);
		if (isArgumentProblem || NullAnnotationsFix.isComplainingAboutReturn(selectedNode))
			addNullAnnotationInSignatureProposal(context, problem, proposals, false, isArgumentProblem);
	}

	public static void addNullAnnotationInSignatureProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals, boolean modifyOverridden,
			boolean isArgumentProblem) {
		NullAnnotationsFix fix= NullAnnotationsFix.createNullAnnotationInSignatureFix(context.getASTRoot(), problem, modifyOverridden, isArgumentProblem);

		if (fix != null) {
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			Map<String, String> options= new Hashtable<String, String>();
			if (fix.getCu() != context.getASTRoot()) {
				// workaround: adjust the unit to operate on, depending on the findings of RewriteOperations.createAddAnnotationOperation(..)
				final CompilationUnit cu= fix.getCu();
				final IInvocationContext originalContext= context;
				context= new IInvocationContext() {
					public int getSelectionOffset() {
						return originalContext.getSelectionOffset();
					}

					public int getSelectionLength() {
						return originalContext.getSelectionLength();
					}

					public ASTNode getCoveringNode() {
						return originalContext.getCoveringNode();
					}

					public ASTNode getCoveredNode() {
						return originalContext.getCoveredNode();
					}

					public ICompilationUnit getCompilationUnit() {
						return (ICompilationUnit) cu.getJavaElement();
					}

					public CompilationUnit getASTRoot() {
						return cu;
					}
				};
			}
			int relevance= modifyOverridden ? 9 : 10; //raise local change above change in overridden method
			FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new NullAnnotationsCleanUp(options, problem.getProblemId()), relevance, image, context);
			proposals.add(proposal);
		}
	}
}
