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
package org.eclipse.jdt.ui.tests.reorg;

import junit.framework.Assert;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;

import org.eclipse.jdt.internal.ui.refactoring.reorg.CutSourceReferencesToClipboardAction.DeleteSourceReferencesAction;

class SourceReferenceTestUtil {
	
	private SourceReferenceTestUtil(){
	}
	
	static DeleteSourceReferencesAction createDeleteAction(Object[] elems){
		DeleteSourceReferencesAction deleteAction= new DeleteSourceReferencesAction(new MockWorkbenchSite(elems)){
			protected boolean confirmCusDelete(ICompilationUnit[] cusToDelete) {
				return false;
			}
			protected boolean confirmGetterSetterDelete(){
				return true;
			}
		};
		deleteAction.setAskForDeleteConfirmation(false);
		return deleteAction;
	}

	static void delete(Object[] elems) {
		DeleteSourceReferencesAction deleteAction= createDeleteAction(elems);
		deleteAction.update(deleteAction.getSelection());
		Assert.assertTrue("delete action incorrectly disabled", deleteAction.isEnabled());
		deleteAction.run();
	}
}
