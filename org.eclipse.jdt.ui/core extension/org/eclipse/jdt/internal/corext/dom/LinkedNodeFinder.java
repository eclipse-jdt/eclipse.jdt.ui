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
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;


/**
 * Find all nodes connected to a given binding. e.g. Declartion of a field and all references.
 * For types this includes also the constructor declaration, for methods also overrridden methods
 * or methods overriding (if existing in the same AST)  
  */

public class LinkedNodeFinder extends ASTVisitor {
	private IBinding fBinding;
	private ArrayList fResult;
	
	/**
	 * Find all nodes connected to the given binding. e.g. Declartion of a field and all references.
 	 * For types this includes also the constructor declaration, for methods also overrridden methods
 	 * or methods overriding (if existing in the same AST)  
	 * @param root The root of the AST tree to search
	 * @param binding The binding of the searched nodes
	 * @return Return 
	 */
	public static SimpleName[] perform(ASTNode root, IBinding binding) {
		ArrayList res= new ArrayList();
		LinkedNodeFinder nodeFinder= new LinkedNodeFinder(binding, res);
		root.accept(nodeFinder);
		return (SimpleName[]) res.toArray(new SimpleName[res.size()]);
	}
	
	private LinkedNodeFinder(IBinding binding, ArrayList result) {
		fBinding= binding;
		fResult= result;
	}
	
	public boolean visit(MethodDeclaration node) {
		if (node.isConstructor() && fBinding.getKind() == IBinding.TYPE) {
			ASTNode typeNode= node.getParent();
			if (typeNode instanceof TypeDeclaration) {
				if (fBinding == ((TypeDeclaration) typeNode).resolveBinding()) {
					fResult.add(node.getName());
				}
			}
		}
		return true;
	}
	
	public boolean visit(TypeDeclaration node) {
		if (fBinding.getKind() == IBinding.METHOD) {
			IMethodBinding binding= (IMethodBinding) fBinding;
			if (binding.isConstructor() && binding.getDeclaringClass() == node.resolveBinding()) {
				fResult.add(node.getName());
			}
		}
		return true;
	}		
	
	public boolean visit(SimpleName node) {
		IBinding binding= node.resolveBinding();
		
		if (fBinding == binding) {
			fResult.add(node);
		} else if (binding != null && binding.getKind() == fBinding.getKind() && binding.getKind() == IBinding.METHOD) {
			if (isConnectedMethod((IMethodBinding) binding, (IMethodBinding) fBinding)) {
				fResult.add(node);
			}
		}
		return false;
	}
	
	private boolean isConnectedMethod(IMethodBinding meth1, IMethodBinding meth2) {
		if (Bindings.isEqualMethod(meth1, meth2.getName(), meth2.getParameterTypes())) {
			ITypeBinding type1= meth1.getDeclaringClass();
			ITypeBinding type2= meth2.getDeclaringClass();
			if (Bindings.isSuperType(type2, type1) || Bindings.isSuperType(type1, type2)) {
				return true;
			}
		}
		return false;
	}
}