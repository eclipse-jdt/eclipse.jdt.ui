package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.internal.debug.ui.display.DisplayAction;
import org.eclipse.jdt.internal.debug.ui.display.DisplayView;
import org.eclipse.jdt.internal.debug.ui.display.IDataDisplay;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Displays the result of an evaluation in the Java editor
 */
public class EditorDisplayAction extends DisplayAction {

	public EditorDisplayAction(IWorkbenchPart part, boolean usedInEditor) {
		super(part, usedInEditor);
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.DISPLAY_ACTION });	
	}
	
	protected IDataDisplay getDataDisplay() {
		
		IWorkbenchPage page= JavaPlugin.getDefault().getActivePage();
		IWorkbenchPart activePart= page.getActivePart();
		IViewPart view= page.findView(DisplayView.ID_DISPLAY_VIEW);
		if (view == null) {
			try {
				view= page.showView(DisplayView.ID_DISPLAY_VIEW);		
			} catch (PartInitException e) {
				MessageDialog.openError(getShell(), JavaEditorMessages.getString("EditorDisplay.error.title1"), e.getMessage()); //$NON-NLS-1$
			} finally {
				page.activate(activePart);
			}
		}
		
		if (view != null) {
			page.bringToTop(view);
			Object value= view.getAdapter(IDataDisplay.class);
			if (value instanceof IDataDisplay) {
				return (IDataDisplay) value;
			}	
		}
		
		return null;
	}
}