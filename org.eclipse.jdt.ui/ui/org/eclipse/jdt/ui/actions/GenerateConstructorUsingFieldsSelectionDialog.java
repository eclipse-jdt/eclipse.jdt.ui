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
package org.eclipse.jdt.ui.actions;

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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog;
import org.eclipse.jdt.internal.ui.dialogs.SourceActionLabelProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.IVisibilityChangeListener;

class GenerateConstructorUsingFieldsSelectionDialog extends SourceActionDialog {

	class GenerateConstructorUsingFieldsTreeViewerAdapter implements ISelectionChangedListener, IDoubleClickListener {

		public void doubleClick(DoubleClickEvent event) {
			// Do nothing
		}

		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();

			List selectedList= selection.toList();
			GenerateConstructorUsingFieldsContentProvider cp= (GenerateConstructorUsingFieldsContentProvider) getContentProvider();

			fButtonControls[GenerateNewConstructorUsingFieldsAction.UP_INDEX].setEnabled(cp.canMoveUp(selectedList));
			fButtonControls[GenerateNewConstructorUsingFieldsAction.DOWN_INDEX].setEnabled(cp.canMoveDown(selectedList));
		}
	}

	static final int DOWN_BUTTON= IDialogConstants.CLIENT_ID + 2;

	static final int UP_BUTTON= IDialogConstants.CLIENT_ID + 1;

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

	final String OMIT_SUPER= "OmitCallToSuper"; //$NON-NLS-1$

	final String SETTINGS_SECTION= "GenerateConstructorUsingFieldsSelectionDialog"; //$NON-NLS-1$

	public GenerateConstructorUsingFieldsSelectionDialog(Shell parent, ILabelProvider labelProvider, GenerateConstructorUsingFieldsContentProvider contentProvider, CompilationUnitEditor editor, IType type, IMethodBinding[] superConstructors) throws JavaModelException {
		super(parent, labelProvider, contentProvider, editor, type, true);
		fTreeViewerAdapter= new GenerateConstructorUsingFieldsTreeViewerAdapter();

		fSuperConstructors= superConstructors;

		IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
		fGenConstructorSettings= dialogSettings.getSection(SETTINGS_SECTION);
		if (fGenConstructorSettings == null) {
			fGenConstructorSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
			fGenConstructorSettings.put(OMIT_SUPER, false); //$NON-NLS-1$
		}

		fOmitSuper= fGenConstructorSettings.getBoolean(OMIT_SUPER);
	}

	Composite addSuperClassConstructorChoices(Composite composite) {
		Label label= new Label(composite, SWT.NONE);
		label.setText(ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.sort_constructor_choices.label")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gd);

		SourceActionLabelProvider provider= new SourceActionLabelProvider();
		final Combo combo= new Combo(composite, SWT.READ_ONLY);
		for (int i= 0; i < fSuperConstructors.length; i++) {
			combo.add(provider.getText(fSuperConstructors[i]));
		}

		// TODO: Can we be a little more intelligent about guessing the super() ?
		combo.setText(combo.getItem(0));
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				fSuperIndex= combo.getSelectionIndex();
				// Disable omit super checkbox unless default constructor
				fOmitSuperButton.setEnabled(getSuperConstructorChoice().getParameterTypes().length == 0);
				updateOKStatus();
			}
		});

		return composite;
	}

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

	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		GridData gd= null;

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

	protected Composite createInsertPositionCombo(Composite composite) {
		Composite entryComposite= super.createInsertPositionCombo(composite);
		addVisibilityAndModifiersChoices(entryComposite);
		return entryComposite;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.dialogs.SourceActionDialog#createLinkControl(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createLinkControl(Composite composite) {
		final Control control= createLinkText(composite, new Object[] { JavaUIMessages.getString("GenerateConstructorDialog.link.text.before"), new String[] { JavaUIMessages.getString("GenerateConstructorDialog.link.text.middle"), "org.eclipse.jdt.ui.preferences.CodeTemplatePreferencePage", "constructorcomment", JavaUIMessages.getString("GenerateConstructorDialog.link.tooltip")}, JavaUIMessages.getString("GenerateConstructorDialog.link.text.after")}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		final GridData data= new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.widthHint= 150; // only expand further if anyone else requires it
		control.setLayoutData(data);
		return control;
	}

	protected Composite createOmitSuper(Composite composite) {
		Composite omitSuperComposite= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		omitSuperComposite.setLayout(layout);

		fOmitSuperButton= new Button(omitSuperComposite, SWT.CHECK);
		fOmitSuperButton.setText(ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.omit.super")); //$NON-NLS-1$
		fOmitSuperButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		fOmitSuperButton.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				boolean isSelected= (((Button) e.widget).getSelection());
				setOmitSuper(isSelected);
			}
		});
		fOmitSuperButton.setSelection(isOmitSuper());
		// Disable omit super checkbox unless default constructor
		fOmitSuperButton.setEnabled(getSuperConstructorChoice().getParameterTypes().length == 0);
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		fOmitSuperButton.setLayoutData(gd);

		return omitSuperComposite;
	}

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
		fButtonControls[GenerateNewConstructorUsingFieldsAction.UP_INDEX]= createButton(buttonComposite, UP_BUTTON, ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.up_button"), false); //$NON-NLS-1$	
		fButtonControls[GenerateNewConstructorUsingFieldsAction.DOWN_INDEX]= createButton(buttonComposite, DOWN_BUTTON, ActionMessages.getString("GenerateConstructorUsingFieldsSelectionDialog.down_button"), false); //$NON-NLS-1$			
		boolean defaultState= false;
		fButtonControls[GenerateNewConstructorUsingFieldsAction.UP_INDEX].setEnabled(defaultState);
		fButtonControls[GenerateNewConstructorUsingFieldsAction.DOWN_INDEX].setEnabled(defaultState);
		fButtonsEnabled[GenerateNewConstructorUsingFieldsAction.UP_INDEX]= defaultState;
		fButtonsEnabled[GenerateNewConstructorUsingFieldsAction.DOWN_INDEX]= defaultState;
	}

	protected Composite createVisibilityControlAndModifiers(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
		Composite visibilityComposite= createVisibilityControl(parent, visibilityChangeListener, availableVisibilities, correctVisibility);
		return visibilityComposite;
	}

	List getElementList() {
		IStructuredSelection selection= (IStructuredSelection) getTreeViewer().getSelection();
		List elements= selection.toList();
		ArrayList elementList= new ArrayList();

		for (int i= 0; i < elements.size(); i++) {
			elementList.add(elements.get(i));
		}
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