/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

/**
 * Shows the type hierarchy on a single selected element of type IType
 * IClassFile or ICompilationUnit
 */
public class ShowTypeHierarchyAction extends Action implements IUpdate {
	
	private ISelectionProvider fSelectionProvider;
			
	public ShowTypeHierarchyAction(ISelectionProvider selProvider) {
		super(JavaUIMessages.getString("ShowTypeHierarchyAction.label")); //$NON-NLS-1$
		setDescription(JavaUIMessages.getString("ShowTypeHierarchyAction.description")); //$NON-NLS-1$
		setToolTipText(JavaUIMessages.getString("ShowTypeHierarchyAction.tooltip")); //$NON-NLS-1$
		fSelectionProvider= selProvider;
		
		WorkbenchHelp.setHelp(this,	new Object[] { IJavaHelpContextIds.SHOW_IN_HIERARCHYVIEW_ACTION });	
	}

	/**
	 * Perform the action
	 */
	public void run() {
		IType[] types= getSelectedTypes();
		if (types == null) {
			return;
		}
		IType type= determineType(types);
		if (type != null) {
			showType(type);
		}
	}

	private IType determineType(IType[] types) {
		if (types.length == 1)
			return types[0];

		Shell shell= JavaPlugin.getActiveWorkbenchShell();			
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), true, false);
		dialog.setMessage(JavaUIMessages.getString("ShowTypeHierarchyAction.selectiondialog.message")); //$NON-NLS-1$
		dialog.setTitle(JavaUIMessages.getString("ShowTypeHierarchyAction.selectiondialog.title")); //$NON-NLS-1$
		if (dialog.open(types, null) == dialog.OK) {
			Object[] elements= dialog.getResult();
			if (elements != null && elements.length == 1) {
				return ((IType)elements[0]);
			}
		}
		return null;
	}

	private void showType(IType type) {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null) {
			try {
				IViewPart view= page.showView(JavaUI.ID_TYPE_HIERARCHY);
				if (view instanceof TypeHierarchyViewPart) {
					((TypeHierarchyViewPart) view).setInput(type);
				} else {
					JavaPlugin.logErrorMessage("ShowTypeHierarchyAction.showType: Not a TypeHierarchyViewPart");
				}
			} catch (PartInitException e) {
				JavaPlugin.log(e.getStatus());
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), JavaUIMessages.getString("ShowTypeHierarchyAction.error.title"), "", e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
			}
		} else {
			JavaPlugin.logErrorMessage("ShowTypeHierarchyAction.showType: Active page is null");
		}	
	}
	
	/*
	 * @see IUpdate#update
	 */
	public void update() {
		setEnabled(canActionBeAdded());
	}
	
	public boolean canActionBeAdded() {
		return getSelectedTypes() != null;
	}


	private IType[] getSelectedTypes() {
		Object element= SelectionUtil.getSingleElement(fSelectionProvider.getSelection());
		try {
			if (element == null) {
				return null;
			} else if (element instanceof IClassFile) {
				return new IType[] { ((IClassFile)element).getType() };
			} else if (element instanceof ICompilationUnit) {
				return ((ICompilationUnit)element).getAllTypes();
			} else if (element instanceof IType) {
				return new IType[] { (IType)element };
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e.getStatus());
		}
		return null;
	}
}
