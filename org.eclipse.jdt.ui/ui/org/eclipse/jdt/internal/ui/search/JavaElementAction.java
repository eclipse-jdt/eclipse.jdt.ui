/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;
import org.eclipse.core.resources.IMarker;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
/**
 * Abstract class for actions that run on IJavaElement.
 */
public abstract class JavaElementAction extends Action {
	
	// A dummy which can't be selected in the UI
	protected static final IJavaElement RETURN_WITHOUT_BEEP= JavaCore.create(JavaPlugin.getDefault().getWorkspace().getRoot());
	
	private Class[] fValidTypes;

	public JavaElementAction(String label, Class[] validTypes) {
		super(label);
		fValidTypes= validTypes;
	}
	public boolean canOperateOn(IStructuredSelection sel) {
		boolean hasSelection= !sel.isEmpty();
		if (!hasSelection || fValidTypes == null)
			return hasSelection;

		if (fValidTypes.length == 0)
			return false;

		return canOperateOn(getJavaElement(sel, true));
	}
		
	protected boolean canOperateOn(IJavaElement element) {
		if (element != null) {
			for (int i= 0; i < fValidTypes.length; i++) {
				if (fValidTypes[i].isInstance(element))
					return true;
			}
		}
		return false;
	}
	public void run() {
		if (!canOperateOn(getSelection())) {
			beep();
			return;
		}
		
		IJavaElement element= getJavaElement(getSelection(), false);
		if (element == null) {
			beep();
			return;
		} 
		else if (element == RETURN_WITHOUT_BEEP)
			return;
		
		run(element);
	}
	protected abstract void run(IJavaElement element);

	private IJavaElement getJavaElement(IJavaElement o, boolean silent) {
		if (o == null)
			return null;
		switch (o.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				return findType((ICompilationUnit)o, silent);
			case IJavaElement.CLASS_FILE:
				return findType((IClassFile)o);			
		}
		return o;
	}
	private IJavaElement getJavaElement(IMarker marker, boolean silent) {
		return getJavaElement(SearchUtil.getJavaElement(marker), silent);
	}
	private IJavaElement getJavaElement(Object o, boolean silent) {
		if (o instanceof IJavaElement)
			return getJavaElement((IJavaElement)o, silent);
		else if (o instanceof IMarker)
			return getJavaElement((IMarker)o, silent);
		else if (o instanceof ISelection)
			return getJavaElement((IStructuredSelection)o, silent);
		else if (o instanceof ISearchResultViewEntry)
			return getJavaElement((ISearchResultViewEntry)o, silent);
		return null;
	}

	private IJavaElement getJavaElement(ISearchResultViewEntry entry, boolean silent) {
		if (entry != null)
			return getJavaElement(entry.getSelectedMarker(), silent);
		return null;
	}

	protected IJavaElement getJavaElement(IStructuredSelection selection, boolean silent) {
		if (selection.size() == 1)
			// Selection only enabled if one element selected.
			return getJavaElement(selection.getFirstElement(), silent);
		return null;
	}
	public IStructuredSelection getSelection() {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null) {
			IWorkbenchPart part= JavaPlugin.getActivePage().getActivePart();
			if (part != null) {
				try {
					return SelectionConverter.getStructuredSelection(part);
				} catch (JavaModelException ex) {
					return StructuredSelection.EMPTY;
				}
			}
		}
		return StructuredSelection.EMPTY;
	}
	private IJavaElement chooseFromList(IJavaElement[] openChoices) {
		int flags= JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_QUALIFIED;
		ILabelProvider labelProvider= new JavaElementLabelProvider(flags);

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(JavaPlugin.getActiveWorkbenchShell(), labelProvider);
		dialog.setTitle(SearchMessages.getString("SearchElementSelectionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(SearchMessages.getString("SearchElementSelectionDialog.message")); //$NON-NLS-1$
		dialog.setElements(openChoices);
		if (dialog.open() == dialog.OK)
			return (IJavaElement)dialog.getFirstResult();
		return null;
	}
	/**
	 * Answers if a dialog should prompt the user for a unique Java element
	 */	
	protected boolean shouldUserBePrompted() {
		return true;
	}
	
	protected void beep() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell != null && shell.getDisplay() != null)
			shell.getDisplay().beep();
	}	
	protected IJavaElement findType(ICompilationUnit cu, boolean silent) {
		IType[] types= null;
		try {					
			types= cu.getAllTypes();
		} catch (JavaModelException ex) {
			// silent mode
			ExceptionHandler.log(ex, SearchMessages.getString("JavaElementAction.error.open.message")); //$NON-NLS-1$
			if (silent)
				return RETURN_WITHOUT_BEEP;
			else
				return null;
		}
		if (types.length == 1 || (silent && types.length > 0))
			return types[0];
		if (silent)
			return RETURN_WITHOUT_BEEP;
		if (types.length == 0)
			return null;
		String title= SearchMessages.getString("JavaElementAction.typeSelectionDialog.title"); //$NON-NLS-1$
		String message = SearchMessages.getString("JavaElementAction.typeSelectionDialog.message"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		int flags= (JavaElementLabelProvider.SHOW_DEFAULT);						

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(parent, new JavaElementLabelProvider(flags));
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setElements(types);
		
		if (dialog.open() == dialog.OK)
			return (IType)dialog.getFirstResult();
		else
			return RETURN_WITHOUT_BEEP;
	}

	protected IType findType(IClassFile cf) {
		IType mainType;
		try {					
			mainType= cf.getType();
		} catch (JavaModelException ex) {
			ExceptionHandler.log(ex, SearchMessages.getString("JavaElementAction.error.open.message")); //$NON-NLS-1$
			return null;
		}
		return mainType;
	}
}
