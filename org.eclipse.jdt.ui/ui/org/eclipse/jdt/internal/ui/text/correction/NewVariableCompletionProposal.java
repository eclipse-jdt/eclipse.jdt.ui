/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;

public class NewVariableCompletionProposal extends LinkedCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;
	public static final int PARAM= 3;

	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_TYPE= "type"; //$NON-NLS-1$

	private int  fVariableKind;
	private SimpleName fOriginalNode;
	private ITypeBinding fSenderBinding;
	
	public NewVariableCompletionProposal(String label, ICompilationUnit cu, int variableKind, SimpleName node, ITypeBinding senderBinding, int relevance, Image image) {
		super(label, cu, null, relevance, image);
	
		fVariableKind= variableKind;
		fOriginalNode= node;
		fSenderBinding= senderBinding;
	}
		
	protected ASTRewrite getRewrite() throws CoreException {

		CompilationUnit cu= ASTResolving.findParentCompilationUnit(fOriginalNode);
		if (fVariableKind == PARAM) {
			return doAddParam(cu);
		} else if (fVariableKind == FIELD) {
			return doAddField(cu);
		} else { // LOCAL
			return doAddLocal(cu);
		}
	}

	private ASTRewrite doAddParam(CompilationUnit cu) throws CoreException {
		AST ast= cu.getAST();
		SimpleName node= fOriginalNode;

		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(node);
		if (decl instanceof MethodDeclaration) {
			ASTRewrite rewrite= new ASTRewrite(decl);
			
			SingleVariableDeclaration newDecl= ast.newSingleVariableDeclaration();
			newDecl.setType(evaluateVariableType(ast));
			newDecl.setName(ast.newSimpleName(node.getIdentifier()));
			
			rewrite.markAsInsertBeforeOriginal(decl, ASTNodeConstants.PARAMETERS, newDecl, null, null);
			
			markAsLinked(rewrite, node, true, KEY_NAME);
			markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);
			markAsLinked(rewrite, newDecl.getName(), false, KEY_NAME);
			
			return rewrite;
		}
		return null;
	}

	private ASTRewrite doAddLocal(CompilationUnit cu) throws CoreException {
		AST ast= cu.getAST();
		
		SimpleName node= fOriginalNode;

		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(node);
		if (decl instanceof MethodDeclaration || decl instanceof Initializer) {
			ASTRewrite rewrite= new ASTRewrite(decl);
					
			ASTNode parent= node.getParent();
			if (parent.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment= (Assignment) parent;
				if (node.equals(assignment.getLeftHandSide())) {
					int parentParentKind= parent.getParent().getNodeType();
					if (parentParentKind == ASTNode.EXPRESSION_STATEMENT) {
						
						// x = 1; -> int x = 1;
						VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
						VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);
						newDecl.setType(evaluateVariableType(ast));
						
						Expression placeholder= (Expression) rewrite.createCopy(assignment.getRightHandSide());
						newDeclFrag.setInitializer(placeholder);
						newDeclFrag.setName(ast.newSimpleName(node.getIdentifier()));
						rewrite.markAsReplaced(assignment.getParent(), newDecl);
						
						markAsLinked(rewrite, newDeclFrag.getName(), true, KEY_NAME);
						markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);

						return rewrite;
					} else if (parentParentKind == ASTNode.FOR_STATEMENT) {
						//	for (x = 1;;) ->for (int x = 1;;)
						
						ForStatement forStatement= (ForStatement) parent.getParent();
						if (forStatement.initializers().size() == 1 && assignment.equals(forStatement.initializers().get(0))) {
							VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
							VariableDeclarationExpression expression= ast.newVariableDeclarationExpression(frag);
							frag.setName(ast.newSimpleName(node.getIdentifier()));
							Expression placeholder= (Expression) rewrite.createCopy(assignment.getRightHandSide());
							frag.setInitializer(placeholder);
							expression.setType(evaluateVariableType(ast));
							
							rewrite.markAsReplaced(assignment, expression);
							
							markAsLinked(rewrite, frag.getName(), true, KEY_NAME); 
							markAsLinked(rewrite, expression.getType(), false, KEY_TYPE); 
							
							return rewrite;
						}			
					}			
				}
			}
			//	foo(x) -> int x= 0; foo(x)
			
			VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
			VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);

			newDeclFrag.setName(ast.newSimpleName(node.getIdentifier()));
			newDecl.setType(evaluateVariableType(ast));
			newDeclFrag.setInitializer(ASTNodeFactory.newDefaultExpression(ast, newDecl.getType(), 0));

			markAsLinked(rewrite, node, true, KEY_NAME); 
			markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);
			markAsLinked(rewrite, newDeclFrag.getName(), false, KEY_NAME);

			Statement statement= ASTResolving.findParentStatement(node);
			if (statement != null) {
				List list= ASTNodes.getContainingList(statement);
				while (list == null && statement.getParent() instanceof Statement) { // parent must be if, for or while
					statement= (Statement) statement.getParent();
					list= ASTNodes.getContainingList(statement);
				}
				if (list != null) {
					list.add(list.indexOf(statement), newDecl);
					rewrite.markAsInserted(newDecl);
					return rewrite;					
				}
			}
		}
		return null;
	}

	private ASTRewrite doAddField(CompilationUnit astRoot) throws CoreException {
		SimpleName node= fOriginalNode;
		boolean isInDifferentCU= false;
		
		ASTNode newTypeDecl= astRoot.findDeclaringNode(fSenderBinding);
		if (newTypeDecl == null) {
			astRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			newTypeDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
			isInDifferentCU= true;
		}
		
		if (newTypeDecl != null) {
			ASTRewrite rewrite= new ASTRewrite(astRoot);
			
			AST ast= newTypeDecl.getAST();
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setName(ast.newSimpleName(node.getIdentifier()));
			
			Type type= evaluateVariableType(ast);
			
			FieldDeclaration newDecl= ast.newFieldDeclaration(fragment);
			newDecl.setType(type);
						
			newDecl.setModifiers(evaluateFieldModifiers(newTypeDecl));
			if (fSenderBinding.isInterface()) {
				fragment.setInitializer(ASTNodeFactory.newDefaultExpression(ast, type, 0));
			}
		
			boolean isAnonymous= newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
			List decls= isAnonymous ?  ((AnonymousClassDeclaration) newTypeDecl).bodyDeclarations() :  ((TypeDeclaration) newTypeDecl).bodyDeclarations();
							
			int insertIndex= findFieldInsertIndex(decls, node.getStartPosition());
			rewrite.markAsInsertInOriginal(newTypeDecl, ASTNodeConstants.BODY_DECLARATIONS, newDecl, insertIndex, null);
			
			if (!isInDifferentCU) {
				markAsLinked(rewrite, node, true, KEY_NAME);
			}
			markAsLinked(rewrite, fragment.getName(), false, KEY_NAME);
			markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);

			return rewrite;
		}
		return null;
	}
	
	private int findFieldInsertIndex(List decls, int currPos) {
		for (int i= decls.size() - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof FieldDeclaration) {
				if (currPos > curr.getStartPosition() + curr.getLength()) {
					return i + 1;
				}
			}
		}
		return 0;
	}
		
	private Type evaluateVariableType(AST ast) throws CoreException {
		ITypeBinding binding= ASTResolving.guessBindingForReference(fOriginalNode);
		if (binding != null) {
			if (isVariableAssigned()) {
				ITypeBinding[] typeProposals= ASTResolving.getRelaxingTypes(ast, binding);
				for (int i= 0; i < typeProposals.length; i++) {
					addLinkedModeProposal(KEY_TYPE, typeProposals[i]);
				}
			}
			String typeName= addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);			
		}
		Type type= ASTResolving.guessTypeForReference(ast, fOriginalNode);
		if (type != null) {
			return type;
		}
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}
	
	private boolean isVariableAssigned() {
		ASTNode parent= fOriginalNode.getParent();
		return (parent instanceof Assignment) && (fOriginalNode == ((Assignment) parent).getLeftHandSide());
	}
	
	
	private int evaluateFieldModifiers(ASTNode newTypeDecl) {
		if (fSenderBinding.isInterface()) {
			// for interface members copy the modifiers from an existing field
			FieldDeclaration[] fieldDecls= ((TypeDeclaration) newTypeDecl).getFields();
			if (fieldDecls.length > 0) {
				return fieldDecls[0].getModifiers();
			}
			return 0;
		}
		int modifiers= 0;

		ASTNode parent= fOriginalNode.getParent();	
		if (parent instanceof QualifiedName) {
			IBinding qualifierBinding= ((QualifiedName)parent).getQualifier().resolveBinding();
			if (qualifierBinding instanceof ITypeBinding) {
				modifiers |= Modifier.STATIC;
			}			
		} else if (ASTResolving.isInStaticContext(fOriginalNode)) {
			modifiers |= Modifier.STATIC;
		}
		ASTNode node= ASTResolving.findParentType(fOriginalNode);
		if (newTypeDecl.equals(node)) {
			modifiers |= Modifier.PRIVATE;
		} else if (node instanceof AnonymousClassDeclaration) {
			modifiers |= Modifier.PROTECTED;
		} else {
			modifiers |= Modifier.PUBLIC;
		}
		return modifiers;
	}	


	/**
	 * Returns the variable kind.
	 * @return int
	 */
	public int getVariableKind() {
		return fVariableKind;
	}

}
