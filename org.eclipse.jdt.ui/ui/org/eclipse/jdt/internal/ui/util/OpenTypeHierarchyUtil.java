/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
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

public class OpenTypeHierarchyUtil {
	
	private OpenTypeHierarchyUtil() {
	}

	public static boolean canOperateOn(ISelection s) {
		Object element= getElement(s);
			
		return (element != null) 
			? (getCandidates(element) != null) 
			: false;
	}
	
	public static void addToMenu(IWorkbenchWindow window, IMenuManager menu, ISelection s) {
		addToMenu(window, menu, getElement(s));
	}
	
	public static void addToMenu(IWorkbenchWindow window, IMenuManager menu, Object element) {	
		IJavaElement[] candidates= getCandidates(element);
		if (candidates != null) {
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, new OpenHierarchyPerspectiveItem(window, candidates));
		}
	}
	
	public static TypeHierarchyViewPart open(ISelection selection, IWorkbenchWindow window) {
		IJavaElement[] candidates= getCandidates(getElement(selection));
		if (candidates != null) {
			return open(candidates, window);
		}
		return null;
	}
	
	public static TypeHierarchyViewPart open(IJavaElement[] candidates, IWorkbenchWindow window) {
		IPreferenceStore store= WorkbenchPlugin.getDefault().getPreferenceStore();
		String perspectiveSetting=
			store.getString(IWorkbenchPreferenceConstants.OPEN_NEW_PERSPECTIVE);
		return open(candidates, window, perspectiveSetting);	
	}
	
	public static TypeHierarchyViewPart open(IJavaElement[] candidates, IWorkbenchWindow window, String setting) {
		Assert.isTrue(candidates != null && candidates.length != 0);
			
		IJavaElement input= null;
		if (candidates.length > 1) {
			input= selectCandidate(candidates, window.getShell());
		} else {
			input= candidates[0];
		}
		if (input == null)
			return null;
			
		try {
			if (JavaBasePreferencePage.openTypeHierarchyInPerspective()) {
				openInPerspective(window, input, setting);
			} else {
				openInViewPart(window, input);
			}
				
		} catch (WorkbenchException e) {
			JavaPlugin.log(e);
			MessageDialog.openError(window.getShell(),
				JavaUIMessages.getString("OpenTypeHierarchyUtil.error.open_perspective"), //$NON-NLS-1$
				e.getMessage());
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
			MessageDialog.openError(window.getShell(),
				JavaUIMessages.getString("OpenTypeHierarchyUtil.error.open_editor"), //$NON-NLS-1$
				e.getMessage());
		}
		return null;
	}

	private static TypeHierarchyViewPart openInViewPart(IWorkbenchWindow window, IJavaElement input) {
		IWorkbenchPage page= window.getActivePage();
		try {
			// 1GEUMSG: ITPJUI:WINNT - Class hierarchy not shown when fast view
			if (input.getElementType() == IJavaElement.TYPE) {
				openEditor(input);
			}
			TypeHierarchyViewPart result= (TypeHierarchyViewPart)page.showView(JavaUI.ID_TYPE_HIERARCHY);
			result.setInputElement(input);
			return result;
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			MessageDialog.openError(window.getShell(), 
				JavaUIMessages.getString("OpenTypeHierarchyUtil.error.open_view"), e.getMessage()); //$NON-NLS-1$
		}
		return null;		
	}
	
	private static TypeHierarchyViewPart openInPerspective(IWorkbenchWindow window, IJavaElement input, String setting) throws WorkbenchException, JavaModelException {
		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor pd= registry.findPerspectiveWithId(JavaUI.ID_HIERARCHYPERSPECTIVE);
		if (pd == null) {
			JavaPlugin.getDefault().logErrorMessage(JavaUIMessages.getString("OpenTypeHierarchyUtil.error.no_perspective")); //$NON-NLS-1$
			return null;
		}
		
		if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_WINDOW))
			openWindow(window, pd, input);
		
		else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_PAGE))
			openPage(window, pd, input);
		
		else if (setting.equals(IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_REPLACE))
			// We can't change the input of a perspective. So fall back an open 
			// a new one.
			openPage(window, pd, input);
		return null;
	}

	private static void openEditor(Object input) throws PartInitException, JavaModelException {
		IEditorPart part= EditorUtility.openInEditor(input, true);
		if (input instanceof ISourceReference)
			EditorUtility.revealInEditor(part, (ISourceReference)input);
	}
	
	private static void openWindow(IWorkbenchWindow activeWindow, IPerspectiveDescriptor pd, IJavaElement input) throws WorkbenchException, JavaModelException {
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
		if (input.getElementType() == IJavaElement.TYPE) {
			openEditor(input);
		}
	}

	private static void openPage(IWorkbenchWindow window, IPerspectiveDescriptor pd, IJavaElement input) throws WorkbenchException, JavaModelException {
		IWorkbenchPage page= findPageFor(window, input);
		if (page != null) {
			window.setActivePage(page);
		} else {
			window.openPage(pd.getId(), input);
			if (input.getElementType() == IJavaElement.TYPE) {
				openEditor(input);
			}
		}
	}

	private static IWorkbenchPage findPageFor(IWorkbenchWindow window, IJavaElement input) {
		IWorkbenchPage pages[]= window.getPages();
		for (int i= 0; i < pages.length; i++) {
			IWorkbenchPage page= pages[i];
			if (input.equals(page.getInput()))
				return page;
		}
		return null;
	}
	
	private static IJavaElement selectCandidate(IJavaElement[] candidates, Shell shell) {		
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT);						

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell,			
			new JavaElementLabelProvider(flags));
		dialog.setTitle(JavaUIMessages.getString("OpenTypeHierarchyUtil.selectionDialog.title"));  //$NON-NLS-1$
		dialog.setMessage(JavaUIMessages.getString("OpenTypeHierarchyUtil.selectionDialog.message")); //$NON-NLS-1$
		dialog.setElements(candidates);

		if (dialog.open() == dialog.OK) {
			Object[] elements= dialog.getResult();
			if ((elements != null) && (elements.length == 1))
				return (IJavaElement) elements[0];
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
	 * Converts the input to a possible input candidates
	 */	
	private static IJavaElement[] getCandidates(Object input) {
		if (!(input instanceof IJavaElement)) {
			return null;
		}
		IJavaElement elem= (IJavaElement) input;
		switch (elem.getElementType()) {
			case IJavaElement.TYPE:
			case IJavaElement.PACKAGE_FRAGMENT:
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			case IJavaElement.JAVA_PROJECT:
				return new IJavaElement[] { elem };
			case IJavaElement.CLASS_FILE:
				try {
					IType type= ((IClassFile)input).getType();
					return new IJavaElement[] { type };
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
					return null;
				}
			case IJavaElement.COMPILATION_UNIT:
				try {
					IType[] types= ((ICompilationUnit)input).getAllTypes();
					if (types == null || types.length == 0)
						return null;
					return types;
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
					return null;
				}
			default:
		}
		return null;	
	}	
}
