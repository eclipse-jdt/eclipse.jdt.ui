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
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Action to pull up method and fields into a superclass.
 * <p>
 * Action is applicable to selections containing elements of
 * type <code>IField</code> and <code>IMethod</code>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class PullUpAction extends SelectionDispatchAction{

	private PullUpRefactoring fRefactoring;
	private CompilationUnitEditor fEditor;
	
	/**
	 * Creates a new <code>PullUpAction</code>. The action requires that the selection 
	 * provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public PullUpAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("RefactoringGroup.pull_Up_label"));//$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.PULL_UP_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public PullUpAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(SelectionConverter.canOperateOn(fEditor));
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canEnable(selection));
		if (! isEnabled())
			fRefactoring= null;
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(ITextSelection)
	 */
	protected void selectionChanged(ITextSelection selection) {
	}
	
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		if (fRefactoring == null)
			selectionChanged(selection);
		if (isEnabled())
			startRefactoring();
		fRefactoring= null;	
		selectionChanged(selection);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(ITextSelection)
	 */
	protected void run(ITextSelection selection) {
		if (!ActionUtil.isProcessable(getShell(), fEditor))
			return;
		if (canRun(selection)){
			startRefactoring();	
		} else {
			String unavailable= RefactoringMessages.getString("PullUpAction.unavailable"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.unavailable"), unavailable); //$NON-NLS-1$
		}
		fRefactoring= null;
		selectionChanged(selection);
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
		try {
			IJavaElement element= SelectionConverter.getElementAtOffset(fEditor);
			if (element == null)
				return false;
			return (element instanceof IMember) && shouldAcceptElements(new IJavaElement[]{element});
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;
		}
	}

	private PullUpRefactoring createNewRefactoringInstance(Object[] obj){
		return new PullUpRefactoring(convertToMemberArray(obj), JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private IMember[] convertToMemberArray(Object[] obj) {
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList(obj));
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}

	private boolean shouldAcceptElements(Object[] elements) {
		try{
			fRefactoring= createNewRefactoringInstance(elements);
			return fRefactoring.checkPreactivation().isOK();
		} catch (JavaModelException e){
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.filterNotPresentException(e))
				JavaPlugin.log(e); //this happen on selection changes in viewers - do not show ui if fails, just log
			return false;
		}	
	}
		
	private RefactoringWizard createWizard(){
		String title= RefactoringMessages.getString("RefactoringGroup.pull_up"); //$NON-NLS-1$
		String helpId= IJavaHelpContextIds.PULL_UP_ERROR_WIZARD_PAGE;
		return new PullUpWizard(fRefactoring, title, helpId);
	}
		
	private void startRefactoring() {
		Assert.isNotNull(fRefactoring);
		// Work around for http://dev.eclipse.org/bugs/show_bug.cgi?id=19104
		if (!ActionUtil.isProcessable(getShell(), fRefactoring.getDeclaringType()))
			return;
		
		try{
			Object newElementToProcess= new RefactoringStarter().activate(fRefactoring, createWizard(), getShell(), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
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