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
package org.eclipse.jdt.internal.ui.refactoring.code;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.Document;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ChangeParametersControl;
import org.eclipse.jdt.internal.ui.refactoring.IParameterListChangeListener;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class ExtractMethodInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ExtractMethodInputPage";//$NON-NLS-1$

	private ExtractMethodRefactoring fRefactoring;
	private Text fTextField;
	private boolean fFirstTime;
	private JavaSourceViewer fSignaturePreview;
	private Document fSignaturePreviewDocument;
	private IDialogSettings fSettings;
	
	private static final String DESCRIPTION = RefactoringMessages.getString("ExtractMethodInputPage.description");//$NON-NLS-1$
	private static final String THROW_RUNTIME_EXCEPTIONS= "ThrowRuntimeExceptions"; //$NON-NLS-1$
	private static final String GENERATE_JAVADOC= "GenerateJavadoc";  //$NON-NLS-1$

	public ExtractMethodInputPage() {
		super(PAGE_NAME);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
		setDescription(DESCRIPTION);
		fFirstTime= true;
		fSignaturePreviewDocument= new Document();
	}

	public void createControl(Composite parent) {
		fRefactoring= (ExtractMethodRefactoring)getRefactoring();
		loadSettings();
		
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		RowLayouter layouter= new RowLayouter(2);
		GridData gd= null;
		
		initializeDialogUnits(result);
		
		Label label= new Label(result, SWT.NONE);
		label.setText(getLabelText());
		
		fTextField= createTextInputField(result, SWT.BORDER);
		fTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		layouter.perform(label, fTextField, 1);
		
		ASTNode[] destinations= fRefactoring.getDestinations();
		if (destinations.length > 1) {
			label= new Label(result, SWT.NONE);
			label.setText(RefactoringMessages.getString("ExtractMethodInputPage.destination_type")); //$NON-NLS-1$
			final Combo combo= new Combo(result, SWT.READ_ONLY | SWT.DROP_DOWN);
			for (int i= 0; i < destinations.length; i++) {
				ASTNode declaration= destinations[i];
				combo.add(getLabel(declaration));
			}
			combo.select(0);
			combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			combo.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setDestination(combo.getSelectionIndex());
				}
				public void widgetDefaultSelected(SelectionEvent e) {
					// nothing
				}
			});
		}
		
		label= new Label(result, SWT.NONE);
		label.setText(RefactoringMessages.getString("ExtractMethodInputPage.access_Modifiers")); //$NON-NLS-1$
		
		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 4; layout.marginWidth= 0;
		group.setLayout(layout);
		
		String[] labels= new String[] {
			RefactoringMessages.getString("ExtractMethodInputPage.public"),  //$NON-NLS-1$
			RefactoringMessages.getString("ExtractMethodInputPage.protected"), //$NON-NLS-1$
			RefactoringMessages.getString("ExtractMethodInputPage.default"), //$NON-NLS-1$
			RefactoringMessages.getString("ExtractMethodInputPage.private") //$NON-NLS-1$
		};
		Integer[] data= new Integer[] {new Integer(Modifier.PUBLIC), new Integer(Modifier.PROTECTED), new Integer(Modifier.NONE), new Integer(Modifier.PRIVATE)};
		Integer visibility= new Integer(fRefactoring.getVisibility());
		for (int i= 0; i < labels.length; i++) {
			Button radio= new Button(group, SWT.RADIO);
			radio.setText(labels[i]);
			radio.setData(data[i]);
			if (data[i].equals(visibility))
				radio.setSelection(true);
			radio.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					setVisibility((Integer)event.widget.getData());
				}
			});
		}
		layouter.perform(label, group, 1);
		
		if (!fRefactoring.getParameterInfos().isEmpty()) {
			ChangeParametersControl cp= new ChangeParametersControl(result, SWT.NONE, 
				RefactoringMessages.getString("ExtractMethodInputPage.parameters"), //$NON-NLS-1$
				new IParameterListChangeListener() {
				public void parameterChanged(ParameterInfo parameter) {
					parameterModified();
				}
				public void parameterListChanged() {
					updatePreview(getText());
				}
				public void parameterAdded(ParameterInfo parameter) {
					updatePreview(getText());
				}
			}, true, false, false);
			gd= new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan= 2;
			cp.setLayoutData(gd);
			cp.setInput(fRefactoring.getParameterInfos());
		}
		
		Button checkBox= new Button(result, SWT.CHECK);
		checkBox.setText(RefactoringMessages.getString("ExtractMethodInputPage.throwRuntimeExceptions")); //$NON-NLS-1$
		checkBox.setSelection(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setRethrowRuntimeException(((Button)e.widget).getSelection());
			}
		});
		layouter.perform(checkBox);
		
		checkBox= new Button(result, SWT.CHECK);
		checkBox.setText(RefactoringMessages.getString("ExtractMethodInputPage.generateJavadocComment")); //$NON-NLS-1$
		boolean generate= computeGenerateJavadoc();
		setGenerateJavadoc(generate);
		checkBox.setSelection(generate);
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setGenerateJavadoc(((Button)e.widget).getSelection());
			}
		});
		layouter.perform(checkBox);
		
		int duplicates= fRefactoring.getNumberOfDuplicates();
		checkBox= new Button(result, SWT.CHECK);
		if (duplicates == 0) {
			checkBox.setText(RefactoringMessages.getString("ExtractMethodInputPage.duplicates.none")); //$NON-NLS-1$
		} else  if (duplicates == 1) {
			checkBox.setText(RefactoringMessages.getString("ExtractMethodInputPage.duplicates.single")); //$NON-NLS-1$
		} else {
			checkBox.setText(RefactoringMessages.getFormattedString(
				"ExtractMethodInputPage.duplicates.multi", //$NON-NLS-1$
				new Integer(duplicates))); 
		}
		checkBox.setSelection(fRefactoring.getReplaceDuplicates());
		checkBox.setEnabled(duplicates > 0);
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fRefactoring.setReplaceDuplicates(((Button)e.widget).getSelection());
			}
		});
		layouter.perform(checkBox);
		
		label= new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layouter.perform(label);
		
		createSignaturePreview(result, layouter);
		
		Dialog.applyDialogFont(result);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.EXTRACT_METHOD_WIZARD_PAGE);		
	}
	
	private String getLabel(ASTNode node) {
		if (node instanceof AbstractTypeDeclaration) {
			return ((AbstractTypeDeclaration)node).getName().getIdentifier();
		} else {
			ClassInstanceCreation creation= (ClassInstanceCreation)ASTNodes.getParent(node, ClassInstanceCreation.class);
			return RefactoringMessages.getFormattedString(
				"ExtractMethodInputPage.anonymous_type_label",  //$NON-NLS-1$
				ASTNodes.asString(creation.getType()));
		}
	}

	private Text createTextInputField(Composite parent, int style) {
		Text result= new Text(parent, style);
		result.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				textModified(getText());
			}
		});
		return result;
	}
	
	private String getText() {
		if (fTextField == null)
			return null;
		return fTextField.getText();	
	}
	
	private String getLabelText(){
		return RefactoringMessages.getString("ExtractMethodInputPage.label_text"); //$NON-NLS-1$
	}
	
	private void setVisibility(Integer visibility) {
		fRefactoring.setVisibility(visibility.intValue());
		updatePreview(getText());
	}
	
	private void setRethrowRuntimeException(boolean value) {
		fSettings.put(THROW_RUNTIME_EXCEPTIONS, value);
		fRefactoring.setThrowRuntimeExceptions(value);
		updatePreview(getText());
	}
	
	private boolean computeGenerateJavadoc() {
		boolean result= fRefactoring.getGenerateJavadoc();
		if (result)
			return result;
		return fSettings.getBoolean(GENERATE_JAVADOC);
	}
	
	private void setGenerateJavadoc(boolean value) {
		fSettings.put(GENERATE_JAVADOC, value);
		fRefactoring.setGenerateJavadoc(value);
	}
	
	private void createSignaturePreview(Composite composite, RowLayouter layouter) {
		//XXX: same as in ChangeSignatureInputPage
		
		Label previewLabel= new Label(composite, SWT.NONE);
		previewLabel.setText(RefactoringMessages.getString("ExtractMethodInputPage.signature_preview")); //$NON-NLS-1$
		layouter.perform(previewLabel);
		
//		//XXX: use ViewForm to draw a flat border. Beware of common problems with wrapping layouts
//		//inside GridLayout. GridData must be constrained to force wrapping. See bug 9866 et al.
//		ViewForm border= new ViewForm(composite, SWT.BORDER | SWT.FLAT);
		
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		fSignaturePreview= new JavaSourceViewer(composite, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP /*| SWT.BORDER*/, store);
		fSignaturePreview.configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
		fSignaturePreview.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		fSignaturePreview.getTextWidget().setBackground(composite.getBackground());
		fSignaturePreview.setDocument(fSignaturePreviewDocument);
		fSignaturePreview.setEditable(false);
		
		//Layouting problems with wrapped text: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=9866
		Control signaturePreviewControl= fSignaturePreview.getControl();
		PixelConverter pixelConverter= new PixelConverter(signaturePreviewControl);
		GridData gdata= new GridData(GridData.FILL_BOTH);
		gdata.widthHint= pixelConverter.convertWidthInCharsToPixels(50);
		gdata.heightHint= pixelConverter.convertHeightInCharsToPixels(2);
		signaturePreviewControl.setLayoutData(gdata);
		layouter.perform(signaturePreviewControl);
		
//		//XXX must force JavaSourceViewer text widget to wrap:
//		border.setContent(signaturePreviewControl);
//		GridData borderData= new GridData(GridData.FILL_BOTH);
//		borderData.widthHint= gdata.widthHint;
//		borderData.heightHint= gdata.heightHint;
//		border.setLayoutData(borderData);
	}
	
	private void updatePreview(String text) {
		if (fSignaturePreview == null)
			return;
			
		if (text.length() == 0)
			text= "someMethodName";			 //$NON-NLS-1$
		
		int top= fSignaturePreview.getTextWidget().getTopPixel();
		String signature;
		try {
			//TODO: use robust signature composer like in ChangeSignatureRefactoring
			signature= fRefactoring.getSignature(text);
		} catch (IllegalArgumentException e) { 
			signature= ""; //$NON-NLS-1$ 
		}
		fSignaturePreviewDocument.set(signature);
		fSignaturePreview.getTextWidget().setTopPixel(top);
	}
	
	private void loadSettings() {
		fSettings= getDialogSettings().getSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
		if (fSettings == null) {
			fSettings= getDialogSettings().addNewSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
			fSettings.put(THROW_RUNTIME_EXCEPTIONS, false);
			fSettings.put(GENERATE_JAVADOC, JavaPreferencesSettings.getCodeGenerationSettings().createComments);
		}
		fRefactoring.setThrowRuntimeExceptions(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
	}
	
	//---- Input validation ------------------------------------------------------
	
	public void setVisible(boolean visible) {
		if (visible) {
			if (fFirstTime) {
				fFirstTime= false;
				setPageComplete(false);
				updatePreview(getText());
				fTextField.setFocus();
			} else {
				setPageComplete(validatePage(true));
			}
		}
		super.setVisible(visible);
	}
	
	private void textModified(String text) {
		fRefactoring.setMethodName(text);
		RefactoringStatus status= validatePage(true);
		if (!status.hasFatalError()) {
			updatePreview(text);
		} else {
			fSignaturePreviewDocument.set(""); //$NON-NLS-1$
		}
		setPageComplete(status);
	}
	
	private void parameterModified() {
		updatePreview(getText());
		setPageComplete(validatePage(false));
	}
	
	private RefactoringStatus validatePage(boolean text) {
		RefactoringStatus result= new RefactoringStatus();
		if (text) {
			result.merge(validateMethodName());
			result.merge(validateParameters());
		} else {
			result.merge(validateParameters());
			result.merge(validateMethodName());
		}
		return result;
	}
	
	private RefactoringStatus validateMethodName() {
		RefactoringStatus result= new RefactoringStatus();
		String text= getText();
		if ("".equals(text)) { //$NON-NLS-1$
			result.addFatalError(RefactoringMessages.getString("ExtractMethodInputPage.validation.emptyMethodName")); //$NON-NLS-1$
			return result;
		}
		result.merge(fRefactoring.checkMethodName());
		return result;
	}
	
	private RefactoringStatus validateParameters() {
		RefactoringStatus result= new RefactoringStatus();
		List parameters= fRefactoring.getParameterInfos();
		for (Iterator iter= parameters.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if ("".equals(info.getNewName())) { //$NON-NLS-1$
				result.addFatalError(RefactoringMessages.getString("ExtractMethodInputPage.validation.emptyParameterName")); //$NON-NLS-1$
				return result;
			}
		}
		result.merge(fRefactoring.checkParameterNames());
		return result;
	}
}
