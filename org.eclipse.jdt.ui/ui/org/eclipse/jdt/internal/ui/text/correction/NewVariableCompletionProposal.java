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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class NewVariableCompletionProposal extends ASTRewriteCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;
	public static final int PARAM= 3;

	private int  fVariableKind;
	private SimpleName fOriginalNode;
	private ITypeBinding fSenderBinding;
	private boolean fIsInDifferentCU;

	public NewVariableCompletionProposal(String label, ICompilationUnit cu, int variableKind, SimpleName node, ITypeBinding senderBinding, int relevance, Image image) {
		super(label, cu, null, relevance, image);
	
		fVariableKind= variableKind;
		fOriginalNode= node;
		fSenderBinding= senderBinding;
		fIsInDifferentCU= false;
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
			
			rewrite.markAsInserted(newDecl);
			((MethodDeclaration)decl).parameters().add(newDecl);
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
			
			VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
			VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);
			
			Type type= evaluateVariableType(ast);
			newDecl.setType(type);
			newDeclFrag.setName(ast.newSimpleName(node.getIdentifier()));
			newDeclFrag.setInitializer(ASTResolving.getInitExpression(type, 0));
			
			ASTNode parent= node.getParent();
			if (parent.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment= (Assignment) parent;
				if (node.equals(assignment.getLeftHandSide())) {
					int parentParentKind= parent.getParent().getNodeType();
					if (parentParentKind == ASTNode.EXPRESSION_STATEMENT) {
						Expression placeholder= (Expression) rewrite.createCopy(assignment.getRightHandSide());
						newDeclFrag.setInitializer(placeholder);
				
						rewrite.markAsReplaced(assignment.getParent(), newDecl);
						return rewrite;
					} else if (parentParentKind == ASTNode.FOR_STATEMENT) {
						ForStatement forStatement= (ForStatement) parent.getParent();
						if (forStatement.initializers().size() == 1 && assignment.equals(forStatement.initializers().get(0))) {
							VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
							VariableDeclarationExpression expression= ast.newVariableDeclarationExpression(frag);
							frag.setName(ast.newSimpleName(node.getIdentifier()));
							Expression placeholder= (Expression) rewrite.createCopy(assignment.getRightHandSide());
							frag.setInitializer(placeholder);
							expression.setType(evaluateVariableType(ast));
							
							rewrite.markAsReplaced(assignment, expression);
							return rewrite;
						}			
					}			
				}
			} else if (parent.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				rewrite.markAsReplaced(parent, newDecl);
				return rewrite;
			}
			Statement statement= ASTResolving.findParentStatement(node);
			if (statement != null && statement.getParent() instanceof Block) {
				Block block= (Block) statement.getParent();
				List statements= block.statements();
				statements.add(0, newDecl);
				rewrite.markAsInserted(newDecl);
				return rewrite;
			}
		}
		return null;
	}

	private ASTRewrite doAddField(CompilationUnit astRoot) throws CoreException {
		SimpleName node= fOriginalNode;
		
		ASTNode newTypeDecl= astRoot.findDeclaringNode(fSenderBinding);
		if (newTypeDecl != null) {
		} else {
			astRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			newTypeDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
			fIsInDifferentCU= true;
		}
		
		if (newTypeDecl != null) {
			ASTRewrite rewrite= new ASTRewrite(newTypeDecl);
			
			AST ast= newTypeDecl.getAST();
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setName(ast.newSimpleName(node.getIdentifier()));
			
			Type type= evaluateVariableType(ast);
			
			FieldDeclaration newDecl= ast.newFieldDeclaration(fragment);
			newDecl.setType(type);
						
			newDecl.setModifiers(evaluateFieldModifiers(newTypeDecl));
			if (fSenderBinding.isInterface()) {
				fragment.setInitializer(ASTResolving.getInitExpression(type, 0));
			}
		
			boolean isAnonymous= newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
			List decls= isAnonymous ?  ((AnonymousClassDeclaration) newTypeDecl).bodyDeclarations() :  ((TypeDeclaration) newTypeDecl).bodyDeclarations();
							
			decls.add(findInsertIndex(decls, node.getStartPosition()), newDecl);
			
			rewrite.markAsInserted(newDecl);
			return rewrite;
		}
		return null;
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
		
	private Type evaluateVariableType(AST ast) throws CoreException {
		ITypeBinding binding= ASTResolving.guessBindingForReference(fOriginalNode);
		if (binding != null) {
			addImport(binding);
			return ASTResolving.getTypeFromTypeBinding(ast, binding);
		}
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
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
		
		ASTNode node= ASTResolving.findParentType(fOriginalNode);
		if (newTypeDecl.equals(node)) {
			modifiers |= Modifier.PRIVATE;
			if (ASTResolving.isInStaticContext(fOriginalNode)) {
				modifiers |= Modifier.STATIC;
			}
		} else if (node instanceof AnonymousClassDeclaration) {
			modifiers |= Modifier.PROTECTED;
		} else {
			modifiers |= Modifier.PUBLIC;
			ASTNode parent= fOriginalNode.getParent();	
			if (parent instanceof QualifiedName) {
				Name qualifier= ((QualifiedName)parent).getQualifier();
				if (qualifier.resolveBinding().getKind() == IBinding.TYPE) {
					modifiers |= Modifier.STATIC;
				}
			}
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
	
	public void apply(IDocument document) {
		try {
			CompilationUnitChange change= getCompilationUnitChange();
			
			IEditorPart part= null;
			if (fIsInDifferentCU) {
				change.setKeepExecutedTextEdits(true);
				part= EditorUtility.openInEditor(getCompilationUnit(), true);
			}
			super.apply(document);
		
			if (part instanceof ITextEditor) {
				TextRange range= change.getExecutedTextEdit(change.getEdit()).getTextRange();		
				((ITextEditor) part).selectAndReveal(range.getOffset(), range.getLength());
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}		
	}		

}
