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

import java.util.List;
import java.util.ResourceBundle;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.IEditorInput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.textmanipulation.*;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.compare.*;


public class JavaAddElementFromHistory extends JavaHistoryAction {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.AddFromHistoryAction"; //$NON-NLS-1$
	
	private JavaEditor fEditor;

	public JavaAddElementFromHistory() {
		super(true);
	}
	
	public void run(IAction action) {
		
		String errorTitle= CompareMessages.getString("AddFromHistory.title"); //$NON-NLS-1$
		String errorMessage= CompareMessages.getString("AddFromHistory.internalErrorMessage"); //$NON-NLS-1$
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		// shell can be null; as a result error dialogs won't be parented
		
		ICompilationUnit cu= null;
		IParent parent= null;
		IMember input= null;
		
		// analyse selection
		ISelection selection= getSelection();
		if (selection.isEmpty()) {
			// no selection: we try to use the editor's input
			if (fEditor != null) {
				IEditorInput editorInput= fEditor.getEditorInput();
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				if (manager != null) {
					cu= manager.getWorkingCopy(editorInput);
					parent= cu;
				}
			}
		} else {
			input= getEditionElement(selection);
			if (input != null) {
				cu= input.getCompilationUnit();
			
				if (input instanceof IParent) {
					parent= (IParent)input;
					input= null;
				} else {
					IJavaElement parentElement= input.getParent();
					if (parentElement instanceof IParent)
						parent= (IParent)parentElement;
				}
			} else {
				if (selection instanceof IStructuredSelection) {
					Object o= ((IStructuredSelection)selection).getFirstElement();
					if (o instanceof ICompilationUnit) {
						cu= (ICompilationUnit) o;
						parent= cu;
					}
				}
			}
		}
		
		if (parent == null || cu == null) {
			String invalidSelectionMessage= CompareMessages.getString("AddFromHistory.invalidSelectionMessage"); //$NON-NLS-1$
			MessageDialog.openInformation(shell, errorTitle, invalidSelectionMessage);
			return;
		}
		
		IFile file= getFile(parent);
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
				
		boolean inEditor= beingEdited(file);
		if (inEditor) {
			parent= (IParent) getWorkingCopy((IJavaElement)parent);
			if (input != null)
				input= (IMember) getWorkingCopy(input);
		}

		// get a TextBuffer where to insert the text
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(file);
		
			// configure EditionSelectionDialog and let user select an edition
			ITypedElement target= new JavaTextBufferNode(buffer, inEditor);

			ITypedElement[] editions= buildEditions(target, file);
											
			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			d.setAddMode(true);
			d.setHelpContextId(IJavaHelpContextIds.ADD_ELEMENT_FROM_HISTORY_DIALOG);
			ITypedElement selected= d.selectEdition(target, editions, parent);
			if (selected == null)
				return;	// user cancel
								
			ICompilationUnit cu2= cu;
			if (parent instanceof IMember)
				cu2= ((IMember)parent).getCompilationUnit();
			
			CompilationUnit root= AST.parsePartialCompilationUnit(cu2, 0, false);
			
			ASTRewrite rewriter= new ASTRewrite(root);
			
			List list= getContainerList(parent, root);
			if (list == null) {
				MessageDialog.openError(shell, errorTitle, errorMessage);
				return;					
			}
			
			int pos= getIndex(input);
							
			ITypedElement[] results= d.getSelection();
			for (int i= 0; i < results.length; i++) {
				ITypedElement ti= results[i];
				if (!(ti instanceof IStreamContentAccessor))
					continue;
								
				InputStream is= ((IStreamContentAccessor)ti).getContents();
				String content= trimTextBlock(is, buffer.getLineDelimiter());
				if (content == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;
				}
				
				ASTNode n= rewriter.createPlaceholder(content, getPlaceHolderType(parent, content));
				if (pos < 0 && pos <= list.size())
					list.add(n);
				else
					list.add(pos, n);
				rewriter.markAsInserted(n);
			}
			
			applyChanges(rewriter, buffer, shell, inEditor);

	 	} catch(InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
			
		} catch(InterruptedException ex) {
			// shouldn't be called because is not cancable
			Assert.isTrue(false);
			
		} catch(CoreException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}
	
	/**
	 * Finds the corresponding ASTNode for the given container and returns 
	 * its list of children. This list can be used to add a new child nodes to the container.
	 * @param container the container for which to return the children list
	 * @param root the AST
	 * @return list of children or null
	 * @throws JavaModelException
	 */
	private List getContainerList(IParent container, CompilationUnit root) throws JavaModelException {
		
		if (container instanceof ICompilationUnit)
			return root.types();
			
		if (container instanceof IType) {
			ISourceRange sourceRange= ((IType)container).getNameRange();
			ASTNode n= NodeFinder.perform(root, sourceRange);
			BodyDeclaration parentNode= (BodyDeclaration)ASTNodes.getParent(n, BodyDeclaration.class);
			if (parentNode != null)
				return ((TypeDeclaration)parentNode).bodyDeclarations();
			return null;
		}
			
		return null;
	}
	
	/**
	 * Returns the corresponding place holder type for the given container.
	 * If the container can accept more than one type the type is determined 
	 * by analyzing the content.
	 * @param container
	 * @param container
	 * @return a place holder type (see ASTRewrite)
	 */
	private int getPlaceHolderType(IParent container, String content) {
		
		if (container instanceof ICompilationUnit)
			// since we cannot deal with import statements, the only place holder type is a TYPE_DECLARATION
			return ASTRewrite.TYPE_DECLARATION;
			
		if (container instanceof IType)
			return ASTRewrite.METHOD_DECLARATION;
			
		// cannot happen
		Assert.isTrue(false);
		return ASTRewrite.UNKNOWN;
	}

	private int getIndex(IMember node) {
		// NeedWork: not yet implemented
		return -1;
	}
		
	protected boolean isEnabled(ISelection selection) {
		
		if (selection.isEmpty()) {
			if (fEditor != null) {
				// we check whether editor shows CompilationUnit
				IEditorInput editorInput= fEditor.getEditorInput();
				IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
				return manager.getWorkingCopy(editorInput) != null;
			}
			return false;
		}
		
		if (selection instanceof IStructuredSelection) {
			Object o= ((IStructuredSelection)selection).getFirstElement();
			if (o instanceof ICompilationUnit)
				return true;
		}
		
		return super.isEnabled(selection);
	}
}
