/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * Preference page for JUnit settings. Supports to define the failure
 * stack filter patterns.
 */
public class JUnitPreferencePage extends PreferencePage 
		implements IWorkbenchPreferencePage, SelectionListener {
					
	private String fFilterString;
	private Table fTable;
	private Button fAddButton;
	private Button fRemoveButton;
	
	public static String STACK_FILTER_ENTRIES_COUNT= "NOF_STACK_FILTER_ENTRIES"; //$NON-NLS-1$
	public static String STACK_FILTER_ENTRY_= "STACK_FILTER_ENTRY_"; //$NON-NLS-1$
	public static String DO_FILTER_STACK= "DO_FILTER_STACK"; //$NON-NLS-1$

	private static String[] fgDefaultFilterPatterns= new String[] {
		"org.eclipse.jdt.internal.junit.runner", //$NON-NLS-1$
		"org.eclipse.jdt.internal.junit.ui", //$NON-NLS-1$
		"junit.framework.TestCase", //$NON-NLS-1$
		"junit.framework.TestResult", //$NON-NLS-1$
		"junit.framework.TestSuite", //$NON-NLS-1$
		"junit.framework.Assert.", // don't filter AssertionFailure //$NON-NLS-1$
		"java.lang.reflect.Method.invoke" //$NON-NLS-1$
	};

	public JUnitPreferencePage() {
		super();
		setPreferenceStore(JUnitPlugin.getDefault().getPreferenceStore());
	}

	public static String[] getFilterPatterns() {
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
		int count= store.getInt(STACK_FILTER_ENTRIES_COUNT);
		String[] result= new String[count];
		for (int i= 0; i < count; i++) {
			result[i]= store.getString(STACK_FILTER_ENTRY_ + i);
		}
		return result;
	}
	
	public static boolean getFilterStack() {
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(DO_FILTER_STACK);
	}
	
	public static void setFilterStack(boolean filter) {
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
		store.setValue(DO_FILTER_STACK, filter);
	}
	
	public static void initializeDefaults(IPreferenceStore store) {
		store.setDefault(JUnitPreferencePage.DO_FILTER_STACK, true); 
		int count= store.getInt(STACK_FILTER_ENTRIES_COUNT);
		if (count == 0) {
			store.setValue(STACK_FILTER_ENTRIES_COUNT, fgDefaultFilterPatterns.length);
			for (int i= 0; i < fgDefaultFilterPatterns.length; i++) {
				store.setValue(STACK_FILTER_ENTRY_ + i, fgDefaultFilterPatterns[i]);
			}
		}
	}
	
	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		
		createFilterTable(composite);
		createListButtons(composite);
		return composite;
	}
		
	protected void createListButtons(Composite composite) {
		Composite buttonComposite= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		buttonComposite.setLayout(layout);	
		GridData gridData= new GridData();
		gridData.verticalAlignment= GridData.FILL;
		gridData.horizontalAlignment= GridData.FILL;
		buttonComposite.setLayoutData(gridData);
		
		fAddButton= new Button(buttonComposite, SWT.CENTER | SWT.PUSH);
		fAddButton.setText(JUnitMessages.getString("JUnitPreferencePage.addbutton.label")); //$NON-NLS-1$
		setButtonGridData(fAddButton);
		fAddButton.addSelectionListener(this);

		fRemoveButton= new Button(buttonComposite, SWT.CENTER | SWT.PUSH);
		fRemoveButton.setText(JUnitMessages.getString("JUnitPreferencePage.removebutton.label")); //$NON-NLS-1$
		setButtonGridData(fRemoveButton);
		fRemoveButton.addSelectionListener(this);
	}
	
	private void setButtonGridData(Button button) {
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.heightHint= convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		int widthHint= convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint= Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
	}

	protected void createFilterTable(Composite composite) {
		Label label= new Label(composite, SWT.NONE);
		label.setText(JUnitMessages.getString("JUnitPreferencePage.filter.label")); //$NON-NLS-1$
		GridData gridData= new GridData();
		gridData.horizontalAlignment= GridData.FILL;
		gridData.horizontalSpan= 2;
		label.setLayoutData(gridData);
		
		fTable= new Table(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data= new GridData();
		//Set heightHint with a small value so the list size will be defined by 
		//the space available in the dialog instead of resizing the dialog to
		//fit all the items in the list.
		data.heightHint= fTable.getItemHeight();
		data.verticalAlignment= GridData.FILL;
		data.horizontalAlignment= GridData.FILL;
		data.grabExcessHorizontalSpace= true;
		data.grabExcessVerticalSpace= true;

		fTable.setLayoutData(data);
		fTable.addSelectionListener(this);
		fillList();
		if (fTable.getItemCount() > 0)
			fTable.setSelection(0);
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/*
	 * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent selectionEvent) {
		widgetSelected(selectionEvent);
	}


	protected void addFilterString(String filterEntry) {
		if (filterEntry != null) {
			TableItem tableItem= new TableItem(fTable, SWT.NONE);
			tableItem.setText(filterEntry);
		}
	}
			
	/*
	 * @see SelectionListener#widgetSelected(SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent selectionEvent) {
		if (selectionEvent.getSource().equals(fAddButton)) {
			FilterPatternsDialog dialog= new FilterPatternsDialog(getControl().getShell(), JUnitMessages.getString("JUnitPreferencePage.adddialog.title"), JUnitMessages.getString("JUnitPreferencePage.addialog.prompt")); //$NON-NLS-1$ //$NON-NLS-2$
			dialog.open();
			String pattern= dialog.getValue();
			addFilterString(pattern);
			fTable.deselectAll();
			fTable.select(fTable.getItemCount()-1);
		}
		else if (selectionEvent.getSource().equals(fRemoveButton)) {
			fTable.remove(fTable.getSelectionIndices());
		}	
	    fRemoveButton.setEnabled(fTable.getSelectionIndex() != -1);
	}
	
	public boolean performOk() {
		IPreferenceStore store= getPreferenceStore();
		int filterEntriesCount= fTable.getItemCount();
		
		store.setValue(STACK_FILTER_ENTRIES_COUNT, filterEntriesCount);
		for (int i= 0; i < filterEntriesCount; i++) {
			store.setValue(STACK_FILTER_ENTRY_ + i, fTable.getItem(i).getText());
		}
		return true;			
	}
	
	private void fillList() {
		String[] patterns= getFilterPatterns();		
		for (int i= 0; i < patterns.length; i++) {
			TableItem tableItem= new TableItem(fTable, SWT.NONE);
			tableItem.setText(patterns[i]);
		}
	}
}