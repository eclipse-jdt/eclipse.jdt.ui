/*******************************************************************************
 * Copyright (c) 2014, 2018 Yatta Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lukas Hanke <hanke@yatta.de> - Bug 241696 [quick fix] quickfix to iterate over a collection - https://bugs.eclipse.org/bugs/show_bug.cgi?id=241696
 *     Lukas Hanke <hanke@yatta.de> - Bug 430818 [1.8][quick fix] Quick fix for "for loop" is not shown for bare local variable/argument/field - https://bugs.eclipse.org/bugs/show_bug.cgi?id=430818
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.IProposalRelevance;

/**
 * Generates a proposal for quick assist, to loop over a variable or method result which represents
 * an {@link Iterable} or an array.
 */
public class GenerateForLoopAssistProposal extends LinkedCorrectionProposal {

	public static final int GENERATE_FOREACH= GenerateForLoopAssistProposalCore.GENERATE_FOREACH;
	public static final int GENERATE_ITERATOR_FOR= GenerateForLoopAssistProposalCore.GENERATE_ITERATOR_FOR;
	public static final int GENERATE_ITERATE_ARRAY= GenerateForLoopAssistProposalCore.GENERATE_ITERATE_ARRAY;
	public static final int GENERATE_ITERATE_LIST= GenerateForLoopAssistProposalCore.GENERATE_ITERATE_LIST;

	/**
	 * Creates an instance of a {@link GenerateForLoopAssistProposal}.
	 *
	 * @param cu the current {@link ICompilationUnit}
	 * @param currentStatement the {@link ExpressionStatement} representing the statement on which
	 *            the assist was called
	 * @param loopTypeToGenerate the type of the loop to generate, possible values are
	 *            {@link GenerateForLoopAssistProposal#GENERATE_FOREACH},
	 *            {@link GenerateForLoopAssistProposal#GENERATE_ITERATOR_FOR} or
	 *            {@link GenerateForLoopAssistProposal#GENERATE_ITERATE_ARRAY}
	 */
	public GenerateForLoopAssistProposal(ICompilationUnit cu, ExpressionStatement currentStatement, int loopTypeToGenerate) {
		super("", cu, null, IProposalRelevance.GENERATE_FOR_LOOP, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$
		setDelegate(new GenerateForLoopAssistProposalCore(cu, currentStatement, loopTypeToGenerate));
	}
}