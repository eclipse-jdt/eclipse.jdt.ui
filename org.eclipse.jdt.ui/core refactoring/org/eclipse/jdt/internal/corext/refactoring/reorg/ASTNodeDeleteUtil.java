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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;


public class ASTNodeDeleteUtil {
	private ASTNodeDeleteUtil(){}
	
	public static void markAsDeleted(IJavaElement[] javaElements, CompilationUnit cuNode, OldASTRewrite rewrite) throws JavaModelException{
		for (int i= 0; i<javaElements.length;i++) {
			markAsDeleted(javaElements[i], cuNode, rewrite);
		}
		propagateFieldDeclarationNodeDeletions(rewrite);			
	}
	
	private static void markAsDeleted(IJavaElement element, CompilationUnit cuNode, OldASTRewrite rewrite) throws JavaModelException {
		ASTNode[] declarationNodes= getNodesToDelete(element, cuNode);
		for (int i= 0; i < declarationNodes.length; i++) {
			ASTNode node= declarationNodes[i];
			if (node != null)
				rewrite.remove(node, null);
		}
	}

	private static ASTNode[] getNodesToDelete(IJavaElement element, CompilationUnit cuNode) throws JavaModelException {
		//fields are different because you don't delete the whole declaration but only a fragment of it
		if (element.getElementType() == IJavaElement.FIELD)
			return new ASTNode[] {ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) element, cuNode)};
		if (element.getElementType() == IJavaElement.TYPE && ((IType)element).isLocal()) {
			IType type= (IType)element;
			if (type.isAnonymous()) {
				return new ASTNode[] {ASTNodeSearchUtil.getClassInstanceCreationNode(type, cuNode)};
			} else {
				ASTNode[] nodes= ASTNodeSearchUtil.getDeclarationNodes(element, cuNode);
				// we have to delete the TypeDeclarationStatement
				nodes[0]= nodes[0].getParent();
				return nodes;
			}
		}
		return ASTNodeSearchUtil.getDeclarationNodes(element, cuNode);
	}

	//TODO use this method in pull up and push down
	private static void propagateFieldDeclarationNodeDeletions(OldASTRewrite rewrite) {
		Set removedNodes= getRemovedNodes(rewrite);
		for (Iterator iter= removedNodes.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			if (node instanceof VariableDeclarationFragment){
				if (node.getParent() instanceof FieldDeclaration){
					FieldDeclaration fd= (FieldDeclaration)node.getParent();
					if (! rewrite.isRemoved(fd) && removedNodes.containsAll(fd.fragments()))
						rewrite.remove(fd, null);
				}
			}
		}
	}

	/*
	 * return Set<ASTNode>
	 */
	private static Set getRemovedNodes(final OldASTRewrite rewrite) {
		final Set result= new HashSet();
		ASTNode rootNode= rewrite.getRootNode();
		if (rootNode != null) {
			rootNode.accept(new GenericVisitor(){
				protected boolean visitNode(ASTNode node) {
					if (rewrite.isRemoved(node))
						result.add(node);
					return true;
				}
			});
		}
		return result;
	}
}
