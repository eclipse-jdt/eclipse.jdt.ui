package org.eclipse.jdt.ui.actions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class PullUpAction extends SelectionDispatchAction{

	private PullUpRefactoring fRefactoring;
	private CompilationUnitEditor fEditor;
	
	public PullUpAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	public PullUpAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("RefactoringGroup.pull_Up_label"));//$NON-NLS-1$
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
	}

	protected void selectionChanged(ITextSelection selection) {
	}
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		startRefactoring();
	}

	protected void run(ITextSelection selection) {
		if (! canRun(selection)){
			String unavailable= RefactoringMessages.getString("PullUpAction.unavailable"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
			fRefactoring= null;
			return;
		}
		startRefactoring();	
	}
		
	private boolean canEnable(IStructuredSelection selection){
		if (selection.isEmpty())
			return false;
		
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			if (! (iter.next() instanceof IMember))
				return false;
		}
		return shouldAcceptElements(selection.toArray());
	}
		
	private boolean canRun(ITextSelection selection){
		IJavaElement[] elements= resolveElements();
		if (elements.length != 1)
			return false;

		return (elements[0] instanceof IMember) && shouldAcceptElements(elements);
	}

	private PullUpRefactoring createNewRefactoringInstance(Object[] obj){
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(obj));
		IMember[] members= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
		return new PullUpRefactoring(members, JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private boolean shouldAcceptElements(Object[] elements) {
		try{
			fRefactoring= createNewRefactoringInstance(elements);
			return fRefactoring.checkPreactivation().isOK();
		} catch (JavaModelException e){
			JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;
		}	
	}
		
	private IJavaElement[] resolveElements() {
		return SelectionConverter.codeResolveHandled(fEditor, getShell(),  RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"));  //$NON-NLS-1$
	}

	private RefactoringWizard createWizard(){
		String title= RefactoringMessages.getString("RefactoringGroup.pull_up"); //$NON-NLS-1$
		//FIX ME: wrong
		String helpId= "HELPID"; //$NON-NLS-1$
		return new PullUpWizard(fRefactoring, title, helpId);
	}
		
	private void startRefactoring() {
		Assert.isNotNull(fRefactoring);
		try{
			Object newElementToProcess= new RefactoringStarter().activate(fRefactoring, createWizard(), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
			if (newElementToProcess == null)
				return;
			IStructuredSelection mockSelection= new StructuredSelection(newElementToProcess);
			selectionChanged(mockSelection);
			if (isEnabled())
				run(mockSelection);
			else
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), ActionMessages.getString("PullUpAction.problem.title"), ActionMessages.getString("PullUpAction.problem.message"));	 //$NON-NLS-1$ //$NON-NLS-2$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}	
}