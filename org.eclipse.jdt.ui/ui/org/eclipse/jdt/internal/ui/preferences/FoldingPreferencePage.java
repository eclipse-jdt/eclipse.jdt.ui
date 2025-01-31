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

package org.eclipse.jdt.internal.ui.preferences;


import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * The page for setting the editor options.
 */
public final class FoldingPreferencePage extends AbstractConfigurationBlockPreferenceAndPropertyPage {

	public static final String PROPERTY_PAGE_ID= "org.eclipse.jdt.ui.propertyPages.FoldingPreferencePage"; //$NON-NLS-1$
	public static final String PREFERENCE_PAGE_ID= "org.eclipse.jdt.ui.preferences.FoldingPreferencePage"; //$NON-NLS-1$
	private OverlayPreferenceStore fOverlayStore;


	public FoldingPreferencePage() {
		setDescription(PreferencesMessages.JavaEditorPreferencePage_folding_title);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.preferences.AbstractConfigurationBlockPreferenceAndPropertyPage#getHelpId()
	 */
	@Override
	protected String getHelpId() {
		return IJavaHelpContextIds.JAVA_EDITOR_PREFERENCE_PAGE;
	}

	@Override
	protected IPreferenceAndPropertyConfigurationBlock createConfigurationBlock(IScopeContext context) {
		ScopedPreferenceStore scopedStore= new ScopedPreferenceStore(context, JavaUI.ID_PLUGIN);
		fOverlayStore= new OverlayPreferenceStore(
				scopedStore,
				new OverlayPreferenceStore.OverlayKey[] {});
		FoldingConfigurationBlock foldingConfigurationBlock= new FoldingConfigurationBlock(fOverlayStore, context, isProjectPreferencePage());
		fOverlayStore.load();
		fOverlayStore.start();
		return foldingConfigurationBlock;
	}

	@Override
	protected boolean hasProjectSpecificOptions(IProject project) {
		return JavaPlugin.getDefault().getFoldingStructureProviderRegistry().hasProjectSpecificOptions(new ProjectScope(project));
	}

	@Override
	protected String getPreferencePageID() {
		return PREFERENCE_PAGE_ID;
	}

	@Override
	protected String getPropertyPageID() {
		return PROPERTY_PAGE_ID;
	}

	@Override
	public boolean performOk() {
		boolean result= super.performOk();
		fOverlayStore.propagate();
		return result;
	}

	@Override
	public void performDefaults() {
		fOverlayStore.loadDefaults();
		super.performDefaults();

	}

	@Override
	public void dispose() {
		super.dispose();
		fOverlayStore.stop();
	}

	public static IPreferenceStore getFoldingPreferenceStore(JavaEditor editor) {
		IJavaProject project= EditorUtility.getJavaProject(editor);
		IPreferenceStore scopedPreferenceStore = JavaPlugin.getDefault().getPreferenceStore();
		if (project != null) {
			ProjectScope projectScope= new ProjectScope(project.getProject());
			if(projectScope.getNode(JavaUI.ID_PLUGIN).getBoolean(PreferenceConstants.EDITOR_FOLDING_PROJECT_SPECIFIC_SETTINGS_ENABLED, false)) {
				scopedPreferenceStore= new ScopedPreferenceStore(projectScope, JavaUI.ID_PLUGIN);
			}
		}
		return scopedPreferenceStore;
	}
}
