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

import org.eclipse.core.resources.IProject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;

/**
  */
public class JavadocProblemsConfigurationBlock extends OptionsConfigurationBlock {

	private static final Key PREF_JAVADOC_SUPPORT= getJDTCoreKey(JavaCore.COMPILER_DOC_COMMENT_SUPPORT);

	private static final Key PREF_PB_INVALID_JAVADOC= getJDTCoreKey(JavaCore.COMPILER_PB_INVALID_JAVADOC);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS= getJDTCoreKey(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF= getJDTCoreKey(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS__NOT_VISIBLE_REF);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF= getJDTCoreKey(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS__DEPRECATED_REF);
	private static final Key PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY= getJDTCoreKey(JavaCore.COMPILER_PB_INVALID_JAVADOC_TAGS_VISIBILITY);

	private static final Key PREF_PB_MISSING_JAVADOC_TAGS= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS);
	private static final Key PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_VISIBILITY);
	private static final Key PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_OVERRIDING);

	private static final Key PREF_PB_MISSING_JAVADOC_COMMENTS= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS);
	private static final Key PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY);
	private static final Key PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING);
	

	// values
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;
	
	private static final String PUBLIC= JavaCore.PUBLIC;
	private static final String PROTECTED= JavaCore.PROTECTED;
	private static final String DEFAULT= JavaCore.DEFAULT;
	private static final String PRIVATE= JavaCore.PRIVATE;
	
	private PixelConverter fPixelConverter;
	private Composite fJavadocComposite;


	public JavadocProblemsConfigurationBlock(IStatusChangeListener context, IProject project) {
		super(context, project, getKeys());
	}
	
	private static Key[] getKeys() {
		Key[] keys= new Key[] {
				PREF_JAVADOC_SUPPORT,
				PREF_PB_INVALID_JAVADOC, PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, PREF_PB_INVALID_JAVADOC_TAGS,
				PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY,
				PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF, PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF,
				PREF_PB_MISSING_JAVADOC_TAGS, PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING,
				PREF_PB_MISSING_JAVADOC_COMMENTS, PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING,
			};
		return keys;
	}
	
	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite javadocComposite= createJavadocTabContent(parent);
		
		validateSettings(null, null, null);
	
		return javadocComposite;
	}
	
	
	private Composite createJavadocTabContent(Composite folder) {
		String[] errorWarningIgnore= new String[] { ERROR, WARNING, IGNORE };
		
		String[] errorWarningIgnoreLabels= new String[] {
				PreferencesMessages.getString("JavadocProblemsConfigurationBlock.error"),  //$NON-NLS-1$
				PreferencesMessages.getString("JavadocProblemsConfigurationBlock.warning"), //$NON-NLS-1$
				PreferencesMessages.getString("JavadocProblemsConfigurationBlock.ignore") //$NON-NLS-1$
		};
		
		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		
		String[] visibilities= new String[] { PUBLIC, PROTECTED, DEFAULT, PRIVATE  };
		
		String[] visibilitiesLabels= new String[] {
				PreferencesMessages.getString("JavadocProblemsConfigurationBlock.public"), //$NON-NLS-1$
				PreferencesMessages.getString("JavadocProblemsConfigurationBlock.protected"), //$NON-NLS-1$
				PreferencesMessages.getString("JavadocProblemsConfigurationBlock.default"), //$NON-NLS-1$
				PreferencesMessages.getString("JavadocProblemsConfigurationBlock.private") //$NON-NLS-1$
		};
		int nColumns= 3;
				

		final ScrolledPageContent sc1 = new ScrolledPageContent(folder);
		
		Composite outer= sc1.getBody();
		
		GridLayout layout = new GridLayout();
		layout.numColumns= nColumns;
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		outer.setLayout(layout);
		
		String label= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_javadoc_support.label"); //$NON-NLS-1$
		addCheckBox(outer, label, PREF_JAVADOC_SUPPORT, enabledDisabled, 0);
		
		layout = new GridLayout();
		layout.numColumns= nColumns;
		layout.marginHeight= 0;
		//layout.marginWidth= 0;
				
		Composite composite= new Composite(outer, SWT.NONE);
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, true));
		
		fJavadocComposite= composite;
		
		Label description= new Label(composite, SWT.WRAP);
		description.setText(PreferencesMessages.getString("JavadocProblemsConfigurationBlock.javadoc.description")); //$NON-NLS-1$
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		//gd.widthHint= fPixelConverter.convertWidthInCharsToPixels(60);
		description.setLayoutData(gd);
			
		int indent= fPixelConverter.convertWidthInCharsToPixels(2);
		
		label = PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_invalid_javadoc.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_INVALID_JAVADOC, errorWarningIgnore, errorWarningIgnoreLabels, 0);
		
		label = PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_invalid_javadoc_tags_visibility.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, visibilities, visibilitiesLabels, indent);

		label= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_invalid_javadoc_tags.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS, enabledDisabled, indent);
		
		label= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_invalid_javadoc_tags_not_visible_ref.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF, enabledDisabled, indent);
		
		label= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_invalid_javadoc_tags_deprecated.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF, enabledDisabled, indent);

		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		
		label = PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_missing_javadoc.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_missing_javadoc_tags_visibility.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, visibilities, visibilitiesLabels, indent);
		
		label= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_missing_javadoc_tags_overriding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING, enabledDisabled, indent);

		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= nColumns;
		
		label = PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_missing_comments.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS, errorWarningIgnore, errorWarningIgnoreLabels, 0);

		label = PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_missing_comments_visibility.label"); //$NON-NLS-1$
		addComboBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, visibilities, visibilitiesLabels, indent);
		
		label= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.pb_missing_comments_overriding.label"); //$NON-NLS-1$
		addCheckBox(composite, label, PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING, enabledDisabled, indent);

		return sc1;
	}
	
	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */	
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (changedKey != null) {
			if (PREF_PB_INVALID_JAVADOC.equals(changedKey) ||
					PREF_PB_MISSING_JAVADOC_TAGS.equals(changedKey) ||
					PREF_PB_MISSING_JAVADOC_COMMENTS.equals(changedKey) ||
					PREF_JAVADOC_SUPPORT.equals(changedKey)) {				
				updateEnableStates();
			} else {
				return;
			}
		} else {
			updateEnableStates();
		}		
		fContext.statusChanged(new StatusInfo());
	}
	
	private void updateEnableStates() {
		boolean enableJavadoc= checkValue(PREF_JAVADOC_SUPPORT, ENABLED);
		fJavadocComposite.setVisible(enableJavadoc);

		boolean enableInvalidTagsErrors= !checkValue(PREF_PB_INVALID_JAVADOC, IGNORE);
		getCheckBox(PREF_PB_INVALID_JAVADOC_TAGS).setEnabled(enableInvalidTagsErrors);
		getCheckBox(PREF_PB_INVALID_JAVADOC_TAGS_NOT_VISIBLE_REF).setEnabled(enableInvalidTagsErrors);
		getCheckBox(PREF_PB_INVALID_JAVADOC_TAGS_DEPRECATED_REF).setEnabled(enableInvalidTagsErrors);
		setComboEnabled(PREF_PB_INVALID_JAVADOC_TAGS_VISIBILITY, enableInvalidTagsErrors);
		
		boolean enableMissingTagsErrors= !checkValue(PREF_PB_MISSING_JAVADOC_TAGS, IGNORE);
		getCheckBox(PREF_PB_MISSING_JAVADOC_TAGS_OVERRIDING).setEnabled(enableMissingTagsErrors);
		setComboEnabled(PREF_PB_MISSING_JAVADOC_TAGS_VISIBILITY, enableMissingTagsErrors);
		
		boolean enableMissingCommentsErrors= !checkValue(PREF_PB_MISSING_JAVADOC_COMMENTS, IGNORE);
		getCheckBox(PREF_PB_MISSING_JAVADOC_COMMENTS_OVERRIDING).setEnabled(enableMissingCommentsErrors);
		setComboEnabled(PREF_PB_MISSING_JAVADOC_COMMENTS_VISIBILITY, enableMissingCommentsErrors);
	}
	
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.needsbuild.title"); //$NON-NLS-1$
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.needsfullbuild.message"); //$NON-NLS-1$
		} else {
			message= PreferencesMessages.getString("JavadocProblemsConfigurationBlock.needsprojectbuild.message"); //$NON-NLS-1$
		}
		return new String[] { title, message };
	}
	
}
