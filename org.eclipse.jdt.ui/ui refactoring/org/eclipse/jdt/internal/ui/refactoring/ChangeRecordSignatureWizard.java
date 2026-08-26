package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeRecordSignatureProcessor;

public class ChangeRecordSignatureWizard extends RefactoringWizard{

	ChangeRecordSignatureProcessor fProcessor;

	public ChangeRecordSignatureWizard(ChangeRecordSignatureProcessor processor, Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE);
		this.fProcessor = processor;
		setDefaultPageTitle(RefactoringMessages.ChangeSignatureRefactoring_modify_Parameters);
	}

	@Override
	protected void addUserInputPages() {
		addPage(new ChangeRecordSignatureInputPage(fProcessor));
	}

	private static class ChangeRecordSignatureInputPage extends UserInputWizardPage {

		public static final String PAGE_NAME= "ChangeRecordSignatureInputPage"; //$NON-NLS-1$

		private final ChangeRecordSignatureProcessor fProcessor;

		public ChangeRecordSignatureInputPage(ChangeRecordSignatureProcessor processor) {
			super(PAGE_NAME);
			this.fProcessor = processor;
		}

		@Override
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NONE);
			final GridLayout layout= new GridLayout();
			composite.setLayout(layout);
			initializeDialogUnits(composite);
			createRecordParameterControl(composite);
			Label sep= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			sep.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));

			update(false);
			setControl(composite);
			Dialog.applyDialogFont(composite);
		}

		private void createHeadControls(Composite parent) throws JavaModelException {
			//must create controls column-wise to get mnemonics working:
			//The record can be only public, so we don't need to add access ocntrols
			// We don't even need to change the return type or the method name.
			// We don't need an exception change tab too.
			Composite composite= new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			GridLayout layout= new GridLayout(3, false);
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			composite.setLayout(layout);
		}

		private Control createRecordParameterControl(Composite parent) {
			Composite border= new Composite(parent, SWT.NONE);
			border.setLayout(new GridLayout());
			border.setLayoutData(new GridData(GridData.FILL_BOTH));

			ChangeParametersControl cp= new ChangeParametersControl(border, SWT.NONE, null, new IParameterListChangeListener() {
				@Override
				public void parameterChanged(ParameterInfo parameter) {
					update(true);
				}
				@Override
				public void parameterListChanged() {
					//update(true);
				}
				@Override
				public void parameterAdded(ParameterInfo parameter) {
					//update(true);
				}
			}, ChangeParametersControl.Mode.CHANGE_METHOD_SIGNATURE, getChangeRecordSignatureProcessor().getStubTypeContext());
			cp.setLayoutData(new GridData(GridData.FILL_BOTH));
			cp.setInput(getChangeRecordSignatureProcessor().getParameterInfos());
			return border;
		}

		private void update(boolean displayErrorMessage){
			//updateStatus(displayErrorMessage);
			//updateSignaturePreview();
		}

		public ChangeRecordSignatureProcessor getChangeRecordSignatureProcessor() {
			return fProcessor;
		}

//		private void createNameControl(Composite parent) {
//			Composite name= new Composite(parent, SWT.NONE);
//			name.setLayoutData(new GridData(GridData.FILL_BOTH));
//			GridLayout layout= new GridLayout(1, false);
//			layout.marginHeight= 0;
//			layout.marginWidth= 0;
//			name.setLayout(layout);
//
//			Label label= new Label(name, SWT.NONE);
//			label.setText(RefactoringMessages.ChangeSignatureInputPage_method_name);
//
//			final Text text= new Text(name, SWT.BORDER);
//			text.setText(getChangeRecordMethodSignatureProcessor().getMethodName());
//			text.setLayoutData((new GridData(GridData.FILL_HORIZONTAL)));
//			TextFieldNavigationHandler.install(text);
//
//			if (getChangeRecordMethodSignatureProcessor().canChangeNameAndReturnType()) {
//				text.addModifyListener(e -> {
//					getChangeRecordMethodSignatureProcessor().setNewMethodName(text.getText());
//					update(true);
//				});
//			} else {
//				text.setEnabled(false);
//			}
//		}


		private ChangeRecordSignatureProcessor getChangeRecordMethodSignatureProcessor() {
			return fProcessor;
		}

	}

}
