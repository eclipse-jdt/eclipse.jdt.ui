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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
  */
public class HierarchyViewerSorter extends ViewerSorter {
	
	private static final int OTHER= 1;
	private static final int CLASS= 2;
	private static final int INTERFACE= 3;
	private static final int ANONYM= 4;
	
	private TypeHierarchyLifeCycle fHierarchy;
	private boolean fSortByDefiningType;
	private JavaElementSorter fNormalSorter;
	
	public HierarchyViewerSorter(TypeHierarchyLifeCycle cycle) {
		fHierarchy= cycle;
		fNormalSorter= new JavaElementSorter();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
	 */
	public int category(Object element) {
		if (element instanceof IType) {
			IType type= (IType) element;
			if (type.getElementName().length() == 0) {
				return ANONYM;
			}
			ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
			if (hierarchy != null) {
				if (Flags.isInterface(hierarchy.getCachedFlags((IType) element))) {
					return INTERFACE;
				} else {
					return CLASS;
				}
			}
		}
		return OTHER;
	}

	public boolean isSortByDefiningType() {
		return fSortByDefiningType;
	}

	public void setSortByDefiningType(boolean sortByDefiningType) {
		fSortByDefiningType= sortByDefiningType;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#compare(null, null, null)
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1= category(e1);
		int cat2= category(e2);

		if (cat1 != cat2)
			return cat1 - cat2;
		
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy == null) {
			return fNormalSorter.compare(viewer, e1, e2);
		}
		
		if (cat1 == OTHER) { // method or field
			if (fSortByDefiningType) {
				try {
					IType def1= (e1 instanceof IMethod) ? getDefiningType(hierarchy, (IMethod) e1) : null;
					IType def2= (e2 instanceof IMethod) ? getDefiningType(hierarchy, (IMethod) e2) : null;
					if (def1 != null) {
						if (def2 != null) {
							if (!def2.equals(def1)) {
								return compareInHierarchy(hierarchy, def1, def2);
							}
						} else {
							return -1;						
						}					
					} else {
						if (def2 != null) {
							return 1;
						}	
					}
				} catch (JavaModelException e) {
					// ignore, default to normal comparison
				}
			}
			return fNormalSorter.compare(viewer, e1, e2); // use appearance pref page settings
		} else if (cat1 == ANONYM) {
			return 0;
		} else {
			String name1= ((IType) e1).getElementName(); //$NON-NLS-1$
			String name2= ((IType) e2).getElementName(); //$NON-NLS-1$
			return getCollator().compare(name1, name2);
		}
	}
	
	private IType getDefiningType(ITypeHierarchy hierarchy, IMethod method) throws JavaModelException {
		IType declaringType= method.getDeclaringType();
		int flags= method.getFlags();
		if (Flags.isPrivate(flags) || Flags.isStatic(flags) || method.isConstructor()) {
			return null;
		}
	
		IMethod res= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, method.getElementName(), method.getParameterTypes(), false);
		if (res == null || method.equals(res)) {
			return null;
		}
		return res.getDeclaringType();
	}
	

	private int compareInHierarchy(ITypeHierarchy hierarchy, IType def1, IType def2) {
		if (isSuperType(hierarchy, def1, def2)) {
			return 1;
		} else if (isSuperType(hierarchy, def2, def1)) {
			return -1;
		}
		// interfaces after classes
		int flags1= hierarchy.getCachedFlags(def1);
		int flags2= hierarchy.getCachedFlags(def2);
		if (Flags.isInterface(flags1)) {
			if (!Flags.isInterface(flags2)) {
				return 1;
			}
		} else if (Flags.isInterface(flags2)) {
			return -1;
		}
		String name1= def1.getElementName();
		String name2= def2.getElementName();
		
		return getCollator().compare(name1, name2);
	}

	private boolean isSuperType(ITypeHierarchy hierarchy, IType def1, IType def2) {
		IType superType= hierarchy.getSuperclass(def1);
		if (superType != null) {
			if (superType.equals(def2) || isSuperType(hierarchy, superType, def2)) {
				return true;
			}
		}
		IType[] superInterfaces= hierarchy.getAllSuperInterfaces(def1);
		for (int i= 0; i < superInterfaces.length; i++) {
			IType curr= superInterfaces[i];
			if (curr.equals(def2) || isSuperType(hierarchy, curr, def2)) {
				return true;
			}		
		}
		return false;
	}

}
