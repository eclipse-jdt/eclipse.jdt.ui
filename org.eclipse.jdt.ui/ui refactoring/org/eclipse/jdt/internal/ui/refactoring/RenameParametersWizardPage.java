/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;
import org.eclipse.swt.SWT;import org.eclipse.swt.custom.TableEditor;import org.eclipse.swt.events.ModifyEvent;import org.eclipse.swt.events.ModifyListener;import org.eclipse.swt.events.SelectionAdapter;import org.eclipse.swt.events.SelectionEvent;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Table;import org.eclipse.swt.widgets.TableColumn;import org.eclipse.swt.widgets.TableItem;import org.eclipse.swt.widgets.Text;import org.eclipse.jface.viewers.ColumnWeightData;import org.eclipse.jface.viewers.TableLayout;import org.eclipse.ui.help.DialogPageContextComputer;import org.eclipse.ui.help.WorkbenchHelp;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.core.refactoring.methods.RenameParametersRefactoring;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;public class RenameParametersWizardPage extends UserInputWizardPage{

	private Table fTable;
	private TableItem[] fItems;
	
	public static final String PAGE_NAME= "RenameParametersInputPage"; //$NON-NLS-1$
	
	/**
	 * Creates a new text input page.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public RenameParametersWizardPage(boolean isLastUserPage) {
		super(PAGE_NAME, isLastUserPage);
	}
		
	private RenameParametersRefactoring getRenameParametersRefactoring(){
		return (RenameParametersRefactoring)getRefactoring();
	}			
	
	/**
	 * Performs input validation. Returns a <code>RefactoringStatus</code> which
	 * describes the result of input validation. <code>Null<code> is interpreted
	 * as no error.
	 */
	private RefactoringStatus validateTable(String[] newNames){
		RenameParametersRefactoring ref= getRenameParametersRefactoring();
		ref.setNewParameterNames(newNames);
		return ref.checkNewNames();
	}
	
	private String[] getOldParameterNames(){
		RenameParametersRefactoring ref= getRenameParametersRefactoring();
		String[] oldNames;
		try{
			 oldNames= ref.getMethod().getParameterNames();
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameParametersWizardPage.refactoring"), RefactoringMessages.getString("RenameParametersWizardPage.internal_error")); //$NON-NLS-2$ //$NON-NLS-1$
			oldNames= new String[]{""}; //$NON-NLS-1$
		}
		return oldNames;
	}
	
	private void createTableItems(){
		String[] oldNames= getOldParameterNames();
		fItems= new TableItem[oldNames.length];		
		for (int i= 0; i < oldNames.length ; i++){
			fItems[i]= new TableItem(fTable, SWT.NONE);
			fItems[i].setText(new String[]{oldNames[i], oldNames[i]});
		}		
	}
	
	private void createTable(Composite parent) {
		fTable = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		TableLayout layout= new TableLayout();
		fTable.setLayout(layout);
		fTable.setHeaderVisible(true);
		fTable.setLinesVisible(true);
		TableColumn column1= new TableColumn(fTable, SWT.NONE);
		column1.setText(RefactoringMessages.getString("RenameParametersWizardPage.old_names")); //$NON-NLS-1$
		TableColumn column2= new TableColumn(fTable, SWT.NONE);
		column2.setText(RefactoringMessages.getString("RenameParametersWizardPage.new_names")); //$NON-NLS-1$
		
		layout.addColumnData(new ColumnWeightData(50));
		layout.addColumnData(new ColumnWeightData(50));
		createTableItems();
 		final TableEditor editor = new TableEditor (fTable);
		fTable.addSelectionListener (new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
		
				// Clean up any previous editor control
				Control oldEditor = editor.getEditor();
				if (oldEditor != null)
					oldEditor.dispose();	
		
				// Identify the selected row
				int index = fTable.getSelectionIndex ();
				if (index == -1)
					return;
				TableItem item = fTable.getItem (index);
		
				// The control that will be the editor must be a child of the Table
				Text text = new Text(fTable, SWT.NONE);
				text.addModifyListener(createTextModifyListener(text, item));
		
				//The text editor must have the same size as the cell and must
				//not be any smaller than 50 pixels.
				editor.horizontalAlignment = SWT.LEFT;
				editor.grabHorizontal = true;
				editor.minimumWidth = 50;
		
				// Open the text editor in the second column of the selected row.
				editor.setEditor (text, item, 1);
		
				// Assign focus to the text control
				text.setFocus ();
			}
			});
	}
	
	private ModifyListener createTextModifyListener(final Text text, final TableItem item){
		return new ModifyListener(){
			public void modifyText(ModifyEvent e){
				item.setText(1, text.getText());
				tableModified(getNewNames());
			}
		};
	}
	
	/**
	 * @see DialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		result.setLayout(layout);
		createTable(result);
		fTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		WorkbenchHelp.setHelp(getControl(), new DialogPageContextComputer(this, IJavaHelpContextIds.RENAME_PARAMS_WIZARD_PAGE));		
	}
		
	/**
	 * Checks the page's state and issues a corresponding error message. The page validation
	 * is computed by calling <code>validateTable</code>.
	 */
	private void tableModified(String[] newNames) {			
		RefactoringStatus status= validateTable(newNames);
		getRefactoringWizard().setStatus(status);
		if (status != null && status.hasFatalError()) {
			setPageComplete(false);
			setErrorMessage(status.getFirstMessage(RefactoringStatus.FATAL));
		} else {
			setPageComplete(true);	
			setErrorMessage(null);
		}	
	}
	
	/* (non-Javadoc)
	 * Method declared in IDialogPage
	 */
	public void dispose() {
		//the table will be sent the dispose message when its parent is disposed
		fTable= null;
	}
	
	/* (non-Javadoc)
	 * Method declared in WizardPage
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			tableModified(getNewNames());
		}
		super.setVisible(visible);
		if (visible && fTable != null) {
			fTable.setFocus();
		}
	}
	
	private String[] getNewNames(){
		String[] result= new String[fTable.getItemCount()];
		for (int i= 0; i < result.length; i++){
			result[i]= fItems[i].getText(1); //second column
		}
		return result;
	}
}
