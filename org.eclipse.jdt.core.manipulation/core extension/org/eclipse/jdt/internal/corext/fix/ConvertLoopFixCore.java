/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified to create core class
 *******************************************************************************/
/**
 *
 **/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class ConvertLoopFixCore extends CompilationUnitRewriteOperationsFixCore {

	public final static class ControlStatementFinder extends GenericVisitor {

		private final List<ConvertLoopOperation> fResult;
		private final Hashtable<ForStatement, String> fUsedNames;
		private final boolean fFindForLoopsToConvert;
		private final boolean fConvertIterableForLoops;
		private final boolean fMakeFinal;
		private final boolean fCheckIfLoopVarUsed;
		private ConvertLoopOperation fParentOperation;

		public ControlStatementFinder(boolean findForLoopsToConvert, boolean convertIterableForLoops, boolean makeFinal,
				boolean checkIfLoopVarUsed, List<ConvertLoopOperation> resultingCollection) {
			fFindForLoopsToConvert= findForLoopsToConvert;
			fConvertIterableForLoops= convertIterableForLoops;
			fMakeFinal= makeFinal;
			fCheckIfLoopVarUsed= checkIfLoopVarUsed;
			fResult= resultingCollection;
			fUsedNames= new Hashtable<>();
			fParentOperation= null;
		}

		@Override
		public boolean visit(ForStatement node) {
			if (fFindForLoopsToConvert || fConvertIterableForLoops) {
				ForStatement current= node;
				ConvertLoopOperation operation= getConvertOperation(current);
				if (fParentOperation != null) {
					fParentOperation.setChildLoopOperation(operation);
				}
				fParentOperation= operation;
				ConvertLoopOperation oldOperation= null;
				while (operation != null) {
					if (oldOperation == null) {
						fResult.add(operation);
					} else {
						oldOperation.setBodyConverter(operation);
					}

					if (current.getBody() instanceof ForStatement) {
						current= (ForStatement)current.getBody();
						oldOperation= operation;
						operation= getConvertOperation(current);
						oldOperation.setChildLoopOperation(operation);
						fParentOperation= operation;
					} else {
						operation= null;
					}
				}
				current.getBody().accept(this);
				return false;
			}

			return super.visit(node);
		}

		private ConvertLoopOperation getConvertOperation(ForStatement node) {

			Collection<String> usedNamesCollection= fUsedNames.values();
			String[] usedNames= usedNamesCollection.toArray(new String[usedNamesCollection.size()]);
			ConvertLoopOperation convertForLoopOperation= new ConvertForLoopOperation(node, usedNames, fMakeFinal, fCheckIfLoopVarUsed);
			if (convertForLoopOperation.satisfiesPreconditions().isOK()) {
				if (fFindForLoopsToConvert) {
					fUsedNames.put(node, convertForLoopOperation.getIntroducedVariableName());
					return convertForLoopOperation;
				}
			} else if (fConvertIterableForLoops) {
				ConvertLoopOperation iterableConverter= new ConvertIterableLoopOperation(node, usedNames, fMakeFinal, fCheckIfLoopVarUsed);
				if (iterableConverter.satisfiesPreconditions().isOK()) {
					fUsedNames.put(node, iterableConverter.getIntroducedVariableName());
					return iterableConverter;
				}
			}

			return null;
		}

		@Override
		public void endVisit(ForStatement node) {
			if (fFindForLoopsToConvert || fConvertIterableForLoops) {
				fUsedNames.remove(node);
			}
			super.endVisit(node);
		}

	}

	public static ICleanUpFixCore createCleanUp(CompilationUnit compilationUnit, boolean convertForLoops, boolean convertIterableForLoops,
			boolean makeFinal, boolean checkIfLoopVarUsed) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		if (!convertForLoops && !convertIterableForLoops)
			return null;

		List<ConvertLoopOperation> operations= new ArrayList<>();
		ControlStatementFinder finder= new ControlStatementFinder(convertForLoops, convertIterableForLoops, makeFinal, checkIfLoopVarUsed, operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[operations.size()]);
		return new ConvertLoopFixCore(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops, null);
	}

	public static ConvertLoopFixCore createConvertForLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertLoopOperation convertForLoopOperation= new ConvertForLoopOperation(loop);
		if (!convertForLoopOperation.satisfiesPreconditions().isOK())
			return null;

		return new ConvertLoopFixCore(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {convertForLoopOperation}, null);
	}

	public static ConvertLoopFixCore createConvertIterableLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopOperation loopConverter= new ConvertIterableLoopOperation(loop);
		IStatus status= loopConverter.satisfiesPreconditions();
		if (status.getSeverity() == IStatus.ERROR)
			return null;

		return new ConvertLoopFixCore(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {loopConverter}, status);
	}

	private final IStatus fStatus;

	protected ConvertLoopFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations, IStatus status) {
		super(name, compilationUnit, fixRewriteOperations);
		fStatus= status;
	}

	@Override
	public IStatus getStatus() {
		return fStatus;
	}

}
