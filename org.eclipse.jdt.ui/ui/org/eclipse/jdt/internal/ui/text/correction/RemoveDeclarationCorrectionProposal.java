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
import java.util.List;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
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
			rewrite= new ASTRewrite(declaration.getParent());
			rewrite.markAsRemoved(declaration);
		} else { // variable
			// needs full AST
			CompilationUnit completeRoot= AST.parseCompilationUnit(getCompilationUnit(), true, null, null);
			SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, fName.getStartPosition(), fName.getLength());

			rewrite= new ASTRewrite(completeRoot); 
			SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
			for (int i= 0; i < references.length; i++) {
				removeVariableReferences(rewrite, references[i]);
			}
			
			ASTNode declaringNode= completeRoot.findDeclaringNode(nameNode.resolveBinding());
			if (declaringNode instanceof SingleVariableDeclaration) {
				if (declaringNode.getParent() instanceof MethodDeclaration) {
					Javadoc javadoc= ((MethodDeclaration) declaringNode.getParent()).getJavadoc();
					if (javadoc != null) {
						TagElement tagElement= findThrowsTag(javadoc, nameNode.resolveBinding());
						if (tagElement != null) {
							rewrite.markAsRemoved(tagElement);
						}
					}
				}
			}
		}
		return rewrite;
	}
	
	private static TagElement findThrowsTag(Javadoc javadoc, IBinding binding) {
		List tags= javadoc.tags();
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String currName= curr.getTagName();
			if ("@param".equals(currName)) {  //$NON-NLS-1$
				List fragments= curr.fragments();
				if (!fragments.isEmpty() && fragments.get(0) instanceof Name) {
					Name name= (Name) fragments.get(0);
					if (name.resolveBinding() == binding) {
						return curr;
					}
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Remove the field or variable declaration including the initializer.
	 * 
	 */
	private void removeVariableReferences(ASTRewrite rewrite, SimpleName reference) {
		int nameParentType= reference.getParent().getNodeType();
		if (nameParentType == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) reference.getParent();
			Expression rightHand= assignment.getRightHandSide();
			
			ASTNode parent= assignment.getParent();
			if (parent.getNodeType() == ASTNode.EXPRESSION_STATEMENT && rightHand.getNodeType() != ASTNode.ASSIGNMENT) {
				removeVariableWithInitializer(rewrite, rightHand, parent);
			}	else {
				rewrite.markAsReplaced(assignment, rewrite.createCopy(rightHand));
			}
		} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			rewrite.markAsRemoved(reference.getParent());
		} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			VariableDeclarationFragment frag= (VariableDeclarationFragment) reference.getParent();
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
				rewrite.markAsRemoved(varDecl);
			} else {
				rewrite.markAsRemoved(frag); // don't try to preserve
			}
		}
	}
	
	private void removeVariableWithInitializer(ASTRewrite rewrite, ASTNode initializerNode, ASTNode statementNode) {
		ArrayList sideEffectNodes= new ArrayList();
		initializerNode.accept(new SideEffectFinder(sideEffectNodes));
		int nSideEffects= sideEffectNodes.size();
		if (nSideEffects == 0) {
			rewrite.markAsRemoved(statementNode); 
		} else {
			// do nothing yet
		}
	}		
	
}
