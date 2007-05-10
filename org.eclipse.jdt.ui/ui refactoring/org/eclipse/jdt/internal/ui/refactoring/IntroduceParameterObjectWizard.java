/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.IntroduceParameterObjectRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;

public class IntroduceParameterObjectWizard extends RefactoringWizard {

	public IntroduceParameterObjectWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		setDefaultPageTitle(RefactoringMessages.IntroduceParameterObjectWizard_wizardpage_title);
	}

	protected void addUserInputPages() {
		addPage(new IntroduceParameterObjectInputPage());
	}

	private static class IntroduceParameterObjectInputPage extends UserInputWizardPage {

		private final class ParameterObjectCreatorContentProvider implements IStructuredContentProvider {
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof IntroduceParameterObjectRefactoring) {
					IntroduceParameterObjectRefactoring refactoring= (IntroduceParameterObjectRefactoring) inputElement;
					List parameterInfos= refactoring.getParameterInfos();
					List result= new ArrayList(parameterInfos.size());
					for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
						ParameterInfo pi= (ParameterInfo) iter.next();
						if (!pi.isAdded())
							result.add(pi);
					}
					return result.toArray();
				}
				return null;
			}
		}

		private abstract class ParameterInfoLabelProvider extends CellLabelProvider {
			public void update(ViewerCell cell) {
				ParameterInfo pi= (ParameterInfo) cell.getElement();
				cell.setText(doGetValue(pi));
			}

			protected abstract String doGetValue(ParameterInfo pi);
		}

		private abstract class ParameterInfoEditingSupport extends EditingSupport {
			private CellEditor fTextEditor;

			private ParameterInfoEditingSupport(CellEditor textEditor, ColumnViewer tv) {
				super(tv);
				fTextEditor= textEditor;
			}

			protected void setValue(Object element, Object value) {
				if (element instanceof ParameterInfo) {
					ParameterInfo pi= (ParameterInfo) element;
					doSetValue(pi, value.toString());
					getViewer().update(element, null);
				}
				validateRefactoring();
				updateSignaturePreview();
			}

			public abstract void doSetValue(ParameterInfo pi, String string);

			protected Object getValue(Object element) {
				if (element instanceof ParameterInfo) {
					ParameterInfo pi= (ParameterInfo) element;
					return doGetValue(pi);
				}
				return null;
			}

			public abstract String doGetValue(ParameterInfo pi);

			protected CellEditor getCellEditor(Object element) {
				return fTextEditor;
			}

			protected boolean canEdit(Object element) {
				if (element instanceof ParameterInfo) {
					ParameterInfo pi= (ParameterInfo) element;
					return fTextEditor!=null && pi.isCreateField();
				}
				return false;
			}
		}

		private IntroduceParameterObjectRefactoring fRefactoring;
		private JavaSourceViewer fSignaturePreview;
		private IDocument fSignaturePreviewDocument= new Document();

		public IntroduceParameterObjectInputPage() {
			super(RefactoringMessages.IntroduceParameterObjectWizard_wizardpage_name);
			setTitle(RefactoringMessages.IntroduceParameterObjectWizard_wizardpage_title);
			setDescription(RefactoringMessages.IntroduceParameterObjectWizard_wizardpage_description);
		}

		public void createControl(Composite parent) {
			initializeDialogUnits(parent);
			fRefactoring= (IntroduceParameterObjectRefactoring) getRefactoring();
			Composite result= new Composite(parent, SWT.NONE);
			result.setLayout(new GridLayout(2, false));
			Group group= createGroup(result, RefactoringMessages.IntroduceParameterObjectWizard_type_group);

			createClassNameInput(group);
			createLocationInput(group);

			createTable(group);
			createGetterInput(group);
			createSetterInput(group);

			group= createGroup(result, RefactoringMessages.IntroduceParameterObjectWizard_method_group);
			createParameterNameInput(group);
			createDelegateInput(group);
			createSignaturePreview(group);

			setControl(result);
		}

		private void createParameterNameInput(Group group) {
			Label l= new Label(group, SWT.NONE);
			l.setText(RefactoringMessages.IntroduceParameterObjectWizard_parameterfield_label);
			final Text text= new Text(group, SWT.BORDER);
			text.setText(fRefactoring.getParameterName());
			text.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					fRefactoring.setParameterName(text.getText());
					updateSignaturePreview();
					validateRefactoring();
				}

			});
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		protected void validateRefactoring() {
			List names= new ArrayList();
			boolean oneChecked= false;
			setMessage(null);
			setErrorMessage(null);
			setPageComplete(true);
			IJavaProject project= fRefactoring.getMethod().getJavaProject();
			String sourceLevel= project.getOption(JavaCore.COMPILER_SOURCE, true);
			String compliance= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
			List parameterInfos= fRefactoring.getParameterInfos();
			for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
				ParameterInfo pi= (ParameterInfo) iter.next();
				if (names.contains(pi.getNewName())) {
					setErrorMessage(Messages.format(RefactoringMessages.IntroduceParameterObjectWizard_parametername_check_notunique, pi.getNewName()));
					setPageComplete(false);
					return;
				}
				names.add(pi.getNewName());
				IStatus validateIdentifier= JavaConventions.validateIdentifier(pi.getNewName(), sourceLevel, compliance);
				if (isErrorMessage(validateIdentifier))
					return;
				if (pi.isCreateField())
					oneChecked= true;
			}
			if (!oneChecked) {
				setErrorMessage(RefactoringMessages.IntroduceParameterObjectWizard_parametername_check_atleastoneparameter);
				setPageComplete(false);
				return;
			}
			IStatus validateJavaTypeName= JavaConventions.validateJavaTypeName(fRefactoring.getClassName(), sourceLevel, compliance);
			if (isErrorMessage(validateJavaTypeName))
				return;
			if (!"".equals(fRefactoring.getPackage())) { //$NON-NLS-1$
				IStatus validatePackageName= JavaConventions.validatePackageName(fRefactoring.getPackage(), sourceLevel, compliance);
				if (isErrorMessage(validatePackageName))
					return;
			}
			try {
				IPackageFragment fragment= getPackageFragmentRoot().getPackageFragment(fRefactoring.getPackage());
				if (fragment.exists()) {
					ICompilationUnit[] cus= fragment.getCompilationUnits();
					for (int i= 0; i < cus.length; i++) {
						ICompilationUnit cu= cus[i];
						IType type= cu.getType(fRefactoring.getClassName());
						if (type.exists()) {
							setErrorMessage(Messages.format(RefactoringMessages.IntroduceParameterObjectWizard_parametername_check_alreadyexists, new Object[] { fRefactoring.getClassName(), cu.getElementName() }));
							setPageComplete(false);
							return;
						}
					}
				}
			} catch (JavaModelException e) {
				// Don't care. The error will popup later anyway..
			}
		}

		private boolean isErrorMessage(IStatus validationStatus) {
			if (!validationStatus.isOK()) {
				if (validationStatus.getSeverity() == IStatus.ERROR) {
					setErrorMessage(validationStatus.getMessage());
					setPageComplete(false);
					return true;
				} else {
					if (validationStatus.getSeverity() == IStatus.INFO)
						setMessage(validationStatus.getMessage(), IMessageProvider.INFORMATION);
					else
						setMessage(validationStatus.getMessage(), IMessageProvider.WARNING);
				}
			}
			return false;
		}

		private IPackageFragmentRoot getPackageFragmentRoot() throws JavaModelException {
			return fRefactoring.getPackageFragmentRoot();
		}

		private void createSignaturePreview(Composite composite) {
			Label previewLabel= new Label(composite, SWT.NONE);
			previewLabel.setText(RefactoringMessages.IntroduceParameterObjectWizard_signaturepreview_label);
			GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan= 2;
			previewLabel.setLayoutData(gridData);

			// //XXX: use ViewForm to draw a flat border. Beware of common
			// problems
			// with wrapping layouts
			// //inside GridLayout. GridData must be constrained to force
			// wrapping.
			// See bug 9866 et al.
			// ViewForm border= new ViewForm(composite, SWT.BORDER | SWT.FLAT);

			IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
			fSignaturePreview= new JavaSourceViewer(composite, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP, store);
			fSignaturePreview.configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
			fSignaturePreview.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
			fSignaturePreview.getTextWidget().setBackground(composite.getBackground());
			fSignaturePreview.setDocument(fSignaturePreviewDocument);
			fSignaturePreview.setEditable(false);

			// Layouting problems with wrapped text: see
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=9866
			Control signaturePreviewControl= fSignaturePreview.getControl();
			PixelConverter pixelConverter= new PixelConverter(signaturePreviewControl);
			GridData gdata= new GridData(GridData.FILL_BOTH);
			gdata.widthHint= pixelConverter.convertWidthInCharsToPixels(50);
			gdata.heightHint= pixelConverter.convertHeightInCharsToPixels(2);
			gdata.horizontalSpan= 2;
			signaturePreviewControl.setLayoutData(gdata);

			updateSignaturePreview();
			// //XXX must force JavaSourceViewer text widget to wrap:
			// border.setContent(signaturePreviewControl);
			// GridData borderData= new GridData(GridData.FILL_BOTH);
			// borderData.widthHint= gdata.widthHint;
			// borderData.heightHint= gdata.heightHint;
			// border.setLayoutData(borderData);
		}

		private void createDelegateInput(Group group) {
			final Button fLeaveDelegateCheckBox= DelegateUIHelper.generateLeaveDelegateCheckbox(group, getRefactoring(), false);
			GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan= 2;
			fLeaveDelegateCheckBox.setLayoutData(gridData);
			if (fLeaveDelegateCheckBox != null) {
				final Button fDeprecateDelegateCheckBox= new Button(group, SWT.CHECK);
				GridData data= new GridData();
				data.horizontalAlignment= GridData.FILL;
				GridLayout layout= (GridLayout) group.getLayout();
				data.horizontalIndent= (layout.marginWidth + fDeprecateDelegateCheckBox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
				data.horizontalSpan= 2;
				fDeprecateDelegateCheckBox.setLayoutData(data);
				fDeprecateDelegateCheckBox.setText(DelegateUIHelper.getDeprecateDelegateCheckBoxTitle());
				final ChangeSignatureRefactoring refactoring= fRefactoring;
				fDeprecateDelegateCheckBox.setSelection(DelegateUIHelper.loadDeprecateDelegateSetting(refactoring));
				refactoring.setDeprecateDelegates(fDeprecateDelegateCheckBox.getSelection());
				fDeprecateDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						refactoring.setDeprecateDelegates(fDeprecateDelegateCheckBox.getSelection());
						validateRefactoring();
					}
				});
				fDeprecateDelegateCheckBox.setEnabled(fLeaveDelegateCheckBox.getSelection());
				fLeaveDelegateCheckBox.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fDeprecateDelegateCheckBox.setEnabled(fLeaveDelegateCheckBox.getSelection());
					}
				});
			}
		}

		private Group createGroup(Composite result, String caption) {
			Group group= new Group(result, SWT.None);
			group.setLayout(new GridLayout(2, false));
			group.setText(caption);
			GridData gridData= new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan= 2;
			group.setLayoutData(gridData);
			return group;
		}

		private void createLocationInput(Composite parent) {
			Label l= new Label(parent, SWT.NONE);
			l.setText(RefactoringMessages.IntroduceParameterObjectWizard_destination_label);

			Composite composite= new Composite(parent, SWT.None);
			GridLayout gridLayout= new GridLayout(2, false);
			gridLayout.marginHeight= 0;
			gridLayout.marginWidth= 0;
			composite.setLayout(gridLayout);
			GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
			composite.setLayoutData(gridData);

			final Button topLvlRadio= new Button(composite, SWT.RADIO);
			topLvlRadio.setText(RefactoringMessages.IntroduceParameterObjectWizard_createastoplevel_radio);
			topLvlRadio.setSelection(fRefactoring.isCreateAsTopLevel());
			topLvlRadio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean fAsTopLevel= topLvlRadio.getSelection();
					fRefactoring.setCreateAsTopLevel(fAsTopLevel);
					validateRefactoring();
				}
			});

			Button nestedRadio= new Button(composite, SWT.RADIO);
			nestedRadio.setText(Messages.format(RefactoringMessages.IntroduceParameterObjectWizard_createasnestedclass_radio, fRefactoring.getContainingClass().getName()));
			nestedRadio.setSelection(!fRefactoring.isCreateAsTopLevel());

		}

		private void createTable(Composite parent) {
			Composite result= new Composite(parent, SWT.NONE);
			GridLayout layout= new GridLayout(2, false);
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			result.setLayout(layout);
			GridData gridData= new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan= 2;
			result.setLayoutData(gridData);

			Label l= new Label(result, SWT.NONE);
			l.setText(RefactoringMessages.IntroduceParameterObjectWizard_fields_selection_label);
			gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan= 2;
			gridData.verticalIndent= 5;
			l.setLayoutData(gridData);
			
			TableLayoutComposite layoutComposite= new TableLayoutComposite(result, SWT.NONE);
			layoutComposite.addColumnData(new ColumnWeightData(40, convertWidthInCharsToPixels(20), true));
			layoutComposite.addColumnData(new ColumnWeightData(60, convertWidthInCharsToPixels(20), true));
			final CheckboxTableViewer tv= CheckboxTableViewer.newCheckList(layoutComposite, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
			tv.setContentProvider(new ParameterObjectCreatorContentProvider());
			createColumns(tv);

			Table table= tv.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			gridData= new GridData(GridData.FILL_BOTH);
			table.setLayoutData(gridData);
			tv.setInput(fRefactoring);
			List parameterInfos= fRefactoring.getParameterInfos();
			for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
				ParameterInfo pi= (ParameterInfo) iter.next();
				tv.setChecked(pi, pi.isCreateField());
			}
			tv.refresh(true);
			gridData= new GridData(GridData.FILL_BOTH);
			gridData.heightHint= SWTUtil.getTableHeightHint(table, parameterInfos.size());
			layoutComposite.setLayoutData(gridData);
			Composite controls= new Composite(result, SWT.NONE);
			gridData= new GridData(GridData.FILL, GridData.FILL, false, false);
			controls.setLayoutData(gridData);
			GridLayout gridLayout= new GridLayout();
			gridLayout.marginHeight= 0;
			gridLayout.marginWidth= 0;
			controls.setLayout(gridLayout);

			final Button upButton= new Button(controls, SWT.NONE);
			upButton.setText(RefactoringMessages.IntroduceParameterObjectWizard_moveentryup_button);
			gridData= new GridData(GridData.FILL_HORIZONTAL);
			upButton.setLayoutData(gridData);
			SWTUtil.setButtonDimensionHint(upButton);
			upButton.setEnabled(false);

			final Button downButton= new Button(controls, SWT.NONE);
			downButton.setText(RefactoringMessages.IntroduceParameterObjectWizard_moventrydown_button);
			gridData= new GridData(GridData.FILL_HORIZONTAL);
			downButton.setLayoutData(gridData);
			SWTUtil.setButtonDimensionHint(downButton);
			downButton.setEnabled(false);

			addSpacer(controls);

			final Button editButton= new Button(controls, SWT.NONE);
			editButton.setText(RefactoringMessages.IntroduceParameterObjectWizard_edit_button);
			editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			editButton.setEnabled(!tv.getSelection().isEmpty());
			SWTUtil.setButtonDimensionHint(editButton);
			editButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					ISelection selection= tv.getSelection();
					if (selection instanceof IStructuredSelection) {
						IStructuredSelection ss= (IStructuredSelection) selection;
						ParameterInfo selected= (ParameterInfo) ss.getFirstElement();
						String message= RefactoringMessages.IntroduceParameterObjectWizard_fieldname_message;
						String title= RefactoringMessages.IntroduceParameterObjectWizard_fieldname_title;
						InputDialog inputDialog= new InputDialog(getShell(), title, message, selected.getNewName(), new IInputValidator() {

							public String isValid(String newText) {
								IJavaProject project= fRefactoring.getCompilationUnit().getJavaProject();
								String sourceLevel= project.getOption(JavaCore.COMPILER_SOURCE, true);
								String compliance= project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
								IStatus status= JavaConventions.validateIdentifier(newText, sourceLevel, compliance);
								if (!status.isOK())
									return status.getMessage();
								return null;
							}

						});
						if (inputDialog.open() == Window.OK) {
							selected.setNewName(inputDialog.getValue());
							tv.refresh(selected);
							updateSignaturePreview();
						}
							
					}
				}
			});

			downButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					ISelection selection= tv.getSelection();
					if (selection instanceof IStructuredSelection) {
						IStructuredSelection ss= (IStructuredSelection) selection;
						ParameterInfo selected= (ParameterInfo) ss.getFirstElement();
						fRefactoring.moveFieldDown(selected);
						tv.refresh();
						updateButtons(tv, upButton, downButton, editButton);
					}
				}

			});
			upButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					ISelection selection= tv.getSelection();
					if (selection instanceof IStructuredSelection) {
						IStructuredSelection ss= (IStructuredSelection) selection;
						ParameterInfo selected= (ParameterInfo) ss.getFirstElement();
						fRefactoring.moveFieldUp(selected);
						tv.refresh();
						updateButtons(tv, upButton, downButton, editButton);
					}
				}

			});
			tv.addCheckStateListener(new ICheckStateListener() {
				Map fLastNames=new HashMap();
				public void checkStateChanged(CheckStateChangedEvent event) {
					ParameterInfo element= (ParameterInfo) event.getElement();
					element.setCreateField(event.getChecked());
					if (element.isCreateField()){
						String lastName= (String) fLastNames.get(element);
						if (lastName==null){
							lastName=fRefactoring.getFieldName(element);
						}
						element.setNewName(lastName);
					} else {
						fLastNames.put(element, element.getNewName());
						element.setNewName(element.getOldName());
					}
					tv.update(element, null);
					updateButtons(tv, upButton, downButton, editButton);
					validateRefactoring();
				}

			});
			tv.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updateButtons(tv, upButton, downButton, editButton);
				}
			});
		}

		private void addSpacer(Composite parent) {
			Label label= new Label(parent, SWT.NONE);
			GridData gd= new GridData(GridData.FILL_HORIZONTAL);
			gd.heightHint= 5;
			label.setLayoutData(gd);
		}

		private void createColumns(final CheckboxTableViewer tv) {
			TextCellEditor cellEditor= new TextCellEditor(tv.getTable());

			TableViewerColumn viwerColumn= new TableViewerColumn(tv, SWT.LEAD);
			viwerColumn.setLabelProvider(new ParameterInfoLabelProvider() {
				protected String doGetValue(ParameterInfo pi) {
					return pi.getNewTypeName();
				}
			});

			TableColumn column= viwerColumn.getColumn();
			column.setText(RefactoringMessages.IntroduceParameterObjectWizard_type_column);
			viwerColumn= new TableViewerColumn(tv, SWT.LEAD);
			viwerColumn.setLabelProvider(new ParameterInfoLabelProvider() {
				protected String doGetValue(ParameterInfo pi) {
					return pi.getNewName();
				}
			});
			viwerColumn.setEditingSupport(new ParameterInfoEditingSupport(cellEditor, tv) {
				public String doGetValue(ParameterInfo pi) {
					return pi.getNewName();
				}
				public void doSetValue(ParameterInfo pi, String string) {
					pi.setNewName(string);
				}
			});
			column= viwerColumn.getColumn();
			column.setText(RefactoringMessages.IntroduceParameterObjectWizard_name_column);
		}

		private void createGetterInput(Composite result) {
			Composite buttons= new Composite(result, SWT.NONE);
			GridLayout gridLayout= new GridLayout(2, true);
			gridLayout.marginHeight= 0;
			gridLayout.marginWidth= 0;
			buttons.setLayout(gridLayout);
			GridData gridData= new GridData();
			gridData.horizontalSpan= 2;
			buttons.setLayoutData(gridData);

			final Button button= new Button(buttons, SWT.CHECK);
			button.setText(RefactoringMessages.IntroduceParameterObjectWizard_creategetter_checkbox);
			button.setSelection(fRefactoring.isCreateGetter());
			button.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setCreateGetter(button.getSelection());
					validateRefactoring();
				}

			});
			gridData= new GridData();
			button.setLayoutData(gridData);
		}

		private void createSetterInput(Composite result) {
			final Button button= new Button(result, SWT.CHECK);
			button.setText(RefactoringMessages.IntroduceParameterObjectWizard_createsetter_checkbox);
			button.setSelection(fRefactoring.isCreateSetter());
			button.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setCreateSetter(button.getSelection());
					validateRefactoring();
				}

			});
			GridData gridData= new GridData();
			button.setLayoutData(gridData);
		}

		private void createClassNameInput(Composite result) {
			Label label= new Label(result, SWT.LEAD);
			label.setText(RefactoringMessages.IntroduceParameterObjectWizard_classnamefield_label);
			final Text text= new Text(result, SWT.SINGLE | SWT.BORDER);
			text.setText(fRefactoring.getClassName());
			text.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					fRefactoring.setClassName(text.getText());
					updateSignaturePreview();
					validateRefactoring();
				}

			});
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		private void updateButtons(final TableViewer tv, Button upButton, Button downButton, Button editButton) {
			IStructuredSelection selection= (IStructuredSelection) tv.getSelection();
			ParameterInfo firstElement= (ParameterInfo) selection.getFirstElement();
			if (selection.isEmpty()) {
				upButton.setEnabled(false);
				downButton.setEnabled(false);
				editButton.setEnabled(false);
			} else {
				int selectionIndex= tv.getTable().getSelectionIndex();
				upButton.setEnabled(selectionIndex != 0);
				downButton.setEnabled(selectionIndex != tv.getTable().getItemCount() - 1);
				editButton.setEnabled(firstElement.isCreateField());
			}
			fRefactoring.updateParameterPosition();
			updateSignaturePreview();
		}

		private void updateSignaturePreview() {
			try {
				int top= fSignaturePreview.getTextWidget().getTopPixel();
				fSignaturePreviewDocument.set(fRefactoring.getNewMethodSignature());
				fSignaturePreview.getTextWidget().setTopPixel(top);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.IntroduceParameterObjectWizard_error_title, RefactoringMessages.IntroduceParameterObjectWizard_error_description);
			}
		}
	}

}
