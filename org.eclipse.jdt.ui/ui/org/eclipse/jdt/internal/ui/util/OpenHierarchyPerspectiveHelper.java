/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.ui.IPerspectiveDescriptor;import org.eclipse.ui.IPerspectiveRegistry;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchPreferenceConstants;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.PlatformUI;import org.eclipse.ui.WorkbenchException;import org.eclipse.ui.internal.WorkbenchPlugin;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.ui.JavaUI;

public class OpenHierarchyPerspectiveHelper {

	private static String fgMessage= "Select the element to be used as input";
	
	public static void run(IType[] types, IWorkbenchWindow window) {
		IPreferenceStore store= WorkbenchPlugin.getDefault().getPreferenceStore();
		String perspectiveSetting=
			store.getString(IWorkbenchPreferenceConstants.OPEN_NEW_PERSPECTIVE);
		run(types, window, perspectiveSetting);	
	}
	
	public static void run(IType[] types, IWorkbenchWindow window, String setting) {
		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor pd= registry.findPerspectiveWithId(JavaUI.ID_HIERARCHYPERSPECTIVE);
		if (pd == null) {
			JavaPlugin.getDefault().logErrorMessage("Type Hierarchy perspective not found");
			return;
		}
		IType input= null;
		if (types.length > 1) {
			input= selectType(types, window.getShell(), "Select Type", fgMessage);
		} else {
			input= types[0];
		}
		if (input == null)
			return;
			
		try {
			if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_WINDOW))
				openWindow(window, pd, input);
			
			else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_PAGE))
				openPage(window, pd, input);
			
			else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_REPLACE))
				// We can't change the input of a perspective. So fall back an open 
				// a new one.
				openPage(window, pd, input);
				
			EditorUtility.openInEditor(input);
		} catch (WorkbenchException e) {
			MessageDialog.openError(window.getShell(),
				"Problems Opening Perspective",
				e.getMessage());
		} catch (JavaModelException e) {
			MessageDialog.openError(window.getShell(),
				"Problems Opening Editor",
				e.getMessage());
		}
	}

	private static void openWindow(IWorkbenchWindow activeWindow, IPerspectiveDescriptor pd, IType input) throws WorkbenchException {
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchWindow window= windows[i];
			if (window.equals(activeWindow))
				continue;
				
			IWorkbenchPage page= findPageFor(window, input);
			if (page != null) {
				Shell shell= window.getShell();
				shell.moveAbove(null);
				shell.setFocus();
				window.setActivePage(page);
				return;
			}
		}
		workbench.openWorkbenchWindow(pd.getId(), input);
	}

	private static void openPage(IWorkbenchWindow window, IPerspectiveDescriptor pd, IType input) throws WorkbenchException {
		IWorkbenchPage page= findPageFor(window, input);
		if (page != null)
			window.setActivePage(page);
		else
			window.openPage(pd.getId(), input);
	}

	private static IWorkbenchPage findPageFor(IWorkbenchWindow window, IType input) {
		IWorkbenchPage pages[]= window.getPages();
		for (int i= 0; i < pages.length; i++) {
			IWorkbenchPage page= pages[i];
			if (input.equals(page.getInput()))
				return page;
		}
		return null;
	}
	
	private static IType selectType(IType[] types, Shell shell, String title, String message) {		
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT);						
		ElementListSelectionDialog d= new ElementListSelectionDialog(shell, title, null, new JavaElementLabelProvider(flags), true, false);
		d.setMessage(message);
		if (d.open(types, null) == d.OK) {
			Object[] elements= d.getResult();
			if (elements != null && elements.length == 1) {
				return ((IType)elements[0]);
			}
		}
		return null;
	}
	
}
