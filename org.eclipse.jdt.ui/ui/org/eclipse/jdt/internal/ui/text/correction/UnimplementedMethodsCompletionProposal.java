/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class UnimplementedMethodsCompletionProposal extends ASTRewriteCorrectionProposal {

	private ASTNode fTypeNode;
	private IMethodBinding[] fMethodsToOverride;

	public UnimplementedMethodsCompletionProposal(ICompilationUnit cu, ASTNode typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		setDisplayName(CorrectionMessages.UnimplementedMethodsCompletionProposal_description);
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));

		fTypeNode= typeNode;
		fMethodsToOverride= null;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		ITypeBinding binding;
		AST ast= fTypeNode.getAST();

		ASTRewrite rewrite= ASTRewrite.create(ast);
		ListRewrite listRewrite;
		if (fTypeNode instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration decl= (AnonymousClassDeclaration) fTypeNode;
			binding= decl.resolveBinding();
			listRewrite= rewrite.getListRewrite(decl, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
		} else {
			AbstractTypeDeclaration decl= (AbstractTypeDeclaration) fTypeNode;
			binding= decl.resolveBinding();
			listRewrite= rewrite.getListRewrite(decl, decl.getBodyDeclarationsProperty());
		}
		IMethodBinding[] methods= StubUtility2.getUnimplementedMethods(binding);
		fMethodsToOverride= methods;

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(getCompilationUnit().getJavaProject());
		if (binding.isAnonymous()) {
			settings.createComments= false;
		}
		ImportRewrite imports= createImportRewrite((CompilationUnit) fTypeNode.getRoot());
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext((CompilationUnit) fTypeNode.getRoot(), fTypeNode.getStartPosition(), imports);
		for (int i= 0; i < methods.length; i++) {
			MethodDeclaration newMethodDecl= StubUtility2.createImplementationStub(getCompilationUnit(), rewrite, imports, context, methods[i], binding.getName(), settings, binding.isInterface());
			listRewrite.insertLast(newMethodDecl, null);
		}
		return rewrite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		try {
			getChange(); // force the creation of the rewrite
			StringBuffer buf= new StringBuffer();
			buf.append("<b>"); //$NON-NLS-1$
			buf.append(Messages.format(CorrectionMessages.UnimplementedMethodsCompletionProposal_info, String.valueOf(fMethodsToOverride.length)));
			buf.append("</b><ul>"); //$NON-NLS-1$
			for (int i= 0; i < fMethodsToOverride.length; i++) {
				buf.append("<li>"); //$NON-NLS-1$
				buf.append(BindingLabelProvider.getBindingLabel(fMethodsToOverride[i], JavaElementLabels.ALL_FULLY_QUALIFIED));
				buf.append("</li>"); //$NON-NLS-1$
			}
			buf.append("</ul>"); //$NON-NLS-1$
			return buf.toString();
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
}
