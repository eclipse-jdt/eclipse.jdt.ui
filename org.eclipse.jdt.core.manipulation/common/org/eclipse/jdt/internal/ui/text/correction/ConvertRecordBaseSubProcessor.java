/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore;

import org.eclipse.jdt.internal.corext.refactoring.code.ConvertToRecordRefactoring;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

/**
 * Subprocessor for converting a type to record.
 *
 */
public abstract class ConvertRecordBaseSubProcessor<T> {
	public ConvertRecordBaseSubProcessor() {
	}

	final static int CONVERT_RECORD= 0;

	/**
	 * Adds proposal to change a type into a record if possible.
	 *
	 * @param context the invocation context
	 * @param proposals the proposal collection to extend
	 * @return true if proposal is possible, false otherwise
	 */
	public boolean addConvertToRecordProposals(final IInvocationContext context, final ASTNode node, final Collection<T> proposals) {

		Assert.isNotNull(context);

		ConvertToRecordRefactoring refactoring= new ConvertToRecordRefactoring(context.getCompilationUnit(), (CompilationUnit) node.getRoot(), node.getStartPosition(), node.getLength());
		try {
			if (refactoring.checkAllConditions(null).isOK()) {
				if (proposals == null) {
					return true;
				}
				Change change= refactoring.createChange(new NullProgressMonitor());
				ChangeCorrectionProposalCore core= new ChangeCorrectionProposalCore(refactoring.getName(), change, IProposalRelevance.CONVERT_TO_RECORD);
				proposals.add(changeCorrectionProposalToT(core, CONVERT_RECORD));
				return true;
			}
		} catch (OperationCanceledException | CoreException e) {
			// do nothing
		}
		return false;
	}

	protected abstract T changeCorrectionProposalToT(ChangeCorrectionProposalCore core, int uid);

}
