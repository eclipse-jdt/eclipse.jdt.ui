/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class MoveAction extends ReorgDestinationAction {
	
	private static final int PREVIEW_ID= IDialogConstants.CLIENT_ID + 1;
	private static boolean fShowPreview= false;
	
	public MoveAction(ISelectionProvider viewer) {
		this(viewer, ReorgMessages.getString("moveAction.label")); //$NON-NLS-1$
	}
	
	public MoveAction(ISelectionProvider viewer, String name) {
		super(viewer, name);
		setDescription(ReorgMessages.getString("moveAction.description")); //$NON-NLS-1$
	}

	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	String getActionName() {
		return ReorgMessages.getString("moveAction.name"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#getDestinationDialogMessage
	 */
	String getDestinationDialogMessage() {
		return ReorgMessages.getString("moveAction.destination.label"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#createRefactoring
	 */
	ReorgRefactoring createRefactoring(List elements){
		IDocumentProvider documentProvider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		ITextBufferChangeCreator changeCreator= new DocumentTextBufferChangeCreator(documentProvider);
		return new MoveRefactoring(elements, changeCreator);
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException{
		if (!isOkToMoveReadOnly(refactoring))
			return false;
		return askIfUpdateReferences((MoveRefactoring)refactoring);
	}
	
	private static boolean isOkToMoveReadOnly(ReorgRefactoring refactoring){
		for (Iterator iter= refactoring.getElementsToReorg().iterator(); iter.hasNext(); ){
			Object element= iter.next();
			if (ReorgUtils.shouldConfirmReadOnly(element)) {
				return  MessageDialog.openQuestion(
					JavaPlugin.getActiveWorkbenchShell(),
					ReorgMessages.getString("moveAction.checkMove"),  //$NON-NLS-1$
					ReorgMessages.getString("moveAction.error.readOnly")); //$NON-NLS-1$
			}
		}
		return true;
	}
	
	//returns false iff canceled or error
	private boolean askIfUpdateReferences(MoveRefactoring ref) throws JavaModelException{
		if (! ref.canUpdateReferences()){
			fShowPreview= false;
			return true;
		}	
		switch (askIfUpdateReferences()){
			case IDialogConstants.CANCEL_ID:
				fShowPreview= false;
				return false;
			case IDialogConstants.NO_ID:
				ref.setUpdateReferences(false);
				fShowPreview= false;
				return true;	
			case IDialogConstants.YES_ID:
				ref.setUpdateReferences(true);
				fShowPreview= false;
				return true;
			case PREVIEW_ID:		 
				ref.setUpdateReferences(true);
				fShowPreview= true;
				return true;
			default: 
				Assert.isTrue(false); //not expected to get here
				return false;
		}
	}
	
	private static int askIfUpdateReferences(){
		Shell shell= JavaPlugin.getActiveWorkbenchShell().getShell();
		String title= "Move";
		String preview= "Pre&view";
		String question= "Do you want to update references to the moved elements? Press '" + preview + "' to see the preview of the reference updates.";
		
		String[] labels= new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
															 preview,  IDialogConstants.CANCEL_LABEL };
		final MessageDialog dialog = new MessageDialog(shell,	title, null, question, MessageDialog.QUESTION,	labels, 2); //preview is default
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		int result = dialog.getReturnCode();
		if (result == 0)
			return IDialogConstants.YES_ID;
		if (result == 1)
			return IDialogConstants.NO_ID;
		if (result == 2)
			return PREVIEW_ID;
		return IDialogConstants.CANCEL_ID;
	}
	
	/* non java-doc
	 * @see ReorgDestinationAction#doReorg(ReorgRefactoring) 
	 */
	void doReorg(ReorgRefactoring refactoring) throws JavaModelException{
		if (!fShowPreview){
			super.doReorg(refactoring);
			return;
		}	
		RefactoringWizard wizard= new MoveWizard();	
		wizard.init(refactoring);
		new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard).open();	
	}
	
	//--- static inner classes
		
	private static class MoveWizard extends RefactoringWizard{
		MoveWizard(){
			//XX incorrect help
			super("Move", IJavaHelpContextIds.MOVE_CU_ERROR_WIZARD_PAGE); 
		}
		
		/* (non-Javadoc)
		 * Method overridden from RefactoringWizard
		 */
		public void addPages() {
			addPreviewPage();	
			setupPageTitles();
		}
		
		public void createPageControls(Composite pageContainer) {
			super.createPageControls(pageContainer);
			
			//XX nasty to have to do it here
			setChange();
		}
		
		private void setChange(){
			IChange change= null;
			try{
				change= computeChange(getRefactoring());
			} catch (CoreException e){
				JavaPlugin.log(e.getStatus());
				return;
			}	
			if (change == null)
				return;
			setChange(change);
		}
		
		private IChange computeChange(Refactoring ref) throws CoreException{
			CreateChangeOperation op= new CreateChangeOperation(ref, CreateChangeOperation.CHECK_NONE);
			try{
				new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(true, true, op);
			} catch (InvocationTargetException e) {
				Throwable t= e.getTargetException();
				if (t instanceof CoreException)
					throw (CoreException)t;
				if (t instanceof Error)
					throw (Error)t;
				if (t instanceof RuntimeException)
					throw (RuntimeException)t;
				Assert.isTrue(false);//should never get here
				return null;
			} catch (InterruptedException e) {
				//fall thru
			}
			return op.getChange();
		}		
	}
}
