/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.action.IAction;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.viewers.ISelection;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.IWorkbenchWindowActionDelegate;import org.eclipse.ui.PartInitException;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.OpenHierarchyPerspectiveHelper;import org.eclipse.jdt.ui.IJavaElementSearchConstants;import org.eclipse.jdt.ui.ITypeHierarchyViewPart;import org.eclipse.jdt.ui.JavaUI;

public class OpenTypeAction extends OpenJavaElementAction implements IWorkbenchWindowActionDelegate {
	
	private static final String PREFIX= "OpenTypeAction.";
	private static final String DIALOG_PREFIX= PREFIX + "dialog.";
	private static final String ERROR_OPEN_PREFIX= PREFIX + "error.open.";
	
	public OpenTypeAction() {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		setImageDescriptor(JavaPluginImages.DESC_TOOL_OPENTYPE);
	}

	public void run() {
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		OpenTypeSelectionDialog dialog= new OpenTypeSelectionDialog(parent, new ProgressMonitorDialog(parent), 
			SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, 
			true, true);				
	
		dialog.setTitle(JavaPlugin.getResourceString(DIALOG_PREFIX + "title"));
		dialog.setMessage(JavaPlugin.getResourceString(DIALOG_PREFIX + "message"));
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			try {
				IType type= (IType)types[0];
				if (!dialog.showInTypeHierarchy()) {
					open(type);
				} else {	
					if (openPerspective()) {
						OpenHierarchyPerspectiveHelper.run(new IType[] { type }, JavaPlugin.getActiveWorkbenchWindow());
					} else {
						open(type);
						updateTypeHierarchyViewPart(type);
					}
				}
			} catch (JavaModelException x) {
				ExceptionHandler.handle(x, JavaPlugin.getResourceBundle(), ERROR_OPEN_PREFIX);
			} catch (PartInitException x) {
				ExceptionHandler.handle(x, JavaPlugin.getResourceBundle(), ERROR_OPEN_PREFIX);
			}
		}
				
	}

	private boolean openPerspective() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(IPreferencesConstants.OPEN_TYPE_DIALOG_OPEN_TYPE_HIERARCHY_PERSPECTIVE);
	}
	
	private void updateTypeHierarchyViewPart(IType type) {
		try {
			ITypeHierarchyViewPart part= (ITypeHierarchyViewPart)JavaPlugin.getActiveWorkbenchWindow().getActivePage().showView(JavaUI.ID_TYPE_HIERARCHY);
			if (part != null) {
				part.setInput(type);
			}
		} catch (PartInitException e) {
			ExceptionHandler.handle(e, JavaPlugin.getResourceBundle(), ERROR_OPEN_PREFIX);
		}
	}
	
	//---- IWorkbenchWindowActionDelegate ------------------------------------------------

	public void run(IAction action) {
		run();
	}
	
	public void dispose() {
		// do nothing.
	}
	
	public void init(IWorkbenchWindow window) {
		// do nothing.
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing. Action doesn't depend on selection.
	}
}