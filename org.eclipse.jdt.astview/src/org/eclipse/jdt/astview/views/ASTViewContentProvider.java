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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;

public class ASTViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

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

		if (node instanceof Name) {
			IBinding binding= ((Name) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof MethodInvocation) {
			IBinding binding= ((MethodInvocation) node).resolveMethodBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof SuperMethodInvocation) {
			IBinding binding= ((SuperMethodInvocation) node).resolveMethodBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof ClassInstanceCreation) {
			IBinding binding= ((ClassInstanceCreation) node).resolveConstructorBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof ConstructorInvocation) {
			IBinding binding= ((ConstructorInvocation) node).resolveConstructorBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof SuperConstructorInvocation) {
			IBinding binding= ((SuperConstructorInvocation) node).resolveConstructorBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof FieldAccess) {
			IBinding binding= ((FieldAccess) node).resolveFieldBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof SuperFieldAccess) {
			IBinding binding= ((SuperFieldAccess) node).resolveFieldBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof MethodRef) {
			IBinding binding= ((MethodRef) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof MemberRef) {
			IBinding binding= ((MemberRef) node).resolveBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof Expression) {
			IBinding binding= ((Expression) node).resolveTypeBinding();
			res.add(createBinding(node, binding));
		} else if (node instanceof Type) {
			IBinding binding= ((Type) node).resolveBinding();
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
	
	
	public boolean hasChildren(Object parent) {
		return getChildren(parent).length > 0;
	}
}
