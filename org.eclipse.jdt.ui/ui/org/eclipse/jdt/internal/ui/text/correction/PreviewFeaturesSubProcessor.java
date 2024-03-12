/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.IProgressService;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.preferences.ComplianceConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaHoverMessages;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class PreviewFeaturesSubProcessor {
	private static final String CONFIGURE_COMPILER_PROBLEM_SEVERITY_DIALOG_ID= "configure_compiler_settings_dialog_id"; //$NON-NLS-1$
	private static final String ENABLED= "enabled"; //$NON-NLS-1$

	private static class EnablePreviewFeatureProposal extends ChangeCorrectionProposal implements IWorkspaceRunnable {

		private final IJavaProject fJavaProject;
		private IProject fProject;
		private Job fUpdateJob;
		private boolean fChangeOnWorkspace;


		public EnablePreviewFeatureProposal(String name, IJavaProject project, boolean changeOnWorkspace, int relevance) {
			super(name, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
			fJavaProject= project;
			fUpdateJob= null;
			fChangeOnWorkspace= changeOnWorkspace;
			if (fJavaProject != null) {
				fProject= fJavaProject.getProject();
			}

		}

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			fUpdateJob= CoreUtility.getBuildJob(fChangeOnWorkspace ? null : fProject.getProject());
		}

		@Override
		public void apply(IDocument document) {
			if (fJavaProject == null) {
				return;
			}
			if (fChangeOnWorkspace) {
				Hashtable<String, String> map= JavaCore.getOptions();
				map.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
				JavaCore.setOptions(map);
			} else {
				Map<String, String> map= fJavaProject.getOptions(false);
				map.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
				fJavaProject.setOptions(map);
			}
			try {
				IProgressService progressService= PlatformUI.getWorkbench().getProgressService();
				progressService.run(true, true, new WorkbenchRunnableAdapter(this));
			} catch (InvocationTargetException e) {
				JavaPlugin.log(e);
			} catch (InterruptedException e) {
				return;
			}
			if (fUpdateJob != null) {
				fUpdateJob.schedule();
			}
		}

		@Override
		public String getAdditionalProposalInfo(IProgressMonitor monitor) {
			if (fChangeOnWorkspace) {
				return CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_workspace_info;
			}else {
				return CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_info;
			}

		}
	}

	public static void getOpenCompliancePageToEnablePreviewFeaturesProposal(IInvocationContext context, Collection<ICommandAccess> proposals) {

		IJavaProject javaProject= context.getCompilationUnit().getJavaProject();
		boolean hasProjectSpecificOptions= hasProjectSpecificOptions(javaProject);

		String label= CorrectionMessages.PreviewFeaturesSubProcessor_open_compliance_page_enable_preview_features;
		if (hasProjectSpecificOptions) {
			label= CorrectionMessages.PreviewFeaturesSubProcessor_open_compliance_properties_page_enable_preview_features;
		}

		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(label, null, IProposalRelevance.ENABLE_PREVIEW_FEATURES,
				JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)) {

			@Override
			public void apply(IDocument document) {

				Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				boolean usePropertyPage;

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

					switch (result) {
						case 2:
						case SWT.DEFAULT:
							return;
						case 0:
							usePropertyPage= true;
							break;
						case OptionalMessageDialog.NOT_SHOWN:
						default:
							usePropertyPage= false;
							break;
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
				String additionalProposalInfo= CorrectionMessages.PreviewFeaturesSubProcessor_open_compliance_page_enable_preview_features_info;
				if (hasProjectSpecificOptions) {
					additionalProposalInfo= CorrectionMessages.PreviewFeaturesSubProcessor_open_compliance_properties_page_enable_preview_features_info;
				}
				return additionalProposalInfo;
			}
		};

		proposals.add(proposal);
	}

	public static void getEnablePreviewFeaturesProposal(IInvocationContext context, Collection<ICommandAccess> proposals) {
		IJavaProject javaProject= context.getCompilationUnit().getJavaProject();
		if (javaProject != null) {
			boolean changeOnWorkspace= !hasProjectSpecificOptions(javaProject);
			String label= CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features;
			if (changeOnWorkspace) {
				label= CorrectionMessages.PreviewFeaturesSubProcessor_enable_preview_features_workspace;
			}
			EnablePreviewFeatureProposal enableProposal= new EnablePreviewFeatureProposal(label, javaProject, changeOnWorkspace, IProposalRelevance.ENABLE_PREVIEW_FEATURES);
			proposals.add(enableProposal);
		}
	}

	public static void getNeedHigherComplianceProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		String[] args= problem.getProblemArguments();
		if (args != null && args.length > 0) {
			String supportedVersion= JavaCore.latestSupportedJavaVersion();
			String arg= args[1];
			if (arg.equals(supportedVersion)) {
				ReorgCorrectionsSubProcessor.getNeedHigherComplianceProposals(context, problem, proposals, true, supportedVersion);
			}
		}
	}

	public static boolean hasProjectSpecificOptions(IJavaProject javaProject) {
		boolean hasProjectSpecificOptions= false;
		if (javaProject != null) {
			IProject project= javaProject.getProject();
			Key[] keys= ComplianceConfigurationBlock.getKeys(true);
			hasProjectSpecificOptions= OptionsConfigurationBlock.hasProjectSpecificOptions(project, keys, null);
		}
		return hasProjectSpecificOptions;
	}

	public static boolean isPreviewFeatureEnabled(IJavaProject javaProject) {
		boolean isPreviewFeatureEnabled= false;
		if (javaProject != null && JavaModelUtil.isLatestOrHigherJavaVersion(javaProject)) {
			IProject project= javaProject.getProject();
			Key[] keys= ComplianceConfigurationBlock.getKeys(true);
			boolean hasProjectSpecificOptions= OptionsConfigurationBlock.hasProjectSpecificOptions(project, keys, null);
			if (hasProjectSpecificOptions) {
				String option= javaProject.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, true);
				if (option != null && ENABLED.equals(option)) {
					isPreviewFeatureEnabled= true;
				}
			} else {
				String option= JavaCore.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES);
				if (option != null && ENABLED.equals(option)) {
					isPreviewFeatureEnabled= true;
				}
			}
		}
		return isPreviewFeatureEnabled;
	}

	private PreviewFeaturesSubProcessor() {
	}
}
