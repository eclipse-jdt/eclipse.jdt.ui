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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

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

	private String createTypeComment(IType type, String lineDelimiter) throws CoreException {
		Template[] templates= Templates.getInstance().getTemplates();
		StringBuffer buf= new StringBuffer();
		String comment= null;
		for (int i= 0; i < templates.length; i++) {
			if ("typecomment".equals(templates[i].getName())) { //$NON-NLS-1$
				comment= JavaContext.evaluateTemplate(templates[i], type.getCompilationUnit(), type.getSourceRange().getOffset());
				break;
			}
		}
		if (comment == null || comment.length() == 0) {
			buf.append("/**\n"); //$NON-NLS-1$
			buf.append(" * "); //$NON-NLS-1$
			buf.append(CodeGenerationMessages.getString("AddJavaDocStubOperation.configure.message")); //$NON-NLS-1$
			buf.append('\n');
			buf.append(" */\n"); //$NON-NLS-1$
		} else {
			buf.append(comment);
			buf.append(lineDelimiter);
		}
		return buf.toString();
	}		
	
	private String createMethodComment(IMethod meth) throws JavaModelException {
		IType declaringType= meth.getDeclaringType();
		if (fLastTypeHierarchy == null || !fLastTypeHierarchy.getType().equals(declaringType)) {
			fLastTypeHierarchy= declaringType.newSupertypeHierarchy(null);
		}
		StringBuffer buf= new StringBuffer();
		IMethod inheritedMethod= JavaModelUtil.findMethodDeclarationInHierarchy(fLastTypeHierarchy, declaringType, meth.getElementName(), meth.getParameterTypes(), meth.isConstructor());
		if (inheritedMethod != null) {
			boolean nonJavaDocComments= fSettings.createNonJavadocComments;
			boolean isDeprecated= Flags.isDeprecated(inheritedMethod.getFlags());
			StubUtility.genJavaDocSeeTag(JavaModelUtil.getFullyQualifiedName(inheritedMethod.getDeclaringType()), inheritedMethod.getElementName(), inheritedMethod.getParameterTypes(), nonJavaDocComments, isDeprecated, buf);
		} else {
			String desc= "Method " + meth.getElementName(); //$NON-NLS-1$
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
			if (cu.isWorkingCopy()) {
				cu= (ICompilationUnit) cu.getOriginalElement();
			}
						
			IFile file= (IFile) cu.getUnderlyingResource();
			buffer= TextBuffer.acquire(file);				
			
			sortEntries(); // sort botton to top, so changing the document does not invalidate positions
			
			String lineDelim= buffer.getLineDelimiter();
			
			for (int i= 0; i < fMembers.length; i++) {
				IMember curr= fMembers[i];
				String comment= null;
				switch (curr.getElementType()) {
					case IJavaElement.TYPE:
						comment= createTypeComment((IType) curr, lineDelim);
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
					TextRegion region= buffer.getLineInformationOfOffset(curr.getSourceRange().getOffset());
					if (region != null) {
						TextRange range= new TextRange(region.getOffset(), 0);
						buffer.replace(range, formattedComment);
					}
				}
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
