/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

public class MissingAnnotationAttributesProposalOperation extends CompilationUnitRewriteOperation {

	private Annotation fAnnotation;

	public MissingAnnotationAttributesProposalOperation(Annotation annotation) {
		fAnnotation= annotation;
		Assert.isNotNull(annotation.resolveTypeBinding());
	}

	@Override
	public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();

		AST ast= fAnnotation.getAST();

		ListRewrite listRewrite;
		if (fAnnotation instanceof NormalAnnotation) {
			listRewrite= rewrite.getListRewrite(fAnnotation, NormalAnnotation.VALUES_PROPERTY);
		} else {
			NormalAnnotation newAnnotation= ast.newNormalAnnotation();
			newAnnotation.setTypeName((Name) rewrite.createMoveTarget(fAnnotation.getTypeName()));
			rewrite.replace(fAnnotation, newAnnotation, null);

			listRewrite= rewrite.getListRewrite(newAnnotation, NormalAnnotation.VALUES_PROPERTY);
		}
		addMissingAtributes(fAnnotation.resolveTypeBinding(), listRewrite, cuRewrite, linkedModel);

	}

	private void addMissingAtributes(ITypeBinding binding, ListRewrite listRewriter, CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) {
		Set<String> implementedAttribs= new HashSet<>();
		if (fAnnotation instanceof NormalAnnotation) {
			List<MemberValuePair> list= ((NormalAnnotation) fAnnotation).values();
			for (MemberValuePair curr : list) {
				implementedAttribs.add(curr.getName().getIdentifier());
			}
		} else if (fAnnotation instanceof SingleMemberAnnotation) {
			implementedAttribs.add("value"); //$NON-NLS-1$
		}
		ASTRewrite rewriter= listRewriter.getASTRewrite();
		AST ast= rewriter.getAST();
		ImportRewriteContext context= null;
		ASTNode bodyDeclaration= ASTResolving.findParentBodyDeclaration(listRewriter.getParent());
		if (bodyDeclaration != null) {
			context= new ContextSensitiveImportRewriteContext(bodyDeclaration, cuRewrite.getImportRewrite());
		}

		IMethodBinding[] declaredMethods= binding.getDeclaredMethods();
		for (int i= 0; i < declaredMethods.length; i++) {
			IMethodBinding curr= declaredMethods[i];
			if (!implementedAttribs.contains(curr.getName()) && curr.getDefaultValue() == null) {
				MemberValuePair pair= ast.newMemberValuePair();
				pair.setName(ast.newSimpleName(curr.getName()));
				pair.setValue(newDefaultExpression(ast, curr.getReturnType(), context, cuRewrite.getImportRewrite()));
				listRewriter.insertLast(pair, null);

				linkedModel.getPositionGroup("val_name_" + i, true).addPosition(rewriter.track(pair.getName()), false); //$NON-NLS-1$
				linkedModel.getPositionGroup("val_type_" + i, true).addPosition(rewriter.track(pair.getValue()), false); //$NON-NLS-1$
			}
		}
	}

	private Expression newDefaultExpression(AST ast, ITypeBinding type, ImportRewriteContext context, ImportRewrite importRewrite) {
		if (type.isPrimitive()) {
			String name= type.getName();
			if ("boolean".equals(name)) { //$NON-NLS-1$
				return ast.newBooleanLiteral(false);
			} else {
				return ast.newNumberLiteral("0"); //$NON-NLS-1$
			}
		}
		if (type == ast.resolveWellKnownType("java.lang.String")) { //$NON-NLS-1$
			return ast.newStringLiteral();
		}
		if (type.isArray()) {
			ArrayInitializer initializer= ast.newArrayInitializer();
			initializer.expressions().add(newDefaultExpression(ast, type.getElementType(), context, importRewrite));
			return initializer;
		}
		if (type.isAnnotation()) {
			MarkerAnnotation annotation= ast.newMarkerAnnotation();
			annotation.setTypeName(ast.newName(importRewrite.addImport(type, context)));
			return annotation;
		}
		return ast.newNullLiteral();
	}

}
