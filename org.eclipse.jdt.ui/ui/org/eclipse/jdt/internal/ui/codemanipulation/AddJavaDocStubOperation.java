/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.codemanipulation;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.DocumentManager;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

/**
 * Add javadoc stubs to members. All members must belong to the same compilation unit.
 * If the parent type is open in an editor, be sure to pass over its working copy.
 */
public class AddJavaDocStubOperation implements IWorkspaceRunnable {
	
	private IMember[] fMembers;
	
	private ITypeHierarchy fLastTypeHierarchy;
	
	public AddJavaDocStubOperation(IMember[] members) {
		super();
		fMembers= members;
	}


	private String createTypeComment(IType type) throws JavaModelException {
		// not yet supported
		return null;
	}
	
	private String createMethodComment(IMethod meth) throws JavaModelException {
		IType declaringType= meth.getDeclaringType();
		if (fLastTypeHierarchy == null || !fLastTypeHierarchy.getType().equals(declaringType)) {
			fLastTypeHierarchy= declaringType.newSupertypeHierarchy(null);
		}
		StringBuffer buf= new StringBuffer();
		IMethod inheritedMethod= JavaModelUtil.findMethodDeclarationInHierarchy(fLastTypeHierarchy, meth.getElementName(), meth.getParameterTypes(), meth.isConstructor());
		if (inheritedMethod != null) {
			StubUtility.genJavaDocSeeTag(inheritedMethod.getDeclaringType().getElementName(), inheritedMethod.getElementName(), inheritedMethod.getParameterTypes(), buf);
		} else {
			String desc= "Method " + meth.getElementName();
			StubUtility.genJavaDocStub(desc, meth.getParameterNames(), meth.getReturnType(), meth.getExceptionTypes(), buf);
		}
		return buf.toString();
	}
	
	private String createFieldComment(IField field) throws JavaModelException {
		// not yet supported
		return null;			
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
		try {
			if (monitor == null) {
				monitor= new NullProgressMonitor();
			}			
			
			monitor.beginTask(CodeManipulationMessages.getString("AddJavaDocStubOperation.description"), fMembers.length); //$NON-NLS-1$
			if (fMembers.length == 0) {
				return;
			}
			ICompilationUnit cu= fMembers[0].getCompilationUnit();
			
			DocumentManager docManager= new DocumentManager(cu);
			docManager.connect();				
			try {
				sortEntries(); // sort botton to top, so changing the document does not invalidate positions
				
				IDocument doc= docManager.getDocument();
				String lineDelim= StubUtility.getLineDelimiterFor(doc);
				
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
					if (comment != null) {
						int indent= StubUtility.getIndentUsed(curr);
						String formattedComment= StubUtility.codeFormat(comment, indent, lineDelim);
						int insertPosition= findInsertPosition(doc, curr.getSourceRange().getOffset());
						doc.replace(insertPosition, 0, formattedComment);
					}
					monitor.worked(1);
				}				
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			} finally {
				docManager.disconnect();
				fLastTypeHierarchy= null;
			}			
		} finally {
			monitor.done();
		}
	}
	
	private int findInsertPosition(IDocument doc, int memberOffset) throws BadLocationException {
		int i= memberOffset;
		if (i > 0) {
			char ch;
			do {
				i--;
				ch= doc.getChar(i);
			} while (Character.isWhitespace(ch) && ch != '\n' && ch != '\r');
		}
		return i + 1;
	}
	
}
