/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.preferences;

import static org.eclipse.jdt.bcoview.BytecodeOutlinePlugin.getDefault;
import static org.eclipse.jdt.bcoview.BytecodeOutlinePlugin.getResourceString;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.DIFF_EXPAND_STACKMAP;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.DIFF_SHOW_ASMIFIER_CODE;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.DIFF_SHOW_LINE_INFO;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.DIFF_SHOW_STACKMAP;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.DIFF_SHOW_VARIABLES;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.EXPAND_STACKMAP;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.LINK_REF_VIEW_TO_EDITOR;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.LINK_VIEW_TO_EDITOR;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_ANALYZER;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_ASMIFIER_CODE;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_HEX_VALUES;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_LINE_INFO;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_ONLY_SELECTED_ELEMENT;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_RAW_BYTECODE;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_STACKMAP;
import static org.eclipse.jdt.bcoview.preferences.BCOConstants.SHOW_VARIABLES;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class BCOPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public BCOPreferencePage() {
		super(GRID);
		setPreferenceStore(getDefault().getPreferenceStore());
		setDescription(getResourceString("BCOPreferencePage.description")); //$NON-NLS-1$
	}

	@Override
	public void createFieldEditors() {
		Composite fieldEditorParent = getFieldEditorParent();

		TabFolder tabFolder = new TabFolder(fieldEditorParent, SWT.TOP);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem tabPrefs = new TabItem(tabFolder, SWT.NONE);
		tabPrefs.setText(getResourceString("BCOPreferencePage.defaultsGroup")); //$NON-NLS-1$

		TabItem tabCompare = new TabItem(tabFolder, SWT.NONE);
		tabCompare.setText(getResourceString("BCOPreferencePage.compareGroup")); //$NON-NLS-1$

		Group viewGroup = new Group(tabFolder, SWT.NONE);
		viewGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabPrefs.setControl(viewGroup);

		Group compareGroup = new Group(tabFolder, SWT.NONE);
		compareGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabCompare.setControl(compareGroup);

		addField(new BooleanFieldEditor(LINK_VIEW_TO_EDITOR, getResourceString("BCOPreferencePage.linkViewToEditor"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(LINK_REF_VIEW_TO_EDITOR, getResourceString("BCOPreferencePage.linkRefViewToEditor"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_ONLY_SELECTED_ELEMENT, getResourceString("BCOPreferencePage.showOnlySelected"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_RAW_BYTECODE, getResourceString("BCOPreferencePage.showRawBytecode"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_ASMIFIER_CODE, getResourceString("BCOPreferencePage.showAsmifierCode"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_ANALYZER, getResourceString("BCOPreferencePage.showAnalyzer"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_LINE_INFO, getResourceString("BCOPreferencePage.showLineInfo"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_VARIABLES, getResourceString("BCOPreferencePage.showVariables"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_STACKMAP, getResourceString("BCOPreferencePage.showStackMap"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(SHOW_HEX_VALUES, getResourceString("BCOPreferencePage.showHexValues"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(EXPAND_STACKMAP, getResourceString("BCOPreferencePage.expandStackMap"), viewGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(DIFF_SHOW_ASMIFIER_CODE, getResourceString("BCOPreferencePage.diffShowAsmifierCode"), compareGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(DIFF_SHOW_LINE_INFO, getResourceString("BCOPreferencePage.diffShowLineInfo"), compareGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(DIFF_SHOW_VARIABLES, getResourceString("BCOPreferencePage.diffShowVariables"), compareGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(DIFF_SHOW_STACKMAP, getResourceString("BCOPreferencePage.diffShowStackMap"), compareGroup)); //$NON-NLS-1$
		addField(new BooleanFieldEditor(DIFF_EXPAND_STACKMAP, getResourceString("BCOPreferencePage.diffExpandStackMap"), compareGroup)); //$NON-NLS-1$
	}

	@Override
	public void init(IWorkbench workbench) {
		//
	}

}
