/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;

/**
 * Add javadoc stubs to members. All members must belong to the same compilation unit.
 * If the parent type is open in an editor, be sure to pass over its working copy.
 */
public class AddJavaDocStubOperation implements IWorkspaceRunnable {
	
	private IMember[] fMembers;
	
	private ITypeHierarchy fLastTypeHierarchy;
	
	private CodeGenerationSettings fSettings;
	
	public AddJavaDocStubOperation(IMember[] members, CodeGenerationSettings settings) {
		super();
		fMembers= members;
		fSettings= settings;
	}

	private String createTypeComment(IType type) throws CoreException {
		return StubUtility.getTypeComment(type.getCompilationUnit(), type.getElementName());
	}		
	
	private String createMethodComment(IMethod meth) throws CoreException {
		ICompilationUnit cu= meth.getCompilationUnit();
		IType declaringType= meth.getDeclaringType();
		String methName= meth.getElementName();
		
		String returnType= null;
		IMethod overridden= null;
		if (!meth.isConstructor()) {
			ITypeHierarchy hierarchy= SuperTypeHierarchyCache.getTypeHierarchy(declaringType);
			overridden= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, methName, meth.getParameterTypes(), false);
			returnType= meth.getReturnType();
		}
		return StubUtility.getMethodComment(cu, declaringType.getElementName(), methName, meth.getParameterNames(), meth.getExceptionTypes(), returnType, overridden);
	}
	
	private String createFieldComment(IField field) throws JavaModelException {
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n"); //$NON-NLS-1$
		buf.append(" *\n"); //$NON-NLS-1$
		buf.append(" */\n"); //$NON-NLS-1$
		return buf.toString();			
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
			buffer= TextBuffer.acquire((IFile) JavaModelUtil.toOriginal(cu).getResource());				
			
			sortEntries(); // sort botton to top, so changing the document does not invalidate positions
			
			String lineDelim= buffer.getLineDelimiter();
			
			for (int i= 0; i < fMembers.length; i++) {
				IMember curr= fMembers[i];
				String comment= null;
				switch (curr.getElementType()) {
					case IJavaElement.TYPE:
						comment= createTypeComment((IType) curr);
						break;
					case IJavaElement.FIELD:
						comment= createFieldComment((IField) curr);	
						break;
					case IJavaElement.METHOD:
						comment= createMethodComment((IMethod) curr);
						break;
				}
				if (comment == null) {
					comment= "/**\n *\n **/";
				}
				int indent= StubUtility.getIndentUsed(curr);
				String formattedComment= StubUtility.codeFormat(comment + lineDelim, indent, lineDelim);
				int codeStart= 0;
				while (Strings.isIndentChar(formattedComment.charAt(codeStart))) {
					codeStart++;
				}
				String insertString= formattedComment.substring(codeStart) + formattedComment.substring(0, codeStart);
				TextRange range= new TextRange(curr.getSourceRange().getOffset(), 0);
				buffer.replace(range, insertString);

				monitor.worked(1);

			}				

		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
			fLastTypeHierarchy= null;			
			
			monitor.done();
		}
	}
		
}
