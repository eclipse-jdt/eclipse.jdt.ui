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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class TypeArgumentMismatchSubProcessor extends TypeArgumentMismatchBaseSubProcessor<ICommandAccess>{

//	public static void getTypeParameterMismatchProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
//	CompilationUnit astRoot= context.getASTRoot();
//	ASTNode selectedNode= problem.getCoveredNode(astRoot);
//	if (!(selectedNode instanceof SimpleName)) {
//	return;
//	}

//	ASTNode normalizedNode= ASTNodes.getNormalizedNode(selectedNode);
//	if (!(normalizedNode instanceof ParameterizedType)) {
//	return;
//	}
//	// waiting for result of https://bugs.eclipse.org/bugs/show_bug.cgi?id=81544


//	}

	public static void removeMismatchedArguments(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals){
		new TypeArgumentMismatchSubProcessor().addRemoveMismatchedArgumentProposals(context, problem, proposals);
	}

	private TypeArgumentMismatchSubProcessor() {
	}

	@Override
	public ICommandAccess astRewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore core) {
		return new ASTRewriteCorrectionProposal(core, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

}
