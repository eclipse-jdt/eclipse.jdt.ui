package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;
import org.eclipse.jdt.internal.ui.viewsupport.IAdornmentProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class HierarchyAdornmentProvider implements IAdornmentProvider {

	private TypeHierarchyLifeCycle fHierarchy;

	/**
	 * Constructor for HierarchyLabelProvider.
	 * @param flags
	 */
	public HierarchyAdornmentProvider(TypeHierarchyLifeCycle hierarchy) {
		super();
		fHierarchy= hierarchy;
	}

	/*
	 * @see IAdornmentProvider#computeAdornmentFlags(Object)
	 */
	public int computeAdornmentFlags(Object element) {
		if (element instanceof IType) {
			IType type= (IType) element;
			IJavaElement input= fHierarchy.getInputElement();
			if (input != null && input.getElementType() != IJavaElement.TYPE) {
				IJavaElement parent= type.getAncestor(input.getElementType());
				if (input.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					if (parent == null || !parent.getElementName().equals(input.getElementName())) {
						return JavaElementImageProvider.LIGHT_TYPE_ICONS;
					}
				} else if (!input.equals(parent)) {
					return JavaElementImageProvider.LIGHT_TYPE_ICONS;
				}
			}
		} else if (element instanceof IMethod && AppearancePreferencePage.showOverrideIndicators()) {
			try {
				IMethod method= (IMethod) element;
				int flags= method.getFlags();
				IType type= getOriginalType(method.getDeclaringType());
				ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
				if (type.isClass() && !method.isConstructor() && !Flags.isPrivate(flags) && !Flags.isStatic(flags) && (hierarchy != null)) {
					IMethod impl= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, method.getElementName(), method.getParameterTypes(), false);
					if (impl != null) {
						IMethod overridden= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, method.getElementName(), method.getParameterTypes(), false);
						if (overridden != null) {
							return JavaElementImageProvider.OVERLAY_OVERRIDE;
						} else {
							return JavaElementImageProvider.OVERLAY_IMPLEMENTS;
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return 0;
	}

	/*
	 * @see IAdornmentProvider#dispose
	 */
	public void dispose() {
	}
	
	/*
	 * Returns the corresponding original type or
	 * the type itself if it is not in a working copy.
	 */
	private IType getOriginalType(IType type) {
		ICompilationUnit cu= type.getCompilationUnit();
		if (cu != null && cu.isWorkingCopy())
			return (IType)cu.getOriginal(type);
		return type;
	}	

}

