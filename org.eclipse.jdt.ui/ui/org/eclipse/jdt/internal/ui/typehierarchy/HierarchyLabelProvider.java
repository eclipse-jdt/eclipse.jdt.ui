package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.viewsupport.IErrorTickProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaImageLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;

public class HierarchyLabelProvider extends LabelProvider {

	private TypeHierarchyViewPart fViewPart;
	private JavaTextLabelProvider fTextLabelProvider;
	private JavaImageLabelProvider fImageLabelProvider;

	/**
	 * Constructor for HierarchyLabelProvider.
	 * @param flags
	 */
	public HierarchyLabelProvider(TypeHierarchyViewPart viewPart, IErrorTickProvider provider) {
		super();
		fViewPart= viewPart;
		fTextLabelProvider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		fImageLabelProvider= new JavaImageLabelProvider();
		fImageLabelProvider.setErrorTickProvider(provider);
	}

	/*
	 * @see ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object obj) {
		if (obj instanceof IJavaElement) {
			IJavaElement element= (IJavaElement) obj;
			int flags= JavaImageLabelProvider.OVERLAY_ICONS;

			IJavaElement input= fViewPart.getInputElement();
			if (input != null && input.getElementType() != IJavaElement.TYPE && element.getElementType() == IJavaElement.TYPE) {
				IJavaElement parent= JavaModelUtil.findElementOfKind((IType) element, input.getElementType());
				if (!input.equals(parent)) {
					flags |= JavaImageLabelProvider.LIGHT_TYPE_ICONS;
				}
			}
			return fImageLabelProvider.getLabelImage(element, flags); 
		}
		return super.getImage(obj);
	}
	
	/*
	 * @see ILabelProvider#getText(Object)
	 */
	public String getText(Object obj) {
		if (obj instanceof IJavaElement) {
			return fTextLabelProvider.getTextLabel((IJavaElement) obj);
		}
		return super.getText(obj);
	}

}

