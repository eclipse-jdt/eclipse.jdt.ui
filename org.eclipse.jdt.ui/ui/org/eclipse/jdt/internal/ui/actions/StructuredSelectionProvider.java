/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;

/**
 */
public abstract class StructuredSelectionProvider {
	
	public static int FLAGS_DO_CODERESOLVE= 1;
	public static int FLAGS_DO_ELEMENT_AT_OFFSET= 2;

	private static abstract class Adapter extends StructuredSelectionProvider {
		private ITextSelection fLastTextSelection;
		private IStructuredSelection fLastStructuredSelection;
		
		private Adapter() {
		}
		
		protected IStructuredSelection asStructuredSelection(ITextSelection selection, int flags) {
			IEditorPart editor= JavaPlugin.getActivePage().getActiveEditor();
			if (editor == null)
				return null;
			return asStructuredSelection(selection, editor, flags);				
		}
		
		protected IStructuredSelection asStructuredSelection(ITextSelection selection, IEditorPart editor, int flags) {
			if ((flags & FLAGS_DO_CODERESOLVE) != 0) {
				if (selection.getLength() == 0)
					return StructuredSelection.EMPTY;				
				IStructuredSelection result= considerCache(selection);
				if (result != null)
					return result;
					
				IJavaElement assist= getEditorInput(editor);
				if (assist instanceof ICodeAssist) {
					try {
						IJavaElement[] elements= ((ICodeAssist)assist).codeSelect(selection.getOffset(), selection.getLength());
						result= new StructuredSelection(elements);
						cacheResult(selection, result);
						return result;						
					} catch (JavaModelException e) {
						JavaPlugin.log(e);
					}
				}
			} else if ((flags & FLAGS_DO_ELEMENT_AT_OFFSET) != 0) {
				try {
					IJavaElement assist= getEditorInput(editor);
					if (assist instanceof ICompilationUnit) {
						ICompilationUnit cu= (ICompilationUnit) assist;
						if (cu.isWorkingCopy()) {
							synchronized (cu) {
								cu.reconcile();
							}
						}
						IJavaElement ref= ((ICompilationUnit)assist).getElementAt(selection.getOffset());
						if (ref != null) {
							return new StructuredSelection(ref);
						}
					} else if (assist instanceof IClassFile) {
						IJavaElement ref= ((IClassFile)assist).getElementAt(selection.getOffset());
						if (ref != null) {
							return new StructuredSelection(ref);
						}
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
				}
			}
			return StructuredSelection.EMPTY;
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
		
		private IJavaElement getEditorInput(IEditorPart editorPart) {
			IEditorInput input= editorPart.getEditorInput();
			if (input instanceof IClassFileEditorInput)
				return ((IClassFileEditorInput)input).getClassFile();
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
			return manager.getWorkingCopy(input);
		}
	}

	private static class SelectionProviderAdapter extends Adapter {
		private ISelectionProvider fProvider; 
		public SelectionProviderAdapter(ISelectionProvider provider) {
			super();
			fProvider= provider;
			Assert.isNotNull(fProvider);
		}
		public IStructuredSelection getSelection(int flags) {
			ISelection result= fProvider.getSelection();
			if (result instanceof IStructuredSelection)
				return (IStructuredSelection)result;
			if (result instanceof ITextSelection && fProvider instanceof IEditorPart)
				return asStructuredSelection((ITextSelection)result, (IEditorPart)fProvider, flags);
			return StructuredSelection.EMPTY;
		}
	}
	
	private static class SelectionServiceAdapter extends Adapter {
		private ISelectionService fService; 
		public SelectionServiceAdapter(ISelectionService service) {
			super();
			fService= service;
			Assert.isNotNull(fService);
		}
		public IStructuredSelection getSelection(int flags) {
			ISelection result= fService.getSelection();
			if (result instanceof IStructuredSelection)
				return (IStructuredSelection)result;
			if (result instanceof ITextSelection)
				return asStructuredSelection((ITextSelection)result, flags);
			return null;
		}
	}
	
	public IStructuredSelection getSelection() {
		return getSelection(FLAGS_DO_CODERESOLVE);
	}
	
	public abstract IStructuredSelection getSelection(int flags);

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

