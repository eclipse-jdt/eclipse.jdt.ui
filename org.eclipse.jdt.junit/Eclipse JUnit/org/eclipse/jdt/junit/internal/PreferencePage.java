/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage 
		implements IWorkbenchPreferencePage, SelectionListener {
	public static String NOF_PLUGINS_DIRS= "NOF_PLUGINS_DIRS";
	public static String PLUGINS_DIR_= "PLUGINS_DIR_";
	public static String PLUGINS_DIR_IS_CHECKED_= "PLUGINS_DIR_IS_CHECKED_";
	public static String PLUGINS_DIR_WS_CHECKED= "PLUGINS_DIR_WS_CHECKED";
	public static String PLUGINS_DIR_STARTING_CHECKED= "PLUGINS_DIR_STARTING_CHECKED";
	public static String PLUGIN_INIT_DONE= "PLUGIN_INIT_DONE";
	public static String TESTPLUGIN_FROM_WS= "TESTPLUGIN_FROM_WS";
	public static String CHECK_ALL_FOR_STARTUPJAR= "CHECK_ALL_FOR_STARTUPJAR";
	
	private static Table fgTable;
	private static Button fgAddButton;
	private static Button fgRemoveButton;
	private static DirectoryDialog fgDirectoryDialog;
	private static Button fTestPluginCheckBox;
	private static Button fStartupJarCheckBox;
	
	/**
	 * Constructor for JUnitPluginTestPreferencePage
	 */
	public PreferencePage() {
		super();
		setPreferenceStore(JUnitUIPlugin.getDefault().getPreferenceStore());
		fgDirectoryDialog= new DirectoryDialog(JUnitUIPlugin.getActiveShell());
		fgDirectoryDialog.setText("Select Eclipse plugins directory");
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
		IPreferenceStore store= getPreferenceStore();
		noDefaultAndApplyButton();
		
		Composite composite = createContainer(parent);	
		createCheckPanel(composite, store);
		createPluginsTable(composite);
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

	protected void createPluginsTable(Composite composite) {
		Label label= new Label(composite, SWT.WRAP);
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gridData.horizontalSpan= 2;
		label.setLayoutData(gridData);
		label.setText("Locations where Eclipse plugins can be found.");
		label= new Label(composite, SWT.WRAP);
		gridData= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gridData.horizontalSpan= 2;
		label.setLayoutData(gridData);
		label.setText("Required plugins and startup.jar are collected top - down in this list.");

		fgTable= new Table(composite, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
		gridData= new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
		fgTable.setLayoutData(gridData);
		fgTable.addSelectionListener(this);
		fillList();
	}
	protected void createAddRemovePanel(Composite composite) {
		Composite buttonPanel= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		buttonPanel.setLayout(layout);	
		
		GridData gridData= new GridData();
		buttonPanel.setLayoutData(gridData);
		
		fgAddButton= new Button(buttonPanel, SWT.PUSH);
		fgAddButton.setText("&Add...");
		fgAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fgAddButton.addSelectionListener(this);
		
		fgRemoveButton= new Button(buttonPanel, SWT.PUSH);
		fgRemoveButton.setText("&Remove");
		fgRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		fgRemoveButton.addSelectionListener(this);
	}
	protected void createCheckPanel(Composite composite, IPreferenceStore store) {
		Composite checkPanel= new Composite(composite, SWT.NONE);
		GridData gridData= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
		gridData.horizontalSpan= 2;
		checkPanel.setLayoutData(gridData);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.makeColumnsEqualWidth= false;
		checkPanel.setLayout(layout);
		
		createTestPluginCheckLabel(checkPanel, store);
		createStartupJarCheckLabel(checkPanel, store);
	}

	protected void createTestPluginCheckLabel(Composite checkPanel, IPreferenceStore store) {
		fTestPluginCheckBox= new Button(checkPanel, SWT.CHECK);
		GridData gridData= new GridData();
		fTestPluginCheckBox.setLayoutData(gridData);		
		if (!store.getBoolean(PLUGIN_INIT_DONE))
			store.setValue(TESTPLUGIN_FROM_WS, true);
		fTestPluginCheckBox.setSelection(store.getBoolean(TESTPLUGIN_FROM_WS));

		Label label= new Label(checkPanel, SWT.NONE);
		label.setText("Take the tested plugin form the current workspace.");
		gridData= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);
	}
	
	protected void createStartupJarCheckLabel(Composite checkPanel, IPreferenceStore store) {	
		fStartupJarCheckBox= new Button(checkPanel, SWT.CHECK);
		GridData gridData= new GridData();
		fStartupJarCheckBox.setLayoutData(gridData);
		if (!store.getBoolean(PLUGIN_INIT_DONE))
			store.setValue(CHECK_ALL_FOR_STARTUPJAR, true);
		fStartupJarCheckBox.setSelection(store.getBoolean(CHECK_ALL_FOR_STARTUPJAR));
		
		Label label= new Label(checkPanel, SWT.NONE);
		label.setText("Search for startup.jar relative to all directories, also if they are unchecked in the list.");
		gridData= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);	
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
	/**
	 * @see SelectionListener#widgetSelected(SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent selectionEvent) {
		if (fgTable.getSelectionCount() == 0)
			fgTable.setSelection(0);
		if (fgTable.getSelectionIndex() < 2) 
			fgRemoveButton.setEnabled(false);
		else 
			fgRemoveButton.setEnabled(true);
			
		if (selectionEvent.getSource().equals(fgAddButton)) {
			String directory= fgDirectoryDialog.open();
			if (directory != null) {
				TableItem tableItem= new TableItem(fgTable, SWT.CHECK);
				tableItem.setText(directory);
				tableItem.setChecked(true);
			}
		}
		else if (selectionEvent.getSource().equals(fgRemoveButton)) {
			fgTable.remove(fgTable.getSelectionIndex());
		}	
	}
	
	public boolean performOk() {
		IPreferenceStore store= getPreferenceStore();
		int nOfPluginDirs= fgTable.getItemCount() - 2;
		
		store.setValue(PLUGINS_DIR_WS_CHECKED, fgTable.getItem(0).getChecked());
		store.setValue(PLUGINS_DIR_STARTING_CHECKED, fgTable.getItem(1).getChecked());
		
		store.setValue(NOF_PLUGINS_DIRS, nOfPluginDirs);
		for (int i= 0; i < nOfPluginDirs; i++) {
			store.setValue(PLUGINS_DIR_ + i, fgTable.getItem(i + 2).getText());
			store.setValue(PLUGINS_DIR_IS_CHECKED_ + i, fgTable.getItem(i + 2).getChecked());
		}
		store.setValue(TESTPLUGIN_FROM_WS, fTestPluginCheckBox.getSelection());
		store.setValue(CHECK_ALL_FOR_STARTUPJAR, fStartupJarCheckBox.getSelection());
		store.setValue(PLUGIN_INIT_DONE, true);
		return true;			
	}
	
	private void fillList() {
		IPreferenceStore store= getPreferenceStore();
		
		boolean initDone= store.getBoolean(PLUGIN_INIT_DONE);
		TableItem tableItem= new TableItem(fgTable, SWT.CHECK);
		tableItem.setText("Workspace");
		if(initDone)
			tableItem.setChecked(store.getBoolean(PLUGINS_DIR_WS_CHECKED));
		else {
			tableItem.setChecked(true);
			store.setValue(PLUGINS_DIR_WS_CHECKED, true);
		}
		
		tableItem= new TableItem(fgTable, SWT.CHECK);
		tableItem.setText("Plugins directory of current Eclipse instance");
		if(initDone)
			tableItem.setChecked(store.getBoolean(PLUGINS_DIR_STARTING_CHECKED));
		else {
			tableItem.setChecked(true);
			store.setValue(PLUGINS_DIR_STARTING_CHECKED, true);
		}
			
		int nOfPluginsDirs= store.getInt(NOF_PLUGINS_DIRS);
		
		for (int i= 0; i < nOfPluginsDirs; i++) {
			tableItem= new TableItem(fgTable, SWT.CHECK);
			tableItem.setText(store.getString(PLUGINS_DIR_ + i));
			tableItem.setChecked(store.getBoolean(PLUGINS_DIR_IS_CHECKED_ + i));
		}
	}
}

