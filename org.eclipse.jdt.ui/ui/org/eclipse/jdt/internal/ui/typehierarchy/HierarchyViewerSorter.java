package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

/**
  */
public class HierarchyViewerSorter extends JavaElementSorter {
	
	private TypeHierarchyLifeCycle fHierarchy;
	private boolean fSortByDefiningType;
	
	public HierarchyViewerSorter(TypeHierarchyLifeCycle cycle) {
		fHierarchy= cycle;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerSorter#category(java.lang.Object)
	 */
	public int category(Object element) {
		int cat= super.category(element) * 2;
		if (element instanceof IType) {
			ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
			if (hierarchy != null && Flags.isInterface(hierarchy.getCachedFlags((IType)element))) {
				cat++;
			}
		}
		return cat;
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
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (fSortByDefiningType && hierarchy != null) {
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
		return super.compare(viewer, e1, e2);
	}
	
	private IType getDefiningType(ITypeHierarchy hierarchy, IMethod method) throws JavaModelException {
		IType declaringType= (IType) JavaModelUtil.toOriginal(method.getDeclaringType());
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
		// interfaces before classes
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
