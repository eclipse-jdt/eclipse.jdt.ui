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
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility.GenStubSettings;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * An advanced version of CheckedTreeSelectionDialog with right-side button layout and
 * extra buttons and composites.
 */
public class SourceActionDialog extends CheckedTreeSelectionDialog {
	
	private int fInsertionIndex;
	private CompilationUnitEditor fEditor;
	private ITreeContentProvider fContentProvider;
	private boolean fGenerateComment;
	private IType fType;
	private int fWidth = 60;
	private int fHeight = 18;
	private String fCommentString;

	public SourceActionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, CompilationUnitEditor editor, IType type) {
		super(parent, labelProvider, contentProvider);
		fEditor= editor;
		fContentProvider= contentProvider;		
		fType= type;
		fCommentString= ActionMessages.getString("SourceActionDialog.createMethodComment"); //$NON-NLS-1$
		
		// Take the default from the default for generating comments from the code gen prefs
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		fGenerateComment= genStubSettings.createComments;		
	}

	public void setCommentString(String string) {
		fCommentString= string;
	}
		
	protected ITreeContentProvider getContentProvider() {
		return fContentProvider;
	}

	public int getInsertionIndex() {
		return fInsertionIndex;
	}

	private void setInsertionIndex(int index) {
		fInsertionIndex = index;
	}
	
	public boolean getGenerateComment() {
		return fGenerateComment;
	}
	
	private void setGenerateComment(boolean comment) {
		fGenerateComment= comment;
	}
	
	/**
	 * Sets the size of the tree in unit of characters.
	 * @param width  the width of the tree.
	 * @param height the height of the tree.
	 */
	public void setSize(int width, int height) {
		fWidth = width;
		fHeight = height;
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
	
	/**
	 * Returns a composite containing the label created at the top of the dialog. Returns null if there is the
	 * message for the label is null.
	 */
	protected Label createMessageArea(Composite composite) {
		if (getMessage() != null) {
			Label label = new Label(composite,SWT.NONE);
			label.setText(getMessage());
			label.setFont(composite.getFont());
			return label;
		} 
		return null;
	}
	
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		initializeDialogUnits(parent);
			
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		GridData gd= null;
		
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=	convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);			
		composite.setLayout(layout);
		composite.setFont(parent.getFont());	
						
		Label messageLabel = createMessageArea(composite);			
		if (messageLabel != null) {
			gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan= 2;
			messageLabel.setLayoutData(gd);	
		}
			
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout innerLayout = new GridLayout();
		innerLayout.numColumns= 2;
		innerLayout.marginHeight= 0;
		innerLayout.marginWidth= 0;
		inner.setLayout(innerLayout);
		inner.setFont(parent.getFont());		
			
		CheckboxTreeViewer treeViewer= createTreeViewer(inner);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint = convertWidthInCharsToPixels(fWidth);
		gd.heightHint = convertHeightInCharsToPixels(fHeight);
		treeViewer.getControl().setLayoutData(gd);			
					
		Composite buttonComposite= createSelectionButtons(inner);
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		buttonComposite.setLayoutData(gd);
			
		gd= new GridData(GridData.FILL_BOTH);
		inner.setLayoutData(gd);
		
		Composite entryComposite= createEntryPtCombo(composite);
		entryComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		Composite commentComposite= createCommentSelection(composite);
		commentComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));		

		gd= new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);
			
		return composite;
	}				
	
	protected Composite createCommentSelection(Composite composite) {
		Composite commentComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		commentComposite.setLayout(layout);
		commentComposite.setFont(composite.getFont());	
		
		Button commentButton= new Button(commentComposite, SWT.CHECK);
		commentButton.setText(fCommentString); //$NON-NLS-1$
		commentButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		commentButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected= (((Button) e.widget).getSelection());
				setGenerateComment(isSelected);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		commentButton.setSelection(getGenerateComment());
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		commentButton.setLayoutData(gd);
		
		return commentComposite;
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
		enterLabel.setText(ActionMessages.getString("SourceActionDialog.enterAt_label")); //$NON-NLS-1$
			
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
			if (fEditor != null) {
				presetOffset= ((TextSelection) fEditor.getSelectionProvider().getSelection()).getOffset();
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
					else if (type == IJavaElement.METHOD)
						presetOffset= ((IMethod)element).getSourceRange().getOffset();	
				}
			}
			
			IMethod[] methods= fType.getMethods();

			combo.add(ActionMessages.getString("SourceActionDialog.first_method")); //$NON-NLS-1$
				
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
	
	/*
	 * Determine where in the file to enter the newly created methods.
	 */
	public IJavaElement getElementPosition() {
		int comboBoxIndex= getInsertionIndex();		
		
		try {
			if (comboBoxIndex == 0)				// as first method
				return asFirstMethod(fType);
			else								// method position
				return atMethodPosition(fType, comboBoxIndex);
		} catch (JavaModelException e) {
			return null;
		}			
	}
	
	private IMethod asFirstMethod(IType type) throws JavaModelException {
		if (type != null) {
			IMethod[] methods= type.getMethods();
			if (methods.length > 0) {
				return methods[0];		
			}
		}
		return null;
	}
	
	private IMethod atMethodPosition(IType type, int index) throws JavaModelException {
		if (type != null) {
			IMethod[] methods= type.getMethods();
			if (index < methods.length) {
				return methods[index];
			}
		}
		return null;
	}



}
