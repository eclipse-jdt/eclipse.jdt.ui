/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatusEntry;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.help.DialogPageContextComputer;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Presents the list of failed preconditions to the user
 */
public class ErrorWizardPage extends RefactoringWizardPage {
		
	private RefactoringStatus fStatus;
	private TableViewer fTableViewer;
	private Label fContextLabel;
	private SourceViewer fSourceViewer;
	private final String fHelpContextID;
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
			fTableViewer= new TableViewer(new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL));
			fTableViewer.setLabelProvider(new RefactoringStatusEntryLabelProvider());
			fTableViewer.setContentProvider(new RefactoringStatusContentProvider());
			fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					ErrorWizardPage.this.selectionChanged(event);
				}
			});	
		}
		return fTableViewer;
	}
	
	private  void createTableViewer(Composite content) {
		fTableViewer= getTableViewer(content);
		
		Table tableControl= fTableViewer.getTable();
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(60);
		tableControl.setLayoutData(gd);
		// Add a column so that we can pack it in setVisible.
		TableColumn tc= new TableColumn(tableControl, SWT.NONE);
		tc.setResizable(false);
	}
	
	private void createSourceViewer(Composite parent){
		Composite c= new Composite(parent, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		c.setLayout(new GridLayout());
		
		fContextLabel= new Label(c, SWT.NONE);
		setLabelText(null);
		fContextLabel.setLayoutData(new GridData());
		
		// source viewer
		int styles= SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION;
		fSourceViewer= new SourceViewer(c, null, styles);
		fSourceViewer.configure(new JavaSourceViewerConfiguration(getJavaTextTools(), null));
		
		showInSourceViewer(getFirstEntry());
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));			
	}

	private void setSourceViewerContents(String contents) {
		if (fSourceViewer.getDocument() != null){
			IDocument document= fSourceViewer.getDocument();
			document.getDocumentPartitioner().disconnect();
			document.setDocumentPartitioner(null);
		}
		
		IDocument document= new Document(contents);
		
		IDocumentPartitioner partitioner= getJavaTextTools().createDocumentPartitioner();
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		
		fSourceViewer.setDocument(document);
	}

	private static JavaTextTools getJavaTextTools() {
		return JavaPlugin.getDefault().getJavaTextTools();	
	}
	
	private void selectionChanged(SelectionChangedEvent event) {
		ISelection s= event.getSelection();
		if (!(s instanceof IStructuredSelection))
			return;
		Object first= ((IStructuredSelection) s).getFirstElement();
		if (! (first instanceof RefactoringStatusEntry))
			return;
			
		showInSourceViewer((RefactoringStatusEntry)first);
	}

	private void showInSourceViewer(RefactoringStatusEntry selected) {
		if (selected == null || selected.getCorrespondingResource() == null){
			setSourceViewerContents(null);
			return;
		}	
		
		String newSourceViewerContents= null;
		IJavaElement element= JavaCore.create(selected.getCorrespondingResource());
		if (element == null || !(element instanceof ISourceReference)){
			setSourceViewerContents(null);
			return;
		}
		String content;
		try{
			content= ((ISourceReference)element).getSource();
		} catch (JavaModelException e){
			content= null;
		}
		setSourceViewerContents(content);
		if (selected.getSourceRange() == null)
			return;
		fSourceViewer.setSelectedRange(selected.getSourceRange().getOffset(), selected.getSourceRange().getLength());
		fSourceViewer.revealRange(selected.getSourceRange().getOffset(), selected.getSourceRange().getLength());
		setLabelText(selected);
	}
	
	private void setLabelText(RefactoringStatusEntry entry){
		if (entry == null)
			fContextLabel.setText(createLabelText(null));
		else	
			fContextLabel.setText(createLabelText(entry.getCorrespondingResource()));
		fContextLabel.pack(true);		
	}
	
	private static String createLabelText(IResource resource){
		String prefix= "Context";
		if (resource == null)
			return prefix;
		return prefix + " in:" + resource.getFullPath();	
	}
	
	private RefactoringStatusEntry getFirstEntry(){
		if (fStatus == null || fStatus.getEntries().isEmpty())
			return null;
		return (RefactoringStatusEntry)fStatus.getEntries().get(0);
	}
		
	//---- Reimplementation of WizardPage methods ------------------------------------------

	/* (non-Javadoc)
	 * Method declared in IWizardPage.
	 */
	public void createControl(Composite parent) {
		// Additional composite is needed to limit the size of the table to
		// 60 characters.
		//Composite content= new Composite(parent, SWT.NONE);
		SashForm content= new SashForm(parent, SWT.VERTICAL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1; layout.marginWidth= 0; layout.marginHeight= 0;
		content.setLayout(layout);
		
		createTableViewer(content);
		createSourceViewer(content);
		
		content.setWeights(new int[]{50, 50});
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
			showInSourceViewer(getFirstEntry());
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
