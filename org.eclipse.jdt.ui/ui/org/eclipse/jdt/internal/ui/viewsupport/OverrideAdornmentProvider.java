package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyLifeCycle;

public class OverrideAdornmentProvider implements IAdornmentProvider {
	
	private TypeHierarchyLifeCycle fTypeHierarchy;
	
	public OverrideAdornmentProvider() {
		fTypeHierarchy= null;
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
					ITypeHierarchy hierarchy= getTypeHierarchy(type);
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
	
	private ITypeHierarchy getTypeHierarchy(IType type) throws JavaModelException {
		if (fTypeHierarchy ==  null) {
			fTypeHierarchy= new TypeHierarchyLifeCycle(true);
		}
		fTypeHierarchy.ensureRefreshedTypeHierarchy(type);
		return fTypeHierarchy.getHierarchy();
	}
		
	/*
	 * @see IAdornmentProvider#dispose()
	 */
	public void dispose() {
		if (fTypeHierarchy != null) {
			fTypeHierarchy.freeHierarchy();
		}
	}

}
