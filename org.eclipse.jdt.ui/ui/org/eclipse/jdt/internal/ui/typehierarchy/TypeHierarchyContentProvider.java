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
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyProvider;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
 * Base class for content providers for type hierarchy viewers.
 * Implementors must override 'getTypesInHierarchy'.
 * Java delta processing is also performed by the content provider
 */
public abstract class TypeHierarchyContentProvider implements ITreeContentProvider, IWorkingCopyProvider {
	protected static final Object[] NO_ELEMENTS= new Object[0];
	
	protected TypeHierarchyLifeCycle fTypeHierarchy;
	protected IMember[] fMemberFilter;
	
	protected TreeViewer fViewer;

	private ViewerFilter fWorkingSetFilter;
	
	public TypeHierarchyContentProvider(TypeHierarchyLifeCycle lifecycle) {
		fTypeHierarchy= lifecycle;
		fMemberFilter= null;
		fWorkingSetFilter= null;
	}
	
	/**
	 * Sets members to filter the hierarchy for. Set to <code>null</code> to disable member filtering.
	 * When member filtering is enabled, the hierarchy contains only types that contain
	 * an implementation of one of the filter members and the members themself.
	 * The hierarchy can be empty as well.
	 */
	public void setMemberFilter(IMember[] memberFilter) {
		fMemberFilter= memberFilter;
	}
	
	/**
	 * The members to filter or <code>null</code> if member filtering is disabled.
	 */
	public IMember[] getMemberFilter() {
		return fMemberFilter;
	}
	
	/**
	 * Sets a filter representing a working set or <code>null</code> if working sets are disabled.
	 */
	public void setWorkingSetFilter(ViewerFilter filter) {
		fWorkingSetFilter= filter;
	}
		
	
	protected final ITypeHierarchy getHierarchy() {
		return fTypeHierarchy.getHierarchy();
	}
	
	
	/* (non-Javadoc)
	 * @see IReconciled#providesWorkingCopies()
	 */
	public boolean providesWorkingCopies() {
		return fTypeHierarchy.isReconciled();
	}		
	
	
	/*
	 * Called for the root element
	 * @see IStructuredContentProvider#getElements	 
	 */
	public Object[] getElements(Object parent) {
		ITypeHierarchy hierarchy= getHierarchy();
		if (hierarchy != null) {
			IType input= hierarchy.getType();
			if (input != null) {
				return new IType[] { input };
			}
			// opened on a region: dont show
		}
		return NO_ELEMENTS; 
	}
	
	/**
	 * Hook to overwrite. Filter will be applied on the returned types
	 */	
	protected abstract void getTypesInHierarchy(IType type, List res);
	
	/**
	 * Hook to overwrite. Return null if parent is ambiguous.
	 */	
	protected abstract IType getParentType(IType type);	
	
	
	private boolean isInWorkingSet(Object element) {
		return fWorkingSetFilter == null || fWorkingSetFilter.select(null, null, element);
	}
	
	/*
	 * Called for the tree children.
	 * @see ITreeContentProvider#getChildren
	 */	
	public Object[] getChildren(Object element) {
		if (element instanceof IType) {
			try {
				IType type= (IType)element;
	
				List children= new ArrayList();
				if (fMemberFilter != null) {
					addFilteredMemberChildren(type, children);
				}
	
				addTypeChildren(type, children);
				
				return children.toArray();
			} catch (JavaModelException e) {
				// ignore
			}
		}
		return NO_ELEMENTS;
	}
	
	/*
	 * @see ITreeContentProvider#hasChildren
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof IType) {
			try {
				IType type= (IType) element;
				return hasTypeChildren(type) || (fMemberFilter != null && hasMemberFilterChildren(type));
			} catch (JavaModelException e) {
				return false;
			}			
		}
		return false;
	}	
	
	private void addFilteredMemberChildren(IType parent, List children) throws JavaModelException {
		IMethod[] methods= parent.getMethods();
		for (int i= 0; i < fMemberFilter.length; i++) {
			IMember member= fMemberFilter[i];
			if (parent.equals(member.getDeclaringType())) {
				if (!children.contains(member)) {
					children.add(member);
				}
			} else if (member instanceof IMethod) {
				IMethod curr= (IMethod)member;
				IMethod meth= JavaModelUtil.findMethod(curr.getElementName(), curr.getParameterTypes(), curr.isConstructor(), methods);
				if (meth != null && !children.contains(meth)) {
					children.add(meth);
				}
			}
		}		
	}
		
	private void addTypeChildren(IType type, List children) throws JavaModelException {
		ArrayList types= new ArrayList();
		getTypesInHierarchy(type, types);
		int len= types.size();
		for (int i= 0; i < len; i++) {
			IType curr= (IType) types.get(i);
			if (isInTree(curr)) {
				children.add(curr);
			}
		}
	}
	

	private boolean isInTree(IType type) throws JavaModelException {
		if (isInWorkingSet(type)) {
			if (fMemberFilter != null) {
				return hasMemberFilterChildren(type);
			} else {
				return true;
			}
		}
		
		return hasTypeChildren(type);
	}
	
	private boolean hasMemberFilterChildren(IType type) throws JavaModelException {
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < fMemberFilter.length; i++) {
			IMember member= fMemberFilter[i];
			if (type.equals(member.getDeclaringType())) {
				return true;
			} else if (member instanceof IMethod) {
				IMethod curr= (IMethod)member;
				IMethod meth= JavaModelUtil.findMethod(curr.getElementName(), curr.getParameterTypes(), curr.isConstructor(), methods);
				if (meth != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	private boolean hasTypeChildren(IType type) throws JavaModelException {
		ArrayList types= new ArrayList();
		getTypesInHierarchy(type, types);
		int len= types.size();
		for (int i= 0; i < len; i++) {
			IType curr= (IType) types.get(i);
			if (isInTree(curr)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * @see IContentProvider#inputChanged
	 */
	public void inputChanged(Viewer part, Object oldInput, Object newInput) {
		Assert.isTrue(part instanceof TreeViewer);
		fViewer= (TreeViewer)part;
	}
	
	/*
	 * @see IContentProvider#dispose
	 */	
	public void dispose() {
	}
	
	/*
	 * @see ITreeContentProvider#getParent
	 */
	public Object getParent(Object element) {
		if (element instanceof IMember) {
			IMember member= (IMember) element;
			if (member.getElementType() == IJavaElement.TYPE) {
				return getParentType((IType)member);
			}
			return member.getDeclaringType();
		}
		return null;
	}
	
	protected final boolean isAnonymous(IType type) {
		return type.getElementName().length() == 0;
	}
	
	protected final boolean isObject(IType type) {
		return "Object".equals(type.getElementName()) && type.getDeclaringType() == null && "java.lang".equals(type.getPackageFragment().getElementName());  //$NON-NLS-1$//$NON-NLS-2$
	}


	
}
