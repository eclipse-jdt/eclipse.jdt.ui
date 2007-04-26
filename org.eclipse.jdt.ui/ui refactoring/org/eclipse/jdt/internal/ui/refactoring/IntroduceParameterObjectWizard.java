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
import java.util.Iterator;
import java.util.List;

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

import org.eclipse.jface.dialogs.IMessageProvider;
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

import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.IntroduceParameterObjectRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
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
				return fTextEditor != null;
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

			Text packageInput= createPackageInput(group);
			createClassNameInput(group);
			createLocationInput(group, packageInput);

			//group= createGroup(result, RefactoringMessages.IntroduceParameterObjectWizard_fieldgroup_text);
			createTable(group);
			createGetterInput(group);
			createSetterInput(group);
			createCommentsInput(group);

			group= createGroup(result, RefactoringMessages.IntroduceParameterObjectWizard_method_group);
			createParameterNameInput(group);
			createSignaturePreview(group);
			createDelegateInput(group);

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
				if (isErrorMessage(validatePackageName)) return;
			}
			IPackageFragment fragment= getPackageFragmentRoot().getPackageFragment(fRefactoring.getPackage());
			if (fragment.exists()) {
				try {
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
				} catch (JavaModelException e) {
					// Don't care. The error will popup later anyway..
				}
			}
		}

		private boolean isErrorMessage(IStatus validationStatus) {
			if (!validationStatus.isOK()) {
				if (validationStatus.getSeverity() == IStatus.ERROR) {
					setErrorMessage(validationStatus.getMessage());
					setPageComplete(false);
					return true;
				} else {
					if (validationStatus.getSeverity()==IStatus.INFO)
						setMessage(validationStatus.getMessage(), IMessageProvider.INFORMATION);
					else
						setMessage(validationStatus.getMessage(), IMessageProvider.WARNING);
				}
			}
			return false;
		}

		/**
		 * Opens a selection dialog that allows to select a package.
		 * 
		 * @return returns the selected package or <code>null</code> if the dialog
		 *         has been canceled. The caller typically sets the result to the
		 *         package input field.
		 *         <p>
		 *         Clients can override this method if they want to offer a
		 *         different dialog.
		 *         </p>
		 * 
		 * @since 3.2
		 */
		private IPackageFragment choosePackage() {
			IPackageFragmentRoot froot= getPackageFragmentRoot();
			IJavaElement[] packages= null;
			try {
				if (froot != null && froot.exists()) {
					packages= froot.getChildren();
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			if (packages == null) {
				packages= new IJavaElement[0];
			}

			ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
			dialog.setIgnoreCase(false);
			dialog.setTitle(RefactoringMessages.IntroduceParameterObjectWizard_ChoosePackageDialog_title);
			dialog.setMessage(RefactoringMessages.IntroduceParameterObjectWizard_ChoosePackageDialog_description);
			dialog.setEmptyListMessage(RefactoringMessages.IntroduceParameterObjectWizard_ChoosePackageDialog_empty);
			dialog.setElements(packages);
			dialog.setHelpAvailable(false);

			IPackageFragment pack= getPackageFragment();
			if (pack != null) {
				dialog.setInitialSelections(new Object[] { pack });
			}

			if (dialog.open() == Window.OK) {
				return (IPackageFragment) dialog.getFirstResult();
			}
			return null;
		}

		private IPackageFragment getPackageFragment() {
			try {
				return fRefactoring.getPackageFragment();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return null;
		}

		private IPackageFragmentRoot getPackageFragmentRoot() {
			try {
				return fRefactoring.getPackageFragmentRoot();
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return null;
		}

		private Text createPackageInput(Group group) {
			Label l= new Label(group, SWT.NONE);
			l.setText(RefactoringMessages.IntroduceParameterObjectWizard_choosepackage_label);
			Composite comp= new Composite(group, SWT.NONE);
			comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			GridLayout gridLayout= new GridLayout(2, false);
			gridLayout.marginHeight= 0;
			gridLayout.marginWidth= 0;
			comp.setLayout(gridLayout);
			final Text text= new Text(comp, SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			String package1= fRefactoring.getPackage();
			if (package1 != null)
				text.setText(package1);
			text.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					fRefactoring.setPackage(text.getText());
					validateRefactoring();
				}

			});
			text.setEnabled(fRefactoring.isCreateAsTopLevel());

			Button button= new Button(comp, SWT.NONE);
			button.setText(RefactoringMessages.IntroduceParameterObjectWizard_choosepackage_button);
			button.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					IPackageFragment fragment= choosePackage();
					if (fragment != null)
						text.setText(fragment.getElementName());
				}

			});
			button.setLayoutData(new GridData());
			SWTUtil.setButtonDimensionHint(button);
			return text;
		}

		private void createSignaturePreview(Composite composite) {
			Label previewLabel= new Label(composite, SWT.NONE);
			previewLabel.setText(RefactoringMessages.IntroduceParameterObjectWizard_signaturepreview_label);
			GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan= 2;
			previewLabel.setLayoutData(gridData);

			// //XXX: use ViewForm to draw a flat border. Beware of common problems
			// with wrapping layouts
			// //inside GridLayout. GridData must be constrained to force wrapping.
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
			final Button button= new Button(group, SWT.CHECK);
			button.setText(RefactoringMessages.IntroduceParameterObjectWizard_createdelegate_checkbox);
			GridData gridData= new GridData();
			gridData.horizontalSpan= 2;
			button.setLayoutData(gridData);
			button.setEnabled(fRefactoring.canEnableDelegateUpdating());
			button.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setDelegateUpdating(button.getSelection());
					validateRefactoring();
				}

			});
		}

		private void createCommentsInput(Group group) {
			final Button button= new Button(group, SWT.CHECK);
			button.setText(RefactoringMessages.IntroduceParameterObjectWizard_createcomments_checkbox);
			GridData gridData= new GridData();
			gridData.horizontalSpan= 2;
			button.setLayoutData(gridData);
			button.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setCreateComments(button.getSelection());
					validateRefactoring();
				}

			});
		}

		private Group createGroup(Composite result, String caption) {
			Group group= new Group(result, SWT.None);
			group.setLayout(new GridLayout(2, false));
			group.setText(caption);
			GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan= 2;
			group.setLayoutData(gridData);
			return group;
		}

		private void createLocationInput(Composite parent, final Text packageInput) {
			Composite composite= new Composite(parent, SWT.None);
			GridLayout gridLayout= new GridLayout(2, true);
			gridLayout.marginHeight= 0;
			gridLayout.marginWidth= 0;
			composite.setLayout(gridLayout);
			GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan= 2;
			composite.setLayoutData(gridData);
			Button nestedRadio= new Button(composite, SWT.RADIO);
			nestedRadio.setText(Messages.format(RefactoringMessages.IntroduceParameterObjectWizard_createasnestedclass_radio, fRefactoring.getContainingClass().getName()));
			nestedRadio.setSelection(!fRefactoring.isCreateAsTopLevel());

			final Button topLvlRadio= new Button(composite, SWT.RADIO);
			topLvlRadio.setText(RefactoringMessages.IntroduceParameterObjectWizard_createastoplevel_radio);
			topLvlRadio.setSelection(fRefactoring.isCreateAsTopLevel());
			topLvlRadio.addSelectionListener(new SelectionAdapter() {

				private String lastPackage= packageInput.getText();

				public void widgetSelected(SelectionEvent e) {
					boolean fAsTopLevel= topLvlRadio.getSelection();
					fRefactoring.setCreateAsTopLevel(fAsTopLevel);
					packageInput.setEnabled(fAsTopLevel);
					if (fAsTopLevel) {
						packageInput.setText(lastPackage);
					} else {
						lastPackage= packageInput.getText();
						packageInput.setText(fRefactoring.getEnclosingPackage());
					}
					validateRefactoring();
				}

			});
		}

		private void createTable(Composite parent) {
			Composite result=new Composite(parent, SWT.NONE);
			GridLayout layout= new GridLayout(2,false);
			layout.marginHeight=0;
			layout.marginWidth=0;
			result.setLayout(layout);
			GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan=2;
			result.setLayoutData(gridData);
			TableLayoutComposite tlc= new TableLayoutComposite(result, SWT.NONE);
			tlc.addColumnData(new ColumnWeightData(40, convertWidthInCharsToPixels(20), true));
			tlc.addColumnData(new ColumnWeightData(60, convertWidthInCharsToPixels(20), true));
			final CheckboxTableViewer tv= CheckboxTableViewer.newCheckList(tlc, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
			tv.setContentProvider(new ParameterObjectCreatorContentProvider());
			TextCellEditor tce= new TextCellEditor(tv.getTable());

			TableViewerColumn tvc= new TableViewerColumn(tv, SWT.LEAD);
			tvc.setLabelProvider(new ParameterInfoLabelProvider() {

				protected String doGetValue(ParameterInfo pi) {
					return pi.getNewTypeName();
				}
			});
			/*
			 * tvc.setEditingSupport(new ParameterInfoEditingSupport(null, tv) {
			 * 
			 * public String doGetValue(ParameterInfo pi) { return
			 * pi.getNewTypeName(); }
			 * 
			 * public void doSetValue(ParameterInfo pi, String string) { // Not
			 * supported } });
			 */
			TableColumn column= tvc.getColumn();
			column.setText(RefactoringMessages.IntroduceParameterObjectWizard_type_column);
			tvc= new TableViewerColumn(tv, SWT.LEAD);
			tvc.setLabelProvider(new ParameterInfoLabelProvider() {

				protected String doGetValue(ParameterInfo pi) {
					return pi.getNewName();
				}
			});
			tvc.setEditingSupport(new ParameterInfoEditingSupport(tce, tv) {

				public String doGetValue(ParameterInfo pi) {
					return pi.getNewName();
				}

				public void doSetValue(ParameterInfo pi, String string) {
					pi.setNewName(string);
				}
			});
			column= tvc.getColumn();
			column.setText(RefactoringMessages.IntroduceParameterObjectWizard_name_column);

			Table table= tv.getTable();
			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			gridData= new GridData(GridData.FILL_BOTH);
			table.setLayoutData(gridData);
			tv.setInput(fRefactoring);
			List parameterInfos= fRefactoring.getParameterInfos();
			for (Iterator iter= parameterInfos.iterator(); iter.hasNext();) {
				ParameterInfo pi= (ParameterInfo) iter.next();
				if (!pi.isAdded()) {
					pi.setCreateField(true);
					tv.setChecked(pi, true);
				}
			}
			gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.heightHint= SWTUtil.getTableHeightHint(table, parameterInfos.size());
			tlc.setLayoutData(gridData);
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
			gridData.widthHint= SWTUtil.getButtonWidthHint(upButton);
			upButton.setLayoutData(gridData);
			upButton.setEnabled(false);

			final Button downButton= new Button(controls, SWT.NONE);
			downButton.setText(RefactoringMessages.IntroduceParameterObjectWizard_moventrydown_button);
			gridData= new GridData(GridData.FILL_HORIZONTAL);
			gridData.widthHint= SWTUtil.getButtonWidthHint(downButton);
			downButton.setLayoutData(gridData);
			downButton.setEnabled(false);

			downButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					ISelection selection= tv.getSelection();
					if (selection instanceof IStructuredSelection) {
						IStructuredSelection ss= (IStructuredSelection) selection;
						ParameterInfo selected= (ParameterInfo) ss.getFirstElement();
						fRefactoring.moveFieldDown(selected);
						tv.refresh();
						updateButtons(tv, upButton, downButton);
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
						updateButtons(tv, upButton, downButton);
					}
				}

			});
			tv.addCheckStateListener(new ICheckStateListener() {

				public void checkStateChanged(CheckStateChangedEvent event) {
					ParameterInfo element= (ParameterInfo) event.getElement();
					element.setCreateField(event.getChecked());
					updateButtons(tv, upButton, downButton);
					validateRefactoring();
				}

			});
			tv.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(SelectionChangedEvent event) {
					updateButtons(tv, upButton, downButton);
				}

			});
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

		private void updateButtons(final TableViewer tv, Button upButton, Button downButton) {
			int selectionIndex= tv.getTable().getSelectionIndex();
			if (selectionIndex == -1) {
				upButton.setEnabled(false);
				downButton.setEnabled(false);
			} else {
				upButton.setEnabled(selectionIndex != 0);
				downButton.setEnabled(selectionIndex != tv.getTable().getItemCount() - 1);
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
