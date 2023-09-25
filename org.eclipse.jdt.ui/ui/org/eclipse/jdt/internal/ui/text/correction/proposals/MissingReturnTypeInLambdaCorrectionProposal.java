/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;

public class MissingReturnTypeInLambdaCorrectionProposal extends MissingReturnTypeCorrectionProposal {

	public MissingReturnTypeInLambdaCorrectionProposal(ICompilationUnit cu, LambdaExpression lambda, ReturnStatement existingReturn, int relevance) {
		super(cu, null, existingReturn, relevance);
		setDelegate(new MissingReturnTypeInLambdaCorrectionProposalCore(cu, lambda, existingReturn, relevance));
	}
}