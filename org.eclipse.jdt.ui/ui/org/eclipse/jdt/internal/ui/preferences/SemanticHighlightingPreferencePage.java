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

import org.eclipse.swt.SWT;
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;

/**
 * The page for setting the semantic highlighting options.
 * 
 * @since 3.0
 */
public class SemanticHighlightingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	/** Semantic message key prefix */
	private static final String PREFIX= "SemanticHighlightingPreferencePage."; //$NON-NLS-1$
	
	/** Semantic highlighting enable checkbox */
	private Button fSemanticEnableCheckBox;
	/** Semantic highlighting color list */
	private org.eclipse.swt.widgets.List fSemanticColorList;
	/** Semantic highlighting forground color editor */
	private ColorEditor fSemanticForegroundColorEditor;
	/** Semantic highlighting bold checkbox */
	private Button fSemanticBoldCheckBox;

	/** The keys of the overlay store */
	public final OverlayPreferenceStore.OverlayKey[] fKeys;	
	/** The overlay store */
	private OverlayPreferenceStore fOverlayStore;
	
	private Map fCheckBoxes= new HashMap();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
		}
	};
	
	/** The semantic highlightings */
	private SemanticHighlighting[] fSemanticHighlightings;
	
	/**
	 * Creates a new preference page.
	 */
	public SemanticHighlightingPreferencePage() {
		fSemanticHighlightings= SemanticHighlightings.getSemanticHighlightings();
		
		setDescription(PreferencesMessages.getString(PREFIX + "description")); //$NON-NLS-1$
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());

		fKeys= createOverlayStoreKeys();
		fOverlayStore= new OverlayPreferenceStore(getPreferenceStore(), fKeys);
	}
	
	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {
		
		ArrayList overlayKeys= new ArrayList();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED));

		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++) {
			SemanticHighlighting semanticHighlighting= fSemanticHighlightings[i];
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, SemanticHighlightings.getColorPreferenceKey(semanticHighlighting)));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting)));
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

	private void handleSemanticColorListSelection() {	
		int i= fSemanticColorList.getSelectionIndex();
		RGB rgb= PreferenceConverter.getColor(fOverlayStore, SemanticHighlightings.getColorPreferenceKey(fSemanticHighlightings[i]));
		fSemanticForegroundColorEditor.setColorValue(rgb);		
		fSemanticBoldCheckBox.setSelection(fOverlayStore.getBoolean(SemanticHighlightings.getBoldPreferenceKey(fSemanticHighlightings[i])));
	}

	private Control createSemanticPage(Composite parent) {
		
		Composite colorComposite= new Composite(parent, SWT.NULL);
		colorComposite.setLayout(new GridLayout());

		fSemanticEnableCheckBox= addCheckBox(colorComposite, PreferencesMessages.getString(PREFIX + "option"), PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED, 0); //$NON-NLS-1$
		fSemanticEnableCheckBox.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				updateSemanticControls();
			}
		});		

		Label label= new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString(PREFIX + "foreground")); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite= new Composite(colorComposite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		editorComposite.setLayout(layout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		editorComposite.setLayoutData(gd);		

		fSemanticColorList= new List(editorComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		gd= new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
		gd.heightHint= convertHeightInCharsToPixels(4);
		fSemanticColorList.setLayoutData(gd);
						
		Composite stylesComposite= new Composite(editorComposite, SWT.NONE);
		layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		label= new Label(stylesComposite, SWT.LEFT);
		label.setText(PreferencesMessages.getString(PREFIX + "color")); //$NON-NLS-1$
		gd= new GridData();
		gd.horizontalAlignment= GridData.BEGINNING;
		label.setLayoutData(gd);

		fSemanticForegroundColorEditor= new ColorEditor(stylesComposite);
		Button foregroundColorButton= fSemanticForegroundColorEditor.getButton();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		foregroundColorButton.setLayoutData(gd);
		
		fSemanticBoldCheckBox= new Button(stylesComposite, SWT.CHECK);
		fSemanticBoldCheckBox.setText(PreferencesMessages.getString(PREFIX + "bold")); //$NON-NLS-1$
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment= GridData.BEGINNING;
		gd.horizontalSpan= 2;
		fSemanticBoldCheckBox.setLayoutData(gd);
		
		
		fSemanticColorList.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				handleSemanticColorListSelection();
			}
		});
		
		foregroundColorButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fSemanticColorList.getSelectionIndex();
				String key= SemanticHighlightings.getColorPreferenceKey(fSemanticHighlightings[i]);
				PreferenceConverter.setValue(fOverlayStore, key, fSemanticForegroundColorEditor.getColorValue());
			}
		});

		fSemanticBoldCheckBox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetSelected(SelectionEvent e) {
				int i= fSemanticColorList.getSelectionIndex();
				String key= SemanticHighlightings.getBoldPreferenceKey(fSemanticHighlightings[i]);
				fOverlayStore.setValue(key, fSemanticBoldCheckBox.getSelection());
			}
		});
				
		return colorComposite;
	}
	
	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		
		fOverlayStore.load();
		fOverlayStore.start();
		
		Control page= createSemanticPage(parent);
		
		initialize();
		
		Dialog.applyDialogFont(page);
		return page;
	}
	
	private void initialize() {
		
		initializeFields();
		
		for (int i= 0, n= fSemanticHighlightings.length; i < n; i++)
			fSemanticColorList.add(fSemanticHighlightings[i].getDisplayName());
		fSemanticColorList.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (fSemanticColorList != null && !fSemanticColorList.isDisposed()) {
					fSemanticColorList.select(0);
					handleSemanticColorListSelection();
				}
			}
		});
		
	}
	
	private void initializeFields() {
		
		Iterator e= fCheckBoxes.keySet().iterator();
		while (e.hasNext()) {
			Button b= (Button) e.next();
			String key= (String) fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
		
		updateSemanticControls();
	}

	private void updateSemanticControls() {
		boolean enabled= fSemanticEnableCheckBox.getSelection();
		fSemanticColorList.setEnabled(enabled);
		fSemanticForegroundColorEditor.getButton().setEnabled(enabled);
		fSemanticBoldCheckBox.setEnabled(enabled);
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

		handleSemanticColorListSelection();

		super.performDefaults();
	}
	
	/*
	 * @see DialogPage#dispose()
	 */
	public void dispose() {
		
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
	
}
