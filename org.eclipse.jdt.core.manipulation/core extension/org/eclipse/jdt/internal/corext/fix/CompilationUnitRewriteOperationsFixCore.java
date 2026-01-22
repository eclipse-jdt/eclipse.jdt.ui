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
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;

public class CompilationUnitRewriteOperationsFixCore extends AbstractFixCore {

	public static final String UNTOUCH_COMMENT_PROPERTY= "untouchComment"; //$NON-NLS-1$
	public abstract static class CompilationUnitRewriteOperationWithSourceRange extends CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation {
		public abstract void rewriteASTInternal(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException;

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			cuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode node) {
					if (Boolean.TRUE.equals(node.getProperty(UNTOUCH_COMMENT_PROPERTY))) {
						return new SourceRange(node.getStartPosition(), node.getLength());
					}

					return super.computeSourceRange(node);
				}
			});
			rewriteASTInternal(cuRewrite, linkedModel);
		}
	}

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
	private ImportRemover fSharedImportRemover;

	public CompilationUnitRewriteOperationsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		this(name, compilationUnit, new CompilationUnitRewriteOperation[] { operation });
		Assert.isNotNull(operation);
	}

	public CompilationUnitRewriteOperationsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		this(name, compilationUnit, operations, new LinkedProposalModelCore());
	}

	public CompilationUnitRewriteOperationsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations, LinkedProposalModelCore proposalModel) {
		super(name);
		Assert.isNotNull(operations);
		Assert.isLegal(operations.length > 0);
		fCompilationUnit= compilationUnit;
		fOperations= operations;
		fLinkedProposalModel= proposalModel != null ? proposalModel : new LinkedProposalModelCore();
	}

	@Override
	public LinkedProposalModelCore getLinkedPositions() {
		if (!fLinkedProposalModel.hasLinkedPositions())
			return null;

		return fLinkedProposalModel;
	}

	/**
	 * Sets the shared ImportRemover to be used by this fix.
	 * This allows multiple fixes to share a single ImportRemover instance.
	 *
	 * @param remover the ImportRemover to share
	 * @since 1.13
	 */
	public void setSharedImportRemover(ImportRemover remover) {
		fSharedImportRemover = remover;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		CompilationUnitRewrite cuRewrite= new CompilationUnitRewrite((ICompilationUnit)fCompilationUnit.getJavaElement(), fCompilationUnit);

		// Use shared ImportRemover if available
		if (fSharedImportRemover != null) {
			cuRewrite.setImportRemover(fSharedImportRemover);
		}

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
