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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

import org.eclipse.jface.text.Assert;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.PixelConverter;

/**
 * Configures the code assist preferences.
 * 
 * @since 3.0
 */
class CodeAssistConfigurationBlock implements IPreferenceConfigurationBlock {


	private OverlayPreferenceStore fStore;
	private PreferencePage fMainPreferencePage;
	
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
	 * @see #createDependency(Button, String, Control)
	 * @since 3.0
	 */
	private ArrayList fMasterSlaveListeners= new ArrayList();

	private final String[][] fContentAssistColorListModel= new String[][] {
			{PreferencesMessages.getString("JavaEditorPreferencePage.backgroundForCompletionProposals"), PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.foregroundForCompletionProposals"), PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.backgroundForMethodParameters"), PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.foregroundForMethodParameters"), PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.backgroundForCompletionReplacement"), PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND }, //$NON-NLS-1$
			{PreferencesMessages.getString("JavaEditorPreferencePage.foregroundForCompletionReplacement"), PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND } //$NON-NLS-1$
		};

	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	private Map fTextFields= new HashMap();
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			Text text= (Text) e.widget;
			fStore.setValue((String) fTextFields.get(text), text.getText());
		}
	};

	private ArrayList fNumberFields= new ArrayList();
	private ModifyListener fNumberFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			numberFieldChanged((Text) e.widget);
		}
	};
	
	
	public CodeAssistConfigurationBlock(PreferencePage mainPreferencePage, OverlayPreferenceStore store) {
		Assert.isNotNull(mainPreferencePage);
		Assert.isNotNull(store);
		fStore= store;
		fStore.addKeys(createOverlayStoreKeys());
		fMainPreferencePage= mainPreferencePage;
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
		
		PixelConverter pixelConverter= new PixelConverter(parent);

		Composite contentAssistComposite= new Composite(parent, SWT.NONE);
		contentAssistComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout= new GridLayout(); 
		layout.numColumns= 2;
		contentAssistComposite.setLayout(layout);
		
		addCompletionRadioButtons(contentAssistComposite);
		
		String label;		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.insertSingleProposalsAutomatically"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOINSERT, 0);		
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.completePrefixes"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, 0);		
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.showOnlyProposalsVisibleInTheInvocationContext"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.presentProposalsInAlphabeticalOrder"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_ORDER_PROPOSALS, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.automaticallyAddImportInsteadOfQualifiedName"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_ADDIMPORT, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.fillArgumentNamesOnMethodCompletion"); //$NON-NLS-1$
		Button master= addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.guessArgumentNamesOnMethodCompletion"); //$NON-NLS-1$
		Button slave= addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, 0);
		createDependency(master, PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, slave);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.enableAutoActivation"); //$NON-NLS-1$
		final Button autoactivation= addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION, 0);
		autoactivation.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateAutoactivationControls();
			}
		});		
		
		Control[] labelledTextField;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.autoActivationDelay"); //$NON-NLS-1$
		labelledTextField= addLabelledTextField(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 4, 0, true);
		fAutoInsertDelayLabel= getLabelControl(labelledTextField);
		fAutoInsertDelayText= getTextControl(labelledTextField);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJava"); //$NON-NLS-1$
		labelledTextField= addLabelledTextField(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, 4, 0, false);
		fAutoInsertJavaTriggerLabel= getLabelControl(labelledTextField);
		fAutoInsertJavaTriggerText= getTextControl(labelledTextField);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJavaDoc"); //$NON-NLS-1$
		labelledTextField= addLabelledTextField(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, 4, 0, false);
		fAutoInsertJavaDocTriggerLabel= getLabelControl(labelledTextField);
		fAutoInsertJavaDocTriggerText= getTextControl(labelledTextField);
		
		
		Label l= new Label(contentAssistComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("JavaEditorPreferencePage.codeAssist.colorOptions")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		l.setLayoutData(gd);
		
		Composite editorComposite= new Composite(contentAssistComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		
		
		fContentAssistColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= pixelConverter.convertHeightInCharsToPixels(8);
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
				
				PreferenceConverter.setValue(fStore, key, fContentAssistColorEditor.getColorValue());
			}
		});
		
		return contentAssistComposite;
	}
	
	private void createDependency(final Button master, String masterKey, final Control slave) {
		indent(slave);
		boolean masterState= fStore.getBoolean(masterKey);
		slave.setEnabled(masterState);
		SelectionListener listener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		master.addSelectionListener(listener);
		fMasterSlaveListeners.add(listener);
	}

	private static void indent(Control control) {
		GridData gridData= new GridData();
		gridData.horizontalIndent= 20;
		control.setLayoutData(gridData);		
	}
	
	private static Text getTextControl(Control[] labelledTextField){
		return (Text)labelledTextField[1];
	}

	private static Label getLabelControl(Control[] labelledTextField){
		return (Label)labelledTextField[0];
	}

	/**
	 * Returns an array of size 2:
	 *  - first element is of type <code>Label</code>
	 *  - second element is of type <code>Text</code>
	 * Use <code>getLabelControl</code> and <code>getTextControl</code> to get the 2 controls.
	 * 
	 * @param composite 	the parent composite
	 * @param label			the text field's label
	 * @param key			the preference key
	 * @param textLimit		the text limit
	 * @param indentation	the field's indentation
	 * @param isNumber		<code>true</code> iff this text field is used to e4dit a number
	 * @return
	 */
	private Control[] addLabelledTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		
		PixelConverter pixelConverter= new PixelConverter(composite);
		
		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		Text textControl= new Text(composite, SWT.BORDER | SWT.SINGLE);		
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint= pixelConverter.convertWidthInCharsToPixels(textLimit + 1);
		textControl.setLayoutData(gd);
		textControl.setTextLimit(textLimit);
		fTextFields.put(textControl, key);
		if (isNumber) {
			fNumberFields.add(textControl);
			textControl.addModifyListener(fNumberFieldListener);
		} else {
			textControl.addModifyListener(fTextFieldListener);
		}
			
		return new Control[]{labelControl, textControl};
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
				fStore.setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, insert);
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

	void initializeFields() {
		Iterator e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fStore.getBoolean(key));
		}
		
		e= fTextFields.keySet().iterator();
		while (e.hasNext()) {
			Text t= (Text) e.next();
			String key= (String) fTextFields.get(t);
			t.setText(fStore.getString(key));
		}
		
		boolean completionInserts= fStore.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
		fCompletionInsertsRadioButton.setSelection(completionInserts);
		fCompletionOverwritesRadioButton.setSelection(! completionInserts);
		
		updateAutoactivationControls();
        
        updateStatus(validatePositiveNumber("0")); //$NON-NLS-1$
        
        // Update slaves
        Iterator iter= fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener= (SelectionListener)iter.next();
            listener.widgetSelected(null);
        }
	}
	
    private void updateAutoactivationControls() {
        boolean autoactivation= fStore.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION);
        fAutoInsertDelayText.setEnabled(autoactivation);
		fAutoInsertDelayLabel.setEnabled(autoactivation);

        fAutoInsertJavaTriggerText.setEnabled(autoactivation);
		fAutoInsertJavaTriggerLabel.setEnabled(autoactivation);

        fAutoInsertJavaDocTriggerText.setEnabled(autoactivation);
		fAutoInsertJavaDocTriggerLabel.setEnabled(autoactivation);
    }

	public void performOk() {
	}

	public  void performDefaults() {
		handleContentAssistColorListSelection();
		initializeFields();
	}
	
	private void handleContentAssistColorListSelection() {	
		int i= fContentAssistColorList.getSelectionIndex();
		String key= fContentAssistColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fStore, key);
		fContentAssistColorEditor.setColorValue(rgb);
	}
	
	private void numberFieldChanged(Text textControl) {
		String number= textControl.getText();
		IStatus status= validatePositiveNumber(number);
		if (!status.matches(IStatus.ERROR))
			fStore.setValue((String) fTextFields.get(textControl), number);
		updateStatus(status);
	}
	
	private IStatus validatePositiveNumber(String number) {
		StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.getString("JavaEditorPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0)
					status.setError(PreferencesMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				status.setError(PreferencesMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	private void updateStatus(IStatus status) {
		fMainPreferencePage.setValid(status.isOK());
		StatusUtil.applyToStatusLine(fMainPreferencePage, status);
	}
	
	private Button addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 2;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		
		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		// nothing to dispose
	}
}
