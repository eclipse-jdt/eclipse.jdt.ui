/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.jface.viewers.ILabelProvider;import org.eclipse.jface.viewers.IStructuredContentProvider;import org.eclipse.jface.viewers.LabelProvider;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.wizard.IWizardPage;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatusEntry;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Presents the list of failed preconditions to the user
 */
public class ErrorWizardPage extends RefactoringWizardPage {
		
	private RefactoringStatus fStatus;
	
	private TableViewer fTableViewer;
	private static IStructuredContentProvider fgContentProvider;
	
	private String fHelpContextID;

	public static final String PAGE_NAME= "ErrorPage"; //$NON-NLS-1$
	
	public ErrorWizardPage(String helpContextId){
		super(PAGE_NAME);
		fHelpContextID= helpContextId;
	}
	
	/**
	 * Sets the page's refactoring status to the given value.
	 * @param status the refactoring status.
	 */
	public void setStatus(RefactoringStatus status){
		fStatus= status;
		if (fStatus != null) {
			setPageComplete(isRefactoringPossible());
			int severity= fStatus.getSeverity();
			if (severity >= RefactoringStatus.FATAL) {
				setDescription(RefactoringMessages.getString("ErrorWizardPage.cannot_proceed")); //$NON-NLS-1$
			} else if (severity >= RefactoringStatus.INFO) {
				setDescription(RefactoringMessages.getString("ErrorWizardPage.confirm")); //$NON-NLS-1$
			} else {
				setDescription(""); //$NON-NLS-1$
			}
		} else {
			setPageComplete(true);
			setDescription(""); //$NON-NLS-1$
		}	
	}
	
	/* (non-JavaDoc)
	 * Method defined in RefactoringWizardPage
	 */
	protected boolean performFinish() {
		RefactoringWizard wizard= getRefactoringWizard();
		IChange change= wizard.getChange();
		PerformChangeOperation op= null;
		if (change != null) {
			op= new PerformChangeOperation(change);
		} else {
			CreateChangeOperation ccop= new CreateChangeOperation(getRefactoring(), CreateChangeOperation.CHECK_NONE);
			ccop.setCheckPassedSeverity(RefactoringStatus.ERROR);
			
			op= new PerformChangeOperation(ccop);
			op.setCheckPassedSeverity(RefactoringStatus.ERROR);
		}
		return wizard.performFinish(op);
	} 
	
	protected boolean isRefactoringPossible() {
		return fStatus.getSeverity() < RefactoringStatus.FATAL;
	}
	 
	//---- UI creation ----------------------------------------------------------------------
	
	private TableViewer getTableViewer(Composite parent){
		if (fTableViewer == null){
			Table table= new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL);
			fTableViewer= new TableViewer(table);
			fTableViewer.setLabelProvider(createLabelProvider());
			fTableViewer.setContentProvider(createContentProvider());
		}
		return fTableViewer;
	}
		
	private ILabelProvider createLabelProvider(){
		return new LabelProvider(){
			public String getText(Object element){
				return ((RefactoringStatusEntry)element).getMessage();
			}
			public Image getImage(Object element){
				RefactoringStatusEntry entry= (RefactoringStatusEntry)element;
				if (entry.isFatalError())
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_FATAL);
				else if (entry.isError())
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_ERROR);
				else if (entry.isWarning())	
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_WARNING);
				else 
					return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_INFO);
			}
		};
	}
	
	private IStructuredContentProvider createContentProvider() {
		if (fgContentProvider == null) {
			fgContentProvider= new IStructuredContentProvider() {
				// ------- ITableContentProvider Interface ------------

				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}

				public boolean isDeleted(Object element) {
					return false;
				}

				public void dispose() {
				}

				public Object[] getElements(Object obj) {
					return ((RefactoringStatus)obj).getEntries().toArray();
				}
			};
		}
		return fgContentProvider;
	}

	//---- Reimplementation of WizardPage methods ------------------------------------------

	/* (non-Javadoc)
	 * Method declared in IWizardPage.
	 */
	public void createControl(Composite parent) {
		// Additional composite is needed to limit the size of the table to
		// 60 characters.
		Composite content= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1; layout.marginWidth= 0; layout.marginHeight= 0;
		content.setLayout(layout);
		
		fTableViewer= getTableViewer(content);

		Table tableControl= fTableViewer.getTable();
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(60);
		tableControl.setLayoutData(gd);
		// Add a column so that we can pack it in setVisible.
		TableColumn tc= new TableColumn(tableControl, SWT.NONE);
		tc.setResizable(false);
		
		setControl(content);
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, fHelpContextID));			
	}
	
	/* (non-Javadoc)
	 * Method declared on IDialog.
	 */
	public void setVisible(boolean visible) {
		if (visible && fTableViewer.getInput() != fStatus) {
			fTableViewer.setInput(fStatus);
			fTableViewer.getTable().getColumn(0).pack();
		}
		super.setVisible(visible);
	}
	
	/* (non-Javadoc)
	 * Method declared in IWizardPage.
	 */
	public boolean canFlipToNextPage() {
		// We have to call super.getNextPage since computing the next
		// page is expensive. So we avoid it as long as possible.
		return fStatus != null && isRefactoringPossible() &&
			   isPageComplete() && super.getNextPage() != null;
	}
	
	/* (non-Javadoc)
	 * Method declared in IWizardPage.
	 */
	public IWizardPage getNextPage() {
		RefactoringWizard wizard= getRefactoringWizard();
		IChange change= wizard.getChange();
		if (change == null) {
			change= wizard.createChange(CreateChangeOperation.CHECK_NONE, RefactoringStatus.ERROR, false);
			wizard.setChange(change);
		}
		if (change == null)
			return this;
			
		return super.getNextPage();
	}	
}
