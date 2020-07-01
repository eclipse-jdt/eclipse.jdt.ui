/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - copied over to jdt.core.manipulation and renamed
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class CompilationUnitRewriteOperationsFixCore extends AbstractFix {

	public abstract static class CompilationUnitRewriteOperation {

		public abstract void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException;

		protected Type importType(final ITypeBinding toImport, final ASTNode accessor, ImportRewrite imports, final CompilationUnit compilationUnit, TypeLocation location) {
			ImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(compilationUnit, accessor.getStartPosition(), imports);
			return imports.addImport(toImport, compilationUnit.getAST(), importContext, location);
		}

		protected TextEditGroup createTextEditGroup(String label, CompilationUnitRewrite rewrite) {
			if (label.length() > 0) {
				return rewrite.createCategorizedGroupDescription(label, new GroupCategorySet(new GroupCategory(label, label, label)));
			} else {
				return rewrite.createGroupDescription(label);
			}
		}

		public String getAdditionalInfo() {
			return null;
		}
	}

	private final CompilationUnitRewriteOperation[] fOperations;
	private final CompilationUnit fCompilationUnit;
	protected LinkedProposalModelCore fLinkedProposalModel;

	public CompilationUnitRewriteOperationsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		this(name, compilationUnit, new CompilationUnitRewriteOperation[] { operation });
		Assert.isNotNull(operation);
	}

	public CompilationUnitRewriteOperationsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		super(name);
		Assert.isNotNull(operations);
		Assert.isLegal(operations.length > 0);
		fCompilationUnit= compilationUnit;
		fOperations= operations;
		fLinkedProposalModel= new LinkedProposalModelCore();
	}

	@Override
	public LinkedProposalModelCore getLinkedPositionsCore() {
		if (!fLinkedProposalModel.hasLinkedPositions())
			return null;

		return fLinkedProposalModel;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite((ICompilationUnit)fCompilationUnit.getJavaElement(), fCompilationUnit);

		fLinkedProposalModel.clear();
		for (CompilationUnitRewriteOperation operation : fOperations) {
			operation.rewriteAST(cuRewrite, fLinkedProposalModel);
		}

		CompilationUnitChange result= cuRewrite.createChange(getDisplayString(), true, null);
		if (result == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, Messages.format(FixMessages.CompilationUnitRewriteOperationsFix_nullChangeError, getDisplayString())));

		return result;
	}

	@Override
	public String getAdditionalProposalInfo(){
		StringBuilder sb= new StringBuilder();
		for (CompilationUnitRewriteOperation operation : fOperations) {
			String info= operation.getAdditionalInfo();
			if (info != null)
				sb.append(info);
		}

		if (sb.length() == 0)
			return null;

		return sb.toString();
	}

}
