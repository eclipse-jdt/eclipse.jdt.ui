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
package org.eclipse.jdt.internal.ui.reorg;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.MoveProjectAction;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.QualifiedNameComponent;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

public class JdtMoveAction extends ReorgDestinationAction {

	public static final int PREVIEW_ID= IDialogConstants.CLIENT_ID + 1;
	private static final String DEFAULT_PACKAGE_WARNING= "DefaultPackageWarningDialog"; //$NON-NLS-1$
	
	private boolean fShowPreview= false;

	public JdtMoveAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.getString("moveAction.label"));//$NON-NLS-1$
	}

	protected void run(IStructuredSelection selection) {
		if (ClipboardActionUtil.hasOnlyProjects(selection)){
			moveProject(selection);
		}	else {
			super.run(selection);
		}
	}

	public static ElementTreeSelectionDialog makeDialog(Shell parent, MoveRefactoring refactoring) {
		StandardJavaElementContentProvider cp= new DestinationDialogContentProvider(); 
		MoveDestinationDialog dialog= new MoveDestinationDialog(
			parent,  new DestinationRenderer(JavaElementLabelProvider.SHOW_SMALL_ICONS),
			cp, refactoring);
		return dialog;
	}
		
	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	String getActionName() {
		return ReorgMessages.getString("moveAction.name"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#getDestinationDialogMessage
	 */
	String getDestinationDialogMessage() {
		return ReorgMessages.getString("moveAction.destination.label"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#createRefactoring
	 */
	ReorgRefactoring createRefactoring(List elements){
		IPackageFragmentRootManipulationQuery query= createUpdateClasspathQuery(getShell());
		return new MoveRefactoring(elements, JavaPreferencesSettings.getCodeGenerationSettings(), query);
	}
	
	ElementTreeSelectionDialog createDestinationSelectionDialog(Shell parent, ILabelProvider labelProvider, StandardJavaElementContentProvider cp, ReorgRefactoring refactoring){
		return new MoveDestinationDialog(parent, labelProvider, cp, (MoveRefactoring)refactoring);
	}	
	
	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	protected boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException{
		return isOkToMoveReadOnly(refactoring);
	}
	
	protected void setShowPreview(boolean showPreview) {
		fShowPreview = showPreview;
	}
	
	private static boolean isOkToMoveReadOnly(ReorgRefactoring refactoring){
		if (! hasReadOnlyElements(refactoring))
			return true;

		//we need to confirm this in all cases except for the case of moving cus to a package
		//Then, confirmation does not make sense because jcore throws an exception anyway
		if (refactoring.getDestination() instanceof IPackageFragment){
			List list= refactoring.getElementsToReorg();
			for (Iterator iter= list.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (! (element instanceof ICompilationUnit))
					return askIfOkToMoveReadOnly();
			}
			return true;
		} else {
			return askIfOkToMoveReadOnly();
		}
	}
	
	private static boolean askIfOkToMoveReadOnly(){
		return  MessageDialog.openQuestion(
				JavaPlugin.getActiveWorkbenchShell(),
				ReorgMessages.getString("moveAction.checkMove"),  //$NON-NLS-1$
				ReorgMessages.getString("moveAction.error.readOnly")); //$NON-NLS-1$
	}
	
	private static boolean hasReadOnlyElements(ReorgRefactoring refactoring){
		for (Iterator iter= refactoring.getElementsToReorg().iterator(); iter.hasNext(); ){
			if (ReorgUtils.shouldConfirmReadOnly(iter.next())) 
				return true;
		}
		return false;
	}
	
	protected Object openDialog(ElementTreeSelectionDialog dialog) {
		Object result= super.openDialog(dialog);
		if (dialog instanceof MoveDestinationDialog) {
			fShowPreview= dialog.getReturnCode() == PREVIEW_ID;
		} else {
			fShowPreview= false;
		}
		return result;
	}

		
	/* non java-doc
	 * @see ReorgDestinationAction#doReorg(ReorgRefactoring) 
	 */
	void reorg(ReorgRefactoring refactoring) throws JavaModelException{
		//moving to/from default package does not update anything - show info about it and provide 'cancel'
		if (! showWarningAboutDefaultPackages(refactoring))
			return;
		
		if (fShowPreview){		
			openWizard(getShell(), refactoring);
			return;
		} else
			super.reorg(refactoring);
	}

	/*
	 * returns true if it's ok to continue reorging
	 * false otherwise
	 */
	private boolean showWarningAboutDefaultPackages(ReorgRefactoring refactoring) {
		try {
			IPackageFragment destination= ReorgRefactoring.getDestinationAsPackageFragment(refactoring.getDestination());
			if (! JavaElementUtil.isDefaultPackage(destination)){
				List elementsToMove= getNotExcluded(refactoring.getElementsToReorg(), refactoring.getExcludedElements());
				if (! containsAnyCusFromDefaultPackage(elementsToMove))
					return true;
			}
			int result= OptionalMessageDialog.open(
						DEFAULT_PACKAGE_WARNING, 
						getShell(), 
						ReorgMessages.getString("JdtMoveAction.move"), //$NON-NLS-1$
						null, 
						ReorgMessages.getString("JdtMoveAction.default_package_warning"), //$NON-NLS-1$
						MessageDialog.INFORMATION, 
						new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 
						0);
			return result != MessageDialog.CANCEL;
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, ReorgMessages.getString("JdtMoveAction.move"), ReorgMessages.getString("JdtMoveAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
	}

	private static boolean containsAnyCusFromDefaultPackage(List list) {
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof ICompilationUnit){
				if (JavaElementUtil.isDefaultPackage(((ICompilationUnit)element).getParent()))
					return true;
			}
		}
		return false;
	}

	private static List getNotExcluded(Collection elements, Collection excluded) {
		List result= new ArrayList(elements.size());
		result.addAll(elements);
		result.removeAll(excluded);
		return result;
	}

	public static void openWizard(Shell parent, ReorgRefactoring refactoring) {
		//XX incorrect help
		RefactoringWizard wizard= new RefactoringWizard(refactoring, ReorgMessages.getString("JdtMoveAction.move"), IJavaHelpContextIds.MOVE_CU_ERROR_WIZARD_PAGE); //$NON-NLS-1$
		wizard.setChangeCreationCancelable(false);
		new RefactoringWizardDialog(parent, wizard).open();	
		return;	
	}
	
	private void moveProject(IStructuredSelection selection){
		MoveProjectAction action= new MoveProjectAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(selection);
		action.run();
	}
	
	//--- static inner classes
		
	private static class MoveDestinationDialog extends ElementTreeSelectionDialog {
		private MoveRefactoring fRefactoring;
		private Button fReferenceCheckbox;
		private Button fQualifiedNameCheckbox;
		private QualifiedNameComponent fQualifiedNameComponent;
		private Button fPreview;
		
		MoveDestinationDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider, MoveRefactoring refactoring) {
			super(parent, labelProvider, contentProvider);
			fRefactoring= refactoring;
			setDoubleClickSelects(false);
		}
		
		protected void updateOKStatus() {
			super.updateOKStatus();
			try{
				Button okButton= getOkButton();
				boolean okEnabled= okButton.getEnabled();
				fRefactoring.setDestination(getFirstResult());
				fReferenceCheckbox.setEnabled(okEnabled && canUpdateReferences());
				fRefactoring.setUpdateReferences(fReferenceCheckbox.getEnabled() && fReferenceCheckbox.getSelection());
				if (fQualifiedNameCheckbox != null) {
					boolean enabled= okEnabled && fRefactoring.canEnableQualifiedNameUpdating();
					fQualifiedNameCheckbox.setEnabled(enabled);
					if (enabled) {
						fQualifiedNameComponent.setEnabled(fRefactoring.getUpdateQualifiedNames());
						if (fRefactoring.getUpdateQualifiedNames())
							okButton.setEnabled(false);
					} else {
						fQualifiedNameComponent.setEnabled(false);
					}
					fRefactoring.setUpdateQualifiedNames(fQualifiedNameCheckbox.getEnabled() && fQualifiedNameCheckbox.getSelection());
				}
				boolean preview= okEnabled;
				if (preview)
					preview= 
						fRefactoring.getUpdateQualifiedNames() && fRefactoring.canEnableQualifiedNameUpdating() ||
						fReferenceCheckbox.getSelection() && fRefactoring.canUpdateReferences();
				fPreview.setEnabled(preview);
			} catch (JavaModelException e){
				ExceptionHandler.handle(e, ReorgMessages.getString("JdtMoveAction.move"), ReorgMessages.getString("JdtMoveAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			}		
		}

		protected void buttonPressed(int buttonId) {
			super.buttonPressed(buttonId);
			if (buttonId == PREVIEW_ID) {
				setReturnCode(PREVIEW_ID);
				close();
			}
		}
		
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.MOVE_DESTINATION_DIALOG);
		}

		protected void createButtonsForButtonBar(Composite parent) {
			fPreview= createButton(parent, PREVIEW_ID, ReorgMessages.getString("JdtMoveAction.preview"), false); //$NON-NLS-1$
			super.createButtonsForButtonBar(parent);
		}
		
		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			addUpdateReferenceComponent(result);
			addUpdateQualifiedNameComponent(result, ((GridLayout)result.getLayout()).marginWidth);
			applyDialogFont(result);		
			return result;
		}

		private void addUpdateReferenceComponent(Composite result) {
			fReferenceCheckbox= new Button(result, SWT.CHECK);
			fReferenceCheckbox.setText(ReorgMessages.getString("JdtMoveAction.update_references")); //$NON-NLS-1$
			fReferenceCheckbox.setSelection(fRefactoring.getUpdateReferences());
			fReferenceCheckbox.setEnabled(canUpdateReferences());
			
			fReferenceCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fRefactoring.setUpdateReferences(((Button)e.widget).getSelection());
					updateOKStatus();
				}
			});
		}
		
		private boolean canUpdateReferences() {
			try{
				return fRefactoring.canUpdateReferences();
			} catch (JavaModelException e){
				return false;
			}
		}
		
		private void addUpdateQualifiedNameComponent(Composite parent, int marginWidth) {
			if (!fRefactoring.canUpdateQualifiedNames())
				return;
			fQualifiedNameCheckbox= new Button(parent, SWT.CHECK);
			int indent= marginWidth + fQualifiedNameCheckbox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			fQualifiedNameCheckbox.setText(RefactoringMessages.getString("RenameInputWizardPage.update_qualified_names")); //$NON-NLS-1$
			fQualifiedNameCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fQualifiedNameCheckbox.setSelection(fRefactoring.getUpdateQualifiedNames());
		
			fQualifiedNameComponent= new QualifiedNameComponent(parent, SWT.NONE, fRefactoring, getRefactoringSettings());
			fQualifiedNameComponent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			GridData gd= (GridData)fQualifiedNameComponent.getLayoutData();
			gd.horizontalAlignment= GridData.FILL;
			gd.horizontalIndent= indent;
			fQualifiedNameComponent.setEnabled(false);

			fQualifiedNameCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean enabled= ((Button)e.widget).getSelection();
					fQualifiedNameComponent.setEnabled(enabled);
					fRefactoring.setUpdateQualifiedNames(enabled);
					updateOKStatus();
				}
			});
		}
		
		protected IDialogSettings getRefactoringSettings() {
			IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
			if (settings == null)
				return null;
			IDialogSettings result= settings.getSection(RefactoringWizardPage.REFACTORING_SETTINGS);
			if (result == null) {
				result= new DialogSettings(RefactoringWizardPage.REFACTORING_SETTINGS);
				settings.addSection(result); 
			}
			return result;
		}			
		public boolean close() {
			if (getReturnCode() != IDialogConstants.CANCEL_ID && fQualifiedNameComponent != null) {
				fQualifiedNameComponent.savePatterns(getRefactoringSettings());
			}
			return super.close();
		}
	}
	
	public static IPackageFragmentRootManipulationQuery createUpdateClasspathQuery(Shell shell){
		String messagePattern= ReorgMessages.getString("JdtMoveAction.referenced") + //$NON-NLS-1$
			ReorgMessages.getString("JdtMoveAction.update_classpath"); //$NON-NLS-1$
		return new PackageFragmentRootManipulationQuery(shell, ReorgMessages.getString("JdtMoveAction.Move"), messagePattern); //$NON-NLS-1$
	}
}
