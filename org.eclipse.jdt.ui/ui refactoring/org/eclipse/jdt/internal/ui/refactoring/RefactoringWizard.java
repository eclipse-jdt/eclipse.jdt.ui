/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;import org.eclipse.jface.operation.IRunnableContext;import org.eclipse.jface.util.Assert;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.jface.wizard.Wizard;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.ui.actions.WorkspaceModifyOperation;import org.eclipse.jdt.core.refactoring.ChangeAbortException;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.Refactoring;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;import org.eclipse.jdt.internal.ui.refactoring.changes.ChangeExceptionHandler;import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

public class RefactoringWizard extends Wizard {

	private String fPageTitle;
	private Refactoring fRefactoring;
	private IChange fChange;
	private RefactoringStatus fActivationStatus= new RefactoringStatus();
	private RefactoringStatus fStatus;
	
	public RefactoringWizard(String pageTitle) {
		setNeedsProgressMonitor(true);
		fPageTitle= pageTitle;
		setWindowTitle("Refactoring");
	}
	
	/**
	 * Initializes the wizard with the given refactoring.
	 */
	public void init(Refactoring refactoring) {
		fRefactoring= refactoring;
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR);
	}

	//---- Hooks to overide ---------------------------------------------------------------
	
	/**
	 * Some refactorings do activation checking when the wizard is going to be opened. 
	 * They do this since activation checking is expensive and can't be performed on 
	 * opening a corresponding menu. Wizards that need activation checking on opening
	 * should reimplement this method and should return <code>true</code>. This default
	 * implementation returns <code>false</code>.
	 *
	 * @return <code>true<code> if activation checking should be performed on opening;
	 *  otherwise <code>false</code> is returned
	 */
	protected boolean checkActivationOnOpen() {
		return false;
	}
	 
	/**
	 * Hook to add user input pages to the wizard. This default implementation 
	 * adds nothing.
	 */
	protected void addUserInputPages(){
	}
	
	/**
	 * Hook to add the error page to the wizard. This default implementation 
	 * adds an <code>ErrorWizardPage</code> to the wizard.
	 */
	protected void addErrorPage(){
		addPage(new ErrorWizardPage());
	}
	
	/**
	 * Hook to add the page the gives a prefix of the changes to be performed. This default 
	 * implementation  adds a <code>PreviewWizardPage</code> to the wizard.
	 */
	protected void addPreviewPage(){
		addPage(new PreviewWizardPage());
	}
	
	//---- Setter and Getters ------------------------------------------------------------
	
	/**
	 * Returns the refactoring this wizard is using.
	 */	
	public Refactoring getRefactoring(){
		return fRefactoring;
	}

	/**
	 * Sets the change object.
	 */
	public void setChange(IChange change){
		PreviewWizardPage page= (PreviewWizardPage)getPage(PreviewWizardPage.PAGE_NAME);
		if (page != null)
			page.setChange(change);
		fChange= change;
	}

	/**
	 * Returns the current change object.
	 */
	public IChange getChange() {
		return fChange;
	}
	
	/**
	 * Sets the refactoring status.
	 * 
	 * @param status the refactoring status to set.
	 */
	public void setStatus(RefactoringStatus status) {
		ErrorWizardPage page= (ErrorWizardPage)getPage(ErrorWizardPage.PAGE_NAME);
		if (page != null)
			page.setStatus(status);
		fStatus= status;
	}
	
	/**
	 * Returns the current refactoring status.
	 */
	public RefactoringStatus getStatus() {
		return fStatus;
	} 
	
	/**
	 * Sets the refactoring status returned from input checking. Any previously 
	 * computed activation status is merged into the given status before it is set 
	 * to the error page.
	 * 
	 * @param status the input status to set.
	 * @see #checkActivationOnOpen()
	 * @see #getActivationStatus()
	 */
	public void setInputStatus(RefactoringStatus status) {
		RefactoringStatus newStatus= new RefactoringStatus();
		if (fActivationStatus != null)
			newStatus.merge(fActivationStatus);
		newStatus.merge(status);	
		setStatus(newStatus);			
	}
	
	/**
	 * Sets the refactoring status returned from activation checking.
	 * 
	 * @param status the activation status to be set.
	 */
	public void setActivationStatus(RefactoringStatus status) {
		fActivationStatus= status;
		setStatus(status);
	}
		
	/**
	 * Returns the activation status computed during the start up off this
	 * wizard. This methdod returns <code>null</code> if no activation
	 * checking has been performed during startup.
	 * 
	 * @return the activation status computed during startup.
	 * @see #checkActivationOnOpen()
	 */
	public RefactoringStatus getActivationStatus() {
		return fActivationStatus;
	}
	
	/**
	 * Returns the default page title used for this refactoring.
	 */
	public String getPageTitle() {
		return fPageTitle;
	}
	
	/**
	 * Set the default page title used for this refactoring.
	 */
	public void setPageTitle(String title) {
		fPageTitle= title;
		setupPageTitles();
	}
	 
	/**
	 * Computes the wizard page that should follow the user input page. This is
	 * either the error page or the proposed changes page, depending on the
	 * result of the condition checking.
	 * @return the wizard page that should be shown after the last user input
	 *  page.
	 */
	public IWizardPage computeUserInputSuccessorPage() {
		IChange change= createChange(CheckConditionsOperation.INPUT, RefactoringStatus.OK, true);
		RefactoringStatus status= getStatus();
		
		// Set change if we don't have errors.
		if (!status.hasFatalError())
			setChange(change);
		
		if (status.isOK()) {
			return getPage(PreviewWizardPage.PAGE_NAME);
		} else {
			return getPage(ErrorWizardPage.PAGE_NAME);
		}
	} 
	
	/**
	 * Initialize all pages with the managed page title.
	 */
	protected void setupPageTitles() {
		if (fPageTitle == null)
			return;
			
		IWizardPage[] pages= getPages();
		for (int i= 0; i < pages.length; i++) {
			pages[i].setTitle(fPageTitle);
		}
	}

	//---- Change management -------------------------------------------------------------

	/**
	 * Creates a new change object for the refactoring. Method returns <code>
	 * null</code> if the change cannot be created.
	 * 
	 * @param style the conditions to check before creating the change.
	 * @param checkPassedSeverity the severity below which the conditions check
	 *  is treated as 'passed'
	 * @param updateStatus if <code>true</code> the wizard's status is updated
	 *  with the status returned from the <code>CreateChangeOperation</code>.
	 *  if <code>false</code> no status updating is performed.
	 */
	IChange createChange(int style, int checkPassedSeverity, boolean updateStatus){
		CreateChangeOperation op= new CreateChangeOperation(fRefactoring, style);
		op.setCheckPassedSeverity(checkPassedSeverity); 

		Exception exception= null;
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			exception= e;
		} catch (InvocationTargetException e) {
			exception= e;
		}
		
		if (updateStatus) {
			RefactoringStatus status= null;
			if (exception != null) {
				status= new RefactoringStatus();
				status.addFatalError(exception.getMessage() != null ? exception.getMessage(): exception.getClass().getName());
			} else {
				status= op.getStatus();
			}
			setStatus(status, style);
		}
		IChange change= op.getChange();	
		return change;
	}

	public boolean performFinish(PerformChangeOperation op) {
		ChangeContext context= new ChangeContext(new ChangeExceptionHandler());
		try{
			op.setChangeContext(context);
			getContainer().run(false, false, op);	
			JdtHackFinder.fixme("this should be done by someone else");
			if (op.changeExecuted())
				fRefactoring.getUndoManager().addUndo(fRefactoring.getName(), op.getChange().getUndoChange());
		} catch (InvocationTargetException e) {
			Throwable t= e.getTargetException();
			if (t instanceof ChangeAbortException) {
				handleChangeAbortException(context, (ChangeAbortException)t);
				return true;
			} else {
				handleUnexpectedException(e);
			}	
			return false;
		} catch (InterruptedException e) {
			return false;
		} finally {
			context.clearPerformedChanges();
		}
		
		return true;
	}
	
	private void handleChangeAbortException(final ChangeContext context, ChangeAbortException exception) {
		if (!context.getTryToUndo())
			return;
			
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor pm) throws CoreException, InvocationTargetException {
				ChangeContext undoContext= new ChangeContext(new AbortChangeExceptionHandler());
				try {
					IChange[] changes= context.getPerformedChanges();
					pm.beginTask("Undoing changes: ", changes.length);
					IProgressMonitor sub= new NullProgressMonitor();
					for (int i= changes.length - 1; i >= 0; i--) {
						IChange change= changes[i];
						pm.subTask(change.getName());
						change.getUndoChange().perform(undoContext, sub);
						pm.worked(1);
					}
				} catch (ChangeAbortException e) {
					throw new InvocationTargetException(e.getThrowable());
				} finally {
					pm.done();
				} 
			}
		};
		
		try {
			getContainer().run(false, false, op);
		} catch (InvocationTargetException e) {
			handleUnexpectedException(e);
		} catch (InterruptedException e) {
			// not possible. Operation not cancelable.
		}
	}
	
	private void handleUnexpectedException(InvocationTargetException e) {
		ExceptionHandler.handle(e, "Refactoring", "Unexpected exception while performing the refactoring");
	}

	//---- Condition checking ------------------------------------------------------------

	public RefactoringStatus checkInput() {
		return internalCheckCondition(getContainer(), CheckConditionsOperation.INPUT);
	}
	
	/**
	 * Checks the condition for the given style.
	 * @param style the conditions to check.
	 * @return the result of the condition check.
	 * @see CheckPreconditionsOperation
	 */
	protected RefactoringStatus internalCheckCondition(IRunnableContext context, int style) {
		
		CheckConditionsOperation op= new CheckConditionsOperation(fRefactoring, style); 

		Exception exception= null;
		try {
			context.run(true, true, op);
		} catch (InterruptedException e) {
			exception= e;
		} catch (InvocationTargetException e) {
			exception= e;
		}
		RefactoringStatus status= null;
		if (exception != null) {
			JavaPlugin.log(exception);
			status= new RefactoringStatus();
			status.addFatalError("Internal error during precondition checking. See log for detailed error description");
			JavaPlugin.log(exception);
		} else {
			status= op.getStatus();
		}
		setStatus(status, style);
		return status;	
	}
	
	/**
	 * Sets the status according to the given style flag.
	 * 
	 * @param status the refactoring status to set.
	 * @param style a flag indicating if the status is a activation, input checking, or
	 *  precondition checking status.
	 * @see CheckConditionsOperation
	 */
	protected void setStatus(RefactoringStatus status, int style) {
		if ((style & CheckConditionsOperation.PRECONDITIONS) == CheckConditionsOperation.PRECONDITIONS)
			setStatus(status);
		else if ((style & CheckConditionsOperation.ACTIVATION) == CheckConditionsOperation.ACTIVATION)
			setActivationStatus(status);
		else if ((style & CheckConditionsOperation.INPUT) == CheckConditionsOperation.INPUT)
			setInputStatus(status);
	}

	
	//---- Reimplementation of Wizard methods --------------------------------------------

	/* (non-Javadoc)
	 * Method implemented in Wizard.
	 */
	public boolean performFinish() {
		Assert.isNotNull(fRefactoring);
		
		RefactoringWizardPage page= (RefactoringWizardPage)getContainer().getCurrentPage();
		return page.performFinish();
	}
	
	/* (non-Javadoc)
	 * Method implemented in Wizard.
	 */
	public void addPages() {
		if (checkActivationOnOpen()) {
			internalCheckCondition(new BusyIndicatorRunnableContext(), CheckConditionsOperation.ACTIVATION);
		}
		if (fActivationStatus.hasFatalError()) {
			addErrorPage();
			// Set the status since we added the error page
			setStatus(getStatus());	
		} else { 
			addUserInputPages();
			addErrorPage();
			addPreviewPage();	
		}
		setupPageTitles();
	}
	
	/* (non-Javadoc)
	 * Method implemented in Wizard.
	 */
	public void addPage(IWizardPage page) {
		Assert.isTrue(page instanceof RefactoringWizardPage);
		super.addPage(page);
	}
}