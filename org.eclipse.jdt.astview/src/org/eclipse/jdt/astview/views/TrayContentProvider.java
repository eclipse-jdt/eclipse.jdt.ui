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

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


public class TrayContentProvider implements ITreeContentProvider {
	
	public static final int DEFAULT_CHILDREN_COUNT= 7;
	
	protected static final String N_A= "N/A"; //$NON-NLS-1$
	protected static final Object[] EMPTY= new Object[0];
	
	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		ArrayList result= new ArrayList();
		if (! (parentElement instanceof Binding))
			return EMPTY;
		
		Binding trayElement= (Binding) parentElement;
		IBinding trayBinding= trayElement.getBinding();
		
		addBindingComparisons(result, trayElement);
		if (trayBinding instanceof ITypeBinding)
			addTypeBindingComparions(result, trayElement);
		if (trayBinding instanceof IMethodBinding)
			addMethodBindingComparions(result, trayElement);
		
		return result.toArray();
	}
	
	private void addBindingComparisons(ArrayList result, Binding trayElement) {
		class IdentityProperty extends DynamicBindingProperty {
			public IdentityProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "* == this: "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				return Boolean.toString(viewerBinding == trayBinding);
			}
		} 
		result.add(new IdentityProperty(trayElement));
		
		class EqualsProperty extends DynamicBindingProperty {
			public EqualsProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "*.equals(this): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				if (viewerBinding != null)
					return Boolean.toString(viewerBinding.equals(trayBinding));
				else
					return "* is null"; //$NON-NLS-1$
			}
		} 
		result.add(new EqualsProperty(trayElement));
		
		class IsEqualToProperty extends DynamicBindingProperty {
			public IsEqualToProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "*.isEqualTo(this): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				if (viewerBinding != null)
					return Boolean.toString(viewerBinding.isEqualTo(trayBinding));
				else
					return "* is null"; //$NON-NLS-1$
			}
		} 
		result.add(new IsEqualToProperty(trayElement));
		
		class KeysEqualProperty extends DynamicBindingProperty {
			public KeysEqualProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "*.getKey().equals(this.getKey()): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				if (viewerBinding == null)
					return "* is null"; //$NON-NLS-1$
				else if (viewerBinding.getKey() == null)
					return "*.getKey() is null"; //$NON-NLS-1$
				else if (trayBinding.getKey() == null)
					return "this.getKey() is null"; //$NON-NLS-1$
				else
					return Boolean.toString(viewerBinding.getKey().equals(trayBinding.getKey()));
			}
		} 
		result.add(new KeysEqualProperty(trayElement));
	}

	private void addTypeBindingComparions(ArrayList result, Binding trayElement) {
		class IsSubTypeCompatibleProperty extends DynamicBindingProperty {
			public IsSubTypeCompatibleProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "*.isSubTypeCompatible(this): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				if (viewerBinding instanceof ITypeBinding) {
					ITypeBinding viewerTB= (ITypeBinding) viewerBinding;
					ITypeBinding trayTB= (ITypeBinding) trayBinding;
					return Boolean.toString(viewerTB.isSubTypeCompatible(trayTB));
				} else {
					return "* not an ITypeBinding"; //$NON-NLS-1$
				}
			}
		} 
		result.add(new IsSubTypeCompatibleProperty(trayElement));
		
		class IsCastCompatibleProperty extends DynamicBindingProperty {
			public IsCastCompatibleProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "*.isCastCompatible(this): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				if (viewerBinding instanceof ITypeBinding) {
					ITypeBinding viewerTB= (ITypeBinding) viewerBinding;
					ITypeBinding trayTB= (ITypeBinding) trayBinding;
					return Boolean.toString(viewerTB.isCastCompatible(trayTB));
				} else {
					return "* not an ITypeBinding"; //$NON-NLS-1$
				}
			}
		} 
		result.add(new IsCastCompatibleProperty(trayElement));
		
		class IsAssignmentCompatibleProperty extends DynamicBindingProperty {
			public IsAssignmentCompatibleProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "*.isAssignmentCompatible(this): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				if (viewerBinding instanceof ITypeBinding) {
					ITypeBinding viewerTB= (ITypeBinding) viewerBinding;
					ITypeBinding trayTB= (ITypeBinding) trayBinding;
					return Boolean.toString(viewerTB.isAssignmentCompatible(trayTB));
				} else {
					return "* not an ITypeBinding"; //$NON-NLS-1$
				}
			}
		} 
		result.add(new IsAssignmentCompatibleProperty(trayElement));
	}

	private void addMethodBindingComparions(ArrayList result, Binding trayElement) {
		class OverridesProperty extends DynamicBindingProperty {
			public OverridesProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return "*.overrides(this): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding viewerBinding, IBinding trayBinding) {
				if (viewerBinding instanceof IMethodBinding) {
					IMethodBinding viewerMB= (IMethodBinding) viewerBinding;
					IMethodBinding trayMB= (IMethodBinding) trayBinding;
					return Boolean.toString(viewerMB.overrides(trayMB));
				} else {
					return "* not an IMethodBinding"; //$NON-NLS-1$
				}
			}
		} 
		result.add(new OverridesProperty(trayElement));
	}

	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof ASTAttribute) {
			return ((ASTAttribute) element).getParent();
		} else {
			return null;
		}
	}
	
	/*
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return element instanceof Binding;
	}
	
	/*
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ArrayList)
			return ((ArrayList) inputElement).toArray();
		return EMPTY;
	}
	
	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		// do nothing
	}
	
	/*
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// do nothing
	}
}
