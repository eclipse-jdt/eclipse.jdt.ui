/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Mateusz Wenus <mateusz.wenus@gmail.com> - [override method] generate in declaration order [code generation] - https://bugs.eclipse.org/bugs/show_bug.cgi?id=140971
 *     Stephan Herrmann - Contribution for Bug 463360 - [override method][null] generating method override should not create redundant null annotations
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *     Microsoft Corporation - read formatting options from the compilation unit
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.fix;

import java.util.Arrays;
import java.util.function.Predicate;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;
import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.MethodsSourcePositionComparator;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class AddUnimplementedMethodsOperation extends CompilationUnitRewriteOperation {

	private ASTNode fTypeNode;
	private Predicate<IMethodBinding> fMethodFilter;

	/**
	 * Create a {@link AddUnimplementedMethodsOperation}
	 * @param typeNode must be one of the following types:
	 * <ul><li>AnonymousClassDeclaration</li>
	 * <li>AbstractTypeDeclaration</li>
	 * <li>EnumConstantDeclaration</li></ul>
	 * @param methodFilter a filter for methods to ignore when looking for unimplemented methods
	 */
	public AddUnimplementedMethodsOperation(ASTNode typeNode, Predicate<IMethodBinding> methodFilter) {
		this.fTypeNode= typeNode;
		this.fMethodFilter= methodFilter;
	}

	@Override
	public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
		IMethodBinding[] unimplementedMethods= getUnimplementedMethods(fTypeNode);
		if (unimplementedMethods.length == 0)
			return;

		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(fTypeNode, cuRewrite.getImportRewrite());
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		ICompilationUnit unit= cuRewrite.getCu();
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(unit);

		ListRewrite listRewrite;

		if (fTypeNode instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration decl= (AnonymousClassDeclaration) fTypeNode;
			listRewrite= rewrite.getListRewrite(decl, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
			settings.createComments= false;
		} else if (fTypeNode instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration decl= (AbstractTypeDeclaration) fTypeNode;
			listRewrite= rewrite.getListRewrite(decl, decl.getBodyDeclarationsProperty());
		} else if (fTypeNode instanceof EnumConstantDeclaration) {
			EnumConstantDeclaration enumConstantDeclaration= (EnumConstantDeclaration) fTypeNode;
			AnonymousClassDeclaration anonymousClassDeclaration= enumConstantDeclaration.getAnonymousClassDeclaration();
			if (anonymousClassDeclaration == null) {
				anonymousClassDeclaration= rewrite.getAST().newAnonymousClassDeclaration();
				rewrite.set(enumConstantDeclaration, EnumConstantDeclaration.ANONYMOUS_CLASS_DECLARATION_PROPERTY, anonymousClassDeclaration, createTextEditGroup(
						CorrectionMessages.AddUnimplementedMethodsOperation_AddMissingMethod_group, cuRewrite));
			}
			listRewrite= rewrite.getListRewrite(anonymousClassDeclaration, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
			settings.createComments= false;
		} else {
			Assert.isTrue(false, "Unknown type node"); //$NON-NLS-1$
			return;
		}

		ImportRewrite imports= cuRewrite.getImportRewrite();

		for (IMethodBinding curr : unimplementedMethods) {
			MethodDeclaration newMethodDecl= StubUtility2Core.createImplementationStubCore(unit, rewrite, imports, context, curr, curr.getDeclaringClass(), settings, false, fTypeNode, false);
			listRewrite.insertLast(newMethodDecl, createTextEditGroup(CorrectionMessages.AddUnimplementedMethodsOperation_AddMissingMethod_group, cuRewrite));
		}
	}

	@Override
	public String getAdditionalInfo() {
		if (fTypeNode instanceof EnumDeclaration)
			return CorrectionMessages.UnimplementedMethodsCorrectionProposal_enum_info;

		IMethodBinding[] methodsToOverride= getMethodsToImplement();
		StringBuilder buf= new StringBuilder();
		buf.append("<b>"); //$NON-NLS-1$
		if (methodsToOverride.length == 1) {
			buf.append(CorrectionMessages.UnimplementedMethodsCorrectionProposal_info_singular);
		} else {
			buf.append(Messages.format(CorrectionMessages.UnimplementedMethodsCorrectionProposal_info_plural, String.valueOf(methodsToOverride.length)));
		}
		buf.append("</b><ul>"); //$NON-NLS-1$
		for (IMethodBinding element : methodsToOverride) {
			buf.append("<li>"); //$NON-NLS-1$
			buf.append(BindingLabelProviderCore.getBindingLabel(element, JavaElementLabelsCore.ALL_FULLY_QUALIFIED));
			buf.append("</li>"); //$NON-NLS-1$
		}
		buf.append("</ul>"); //$NON-NLS-1$
		return buf.toString();
	}

	public IMethodBinding[] getMethodsToImplement() {
		return getUnimplementedMethods(fTypeNode);
	}

	private IMethodBinding[] getUnimplementedMethods(ASTNode typeNode) {
		ITypeBinding binding= null;
		Predicate<IMethodBinding> filter= StubUtility2Core.ignoreAbstractsOfInput(binding);
		if (typeNode instanceof AnonymousClassDeclaration) {
			AnonymousClassDeclaration decl= (AnonymousClassDeclaration) typeNode;
			binding= decl.resolveBinding();
		} else if (typeNode instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration decl= (AbstractTypeDeclaration) typeNode;
			binding= decl.resolveBinding();
		} else if (typeNode instanceof EnumConstantDeclaration) {
			EnumConstantDeclaration enumConstantDeclaration= (EnumConstantDeclaration) typeNode;
			if (enumConstantDeclaration.getAnonymousClassDeclaration() != null) {
				binding= enumConstantDeclaration.getAnonymousClassDeclaration().resolveBinding();
			} else {
				IVariableBinding varBinding= enumConstantDeclaration.resolveVariable();
				if (varBinding != null) {
					binding= varBinding.getDeclaringClass();
					filter= m->false;
				}
			}
		}
		if (binding == null)
			return new IMethodBinding[0];

		if (fMethodFilter != null) {
			filter= filter.or(fMethodFilter);
		}

		IMethodBinding[] unimplementedMethods= StubUtility2Core.getUnimplementedMethods(binding, filter);
		Arrays.sort(unimplementedMethods, new MethodsSourcePositionComparator(binding));
		return unimplementedMethods;
	}
}
