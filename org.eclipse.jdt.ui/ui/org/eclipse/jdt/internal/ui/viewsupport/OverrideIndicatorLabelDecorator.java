package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;

/**
  */
public class OverrideIndicatorLabelDecorator implements ILabelDecorator {

	private ImageDescriptorRegistry fRegistry;

	/**
	 * Constructor for OverrideIndicatorLabelDecorator. Internal usage only!
	 */
	public OverrideIndicatorLabelDecorator() {
		fRegistry= JavaPlugin.getImageDescriptorRegistry();
	}
	
	/* (non-Javadoc)
	 * @see ILabelDecorator#decorateText(String, Object)
	 */
	public String decorateText(String text, Object element) {
		return text;
	}	

	/* (non-Javadoc)
	 * @see ILabelDecorator#decorateImage(Image, Object)
	 */
	public Image decorateImage(Image image, Object element) {
		int adornmentFlags= computeAdornmentFlags(element);
		if (adornmentFlags != 0) {
			ImageDescriptor baseImage= new ImageImageDescriptor(image);
			Rectangle bounds= image.getBounds();
			return fRegistry.get(new JavaElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
		}
		return image;
	}
	
	public int computeAdornmentFlags(Object element) {
		if (AppearancePreferencePage.showOverrideIndicators()) {
			if (element instanceof IMethod) {
				try {
					IMethod method= (IMethod) element;
					int flags= method.getFlags();
					if (method.getDeclaringType().isClass() && !method.isConstructor() && !Flags.isPrivate(flags) && !Flags.isStatic(flags)) {
						return getOverrideIndicators(method);
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return 0;
	}
	
	protected int getOverrideIndicators(IMethod method) throws JavaModelException {
		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(type);
		if (hierarchy != null) {
			return findInHierarchy(type, hierarchy, method.getElementName(), method.getParameterTypes());
		}
		return 0;
	}
	
	protected int findInHierarchy(IType type, ITypeHierarchy hierarchy, String name, String[] paramTypes) throws JavaModelException {
		IMethod impl= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, name, paramTypes, false);
		if (impl != null) {
			IMethod overridden= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, name, paramTypes, false);
			if (overridden != null) {
				return JavaElementImageDescriptor.OVERRIDES;
			} else {
				return JavaElementImageDescriptor.IMPLEMENTS;
			}
		}
		return 0;
	}	 

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	/* (non-Javadoc)
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}

}
