package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.typehierarchy.ITypeHierarchyLifeCycleListener;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyLifeCycle;

public class OverrideImageProvider extends ErrorTickImageProvider {
	
	private TypeHierarchyLifeCycle fTypeHierarchy;
	
	public OverrideImageProvider() {
		fTypeHierarchy= new TypeHierarchyLifeCycle(true);
	}
	
	/*
	 * @see JavaElementImageProvider#computeExtraAdornmentFlags(Object)
	 */
	protected int computeExtraAdornmentFlags(Object element) {
		int adornmentFlags= super.computeExtraAdornmentFlags(element);
		if (element instanceof IMethod) {
			try {
				IMethod method= (IMethod) element;
				int flags= method.getFlags();
				IType type= method.getDeclaringType();
				if (type.isClass() && !method.isConstructor() && !Flags.isPrivate(flags) && !Flags.isStatic(flags)) {
					ITypeHierarchy hierarchy= getTypeHierarchy(type);
					IMethod overridden= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, method.getElementName(), method.getParameterTypes(), false);
					if (overridden != null) {
						adornmentFlags |= JavaElementImageDescriptor.OVERRIDDEN;
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return adornmentFlags;
	}
	
	private ITypeHierarchy getTypeHierarchy(IType type) throws JavaModelException {
		fTypeHierarchy.ensureRefreshedTypeHierarchy(type);
		return fTypeHierarchy.getHierarchy();
	}
		
	/*
	 * @see JavaElementImageProvider#dispose()
	 */
	public void dispose() {
		fTypeHierarchy.freeHierarchy();
		super.dispose();
	}

}
