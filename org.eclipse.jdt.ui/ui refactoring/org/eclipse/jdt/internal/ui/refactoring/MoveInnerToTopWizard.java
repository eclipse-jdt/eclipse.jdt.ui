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

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class MoveInnerToTopWizard extends RefactoringWizard {

	public MoveInnerToTopWizard(Refactoring ref) {
		super(ref, RefactoringMessages.getString("MoveInnerToTopWizard.Move_Inner"), IJavaHelpContextIds.MOVE_INNER_TO_TOP_ERROR_WIZARD_PAGE); //$NON-NLS-1$
//		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		try{			
			//no input page if the type is static
			if (! JdtFlags.isStatic(getMoveRefactoring().getInputType()))
				addPage(new MoveInnerToToplnputPage(getInitialNameForEnclosingInstance()));
			else
				setChangeCreationCancelable(false);
		} catch (JavaModelException e){
			//log and try anyway
			JavaPlugin.log(e);
			addPage(new MoveInnerToToplnputPage(getInitialNameForEnclosingInstance())); 
		}		
	}

	private String getInitialNameForEnclosingInstance() {
		return getMoveRefactoring().getEnclosingInstanceName();
	}

	private MoveInnerToTopRefactoring getMoveRefactoring() {
		return (MoveInnerToTopRefactoring)getRefactoring();
	}
}
