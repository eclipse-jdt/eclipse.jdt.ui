/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.ImplementInterfaceProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class ImplementInterfaceProposalCore extends LinkedCorrectionProposalCore {
	private IBinding fBinding;
	private CompilationUnit fAstRoot;
	private ITypeBinding fNewInterface;

	public ImplementInterfaceProposalCore(ICompilationUnit targetCU, ITypeBinding binding, CompilationUnit astRoot, ITypeBinding newInterface, int relevance) {
		super("", targetCU, null, relevance); //$NON-NLS-1$
		Assert.isTrue(binding != null && Bindings.isDeclarationBinding(binding));

		fBinding= binding;
		fAstRoot= astRoot;
		fNewInterface= newInterface;

		String[] args= { BasicElementLabels.getJavaElementName(binding.getName()), BasicElementLabels.getJavaElementName(Bindings.getRawName(newInterface)) };
		setDisplayName(Messages.format(CorrectionMessages.ImplementInterfaceProposal_name, args));
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		ASTNode boundNode= fAstRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;
		CompilationUnit newRoot= fAstRoot;
		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			newRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		ImportRewrite imports= createImportRewrite(newRoot);

		if (declNode instanceof TypeDeclaration) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);

			ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(declNode, imports);
			Type newInterface= imports.addImport(fNewInterface, ast, importRewriteContext, TypeLocation.OTHER);
			ListRewrite listRewrite= rewrite.getListRewrite(declNode, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
			listRewrite.insertLast(newInterface, null);

			// set up linked mode
			final String KEY_TYPE= "type"; //$NON-NLS-1$
			addLinkedPosition(rewrite.track(newInterface), true, KEY_TYPE);
			return rewrite;
		}
		return null;
	}
}