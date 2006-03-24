/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeRefactoring;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.TextFieldNavigationHandler;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Wizard page for the extract supertype refactoring, which, apart from pull up
 * facilities, also allows to specify the types where to extract the supertype.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeMemberPage extends PullUpMemberPage {

	/** Dialog to select supertypes */
	private class SupertypeSelectionDialog extends SelectionDialog {

		/** The table viewer */
		private TableViewer fViewer;

		/**
		 * Creates a new supertype selection dialog.
		 * 
		 * @param shell
		 *            the parent shell
		 */
		public SupertypeSelectionDialog(final Shell shell) {
			super(shell);
		}

		/**
		 * {@inheritDoc}
		 */
		public void create() {
			setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE);
			super.create();
			getShell().setText(RefactoringMessages.ExtractSupertypeMemberPage_choose_type_caption);
		}

		/**
		 * {@inheritDoc}
		 */
		protected Control createDialogArea(final Composite composite) {
			Assert.isNotNull(composite);
			setMessage(RefactoringMessages.ExtractSupertypeMemberPage_choose_type_message);
			final Composite control= (Composite) super.createDialogArea(composite);
			createMessageArea(control);
			fViewer= new TableViewer(control, SWT.VIRTUAL | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			fViewer.setLabelProvider(createLabelProvider());
			fViewer.setContentProvider(new ArrayContentProvider());
			fViewer.setSorter(new JavaElementSorter());
			fViewer.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(final SelectionChangedEvent event) {
					setSelectionResult(((IStructuredSelection) fViewer.getSelection()).toArray());
				}
			});
			fViewer.addDoubleClickListener(new IDoubleClickListener() {

				public void doubleClick(final DoubleClickEvent event) {
					setSelectionResult(((IStructuredSelection) fViewer.getSelection()).toArray());
					close();
				}
			});
			final GridData data= new GridData(GridData.FILL_BOTH);
			data.heightHint= convertHeightInCharsToPixels(15);
			data.widthHint= convertWidthInCharsToPixels(55);
			fViewer.getTable().setLayoutData(data);
			applyDialogFont(control);
			return control;
		}

		/**
		 * Sets the input of this dialog.
		 * 
		 * @param object
		 *            the input
		 */
		public void setInput(final Object object) {
			fViewer.setInput(object);
		}
	}

	/** The supertype name field */
	private Text fNameField;

	/** The table viewer */
	private TableViewer fTableViewer;

	/** The types to extract */
	private final Set fTypesToExtract= new HashSet(2);

	/** Have the working copies already been created? */
	private boolean fWorkingCopiesCreated= false;

	/**
	 * Creates a new extract supertype member page.
	 * 
	 * @param name
	 *            the page name
	 * @param page
	 *            the method page
	 */
	public ExtractSupertypeMemberPage(final String name, final PullUpMethodPage page) {
		super(name, page);
		setDescription(RefactoringMessages.ExtractSupertypeMemberPage_page_title);
		METHOD_LABELS[PULL_UP_ACTION]= RefactoringMessages.ExtractSupertypeMemberPage_extract;
		METHOD_LABELS[DECLARE_ABSTRACT_ACTION]= RefactoringMessages.ExtractSupertypeMemberPage_declare_abstract;
		TYPE_LABELS[PULL_UP_ACTION]= RefactoringMessages.ExtractSupertypeMemberPage_extract;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void checkPageCompletionStatus(final boolean display) {
		final RefactoringStatus status= getProcessor().checkExtractedCompilationUnit();
		if (!status.hasFatalError()) {
			if (areAllMembersMarkedAsWithNoAction())
				status.addFatalError(getNoMembersMessage());
		}
		setMessage(null);
		if (display)
			setPageComplete(status);
		else
			setPageComplete(!status.hasFatalError());
		fSuccessorPage.fireSettingsChanged();
	}

	/**
	 * Creates the button composite.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createButtonComposite(final Composite parent) {
		final Composite buttons= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		buttons.setLayout(layout);
		buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		final Button addButton= new Button(buttons, SWT.PUSH);
		addButton.setText(RefactoringMessages.ExtractSupertypeMemberPage_add_button_label);
		addButton.setEnabled(isAddTypeEnabled());
		addButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(addButton);
		addButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				final Set set= new HashSet(Arrays.asList(fCandidateTypes));
				set.removeAll(fTypesToExtract);
				if (!set.isEmpty()) {
					final SupertypeSelectionDialog dialog= new SupertypeSelectionDialog(getShell());
					dialog.create();
					dialog.setInput(set.toArray());
					final int result= dialog.open();
					if (result == Window.OK) {
						final Object[] objects= dialog.getResult();
						for (int index= 0; index < objects.length; index++) {
							fTypesToExtract.add(objects[index]);
						}
						fTableViewer.setInput(fTypesToExtract.toArray());
						addButton.setEnabled(isAddTypeEnabled());
						handleTypesChanged();
					}
				}
			}
		});

		final Button removeButton= new Button(buttons, SWT.PUSH);
		removeButton.setText(RefactoringMessages.ExtractSupertypeMemberPage_remove_button_label);
		removeButton.setEnabled(fCandidateTypes.length > 0 && !fTableViewer.getSelection().isEmpty());
		removeButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(removeButton);
		removeButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				final IStructuredSelection selection= (IStructuredSelection) fTableViewer.getSelection();
				if (!selection.isEmpty()) {
					final IType declaring= getDeclaringType();
					for (final Iterator iterator= selection.iterator(); iterator.hasNext();) {
						final Object element= iterator.next();
						if (!declaring.equals(element))
							fTypesToExtract.remove(element);
					}
					fTableViewer.setInput(fTypesToExtract.toArray());
					addButton.setEnabled(isAddTypeEnabled());
					handleTypesChanged();
				}
			}
		});
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(final SelectionChangedEvent event) {
				final IStructuredSelection selection= (IStructuredSelection) fTableViewer.getSelection();
				if (selection.isEmpty()) {
					removeButton.setEnabled(false);
					return;
				} else {
					final Object[] elements= selection.toArray();
					if (elements.length == 1 && elements[0].equals(getDeclaringType())) {
						removeButton.setEnabled(false);
						return;
					}
				}
				removeButton.setEnabled(true);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public void createControl(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		composite.setLayout(layout);
		createSuperTypeField(composite);
		createSpacer(composite);
		createSuperTypeCheckbox(composite);
		createInstanceOfCheckbox(composite, layout.marginWidth);
		createStubCheckbox(composite);
		createSuperTypeControl(composite);
		createSpacer(composite);
		createMemberTableLabel(composite);
		createMemberTableComposite(composite);
		createStatusLine(composite);

		setControl(composite);
		Dialog.applyDialogFont(composite);
		initializeCheckboxes();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.EXTRACT_SUPERTYPE_WIZARD_PAGE);
	}

	/**
	 * Creates a label provider for a type list.
	 * 
	 * @return a label provider
	 */
	protected ILabelProvider createLabelProvider() {
		return new AppearanceAwareLabelProvider(JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.T_POST_QUALIFIED, JavaElementImageProvider.OVERLAY_ICONS);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void createSuperTypeControl(final Composite parent) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) throws InvocationTargetException {
					try {
						fCandidateTypes= getProcessor().getCandidateTypes(new RefactoringStatus(), monitor);
						createSuperTypeList(parent);
					} catch (JavaModelException exception) {
						throw new InvocationTargetException(exception);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (InvocationTargetException exception) {
			ExceptionHandler.handle(exception, getShell(), RefactoringMessages.ExtractSupertypeMemberPage_extract_supertype, RefactoringMessages.PullUpInputPage_exception);
		} catch (InterruptedException exception) {
			Assert.isTrue(false);
		}
	}

	/**
	 * Creates the super type field.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createSuperTypeField(final Composite parent) {
		final Label label= new Label(parent, SWT.NONE);
		label.setText(RefactoringMessages.ExtractSupertypeMemberPage_name_label);
		label.setLayoutData(new GridData());

		fNameField= new Text(parent, SWT.BORDER);
		fNameField.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				handleNameChanged(fNameField.getText());
			}
		});
		fNameField.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		TextFieldNavigationHandler.install(fNameField);
	}

	/**
	 * Creates the super type list.
	 * 
	 * @param parent
	 *            the parent control
	 */
	protected void createSuperTypeList(final Composite parent) throws JavaModelException {
		final Label separator= new Label(parent, SWT.NONE);
		GridData data= new GridData();
		data.horizontalSpan= 2;
		separator.setLayoutData(data);

		final Label label= new Label(parent, SWT.NONE);
		label.setText(RefactoringMessages.ExtractSupertypeMemberPage_types_list_caption);
		label.setEnabled(fCandidateTypes.length > 0);
		data= new GridData();
		data.horizontalSpan= 2;
		label.setLayoutData(data);

		Composite composite= new Composite(parent, SWT.NONE);
		final GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		composite.setLayout(layout);
		data= new GridData(GridData.FILL_BOTH);
		data.heightHint= convertHeightInCharsToPixels(12);
		data.horizontalSpan= 2;
		composite.setLayoutData(data);

		fTableViewer= new TableViewer(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		fTableViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		fTableViewer.setLabelProvider(createLabelProvider());
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setSorter(new JavaElementSorter());
		fTypesToExtract.add(getDeclaringType());
		fTableViewer.setInput(fTypesToExtract.toArray());
		fTableViewer.getControl().setEnabled(fCandidateTypes.length > 0);

		createButtonComposite(composite);
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getCreateStubsButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_create_stubs_label;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getDeclareAbstractActionLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_declare_abstract;
	}

	/**
	 * Returns the declaring type.
	 * 
	 * @return the declaring type
	 */
	public IType getDeclaringType() {
		return getProcessor().getDeclaringType();
	}

	/**
	 * {@inheritDoc}
	 */
	public IType getDestinationType() {
		return getProcessor().computeDestinationType(fNameField.getText());
	}

	/**
	 * Returns the extract supertype refactoring.
	 */
	public ExtractSupertypeRefactoring getExtractSuperTypeRefactoring() {
		return (ExtractSupertypeRefactoring) getRefactoring();
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getInstanceofButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_use_supertype_label;
	}

	/**
	 * {@inheritDoc}
	 */
	public IWizardPage getNextPage() {
		if (!fWorkingCopiesCreated) {
			try {
				getWizard().getContainer().run(true, false, new IRunnableWithProgress() {

					public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						getProcessor().createWorkingCopyLayer(monitor);
					}
				});
			} catch (InvocationTargetException exception) {
				JavaPlugin.log(exception);
			} catch (InterruptedException exception) {
				// Does not happen
			} finally {
				fWorkingCopiesCreated= true;
			}
		}
		return super.getNextPage();
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getNoMembersMessage() {
		return RefactoringMessages.ExtractSupertypeMemberPage_no_members_selected;
	}

	/**
	 * Returns the refactoring processor.
	 * 
	 * @return the refactoring processor
	 */
	protected ExtractSupertypeProcessor getProcessor() {
		return getExtractSuperTypeRefactoring().getExtractSupertypeProcessor();
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getPullUpActionLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_extract;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getReplaceButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_use_instanceof_label;
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getTableRowCount() {
		return 6;
	}

	/**
	 * Handles the name changed event.
	 * 
	 * @param name
	 *            the name
	 */
	protected void handleNameChanged(final String name) {
		if (name != null)
			getProcessor().setTypeName(name);
		checkPageCompletionStatus(true);
	}

	/**
	 * Handles the types changed event.
	 */
	protected void handleTypesChanged() {
		getProcessor().setTypesToExtract((IType[]) fTypesToExtract.toArray(new IType[fTypesToExtract.size()]));
	}

	/**
	 * Is the add type button enabled?
	 * 
	 * @return <code>true</code> if it is enabled, <code>false</code>
	 *         otherwise
	 */
	protected boolean isAddTypeEnabled() {
		return fCandidateTypes.length > 0 && fTypesToExtract.size() < fCandidateTypes.length;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		if (visible)
			fNameField.setFocus();
	}
}