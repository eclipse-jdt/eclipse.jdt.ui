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
				return " == *: "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				return Boolean.toString(trayBinding == viewerBinding);
			}
		} 
		result.add(new IdentityProperty(trayElement));
		
		class EqualsProperty extends DynamicBindingProperty {
			public EqualsProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return ".equals(*): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				return Boolean.toString(trayBinding.equals(viewerBinding));
			}
		} 
		result.add(new EqualsProperty(trayElement));
		
		class IsEqualToProperty extends DynamicBindingProperty {
			public IsEqualToProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return ".isEqualTo(*): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				return Boolean.toString(trayBinding.isEqualTo(viewerBinding));
			}
		} 
		result.add(new IsEqualToProperty(trayElement));
		
		class KeysEqualProperty extends DynamicBindingProperty {
			public KeysEqualProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return ".getKey().equals(*.getKey()): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				if (trayBinding.getKey() == null)
					return ".getKey() == null"; //$NON-NLS-1$
				else if (viewerBinding == null)
					return "* == null"; //$NON-NLS-1$
				else if (viewerBinding.getKey() == null)
					return "*.getKey() == null"; //$NON-NLS-1$
				else
					return Boolean.toString(trayBinding.getKey().equals(viewerBinding.getKey()));
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
				return ".isSubTypeCompatible(*): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				if (viewerBinding instanceof ITypeBinding) {
					ITypeBinding trayTB= (ITypeBinding) trayBinding;
					ITypeBinding viewerTB= (ITypeBinding) viewerBinding;
					return Boolean.toString(trayTB.isSubTypeCompatible(viewerTB));
				} else {
					return "other not an ITypeBinding"; //$NON-NLS-1$
				}
			}
		} 
		result.add(new IsSubTypeCompatibleProperty(trayElement));
		
		class IsCastCompatibleProperty extends DynamicBindingProperty {
			public IsCastCompatibleProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return ".isCastCompatible(*): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				if (viewerBinding instanceof ITypeBinding) {
					ITypeBinding trayTB= (ITypeBinding) trayBinding;
					ITypeBinding viewerTB= (ITypeBinding) viewerBinding;
					return Boolean.toString(trayTB.isCastCompatible(viewerTB));
				} else {
					return "other not an ITypeBinding"; //$NON-NLS-1$
				}
			}
		} 
		result.add(new IsCastCompatibleProperty(trayElement));
		
		class IsAssignmentCompatibleProperty extends DynamicBindingProperty {
			public IsAssignmentCompatibleProperty(Binding parent) {
				super(parent);
			}
			protected String getName() {
				return ".isAssignmentCompatible(*): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				if (viewerBinding instanceof ITypeBinding) {
					ITypeBinding trayTB= (ITypeBinding) trayBinding;
					ITypeBinding viewerTB= (ITypeBinding) viewerBinding;
					return Boolean.toString(trayTB.isAssignmentCompatible(viewerTB));
				} else {
					return "other not an ITypeBinding"; //$NON-NLS-1$
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
				return ".overrides(*): "; //$NON-NLS-1$
			}
			protected String executeQuery(IBinding trayBinding, IBinding viewerBinding) {
				if (viewerBinding instanceof IMethodBinding) {
					IMethodBinding trayMB= (IMethodBinding) trayBinding;
					IMethodBinding viewerMB= (IMethodBinding) viewerBinding;
					return Boolean.toString(trayMB.overrides(viewerMB));
				} else {
					return "other not an IMethodBinding"; //$NON-NLS-1$
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
