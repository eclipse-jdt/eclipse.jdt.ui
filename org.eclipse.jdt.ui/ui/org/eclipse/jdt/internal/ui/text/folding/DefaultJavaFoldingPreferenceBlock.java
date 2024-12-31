/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.folding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.folding.IScopedJavaFoldingPreferenceBlock;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore;
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey;


/**
 * Java default folding preferences.
 *
 * @since 3.0
 */
public class DefaultJavaFoldingPreferenceBlock implements IScopedJavaFoldingPreferenceBlock {

	private IPreferenceStore fStore;
	private OverlayPreferenceStore fOverlayStore;
	private OverlayKey[] fKeys;
	private Map<Button, String> fCheckBoxes= new HashMap<>();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fOverlayStore.setValue(fCheckBoxes.get(button), button.getSelection());
		}
	};
	private Map<Text, String> fStringInputs= new HashMap<>();
	private ModifyListener fModifyListener = e -> {
		Text text = (Text)e.widget;
		fOverlayStore.setValue(fStringInputs.get(text), text.getText());
	};



	public DefaultJavaFoldingPreferenceBlock() {
		fStore= JavaPlugin.getDefault().getPreferenceStore();
		fKeys= createKeys();
		fOverlayStore= new OverlayPreferenceStore(fStore, fKeys);
	}

	@Override
	public void setScopeContext(IScopeContext context) {
		if(context == null) {
			fStore = JavaPlugin.getDefault().getPreferenceStore();
		} else {
			fStore= new ScopedPreferenceStore(context, JavaUI.ID_PLUGIN);
		}
		fOverlayStore= new OverlayPreferenceStore(fStore, fKeys);
	}

	private OverlayKey[] createKeys() {
		ArrayList<OverlayKey> overlayKeys= new ArrayList<>();

		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_JAVADOC));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_INNERTYPES));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_METHODS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_IMPORTS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_HEADERS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START));
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END));

		return overlayKeys.toArray(new OverlayKey[overlayKeys.size()]);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.folding.IJavaFoldingPreferences#createControl(org.eclipse.swt.widgets.Group)
	 */
	@Override
	public Control createControl(Composite composite) {
		fOverlayStore.load();
		fOverlayStore.start();

		GridLayout layout= new GridLayout(1, true);
		layout.verticalSpacing= 3;
		layout.marginWidth= 0;
		composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		Composite outer= new Composite(composite, SWT.NONE);
		outer.setLayout(layout);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group initialFoldingGroup= new Group(outer, SWT.NONE);
		initialFoldingGroup.setLayout(layout);
		initialFoldingGroup.setText(FoldingMessages.DefaultJavaFoldingPreferenceBlock_title);
		initialFoldingGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		addCheckBox(initialFoldingGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_comments, PreferenceConstants.EDITOR_FOLDING_JAVADOC, 0);
		addCheckBox(initialFoldingGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_headers, PreferenceConstants.EDITOR_FOLDING_HEADERS, 0);
		addCheckBox(initialFoldingGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_innerTypes, PreferenceConstants.EDITOR_FOLDING_INNERTYPES, 0);
		addCheckBox(initialFoldingGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_methods, PreferenceConstants.EDITOR_FOLDING_METHODS, 0);
		addCheckBox(initialFoldingGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_imports, PreferenceConstants.EDITOR_FOLDING_IMPORTS, 0);
		addCheckBox(initialFoldingGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_customRegions, PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGIONS, 0);

		Group customRegionGroup= new Group(outer, SWT.NONE);
		customRegionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout customRegionLayout= new GridLayout(2, false);
		customRegionGroup.setLayout(customRegionLayout);
		customRegionGroup.setText(FoldingMessages.DefaultJavaFoldingPreferenceBlock_custom_region_title);
		addStringInput(customRegionGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_CustomRegionStart, PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_START);
		addStringInput(customRegionGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_CustomRegionEnd, PreferenceConstants.EDITOR_FOLDING_CUSTOM_REGION_END);

		return outer;
	}

	private Button addCheckBox(Composite parent, String label, String key, int indentation) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);

		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 1;
		gd.grabExcessVerticalSpace= false;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);

		fCheckBoxes.put(checkBox, key);

		return checkBox;
	}

	private void addStringInput(Composite parent, String label, String key) {
		Label labelElement = new Label(parent, SWT.LEFT);
		labelElement.setText(label);
		GridData labelGridData= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		labelGridData.horizontalSpan= 1;
		labelGridData.grabExcessVerticalSpace= false;
		labelElement.setLayoutData(labelGridData);

		Text textInput = new Text(parent, SWT.SINGLE | SWT.BORDER);
		textInput.setText(label);
		textInput.addModifyListener(fModifyListener);

		GridData textGridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		textGridData.horizontalSpan= 1;
		textGridData.grabExcessVerticalSpace= true;
		textInput.setLayoutData(textGridData);

		fStringInputs.put(textInput, key);
	}

	private void initializeFields() {
		fCheckBoxes.forEach((b, key) -> b.setSelection(fOverlayStore.getBoolean(key)));
		fStringInputs.forEach((text, key) -> text.setText(fOverlayStore.getString(key)));
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.folding.AbstractJavaFoldingPreferences#performOk()
	 */
	@Override
	public void performOk() {
		fOverlayStore.propagate();
	}


	/*
	 * @see org.eclipse.jdt.internal.ui.text.folding.AbstractJavaFoldingPreferences#initialize()
	 */
	@Override
	public void initialize() {
		initializeFields();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.folding.AbstractJavaFoldingPreferences#performDefaults()
	 */
	@Override
	public void performDefaults() {
		fOverlayStore.loadDefaults();
		initializeFields();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.folding.AbstractJavaFoldingPreferences#dispose()
	 */
	@Override
	public void dispose() {
		fOverlayStore.stop();
	}
}
