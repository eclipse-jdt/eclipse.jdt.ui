package org.eclipse.jdt.internal.ui.search;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

 
/**
 * Abstract class for all search actions which work on java elements.
 */
public abstract class ElementSearchAction extends Action {

	private Class[] fValidTypes;

	public ElementSearchAction(String label, Class[] validTypes) {
		super(label);
		fValidTypes= validTypes;
	}

	public boolean canOperateOn(ISelection sel) {
		boolean result= !sel.isEmpty();
		if (!result || fValidTypes == null)
			return result;

		if (fValidTypes.length == 0)
			return false;

		IJavaElement element= getJavaElement(sel);
		if (element != null) {
			for (int i= 0; i < fValidTypes.length; i++) {
				if (fValidTypes[i].isInstance(element))
					return true;
			}
		}
		return false;
	}

	public void run() {
		IJavaElement element= getJavaElement(getSelection());
		if (element == null) {
			beep();
			return;
		}
		SearchUI.activateSearchResultView();
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		JavaSearchOperation op= null;
		ResourceBundle bundle= JavaPlugin.getResourceBundle();
		try {
			op= makeOperation(element);
		} catch (JavaModelException ex) {
			ExceptionHandler.handle(ex, shell, bundle, "Search.Error.search.");
			return;
		}
		IWorkspaceDescription workspaceDesc= JavaPlugin.getWorkspace().getDescription();
		boolean isAutoBuilding= workspaceDesc.isAutoBuilding();
		if (isAutoBuilding)
			// disable auto-build during search operation
			workspaceDesc.setAutoBuilding(false);
			try {
				JavaPlugin.getWorkspace().setDescription(workspaceDesc);
			}
			catch (CoreException ex) {
				ExceptionHandler.handle(ex, shell, bundle, "Search.Error.setDescription.");
			}				
		try {
			new ProgressMonitorDialog(shell).run(true, true, op);
		} catch (InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, bundle, "Search.Error.search.");
		} catch(InterruptedException e) {
		} finally {
			if (isAutoBuilding)
				// enable auto-building again
				workspaceDesc= JavaPlugin.getWorkspace().getDescription();
				workspaceDesc.setAutoBuilding(true);
				try {
					JavaPlugin.getWorkspace().setDescription(workspaceDesc);
				}
				catch (CoreException ex) {
					ExceptionHandler.handle(ex, shell, bundle, "Search.Error.setDescription.");
				}				
		}
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(), getCollector());
	};

	protected abstract int getLimitTo();

	protected IJavaSearchScope getScope() {
		return SearchEngine.createWorkspaceScope();
	}

	protected JavaSearchResultCollector getCollector() {
		return new JavaSearchResultCollector();
	}

	private IJavaElement getJavaElement(IJavaElement o) {
		return o;
	}

	private IJavaElement getJavaElement(IMarker o) {
		try {
			return JavaCore.create((String) ((IMarker) o).getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID));
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.createJavaElement.");
			return null;
		}
	}

	protected IJavaElement getJavaElement(ISelection selection) {
		if (selection instanceof ITextSelection)
			return getJavaElement((ITextSelection) selection);
		else
			if (selection instanceof IStructuredSelection)
				return getJavaElement((IStructuredSelection) selection);
		return null;
	}

	private IJavaElement getJavaElement(Object o) {
		if (o instanceof IJavaElement)
			return getJavaElement((IJavaElement)o);
		else if (o instanceof IMarker)
			return getJavaElement((IMarker)o);
		else if (o instanceof ISelection)
			return getJavaElement((ISelection)o);
		else if (o instanceof ISearchResultViewEntry)
			return getJavaElement((ISearchResultViewEntry)o);
		return null;
	}

	private IJavaElement getJavaElement(ISearchResultViewEntry entry) {
		if (entry != null)
			return getJavaElement(entry.getSelectedMarker());
		return null;
	}

	private IJavaElement getJavaElement(IStructuredSelection selection) {
		if (selection.size() == 1)
			// Selection only enabled if one element selected.
			return getJavaElement(selection.getFirstElement());
		return null;
	}

	private IJavaElement getJavaElement(ITextSelection selection) {
		IEditorPart editorPart= JavaPlugin.getActivePage().getActiveEditor();

		if (editorPart == null)
			return null;

		ICodeAssist assist= getCodeAssist(editorPart);
		ITextSelection ts= (ITextSelection) selection;
		if (assist != null) {
			IJavaElement[] elements;
			try {
				elements= assist.codeSelect(ts.getOffset(), ts.getLength());
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.createJavaElement.");
				return null;
			}
			if (elements != null && elements.length > 0) {
				if (elements.length == 1 || !shouldUserBePrompted())
					return elements[0];
				else if  (elements.length > 1)
					return chooseFromList(Arrays.asList(elements));
			}
		}
		return null;
	}

	public ISelection getSelection() {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		if (window != null)
			return window.getSelectionService().getSelection();
		else
			return TextSelection.emptySelection();
	}

	protected ICodeAssist getCodeAssist(IEditorPart editorPart) {
		IEditorInput input= editorPart.getEditorInput();
		if (input instanceof ClassFileEditorInput)
			return ((ClassFileEditorInput)input).getClassFile();
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(input);
	}

	private IJavaElement chooseFromList(List openChoices) {
		ILabelProvider labelProvider= new JavaElementLabelProvider(
			  JavaElementLabelProvider.SHOW_DEFAULT 
			| JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(JavaPlugin.getActiveWorkbenchShell(), labelProvider, true, false);
		dialog.setTitle(JavaPlugin.getResourceString("SearchElementSelectionDialog.title"));
		dialog.setMessage(JavaPlugin.getResourceString("SearchElementSelectionDialog.message"));
		if (dialog.open(openChoices) == dialog.OK)
			return (IJavaElement)Arrays.asList(dialog.getResult()).get(0);
		return null;
	}

	/**
	 * Answers if a dialog should prompt the user for a unique Java element
	 */	
	protected boolean shouldUserBePrompted() {
		return true;
	}
	
	private void beep() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell != null && shell.getDisplay() != null)
			shell.getDisplay().beep();
	}	
}
