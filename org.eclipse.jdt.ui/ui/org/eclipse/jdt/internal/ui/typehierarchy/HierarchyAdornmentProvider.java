package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jdt.core.*;

import org.eclipse.jdt.internal.corext.util.*;
import org.eclipse.jdt.internal.ui.*;
import org.eclipse.jdt.internal.ui.preferences.*;
import org.eclipse.jdt.internal.ui.viewsupport.*;

public class HierarchyAdornmentProvider implements IAdornmentProvider {

	private TypeHierarchyViewPart fViewPart;
	private TypeHierarchyLifeCycle fHierarchy;

	/**
	 * Constructor for HierarchyLabelProvider.
	 * @param flags
	 */
	public HierarchyAdornmentProvider(TypeHierarchyViewPart viewPart, TypeHierarchyLifeCycle hierarchy) {
		super();
		fViewPart= viewPart;
		fHierarchy= hierarchy;
	}

	/*
	 * @see IAdornmentProvider#computeAdornmentFlags(Object)
	 */
	public int computeAdornmentFlags(Object element) {
		if (element instanceof IType) {
			IType type= (IType) element;
			IJavaElement input= fViewPart.getInputElement();
			if (input != null && input.getElementType() != IJavaElement.TYPE) {
				IJavaElement parent= JavaModelUtil.findElementOfKind(type, input.getElementType());
				if (!input.equals(parent)) {
					return JavaElementImageProvider.LIGHT_TYPE_ICONS;
				}
			}
		} else if (element instanceof IMethod && AppearancePreferencePage.showOverrideIndicators()) {
			try {
				IMethod method= (IMethod) element;
				int flags= method.getFlags();
				IType type= method.getDeclaringType();
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

}

