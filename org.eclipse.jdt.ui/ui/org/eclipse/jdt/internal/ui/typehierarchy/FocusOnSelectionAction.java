/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

/**
 * Refocuses the type hierarchy on the currently selection type.
 */
public class FocusOnSelectionAction extends Action {
		
	private TypeHierarchyViewPart fViewPart;
	private ISelectionProvider fSelectionProvider;
	private String fText;
	
	public FocusOnSelectionAction(TypeHierarchyViewPart part) {
		super(TypeHierarchyMessages.getString("FocusOnSelectionAction.label")); //$NON-NLS-1$
		setDescription(TypeHierarchyMessages.getString("FocusOnSelectionAction.description")); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getString("FocusOnSelectionAction.tooltip")); //$NON-NLS-1$
		fViewPart= part;
		fText= TypeHierarchyMessages.getString("FocusOnSelectionAction.label"); //$NON-NLS-1$
		
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FOCUS_ON_SELECTION_ACTION);
	}
	
	private ISelection getSelection() {
		ISelectionProvider provider= fViewPart.getSite().getSelectionProvider();
		if (provider != null) {
			return provider.getSelection();
		}
		return null;
	}
	

	/*
	 * @see Action#run
	 */
	public void run() {
		Object element= SelectionUtil.getSingleElement(getSelection());
		if (element instanceof IType) {
			fViewPart.setInputElement((IType)element);
		}
	}	
	
	public boolean canActionBeAdded() {
		Object element= SelectionUtil.getSingleElement(getSelection());
		if (element instanceof IType) {
			setText(TypeHierarchyMessages.getFormattedString("FocusOnSelectionAction.label", ((IType)element).getElementName())); //$NON-NLS-1$
			return true;
		}
		return false;
	}
}
