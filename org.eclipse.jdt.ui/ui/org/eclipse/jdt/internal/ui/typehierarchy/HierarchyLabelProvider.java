package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.viewsupport.JavaImageLabelProvider;

public class HierarchyLabelProvider extends JavaElementLabelProvider {

	private TypeHierarchyViewPart fViewPart;

	/**
	 * Constructor for HierarchyLabelProvider.
	 * @param flags
	 */
	public HierarchyLabelProvider(int flags, TypeHierarchyViewPart viewPart) {
		super(flags);
		fViewPart= viewPart;
	}

	/*
	 * @see ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof IType) {
			IJavaElement input= fViewPart.getInputElement();
			if (input != null && input.getElementType() != IJavaElement.TYPE) {
				IJavaElement parent= JavaModelUtil.findElementOfKind((IType) element, input.getElementType());
				try {
					if (!input.equals(parent)) {
						turnOn(JavaImageLabelProvider.SHOW_ALTERNATIVE_TYPE_ICONS);
					} else {
						turnOff(JavaImageLabelProvider.SHOW_ALTERNATIVE_TYPE_ICONS);
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return super.getImage(element);
	}
}

