package org.eclipse.jdt.ui.actions;

import java.util.Iterator;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;


import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class OpenRefactoringWizardAction extends SelectionDispatchAction {

	private Class fActivationType;
	private Refactoring fRefactoring;
	private CompilationUnitEditor fEditor;
	private boolean fCanActivateOnMultiSelection;
	private String fUnavailableMessage;
	
	protected OpenRefactoringWizardAction(String label, String unavailableString, IWorkbenchSite site, Class activatedOnType, boolean canActivateOnMultiSelection) {
		super(site);
		Assert.isNotNull(unavailableString);
		Assert.isNotNull(activatedOnType);
		setText(label);
		fActivationType= activatedOnType;
		fCanActivateOnMultiSelection= canActivateOnMultiSelection;
		fUnavailableMessage= unavailableString;
	}
	
	protected OpenRefactoringWizardAction(String label, String unavailableString, CompilationUnitEditor editor, Class activatedOnType, boolean canActivateOnMultiSelection) {
		this(label, unavailableString, editor.getEditorSite(), activatedOnType, canActivateOnMultiSelection);
		fEditor= editor;
	}
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
	}

	protected void selectionChanged(ITextSelection selection) {
		//resolving is too expensive to do on selection changes in the editor - just enable here, we'll check it later
		setEnabled(fEditor != null);
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		startRefactoring();
	}

	protected void run(ITextSelection selection) {
		if (! canRun(selection)){
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), fUnavailableMessage); //$NON-NLS-1$
			fRefactoring= null;
			return;
		}
		startRefactoring();	
	}
		
	/**
	 * Creates a new instance of <code>Refactoring</code>.
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 * However, if <code>fCanOperateOnMultiSelection</code> is set to <code>true</code>, 
	 * then obj is <code>Object[]</code> and contains elements of the accepted type (passed in the constructor) 
	 */
	protected abstract Refactoring createNewRefactoringInstance(Object obj) throws JavaModelException;	
	
	protected abstract RefactoringWizard createWizard(Refactoring refactoring);	
	
	protected abstract boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException;

	private boolean canEnable(IStructuredSelection selection){
		if (selection.isEmpty())
			return false;
		
		if (selection.size() > 1 && ! fCanActivateOnMultiSelection)	
			return false;
		
		if (selection.size() == 1 && ! fCanActivateOnMultiSelection)
			return fActivationType.isInstance(selection.getFirstElement()) && shouldAcceptElement(selection.getFirstElement());
				
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			if (!fActivationType.isInstance(iter.next()))
				return false;
		}
		return shouldAcceptElement(selection.toArray());
	}
		
	private boolean canRun(ITextSelection selection){
		IJavaElement[] elements= resolveElements();
		if (elements.length != 1)
			return false;

		if (fCanActivateOnMultiSelection)
			return fActivationType.isInstance(elements[0]) && shouldAcceptElement(elements);	//XXX not pretty
		else	
			return fActivationType.isInstance(elements[0]) && shouldAcceptElement(elements[0]);
	}

	/**
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 * However, if <code>canOperateOnMultiSelection</code> resturns <code>true</code>, 
	 * then obj is <code>Object[]</code> and contains elements of the accepted type (passed in the constructor) 
	 * @see OpenWizardAction#shouldAcceptElement
	 */
	private final boolean shouldAcceptElement(Object obj) {
		try{
			fRefactoring= createNewRefactoringInstance(obj);
			return canActivateRefactoring(fRefactoring);
		} catch (JavaModelException e){
			JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;
		}	
	}
		
	private IJavaElement[] resolveElements() {
		return SelectionConverter.codeResolveHandled(fEditor, getShell(),  RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"));  //$NON-NLS-1$
	}
	
	private void startRefactoring() {
		Assert.isNotNull(fRefactoring);
		try{
			new RefactoringStarter().activate(fRefactoring, createWizard(fRefactoring), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
