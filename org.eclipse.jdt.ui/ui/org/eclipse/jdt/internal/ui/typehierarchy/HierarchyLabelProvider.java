package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.viewers.ILabelDecorator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.OverrideIndicatorLabelDecorator;

/**
 * Label provider for the hierarchy viewers. Types in the hierarchy that are not belonging to the
 * input scope are rendered differntly.
  */
public class HierarchyLabelProvider extends AppearanceAwareLabelProvider {

	private static class HierarchyOverrideIndicatorLabelDecorator extends OverrideIndicatorLabelDecorator {
		
		private TypeHierarchyLifeCycle fHierarchy;
		
		public HierarchyOverrideIndicatorLabelDecorator(TypeHierarchyLifeCycle lifeCycle) {
			fHierarchy= lifeCycle;
		}
		
		/* (non-Javadoc)
		 * @see OverrideIndicatorLabelDecorator#getOverrideIndicators(IMethod)
		 */
		protected int getOverrideIndicators(IMethod method) throws JavaModelException {
			IType type= getOriginalType(method.getDeclaringType());
			ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
			if (hierarchy != null) {
				return findInHierarchy(type, hierarchy, method.getElementName(), method.getParameterTypes());
			}
			return 0;
		}
		
		private IType getOriginalType(IType type) {
			ICompilationUnit cu= type.getCompilationUnit();
			if (cu != null && cu.isWorkingCopy())
				return (IType) cu.getOriginal(type);
			return type;
		}
	}


	private TypeHierarchyLifeCycle fHierarchy;

	public HierarchyLabelProvider(TypeHierarchyLifeCycle lifeCycle) {
		super(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS, getDecorators(lifeCycle));
		fHierarchy= lifeCycle;
	}
	
	private static ILabelDecorator[] getDecorators(TypeHierarchyLifeCycle lifeCycle) {
		ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
		return new ILabelDecorator[] { new ProblemsLabelDecorator(registry), new HierarchyOverrideIndicatorLabelDecorator(lifeCycle) };
	}
	

	/* (non-Javadoc)
	 * @see JavaUILabelProvider#evaluateImageFlags(Object)
	 */
	protected int evaluateImageFlags(Object element) {
		int flags= super.evaluateImageFlags(element);
		if (element instanceof IType) {
			if (isDifferentScope((IType) element)) {
				flags |= JavaElementImageProvider.LIGHT_TYPE_ICONS;
			}
		}
		return flags;
	}
	
	private boolean isDifferentScope(IType type) {
		IJavaElement input= fHierarchy.getInputElement();
		if (input == null || input.getElementType() == IJavaElement.TYPE) {
			return false;
		}
			
		IJavaElement parent= type.getAncestor(input.getElementType());
		if (input.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			if (parent == null || parent.getElementName().equals(input.getElementName())) {
				return false;
			}
		} else if (input.equals(parent)) {
			return false;
		}
		return true;
	}	

}
