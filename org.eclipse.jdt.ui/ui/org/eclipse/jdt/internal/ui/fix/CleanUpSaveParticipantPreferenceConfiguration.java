/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;

import org.eclipse.jface.preference.IPreferencePageContainer;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.AbstractSaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.formatter.JavaPreview;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.util.PixelConverter;

/**
 * Preference configuration UI for the clean up save participant.
 * 
 * @since 3.3
 */
public class CleanUpSaveParticipantPreferenceConfiguration extends AbstractSaveParticipantPreferenceConfiguration {
	
	private static class PreferenceModifyBlock extends ModifyDialogTabPage {
		
		/**
		 * Constant array for boolean selection
		 */
		private static String[] FALSE_TRUE= {CleanUpConstants.FALSE, CleanUpConstants.TRUE};
		
		private final ArrayList fPreferences;
		private final IPreferencePageContainer fContainer;
		private final Map fSettings;
		private final ArrayList fJava50Preferences;
		private IJavaProject fJavaProject;
		
		public PreferenceModifyBlock(Map settings, IPreferencePageContainer container) {
			super(null, settings);
			fSettings= settings;
			
			fContainer= container;
			fPreferences= new ArrayList();
			fJava50Preferences= new ArrayList();
		}
		
		public void setProject(IJavaProject javaProject) {
			fJavaProject= javaProject;
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected JavaPreview doCreateJavaPreview(Composite parent) {
			return null;
		}
		
		public Composite createBlockContent(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout gridLayout= new GridLayout(3, false);
			gridLayout.marginHeight= 0;
			gridLayout.marginWidth= 0;
			composite.setLayout(gridLayout);
			
			fPixelConverter= new PixelConverter(composite);
			doCreatePreferences(composite, 3);
			
			return composite;
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected void doCreatePreferences(Composite composite, int numColumns) {
			createFormattingGroup(composite, numColumns);
			createImportsGroup(composite, numColumns);
			createDeclarationsGroup(composite, numColumns);
			createExpressionsGroup(composite, numColumns);
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected CheckboxPreference createCheckboxPref(Composite composite, int numColumns, String name, String key, String[] values) {
			CheckboxPreference result= super.createCheckboxPref(composite, numColumns, name, key, values);
			fPreferences.add(result);
			return result;
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected RadioPreference createRadioPref(Composite composite, int numColumns, String name, String key, String[] values) {
			RadioPreference result= super.createRadioPref(composite, numColumns, name, key, values);
			fPreferences.add(result);
			return result;
		}
		
		/**
		 * {@inheritDoc}
		 */
		protected void doUpdatePreview() {}
		
		/**
		 * {@inheritDoc}
		 */
		protected void initializePage() {
			for (int i= 0; i < fPreferences.size(); i++) {
				ButtonPreference pref= (ButtonPreference)fPreferences.get(i);
				pref.setChecked(CleanUpConstants.TRUE.equals(fSettings.get(pref.getKey())));
			}
			
			if (fJavaProject != null && !JavaModelUtil.is50OrHigher(fJavaProject)) {
				for (int i= 0; i < fJava50Preferences.size(); i++) {
					Preference pref= (Preference)fJava50Preferences.get(i);
					pref.setEnabled(false);
				}
			}
		}
		
		private void createFormattingGroup(Composite parent, int numColumns) {
			Group formatGroup= createGroup(numColumns, parent, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_Formatter_Group);
			
			createCheckboxPref(formatGroup, numColumns - 1, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_FormatSource_Checkbox, CleanUpConstants.FORMAT_SOURCE_CODE, FALSE_TRUE);
			createConfigureLink(formatGroup, CodeFormatterPreferencePage.PREF_ID, CodeFormatterPreferencePage.PROP_ID);
			
			final CheckboxPreference formatJavadocPref= createCheckboxPref(formatGroup, numColumns - 1, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_FormatJavaDoc_Checkbox, CleanUpConstants.FORMAT_COMMENT, FALSE_TRUE);
			synchronizeOption(CleanUpConstants.FORMAT_JAVADOC, formatJavadocPref);
			createConfigureLink(formatGroup, CodeFormatterPreferencePage.PREF_ID, CodeFormatterPreferencePage.PROP_ID);
			
			final CheckboxPreference whiteSpace= createCheckboxPref(formatGroup, numColumns, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_RemoveTrailingWhitesapce_Checkbox, CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES, FALSE_TRUE);
			
			Composite whiteSpaceGroup= new Composite(formatGroup, SWT.NONE);
			GridData gridData= new GridData(SWT.FILL, SWT.TOP, true, false);
			gridData.horizontalSpan= numColumns;
			gridData.horizontalIndent= fPixelConverter.convertWidthInCharsToPixels(4);
			whiteSpaceGroup.setLayoutData(gridData);
			GridLayout layout= new GridLayout(2, false);
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			whiteSpaceGroup.setLayout(layout);
			
			final RadioPreference allLines= createRadioPref(whiteSpaceGroup, 1, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_AllLines_Radio, CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL, FALSE_TRUE);
			final RadioPreference ignoreEmptyPref= createRadioPref(whiteSpaceGroup, 1, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_IgnoreEmpty_Radio, CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_IGNORE_EMPTY, FALSE_TRUE);
			
			whiteSpace.addObserver(new Observer() {
				public void update(Observable o, Object arg) {
					allLines.setEnabled(whiteSpace.getChecked());
					ignoreEmptyPref.setEnabled(whiteSpace.getChecked());
				}
			});
			
			allLines.setEnabled(whiteSpace.getChecked());
			ignoreEmptyPref.setEnabled(whiteSpace.getChecked());
		}
		
		private void createImportsGroup(Composite parent, int numColumns) {
			Group importsGroup= createGroup(numColumns, parent, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_Imports_Group);
			createCheckboxPref(importsGroup, numColumns - 1, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_OrganizeImports_Checkbox, CleanUpConstants.ORGANIZE_IMPORTS, FALSE_TRUE);
			
			createConfigureLink(importsGroup, ImportOrganizePreferencePage.PREF_ID, ImportOrganizePreferencePage.PROP_ID);
		}
		
		private void createDeclarationsGroup(Composite parent, int numColumns) {
			Group declarationGroup= createGroup(numColumns, parent, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_Declarations_Group);
			
			final CheckboxPreference useFinalPref= createCheckboxPref(declarationGroup, numColumns, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_AddFinal_Checkbox, CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL, FALSE_TRUE);
			synchronizeOption(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS, useFinalPref);
			
			final CheckboxPreference annotationsPref= createCheckboxPref(declarationGroup, numColumns, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_AddAnnotations_Checkbox, CleanUpConstants.ADD_MISSING_ANNOTATIONS, FALSE_TRUE);
	    	
			Composite annotationsGroup= new Composite(declarationGroup, SWT.NONE);
			GridData gridData= new GridData(SWT.FILL, SWT.TOP, true, false);
			gridData.horizontalSpan= numColumns;
			gridData.horizontalIndent= fPixelConverter.convertWidthInCharsToPixels(4);
			annotationsGroup.setLayoutData(gridData);
			GridLayout layout= new GridLayout(2, false);
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			annotationsGroup.setLayout(layout);
			
			final CheckboxPreference overridePref= createCheckboxPref(annotationsGroup, 1, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_Override_Checkbox, CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, FALSE_TRUE);
			final CheckboxPreference deprecatedPref= createCheckboxPref(annotationsGroup, 1, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_Deprecated_Checkbox, CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, FALSE_TRUE);
			
	    	annotationsPref.addObserver( new Observer() {
	    		public void update(Observable o, Object arg) {
	    			overridePref.setEnabled(annotationsPref.getChecked());
	    			deprecatedPref.setEnabled(annotationsPref.getChecked());
	    		}
	    	});

			overridePref.setEnabled(annotationsPref.getChecked());
			deprecatedPref.setEnabled(annotationsPref.getChecked());
			
			fJava50Preferences.add(deprecatedPref);
			fJava50Preferences.add(overridePref);
		}
		
		private void createExpressionsGroup(Composite composite, int numColumns) {
	        Group expressionsGroup= createGroup(numColumns, composite, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_Expressions_Group);
			
			final CheckboxPreference thisFieldPref= createCheckboxPref(expressionsGroup, numColumns, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_AddThis_Checkbox, CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS, FALSE_TRUE);
			synchronizeOption(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_ALWAYS, thisFieldPref);
			
	    	final CheckboxPreference useParenthesesPref= createCheckboxPref(expressionsGroup, numColumns, SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_AddParanoicalParanthesis_Checkbox, CleanUpConstants.EXPRESSIONS_USE_PARENTHESES_ALWAYS, FALSE_TRUE);
			synchronizeOption(CleanUpConstants.EXPRESSIONS_USE_PARENTHESES, useParenthesesPref);
        }
		
		private void createConfigureLink(final Composite parent, final String preferenceId, final String propertyId) {
			Link link= new Link(parent, SWT.NONE);
			link.setText(SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_SaveActionPreferencePage_Configure_Link);
			link.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			link.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (fContainer instanceof IWorkbenchPreferenceContainer) {
						IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer)fContainer;
						if (fJavaProject != null) {
							container.openPage(propertyId, null);
						} else {
							container.openPage(preferenceId, null);
						}
					} else {
						PreferencesUtil.createPreferenceDialogOn(parent.getShell(), preferenceId, null, null);
					}
				}
			});
		}
		
		/**
		 * keeps the option with key name synchronized with the checked state of
		 * preference
		 */
		private void synchronizeOption(final String keyName, final ButtonPreference preference) {
			preference.addObserver(new Observer() {
				public void update(Observable o, Object arg) {
					if (preference.getChecked()) {
						fSettings.put(keyName, CleanUpConstants.TRUE);
					} else {
						fSettings.put(keyName, CleanUpConstants.FALSE);
					}
				}
			});
		}
	}
	
	private IScopeContext fContext;
	private Map fSettings;
	private PreferenceModifyBlock fBlock;
	
	/**
	 * {@inheritDoc}
	 */
	public Control createConfigControl(Composite parent, IPreferencePageContainer container) {
		fSettings= CleanUpConstants.getSaveParticipantSettings();
		fBlock= new PreferenceModifyBlock(fSettings, container);
		return fBlock.createBlockContent(parent);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initialize(final IScopeContext context, IAdaptable element) {		
		fContext= context;
		copyMap(CleanUpPreferenceUtil.loadSaveParticipantOptions(context), fSettings);
		if (element != null) {
			IProject project= (IProject)element.getAdapter(IProject.class);
			if (project != null) {
				IJavaProject javaProject= JavaCore.create(project);
				if (javaProject != null && javaProject.exists()) {
					fBlock.setProject(javaProject);
				}
			}
		}
		fBlock.initializePage();
		
		super.initialize(context, element);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		super.dispose();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void performDefaults() {
		super.performDefaults();
		
		copyMap(CleanUpConstants.getSaveParticipantSettings(), fSettings);
		fBlock.initializePage();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void performOk() {
		super.performOk();
		
		if (!ProjectScope.SCOPE.equals(fContext.getName()) || hasSettingsInScope(fContext))
			CleanUpPreferenceUtil.saveSaveParticipantOptions(fContext, fSettings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void enableProjectSettings() {
		super.enableProjectSettings();
		
		CleanUpPreferenceUtil.saveSaveParticipantOptions(fContext, fSettings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void disableProjectSettings() {
		super.disableProjectSettings();
		
		IEclipsePreferences node= fContext.getNode(JavaUI.ID_PLUGIN);
		
		Map settings= CleanUpConstants.getSaveParticipantSettings();
		for (Iterator iterator= settings.keySet().iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			node.remove(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + key);
		}
	}
	
	/**
     * {@inheritDoc}
     */
    protected String getPostSaveListenerId() {
	    return CleanUpPostSaveListener.POSTSAVELISTENER_ID;
    }

	/**
     * {@inheritDoc}
     */
    protected String getPostSaveListenerName() {
	    return SaveParticipantMessages.CleanUpSaveParticipantPreferenceConfiguration_CleanUpActionsTopNodeName_Checkbox;
    }
    
	private void copyMap(Map source, Map target) {
		for (Iterator iterator= source.keySet().iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			target.put(key, source.get(key));
		}
	}
}