/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [preferences] Add preference for new compiler warning: MissingSynchronizedModifierInInheritedMethod - https://bugs.eclipse.org/bugs/show_bug.cgi?id=245240
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - [compiler][null] inheritance of null annotations as an option - https://bugs.eclipse.org/388281
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.util.Arrays;
import java.util.List;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.osgi.util.NLS;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.DefaultScope;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contentassist.ContentAssistHandler;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.StubTypeContext;
import org.eclipse.jdt.internal.corext.refactoring.TypeContextChecker;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.dialogs.TableTextCellEditor;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.preferences.FilteredPreferenceTree.PreferenceTreeNode;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.CompletionContextRequestor;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;


public class ProblemSeveritiesConfigurationBlock extends OptionsConfigurationBlock {

	private interface IAnnotationDialogField {
		String getErrorMessage();
		void setStatus(NullAnnotationsConfigurationDialog.AnnotationWrapper element, IStatus newStatus);
	}

	private class NullAnnotationsConfigurationDialog extends StatusDialog {

		private static final String COLUMN_ANNOTATION= "annotation"; //$NON-NLS-1$
		private static final String EMPTY_STRING= ""; //$NON-NLS-1$

		private class AnnotationDialogField extends StringDialogField implements IAnnotationDialogField {

			final String fErrorMessage;
			IStatus fStatus;

			public AnnotationDialogField(String errorMessage) {
				fErrorMessage= errorMessage;
				fStatus= new StatusInfo();
			}
			@Override
			public String getText() {
				return super.getText().trim();
			}
			@Override
			public String getErrorMessage() {
				return fErrorMessage;
			}
			public IStatus getStatus() {
				return fStatus;
			}
			@Override
			public void setStatus(AnnotationWrapper element, IStatus newStatus) {
				fStatus= newStatus;
			}
			IStatus doValidation() {
				return NullAnnotationsConfigurationDialog.this.doValidation(this, null, getText(), true);
			}
		}

		private class AnnotationWrapper { // mimic a mutable string
			public String annotationName;
			public IStatus status; // null means: no status computed, yet.

			public AnnotationWrapper(String annotationName) {
				this.annotationName= annotationName;
			}
		}

		private class AnnotationListDialogField extends ListDialogField<AnnotationWrapper> implements IAnnotationDialogField {

			final String fErrorMessage;

			public AnnotationListDialogField(IListAdapter<AnnotationWrapper> adapter, String[] buttonLabels, ILabelProvider lprovider, String errorMessage) {
				super(adapter, buttonLabels, lprovider);
				fErrorMessage= errorMessage;
			}

			public void addElementsFromCommaSeparatedList(String value) {
				value= value.trim();
				if (value.isEmpty())
					return;
				for (String string : value.split(",")) { //$NON-NLS-1$
					addElement(new AnnotationWrapper(string.trim()));
				}
			}

			public String getCommaSeparatedElements() {
				List<AnnotationWrapper> elements= getElements();
				if (elements.isEmpty()) return EMPTY_STRING;
				StringBuilder buf= new StringBuilder();
				for (int i= 0; i < elements.size(); i++) {
					String annotationName= elements.get(i).annotationName;
					if (annotationName.isEmpty())
						continue;
					if (i > 0) buf.append(',');
					buf.append(annotationName);
				}
				return buf.toString();
			}

			@Override
			public void elementChanged(AnnotationWrapper element) throws IllegalArgumentException {
				super.elementChanged(element);
				NullAnnotationsConfigurationDialog.this.doValidation(this, element, element.annotationName, false);
			}
			@Override
			public String getErrorMessage() {
				return fErrorMessage;
			}
			public IStatus getMostSevereStatus() {
				List<AnnotationWrapper> elements= getElements();
				if (elements.isEmpty())
					return new StatusInfo();
				IStatus[] all= new IStatus[elements.size()];
				for (int i= 0; i < elements.size(); i++) {
					all[i]= elements.get(i).status;
					if (all[i] == null)
						all[i]= new StatusInfo();
				}
				return StatusUtil.getMostSevere(all);
			}
			@Override
			public void setStatus(AnnotationWrapper element, IStatus newStatus) {
				element.status= newStatus;
			}
		}

		private class FieldListener implements ModifyListener {
			AnnotationDialogField fField;
			FieldListener(AnnotationDialogField field) {
				fField= field;
			}

			@Override
			public void modifyText(ModifyEvent e) {
				fField.doValidation();
			}
		}

		private class AnnotationCompletionContextRequestor extends CompletionContextRequestor {
			@Override
			public StubTypeContext getStubTypeContext() {
				return TypeContextChecker.createAnnotationStubTypeContext(fProject);
			}
		}

		private class AnnotationListAdapter implements IListAdapter<AnnotationWrapper> {

			@Override
			public void customButtonPressed(ListDialogField<AnnotationWrapper> field, int index) {
				if (index == 0) { // "Add"
					AnnotationWrapper newElement= new AnnotationWrapper(EMPTY_STRING);
					field.addElement(newElement);
					field.editElement(newElement);
				}
			}

			@Override
			public void selectionChanged(ListDialogField<AnnotationWrapper> field) {
				// nothing
			}

			@Override
			public void doubleClicked(ListDialogField<AnnotationWrapper> field) {
				// nothing
			}
		}

		private class AnnotationListLabelProvider extends LabelProvider {

			AnnotationListDialogField fField;

			void setDialogField(AnnotationListDialogField field) {
				fField= field;
			}

			@Override
			public String getText(Object element) {
				return BasicElementLabels.getJavaElementName(((AnnotationWrapper) element).annotationName);
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof AnnotationWrapper) {
					AnnotationWrapper annotationWrapper= (AnnotationWrapper) element;
					IStatus status= annotationWrapper.status;
					if (status == null) {
						status= validateNullnessAnnotation(annotationWrapper.annotationName, fField.getErrorMessage(), false);
						annotationWrapper.status= status;
					}
					switch (status.getSeverity()) {
						case IStatus.INFO:
							return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_INFO);
						case IStatus.ERROR:
							return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_ERROR);
						default:
							// fall through
					}
				}
				return JavaPluginImages.get(JavaPluginImages.IMG_BLANK);
			}
		}

		private static final int RESTORE_DEFAULTS_BUTTON_ID= IDialogConstants.CLIENT_ID + 1;


		private AnnotationDialogField fNullableAnnotationDialogField;
		private AnnotationDialogField fNonNullAnnotationDialogField;
		private AnnotationDialogField fNonNullByDefaultAnnotationDialogField;
		private AnnotationListDialogField fOtherNullableAnnotationsDialogField;
		private AnnotationListDialogField fOtherNonNullAnnotationsDialogField;
		private AnnotationListDialogField fOtherNonNullByDefaultAnnotationsDialogField;

		private NullAnnotationsConfigurationDialog() {
			super(ProblemSeveritiesConfigurationBlock.this.getShell());

			setTitle(PreferencesMessages.NullAnnotationsConfigurationDialog_title);

			String errorMessage= PreferencesMessages.NullAnnotationsConfigurationDialog_nullable_annotation_error;
			fNullableAnnotationDialogField= createAnnotationDialogField(PREF_NULLABLE_ANNOTATION_NAME, errorMessage);
			fOtherNullableAnnotationsDialogField= createAnnotationListDialogField(PREF_NULLABLE_ANNOTATION_SECONDARY_NAMES, errorMessage);

			errorMessage= PreferencesMessages.NullAnnotationsConfigurationDialog_nonnull_annotation_error;
			fNonNullAnnotationDialogField= createAnnotationDialogField(PREF_NONNULL_ANNOTATION_NAME, errorMessage);
			fOtherNonNullAnnotationsDialogField= createAnnotationListDialogField(PREF_NONNULL_ANNOTATION_SECONDARY_NAMES, errorMessage);

			errorMessage= PreferencesMessages.NullAnnotationsConfigurationDialog_nonnullbydefault_annotation_error;
			fNonNullByDefaultAnnotationDialogField= createAnnotationDialogField(PREF_NONNULL_BY_DEFAULT_ANNOTATION_NAME, errorMessage);
			fOtherNonNullByDefaultAnnotationsDialogField= createAnnotationListDialogField(PREF_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES, errorMessage);
		}

		private AnnotationDialogField createAnnotationDialogField(Key key, String errorMessage) {
			AnnotationDialogField field= new AnnotationDialogField(errorMessage);
			field.setLabelText(PreferencesMessages.NullAnnotationsConfigurationDialog_primary_label);
			field.setText(getValue(key));
			return field;
		}

		private AnnotationListDialogField createAnnotationListDialogField(Key key, String errorMessage) {
			String[] buttons= new String[] { PreferencesMessages.NullAnnotationsConfigurationDialog_add_button };
			AnnotationListLabelProvider annotationLabelProvider= new AnnotationListLabelProvider();
			AnnotationListDialogField field= new AnnotationListDialogField(new AnnotationListAdapter(), buttons, annotationLabelProvider, errorMessage);
			field.setLabelText(PreferencesMessages.NullAnnotationsConfigurationDialog_secondary_label);
			field.setTableColumns(new ListDialogField.ColumnsDescription(1, false));
			field.addElementsFromCommaSeparatedList(getValue(key));
			annotationLabelProvider.setDialogField(field);
			return field;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			initializeDialogUnits(parent);

			GridLayout layout= (GridLayout) composite.getLayout();
			layout.numColumns= 1;

			int fieldWidthHint= convertWidthInCharsToPixels(90); // heuristic to match the default size of the dialog

			Label intro= new Label(composite, SWT.WRAP);
			intro.setText(PreferencesMessages.NullAnnotationsConfigurationDialog_null_annotations_description);
	    	intro.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, layout.numColumns, 1));
			LayoutUtil.setWidthHint(intro, fieldWidthHint);

			String[] texts= { PreferencesMessages.NullAnnotationsConfigurationDialog_nullable_annotations_label,
					PreferencesMessages.NullAnnotationsConfigurationDialog_nullable_annotations_description };
			createNullAnnotationGroup(fNullableAnnotationDialogField, fOtherNullableAnnotationsDialogField, composite, texts, fieldWidthHint);

			texts= new String[] { PreferencesMessages.NullAnnotationsConfigurationDialog_nonnull_annotations_label,
					PreferencesMessages.NullAnnotationsConfigurationDialog_nonnull_annotations_description };
			createNullAnnotationGroup(fNonNullAnnotationDialogField, fOtherNonNullAnnotationsDialogField, composite, texts, fieldWidthHint);


			texts= new String[] { PreferencesMessages.NullAnnotationsConfigurationDialog_nonnullbydefault_annotations_label,
					PreferencesMessages.NullAnnotationsConfigurationDialog_nonnullbydefault_annotations_description };
			createNullAnnotationGroup(fNonNullByDefaultAnnotationDialogField, fOtherNonNullByDefaultAnnotationsDialogField, composite, texts, fieldWidthHint);

			fNullableAnnotationDialogField.postSetFocusOnDialogField(parent.getDisplay());

			applyDialogFont(composite);
			return composite;
		}

		private void createNullAnnotationGroup(AnnotationDialogField primaryField, final AnnotationListDialogField secondaryList, Composite parent, String[] texts, int fieldWidthHint) {
			Group group= new Group(parent, SWT.NONE);
			GridLayout layout= new GridLayout(3, false);
			layout.marginLeft= convertWidthInCharsToPixels(2);
			GridData groupData= new GridData(SWT.FILL, SWT.FILL, true, true);

			// compensate different height of intro texts when suggesting height of the group:
			GC gc= new GC(parent);
			gc.setFont(JFaceResources.getDialogFont());
			Point size= gc.stringExtent(texts[1]);
			int lines= (size.x / fieldWidthHint) + 1;
			gc.dispose();
			groupData.heightHint= convertHeightInCharsToPixels(8+lines);

			group.setLayoutData(groupData);
			group.setLayout(layout);
			group.setText(texts[0]);

			Label intro= new Label(group, SWT.WRAP);
			intro.setText(texts[1]);
			intro.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, layout.numColumns, 1));
			LayoutUtil.setWidthHint(intro, 1); // force text wrapping


			primaryField.doFillIntoGrid(group, 2);
			addSpacer(group); // button column is empty for the primary field

			Text text= primaryField.getTextControl(null);
			((GridData)text.getLayoutData()).grabExcessHorizontalSpace= true;
			text.addModifyListener(new FieldListener(primaryField));
			TextFieldNavigationHandler.install(text);
			BidiUtils.applyBidiProcessing(text, StructuredTextTypeHandlerFactory.JAVA);

			if (fProject != null) {
				JavaTypeCompletionProcessor annotationCompletionProcessor= new JavaTypeCompletionProcessor(false, false, true);
				annotationCompletionProcessor.setCompletionContextRequestor(new AnnotationCompletionContextRequestor());
				ControlContentAssistHelper.createTextContentAssistant(text, annotationCompletionProcessor);
			}


			secondaryList.doFillIntoGrid(group, 3);
			final TableViewer tableViewer= secondaryList.getTableViewer();
			tableViewer.setColumnProperties(new String[] {COLUMN_ANNOTATION});
			TableTextCellEditor cellEditor= new TableTextCellEditor(tableViewer, 0) {
				@Override
				protected Control createControl(Composite parent2) {
					Control control= super.createControl(parent2);
					BidiUtils.applyBidiProcessing(text, StructuredTextTypeHandlerFactory.JAVA);
					return control;
				}

				@Override
			    protected void keyReleaseOccured(KeyEvent event) {
					if (event.keyCode == SWT.F2 && event.stateMask == 0) {
						tableViewer.refresh(); // ensure icon is updated on F2
					}
			    	super.keyReleaseOccured(event);
			    }
			};
			Text cellEditorText= cellEditor.getText();
			TextFieldNavigationHandler.install(cellEditorText);
			if (fProject != null) {
				JavaTypeCompletionProcessor annotationCompletionProcessor= new JavaTypeCompletionProcessor(false, false, true);
				annotationCompletionProcessor.setCompletionContextRequestor(new AnnotationCompletionContextRequestor());
				SubjectControlContentAssistant contentAssistant= ControlContentAssistHelper.createJavaContentAssistant(annotationCompletionProcessor);
				ContentAssistHandler.createHandlerForText(cellEditorText, contentAssistant);
				cellEditor.setContentAssistant(contentAssistant);
			}

			tableViewer.setCellEditors(new CellEditor[] { cellEditor });
			tableViewer.setCellModifier(new ICellModifier() {
				@Override
				public void modify(Object element, String property, Object value) {
					if (element instanceof Item)
						element= ((Item) element).getData();

					AnnotationWrapper annotationWrapper= (AnnotationWrapper) element;
					annotationWrapper.annotationName= ((String) value).trim();
					secondaryList.elementChanged(annotationWrapper);
				}
				@Override
				public Object getValue(Object element, String property) {
					return ((AnnotationWrapper) element).annotationName;
				}
				@Override
				public boolean canModify(Object element, String property) {
					return true;
				}
			});
			tableViewer.getTable().addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent event) {
					if (event.keyCode == SWT.F2 && event.stateMask == 0) {
						ISelection selection= tableViewer.getSelection();
						if (! (selection instanceof IStructuredSelection))
							return;
						IStructuredSelection structuredSelection= (IStructuredSelection) selection;
						if (!structuredSelection.isEmpty())
							tableViewer.editElement(structuredSelection.getFirstElement(), 0);
					}
				}
			});
		}

		private Label addSpacer(Group group) {
			return new Label(group,  SWT.NONE);
		}

		@Override
		public void create() {
			super.create(); // cannot show error status before this super call
			StringDialogField firstErrorField= null;
			IStatus firstError= null;
			AnnotationDialogField[] primaryFields= { fNullableAnnotationDialogField, fNonNullAnnotationDialogField, fNonNullByDefaultAnnotationDialogField };
			for (AnnotationDialogField field : primaryFields) {
				IStatus status= field.doValidation();
				if (status.getSeverity() == IStatus.ERROR && firstError == null) {
					firstErrorField= field;
					firstError= status;
				}
			}
			if (firstErrorField != null && firstError != null) {
				updateStatus(firstError);
				firstErrorField.postSetFocusOnDialogField(dialogArea.getDisplay());
			}
		}

		private IStatus doValidation(IAnnotationDialogField dialogField, AnnotationWrapper element, String newValue, boolean isTypeMandatory) {
			IStatus fieldStatus= validateNullnessAnnotation(newValue, dialogField.getErrorMessage(), isTypeMandatory);
			if (fieldStatus != null) {
				dialogField.setStatus(element, fieldStatus);

				// compute most severe among all known statuses, preferring fieldStatus then first-found if equal severities:
				IStatus mostSevereStatus= StatusUtil.getMoreSevere(fNullableAnnotationDialogField.getStatus(), fieldStatus);
				mostSevereStatus= StatusUtil.getMoreSevere(fNonNullAnnotationDialogField.getStatus(), mostSevereStatus);
				mostSevereStatus= StatusUtil.getMoreSevere(fNonNullByDefaultAnnotationDialogField.getStatus(), mostSevereStatus);
				mostSevereStatus= StatusUtil.getMoreSevere(fOtherNullableAnnotationsDialogField.getMostSevereStatus(), mostSevereStatus);
				mostSevereStatus= StatusUtil.getMoreSevere(fOtherNonNullAnnotationsDialogField.getMostSevereStatus(), mostSevereStatus);
				mostSevereStatus= StatusUtil.getMoreSevere(fOtherNonNullByDefaultAnnotationsDialogField.getMostSevereStatus(), mostSevereStatus);
				updateStatus(mostSevereStatus);
				return fieldStatus;
			}
			return new StatusInfo();
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, RESTORE_DEFAULTS_BUTTON_ID, PreferencesMessages.NullAnnotationsConfigurationDialog_restore_defaults, false);
			super.createButtonsForButtonBar(parent);
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == RESTORE_DEFAULTS_BUTTON_ID) {
				fNullableAnnotationDialogField.setText(NULL_ANNOTATIONS_DEFAULTS[0]);
				fNonNullAnnotationDialogField.setText(NULL_ANNOTATIONS_DEFAULTS[1]);
				fNonNullByDefaultAnnotationDialogField.setText(NULL_ANNOTATIONS_DEFAULTS[2]);
				fOtherNullableAnnotationsDialogField.removeAllElements();
				fOtherNonNullAnnotationsDialogField.removeAllElements();
				fOtherNonNullByDefaultAnnotationsDialogField.removeAllElements();
			} else {
				super.buttonPressed(buttonId);
			}
		}

		public String[] getResult() {
			return new String[] {
					fNullableAnnotationDialogField.getText(),
					fNonNullAnnotationDialogField.getText(),
					fNonNullByDefaultAnnotationDialogField.getText(),
					fOtherNullableAnnotationsDialogField.getCommaSeparatedElements(),
					fOtherNonNullAnnotationsDialogField.getCommaSeparatedElements(),
					fOtherNonNullByDefaultAnnotationsDialogField.getCommaSeparatedElements()
			};
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.PROBLEM_SEVERITIES_PROPERTY_PAGE);
		}
	}

	private static final String SETTINGS_SECTION_NAME= "ProblemSeveritiesConfigurationBlock";  //$NON-NLS-1$

	// Preference store keys, see JavaCore.getOptions
	private static final Key PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD);
	private static final Key PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME= getJDTCoreKey(JavaCore.COMPILER_PB_METHOD_WITH_CONSTRUCTOR_NAME);
	private static final Key PREF_PB_DEPRECATION= getJDTCoreKey(JavaCore.COMPILER_PB_DEPRECATION);
	private static final Key PREF_PB_DEPRECATION_IN_DEPRECATED_CODE=getJDTCoreKey(JavaCore.COMPILER_PB_DEPRECATION_IN_DEPRECATED_CODE);
	private static final Key PREF_PB_DEPRECATION_WHEN_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_DEPRECATION_WHEN_OVERRIDING_DEPRECATED_METHOD);
	private static final Key PREF_PB_TERMINAL_DEPRECATION= getJDTCoreKey(JavaCore.COMPILER_PB_TERMINAL_DEPRECATION);

	private static final Key PREF_PB_API_LEAKS= getJDTCoreKey(JavaCore.COMPILER_PB_API_LEAKS);
	private static final Key PREF_PB_UNSTABLE_AUTO_MODULE_NAME= getJDTCoreKey(JavaCore.COMPILER_PB_UNSTABLE_AUTO_MODULE_NAME);

	private static final Key PREF_PB_HIDDEN_CATCH_BLOCK= getJDTCoreKey(JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK);
	private static final Key PREF_PB_UNUSED_LOCAL= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_LOCAL);
	private static final Key PREF_PB_UNUSED_PARAMETER= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER);
	private static final Key PREF_PB_UNUSED_EXCEPTION_PARAMETER= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_EXCEPTION_PARAMETER);
	private static final Key PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_OVERRIDING_CONCRETE);
	private static final Key PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE);
	private static final Key PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PARAMETER_WHEN_IMPLEMENTING_ABSTRACT);
	private static final Key PREF_PB_SYNTHETIC_ACCESS_EMULATION= getJDTCoreKey(JavaCore.COMPILER_PB_SYNTHETIC_ACCESS_EMULATION);
	private static final Key PREF_PB_NON_EXTERNALIZED_STRINGS= getJDTCoreKey(JavaCore.COMPILER_PB_NON_NLS_STRING_LITERAL);
	private static final Key PREF_PB_UNUSED_IMPORT= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_IMPORT);
	private static final Key PREF_PB_UNUSED_PRIVATE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER);
	private static final Key PREF_PB_UNUSED_TYPE_PARAMETER= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_TYPE_PARAMETER);
	private static final Key PREF_PB_STATIC_ACCESS_RECEIVER= getJDTCoreKey(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER);
	private static final Key PREF_PB_NO_EFFECT_ASSIGNMENT= getJDTCoreKey(JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT);
	private static final Key PREF_PB_CHAR_ARRAY_IN_CONCAT= getJDTCoreKey(JavaCore.COMPILER_PB_CHAR_ARRAY_IN_STRING_CONCATENATION);
	private static final Key PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT= getJDTCoreKey(JavaCore.COMPILER_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT);
	private static final Key PREF_PB_LOCAL_VARIABLE_HIDING= getJDTCoreKey(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING);
	private static final Key PREF_PB_FIELD_HIDING= getJDTCoreKey(JavaCore.COMPILER_PB_FIELD_HIDING);
	private static final Key PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD= getJDTCoreKey(JavaCore.COMPILER_PB_SPECIAL_PARAMETER_HIDING_FIELD);
	private static final Key PREF_PB_INDIRECT_STATIC_ACCESS= getJDTCoreKey(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS);
	private static final Key PREF_PB_EMPTY_STATEMENT= getJDTCoreKey(JavaCore.COMPILER_PB_EMPTY_STATEMENT);
	private static final Key PREF_PB_UNNECESSARY_ELSE= getJDTCoreKey(JavaCore.COMPILER_PB_UNNECESSARY_ELSE);
	private static final Key PREF_PB_UNNECESSARY_TYPE_CHECK= getJDTCoreKey(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK);
	private static final Key PREF_PB_INCOMPATIBLE_INTERFACE_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_INCOMPATIBLE_NON_INHERITED_INTERFACE_METHOD);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE);
	private static final Key PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE);
	private static final Key PREF_PB_MISSING_SERIAL_VERSION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_SERIAL_VERSION);
	private static final Key PREF_PB_UNDOCUMENTED_EMPTY_BLOCK= getJDTCoreKey(JavaCore.COMPILER_PB_UNDOCUMENTED_EMPTY_BLOCK);
	private static final Key PREF_PB_FINALLY_BLOCK_NOT_COMPLETING= getJDTCoreKey(JavaCore.COMPILER_PB_FINALLY_BLOCK_NOT_COMPLETING);
	private static final Key PREF_PB_UNQUALIFIED_FIELD_ACCESS= getJDTCoreKey(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS);
	private static final Key PREF_PB_MISSING_DEPRECATED_ANNOTATION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_DEPRECATED_ANNOTATION);
	private static final Key PREF_PB_FORBIDDEN_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE);
	private static final Key PREF_PB_DISCOURRAGED_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE);
	private static final Key PREF_PB_UNUSED_LABEL= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_LABEL);
	private static final Key PREF_PB_PARAMETER_ASSIGNMENT= getJDTCoreKey(JavaCore.COMPILER_PB_PARAMETER_ASSIGNMENT);
	private static final Key PREF_PB_FALLTHROUGH_CASE= getJDTCoreKey(JavaCore.COMPILER_PB_FALLTHROUGH_CASE);
	private static final Key PREF_PB_COMPARING_IDENTICAL= getJDTCoreKey(JavaCore.COMPILER_PB_COMPARING_IDENTICAL);
	private static final Key PREF_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD);

	private static final Key PREF_PB_NULL_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_NULL_REFERENCE);
	private static final Key PREF_PB_POTENTIAL_NULL_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE);

	private static final Key PREF_ANNOTATION_NULL_ANALYSIS= getJDTCoreKey(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS);

	private static final Key PREF_PB_SYNTACTIC_NULL_ANLYSIS_FOR_FIELDS= getJDTCoreKey(JavaCore.COMPILER_PB_SYNTACTIC_NULL_ANALYSIS_FOR_FIELDS);

	private static final Key PREF_INHERIT_NULL_ANNOTATIONS= getJDTCoreKey(JavaCore.COMPILER_INHERIT_NULL_ANNOTATIONS);

	private static final Key PREF_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS= getJDTCoreKey(JavaCore.CORE_JAVA_BUILD_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS);
	/**
	 * Key for the "Use default annotations for null " setting.
	 * <p>Values are { {@link #ENABLED}, {@link #DISABLED} }.
	 */
	private static final Key INTR_DEFAULT_NULL_ANNOTATIONS= getLocalKey("internal.default.null.annotations"); //$NON-NLS-1$
	private static final Key PREF_NULLABLE_ANNOTATION_NAME= getJDTCoreKey(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME);
	private static final Key PREF_NONNULL_ANNOTATION_NAME= getJDTCoreKey(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME);
	private static final Key PREF_NONNULL_BY_DEFAULT_ANNOTATION_NAME= getJDTCoreKey(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME);
	private static final Key PREF_NULLABLE_ANNOTATION_SECONDARY_NAMES= getJDTCoreKey(JavaCore.COMPILER_NULLABLE_ANNOTATION_SECONDARY_NAMES);
	private static final Key PREF_NONNULL_ANNOTATION_SECONDARY_NAMES= getJDTCoreKey(JavaCore.COMPILER_NONNULL_ANNOTATION_SECONDARY_NAMES);
	private static final Key PREF_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES= getJDTCoreKey(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES);
	private static final String[] NULL_ANNOTATIONS_DEFAULTS= {
		PREF_NULLABLE_ANNOTATION_NAME.getStoredValue(DefaultScope.INSTANCE, null),
		PREF_NONNULL_ANNOTATION_NAME.getStoredValue(DefaultScope.INSTANCE, null),
		PREF_NONNULL_BY_DEFAULT_ANNOTATION_NAME.getStoredValue(DefaultScope.INSTANCE, null),
	};

	private static final Key PREF_MISSING_NONNULL_BY_DEFAULT_ANNOTATION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_NONNULL_BY_DEFAULT_ANNOTATION);

	private static final Key PREF_PB_NULL_SPECIFICATION_VIOLATION= getJDTCoreKey(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION);
	private static final Key PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT= getJDTCoreKey(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT);
	private static final Key PREF_PB_NULL_UNCHECKED_CONVERSION= getJDTCoreKey(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION);
	private static final Key PREF_PB_ANNOTATED_TYPE_ARGUMENT_TO_UNANNOTATED= getJDTCoreKey(JavaCore.COMPILER_PB_ANNOTATED_TYPE_ARGUMENT_TO_UNANNOTATED);
	private static final Key PREF_PB_PESSIMISTIC_NULL_ANALYSIS_FOR_FREE_TYPE_VARIABLES= getJDTCoreKey(JavaCore.COMPILER_PB_PESSIMISTIC_NULL_ANALYSIS_FOR_FREE_TYPE_VARIABLES);
	private static final Key PREF_PB_NONNULL_TYPEVAR_FROM_LEGACY_INVOCATION= getJDTCoreKey(JavaCore.COMPILER_PB_NONNULL_TYPEVAR_FROM_LEGACY_INVOCATION);
	private static final Key PREF_PB_REDUNDANT_NULL_ANNOTATION= getJDTCoreKey(JavaCore.COMPILER_PB_REDUNDANT_NULL_ANNOTATION);
	private static final Key PREF_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED= getJDTCoreKey(JavaCore.COMPILER_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED);

	private static final Key PREF_PB_REDUNDANT_NULL_CHECK= getJDTCoreKey(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK);

	private static final Key PREF_PB_UNCLOSED_CLOSEABLE= getJDTCoreKey(JavaCore.COMPILER_PB_UNCLOSED_CLOSEABLE);
	private static final Key PREF_PB_POTENTIALLY_UNCLOSED_CLOSEABLE= getJDTCoreKey(JavaCore.COMPILER_PB_POTENTIALLY_UNCLOSED_CLOSEABLE);
	private static final Key PREF_PB_EXPLICITLY_CLOSED_AUTOCLOSEABLE= getJDTCoreKey(JavaCore.COMPILER_PB_EXPLICITLY_CLOSED_AUTOCLOSEABLE);

	private static final Key PREF_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS= getJDTCoreKey(JavaCore.COMPILER_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS);
	private static final Key PREF_PB_REDUNDANT_SUPERINTERFACE= getJDTCoreKey(JavaCore.COMPILER_PB_REDUNDANT_SUPERINTERFACE);

	private static final Key PREF_PB_UNUSED_WARNING_TOKEN= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_WARNING_TOKEN);
	private static final Key PREF_PB_SUPPRESS_WARNINGS_NOT_FULLY_ANALYSED= getJDTCoreKey(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS_NOT_FULLY_ANALYSED);

	private static final Key PREF_15_PB_UNCHECKED_TYPE_OPERATION= getJDTCoreKey(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION);
	private static final Key PREF_15_PB_FINAL_PARAM_BOUND= getJDTCoreKey(JavaCore.COMPILER_PB_FINAL_PARAMETER_BOUND);
	private static final Key PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST= getJDTCoreKey(JavaCore.COMPILER_PB_VARARGS_ARGUMENT_NEED_CAST);
	private static final Key PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE= getJDTCoreKey(JavaCore.COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE);
	private static final Key PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE_STRICT= getJDTCoreKey(JavaCore.COMPILER_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE_STRICT);
	private static final Key PREF_15_PB_UNLIKELY_EQUALS_ARGUMENT_TYPE= getJDTCoreKey(JavaCore.COMPILER_PB_UNLIKELY_EQUALS_ARGUMENT_TYPE);
	private static final Key PREF_15_PB_AUTOBOXING_PROBLEM= getJDTCoreKey(JavaCore.COMPILER_PB_AUTOBOXING);

	private static final Key PREF_15_PB_MISSING_OVERRIDE_ANNOTATION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION);
	private static final Key PREF_16_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION);
	private static final Key PREF_15_PB_ANNOTATION_SUPER_INTERFACE= getJDTCoreKey(JavaCore.COMPILER_PB_ANNOTATION_SUPER_INTERFACE);
	private static final Key PREF_15_PB_TYPE_PARAMETER_HIDING= getJDTCoreKey(JavaCore.COMPILER_PB_TYPE_PARAMETER_HIDING);
	private static final Key PREF_15_PB_INCOMPLETE_ENUM_SWITCH= getJDTCoreKey(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH);
	private static final Key PREF_15_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT);
	private static final Key PREF_15_PB_SWITCH_MISSING_DEFAULT_CASE= getJDTCoreKey(JavaCore.COMPILER_PB_SWITCH_MISSING_DEFAULT_CASE);
	private static final Key PREF_15_PB_RAW_TYPE_REFERENCE= getJDTCoreKey(JavaCore.COMPILER_PB_RAW_TYPE_REFERENCE);
	private static final Key PREF_17_PB_REDUNDANT_TYPE_ARGUMENTS= getJDTCoreKey(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS);
	private static final Key PREF_15_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS= getJDTCoreKey(JavaCore.COMPILER_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS);

	private static final Key PREF_PB_SUPPRESS_WARNINGS= getJDTCoreKey(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS);
	private static final Key PREF_PB_SUPPRESS_OPTIONAL_ERRORS= getJDTCoreKey(JavaCore.COMPILER_PB_SUPPRESS_OPTIONAL_ERRORS);
	private static final Key PREF_PB_UNHANDLED_WARNING_TOKEN= getJDTCoreKey(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN);
	private static final Key PREF_PB_FATAL_OPTIONAL_ERROR= getJDTCoreKey(JavaCore.COMPILER_PB_FATAL_OPTIONAL_ERROR);

	private static final Key PREF_PB_MISSING_HASHCODE_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD);
	private static final Key PREF_PB_DEAD_CODE= getJDTCoreKey(JavaCore.COMPILER_PB_DEAD_CODE);
	private static final Key PREF_PB_UNUSED_OBJECT_ALLOCATION= getJDTCoreKey(JavaCore.COMPILER_PB_UNUSED_OBJECT_ALLOCATION);
	private static final Key PREF_PB_MISSING_STATIC_ON_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_MISSING_STATIC_ON_METHOD);
	private static final Key PREF_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD= getJDTCoreKey(JavaCore.COMPILER_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD);

	// values
	private static final String ERROR= JavaCore.ERROR;
	private static final String WARNING= JavaCore.WARNING;
	private static final String INFO= JavaCore.INFO;
	private static final String IGNORE= JavaCore.IGNORE;

	private static final String ENABLED= JavaCore.ENABLED;
	private static final String DISABLED= JavaCore.DISABLED;

	private PixelConverter fPixelConverter;

	private PreferenceTree fFilteredPrefTree;

	public ProblemSeveritiesConfigurationBlock(IStatusChangeListener context, IProject project, IWorkbenchPreferenceContainer container) {
		super(context, project, getKeys(), container);

		// Compatibility code for the merge of the two option PB_SIGNAL_PARAMETER:
		if (ENABLED.equals(getValue(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT))) {
			setValue(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, ENABLED);
		}
	}

	public static Key[] getKeys() {
		return new Key[] {
				PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD,
				PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, PREF_PB_DEPRECATION, PREF_PB_TERMINAL_DEPRECATION, PREF_PB_HIDDEN_CATCH_BLOCK, PREF_PB_UNUSED_LOCAL,
				PREF_PB_API_LEAKS,
				PREF_PB_UNSTABLE_AUTO_MODULE_NAME,
				PREF_PB_UNUSED_PARAMETER, PREF_PB_UNUSED_EXCEPTION_PARAMETER, PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE,
				PREF_PB_SYNTHETIC_ACCESS_EMULATION, PREF_PB_NON_EXTERNALIZED_STRINGS,
				PREF_PB_UNUSED_IMPORT, PREF_PB_UNUSED_LABEL,
				PREF_PB_STATIC_ACCESS_RECEIVER, PREF_PB_DEPRECATION_IN_DEPRECATED_CODE,
				PREF_PB_NO_EFFECT_ASSIGNMENT, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD,
				PREF_PB_UNUSED_PRIVATE, PREF_PB_UNUSED_TYPE_PARAMETER, PREF_PB_CHAR_ARRAY_IN_CONCAT, PREF_PB_UNNECESSARY_ELSE,
				PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, PREF_PB_LOCAL_VARIABLE_HIDING, PREF_PB_FIELD_HIDING,
				PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD, PREF_PB_INDIRECT_STATIC_ACCESS,
				PREF_PB_EMPTY_STATEMENT, PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT,
				PREF_PB_UNNECESSARY_TYPE_CHECK, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, PREF_PB_UNQUALIFIED_FIELD_ACCESS,
				PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, PREF_PB_DEPRECATION_WHEN_OVERRIDING,
				PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE,
				PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE,
				PREF_PB_MISSING_SERIAL_VERSION, PREF_PB_PARAMETER_ASSIGNMENT,
				PREF_PB_NULL_REFERENCE, PREF_PB_POTENTIAL_NULL_REFERENCE,
				PREF_ANNOTATION_NULL_ANALYSIS,
				INTR_DEFAULT_NULL_ANNOTATIONS,
				PREF_NULLABLE_ANNOTATION_NAME,
				PREF_NULLABLE_ANNOTATION_SECONDARY_NAMES,
				PREF_NONNULL_ANNOTATION_NAME,
				PREF_NONNULL_ANNOTATION_SECONDARY_NAMES,
				PREF_NONNULL_BY_DEFAULT_ANNOTATION_NAME,
				PREF_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES,
				PREF_MISSING_NONNULL_BY_DEFAULT_ANNOTATION,
				PREF_PB_NULL_SPECIFICATION_VIOLATION,
				PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT,
				PREF_PB_NULL_UNCHECKED_CONVERSION,
				PREF_PB_ANNOTATED_TYPE_ARGUMENT_TO_UNANNOTATED,
				PREF_PB_PESSIMISTIC_NULL_ANALYSIS_FOR_FREE_TYPE_VARIABLES,
				PREF_PB_NONNULL_TYPEVAR_FROM_LEGACY_INVOCATION,
				PREF_PB_REDUNDANT_NULL_ANNOTATION,
				PREF_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED,
				PREF_PB_REDUNDANT_NULL_CHECK, PREF_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS,
				PREF_PB_SYNTACTIC_NULL_ANLYSIS_FOR_FIELDS,
				PREF_INHERIT_NULL_ANNOTATIONS,
				PREF_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS,
				PREF_PB_UNCLOSED_CLOSEABLE, PREF_PB_POTENTIALLY_UNCLOSED_CLOSEABLE, PREF_PB_EXPLICITLY_CLOSED_AUTOCLOSEABLE,
				PREF_PB_FALLTHROUGH_CASE, PREF_PB_REDUNDANT_SUPERINTERFACE, PREF_PB_UNUSED_WARNING_TOKEN, PREF_PB_SUPPRESS_WARNINGS_NOT_FULLY_ANALYSED,
				PREF_15_PB_UNCHECKED_TYPE_OPERATION, PREF_15_PB_FINAL_PARAM_BOUND, PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST,
				PREF_15_PB_AUTOBOXING_PROBLEM, PREF_15_PB_MISSING_OVERRIDE_ANNOTATION, PREF_16_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION,
				PREF_15_PB_ANNOTATION_SUPER_INTERFACE,
				PREF_15_PB_TYPE_PARAMETER_HIDING,
				PREF_15_PB_INCOMPLETE_ENUM_SWITCH, PREF_15_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT, PREF_15_PB_SWITCH_MISSING_DEFAULT_CASE,
				PREF_PB_MISSING_DEPRECATED_ANNOTATION,
				PREF_15_PB_RAW_TYPE_REFERENCE, PREF_15_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS, PREF_17_PB_REDUNDANT_TYPE_ARGUMENTS,
				PREF_PB_FATAL_OPTIONAL_ERROR,
				PREF_PB_FORBIDDEN_REFERENCE, PREF_PB_DISCOURRAGED_REFERENCE,
				PREF_PB_SUPPRESS_WARNINGS, PREF_PB_SUPPRESS_OPTIONAL_ERRORS,
				PREF_PB_UNHANDLED_WARNING_TOKEN,
				PREF_PB_COMPARING_IDENTICAL, PREF_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, PREF_PB_MISSING_HASHCODE_METHOD,
				PREF_PB_DEAD_CODE, PREF_PB_UNUSED_OBJECT_ALLOCATION,
				PREF_PB_MISSING_STATIC_ON_METHOD, PREF_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD,
				PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE,
				PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE_STRICT,
				PREF_15_PB_UNLIKELY_EQUALS_ARGUMENT_TYPE
			};
	}

	/*
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		fPixelConverter= new PixelConverter(parent);
		setShell(parent.getShell());

		Composite mainComp= new Composite(parent, SWT.NONE);
		mainComp.setFont(parent.getFont());
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		mainComp.setLayout(layout);

		createIgnoreOptionalProblemsLink(mainComp);

		Composite spacer= new Composite(mainComp, SWT.NONE);
		spacer.setLayoutData(new GridData(0, 0));

		Composite commonComposite= createStyleTabContent(mainComp);
		GridData gridData= new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint= fPixelConverter.convertHeightInCharsToPixels(30);
		commonComposite.setLayoutData(gridData);

		validateSettings(null, null, null);

		return mainComp;
	}

	private Composite createStyleTabContent(Composite folder) {
		String[] errorWarningInfoIgnore= new String[] { ERROR, WARNING, INFO, IGNORE };
		String[] errorWarningInfoIgnoreLabels= new String[] {
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_error,
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_warning,
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_info,
			PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore
		};

		String[] errorWarning= new String[] { ERROR, WARNING };
		String[] errorWarningLabels= new String[] {
				PreferencesMessages.ProblemSeveritiesConfigurationBlock_error,
				PreferencesMessages.ProblemSeveritiesConfigurationBlock_warning
		};

		String[] enabledDisabled= new String[] { ENABLED, DISABLED };
		String[] disabledEnabled= new String[] { DISABLED, ENABLED };

		fFilteredPrefTree= new PreferenceTree(this, folder, PreferencesMessages.ProblemSeveritiesConfigurationBlock_common_description);
		final ScrolledPageContent sc1= fFilteredPrefTree.getScrolledPageContent();

		int nColumns= 3;

		Composite composite= sc1.getBody();
		GridLayout layout= new GridLayout(nColumns, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		final int defaultIndent= 0;
		final int extraIndent= LayoutUtil.getIndent();

		String label;
		ExpandableComposite excomposite;
		Composite inner;
		PreferenceTreeNode<?> section;
		PreferenceTreeNode<?> node;
		Key twistieKey;

		// --- style

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_code_style;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_code_style"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		// - expression level
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_static_access_receiver_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_STATIC_ACCESS_RECEIVER, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_indirect_access_to_static_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_INDIRECT_STATIC_ACCESS, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unqualified_field_access_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNQUALIFIED_FIELD_ACCESS, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_synth_access_emul_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_SYNTHETIC_ACCESS_EMULATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_parameter_assignment;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_PARAMETER_ASSIGNMENT, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// - statements level
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_non_externalized_strings_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NON_EXTERNALIZED_STRINGS, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_undocumented_empty_block_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNDOCUMENTED_EMPTY_BLOCK, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_resource_not_managed_via_twr_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_EXPLICITLY_CLOSED_AUTOCLOSEABLE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// - member level
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_method_naming_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_METHOD_WITH_CONSTRUCTOR_NAME, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_static_on_method_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_STATIC_ON_METHOD, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_potentially_missing_static_on_method_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POTENTIALLY_MISSING_STATIC_ON_METHOD, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// --- potential_programming_problems

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_potential_programming_problems;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_potential_programming_problems"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		// - affecting expressions
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_comparing_identical;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_COMPARING_IDENTICAL, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_no_effect_assignment_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NO_EFFECT_ASSIGNMENT, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_accidential_assignement_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POSSIBLE_ACCIDENTAL_BOOLEAN_ASSIGNMENT, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_autoboxing_problem_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_AUTOBOXING_PROBLEM, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_char_array_in_concat_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_CHAR_ARRAY_IN_CONCAT, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// - affecting method invocations
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_inexact_vararg_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_VARARGS_ARGUMENT_NEED_CAST, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unlikely_collection_method_argument_type_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unlikely_collection_method_argument_type_strict_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE_STRICT, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unlikely_equals_argument_type_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_UNLIKELY_EQUALS_ARGUMENT_TYPE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// - affecting single statements
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_empty_statement_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_EMPTY_STATEMENT, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_object_allocation_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_OBJECT_ALLOCATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_incomplete_enum_switch_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_INCOMPLETE_ENUM_SWITCH, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_enum_case_despite_default;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_15_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_switch_missing_default_case_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_SWITCH_MISSING_DEFAULT_CASE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_fall_through_case;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FALLTHROUGH_CASE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_hidden_catchblock_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_HIDDEN_CATCH_BLOCK, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_finally_block_not_completing_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FINALLY_BLOCK_NOT_COMPLETING, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// - affecting code blocks
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_dead_code;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_DEAD_CODE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_resource_leak_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNCLOSED_CLOSEABLE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_potential_resource_leak_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POTENTIALLY_UNCLOSED_CLOSEABLE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// - affecting members
		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_serial_version_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_SERIAL_VERSION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_synchronized_on_inherited_method;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_SYNCHRONIZED_ON_INHERITED_METHOD, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_hashcode_method;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_HASHCODE_METHOD, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);


		// --- name_shadowing

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_name_shadowing;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_name_shadowing"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_field_hiding_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FIELD_HIDING, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_local_variable_hiding_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_LOCAL_VARIABLE_HIDING, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_special_param_hiding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_type_parameter_hiding_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_TYPE_PARAMETER_HIDING, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_overriding_pkg_dflt_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_OVERRIDING_PACKAGE_DEFAULT_METHOD, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_incompatible_interface_method_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_INCOMPATIBLE_INTERFACE_METHOD, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// --- API access rules

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_deprecations;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_deprecations"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_DEPRECATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_in_deprecation_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_DEPRECATION_IN_DEPRECATED_CODE, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_deprecation_when_overriding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_DEPRECATION_WHEN_OVERRIDING, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_terminal_deprecation_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_TERMINAL_DEPRECATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_forbidden_reference_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_FORBIDDEN_REFERENCE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_discourraged_reference_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_DISCOURRAGED_REFERENCE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// --- module

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_module;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_module"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_api_leak_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_API_LEAKS, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unstable_auto_module_name_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNSTABLE_AUTO_MODULE_NAME, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// --- unnecessary_code

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_unnecessary_code;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_unnecessary_code"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_local_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_LOCAL, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_parameter_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_PARAMETER, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_signal_param_in_overriding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING, disabledEnabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_type_parameter;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_TYPE_PARAMETER, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore_documented_unused_parameters;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_PARAMETER_INCLUDE_DOC_COMMENT_REFERENCE, enabledDisabled, defaultIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_exception_parameter_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_EXCEPTION_PARAMETER, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_imports_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_IMPORT, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_private_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_PRIVATE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unnecessary_else_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNNECESSARY_ELSE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unnecessary_type_check_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNNECESSARY_TYPE_CHECK, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_when_overriding_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING, disabledEnabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_ignore_documented_unused_exceptions;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_throwing_exception_ignore_unchecked_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unused_label_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_LABEL, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_super_interface_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_REDUNDANT_SUPERINTERFACE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		// --- generics

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_generics;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_generics"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unsafe_type_op_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_UNCHECKED_TYPE_OPERATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_raw_type_reference;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_RAW_TYPE_REFERENCE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_final_param_bound_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_FINAL_PARAM_BOUND, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_type_arguments_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_17_PB_REDUNDANT_TYPE_ARGUMENTS, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unavoidable_generic_type_problems;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_15_PB_UNAVOIDABLE_GENERIC_TYPE_PROBLEMS, disabledEnabled, defaultIndent, section);

		// --- annotations

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_annotations;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_annotations"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_override_annotation_label;
		node= fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_MISSING_OVERRIDE_ANNOTATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_override_annotation_for_interface_method_implementations_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_16_PB_MISSING_OVERRIDE_ANNOTATION_FOR_INTERFACE_METHOD_IMPLEMENTATION, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_missing_deprecated_annotation_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_MISSING_DEPRECATED_ANNOTATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_annotation_super_interface_label;
		fFilteredPrefTree.addComboBox(inner, label, PREF_15_PB_ANNOTATION_SUPER_INTERFACE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_unhandled_surpresswarning_tokens;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNHANDLED_WARNING_TOKEN, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_enable_surpresswarning_annotation;
		node= fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SUPPRESS_WARNINGS, enabledDisabled, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_unused_suppresswarnings_token;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_UNUSED_WARNING_TOKEN, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_suppresswarnings_not_fully_analysed;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_SUPPRESS_WARNINGS_NOT_FULLY_ANALYSED, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_suppress_optional_errors_label;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SUPPRESS_OPTIONAL_ERRORS, enabledDisabled, extraIndent, node);

		// --- null analysis

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_section_null_analysis;
		twistieKey= OptionsConfigurationBlock.getLocalKey("ProblemSeveritiesConfigurationBlock_section_null_analysis"); //$NON-NLS-1$
		section= fFilteredPrefTree.addExpandableComposite(composite, label, nColumns, twistieKey, null, false);
		excomposite= getExpandableComposite(twistieKey);

		inner= createInnerComposite(excomposite, nColumns, composite.getFont());

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_null_reference;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NULL_REFERENCE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_potential_null_reference;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POTENTIAL_NULL_REFERENCE, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_null_check;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_REDUNDANT_NULL_CHECK, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_include_assert_in_null_analysis;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_INCLUDE_ASSERTS_IN_NULL_ANALYSIS, enabledDisabled, defaultIndent, section);

		// --- annotation-based nulll analysis

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_enable_annotation_null_analysis;
		node= fFilteredPrefTree.addCheckBox(inner, label, PREF_ANNOTATION_NULL_ANALYSIS, enabledDisabled, defaultIndent, section);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_null_spec_violation;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NULL_SPECIFICATION_VIOLATION, errorWarning, errorWarningLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_null_annotation_inference_conflict;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_null_unchecked_conversion;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NULL_UNCHECKED_CONVERSION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_null_annotated_type_argument_to_unannotated;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_ANNOTATED_TYPE_ARGUMENT_TO_UNANNOTATED, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_pessimistic_analysis_for_free_type_variables;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_PESSIMISTIC_NULL_ANALYSIS_FOR_FREE_TYPE_VARIABLES, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_nonnull_typevar_maybe_legacy;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NONNULL_TYPEVAR_FROM_LEGACY_INVOCATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_redundant_null_annotation;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_REDUNDANT_NULL_ANNOTATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_pb_nonnull_parameter_annotation_dropped;
		fFilteredPrefTree.addComboBox(inner, label, PREF_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_missing_nonnull_by_default_annotation;
		fFilteredPrefTree.addComboBox(inner, label, PREF_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, errorWarningInfoIgnore, errorWarningInfoIgnoreLabels, extraIndent, node);

		label= PreferencesMessages.NullAnnotationsConfigurationDialog_use_default_annotations_for_null;
		fFilteredPrefTree.addCheckBoxWithLink(inner, label, INTR_DEFAULT_NULL_ANNOTATIONS, enabledDisabled, extraIndent, node, SWT.DEFAULT,
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						openNullAnnotationsConfigurationDialog();
					}
				});

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_inherit_null_annotations;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_INHERIT_NULL_ANNOTATIONS, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_enable_syntactic_null_analysis_for_fields;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_PB_SYNTACTIC_NULL_ANLYSIS_FOR_FIELDS, enabledDisabled, extraIndent, node);

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_external_annotations_from_all_locations;
		fFilteredPrefTree.addCheckBox(inner, label, PREF_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS, enabledDisabled, extraIndent, node);

		// --- global

		// add some vertical space before:
		GridData gd= new GridData();
		gd.verticalIndent= fPixelConverter.convertHeightInCharsToPixels(1);
		gd.horizontalSpan= 2;

		label= PreferencesMessages.ProblemSeveritiesConfigurationBlock_treat_optional_as_fatal;
		addCheckBox(composite, label, PREF_PB_FATAL_OPTIONAL_ERROR, enabledDisabled, defaultIndent).setLayoutData(gd);

		IDialogSettings settingsSection= JavaPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION_NAME);
		restoreSectionExpansionStates(settingsSection);

		return sc1;
	}

	private void openNullAnnotationsConfigurationDialog() {
		NullAnnotationsConfigurationDialog dialog= new NullAnnotationsConfigurationDialog();
		int result= dialog.open();
		if (result == Window.OK) {
			String[] annotationNames= dialog.getResult();
			setValue(PREF_NULLABLE_ANNOTATION_NAME, annotationNames[0]);
			setValue(PREF_NONNULL_ANNOTATION_NAME, annotationNames[1]);
			setValue(PREF_NONNULL_BY_DEFAULT_ANNOTATION_NAME, annotationNames[2]);
			setValue(PREF_NULLABLE_ANNOTATION_SECONDARY_NAMES, annotationNames[3]);
			setValue(PREF_NONNULL_ANNOTATION_SECONDARY_NAMES, annotationNames[4]);
			setValue(PREF_NONNULL_BY_DEFAULT_ANNOTATION_SECONDARY_NAMES, annotationNames[5]);
		}
		updateNullAnnotationsSetting();
	}

	private Composite createInnerComposite(ExpandableComposite excomposite, int nColumns, Font font) {
		Composite inner= new Composite(excomposite, SWT.NONE);
		inner.setFont(font);
		inner.setLayout(new GridLayout(nColumns, false));
		excomposite.setClient(inner);
		return inner;
	}

	/* (non-javadoc)
	 * Update fields and validate.
	 * @param changedKey Key that changed, or null, if all changed.
	 */
	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		if (!areSettingsEnabled()) {
			return;
		}

		if (changedKey != null) {
			if (PREF_PB_UNUSED_PARAMETER.equals(changedKey) ||
					PREF_PB_DEPRECATION.equals(changedKey) ||
					PREF_PB_LOCAL_VARIABLE_HIDING.equals(changedKey) ||
					PREF_15_PB_INCOMPLETE_ENUM_SWITCH.equals(changedKey) ||
					PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE.equals(changedKey) ||
					PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION.equals(changedKey) ||
					PREF_PB_SUPPRESS_WARNINGS.equals(changedKey) ||
					PREF_ANNOTATION_NULL_ANALYSIS.equals(changedKey)) {
				updateEnableStates();
			}

			if (checkValue(PREF_ANNOTATION_NULL_ANALYSIS, ENABLED)
					&& (PREF_ANNOTATION_NULL_ANALYSIS.equals(changedKey)
							|| PREF_PB_NULL_REFERENCE.equals(changedKey)
							|| PREF_PB_POTENTIAL_NULL_REFERENCE.equals(changedKey)
							|| PREF_PB_NULL_SPECIFICATION_VIOLATION.equals(changedKey)
							|| PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT.equals(changedKey))) {
				boolean badNullRef= lessSevere(getValue(PREF_PB_NULL_REFERENCE), getValue(PREF_PB_NULL_SPECIFICATION_VIOLATION));
				boolean badPotNullRef= lessSevere(getValue(PREF_PB_POTENTIAL_NULL_REFERENCE), getValue(PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT));
				boolean ask= false;
				ask |= badNullRef && (PREF_PB_NULL_REFERENCE.equals(changedKey) || PREF_PB_NULL_SPECIFICATION_VIOLATION.equals(changedKey));
				ask |= badPotNullRef && (PREF_PB_POTENTIAL_NULL_REFERENCE.equals(changedKey) || PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT.equals(changedKey));
				ask |= (badNullRef || badPotNullRef) && PREF_ANNOTATION_NULL_ANALYSIS.equals(changedKey);
				if (ask) {
					final Combo comboBoxNullRef= getComboBox(PREF_PB_NULL_REFERENCE);
					final PreferenceHighlight highlightNullRef= (PreferenceHighlight) fLabels.get(comboBoxNullRef).getData(DATA_PREF_HIGHLIGHT);
					final Combo comboBoxPotNullRef= getComboBox(PREF_PB_POTENTIAL_NULL_REFERENCE);
					final PreferenceHighlight highlightPotNullRef= (PreferenceHighlight) fLabels.get(comboBoxPotNullRef).getData(DATA_PREF_HIGHLIGHT);

					getShell().getDisplay().asyncExec(() -> {
						highlightNullRef.setFocus(true);
						highlightPotNullRef.setFocus(true);
					});

					MessageDialog messageDialog= new MessageDialog(
							getShell(),
							PreferencesMessages.ProblemSeveritiesConfigurationBlock_adapt_null_pointer_access_settings_dialog_title,
							null,
							PreferencesMessages.ProblemSeveritiesConfigurationBlock_adapt_null_pointer_access_settings_dialog_message,
							MessageDialog.QUESTION,
							new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
							0);
					messageDialog.create();
					Shell messageShell= messageDialog.getShell();
					messageShell.setLocation(messageShell.getLocation().x, getShell().getLocation().y + 40);
					if (messageDialog.open() == 0) {
						if (badNullRef) {
							setValue(PREF_PB_NULL_REFERENCE, getValue(PREF_PB_NULL_SPECIFICATION_VIOLATION));
							updateCombo(getComboBox(PREF_PB_NULL_REFERENCE));
						}
						if (badPotNullRef) {
							setValue(PREF_PB_POTENTIAL_NULL_REFERENCE, getValue(PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT));
							updateCombo(getComboBox(PREF_PB_POTENTIAL_NULL_REFERENCE));
						}
					}

					highlightNullRef.setFocus(false);
					highlightPotNullRef.setFocus(false);
				}

			} else if (PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING.equals(changedKey)) {
				// merging the two options
				setValue(PREF_PB_SIGNAL_PARAMETER_IN_ABSTRACT, newValue);

			} else if (INTR_DEFAULT_NULL_ANNOTATIONS.equals(changedKey)) {
				if (ENABLED.equals(newValue)) {
					setValue(PREF_NULLABLE_ANNOTATION_NAME, NULL_ANNOTATIONS_DEFAULTS[0]);
					setValue(PREF_NONNULL_ANNOTATION_NAME, NULL_ANNOTATIONS_DEFAULTS[1]);
					setValue(PREF_NONNULL_BY_DEFAULT_ANNOTATION_NAME, NULL_ANNOTATIONS_DEFAULTS[2]);
				} else {
					openNullAnnotationsConfigurationDialog();
				}

			} else {
				return;
			}
		} else {
			updateEnableStates();
			updateNullAnnotationsSetting();
		}
	}

	private static boolean lessSevere(String errorWarningInfoIgnore, String errorWarningInfoIgnore2) {
		switch (errorWarningInfoIgnore) {
			case IGNORE:
				return !IGNORE.equals(errorWarningInfoIgnore2);
			case INFO:
				return !IGNORE.equals(errorWarningInfoIgnore2) && !INFO.equals(errorWarningInfoIgnore2);
			case WARNING:
				return ERROR.equals(errorWarningInfoIgnore2);
			default:
				return false;
		}
	}

	private void updateNullAnnotationsSetting() {
		String[] annotationNames= {
				getValue(PREF_NULLABLE_ANNOTATION_NAME),
				getValue(PREF_NONNULL_ANNOTATION_NAME),
				getValue(PREF_NONNULL_BY_DEFAULT_ANNOTATION_NAME)
		};
		String defaultNullAnnotationsValue= Arrays.equals(annotationNames, NULL_ANNOTATIONS_DEFAULTS) ? ENABLED : DISABLED;
		setValue(INTR_DEFAULT_NULL_ANNOTATIONS, defaultNullAnnotationsValue);
		updateCheckBox(getCheckBox(INTR_DEFAULT_NULL_ANNOTATIONS));
	}

	private void updateEnableStates() {
		boolean enableUnusedParams= !checkValue(PREF_PB_UNUSED_PARAMETER, IGNORE);
		getCheckBox(PREF_PB_SIGNAL_PARAMETER_IN_OVERRIDING).setEnabled(enableUnusedParams);

		boolean enableDeprecation= !checkValue(PREF_PB_DEPRECATION, IGNORE);
		getCheckBox(PREF_PB_DEPRECATION_IN_DEPRECATED_CODE).setEnabled(enableDeprecation);
		getCheckBox(PREF_PB_DEPRECATION_WHEN_OVERRIDING).setEnabled(enableDeprecation);

		boolean enableThrownExceptions= !checkValue(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION, IGNORE);
		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_WHEN_OVERRIDING).setEnabled(enableThrownExceptions);
		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_INCLUDE_DOC_COMMENT_REFERENCE).setEnabled(enableThrownExceptions);
		getCheckBox(PREF_PB_UNUSED_DECLARED_THROWN_EXCEPTION_EXEMPT_EXCEPTION_AND_THROWABLE).setEnabled(enableThrownExceptions);

		boolean enableHiding= !checkValue(PREF_PB_LOCAL_VARIABLE_HIDING, IGNORE);
		getCheckBox(PREF_PB_SPECIAL_PARAMETER_HIDING_FIELD).setEnabled(enableHiding);

		boolean enableMissingEnumDespiteDefault= !checkValue(PREF_15_PB_INCOMPLETE_ENUM_SWITCH, IGNORE);
		getCheckBox(PREF_15_PB_MISSING_ENUM_CASE_DESPITE_DEFAULT).setEnabled(enableMissingEnumDespiteDefault);

		boolean enableUnlikelyCollectionMethodArgumentTypeStrict= !checkValue(PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE, IGNORE);
		getCheckBox(PREF_15_PB_UNLIKELY_COLLECTION_METHOD_ARGUMENT_TYPE_STRICT).setEnabled(enableUnlikelyCollectionMethodArgumentTypeStrict);

		boolean enableSuppressWarnings= checkValue(PREF_PB_SUPPRESS_WARNINGS, ENABLED);
		getCheckBox(PREF_PB_SUPPRESS_OPTIONAL_ERRORS).setEnabled(enableSuppressWarnings);
		setComboEnabled(PREF_PB_UNUSED_WARNING_TOKEN, enableSuppressWarnings);

		boolean enableAnnotationNullAnalysis= checkValue(PREF_ANNOTATION_NULL_ANALYSIS, ENABLED);
		getCheckBox(INTR_DEFAULT_NULL_ANNOTATIONS).setEnabled(enableAnnotationNullAnalysis);
		getCheckBoxLink(INTR_DEFAULT_NULL_ANNOTATIONS).setEnabled(enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_NULL_SPECIFICATION_VIOLATION, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_POTENTIAL_NULL_ANNOTATION_INFERENCE_CONFLICT, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_NULL_UNCHECKED_CONVERSION, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_ANNOTATED_TYPE_ARGUMENT_TO_UNANNOTATED, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_PESSIMISTIC_NULL_ANALYSIS_FOR_FREE_TYPE_VARIABLES, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_NONNULL_TYPEVAR_FROM_LEGACY_INVOCATION, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_REDUNDANT_NULL_ANNOTATION, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_PB_NONNULL_PARAMETER_ANNOTATION_DROPPED, enableAnnotationNullAnalysis);
		setComboEnabled(PREF_MISSING_NONNULL_BY_DEFAULT_ANNOTATION, enableAnnotationNullAnalysis);
		getCheckBox(PREF_INHERIT_NULL_ANNOTATIONS).setEnabled(enableAnnotationNullAnalysis);
		getCheckBox(PREF_PB_SYNTACTIC_NULL_ANLYSIS_FOR_FIELDS).setEnabled(enableAnnotationNullAnalysis);
		getCheckBox(PREF_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS).setEnabled(enableAnnotationNullAnalysis);
	}

	private IStatus validateNullnessAnnotation(String value, String errorMessage, boolean isTypeMandatory) {
		StatusInfo status= new StatusInfo();
		if (value.isEmpty() && !isTypeMandatory)
			return status;
		if (JavaConventions.validateJavaTypeName(value, JavaCore.VERSION_1_5, JavaCore.VERSION_1_5, null).matches(IStatus.ERROR)
				|| value.indexOf('.') == -1) {
			status.setError(errorMessage);
		} else if (fProject != null) {
			try {
				if (JavaCore.create(fProject).findType(value) == null) {
					String notFoundMessage= NLS.bind(PreferencesMessages.NullAnnotationsConfigurationDialog_notFound_info, value);
					if (isTypeMandatory)
						status.setError(notFoundMessage);
					else
						status.setInfo(notFoundMessage);
				}
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, getShell(), PreferencesMessages.NullAnnotationsConfigurationDialog_error_title, PreferencesMessages.NullAnnotationsConfigurationDialog_error_message);
			}
		}
		return status;
	}

	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		String title= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsbuild_title;
		String message;
		if (workspaceSettings) {
			message= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsfullbuild_message;
		} else {
			message= PreferencesMessages.ProblemSeveritiesConfigurationBlock_needsprojectbuild_message;
		}
		return new String[] { title, message };
	}

	@Override
	public void dispose() {
		IDialogSettings section= JavaPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION_NAME);
		storeSectionExpansionStates(section);
		super.dispose();
	}

}
