/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.jdt.internal.corext.refactoring.TypeContextChecker;
import org.eclipse.jdt.internal.corext.util.Messages;

public abstract class AbstractSignatureProcessor extends RefactoringProcessor {

	List<ParameterInfo> fParameterInfos;
	private StubTypeContext fContextCuStartEnd;
	CompilationUnitRewrite fBaseCuRewrite;

	static final String CONST_CLASS_DECL = "class A{";//$NON-NLS-1$
	static final String CONST_ASSIGN = " i=";		//$NON-NLS-1$
	static final String CONST_CLOSE = ";}";			//$NON-NLS-1$

	public abstract int getSourceRangeOffset() throws JavaModelException;
	abstract ICompilationUnit getCu();
	public abstract boolean isSignatureSameAsInitial() throws JavaModelException;
	public abstract IJavaElement getJavaElementContext();
	abstract void checkParameterDefaultValue(RefactoringStatus result, ParameterInfo info);

	/**
	 *
	 * @return List of <code>ParameterInfo</code> objects.
	 */
	public List<ParameterInfo> getParameterInfos() {
		return fParameterInfos;
	}

	public StubTypeContext getStubTypeContext() {
		if (fContextCuStartEnd == null)
			try {
				fContextCuStartEnd= TypeContextChecker.createStubTypeContext(getCu(), fBaseCuRewrite.getRoot(), getSourceRangeOffset());
			} catch (CoreException e) {
				//cannot do anything here
				throw new RuntimeException(e);
			}
		return fContextCuStartEnd;
	}

	/**
	 * If this occurrence update is called from within a declaration update
	 * (i.e., to update the call inside the newly created delegate), the old
	 * node does not yet exist and therefore cannot be a move target.
	 *
	 * Normally, always use createMoveTarget as this has the advantage of
	 * being able to add changes inside changed nodes (for example, a method
	 * call within a method call, see test case #4) and preserving comments
	 * inside calls.
	 * @param oldNode original node
	 * @param rewrite an AST rewrite
	 * @return the node to insert at the target location
	 */
	<T extends ASTNode> T moveNode(T oldNode, ASTRewrite rewrite) {
		T movedNode;
		if (ASTNodes.isExistingNode(oldNode))
			movedNode= ASTNodes.createMoveTarget(rewrite, oldNode); //node must be one of ast
		else
			movedNode= ASTNodes.copySubtree(rewrite.getAST(), oldNode);
		return movedNode;
	}

	boolean isOrderSameAsInitial(){
		int i= 0;
		for (Iterator<ParameterInfo> iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= iter.next();
			if (info.getOldIndex() != i) // includes info.isAdded()
				return false;
			if (info.isDeleted())
				return false;
		}
		return true;
	}

	boolean areParameterTypesSameAsInitial() {
		for (ParameterInfo info : fParameterInfos) {
			if (! info.isAdded() && ! info.isDeleted() && info.isTypeNameChanged())
				return false;
		}
		return true;
	}

	boolean areNamesSameAsInitial() {
		for (ParameterInfo info : fParameterInfos) {
			if (info.isRenamed())
				return false;
		}
		return true;
	}

	void checkParameterNamesAndValues(RefactoringStatus result) {
		int i= 1;
		for (Iterator<ParameterInfo> iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= iter.next();
			if (info.isDeleted())
				continue;
			checkParameterName(result, info, i);
			if (result.hasFatalError())
				return;
			if (info.isAdded())	{
				checkParameterDefaultValue(result, info);
				if (result.hasFatalError())
					return;
			}
		}
	}

	void checkParameterName(RefactoringStatus result, ParameterInfo info, int position) {
		if (info.getNewName().trim().length() == 0) {
			result.addFatalError(Messages.format(
					RefactoringCoreMessages.ChangeSignatureRefactoring_param_name_not_empty, Integer.toString(position)));
		} else {
			result.merge(Checks.checkTempName(info.getNewName(), getJavaElementContext()));
		}
	}
}
