/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others. All
 * rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveInstanceMethodWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringPreferences;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.reorg.JdtMoveAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgDestinationAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgMessages;

/**
 * Central access point to execute rename refactorings.
 * 
 * @since 2.1
 */
public class MoveSupport {
	
	private Refactoring fRefactoring;
	private RefactoringStatus fPreCheckStatus;

	/** Flag indication that no additional update is to be performed. */
	public static final int NONE= 0;
	
	/** Flag indicating that references are to be updated as well. */
	public static final int UPDATE_REFERENCES= 1 << 0;
	
	private MoveSupport(Refactoring refactoring) {
		Assert.isNotNull(refactoring);
		fRefactoring= refactoring;
	}
	
	/**
	 * Executes some light weight precondition checking. If the returned status
	 * is an error then the refactoring can't be executed at all. However,
	 * returning an OK status doesn't guarantee that the refactoring can be
	 * executed. It may still fail while performing the exhaustive precondition
	 * checking done inside the methods <code>openDialog</code> or
	 * <code>perform</code>.
	 * 
	 * The method is mainly used to determine enable/disablement of actions.
	 * 
	 * @return the result of the light weight precondition checking.
	 * 
	 * @throws if an unexpected exception occurs while performing the checking.
	 * 
	 * @see #openDialog(Shell)
	 * @see #perform(Shell, IRunnableContext)
	 */
	public IStatus preCheck() throws CoreException {
		ensureChecked();
		if (fPreCheckStatus.hasFatalError())
			return fPreCheckStatus.getFirstEntry(RefactoringStatus.FATAL).asStatus();
		else
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null);
	}

	/**
	 * Executes the rename refactoring without showing a dialog to gather
	 * additional user input (e.g. the new name of the <tt>IJavaElement</tt>,
	 * ...). Only an error dialog is shown (if necessary) to present the result
	 * of the refactoring's full precondition checking.
	 * 
	 * @param parent a shell used as a parent for the error dialog.
	 * @param context a <tt>IRunnableContext</tt> to execute the operation.
	 * 
	 * @throws InterruptedException if the operation has been canceled by the
	 * user.
	 * @throws InvocationTargetException if an error occured while executing the
	 * operation.
	 * 
	 * @see #openDialog(Shell)
	 * @see IRunnableContext#run(boolean, boolean, org.eclipse.jface.operation.IRunnableWithProgress)
	 */
	public void perform(Shell parent, IRunnableContext context) throws InterruptedException, InvocationTargetException {
		try {
			ensureChecked();
			if (fPreCheckStatus.hasFatalError()) {
				showInformation(parent, fPreCheckStatus);
				return; 
			}
		} catch (CoreException e){
			throw new InvocationTargetException(e);
		}
		RefactoringExecutionHelper helper= new RefactoringExecutionHelper(fRefactoring,
			RefactoringPreferences.getStopSeverity(), parent, context);
		helper.perform();
	}
	
	/**
	 * Opens the refactoring dialog for this rename support. 
	 * 
	 * @param parent a shell used as a parent for the refactoring dialog.
	 * @throws CoreException if an unexpected exception occurs while opening the
	 * dialog.
	 */
	public void openDialog(Shell parent) throws CoreException {
		ensureChecked();
		if (fPreCheckStatus.hasFatalError()) {
			showInformation(parent, fPreCheckStatus);
			return; 
		}
		if (fRefactoring instanceof MoveRefactoring) {
			// To make the API equavilent with Rename support
			try {
				handleNormalMove(parent, (MoveRefactoring)fRefactoring);
			} catch (InterruptedException e) {
				// Can't happen since cancel button is disabled during change execution.
			} catch (InvocationTargetException e) {
				throw new JavaModelException(e, IStatus.ERROR);
			}
		} else {
			handleMemberMove(parent, fRefactoring);
		}
	}
	
	public static MoveSupport create(IPackageFragment[] fragments, IPackageFragmentRoot destination, int flags) throws CoreException {
		MoveRefactoring refactoring= new MoveRefactoring(Arrays.asList(fragments), JavaPreferencesSettings.getCodeGenerationSettings());
		refactoring.setDestination(destination);
		refactoring.setUpdateReferences(updateReferences(flags));
		return new MoveSupport(refactoring);
	}
	
	public static MoveSupport create(ICompilationUnit[] units, IPackageFragment destination, int flags) throws CoreException {
		MoveRefactoring refactoring= new MoveRefactoring(Arrays.asList(units), JavaPreferencesSettings.getCodeGenerationSettings());
		refactoring.setDestination(destination);
		refactoring.setUpdateReferences(updateReferences(flags));
		return new MoveSupport(refactoring);
	}
	
	private static void handleNormalMove(Shell parent, MoveRefactoring refactoring) throws InterruptedException, InvocationTargetException {
		ElementTreeSelectionDialog dialog= JdtMoveAction.makeDialog(parent, refactoring);
		ReorgDestinationAction.initDialog(dialog, 
			ReorgMessages.getString("moveAction.name"), //$NON-NLS-1$
			ReorgMessages.getString("moveAction.destination.label"), //$NON-NLS-1$
			refactoring,
			refactoring.getDestination());
		switch (dialog.open()) {
			case IDialogConstants.CANCEL_ID:
				return;
			case IDialogConstants.OK_ID:
				RefactoringExecutionHelper helper= new RefactoringExecutionHelper(refactoring,
					RefactoringPreferences.getStopSeverity(), parent, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
				helper.perform();
				return;
			case JdtMoveAction.PREVIEW_ID:
				JdtMoveAction.openWizard(parent, refactoring);
				return;
		}
	}

	private static void handleMemberMove(Shell parent, Refactoring refactoring) throws JavaModelException {
		RefactoringWizard wizard= null;
		if (refactoring instanceof MoveStaticMembersRefactoring) { 
			wizard= new MoveMembersWizard((MoveStaticMembersRefactoring)refactoring);
		} else if (refactoring instanceof MoveInstanceMethodRefactoring) {
			wizard= new MoveInstanceMethodWizard((MoveInstanceMethodRefactoring)refactoring);
		}
		
		RefactoringStarter starter= new RefactoringStarter();
		starter.activate(refactoring, wizard, parent, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
	}

	private void ensureChecked() throws CoreException {
		if (fPreCheckStatus == null) {
			if (fRefactoring instanceof MoveStaticMembersRefactoring) {
				fPreCheckStatus= ((MoveStaticMembersRefactoring)fRefactoring).checkPreactivation();
			} else {
				fPreCheckStatus= fRefactoring.checkActivation(new NullProgressMonitor());
			}
		}
	}
	
	private void showInformation(Shell parent, RefactoringStatus status) {
		String message= status.getFirstMessage(RefactoringStatus.FATAL);
		MessageDialog.openInformation(parent, fRefactoring.getName(), message);
	}
	
	private static boolean updateReferences(int flags) {
		return (flags & UPDATE_REFERENCES) != 0;
	}	
}
