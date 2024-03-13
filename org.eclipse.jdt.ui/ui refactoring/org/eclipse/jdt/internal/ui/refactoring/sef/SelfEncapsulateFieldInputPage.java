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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldCompositeRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.preferences.CodeStylePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

public class SelfEncapsulateFieldInputPage extends UserInputWizardPage {

	private static final String REFACTORING_TYPE= "refactoringType"; //$NON-NLS-1$
	private static final String REFACTORING= "refactoring"; //$NON-NLS-1$
	private static final String WARNING_ICON= "warningIcon"; //$NON-NLS-1$
	private static final String WARNING_TEXT= "warningText"; //$NON-NLS-1$
	private SelfEncapsulateFieldCompositeRefactoring fRefactorings;
	private IDialogSettings fSettings;
	private List<Control> fEnablements = new ArrayList<>();

	private HashMap<SelfEncapsulateFieldRefactoring, TreeItem> refactoringButtonMap = new HashMap<>();

	private HashMap<String, Label> warningLabelMap = new HashMap<>();
	private SelfEncapsulateFieldRefactoring selectedField;

	private static final String GENERATE_JAVADOC= "GenerateJavadoc";  //$NON-NLS-1$

	private enum RefactoringType {
		GETTER, SETTER;
	}

	public SelfEncapsulateFieldInputPage() {
		super("InputPage"); //$NON-NLS-1$
		setDescription(RefactoringMessages.SelfEncapsulateFieldInputPage_description);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
	}

	public List<SelfEncapsulateFieldRefactoring> getSelectedRefactorings() {
		return fRefactorings.getRefactorings().stream()
				.filter(SelfEncapsulateFieldRefactoring::isSelected).toList();
	}

	@Override
	public void createControl(Composite parent) {
		SelfEncapsulateFieldWizard wizard = (SelfEncapsulateFieldWizard) getRefactoringWizard();
		fRefactorings = (SelfEncapsulateFieldCompositeRefactoring) wizard.getRefactoring();

		fEnablements= new ArrayList<>();
		loadSettings();

		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		initializeDialogUnits(result);
		result.setLayout(new GridLayout(3, false));

		Composite selectorGroup = new Composite(result, SWT.NONE);
		selectorGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		selectorGroup.setLayout(new GridLayout(2, false));

		Tree tree = new Tree(selectorGroup, SWT.CHECK);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite editorContainerGroup = new Composite(selectorGroup, SWT.NONE);
		final GridData editorContainerGroupLayoutData = new GridData(SWT.FILL, SWT.TOP, false, true);
		editorContainerGroupLayoutData.widthHint = 300;
		editorContainerGroup.setLayoutData(editorContainerGroupLayoutData);
		editorContainerGroup.setLayout(new GridLayout(1, false));
		editorContainerGroup.setEnabled(false);

		Label fieldLabel= new Label(editorContainerGroup, SWT.LEFT);
		fieldLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fieldLabel.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_selected_field_name);
		fEnablements.add(fieldLabel);

		Composite editorGroup = new Composite(editorContainerGroup, SWT.NONE);
		editorGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout editorLayout = new GridLayout(2, false);
		editorLayout.marginWidth = 0;
		editorGroup.setLayout(editorLayout);

		GridData textLayoutData = new GridData(SWT.FILL, SWT.LEFT, true, false);
		textLayoutData.widthHint = SWT.DEFAULT;

		Label getterLabel= new Label(editorGroup, SWT.LEFT);
		getterLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		getterLabel.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_getter_name);
		fEnablements.add(getterLabel);

		Text getterText = new Text(editorGroup, SWT.LEFT | SWT.BORDER);
		getterText.setLayoutData(textLayoutData);
		getterText.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				selectedField.setGetterName(getterText.getText());
                for (TreeItem treeItem : refactoringButtonMap.get(selectedField).getItems()) {
                	if (treeItem.getData(REFACTORING_TYPE) == RefactoringType.GETTER)
                		treeItem.setText(getterText.getText());
                }
                processValidation();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// Not needed
			}
		});
		fEnablements.add(getterText);

		Label setterLabel= new Label(editorGroup, SWT.LEFT);
		setterLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		setterLabel.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_setter_name);
		fEnablements.add(setterLabel);

		Text setterText = new Text(editorGroup, SWT.LEFT | SWT.BORDER);
		setterText.setLayoutData(textLayoutData);
		setterText.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				selectedField.setSetterName(setterText.getText());
                for (TreeItem treeItem : refactoringButtonMap.get(selectedField).getItems()) {
                	if (treeItem.getData(REFACTORING_TYPE) == RefactoringType.SETTER)
                		treeItem.setText(setterText.getText());
                }
                processValidation();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// Not needed
			}
		});
		fEnablements.add(setterText);

		tree.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				 TreeItem selectedItem = (TreeItem) event.item;
            	 TreeItem parentItem = selectedItem.getParentItem();
				 selectField(editorContainerGroup, fieldLabel, getterText, setterText, parentItem == null ? selectedItem : parentItem);
	             if(event.detail == SWT.CHECK) {
	            	 if(parentItem != null) {	// Setter or Getter
	            		 SelfEncapsulateFieldRefactoring refactoring = (SelfEncapsulateFieldRefactoring) selectedItem.getData(REFACTORING);
	            		 String methodName = ""; //$NON-NLS-1$
	            		 if ((RefactoringType) selectedItem.getData(REFACTORING_TYPE) == RefactoringType.GETTER) {
	            			 refactoring.setGetterName(methodName);
	            		 } else {
	            			 refactoring.setSetterName(methodName);
	            		 }
	            		 boolean isChecked = false;
	            		 for(TreeItem item: parentItem.getItems()) {
	            			 isChecked |= item.getChecked();
	            		 }
	            		 parentItem.setChecked(isChecked);
	            		 refactoring.setSelected(isChecked);
	            	 } else {					// Field
		            	 for (TreeItem item: selectedItem.getItems()) {
		            		 item.setChecked(selectedItem.getChecked());
		            		 ((SelfEncapsulateFieldRefactoring) item.getData(REFACTORING)).setSelected(selectedItem.getChecked());
		            	 }
	            	 }
		             updateRefactorings();
		             processValidation();
	             }
			}
		});

		// Populate the tree with fields and their getter and setter
		fRefactorings.getRefactorings().forEach(refactoring -> {
			boolean isChecked = wizard.getPreselected().contains(refactoring.getField());
			String fieldName = refactoring.getField().getElementName();
        	TreeItem item = new TreeItem(tree, SWT.CHECK);
        	refactoringButtonMap.put(refactoring, item);
			item.setChecked(isChecked);
			item.setData(REFACTORING, refactoring);

        	TreeItem generateGetter = new TreeItem(item, SWT.CHECK);
            generateGetter.setChecked(isChecked);
            generateGetter.setText(refactoring.getGetterName());
            generateGetter.setData(REFACTORING_TYPE, RefactoringType.GETTER);
            generateGetter.setData(REFACTORING, refactoring);

            if(needsSetter(refactoring)) {
            	TreeItem generateSetter = new TreeItem(item, SWT.CHECK);
            	generateSetter.setChecked(isChecked);
                generateSetter.setText(refactoring.getSetterName());
                generateSetter.setData(REFACTORING_TYPE, RefactoringType.SETTER);
                generateSetter.setData(REFACTORING, refactoring);
            } else {
            	fieldName += String.format(" %s", RefactoringMessages.SelfEncapsulateFieldInputPage_final_field) ; //$NON-NLS-1$
            }
        	item.setText(fieldName);
            refactoring.setSelected(isChecked);
		});

		// Select the first preselected field
		getSelectedRefactorings().stream()
			.findFirst()
			.ifPresent(refactoring -> {
				TreeItem item = refactoringButtonMap.get(refactoring);
				tree.setSelection(item);
				selectField(editorContainerGroup, fieldLabel, getterText, setterText, item);
			});

		Link link= new Link(result, SWT.NONE);
		link.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_configure_link);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doOpenPreference();
			}
		});
		link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));

		Label separator= new Label(result, SWT.NONE);
		separator.setLayoutData(new GridData(SWT.INHERIT_NONE, SWT.CENTER, true, false, 3, 1));

		createFieldAccessBlock(result);

		Label label= new Label(result, SWT.LEFT);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_insert_after);
		fEnablements.add(label);

		final Combo combo= new Combo(result, SWT.READ_ONLY);
		SWTUtil.setDefaultVisibleItemCount(combo);
		fillWithPossibleInsertPositions(combo);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				for(SelfEncapsulateFieldRefactoring refactoring: fRefactorings.getRefactorings()) {
					refactoring.setInsertionIndex(combo.getSelectionIndex() - 1);
				}
			}
		});
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		fEnablements.add(combo);
		createAccessModifier(result);

		Button checkBox= new Button(result, SWT.CHECK);
		checkBox.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_generateJavadocComment);
		checkBox.setSelection(fRefactorings.getRefactorings().get(0).getGenerateJavadoc());
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

		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.SEF_WIZARD_PAGE);
	}

	/*
	 * Selects the field in the tree and fills its getter and setter in the text boxes
	 */
	private void selectField(Composite group, Label header, Text getter, Text setter, TreeItem parentItem) {
		SelfEncapsulateFieldRefactoring refactoring = (SelfEncapsulateFieldRefactoring) parentItem.getData(REFACTORING);
		this.selectedField = refactoring;
		group.setEnabled(true);
		header.setText(String.format("%s %s", RefactoringMessages.SelfEncapsulateFieldInputPage_selected_field_name, refactoring.getField().getElementName())); //$NON-NLS-1$
		for (TreeItem child : parentItem.getItems()) {
			if (child.getData(REFACTORING_TYPE) == RefactoringType.GETTER) {
				getter.setText(child.getText());
			}
			else if (child.getData(REFACTORING_TYPE) == RefactoringType.SETTER) {
				setter.setEnabled(true);
				setter.setText(child.getText());
			}
		}
		if (!needsSetter(refactoring)) {
			setter.setEnabled(false);
			setter.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_final_field);
		}
	}

	private void doOpenPreference() {
		String id= CodeStylePreferencePage.PROP_ID;
		IJavaProject project= fRefactorings.getRefactorings().get(0).getField().getJavaProject();

		String[] relevantOptions= getRelevantOptions(project);

		int open= PreferencesUtil.createPropertyDialogOn(getShell(), project, id, new String[] { id }, null).open();
		if (open == Window.OK && !Arrays.equals(relevantOptions, getRelevantOptions(project))) { // relevant options changes
			fRefactorings.getRefactorings().forEach(SelfEncapsulateFieldRefactoring::reinitialize);
			refactoringButtonMap.values().forEach(treeItem -> {
				for (TreeItem childItem : treeItem.getItems()) {
					SelfEncapsulateFieldRefactoring refactoring = (SelfEncapsulateFieldRefactoring) childItem.getData(REFACTORING);
					if ((RefactoringType)childItem.getData(REFACTORING_TYPE) == RefactoringType.GETTER) {
						childItem.setText(refactoring.getGetterName());
					} else {
						childItem.setText(refactoring.getSetterName());
					}
				}
			});
		}
		updateRefactorings();
	}

	private String[] getRelevantOptions(IJavaProject project) {
		return new String[] {
			project.getOption(JavaCore.CODEASSIST_FIELD_PREFIXES, true),
			project.getOption(JavaCore.CODEASSIST_FIELD_SUFFIXES, true),
			PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, project)
		};
	}

	private void updateEnablements() {
		boolean enable= !fRefactorings.getRefactorings().get(0).isUsingLocalSetter() || !fRefactorings.getRefactorings().get(0).isUsingLocalGetter();
		for (Control control : fEnablements) {
			control.setEnabled(enable);
		}
	}

	private void updateRefactorings() {
		refactoringButtonMap.values().forEach(treeItem -> {
			for (TreeItem childItem : treeItem.getItems()) {
				SelfEncapsulateFieldRefactoring refactoring = (SelfEncapsulateFieldRefactoring) childItem.getData(REFACTORING);
				String text = childItem.getChecked() ? childItem.getText() : ""; //$NON-NLS-1$
				if ((RefactoringType)childItem.getData(REFACTORING_TYPE) == RefactoringType.GETTER) {
					refactoring.setGetterName(text);
				} else {
					refactoring.setSetterName(text);
				}
			}
		});
	}

	private void loadSettings() {
		fSettings= getDialogSettings().getSection(SelfEncapsulateFieldWizard.DIALOG_SETTING_SECTION);
		if (fSettings == null) {
			fSettings= getDialogSettings().addNewSection(SelfEncapsulateFieldWizard.DIALOG_SETTING_SECTION);
			fSettings.put(GENERATE_JAVADOC, JavaPreferencesSettings.getCodeGenerationSettings(fRefactorings.getRefactorings().get(0).getField().getJavaProject()).createComments);
		}
		fRefactorings.getRefactorings().forEach(refactoring ->
		refactoring.setGenerateJavadoc(fSettings.getBoolean(GENERATE_JAVADOC)));
	}

	private void createAccessModifier(Composite result) {

		Label label= new Label(result, SWT.NONE);
		label.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_access_Modifiers);
		fEnablements.add(label);

		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		GridLayout layout= new GridLayout(5, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		group.setLayout(layout);

		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite warningGroup= new Composite(result, SWT.NONE);
		warningGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		GridLayout warningLayout= new GridLayout();
		warningLayout.numColumns= 2;
		warningLayout.marginWidth= 0;
		warningLayout.marginHeight= 0;
		warningGroup.setLayout(warningLayout);

		Label warningIcon= new Label(warningGroup, SWT.WRAP);
		warningIcon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		warningLabelMap.put(WARNING_ICON, warningIcon);
		fEnablements.add(warningIcon);

		Label warningText= new Label(warningGroup, SWT.WRAP);
		warningText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		warningLabelMap.put(WARNING_TEXT, warningText);
		fEnablements.add(warningText);

		Object[] info= createData();
		String[] labels= (String[])info[0];
		Integer[] data= (Integer[])info[1];
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			radio.setText(labels[i]);
			radio.setData(data[i]);
			int iData= data[i];
			if (iData == -1)
				radio.setSelection(true);
			radio.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if((Integer) event.widget.getData() == -1) {
						fRefactorings.getRefactorings().forEach(refactoring -> {
							try {
								refactoring.setVisibility(refactoring.getFieldVisibility());
							} catch (JavaModelException e) {
								refactoring.setVisibility(Flags.AccPublic);
							}
						});
					} else {
						fRefactorings.getRefactorings().forEach(refactoring -> refactoring.setVisibility(((Integer)event.widget.getData())));
					}
					processValidation();
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
				fRefactorings.getRefactorings().forEach(refactoring -> refactoring.setEncapsulateDeclaringClass(true));
			}
		});
		radio.setLayoutData(new GridData());

		radio= new Button(group, SWT.RADIO);
		radio.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_keep_references);
		radio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fRefactorings.getRefactorings().forEach(refactoring -> refactoring.setEncapsulateDeclaringClass(true));
			}
		});
		radio.setLayoutData(new GridData());
	}

	private Object[] createData() {
		String sameAsField= RefactoringMessages.SelfEncapsulateFieldInputPage_same;
		String pub= RefactoringMessages.SelfEncapsulateFieldInputPage_public;
		String pro= RefactoringMessages.SelfEncapsulateFieldInputPage_protected;
		String def= RefactoringMessages.SelfEncapsulateFieldInputPage_default;
		String priv= RefactoringMessages.SelfEncapsulateFieldInputPage_private;

		String[] labels= new String[] { sameAsField, pub, pro, def, priv };
		Integer[] data= new Integer[] { -1 , Flags.AccPublic, Flags.AccProtected, 0, Flags.AccPrivate };
		return new Object[] {labels, data};
	}

	private void fillWithPossibleInsertPositions(Combo combo) {
		int select= 0;
		combo.add(RefactoringMessages.SelfEncapsulateFieldInputPage_first_method);
		try {
			IMethod[] methods = fRefactorings.getRefactorings().get(0).getField().getDeclaringType().getMethods();
			for (IMethod method : methods) {
				combo.add(JavaElementLabels.getElementLabel(method, JavaElementLabels.M_PARAMETER_TYPES));
			}
			if (methods.length > 0)
				select= methods.length;
		} catch (JavaModelException e) {
			// Fall through
		}
		combo.select(select);
		for(SelfEncapsulateFieldRefactoring refactoring: fRefactorings.getRefactorings()) {
			refactoring.setInsertionIndex(select - 1);
		}
	}

	private void setGenerateJavadoc(boolean value) {
		fSettings.put(GENERATE_JAVADOC, value);
		fRefactorings.getRefactorings().forEach(refactoring -> refactoring.setGenerateJavadoc(value));
	}

	private void processValidation() {
		RefactoringStatus status = new RefactoringStatus();
		fRefactorings.getRefactorings().forEach(refactoring -> status.merge(refactoring.checkMethodNames()));
		String message= null;
		boolean valid= true;
		if (status.hasFatalError()) {
			message= status.getMessageMatchingSeverity(RefactoringStatus.FATAL);
			valid= false;
		}
		if (getSelectedRefactorings().size() == 0) {
			valid = false;
		} else if (refactoringButtonMap.values().stream().map(item -> item.getItems()).flatMap(Arrays::stream)
				.filter(item -> (RefactoringType) item.getData(REFACTORING_TYPE) == RefactoringType.GETTER)
				.filter(TreeItem::getChecked).map(TreeItem::getText).anyMatch(String::isEmpty)) {
			message= RefactoringMessages.SelfEncapsulateFieldInputPage_no_getter_name;
			valid= false;
		} else if (refactoringButtonMap.values().stream().map(item -> item.getItems()).flatMap(Arrays::stream)
				.filter(item -> (RefactoringType) item.getData(REFACTORING_TYPE) == RefactoringType.SETTER)
				.filter(TreeItem::getChecked).map(TreeItem::getText).anyMatch(String::isEmpty)) {
			message= RefactoringMessages.SelfEncapsulateFieldInputPage_no_setter_name;
			valid= false;
		}
		checkForWarning();
		setErrorMessage(message);
		setPageComplete(valid);
	}

	private boolean needsSetter(SelfEncapsulateFieldRefactoring refactoring) {
		try {
			return !JdtFlags.isFinal(refactoring.getField());
		} catch(JavaModelException e) {
			return true;
		}
	}

	private void checkForWarning() {
		Label warningIcon = warningLabelMap.get(WARNING_ICON);
		Label warningText = warningLabelMap.get(WARNING_TEXT);
		if(warningIcon == null || warningText == null) {
			return;
		}
		List<Integer> visibilityIndex = Stream.of(Flags.AccPrivate, Flags.AccDefault, Flags.AccProtected, Flags.AccPublic).toList();
		boolean shouldWarn = getSelectedRefactorings().stream().anyMatch(refactoring -> {
			try {
				return visibilityIndex.indexOf(refactoring.getVisibility()) < visibilityIndex.indexOf(refactoring.getFieldVisibility());
			} catch (JavaModelException e) {
				return true;
			}
		});
		if (shouldWarn) {
			warningIcon.setImage(Dialog.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));
			warningText.setText(RefactoringMessages.SelfEncapsulateFieldInputPage_access_warning);
		} else {
			warningIcon.setImage(null);
			warningText.setText(""); //$NON-NLS-1$
		}
		warningIcon.getParent().layout();
	}
}
