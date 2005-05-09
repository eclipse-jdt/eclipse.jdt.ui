/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;

import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Configures the code assist preferences.
 * 
 * @since 3.0
 */
class CodeAssistConfigurationBlock extends OptionsConfigurationBlock {
	
	private static final Key PREF_CODEASSIST_AUTOACTIVATION= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION);
	private static final Key PREF_CODEASSIST_AUTOACTIVATION_DELAY= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY);
	private static final Key PREF_CODEASSIST_AUTOINSERT= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOINSERT);
	private static final Key PREF_CODEASSIST_PROPOSALS_BACKGROUND= getJDTUIKey(PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND);
	private static final Key PREF_CODEASSIST_PROPOSALS_FOREGROUND= getJDTUIKey(PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND);
	private static final Key PREF_CODEASSIST_PARAMETERS_BACKGROUND= getJDTUIKey(PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND);
	private static final Key PREF_CODEASSIST_PARAMETERS_FOREGROUND= getJDTUIKey(PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND);
	private static final Key PREF_CODEASSIST_REPLACEMENT_BACKGROUND= getJDTUIKey(PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND);
	private static final Key PREF_CODEASSIST_REPLACEMENT_FOREGROUND= getJDTUIKey(PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND);		
	private static final Key PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
	private static final Key PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC= getJDTUIKey(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC);
	private static final Key PREF_CODEASSIST_SHOW_VISIBLE_PROPOSALS= getJDTUIKey(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS);
	private static final Key PREF_CODEASSIST_ORDER_PROPOSALS= getJDTUIKey(PreferenceConstants.CODEASSIST_ORDER_PROPOSALS);
	private static final Key PREF_CODEASSIST_CASE_SENSITIVITY= getJDTUIKey(PreferenceConstants.CODEASSIST_CASE_SENSITIVITY);
	private static final Key PREF_CODEASSIST_ADDIMPORT= getJDTUIKey(PreferenceConstants.CODEASSIST_ADDIMPORT);
	private static final Key PREF_CODEASSIST_INSERT_COMPLETION= getJDTUIKey(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
	private static final Key PREF_CODEASSIST_FILL_ARGUMENT_NAMES= getJDTUIKey(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES);
	private static final Key PREF_CODEASSIST_GUESS_METHOD_ARGUMENTS= getJDTUIKey(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
	private static final Key PREF_CODEASSIST_PREFIX_COMPLETION= getJDTUIKey(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION);
	private static final Key PREF_CODEASSIST_HIDE_RESTRICTED_REFERENCES= getJDTCoreKey(JavaCore.CODEASSIST_HIDE_RESTRICTED_REFERENCES);

	private static Key[] getAllKeys() {
		return new Key[] {
				PREF_CODEASSIST_AUTOACTIVATION,
				PREF_CODEASSIST_AUTOACTIVATION_DELAY,
				PREF_CODEASSIST_AUTOINSERT,
				PREF_CODEASSIST_PROPOSALS_BACKGROUND,
				PREF_CODEASSIST_PROPOSALS_FOREGROUND,
				PREF_CODEASSIST_PARAMETERS_BACKGROUND,
				PREF_CODEASSIST_PARAMETERS_FOREGROUND,
				PREF_CODEASSIST_REPLACEMENT_BACKGROUND,
				PREF_CODEASSIST_REPLACEMENT_FOREGROUND,		
				PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA,
				PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC,
				PREF_CODEASSIST_SHOW_VISIBLE_PROPOSALS,
				PREF_CODEASSIST_ORDER_PROPOSALS,
				PREF_CODEASSIST_CASE_SENSITIVITY,
				PREF_CODEASSIST_ADDIMPORT,
				PREF_CODEASSIST_INSERT_COMPLETION,
				PREF_CODEASSIST_FILL_ARGUMENT_NAMES,
				PREF_CODEASSIST_GUESS_METHOD_ARGUMENTS,
				PREF_CODEASSIST_PREFIX_COMPLETION,
				PREF_CODEASSIST_HIDE_RESTRICTED_REFERENCES,
		};	
	}
	
	private static final String[] trueFalse= new String[] { IPreferenceStore.TRUE, IPreferenceStore.FALSE };

	private List fContentAssistColorList;
	private ColorEditor fContentAssistColorEditor;
	private Button fCompletionInsertsRadioButton;
	private Button fCompletionOverwritesRadioButton;

	private final Object[][] fContentAssistColorListModel= new Object[][] {
			{PreferencesMessages.JavaEditorPreferencePage_backgroundForCompletionProposals, PREF_CODEASSIST_PROPOSALS_BACKGROUND }, 
			{PreferencesMessages.JavaEditorPreferencePage_foregroundForCompletionProposals, PREF_CODEASSIST_PROPOSALS_FOREGROUND }, 
			{PreferencesMessages.JavaEditorPreferencePage_backgroundForMethodParameters, PREF_CODEASSIST_PARAMETERS_BACKGROUND }, 
			{PreferencesMessages.JavaEditorPreferencePage_foregroundForMethodParameters, PREF_CODEASSIST_PARAMETERS_FOREGROUND }, 
			{PreferencesMessages.JavaEditorPreferencePage_backgroundForCompletionReplacement, PREF_CODEASSIST_REPLACEMENT_BACKGROUND }, 
			{PreferencesMessages.JavaEditorPreferencePage_foregroundForCompletionReplacement, PREF_CODEASSIST_REPLACEMENT_FOREGROUND } 
		};


	public CodeAssistConfigurationBlock(IStatusChangeListener statusListener, IWorkbenchPreferenceContainer workbenchcontainer) {
		super(statusListener, null, getAllKeys(), workbenchcontainer);
	}

	protected Control createContents(Composite parent) {
		ScrolledPageContent scrolled= new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
//		scrolled.setDelayedReflow(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite control= new Composite(scrolled, SWT.NONE);
		GridLayout layout= new GridLayout();
		control.setLayout(layout);

		Composite composite;

		composite= createSubsection(control, PreferencesMessages.CodeAssistConfigurationBlock_insertionSection_title); 
		addInsertionSection(composite);
		
		composite= createSubsection(control, PreferencesMessages.CodeAssistConfigurationBlock_sortingSection_title); 
		addSortingSection(composite);
		
		composite= createSubsection(control, PreferencesMessages.CodeAssistConfigurationBlock_autoactivationSection_title); 
		addAutoActivationSection(composite);
		
		composite= createSubsection(control, PreferencesMessages.CodeAssistConfigurationBlock_appearanceSection_title); 
		createAppearanceSection(composite);
		
		initialize();
		
		scrolled.setContent(control);
		final Point size= control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(size.x, size.y);
		return scrolled;
	}

	protected Composite createSubsection(Composite parent, String label) {
		Group group= new Group(parent, SWT.SHADOW_NONE);
		group.setText(label);
		GridData data= new GridData(SWT.FILL, SWT.CENTER, true, false);
		group.setLayoutData(data);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		group.setLayout(layout);

		return group;
	}

	private void addInsertionSection(Composite composite) {
		addCompletionRadioButtons(composite);
		
		String label;		
		label= PreferencesMessages.JavaEditorPreferencePage_insertSingleProposalsAutomatically;
		addCheckBox(composite, label, PREF_CODEASSIST_AUTOINSERT, trueFalse, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_completePrefixes; 
		addCheckBox(composite, label, PREF_CODEASSIST_PREFIX_COMPLETION, trueFalse, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_automaticallyAddImportInsteadOfQualifiedName; 
		addCheckBox(composite, label, PREF_CODEASSIST_ADDIMPORT, trueFalse, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_fillArgumentNamesOnMethodCompletion; 
		Button master= addCheckBox(composite, label, PREF_CODEASSIST_FILL_ARGUMENT_NAMES, trueFalse, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_guessArgumentNamesOnMethodCompletion; 
		Button slave= addCheckBox(composite, label, PREF_CODEASSIST_GUESS_METHOD_ARGUMENTS, trueFalse, 20);
		createSelectionDependency(master, slave);
	}

	/**
	 * Creates a selection dependency between a master and a slave control.
	 * 
	 * @param master
	 *                   The master button that controls the state of the slave
	 * @param slave
	 *                   The slave control that is enabled only if the master is
	 *                   selected
	 */
	protected static void createSelectionDependency(final Button master, final Control slave) {

		master.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent event) {
				// Do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				slave.setEnabled(master.getSelection());
			}
		});
		slave.setEnabled(master.getSelection());
	}
	
	private void addSortingSection(Composite composite) {
		String label;
		label= PreferencesMessages.JavaEditorPreferencePage_presentProposalsInAlphabeticalOrder; 
		addCheckBox(composite, label, PREF_CODEASSIST_ORDER_PROPOSALS, trueFalse, 0);
		
		label= PreferencesMessages.JavaEditorPreferencePage_showOnlyProposalsVisibleInTheInvocationContext; 
		addCheckBox(composite, label, PREF_CODEASSIST_SHOW_VISIBLE_PROPOSALS, trueFalse, 0);
		
		label= PreferencesMessages.CodeAssistConfigurationBlock_hideDiscouraged_label;
		String[] vals= {JavaCore.NEVER, JavaCore.ERROR, JavaCore.WARNING};
		String[] labels= {
				PreferencesMessages.CodeAssistConfigurationBlock_hideDiscouraged_value_never,
				PreferencesMessages.CodeAssistConfigurationBlock_hideDiscouraged_value_error,
				PreferencesMessages.CodeAssistConfigurationBlock_hideDiscouraged_value_warning
		};
		addComboBox(composite, label, PREF_CODEASSIST_HIDE_RESTRICTED_REFERENCES, vals, labels, 0);
	}

	private void addAutoActivationSection(Composite composite) {
		String label;
		label= PreferencesMessages.JavaEditorPreferencePage_enableAutoActivation; 
		final Button autoactivation= addCheckBox(composite, label, PREF_CODEASSIST_AUTOACTIVATION, trueFalse, 0);
		autoactivation.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateAutoactivationControls();
			}
		});		
		
		label= PreferencesMessages.JavaEditorPreferencePage_autoActivationDelay; 
		addLabelledTextField(composite, label, PREF_CODEASSIST_AUTOACTIVATION_DELAY, 4, 0, true);
		
		label= PreferencesMessages.JavaEditorPreferencePage_autoActivationTriggersForJava; 
		addLabelledTextField(composite, label, PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, 4, 0, false);
		
		label= PreferencesMessages.JavaEditorPreferencePage_autoActivationTriggersForJavaDoc; 
		addLabelledTextField(composite, label, PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, 4, 0, false);
	}
	
	protected Text addLabelledTextField(Composite parent, String label, Key key, int textlimit, int indent, boolean dummy) {	
		PixelConverter pixelConverter= new PixelConverter(parent);
		
		Label labelControl= new Label(parent, SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(new GridData());
				
		Text textBox= new Text(parent, SWT.BORDER | SWT.SINGLE);
		textBox.setData(key);
		textBox.setLayoutData(new GridData());
		
		fLabels.put(textBox, labelControl);
		
		String currValue= getValue(key);	
		if (currValue != null) {
			textBox.setText(currValue);
		}
		textBox.addModifyListener(getTextModifyListener());

		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		if (textlimit != 0) {
			textBox.setTextLimit(textlimit);
			data.widthHint= pixelConverter.convertWidthInCharsToPixels(textlimit + 1);
		}
		data.horizontalIndent= indent;
		data.horizontalSpan= 2;
		textBox.setLayoutData(data);

		fTextBoxes.add(textBox);
		return textBox;
	}

	private void createAppearanceSection(Composite composite) {
		Label l= new Label(composite, SWT.LEFT);
		l.setText(PreferencesMessages.JavaEditorPreferencePage_codeAssist_colorOptions); 
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
		
		PixelConverter pixelConverter= new PixelConverter(composite);
		
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
		l.setText(PreferencesMessages.JavaEditorPreferencePage_codeAssist_color); 
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
				Key key= (Key) fContentAssistColorListModel[i][1];
				setValue(key, StringConverter.asString(fContentAssistColorEditor.getColorValue()));
			}
		});
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
				setValue(PREF_CODEASSIST_INSERT_COMPLETION, insert);
			}
		};
		
		fCompletionInsertsRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionInsertsRadioButton.setText(PreferencesMessages.JavaEditorPreferencePage_completionInserts); 
		fCompletionInsertsRadioButton.setLayoutData(new GridData());
		fCompletionInsertsRadioButton.addSelectionListener(completionSelectionListener);
		
		fCompletionOverwritesRadioButton= new Button(completionComposite, SWT.RADIO | SWT.LEFT);
		fCompletionOverwritesRadioButton.setText(PreferencesMessages.JavaEditorPreferencePage_completionOverwrites); 
		fCompletionOverwritesRadioButton.setLayoutData(new GridData());
		fCompletionOverwritesRadioButton.addSelectionListener(completionSelectionListener);
	}
	
	public void initialize() {
		initializeFields();
		
		for (int i= 0; i < fContentAssistColorListModel.length; i++)
			fContentAssistColorList.add((String) fContentAssistColorListModel[i][0]);
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
		boolean completionInserts= getBooleanValue(PREF_CODEASSIST_INSERT_COMPLETION);
		fCompletionInsertsRadioButton.setSelection(completionInserts);
		fCompletionOverwritesRadioButton.setSelection(! completionInserts);
		
		updateAutoactivationControls();
 	}
	
    private void updateAutoactivationControls() {
        boolean autoactivation= getBooleanValue(PREF_CODEASSIST_AUTOACTIVATION);
        setControlEnabled(PREF_CODEASSIST_AUTOACTIVATION_DELAY, autoactivation);
        setControlEnabled(PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, autoactivation);
        setControlEnabled(PREF_CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, autoactivation);
    }

    
	public void performDefaults() {
		super.performDefaults();
		initializeFields();
		handleContentAssistColorListSelection();
	}
	
	private void handleContentAssistColorListSelection() {	
		int i= fContentAssistColorList.getSelectionIndex();
		if (i == -1)
			return;
		Key key= (Key) fContentAssistColorListModel[i][1];
		RGB rgb= StringConverter.asRGB(getValue(key));
		fContentAssistColorEditor.setColorValue(rgb);
	}

	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return null;
	}
	
	/**
	 * Validates that the specified number is positive.
	 * 
	 * @param number
	 *                   The number to validate
	 * @return The status of the validation
	 */
	protected static IStatus validatePositiveNumber(final String number) {

		final StatusInfo status= new StatusInfo();
		if (number.length() == 0) {
			status.setError(PreferencesMessages.SpellingPreferencePage_empty_threshold); 
		} else {
			try {
				final int value= Integer.parseInt(number);
				if (value < 0) {
					status.setError(Messages.format(PreferencesMessages.SpellingPreferencePage_invalid_threshold, number)); 
				}
			} catch (NumberFormatException exception) {
				status.setError(Messages.format(PreferencesMessages.SpellingPreferencePage_invalid_threshold, number)); 
			}
		}
		return status;
	}
	
	protected void validateSettings(Key key, String oldValue, String newValue) {
		if (key == null || PREF_CODEASSIST_AUTOACTIVATION_DELAY.equals(key))
			fContext.statusChanged(validatePositiveNumber(getValue(PREF_CODEASSIST_AUTOACTIVATION_DELAY)));
	}

	public Control createControl(Composite parent) {
		ScrolledPageContent scrolled= new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolled.setDelayedReflow(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		Control control= createContents(scrolled);
		scrolled.setContent(control);
		final Point size= control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(size.x, size.y);
		return scrolled;
	}

	protected void setControlEnabled(Key key, boolean enabled) {
		Control control= getControl(key);
		Label label= (Label) fLabels.get(control);
		control.setEnabled(enabled);
		label.setEnabled(enabled);
	}

	private Control getControl(Key key) {
		for (int i= fComboBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fComboBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		for (int i= fCheckBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fCheckBoxes.get(i);
			ControlData data= (ControlData) curr.getData();
			if (key.equals(data.getKey())) {
				return curr;
			}
		}
		for (int i= fTextBoxes.size() - 1; i >= 0; i--) {
			Control curr= (Control) fTextBoxes.get(i);
			Key currKey= (Key) curr.getData();
			if (key.equals(currKey)) {
				return curr;
			}
		}
		return null;		
	}
}
