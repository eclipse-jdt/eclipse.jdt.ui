/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatusEntry;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Presents the list of failed preconditions to the user
 */
public class ErrorWizardPage extends RefactoringWizardPage {
		
	private RefactoringStatus fStatus;
	
	private TableViewer fTableViewer;
	private static IStructuredContentProvider fgContentProvider;

	public static final String PAGE_NAME= "ErrorPage";
	
	public ErrorWizardPage(){
		super(PAGE_NAME);
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
				setDescription(RefactoringResources.getResourceString(getName() + ".notPossible"));
			} else if (severity >= RefactoringStatus.INFO) {
				setDescription(RefactoringResources.getResourceString(getName() + ".confirm"));
			} else {
				setDescription("");
			}
		} else {
			setPageComplete(true);
			setDescription("");
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
		fTableViewer= getTableViewer(parent);

		Table tableControl= (Table)fTableViewer.getControl();

		// Add a table column.
		TableLayout tableLayout= new TableLayout();
		tableLayout.addColumnData(new ColumnWeightData(100));
		TableColumn tc= new TableColumn(tableControl, SWT.NONE);
		tc.setResizable(false);
		tableControl.setLayout(tableLayout);		
		
		setControl(tableControl);
	}
	
	/* (non-Javadoc)
	 * Method declared on IDialog.
	 */
	public void setVisible(boolean visible) {
		if (visible && fTableViewer.getInput() != fStatus)
			fTableViewer.setInput(fStatus);
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
