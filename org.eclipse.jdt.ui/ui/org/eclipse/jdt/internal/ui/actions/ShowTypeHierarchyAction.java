/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionProvider;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.ui.IViewPart;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.PartInitException;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.ui.texteditor.IUpdate;import org.eclipse.jdt.core.IClassFile;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;

/**
 * Shows the type hierarchy on a single selected element of type IType or IClassFile 
 */
public class ShowTypeHierarchyAction extends JavaUIAction implements IUpdate {
	
	private ISelectionProvider fSelectionProvider;
	
	public static final String PREFIX= "ShowTypeHierarchyAction.";
	public static final String ERROR_OPEN_VIEW= PREFIX+"error.open_view";
		
	public ShowTypeHierarchyAction(ISelectionProvider selProvider) {
		super(JavaPlugin.getResourceBundle(), PREFIX);
		fSelectionProvider= selProvider;
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.SHOW_IN_HIERARCHYVIEW_ACTION });	
	}

	/**
	 * Perform the action
	 */
	public void run() {
		ISelection sel= fSelectionProvider.getSelection();
		if (!(sel instanceof IStructuredSelection))
			return;
			
		Object element= ((IStructuredSelection)sel).getFirstElement();
		IType[] types= getAllTypesFrom(element);
		if (types == null || types.length == 0) {
			Display.getCurrent().beep();
			return;
		}
		
		IType type= determineType(types);
		if (type != null)
			showType(type);
	}

	private IType determineType(IType[] types) {
		if (types.length == 1)
			return types[0];

		String title= JavaPlugin.getResourceString(PREFIX + "selectionDialog.title");
		String message = JavaPlugin.getResourceString(PREFIX + "selectionDialog.message");
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT);						
		ElementListSelectionDialog d= new ElementListSelectionDialog(parent, title, null, new JavaElementLabelProvider(flags), true, false);
		d.setMessage(message);
		if (d.open(types, null) == d.OK) {
			Object[] elements= d.getResult();
			if (elements != null && elements.length == 1) {
				return ((IType)elements[0]);
			}
		}
		return null;
			
	}

	private void showType(IType type) {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		try {
			IViewPart view= page.showView(JavaUI.ID_TYPE_HIERARCHY);
			((TypeHierarchyViewPart) view).setInput(type);
		} catch (PartInitException x) {
			MessageDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaPlugin.getResourceString(ERROR_OPEN_VIEW), x.getMessage());
		}			
	}
	
	public void update() {
		setEnabled(canActionBeAdded());
	}
	
	public boolean canActionBeAdded() {
		ISelection sel= fSelectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			Iterator iter= ((IStructuredSelection)sel).iterator();
			if (iter.hasNext()) {
				Object obj= iter.next();
				if (obj instanceof IType || obj instanceof IClassFile || obj instanceof ICompilationUnit) {
					return !iter.hasNext();
				}
			}
		}
		return false;
	}


	private IType[] getAllTypesFrom(Object element) {
		IType[] result= null;
		try {
			if (element instanceof IClassFile) {
				result= new IType[] { ((IClassFile)element).getType() };
			} else if (element instanceof ICompilationUnit) {
				result= ((ICompilationUnit)element).getAllTypes();
			} else if (element instanceof IType) {
				result= new IType[] { (IType)element };
			}
		} catch (JavaModelException e) {
			// don't show menu on error.
		}
		return result;
	}
}
