/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CodeRefactoringUtil {

    public static RefactoringStatus checkMethodSyntaxErrors(int selectionStart, int selectionLength, CompilationUnit cuNode, String invalidSelectionMessage){
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(selectionStart, selectionLength), true);
		cuNode.accept(analyzer);
		ASTNode coveringNode= analyzer.getLastCoveringNode();
		if (! (coveringNode instanceof Block) || ! (coveringNode.getParent() instanceof MethodDeclaration))
			return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 
		if (ASTNodes.getMessages(coveringNode, ASTNodes.NODE_ONLY).length == 0)
			return RefactoringStatus.createFatalErrorStatus(invalidSelectionMessage); 

		MethodDeclaration methodDecl= (MethodDeclaration)coveringNode.getParent();
		String[] keys= {methodDecl.getName().getIdentifier()};
		String message= RefactoringCoreMessages.getFormattedString("CodeRefactoringUtil.error.message", keys); //$NON-NLS-1$
		return RefactoringStatus.createFatalErrorStatus(message);	
	}
	
	public static int getIndentationLevel(ASTNode node, IFile file) throws CoreException {
		try{
			FileBuffers.getTextFileBufferManager().connect(file.getFullPath(), new NullProgressMonitor());
			ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath());
			try {
				IRegion region= buffer.getDocument().getLineInformationOfOffset(node.getStartPosition());
				return Strings.computeIndent(buffer.getDocument().get(region.getOffset(), region.getLength()), CodeFormatterUtil.getTabWidth());
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			}
			return 0;
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(file.getFullPath(), new NullProgressMonitor());
		}
	}	

    private CodeRefactoringUtil() {}
}