/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.PixelConverter;

/**
 * Configures the code assist preferences.
 * 
 * @since 3.0
 */
class CodeAssistConfigurationBlock extends AbstractConfigurationBlock {

	private List fContentAssistColorList;
	private ColorEditor fContentAssistColorEditor;
    private Control fAutoInsertDelayText;
    private Control fAutoInsertJavaTriggerText;
    private Control fAutoInsertJavaDocTriggerText;
	private Label fAutoInsertDelayLabel;
	private Label fAutoInsertJavaTriggerLabel;
	private Label fAutoInsertJavaDocTriggerLabel;
	private Button fCompletionInsertsRadioButton;
	private Button fCompletionOverwritesRadioButton;
	/**
	 * List of master/slave listeners when there's a dependency.
	 * 
	 * @see #createDependency(Button, Control)
	 * @since 3.0
	 */

	private final String[][] fContentAssistColorListModel= new String[][] {
			{PreferencesMessages.getString("JavaEditorPreferencePage.backgroundForCompletionProposals"), PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.foregroundForCompletionProposals"), PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.backgroundForMethodParameters"), PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.foregroundForMethodParameters"), PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.backgroundForCompletionReplacement"), PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.foregroundForCompletionReplacement"), PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND } //$NON-NLS-1$
		};

	
	public CodeAssistConfigurationBlock(PreferencePage mainPreferencePage, OverlayPreferenceStore store) {
		super(store, mainPreferencePage);
		
		store.addKeys(createOverlayStoreKeys());
	}

	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList overlayKeys= new ArrayList();
	
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_AUTOACTIVATION));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_AUTOINSERT));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND));		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_ORDER_PROPOSALS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_CASE_SENSITIVITY));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_ADDIMPORT));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_INSERT_COMPLETION));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_PREFIX_COMPLETION));
		
		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}

	/**
	 * Creates page for hover preferences.
	 * 
	 * @param parent the parent composite
	 * @return the control for the preference page
	 */
	public Control createControl(Composite parent) {
		SectionManager manager= new SectionManager(JavaPlugin.getDefault().getPreferenceStore(), "code_assist_preferences_last_open"); //$NON-NLS-1$
		Composite contents= manager.createSectionComposite(parent);
		Composite composite;
		
		composite= manager.createSection(PreferencesMessages.getString("CodeAssistConfigurationBlock.insertionSection.title")); //$NON-NLS-1$
		composite.setLayout(new GridLayout(2, false));
		
		addCompletionRadioButtons(composite);
		
		String label;		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.insertSingleProposalsAutomatically"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.CODEASSIST_AUTOINSERT, 0);		
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.completePrefixes"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, 0);		
		
		composite= manager.createSection(PreferencesMessages.getString("CodeAssistConfigurationBlock.sortingSection.title")); //$NON-NLS-1$
		composite.setLayout(new GridLayout(2, false));
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.showOnlyProposalsVisibleInTheInvocationContext"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.presentProposalsInAlphabeticalOrder"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.CODEASSIST_ORDER_PROPOSALS, 0);
		
		composite= manager.createSection(PreferencesMessages.getString("CodeAssistConfigurationBlock.advancedSection.title")); //$NON-NLS-1$
		composite.setLayout(new GridLayout(2, false));
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.automaticallyAddImportInsteadOfQualifiedName"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.CODEASSIST_ADDIMPORT, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.fillArgumentNamesOnMethodCompletion"); //$NON-NLS-1$
		Button master= addCheckBox(composite, label, PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.guessArgumentNamesOnMethodCompletion"); //$NON-NLS-1$
		Button slave= addCheckBox(composite, label, PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, 0);
		createDependency(master, slave);
		
		composite= manager.createSection(PreferencesMessages.getString("CodeAssistConfigurationBlock.autoactivationSection.title")); //$NON-NLS-1$
		composite.setLayout(new GridLayout(2, false));
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.enableAutoActivation"); //$NON-NLS-1$
		final Button autoactivation= addCheckBox(composite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION, 0);
		autoactivation.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateAutoactivationControls();
			}
		});		
		
		Control[] labelledTextField;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.autoActivationDelay"); //$NON-NLS-1$
		labelledTextField= addLabelledTextField(composite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 4, 0, true);
		fAutoInsertDelayLabel= getLabelControl(labelledTextField);
		fAutoInsertDelayText= getTextControl(labelledTextField);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJava"); //$NON-NLS-1$
		labelledTextField= addLabelledTextField(composite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, 4, 0, false);
		fAutoInsertJavaTriggerLabel= getLabelControl(labelledTextField);
		fAutoInsertJavaTriggerText= getTextControl(labelledTextField);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJavaDoc"); //$NON-NLS-1$
		labelledTextField= addLabelledTextField(composite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, 4, 0, false);
		fAutoInsertJavaDocTriggerLabel= getLabelControl(labelledTextField);
		fAutoInsertJavaDocTriggerText= getTextControl(labelledTextField);
		
		composite= manager.createSection(PreferencesMessages.getString("CodeAssistConfigurationBlock.appearanceSection.title")); //$NON-NLS-1$
		composite.setLayout(new GridLayout(2, false));
		
		Label l= new Label(composite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("JavaEditorPreferencePage.codeAssist.colorOptions")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		l.setLayoutData(gd);
		
		Composite editorComposite= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(SWT.BEGINNING, SWT.FILL, false, true);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		
		
		PixelConverter pixelConverter= new PixelConverter(parent);
		fContentAssistColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		gd.heightHint= pixelConverter.convertHeightInCharsToPixels(9); // limit initial size, but allow to take all it can.
		fContentAssistColorList.setLayoutData(gd);
		
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l= new Label(stylesComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("JavaEditorPreferencePage.codeAssist.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		l.setLayoutData(gd);
		
		fContentAssistColorEditor= new ColorEditor(stylesComposite);
		Button colorButton= fContentAssistColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		colorButton.setLayoutData(gd);
		
		fContentAssistColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleContentAssistColorListSelection();
			}
		});
		colorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fContentAssistColorList.getSelectionIndex();
				String key= fContentAssistColorListModel[i][1];
				
				PreferenceConverter.setValue(getPreferenceStore(), key, fContentAssistColorEditor.getColorValue());
			}
		});
		
		contents.layout();
		
		return contents;
	}
	
	private static Text getTextControl(Control[] labelledTextField){
		return (Text)labelledTextField[1];
	}

	private static Label getLabelControl(Control[] labelledTextField){
		return (Label)labelledTextField[0];
	}

	private void addCompletionRadioButtons(Composite contentAssistComposite) {
		Composite completionComposite= new Composite(contentAssistComposite, SWT.NONE);
		GridData ccgd= new GridData();
		ccgd.horizontalSpan= 2;
		completionComposite.setLayoutData(ccgd);
		GridLayout ccgl= new GridLayout();
		ccgl.marginWidth= 0;
		ccgl.numColumns= 2;
		completionComposite.setLayout(ccgl);
		
		SelectionListener completionSelectionListener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {				
				boolean insert= fCompletionInsertsRadioButton.getSelection();
				getPreferenceStore().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, insert);
			}
		};
		
		fCompletionInsertsRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionInsertsRadioButton.setText(PreferencesMessages.getString("JavaEditorPreferencePage.completionInserts")); //$NON-NLS-1$
		fCompletionInsertsRadioButton.setLayoutData(new GridData());
		fCompletionInsertsRadioButton.addSelectionListener(completionSelectionListener);
		
		fCompletionOverwritesRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionOverwritesRadioButton.setText(PreferencesMessages.getString("JavaEditorPreferencePage.completionOverwrites")); //$NON-NLS-1$
		fCompletionOverwritesRadioButton.setLayoutData(new GridData());
		fCompletionOverwritesRadioButton.addSelectionListener(completionSelectionListener);
	}
	
	public void initialize() {
		super.initialize();
		
		initializeFields();
		
		for (int i= 0; i < fContentAssistColorListModel.length; i++)
			fContentAssistColorList.add(fContentAssistColorListModel[i][0]);
		fContentAssistColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fContentAssistColorList != null && !fContentAssistColorList.isDisposed()) {
					fContentAssistColorList.select(0);
					handleContentAssistColorListSelection();
				}
			}
		});
		
	}

	private void initializeFields() {
		boolean completionInserts= getPreferenceStore().getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
		fCompletionInsertsRadioButton.setSelection(completionInserts);
		fCompletionOverwritesRadioButton.setSelection(! completionInserts);
		
		updateAutoactivationControls();
 	}
	
    private void updateAutoactivationControls() {
        boolean autoactivation= getPreferenceStore().getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION);
        fAutoInsertDelayText.setEnabled(autoactivation);
		fAutoInsertDelayLabel.setEnabled(autoactivation);

        fAutoInsertJavaTriggerText.setEnabled(autoactivation);
		fAutoInsertJavaTriggerLabel.setEnabled(autoactivation);

        fAutoInsertJavaDocTriggerText.setEnabled(autoactivation);
		fAutoInsertJavaDocTriggerLabel.setEnabled(autoactivation);
    }

	public void performDefaults() {
		super.performDefaults();
		initializeFields();
		handleContentAssistColorListSelection();
	}
	
	private void handleContentAssistColorListSelection() {	
		int i= fContentAssistColorList.getSelectionIndex();
		String key= fContentAssistColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(getPreferenceStore(), key);
		fContentAssistColorEditor.setColorValue(rgb);
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		// nothing to dispose
	}
}
