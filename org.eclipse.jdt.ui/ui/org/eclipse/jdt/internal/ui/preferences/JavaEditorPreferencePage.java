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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.ui.internal.dialogs.WorkbenchPreferenceDialog;
import org.eclipse.ui.internal.editors.text.CHyperLink;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

/**
 * The page for setting the editor options.
 */
public class JavaEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private static final String DELIMITER= PreferencesMessages.getString("JavaEditorPreferencePage.navigation.delimiter"); //$NON-NLS-1$

	private OverlayPreferenceStore fOverlayStore;
	
	private FoldingConfigurationBlock fFoldingConfigurationBlock;
	private MarkOccurrencesConfigurationBlock fOccurrencesBlock;
	private JavaEditorAppearanceConfigurationBlock fAppearanceBlock;
	
	private Map fColorButtons= new HashMap();
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	private Map fTextFields= new HashMap();
	private ModifyListener fTextFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			Text text= (Text) e.widget;
			fOverlayStore.setValue((String) fTextFields.get(text), text.getText());
		}
	};

	private ArrayList fNumberFields= new ArrayList();
	private ModifyListener fNumberFieldListener= new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			numberFieldChanged((Text) e.widget);
		}
	};
	
	private Text fBrowserLikeLinksKeyModifierText;
	private Button fBrowserLikeLinksCheckBox;
	private StatusInfo fBrowserLikeLinksKeyModifierStatus;
	
	/**
	 * Tells whether the fields are initialized.
	 * @since 3.0
	 */
	private boolean fFieldsInitialized= false;
	
	/**
	 * List of master/slave listeners when there's a dependency.
	 * 
	 * @see #createDependency(Button, String, Control)
	 * @since 3.0
	 */
	private ArrayList fMasterSlaveListeners= new ArrayList();

	
	/**
	 * Creates a new preference page.
	 */
	public JavaEditorPreferencePage() {
		setDescription(PreferencesMessages.getString("JavaEditorPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());

		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), createOverlayStoreKeys());
	}
	
	
	protected Label createDescriptionLabel(Composite parent) {
		return null; // no description since we add a hyperlinked text
	}
	
	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList overlayKeys= new ArrayList();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SPACES_FOR_TABS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_PASTE));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_STRINGS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACKETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACES));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_JAVADOCS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WRAP_STRINGS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ESCAPE_STRINGS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_HOME_END));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_SEMICOLON));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_TAB));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_OPENING_BRACE));
		
		OverlayPreferenceStore.OverlayKey[] keys= new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init()
	 */	
	public void init(IWorkbench workbench) {
	}

	/*
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE);
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
	

	
	private Composite createTypingPage2(Composite parent) {
		final ScrolledPageContent content = new ScrolledPageContent(parent);
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
		
		Composite body= content.getBody();
		body.setLayout(new GridLayout(nColumns, false));
		
		String label;
		ExpandableComposite excomposite;
		Composite composite;
		GridLayout layout;
		Control client;
		
		// appearance
		label= "Appearance"; //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= fAppearanceBlock.createControl(excomposite);
		excomposite.setClient(client);
		
		// misc
		label= "Miscellaneous"; //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		composite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(composite);
		
		layout= new GridLayout(nColumns, false);
		composite.setLayout(layout);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.analyseAnnotationsWhileTyping"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.overwriteMode"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE, 1);

		// smart typing
		label= "Smart Typing"; //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		composite= new Composite(excomposite, SWT.NONE);
		excomposite.setClient(composite);
		
		layout= new GridLayout(nColumns, false);
		composite.setLayout(layout);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.wrapStrings"); //$NON-NLS-1$
		Button master= addCheckBox(composite, label, PreferenceConstants.EDITOR_WRAP_STRINGS, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.escapeStrings"); //$NON-NLS-1$
		Button slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_ESCAPE_STRINGS, 1);
		createDependency(master, PreferenceConstants.EDITOR_WRAP_STRINGS, slave);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.smartPaste"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_PASTE, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.importsOnPaste"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.insertSpaceForTabs"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SPACES_FOR_TABS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeStrings"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_STRINGS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeBrackets"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_BRACKETS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeBraces"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_BRACES, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeJavaDocs"); //$NON-NLS-1$
		master= addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_JAVADOCS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.addJavaDocTags"); //$NON-NLS-1$
		slave= addCheckBox(composite, label, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, 1);
		createDependency(master, PreferenceConstants.EDITOR_CLOSE_JAVADOCS, slave);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartSemicolon"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_SEMICOLON, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartOpeningBrace"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_OPENING_BRACE, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartTab"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_TAB, 1);
		
		// mark occurrences
		label= "Mark Occurrences";
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		
		client= fOccurrencesBlock.createControl(excomposite);
		excomposite.setClient(client);

		// navigation
		label= PreferencesMessages.getString("JavaEditorPreferencePage.navigationTab.title"); //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		client= createNavigationPage(excomposite);
		excomposite.setClient(client);
		
		// folding
		label= PreferencesMessages.getString("JavaEditorPreferencePage.folding.title");  //$NON-NLS-1$
		excomposite= createManagedStyleSection(body, label, nColumns);
		mgr.manage(excomposite);
		client= fFoldingConfigurationBlock.createControl(excomposite);
		excomposite.setClient(client);
		

		return content;
	}

	private void addFiller(Composite composite) {
		Label filler= new Label(composite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		filler.setLayoutData(gd);
	}
	
	private static void indent(Control control) {
		GridData gridData= new GridData();
		gridData.horizontalIndent= 20;
		control.setLayoutData(gridData);		
	}
	
	private void createDependency(final Button master, String masterKey, final Control slave) {
		indent(slave);
		boolean masterState= fOverlayStore.getBoolean(masterKey);
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

	private Control createNavigationPage(Composite parent) {
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		composite.setLayout(layout);
		
		String label= PreferencesMessages.getString("JavaEditorPreferencePage.smartHomeEnd"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_HOME_END, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.subWordNavigation"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION, 1);

		addFiller(composite);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.navigation.browserLikeLinks"); //$NON-NLS-1$
		fBrowserLikeLinksCheckBox= addCheckBox(composite, label, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS, 0);
		fBrowserLikeLinksCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean state= fBrowserLikeLinksCheckBox.getSelection();
				fBrowserLikeLinksKeyModifierText.setEnabled(state);
				handleBrowserLikeLinksKeyModifierModified();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		// Text field for modifier string
		label= PreferencesMessages.getString("JavaEditorPreferencePage.navigation.browserLikeLinksKeyModifier"); //$NON-NLS-1$
		fBrowserLikeLinksKeyModifierText= addTextField(composite, label, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER, 20, 0, false);
		fBrowserLikeLinksKeyModifierText.setTextLimit(Text.LIMIT);
		
		if (computeStateMask(fOverlayStore.getString(PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER)) == -1) {
			// Fix possible illegal modifier string
			int stateMask= fOverlayStore.getInt(PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK);
			if (stateMask == -1)
				fBrowserLikeLinksKeyModifierText.setText(""); //$NON-NLS-1$
			else
				fBrowserLikeLinksKeyModifierText.setText(EditorUtility.getModifierString(stateMask));
		}
		
		fBrowserLikeLinksKeyModifierText.addKeyListener(new KeyListener() {
			private boolean isModifierCandidate;
			public void keyPressed(KeyEvent e) {
				isModifierCandidate= e.keyCode > 0 && e.character == 0 && e.stateMask == 0;
			}
		
			public void keyReleased(KeyEvent e) {
				if (isModifierCandidate && e.stateMask > 0 && e.stateMask == e.stateMask && e.character == 0) {// && e.time -time < 1000) {
					String modifierString= fBrowserLikeLinksKeyModifierText.getText();
					Point selection= fBrowserLikeLinksKeyModifierText.getSelection();
					int i= selection.x - 1;
					while (i > -1 && Character.isWhitespace(modifierString.charAt(i))) {
						i--;
					}
					boolean needsPrefixDelimiter= i > -1 && !String.valueOf(modifierString.charAt(i)).equals(DELIMITER);

					i= selection.y;
					while (i < modifierString.length() && Character.isWhitespace(modifierString.charAt(i))) {
						i++;
					}
					boolean needsPostfixDelimiter= i < modifierString.length() && !String.valueOf(modifierString.charAt(i)).equals(DELIMITER);

					String insertString;

					if (needsPrefixDelimiter && needsPostfixDelimiter)
						insertString= PreferencesMessages.getFormattedString("JavaEditorPreferencePage.navigation.insertDelimiterAndModifierAndDelimiter", new String[] {Action.findModifierString(e.stateMask)}); //$NON-NLS-1$
					else if (needsPrefixDelimiter)
						insertString= PreferencesMessages.getFormattedString("JavaEditorPreferencePage.navigation.insertDelimiterAndModifier", new String[] {Action.findModifierString(e.stateMask)}); //$NON-NLS-1$
					else if (needsPostfixDelimiter)
						insertString= PreferencesMessages.getFormattedString("JavaEditorPreferencePage.navigation.insertModifierAndDelimiter", new String[] {Action.findModifierString(e.stateMask)}); //$NON-NLS-1$
					else
						insertString= Action.findModifierString(e.stateMask);

					fBrowserLikeLinksKeyModifierText.insert(insertString);
				}
			}
		});

		fBrowserLikeLinksKeyModifierText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				handleBrowserLikeLinksKeyModifierModified();
			}
		});

		return composite;
	}

	private void handleBrowserLikeLinksKeyModifierModified() {
		String modifiers= fBrowserLikeLinksKeyModifierText.getText();
		int stateMask= computeStateMask(modifiers);

		if (fBrowserLikeLinksCheckBox.getSelection() && (stateMask == -1 || (stateMask & SWT.SHIFT) != 0)) {
			if (stateMask == -1)
				fBrowserLikeLinksKeyModifierStatus= new StatusInfo(IStatus.ERROR, PreferencesMessages.getFormattedString("JavaEditorPreferencePage.navigation.modifierIsNotValid", modifiers)); //$NON-NLS-1$
			else
				fBrowserLikeLinksKeyModifierStatus= new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("JavaEditorPreferencePage.navigation.shiftIsDisabled")); //$NON-NLS-1$
			setValid(false);
			StatusUtil.applyToStatusLine(this, fBrowserLikeLinksKeyModifierStatus);
		} else {
			fBrowserLikeLinksKeyModifierStatus= new StatusInfo();
			updateStatus(fBrowserLikeLinksKeyModifierStatus);
		}
	}
	
	private IStatus getBrowserLikeLinksKeyModifierStatus() {
		if (fBrowserLikeLinksKeyModifierStatus == null)
		fBrowserLikeLinksKeyModifierStatus= new StatusInfo();
		return fBrowserLikeLinksKeyModifierStatus;
	}

	/**
	 * Computes the state mask for the given modifier string.
	 * 
	 * @param modifiers	the string with the modifiers, separated by '+', '-', ';', ',' or '.'
	 * @return the state mask or -1 if the input is invalid
	 */
	private int computeStateMask(String modifiers) {
		if (modifiers == null)
			return -1;
		
		if (modifiers.length() == 0)
			return SWT.NONE;

		int stateMask= 0;
		StringTokenizer modifierTokenizer= new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
		while (modifierTokenizer.hasMoreTokens()) {
			int modifier= EditorUtility.findLocalizedModifier(modifierTokenizer.nextToken());
			if (modifier == 0 || (stateMask & modifier) == modifier)
				return -1;
			stateMask= stateMask | modifier;
		}
		return stateMask;
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		
		fFoldingConfigurationBlock= new FoldingConfigurationBlock(fOverlayStore);
		fOccurrencesBlock= new MarkOccurrencesConfigurationBlock(fOverlayStore);
		fAppearanceBlock= new JavaEditorAppearanceConfigurationBlock(this, fOverlayStore);
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		Composite contents= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		contents.setLayout(layout);
		
		Control description= createLinkText(contents, new Object[] {
				"Java editor preferences. Note that some settings are configured on the ", 
				new String[] {"general text editor preference page", "org.eclipse.ui.preferencePages.GeneralTextEditor", "Go to the text editor preferences" },
				"."});
		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint= 150; // only expand further if anyone else requires it
		description.setLayoutData(gridData);
		
		createHBar(contents);

		Composite expandableSection= createTypingPage2(contents);
		expandableSection.setLayoutData(new GridData(GridData.FILL_BOTH));

		initialize();
		
		Dialog.applyDialogFont(contents);
		return contents;
	}
	
	private Control createLinkText(Composite contents, Object[] tokens) {
		Composite description= new Composite(contents, SWT.NONE);
		RowLayout rowLayout= new RowLayout(SWT.HORIZONTAL);
		rowLayout.justify= false;
		rowLayout.fill= true;
		rowLayout.marginBottom= 0;
		rowLayout.marginHeight= 0;
		rowLayout.marginLeft= 0;
		rowLayout.marginRight= 0;
		rowLayout.marginTop= 0;
		rowLayout.marginWidth= 0;
		rowLayout.spacing= 0;
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
				label.setText(token + " ");
			}
		}
		
		return description;
	}


	private void createHBar(Composite contents) {
		GridLayout layout;
		Composite separator= new Composite(contents, SWT.NONE);
		layout= new GridLayout(1, false);
		layout.marginWidth= 50;
		layout.marginHeight= 5;
		separator.setLayout(layout);
		Label bar= new Label(separator, SWT.SEPARATOR | SWT.HORIZONTAL);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}


	private void initialize() {
		
		initializeFields();
		
		fFoldingConfigurationBlock.initialize();
	}
	
	private void initializeFields() {
		fOccurrencesBlock.initialize();
		fAppearanceBlock.initialize();

		Iterator e= fColorButtons.keySet().iterator();
		while (e.hasNext()) {
			ColorEditor c= (ColorEditor) e.next();
			String key= (String) fColorButtons.get(c);
			RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
			c.setColorValue(rgb);
		}
		
		e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
		
		e= fTextFields.keySet().iterator();
		while (e.hasNext()) {
			Text t= (Text) e.next();
			String key= (String) fTextFields.get(t);
			t.setText(fOverlayStore.getString(key));
		}
		
		fBrowserLikeLinksKeyModifierText.setEnabled(fBrowserLikeLinksCheckBox.getSelection());

        fFieldsInitialized= true;
        updateStatus(validatePositiveNumber("0")); //$NON-NLS-1$
        
        // Update slaves
        Iterator iter= fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener= (SelectionListener)iter.next();
            listener.widgetSelected(null);
        }
	}
	
	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fOccurrencesBlock.performOk();
		fFoldingConfigurationBlock.performOk();
		fAppearanceBlock.performOk();
		fOverlayStore.setValue(PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK, computeStateMask(fBrowserLikeLinksKeyModifierText.getText()));
		fOverlayStore.propagate();
		JavaPlugin.getDefault().savePluginPreferences();
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fOverlayStore.loadDefaults();

		fOccurrencesBlock.performDefaults();
		fFoldingConfigurationBlock.performDefaults();
		fAppearanceBlock.performDefaults();

		initializeFields();

		super.performDefaults();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		fOccurrencesBlock.dispose();
		fAppearanceBlock.dispose();
		fFoldingConfigurationBlock.dispose();
		
		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		super.dispose();
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
	
	private Text addTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		return getTextControl(addLabelledTextField(composite, label, key, textLimit, indentation, isNumber));
	}
	
	private static Text getTextControl(Control[] labelledTextField){
		return (Text)labelledTextField[1];
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
	 * @return the array containing the label and text controls
	 */
	private Control[] addLabelledTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		Text textControl= new Text(composite, SWT.BORDER | SWT.SINGLE);		
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint= convertWidthInCharsToPixels(textLimit + 1);
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
	
	private void numberFieldChanged(Text textControl) {
		String number= textControl.getText();
		IStatus status= validatePositiveNumber(number);
		if (!status.matches(IStatus.ERROR))
			fOverlayStore.setValue((String) fTextFields.get(textControl), number);
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
	
	void updateStatus(IStatus status) {
		if (!fFieldsInitialized)
			return;
		
		if (!status.matches(IStatus.ERROR)) {
			for (int i= 0; i < fNumberFields.size(); i++) {
				Text text= (Text) fNumberFields.get(i);
				IStatus s= validatePositiveNumber(text.getText());
				status= StatusUtil.getMoreSevere(s, status);
			}
		}	
		status= StatusUtil.getMoreSevere(getBrowserLikeLinksKeyModifierStatus(), status);
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}
}
