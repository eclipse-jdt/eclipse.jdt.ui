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
package org.eclipse.jdt.internal.ui.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.text.edits.MultiTextEdit;

import org.eclipse.compare.HistoryItem;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Base class for the "Replace with local history"
 * and "Add from local history" actions.
 */
public abstract class JavaHistoryAction extends Action implements IActionDelegate { 
	
	/**
	 * Implements the IStreamContentAccessor and ITypedElement protocols
	 * for a TextBuffer.
	 */
	class JavaTextBufferNode implements ITypedElement, IEncodedStreamContentAccessor {
		
		private TextBuffer fBuffer;
		private boolean fInEditor;
		
		JavaTextBufferNode(TextBuffer buffer, boolean inEditor) {
			fBuffer= buffer;
			fInEditor= inEditor;
		}
		
		public String getName() {
			if (fInEditor)
				return CompareMessages.getString("Editor_Buffer"); //$NON-NLS-1$
			return CompareMessages.getString("Workspace_File"); //$NON-NLS-1$
		}
		
		public String getType() {
			return "java";	//$NON-NLS-1$
		}
		
		public Image getImage() {
			return null;
		}
		
		public InputStream getContents() {
			return new ByteArrayInputStream(JavaCompareUtilities.getBytes(fBuffer.getContent(), "UTF-16")); //$NON-NLS-1$
		}
		
		public String getCharset() {
			return "UTF-16"; //$NON-NLS-1$
		}
	}

	private boolean fModifiesFile;
	private ISelection fSelection;
	
	

	JavaHistoryAction(boolean modifiesFile) {
		fModifiesFile= modifiesFile;
	}
	
	ISelection getSelection() {
		return fSelection;
	}
		
	final IFile getFile(Object input) {
		// extract CU from input
		ICompilationUnit cu= null;
		if (input instanceof ICompilationUnit)
			cu= (ICompilationUnit) input;
		else if (input instanceof IMember)
			cu= ((IMember)input).getCompilationUnit();
			
		if (cu == null || !cu.exists())
			return null;
			
		// get to original CU
		cu= JavaModelUtil.toOriginal(cu);
			
		// find underlying file
		IFile file= (IFile) cu.getResource();
		if (file != null && file.exists())
			return file;
		return null;
	}
	
	final ITypedElement[] buildEditions(ITypedElement target, IFile file) {

		// setup array of editions
		IFileState[] states= null;		
		// add available editions
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
			JavaPlugin.log(ex);
		}
		
		int count= 1;
		if (states != null)
			count+= states.length;

		ITypedElement[] editions= new ITypedElement[count];
		editions[0]= new ResourceNode(file);
		if (states != null)
			for (int i= 0; i < states.length; i++)
				editions[i+1]= new HistoryItem(target, states[i]);
		return editions;
	}
	
	final Shell getShell() {
		if (fEditor != null)
			return fEditor.getEditorSite().getShell();
		return JavaPlugin.getActiveWorkbenchShell();
	}
	
	/**
	 * Tries to find the given element in a workingcopy.
	 */
	final IJavaElement getWorkingCopy(IJavaElement input) {
		// TODO: With new working copy story: original == working copy.
		// Note that the previous code could result in a reconcile as side effect. Should check if that
		// is still required.
		return input;
	}
	
	/**
	 * Returns true if the given file is open in an editor.
	 */
	final boolean beingEdited(IFile file) {
		IDocumentProvider dp= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		FileEditorInput input= new FileEditorInput(file);	
		return dp.getDocument(input) != null;
	}

	/**
	 * Returns an IMember or null.
	 */
	final IMember getEditionElement(ISelection selection) {
		
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o= ss.getFirstElement();
				if (o instanceof IMember) {
					IMember m= (IMember) o;
					if (m.exists() && !m.isBinary() && JavaStructureCreator.hasEdition(m))
						return m;
				}
			}
		}
		return null;
	}
	
	final boolean isEnabled(IFile file) {
		if (file == null || ! file.exists())
			return false;
		if (fModifiesFile) {
			// without validate/edit we would do this:
			//    return !file.isReadOnly();
			// with validate/edit we have to return true
			return true;
		}
		return true;
	}
	
	boolean isEnabled(ISelection selection) {
		IMember m= getEditionElement(selection);
		if (m == null)
			return false;
		IFile file= getFile(m);
		if (!isEnabled(file))
			return false;
		if (file != null && beingEdited(file))
			return getWorkingCopy(m) != null;
		return true;
	}
	
	void applyChanges(OldASTRewrite rewriter, final TextBuffer buffer, Shell shell, boolean inEditor) throws CoreException, InvocationTargetException, InterruptedException {

		MultiTextEdit edit= new MultiTextEdit();
		rewriter.rewriteNode(buffer, edit);
										
		IProgressMonitor nullProgressMonitor= new NullProgressMonitor();
				
		TextBufferEditor editor= new TextBufferEditor(buffer);
		editor.add(edit);
		editor.performEdits(nullProgressMonitor);
				
		IRunnableWithProgress r= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					TextBuffer.commitChanges(buffer, false, pm);
				} catch (CoreException ex) {
					throw new InvocationTargetException(ex);
				}
			}
		};


		if (inEditor) {
			// we don't show progress
			r.run(nullProgressMonitor);
		} else {
			PlatformUI.getWorkbench().getProgressService().run(true, false, r);
		}
	}

	static String trimTextBlock(String content, String delimiter) {
		if (content != null) {
			String[] lines= Strings.convertIntoLines(content);
			if (lines != null) {
				Strings.trimIndentation(lines, JavaCompareUtilities.getTabSize());
				return Strings.concatenate(lines, delimiter);
			}
		}
		return null;
	}
	
	final JavaEditor getEditor(IFile file) {
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getDirtyEditors();
				for (int z= 0; z < editors.length; z++) {
					IEditorPart ep= editors[z];
					if (ep instanceof JavaEditor)
						return (JavaEditor) ep;
				}
			}
		}
		return null;
	}

	/**
	 * Executes this action with the given selection.
	 */
	public abstract void run(ISelection selection);

	//---- Action
	
	private JavaEditor fEditor;
	private String fTitle;
	private String fMessage;

	void init(JavaEditor editor, String text, String title, String message) {
		Assert.isNotNull(editor);
		Assert.isNotNull(title);
		Assert.isNotNull(message);
		fEditor= editor;
		fTitle= title;
		fMessage= message;
		setText(text);
		setEnabled(checkEnabled());
	}
	
	final JavaEditor getEditor() {
		return fEditor;
	}

	final public void run() {
		
		// this run is called from Editor
		IJavaElement element= null;
		try {
			element= SelectionConverter.getElementAtOffset(fEditor);
		} catch (JavaModelException e) {
			// ignored
		}
		fSelection= element != null
						? new StructuredSelection(element)
						: StructuredSelection.EMPTY;
		boolean isEnabled= isEnabled(fSelection);
		setEnabled(isEnabled);
		
		if (!isEnabled) {
			MessageDialog.openInformation(getShell(), fTitle, fMessage);
			return;
		}
		run(fSelection);
	}

	private boolean checkEnabled() {
		ICompilationUnit unit= SelectionConverter.getInputAsCompilationUnit(fEditor);
		IFile file= getFile(unit);
		return isEnabled(file);
	}	

	final public void update() {
		setEnabled(checkEnabled());
	}
	
 	//---- IActionDelegate
	
	final public void selectionChanged(IAction uiProxy, ISelection selection) {
		fSelection= selection;
		uiProxy.setEnabled(isEnabled(selection));
	}
	
	final public void run(IAction action) {
		run(fSelection);
	}
	
	static CompilationUnit parsePartialCompilationUnit(
		ICompilationUnit unit,
		int position,
		boolean resolveBindings) {
				
		if (unit == null) {
			throw new IllegalArgumentException();
		}
		try {
			ASTParser c= ASTParser.newParser(AST.JLS2);
			c.setSource(unit);
			c.setFocalPosition(position);
			c.setResolveBindings(resolveBindings);
			c.setWorkingCopyOwner(null);
			ASTNode result= c.createAST(null);
			return (CompilationUnit) result;
		} catch (IllegalStateException e) {
			// convert ASTParser's complaints into old form
			throw new IllegalArgumentException();
		}
	}
}
