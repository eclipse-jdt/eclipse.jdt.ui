/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.runtime.CoreException;

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
 * @deprecated Use SelectionDispatchAction and SelectionConverter instead
 */
public abstract class StructuredSelectionProvider {
	
	public static int FLAGS_DO_CODERESOLVE= 1;
	public static int FLAGS_DO_ELEMENT_AT_OFFSET= 2;
	public static int FLAGS_GET_EDITOR_INPUT= 4;

	private static abstract class Adapter extends StructuredSelectionProvider {
		private ITextSelection fLastTextSelection;
		private IStructuredSelection fLastStructuredSelection;
		
		private Adapter() {
		}
		
		protected IStructuredSelection asStructuredSelection(ITextSelection selection, int flags) throws CoreException {
			IEditorPart editor= JavaPlugin.getActivePage().getActiveEditor();
			if (editor == null)
				return StructuredSelection.EMPTY;
			return asStructuredSelection(selection, editor, flags);				
		}
		
		protected IStructuredSelection asStructuredSelection(ITextSelection selection, IEditorPart editor, int flags) throws CoreException {
			if ((flags & FLAGS_DO_CODERESOLVE) != 0) {
				IStructuredSelection result;
				if (!selection.isEmpty()) {		
					result= considerCache(selection);
					if (result != null)
						return result;
				}
				
				IJavaElement assist= getEditorInput(editor);
				if (assist instanceof ICodeAssist) {
					IJavaElement[] elements= ((ICodeAssist)assist).codeSelect(selection.getOffset(), selection.getLength());
					result= new StructuredSelection(elements);
					if (!selection.isEmpty())
						cacheResult(selection, result);
					return result;
				}				
			}
			if ((flags & FLAGS_DO_ELEMENT_AT_OFFSET) != 0) {
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
			}
			if ((flags & FLAGS_GET_EDITOR_INPUT) != 0) {
				IJavaElement assist= getEditorInput(editor);
				if (assist != null) {
					return new StructuredSelection(assist);
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
			try {
				ISelection result= fProvider.getSelection();
				if (result instanceof IStructuredSelection)
					return (IStructuredSelection)result;
				if (result instanceof ITextSelection && fProvider instanceof IEditorPart)
					return asStructuredSelection((ITextSelection)result, (IEditorPart)fProvider, flags);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
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
			try {
				ISelection result= fService.getSelection();
				if (result instanceof IStructuredSelection)
					return (IStructuredSelection)result;
				if (result instanceof ITextSelection)
					return asStructuredSelection((ITextSelection)result, flags);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
			return StructuredSelection.EMPTY;
		}
	}

	/**
	 * Returns the current selection. Does not return <code>null</code>, but the empty selection
	 * in case no selected element could be found.
	 * For text selections the element referenced at the current position is taken.
	 */
	public IStructuredSelection getSelection() {
		return getSelection(FLAGS_DO_CODERESOLVE);
	}
	
	/**
	 * Returns the current selection. Does not return <code>null</code>, but the empty selection
	 * in case no selected element could be found.
	 * @param flags Defines how text selections should be processed. FLAGS_DO_CODERESOLVE or
	 * FLAGS_DO_ELEMENT_AT_OFFSET,  FLAGS_GET_EDITOR_INPUT are valid options.
	 */
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

