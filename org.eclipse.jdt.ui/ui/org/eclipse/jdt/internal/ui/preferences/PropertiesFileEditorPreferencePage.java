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

import org.eclipse.core.runtime.IStatus;

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

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchViewerSorter;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IColorManager;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.IPropertiesFilePartitions;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileDocumentSetupParticipant;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileSourceViewerConfiguration;
import org.eclipse.jdt.internal.ui.text.JavaColorManager;

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
					if (event.getProperty().equals(PreferenceConstants.PROPERTIES_FILE_EDITOR_TEXT_FONT)) {
						Font font= JFaceResources.getFont(PreferenceConstants.PROPERTIES_FILE_EDITOR_TEXT_FONT);
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
	/** The keys of the overlay store. */
	private final String[][] fSyntaxColorListModel= new String[][] {
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.key"), PreferenceConstants.PROPERTIES_FILE_COLORING_KEY }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.value"), PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.assignment"), PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.argument"), PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT }, //$NON-NLS-1$
		{ PreferencesMessages.getString("PropertiesFileEditorPreferencePage.comment"), PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT } //$NON-NLS-1$
	};
	
	private OverlayPreferenceStore fOverlayStore;
	
	private Map fColorButtons= new HashMap();
	
	
	private ColorEditor fSyntaxForegroundColorEditor;
	private Button fBoldCheckBox;

	/**
	 * Check box for italic preference.
	 */
	private Button fItalicCheckBox;
	private SourceViewer fPreviewViewer;
	
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
	 * The color manager.
	 */
	private IColorManager fColorManager;
	

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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE);
	}

	private void handleSyntaxColorListSelection() {
		HighlightingColorListItem item= getHighlightingColorListItem();
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, item.getColorKey());
		fSyntaxForegroundColorEditor.setColorValue(rgb);		
		fBoldCheckBox.setSelection(fOverlayStore.getBoolean(item.getBoldKey()));
		fItalicCheckBox.setSelection(fOverlayStore.getBoolean(item.getItalicKey()));

		fSyntaxForegroundColorEditor.getButton().setEnabled(true);
		fBoldCheckBox.setEnabled(true);
		fItalicCheckBox.setEnabled(true);
	}

	private Control createSyntaxPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NONE);
		colorComposite.setLayout(new GridLayout());

		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString("PropertiesFileEditorPreferencePage.foreground")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		editorComposite.setLayoutData(gd);		

		fHighlightingColorListViewer= new TableViewer(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		fHighlightingColorListViewer.setLabelProvider(new ColorListLabelProvider());
		fHighlightingColorListViewer.setContentProvider(new ColorListContentProvider());
		fHighlightingColorListViewer.setSorter(new WorkbenchViewerSorter());
		gd= new GridData(SWT.BEGINNING, SWT.FILL, false, true);
		gd.heightHint= convertHeightInCharsToPixels(5);
		fHighlightingColorListViewer.getControl().setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		
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
		
		colorComposite.layout();
		
		return colorComposite;
	}
	
	private Control createPreviewer(Composite parent) {
		
		IPreferenceStore store= new ChainedPreferenceStore(new IPreferenceStore[] { fOverlayStore, JavaPlugin.getDefault().getCombinedPreferenceStore()});
		fPreviewViewer= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, store);
		fColorManager= new JavaColorManager(false);
		PropertiesFileSourceViewerConfiguration configuration= new PropertiesFileSourceViewerConfiguration(fColorManager, store, null, IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING);
		fPreviewViewer.configure(configuration);
		Font font= JFaceResources.getFont(PreferenceConstants.PROPERTIES_FILE_EDITOR_TEXT_FONT);
		fPreviewViewer.getTextWidget().setFont(font);
		new SourcePreviewerUpdater(fPreviewViewer, configuration, store);
		fPreviewViewer.setEditable(false);
		
		String content= loadPreviewContentFromFile("PropertiesFileEditorColorSettingPreviewCode.txt"); //$NON-NLS-1$
		IDocument document= new Document(content);
		PropertiesFileDocumentSetupParticipant.setupDocument(document);
		fPreviewViewer.setDocument(document);

		return fPreviewViewer.getControl();
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
		fOverlayStore.load();
		fOverlayStore.start();

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
	}
	
	private void initializeFields() {
		
		Iterator e= fColorButtons.keySet().iterator();
		while (e.hasNext()) {
			ColorEditor c= (ColorEditor) e.next();
			String key= (String) fColorButtons.get(c);
			RGB rgb= PreferenceConverter.getColor(fOverlayStore, key);
			c.setColorValue(rgb);
		}
		
        fFieldsInitialized= true;
        updateStatus(new StatusInfo());
        
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
		
		fColorManager.dispose();

		super.dispose();
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
	
	void updateStatus(IStatus status) {
		if (!fFieldsInitialized)
			return;
		
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
