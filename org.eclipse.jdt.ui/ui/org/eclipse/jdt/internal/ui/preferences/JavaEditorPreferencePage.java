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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchViewerSorter;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager.HighlightedRange;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.JavaColorManager;
import org.eclipse.jdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.jdt.internal.ui.util.TabFolderLayout;

/**
 * The page for setting the editor options.
 */
public class JavaEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	/**
	 * Item in the highlighting color list.
	 * 
	 * @since 3.0
	 */
	private static class HighlightingColorListItem {
		/** Display name */
		private String fDisplayName;
		/** Color preference key */
		private String fColorKey;
		/** Bold preference key */
		private String fBoldKey;
		/** Italic preference key */
		private String fItalicKey;
		/** Item color */
		private Color fItemColor;
		
		/**
		 * Initialize the item with the given values.
		 * 
		 * @param displayName the display name
		 * @param colorKey the color preference key
		 * @param boldKey the bold preference key
		 * @param italicKey the italic preference key
		 * @param itemColor the item color
		 */
		public HighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey, Color itemColor) {
			fDisplayName= displayName;
			fColorKey= colorKey;
			fBoldKey= boldKey;
			fItalicKey= italicKey;
			fItemColor= itemColor;
		}
		
		/**
		 * @return the bold preference key
		 */
		public String getBoldKey() {
			return fBoldKey;
		}
		
		/**
		 * @return the bold preference key
		 */
		public String getItalicKey() {
			return fItalicKey;
		}
		
		/**
		 * @return the color preference key
		 */
		public String getColorKey() {
			return fColorKey;
		}
		
		/**
		 * @return the display name
		 */
		public String getDisplayName() {
			return fDisplayName;
		}
		
		/**
		 * @return the item color
		 */
		public Color getItemColor() {
			return fItemColor;
		}
	}
	
	private static class SemanticHighlightingColorListItem extends HighlightingColorListItem {

		/** Enablement preference key */
		private final String fEnableKey;
		
		/**
		 * Initialize the item with the given values.
		 * 
		 * @param displayName the display name
		 * @param colorKey the color preference key
		 * @param boldKey the bold preference key
		 * @param italicKey the italic preference key
		 * @param enableKey the enable preference key
		 * @param itemColor the item color
		 */
		public SemanticHighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey, String enableKey, Color itemColor) {
			super(displayName, colorKey, boldKey, italicKey, itemColor);
			fEnableKey= enableKey;
		}

		/**
		 * @return the enablement preference key
		 */
		public String getEnableKey() {
			return fEnableKey;
		}
	}
	
	/**
	 * Color list label provider.
	 * 
	 * @since 3.0
	 */
	private class ColorListLabelProvider extends LabelProvider implements IColorProvider {

		/*
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return ((HighlightingColorListItem)element).getDisplayName();
		}
		
		/*
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		public Color getForeground(Object element) {
			return ((HighlightingColorListItem)element).getItemColor();
		}

		/*
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		public Color getBackground(Object element) {
			return null;
		}
	}
	
	/**
	 * Color list content provider.
	 * 
	 * @since 3.0
	 */
	private class ColorListContentProvider implements IStructuredContentProvider {

		/*
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return ((java.util.List)inputElement).toArray();
		}

		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	private static final String BOLD= PreferenceConstants.EDITOR_BOLD_SUFFIX;
	/**
	 * Preference key suffix for italic preferences.
	 * @since 3.0
	 */
	private static final String ITALIC= PreferenceConstants.EDITOR_ITALIC_SUFFIX;
	private static final String COMPILER_TASK_TAGS= JavaCore.COMPILER_TASK_TAGS;	
	private static final String DELIMITER= PreferencesMessages.getString("JavaEditorPreferencePage.navigation.delimiter"); //$NON-NLS-1$

	/** The keys of the overlay store. */
	private final String[][] fSyntaxColorListModel= new String[][] {
		{ PreferencesMessages.getString("JavaEditorPreferencePage.multiLineComment"), PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.singleLineComment"), PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.returnKeyword"), PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.keywords"), PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.strings"), PreferenceConstants.EDITOR_STRING_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.methodNames"), PreferenceConstants.EDITOR_JAVA_METHOD_NAME_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.operators"), PreferenceConstants.EDITOR_JAVA_OPERATOR_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.others"), PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.javaCommentTaskTags"), PreferenceConstants.EDITOR_TASK_TAG_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.javaDocKeywords"), PreferenceConstants.EDITOR_JAVADOC_KEYWORD_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.javaDocHtmlTags"), PreferenceConstants.EDITOR_JAVADOC_TAG_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.javaDocLinks"), PreferenceConstants.EDITOR_JAVADOC_LINKS_COLOR }, //$NON-NLS-1$
		{ PreferencesMessages.getString("JavaEditorPreferencePage.javaDocOthers"), PreferenceConstants.EDITOR_JAVADOC_DEFAULT_COLOR } //$NON-NLS-1$
	};
	
	private final String[][] fAppearanceColorListModel= new String[][] {
		{PreferencesMessages.getString("JavaEditorPreferencePage.lineNumberForegroundColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("JavaEditorPreferencePage.matchingBracketsHighlightColor2"), PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("JavaEditorPreferencePage.currentLineHighlighColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("JavaEditorPreferencePage.printMarginColor2"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("JavaEditorPreferencePage.findScopeColor2"), PreferenceConstants.EDITOR_FIND_SCOPE_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("JavaEditorPreferencePage.linkColor2"), PreferenceConstants.EDITOR_LINK_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("JavaEditorPreferencePage.selectionForegroundColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR}, //$NON-NLS-1$
		{PreferencesMessages.getString("JavaEditorPreferencePage.selectionBackgroundColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR}, //$NON-NLS-1$
	};
	
	private OverlayPreferenceStore fOverlayStore;
	private JavaEditorHoverConfigurationBlock fJavaEditorHoverConfigurationBlock;
	private FoldingConfigurationBlock fFoldingConfigurationBlock;
	
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
	
	private List fAppearanceColorList;
	private ColorEditor fSyntaxForegroundColorEditor;
	private ColorEditor fAppearanceColorEditor;
	private Button fAppearanceColorDefault;
	private ColorEditor fBackgroundColorEditor;
	private Button fBackgroundDefaultRadioButton;
	private Button fBackgroundCustomRadioButton;
	private Button fBackgroundColorButton;
	private Button fBoldCheckBox;
	private Button fEnableCheckbox;
	/**
	 * Check box for italic preference.
	 * @since 3.0
	 */
	private Button fItalicCheckBox;
	private JavaSourceViewer fPreviewViewer;
	private Color fBackgroundColor;
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
	 * Highlighting color list
	 * @since 3.0
	 */
	private final java.util.List fHighlightingColorList= new ArrayList();
	/**
	 * Highlighting color list viewer
	 * @since 3.0
	 */
	private TableViewer fHighlightingColorListViewer;

	/**
	 * Color of Semantic Highlighting items in color list (RGB)
	 * @since 3.0
	 */
	private static final int[] SEMANTIC_HIGHLIGHTING_ITEM_COLOR= new int[] { 0, 0, 0x80 };
	/**
	 * Semantic Highlighting color list
	 * @since 3.0
	 */
	private java.util.List fSemanticHighlightingColorList= new ArrayList();
	/**
	 * Semantic highlighting manager
	 * @since 3.0
	 */
	private SemanticHighlightingManager fSemanticHighlightingManager;
	/**
	 * Is semantic highlighting enabled? (internal state)
	 * @since 3.0
	 */
	private boolean fIsSemanticHighlightingEnabled;
	
	/**
	 * The color manager.
	 * @since 3.1
	 */
	private IColorManager fColorManager;
	
	
	/**
	 * Creates a new preference page.
	 */
	public JavaEditorPreferencePage() {
		setDescription(PreferencesMessages.getString("JavaEditorPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());

		fColorManager= new JavaColorManager(false);
		Color itemColor= fColorManager.getColor(new RGB(SEMANTIC_HIGHLIGHTING_ITEM_COLOR[0], SEMANTIC_HIGHLIGHTING_ITEM_COLOR[1], SEMANTIC_HIGHLIGHTING_ITEM_COLOR[2]));
		SemanticHighlighting[] semanticHighlightings= SemanticHighlightings.getSemanticHighlightings();
		for (int i= 0, n= semanticHighlightings.length; i < n; i++)
			fSemanticHighlightingColorList.add(new SemanticHighlightingColorListItem(semanticHighlightings[i].getDisplayName(), SemanticHighlightings.getColorPreferenceKey(semanticHighlightings[i]), SemanticHighlightings.getBoldPreferenceKey(semanticHighlightings[i]), SemanticHighlightings.getItalicPreferenceKey(semanticHighlightings[i]), SemanticHighlightings.getEnabledPreferenceKey(semanticHighlightings[i]), itemColor));
		
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), createOverlayStoreKeys());
	}
	
	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList overlayKeys= new ArrayList();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOREGROUND_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BACKGROUND_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
		
		for (int i= 0; i < fSyntaxColorListModel.length; i++) {
			String colorKey= fSyntaxColorListModel[i][1];
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, colorKey));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + BOLD));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + ITALIC));
		}
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_MATCHING_BRACKETS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_QUICKASSIST_LIGHTBULB));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FIND_SCOPE_COLOR));

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LINK_COLOR));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CORRECTION_INDICATION));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER));
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER));
				
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
		
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED));
		for (int i= 0, n= fSemanticHighlightingColorList.size(); i < n; i++) {
			SemanticHighlightingColorListItem item= (SemanticHighlightingColorListItem) fSemanticHighlightingColorList.get(i);
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, item.getColorKey()));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getBoldKey()));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getItalicKey()));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getEnableKey()));
		}
		
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

	private void handleSyntaxColorListSelection() {
		HighlightingColorListItem item= getHighlightingColorListItem();
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, item.getColorKey());
		fSyntaxForegroundColorEditor.setColorValue(rgb);		
		fBoldCheckBox.setSelection(fOverlayStore.getBoolean(item.getBoldKey()));
		fItalicCheckBox.setSelection(fOverlayStore.getBoolean(item.getItalicKey()));
		if (item instanceof SemanticHighlightingColorListItem) {
			fEnableCheckbox.setEnabled(true);
			boolean enable= fOverlayStore.getBoolean(((SemanticHighlightingColorListItem) item).getEnableKey());
			fEnableCheckbox.setSelection(enable);
			fSyntaxForegroundColorEditor.getButton().setEnabled(enable);
			fBoldCheckBox.setEnabled(enable);
			fItalicCheckBox.setEnabled(enable);
		} else {
			fSyntaxForegroundColorEditor.getButton().setEnabled(true);
			fBoldCheckBox.setEnabled(true);
			fItalicCheckBox.setEnabled(true);
			fEnableCheckbox.setEnabled(false);
			fEnableCheckbox.setSelection(true);
		}
	}

	private void handleSemanticHighlightingEnabled() {
		boolean shouldBeEnabled= fOverlayStore.getBoolean(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED);
		if (shouldBeEnabled && !fIsSemanticHighlightingEnabled) {
			fHighlightingColorListViewer.getControl().setRedraw(false);
			fHighlightingColorList.addAll(fSemanticHighlightingColorList);
			fHighlightingColorListViewer.add(fSemanticHighlightingColorList.toArray());
			fHighlightingColorListViewer.getControl().setRedraw(true);
			fHighlightingColorListViewer.reveal(getHighlightingColorListItem());
			fIsSemanticHighlightingEnabled= true;
		}
		if (!shouldBeEnabled && fIsSemanticHighlightingEnabled) {
			fHighlightingColorListViewer.getControl().setRedraw(false);
			int fullSize= fHighlightingColorList.size();
			fHighlightingColorList.removeAll(fSemanticHighlightingColorList);
			HighlightingColorListItem item= getHighlightingColorListItem();
			if (!fHighlightingColorList.contains(item)) {
				int i= 0;
				while (item != fHighlightingColorListViewer.getElementAt(i))
					i++;
				while (!fHighlightingColorList.contains(fHighlightingColorListViewer.getElementAt(i)) && i < fullSize)
					i++;
				while (!fHighlightingColorList.contains(fHighlightingColorListViewer.getElementAt(i)) && i >= 0)
					i--;
				// Assume non-empty list
				fHighlightingColorListViewer.setSelection(new StructuredSelection(fHighlightingColorListViewer.getElementAt(i)));
			}
			fHighlightingColorListViewer.remove(fSemanticHighlightingColorList.toArray());
			fHighlightingColorListViewer.getControl().setRedraw(true);
			fHighlightingColorListViewer.reveal(getHighlightingColorListItem());
			fIsSemanticHighlightingEnabled= false;
		}
	}
	
	private void handleAppearanceColorListSelection() {	
		int i= fAppearanceColorList.getSelectionIndex();
		String key= fAppearanceColorListModel[i][1];
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
		fAppearanceColorEditor.setColorValue(rgb);		
		updateAppearanceColorWidgets(fAppearanceColorListModel[i][2]);
	}

	private void updateAppearanceColorWidgets(String systemDefaultKey) {
		if (systemDefaultKey == null) {
			fAppearanceColorDefault.setSelection(false);
			fAppearanceColorDefault.setVisible(false);
			fAppearanceColorEditor.getButton().setEnabled(true);
		} else {
			boolean systemDefault= fOverlayStore.getBoolean(systemDefaultKey);
			fAppearanceColorDefault.setSelection(systemDefault);
			fAppearanceColorDefault.setVisible(true);
			fAppearanceColorEditor.getButton().setEnabled(!systemDefault);
		}
	}

	private Control createSyntaxPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NONE);
		colorComposite.setLayout(new GridLayout());

		Group backgroundComposite= new Group(colorComposite, SWT.SHADOW_ETCHED_IN);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		backgroundComposite.setLayout(layout);
		backgroundComposite.setText(PreferencesMessages.getString("JavaEditorPreferencePage.backgroundColor"));//$NON-NLS-1$
	
		SelectionListener backgroundSelectionListener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {				
				boolean custom= fBackgroundCustomRadioButton.getSelection();
				fBackgroundColorButton.setEnabled(custom);
				fOverlayStore.setValue(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR, !custom);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};

		fBackgroundDefaultRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundDefaultRadioButton.setText(PreferencesMessages.getString("JavaEditorPreferencePage.systemDefault")); //$NON-NLS-1$
		fBackgroundDefaultRadioButton.addSelectionListener(backgroundSelectionListener);

		fBackgroundCustomRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
		fBackgroundCustomRadioButton.setText(PreferencesMessages.getString("JavaEditorPreferencePage.custom")); //$NON-NLS-1$
		fBackgroundCustomRadioButton.addSelectionListener(backgroundSelectionListener);
		
		fBackgroundColorEditor= new ColorEditor(backgroundComposite);
		fBackgroundColorButton= fBackgroundColorEditor.getButton();

		addCheckBox(colorComposite, PreferencesMessages.getString("JavaEditorPreferencePage.semanticHighlighting.option"), PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED, 0); //$NON-NLS-1$
		
		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("JavaEditorPreferencePage.foreground")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);		

		fHighlightingColorListViewer= new TableViewer(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		fHighlightingColorListViewer.setLabelProvider(new ColorListLabelProvider());
		fHighlightingColorListViewer.setContentProvider(new ColorListContentProvider());
		fHighlightingColorListViewer.setSorter(new WorkbenchViewerSorter());
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fHighlightingColorListViewer.getControl().setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fEnableCheckbox= new Button(stylesComposite, SWT.CHECK);
		fEnableCheckbox.setText(PreferencesMessages.getString("JavaEditorPreferencePage.enable")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fEnableCheckbox.setLayoutData(gd);
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("JavaEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= 20;
		label.setLayoutData(gd);

		fSyntaxForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fSyntaxForegroundColorEditor.getButton();
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		foregroundColorButton.setLayoutData(gd);
		
		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
		fBoldCheckBox.setText(PreferencesMessages.getString("JavaEditorPreferencePage.bold")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= 20;
		gd.horizontalSpan= 2;
		fBoldCheckBox.setLayoutData(gd);
		
		fItalicCheckBox= new Button(stylesComposite, SWT.CHECK);
		fItalicCheckBox.setText(PreferencesMessages.getString("JavaEditorPreferencePage.italic")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= 20;
		gd.horizontalSpan= 2;
		fItalicCheckBox.setLayoutData(gd);
		
		label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("JavaEditorPreferencePage.preview")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Control previewer= createPreviewer(colorComposite);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(20);
		gd.heightHint= convertHeightInCharsToPixels(5);
		previewer.setLayoutData(gd);

		
		fHighlightingColorListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSyntaxColorListSelection();
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item= getHighlightingColorListItem();
				PreferenceConverter.setValue(fOverlayStore, item.getColorKey(), fSyntaxForegroundColorEditor.getColorValue());
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
				HighlightingColorListItem item= getHighlightingColorListItem();
				fOverlayStore.setValue(item.getBoldKey(), fBoldCheckBox.getSelection());
			}
		});
				
		fItalicCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item= getHighlightingColorListItem();
				fOverlayStore.setValue(item.getItalicKey(), fItalicCheckBox.getSelection());
			}
		});
				
		fEnableCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item= getHighlightingColorListItem();
				if (item instanceof SemanticHighlightingColorListItem) {
					boolean enable= fEnableCheckbox.getSelection();
					fOverlayStore.setValue(((SemanticHighlightingColorListItem) item).getEnableKey(), enable);
					fEnableCheckbox.setSelection(enable);
					fSyntaxForegroundColorEditor.getButton().setEnabled(enable);
					fBoldCheckBox.setEnabled(enable);
					fItalicCheckBox.setEnabled(enable);
					uninstallSemanticHighlighting();
					installSemanticHighlighting();
				}
			}
		});
				
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		
		IPreferenceStore generalTextStore= EditorsUI.getPreferenceStore();
		IPreferenceStore store= new ChainedPreferenceStore(new IPreferenceStore[] { fOverlayStore, new PreferencesAdapter(createTemporaryCorePreferenceStore()), generalTextStore });
		fPreviewViewer= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, store);
		JavaSourceViewerConfiguration configuration= new JavaSourceViewerConfiguration(fColorManager, store, null, IJavaPartitions.JAVA_PARTITIONING);
		fPreviewViewer.configure(configuration);
		Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
		fPreviewViewer.getTextWidget().setFont(font);
		new JavaSourcePreviewerUpdater(fPreviewViewer, configuration, store);
		fPreviewViewer.setEditable(false);
		
		String content= loadPreviewContentFromFile("ColorSettingPreviewCode.txt"); //$NON-NLS-1$
		IDocument document= new Document(content);
		JavaPlugin.getDefault().getJavaTextTools().setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);
		fPreviewViewer.setDocument(document);

		installSemanticHighlighting();
		
		return fPreviewViewer.getControl();
	}
	
	private Preferences createTemporaryCorePreferenceStore() {
		Preferences result= new Preferences();
		
		result.setValue(COMPILER_TASK_TAGS, "TASK,TODO"); //$NON-NLS-1$
		
		return result;
	}
	
	private Control createAppearancePage(Composite parent) {

		Composite appearanceComposite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(); layout.numColumns= 2;
		appearanceComposite.setLayout(layout);

		String label= PreferencesMessages.getString("JavaEditorPreferencePage.displayedTabWidth"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 3, 0, true);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.printMarginColumn"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, 3, 0, true);
				
		label= PreferencesMessages.getString("JavaEditorPreferencePage.showOverviewRuler"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER, 0);
				
		label= PreferencesMessages.getString("JavaEditorPreferencePage.showLineNumbers"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER, 0);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.highlightMatchingBrackets"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_MATCHING_BRACKETS, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.highlightCurrentLine"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE, 0);
				
		label= PreferencesMessages.getString("JavaEditorPreferencePage.showPrintMargin"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN, 0);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.quickassist.lightbulb"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_QUICKASSIST_LIGHTBULB, 0); //$NON-NLS-1$

		label= PreferencesMessages.getString("JavaEditorPreferencePage.showQuickFixables"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_CORRECTION_INDICATION, 0);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.accessibility.disableCustomCarets"); //$NON-NLS-1$
		Button master= addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS, 0);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.accessibility.wideCaret"); //$NON-NLS-1$
		Button slave= addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET, 0);
		createDependency(master, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS, slave);

		Label l= new Label(appearanceComposite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
		
		l= new Label(appearanceComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("JavaEditorPreferencePage.appearanceOptions")); //$NON-NLS-1$
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
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(8);
		fAppearanceColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l= new Label(stylesComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("JavaEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		l.setLayoutData(gd);

		fAppearanceColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fAppearanceColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);

		SelectionListener colorDefaultSelectionListener= new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean systemDefault= fAppearanceColorDefault.getSelection();
				fAppearanceColorEditor.getButton().setEnabled(!systemDefault);
				
				int i= fAppearanceColorList.getSelectionIndex();
				String key= fAppearanceColorListModel[i][2];
				if (key != null)
					fOverlayStore.setValue(key, systemDefault);
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		};
		
		fAppearanceColorDefault= new Button(stylesComposite, SWT.CHECK);
		fAppearanceColorDefault.setText(PreferencesMessages.getString("JavaEditorPreferencePage.systemDefault")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fAppearanceColorDefault.setLayoutData(gd);
		fAppearanceColorDefault.setVisible(false);
		fAppearanceColorDefault.addSelectionListener(colorDefaultSelectionListener);
		
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
				
				PreferenceConverter.setValue(fOverlayStore, key, fAppearanceColorEditor.getColorValue());
			}
		});
		return appearanceComposite;
	}

	private Control createTypingPage(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		composite.setLayout(layout);
		
		String label= PreferencesMessages.getString("JavaEditorPreferencePage.analyseAnnotationsWhileTyping"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS, 0);
		
		addFiller(composite);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.overwriteMode"); //$NON-NLS-1$
		addCheckBox(composite, label, PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE, 1);
		
		addFiller(composite);
		
		Group group= new Group(composite, SWT.NONE);
		layout= new GridLayout();
		layout.numColumns= 2;
		group.setLayout(layout);

		group.setText(PreferencesMessages.getString("JavaEditorPreferencePage.typing.description")); //$NON-NLS-1$

		label= PreferencesMessages.getString("JavaEditorPreferencePage.wrapStrings"); //$NON-NLS-1$
		Button master= addCheckBox(group, label, PreferenceConstants.EDITOR_WRAP_STRINGS, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.escapeStrings"); //$NON-NLS-1$
		Button slave= addCheckBox(group, label, PreferenceConstants.EDITOR_ESCAPE_STRINGS, 1);
		createDependency(master, PreferenceConstants.EDITOR_WRAP_STRINGS, slave);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.smartPaste"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_SMART_PASTE, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.importsOnPaste"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.insertSpaceForTabs"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_SPACES_FOR_TABS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeStrings"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_CLOSE_STRINGS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeBrackets"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_CLOSE_BRACKETS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeBraces"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_CLOSE_BRACES, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.closeJavaDocs"); //$NON-NLS-1$
		master= addCheckBox(group, label, PreferenceConstants.EDITOR_CLOSE_JAVADOCS, 1);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.addJavaDocTags"); //$NON-NLS-1$
		slave= addCheckBox(group, label, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, 1);
		createDependency(master, PreferenceConstants.EDITOR_CLOSE_JAVADOCS, slave);

		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartSemicolon"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_SMART_SEMICOLON, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartOpeningBrace"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_SMART_OPENING_BRACE, 1);
		
		label= PreferencesMessages.getString("JavaEditorPreferencePage.typing.smartTab"); //$NON-NLS-1$
		addCheckBox(group, label, PreferenceConstants.EDITOR_SMART_TAB, 1);

//		label= PreferencesMessages.getString("JavaEditorPreferencePage.formatJavaDocs"); //$NON-NLS-1$
//		addCheckBox(group, label, PreferenceConstants.EDITOR_FORMAT_JAVADOCS, 1);

		return composite;
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
		
		initializeDefaultColors();
		
		fJavaEditorHoverConfigurationBlock= new JavaEditorHoverConfigurationBlock(this, fOverlayStore);
		fFoldingConfigurationBlock= new FoldingConfigurationBlock(fOverlayStore);
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		TabFolder folder= new TabFolder(parent, SWT.NONE);
		folder.setLayout(new TabFolderLayout());	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TabItem item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("JavaEditorPreferencePage.general")); //$NON-NLS-1$
		item.setControl(createAppearancePage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("JavaEditorPreferencePage.colors")); //$NON-NLS-1$
		item.setControl(createSyntaxPage(folder));

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("JavaEditorPreferencePage.typing.tabTitle")); //$NON-NLS-1$
		item.setControl(createTypingPage(folder));

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("JavaEditorPreferencePage.hoverTab.title")); //$NON-NLS-1$
		item.setControl(fJavaEditorHoverConfigurationBlock.createControl(folder));

		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("JavaEditorPreferencePage.navigationTab.title")); //$NON-NLS-1$
		item.setControl(createNavigationPage(folder));
		
		item= new TabItem(folder, SWT.NONE);
		item.setText(PreferencesMessages.getString("JavaEditorPreferencePage.folding.title")); //$NON-NLS-1$
		item.setControl(fFoldingConfigurationBlock.createControl(folder));

		initialize();
		
		Dialog.applyDialogFont(folder);
		return folder;
	}
	
	private void initialize() {
		
		initializeFields();
		
		for (int i= 0, n= fSyntaxColorListModel.length; i < n; i++)
			fHighlightingColorList.add(new HighlightingColorListItem (fSyntaxColorListModel[i][0], fSyntaxColorListModel[i][1], fSyntaxColorListModel[i][1] + BOLD, fSyntaxColorListModel[i][1] + ITALIC, null));
		if (fOverlayStore.getBoolean(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED)) {
			fHighlightingColorList.addAll(fSemanticHighlightingColorList);
			fIsSemanticHighlightingEnabled= true;
		}
		fHighlightingColorListViewer.setInput(fHighlightingColorList);
		fHighlightingColorListViewer.setSelection(new StructuredSelection(fHighlightingColorListViewer.getElementAt(0)));
		fOverlayStore.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED.equals(event.getProperty()))
					handleSemanticHighlightingEnabled();
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
		
		fFoldingConfigurationBlock.initialize();
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

	private void initializeDefaultColors() {	
		if (!getPreferenceStore().contains(PreferenceConstants.EDITOR_BACKGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), PreferenceConstants.EDITOR_BACKGROUND_COLOR, rgb);
		}
		if (!getPreferenceStore().contains(PreferenceConstants.EDITOR_FOREGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, PreferenceConstants.EDITOR_FOREGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), PreferenceConstants.EDITOR_FOREGROUND_COLOR, rgb);
		}
		if (!getPreferenceStore().contains(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, rgb);
		}
		if (!getPreferenceStore().contains(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR)) {
			RGB rgb= getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT).getRGB();
			PreferenceConverter.setDefault(fOverlayStore, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, rgb);
			PreferenceConverter.setDefault(getPreferenceStore(), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, rgb);
		}
	}
	
	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		fJavaEditorHoverConfigurationBlock.performOk();
		fFoldingConfigurationBlock.performOk();
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

		initializeFields();

		handleSyntaxColorListSelection();
		handleAppearanceColorListSelection();

		fJavaEditorHoverConfigurationBlock.performDefaults();
		fFoldingConfigurationBlock.performDefaults();

		super.performDefaults();

		fPreviewViewer.invalidateTextPresentation();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
		uninstallSemanticHighlighting();
		
		fFoldingConfigurationBlock.dispose();
		fColorManager.dispose();
		
		if (fOverlayStore != null) {
			fOverlayStore.stop();
			fOverlayStore= null;
		}
		if (fBackgroundColor != null && !fBackgroundColor.isDisposed())
			fBackgroundColor.dispose();
		
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
	 * @return
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
		status= StatusUtil.getMoreSevere(fJavaEditorHoverConfigurationBlock.getStatus(), status);
		status= StatusUtil.getMoreSevere(getBrowserLikeLinksKeyModifierStatus(), status);
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}
	
	/**
	 * Install Semantic Highlighting on the previewer
	 * 
	 * @since 3.0
	 */
	private void installSemanticHighlighting() {
		if (fSemanticHighlightingManager == null) {
			fSemanticHighlightingManager= new SemanticHighlightingManager();
			fSemanticHighlightingManager.install(fPreviewViewer, fColorManager, fOverlayStore, createPreviewerRanges());
		}
	}
	
	/**
	 * Uninstall Semantic Highlighting from the previewer
	 * 
	 * @since 3.0
	 */
	private void uninstallSemanticHighlighting() {
		if (fSemanticHighlightingManager != null) {
			fSemanticHighlightingManager.uninstall();
			fSemanticHighlightingManager= null;
		}
	}

	/**
	 * Create the hard coded previewer ranges
	 * 
	 * @return the hard coded previewer ranges
	 * @since 3.0
	 */
	private SemanticHighlightingManager.HighlightedRange[][] createPreviewerRanges() {
		return new SemanticHighlightingManager.HighlightedRange[][] {
			{ createHighlightedRange(6, 13, 9, SemanticHighlightings.DEPRECATED_MEMBER) },
			{ createHighlightedRange(7, 26, 8, SemanticHighlightings.STATIC_FINAL_FIELD), createHighlightedRange(7, 26, 8, SemanticHighlightings.STATIC_FIELD), createHighlightedRange(7, 26, 8, SemanticHighlightings.FIELD) },
			{ createHighlightedRange(9, 20, 11, SemanticHighlightings.STATIC_FIELD), createHighlightedRange(9, 20, 11, SemanticHighlightings.FIELD) },
			{ createHighlightedRange(11, 16, 5, SemanticHighlightings.FIELD) },
			{ createHighlightedRange(13, 12, 3, SemanticHighlightings.METHOD_DECLARATION) },
			{ createHighlightedRange(13, 20, 9, SemanticHighlightings.PARAMETER_VARIABLE) },
			{ createHighlightedRange(14, 2, 14, SemanticHighlightings.ABSTRACT_METHOD_INVOCATION) },
			{ createHighlightedRange(15, 6, 5, SemanticHighlightings.LOCAL_VARIABLE_DECLARATION) },
			{ createHighlightedRange(15, 16, 8, SemanticHighlightings.INHERITED_METHOD_INVOCATION) },
			{ createHighlightedRange(16, 2, 12, SemanticHighlightings.STATIC_METHOD_INVOCATION) },
			{ createHighlightedRange(17, 13, 5, SemanticHighlightings.LOCAL_VARIABLE) }
		};
	}

	/**
	 * Create a highlighted range on the previewers document with the given line, column, length and key.
	 * 
	 * @param line the line
	 * @param column the column
	 * @param length the length
	 * @param key the key
	 * @return the highlighted range
	 * @since 3.0
	 */
	private HighlightedRange createHighlightedRange(int line, int column, int length, String key) {
		try {
			IDocument document= fPreviewViewer.getDocument();
			int offset= document.getLineOffset(line) + column;
			return new HighlightedRange(offset, length, key);
		} catch (BadLocationException x) {
			JavaPlugin.log(x);
		}
		return null;
	}

	/**
	 * Returns the current highlighting color list item.
	 * 
	 * @return the current highlighting color list item
	 * @since 3.0
	 */
	private HighlightingColorListItem getHighlightingColorListItem() {
		IStructuredSelection selection= (IStructuredSelection) fHighlightingColorListViewer.getSelection();
		return (HighlightingColorListItem) selection.getFirstElement();
	}
}
