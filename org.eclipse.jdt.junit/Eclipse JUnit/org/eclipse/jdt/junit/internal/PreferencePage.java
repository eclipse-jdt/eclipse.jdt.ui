/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import org.eclipse.jface.preference.IPreferenceStore;
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

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage 
		implements IWorkbenchPreferencePage, SelectionListener {
			
	private static class FilterDialog implements SelectionListener{ 		
		private Shell fShell;
		private Text fText;
		private Button fOkButton;
		private Button fCancelButton;
		private Button fFilterCheckBox;
		
		public FilterDialog(){ 
			fShell= new Shell(fgShell, SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.APPLICATION_MODAL);
			GridLayout gridLayout= new GridLayout();
			gridLayout.numColumns= 2;
			gridLayout.makeColumnsEqualWidth= false;
			fShell.setLayout(gridLayout);
			GridData gridData= new GridData();
			fShell.setLayoutData(gridData);
			fShell.setText("Add stack filter pattern");
			
			fText= new Text(fShell, SWT.BORDER | SWT.SINGLE | GridData.FILL_HORIZONTAL);
			gridData= new GridData();
			gridData.widthHint= 150;
			fText.setLayoutData(gridData);	
		
			Composite composite= new Composite(fShell, SWT.NONE);
			gridLayout= new GridLayout();
			gridLayout.numColumns= 2;
			gridLayout.makeColumnsEqualWidth= true;
			composite.setLayout(gridLayout);
			gridData= new GridData();
			composite.setLayoutData(gridData);
			
			fOkButton= new Button(composite, SWT.PUSH);
			fOkButton.setText("Ok");
			gridData= new GridData(GridData.FILL_HORIZONTAL);
			fOkButton.setLayoutData(gridData);
			fOkButton.addSelectionListener(this);
			
			fCancelButton= new Button(composite, SWT.PUSH);
			fCancelButton.setText("Cancel");
			gridData= new GridData(GridData.FILL_HORIZONTAL);
			fCancelButton.setLayoutData(gridData);			
			fCancelButton.addSelectionListener(this);
				
			fShell.pack();
		}
		
		public void open() {
			fShell.open();
		}
		
		/**
		 * @see SelectionListener#widgetSelected(SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent selectionEvent) {
			if (selectionEvent.getSource().equals(fOkButton)) {
				addFilterString(fText.getText());
				fShell.close();
			}
			else if (selectionEvent.getSource().equals(fCancelButton)) {
				fShell.close();
			}				
		}

		/**
		 * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent selectionEvent) {
			widgetSelected(selectionEvent);
		}
	}			
					
	private static String fFilterString;
	private static Table fgTable;
	private static Button fgAddButton;
	private static Button fgRemoveButton;
	private static FilterDialog fgFilterDialog;
	private static Shell fgShell;
	private static Button fFilterCheckBox;

	
	public static String PLUGIN_INIT_DONE= "PLUGIN_INIT_DONE";
	public static String NOF_STACK_FILTER_ENTRIES= "NOF_STACK_FILTER_ENTRIES";
	public static String STACK_FILTER_ENTRY_= "STACK_FILTER_ENTRY_";
	public static String DO_FILTER_STACK= "DO_FILTER_STACK";

	public static String[] fgFilterPatterns= new String[] {
		"org.eclipse.jdt.junit.internal",
		"org.eclipse.jdt.junit.eclipse.internal",
		"junit.framework.TestCase",
		"junit.framework.TestResult",
		"junit.framework.TestSuite",
		"junit.framework.Assert.", // don't filter AssertionFailure
		"junit.swingui.TestRunner",
		"junit.awtui.TestRunner",
		"junit.textui.TestRunner",
		"java.lang.reflect.Method.invoke"
	};

	/**
	 * Constructor for JUnitPluginTestPreferencePage
	 */
	public PreferencePage() {
		super();
		setPreferenceStore(JUnitPlugin.getDefault().getPreferenceStore());
		fgShell= new Shell(JUnitPlugin.getActiveShell());		
	}

	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		// added for 1GEUGE6: ITPJUI:WIN2000 - Help is the same on all preference pages
		super.createControl(parent);
	}
	
	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		
		Composite composite = createContainer(parent);
		
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
		if (!store.getBoolean(PLUGIN_INIT_DONE))
			store.setValue(DO_FILTER_STACK, true);
		fFilterCheckBox.setSelection(store.getBoolean(DO_FILTER_STACK));

		Label label= new Label(checkPanel, SWT.NONE);
		label.setText("Filter stack");
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
		fgRemoveButton= new Button(buttonPanel, SWT.PUSH);
		fgRemoveButton.setText("&Remove");
		fgRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		fgRemoveButton.addSelectionListener(this);
	}

	protected void createAddButton(Composite buttonPanel) {
		fgAddButton= new Button(buttonPanel, SWT.PUSH);
		fgAddButton.setText("&Add...");
		fgAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fgAddButton.addSelectionListener(this);
	}

	protected void createFilterTable(Composite composite) {
		Label label= new Label(composite, SWT.WRAP);
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gridData.horizontalSpan= 2;
		label.setLayoutData(gridData);
		label.setText("Stack filter lines:");
		
		fgTable= new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.NONE);
		gridData= new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
		fgTable.setLayoutData(gridData);
		fgTable.addSelectionListener(this);
		fillList();
	}
	
	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/**
	 * @see SelectionListener#widgetDefaultSelected(SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent selectionEvent) {
		widgetSelected(selectionEvent);
	}

	protected static void addFilterString(String filterEntry) {
		fgShell.setEnabled(true);
		if (filterEntry != null) {
			TableItem tableItem= new TableItem(fgTable, SWT.NONE);
			tableItem.setText(filterEntry);
		}
	}
			
	/**
	 * @see SelectionListener#widgetSelected(SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent selectionEvent) {
		if (selectionEvent.getSource().equals(fgAddButton)) {
			fgFilterDialog= new FilterDialog();
			fgFilterDialog.open();
			fgShell.setEnabled(false);
		}
		else if (selectionEvent.getSource().equals(fgRemoveButton)) {
			fgTable.remove(fgTable.getSelectionIndex());
		}	
	}
	
	public boolean performOk() {
		IPreferenceStore store= getPreferenceStore();
		int nOfStackFilterEntries= fgTable.getItemCount();
		
		store.setValue(NOF_STACK_FILTER_ENTRIES, nOfStackFilterEntries);
		for (int i= 0; i < nOfStackFilterEntries; i++) {
			store.setValue(STACK_FILTER_ENTRY_ + i, fgTable.getItem(i).getText());
		}
		store.setValue(DO_FILTER_STACK, fFilterCheckBox.getSelection());
		store.setValue(PLUGIN_INIT_DONE, true);
		return true;			
	}
	
	private void fillList() {
		IPreferenceStore store= getPreferenceStore();
		if (!store.getBoolean(PLUGIN_INIT_DONE)) {	
			for (int i= 0; i < fgFilterPatterns.length; i++) {
				TableItem tableItem= new TableItem(fgTable, SWT.NONE);
				tableItem.setText(fgFilterPatterns[i]);
			}
		}
		int nOfStackFilterEntries= store.getInt(NOF_STACK_FILTER_ENTRIES);
		if (nOfStackFilterEntries == 0) return;
		
		for (int i= 0; i < nOfStackFilterEntries; i++) {
			TableItem tableItem= new TableItem(fgTable, SWT.NONE);
			tableItem.setText(store.getString(STACK_FILTER_ENTRY_ + i));
		}
	}
}


