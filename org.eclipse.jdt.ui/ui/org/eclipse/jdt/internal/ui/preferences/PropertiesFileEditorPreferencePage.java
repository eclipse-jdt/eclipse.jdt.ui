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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Assert;
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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchViewerSorter;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.IPropertiesFilePartitions;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileDocumentSetupParticipant;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileSourceViewerConfiguration;
import org.eclipse.jdt.internal.ui.text.PreferencesAdapter;

/**
 * The page for setting the properties file editor preferences.
 * 
 * @since 3.1
 */
public class PropertiesFileEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	
	private static class SourcePreviewerUpdater {
		
		/**
		 * Creates a Java source preview updater for the given viewer, configuration and preference store.
		 *
		 * @param viewer the viewer
		 * @param configuration the configuration
		 * @param preferenceStore the preference store
		 */
		SourcePreviewerUpdater(final SourceViewer viewer, final PropertiesFileSourceViewerConfiguration configuration, final IPreferenceStore preferenceStore) {
			Assert.isNotNull(viewer);
			Assert.isNotNull(configuration);
			Assert.isNotNull(preferenceStore);
			final IPropertyChangeListener fontChangeListener= new IPropertyChangeListener() {
				/*
				 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
				 */
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(PreferenceConstants.EDITOR_TEXT_FONT)) {
						Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
						viewer.getTextWidget().setFont(font);
					}
				}
			};
			final IPropertyChangeListener propertyChangeListener= new IPropertyChangeListener() {
				/*
				 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
				 */
				public void propertyChange(PropertyChangeEvent event) {
					if (configuration.affectsTextPresentation(event)) {
						configuration.handlePropertyChangeEvent(event);
						viewer.invalidateTextPresentation();
					}
				}
			};
			viewer.getTextWidget().addDisposeListener(new DisposeListener() {
				/*
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				public void widgetDisposed(DisposeEvent e) {
					preferenceStore.removePropertyChangeListener(propertyChangeListener);
					JFaceResources.getFontRegistry().removeListener(fontChangeListener);
				}
			});
			JFaceResources.getFontRegistry().addListener(fontChangeListener);
			preferenceStore.addPropertyChangeListener(propertyChangeListener);
		}
	}
	
	
	/**
	 * Item in the highlighting color list.
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
	 */
	private static final String ITALIC= PreferenceConstants.EDITOR_ITALIC_SUFFIX;
	private static final String COMPILER_TASK_TAGS= JavaCore.COMPILER_TASK_TAGS;	

	/** The keys of the overlay store. */
	private final String[][] fSyntaxColorListModel= new String[][] {
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.key"), PreferenceConstants.PROPERTIES_FILE_COLORING_KEY }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.value"), PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.assignment"), PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.argument"), PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.comment"), PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT } //$NON-NLS-1$
	};
	
	private final String[][] fAppearanceColorListModel= new String[][] {
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.lineNumberForegroundColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.matchingBracketsHighlightColor2"), PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.currentLineHighlighColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.printMarginColor2"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.findScopeColor2"), PreferenceConstants.EDITOR_FIND_SCOPE_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.linkColor2"), PreferenceConstants.EDITOR_LINK_COLOR, null}, //$NON-NLS-1$
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.selectionForegroundColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR}, //$NON-NLS-1$
		{PreferencesMessages.getString("PropertiesFileEditorPreferencePage.selectionBackgroundColor"), AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR}, //$NON-NLS-1$
	};
	
	private OverlayPreferenceStore fOverlayStore;
	
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
	 */
	private Button fItalicCheckBox;
	private SourceViewer fPreviewViewer;
	private Color fBackgroundColor;
	
	/**
	 * Tells whether the fields are initialized.
	 */
	private boolean fFieldsInitialized= false;
	
	/**
	 * List of master/slave listeners when there's a dependency.
	 * 
	 * @see #createDependency(Button, String, Control)
	 */
	private ArrayList fMasterSlaveListeners= new ArrayList();

	/**
	 * Highlighting color list
	 */
	private final java.util.List fHighlightingColorList= new ArrayList();
	/**
	 * Highlighting color list viewer
	 */
	private TableViewer fHighlightingColorListViewer;

	
	/**
	 * Creates a new preference page.
	 */
	public PropertiesFileEditorPreferencePage() {
		setDescription(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.description")); //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), createOverlayStoreKeys());
	}
	
	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList overlayKeys= new ArrayList();

		for (int i= 0; i < fSyntaxColorListModel.length; i++) {
			String colorKey= fSyntaxColorListModel[i][1];
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, colorKey));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + BOLD));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, colorKey + ITALIC));
		}
		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOREGROUND_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOREGROUND_DEFAULT_COLOR));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BACKGROUND_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_FOREGROUND_DEFAULT_COLOR));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SELECTION_BACKGROUND_DEFAULT_COLOR));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
//		
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_MATCHING_BRACKETS));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.INT, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_QUICKASSIST_LIGHTBULB));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FIND_SCOPE_COLOR));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_LINK_COLOR));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CORRECTION_INDICATION));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_EVALUTE_TEMPORARY_PROBLEMS));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER_COLOR));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER));
//				
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SPACES_FOR_TABS));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_PASTE));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_IMPORTS_ON_PASTE));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_STRINGS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACKETS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_BRACES));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_CLOSE_JAVADOCS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_WRAP_STRINGS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ESCAPE_STRINGS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_HOME_END));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_DISABLE_OVERWRITE_MODE));
//
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_BROWSER_LIKE_LINKS_KEY_MODIFIER_MASK));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_SEMICOLON));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_TAB));
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SMART_OPENING_BRACE));
//		
//		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED));
		
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

//		Group backgroundComposite= new Group(colorComposite, SWT.SHADOW_ETCHED_IN);
//		GridLayout layout= new GridLayout();
//		layout.numColumns= 3;
//		backgroundComposite.setLayout(layout);
//		backgroundComposite.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.backgroundColor"));//$NON-NLS-1$
//	
//		SelectionListener backgroundSelectionListener= new SelectionListener() {
//			public void widgetSelected(SelectionEvent e) {				
//				boolean custom= fBackgroundCustomRadioButton.getSelection();
//				fBackgroundColorButton.setEnabled(custom);
//				fOverlayStore.setValue(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR, !custom);
//			}
//			public void widgetDefaultSelected(SelectionEvent e) {}
//		};
//
//		fBackgroundDefaultRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
//		fBackgroundDefaultRadioButton.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.systemDefault")); //$NON-NLS-1$
//		fBackgroundDefaultRadioButton.addSelectionListener(backgroundSelectionListener);
//
//		fBackgroundCustomRadioButton= new Button(backgroundComposite, SWT.RADIO | SWT.LEFT);
//		fBackgroundCustomRadioButton.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.custom")); //$NON-NLS-1$
//		fBackgroundCustomRadioButton.addSelectionListener(backgroundSelectionListener);
//		
//		fBackgroundColorEditor= new ColorEditor(backgroundComposite);
//		fBackgroundColorButton= fBackgroundColorEditor.getButton();

		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.foreground")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		GridLayout layout= new GridLayout();
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
		fEnableCheckbox.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.enable")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fEnableCheckbox.setLayoutData(gd);
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.color")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= 20;
		label.setLayoutData(gd);

		fSyntaxForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fSyntaxForegroundColorEditor.getButton();
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		foregroundColorButton.setLayoutData(gd);
		
		fBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
		fBoldCheckBox.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.bold")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= 20;
		gd.horizontalSpan= 2;
		fBoldCheckBox.setLayoutData(gd);
		
		fItalicCheckBox= new Button(stylesComposite, SWT.CHECK);
		fItalicCheckBox.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.italic")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= 20;
		gd.horizontalSpan= 2;
		fItalicCheckBox.setLayoutData(gd);
		
		label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.preview")); //$NON-NLS-1$
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

//		fBackgroundColorButton.addSelectionListener(new SelectionListener() {
//			public void widgetDefaultSelected(SelectionEvent e) {
//				// do nothing
//			}
//			public void widgetSelected(SelectionEvent e) {
//				PreferenceConverter.setValue(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR, fBackgroundColorEditor.getColorValue());					
//			}
//		});

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
				}
			}
		});
				
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		
		IPreferenceStore generalTextStore= EditorsUI.getPreferenceStore();
		IPreferenceStore store= new ChainedPreferenceStore(new IPreferenceStore[] { fOverlayStore, new PreferencesAdapter(createTemporaryCorePreferenceStore()), generalTextStore });
		fPreviewViewer= new SourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		PropertiesFileSourceViewerConfiguration configuration= new PropertiesFileSourceViewerConfiguration(tools.getColorManager(), store, null, IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING);
		fPreviewViewer.configure(configuration);
		Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
		fPreviewViewer.getTextWidget().setFont(font);
		new SourcePreviewerUpdater(fPreviewViewer, configuration, store);
		fPreviewViewer.setEditable(false);
		
		String content= loadPreviewContentFromFile("PropertiesFileEditorColorSettingPreviewCode.txt"); //$NON-NLS-1$
		IDocument document= new Document(content);
		new PropertiesFileDocumentSetupParticipant().setup(document);
		fPreviewViewer.setDocument(document);

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

		String label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.displayedTabWidth"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 3, 0, true);

		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.printMarginColumn"); //$NON-NLS-1$
		addTextField(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN, 3, 0, true);
				
		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.showOverviewRuler"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_OVERVIEW_RULER, 0);
				
		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.showLineNumbers"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER, 0);

		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.highlightMatchingBrackets"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_MATCHING_BRACKETS, 0);
		
		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.highlightCurrentLine"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE, 0);
				
		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.showPrintMargin"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN, 0);

		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.quickassist.lightbulb"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_QUICKASSIST_LIGHTBULB, 0); //$NON-NLS-1$

		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.showQuickFixables"); //$NON-NLS-1$
		addCheckBox(appearanceComposite, label, PreferenceConstants.EDITOR_CORRECTION_INDICATION, 0);
		
		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.accessibility.disableCustomCarets"); //$NON-NLS-1$
		Button master= addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS, 0);

		label= PreferencesMessages.getString("PropertiesFileEditorPreferencePage.accessibility.wideCaret"); //$NON-NLS-1$
		Button slave= addCheckBox(appearanceComposite, label, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_WIDE_CARET, 0);
		createDependency(master, AbstractDecoratedTextEditorPreferenceConstants.EDITOR_USE_CUSTOM_CARETS, slave);

		Label l= new Label(appearanceComposite, SWT.LEFT );
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.heightHint= convertHeightInCharsToPixels(1) / 2;
		l.setLayoutData(gd);
		
		l= new Label(appearanceComposite, SWT.LEFT);
		l.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.appearanceOptions")); //$NON-NLS-1$
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
		l.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.color")); //$NON-NLS-1$
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
		fAppearanceColorDefault.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.systemDefault")); //$NON-NLS-1$
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

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		initializeDefaultColors();
		
		fOverlayStore.load();
		fOverlayStore.start();
		
//		TabFolder folder= new TabFolder(parent, SWT.NONE);
//		folder.setLayout(new TabFolderLayout());	
//		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		
//		TabItem item= new TabItem(folder, SWT.NONE);
//		item.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.general")); //$NON-NLS-1$
//		item.setControl(createAppearancePage(folder));
	
//		item= new TabItem(folder, SWT.NONE);
//		item.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.colors")); //$NON-NLS-1$
//		item.setControl(createSyntaxPage(folder));

		Control control= createSyntaxPage(parent);

		initialize();
		
		Dialog.applyDialogFont(control);
		return control;
	}
	
	private void initialize() {
		
		initializeFields();
		
		for (int i= 0, n= fSyntaxColorListModel.length; i < n; i++)
			fHighlightingColorList.add(new HighlightingColorListItem (fSyntaxColorListModel[i][0], fSyntaxColorListModel[i][1], fSyntaxColorListModel[i][1] + BOLD, fSyntaxColorListModel[i][1] + ITALIC, null));

		fHighlightingColorListViewer.setInput(fHighlightingColorList);
		fHighlightingColorListViewer.setSelection(new StructuredSelection(fHighlightingColorListViewer.getElementAt(0)));
//		
//		for (int i= 0; i < fAppearanceColorListModel.length; i++)
//			fAppearanceColorList.add(fAppearanceColorListModel[i][0]);
//		fAppearanceColorList.getDisplay().asyncExec(new Runnable() {
//			public void run() {
//				if (fAppearanceColorList != null && !fAppearanceColorList.isDisposed()) {
//					fAppearanceColorList.select(0);
//					handleAppearanceColorListSelection();
//				}
//			}
//		});
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
		
//		RGB rgb= PreferenceConverter.getColor(fOverlayStore, PreferenceConstants.EDITOR_BACKGROUND_COLOR);
//		fBackgroundColorEditor.setColorValue(rgb);		
//		
//		boolean default_= fOverlayStore.getBoolean(PreferenceConstants.EDITOR_BACKGROUND_DEFAULT_COLOR);
//		fBackgroundDefaultRadioButton.setSelection(default_);
//		fBackgroundCustomRadioButton.setSelection(!default_);
//		fBackgroundColorButton.setEnabled(!default_);

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

		super.performDefaults();

		fPreviewViewer.invalidateTextPresentation();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
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
	 * @return the labelled text field
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
			status.setError(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.empty_input")); //$NON-NLS-1$
		} else {
			try {
				int value= Integer.parseInt(number);
				if (value < 0)
					status.setError(PreferencesMessages.getFormattedString("PropertiesFileEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				status.setError(PreferencesMessages.getFormattedString("PropertiesFileEditorPreferencePage.invalid_input", number)); //$NON-NLS-1$
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
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}
	
	/**
	 * Returns the current highlighting color list item.
	 * 
	 * @return the current highlighting color list item
	 */
	private HighlightingColorListItem getHighlightingColorListItem() {
		IStructuredSelection selection= (IStructuredSelection) fHighlightingColorListViewer.getSelection();
		return (HighlightingColorListItem) selection.getFirstElement();
	}
}
