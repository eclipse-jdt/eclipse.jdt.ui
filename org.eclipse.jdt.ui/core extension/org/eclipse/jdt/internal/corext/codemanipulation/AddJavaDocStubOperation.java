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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

/**
 * Add javadoc stubs to members. All members must belong to the same compilation unit.
 * If the parent type is open in an editor, be sure to pass over its working copy.
 */
public class AddJavaDocStubOperation implements IWorkspaceRunnable {
	
	private IMember[] fMembers;
	
	public AddJavaDocStubOperation(IMember[] members) {
		super();
		fMembers= members;
	}

	private String createTypeComment(IType type, String lineDelimiter) throws CoreException {
		return CodeGeneration.getTypeComment(type.getCompilationUnit(), type.getTypeQualifiedName('.'), lineDelimiter);
	}		
	
	private String createMethodComment(IMethod meth, String lineDelimiter) throws CoreException {
		IType declaringType= meth.getDeclaringType();
		
		IMethod overridden= null;
		if (!meth.isConstructor()) {
			ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(declaringType);
			overridden= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, meth.getElementName(), meth.getParameterTypes(), false);
		}
		return CodeGeneration.getMethodComment(meth, overridden, lineDelimiter);
	}
	
	private String createFieldComment(IField field, String lineDelimiter) throws JavaModelException, CoreException {
		String typeName= Signature.toString(field.getTypeSignature());
		String fieldName= field.getElementName();
		return CodeGeneration.getFieldComment(field.getCompilationUnit(), typeName, fieldName, String.valueOf('\n'));
	}		
	
	private void sortEntries() {
		Arrays.sort(fMembers, new Comparator() {
			public int compare(Object object1, Object object2) {
				try {
					return ((IMember)object2).getSourceRange().getOffset() - ((IMember)object1).getSourceRange().getOffset();
				} catch (JavaModelException e) {
					// ignore
				}
				return 0;
			}
		});	
	}

	/**
	 * Runs the operation.
	 * @throws OperationCanceledException Runtime error thrown when operation is cancelled.
	 */	
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		TextBuffer buffer= null;
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			monitor.beginTask(CodeGenerationMessages.getString("AddJavaDocStubOperation.description"), fMembers.length); //$NON-NLS-1$
			if (fMembers.length == 0) {
				return;
			}
			ICompilationUnit cu= fMembers[0].getCompilationUnit();
			buffer= TextBuffer.acquire((IFile) cu.getResource());				
			
			sortEntries(); // sort botton to top, so changing the document does not invalidate positions
			
			String lineDelim= buffer.getLineDelimiter();
			
			for (int i= 0; i < fMembers.length; i++) {
				IMember curr= fMembers[i];
				int memberStartOffset= curr.getSourceRange().getOffset();
				
				
				String comment= null;
				switch (curr.getElementType()) {
					case IJavaElement.TYPE:
						comment= createTypeComment((IType) curr, lineDelim);
						break;
					case IJavaElement.FIELD:
						comment= createFieldComment((IField) curr, lineDelim);	
						break;
					case IJavaElement.METHOD:
						comment= createMethodComment((IMethod) curr, lineDelim);
						break;
				}
				if (comment == null) {
					StringBuffer buf= new StringBuffer();
					buf.append("/**").append(lineDelim); //$NON-NLS-1$
					buf.append(" *").append(lineDelim); //$NON-NLS-1$
					buf.append(" */").append(lineDelim); //$NON-NLS-1$
					comment= buf.toString();						
				} else {
					if (!comment.endsWith(lineDelim)) {
						comment= comment + lineDelim;
					}
				}
				
				int tabWidth= CodeFormatterUtil.getTabWidth();
				
				String line= buffer.getLineContentOfOffset(memberStartOffset);
				String indentString= Strings.getIndentString(line, tabWidth);
				
				String indentedComment= Strings.changeIndent(comment, 0, tabWidth, indentString, lineDelim);

				String insertString= indentedComment;
				IRegion range= new Region(memberStartOffset, 0);
				buffer.replace(range, insertString);

				monitor.worked(1);

			}				

		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
			
			monitor.done();
		}
	}
		
}
