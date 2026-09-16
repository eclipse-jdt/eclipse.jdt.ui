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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.util.TightSourceRangeComputer;

public abstract class AbstractOccurrenceUpdate <N extends ASTNode> {
	protected final CompilationUnitRewrite fCuRewrite;
	protected final TextEditGroup fDescription;
	protected RefactoringStatus fResult;

	public abstract void updateNode() throws CoreException;
    /**
     * @return ListRewrite of parameters or arguments
	*/
    protected abstract ListRewrite getParamgumentsRewrite();
    protected abstract N createNewParamgument(ParameterInfo info, List<ParameterInfo> parameterInfos, List<N> nodes);

	protected AbstractOccurrenceUpdate(CompilationUnitRewrite cuRewrite, TextEditGroup description, RefactoringStatus result) {
		this.fCuRewrite = cuRewrite;
		this.fDescription = description;
		this.fResult = result;
	}

	protected final ASTRewrite getASTRewrite() {
		return fCuRewrite.getASTRewrite();
	}

	/**
	 * @param info the parameter info
	 */
	protected void changeParamgumentName(ParameterInfo info) {
		// no-op
	}

	/**
	 * @param info the parameter info
	 */
	protected void changeParamgumentType(ParameterInfo info) {
		// no-op
	}

	protected final TightSourceRangeComputer getTightSourceRangeComputer() {
		return (TightSourceRangeComputer) fCuRewrite.getASTRewrite().getExtendedSourceRangeComputer();
	}

}
