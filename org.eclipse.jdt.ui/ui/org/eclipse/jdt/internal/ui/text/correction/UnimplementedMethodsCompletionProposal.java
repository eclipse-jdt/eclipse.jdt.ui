/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabels;

public class UnimplementedMethodsCompletionProposal extends ASTRewriteCorrectionProposal {

	private boolean fAnnotations= true;
	private ASTNode fTypeNode;
	private IMethodBinding[] fMethodsToOverride;

	public UnimplementedMethodsCompletionProposal(ICompilationUnit cu, ASTNode typeNode, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		setDisplayName(CorrectionMessages.getString("UnimplementedMethodsCompletionProposal.description"));//$NON-NLS-1$
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		
		fTypeNode= typeNode;
		fMethodsToOverride= null;
	}
	
	public void setGenerateAnnotations(boolean generate) {
		fAnnotations= generate;
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
		
		IJavaProject project= getCompilationUnit().getJavaProject();
		boolean annotations= project.getOption(JavaCore.COMPILER_COMPLIANCE, true).equals(JavaCore.VERSION_1_5) && project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true).equals(JavaCore.VERSION_1_5);
		
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(getCompilationUnit().getJavaProject());
		if (!settings.createComments || binding.isAnonymous()) {
			settings= null;
		}
		
		for (int i= 0; i < methods.length; i++) {
			MethodDeclaration newMethodDecl= StubUtility2.createImplementationStub(getCompilationUnit(), rewrite, getImportRewrite(), ast, methods[i], binding.getName(), settings, fAnnotations && annotations);
			listRewrite.insertLast(newMethodDecl, null);
		}
		return rewrite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		try {
			initializeTextChange(); // force the creation of the rewrite
			StringBuffer buf= new StringBuffer();
			buf.append("<b>"); //$NON-NLS-1$
			buf.append(CorrectionMessages.getFormattedString("UnimplementedMethodsCompletionProposal.info", String.valueOf(fMethodsToOverride.length))); //$NON-NLS-1$
			buf.append("</b><ul>"); //$NON-NLS-1$
			for (int i= 0; i < fMethodsToOverride.length; i++) {
				buf.append("<li>"); //$NON-NLS-1$
				buf.append(BindingLabels.getFullyQualified(fMethodsToOverride[i]));
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