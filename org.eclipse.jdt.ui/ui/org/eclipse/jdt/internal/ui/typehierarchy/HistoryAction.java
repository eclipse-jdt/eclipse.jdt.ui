/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

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
		
		String elementName= JavaElementLabels.getElementLabel(element, JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES);
		setText(elementName);
		setImageDescriptor(getImageDescriptor(element));
				
		setDescription(TypeHierarchyMessages.getFormattedString("HistoryAction.description", elementName)); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getFormattedString("HistoryAction.tooltip", elementName)); //$NON-NLS-1$
	}
	
	private ImageDescriptor getImageDescriptor(IJavaElement elem) {
		JavaElementImageProvider imageProvider= new JavaElementImageProvider();
		return imageProvider.getBaseImageDescriptor(elem, 0);
	}
	
	/*
	 * @see Action#run()
	 */
	public void run() {
		fViewPart.gotoHistoryEntry(fElement);
	}
	
}
