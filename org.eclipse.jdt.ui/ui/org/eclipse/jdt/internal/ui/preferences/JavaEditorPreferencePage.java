/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.preferences;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.WorkbenchChainedTextFontFieldEditor;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;

/*
 * The page for setting the editor options.
 */
public class JavaEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private static final String BOLD= PreferenceConstants.EDITOR_BOLD_SUFFIX;

	public final OverlayPreferenceStore.OverlayKey[] fKeys= new OverlayPreferenceStore.OverlayKey[] {
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOREGROUND_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BACKGROUND_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PreferenceConstants.EDITOR_TAB_WIDTH),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_JAVA_KEYWORD_BOLD),
				
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_STRING_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_STRING_BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_JAVA_DEFAULT_BOLD),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_JAVADOC_KEYWORD_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_JAVADOC_KEYWORD_BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_JAVADOC_TAG_BOLD),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_JAVADOC_TAG_BOLD),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_JAVADOC_LINKS_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_JAVADOC_LINKS_BOLD),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_JAVADOC_DEFAULT_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_JAVADOC_DEFAULT_BOLD),
				
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_MATCHING_BRACKETS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_CURRENT_LINE_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CURRENT_LINE),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_PRINT_MARGIN),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FIND_SCOPE_COLOR),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LINKED_POSITION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LINK_COLOR),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_PROBLEM_INDICATION),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WARNING_INDICATION),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_TASK_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_TASK_INDICATION),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BOOKMARK_INDICATION),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_UNKNOWN_INDICATION),

		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CORRECTION_INDICATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_OVERVIEW_RULER),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_LINE_NUMBER_RULER),
				
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SPACES_FOR_TABS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_AUTOACTIVATION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_AUTOINSERT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND),		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_ORDER_PROPOSALS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_CASE_SENSITIVITY),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_ADDIMPORT),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_INSERT_COMPLETION),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_PASTE),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_STRINGS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACKETS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_JAVADOCS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WRAP_STRINGS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FORMAT_JAVADOCS),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_HOME_END),
		
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_DEFAULT_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_NONE_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_CTRL_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_SHIFT_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_CTRL_SHIFT_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_CTRL_ALT_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_CTRL_ALT_SHIFT_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_CTRL_SHIFT_HOVER),
		new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_ALT_SHIFT_HOVER),
	};
	
	private final String[][] fSyntaxColorListModel= new String[][] {
		{ JavaUIMessages.getString("JavaEditorPreferencePage.multiLineComment"), PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.singleLineComment"), PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.keywords"), PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.strings"), PreferenceConstants.EDITOR_STRING_COLOR }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.others"), PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocKeywords"), PreferenceConstants.EDITOR_JAVADOC_KEYWORD_COLOR }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocHtmlTags"), PreferenceConstants.EDITOR_JAVADOC_TAG_BOLD }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocLinks"), PreferenceConstants.EDITOR_JAVADOC_LINKS_COLOR }, //$NON-NLS-1$
		{ JavaUIMessages.getString("JavaEditorPreferencePage.javaDocOthers"), PreferenceConstants.EDITOR_JAVADOC_DEFAULT_COLOR } //$NON-NLS-1$
	};
	
	private final String[][] fAppearanceColorListModel= new String[][] {
		{JavaUIMessages.getString("JavaEditorPreferencePage.lineNumberForegroundColor"), PreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR}, //$NON-NLS-1$
		{JavaUIMessages.getString("JavaEditorPreferencePage.matchingBracketsHighlightColor2"), PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR}, //$NON-NLS-1$
		{JavaUIMessages.getString("JavaEditorPreferencePage.currentLineHighlighColor"), PreferenceConstants.EDITOR_CURRENT_LINE_COLOR}, //$NON-NLS-1$
		{JavaUIMessages.getString("JavaEditorPreferencePage.printMarginColor2"), PreferenceConstants.EDITOR_PRINT_MARGIN_COLOR}, //$NON-NLS-1$
		{JavaUIMessages.getString("JavaEditorPreferencePage.findScopeColor2"), PreferenceConstants.EDITOR_FIND_SCOPE_COLOR}, //$NON-NLS-1$
		{JavaUIMessages.getString("JavaEditorPreferencePage.linkedPositionColor2"), PreferenceConstants.EDITOR_LINKED_POSITION_COLOR}, //$NON-NLS-1$
		{JavaUIMessages.getString("JavaEditorPreferencePage.linkColor2"), PreferenceConstants.EDITOR_LINK_COLOR}, //$NON-NLS-1$
	};
	
	private final String[][] fProblemIndicationColorListModel= new String[][] {
		{"Errors", PreferenceConstants.EDITOR_PROBLEM_INDICATION_COLOR, PreferenceConstants.EDITOR_PROBLEM_INDICATION, PreferenceConstants.EDITOR_ERROR_INDICATION_IN_OVERVIEW_RULER },
		{"Warnings", PreferenceConstants.EDITOR_WARNING_INDICATION_COLOR, PreferenceConstants.EDITOR_WARNING_INDICATION, PreferenceConstants.EDITOR_WARNING_INDICATION_IN_OVERVIEW_RULER },
		{"Tasks", PreferenceConstants.EDITOR_TASK_INDICATION_COLOR, PreferenceConstants.EDITOR_TASK_INDICATION, PreferenceConstants.EDITOR_TASK_INDICATION_IN_OVERVIEW_RULER },
		{"Search Results", PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_COLOR, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION, PreferenceConstants.EDITOR_SEARCH_RESULT_INDICATION_IN_OVERVIEW_RULER },
		{"Bookmarks", PreferenceConstants.EDITOR_BOOKMARK_INDICATION_COLOR, PreferenceConstants.EDITOR_BOOKMARK_INDICATION, PreferenceConstants.EDITOR_BOOKMARK_INDICATION_IN_OVERVIEW_RULER },
		{"Others", PreferenceConstants.EDITOR_UNKNOWN_INDICATION_COLOR, PreferenceConstants.EDITOR_UNKNOWN_INDICATION, PreferenceConstants.EDITOR_UNKNOWN_INDICATION_IN_OVERVIEW_RULER }
	};
	
	private OverlayPreferenceStore fOverlayStore;
	private JavaTextTools fJavaTextTools;
	private JavaEditorHoverConfigurationBlock fJavaEditorHoverConfigurationBlock;
	
	private Map fColorButtons= new HashMap();
	private SelectionListener fColorButtonListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			ColorEditor editor= (ColorEditor) e.widget.getData();
			PreferenceConverter.setValue(fOverlayStore, (String) fColorButtons.get(editor), editor.getColorValue());
		}
	};
	
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
	
	private WorkbenchChainedTextFontFieldEditor fFontEditor;
	private List fSyntaxColorList;
	private List fAppearanceColorList;
	private List fProblemIndicationList;
	private ColorEditor fSyntaxForegroundColorEditor;
	private ColorEditor fAppearanceForegroundColorEditor;
	private ColorEditor fProblemIndicationForegroundColorEditor;
	private ColorEditor fBackgroundColorEditor;
	private Button fBackgroundDefaultRadioButton;
	private Button fBackgroundCustomRadioButton;
	private Button fBackgroundColorButton;
	private Button fBoldCheckBox;
	private Button fAddJavaDocTagsButton;
	private Button fGuessMethodArgumentsButton;
	private SourceViewer fPreviewViewer;
	private Color fBackgroundColor;
    private Control fAutoInsertDelayText;
    private Control fAutoInsertJavaTriggerText;
    private Control fAutoInsertJavaDocTriggerText;
	private Button fShowInTextCheckBox;
	private Button fShowInOverviewRulerCheckBox;
	
	public JavaEditorPreferencePage() {
		setDescription(JavaUIMessages.getString("JavaEditorPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), fKeys);
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

	private void handleSyntaxColorListSelection() {	
		int i= fSyntaxColorList.getSelectionIndex();
		String key= fSyntaxColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fSyntaxForegroundColorEditor.setColorValue(rgb);		
		fBoldCheckBox.setSelection(fOverlayStore.getBoolean(key + BOLD));
	}

	private void handleAppearanceColorListSelection() {	
		int i= fAppearanceColorList.getSelectionIndex();
		String key= fAppearanceColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fAppearanceForegroundColorEditor.setColorValue(rgb);		
	}
	
	private void handleProblemIndicationColorListSelection() {
		int i= fProblemIndicationList.getSelectionIndex();
		
		String key= fProblemIndicationColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fProblemIndicationForegroundColorEditor.setColorValue(rgb);
		
		key= fProblemIndicationColorListModel[i][2];
		fShowInTextCheckBox.setSelection(fOverlayStore.getBoolean(key));
		
		key= fProblemIndicationColorListModel[i][3];
		fShowInOverviewRulerCheckBox.setSelection(fOverlayStore.getBoolean(key));				
	}
	
	private Control createSyntaxPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NULL);
		colorComposite.setLayout(new GridLayout());

		Group backgroundComposite= new Group(colorComposite, SWT.SHADOW_ETCHED_IN);
		backgroundComposite.setLayout(new RowLayout());
		backgroundComposite.setText(JavaUIMessages.getString("JavaEditorPreferencePage.backgroundColor"));//$NON-NLS-1$
	
		SelectionListener backgroundSelectionListener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				boolean custom= fBackgroundCustomRadioButton.getSelection();
				fBackgroundColorButton.setEnabled(custom);
				fOverlayStore.setValue(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR, !custom);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};

		fBackgroundDefaultRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundDefaultRadioButton.setText(JavaUIMessages.getString("JavaEditorPreferencePage.systemDefault")); //$NON-NLS-1$
		fBackgroundDefaultRadioButton.addSelectionListener(backgroundSelectionListener);

		fBackgroundCustomRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundCustomRadioButton.setText(JavaUIMessages.getString("JavaEditorPreferencePage.custom")); //$NON-NLS-1$
		fBackgroundCustomRadioButton.addSelectionListener(backgroundSelectionListener);

		fBackgroundColorEditor= new ColorEditor(backgroundComposite);
		fBackgroundColorButton= fBackgroundColorEditor.getButton();

		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.foreground")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);		

		fSyntaxColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fSyntaxColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fSyntaxForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fSyntaxForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);
		
		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
        fBoldCheckBox.setText(JavaUIMessages.getString("JavaEditorPreferencePage.bold")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
        gd.horizontalSpan= 2;
		fBoldCheckBox.setLayoutData(gd);
		
		label= new Label(colorComposite, SWT.LEFT);
		label.setText(JavaUIMessages.getString("JavaEditorPreferencePage.preview")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control previewer= createPreviewer(colorComposite);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(20);
		gd.heightHint= convertHeightInCharsToPixels(5);
		previewer.setLayoutData(gd);

		
		fSyntaxColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleSyntaxColorListSelection();
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fSyntaxColorList.getSelectionIndex();
				String key= fSyntaxColorListModel[i][1];
				
				PreferenceConverter.setValue(fOverlayStore, key, fSyntaxForegroundColorEditor.getColorValue());
			}
		});

		fBackgroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				PreferenceConverter.setValue(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR, fBackgroundColorEditor.getColorValue());					
			}
		});

		fBoldCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fSyntaxColorList.getSelectionIndex();
				String key= fSyntaxColorListModel[i][1];
				fOverlayStore.setValue(key + BOLD, fBoldCheckBox.getSelection());
			}
		});
				
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		
		fJavaTextTools= new JavaTextTools(fOverlayStore);
		
		fPreviewViewer= new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		fPreviewViewer.configure(new JavaSourceViewerConfiguration(fJavaTextTools, null));
		fPreviewViewer.getTextWidget().setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		fPreviewViewer.setEditable(false);
		
		initializeViewerColors(fPreviewViewer);
		
		String content= loadPreviewContentFromFile("ColorSettingPreviewCode.txt"); //$NON-NLS-1$
		IDocument document= new Document(content);
		IDocumentPartitioner partitioner= fJavaTextTools.createDocumentPartitioner();
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		
		fPreviewViewer.setDocument(document);
		
		fOverlayStore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String p= event.getProperty();
				if (p.equals(PreferenceConstants.EDITOR_BACKGROUND_COLOR) ||
					p.equals(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR))
				{
					initializeViewerColors(fPreviewViewer);
				}
				
				fPreviewViewer.invalidateTextPresentation();
			}
		});
		
		return fPreviewViewer.getControl();
	}
	
	/**
	 * Initializes the given viewer's colors.
	 * 
	 * @param viewer the viewer to be initialized
	 */
	private void initializeViewerColors(ISourceViewer viewer) {
		
		IPreferenceStore store= fOverlayStore;
		if (store != null) {
			
			StyledText styledText= viewer.getTextWidget();
						
			// ---------- background color ----------------------
			Color color= store.getBoolean(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR)
				? null
				: createColor(store, PreferenceConstants.EDITOR_BACKGROUND_COLOR, styledText.getDisplay());
			styledText.setBackground(color);
				
			if (fBackgroundColor != null)
				fBackgroundColor.dispose();
				
			fBackgroundColor= color;
		}
	}

	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 */
	private Color createColor(IPreferenceStore store, String key, Display display) {
	
		RGB rgb= null;		
		
		if (store.contains(key)) {
			
			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);
		
			if (rgb != null)
				return new Color(display, rgb);
		}
		
		return null;
	}	
	
	// sets enabled flag for a control and all its sub-tree
	private static void setEnabled(Control control, boolean enable) {
		control.setEnabled(enable);
		if (control instanceof Composite) {
			Composite composite= (Composite) control;
			Control[] children= composite.getChildren();
			for (int i= 0; i < children.length; i++)
				setEnabled(children[i], enable);
		}
	}

	private Control createAppearancePage(Composite parent) {

		Composite appearanceComposite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		appearanceComposite.setLayout(layout);

		String label= JavaUIMessages.getString("JavaEditorPreferencePage.textFont"); //$NON-NLS-1$
		addTextFontEditor(appearanceComposite, label, AbstractTextEditor.PREFERENCE_FONT);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.displayedTabWidth"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, PreferenceConstants.EDITOR_TAB_WIDTH, 3, 0, true);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.printMarginColumn"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, PreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, 3, 0, true);
				
		label= JavaUIMessages.getString("JavaEditorPreferencePage.synchronizeOnCursor"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.showOverviewRuler"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_OVERVIEW_RULER, 0);
				
		label= JavaUIMessages.getString("JavaEditorPreferencePage.showLineNumbers"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_LINE_NUMBER_RULER, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.highlightMatchingBrackets"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_MATCHING_BRACKETS, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.highlightCurrentLine"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_CURRENT_LINE, 0);
				
		label= JavaUIMessages.getString("JavaEditorPreferencePage.showPrintMargin"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_PRINT_MARGIN, 0);


		Label l= new Label(appearanceComposite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
		
		l= new Label(appearanceComposite, SWT.LEFT);
		l.setText(JavaUIMessages.getString("JavaEditorPreferencePage.appearanceOptions")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		l.setLayoutData(gd);

		Composite editorComposite= new Composite(appearanceComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		

		fAppearanceColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fAppearanceColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l= new Label(stylesComposite, SWT.LEFT);
		l.setText(JavaUIMessages.getString("JavaEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		l.setLayoutData(gd);

		fAppearanceForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fAppearanceForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		fAppearanceColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleAppearanceColorListSelection();
			}
		});
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fAppearanceColorList.getSelectionIndex();
				String key= fAppearanceColorListModel[i][1];
				
				PreferenceConverter.setValue(fOverlayStore, key, fAppearanceForegroundColorEditor.getColorValue());
			}
		});
		return appearanceComposite;
	}
	
	
	private Control createProblemIndicationPage(Composite parent) {
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		composite.setLayout(layout);
				
		String text= "Analyse &problems while typing";
		addCheckBox(composite, text, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, 0);
		
		text= JavaUIMessages.getString("JavaEditorPreferencePage.showQuickFixables"); //$NON-NLS-1$
		addCheckBox(composite, text, PreferenceConstants.EDITOR_CORRECTION_INDICATION, 0);
		
		Label label= new Label(composite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		label.setLayoutData(gd);
		
		label= new Label(composite, SWT.LEFT);
		label.setText("&Marker presentation options:");
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		label.setLayoutData(gd);

		Composite editorComposite= new Composite(composite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);		

		fProblemIndicationList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fProblemIndicationList.setLayoutData(gd);
						
		Composite optionsComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		optionsComposite.setLayout(layout);
		optionsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fShowInTextCheckBox= new Button(optionsComposite, SWT.CHECK);
        fShowInTextCheckBox.setText("Show in &text");
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
        gd.horizontalSpan= 2;
		fShowInTextCheckBox.setLayoutData(gd);
		
		fShowInOverviewRulerCheckBox= new Button(optionsComposite, SWT.CHECK);
        fShowInOverviewRulerCheckBox.setText("Show in overview &ruler");
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
        gd.horizontalSpan= 2;
		fShowInOverviewRulerCheckBox.setLayoutData(gd);
		
		label= new Label(optionsComposite, SWT.LEFT);
		label.setText("C&olor:");
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fProblemIndicationForegroundColorEditor= new ColorEditor(optionsComposite);
		Button foregroundColorButton= fProblemIndicationForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		fProblemIndicationList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				handleProblemIndicationColorListSelection();
			}
		});
		
		fShowInTextCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				int i= fProblemIndicationList.getSelectionIndex();
				String key= fProblemIndicationColorListModel[i][2];
				fOverlayStore.setValue(key, fShowInTextCheckBox.getSelection());
			}
		});
		
		fShowInOverviewRulerCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				int i= fProblemIndicationList.getSelectionIndex();
				String key= fProblemIndicationColorListModel[i][3];
				fOverlayStore.setValue(key, fShowInOverviewRulerCheckBox.getSelection());
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			
			public void widgetSelected(SelectionEvent e) {
				int i= fProblemIndicationList.getSelectionIndex();
				String key= fProblemIndicationColorListModel[i][1];
				PreferenceConverter.setValue(fOverlayStore, key, fProblemIndicationForegroundColorEditor.getColorValue());
			}
		});
		
		return composite;
	}

	private Control createBehaviourPage(Composite parent) {
		Composite composite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		composite.setLayout(layout);

		String label= JavaUIMessages.getString("JavaEditorPreferencePage.wrapStrings"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_WRAP_STRINGS, 1);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.smartHomeEnd"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_HOME_END, 1);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.smartPaste"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SMART_PASTE, 1);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.insertSpaceForTabs"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_SPACES_FOR_TABS, 1);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.closeStrings"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_STRINGS, 1);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.closeBrackets"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_BRACKETS, 1);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.closeJavaDocs"); //$NON-NLS-1$
		Button button= addCheckBox(composite, label, PreferenceConstants.EDITOR_CLOSE_JAVADOCS, 1);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.addJavaDocTags"); //$NON-NLS-1$
		fAddJavaDocTagsButton= addCheckBox(composite, label, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, 1);
		createDependency(button, fAddJavaDocTagsButton);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.formatJavaDocs"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_FORMAT_JAVADOCS, 1);
	
		return composite;
	}
	
	private static void indent(Control control) {
		GridData gridData= new GridData();
		gridData.horizontalIndent= 20;
		control.setLayoutData(gridData);		
	}
	
	private static void createDependency(final Button master, final Control slave) {
		indent(slave);
		master.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				slave.setEnabled(master.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {}
		});		
	}

	private Control createContentAssistPage(Composite parent) {

		Composite contentAssistComposite= new Composite(parent, SWT.NULL);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		contentAssistComposite.setLayout(layout);

		String label= JavaUIMessages.getString("JavaEditorPreferencePage.insertSingleProposalsAutomatically"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOINSERT, 0);		

		label= JavaUIMessages.getString("JavaEditorPreferencePage.showOnlyProposalsVisibleInTheInvocationContext"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.presentProposalsInAlphabeticalOrder"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_ORDER_PROPOSALS, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.automaticallyAddImportInsteadOfQualifiedName"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_ADDIMPORT, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.insertCompletion"); //$NON-NLS-1$
		addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_INSERT_COMPLETION, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.fillArgumentNamesOnMethodCompletion"); //$NON-NLS-1$
		Button button= addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.guessArgumentNamesOnMethodCompletion"); //$NON-NLS-1$
		fGuessMethodArgumentsButton= addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, 0);
		createDependency(button, fGuessMethodArgumentsButton);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.enableAutoActivation"); //$NON-NLS-1$
		final Button autoactivation= addCheckBox(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.autoActivationDelay"); //$NON-NLS-1$
		fAutoInsertDelayText= addTextField(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY, 4, 0, true);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJava"); //$NON-NLS-1$
		fAutoInsertJavaTriggerText= addTextField(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA, 4, 0, false);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.autoActivationTriggersForJavaDoc"); //$NON-NLS-1$
		fAutoInsertJavaDocTriggerText= addTextField(contentAssistComposite, label, PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVADOC, 4, 0, false);
								
		label= JavaUIMessages.getString("JavaEditorPreferencePage.backgroundForCompletionProposals"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, PreferenceConstants.CODEASSIST_PROPOSALS_BACKGROUND, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.foregroundForCompletionProposals"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, PreferenceConstants.CODEASSIST_PROPOSALS_FOREGROUND, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.backgroundForMethodParameters"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.foregroundForMethodParameters"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND, 0);

		label= JavaUIMessages.getString("JavaEditorPreferencePage.backgroundForCompletionReplacement"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND, 0);
		
		label= JavaUIMessages.getString("JavaEditorPreferencePage.foregroundForCompletionReplacement"); //$NON-NLS-1$
		addColorButton(contentAssistComposite, label, PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND, 0);
		
		autoactivation.addSelectionListener(new SelectionAdapter(){
            public void widgetSelected(SelectionEvent e) {
            	updateAutoactivationControls();
            }
		});		
		return contentAssistComposite;
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.general")); //$NON-NLS-1$
		item.setControl(createAppearancePage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.colors")); //$NON-NLS-1$
		item.setControl(createSyntaxPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.codeAssist")); //$NON-NLS-1$
		item.setControl(createContentAssistPage(folder));

		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.problemIndicationTab.title")); //$NON-NLS-1$
		item.setControl(createProblemIndicationPage(folder));

		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.behaviourTab.title")); //$NON-NLS-1$
		item.setControl(createBehaviourPage(folder));

		item= new TabItem(folder, SWT.NONE);
		item.setText(JavaUIMessages.getString("JavaEditorPreferencePage.hoverTab.title")); //$NON-NLS-1$
		fJavaEditorHoverConfigurationBlock= new JavaEditorHoverConfigurationBlock(fOverlayStore);
		item.setControl(fJavaEditorHoverConfigurationBlock.createControl(folder));
		
		initialize();
		
		return folder;
	}
	
	private void initialize() {
		
		fFontEditor.setPreferenceStore(getPreferenceStore());
		fFontEditor.setPreferencePage(this);
		fFontEditor.load();
		
		initializeFields();
		
		for (int i= 0; i < fSyntaxColorListModel.length; i++)
			fSyntaxColorList.add(fSyntaxColorListModel[i][0]);
			
		fSyntaxColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fSyntaxColorList != null && !fSyntaxColorList.isDisposed()) {
					fSyntaxColorList.select(0);
					handleSyntaxColorListSelection();
				}
			}
		});
		
		for (int i= 0; i < fAppearanceColorListModel.length; i++)
			fAppearanceColorList.add(fAppearanceColorListModel[i][0]);
			
		fAppearanceColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fAppearanceColorList != null && !fAppearanceColorList.isDisposed()) {
					fAppearanceColorList.select(0);
					handleAppearanceColorListSelection();
				}
			}
		});
		
		for (int i= 0; i < fProblemIndicationColorListModel.length; i++)
			fProblemIndicationList.add(fProblemIndicationColorListModel[i][0]);
			
		fProblemIndicationList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fProblemIndicationList != null && !fProblemIndicationList.isDisposed()) {
					fProblemIndicationList.select(0);
					handleProblemIndicationColorListSelection();
				}
			}
		});
	}
	
	private void initializeFields() {
		
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
		
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR);
		fBackgroundColorEditor.setColorValue(rgb);		
		
		boolean default_= fOverlayStore.getBoolean(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR);
		fBackgroundDefaultRadioButton.setSelection(default_);
		fBackgroundCustomRadioButton.setSelection(!default_);
		fBackgroundColorButton.setEnabled(!default_);

		boolean closeJavaDocs= fOverlayStore.getBoolean(PreferenceConstants.EDITOR_CLOSE_JAVADOCS);
		fAddJavaDocTagsButton.setEnabled(closeJavaDocs);

		boolean fillMethodArguments= fOverlayStore.getBoolean(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES);
		fGuessMethodArgumentsButton.setEnabled(fillMethodArguments);
		
        updateAutoactivationControls();
        
		fJavaEditorHoverConfigurationBlock.initializeFields();
	}
	
    private void updateAutoactivationControls() {
        boolean autoactivation= fOverlayStore.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION);
        fAutoInsertDelayText.setEnabled(autoactivation);
        fAutoInsertJavaTriggerText.setEnabled(autoactivation);
        fAutoInsertJavaDocTriggerText.setEnabled(autoactivation);
    }
	
	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fFontEditor.store();
		fJavaEditorHoverConfigurationBlock.performOk();
		fOverlayStore.propagate();
		JavaPlugin.getDefault().savePluginPreferences();
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		
		fFontEditor.loadDefault();
		
		fOverlayStore.loadDefaults();
		initializeFields();

		handleSyntaxColorListSelection();
		handleAppearanceColorListSelection();
		handleProblemIndicationColorListSelection();
		super.performDefaults();
		
		fPreviewViewer.invalidateTextPresentation();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
		if (fJavaTextTools != null) {
			fJavaTextTools= null;
		}
		
		fFontEditor.setPreferencePage(null);
		fFontEditor.setPreferenceStore(null);
		
		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		
		super.dispose();
	}
	
	private Control addColorButton(Composite composite, String label, String key, int indentation) {

		Label labelControl= new Label(composite, SWT.NONE);
		labelControl.setText(label);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		labelControl.setLayoutData(gd);
		
		ColorEditor editor= new ColorEditor(composite);
		Button button= editor.getButton();
		button.setData(editor);
		
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		button.addSelectionListener(fColorButtonListener);
		
		fColorButtons.put(editor, key);
		
		return composite;
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
	
	private Control addTextField(Composite composite, String label, String key, int textLimit, int indentation, boolean isNumber) {
		
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
			
		return textControl;
	}
	
	private void addTextFontEditor(Composite parent, String label, String key) {
		
		Composite editorComposite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		editorComposite.setLayout(layout);		
		fFontEditor= new WorkbenchChainedTextFontFieldEditor(key, label, editorComposite);
		fFontEditor.setChangeButtonText(JavaUIMessages.getString("JavaEditorPreferencePage.change")); //$NON-NLS-1$
				
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		editorComposite.setLayoutData(gd);
	}
	
	private String loadPreviewContentFromFile(String filename) {
		String line;
		String separator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer= new StringBuffer(512);
		BufferedReader reader= null;
		try {
			reader= new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
			while ((line= reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(separator);
			}
		} catch (IOException io) {
			JavaPlugin.log(io);
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException e) {}
			}
		}
		return buffer.toString();
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
			status.setError(JavaUIMessages.getString("JavaEditorPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0)
					status.setError(JavaUIMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				status.setError(JavaUIMessages.getFormattedString("JavaEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			}
		}
		return status;
	}
	
	private void updateStatus(IStatus status) {
		if (!status.matches(IStatus.ERROR)) {
			for (int i= 0; i < fNumberFields.size(); i++) {
				Text text= (Text) fNumberFields.get(i);
				IStatus s= validatePositiveNumber(text.getText());
				status= StatusUtil.getMoreSevere(s, status);
			}
		}	
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */
	public static boolean indicateQuixFixableProblems() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CORRECTION_INDICATION);
	}

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */	
	static public boolean synchronizeOutlineOnCursorMove() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SYNC_OUTLINE_ON_CURSOR_MOVE);
	}

}