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
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.textmanipulation.*;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;


public class JavaAddElementFromHistory extends JavaHistoryAction {
	
	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.compare.AddFromHistoryAction"; //$NON-NLS-1$
	
	public JavaAddElementFromHistory() {
		super(true);
	}
	
	public void run(ISelection selection) {
		
		String errorTitle= CompareMessages.getString("AddFromHistory.title"); //$NON-NLS-1$
		String errorMessage= CompareMessages.getString("AddFromHistory.internalErrorMessage"); //$NON-NLS-1$
		Shell shell= getShell();
		
		ICompilationUnit cu= null;
		IParent parent= null;
		IMember input= null;
		
		// analyse selection
		if (selection.isEmpty()) {
			// no selection: we try to use the editor's input
			JavaEditor editor= getEditor();
			if (editor != null) {
				IEditorInput editorInput= editor.getEditorInput();
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
			
				if (input != null) {
					parent= input;
					input= null;
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
		
			if (! buffer.makeCommittable(shell).isOK())
				return;
			
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
			
			CompilationUnit root= parsePartialCompilationUnit(cu2, 0, false);
			OldASTRewrite rewriter= new OldASTRewrite(root);
			List list= null;
			int pos= getIndex(root, input, list);
							
			ITypedElement[] results= d.getSelection();
			for (int i= 0; i < results.length; i++) {
				
			    // create an AST node
				ASTNode newNode= createASTNode(rewriter, results[i], buffer.getLineDelimiter());
				if (newNode == null) {
					MessageDialog.openError(shell, errorTitle, errorMessage);
					return;					
				}
				
				// now determine where to put the new node
				if (newNode instanceof PackageDeclaration) {
				    root.setPackage((PackageDeclaration) newNode);
				} else if (newNode instanceof ImportDeclaration) {
				    root.imports().add(newNode);
				} else {
				    if (list == null) {
						list= getContainerList(parent, root);
						if (list == null) {
							MessageDialog.openError(shell, errorTitle, errorMessage);
							return;					
						}
				    }
					if (pos < 0 || pos >= list.size()) {
						if (newNode instanceof BodyDeclaration) {
							pos= ASTNodes.getInsertionIndex((BodyDeclaration)newNode, list);
							list.add(pos, newNode);
						} else
							list.add(newNode);
					} else
						list.add(pos+1, newNode);
				}
				
				rewriter.markAsInserted(newNode);
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
	 * Creates a place holder ASTNode for the given element.
	 * @param rewriter
	 * @param element
	 * @param delimiter the line delimiter
	 * @return a ASTNode or null
	 * @throws CoreException
	 */
	private ASTNode createASTNode(OldASTRewrite rewriter, ITypedElement element, String delimiter) throws CoreException {
		if (element instanceof IStreamContentAccessor) {
			String content= JavaCompareUtilities.readString((IStreamContentAccessor)element);
			if (content != null) {
				content= trimTextBlock(content, delimiter);
				if (content != null) {
				    int type= getPlaceHolderType(element);
				    if (type != -1)
				        return rewriter.createStringPlaceholder(content, type);
				}
			}
		}
		return null;
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
			if (n != null) {
				BodyDeclaration parentNode= (BodyDeclaration)ASTNodes.getParent(n, BodyDeclaration.class);
				if (parentNode != null)
					return ((TypeDeclaration)parentNode).bodyDeclarations();
			}
			return null;
		}
			
		return null;
	}
	
	/**
	 * Returns the corresponding place holder type for the given element.
	 * @return a place holder type (see ASTRewrite) or -1 if there is no corresponding placeholder
	 */
	private int getPlaceHolderType(ITypedElement element) {
		
		if (element instanceof DocumentRangeNode) {
			JavaNode jn= (JavaNode) element;
			switch (jn.getTypeCode()) {
				
			case JavaNode.PACKAGE:
			    return ASTNode.PACKAGE_DECLARATION;

			case JavaNode.CLASS:
			case JavaNode.INTERFACE:
				return ASTNode.TYPE_DECLARATION;
				
			case JavaNode.CONSTRUCTOR:
			case JavaNode.METHOD:
				return ASTNode.METHOD_DECLARATION;
				
			case JavaNode.FIELD:
				return ASTNode.FIELD_DECLARATION;
				
			case JavaNode.INIT:
				return ASTNode.INITIALIZER;

			case JavaNode.IMPORT:
			case JavaNode.IMPORT_CONTAINER:
				return ASTNode.IMPORT_DECLARATION;

			case JavaNode.CU:
			    return ASTNode.COMPILATION_UNIT;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the given node within its container.
	 * If node is null -1 is returned.
	 * @param node
	 * @return the index of the given node or -1 if the node couldn't be found
	 */
	private int getIndex(CompilationUnit root, IMember node, List container) throws JavaModelException {
		
		if (node != null) {
			ISourceRange sourceRange= node.getNameRange();
			ASTNode n= NodeFinder.perform(root, sourceRange);
			if (n != null) {
				MethodDeclaration parentNode= (MethodDeclaration)ASTNodes.getParent(n, MethodDeclaration.class);
				if (parentNode != null)
					return container.indexOf(parentNode);
			}
		}
		
		// NeedWork: not yet implemented
		return -1;
	}
		
	protected boolean isEnabled(ISelection selection) {
		
		if (selection.isEmpty()) {
			JavaEditor editor= getEditor();
			if (editor != null) {
				// we check whether editor shows CompilationUnit
				IEditorInput editorInput= editor.getEditorInput();
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
