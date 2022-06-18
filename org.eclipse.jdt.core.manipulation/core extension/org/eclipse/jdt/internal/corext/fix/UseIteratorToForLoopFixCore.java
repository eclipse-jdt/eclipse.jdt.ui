/*******************************************************************************
 * Copyright (c) 2021, 2022 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.helper.AbstractTool;
import org.eclipse.jdt.internal.corext.fix.helper.WhileLoopToChangeHit;
import org.eclipse.jdt.internal.corext.fix.helper.WhileToForEach;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public enum UseIteratorToForLoopFixCore {

	LOOP(new WhileToForEach());

	AbstractTool<WhileLoopToChangeHit> iteratortofor;

	@SuppressWarnings("unchecked")
	UseIteratorToForLoopFixCore(AbstractTool<? extends WhileLoopToChangeHit> explicitencoding) {
		this.iteratortofor= (AbstractTool<WhileLoopToChangeHit>) explicitencoding;
	}

	public String getPreview(boolean i) {
		return iteratortofor.getPreview(i);
	}

	/**
	 * Compute set of CompilationUnitRewriteOperation to refactor supported situations
	 *
	 * @param compilationUnit unit to search in
	 * @param operations set of all CompilationUnitRewriteOperations created already
	 * @param nodesprocessed list to remember nodes already processed
	 * @param createForOnlyIfVarUsed true if for loop should be created only only if loop var used within
	 */
	public void findOperations(final CompilationUnit compilationUnit, final Set<CompilationUnitRewriteOperation> operations,
			final Set<ASTNode> nodesprocessed, boolean createForOnlyIfVarUsed) {
		iteratortofor.find(this, compilationUnit, operations, nodesprocessed, createForOnlyIfVarUsed);
	}

	public CompilationUnitRewriteOperation rewrite(final WhileLoopToChangeHit hit) {
		return new CompilationUnitRewriteOperation() {
			@Override
			public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
				TextEditGroup group= createTextEditGroup(MultiFixMessages.Java50CleanUp_ConvertToEnhancedForLoop_description, cuRewrite);
				TightSourceRangeComputer rangeComputer;
				ASTRewrite rewrite= cuRewrite.getASTRewrite();
				if (rewrite.getExtendedSourceRangeComputer() instanceof TightSourceRangeComputer) {
					rangeComputer= (TightSourceRangeComputer)rewrite.getExtendedSourceRangeComputer();
				} else {
					rangeComputer= new TightSourceRangeComputer();
				}
				rangeComputer.addTightSourceNode(hit.whileStatement);
				rewrite.setTargetSourceRangeComputer(rangeComputer);
				iteratortofor.rewrite(UseIteratorToForLoopFixCore.this, hit, cuRewrite, group);
			}
		};
	}

}
