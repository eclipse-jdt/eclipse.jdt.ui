/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;

/**
 * Action used for the type hierarchy forward / backward buttons
 */
public class HistoryAction extends Action {

	private TypeHierarchyViewPart fViewPart;
	private IJavaElement fElement;
	
	public HistoryAction(TypeHierarchyViewPart viewPart, IJavaElement element) {
		super();
		fViewPart= viewPart;
		fElement= element;		
		
		JavaTextLabelProvider labelProvider= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
		String elementName= labelProvider.getTextLabel(element);
		setText(elementName);
		setImageDescriptor(getImageDescriptor(element));
				
		setDescription(TypeHierarchyMessages.getFormattedString("HistoryAction.description", elementName)); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getFormattedString("HistoryAction.tooltip", elementName)); //$NON-NLS-1$
	}
	
	private ImageDescriptor getImageDescriptor(IJavaElement elem) {
		try {
			switch (elem.getElementType()) {
				case IJavaElement.TYPE:
					if (((IType) elem).isClass()) {
						return JavaPluginImages.DESC_OBJS_CLASS;
					} else {
						return JavaPluginImages.DESC_OBJS_INTERFACE;
					}
				case IJavaElement.PACKAGE_FRAGMENT:
					return JavaPluginImages.DESC_OBJS_PACKAGE;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:							
					return JavaPluginImages.DESC_OBJS_PACKFRAG_ROOT;
				case IJavaElement.JAVA_PROJECT:							
					ISharedImages images= JavaPlugin.getDefault().getWorkbench().getSharedImages();
					return images.getImageDescriptor(ISharedImages.IMG_OBJ_PROJECT);
				default:
			}
					
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return JavaPluginImages.DESC_OBJS_GHOST;
	}
	
	
	/*
	 * @see Action#run()
	 */
	public void run() {
		fViewPart.gotoHistoryEntry(fElement);
	}
	
}
