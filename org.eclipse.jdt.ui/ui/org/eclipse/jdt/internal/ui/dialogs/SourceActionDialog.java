/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * An advanced version of CheckedTreeSelectionDialog with right-side button layout and
 * extra buttons and composites.
 */
public abstract class SourceActionDialog extends CheckedTreeSelectionDialog {
	
	private int fInsertionIndex;
	private CompilationUnitEditor fEditor;
	private ITreeContentProvider fContentProvider;
	
	protected ITreeContentProvider getContentProvider() {
		return fContentProvider;
	}

	protected CompilationUnitEditor getEditor() {
		return fEditor;
	}

	public int getInsertionIndex() {
		return fInsertionIndex;
	}

	protected void setContentProvider(ITreeContentProvider provider) {
		fContentProvider = provider;
	}

	protected void setEditor(CompilationUnitEditor editor) {
		fEditor = editor;
	}

	protected void setInsertionIndex(int index) {
		fInsertionIndex = index;
	}

	/**
	 * @param parent
	 * @param labelProvider
	 * @param contentProvider
	 */
	public SourceActionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, CompilationUnitEditor editor) {
		super(parent, labelProvider, contentProvider);
		fEditor= editor;
		fContentProvider= contentProvider;
	}

	protected Composite createSelectionButtons(Composite composite) {
		Composite buttonComposite= super.createSelectionButtons(composite);

		GridLayout layout = new GridLayout();
		buttonComposite.setLayout(layout);						

		layout.marginHeight= 0;
		layout.marginWidth= 0;						
		layout.numColumns= 1;
			
		return buttonComposite;
	}	
	
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case IDialogConstants.OK_ID: {
				okPressed();
				break;
			}
			case IDialogConstants.CANCEL_ID: {
				cancelPressed();
				break;
			}
		}
	}
	
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);
			
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=	convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);			
		composite.setLayout(layout);
		composite.setFont(parent.getFont());	
						
		Label messageLabel = createMessageArea(composite);			
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		messageLabel.setLayoutData(gd);			
			
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout innerLayout = new GridLayout();
		innerLayout.numColumns= 2;
		innerLayout.marginHeight= 0;
		innerLayout.marginWidth= 0;
		inner.setLayout(innerLayout);
		inner.setFont(parent.getFont());		
			
		CheckboxTreeViewer treeViewer= createTreeViewer(inner);
		gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint = convertHeightInCharsToPixels(20);
		treeViewer.getControl().setLayoutData(gd);			
					
		Composite buttonComposite= createSelectionButtons(inner);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		buttonComposite.setLayoutData(gd);
			
		gd= new GridData(GridData.FILL_BOTH);
		inner.setLayoutData(gd);
		
		Composite entryComposite= createEntryPtCombo(composite);
		entryComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		gd= new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);
			
		return composite;
	}				
	
	protected Composite createEntryPtCombo(Composite composite) {

		Composite selectionComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		selectionComposite.setLayout(layout);
		selectionComposite.setFont(composite.getFont());	
						
		addOrderEntryChoices(selectionComposite);	
											
		return selectionComposite;
	}
	
	private Composite addOrderEntryChoices(Composite buttonComposite) {
		Label enterLabel= new Label(buttonComposite, SWT.NONE);
		enterLabel.setText(ActionMessages.getString("GetterSetterTreeSelectionDialog.enterAt_label")); //$NON-NLS-1$
			
		GridData gd= new GridData(GridData.FILL_BOTH);
		enterLabel.setLayoutData(gd);

		final Combo enterCombo= new Combo(buttonComposite, SWT.READ_ONLY);
		fillWithPossibleInsertPositions(enterCombo);
			
		gd= new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace= true;
		enterCombo.setLayoutData(gd);
		enterCombo.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setInsertionIndex(enterCombo.getSelectionIndex());
			}
		});	

		return buttonComposite;			
	}

	private void fillWithPossibleInsertPositions(Combo combo) {
		try {
			int position= 0;
			int presetOffset= 0;
			if (getEditor() != null) {
				presetOffset= ((TextSelection)getEditor().getSelectionProvider().getSelection()).getOffset();
			}
			else {
				List preselected= getInitialElementSelections();	
				int size= preselected.size();
				if ((size > 1) || (size == 0))
					presetOffset= 0;		
				else {				
					IJavaElement element= (IJavaElement) preselected.get(0);
					int type= element.getElementType();
					if (type == IJavaElement.FIELD)
						presetOffset= ((IField)element).getSourceRange().getOffset();									
					else if (type == IJavaElement.FIELD)
						presetOffset= ((IField)element).getSourceRange().getOffset();	
				}
			}
			
			Object allFields[]= getContentProvider().getElements(null);
			IField field= (IField)allFields[0];
			combo.add(ActionMessages.getString("GetterSetterTreeSelectionDialog.first_method")); //$NON-NLS-1$
				
			IMethod[] methods= field.getDeclaringType().getMethods();
			int bestDiff= Integer.MAX_VALUE;
			
			for (int i= 0; i < methods.length; i++) {
				int currDiff= 0;
				IMethod curr= methods[i];
				combo.add(JavaElementLabels.getElementLabel(methods[i], JavaElementLabels.M_PARAMETER_TYPES));
				// calculate method to pre-select
				currDiff= presetOffset - curr.getSourceRange().getOffset();
				if (currDiff >= 0)
					if(currDiff < bestDiff) {
						bestDiff= currDiff;
						position= i + 1;	// first entry is "as first method"
					}
					else
						break;
			}	
			combo.select(position);
			setInsertionIndex(combo.getSelectionIndex());
		} catch (JavaModelException e) {
		}	
	}
	

}
