package org.eclipse.jdt.internal.ui.refactoring;

import java.text.MessageFormat;

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

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class ExtractInterfaceInputPage extends TextInputWizardPage {

	public ExtractInterfaceInputPage() {
		super(true);
	}

	public ExtractInterfaceInputPage(String initialValue) {
		super(true, "");
	}

	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.verticalSpacing= 8;
		result.setLayout(layout);
		
		Label label= new Label(result, SWT.NONE);
		label.setText("Interface name:");
		
		Text text= createTextInputField(result);
		text.selectAll();
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		addReplaceAllCheckbox(result);
		addMemberListComposite(result);
	}

	private void addMemberListComposite(Composite result) {
		Composite composite= new Composite(result, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan= 2;
		composite.setLayoutData(gd);
		
		final CheckboxTableViewer tableViewer= CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		tableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer.setLabelProvider(new JavaElementLabelProvider());
		tableViewer.setContentProvider(createContentProvider());
		try {
			tableViewer.setInput(getExtractInterfaceRefactoring().getExtractableMembers());
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, "Extract Interface", "Internal Error"); //XXX
			tableViewer.setInput(new IMember[0]);
		}

		Composite buttonComposite= new Composite(composite, SWT.NONE);
		GridLayout gl= new GridLayout();
		gl.marginHeight= 0;
		gl.marginWidth= 0;
		buttonComposite.setLayout(gl);
		gd= new GridData(GridData.FILL_VERTICAL);
		buttonComposite.setLayoutData(gd);
		
		Button selectAll= new Button(buttonComposite, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(selectAll);
		selectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(true);
			}
		});

		Button deSelectAll= new Button(buttonComposite, SWT.PUSH);
		deSelectAll.setText("Deselect All");
		deSelectAll.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(deSelectAll);
		deSelectAll.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				tableViewer.setAllChecked(false);
			}
		});
	}

	private IStructuredContentProvider createContentProvider() {
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
		String key= "Change references to the class ''{0}'' into references to the interface (where possible)"; 
		String title= MessageFormat.format(key, new String[]{getExtractInterfaceRefactoring().getInputClass().getElementName()});
		boolean defaultValue= getExtractInterfaceRefactoring().isReplaceOccurrences();
		final Button checkBox= createCheckbox(result,  title, defaultValue);
		getExtractInterfaceRefactoring().setReplaceOccurrences(checkBox.getSelection());
		checkBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				getExtractInterfaceRefactoring().setReplaceOccurrences(checkBox.getSelection());
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
}
