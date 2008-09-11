/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.wizards.WizardMessages;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * The class <code>NewTestCaseWizardPageTwo</code> contains controls and validation routines
 * for the second page of the  'New JUnit TestCase Wizard'.
 * <p>
 * Clients can use the page as-is and add it to their own wizard, or extend it to modify
 * validation or add and remove controls.
 * </p>
 *
 * @since 3.1
 */
public class NewTestCaseWizardPageTwo extends WizardPage {

	private final static String PAGE_NAME= "NewTestCaseCreationWizardPage2"; //$NON-NLS-1$

	private final static String STORE_USE_TASKMARKER= PAGE_NAME + ".USE_TASKMARKER"; //$NON-NLS-1$
	private final static String STORE_CREATE_FINAL_METHOD_STUBS= PAGE_NAME + ".CREATE_FINAL_METHOD_STUBS"; //$NON-NLS-1$

	private IType fClassToTest;

	private Button fCreateFinalMethodStubsButton;
	private Button fCreateTasksButton;
	private ContainerCheckedTreeViewer fMethodsTree;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Label fSelectedMethodsLabel;
	private Object[] fCheckedObjects;
	private boolean fCreateFinalStubs;
	private boolean fCreateTasks;

	/**
	 * Creates a new <code>NewTestCaseWizardPageTwo</code>.
	 */
	public NewTestCaseWizardPageTwo() {
		super(PAGE_NAME);
		setTitle(WizardMessages.NewTestCaseWizardPageTwo_title);
		setDescription(WizardMessages.NewTestCaseWizardPageTwo_description);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		container.setLayout(layout);

		createMethodsTreeControls(container);
		createSpacer(container);
		createButtonChoices(container);
		setControl(container);
		restoreWidgetValues();
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IJUnitHelpContextIds.NEW_TESTCASE_WIZARD_PAGE2);
	}

	private void createButtonChoices(Composite container) {
		GridLayout layout;
		GridData gd;
		Composite prefixContainer= new Composite(container, SWT.NONE);
		gd= new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.horizontalSpan = 1;
		prefixContainer.setLayoutData(gd);

		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		prefixContainer.setLayout(layout);

		SelectionListener listener= new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doCheckBoxSelected(e.widget);
			}
		};
		fCreateFinalMethodStubsButton= createCheckBox(prefixContainer, WizardMessages.NewTestCaseWizardPageTwo_create_final_method_stubs_text, listener);
		fCreateTasksButton= createCheckBox(prefixContainer, WizardMessages.NewTestCaseWizardPageTwo_create_tasks_text, listener);
	}

	private Button createCheckBox(Composite parent, String name, SelectionListener listener) {
		Button button= new Button(parent, SWT.CHECK | SWT.LEFT);
		button.setText(name);
		button.setEnabled(true);
		button.setSelection(true);
		button.addSelectionListener(listener);
		GridData gd= new GridData(GridData.FILL, GridData.CENTER, false, false);
		button.setLayoutData(gd);
		return button;
	}


	private void doCheckBoxSelected(Widget widget) {
		if (widget == fCreateFinalMethodStubsButton) {
			fCreateFinalStubs= fCreateFinalMethodStubsButton.getSelection();
		} else if (widget == fCreateTasksButton) {
			fCreateTasks= fCreateTasksButton.getSelection();
		}
		saveWidgetValues();
	}

	private void createMethodsTreeControls(Composite container) {
		Label label= new Label(container, SWT.LEFT | SWT.WRAP);
		label.setFont(container.getFont());
		label.setText(WizardMessages.NewTestCaseWizardPageTwo_methods_tree_label);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		fMethodsTree= new ContainerCheckedTreeViewer(container, SWT.BORDER);
		gd= new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.heightHint= 180;
		fMethodsTree.getTree().setLayoutData(gd);

		fMethodsTree.setLabelProvider(new JavaElementLabelProvider());
		fMethodsTree.setAutoExpandLevel(2);
		fMethodsTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				doCheckedStateChanged();
			}
		});
		fMethodsTree.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IMethod) {
					IMethod method = (IMethod) element;
					return !method.getElementName().equals("<clinit>"); //$NON-NLS-1$
				}
				return true;
			}
		});


		Composite buttonContainer= new Composite(container, SWT.NONE);
		gd= new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout= new GridLayout();
		buttonLayout.marginWidth= 0;
		buttonLayout.marginHeight= 0;
		buttonContainer.setLayout(buttonLayout);

		fSelectAllButton= new Button(buttonContainer, SWT.PUSH);
		fSelectAllButton.setText(WizardMessages.NewTestCaseWizardPageTwo_selectAll);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fSelectAllButton.setLayoutData(gd);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fMethodsTree.setCheckedElements((Object[]) fMethodsTree.getInput());
				doCheckedStateChanged();
			}
		});
		LayoutUtil.setButtonDimensionHint(fSelectAllButton);

		fDeselectAllButton= new Button(buttonContainer, SWT.PUSH);
		fDeselectAllButton.setText(WizardMessages.NewTestCaseWizardPageTwo_deselectAll);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fDeselectAllButton.setLayoutData(gd);
		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fMethodsTree.setCheckedElements(new Object[0]);
				doCheckedStateChanged();
			}
		});
		LayoutUtil.setButtonDimensionHint(fDeselectAllButton);

		/* No of selected methods label */
		fSelectedMethodsLabel= new Label(container, SWT.LEFT);
		fSelectedMethodsLabel.setFont(container.getFont());
		doCheckedStateChanged();
		gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 1;
		fSelectedMethodsLabel.setLayoutData(gd);

		Label emptyLabel= new Label(container, SWT.LEFT);
		gd= new GridData();
		gd.horizontalSpan= 1;
		emptyLabel.setLayoutData(gd);
	}

	private void createSpacer(Composite container) {
		Label spacer= new Label(container, SWT.NONE);
		GridData data= new GridData();
		data.horizontalSpan= 2;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		data.heightHint= 4;
		spacer.setLayoutData(data);
	}

	/**
	 * Sets the class under test.
	 *
	 * @param classUnderTest the class under test
	 */
	public void setClassUnderTest(IType classUnderTest) {
		fClassToTest= classUnderTest;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			if (fClassToTest == null) {
				return;
			}

			ArrayList types= null;
			try {
				ITypeHierarchy hierarchy= fClassToTest.newSupertypeHierarchy(null);
				IType[] superTypes;
				if (fClassToTest.isClass())
					superTypes= hierarchy.getAllSuperclasses(fClassToTest);
				else if (fClassToTest.isInterface())
					superTypes= hierarchy.getAllSuperInterfaces(fClassToTest);
				else
					superTypes= new IType[0];
				types= new ArrayList(superTypes.length+1);
				types.add(fClassToTest);
				types.addAll(Arrays.asList(superTypes));
			} catch(JavaModelException e) {
				JUnitPlugin.log(e);
			}
			if (types == null)
				types= new ArrayList();
			fMethodsTree.setContentProvider(new MethodsTreeContentProvider(types.toArray()));
			fMethodsTree.setInput(types.toArray());
			fMethodsTree.setSelection(new StructuredSelection(fClassToTest), true);
			doCheckedStateChanged();

			fMethodsTree.getControl().setFocus();
		}
	}

	/**
	 * Returns all checked methods in the methods tree.
	 *
	 * @return the checked methods
	 */
	public IMethod[] getCheckedMethods() {
		int methodCount= 0;
		for (int i = 0; i < fCheckedObjects.length; i++) {
			if (fCheckedObjects[i] instanceof IMethod)
				methodCount++;
		}
		IMethod[] checkedMethods= new IMethod[methodCount];
		int j= 0;
		for (int i = 0; i < fCheckedObjects.length; i++) {
			if (fCheckedObjects[i] instanceof IMethod) {
				checkedMethods[j]= (IMethod)fCheckedObjects[i];
				j++;
			}
		}
		return checkedMethods;
	}

	private static class MethodsTreeContentProvider implements ITreeContentProvider {
		private Object[] fTypes;
		private IMethod[] fMethods;
		private final Object[] fEmpty= new Object[0];

		public MethodsTreeContentProvider(Object[] types) {
			fTypes= types;
			Vector methods= new Vector();
			for (int i = types.length-1; i > -1; i--) {
				Object object = types[i];
				if (object instanceof IType) {
					IType type = (IType) object;
					try {
						IMethod[] currMethods= type.getMethods();
						for_currMethods:
						for (int j = 0; j < currMethods.length; j++) {
							IMethod currMethod = currMethods[j];
							int flags= currMethod.getFlags();
							if (!Flags.isPrivate(flags) && !Flags.isSynthetic(flags)) {
								for (int k = 0; k < methods.size(); k++) {
									IMethod m= ((IMethod)methods.get(k));
									if (m.getElementName().equals(currMethod.getElementName())
										&& m.getSignature().equals(currMethod.getSignature())) {
										methods.set(k,currMethod);
										continue for_currMethods;
									}
								}
								methods.add(currMethod);
							}
						}
					} catch (JavaModelException e) {
						JUnitPlugin.log(e);
					}
				}
			}
			fMethods= new IMethod[methods.size()];
			methods.copyInto(fMethods);
		}

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IType) {
				IType parentType= (IType)parentElement;
				ArrayList result= new ArrayList(fMethods.length);
				for (int i= 0; i < fMethods.length; i++) {
					if (fMethods[i].getDeclaringType().equals(parentType)) {
						result.add(fMethods[i]);
					}
				}
				return result.toArray();
			}
			return fEmpty;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IMethod)
				return ((IMethod)element).getDeclaringType();
			return null;
		}

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		/*
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fTypes;
		}

		/*
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/*
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public IMethod[] getAllMethods() {
			return fMethods;
		}
	}

	/**
	 * Returns true if the checkbox for creating tasks is checked.
	 *
	 * @return <code>true</code> is returned if tasks should be created
	 */
	public boolean isCreateTasks() {
		return fCreateTasks;
	}

	/**
	 * Returns true if the checkbox for final method stubs is checked.
	 * @return <code>true</code> is returned if methods should be created final
	 */
	public boolean getCreateFinalMethodStubsButtonSelection() {
		return fCreateFinalStubs;
	}

	private void doCheckedStateChanged() {
		Object[] checked= fMethodsTree.getCheckedElements();
		fCheckedObjects= checked;

		int checkedMethodCount= 0;
		for (int i= 0; i < checked.length; i++) {
			if (checked[i] instanceof IMethod)
				checkedMethodCount++;
		}
		String label= ""; //$NON-NLS-1$
		if (checkedMethodCount == 1)
			label= Messages.format(WizardMessages.NewTestCaseWizardPageTwo_selected_methods_label_one, new Integer(checkedMethodCount));
		else
			label= Messages.format(WizardMessages.NewTestCaseWizardPageTwo_selected_methods_label_many, new Integer(checkedMethodCount));
		fSelectedMethodsLabel.setText(label);
	}

	/**
	 * Returns all the methods in the methods tree.
	 *
	 * @return all methods in the methods tree
	 */
	public IMethod[] getAllMethods() {
		return ((MethodsTreeContentProvider)fMethodsTree.getContentProvider()).getAllMethods();
	}

	/**
	 *	Use the dialog store to restore widget values to the values that they held
	 *	last time this wizard was used to completion
	 */
	private void restoreWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			fCreateTasks= settings.getBoolean(STORE_USE_TASKMARKER);
			fCreateTasksButton.setSelection(fCreateTasks);
			fCreateFinalStubs= settings.getBoolean(STORE_CREATE_FINAL_METHOD_STUBS);
			fCreateFinalMethodStubsButton.setSelection(fCreateFinalStubs);
		}
	}

	private void saveWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			settings.put(STORE_USE_TASKMARKER, fCreateTasks);
			settings.put(STORE_CREATE_FINAL_METHOD_STUBS, fCreateFinalStubs);
		}
	}

}
