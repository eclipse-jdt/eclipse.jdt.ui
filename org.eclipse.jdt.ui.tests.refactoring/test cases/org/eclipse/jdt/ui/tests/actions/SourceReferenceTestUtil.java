package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Assert;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.ui.reorg.CopySourceReferencesToClipboardAction;
import org.eclipse.jdt.internal.ui.reorg.CutSourceReferencesToClipboardAction;
import org.eclipse.jdt.internal.ui.reorg.DeleteSourceReferencesAction;
import org.eclipse.jdt.internal.ui.reorg.PasteSourceReferencesAction;

class SourceReferenceTestUtil {
	
	private SourceReferenceTestUtil(){
	}
	
	static DeleteSourceReferencesAction createDeleteAction(Object[] elems){
		ISelectionProvider provider= new FakeSelectionProvider(elems);

		DeleteSourceReferencesAction deleteAction= new DeleteSourceReferencesAction(provider){
			protected boolean isOkToDeleteCus(ICompilationUnit[] cusToDelete) {
				return false;
			}
		};
		return deleteAction;
	}

	static void delete(Object[] elems) {
		DeleteSourceReferencesAction deleteAction= createDeleteAction(elems);
		deleteAction.update();
		Assert.assertTrue("delete action enabled", deleteAction.isEnabled());
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
		ISelectionProvider provider= new FakeSelectionProvider(elems);
		CopySourceReferencesToClipboardAction copyAction= new CopySourceReferencesToClipboardAction(provider);
		copyAction.update();
		Assert.assertTrue("copy enabled", copyAction.isEnabled());
		copyAction.run();
	}	

	static void paste(Object[] elems) {
		ISelectionProvider provider1= new FakeSelectionProvider(elems);
		PasteSourceReferencesAction pasteAction= new PasteSourceReferencesAction(provider1);
		pasteAction.update();
		Assert.assertTrue("paste enabled", pasteAction.isEnabled());
		pasteAction.run();
	}

}
