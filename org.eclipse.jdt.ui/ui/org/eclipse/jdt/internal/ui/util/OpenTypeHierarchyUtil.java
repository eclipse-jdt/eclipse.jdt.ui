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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.WorkbenchPlugin;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.OpenHierarchyAction;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.preferences.JavaBasePreferencePage;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;

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
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, new OpenHierarchyAction(window, candidates));
		}
	}
	
	public static TypeHierarchyViewPart open(ISelection selection, IWorkbenchWindow window) {
		Object element= getElement(selection);
		if (element instanceof IJavaElement) {
			return open((IJavaElement)element, window);
		}
		return null;
	}
	
	public static TypeHierarchyViewPart open(IJavaElement element, IWorkbenchWindow window) {
		IJavaElement[] candidates= getCandidates(element);
		if (candidates != null) {
			return open(candidates, window);
		}
		return null;
	}	
	
	public static TypeHierarchyViewPart open(IJavaElement[] candidates, IWorkbenchWindow window) {
		return open(candidates, window, 0);	
	}
	
	public static TypeHierarchyViewPart open(IJavaElement[] candidates, IWorkbenchWindow window, int  mask) {
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
				return openInPerspective(window, input, mask);
			} else {
				return openInViewPart(window, input);
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
			if (input instanceof IMember) {
				openEditor(input);
			}
			TypeHierarchyViewPart result= (TypeHierarchyViewPart)page.showView(JavaUI.ID_TYPE_HIERARCHY);
			result.setInputElement(input);
			if (input instanceof IMember) {
				result.selectMember((IMember) input);
			}
			return result;
		} catch (CoreException e) {
			JavaPlugin.log(e.getStatus());
			MessageDialog.openError(window.getShell(), 
				JavaUIMessages.getString("OpenTypeHierarchyUtil.error.open_view"), e.getMessage()); //$NON-NLS-1$
		}
		return null;		
	}
	
	private static TypeHierarchyViewPart openInPerspective(IWorkbenchWindow window, IJavaElement input, int mask) throws WorkbenchException, JavaModelException {
		IPreferenceStore store= WorkbenchPlugin.getDefault().getPreferenceStore();
		String mode= store.getString(IWorkbenchPreferenceConstants.OPEN_NEW_PERSPECTIVE);
		IWorkbenchPage page= null;
		if (IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_WINDOW.equals(mode)) {
			page= openWindow(input, mask);
		} else if (IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_PAGE.equals(mode)) {
			page= openPage(window, input, mask);
		}
		if (input instanceof IMember) {
			openEditor(input);
		}
		return (TypeHierarchyViewPart)page.showView(JavaUI.ID_TYPE_HIERARCHY);
	}

	private static void openEditor(Object input) throws PartInitException, JavaModelException {
		IEditorPart part= EditorUtility.openInEditor(input, true);
		if (input instanceof IJavaElement)
			EditorUtility.revealInEditor(part, (IJavaElement) input);
	}
	
	private static IWorkbenchPage openWindow(IJavaElement input, int mask) throws WorkbenchException, JavaModelException {
		return PlatformUI.getWorkbench().openPage(JavaUI.ID_HIERARCHYPERSPECTIVE, input, mask);
	}

	private static IWorkbenchPage openPage(IWorkbenchWindow window, IJavaElement input, int mask) throws WorkbenchException, JavaModelException {
		IWorkbenchPage page= null;
		/*
		 * not implementable in the current form. See http://dev.eclipse.org/bugs/show_bug.cgi?id=3962
		if (JavaBasePreferencePage.reusePerspectiveForTypeHierarchy()) {
			page= findPage(window);
			if (page != null) {
				window.setActivePage(page);
				TypeHierarchyViewPart part= (TypeHierarchyViewPart)page.showView(JavaUI.ID_TYPE_HIERARCHY);
				if (input instanceof IType)
					part.setInputElement((IType)input);
			}
		}
		*/
		if (page == null) {
			page= PlatformUI.getWorkbench().openPage(JavaUI.ID_HIERARCHYPERSPECTIVE, input, mask);	
		}
		return page;
	}

	private static IWorkbenchPage findPage(IWorkbenchWindow window) {
		IPerspectiveRegistry registry= PlatformUI.getWorkbench().getPerspectiveRegistry();
		IPerspectiveDescriptor pd= registry.findPerspectiveWithId(JavaUI.ID_HIERARCHYPERSPECTIVE);
		IWorkbenchPage pages[]= window.getPages();
		for (int i= 0; i < pages.length; i++) {
			IWorkbenchPage page= pages[i];
			if (page.getPerspective().equals(pd))
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
	public static IJavaElement[] getCandidates(Object input) {
		if (!(input instanceof IJavaElement)) {
			return null;
		}
		try {
			IJavaElement elem= (IJavaElement) input;
			switch (elem.getElementType()) {
				case IJavaElement.INITIALIZER:
				case IJavaElement.METHOD:
				case IJavaElement.FIELD:
				case IJavaElement.TYPE:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.JAVA_PROJECT:
					return new IJavaElement[] { elem };
				case IJavaElement.CLASS_FILE:
					return new IJavaElement[] { ((IClassFile)input).getType() };				
				case IJavaElement.COMPILATION_UNIT:
				case IJavaElement.IMPORT_CONTAINER:
				case IJavaElement.IMPORT_DECLARATION:
				case IJavaElement.PACKAGE_DECLARATION: {
					ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(elem, IJavaElement.COMPILATION_UNIT);
					if (cu != null) {
						IType[] types= cu.getTypes();
						if (types.length > 0) {
							return types;
						}
					}
					break;
				}					
				default:
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return null;	
	}	
}
