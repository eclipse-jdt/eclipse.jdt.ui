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

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
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
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class UnimplementedMethodsCorrectionProposal extends ASTRewriteCorrectionProposal {

	private ASTNode fTypeNode;
	private IMethodBinding[] fMethodsToOverride;
	private final int fProblemId;

	public UnimplementedMethodsCorrectionProposal(ICompilationUnit cu, ASTNode typeNode, int problemId, int relevance) {
		super("", cu, null, relevance, null); //$NON-NLS-1$
		fProblemId= problemId;
		setDisplayName(CorrectionMessages.UnimplementedMethodsCorrectionProposal_description);
		setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));

		fTypeNode= typeNode;
		fMethodsToOverride= null;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		AST ast= fTypeNode.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		CompilationUnit root= (CompilationUnit) fTypeNode.getRoot();
		ImportRewrite imports= createImportRewrite(root);
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(root, fTypeNode.getStartPosition(), imports);
		
		if (fTypeNode instanceof EnumDeclaration && fProblemId == IProblem.EnumAbstractMethodMustBeImplemented) {
			EnumDeclaration typeNode= (EnumDeclaration) fTypeNode;
			List enumConstants= typeNode.enumConstants();
			for (int i= 0; i < enumConstants.size(); i++) {
				EnumConstantDeclaration enumConstant= (EnumConstantDeclaration) enumConstants.get(i);
				AnonymousClassDeclaration anonymousClassDeclaration= enumConstant.getAnonymousClassDeclaration();
				if (anonymousClassDeclaration == null) {
					addEnumConstantDeclarationBody(enumConstant, rewrite, imports, context);
				} else {
					addUnimplementedMethods(anonymousClassDeclaration, rewrite, imports, context);
				}
			}
		} else {
			addUnimplementedMethods(fTypeNode, rewrite, imports, context);
		}
		return rewrite;
	}
	
	private void addUnimplementedMethods(ASTNode typeNode, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context) throws CoreException {
		ListRewrite listRewrite;
		ITypeBinding binding;
		if (typeNode instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration decl= (AnonymousClassDeclaration) typeNode;
			binding= decl.resolveBinding();
			listRewrite= rewrite.getListRewrite(decl, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
		} else {
			AbstractTypeDeclaration decl= (AbstractTypeDeclaration) typeNode;
			binding= decl.resolveBinding();
			listRewrite= rewrite.getListRewrite(decl, decl.getBodyDeclarationsProperty());
		}
		if (binding != null) {
			IMethodBinding[] methods= StubUtility2.getUnimplementedMethods(binding);
			fMethodsToOverride= methods;
	
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(getCompilationUnit().getJavaProject());
			if (binding.isAnonymous()) {
				settings.createComments= false;
			}
	
			for (int i= 0; i < methods.length; i++) {
				MethodDeclaration newMethodDecl= StubUtility2.createImplementationStub(getCompilationUnit(), rewrite, imports, context, methods[i], binding.getName(), settings, binding.isInterface());
				listRewrite.insertLast(newMethodDecl, null);
			}
		}
	}
	
	private void addEnumConstantDeclarationBody(EnumConstantDeclaration constDecl, ASTRewrite rewrite, ImportRewrite imports, ImportRewriteContext context) throws CoreException {
		AnonymousClassDeclaration anonymDecl= constDecl.getAST().newAnonymousClassDeclaration();
		rewrite.set(constDecl, EnumConstantDeclaration.ANONYMOUS_CLASS_DECLARATION_PROPERTY, anonymDecl, null);
		IVariableBinding varBinding= constDecl.resolveVariable();
		if (varBinding != null) {
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(getCompilationUnit().getJavaProject());
			settings.createComments= false;
			
			IMethodBinding[] declaredMethods= varBinding.getDeclaringClass().getDeclaredMethods();
			for (int k= 0; k < declaredMethods.length; k++) {
				IMethodBinding curr= declaredMethods[k];
				if (Modifier.isAbstract(curr.getModifiers())) {
					MethodDeclaration newMethodDecl= StubUtility2.createImplementationStub(getCompilationUnit(), rewrite, imports, context, curr, curr.getDeclaringClass().getName(), settings, false);
					anonymDecl.bodyDeclarations().add(newMethodDecl);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		try {
			if (fTypeNode instanceof EnumDeclaration) {
				return CorrectionMessages.UnimplementedMethodsCorrectionProposal_enum_info;
			}
			getChange(); // force the creation of the rewrite
			StringBuffer buf= new StringBuffer();
			buf.append("<b>"); //$NON-NLS-1$
			buf.append(Messages.format(CorrectionMessages.UnimplementedMethodsCorrectionProposal_info, String.valueOf(fMethodsToOverride.length)));
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
