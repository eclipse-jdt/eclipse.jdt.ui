/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.WorkbenchPlugin;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.OpenHierarchyPerspectiveItem;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;

public class OpenTypeHierarchyHelper {

	private TypeHierarchyViewPart fTypeHierarchy;

	public OpenTypeHierarchyHelper() {
	}

	public static boolean canOperateOn(ISelection s) {
		Object element= getElement(s);
			
		return (element != null) 
			? (convertToTypes(element) != null) 
			: false;
	}
	
	public static void addToMenu(IWorkbenchWindow window, IMenuManager menu, ISelection s) {
		addToMenu(window, menu, getElement(s));
	}
	
	public static void addToMenu(IWorkbenchWindow window, IMenuManager menu, Object element) {	
		IType[] types= convertToTypes(element);
		if (types != null) {
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, new OpenHierarchyPerspectiveItem(window, types));
		}
	}
	
	public void open(ISelection selection, IWorkbenchWindow window) {
		open(convertToTypes(getElement(selection)), window);
	}
	
	public void open(IType[] types, IWorkbenchWindow window) {
		IPreferenceStore store= WorkbenchPlugin.getDefault().getPreferenceStore();
		String perspectiveSetting=
			store.getString(IWorkbenchPreferenceConstants.OPEN_NEW_PERSPECTIVE);
		open(types, window, perspectiveSetting);	
	}
	
	public void open(IType[] types, IWorkbenchWindow window, String setting) {
		if (types == null || types.length == 0) {
			window.getShell().getDisplay().beep();
			return;
		}
			
		IType input= null;
		if (types.length > 1) {
			input= selectType(types, window.getShell());
		} else {
			input= types[0];
		}
		if (input == null)
			return;
			
		try {
			if (JavaBasePreferencePage.openTypeHierarchyInPerspective()) {
				openInPerspective(window, input, setting);
			} else {
				openInViewPart(window, input);
			}
				
		} catch (WorkbenchException e) {
			JavaPlugin.log(e);
			MessageDialog.openError(window.getShell(),
				JavaUIMessages.getString("OpenTypeHierarchyHelper.error.open_perspective"), //$NON-NLS-1$
				e.getMessage());
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
			MessageDialog.openError(window.getShell(),
				JavaUIMessages.getString("OpenTypeHierarchyHelper.error.open_editor"), //$NON-NLS-1$
				e.getMessage());
		}
	}

	public void selectMember(IMember member) {
		if (member == null || fTypeHierarchy == null)
			return;
		fTypeHierarchy.selectMember(member);	
	}
	
	private void openInViewPart(IWorkbenchWindow window, IType input) {
		IWorkbenchPage page= window.getActivePage();
		try {
			// 1GEUMSG: ITPJUI:WINNT - Class hierarchy not shown when fast view
			openEditor(input);
			fTypeHierarchy= (TypeHierarchyViewPart)page.showView(JavaUI.ID_TYPE_HIERARCHY);
			fTypeHierarchy.setInput(input);
		} catch (PartInitException e) {
			JavaPlugin.log(e);
			MessageDialog.openError(window.getShell(), 
				JavaUIMessages.getString("OpenTypeHierarchyHelper.error.open_view"), e.getMessage()); //$NON-NLS-1$
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
			MessageDialog.openError(window.getShell(), 
				JavaUIMessages.getString("OpenTypeHierarchyHelper.error.open_view"), e.getMessage()); //$NON-NLS-1$
		}		
	}
	
	private void openInPerspective(IWorkbenchWindow window, IType input, String setting) throws WorkbenchException, JavaModelException {
		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor pd= registry.findPerspectiveWithId(JavaUI.ID_HIERARCHYPERSPECTIVE);
		if (pd == null) {
			JavaPlugin.getDefault().logErrorMessage("Type Hierarchy perspective not found"); //$NON-NLS-1$
			return;
		}
		
		if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_WINDOW))
			openWindow(window, pd, input);
		
		else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_PAGE))
			openPage(window, pd, input);
		
		else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_REPLACE))
			// We can't change the input of a perspective. So fall back an open 
			// a new one.
			openPage(window, pd, input);			
	}

	private void openEditor(Object input) throws PartInitException, JavaModelException {
		IEditorPart part= EditorUtility.openInEditor(input, false);
		if (input instanceof ISourceReference)
			EditorUtility.revealInEditor(part, (ISourceReference)input);
	}
	
	private void openWindow(IWorkbenchWindow activeWindow, IPerspectiveDescriptor pd, IType input) throws WorkbenchException, JavaModelException {
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
		openEditor(input);	
	}

	private void openPage(IWorkbenchWindow window, IPerspectiveDescriptor pd, IType input) throws WorkbenchException, JavaModelException {
		IWorkbenchPage page= findPageFor(window, input);
		if (page != null) {
			window.setActivePage(page);
		} else {
			window.openPage(pd.getId(), input);
			openEditor(input);	
		}
	}

	private IWorkbenchPage findPageFor(IWorkbenchWindow window, IType input) {
		IWorkbenchPage pages[]= window.getPages();
		for (int i= 0; i < pages.length; i++) {
			IWorkbenchPage page= pages[i];
			if (input.equals(page.getInput()))
				return page;
		}
		return null;
	}
	
	private IType selectType(IType[] types, Shell shell) {		
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT);						

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell,			
			new JavaElementLabelProvider(flags));
		dialog.setTitle(JavaUIMessages.getString("OpenTypeHierarchyHelper.selectionDialog.title"));  //$NON-NLS-1$
		dialog.setMessage(JavaUIMessages.getString("OpenTypeHierarchyHelper.selectionDialog.message")); //$NON-NLS-1$
		dialog.setElements(types);

		if (dialog.open() == dialog.OK) {
			Object[] elements= dialog.getResult();
			if ((elements != null) && (elements.length == 1))
				return (IType) elements[0];
		}
		return null;
	}
	
	private static Object getElement(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return null;
		IStructuredSelection selection= (IStructuredSelection)s;
		if (selection.size() != 1)
			return null;
		return selection.getFirstElement();	
	}
	
	/**
	 * Converts the input to an IType if possible 
	 */	
	private static IType[] convertToTypes(Object input) {
		if (input instanceof IType) { 
			IType[] result= {(IType)input};
			return result;
		} 
		if (input instanceof IClassFile) {
			try {
				IType type= ((IClassFile)input).getType();
				IType[] result= {(IType)type};
				return result;
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
		if (input instanceof ICompilationUnit) {
			try {
				IType[] types= ((ICompilationUnit)input).getAllTypes();
				if (types == null || types.length == 0)
					return null;
				return types;
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
				return null;
			}
		}
		return null;	
	}	
}
