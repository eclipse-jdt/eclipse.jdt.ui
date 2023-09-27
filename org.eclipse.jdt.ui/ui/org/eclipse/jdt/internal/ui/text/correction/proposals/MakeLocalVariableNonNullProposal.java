/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

public class MakeLocalVariableNonNullProposal extends ASTRewriteCorrectionProposal {

	private final IVariableBinding fBinding;
	private final CompilationUnit fAstRoot;
	private final String fNonNullAnnotationName;

	public MakeLocalVariableNonNullProposal(ICompilationUnit targetCU, IVariableBinding varBinding, CompilationUnit astRoot, int relevance, String nonNullAnnotationName) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$
		fNonNullAnnotationName= nonNullAnnotationName;
		fBinding= varBinding;
		fAstRoot= astRoot;
		String annotationNameLabel= fNonNullAnnotationName;
		int lastDot= fNonNullAnnotationName.lastIndexOf('.');
		if (lastDot != -1)
			annotationNameLabel= fNonNullAnnotationName.substring(lastDot + 1);
		annotationNameLabel= BasicElementLabels.getJavaElementName(annotationNameLabel);

		String[] args= { BasicElementLabels.getJavaElementName(varBinding.getName()), annotationNameLabel };
		setDisplayName(Messages.format(CorrectionMessages.NullAnnotationsCorrectionProcessor_change_local_variable_to_nonNull, args));
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		ASTNode declNode= fAstRoot.findDeclaringNode(fBinding);
		if (declNode != null) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ImportRewrite imports= createImportRewrite(fAstRoot);

			ImportRewriteContext context= new ContextSensitiveImportRewriteContext(fAstRoot, declNode.getStartPosition(), imports);
			String nonNullType= imports.addImport(fNonNullAnnotationName, context);

			if (declNode instanceof VariableDeclarationFragment) {
				ASTNode parent= declNode.getParent();
				if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					if (varDecl.fragments().size() > 1 && (varDecl.getParent() instanceof Block)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						VariableDeclarationStatement newStat= ast.newVariableDeclarationStatement(placeholder);
						newStat.setType((Type) rewrite.createCopyTarget(varDecl.getType()));
						newStat.modifiers().addAll(ASTNodeFactory.newModifiers(ast, varDecl.getModifiers()));
						addNullAnnotation(newStat, VariableDeclarationStatement.MODIFIERS2_PROPERTY, rewrite, nonNullType, ast);

						ListRewrite listRewrite= rewrite.getListRewrite(varDecl.getParent(), Block.STATEMENTS_PROPERTY);
						if (varDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newStat, parent, null);
						} else {
							listRewrite.insertAfter(newStat, parent, null);
						}
					} else {
						addNullAnnotation(varDecl, VariableDeclarationStatement.MODIFIERS2_PROPERTY, rewrite, nonNullType, ast);
					}
				} else if (parent instanceof VariableDeclarationExpression) {
					VariableDeclarationExpression varDecl= (VariableDeclarationExpression) parent;
					addNullAnnotation(varDecl, VariableDeclarationExpression.MODIFIERS2_PROPERTY, rewrite, nonNullType, ast);
				}
			} else if (declNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration varDecl= (SingleVariableDeclaration) declNode;
				addNullAnnotation(varDecl, SingleVariableDeclaration.MODIFIERS2_PROPERTY, rewrite, nonNullType, ast);
			}
			return rewrite;
		}
		return null;
	}

	private void addNullAnnotation(ASTNode varDecl, ChildListPropertyDescriptor modifiers2Property, ASTRewrite rewrite, String nonNullType, AST ast) {
		ListRewrite listRewrite= rewrite.getListRewrite(varDecl, modifiers2Property);
		Annotation newAnnotation= ast.newMarkerAnnotation();
		newAnnotation.setTypeName(ast.newName(nonNullType));
		listRewrite.insertLast(newAnnotation, null); // null annotation is last modifier, directly preceding the type
	}
}
