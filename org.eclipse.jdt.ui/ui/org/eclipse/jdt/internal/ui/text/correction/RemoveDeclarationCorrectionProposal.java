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
import java.util.List;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class RemoveDeclarationCorrectionProposal extends ASTRewriteCorrectionProposal {

	private static class SideEffectFinder extends ASTVisitor {
		
		private ArrayList fSideEffectNodes;
		
		public SideEffectFinder(ArrayList res) {
			fSideEffectNodes= res;
		}
			
		public boolean visit(Assignment node) {
			fSideEffectNodes.add(node);
			return false;
		}
		
		public boolean visit(PostfixExpression node) {
			fSideEffectNodes.add(node);
			return false;
		}
		
		public boolean visit(PrefixExpression node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(MethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}		

		public boolean visit(ClassInstanceCreation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(SuperMethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}		
	}


	private SimpleName fName;

	public RemoveDeclarationCorrectionProposal(ICompilationUnit cu, SimpleName name, int relevance) {
		super("", cu, null, relevance, JavaPlugin.getDefault().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE)); //$NON-NLS-1$
		fName= name;
	}

	public String getDisplayString() {
		IBinding binding= fName.resolveBinding();
		String name= fName.getIdentifier();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return CorrectionMessages.getFormattedString("RemoveDeclarationCorrectionProposal.removeunusedtype.description", name); //$NON-NLS-1$
			case IBinding.METHOD:
				if (((IMethodBinding) binding).isConstructor()) {
					return CorrectionMessages.getFormattedString("RemoveDeclarationCorrectionProposal.removeunusedconstructor.description", name); //$NON-NLS-1$
				} else {
					return CorrectionMessages.getFormattedString("RemoveDeclarationCorrectionProposal.removeunusedmethod.description", name); //$NON-NLS-1$
				}
			case IBinding.VARIABLE:
				if (((IVariableBinding) binding).isField()) {
					return CorrectionMessages.getFormattedString("RemoveDeclarationCorrectionProposal.removeunusedfield.description", name); //$NON-NLS-1$
				} else {
					return CorrectionMessages.getFormattedString("RemoveDeclarationCorrectionProposal.removeunusedvar.description", name); //$NON-NLS-1$
				}
			default:
				return super.getDisplayString();		
		}
	}

	/*(non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal#getRewrite()
	 */
	protected ASTRewrite getRewrite() {
		IBinding binding= fName.resolveBinding();
		CompilationUnit root= (CompilationUnit) fName.getRoot();
		ASTRewrite rewrite;
		if (binding.getKind() != IBinding.VARIABLE) {
			ASTNode declaration= root.findDeclaringNode(binding);
			rewrite= ASTRewrite.create(root.getAST());
			rewrite.remove(declaration, null);
		} else { // variable
			// needs full AST
			CompilationUnit completeRoot= JavaPlugin.getDefault().getASTProvider().getAST(getCompilationUnit(), true, null);

			SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, fName.getStartPosition(), fName.getLength());

			rewrite= ASTRewrite.create(completeRoot.getAST()); 
			SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
			for (int i= 0; i < references.length; i++) {
				removeVariableReferences(rewrite, references[i]);
			}
			
			ASTNode declaringNode= completeRoot.findDeclaringNode(nameNode.resolveBinding());
			if (declaringNode instanceof SingleVariableDeclaration) {
				removeParamTag(rewrite, (SingleVariableDeclaration) declaringNode);
			}
		}
		return rewrite;
	}
	
	private void removeParamTag(ASTRewrite rewrite, SingleVariableDeclaration varDecl) {
		if (varDecl.getParent() instanceof MethodDeclaration) {
			Javadoc javadoc= ((MethodDeclaration) varDecl.getParent()).getJavadoc();
			if (javadoc != null) {
				TagElement tagElement= JavadocTagsSubProcessor.findParamTag(javadoc, varDecl.getName().getIdentifier());
				if (tagElement != null) {
					rewrite.remove(tagElement, null);
				}
			}
		}
	}

	/**
	 * Remove the field or variable declaration including the initializer.
	 */
	private void removeVariableReferences(ASTRewrite rewrite, SimpleName reference) {
		ASTNode parent= reference.getParent();
		while (parent instanceof QualifiedName) {
			parent= parent.getParent();
		}
		if (parent instanceof FieldAccess) {
			parent= parent.getParent();
		}
		
		int nameParentType= parent.getNodeType();
		if (nameParentType == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) parent;
			Expression rightHand= assignment.getRightHandSide();
			
			ASTNode assignParent= assignment.getParent();
			if (assignParent.getNodeType() == ASTNode.EXPRESSION_STATEMENT && rightHand.getNodeType() != ASTNode.ASSIGNMENT) {
				removeVariableWithInitializer(rewrite, rightHand, assignParent);
			}	else {
				rewrite.replace(assignment, rewrite.createCopyTarget(rightHand), null);
			}
		} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			rewrite.remove(parent, null);
		} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
			ASTNode varDecl= frag.getParent();
			List fragments;
			if (varDecl instanceof VariableDeclarationExpression) {
				fragments= ((VariableDeclarationExpression) varDecl).fragments();
			} else if (varDecl instanceof FieldDeclaration) {
				fragments= ((FieldDeclaration) varDecl).fragments();
			} else {	
				fragments= ((VariableDeclarationStatement) varDecl).fragments();
			}
			if (fragments.size() == 1) {
				rewrite.remove(varDecl, null);
			} else {
				rewrite.remove(frag, null); // don't try to preserve
			}
		}
	}
	
	private void removeVariableWithInitializer(ASTRewrite rewrite, ASTNode initializerNode, ASTNode statementNode) {
		ArrayList sideEffectNodes= new ArrayList();
		initializerNode.accept(new SideEffectFinder(sideEffectNodes));
		int nSideEffects= sideEffectNodes.size();
		if (nSideEffects == 0) {
			rewrite.remove(statementNode, null); 
		} else {
			// do nothing yet
		}
	}		
	
}
