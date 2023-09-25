/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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

import org.eclipse.swt.graphics.Image;

import org.eclipse.ltk.core.refactoring.Refactoring;

import org.eclipse.jdt.core.ICompilationUnit;


public class RefactoringCorrectionProposal extends LinkedCorrectionProposal {
	public RefactoringCorrectionProposal(String name, ICompilationUnit cu, Refactoring refactoring, int relevance, Image image) {
		super(name, cu, null, relevance, image);
		setDelegate(new RefactoringCorrectionProposalCore(name, cu, refactoring, relevance));
	}
}