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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

public class ChangeSignatureWizard extends RefactoringWizard {

	public ChangeSignatureWizard(ChangeSignatureRefactoring ref) {
		super(ref, RefactoringMessages.getString("ChangeSignatureRefactoring.modify_Parameters")); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ChangeSignatureInputPage());
	}
	
	private static class ChangeSignatureInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "ChangeSignatureInputPage"; //$NON-NLS-1$
		private JavaSourceViewer fSignaturePreview;
		private Document fSignaturePreviewDocument;
		
		public ChangeSignatureInputPage() {
			super(PAGE_NAME, true);
			setMessage(RefactoringMessages.getString("ChangeSignatureInputPage.change")); //$NON-NLS-1$
			fSignaturePreviewDocument= new Document();
		}
	
		/*
		 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayout((new GridLayout()));
			initializeDialogUnits(composite);
		
			try {
				int[] availableVisibilities= getChangeMethodSignatureRefactoring().getAvailableVisibilities();
				int currectVisibility= getChangeMethodSignatureRefactoring().getVisibility();
				IVisibilityChangeListener visibilityChangeListener= new IVisibilityChangeListener(){
					public void visibilityChanged(int newVisibility) {
						getChangeMethodSignatureRefactoring().setVisibility(newVisibility);
						update(true);
					}

					public void modifierChanged(int modifier, boolean isChecked) {
					}
				};

				Composite visibilityComposite= VisibilityControlUtil.createVisibilityControl(composite, visibilityChangeListener, availableVisibilities, currectVisibility);
				if (visibilityComposite != null)
					visibilityComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				if ( getChangeMethodSignatureRefactoring().canChangeReturnType())
					createReturnTypeControl(composite);
				createParameterExceptionsFolder(composite);
				Label sep= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
				sep.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
				createSignaturePreview(composite);
				
				update(false);
				setControl(composite);
				Dialog.applyDialogFont(composite);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, RefactoringMessages.getString("ChangeSignatureInputPage.Change_Signature"), RefactoringMessages.getString("ChangeSignatureInputPage.Internal_Error")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.MODIFY_PARAMETERS_WIZARD_PAGE);
		}

		private void createReturnTypeControl(Composite parent) throws JavaModelException {
				Composite composite= new Composite(parent, SWT.NONE);
				composite.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
				GridLayout layout= new GridLayout();
				layout.numColumns= 2; layout.marginWidth= 0;
				composite.setLayout(layout);
			
				Label label= new Label(composite, SWT.NONE);
				label.setText(RefactoringMessages.getString("ChangeSignatureInputPage.return_type")); //$NON-NLS-1$
				label.setLayoutData((new GridData()));
			
				final Text text= new Text(composite, SWT.BORDER);
				text.setText(getChangeMethodSignatureRefactoring().getReturnTypeString());
				text.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
			
				text.addModifyListener(new ModifyListener(){
					public void modifyText(ModifyEvent e) {
						getChangeMethodSignatureRefactoring().setNewReturnTypeName(text.getText());
						update(true);
					}
				});
		}

		private void createParameterExceptionsFolder(Composite composite) {
			TabFolder folder= new TabFolder(composite, SWT.TOP);
			folder.setLayoutData(new GridData(GridData.FILL_BOTH));
			
			TabItem item= new TabItem(folder, SWT.NONE);
			item.setText(RefactoringMessages.getString("ChangeSignatureInputPage.parameters")); //$NON-NLS-1$
			item.setControl(createParameterTableControl(folder));
			
			TabItem itemEx= new TabItem(folder, SWT.NONE);
			itemEx.setText(RefactoringMessages.getString("ChangeSignatureInputPage.exceptions")); //$NON-NLS-1$
			itemEx.setControl(createExceptionsTableControl(folder));

			folder.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					((TabItem) e.item).getControl().setFocus();
				}
			});
		}
	
		private Control createParameterTableControl(Composite composite) {
			String labelText= null; //no label
			ChangeParametersControl cp= new ChangeParametersControl(composite, SWT.NONE, labelText, new IParameterListChangeListener() {
				public void parameterChanged(ParameterInfo parameter) {
					update(true);
				}
				public void parameterListChanged() {
					update(true);
				}
				public void parameterAdded(ParameterInfo parameter) {
					update(true);
				}
			}, true, true, true);
			cp.setLayoutData(new GridData(GridData.FILL_BOTH));
			cp.setInput(getChangeMethodSignatureRefactoring().getParameterInfos());
			return cp;
		}
		
		private Control createExceptionsTableControl(Composite parent) {
			ChangeExceptionsControl cp= new ChangeExceptionsControl(parent, SWT.NONE, new IExceptionListChangeListener() {
				public void exceptionListChanged() {
					update(true);
				}
			}, getChangeMethodSignatureRefactoring().getMethod().getJavaProject());
			cp.setLayoutData(new GridData(GridData.FILL_BOTH));
			cp.setInput(getChangeMethodSignatureRefactoring().getExceptionInfos());
			return cp;
		}
		
		private void createSignaturePreview(Composite composite) {
			Label previewLabel= new Label(composite, SWT.NONE);
			previewLabel.setText(RefactoringMessages.getString("ChangeSignatureInputPage.method_Signature_Preview")); //$NON-NLS-1$
			
//			//XXX: use ViewForm to draw a flat border. Beware of common problems with wrapping layouts
//			//inside GridLayout. GridData must be constrained to force wrapping. See bug 9866 et al.
//			ViewForm border= new ViewForm(composite, SWT.BORDER | SWT.FLAT);
			
			fSignaturePreview= new JavaSourceViewer(composite, null, null, false, SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP /*| SWT.BORDER*/);
			fSignaturePreview.configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools(), null));
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
			
//			//XXX must force JavaSourceViewer text widget to wrap:
//			border.setContent(signaturePreviewControl);
//			GridData borderData= new GridData(GridData.FILL_BOTH);
//			borderData.widthHint= gdata.widthHint;
//			borderData.heightHint= gdata.heightHint;
//			border.setLayoutData(borderData);
		}

		private ChangeSignatureRefactoring getChangeMethodSignatureRefactoring(){
			return	(ChangeSignatureRefactoring)getRefactoring();
		}

		private void update(boolean displayErrorMessage){
			updateStatus(displayErrorMessage);
			updateSignaturePreview();
		}

		private void updateStatus(boolean displayErrorMessage) {
			try{
				if (getChangeMethodSignatureRefactoring().isSignatureSameAsInitial()){
					setErrorMessage(null);
					setPageComplete(false);
					return;
				}
				RefactoringStatus nameCheck= getChangeMethodSignatureRefactoring().checkSignature();
				if (nameCheck.hasFatalError()){
					if (displayErrorMessage)
						setErrorMessage(nameCheck.getMessageMatchingSeverity(RefactoringStatus.FATAL));
					setPageComplete(false);
				} else {
					setErrorMessage(null);	
					setPageComplete(true);
				}	
			} catch (JavaModelException e){
				setErrorMessage(RefactoringMessages.getString("ChangeSignatureInputPage.Internal_Error")); //$NON-NLS-1$
				setPageComplete(false);
				JavaPlugin.log(e);
			}
		}

		private void updateSignaturePreview() {
			try{
				int top= fSignaturePreview.getTextWidget().getTopPixel();
				fSignaturePreviewDocument.set(getChangeMethodSignatureRefactoring().getMethodSignaturePreview()); //$NON-NLS-1$
				fSignaturePreview.getTextWidget().setTopPixel(top);
			} catch (JavaModelException e){
				ExceptionHandler.handle(e, RefactoringMessages.getString("ChangeSignatureRefactoring.modify_Parameters"), RefactoringMessages.getString("ChangeSignatureInputPage.exception")); //$NON-NLS-2$ //$NON-NLS-1$
			}	
		}
	}
}
