/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.ExtendedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.quickdiff.QuickDiff;
import org.eclipse.ui.texteditor.quickdiff.ReferenceProviderDescriptor;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Preference page for work in progress.
 */
public class WorkInProgressPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	/** prefix for resources */
	private static final String PREFIX= "WorkInProgress."; //$NON-NLS-1$
	
	public final static String PREF_FORMATTER= "use_new_formatter"; //$NON-NLS-1$
	public final static String PREF_BGSEARCH= "search_in_background"; //$NON-NLS-1$
	public final static String PREF_SEARCH_MENU= "small_search_menu"; //$NON-NLS-1$
	public final static String PREF_SEARCH_IGNORE_IMPORTS= "search_ignore_imports"; //$NON-NLS-1$
	
	private List fCheckBoxes;
	private List fRadioButtons;
	private List fTextControls;
	
	/** List for the reference provider default. */
	private org.eclipse.swt.widgets.List fQuickDiffProviderList;
	/** The reference provider default's list model. */
	private String[][] fQuickDiffProviderListModel;
	/** Button controlling default setting of the selected reference provider. */
	private Button fSetDefaultButton;

	/**
	 * creates a new preference page.
	 */
	public WorkInProgressPreferencePage() {
		setPreferenceStore(getPreferenceStore());
		fRadioButtons= new ArrayList();
		fCheckBoxes= new ArrayList();
		fTextControls= new ArrayList();

		List providers= new QuickDiff().getReferenceProviderDescriptors();
		fQuickDiffProviderListModel= createQuickDiffReferenceListModel(providers);
	}

	private String[][] createQuickDiffReferenceListModel(List providers) {
		ArrayList listModelItems= new ArrayList();
		for (Iterator it= providers.iterator(); it.hasNext();) {
			ReferenceProviderDescriptor provider= (ReferenceProviderDescriptor) it.next();
			String label= provider.getLabel();
			int i= label.indexOf('&');
			while (i >= 0) {
				if (i < label.length())
					label= label.substring(0, i) + label.substring(i+1);
				else
					label.substring(0, i);
				i= label.indexOf('&');
			}
			listModelItems.add(new String[] { provider.getId(), label });
		}
		String[][] items= new String[listModelItems.size()][];
		listModelItems.toArray(items);
		return items;
	}
	
	private void handleProviderListSelection() {
		int i= fQuickDiffProviderList.getSelectionIndex();
		
		boolean b= getPreferenceStore().getString(ExtendedTextEditorPreferenceConstants.QUICK_DIFF_DEFAULT_PROVIDER).equals(fQuickDiffProviderListModel[i][0]);
		fSetDefaultButton.setEnabled(!b);
	}
	
	private Button addCheckBox(Composite parent, String label, String key) { 
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		
		Button button= new Button(parent, SWT.CHECK);
		button.setText(label);
		button.setData(key);
		button.setLayoutData(gd);

		button.setSelection(getPreferenceStore().getBoolean(key));
		
		fCheckBoxes.add(button);
		return button;
	}
	
	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), "WORK_IN_PROGRESS_PREFERENCE_PAGE"); //$NON-NLS-1$
	}

	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite result= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= 0;
		layout.verticalSpacing= convertVerticalDLUsToPixels(10);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		result.setLayout(layout);
		
		Group group= new Group(result, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PreferencesMessages.getString(PREFIX + "editor")); //$NON-NLS-1$
	
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "markOccurrences"), PreferenceConstants.EDITOR_MARK_OCCURRENCES); //$NON-NLS-1$
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "rollover"), PreferenceConstants.EDITOR_ANNOTATION_ROLL_OVER); //$NON-NLS-1$
		createSpacer(group, 1);
	
		Label label= new Label(group, SWT.NONE);
		label.setText(PreferencesMessages.getString(PREFIX + "smartTyping.label")); //$NON-NLS-1$
	
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "smartTyping.smartSemicolon"), PreferenceConstants.EDITOR_SMART_SEMICOLON); //$NON-NLS-1$
		
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "smartTyping.smartOpeningBrace"), PreferenceConstants.EDITOR_SMART_OPENING_BRACE); //$NON-NLS-1$
		
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "smartTyping.smartTab"), PreferenceConstants.EDITOR_SMART_TAB); //$NON-NLS-1$
		
		/* line change bar */
		group= new Group(result, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PreferencesMessages.getString(PREFIX + "quickdiff")); //$NON-NLS-1$
		
		Label l= new Label(group, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
	
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "showQuickDiffPerDefault"), ExtendedTextEditorPreferenceConstants.QUICK_DIFF_ALWAYS_ON); //$NON-NLS-1$
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "quickdiff.characterMode"), ExtendedTextEditorPreferenceConstants.QUICK_DIFF_CHARACTER_MODE); //$NON-NLS-1$
		
		l= new Label(group, SWT.LEFT );
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
	
		l= new Label(group, SWT.LEFT);
		l.setText(PreferencesMessages.getString(PREFIX + "quickdiff.referenceprovidertitle")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		l.setLayoutData(gd);
	
		Composite editorComposite= new Composite(group, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		
	
		fQuickDiffProviderList= new org.eclipse.swt.widgets.List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(4);
		fQuickDiffProviderList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fSetDefaultButton= new Button(stylesComposite, SWT.PUSH);
		fSetDefaultButton.setText(PreferencesMessages.getString(PREFIX + "quickdiff.setDefault")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fSetDefaultButton.setLayoutData(gd);
		
		fQuickDiffProviderList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				handleProviderListSelection();
			}
	
		});
		
		fSetDefaultButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				int i= fQuickDiffProviderList.getSelectionIndex();
				for (int j= 0; j < fQuickDiffProviderListModel.length; j++) {
					if (getPreferenceStore().getString(ExtendedTextEditorPreferenceConstants.QUICK_DIFF_DEFAULT_PROVIDER).equals(fQuickDiffProviderListModel[j][0])) {
						fQuickDiffProviderList.remove(j);
						fQuickDiffProviderList.add(fQuickDiffProviderListModel[j][1], j);
					}
					if (i == j) {
						fQuickDiffProviderList.remove(j);
						fQuickDiffProviderList.add(fQuickDiffProviderListModel[j][1] + " " + PreferencesMessages.getString(PREFIX + "quickdiff.defaultlabel"), j);  //$NON-NLS-1$//$NON-NLS-2$
					}
				}
				fSetDefaultButton.setEnabled(false);
				fQuickDiffProviderList.setSelection(i);
				fQuickDiffProviderList.redraw();
				
				getPreferenceStore().setValue(ExtendedTextEditorPreferenceConstants.QUICK_DIFF_DEFAULT_PROVIDER, fQuickDiffProviderListModel[i][0]);
			}
		});
		
		for (int i= 0; i < fQuickDiffProviderListModel.length; i++) {
			String sLabel= fQuickDiffProviderListModel[i][1];
			if (getPreferenceStore().getString(ExtendedTextEditorPreferenceConstants.QUICK_DIFF_DEFAULT_PROVIDER).equals(fQuickDiffProviderListModel[i][0]))
				sLabel += " " + PreferencesMessages.getString(PREFIX + "quickdiff.defaultlabel"); //$NON-NLS-1$ //$NON-NLS-2$
			fQuickDiffProviderList.add(sLabel);
		}
		fQuickDiffProviderList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fQuickDiffProviderList != null && !fQuickDiffProviderList.isDisposed()) {
					fQuickDiffProviderList.select(0);
					handleProviderListSelection();
				}
			}
		});
	
	
		group= new Group(result, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PreferencesMessages.getString(PREFIX + "quickassist.group")); //$NON-NLS-1$
		
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "quickassist.option"), PreferenceConstants.APPEARANCE_QUICKASSIST_LIGHTBULB); //$NON-NLS-1$ //$NON-NLS-2$
	
		group= new Group(result, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PreferencesMessages.getString(PREFIX + "search")); //$NON-NLS-1$
		
		addCheckBox(group, PreferencesMessages.getString(PREFIX + "search.small_menu"), PREF_SEARCH_MENU); //$NON-NLS-1$
		addCheckBox(group, PreferencesMessages.getString(PREFIX+"newsearch.option"), PREF_BGSEARCH); //$NON-NLS-1$
		addCheckBox(group, PreferencesMessages.getString(PREFIX+"search.ignore_imports"), PREF_SEARCH_IGNORE_IMPORTS); //$NON-NLS-1$
		return result;
	}
	
 
	/*
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	protected void createSpacer(Composite composite, int columnSpan) {
		Label label= new Label(composite, SWT.NONE);
		GridData gd= new GridData();
		gd.horizontalSpan= columnSpan;
		label.setLayoutData(gd);
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */
	protected IPreferenceStore doGetPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			button.setSelection(store.getDefaultBoolean(key));
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			String[] info= (String[]) button.getData();
			button.setSelection(info[1].equals(store.getDefaultString(info[0])));
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			text.setText(store.getDefaultString(key));
		}
		handleProviderListSelection();
		
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			store.setValue(key, button.getSelection());
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			if (button.getSelection()) {
				String[] info= (String[]) button.getData();
				store.setValue(info[0], info[1]);
			}
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			store.setValue(key, text.getText());
		}
		
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}

	/**
	 * @param store
	 */
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(PreferenceConstants.EDITOR_SMART_SEMICOLON, false);
		store.setDefault(PreferenceConstants.EDITOR_SMART_OPENING_BRACE, false);
		
		store.setDefault(PreferenceConstants.APPEARANCE_QUICKASSIST_LIGHTBULB, false);
		store.setDefault(PREF_SEARCH_MENU, true);
		store.setDefault(PREF_SEARCH_IGNORE_IMPORTS, false);
		
		store.setDefault(PreferenceConstants.EDITOR_MARK_OCCURRENCES, false);
		
	}
}
