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
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.core.IType;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;

class ChangeTypeContentProvider implements ITreeContentProvider {
	
	private ChangeTypeRefactoring fGeneralizeType;
	
	ChangeTypeContentProvider(ChangeTypeRefactoring gt){
		fGeneralizeType= gt;
	}

	public Object[] getChildren(Object element) {
		if (element instanceof RootType){
			return ((RootType)element).getChildren();
		}	
		IType[] superTypes = fGeneralizeType.getTypeHierarchy().getSupertypes((IType)element);
		Arrays.sort(superTypes, new Comparator(){
			public int compare(Object o1, Object o2) {
				String name1 = ((IType)o1).getFullyQualifiedName();
				String name2 = ((IType)o2).getFullyQualifiedName();
				return name1.compareTo(name2);
			}	
		});
		return superTypes;
		
	}

	public Object[] getElements(Object element) {
		Assert.isTrue(element instanceof RootType);
		return ((RootType)element).getChildren();
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public Object getParent(Object element) {
		return null;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
	
	/**
	 * Artificial "root node" of the tree view. This is needed to handle situations where the replacement
	 * types do not have a single common supertype. Also, the tree view does not show the root node by
	 * default.
	 */
	static class RootType {
		RootType(IType root){
			fRoot = root;
		}
		public IType[] getChildren(){
			return new IType[]{ fRoot };
		}
		private IType fRoot;
	}
}
