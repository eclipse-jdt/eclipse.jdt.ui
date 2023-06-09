/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.IVisibilityChangeListener;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class GenerateConstructorUsingFieldsSelectionDialog extends SourceActionDialog {

	class GenerateConstructorUsingFieldsTreeViewerAdapter implements ISelectionChangedListener, IDoubleClickListener {

		@Override
		public void doubleClick(DoubleClickEvent event) {
			// Do nothing
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();

			List<?> selectedList= selection.toList();
			GenerateConstructorUsingFieldsContentProvider cp= (GenerateConstructorUsingFieldsContentProvider) getContentProvider();

			fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX].setEnabled(cp.canMoveUp(selectedList));
			fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX].setEnabled(cp.canMoveDown(selectedList));
		}
	}

	private static final int DOWN_BUTTON= IDialogConstants.CLIENT_ID + 2;

	private static final int UP_BUTTON= IDialogConstants.CLIENT_ID + 1;

	protected Button[] fButtonControls;

	boolean[] fButtonsEnabled;

	IDialogSettings fGenConstructorSettings;

	int fHeight= 18;

	boolean fOmitSuper;

	Button fOmitSuperButton;

	IMethodBinding[] fSuperConstructors;

	int fSuperIndex;

	GenerateConstructorUsingFieldsTreeViewerAdapter fTreeViewerAdapter;

	int fWidth= 60;

	static final String OMIT_SUPER= "OmitCallToSuper"; //$NON-NLS-1$

	static final String SETTINGS_SECTION= "GenerateConstructorUsingFieldsSelectionDialog"; //$NON-NLS-1$

	private static final int DOWN_INDEX= 1;

	private static final int UP_INDEX= 0;

	public GenerateConstructorUsingFieldsSelectionDialog(Shell parent, ILabelProvider labelProvider, GenerateConstructorUsingFieldsContentProvider contentProvider, CompilationUnitEditor editor, IType type, IMethodBinding[] superConstructors) throws JavaModelException {
		super(parent, labelProvider, contentProvider, editor, type, true);
		fTreeViewerAdapter= new GenerateConstructorUsingFieldsTreeViewerAdapter();

		fSuperConstructors= superConstructors;

		IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
		fGenConstructorSettings= dialogSettings.getSection(SETTINGS_SECTION);
		if (fGenConstructorSettings == null) {
			fGenConstructorSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
			fGenConstructorSettings.put(OMIT_SUPER, false);
		}

		final boolean isEnum= type.isEnum();
		final boolean isRecord= type.isRecord();
		fOmitSuper= fGenConstructorSettings.getBoolean(OMIT_SUPER) || isEnum || isRecord;
		if (isEnum)
			setVisibility(Modifier.PRIVATE);
	}

	Composite addSuperClassConstructorChoices(Composite composite) {
		Label label= new Label(composite, SWT.NONE);
		label.setText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_sort_constructor_choices_label);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);

		BindingLabelProvider provider= new BindingLabelProvider();
		final Combo combo= new Combo(composite, SWT.READ_ONLY);
		SWTUtil.setDefaultVisibleItemCount(combo);
		for (IMethodBinding binding : fSuperConstructors) {
			combo.add(provider.getText(binding));
		}

		// TODO: Can we be a little more intelligent about guessing the super() ?
		combo.setText(combo.getItem(0));
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				fSuperIndex= combo.getSelectionIndex();
				// Disable omit super checkbox unless default constructor
				fOmitSuperButton.setEnabled(getSuperConstructorChoice().getParameterTypes().length == 0);
				updateOKStatus();
			}
		});

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_button_generate, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		super.buttonPressed(buttonId);
		switch (buttonId) {
			case UP_BUTTON: {
				GenerateConstructorUsingFieldsContentProvider contentProvider= (GenerateConstructorUsingFieldsContentProvider) getTreeViewer().getContentProvider();
				contentProvider.up(getElementList(), getTreeViewer());
				updateOKStatus();
				break;
			}
			case DOWN_BUTTON: {
				GenerateConstructorUsingFieldsContentProvider contentProvider= (GenerateConstructorUsingFieldsContentProvider) getTreeViewer().getContentProvider();
				contentProvider.down(getElementList(), getTreeViewer());
				updateOKStatus();
				break;
			}
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.GENERATE_CONSTRUCTOR_USING_FIELDS_SELECTION_DIALOG);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		GridData gd;

		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);

		Composite classConstructorComposite= addSuperClassConstructorChoices(composite);
		gd= new GridData(GridData.FILL_BOTH);
		classConstructorComposite.setLayoutData(gd);

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout innerLayout= new GridLayout();
		innerLayout.numColumns= 2;
		innerLayout.marginHeight= 0;
		innerLayout.marginWidth= 0;
		inner.setLayout(innerLayout);

		Label messageLabel= createMessageArea(inner);
		if (messageLabel != null) {
			gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan= 2;
			messageLabel.setLayoutData(gd);
		}

		CheckboxTreeViewer treeViewer= createTreeViewer(inner);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(fWidth);
		gd.heightHint= convertHeightInCharsToPixels(fHeight);
		treeViewer.getControl().setLayoutData(gd);
		treeViewer.addSelectionChangedListener(fTreeViewerAdapter);

		Composite buttonComposite= createSelectionButtons(inner);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		buttonComposite.setLayoutData(gd);

		gd= new GridData(GridData.FILL_BOTH);
		inner.setLayoutData(gd);

		Composite entryComposite= createInsertPositionCombo(composite);
		entryComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite commentComposite= createCommentSelection(composite);
		commentComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite overrideSuperComposite= createOmitSuper(composite);
		overrideSuperComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control linkControl= createLinkControl(composite);
		if (linkControl != null)
			linkControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		gd= new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		applyDialogFont(composite);

		return composite;
	}

	@Override
	protected Composite createInsertPositionCombo(Composite composite) {
		Composite entryComposite= super.createInsertPositionCombo(composite);
		addVisibilityAndModifiersChoices(entryComposite);
		return entryComposite;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createLinkControl(Composite composite) {
		Link link= new Link(composite, SWT.WRAP);
		link.setText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_template_link_message);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openCodeTempatePage(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID);
			}
		});
		link.setToolTipText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_template_link_tooltip);

		GridData gridData= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint= convertWidthInCharsToPixels(40); // only expand further if anyone else requires it
		link.setLayoutData(gridData);
		return link;
	}

	protected Composite createOmitSuper(Composite composite) {
		Composite omitSuperComposite= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		omitSuperComposite.setLayout(layout);

		fOmitSuperButton= new Button(omitSuperComposite, SWT.CHECK);
		fOmitSuperButton.setText(ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_omit_super);
		fOmitSuperButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		fOmitSuperButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected= (((Button) e.widget).getSelection());
				setOmitSuper(isSelected);
			}
		});
		fOmitSuperButton.setSelection(isOmitSuper());
		try {
			// Disable omit super checkbox unless default constructor and enum
			final boolean hasContructor= getSuperConstructorChoice().getParameterTypes().length == 0;
			IType type= getType();
			fOmitSuperButton.setEnabled(hasContructor && !type.isEnum() && !type.isRecord());
		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
		}
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		fOmitSuperButton.setLayoutData(gd);

		return omitSuperComposite;
	}

	@Override
	protected Composite createSelectionButtons(Composite composite) {
		Composite buttonComposite= super.createSelectionButtons(composite);

		GridLayout layout= new GridLayout();
		buttonComposite.setLayout(layout);

		createUpDownButtons(buttonComposite);

		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;

		return buttonComposite;
	}

	void createUpDownButtons(Composite buttonComposite) {
		int numButtons= 2; // up, down
		fButtonControls= new Button[numButtons];
		fButtonsEnabled= new boolean[numButtons];
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX]= createButton(buttonComposite, UP_BUTTON, ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_up_button,false);
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX]= createButton(buttonComposite, DOWN_BUTTON, ActionMessages.GenerateConstructorUsingFieldsSelectionDialog_down_button,false);
		boolean defaultState= false;
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX].setEnabled(defaultState);
		fButtonControls[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX].setEnabled(defaultState);
		fButtonsEnabled[GenerateConstructorUsingFieldsSelectionDialog.UP_INDEX]= defaultState;
		fButtonsEnabled[GenerateConstructorUsingFieldsSelectionDialog.DOWN_INDEX]= defaultState;
	}

	@Override
	protected Composite createVisibilityControlAndModifiers(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
		int[] visibilities= availableVisibilities;
		try {
			if (getType().isEnum())
				visibilities= new int[] { };
		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
		}
		return createVisibilityControl(parent, visibilityChangeListener, visibilities, correctVisibility);
	}

	List<?> getElementList() {
		IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();
		@SuppressWarnings("unchecked")
		ArrayList<?> elementList= new ArrayList<>(selection.toList());

		return elementList;
	}

	public IMethodBinding getSuperConstructorChoice() {
		return fSuperConstructors[fSuperIndex];
	}

	public boolean isOmitSuper() {
		return fOmitSuper;
	}

	public void setOmitSuper(boolean omitSuper) {
		if (fOmitSuper != omitSuper) {
			fOmitSuper= omitSuper;
			fGenConstructorSettings.put(OMIT_SUPER, omitSuper);
		}
	}
}
