package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;

/**
 * Label provider for the hierarchy viewers. Types in the hierarchy that are not belonging to the
 * input scope are rendered differntly.
  */
public class HierarchyLabelProvider extends AppearanceAwareLabelProvider {

	private static class HierarchyOverrideIndicatorLabelDecorator extends OverrideIndicatorLabelDecorator {
		
		private TypeHierarchyLifeCycle fHierarchy;
		
		public HierarchyOverrideIndicatorLabelDecorator(TypeHierarchyLifeCycle lifeCycle) {
			super(null);
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
		return new ILabelDecorator[] { new ProblemsLabelDecorator(null), new HierarchyOverrideIndicatorLabelDecorator(lifeCycle) };
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
	
	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */ 
	public Image getImage(Object element) {
		Image result= null;
		if (element instanceof IType) {
			ImageDescriptor desc= getTypeImageDescriptor((IType) element);
			if (desc != null) {
				result= JavaPlugin.getImageDescriptorRegistry().get(desc);
			}
		} else {
			result= fImageLabelProvider.getImageLabel(element, evaluateImageFlags(element));
		}

		if (fLabelDecorators != null && result != null) {
			for (int i= 0; i < fLabelDecorators.length; i++) {
				result= fLabelDecorators[i].decorateImage(result, element);
			}
		}			
		return result;
	}

	private ImageDescriptor getTypeImageDescriptor(IType type) {
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy == null) {
			return JavaPluginImages.DESC_OBJS_CLASS;
		}
		
		int flags= hierarchy.getCachedFlags(type);
		if (Flags.isInterface(flags)) {
			if (isDifferentScope(type)) {
				return JavaPluginImages.DESC_OBJS_INTERFACEALT;
			} else if (type.getDeclaringType() != null) {
				return getInnerInterfaceImageDescriptor(flags);
			} else {
				return getInterfaceImageDescriptor(flags);
			}
		} else {
			if (isDifferentScope(type)) {
				return JavaPluginImages.DESC_OBJS_CLASSALT;
			} else if (type.getDeclaringType() != null) {
				return getInnerClassImageDescriptor(flags);
			} else {
				return getClassImageDescriptor(flags);
			}
		}
	}
	
	private ImageDescriptor getClassImageDescriptor(int flags) {
		if (Flags.isPublic(flags) || Flags.isProtected(flags) || Flags.isPrivate(flags))
			return JavaPluginImages.DESC_OBJS_CLASS;
		else
			return JavaPluginImages.DESC_OBJS_CLASS_DEFAULT;
	}
	
	private ImageDescriptor getInnerClassImageDescriptor(int flags) {
		if (Flags.isPublic(flags))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS;
		else if (Flags.isPrivate(flags))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PRIVATE;
		else if (Flags.isProtected(flags))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PROTECTED;
		else
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_DEFAULT;
	}
	
	private ImageDescriptor getInterfaceImageDescriptor(int flags) {
		if (Flags.isPublic(flags) || Flags.isProtected(flags) || Flags.isPrivate(flags))
			return JavaPluginImages.DESC_OBJS_INTERFACE;
		else
			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
	}
	
	private ImageDescriptor getInnerInterfaceImageDescriptor(int flags) {
		if (Flags.isPublic(flags))
			return JavaPluginImages.DESC_OBJS_INTERFACE;
		else if (Flags.isPrivate(flags))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PRIVATE;
		else if (Flags.isProtected(flags))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PROTECTED;
		else
			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
	}		
}
