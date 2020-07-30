/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.jdt.internal.corext.util.CollectionsUtil;

class ChangeTypeContentProvider implements ITreeContentProvider {

	private ChangeTypeRefactoring fGeneralizeType;

	ChangeTypeContentProvider(ChangeTypeRefactoring gt){
		fGeneralizeType= gt;
	}

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof RootType){
			return ((RootType)element).getChildren();
		}
		ITypeBinding[] superTypes = CollectionsUtil.toArray(getDirectSuperTypes((ITypeBinding)element), ITypeBinding.class);
		Arrays.sort(superTypes, (o1, o2) -> {
			String name1 = o1.getQualifiedName();
			String name2 = o2.getQualifiedName();
			return name1.compareTo(name2);
		});
		return superTypes;
	}

	/**
	 * @param type a type
	 * @return the direct superclass and direct superinterfaces. Class Object is
	 * included in the result if the root of the hierarchy is a top-level
	 * interface.
	 */
	public Set<ITypeBinding> getDirectSuperTypes(ITypeBinding type){
		Set<ITypeBinding> result= new HashSet<>();
		if (type.getSuperclass() != null){
			result.add(type.getSuperclass());
		}
		ITypeBinding[] interfaces= type.getInterfaces();
		result.addAll(Arrays.asList(interfaces));
		if (fGeneralizeType.getOriginalType().isInterface() && type != fGeneralizeType.getObject()){
			result.add(fGeneralizeType.getObject());
		}
		return result;
	}

	@Override
	public Object[] getElements(Object element) {
		Assert.isTrue(element instanceof RootType);
		return ((RootType)element).getChildren();
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/**
	 * Artificial "root node" of the tree view. This is needed to handle situations where the replacement
	 * types do not have a single common supertype. Also, the tree view does not show the root node by
	 * default.
	 */
	static class RootType {
		RootType(ITypeBinding root){
			fRoot = root;
		}
		public ITypeBinding[] getChildren(){
			return new ITypeBinding[]{ fRoot };
		}
		private ITypeBinding fRoot;
	}
}
