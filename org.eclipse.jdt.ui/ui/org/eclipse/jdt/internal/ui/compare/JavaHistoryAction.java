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

import java.io.*;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;

import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;

import org.eclipse.compare.*;

/**
 * Base class for the "Replace with local history"
 * and "Add from local history" actions.
 */
public abstract class JavaHistoryAction implements IActionDelegate { 
	
	/**
	 * Implements the IStreamContentAccessor and ITypedElement protocols
	 * for a TextBuffer.
	 */
	class JavaTextBufferNode implements ITypedElement, IStreamContentAccessor {
		
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
			return new ByteArrayInputStream(JavaCompareUtilities.getBytes(fBuffer.getContent()));
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
		
	protected IFile getFile(Object input) {
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
	
	protected ITypedElement[] buildEditions(ITypedElement target, IFile file) {

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
	
	/**
	 * Tries to find the given element in a workingcopy.
	 */
	protected IJavaElement getWorkingCopy(IJavaElement input) {
		try {
			return EditorUtility.getWorkingCopy(input, true);
		} catch (JavaModelException ex) {
			// NeedWork
		}
		return null;
	}
	
	/**
	 * Returns true if the given file is open in an editor.
	 */
	boolean beingEdited(IFile file) {
		CompilationUnitDocumentProvider dp= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		FileEditorInput input= new FileEditorInput(file);	
		return dp.getDocument(input) != null;
	}

	/**
	 * Returns an IMember or null.
	 */
	IMember getEditionElement(ISelection selection) {
		
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
	
	protected boolean isEnabled(IFile file) {
		if (file == null)
			return false;
		return !(fModifiesFile && file.isReadOnly());
	}
	
	protected boolean isEnabled(ISelection selection) {
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
	
	/**
	 * Notifies this action delegate that the selection in the workbench has changed.
	 * <p>
	 * Implementers can use this opportunity to change the availability of the
	 * action or to modify other presentation properties.
	 * </p>
	 *
	 * @param action the action proxy that handles presentation portion of the action
	 * @param selection the current selection in the workbench
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
		action.setEnabled(isEnabled(selection));
	}
	
	void applyChanges(ASTRewrite rewriter, final TextBuffer buffer, Shell shell, boolean inEditor) throws CoreException, InvocationTargetException, InterruptedException {
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
			ProgressMonitorDialog pd= new ProgressMonitorDialog(shell);
			pd.run(true, false, r);
		}
	}

	static String trimTextBlock(InputStream is, String delimiter) {
		String content= JavaCompareUtilities.readString(is);
		if (content != null) {
			String[] lines= Strings.convertIntoLines(content);
			if (lines != null) {
				Strings.trimIndentation(lines, JavaCompareUtilities.getTabSize());
				return Strings.concatenate(lines, delimiter);
			}
		}
		return null;
	}
}
