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
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.core.dom.Modifier;

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
	
	private List fInsertPositions;
	private List fLabels;
	private int fCurrentPositionIndex;
	
	private IDialogSettings fSettings;
	private CompilationUnitEditor fEditor;
	private ITreeContentProvider fContentProvider;
	private boolean fGenerateComment;
	private IType fType;
	private int fWidth, fHeight;
	private String fCommentString;
		
	private int fVisibilityModifier;
	private boolean fFinal;
	private boolean fSynchronized;
	
	private final String SETTINGS_SECTION_METHODS= "SourceActionDialog.methods"; //$NON-NLS-1$
	private final String SETTINGS_SECTION_CONSTRUCTORS= "SourceActionDialog.constructors"; //$NON-NLS-1$
	
	private final String SETTINGS_INSERTPOSITION= "InsertPosition"; //$NON-NLS-1$
	private final String SETTINGS_VISIBILITY_MODIFIER= "VisibilityModifier"; //$NON-NLS-1$
	private final String SETTINGS_FINAL_MODIFIER= "FinalModifier"; //$NON-NLS-1$
	private final String SETTINGS_SYNCHRONIZED_MODIFIER= "SynchronizedModifier"; //$NON-NLS-1$
	private final String SETTINGS_COMMENTS= "Comments"; //$NON-NLS-1$

	
	public SourceActionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, CompilationUnitEditor editor, IType type, boolean isConstructor) throws JavaModelException {
		super(parent, labelProvider, contentProvider);
		fEditor= editor;
		fContentProvider= contentProvider;		
		fType= type;
		fCommentString= ActionMessages.getString("SourceActionDialog.createMethodComment"); //$NON-NLS-1$

		fWidth= 60;
		fHeight= 18;
			
		int insertionDefault= isConstructor ? 0 : 1;
		boolean generateCommentsDefault= JavaPreferencesSettings.getCodeGenerationSettings().createComments;
		
		IDialogSettings dialogSettings= JavaPlugin.getDefault().getDialogSettings();
		String sectionId= isConstructor ? SETTINGS_SECTION_CONSTRUCTORS : SETTINGS_SECTION_METHODS;
		fSettings= dialogSettings.getSection(sectionId);		
		if (fSettings == null)  {
			fSettings= dialogSettings.addNewSection(sectionId);
		}	
		
		fVisibilityModifier= asInt(fSettings.get(SETTINGS_VISIBILITY_MODIFIER), Modifier.PUBLIC);
		fFinal= asBoolean(fSettings.get(SETTINGS_FINAL_MODIFIER), false);
		fSynchronized= asBoolean(fSettings.get(SETTINGS_SYNCHRONIZED_MODIFIER), false);
		fCurrentPositionIndex= asInt(fSettings.get(SETTINGS_INSERTPOSITION), insertionDefault);
		fGenerateComment= asBoolean(fSettings.get(SETTINGS_COMMENTS), generateCommentsDefault);
		
		fInsertPositions= new ArrayList();
		fLabels= new ArrayList(); 
		
		IJavaElement[] members= fType.getChildren();
		IMethod[] methods= fType.getMethods();
		
		fInsertPositions.add(methods.length > 0 ? methods[0]: null); // first
		fInsertPositions.add(null); // last
		
		fLabels.add(ActionMessages.getString("SourceActionDialog.first_method")); //$NON-NLS-1$
		fLabels.add(ActionMessages.getString("SourceActionDialog.last_method")); //$NON-NLS-1$

		if (hasCursorPositionElement(fEditor, members, fInsertPositions)) {
			fLabels.add(ActionMessages.getString("SourceActionDialog.cursor")); //$NON-NLS-1$
			fCurrentPositionIndex= 2;
		}
		
		for (int i = 0; i < methods.length; i++) {
			IMethod curr= methods[i];
			String methodLabel= JavaElementLabels.getElementLabel(curr, JavaElementLabels.M_PARAMETER_TYPES);
			fLabels.add(ActionMessages.getFormattedString("SourceActionDialog.after", methodLabel)); //$NON-NLS-1$
			fInsertPositions.add(findSibling(curr, members));
		}
		fInsertPositions.add(null);
	}
	
	private boolean asBoolean(String string, boolean defaultValue) {
		if (string != null) {
			return StringConverter.asBoolean(string, defaultValue);
		}
		return defaultValue;
	}

	private int asInt(String string, int defaultValue) {
		if (string != null) {
			return StringConverter.asInt(string, defaultValue);
		}
		return defaultValue;
	}

	private IJavaElement findSibling(IMethod curr, IJavaElement[] members) throws JavaModelException {
		IJavaElement res= null;
		int methodStart= curr.getSourceRange().getOffset();
		for (int i= members.length-1; i >= 0; i--) {
			IMember member= (IMember) members[i];
			if (methodStart < member.getSourceRange().getOffset()) {
				return res;
			}
			res= member;
		}
		return null;
	}

	private boolean hasCursorPositionElement(CompilationUnitEditor editor, IJavaElement[] members, List insertPositions) throws JavaModelException {
		if (editor == null) {
			return false;
		}
		int offset= ((ITextSelection) editor.getSelectionProvider().getSelection()).getOffset();

		for (int i= 0; i < members.length; i++) {
			IMember curr= (IMember) members[i];
			ISourceRange range= curr.getSourceRange();
			if (offset < range.getOffset()) {
				insertPositions.add(curr);
				return true;
			} else if (offset < range.getOffset() + range.getLength()) {
				return false; // in the middle of a member
			}
		}
		insertPositions.add(null);
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#close()
	 */
	public boolean close() {
		fSettings.put(SETTINGS_VISIBILITY_MODIFIER, StringConverter.asString(fVisibilityModifier));
		fSettings.put(SETTINGS_FINAL_MODIFIER, StringConverter.asString(fFinal));
		fSettings.put(SETTINGS_SYNCHRONIZED_MODIFIER, StringConverter.asString(fSynchronized));
		
		if (fCurrentPositionIndex == 0 || fCurrentPositionIndex == 1) {
			fSettings.put(SETTINGS_INSERTPOSITION, StringConverter.asString(fCurrentPositionIndex));
		}
		fSettings.put(SETTINGS_COMMENTS, fGenerateComment);
		return super.close();
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

	
	/***
	 * Set insert position valid input is 0 for the first position, 1 for the last position, > 1 for all else.
	 */
	private void setInsertPosition(int insert) {
		fCurrentPositionIndex= insert;
		fSettings.put(SETTINGS_INSERTPOSITION, insert);
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
		
	private void setVisibility(int visibility) {
		fVisibilityModifier= visibility;
	}
	
	private void setFinal(boolean value) {
		fFinal= value;
	}
		
	private void setSynchronized(boolean value)  {
		fSynchronized= value;
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
		gd.widthHint= convertWidthInCharsToPixels(fWidth);
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
		combo.setItems((String[]) fLabels.toArray(new String[fLabels.size()]));
		combo.select(fCurrentPositionIndex);
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
		return (IJavaElement) fInsertPositions.get(fCurrentPositionIndex);	
	}

}
