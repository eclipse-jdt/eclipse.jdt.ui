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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
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
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.IVisibilityChangeListener;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * An advanced version of CheckedTreeSelectionDialog with right-side button layout and
 * extra buttons and composites.
 */
public class SourceActionDialog extends CheckedTreeSelectionDialog {
	private int fInsertPosition;
	private IDialogSettings fSettings;
	private CompilationUnitEditor fEditor;
	private ITreeContentProvider fContentProvider;
	private boolean fGenerateComment;
	private IType fType;
	private int fWidth = 60;
	private int fHeight = 18;
	private String fCommentString;
	private int fVisibilityModifier;
	private boolean fFinal;
	private boolean fSynchronized;
	
	private final String SETTINGS_SECTION= "SourceActionDialog"; //$NON-NLS-1$
	public final String SETTINGS_INSERTPOSITION= "InsertPosition"; //$NON-NLS-1$
	private final String VISIBILITY_MODIFIER= "VisibilityModifier"; //$NON-NLS-1$
	private final String FINAL_MODIFIER= "FinalModifier"; //$NON-NLS-1$
	private final String SYNCHRONIZED_MODIFIER= "SynchronizedModifier"; //$NON-NLS-1$

	public SourceActionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, CompilationUnitEditor editor, IType type) {
		super(parent, labelProvider, contentProvider);
		fEditor= editor;
		fContentProvider= contentProvider;		
		fType= type;
		fCommentString= ActionMessages.getString("SourceActionDialog.createMethodComment"); //$NON-NLS-1$
		
		// Take the default from the default for generating comments from the code gen prefs
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		fGenerateComment= settings.createComments;		

		IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
		fSettings= dialogSettings.getSection(SETTINGS_SECTION);		
		if (fSettings == null)  {
			fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
			fSettings.put(VISIBILITY_MODIFIER, Modifier.PUBLIC); //$NON-NLS-1$
			fSettings.put(FINAL_MODIFIER, false); //$NON-NLS-1$
			fSettings.put(SYNCHRONIZED_MODIFIER, false); //$NON-NLS-1$
			fSettings.put(SETTINGS_INSERTPOSITION, 1); //$NON-NLS-1$
		}

		try {
			fVisibilityModifier= fSettings.getInt(VISIBILITY_MODIFIER);
			fFinal= fSettings.getBoolean(FINAL_MODIFIER);
			fSynchronized= fSettings.getBoolean(SYNCHRONIZED_MODIFIER);
			fInsertPosition= fSettings.getInt(SETTINGS_INSERTPOSITION);
		} catch (NumberFormatException e) {
			fSettings= dialogSettings.addNewSection(SETTINGS_SECTION);
			fSettings.put(VISIBILITY_MODIFIER, Modifier.PUBLIC); //$NON-NLS-1$
			fSettings.put(FINAL_MODIFIER, false); //$NON-NLS-1$
			fSettings.put(SYNCHRONIZED_MODIFIER, false); //$NON-NLS-1$
			fSettings.put(SETTINGS_INSERTPOSITION, 1); //$NON-NLS-1$			
		}
	}

	/***
	 * Returns 0 for the first method, 1 for the last method, > 1  for all else.
	 */
	public int getInsertPosition() {
		return fInsertPosition;
	}
	
	/***
	 * Set insert position valid input is 0 for the first position, 1 for the last position, > 1 for all else.
	 */
	public void setInsertPosition(int insert) {
		if (fInsertPosition != insert) {
			fInsertPosition= insert;
			fSettings.put(SETTINGS_INSERTPOSITION, insert);
		}
	}	
	
	public void setCommentString(String string) {
		fCommentString= string;
	}
		
	protected ITreeContentProvider getContentProvider() {
		return fContentProvider;
	}

	public boolean getGenerateComment() {
		return fGenerateComment;
	}
	
	public int getVisibilityModifier() {
		return fVisibilityModifier;
	}
	
	public void setGenerateComment(boolean comment) {
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
	
	public void setVisibility(int visibility) {
		if (fVisibilityModifier != visibility) {
			fVisibilityModifier= visibility;
			fSettings.put(VISIBILITY_MODIFIER, visibility);
		}
	}
	
	public void setFinal(boolean value) {
		if (fFinal != value)  {
			fFinal= value;
			fSettings.put(FINAL_MODIFIER, value);
		}
	}
		
	public void setSynchronized(boolean value)  {
		if (fSynchronized != value)  {
			fSynchronized= value;
			fSettings.put(SYNCHRONIZED_MODIFIER, value);
		}
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
			
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		GridData gd= null;
		
		layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing=	convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);			
		composite.setLayout(layout);
						
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
		entryComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Composite commentComposite= createCommentSelection(composite);
		commentComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		

		gd= new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);
		
		applyDialogFont(composite);
					
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

	protected Composite addVisibilityAndModifiersChoices(Composite buttonComposite) {
		// Add visibility and modifiers buttons: http://bugs.eclipse.org/bugs/show_bug.cgi?id=35870
		// Add persistence of options: http://bugs.eclipse.org/bugs/show_bug.cgi?id=38400
		IVisibilityChangeListener visibilityChangeListener= new IVisibilityChangeListener(){
			public void visibilityChanged(int newVisibility) {
				setVisibility(newVisibility);
			}
			public void modifierChanged(int modifier, boolean isChecked) {	
				switch (modifier) {
					case Modifier.FINAL:  {
						setFinal(isChecked);
						return; 
					}
					case Modifier.SYNCHRONIZED:  {
						setSynchronized(isChecked);
						return;
					}
					default: return;
				}
			}
		};
			
		int initialVisibility= getVisibilityModifier();
		int[] availableVisibilities= new int[]{Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.NONE};
			
		Composite visibilityComposite= createVisibilityControlAndModifiers(buttonComposite, visibilityChangeListener, availableVisibilities, initialVisibility);
		return visibilityComposite;				
	}
	
	private List convertToIntegerList(int[] array) {
		List result= new ArrayList(array.length);
		for (int i= 0; i < array.length; i++) {
			result.add(new Integer(array[i]));
		}
		return result;
	}	
	
	protected Composite createVisibilityControl(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
		List allowedVisibilities= convertToIntegerList(availableVisibilities);
		if (allowedVisibilities.size() == 1)
			return null;
		
		Group group= new Group(parent, SWT.NONE);
		group.setText(RefactoringMessages.getString("VisibilityControlUtil.Access_modifier")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.FILL_BOTH);
		group.setLayoutData(gd);
		GridLayout layout= new GridLayout();
		layout.makeColumnsEqualWidth= true;
		layout.numColumns= 4; 
		group.setLayout(layout);
		
		String[] labels= new String[] {
			"&public", //$NON-NLS-1$
			"pro&tected", //$NON-NLS-1$
			RefactoringMessages.getString("VisibilityControlUtil.defa&ult_4"), //$NON-NLS-1$
			"pri&vate" //$NON-NLS-1$
		};
		Integer[] data= new Integer[] {
					new Integer(Modifier.PUBLIC),
					new Integer(Modifier.PROTECTED),
					new Integer(Modifier.NONE),
					new Integer(Modifier.PRIVATE)};
		Integer initialVisibility= new Integer(correctVisibility);
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			Integer visibilityCode= data[i];
			radio.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			radio.setText(labels[i]);
			radio.setData(visibilityCode);
			radio.setSelection(visibilityCode.equals(initialVisibility));
			radio.setEnabled(allowedVisibilities.contains(visibilityCode));
			radio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					visibilityChangeListener.visibilityChanged(((Integer)event.widget.getData()).intValue());
				}
			});
		}
		return group;
	}
	
	protected Composite createVisibilityControlAndModifiers(Composite parent, final IVisibilityChangeListener visibilityChangeListener, int[] availableVisibilities, int correctVisibility) {
		Composite visibilityComposite= createVisibilityControl(parent, visibilityChangeListener, availableVisibilities, correctVisibility);

		Button finalCheckboxButton= new Button(visibilityComposite, SWT.CHECK);
		finalCheckboxButton.setText(RefactoringMessages.getString("VisibilityControlUtil.final")); //$NON-NLS-1$
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		finalCheckboxButton.setLayoutData(gd);
		finalCheckboxButton.setData(new Integer(Modifier.FINAL));
		finalCheckboxButton.setEnabled(true);
		finalCheckboxButton.setSelection(isFinal());
		finalCheckboxButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				visibilityChangeListener.modifierChanged(((Integer)event.widget.getData()).intValue(), ((Button) event.widget).getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				widgetSelected(event);
			}
		});	
			
		Button syncCheckboxButton= new Button(visibilityComposite, SWT.CHECK);
		syncCheckboxButton.setText(RefactoringMessages.getString("VisibilityControlUtil.synchronized")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		syncCheckboxButton.setLayoutData(gd);
		syncCheckboxButton.setData(new Integer(Modifier.SYNCHRONIZED));
		syncCheckboxButton.setEnabled(true);
		syncCheckboxButton.setSelection(isSynchronized());
		syncCheckboxButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				visibilityChangeListener.modifierChanged(((Integer)event.widget.getData()).intValue(), ((Button) event.widget).getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				widgetSelected(event);
			}
		});	
		return visibilityComposite;			
	}	
			
	protected Composite createEntryPtCombo(Composite composite) {
		Composite selectionComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		selectionComposite.setLayout(layout);
					
		addOrderEntryChoices(selectionComposite);	
										
		return selectionComposite;
	}
	
	private Composite addOrderEntryChoices(Composite buttonComposite) {
		Label enterLabel= new Label(buttonComposite, SWT.NONE);
		enterLabel.setText(ActionMessages.getString("SourceActionDialog.enterAt_label")); //$NON-NLS-1$
			
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		enterLabel.setLayoutData(gd);

		final Combo enterCombo= new Combo(buttonComposite, SWT.READ_ONLY);
		fillWithPossibleInsertPositions(enterCombo);
			
		gd= new GridData(GridData.FILL_BOTH);
		enterCombo.setLayoutData(gd);
		enterCombo.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				int index= enterCombo.getSelectionIndex();
				// Add persistence only if first or last method: http://bugs.eclipse.org/bugs/show_bug.cgi?id=38400				
				setInsertPosition(index);
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
			combo.add(ActionMessages.getString("SourceActionDialog.last_method")); //$NON-NLS-1$
							
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
						position= i + 2;	// first two entries are first/last
					}
					else
						break;
			}	
			// Add persistence only if first or last method: http://bugs.eclipse.org/bugs/show_bug.cgi?id=38400
			int index= getInsertPosition();
			if (index > 1)
				combo.select(position);
			else			
				combo.select(index);

			setInsertPosition(combo.getSelectionIndex());
		} catch (JavaModelException e) {
		}	
	}
	
	public boolean getFinal() {
		return fFinal;
	}		
	
	public boolean getSynchronized() {
		return fSynchronized;
	}	
	
	public boolean isFinal() {
		return fFinal;
	}

	public boolean isSynchronized() {
		return fSynchronized;
	}	
	
	/*
	 * Determine where in the file to enter the newly created methods.
	 */
	public IJavaElement getElementPosition() {
		int comboBoxIndex= getInsertPosition();		
		
		try {
			if (comboBoxIndex == 0)				// as first method
				return asFirstMethod(fType);
			else if (comboBoxIndex == 1)		// as last method
				return null;
			else								// method position
				return atElementPosition(fType, comboBoxIndex);
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

	/* Returns the element directly following the method to insert after. Index should never 
	 * always be > 2 since 0 means first method, and 1 means last method.
	 */
	private IJavaElement atElementPosition(IType type, int index) throws JavaModelException {
		if (type != null) {
			IMethod[] methods= type.getMethods();			
			IJavaElement[] elements= type.getChildren();
			for (int i= 0; i < (elements.length-1); i++) {
				if (methods[index-2] == elements[i])			// first two entries are first/last
					return elements[i+1];
			}
		}
		return null;
	}
}
