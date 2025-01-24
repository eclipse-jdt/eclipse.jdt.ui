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


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
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
	private IScopeContext fContext;


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
		this.fContext= context;
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
		// When a boolean preference is put to its default value in OverlayPreferenceStore, it is removed
		// To ensure compatibility with ChainedPreferenceStore, it must be set manually
		IEclipsePreferences node= fContext.getNode(JavaUI.ID_PLUGIN);
		if(isProjectSpecificPreferencesEnabled(node)) {
			node.putBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED, fOverlayStore.getBoolean(PreferenceConstants.EDITOR_FOLDING_ENABLED));
		}
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
		if (project != null) {
			ProjectScope projectScope= new ProjectScope(project.getProject());
			if (isProjectSpecificPreferencesEnabled(projectScope.getNode(JavaUI.ID_PLUGIN))) {
				return getProjectPreferenceStore(projectScope);
			}
		}
		return getDefaultPreferenceStore();
	}

	private static boolean isProjectSpecificPreferencesEnabled(IEclipsePreferences node) {
		return node.getBoolean(PreferenceConstants.EDITOR_FOLDING_PROJECT_SPECIFIC_SETTINGS_ENABLED, false);
	}

	public static List<IPreferenceStore> getAllFoldingPreferenceStores(JavaEditor editor) {
		List<IPreferenceStore> preferenceStores= new ArrayList<>();
		preferenceStores.add(getDefaultPreferenceStore());
		IJavaProject project= EditorUtility.getJavaProject(editor);
		if (project != null) {
			ProjectScope projectScope= new ProjectScope(project.getProject());
			preferenceStores.add(getProjectPreferenceStore(projectScope));
		}
		return preferenceStores;
	}

	private static ScopedPreferenceStore getProjectPreferenceStore(ProjectScope projectScope) {
		return new ScopedPreferenceStore(projectScope, JavaUI.ID_PLUGIN);
	}

	private static IPreferenceStore getDefaultPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
}
