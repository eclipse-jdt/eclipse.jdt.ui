/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

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
		overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED));
		return overlayKeys.toArray(new OverlayKey[overlayKeys.size()]);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.folding.IJavaFoldingPreferences#createControl(org.eclipse.swt.widgets.Group)
	 */
	@Override
	public Control createControl(Composite composite) {
		fOverlayStore.load();
		fOverlayStore.start();

		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout(1, false);
		layout.verticalSpacing= 10;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group initialFoldGroup = new Group(inner, SWT.NONE);
		GridLayout initialFoldLayout = new GridLayout(1, false);
		initialFoldLayout.marginWidth = 10;
		initialFoldLayout.marginHeight = 10;
		initialFoldGroup.setLayout(initialFoldLayout);
		initialFoldGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		initialFoldGroup.setText(FoldingMessages.DefaultJavaFoldingPreferenceBlock_title);

		addCheckBox(initialFoldGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_comments, PreferenceConstants.EDITOR_FOLDING_JAVADOC, 0);
		addCheckBox(initialFoldGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_headers, PreferenceConstants.EDITOR_FOLDING_HEADERS, 0);
		addCheckBox(initialFoldGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_innerTypes, PreferenceConstants.EDITOR_FOLDING_INNERTYPES, 0);
		addCheckBox(initialFoldGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_methods, PreferenceConstants.EDITOR_FOLDING_METHODS, 0);
		addCheckBox(initialFoldGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_imports, PreferenceConstants.EDITOR_FOLDING_IMPORTS, 0);

		Group extendedFoldingGroup= new Group(inner, SWT.NONE);
		GridLayout extendedFoldingLayout= new GridLayout(1, false);
		extendedFoldingLayout.marginWidth= 10;
		extendedFoldingLayout.marginHeight= 10;
		extendedFoldingGroup.setLayout(extendedFoldingLayout);
		extendedFoldingGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		extendedFoldingGroup.setText(FoldingMessages.DefaultJavaFoldingPreferenceBlock_New_Setting_Title);
		Label label= new Label(extendedFoldingGroup, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 300;
		label.setLayoutData(gd);
		label.setText(FoldingMessages.DefaultJavaFoldingPreferenceBlock_Warning_New_Feature);
		addCheckBox(extendedFoldingGroup, FoldingMessages.DefaultJavaFoldingPreferenceBlock_New, PreferenceConstants.EDITOR_NEW_FOLDING_ENABLED, 0);
		return inner;
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

	private void initializeFields() {
		Iterator<Button> it= fCheckBoxes.keySet().iterator();
		while (it.hasNext()) {
			Button b= it.next();
			String key= fCheckBoxes.get(b);
			b.setSelection(fOverlayStore.getBoolean(key));
		}
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
