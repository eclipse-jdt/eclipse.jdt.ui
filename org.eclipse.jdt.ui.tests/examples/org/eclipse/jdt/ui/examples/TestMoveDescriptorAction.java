/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.MoveDescriptor;

/** In plugin.xml:
 * <pre>{@code
         <extension
         point="org.eclipse.ui.popupMenus">
      <objectContribution
            objectClass="org.eclipse.jdt.core.ICompilationUnit"
            id="org.eclipse.jdt.ui.examples.TestMoveDescriptorAction">
         <action
               label="Move to default package (jdt.ui.tests)"
               tooltip="Move to default package"
               class="org.eclipse.jdt.ui.examples.TestMoveDescriptorAction"
               menubarPath="TestMoveDescriptorAction"
               enablesFor="1"
               id="TestMoveDescriptorAction">
         </action>
      </objectContribution>
   </extension>
   }</pre>
*/

public class TestMoveDescriptorAction extends Action implements IActionDelegate {

	private ICompilationUnit fCU;


	@Override
	public void run(IAction action) {
		try {
			if (fCU != null) {
				PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
					try {
						performAction(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				});
			}
		} catch (Exception e) {
			JavaTestPlugin.log(e);
		}

	}
	private void performAction(IProgressMonitor monitor) throws CoreException {

		IPackageFragmentRoot root= (IPackageFragmentRoot) fCU.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);

		RefactoringContribution refactoringContribution= RefactoringCore.getRefactoringContribution(IJavaRefactorings.MOVE);
		RefactoringDescriptor desc= refactoringContribution.createDescriptor();
		MoveDescriptor moveDes= (MoveDescriptor) desc;
		moveDes.setComment("Moving cu");
		moveDes.setDescription("Moving cu");
		moveDes.setDestination(root.getPackageFragment(""));
		moveDes.setProject(root.getJavaProject().getElementName());
		moveDes.setMoveResources(new IFile[0], new IFolder[0], new ICompilationUnit[] { fCU });
		moveDes.setUpdateReferences(true);

		RefactoringStatus status= new RefactoringStatus();

		RefactoringContext context= moveDes.createRefactoringContext(status);
		PerformRefactoringOperation op= new PerformRefactoringOperation(context, CheckConditionsOperation.ALL_CONDITIONS);
		op.run(monitor);
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fCU= null;
		if (selection instanceof IStructuredSelection) {
			Object object= ((IStructuredSelection) selection).getFirstElement();
			if (object instanceof ICompilationUnit) {
				fCU= (ICompilationUnit) object;

			}
		}
	}

}
