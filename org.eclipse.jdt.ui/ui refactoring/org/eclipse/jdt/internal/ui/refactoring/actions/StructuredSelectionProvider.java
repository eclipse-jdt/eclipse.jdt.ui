/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;

/**
 */
public abstract class StructuredSelectionProvider {

	private static abstract class Adapter extends StructuredSelectionProvider {
		private ITextSelection fLastTextSelection;
		private IStructuredSelection fLastStructuredSelection;
		
		protected IStructuredSelection asStructuredSelection(ITextSelection selection) {
			IEditorPart editor= JavaPlugin.getActivePage().getActiveEditor();
			if (editor == null)
				return null;
			return asStructuredSelection(selection, editor);				
		}
		
		protected IStructuredSelection asStructuredSelection(ITextSelection selection, IEditorPart editor) {
			if (selection.getLength() == 0)
				return StructuredSelection.EMPTY;
				
			IStructuredSelection result= considerCache(selection);
			if (result != null)
				return result;
				
			ICodeAssist assist= getCodeAssist(editor);
			if (assist == null)
				result= null;
			
			try {
				IJavaElement[] elements= assist.codeSelect(selection.getOffset(), selection.getLength());
				result= new StructuredSelection(elements);
			} catch (JavaModelException e) {
				ExceptionHandler.handle(e, "Selection Converter", "Unexpected exception while converting text selection.");
			}
			cacheResult(selection, result);
			return result;
		}
		
		private IStructuredSelection considerCache(ITextSelection selection) {
			if (selection != fLastTextSelection) {
				fLastTextSelection= null;
				fLastStructuredSelection= null;
			}
			return fLastStructuredSelection;
		}
		
		private void cacheResult(ITextSelection selection, IStructuredSelection result) {
			fLastTextSelection= selection;
			fLastStructuredSelection= result;
		}
		
		private ICodeAssist getCodeAssist(IEditorPart editorPart) {
			IEditorInput input= editorPart.getEditorInput();
			if (input instanceof ClassFileEditorInput)
				return ((ClassFileEditorInput)input).getClassFile();
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
			return manager.getWorkingCopy(input);
		}
	}

	private static class SelectionProviderAdapter extends Adapter {
		private ISelectionProvider fProvider; 
		public SelectionProviderAdapter(ISelectionProvider provider) {
			fProvider= provider;
			Assert.isNotNull(fProvider);
		}
		public IStructuredSelection getSelection() {
			ISelection result= fProvider.getSelection();
			if (result instanceof IStructuredSelection)
				return (IStructuredSelection)result;
			if (result instanceof ITextSelection && fProvider instanceof IEditorPart)
				return asStructuredSelection((ITextSelection)result, (IEditorPart)fProvider);
			return null;
		}
	}
	
	private static class SelectionServiceAdapter extends Adapter {
		private ISelectionService fService; 
		public SelectionServiceAdapter(ISelectionService service) {
			fService= service;
			Assert.isNotNull(fService);
		}
		public IStructuredSelection getSelection() {
			ISelection result= fService.getSelection();
			if (result instanceof IStructuredSelection)
				return (IStructuredSelection)result;
			if (result instanceof ITextSelection)
				return asStructuredSelection((ITextSelection)result);
			return null;
		}
	}
	
	public abstract IStructuredSelection getSelection();

	private StructuredSelectionProvider() {
		// prevent instantiation.
	}

	public static StructuredSelectionProvider createFrom(ISelectionProvider provider) {
		return new SelectionProviderAdapter(provider);
	}
	
	public static StructuredSelectionProvider createFrom(ISelectionService service) {
		return new SelectionServiceAdapter(service);
	}	
}

