package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Assert;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.tests.refactoring.infra.*;

import org.eclipse.jdt.internal.ui.reorg.DeleteSourceReferencesAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgActionFactory;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

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
		deleteAction.update();
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

	static void copy(Object[] elems) {
		SelectionDispatchAction copyAction= ReorgActionFactory.createCopyAction(new MockWorkbenchSite(elems), new MockSelectionProvider(elems));
		copyAction.update();
		Assert.assertTrue("copy incorrectly disabled", copyAction.isEnabled());
		copyAction.run();
	}	

	static void paste(Object[] elems) {
		SelectionDispatchAction pasteAction= ReorgActionFactory.createPasteAction(new MockWorkbenchSite(elems), new MockSelectionProvider(elems));
		pasteAction.update();
		Assert.assertTrue("paste incorrectly disabled", pasteAction.isEnabled());
		pasteAction.run();
	}

}
