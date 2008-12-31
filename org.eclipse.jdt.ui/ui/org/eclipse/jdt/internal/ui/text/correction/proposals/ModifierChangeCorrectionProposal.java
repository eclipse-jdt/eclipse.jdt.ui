/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.swt.graphics.Image;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.VariableDeclarationRewrite;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

public class ModifierChangeCorrectionProposal extends LinkedCorrectionProposal {

	private IBinding fBinding;
	private ASTNode fNode;
	private int fIncludedModifiers;
	private int fExcludedModifiers;

	public ModifierChangeCorrectionProposal(String label, ICompilationUnit targetCU, IBinding binding, ASTNode node, int includedModifiers, int excludedModifiers, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		fBinding= binding;
		fNode= node;
		fIncludedModifiers= includedModifiers;
		fExcludedModifiers= excludedModifiers;
	}

	protected ASTRewrite getRewrite() {
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode boundNode= astRoot.findDeclaringNode(fBinding);
		ASTNode declNode= null;

		TextEditGroup selectionDescription= null;

		if (boundNode != null) {
			declNode= boundNode; // is same CU
		} else {
			selectionDescription= new TextEditGroup("selection"); // in different CU, needs selection //$NON-NLS-1$
			//setSelectionDescription(selectionDescription);
			CompilationUnit newRoot= ASTResolving.createQuickFixAST(getCompilationUnit(), null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);

			if (declNode.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				VariableDeclarationFragment fragment= (VariableDeclarationFragment)declNode;
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) parent;
					if (fieldDecl.fragments().size() > 1 && (fieldDecl.getParent() instanceof AbstractTypeDeclaration)) { // split
						VariableDeclarationRewrite.rewriteModifiers(fieldDecl, new VariableDeclarationFragment[] {fragment}, fIncludedModifiers, fExcludedModifiers, rewrite, selectionDescription);
						return rewrite;
					}
				} else if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					if (varDecl.fragments().size() > 1 && (varDecl.getParent() instanceof Block)) { // split
						VariableDeclarationRewrite.rewriteModifiers(varDecl, new VariableDeclarationFragment[] {fragment}, fIncludedModifiers, fExcludedModifiers, rewrite, selectionDescription);
						return rewrite;
					}
				} else if (parent instanceof VariableDeclarationExpression) {
					// can't separate
				}
				declNode= parent;
			}
			ModifierRewrite listRewrite= ModifierRewrite.create(rewrite, declNode);
			listRewrite.setModifiers(fIncludedModifiers, fExcludedModifiers, selectionDescription);
			return rewrite;
		}
		return null;
	}
}
