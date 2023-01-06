/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
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

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the Preferences dialog.
 * By subclassing <samp>FieldEditorPreferencePage</samp>, we can use the field support
 * built into JFace that allows us to create a page that is small and knows how to save,
 * restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store
 * that belongs to the main plug-in class. That way, preferences can be accessed directly
 * via the preference store.
 */
public class BCOPreferencePage extends FieldEditorPreferencePage
    implements
        IWorkbenchPreferencePage {

    public BCOPreferencePage() {
        super(GRID);
        setPreferenceStore(BytecodeOutlinePlugin.getDefault()
            .getPreferenceStore());
        setDescription(BytecodeOutlinePlugin
            .getResourceString("BCOPreferencePage.description"));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    @Override
    protected void adjustGridLayout() {
        super.adjustGridLayout();
    }

    /*
     * Creates the field editors. Field editors are abstractions of the common GUI blocks
     * needed to manipulate various types of preferences. Each field editor knows how to
     * save and restore itself.
     */
    @Override
    public void createFieldEditors() {

        Composite fieldEditorParent = getFieldEditorParent();

        TabFolder tabFolder = new TabFolder(fieldEditorParent, SWT.TOP);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabItem tabPrefs = new TabItem(tabFolder, SWT.NONE);
        tabPrefs.setText(BytecodeOutlinePlugin
          .getResourceString("BCOPreferencePage.defaultsGroup"));

        TabItem tabCompare = new TabItem(tabFolder, SWT.NONE);
        tabCompare.setText(BytecodeOutlinePlugin
            .getResourceString("BCOPreferencePage.compareGroup"));

        Group viewGroup = new Group(tabFolder, SWT.NONE);
        viewGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        tabPrefs.setControl(viewGroup);

        Group compareGroup = new Group(tabFolder, SWT.NONE);
        compareGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        tabCompare.setControl(compareGroup);

        addField(new BooleanFieldEditor(
            BCOConstants.LINK_VIEW_TO_EDITOR, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.linkViewToEditor"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.LINK_REF_VIEW_TO_EDITOR, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.linkRefViewToEditor"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_ONLY_SELECTED_ELEMENT, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showOnlySelected"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_RAW_BYTECODE, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showRawBytecode"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_ASMIFIER_CODE, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showAsmifierCode"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_ANALYZER, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showAnalyzer"), viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_LINE_INFO, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showLineInfo"), viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_VARIABLES, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showVariables"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_STACKMAP, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showStackMap"), viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.SHOW_HEX_VALUES, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.showHexValues"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.EXPAND_STACKMAP, BytecodeOutlinePlugin
            .getResourceString("BCOPreferencePage.expandStackMap"),
            viewGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.DIFF_SHOW_ASMIFIER_CODE, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.diffShowAsmifierCode"),
            compareGroup));
        addField(new BooleanFieldEditor(
            BCOConstants.DIFF_SHOW_LINE_INFO, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.diffShowLineInfo"),
            compareGroup));
        addField(new BooleanFieldEditor(
            BCOConstants.DIFF_SHOW_VARIABLES, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.diffShowVariables"),
            compareGroup));
        addField(new BooleanFieldEditor(
            BCOConstants.DIFF_SHOW_STACKMAP, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.diffShowStackMap"),
            compareGroup));

        addField(new BooleanFieldEditor(
            BCOConstants.DIFF_EXPAND_STACKMAP, BytecodeOutlinePlugin
                .getResourceString("BCOPreferencePage.diffExpandStackMap"),
            compareGroup));

        // addField( new BooleanFieldEditor( BCOConstants.RECALCULATE_STACKMAP,
        // BytecodeOutlinePlugin.getResourceString(
        // "BCOPreferencePage.recalculateStackMap" ),
        // getFieldEditorParent() ) );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
        //
    }

}
