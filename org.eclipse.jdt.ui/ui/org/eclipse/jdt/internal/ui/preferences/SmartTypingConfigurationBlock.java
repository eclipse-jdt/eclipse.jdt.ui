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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Configures Java Editor typing preferences.
 * 
 * @since 3.1
 */
class SmartTypingConfigurationBlock extends AbstractConfigurationBlock {

	public SmartTypingConfigurationBlock(OverlayPreferenceStore store) {
		super(store);
		
		store.addKeys(createOverlayStoreKeys());
	}
	
	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		return new OverlayPreferenceStore.OverlayKey[] {
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS),
				
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_PASTE),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE),
				
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_STRINGS),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACKETS),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACES),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_JAVADOCS),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WRAP_STRINGS),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ESCAPE_STRINGS),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS),
				
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_SEMICOLON),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_TAB),
				new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_OPENING_BRACE),
		};
	}	

	/**
	 * Creates page for mark occurrences preferences.
	 * 
	 * @param parent the parent composite
	 * @return the control for the preference page
	 */
	public Control createControl(Composite parent) {
		boolean useCollapsibleSections= false;
		
		SectionManager manager;
		Composite control;
		if (useCollapsibleSections) {
			manager= new SectionManager(JavaPlugin.getDefault().getPreferenceStore(), "smart_typing_preference_dialog_last_open_section"); //$NON-NLS-1$
			control= manager.createSectionComposite(parent);
		} else {
			manager= null;
			control= new Composite(parent, SWT.NONE);
			GridLayout layout= new GridLayout();
			layout.numColumns= 1;
			control.setLayout(layout);
		}

		Composite composite;
		
		composite= createSubsection(control, manager, PreferencesMessages.getString("SmartTypingConfigurationBlock.autoclose.title")); //$NON-NLS-1$
		addAutoclosingSection(composite);
		
		composite= createSubsection(control, manager, PreferencesMessages.getString("SmartTypingConfigurationBlock.automove.title")); //$NON-NLS-1$
		addAutopositionSection(composite);
		
		composite= createSubsection(control, manager, PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.title")); //$NON-NLS-1$
		addTabSection(composite);

		composite= createSubsection(control, manager, PreferencesMessages.getString("SmartTypingConfigurationBlock.pasting.title")); //$NON-NLS-1$
		addPasteSection(composite);
		
		composite= createSubsection(control, manager, PreferencesMessages.getString("SmartTypingConfigurationBlock.strings.title")); //$NON-NLS-1$
		addStringsSection(composite);
		
		composite= createSubsection(control, manager, PreferencesMessages.getString("SmartTypingConfigurationBlock.other.title")); //$NON-NLS-1$
		addOthersSection(composite);
		
		return control;
	}

	private Composite createSubsection(Composite parent, SectionManager manager, String label) {
		if (manager != null) {
			return manager.createSection(label);
		} else {
			Group group= new Group(parent, SWT.SHADOW_NONE);
			group.setText(label);
			GridData data= new GridData(SWT.FILL, SWT.CENTER, true, false);
			group.setLayoutData(data);
			return group;
		}
	}
	
	private void addOthersSection(Composite composite) {
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.analyseAnnotationsWhileTyping"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, INDENT);
	}

	private void addStringsSection(Composite composite) {
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		Button master, slave;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.wrapStrings"); //$NON-NLS-1$
		master= addCheckBox(composite, label, PreferenceConstants.EDITOR_WRAP_STRINGS, INDENT);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.escapeStrings"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_ESCAPE_STRINGS, INDENT);
		createDependency(master, slave);
	}

	private void addPasteSection(Composite composite) {
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.smartPaste"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_PASTE, INDENT);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.importsOnPaste"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, INDENT);
	}

	private void addTabSection(Composite composite) {
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartTab"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_TAB, INDENT);
		
		createMessage(composite);
	}

	private void addAutopositionSection(Composite composite) {
		
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartSemicolon"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_SEMICOLON, INDENT);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartOpeningBrace"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_OPENING_BRACE, INDENT);
	}

	private void addAutoclosingSection(Composite composite) {
		
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		composite.setLayout(layout);

		String label;
		Button master, slave;

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeStrings"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_STRINGS, INDENT);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeBrackets"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_BRACKETS, INDENT);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeBraces"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_BRACES, INDENT);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeJavaDocs"); //$NON-NLS-1$
		master= addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_JAVADOCS, INDENT);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.addJavaDocTags"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, INDENT);
		createDependency(master, slave);
	}
	
	private void createMessage(final Composite composite) {
		// TODO create a link with an argument, so the formatter preference page can open the 
		// current profile automatically.
		String linkTooltip= PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.message.tooltip"); //$NON-NLS-1$
		String text= PreferencesMessages.getFormattedString("SmartTypingConfigurationBlock.tabs.message.text", new String[] {Integer.toString(getIndentSize()), getIndentChar()}); //$NON-NLS-1$
		
		final Link link= new Link(composite, SWT.NONE);
		link.setText(text);
		link.setToolTipText(linkTooltip);
		GridData gd= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.widthHint= 300; // don't get wider initially
		link.setLayoutData(gd);
		link.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), "org.eclipse.jdt.ui.preferences.CodeFormatterPreferencePage", null, null); //$NON-NLS-1$
			}
		});
		
		final IPreferenceStore combinedStore= JavaPlugin.getDefault().getCombinedPreferenceStore();
		final IPropertyChangeListener propertyChangeListener= new IPropertyChangeListener() {
			private boolean fHasRun= false;
			public void propertyChange(PropertyChangeEvent event) {
				if (fHasRun)
					return;
				if (composite.isDisposed())
					return;
				String property= event.getProperty();
				if (DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR.equals(property)
						|| DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE.equals(property)) {
					fHasRun= true;
					link.dispose();
					createMessage(composite);
					composite.redraw();
					composite.layout();
				}
			}
		};
		combinedStore.addPropertyChangeListener(propertyChangeListener);
		link.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(org.eclipse.swt.events.DisposeEvent e) {
					combinedStore.removePropertyChangeListener(propertyChangeListener);
				}
		});
	}
	
	private String getIndentChar() {
		boolean useSpace= JavaCore.SPACE.equals(JavaPlugin.getDefault().getCombinedPreferenceStore().getString(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR));
		if (useSpace)
			return PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.message.spaces"); //$NON-NLS-1$
		else
			return PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.message.tabs"); //$NON-NLS-1$
	}

	private int getIndentSize() {
		return JavaPlugin.getDefault().getCombinedPreferenceStore().getInt(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE);
	}


}
