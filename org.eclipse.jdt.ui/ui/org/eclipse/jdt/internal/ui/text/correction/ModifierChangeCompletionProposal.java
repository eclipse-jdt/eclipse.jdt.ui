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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

public class ModifierChangeCompletionProposal extends LinkedCorrectionProposal {

	private IBinding fBinding;
	private ASTNode fNode;
	private int fIncludedModifiers;
	private int fExcludedModifiers;
	
	public ModifierChangeCompletionProposal(String label, ICompilationUnit targetCU, IBinding binding, ASTNode node, int includedModifiers, int excludedModifiers, int relevance, Image image) {
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
			ASTParser astParser= ASTParser.newParser(ASTProvider.AST_LEVEL);
			astParser.setSource(getCompilationUnit());
			astParser.setResolveBindings(true);
			CompilationUnit newRoot= (CompilationUnit) astParser.createAST(null);
			declNode= newRoot.findDeclaringNode(fBinding.getKey());
		}
		if (declNode != null) {
			AST ast= declNode.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			
			if (declNode.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				ASTNode parent= declNode.getParent();
				if (parent instanceof FieldDeclaration) {
					FieldDeclaration fieldDecl= (FieldDeclaration) parent;
					if (fieldDecl.fragments().size() > 1 && (fieldDecl.getParent() instanceof AbstractTypeDeclaration)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						FieldDeclaration newField= ast.newFieldDeclaration(placeholder);
						newField.setType((Type) ASTNode.copySubtree(ast, fieldDecl.getType()));
						newField.modifiers().addAll(ast.newModifiers((fieldDecl.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers));

						AbstractTypeDeclaration typeDecl= (AbstractTypeDeclaration) fieldDecl.getParent();
						ListRewrite listRewrite= rewrite.getListRewrite(typeDecl, typeDecl.getBodyDeclarationsProperty());
						if (fieldDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newField, parent, null);
						} else {
							listRewrite.insertAfter(newField, parent, null);
						}
						declNode= newField;
						return rewrite;
					}
				} else if (parent instanceof VariableDeclarationStatement) {
					VariableDeclarationStatement varDecl= (VariableDeclarationStatement) parent;
					if (varDecl.fragments().size() > 1 && (varDecl.getParent() instanceof Block)) { // split
						VariableDeclarationFragment placeholder= (VariableDeclarationFragment) rewrite.createMoveTarget(declNode);
						VariableDeclarationStatement newStat= ast.newVariableDeclarationStatement(placeholder);
						newStat.setType((Type) ASTNode.copySubtree(ast, varDecl.getType()));
						newStat.modifiers().addAll(ast.newModifiers((newStat.getModifiers() & ~fExcludedModifiers) | fIncludedModifiers));

						ListRewrite listRewrite= rewrite.getListRewrite(varDecl.getParent(), Block.STATEMENTS_PROPERTY);
						if (varDecl.fragments().indexOf(declNode) == 0) { // if it as the first in the list-> insert before
							listRewrite.insertBefore(newStat, parent, null);
						} else {
							listRewrite.insertAfter(newStat, parent, null);
						}
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
