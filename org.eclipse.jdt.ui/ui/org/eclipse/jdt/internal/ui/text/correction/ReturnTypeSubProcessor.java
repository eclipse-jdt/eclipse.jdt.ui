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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
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
					ITypeBinding binding= Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
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
		
		public boolean visit(EnumDeclaration node) {
			return false;
		}
		
		public boolean visit(AnnotationTypeDeclaration node) {
			return false;
		}

	}	
	
	
	public static void addMethodWithConstrNameProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
	
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode instanceof MethodDeclaration) {
			MethodDeclaration declaration= (MethodDeclaration) selectedNode;
			
			ASTRewrite rewrite= ASTRewrite.create(declaration.getAST());
			rewrite.set(declaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.TRUE, null);
			
			String label= CorrectionMessages.getString("ReturnTypeSubProcessor.constrnamemethod.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
			proposals.add(proposal);
		}
	
	}
	
	public static void addVoidMethodReturnsProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
			
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration && selectedNode.getNodeType() == ASTNode.RETURN_STATEMENT) {
			ReturnStatement returnStatement= (ReturnStatement) selectedNode;
			Expression expr= returnStatement.getExpression();
			if (expr != null) {
				ITypeBinding binding= Bindings.normalizeTypeBinding(expr.resolveTypeBinding());
				if (binding == null) {
					binding= selectedNode.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
				}
				MethodDeclaration methodDeclaration= (MethodDeclaration) decl;   
				
				AST ast= astRoot.getAST();
				ASTRewrite rewrite= ASTRewrite.create(ast);
					
				String label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.voidmethodreturns.description", binding.getName()); //$NON-NLS-1$	
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
				
				String returnTypeName= proposal.getImportRewrite().addImport(binding);
				Type newReturnType= ASTNodeFactory.newType(ast, returnTypeName);
				
				if (methodDeclaration.isConstructor()) {
					rewrite.set(methodDeclaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);
					rewrite.set(methodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
				} else {
					rewrite.replace(methodDeclaration.getReturnType2(), newReturnType, null);
				}
				String key= "return_type"; //$NON-NLS-1$
				proposal.addLinkedPosition(rewrite.track(newReturnType), true, key);
				ITypeBinding[] bindings= ASTResolving.getRelaxingTypes(ast, binding);
				for (int i= 0; i < bindings.length; i++) {
					proposal.addLinkedPositionProposal(key, bindings[i]);
				}
				
				Javadoc javadoc= methodDeclaration.getJavadoc();
				if (javadoc != null) {
					TagElement newTag= ast.newTagElement();
					newTag.setTagName(TagElement.TAG_RETURN);
					TextElement commentStart= ast.newTextElement();
					newTag.fragments().add(commentStart);
					
					JavadocTagsSubProcessor.insertTag(rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY), newTag, null);
					proposal.addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$

				}
				proposals.add(proposal);
			}
			ASTRewrite rewrite= ASTRewrite.create(decl.getAST());
			rewrite.remove(returnStatement, null);
			
			String label= CorrectionMessages.getString("ReturnTypeSubProcessor.removereturn.description"); //$NON-NLS-1$	
			Image image= JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
			proposals.add(proposal);			
		}
	}
	

	
	public static void addMissingReturnTypeProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
		
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= problem.getCoveringNode(astRoot);
		if (selectedNode == null) {
			return;
		}
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			MethodDeclaration methodDeclaration= (MethodDeclaration) decl;
			
			ReturnStatementCollector eval= new ReturnStatementCollector();
			decl.accept(eval);

			ITypeBinding typeBinding= eval.getTypeBinding(decl.getAST());
			typeBinding= Bindings.normalizeTypeBinding(typeBinding);

			AST ast= astRoot.getAST();
			ASTRewrite rewrite= ASTRewrite.create(ast);
			ImportRewrite imports= new ImportRewrite(cu);

			Type type;
			String typeName;
			if (typeBinding != null) {
				typeName= imports.addImport(typeBinding);
				type= ASTNodeFactory.newType(ast, typeName);
			} else {
				typeName= "void"; //$NON-NLS-1$		
				type= ast.newPrimitiveType(PrimitiveType.VOID);	
			}

			
			String label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.missingreturntype.description", typeName); //$NON-NLS-1$		
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, 6, image);
			proposal.setImportRewrite(imports);
			
			rewrite.set(methodDeclaration, MethodDeclaration.RETURN_TYPE2_PROPERTY, type, null);
			rewrite.set(methodDeclaration, MethodDeclaration.CONSTRUCTOR_PROPERTY, Boolean.FALSE, null);

			Javadoc javadoc= methodDeclaration.getJavadoc();
			if (javadoc != null && typeBinding != null) {
				TagElement newTag= ast.newTagElement();
				newTag.setTagName(TagElement.TAG_RETURN);
				TextElement commentStart= ast.newTextElement();
				newTag.fragments().add(commentStart);
				
				JavadocTagsSubProcessor.insertTag(rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY), newTag, null);
				proposal.addLinkedPosition(rewrite.track(commentStart), false, "comment_start"); //$NON-NLS-1$
			}
			
			String key= "return_type"; //$NON-NLS-1$
			proposal.addLinkedPosition(rewrite.track(type), true, key);
			if (typeBinding != null) {
				ITypeBinding[] bindings= ASTResolving.getRelaxingTypes(astRoot.getAST(), typeBinding);
				for (int i= 0; i < bindings.length; i++) {
					proposal.addLinkedPositionProposal(key, bindings[i]);
				}
			}
			
			proposals.add(proposal);
			
			// change to constructor
			ASTNode parentType= ASTResolving.findParentType(decl);
			if (parentType instanceof AbstractTypeDeclaration) {
				boolean isInterface= parentType instanceof TypeDeclaration && ((TypeDeclaration) parentType).isInterface();
				if (!isInterface) {
					String constructorName= ((TypeDeclaration) parentType).getName().getIdentifier();
					ASTNode nameNode= methodDeclaration.getName();
					label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.wrongconstructorname.description", constructorName); //$NON-NLS-1$		
					proposals.add(new ReplaceCorrectionProposal(label, cu, nameNode.getStartPosition(), nameNode.getLength(), constructorName, 5));
				}
			}			
		}
	}

	public static void addMissingReturnStatementProposals(IInvocationContext context, IProblemLocation problem, Collection proposals) {
		ICompilationUnit cu= context.getCompilationUnit();
		
		ASTNode selectedNode= problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			Block block= methodDecl.getBody();
			if (block == null) {
				return;
			}
			ReturnStatement existingStatement= (selectedNode instanceof ReturnStatement) ? (ReturnStatement) selectedNode : null;
			proposals.add( new MissingReturnTypeCorrectionProposal(cu, methodDecl, existingStatement, 6));			
				
			Type returnType= methodDecl.getReturnType2();
			if (returnType != null && !"void".equals(ASTNodes.asString(returnType))) { //$NON-NLS-1$
				AST ast= methodDecl.getAST();
				ASTRewrite rewrite= ASTRewrite.create(ast);
				rewrite.replace(returnType, ast.newPrimitiveType(PrimitiveType.VOID), null);
				Javadoc javadoc= methodDecl.getJavadoc();
				if (javadoc != null) {
					TagElement tagElement= JavadocTagsSubProcessor.findTag(javadoc, TagElement.TAG_RETURN, null);
					if (tagElement != null) {
						rewrite.remove(tagElement, null);
					}
				}

				String label= CorrectionMessages.getString("ReturnTypeSubProcessor.changetovoid.description"); //$NON-NLS-1$
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 5, image);
				proposals.add(proposal);				
			}
		}

	}

}
