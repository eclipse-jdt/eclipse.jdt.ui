package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSupportFactory;
import org.eclipse.jdt.internal.ui.reorg.IRefactoringRenameSupport;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RenameJavaElementAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	public RenameJavaElementAction(IWorkbenchSite site) {
		super(site);
	}
	
	public RenameJavaElementAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}
	
	public void run(IStructuredSelection selection) {
		if (! canOperateOn(selection))
			return;
		run(selection.getFirstElement());	
	}

	private void run(Object element){
		IRefactoringRenameSupport refactoringSupport= RefactoringSupportFactory.createRenameSupport(element);
		if (! canRename(refactoringSupport, element))
			return;
		try{
			refactoringSupport.rename(element);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameJavaElementAction.name"), RefactoringMessages.getString("RenameJavaElementAction.exception"));  //$NON-NLS-1$ //$NON-NLS-2$
		}	
	}
	
	public void run(ITextSelection selection) {
		if (! canRun(selection)){
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("RenameJavaElementAction.name"), RefactoringMessages.getString("RenameJavaElementAction.not_available"));  //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		run((IJavaElement)resolveElements()[0]);
	}

	private IJavaElement[] resolveElements() {
		return SelectionConverter.codeResolveHandled(fEditor, getShell(), RefactoringMessages.getString("RenameJavaElementAction.name")); //$NON-NLS-1$
	}
	
	public final boolean canRun(ITextSelection selection){
		IJavaElement[] elements= resolveElements();
		return elements.length == 1 && canRename(elements[0]);
	}
			
	protected void selectionChanged(IStructuredSelection selection) {
		if (! canOperateOn(selection))
			setEnabled(false);
		else	
			setEnabled(canRename(selection.getFirstElement()));	
	}

	private static boolean canOperateOn(IStructuredSelection selection) {
		return (selection.size() == 1);
	}
	
	private static boolean canRename(Object element){
		return canRename(RefactoringSupportFactory.createRenameSupport(element), element);	
	}

	private static boolean canRename(IRefactoringRenameSupport refactoringSupport, Object element){
		if (refactoringSupport == null)
			return false;
		try{	
			return refactoringSupport.canRename(element);
		} catch (JavaModelException e){
			JavaPlugin.log(e);
			return false;
		}	
	}
	
	protected void selectionChanged(ITextSelection selection) {
	}	
}