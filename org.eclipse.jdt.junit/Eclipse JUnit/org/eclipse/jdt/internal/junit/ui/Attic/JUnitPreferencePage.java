/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

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
	private Button fFilterCheckBox;
	
	public static String STACK_FILTER_ENTRIES_COUNT= "NOF_STACK_FILTER_ENTRIES";
	public static String STACK_FILTER_ENTRY_= "STACK_FILTER_ENTRY_";
	public static String DO_FILTER_STACK= "DO_FILTER_STACK";

	private static String[] fgDefaultFilterPatterns= new String[] {
		"org.eclipse.jdt.internal.junit.runner",
		"org.eclipse.jdt.internal.junit.ui",
		"junit.framework.TestCase",
		"junit.framework.TestResult",
		"junit.framework.TestSuite",
		"junit.framework.Assert.", // don't filter AssertionFailure
		"java.lang.reflect.Method.invoke"
	};

	/*
	 * Constructor for JUnitPluginTestPreferencePage
	 */
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
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
	}
	
	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		Composite composite= createContainer(parent);
		createCheckPanel(composite);			
		createFilterTable(composite);
		createAddRemovePanel(composite);
		return composite;
	}

	protected Composite createContainer(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.makeColumnsEqualWidth= false;
		composite.setLayout(layout);
		GridData gridData= new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		composite.setLayoutData(gridData);
		return composite;
	}
	
	protected void createCheckPanel(Composite composite) {
		Composite checkPanel= new Composite(composite, SWT.NONE);
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gridData.horizontalSpan= 2;
		checkPanel.setLayoutData(gridData);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.makeColumnsEqualWidth= false;
		checkPanel.setLayout(layout);
		createFilterCheckLabel(checkPanel);
	}
	
	protected void createFilterCheckLabel(Composite checkPanel) {
		IPreferenceStore store= getPreferenceStore();
		fFilterCheckBox= new Button(checkPanel, SWT.CHECK);
		GridData gridData= new GridData();
		fFilterCheckBox.setLayoutData(gridData);		
		fFilterCheckBox.setSelection(store.getBoolean(DO_FILTER_STACK));

		Label label= new Label(checkPanel, SWT.NONE);
		label.setText("&Filter stack trace entries of failed tests");
		gridData= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);
	}

	protected void createAddRemovePanel(Composite composite) {
		Composite buttonPanel= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		buttonPanel.setLayout(layout);	
		GridData gridData= new GridData();
		buttonPanel.setLayoutData(gridData);
		
		createAddButton(buttonPanel);
		createRemoveButton(buttonPanel);
	}

	protected void createRemoveButton(Composite buttonPanel) {
		fRemoveButton= new Button(buttonPanel, SWT.PUSH);
		fRemoveButton.setText("&Remove");
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		fRemoveButton.addSelectionListener(this);
	}

	protected void createAddButton(Composite buttonPanel) {
		fAddButton= new Button(buttonPanel, SWT.PUSH);
		fAddButton.setText("&Add...");
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fAddButton.addSelectionListener(this);
	}

	protected void createFilterTable(Composite composite) {
		Label label= new Label(composite, SWT.WRAP);
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gridData.horizontalSpan= 2;
		label.setLayoutData(gridData);
		label.setText("&Stack filter patterns:");
		
		fTable= new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.NONE);
		gridData= new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
		fTable.setLayoutData(gridData);
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
			FilterPatternsDialog dialog= new FilterPatternsDialog(getControl().getShell(), "Add Stack Filter Pattern", "Enter Filter Pattern:");
			dialog.open();
			String pattern= dialog.getValue();
			addFilterString(pattern);
			fTable.select(fTable.getItemCount()-1);
		}
		else if (selectionEvent.getSource().equals(fRemoveButton)) {
			fTable.remove(fTable.getSelectionIndex());
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
		store.setValue(DO_FILTER_STACK, fFilterCheckBox.getSelection());
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