package org.eclipse.jdt.internal.ui.refactoring;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class ExtractInterfaceInputPage extends TextInputWizardPage {

	private Button fReplaceAllCheckbox;
	private CheckboxTableViewer fTableViewer;

	public ExtractInterfaceInputPage() {
		super(true);
		setMessage("Select the name for the new interface and select the members that will be declared in the interface.");
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 8;
		result.setLayout(layout);
		
		Label label= new Label(result, SWT.NONE);
		label.setText("&Interface name:");
		
		Text text= createTextInputField(result);
		text.selectAll();
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		addReplaceAllCheckbox(result);

		Label separator= new Label(result, SWT.NONE);
		GridData gd= new GridData();
		gd.horizontalSpan= 2;
		separator.setLayoutData(gd);

		Label tableLabel= new Label(result, SWT.NONE);
		tableLabel.setText("&Members to declare in the interface:");
		tableLabel.setEnabled(anyMembersToExtract());
		gd= new GridData();
		gd.horizontalSpan= 2;
		tableLabel.setLayoutData(gd);
		
		addMemberListComposite(result);
	}

	private void addMemberListComposite(Composite result) {
		Composite composite= new Composite(result, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);
		
		fTableViewer= CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		fTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		fTableViewer.setLabelProvider(new JavaElementLabelProvider());
		fTableViewer.setContentProvider(createContentProvider());
		try {
			fTableViewer.setInput(getExtractInterfaceRefactoring().getExtractableMembers());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, "Extract Interface", "Internal Error. See log for details");
			fTableViewer.setInput(new IMember[0]);
		}
		fTableViewer.getControl().setEnabled(anyMembersToExtract());

		createButtonComposite(composite);
	}

	private void createButtonComposite(Composite composite) {
		GridData gd;
		Composite buttonComposite= new Composite(composite, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		buttonComposite.setLayout(gl);
		gd= new GridData(GridData.FILL_VERTICAL);
		buttonComposite.setLayoutData(gd);
		
		Button selectAll= new Button(buttonComposite, SWT.PUSH);
		selectAll.setText("&Select All");
		selectAll.setEnabled(anyMembersToExtract());
		selectAll.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(selectAll);
		selectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				fTableViewer.setAllChecked(true);
			}
		});
		
		Button deSelectAll= new Button(buttonComposite, SWT.PUSH);
		deSelectAll.setText("&Deselect All");
		deSelectAll.setEnabled(anyMembersToExtract());
		deSelectAll.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(deSelectAll);
		deSelectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				fTableViewer.setAllChecked(false);
			}
		});
	}

	private boolean anyMembersToExtract() {
		try {
			return getExtractInterfaceRefactoring().getExtractableMembers().length > 0;
		} catch (JavaModelException e) {
			return false;
		}
	}

	private static IStructuredContentProvider createContentProvider() {
		return new IStructuredContentProvider(){
			public void dispose() {
			}
			public Object[] getElements(Object inputElement) {
				return (IMember[])inputElement;
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		};
	}

	private void addReplaceAllCheckbox(Composite result) {
		String key= "&Change references to the class ''{0}'' into references to the interface (where possible)"; 
		String title= MessageFormat.format(key, new String[]{getExtractInterfaceRefactoring().getInputClass().getElementName()});
		boolean defaultValue= getExtractInterfaceRefactoring().isReplaceOccurrences();
		fReplaceAllCheckbox= createCheckbox(result,  title, defaultValue);
		getExtractInterfaceRefactoring().setReplaceOccurrences(fReplaceAllCheckbox.getSelection());
		fReplaceAllCheckbox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getExtractInterfaceRefactoring().setReplaceOccurrences(fReplaceAllCheckbox.getSelection());
			}
		});		
	}

	private static Button createCheckbox(Composite parent, String title, boolean value){
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(title);
		checkBox.setSelection(value);
		GridData layoutData= new GridData();
		layoutData.horizontalSpan= 2;

		checkBox.setLayoutData(layoutData);
		return checkBox;		
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage#validateTextField(String)
	 */
	protected RefactoringStatus validateTextField(String text) {
		getExtractInterfaceRefactoring().setNewInterfaceName(text);
		return getExtractInterfaceRefactoring().checkNewInterfaceName(text);
	}	

	private ExtractInterfaceRefactoring getExtractInterfaceRefactoring(){
		return (ExtractInterfaceRefactoring)getRefactoring();
	}
	
	/*
	 * @see org.eclipse.jface.wizard.IWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		try {
			initializeRefactoring();
			return super.getNextPage();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage#performFinish()
	 */
	public boolean performFinish(){
		try {
			initializeRefactoring();
			return super.performFinish();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}

	private void initializeRefactoring() throws JavaModelException {
		getExtractInterfaceRefactoring().setNewInterfaceName(getText());
		getExtractInterfaceRefactoring().setReplaceOccurrences(fReplaceAllCheckbox.getSelection());
		List checked= Arrays.asList(fTableViewer.getCheckedElements());
		getExtractInterfaceRefactoring().setExtractedMembers((IMember[]) checked.toArray(new IMember[checked.size()]));
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		fReplaceAllCheckbox= null;
		fTableViewer= null;
		super.dispose();
	}

}
