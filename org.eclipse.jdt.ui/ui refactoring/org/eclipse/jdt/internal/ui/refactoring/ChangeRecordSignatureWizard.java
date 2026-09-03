package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeRecordSignatureProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;


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
					update(true);
				}
				@Override
				public void parameterAdded(ParameterInfo parameter) {
					update(true);
				}
			}, ChangeParametersControl.Mode.CHANGE_RECORD_SIGNATURE, getChangeRecordSignatureProcessor().getStubTypeContext());
			cp.setLayoutData(new GridData(GridData.FILL_BOTH));
			cp.setInput(getChangeRecordSignatureProcessor().getParameterInfos());
			return border;
		}

		private void update(boolean displayErrorMessage){
			updateStatus(displayErrorMessage);
		}

		private void updateStatus(boolean displayErrorMessage) {
			try{
				if (getChangeRecordSignatureProcessor().isSignatureSameAsInitial()) {
					if (displayErrorMessage)
						setErrorMessage(RefactoringMessages.ChangeSignatureInputPage_unchanged);
					else
						setErrorMessage(null);
					setPageComplete(false);
					return;
				}
				RefactoringStatus nameCheck= getChangeRecordSignatureProcessor().checkSignature();
				if (displayErrorMessage) {
					setPageComplete(nameCheck);
				} else {
					setErrorMessage(null);
					setPageComplete(true);
				}
			} catch (JavaModelException e){
				setErrorMessage(RefactoringMessages.ChangeSignatureInputPage_Internal_Error);
				setPageComplete(false);
				JavaPlugin.log(e);
			}
		}

		public ChangeRecordSignatureProcessor getChangeRecordSignatureProcessor() {
			return fProcessor;
		}
	}

}
