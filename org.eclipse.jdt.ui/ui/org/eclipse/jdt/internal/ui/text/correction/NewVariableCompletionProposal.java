/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewVariableCompletionProposal extends ASTRewriteCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;
	public static final int PARAM= 3;

	private int  fVariableKind;
	private SimpleName fOriginalNode;

	public NewVariableCompletionProposal(String label, ICompilationUnit cu, int variableKind, SimpleName node, int relevance) {
		super(label, cu, null, relevance, null);
	
		fVariableKind= variableKind;
		fOriginalNode= node;
		if (variableKind == FIELD) {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_MISC_PUBLIC));
		} else {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL));
		}
	}
		
	protected ASTRewrite getRewrite() {
		AST ast= new AST();
		CompilationUnit origCU= ASTResolving.findParentCompilationUnit(fOriginalNode);
		
		ASTCloner cloner= new ASTCloner(ast, origCU);

		CompilationUnit cu= (CompilationUnit) cloner.getClonedRoot();
		SimpleName node= (SimpleName) cloner.getCloned(fOriginalNode);
		
		cloner= null;

		ASTRewrite rewrite= new ASTRewrite(cu);
		
		if (fVariableKind == PARAM) {
			doAddParam(ast, node, rewrite);
		} else if (fVariableKind == FIELD) {
			doAddField(ast, node, rewrite);
		} else if (fVariableKind == LOCAL) {
			doAddLocal(ast, node, rewrite);
		}
		return rewrite;
	}

	private void doAddParam(AST ast, SimpleName node, ASTRewrite rewrite) {
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(node);
		if (decl instanceof MethodDeclaration) {
			SingleVariableDeclaration newDecl= ast.newSingleVariableDeclaration();
			newDecl.setType(evaluateVariableType(ast));
			newDecl.setName(ast.newSimpleName(node.getIdentifier()));
			
			rewrite.markAsInserted(newDecl);
			((MethodDeclaration)decl).parameters().add(newDecl);
		}
	}

	private void doAddLocal(AST ast, SimpleName node, ASTRewrite rewrite) {
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(node);
		if (decl instanceof MethodDeclaration || decl instanceof Initializer) {
			
			VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
			VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);
			
			Type type= evaluateVariableType(ast);
			newDecl.setType(type);
			newDeclFrag.setName(ast.newSimpleName(node.getIdentifier()));
			newDeclFrag.setInitializer(ASTResolving.getInitExpression(type));
			
			ASTNode parent= node.getParent();
			if (parent.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment= (Assignment) parent;
				if (node.equals(assignment.getLeftHandSide())) {
					int parentParentKind= parent.getParent().getNodeType();
					if (parentParentKind == ASTNode.EXPRESSION_STATEMENT) {
						Expression placeholder= (Expression) rewrite.createCopyTarget(assignment.getRightHandSide());
						newDeclFrag.setInitializer(placeholder);
				
						rewrite.markAsReplaced(assignment.getParent(), newDecl);
						return;
					} else if (parentParentKind == ASTNode.FOR_STATEMENT) {
						ForStatement forStatement= (ForStatement) parent.getParent();
						if (forStatement.initializers().size() == 1 && assignment.equals(forStatement.initializers().get(0))) {
							VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
							VariableDeclarationExpression expression= ast.newVariableDeclarationExpression(frag);
							frag.setName(ast.newSimpleName(node.getIdentifier()));
							Expression placeholder= (Expression) rewrite.createCopyTarget(assignment.getRightHandSide());
							frag.setInitializer(placeholder);
							expression.setType(evaluateVariableType(ast));
							
							rewrite.markAsReplaced(assignment, expression);
							return;
						}			
					}			
				}
			} else if (parent.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				rewrite.markAsReplaced(parent, newDecl);
				return;
			}
			Statement statement= ASTResolving.findParentStatement(node);
			if (statement != null && statement.getParent() instanceof Block) {
				Block block= (Block) statement.getParent();
				List statements= block.statements();
				statements.add(0, newDecl);
				
				rewrite.markAsInserted(newDecl);
			}
		}
	}

	private void doAddField(AST ast, SimpleName node, ASTRewrite rewrite) {
		ASTNode parentType= ASTResolving.findParentType(node);
		if (parentType != null) {
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setName(ast.newSimpleName(node.getIdentifier()));
			
			FieldDeclaration newDecl= ast.newFieldDeclaration(fragment);
			newDecl.setType(evaluateVariableType(ast));
			
			int modifiers= Modifier.PRIVATE;
			if (ASTResolving.isInStaticContext(node)) {
				modifiers |= Modifier.STATIC;
			}
			
			newDecl.setModifiers(modifiers);
		
			boolean isAnonymous= parentType.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
			List decls= isAnonymous ?  ((AnonymousClassDeclaration) parentType).bodyDeclarations() :  ((TypeDeclaration) parentType).bodyDeclarations();
							
			decls.add(findInsertIndex(decls, node.getStartPosition()), newDecl);
			
			rewrite.markAsInserted(newDecl);
		}
	}
	
	private int findInsertIndex(List decls, int currPos) {
		for (int i= decls.size() - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof FieldDeclaration && currPos > curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return 0;
	}
		
	private Type evaluateVariableType(AST ast) {
		ITypeBinding binding= ASTResolving.getTypeBinding(fOriginalNode);
		if (binding != null) {
			ITypeBinding baseType= binding.isArray() ? binding.getElementType() : binding;
			if (!baseType.isPrimitive()) {
				addImport(Bindings.getFullyQualifiedName(baseType));
			}
			return ASTResolving.getTypeFromTypeBinding(ast, binding);
		}
		return ast.newSimpleType(ast.newSimpleName("Object"));
	}


}
