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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.text.spelling.SpellCheckEngine;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
 * Options configuration block for spell-check related settings.
 * 
 * @since 3.0
 */
public class SpellingConfigurationBlock extends OptionsConfigurationBlock {

	/** Preference keys for the preferences in this block */
	private static final String PREF_SPELLING_CHECK_SPELLING= PreferenceConstants.SPELLING_CHECK_SPELLING;
	private static final String PREF_SPELLING_IGNORE_DIGITS= PreferenceConstants.SPELLING_IGNORE_DIGITS;
	private static final String PREF_SPELLING_IGNORE_MIXED= PreferenceConstants.SPELLING_IGNORE_MIXED;
	private static final String PREF_SPELLING_IGNORE_SENTENCE= PreferenceConstants.SPELLING_IGNORE_SENTENCE;
	private static final String PREF_SPELLING_IGNORE_UPPER= PreferenceConstants.SPELLING_IGNORE_UPPER;
	private static final String PREF_SPELLING_IGNORE_URLS= PreferenceConstants.SPELLING_IGNORE_URLS;
	private static final String PREF_SPELLING_LOCALE= PreferenceConstants.SPELLING_LOCALE;
	private static final String PREF_SPELLING_PROPOSAL_THRESHOLD= PreferenceConstants.SPELLING_PROPOSAL_THRESHOLD;
	private static final String PREF_SPELLING_USER_DICTIONARY= PreferenceConstants.SPELLING_USER_DICTIONARY;
	private static final String PREF_SPELLING_ENABLE_CONTENTASSIST= PreferenceConstants.SPELLING_ENABLE_CONTENTASSIST;

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

	/**
	 * Returns the locale codes for the locale list.
	 * 
	 * @param locales
	 *                   The list of locales
	 * @return Array of locale codes for the list
	 */
	protected static String[] getDictionaryCodes(final Set locales) {

		int index= 0;
		Locale locale= null;

		final String[] codes= new String[locales.size()];
		for (final Iterator iterator= locales.iterator(); iterator.hasNext();) {

			locale= (Locale)iterator.next();
			codes[index++]= locale.toString();
		}
		return codes;
	}

	/**
	 * Returns the display labels for the locale list.
	 * 
	 * @param locales
	 *                   The list of locales
	 * @return Array of display labels for the list
	 */
	protected static String[] getDictionaryLabels(final Set locales) {

		int index= 0;
		Locale locale= null;

		final String[] labels= new String[locales.size()];
		for (final Iterator iterator= locales.iterator(); iterator.hasNext();) {

			locale= (Locale)iterator.next();
			labels[index++]= locale.getDisplayName(SpellCheckEngine.getDefaultLocale());
		}
		return labels;
	}

	/**
	 * Validates that the file with the specified absolute path exists and can
	 * be opened.
	 * 
	 * @param path
	 *                   The path of the file to validate
	 * @return <code>true</code> iff the file exists and can be opened,
	 *               <code>false</code> otherwise
	 */
	protected static IStatus validateAbsoluteFilePath(final String path) {

		final StatusInfo status= new StatusInfo();
		if (path.length() > 0) {

			final File file= new File(path);
			if (!file.isFile() || !file.isAbsolute() || !file.exists() || !file.canRead() || !file.canWrite())
				status.setError(PreferencesMessages.getString("SpellingPreferencePage.dictionary.error")); //$NON-NLS-1$

		}
		return status;
	}

	/**
	 * Validates that the specified locale is available.
	 * 
	 * @param locale
	 *                   The locale to validate
	 * @return The status of the validation
	 */
	protected static IStatus validateLocale(final String locale) {

		final StatusInfo status= new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("SpellingPreferencePage.locale.error")); //$NON-NLS-1$
		final Set locales= SpellCheckEngine.getAvailableLocales();

		Locale current= null;
		for (final Iterator iterator= locales.iterator(); iterator.hasNext();) {

			current= (Locale)iterator.next();
			if (current.toString().equals(locale))
				return new StatusInfo();
		}
		return status;
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
			status.setError(PreferencesMessages.getString("SpellingPreferencePage.empty_threshold")); //$NON-NLS-1$
		} else {
			try {
				final int value= Integer.parseInt(number);
				if (value < 0) {
					status.setError(PreferencesMessages.getFormattedString("SpellingPreferencePage.invalid_threshold", number)); //$NON-NLS-1$
				}
			} catch (NumberFormatException exception) {
				status.setError(PreferencesMessages.getFormattedString("SpellingPreferencePage.invalid_threshold", number)); //$NON-NLS-1$
			}
		}
		return status;
	}

	/** The dictionary path field */
	private Text fDictionaryPath= null;

	/** The status for the workspace dictionary file */
	private IStatus fFileStatus= new StatusInfo();

	/** The status for the platform locale */
	private IStatus fLocaleStatus= new StatusInfo();

	/** The status for the proposal threshold */
	private IStatus fThresholdStatus= new StatusInfo();

	/**
	 * Creates a new spelling configuration block.
	 * 
	 * @param context
	 *                   The status change listener
	 * @param project
	 *                   The Java project
	 */
	public SpellingConfigurationBlock(final IStatusChangeListener context, final IJavaProject project) {
		super(context, project, getAllKeys());

		IStatus status= validateAbsoluteFilePath((String)fWorkingValues.get(PREF_SPELLING_USER_DICTIONARY));
		if (status.getSeverity() != IStatus.OK)
			fWorkingValues.put(PREF_SPELLING_USER_DICTIONARY, ""); //$NON-NLS-1$

		status= validateLocale((String)fWorkingValues.get(PREF_SPELLING_LOCALE));
		if (status.getSeverity() != IStatus.OK)
			fWorkingValues.put(PREF_SPELLING_LOCALE, SpellCheckEngine.getDefaultLocale().toString());
	}

	protected Combo addComboBox(Composite parent, String label, String key, String[] values, String[] valueLabels, int indent) {
		ControlData data= new ControlData(key, values);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indent;
				
		Label labelControl= new Label(parent, SWT.LEFT | SWT.WRAP);
		labelControl.setText(label);
		labelControl.setLayoutData(gd);
		
		Combo comboBox= new Combo(parent, SWT.READ_ONLY);
		comboBox.setItems(valueLabels);
		comboBox.setData(data);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		comboBox.setLayoutData(gd);
		comboBox.addSelectionListener(getSelectionListener());
		
		fLabels.put(comboBox, labelControl);
		
		String currValue= (String)fWorkingValues.get(key);	
		comboBox.select(data.getSelection(currValue));
		
		fComboBoxes.add(comboBox);
		return comboBox;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(final Composite parent) {

		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(); layout.numColumns= 1;
		composite.setLayout(layout);

		final PixelConverter converter= new PixelConverter(parent);

		layout= new GridLayout();
		layout.numColumns= 3;

		final String[] trueFalse= new String[] { IPreferenceStore.TRUE, IPreferenceStore.FALSE };

		Group user= new Group(composite, SWT.NONE);
		user.setText(PreferencesMessages.getString("SpellingPreferencePage.preferences.user")); //$NON-NLS-1$
		user.setLayout(new GridLayout());		
		user.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		String label= PreferencesMessages.getString("SpellingPreferencePage.enable.label"); //$NON-NLS-1$
		final Button master= addCheckBox(user, label, PREF_SPELLING_CHECK_SPELLING, trueFalse, 0);

		label= PreferencesMessages.getString("SpellingPreferencePage.ignore.digits.label"); //$NON-NLS-1$
		Control slave= addCheckBox(user, label, PREF_SPELLING_IGNORE_DIGITS, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("SpellingPreferencePage.ignore.mixed.label"); //$NON-NLS-1$
		slave= addCheckBox(user, label, PREF_SPELLING_IGNORE_MIXED, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("SpellingPreferencePage.ignore.sentence.label"); //$NON-NLS-1$
		slave= addCheckBox(user, label, PREF_SPELLING_IGNORE_SENTENCE, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("SpellingPreferencePage.ignore.upper.label"); //$NON-NLS-1$
		slave= addCheckBox(user, label, PREF_SPELLING_IGNORE_UPPER, trueFalse, 20);
		createSelectionDependency(master, slave);

		label= PreferencesMessages.getString("SpellingPreferencePage.ignore.url.label"); //$NON-NLS-1$
		slave= addCheckBox(user, label, PREF_SPELLING_IGNORE_URLS, trueFalse, 20);
		createSelectionDependency(master, slave);

		final Group engine= new Group(composite, SWT.NONE);
		engine.setText(PreferencesMessages.getString("SpellingPreferencePage.preferences.engine")); //$NON-NLS-1$
		layout= new GridLayout();
		layout.numColumns= 3;
		engine.setLayout(layout);		
		engine.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		final Set locales= SpellCheckEngine.getAvailableLocales();

		label= PreferencesMessages.getString("SpellingPreferencePage.dictionary.label"); //$NON-NLS-1$
		Combo combo= addComboBox(engine, label, PREF_SPELLING_LOCALE, getDictionaryCodes(locales), getDictionaryLabels(locales), 0);
		combo.setEnabled(locales.size() > 1);
		
		label= PreferencesMessages.getString("SpellingPreferencePage.workspace.dictionary.label"); //$NON-NLS-1$
		fDictionaryPath= addTextField(engine, label, PREF_SPELLING_USER_DICTIONARY, 0, 0);
		((GridData)fDictionaryPath.getLayoutData()).horizontalSpan= 1;

		Button button= new Button(engine, SWT.PUSH);
		button.setText(PreferencesMessages.getString("SpellingPreferencePage.browse.label")); //$NON-NLS-1$
		button.addSelectionListener(new SelectionAdapter() {

		public void widgetSelected(final SelectionEvent event) {
				handleBrowseButtonSelected();
			}
		});
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		SWTUtil.setButtonDimensionHint(button);
		

		Group advanced= new Group(composite, SWT.NONE);
		advanced.setText(PreferencesMessages.getString("SpellingPreferencePage.preferences.advanced")); //$NON-NLS-1$
		layout= new GridLayout();
		layout.numColumns= 3;
		advanced.setLayout(layout);		
		advanced.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label= PreferencesMessages.getString("SpellingPreferencePage.proposals.threshold"); //$NON-NLS-1$
		Text text= addTextField(advanced, label, PREF_SPELLING_PROPOSAL_THRESHOLD, 0, 0);
		text.setTextLimit(3);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		data.widthHint= converter.convertWidthInCharsToPixels(4);
		text.setLayoutData(data);
		
		label= PreferencesMessages.getString("SpellingPreferencePage.enable.contentassist.label"); //$NON-NLS-1$
		addCheckBox(advanced, label, PREF_SPELLING_ENABLE_CONTENTASSIST, trueFalse, 0);

		return composite;
	}

	private static String[] getAllKeys() {
		return new String[] { PREF_SPELLING_USER_DICTIONARY, PREF_SPELLING_CHECK_SPELLING, PREF_SPELLING_IGNORE_DIGITS, PREF_SPELLING_IGNORE_MIXED, PREF_SPELLING_IGNORE_SENTENCE, PREF_SPELLING_IGNORE_UPPER, PREF_SPELLING_IGNORE_URLS, PREF_SPELLING_LOCALE, PREF_SPELLING_PROPOSAL_THRESHOLD, PREF_SPELLING_ENABLE_CONTENTASSIST };
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getDefaultOptions()
	 */
	protected Map getDefaultOptions() {

		final String[] keys= fAllKeys;
		final Map options= new HashMap();
		final IPreferenceStore store= PreferenceConstants.getPreferenceStore();

		for (int index= 0; index < keys.length; index++)
			options.put(keys[index], store.getDefaultString(keys[index]));

		return options;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getFullBuildDialogStrings(boolean)
	 */
	protected final String[] getFullBuildDialogStrings(final boolean workspace) {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#getOptions(boolean)
	 */
	protected Map getOptions(final boolean inherit) {

		final String[] keys= fAllKeys;
		final Map options= new HashMap();
		final IPreferenceStore store= PreferenceConstants.getPreferenceStore();

		for (int index= 0; index < keys.length; index++)
			options.put(keys[index], store.getString(keys[index]));

		return options;
	}

	/**
	 * Handles selections of the browse button.
	 */
	protected void handleBrowseButtonSelected() {

		final FileDialog dialog= new FileDialog(fDictionaryPath.getShell(), SWT.OPEN);
		dialog.setText(PreferencesMessages.getString("SpellingPreferencePage.filedialog.title")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] { PreferencesMessages.getString("SpellingPreferencePage.filter.dictionary.extension"), PreferencesMessages.getString("SpellingPreferencePage.filter.all.extension") }); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setFilterNames(new String[] { PreferencesMessages.getString("SpellingPreferencePage.filter.dictionary.label"), PreferencesMessages.getString("SpellingPreferencePage.filter.all.label") }); //$NON-NLS-1$ //$NON-NLS-2$

		final String path= dialog.open();
		if (path != null)
			fDictionaryPath.setText(path);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#setOptions(java.util.Map)
	 */
	protected void setOptions(final Map options) {

		final String[] keys= fAllKeys;
		final IPreferenceStore store= PreferenceConstants.getPreferenceStore();

		for (int index= 0; index < keys.length; index++)
			store.setValue(keys[index], (String)fWorkingValues.get(keys[index]));
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#validateSettings(java.lang.String,java.lang.String)
	 */
	protected void validateSettings(final String key, final String value) {

		if (key == null || PREF_SPELLING_PROPOSAL_THRESHOLD.equals(key))
			fThresholdStatus= validatePositiveNumber((String)fWorkingValues.get(PREF_SPELLING_PROPOSAL_THRESHOLD));

		if (key == null || PREF_SPELLING_USER_DICTIONARY.equals(key))
			fFileStatus= validateAbsoluteFilePath((String)fWorkingValues.get(PREF_SPELLING_USER_DICTIONARY));

		if (key == null || PREF_SPELLING_LOCALE.equals(key))
			fLocaleStatus= validateLocale((String)fWorkingValues.get(PREF_SPELLING_LOCALE));

		fContext.statusChanged(StatusUtil.getMostSevere(new IStatus[] { fThresholdStatus, fFileStatus, fLocaleStatus }));
	}
}
