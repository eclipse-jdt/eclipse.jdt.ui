/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.actions.JavaUIAction;import org.eclipse.jdt.internal.ui.dialogs.TypeSelectionDialog;import org.eclipse.jdt.ui.IJavaElementSearchConstants;

/**
 * Refocuses the type hierarchy on a type selection from a all types dialog.
 */
public class FocusOnTypeAction extends JavaUIAction {
	
	private static final String PREFIX= "FocusOnTypeAction.";
		
	private TypeHierarchyViewPart fViewPart;
	private ISelectionProvider fSelectionProvider;
	
	public FocusOnTypeAction(TypeHierarchyViewPart part) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fViewPart= part;
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.FOCUS_ON_TYPE_ACTION });
	}
	/**
	 *orm the action
	 */
	public void run() {
		Shell parent= fViewPart.getSite().getWorkbenchWindow().getShell();
		TypeSelectionDialog dialog= new TypeSelectionDialog(parent, new ProgressMonitorDialog(parent), 
			SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, 
			true, true);				
	
		dialog.setTitle(JavaPlugin.getResourceString(PREFIX + "dialog.title"));
		dialog.setMessage(JavaPlugin.getResourceString(PREFIX + "dialog.message"));
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			IType type= (IType)types[0];
			fViewPart.setInput(type);
		}
	}	
}
