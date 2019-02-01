/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.preferences.ComplianceConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaHoverMessages;

public class PreviewFeaturesSubProcessor {
	private static final String CONFIGURE_COMPILER_PROBLEM_SEVERITY_DIALOG_ID= "configure_compiler_settings_dialog_id"; //$NON-NLS-1$

	public static void getEnablePreviewFeaturesProposal(IInvocationContext context, Collection<ICommandAccess> proposals) {

		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features, null, IProposalRelevance.ENABLE_PREVIEW_FEATURES,
				JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {

			@Override
			public void apply(IDocument document) {
				IProject project= null;
				IJavaProject javaProject= context.getCompilationUnit().getJavaProject();
				if (javaProject != null) {
					project= javaProject.getProject();
				}
				Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

				boolean usePropertyPage;
				Key[] keys= ComplianceConfigurationBlock.getKeys(javaProject != null);
				boolean hasProjectSpecificOptions= OptionsConfigurationBlock.hasProjectSpecificOptions(project, keys, null);

				if (!hasProjectSpecificOptions) {
					String message= Messages.format(
							JavaHoverMessages.ProblemHover_chooseCompilerSettingsTypeDialog_message,
							new Object[] { JavaElementLabels.getElementLabel(javaProject, JavaElementLabels.ALL_DEFAULT) });

					String[] buttons= new String[] {
							JavaHoverMessages.ProblemHover_chooseSettingsTypeDialog_button_project,
							JavaHoverMessages.ProblemHover_chooseSettingsTypeDialog_button_workspace,
							IDialogConstants.CANCEL_LABEL };

					int result= OptionalMessageDialog.open(
							CONFIGURE_COMPILER_PROBLEM_SEVERITY_DIALOG_ID, shell, JavaHoverMessages.ProblemHover_chooseCompilerSettingsTypeDialog_title, null, message, MessageDialog.QUESTION, buttons,
							0, JavaHoverMessages.ProblemHover_chooseSettingsTypeDialog_checkBox_dontShowAgain);

					if (result == OptionalMessageDialog.NOT_SHOWN) {
						usePropertyPage= false;
					} else if (result == 2 || result == SWT.DEFAULT) {
						return;
					} else if (result == 0) {
						usePropertyPage= true;
					} else {
						usePropertyPage= false;
					}
				} else {
					usePropertyPage= true;
				}

				String pageId;
				Map<String, Object> data= new HashMap<>();

				if (usePropertyPage) {
					pageId= CompliancePreferencePage.PROP_ID;
					data.put(CompliancePreferencePage.USE_PROJECT_SPECIFIC_OPTIONS, Boolean.TRUE);
				} else {
					pageId= CompliancePreferencePage.PREF_ID;
				}

				if (usePropertyPage) {
					PreferencesUtil.createPropertyDialogOn(shell, javaProject, pageId, null, data).open();
				} else {
					PreferencesUtil.createPreferenceDialogOn(shell, pageId, null, data).open();
				}
			}

			@Override
			public String getAdditionalProposalInfo(IProgressMonitor monitor) {
				return CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_info;
			}
		};

		proposals.add(proposal);
	}
}
