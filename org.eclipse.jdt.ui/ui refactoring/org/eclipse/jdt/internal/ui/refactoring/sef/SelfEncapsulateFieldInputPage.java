/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.refactoring.sef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.preferences.CodeStylePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class SelfEncapsulateFieldInputPage extends UserInputWizardPage {

	private SelfEncapsulateFieldRefactoring fRefactoring;
	private IDialogSettings fSettings;
	private List<Control> fEnablements;

	private Text fGetterName;
	private Text fSetterName;

	private Label fGetterInfo;
	private Label fSetterInfo;

	private String fOldSetterValue= ""; //$NON-NLS-1$
	private String fOldGetterValue= ""; //$NON-NLS-1$

	private Button fGenerateGetter;
	private Button fGenerateSetter;

	private static final String GENERATE_JAVADOC= "GenerateJavadoc";  //$NON-NLS-1$

	public SelfEncapsulateFieldInputPage() {
		super("InputPage"); //$NON-NLS-1$
		setDescription(RefactoringMessages.SelfEncapsulateFieldInputPage_description);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
	}

	@Override
	public void createControl(Composite parent) {
		fRefactoring= (SelfEncapsulateFieldRefactoring)getRefactoring();

		fEnablements= new ArrayList<>();
		loadSettings();

		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		initializeDialogUnits(result);

		result.setLayout(new GridLayout(3, false));

		Composite nameComposite= new Composite(result, SWT.NONE);
		nameComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		GridLayout gridLayout= new GridLayout(3, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;

		nameComposite.setLayout(gridLayout);
		fGenerateGetter= new Button(nameComposite, SWT.CHECK);
		fGenerateGetter.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_getter_name);
		fGenerateGetter.setSelection(true);
		fGenerateGetter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fGenerateGetter.getSelection()) {
					fGetterName.setEnabled(true);
					fGetterName.setText(fOldGetterValue);
				} else {
					fOldGetterValue= fGetterName.getText();
					fGetterName.setText(""); //$NON-NLS-1$
					fGetterName.setEnabled(false);
				}
				doGetterModified();
			}
		});
		fGenerateGetter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		fGetterName= new Text(nameComposite, SWT.BORDER);
		fGetterName.setText(fRefactoring.getGetterName());
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(25);
		fGetterName.setLayoutData(gd);
		fGetterName.addModifyListener(e -> doGetterModified());
		TextFieldNavigationHandler.install(fGetterName);


		fGetterInfo= new Label(nameComposite, SWT.LEAD);
		fGetterInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		updateUseGetter();

		fGenerateSetter= new Button(nameComposite, SWT.CHECK);
		fGenerateSetter.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_setter_name);
		fGenerateSetter.setEnabled(needsSetter());
		fGenerateSetter.setSelection(needsSetter());
		fGenerateSetter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fGenerateSetter.getSelection()) {
					fSetterName.setEnabled(true);
					fSetterName.setText(fOldSetterValue);
				} else {
					fOldSetterValue= fSetterName.getText();
					fSetterName.setText(""); //$NON-NLS-1$
					fSetterName.setEnabled(false);
				}
				doSetterModified();
			}
		});
		fGenerateSetter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		fSetterName= new Text(nameComposite, SWT.BORDER);
		if(needsSetter()) {
			fSetterName.setText(fRefactoring.getSetterName());
		} else {
			fSetterName.setText(""); //$NON-NLS-1$
			fSetterName.setEnabled(false);
		}
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(25);
		fSetterName.setLayoutData(gd);
		fSetterName.addModifyListener(e -> doSetterModified());
		TextFieldNavigationHandler.install(fSetterName);

		fSetterInfo= new Label(nameComposite, SWT.LEAD);
		fSetterInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		updateUseSetter();

		Link link= new Link(nameComposite, SWT.NONE);
		link.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_configure_link);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doOpenPreference();
			}
		});
		link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

		Label separator= new Label(result, SWT.NONE);
		separator.setText(""); //$NON-NLS-1$
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

		createFieldAccessBlock(result);

		Label label= new Label(result, SWT.LEFT);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_insert_after);
		fEnablements.add(label);

		final Combo combo= new Combo(result, SWT.READ_ONLY);
		SWTUtil.setDefaultVisibleItemCount(combo);
		fillWithPossibleInsertPositions(combo, fRefactoring.getField());
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				fRefactoring.setInsertionIndex(combo.getSelectionIndex() - 1);
			}
		});
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		fEnablements.add(combo);

		createAccessModifier(result);

		Button checkBox= new Button(result, SWT.CHECK);
		checkBox.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_generateJavadocComment);
		checkBox.setSelection(fRefactoring.getGenerateJavadoc());
		checkBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setGenerateJavadoc(((Button)e.widget).getSelection());
			}
		});
		checkBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

		fEnablements.add(checkBox);

		updateEnablements();

		processValidation();

		fGetterName.setFocus();

		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.SEF_WIZARD_PAGE);
	}

	private void updateUseSetter() {
		if(!needsSetter()) {
			fGenerateSetter.setSelection(false);
			fGenerateSetter.setEnabled(false);
			fSetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_final_field);
		} else if (fGenerateSetter.getSelection() == false)
			fSetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_no_setter_created);
		else if (fSetterName.getText().isEmpty())
			fSetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_no_setter_name);
		else if (fRefactoring.isUsingLocalSetter())
			fSetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_useexistingsetter_label);
		else
			fSetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_usenewgetter_label);
		updateEnablements();
	}

	private void updateEnablements() {
		boolean enable= !fRefactoring.isUsingLocalSetter() || !fRefactoring.isUsingLocalGetter();
		for (Control control : fEnablements) {
			control.setEnabled(enable);
		}
	}

	private void updateUseGetter() {
		if (fGenerateGetter.getSelection() == false)
			fGetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_no_getter_created);
		else if (fRefactoring.isUsingLocalGetter())
			fGetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_useexistinggetter_label);
		else if (fGetterName.getText().isEmpty())
			fGetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_no_getter_name);
		else
			fGetterInfo.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_usenewsetter_label);
		updateEnablements();
	}

	private void loadSettings() {
		fSettings= getDialogSettings().getSection(SelfEncapsulateFieldWizard.DIALOG_SETTING_SECTION);
		if (fSettings == null) {
			fSettings= getDialogSettings().addNewSection(SelfEncapsulateFieldWizard.DIALOG_SETTING_SECTION);
			fSettings.put(GENERATE_JAVADOC, JavaPreferencesSettings.getCodeGenerationSettings(fRefactoring.getField().getJavaProject()).createComments);
		}
		fRefactoring.setGenerateJavadoc(fSettings.getBoolean(GENERATE_JAVADOC));
	}

	private void createAccessModifier(Composite result) {
		int visibility= fRefactoring.getVisibility();
		if (Flags.isPublic(visibility))
			return;

		Label label= new Label(result, SWT.NONE);
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_access_Modifiers);
		fEnablements.add(label);

		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		GridLayout layout= new GridLayout(4, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		group.setLayout(layout);

		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Object[] info= createData(visibility);
		String[] labels= (String[])info[0];
		Integer[] data= (Integer[])info[1];
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			radio.setText(labels[i]);
			radio.setData(data[i]);
			int iData= data[i];
			if (iData == visibility)
				radio.setSelection(true);
			radio.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					fRefactoring.setVisibility(((Integer)event.widget.getData()));
				}
			});
			fEnablements.add(radio);
		}
	}

	private void createFieldAccessBlock(Composite result) {
		Label label= new Label(result, SWT.LEFT);
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_field_access);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Composite group= new Composite(result, SWT.NONE);
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		group.setLayout(layout);

		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		Button radio= new Button(group, SWT.RADIO);
		radio.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_use_setter_getter);
		radio.setSelection(true);
		radio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setEncapsulateDeclaringClass(true);
			}
		});
		radio.setLayoutData(new GridData());

		radio= new Button(group, SWT.RADIO);
		radio.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_keep_references);
		radio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setEncapsulateDeclaringClass(false);
			}
		});
		radio.setLayoutData(new GridData());
	}

	private Object[] createData(int visibility) {
		String pub= RefactoringMessages.SelfEncapsulateFieldInputPage_public;
		String pro= RefactoringMessages.SelfEncapsulateFieldInputPage_protected;
		String def= RefactoringMessages.SelfEncapsulateFieldInputPage_default;
		String priv= RefactoringMessages.SelfEncapsulateFieldInputPage_private;

		String[] labels= null;
		Integer[] data= null;
		if (Flags.isPrivate(visibility)) {
			labels= new String[] { pub, pro, def, priv };
			data= new Integer[] {Flags.AccPublic, Flags.AccProtected, 0, Flags.AccPrivate };
		} else if (Flags.isProtected(visibility)) {
			labels= new String[] { pub, pro };
			data= new Integer[] {Flags.AccPublic, Flags.AccProtected};
		} else {
			labels= new String[] { pub, def };
			data= new Integer[] {Flags.AccPublic, 0};
		}
		return new Object[] {labels, data};
	}

	private void fillWithPossibleInsertPositions(Combo combo, IField field) {
		int select= 0;
		combo.add(RefactoringMessages.SelfEncapsulateFieldInputPage_first_method);
		try {
			IMethod[] methods= field.getDeclaringType().getMethods();
			for (IMethod method : methods) {
				combo.add(JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES));
			}
			if (methods.length > 0)
				select= methods.length;
		} catch (JavaModelException e) {
			// Fall through
		}
		combo.select(select);
		fRefactoring.setInsertionIndex(select - 1);
	}

	private void setGenerateJavadoc(boolean value) {
		fSettings.put(GENERATE_JAVADOC, value);
		fRefactoring.setGenerateJavadoc(value);
	}

	private void processValidation() {
		RefactoringStatus status= fRefactoring.checkMethodNames();
		String message= null;
		boolean valid= true;
		if (status.hasFatalError()) {
			message= status.getMessageMatchingSeverity(RefactoringStatus.FATAL);
			valid= false;
		}
		if (fGenerateGetter.getSelection() && fGetterName.getText().isEmpty()) {
			message= RefactoringMessages.SelfEncapsulateFieldInputPage_no_getter_name;
			valid= false;
		} else if (fGenerateSetter.getSelection() && fSetterName.getText().isEmpty()) {
			message= RefactoringMessages.SelfEncapsulateFieldInputPage_no_setter_name;
			valid= false;
		} else if (!fGenerateGetter.getSelection() && !fGenerateSetter.getSelection()) {
			message= RefactoringMessages.SelfEncapsulateFieldInputPage_please_choose_to_generate_a_getter_setter;
			valid= false;
		}
		setErrorMessage(message);
		setPageComplete(valid);
	}

	private boolean needsSetter() {
		try {
			return !JdtFlags.isFinal(fRefactoring.getField());
		} catch(JavaModelException e) {
			return true;
		}
	}

	private void doOpenPreference() {
		String id= CodeStylePreferencePage.PROP_ID;
		IJavaProject project= fRefactoring.getField().getJavaProject();

		String[] relevantOptions= getRelevantOptions(project);

		int open= PreferencesUtil.createPropertyDialogOn(getShell(), project, id, new String[] { id }, null).open();
		if (open == Window.OK && !Arrays.equals(relevantOptions, getRelevantOptions(project))) { // relevant options changes
			fRefactoring.reinitialize();
			fGetterName.setText(fRefactoring.getGetterName());
			fSetterName.setText(fRefactoring.getSetterName());
		}
	}

	private String[] getRelevantOptions(IJavaProject project) {
		return new String[] {
			project.getOption(JavaCore.CODEASSIST_FIELD_PREFIXES, true),
			project.getOption(JavaCore.CODEASSIST_FIELD_SUFFIXES, true),
			PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, project)
		};
	}

	private void doSetterModified() {
		fRefactoring.setSetterName(fSetterName.getText());
		updateUseSetter();
		processValidation();
	}

	private void doGetterModified() {
		fRefactoring.setGetterName(fGetterName.getText());
		updateUseGetter();
		processValidation();
	}
}
