package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyLifeCycle;

public class OverrideAdornmentProvider implements IAdornmentProvider {
		
	public OverrideAdornmentProvider() {
	}
	
	/*
	 * @see IAdornmentProvider#computeAdornmentFlags(Object, int)
	 */
	public int computeAdornmentFlags(Object element) {
		if (!AppearancePreferencePage.showOverrideIndicators()) {
			return 0;
		}			
		
		int adornmentFlags= 0;
		
		if (element instanceof IMethod) {
			try {
				IMethod method= (IMethod) element;
				int flags= method.getFlags();
				IType type= method.getDeclaringType();
				if (type.isClass() && !method.isConstructor() && !Flags.isPrivate(flags) && !Flags.isStatic(flags)) {
					ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
					if (hierarchy != null) {
						IMethod impl= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, method.getElementName(), method.getParameterTypes(), false);
						if (impl != null) {
							IMethod overridden= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, method.getElementName(), method.getParameterTypes(), false);
							if (overridden != null) {
								adornmentFlags |= JavaElementImageProvider.OVERLAY_OVERRIDE;
							} else {
								adornmentFlags |= JavaElementImageProvider.OVERLAY_IMPLEMENTS;
							}
						}
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return adornmentFlags;
	}
			
	/*
	 * @see IAdornmentProvider#dispose()
	 */
	public void dispose() {
	}

}
