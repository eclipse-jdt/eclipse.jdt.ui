/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.texteditor.IUpdate;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;import org.eclipse.jdt.internal.ui.util.SelectionUtil;

/**
 * Refocuses the type hierarchy on the currently selection type.
 */
public class FocusOnSelectionAction extends JavaUIAction implements IUpdate {
	
	private static final String PREFIX= "FocusOnSelectionAction.";
		
	private TypeHierarchyViewPart fViewPart;
	private ISelectionProvider fSelectionProvider;
	private String fText;
	
	public FocusOnSelectionAction(TypeHierarchyViewPart part, ISelectionProvider selProvider) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fViewPart= part;
		fSelectionProvider= selProvider;
		fText= JavaPlugin.getResourceString(PREFIX + "label");
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.FOCUS_ON_SELECTION_ACTION });
	}
	/**
	 *orm the action
	 */
	public void run() {
		Object element= SelectionUtil.getSingleElement(fSelectionProvider.getSelection());
		if (element instanceof IType) {
			fViewPart.setInput((IType)element);
		}
	}
	
	public void update() {
		setEnabled(canActionBeAdded());
	}
	
	public boolean canActionBeAdded() {
		Object element= SelectionUtil.getSingleElement(fSelectionProvider.getSelection());
		if (element instanceof IType) {
			setText(fText + " \"" + ((IType)element).getElementName() + "\"");
			return true;
		}
		return false;
	}
}
