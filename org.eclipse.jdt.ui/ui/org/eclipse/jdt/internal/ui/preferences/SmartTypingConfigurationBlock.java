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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.Assert;

import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;
import org.eclipse.ui.internal.editors.text.CHyperLink;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/**
 * Configures Java Editor typing preferences.
 * 
 * @since 3.1
 */
class SmartTypingConfigurationBlock implements IPreferenceConfigurationBlock {

	private static final int INDENT= 0;
	private OverlayPreferenceStore fStore;
	
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	/**
	 * List of master/slave listeners when there's a dependency.
	 * 
	 * @see #createDependency(Button, String, Control)
	 * @since 3.0
	 */
	private ArrayList fMasterSlaveListeners= new ArrayList();
	
	private StatusInfo fStatus;

	public SmartTypingConfigurationBlock(OverlayPreferenceStore store) {
		Assert.isNotNull(store);
		fStore= store;
		
		fStore.addKeys(createOverlayStoreKeys());
	}
	
	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList overlayKeys= new ArrayList();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_PASTE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_STRINGS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACKETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACES));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_JAVADOCS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WRAP_STRINGS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ESCAPE_STRINGS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_SEMICOLON));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_TAB));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_OPENING_BRACE));
		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}	

	protected ScrolledPageContent getParentScrolledComposite(Control control) {
		Control parent= control.getParent();
		while (!(parent instanceof ScrolledPageContent) && parent != null) {
			parent= parent.getParent();
		}
		if (parent instanceof ScrolledPageContent) {
			return (ScrolledPageContent) parent;
		}
		return null;
	}

	protected ExpandableComposite getParentExpandableComposite(Control control) {
		Control parent= control.getParent();
		while (!(parent instanceof ExpandableComposite) && parent != null) {
			parent= parent.getParent();
		}
		if (parent instanceof ExpandableComposite) {
			return (ExpandableComposite) parent;
		}
		return null;
	}

	protected ExpandableComposite createManagedStyleSection(Composite parent, String label, int nColumns) {
		final ExpandableComposite excomposite= new ExpandableComposite(parent, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		excomposite.setText(label);
		excomposite.setExpanded(false);
		excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
		
		updateSectionStyle(excomposite);
		return excomposite;
	}
	
	protected void updateSectionStyle(ExpandableComposite excomposite) {
//		if (excomposite.isExpanded()) {
			excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
//		} else {
//			excomposite.setFont(JFaceResources.getFontRegistry().get(JFaceResources.DIALOG_FONT));
//		}
	}
	
	private void makeScrollableCompositeAware(Control control) {
		ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(control);
		if (parentScrolledComposite != null) {
			parentScrolledComposite.adaptChild(control);
		}
	}
	
	/**
	 * Creates page for mark occurrences preferences.
	 * 
	 * @param parent the parent composite
	 * @return the control for the preference page
	 */
	public Control createControl(Composite parent) {

		final Composite content;
		boolean isNested= getParentScrolledComposite(parent) != null;
		if (isNested)
			content = new Composite(parent, SWT.NONE);
		else
			content = new ScrolledPageContent(parent);
			
			
		class StyleSectionManager {
			private Set fSections= new HashSet();
			private boolean fIsBeingManaged= false;
			private ExpansionAdapter fListener= new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {
					ExpandableComposite source= (ExpandableComposite) e.getSource();
					updateSectionStyle(source);
					if (fIsBeingManaged)
						return;
					if (e.getState()) {
						try {
							fIsBeingManaged= true;
							for (Iterator iter= fSections.iterator(); iter.hasNext();) {
								ExpandableComposite composite= (ExpandableComposite) iter.next();
								if (composite != source)
									composite.setExpanded(false);
							}
						} finally {
							fIsBeingManaged= false;
						}
					}
					ExpandableComposite exComp= getParentExpandableComposite(source);
					if (exComp != null)
						exComp.layout(true, true);
					ScrolledPageContent parentScrolledComposite= getParentScrolledComposite(source);
					if (parentScrolledComposite != null) {
						parentScrolledComposite.reflow(true);
					}
				}
			};
			public void manage(ExpandableComposite section) {
				if (section == null)
					throw new NullPointerException();
				if (fSections.add(section))
					section.addExpansionListener(fListener);
			}
		}
		StyleSectionManager mgr= new StyleSectionManager();
		int nColumns= 2;
		
		Composite body;
		if (isNested)
			body= content;
		else
			body= ((ScrolledPageContent) content).getBody();
		body.setLayout(new GridLayout(nColumns, false));
		
		String label;
		ExpandableComposite excomposite;
		Control client;

		label= PreferencesMessages.getString("SmartTypingConfigurationBlock.autoclose.title"); //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= createAutoclosingSection(excomposite);
		excomposite.setClient(client);
		
		label= PreferencesMessages.getString("SmartTypingConfigurationBlock.automove.title"); //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= createAutopositionSection(excomposite);
		excomposite.setClient(client);
		
		label= PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.title"); //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= createTabSection(excomposite);
		excomposite.setClient(client);

		label= PreferencesMessages.getString("SmartTypingConfigurationBlock.pasting.title"); //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= createPasteSection(excomposite);
		excomposite.setClient(client);
		
		label= PreferencesMessages.getString("SmartTypingConfigurationBlock.strings.title"); //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= createStringsSection(excomposite);
		excomposite.setClient(client);
		
		label= PreferencesMessages.getString("SmartTypingConfigurationBlock.other.title"); //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= createOthersSection(excomposite);
		excomposite.setClient(client);
		
		return content;
	}
	
	private Control createOthersSection(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.analyseAnnotationsWhileTyping"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, INDENT);
		
		return composite;
	}

	private Control createStringsSection(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		Button master, slave;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.wrapStrings"); //$NON-NLS-1$
		master= addCheckBox(composite, label, PreferenceConstants.EDITOR_WRAP_STRINGS, INDENT);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.escapeStrings"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_ESCAPE_STRINGS, INDENT);
		createDependency(master, PreferenceConstants.EDITOR_WRAP_STRINGS, slave);
		
		return composite;
	}

	private Control createPasteSection(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.smartPaste"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_PASTE, INDENT);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.importsOnPaste"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, INDENT);
		
		return composite;
	}

	private Control createTabSection(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartTab"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_TAB, INDENT);
		
		createMessage(composite);
		return composite;
	}

	private void createMessage(final Composite composite) {
		// TODO create a link with an argument, so the formatter preference page can open the 
		// current profile automatically.
		String before= PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.message.before"); //$NON-NLS-1$
		String linkText= PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.message.linktext"); //$NON-NLS-1$
		String linkTooltip= PreferencesMessages.getString("SmartTypingConfigurationBlock.tabs.message.linktooltip"); //$NON-NLS-1$
		String after= PreferencesMessages.getFormattedString("SmartTypingConfigurationBlock.tabs.message.after", new String[] {Integer.toString(getIndentSize()), getIndentChar()}); //$NON-NLS-1$
		
		final Control link= createLinkText(composite, new Object[] { before,
				new String[] { linkText, "org.eclipse.jdt.ui.preferences.CodeFormatterPreferencePage", linkTooltip }, //$NON-NLS-1$
				after, });
		GridData gd= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gd.widthHint= 300; // don't get wider initially
		link.setLayoutData(gd);
		
		final IPreferenceStore combinedStore= JavaPlugin.getDefault().getCombinedPreferenceStore();
		combinedStore.addPropertyChangeListener(new IPropertyChangeListener() {
			private boolean fHasRun= false;
			public void propertyChange(PropertyChangeEvent event) {
				if (fHasRun)
					return;
				if (composite.isDisposed())
					return;
				String property= event.getProperty();
				if (DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR.equals(property)
						|| DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE.equals(property)) {
					combinedStore.removePropertyChangeListener(this);
					fHasRun= true;
					link.dispose();
					createMessage(composite);
					composite.redraw();
					composite.layout();
				}
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

	private Control createLinkText(Composite contents, Object[] tokens) {
		Composite description= new Composite(contents, SWT.NONE);
		RowLayout rowLayout= new RowLayout(SWT.HORIZONTAL);
		rowLayout.justify= false;
		rowLayout.fill= false;
		rowLayout.marginBottom= 0;
		rowLayout.marginHeight= 0;
		rowLayout.marginLeft= 0;
		rowLayout.marginRight= 0;
		rowLayout.marginTop= 0;
		rowLayout.marginWidth= 0;
		rowLayout.spacing= 0;
		rowLayout.pack= true;
		rowLayout.wrap= true;
		description.setLayout(rowLayout);
		
		for (int i= 0; i < tokens.length; i++) {
			String text;
			if (tokens[i] instanceof String[]) {
				String[] strings= (String[]) tokens[i];
				text= strings[0];
				final String target= strings[1];
				CHyperLink link= new CHyperLink(description, SWT.NONE);
				link.setText(text);
				link.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
								WorkbenchPreferenceDialog.createDialogOn(target);
						}
				});
				if (strings.length > 2)
					link.setToolTipText(strings[2]);
				continue;
			}
			
			text= (String) tokens[i];
			StringTokenizer tokenizer= new StringTokenizer(text);
			while (tokenizer.hasMoreTokens()) {
				Label label= new Label(description, SWT.NONE);
				String token= tokenizer.nextToken();
				label.setText(token + " "); //$NON-NLS-1$
			}
		}
		
		return description;
	}

	private Control createAutopositionSection(Composite parent) {
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);

		String label;
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartSemicolon"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_SEMICOLON, INDENT);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartOpeningBrace"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_OPENING_BRACE, INDENT);
		
		return composite;
	}

	private Control createAutoclosingSection(Composite parent) {
		
		Composite composite= new Composite(parent, SWT.NONE);
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
		createDependency(master, PreferenceConstants.EDITOR_CLOSE_JAVADOCS, slave);
		
		return composite;
	}

	private Button addCheckBox(Composite parent, String label, String key, int indentation) {		
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);
		makeScrollableCompositeAware(checkBox);

		fCheckBoxes.put(checkBox, key);
		
		return checkBox;
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
		((GridData) control.getLayoutData()).horizontalIndent+= INDENT;
	}

	public void initialize() {
		initializeFields();
	}

	void initializeFields() {
		
		Iterator iter= fCheckBoxes.keySet().iterator();
		while (iter.hasNext()) {
			Button b= (Button) iter.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fStore.getBoolean(key));
		}
		
        // Update slaves
        iter= fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener= (SelectionListener)iter.next();
            listener.widgetSelected(null);
        }
        
	}

	public void performOk() {
	}

	public void performDefaults() {
		restoreFromPreferences();
		initializeFields();
	}

	private void restoreFromPreferences() {

	}

	IStatus getStatus() {
		if (fStatus == null)
			fStatus= new StatusInfo();
		return fStatus;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#dispose()
	 * @since 3.0
	 */
	public void dispose() {
	}
}
