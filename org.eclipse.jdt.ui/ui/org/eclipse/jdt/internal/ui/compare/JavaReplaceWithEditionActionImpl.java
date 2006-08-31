/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.compare.EditionSelectionDialog;
import org.eclipse.compare.HistoryItem;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.util.Resources;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


/**
 * Provides "Replace from local history" for Java elements.
 */
class JavaReplaceWithEditionActionImpl extends JavaHistoryActionImpl {
				
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.ReplaceWithEditionAction"; //$NON-NLS-1$
	
	protected boolean fPrevious= false;

	
	JavaReplaceWithEditionActionImpl(boolean previous) {
		super(true);
		fPrevious= previous;
	}
	
	public void run(ISelection selection) {
		
		String errorTitle= CompareMessages.ReplaceFromHistory_title; 
		String errorMessage= CompareMessages.ReplaceFromHistory_internalErrorMessage; 
		Shell shell= getShell();
		
		IMember input= getEditionElement(selection);
		if (input == null) {
			String invalidSelectionMessage= CompareMessages.ReplaceFromHistory_invalidSelectionMessage; 
			MessageDialog.openInformation(shell, errorTitle, invalidSelectionMessage);
			return;
		}
		
		IFile file= getFile(input);
		if (file == null) {
			MessageDialog.openError(shell, errorTitle, errorMessage);
			return;
		}
		
		
		IStatus status= Resources.makeCommittable(file, shell);
		if (!status.isOK()) {
			return;
		}
		
		boolean inEditor= beingEdited(file);

		// get the document where to insert the text
		IPath path= file.getFullPath();
		ITextFileBufferManager bufferManager= FileBuffers.getTextFileBufferManager();
		ITextFileBuffer textFileBuffer= null;
		try {
			bufferManager.connect(path, null);
			textFileBuffer= bufferManager.getTextFileBuffer(path);
			IDocument document= textFileBuffer.getDocument();

			ResourceBundle bundle= ResourceBundle.getBundle(BUNDLE_NAME);
			EditionSelectionDialog d= new EditionSelectionDialog(shell, bundle);
			d.setHelpContextId(IJavaHelpContextIds.REPLACE_ELEMENT_WITH_HISTORY_DIALOG);
			
			ITypedElement target= new JavaTextBufferNode(file, document, inEditor);

			ITypedElement[] editions= buildEditions(target, file);

			ITypedElement ti= null;
			if (fPrevious) {
				ti= d.selectPreviousEdition(target, editions, input);
				if (ti == null) {
					MessageDialog.openInformation(shell, errorTitle, CompareMessages.ReplaceFromHistory_parsingErrorMessage);	
					return;
				}
			} else
				ti= d.selectEdition(target, editions, input);
						
			if (ti instanceof IStreamContentAccessor) {
				
				String content= JavaCompareUtilities.readString((IStreamContentAccessor)ti);
				String newContent= trimTextBlock(content, TextUtilities.getDefaultLineDelimiter(document), input.getJavaProject());
				if (newContent == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;
				}
				
				ICompilationUnit compilationUnit= input.getCompilationUnit();
				CompilationUnit root= parsePartialCompilationUnit(compilationUnit);
				
				
				final ISourceRange nameRange= input.getNameRange();
				// workaround for bug in getNameRange(): for AnnotationMembers length is negative
				int length= nameRange.getLength();
				if (length < 0)
					length= 1;
				ASTNode node2= NodeFinder.perform(root, new SourceRange(nameRange.getOffset(), length));
				ASTNode node= ASTNodes.getParent(node2, BodyDeclaration.class);
				if (node == null)
					node= ASTNodes.getParent(node2, AnnotationTypeDeclaration.class);
				if (node == null)
					node= ASTNodes.getParent(node2, EnumDeclaration.class);
				
				//ASTNode node= getBodyContainer(root, input);
				if (node == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;
				}
				
				ASTRewrite rewriter= ASTRewrite.create(root.getAST());
				rewriter.replace(node, rewriter.createStringPlaceholder(newContent, node.getNodeType()), null);
				
				if (inEditor) {
					JavaEditor je= getEditor(file);
					if (je != null)
						je.setFocus();
				}
				
				Map options= null;
				IJavaProject javaProject= compilationUnit.getJavaProject();
				if (javaProject != null)
					options= javaProject.getOptions(true);
				applyChanges(rewriter, document, textFileBuffer, shell, inEditor, options);
				
			}
	 	} catch(InvocationTargetException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
			
		} catch(InterruptedException ex) {
			// shouldn't be called because is not cancelable
			Assert.isTrue(false);
			
		} catch(CoreException ex) {
			ExceptionHandler.handle(ex, shell, errorTitle, errorMessage);
			
		} finally {
			try {
				if (textFileBuffer != null)
					bufferManager.disconnect(path, null);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
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
