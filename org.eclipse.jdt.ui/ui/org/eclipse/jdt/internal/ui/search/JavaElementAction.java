/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;
import java.util.Arrays;import java.util.List;import org.eclipse.core.resources.IMarker;import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.jface.action.Action;import org.eclipse.jface.text.ITextSelection;import org.eclipse.jface.text.TextSelection;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
/**
 * Abstract class for actions that run on IJavaElement.
 */
public abstract class JavaElementAction extends Action {
	public static final String PREFIX= "ShowTypeHierarchyAction.";
	
	// A dummy which can't be selected in the UI
	private static final IJavaElement RETURN_WITHOUT_BEEP= JavaCore.create(JavaPlugin.getDefault().getWorkspace().getRoot());
	
	private Class[] fValidTypes;

	public JavaElementAction(String label, Class[] validTypes) {
		super(label);
		fValidTypes= validTypes;
	}
	public boolean canOperateOn(ISelection sel) {
		boolean result= !sel.isEmpty();
		if (!result || fValidTypes == null)
			return result;

		if (fValidTypes.length == 0)
			return false;

		IJavaElement element= getJavaElement(sel, true);
		if (element != null) {
			for (int i= 0; i < fValidTypes.length; i++) {
				if (fValidTypes[i].isInstance(element))
					return true;
			}
		}
		return false;
	}
	public void run() {
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
				return findType((IClassFile)o, silent);			
		}
		return o;
	}
	private IJavaElement getJavaElement(IMarker o, boolean silent) {
		try {
			return getJavaElement(JavaCore.create((String) ((IMarker) o).getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID)), silent);
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "Search.Error.createJavaElement.");
			return null;
		}
	}
	protected IJavaElement getJavaElement(ISelection selection, boolean silent) {
		if (selection instanceof ITextSelection)
			return getJavaElement((ITextSelection) selection, silent);
		else
			if (selection instanceof IStructuredSelection)
				return getJavaElement((IStructuredSelection) selection, silent);
		return null;
	}
	private IJavaElement getJavaElement(Object o, boolean silent) {
		if (o instanceof IJavaElement)
			return getJavaElement((IJavaElement)o, silent);
		else if (o instanceof IMarker)
			return getJavaElement((IMarker)o, silent);
		else if (o instanceof ISelection)
			return getJavaElement((ISelection)o, silent);
		else if (o instanceof ISearchResultViewEntry)
			return getJavaElement((ISearchResultViewEntry)o, silent);
		return null;
	}

	private IJavaElement getJavaElement(ISearchResultViewEntry entry, boolean silent) {
		if (entry != null)
			return getJavaElement(entry.getSelectedMarker(), silent);
		return null;
	}

	private IJavaElement getJavaElement(IStructuredSelection selection, boolean silent) {
		if (selection.size() == 1)
			// Selection only enabled if one element selected.
			return getJavaElement(selection.getFirstElement(), silent);
		return null;
	}
	private IJavaElement getJavaElement(ITextSelection selection, boolean silent) {
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
	
	protected void beep() {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell != null && shell.getDisplay() != null)
			shell.getDisplay().beep();
	}	
	protected IJavaElement findType(ICompilationUnit cu, boolean silent) {
		if (silent) {
			String mainTypeName= cu.getElementName().substring(0, cu.getElementName().length() - 5);
			IType mainType= cu.getType(mainTypeName);
			mainTypeName= JavaModelUtility.getTypeQualifiedName((IType)mainType);
			try {					
				mainType= JavaModelUtility.findTypeInCompilationUnit(cu, mainTypeName);
				if (mainType == null) {
					// fetch type which is declared first in the file
					IType[] types= cu.getTypes();
					if (types.length > 0)
						mainType= types[0];
					else
						return null;
				}
			} catch (JavaModelException ex) {
				// silent mode
				ExceptionHandler.log(ex, JavaPlugin.getResourceBundle(), "OpenTypeAction.error.open.");
				return RETURN_WITHOUT_BEEP;
			}
			return mainType;
		} 
		else {
			IType[] types= null;
			try {
				types= cu.getAllTypes();
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, JavaPlugin.getResourceBundle(), "OpenTypeAction.error.open.");
				return RETURN_WITHOUT_BEEP;
			}
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
			else
				return RETURN_WITHOUT_BEEP;
		}
		return null;
	}
	protected IJavaElement findType(IClassFile cf, boolean silent) {
		IType mainType;
		try {					
			mainType= cf.getType();
		} catch (JavaModelException ex) {
			return null;
		}
		return mainType;
	}
}
