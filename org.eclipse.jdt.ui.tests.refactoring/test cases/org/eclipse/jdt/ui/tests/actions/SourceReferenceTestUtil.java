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
package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Assert;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;

import org.eclipse.jdt.internal.ui.reorg.DeleteSourceReferencesAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgActionFactory;

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

//	static void cut(Object[] elems) {
//		ISelectionProvider provider= new FakeSelectionProvider(elems);
//		CutSourceReferencesToClipboardAction cutAction= new CutSourceReferencesToClipboardAction(provider);
//		cutAction.update();
//		Assert.assertTrue("cut enabled", cutAction.isEnabled());
//		cutAction.run();
//	}	

	static void copy(Object[] elems, Clipboard clipboard) {
		SelectionDispatchAction pasteAction= ReorgActionFactory.createPasteAction(new MockWorkbenchSite(elems), clipboard);
		SelectionDispatchAction copyAction= ReorgActionFactory.createCopyAction(new MockWorkbenchSite(elems), clipboard, pasteAction);
		copyAction.update(copyAction.getSelection());
		Assert.assertTrue("copy incorrectly disabled", copyAction.isEnabled());
		copyAction.run();
	}	

	static void paste(Object[] elems, Clipboard clipboard) {
		SelectionDispatchAction pasteAction= ReorgActionFactory.createPasteAction(new MockWorkbenchSite(elems), clipboard);
		pasteAction.update(pasteAction.getSelection());
		Assert.assertTrue("paste incorrectly disabled", pasteAction.isEnabled());
		pasteAction.run();
	}

}
