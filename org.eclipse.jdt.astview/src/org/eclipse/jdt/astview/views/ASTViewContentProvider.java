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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;

public class ASTViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	public void dispose() {
	}
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}
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
			res.add(new Binding(node, binding)); //$NON-NLS-1$
		} else if (node instanceof MethodRef) {
			IBinding binding= ((MethodRef) node).resolveBinding();
			res.add(new Binding(node, binding)); //$NON-NLS-1$
		} else if (node instanceof MemberRef) {
			IBinding binding= ((MemberRef) node).resolveBinding();
			res.add(new Binding(node, binding)); //$NON-NLS-1$
		} else if (node instanceof Expression) {
			IBinding binding= ((Expression) node).resolveTypeBinding();
			res.add(new Binding(node, binding)); //$NON-NLS-1$
		} else if (node instanceof Type) {
			IBinding binding= ((Type) node).resolveBinding();
			res.add(new Binding(node, binding)); //$NON-NLS-1$
		}
		
		List list= node.structuralPropertiesForType();
		for (int i= 0; i < list.size(); i++) {
			StructuralPropertyDescriptor curr= (StructuralPropertyDescriptor) list.get(i);
			res.add(new NodeProperty(node, curr));
		}
		
		return res.toArray();
	}
	
	public boolean hasChildren(Object parent) {
		return getChildren(parent).length > 0;
	}
}
