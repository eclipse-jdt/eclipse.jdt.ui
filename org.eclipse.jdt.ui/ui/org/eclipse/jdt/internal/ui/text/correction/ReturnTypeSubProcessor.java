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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  */
public class ReturnTypeSubProcessor {
	
	private static class ReturnStatementCollector extends ASTVisitor {
		private ArrayList fResult= new ArrayList();
		
		public Iterator returnStatements() {
			return fResult.iterator();
		}

		public ITypeBinding getTypeBinding(AST ast) {
			boolean couldBeObject= false;
			for (int i= 0; i < fResult.size(); i++) {
				ReturnStatement node= (ReturnStatement) fResult.get(i);
				Expression expr= node.getExpression();
				if (expr != null) {
					ITypeBinding binding= ASTResolving.normalizeTypeBinding(expr.resolveTypeBinding());
					if (binding != null) {
						return binding;
					} else {
						couldBeObject= true;						
					}
				} else {
					return ast.resolveWellKnownType("void"); //$NON-NLS-1$
				}				
			}
			if (couldBeObject) {
				return ast.resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
			}
			return ast.resolveWellKnownType("void"); //$NON-NLS-1$
		}

		public boolean visit(ReturnStatement node) {
			fResult.add(node);
			return false;
		}

		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		public boolean visit(TypeDeclaration node) {
			return false;
		}

	}	
	
	
	public static void addMethodWithConstrNameProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
	
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode instanceof MethodDeclaration) {
			MethodDeclaration declaration= (MethodDeclaration) selectedNode;
			
			ASTRewrite rewrite= new ASTRewrite(declaration);
			rewrite.markAsRemoved(declaration.getReturnType());
			
			String label= CorrectionMessages.getString("ReturnTypeSubProcessor.constrnamemethod.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);
		}
	
	}
	
	public static void addVoidMethodReturnsProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
			
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration && selectedNode.getNodeType() == ASTNode.RETURN_STATEMENT) {
			ReturnStatement returnStatement= (ReturnStatement) selectedNode;
			Expression expr= returnStatement.getExpression();
			if (expr != null) {
				ITypeBinding binding= ASTResolving.normalizeTypeBinding(expr.resolveTypeBinding());
				if (binding == null) {
					binding= selectedNode.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				MethodDeclaration methodDeclaration= (MethodDeclaration) decl;   
				
				ASTRewrite rewrite= new ASTRewrite(methodDeclaration);
					
				String label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.voidmethodreturns.description", binding.getName()); //$NON-NLS-1$	
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 2, image);
				String returnTypeName= proposal.addImport(binding);

				Type newReturnType= ASTNodeFactory.newType(astRoot.getAST(), returnTypeName);
				
				if (methodDeclaration.isConstructor()) {
					MethodDeclaration modifiedNode= astRoot.getAST().newMethodDeclaration();
					modifiedNode.setModifiers(methodDeclaration.getModifiers()); // no changes
					modifiedNode.setExtraDimensions(methodDeclaration.getExtraDimensions()); // no changes
					modifiedNode.setConstructor(false);
					rewrite.markAsModified(methodDeclaration, modifiedNode);
					methodDeclaration.setReturnType(newReturnType);
					rewrite.markAsInserted(newReturnType);
				} else {
					rewrite.markAsReplaced(methodDeclaration.getReturnType(), newReturnType);
				}			

				proposal.ensureNoModifications();
				proposals.add(proposal);
			}
			ASTRewrite rewrite= new ASTRewrite(decl);
			rewrite.markAsRemoved(returnStatement);
			
			String label= CorrectionMessages.getString("ReturnTypeSubProcessor.removereturn.description"); //$NON-NLS-1$	
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);			
		}
	}
	

	
	public static void addMissingReturnTypeProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			MethodDeclaration methodDeclaration= (MethodDeclaration) decl;
			
			ReturnStatementCollector eval= new ReturnStatementCollector();
			decl.accept(eval);

			ITypeBinding typeBinding= eval.getTypeBinding(decl.getAST());
			typeBinding= ASTResolving.normalizeTypeBinding(typeBinding);

			ASTRewrite rewrite= new ASTRewrite(methodDeclaration);
			AST ast= astRoot.getAST();

			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 2, image); //$NON-NLS-1$

			Type type;
			String typeName;
			if (typeBinding != null) {
				typeName= proposal.addImport(typeBinding);
				type= ASTNodeFactory.newType(ast, typeName);
			} else {
				typeName= "void"; //$NON-NLS-1$		
				type= ast.newPrimitiveType(PrimitiveType.VOID);	
			}
			proposal.setDisplayName(CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.missingreturntype.description", typeName)); //$NON-NLS-1$		

			rewrite.markAsInserted(type);
			methodDeclaration.setReturnType(type);
			
			MethodDeclaration modifiedNode= ast.newMethodDeclaration();
			modifiedNode.setModifiers(methodDeclaration.getModifiers()); // no changes
			modifiedNode.setExtraDimensions(methodDeclaration.getExtraDimensions()); // no changes
			modifiedNode.setConstructor(false);
			rewrite.markAsModified(methodDeclaration, modifiedNode);
			
			proposal.ensureNoModifications();
			proposals.add(proposal);
			
			// change to constructor
			ASTNode parentType= ASTResolving.findParentType(decl);
			if (parentType instanceof TypeDeclaration) {
				String constructorName= ((TypeDeclaration) parentType).getName().getIdentifier();
				ASTNode nameNode= methodDeclaration.getName();
				String label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.wrongconstructorname.description", constructorName); //$NON-NLS-1$		
				proposals.add(new ReplaceCorrectionProposal(label, cu, nameNode.getStartPosition(), nameNode.getLength(), constructorName, 1));
			}			
		}
	}

	/**
	 * Method addMissingReturnStatementProposals.
	 * @param context
	 * @param proposals
	 */
	public static void addMissingReturnStatementProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode == null) {
			return;
		}
		AST ast= selectedNode.getAST();
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			
 			if (selectedNode instanceof ReturnStatement) {
				ReturnStatement returnStatement= (ReturnStatement) selectedNode;
				if (returnStatement.getExpression() == null) {
					ASTRewrite rewrite= new ASTRewrite(methodDecl);
					
					Expression expression= ASTNodeFactory.newDefaultExpression(ast, methodDecl.getReturnType(), methodDecl.getExtraDimensions());
					if (expression != null) {
						returnStatement.setExpression(expression);
						rewrite.markAsInserted(expression);
					}
					
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					String label= CorrectionMessages.getString("ReturnTypeSubProcessor.changereturnstatement.description");					 //$NON-NLS-1$
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 3, image);
					proposal.ensureNoModifications();
					proposals.add(proposal);
				}
 			} else {
				Block block= methodDecl.getBody();
				if (block == null) {
					return;
				}

				ASTRewrite rewrite= new ASTRewrite(methodDecl);
				
				List statements= block.statements();
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(ASTNodeFactory.newDefaultExpression(ast, methodDecl.getReturnType(), methodDecl.getExtraDimensions()));
				statements.add(returnStatement);
				rewrite.markAsInserted(returnStatement);
				
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				String label= CorrectionMessages.getString("ReturnTypeSubProcessor.addreturnstatement.description"); //$NON-NLS-1$
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 3, image);
				proposal.ensureNoModifications();
				proposals.add(proposal);
			} 
		}
	
	}

}
