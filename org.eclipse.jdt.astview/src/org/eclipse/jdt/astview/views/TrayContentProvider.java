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
		final IBinding trayBinding= trayElement.getBinding();
		
		addBindingComparisons(result, trayElement, trayBinding);
		if (trayBinding instanceof ITypeBinding)
			addTypeBindingComparions(result, trayElement, (ITypeBinding) trayBinding);
		if (trayBinding instanceof IMethodBinding)
			addMethodBindingComparions(result, trayElement, (IMethodBinding) trayBinding);
		
		return result.toArray();
	}
	
	private void addBindingComparisons(ArrayList result, Binding trayElement, final IBinding trayBinding) {
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(" == *: "); //$NON-NLS-1$
				if (viewerElement != null)
					buf.append(trayBinding == viewerElement.getBinding());
				else
					buf.append(N_A);
				return buf.toString();
			}
		});
		
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(".equals(*): "); //$NON-NLS-1$
				if (viewerElement != null)
					buf.append(trayBinding.equals(viewerElement.getBinding()));
				else
					buf.append(N_A);
				return buf.toString();
			}
		});
		
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(".isEqualTo(*): "); //$NON-NLS-1$
				if (viewerElement != null)
					buf.append(trayBinding.isEqualTo(viewerElement.getBinding()));
				else
					buf.append(N_A);
				return buf.toString();
			}
		});
		
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(".getKey().equals(*.getKey()): "); //$NON-NLS-1$
				if (viewerElement != null) {
					IBinding viewerBinding= viewerElement.getBinding();
					try {
						if (trayBinding.getKey() == null)
							buf.append(".getKey() == null"); //$NON-NLS-1$
						else if (viewerBinding == null)
							buf.append("* == null"); //$NON-NLS-1$
						else if (viewerBinding.getKey() == null)
							buf.append("*.getKey() == null"); //$NON-NLS-1$
						else
							buf.append(trayBinding.getKey().equals(viewerBinding.getKey()));
					} catch (Exception e) {
						buf.append(e.getClass().getName());
					}
				} else {
					buf.append(N_A);
				}
				return buf.toString();
			}
		});
	}

	private void addTypeBindingComparions(ArrayList result, Binding trayElement, final ITypeBinding trayTB) {
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(".isSubTypeCompatible(*): "); //$NON-NLS-1$
				if (viewerElement != null) {
					if (viewerElement.getBinding() instanceof ITypeBinding) {
						ITypeBinding viewerTB= (ITypeBinding) viewerElement.getBinding();
						try {
							buf.append(trayTB.isSubTypeCompatible(viewerTB));
						} catch (Exception e) {
							buf.append(e.getClass().getName());
						}
					} else {
						buf.append("other not an ITypeBinding"); //$NON-NLS-1$
					}
				} else {
					buf.append(N_A);
				}
				return buf.toString();
			}
		});
		
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(".isCastCompatible(*): "); //$NON-NLS-1$
				if (viewerElement != null) {
					if (viewerElement.getBinding() instanceof ITypeBinding) {
						ITypeBinding viewerTB= (ITypeBinding) viewerElement.getBinding();
						try {
							buf.append(trayTB.isCastCompatible(viewerTB));
						} catch (Exception e) {
							buf.append(e.getClass().getName());
						}
					} else {
						buf.append("other not an ITypeBinding"); //$NON-NLS-1$
					}
				} else {
					buf.append(N_A);
				}
				return buf.toString();
			}
		});
		
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(".isAssignmentCompatible(*): "); //$NON-NLS-1$
				if (viewerElement != null) {
					if (viewerElement.getBinding() instanceof ITypeBinding) {
						ITypeBinding viewerTB= (ITypeBinding) viewerElement.getBinding();
						try {
							buf.append(trayTB.isAssignmentCompatible(viewerTB));
						} catch (Exception e) {
							buf.append(e.getClass().getName());
						}
					} else {
						buf.append("other not an ITypeBinding"); //$NON-NLS-1$
					}
				} else {
					buf.append(N_A);
				}
				return buf.toString();
			}
		});
	}

	private void addMethodBindingComparions(ArrayList result, Binding trayElement, final IMethodBinding trayMB) {
		result.add(new DynamicBindingProperty(trayElement) {
			public String getLabel(Binding viewerElement) {
				StringBuffer buf= new StringBuffer(".overrides(*): "); //$NON-NLS-1$
				if (viewerElement != null) {
					if (viewerElement.getBinding() instanceof IMethodBinding) {
						IMethodBinding viewerMB= (IMethodBinding) viewerElement.getBinding();
						try {
							buf.append(trayMB.overrides(viewerMB));
						} catch (Exception e) {
							buf.append(e.getClass().getName());
						}
					} else {
						buf.append("other not a IMethodBinding"); //$NON-NLS-1$
					}
				} else {
					buf.append(N_A);
				}
				return buf.toString();
			}
		});
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
