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

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import org.eclipse.compare.EditionSelectionDialog;
import org.eclipse.compare.HistoryItem;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.swt.widgets.Shell;


/**
 * Provides "Replace from local history" for Java elements.
 */
public class JavaReplaceWithEditionAction extends JavaHistoryAction {
				
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.ReplaceWithEditionAction"; //$NON-NLS-1$
	
	protected boolean fPrevious= false;

	
	public JavaReplaceWithEditionAction() {
		super(true);
	}
	
	public void run(ISelection selection) {
		
		String errorTitle= CompareMessages.getString("ReplaceFromHistory.title"); //$NON-NLS-1$
		String errorMessage= CompareMessages.getString("ReplaceFromHistory.internalErrorMessage"); //$NON-NLS-1$
		Shell shell= getShell();
		
		IMember input= getEditionElement(selection);
		if (input == null) {
			String invalidSelectionMessage= CompareMessages.getString("ReplaceFromHistory.invalidSelectionMessage"); //$NON-NLS-1$
			MessageDialog.openInformation(shell, errorTitle, invalidSelectionMessage);
			return;
		}
		
		IFile file= getFile(input);
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
										
		boolean inEditor= beingEdited(file);
		if (inEditor)
			input= (IMember) getWorkingCopy(input);

		// get a TextBuffer where to insert the text
		TextBuffer buffer= null;
		try {
			buffer= TextBuffer.acquire(file);
			
			if (! buffer.makeCommittable(shell).isOK())
				return;

			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			d.setHelpContextId(IJavaHelpContextIds.REPLACE_ELEMENT_WITH_HISTORY_DIALOG);
			
			ITypedElement target= new JavaTextBufferNode(buffer, inEditor);

			ITypedElement[] editions= buildEditions(target, file);

			ITypedElement ti= null;
			if (fPrevious) {
				ti= d.selectPreviousEdition(target, editions, input);
				if (ti == null) {
					MessageDialog.openInformation(shell, errorTitle, CompareMessages.getString("ReplaceFromHistory.parsingErrorMessage"));	//$NON-NLS-1$
					return;
				}
			} else
				ti= d.selectEdition(target, editions, input);
						
			if (ti instanceof IStreamContentAccessor) {
				
				String content= JavaCompareUtilities.readString((IStreamContentAccessor)ti);
				String newContent= trimTextBlock(content, buffer.getLineDelimiter());
				if (newContent == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;
				}
				
				CompilationUnit root= AST.parsePartialCompilationUnit(input.getCompilationUnit(), 0, false, null, null);
				BodyDeclaration node= (BodyDeclaration)ASTNodes.getParent(NodeFinder.perform(root, input.getNameRange()), BodyDeclaration.class);
				if (node == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;
				}
				
				ASTRewrite rewriter= new ASTRewrite(root);
				rewriter.markAsReplaced(node, rewriter.createPlaceholder(newContent, ASTRewrite.getPlaceholderType(node)));
				
				if (inEditor) {
					JavaEditor je= getEditor(file);
					if (je != null)
						je.setFocus();
				}
				
				applyChanges(rewriter, buffer, shell, inEditor);
				
			}
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
	
	protected ITypedElement[] buildEditions(ITypedElement target, IFile file, IFileState[] states) {
		ITypedElement[] editions= new ITypedElement[states.length+1];
		editions[0]= new ResourceNode(file);
		for (int i= 0; i < states.length; i++)
			editions[i+1]= new HistoryItem(target, states[i]);
		return editions;
	}
}
