package org.eclipse.jdt.internal.ui.text.correction;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
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

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  */
public class ReturnTypeSubProcessor {
	
	public static void addMethodWithConstrNameProposals(ICorrectionContext context, List proposals) throws CoreException {
		ICompilationUnit cu= context.getCompilationUnit();
	
		CompilationUnit astRoot= context.getASTRoot();
		ASTNode selectedNode= context.getCoveringNode();
		if (selectedNode instanceof MethodDeclaration) {
			MethodDeclaration declaration= (MethodDeclaration) selectedNode;
			
			ASTRewrite rewrite= new ASTRewrite(astRoot);
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
				
				ASTRewrite rewrite= new ASTRewrite(astRoot);
				Type newReturnType= ASTResolving.getTypeFromTypeBinding(astRoot.getAST(), binding);
				
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
					
				String label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.voidmethodreturns.description", binding.getName()); //$NON-NLS-1$	
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 2, image);
				proposal.addImport(binding);
				proposal.ensureNoModifications();
				proposals.add(proposal);
			}
			ASTRewrite rewrite= new ASTRewrite(astRoot);
			rewrite.markAsRemoved(returnStatement);
			
			String label= CorrectionMessages.getString("ReturnTypeSubProcessor.removereturn.description"); //$NON-NLS-1$	
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 1, image);
			proposal.ensureNoModifications();
			proposals.add(proposal);			
		}
	}
	
	private static class ReturnTypeEvaluator extends ASTVisitor {
		private ITypeBinding fTypeBinding= null;

		public ITypeBinding getTypeBinding() {
			return fTypeBinding;
		}

		public boolean visit(ReturnStatement node) {
			if (fTypeBinding == null) {
				Expression expr= node.getExpression();
				if (expr != null) {
					ITypeBinding binding= expr.resolveTypeBinding();
					if (binding != null && !binding.isNullType()) {
						fTypeBinding= ASTResolving.normalizeTypeBinding(binding);
					} else {
						fTypeBinding= node.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
					}
				} else {
					fTypeBinding= node.getAST().resolveWellKnownType("void"); //$NON-NLS-1$
				}
			}
			return false;
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
			
			ReturnTypeEvaluator eval= new ReturnTypeEvaluator();
			decl.accept(eval);

			ITypeBinding typeBinding= eval.getTypeBinding();

			ASTRewrite rewrite= new ASTRewrite(astRoot);
			AST ast= astRoot.getAST();

			Type type;
			String typeName;
			if (typeBinding != null) {
				type= ASTResolving.getTypeFromTypeBinding(ast, typeBinding);
				typeName= typeBinding.getName();
			} else {
				type= ast.newPrimitiveType(PrimitiveType.VOID);
				typeName= "void";
			}	

			String label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.missingreturntype.description", typeName); //$NON-NLS-1$		
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 2, image);
			if (typeBinding != null) {
				proposal.addImport(typeBinding);
			}
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
				label= CorrectionMessages.getFormattedString("ReturnTypeSubProcessor.wrongconstructorname.description", constructorName); //$NON-NLS-1$		
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
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
		if (decl instanceof MethodDeclaration) {
			MethodDeclaration methodDecl= (MethodDeclaration) decl;
			
 			if (selectedNode instanceof ReturnStatement) {
				ReturnStatement returnStatement= (ReturnStatement) selectedNode;
				if (returnStatement.getExpression() == null) {
					ASTRewrite rewrite= new ASTRewrite(methodDecl);
					
					Expression expression= ASTResolving.getInitExpression(methodDecl.getReturnType());
					if (expression != null) {
						returnStatement.setExpression(expression);
						rewrite.markAsInserted(expression);
					}
					
					Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
					String label= CorrectionMessages.getString("ReturnTypeSubProcessor.changereturnstatement.description");					
					ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 3, image);
					proposal.ensureNoModifications();
					proposals.add(proposal);
				}
 			} else {
				Block block= methodDecl.getBody();
				if (block == null) {
					return;
				}
				AST ast= methodDecl.getAST();
				ASTRewrite rewrite= new ASTRewrite(methodDecl);
				
				List statements= block.statements();
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(ASTResolving.getInitExpression(methodDecl.getReturnType()));
				statements.add(returnStatement);
				rewrite.markAsInserted(returnStatement);
				
				Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
				String label= CorrectionMessages.getString("ReturnTypeSubProcessor.addreturnstatement.description");
				ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, 3, image);
				proposal.ensureNoModifications();
				proposals.add(proposal);
			} 
		}
	
	}

}
