/*******************************************************************************
 * Copyright (c) 2019 GK Software SE, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail.ModulePatch;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

/**
 * Allows the user to see and copy the module-related command line options
 * that would be used for compiling with javac (JPMS Options from JEP 261).
 *
 * @since 3.18
 */
// inspired by org.eclipse.debug.internal.ui.launchConfigurations.ShowCommandLineDialog
public class ShowJPMSOptionsDialog extends Dialog {

	private static final String BLANK= " "; //$NON-NLS-1$
	private static final String COMMA= ","; //$NON-NLS-1$
	private static final String OPTION_START= "--"; //$NON-NLS-1$
	private static final String ADD_MODULES= "--add-modules "; //$NON-NLS-1$
	private static final String LIMIT_MODULES= "--limit-modules "; //$NON-NLS-1$

	private final ListDialogField<CPListElement> fClassPathList;
	private Text fJPMSModuleOptionsText;

	private Collection<String> fDefaultSystemModules;
	private Function<Collection<String>, Collection<String>> fReduceFun;
	private Function<Collection<String>, Collection<String>> fClosureFun;

	public ShowJPMSOptionsDialog(Shell parentShell,
			ListDialogField<CPListElement> classPathList,
			Collection<String> defaultSystemModules,
			Function<Collection<String>, Collection<String>> closureFun,
			Function<Collection<String>, Collection<String>> reduceFun)
	{
		super(parentShell);
		fClassPathList= classPathList;
		fDefaultSystemModules= defaultSystemModules;
		fClosureFun= closureFun;
		fReduceFun= reduceFun;
		setShellStyle(SWT.RESIZE | getShellStyle());
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(NewWizardMessages.ShowJPMSOptionsDialog_dialog_title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				NewWizardMessages.ShowJPMSOptionsDialog_copyAndCopy_button, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				NewWizardMessages.ShowJPMSOptionsDialog_close_button, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp= (Composite) super.createDialogArea(parent);
		Font font= parent.getFont();
		int widthHint= convertWidthInCharsToPixels(60);

		Label message= new Label(comp, SWT.LEFT + SWT.WRAP);
		message.setText(NewWizardMessages.ShowJPMSOptionsDialog_explanation_label);
		GridData gdLabel= new GridData(SWT.FILL, SWT.NONE, true, false);
		gdLabel.widthHint= widthHint;
		message.setLayoutData(gdLabel);

		Group group= new Group(comp, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		group.setLayout(topLayout);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(15);
		gd.widthHint= widthHint;
		group.setLayoutData(gd);
		group.setFont(font);

		fJPMSModuleOptionsText= new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd= new GridData(GridData.FILL_BOTH);
		fJPMSModuleOptionsText.setLayoutData(gd);

		String command= getOptions();
		if (command == null) {
			command= NewWizardMessages.ShowJPMSOptionsDialog_retrieve_error;
		} else if (command.isEmpty()) {
			command= NewWizardMessages.ShowJPMSOptionsDialog_empty_message;
		}
		fJPMSModuleOptionsText.setText(command);
		fJPMSModuleOptionsText.setEditable(false);

		return comp;
	}

	private String getOptions() {
		CPListElement jre= null;
		for (CPListElement cpListElement : fClassPathList.getElements()) {
			if (LibrariesWorkbookPage.isJREContainer(cpListElement.getPath())) {
				jre= cpListElement;
				break;
			}
		}
		try {
			return getModuleCLIOptions(fClassPathList.getElements(), jre);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	private String getModuleCLIOptions(List<CPListElement> cpListElements, CPListElement systemLibrary) throws JavaModelException {
		StringBuilder buf= new StringBuilder();
		for (CPListElement cpElement : cpListElements) {
			Object attribute= cpElement.getAttribute(CPListElement.MODULE);
			if (attribute instanceof ModuleEncapsulationDetail[]) {
				ModuleEncapsulationDetail[] details= (ModuleEncapsulationDetail[]) attribute;
				for (ModuleEncapsulationDetail detail : details) {
					String optName= detail.getAttributeName();
					switch (optName) {
						case IClasspathAttribute.ADD_EXPORTS:
						case IClasspathAttribute.ADD_OPENS:
						case IClasspathAttribute.ADD_READS:
							buf.append(OPTION_START).append(optName).append(BLANK).append(detail.toString()).append(BLANK);
							break;
						case IClasspathAttribute.PATCH_MODULE:
							buf.append(OPTION_START).append(optName).append(BLANK).append(((ModulePatch) detail).toAbsolutePathsString(cpElement.getJavaProject())).append(BLANK);
							break;
						case IClasspathAttribute.LIMIT_MODULES:
							addLimitModules(buf, systemLibrary.getJavaProject(), detail.toString());
							break;
						default:
							throw new JavaModelException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), "Unexpected option name "+optName)); //$NON-NLS-1$
					}
				}
			}
		}
		return buf.toString().trim();
	}

	private void addLimitModules(StringBuilder buf, IJavaProject prj, String value) throws JavaModelException {
		String[] modules= value.split(COMMA);
		boolean isUnnamed= prj.getModuleDescription() == null;
		if (isUnnamed) {
			Set<String> selected= new HashSet<>(Arrays.asList(modules));
			// Need to distinguish between minimal form (selected) and transitive closure over requires ...
			Set<String> limit= new HashSet<>(fDefaultSystemModules);
			// ... here we need to compute the closure, for intersection with the similarly closed set of defaultModules
			Collection<String> closureOfSelected= fClosureFun.apply(selected);
			if (limit.retainAll(closureOfSelected)) { // limit = selected ∩ default -- only add the option, if limit ⊂ default
				if (limit.isEmpty()) {
					throw new IllegalArgumentException("Cannot hide all modules, at least java.base is required"); //$NON-NLS-1$
				}
				buf.append(LIMIT_MODULES).append(joinedSortedList(fReduceFun.apply(limit))).append(BLANK); // ... but print in reduced form
			}

			// ... here all must be explicit:
			closureOfSelected.removeAll(fDefaultSystemModules);
			if (!closureOfSelected.isEmpty()) { // add = selected \ default
				buf.append(ADD_MODULES).append(joinedSortedList(closureOfSelected)).append(BLANK);
			}
		} else {
			Arrays.sort(modules);
			buf.append(LIMIT_MODULES).append(String.join(COMMA, modules)).append(BLANK);
		}
	}

	private static String joinedSortedList(Collection<String> list) {
		String[] limitArray= list.toArray(new String[list.size()]);
		Arrays.sort(limitArray);
		return String.join(COMMA, limitArray);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			Clipboard clipboard= new Clipboard(null);
			try {
				TextTransfer textTransfer= TextTransfer.getInstance();
				Transfer[] transfers= new Transfer[] { textTransfer };
				Object[] data= new Object[] { fJPMSModuleOptionsText.getText() };
				clipboard.setContents(data, transfers);
			} finally {
				clipboard.dispose();
			}
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		IDialogSettings section= settings.getSection(getDialogSettingsSectionName());
		if (section == null) {
			section= settings.addNewSection(getDialogSettingsSectionName());
		}
		return section;
	}

	/**
	 * @return the name to use to save the dialog settings
	 */
	protected String getDialogSettingsSectionName() {
		return "SHOW_JPMS_MODULE_OPTIONS_DIALOG"; //$NON-NLS-1$
	}
}
