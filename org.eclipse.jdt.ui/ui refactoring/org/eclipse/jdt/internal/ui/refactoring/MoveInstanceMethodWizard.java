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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.util.TableLayoutComposite;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Refactoring wizard for the 'move instance method' refactoring.
 */
public final class MoveInstanceMethodWizard extends RefactoringWizard {

	/**
	 * The input wizard page of the 'move instance method' refactoring.
	 */
	public final class MoveInstanceMethodPage extends UserInputWizardPage {

		/** The page name */
		protected static final String PAGE_NAME= "MoveInstanceMethodPage"; //$NON-NLS-1$

		/** The create delegator button */
		protected Button fCreateDelegator= null;

		/** The deprecate delegator button */
		protected Button fDeprecateDelegator= null;

		/** The method name text field */
		protected Text fMethodNameField= null;

		/** The current method name status */
		protected RefactoringStatus fMethodNameStatus= new RefactoringStatus();

		/** The target name text field */
		protected Text fTargetNameField= null;

		/** The target name label */
		protected Label fTargetNameLabel= null;

		/** The current target name status */
		protected RefactoringStatus fTargetNameStatus= new RefactoringStatus();

		/** The current target type status */
		protected RefactoringStatus fTargetTypeStatus= new RefactoringStatus();

		/**
		 * Creates a new move instance method page.
		 */
		public MoveInstanceMethodPage() {
			super(PAGE_NAME);
		}

		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(final Composite parent) {
			Assert.isNotNull(parent);
			final Composite control= new Composite(parent, SWT.NONE);
			setControl(control);

			final GridLayout layout= new GridLayout();
			layout.numColumns= 2;
			control.setLayout(layout);

			Label label= new Label(control, SWT.SINGLE);
			label.setText(RefactoringMessages.getFormattedString("MoveInstanceMethodPage.New_receiver", JavaElementLabels.getElementLabel(fProcessor.getMethod(), JavaElementLabels.ALL_DEFAULT | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PRE_TYPE_PARAMETERS | JavaElementLabels.M_PARAMETER_NAMES))); //$NON-NLS-1$

			GridData data= new GridData();
			data.horizontalSpan= 2;
			label.setLayoutData(data);

			final TableLayoutComposite composite= new TableLayoutComposite(control, SWT.NULL);
			composite.addColumnData(new ColumnWeightData(40, true));
			composite.addColumnData(new ColumnWeightData(60, true));

			final Table table= new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
			table.setHeaderVisible(true);
			table.setLinesVisible(false);

			TableColumn column= new TableColumn(table, SWT.NONE);
			column.setText(RefactoringMessages.getString("MoveInstanceMethodPage.Name")); //$NON-NLS-1$
			column.setResizable(true);

			column= new TableColumn(table, SWT.NONE);
			column.setText(RefactoringMessages.getString("MoveInstanceMethodPage.Type")); //$NON-NLS-1$
			column.setResizable(true);

			final TableViewer viewer= new TableViewer(table);
			viewer.setContentProvider(new ArrayContentProvider());
			viewer.setLabelProvider(new TargetLabelProvider());

			final IVariableBinding[] candidateTargets= fProcessor.getCandidateTargets();
			viewer.setInput(candidateTargets);
			final IVariableBinding[] possibleTargets= fProcessor.getPossibleTargets();
			viewer.setSelection(new StructuredSelection(new Object[] { possibleTargets[0]}));

			viewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public final void selectionChanged(final SelectionChangedEvent event) {
					final Object element= ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (element instanceof IVariableBinding) {
						final IVariableBinding target= (IVariableBinding) element;
						final IVariableBinding[] targets= fProcessor.getPossibleTargets();
						boolean success= false;
						for (int index= 0; index < targets.length; index++) {
							if (Bindings.equals(target, targets[index])) {
								handleTargetChanged(target);
								success= true;
								break;
							}
						}
						if (!success)
							fTargetTypeStatus= RefactoringStatus.createWarningStatus(RefactoringMessages.getFormattedString("MoveInstanceMethodPage.invalid.target", target.getName())); //$NON-NLS-1$
						else
							fTargetTypeStatus= new RefactoringStatus();
						handleStatusChanged();
					}
				}
			});

			data= new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
			data.heightHint= SWTUtil.getTableHeightHint(table, 7);
			data.horizontalSpan= 2;
			composite.setLayoutData(data);

			label= new Label(control, SWT.SINGLE);
			label.setText(RefactoringMessages.getString("MoveInstanceMethodPage.Method_name")); //$NON-NLS-1$
			label.setLayoutData(new GridData());

			fMethodNameField= new Text(control, SWT.SINGLE | SWT.BORDER);
			fMethodNameField.setText(fProcessor.getMethodName());
			fMethodNameField.selectAll();
			fMethodNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fMethodNameField.setFocus();
			fMethodNameField.addModifyListener(new ModifyListener() {

				public final void modifyText(final ModifyEvent event) {
					fMethodNameStatus= fProcessor.setMethodName(fMethodNameField.getText());
					handleStatusChanged();
				}
			});

			fTargetNameLabel= new Label(control, SWT.SINGLE);
			fTargetNameLabel.setText(RefactoringMessages.getString("MoveInstanceMethodPage.Target_name")); //$NON-NLS-1$
			fTargetNameLabel.setLayoutData(new GridData());

			fTargetNameField= new Text(control, SWT.SINGLE | SWT.BORDER);
			final String name= fProcessor.getTargetName();
			if (name != null && name.length() > 0)
				fTargetNameField.setText(fProcessor.getTargetName());
			else {
				setPageComplete(RefactoringStatus.createInfoStatus(RefactoringCoreMessages.getString("Checks.Choose_name"))); //$NON-NLS-1$
				setPageComplete(false);
			}
			fTargetNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fTargetNameField.addModifyListener(new ModifyListener() {

				public final void modifyText(final ModifyEvent event) {
					fTargetNameStatus= fProcessor.setTargetName(fTargetNameField.getText());
					handleStatusChanged();
				}
			});

			label= new Label(control, SWT.NONE);

			data= new GridData();
			data.horizontalSpan= 2;
			label.setLayoutData(data);

			fCreateDelegator= new Button(control, SWT.CHECK);
			fCreateDelegator.setSelection(DEFAULT_CREATE_DELEGATOR_SETTING);
			fCreateDelegator.setText(RefactoringMessages.getString("MoveInstanceMethodPage.Create_button_name")); //$NON-NLS-1$
			fCreateDelegator.addSelectionListener(new SelectionAdapter() {

				public final void widgetDefaultSelected(final SelectionEvent event) {
					widgetSelected(event);
				}

				public final void widgetSelected(final SelectionEvent event) {
					final boolean selection= fCreateDelegator.getSelection();
					fProcessor.setInlineDelegator(!selection);
					fProcessor.setRemoveDelegator(!selection);
					fDeprecateDelegator.setEnabled(selection);
				}
			});

			data= new GridData();
			data.horizontalSpan= 2;
			fCreateDelegator.setLayoutData(data);

			fDeprecateDelegator= new Button(control, SWT.CHECK);
			fDeprecateDelegator.setSelection(DEFAULT_DEPRECATE_DELEGATOR_SETTING);
			fDeprecateDelegator.setEnabled(DEFAULT_CREATE_DELEGATOR_SETTING);
			fDeprecateDelegator.setText(RefactoringMessages.getString("MoveInstanceMethodPage.Deprecate_button_name")); //$NON-NLS-1$
			fDeprecateDelegator.addSelectionListener(new SelectionAdapter() {

				public final void widgetDefaultSelected(final SelectionEvent event) {
					widgetSelected(event);
				}

				public final void widgetSelected(final SelectionEvent event) {
					fProcessor.setDeprecated(fDeprecateDelegator.getSelection());
				}
			});

			data= new GridData();
			data.horizontalSpan= 2;
			data.horizontalIndent= IDialogConstants.INDENT;
			fDeprecateDelegator.setLayoutData(data);

			fProcessor.setInlineDelegator(!DEFAULT_CREATE_DELEGATOR_SETTING);
			fProcessor.setRemoveDelegator(!DEFAULT_CREATE_DELEGATOR_SETTING);
			fProcessor.setDeprecated(DEFAULT_DEPRECATE_DELEGATOR_SETTING);

			handleTargetChanged(possibleTargets[0]);

			Dialog.applyDialogFont(control);
			WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.MOVE_MEMBERS_WIZARD_PAGE);
		}

		/**
		 * Handles the status changed event.
		 */
		protected final void handleStatusChanged() {
			final RefactoringStatus status= new RefactoringStatus();
			status.merge(fMethodNameStatus);
			status.merge(fTargetNameStatus);
			status.merge(fTargetTypeStatus);
			if (!fTargetTypeStatus.isOK())
				setPageComplete(false);
			else
				setPageComplete(status);
		}

		/**
		 * Handles the target changed event.
		 * 
		 * @param target the changed target
		 */
		protected final void handleTargetChanged(final IVariableBinding target) {
			Assert.isNotNull(target);
			fProcessor.setTarget(target);
			fTargetNameField.setEnabled(fProcessor.needsTargetNode());
			fTargetNameLabel.setEnabled(fProcessor.needsTargetNode());
		}
	}

	/**
	 * Table label provider for the target selection table.
	 */
	public static class TargetLabelProvider extends JavaElementLabelProvider implements ITableLabelProvider {

		/**
		 * Creates a new target label provider.
		 */
		public TargetLabelProvider() {
			super(SHOW_OVERLAY_ICONS);
		}

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		public Image getColumnImage(final Object element, final int column) {
			Assert.isTrue(element instanceof IVariableBinding);
			Assert.isTrue(column >= 0 && column <= 1);
			final IVariableBinding binding= (IVariableBinding) element;
			switch (column) {
				case 0:
					final IJavaElement item= binding.getJavaElement();
					if (item != null)
						return getImage(item);
					else {
						final ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
						if (binding.getDeclaringClass() != null) {
							final int flags= binding.getModifiers();
							if (Flags.isPublic(flags))
								return registry.get(JavaPluginImages.DESC_FIELD_PUBLIC);
							if (Flags.isProtected(flags))
								return registry.get(JavaPluginImages.DESC_FIELD_PROTECTED);
							if (Flags.isPrivate(flags))
								return registry.get(JavaPluginImages.DESC_FIELD_PRIVATE);
							return registry.get(JavaPluginImages.DESC_FIELD_DEFAULT);
						} else
							return registry.get(JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE);
					}
				default:
					return null;
			}
		}

		/*
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(final Object element, final int column) {
			Assert.isTrue(element instanceof IVariableBinding);
			Assert.isTrue(column >= 0 && column <= 1);
			final IVariableBinding binding= (IVariableBinding) element;
			switch (column) {
				case 0:
					return binding.getName();
				case 1:
					return Bindings.getFullyQualifiedName(binding.getType());
				default:
					return null;
			}
		}
	}

	/** The default create delegator setting */
	protected static boolean DEFAULT_CREATE_DELEGATOR_SETTING= false;

	/** The default deprecate delegator setting */
	protected static boolean DEFAULT_DEPRECATE_DELEGATOR_SETTING= false;

	/** The associated move instance method processor */
	protected final MoveInstanceMethodProcessor fProcessor;

	/**
	 * Creates a new move instance method wizard.
	 * 
	 * @param refactoring the refactoring to host
	 */
	public MoveInstanceMethodWizard(final MoveInstanceMethodRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_UESR_INTERFACE);
		fProcessor= refactoring.getMoveMethodProcessor();
		setDefaultPageTitle(RefactoringMessages.getString("MoveInstanceMethodWizard.Move_Method")); //$NON-NLS-1$
	}

	/*
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {
		addPage(new MoveInstanceMethodPage());
	}
}
