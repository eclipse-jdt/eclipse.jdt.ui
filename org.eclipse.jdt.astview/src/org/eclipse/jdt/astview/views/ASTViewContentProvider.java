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

package org.eclipse.jdt.astview.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ASTViewContentProvider implements ITreeContentProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	
	/*(non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object child) {
		if (child instanceof ASTNode) {
			ASTNode node= (ASTNode) child;
			ASTNode parent= node.getParent();
			if (parent != null) {
				StructuralPropertyDescriptor prop= node.getLocationInParent();
				return new NodeProperty(parent, prop);
			}
		} else if (child instanceof ASTAttribute) {
			return ((ASTAttribute) child).getParent();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parent) {
		if (parent instanceof ASTAttribute) {
			return ((ASTAttribute) parent).getChildren();
		} else if (parent instanceof ASTNode) {
			return getNodeChildren((ASTNode) parent);
		}
		return new Object[0];
	}
	
	private Object[] getNodeChildren(ASTNode node) {
		ArrayList res= new ArrayList();

		if (node instanceof Expression) {
			Expression expression= (Expression) node;
			ITypeBinding expressionTypeBinding= expression.resolveTypeBinding();
			res.add(createExpressionTypeBinding(node, expressionTypeBinding));
			
			// expressions:
			if (expression instanceof Name) {
				IBinding binding= ((Name) expression).resolveBinding();
				if (binding != expressionTypeBinding)
					res.add(createBinding(expression, binding));
			} else if (expression instanceof MethodInvocation) {
				IMethodBinding binding= ((MethodInvocation) expression).resolveMethodBinding();
				res.add(createBinding(expression, binding));
			} else if (expression instanceof SuperMethodInvocation) {
				IMethodBinding binding= ((SuperMethodInvocation) expression).resolveMethodBinding();
				res.add(createBinding(expression, binding));
			} else if (expression instanceof ClassInstanceCreation) {
				IMethodBinding binding= ((ClassInstanceCreation) expression).resolveConstructorBinding();
				res.add(createBinding(expression, binding));
			} else if (expression instanceof FieldAccess) {
				IVariableBinding binding= ((FieldAccess) expression).resolveFieldBinding();
				res.add(createBinding(expression, binding));
			} else if (expression instanceof SuperFieldAccess) {
				IVariableBinding binding= ((SuperFieldAccess) expression).resolveFieldBinding();
				res.add(createBinding(expression, binding));
			}
		
		// references:
		} else if (node instanceof ConstructorInvocation) {
			IMethodBinding binding= ((ConstructorInvocation) node).resolveConstructorBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof SuperConstructorInvocation) {
			IMethodBinding binding= ((SuperConstructorInvocation) node).resolveConstructorBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof MethodRef) {
			IBinding binding= ((MethodRef) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof MemberRef) {
			IBinding binding= ((MemberRef) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof Type) {
			IBinding binding= ((Type) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof TypeParameter) {
			IBinding binding= ((TypeParameter) node).resolveBinding();
			res.add(createBinding(node, binding));
			
		// declarations:
		} else if (node instanceof AbstractTypeDeclaration) {
			IBinding binding= ((AbstractTypeDeclaration) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof AnnotationTypeMemberDeclaration) {
			IBinding binding= ((AnnotationTypeMemberDeclaration) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof EnumConstantDeclaration) {
			IBinding binding= ((EnumConstantDeclaration) node).resolveVariable();
			res.add(createBinding(node, binding));
		} else if (node instanceof MethodDeclaration) {
			IBinding binding= ((MethodDeclaration) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof VariableDeclaration) {
			IBinding binding= ((VariableDeclaration) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof AnonymousClassDeclaration) {
			IBinding binding= ((AnonymousClassDeclaration) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof ImportDeclaration) {
			IBinding binding= ((ImportDeclaration) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof PackageDeclaration) {
			IBinding binding= ((PackageDeclaration) node).resolveBinding();
			res.add(createBinding(node, binding));
		}
 		
		List list= node.structuralPropertiesForType();
		for (int i= 0; i < list.size(); i++) {
			StructuralPropertyDescriptor curr= (StructuralPropertyDescriptor) list.get(i);
			res.add(new NodeProperty(node, curr));
		}
		
		if (node instanceof CompilationUnit) {
			CompilationUnit root= (CompilationUnit) node;
			res.add(new CommentsProperty(root));
			res.add(new ProblemsProperty(root));
		}
		
		return res.toArray();
	}
	
	private Binding createBinding(ASTNode parent, IBinding binding) {
		String label;
		if (binding == null) {
			label= ">binding"; //$NON-NLS-1$
		} else {
			switch (binding.getKind()) {
				case IBinding.VARIABLE:
					label= "> variable binding"; //$NON-NLS-1$
					break;
				case IBinding.TYPE:
					label= "> type binding"; //$NON-NLS-1$
					break;
				case IBinding.METHOD:
					label= "> method binding"; //$NON-NLS-1$
					break;
				case IBinding.PACKAGE:
					label= "> package binding"; //$NON-NLS-1$
					break;
				default:
					label= "> unknown binding"; //$NON-NLS-1$
			}
		}
		return new Binding(parent, label, binding, true);
	}
	
	
	private Object createExpressionTypeBinding(ASTNode parent, ITypeBinding binding) {
		String label= "> (Expression) type binding"; //$NON-NLS-1$
		return new Binding(parent, label, binding, true);
	}
	
	public boolean hasChildren(Object parent) {
		return getChildren(parent).length > 0;
	}
}
