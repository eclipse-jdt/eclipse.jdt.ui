/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
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
 *     Harald Albers <eclipse@albersweb.de> - [type wizards] New Annotation dialog could allow generating @Documented, @Retention and @Target - https://bugs.eclipse.org/339292
 *******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;

/**
 * Wizard page to create a new annotation type.
 * <p>
 * Note: This class is not intended to be subclassed, but clients can instantiate.
 * To implement a different kind of a new annotation wizard page, extend <code>NewTypeWizardPage</code>.
 * </p>
 *
 * @since 3.1
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class NewAnnotationWizardPage extends NewTypeWizardPage {

    private final static String PAGE_NAME= "NewAnnotationWizardPage"; //$NON-NLS-1$
    private final static int TYPE = NewTypeWizardPage.ANNOTATION_TYPE;

	private final static String SETTINGS_ADD_DOCUMENTED= "add_documented"; //$NON-NLS-1$

	private AddRetentionControl fRetentionSelection;

	private AddTargetControl fTargetSelection;

	private SelectionButtonDialogField fDocumentedSelection;

	/**
	 * Create a new <code>NewAnnotationWizardPage</code>
	 */
	public NewAnnotationWizardPage() {
		super(TYPE, PAGE_NAME);

		setTitle(NewWizardMessages.NewAnnotationWizardPage_title);
		setDescription(NewWizardMessages.NewAnnotationWizardPage_description);

		fRetentionSelection= new AddRetentionControl();
		fTargetSelection= new AddTargetControl();
		fDocumentedSelection= new SelectionButtonDialogField(SWT.CHECK);
		fDocumentedSelection.setLabelText(NewWizardMessages.NewAnnotationWizardPage_add_documented);
	}

	// -------- Initialization ---------

	/**
	 * The wizard owning this page is responsible for calling this method with the
	 * current selection. The selection is used to initialize the fields of the wizard
	 * page.
	 *
	 * @param selection used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem= getInitialJavaElement(selection);

		initContainerPage(jelem);
		initTypePage(jelem);
		initAnnotationPage();
		doStatusUpdate();
	}

	// ------ validation --------

	private void initAnnotationPage() {
		IDialogSettings dialogSettings= getDialogSettings();
		IDialogSettings section= null;
		if (dialogSettings != null) {
			section= dialogSettings.getSection(PAGE_NAME);
		}
		restoreSettings(section);
	}

	private void restoreSettings(IDialogSettings section) {
		if (section != null) {
			boolean addDocumented= section.getBoolean(SETTINGS_ADD_DOCUMENTED);
			fDocumentedSelection.setSelection(addDocumented);
		}

		fRetentionSelection.init(section);
		fTargetSelection.init(section);
	}

	@Override
	protected IStatus containerChanged() {
		IStatus status= super.containerChanged();
		if (status.isOK()) {
			IJavaProject javaProject= getJavaProject();
			fRetentionSelection.setProject(javaProject);
			fTargetSelection.setProject(javaProject);
		}
		return status;
	}

	private void doStatusUpdate() {
		// all used component status
		IStatus[] status= new IStatus[] {
			fContainerStatus,
			isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
			fTypeNameStatus,
			fModifierStatus,
		};

		// the mode severe status will be displayed and the OK button enabled/disabled.
		updateStatus(status);
	}


	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);

		doStatusUpdate();
	}

	private String getTypeNameWithoutParameters(String typeNameWithParameters) {
		int angleBracketOffset= typeNameWithParameters.indexOf('<');
		if (angleBracketOffset == -1) {
			return typeNameWithParameters;
		} else {
			return typeNameWithParameters.substring(0, angleBracketOffset);
		}
	}

	/**
	 * @since 3.26
	 */
	@Override
	protected IStatus typeNameChanged() {
		StatusInfo status= new StatusInfo();

		String typeName= getTypeName();

		if (!typeName.isEmpty() && typeName != getTypeNameWithoutParameters(typeName)) {
			status.setError(NewWizardMessages.NewAnnotationWizardPage_error_invalidTypeParameters);
			return status;
		}
		return super.typeNameChanged();
	}

	// ------ UI --------

	/**
	 * Control for adding an annotation with enum-based values to the generated source code.
	 *
	 * @param <A> the class to add as an annotation
	 * @param <E> the enum class that supplies values for the annotation
	 */
	private abstract static class AddAnnotationControl<A, E extends Enum<E>> {
		private static final String SETTINGS_ENABLED= "enabled"; //$NON-NLS-1$
		private static final String SETTINGS_SELECTED_ENUMS= "selectedEnums"; //$NON-NLS-1$

		/**
		 * A checkbox for enabling the addition of this annotation.
		 */
		protected final SelectionButtonDialogField fEnableButton;

		/**
		 * A group of buttons for choosing between the available enum values for the annotation.
		 */
		protected final SelectionButtonDialogFieldGroup fEnumButtons;

		/**
		 * The annotation whose addition this control represents.
		 */
		private final Class<A> fAnnotationClass;

		/**
		 * The enum from which one or more constants should be added as values to the annotation.
		 */
		private final Class<E> fEnumClass;

		private boolean fControlsAreCreated;

		/**
		 * The Java project in which the annotation is going to be created. The availability of enum
		 * constants may depend on the project, e.g. its source level.
		 */
		protected IJavaProject fJavaProject;

		public AddAnnotationControl(int style, String enableLabel, Class<A> annotationClass, Class<E> enumClass, int nColumns) {
			fAnnotationClass= annotationClass;
			fEnumClass= enumClass;

			String[] enumLabels= toStringArray(enumClass);

			fEnableButton= new SelectionButtonDialogField(SWT.CHECK);
			fEnableButton.setLabelText(enableLabel);

			fEnumButtons= new SelectionButtonDialogFieldGroup(style, enumLabels, nColumns);

			fEnableButton.setDialogFieldListener(field -> fEnumButtons.setEnabled(fEnableButton.isSelected()));
		}

		private String[] toStringArray(Class<E> enumClass) {
			E[] enums= enumClass.getEnumConstants();
			String[] strings= new String[enums.length];
			for (Enum<?> en : enums) {
				strings[en.ordinal()]= labelFor(en);
			}
			return strings;
		}

		protected String labelFor(Enum<?> en) {
			String name= en.name();
			String first= name.substring(0, 1).toUpperCase();
			String rest= name.substring(1).toLowerCase().replace('_', ' ');
			return first + rest;
		}

		public void init(IDialogSettings settings) {
			boolean enabled= false;
			String[] selectedEnums= defaultSelectedEnums();

			if (settings != null) {
				IDialogSettings section= settings.getSection(dialogSettingsSectionName());
				if (section != null) {
					enabled= section.getBoolean(SETTINGS_ENABLED);
					selectedEnums= section.getArray(SETTINGS_SELECTED_ENUMS);
				}
			}

			applyInitialSettings(enabled, selectedEnums);
		}

		protected String[] defaultSelectedEnums() {
			return new String[] {};
		}

		private void applyInitialSettings(boolean enabled, String[] selectedEnumsAsStrings) {
			fEnableButton.setSelection(enabled);
			fEnumButtons.setEnabled(enabled);

			fEnumButtons.setSelection(0, false);
			for (String string : selectedEnumsAsStrings) {
				E en= Enum.valueOf(fEnumClass, string);
				fEnumButtons.setSelection(en.ordinal(), true);
			}
		}

		/**
		 * @return the section name under which this control persists its settings.
		 */
		abstract protected String dialogSettingsSectionName();

		public void persistSettings(IDialogSettings settings) {
			String sectionName= dialogSettingsSectionName();
			IDialogSettings section= settings.getSection(sectionName);
			if (section == null) {
				section= settings.addNewSection(sectionName);
			}
			section.put(SETTINGS_ENABLED, isEnabled());
			section.put(SETTINGS_SELECTED_ENUMS, selectedEnumsAsStrings());
		}

		private String[] selectedEnumsAsStrings() {
			List<String> resultList= new ArrayList<>();

			for (E en : allEnums()) {
				if (isSelected(en)) {
					resultList.add(en.name());
				}
			}

			String[] resultArray= resultList.toArray(new String[] {});
			return resultArray;
		}

		private E[] allEnums() {
			return fEnumClass.getEnumConstants();
		}

		public void doFillIntoGrid(Composite parent, int nColumns) {
			Button button= fEnableButton.getSelectionButton(parent);
			GridData gdButton= new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			button.setLayoutData(gdButton);

			Composite buttonsGroup= fEnumButtons.getSelectionButtonsGroup(parent);
			GridData gdButtonsGroup= new GridData();
			gdButtonsGroup.horizontalSpan= nColumns - 1;
			buttonsGroup.setLayoutData(gdButtonsGroup);

			fControlsAreCreated= true;
			updateButtons();
		}

		/**
		 * Sets or changes the target <code>IJavaProject</code>. The availability of enum constants
		 * may depend on the project, e.g. its source level.
		 *
		 * @param javaProject the new Java project in which the annotation will be created.
		 */
		public void setProject(IJavaProject javaProject) {
			fJavaProject= javaProject;
			updateButtons();
		}

		private void updateButtons() {
			if (fControlsAreCreated && fJavaProject != null) {
				updateAvailableButtons();
			}
		}

		protected void updateAvailableButtons() {
			// after changing the project, the availability of buttons might change.
		}

		/**
		 * @return <code>true</code> if the control is enabled, else <code>false</code>
		 */
		public boolean isEnabled() {
			return fEnableButton.isSelected();
		}

		public void addAnnotation(IType newType, ImportsManager imports, String lineDelimiter) throws JavaModelException {
			if (isEnabled()) {
				List<E> selectedEnums= availableSelectedEnums();
				if (selectedEnums.size() > 0) {
					String annotation= createAnnotationAndImports(selectedEnums, imports, lineDelimiter);

					int start= newType.getSourceRange().getOffset();
					IBuffer buffer= newType.getCompilationUnit().getBuffer();
					buffer.replace(start, 0, annotation);
				}
			}
		}

		private List<E> availableSelectedEnums() {
			List<E> resultList= new ArrayList<>();
			for (E en : allEnums()) {
				if (isEnabled(en) && isSelected(en)) {
					resultList.add(en);
				}
			}
			return resultList;
		}

		private boolean isEnabled(E en) {
			return fEnumButtons.isEnabled(en.ordinal());
		}

		private boolean isSelected(E en) {
			return fEnumButtons.isSelected(en.ordinal());
		}

		private String createAnnotationAndImports(List<E> selectedEnums, ImportsManager imports, String lineDelimiter) {
			StringBuilder buffer= new StringBuilder();

			String annotationTypeName= imports.addImport(fAnnotationClass.getName());
			buffer.append("@"); //$NON-NLS-1$
			buffer.append(annotationTypeName);
			buffer.append("("); //$NON-NLS-1$

			if (selectedEnums.size() > 1) {
				buffer.append("{"); //$NON-NLS-1$
			}

			for (Enum<?> en : selectedEnums) {
				String enumTypeName= imports.addStaticImport(en.getClass().getName(), en.name(), true);
				buffer.append(enumTypeName);
				buffer.append(", "); //$NON-NLS-1$
			}

			buffer.delete(buffer.length() - 2, buffer.length());

			if (selectedEnums.size() > 1) {
				buffer.append("}"); //$NON-NLS-1$
			}

			buffer.append(")"); //$NON-NLS-1$
			buffer.append(lineDelimiter);

			return buffer.toString();
		}
	}

	/**
	 * Control for adding <code>@Retention(RetentionPolicy)</code>
	 */
	private static class AddRetentionControl extends AddAnnotationControl<Retention, RetentionPolicy> {
		private static final String SETTINGS_SECTION_NAME= "AddRetention"; //$NON-NLS-1$

		private static final String[] MNEMONICS= { "S", "l", "n" }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

		public AddRetentionControl() {
			super(SWT.RADIO, NewWizardMessages.NewAnnotationWizardPage_add_retention, Retention.class, RetentionPolicy.class, 3);
		}

		@Override
		protected String labelFor(Enum<?> en) {
			String label= super.labelFor(en);
			String mnemonic= MNEMONICS[en.ordinal()];
			return label.replaceFirst(mnemonic, "&" + mnemonic); //$NON-NLS-1$
		}

		@Override
		protected String dialogSettingsSectionName() {
			return SETTINGS_SECTION_NAME;
		}

		@Override
		protected String[] defaultSelectedEnums() {
			return new String[] { RetentionPolicy.CLASS.name() };
		}
	}

	/**
	 * Control for adding <code>@Target([ElementType])</code>
	 */
	private static class AddTargetControl extends AddAnnotationControl<Target, ElementType> {
		private static final String SETTINGS_SECTION_NAME= "AddTarget"; //$NON-NLS-1$

		/**
		 * Ordinals of enum values that were added in Java 8. As we are still on Java 7 here, they
		 * cannot be expressed as literals.
		 */
		private static final int[] fEnumValuesSinceJava8= new int[] { 8, 9 };

		public AddTargetControl() {
			super(SWT.CHECK, NewWizardMessages.NewAnnotationWizardPage_add_target, Target.class, ElementType.class, 3);
		}

		@Override
		protected void updateAvailableButtons() {
			boolean isJava8orHigher= JavaModelUtil.is1d8OrHigher(fJavaProject);
			for (int index : fEnumValuesSinceJava8) {
				fEnumButtons.enableSelectionButton(index, isJava8orHigher);
			}
		}

		@Override
		protected String dialogSettingsSectionName() {
			return SETTINGS_SECTION_NAME;
		}
	}

	/*
	 * @see WizardPage#createControl
	 */
	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);

		int nColumns= 4;

		GridLayout layout= new GridLayout();
		layout.numColumns= nColumns;
		composite.setLayout(layout);

		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		createEnclosingTypeControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createAddAnnotationControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createCommentControls(composite, nColumns);
		enableCommentControl(true);

		setControl(composite);

		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_ANNOTATION_WIZARD_PAGE);
	}

	private void createAddAnnotationControls(Composite composite, int nColumns) {
		fRetentionSelection.doFillIntoGrid(composite, nColumns);
		DialogField.createEmptySpace(composite, nColumns);
		fTargetSelection.doFillIntoGrid(composite, nColumns);
		DialogField.createEmptySpace(composite, nColumns);
		fDocumentedSelection.doFillIntoGrid(composite, nColumns);
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}

	// ------ code generation --------

	@Override
	protected void createTypeMembers(IType newType, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		String lineDelimiter= StubUtility.getLineDelimiterUsed(newType.getJavaProject());

		fTargetSelection.addAnnotation(newType, imports, lineDelimiter);
		fRetentionSelection.addAnnotation(newType, imports, lineDelimiter);
		addDocumentedAnnotation(newType, imports, lineDelimiter);

		persistSettings();
	}

	private void addDocumentedAnnotation(IType newType, ImportsManager imports, String lineDelimiter) throws JavaModelException {
		if (fDocumentedSelection.isSelected()) {
			String typeName= imports.addImport(Documented.class.getName());
			int start= newType.getSourceRange().getOffset();
			IBuffer buffer= newType.getCompilationUnit().getBuffer();
			buffer.replace(start, 0, "@" + typeName + lineDelimiter); //$NON-NLS-1$
		}
	}

	private void persistSettings() {
		IDialogSettings dialogSettings= getDialogSettings();
		if (dialogSettings != null) {
			IDialogSettings section= dialogSettings.getSection(PAGE_NAME);
			if (section == null) {
				section= dialogSettings.addNewSection(PAGE_NAME);
			}
			section.put(SETTINGS_ADD_DOCUMENTED, fDocumentedSelection.isSelected());
			fRetentionSelection.persistSettings(section);
			fTargetSelection.persistSettings(section);
		}
	}
}
