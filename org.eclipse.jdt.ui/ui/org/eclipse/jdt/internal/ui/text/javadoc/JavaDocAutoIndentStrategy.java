package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationMessages;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * Auto indent strategy for java doc comments
 */
public class JavaDocAutoIndentStrategy extends DefaultAutoIndentStrategy {

	public JavaDocAutoIndentStrategy() {
	}

	/**
	 * Returns whether the text ends with one of the given search strings.
	 */
	private boolean endsWithDelimiter(IDocument d, String txt) {
		String[] delimiters= d.getLegalLineDelimiters();
		
		for (int i= 0; i < delimiters.length; i++) {
			if (txt.endsWith(delimiters[i]))
				return true;
		}
		
		return false;
	}

	/**
	 * Copies the indentation of the previous line and add a star.
	 * If the javadoc just started on this line add standard method tags
	 * and close the javadoc.
	 *
	 * @param d the document to work on
	 * @param c the command to deal with
	 */
	private void jdocIndentAfterNewLine(IDocument d, DocumentCommand c) {
		
		if (c.offset == -1 || d.getLength() == 0)
			return;
			
		try {
			// find start of line
			int p= (c.offset == d.getLength() ? c.offset  - 1 : c.offset);
			IRegion info= d.getLineInformationOfOffset(p);
			int start= info.getOffset();
				
			// find white spaces
			int end= findEndOfWhiteSpace(d, start, c.offset);
							
			StringBuffer buf= new StringBuffer(c.text);
			if (end >= start) {	// 1GEYL1R: ITPJUI:ALL - java doc edit smartness not work for class comments
				// append to input
				String indentation= d.get(start, end - start);
				buf.append(indentation);				
				if (end < c.offset) {
					if (d.getChar(end) == '/') {
						// javadoc started on this line
						buf.append(" * ");	 //$NON-NLS-1$

						if (isNewComment(d, c.offset)) {
							String lineDelimiter= d.getLegalLineDelimiters()[0];

							c.doit= false;
							d.replace(c.offset, 0, lineDelimiter + indentation + " */"); //$NON-NLS-1$
							
							// evaluate method signature
							ICompilationUnit unit= getCompilationUnit();
							if (unit != null) {
								try {
									unit.reconcile();
									String string= createJavaDocTags(d, c, indentation, lineDelimiter, unit);
									if (string != null)
										d.replace(c.offset, 0, string);						
								} catch (CoreException e) {
									// ignore
								}
							}
						}						

					} else if (d.getChar(end) == '*') {
						buf.append("* "); //$NON-NLS-1$
					}
				}			
			}
						
			c.text= buf.toString();
				
		} catch (BadLocationException excp) {
			// stop work
		}	
	}

	private String createJavaDocTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, ICompilationUnit unit)
		throws CoreException, BadLocationException
	{
		IJavaElement element= unit.getElementAt(command.offset);
		if (element == null)
			return null;

		switch (element.getElementType()) {
		case IJavaElement.TYPE:
			return createTypeTags(document, command, indentation, lineDelimiter, (IType) element);	

		case IJavaElement.METHOD:
			return createMethodTags(document, command, indentation, lineDelimiter, (IMethod) element);

		default:
			return null;
		}		
	}	

	private String createTypeTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, IType type)
		throws CoreException, BadLocationException
	{
		Template[] templates= Templates.getInstance().getTemplates();

		String comment= null;
		for (int i= 0; i < templates.length; i++) {
			if ("typecomment".equals(templates[i].getName())) { //$NON-NLS-1$
				comment= JavaContext.evaluateTemplate(templates[i], type.getCompilationUnit(), type.getSourceRange().getOffset());
				break;
			}
		}

		// trim comment start and end if any
		if (comment != null) {
			comment= comment.trim();
			if (comment.endsWith("*/")) //$NON-NLS-1$
				comment= comment.substring(0, comment.length() - 2);
			comment= comment.trim();
			if (comment.startsWith("/**")) //$NON-NLS-1$
				comment= comment.substring(3);
		}

		return (comment == null || comment.length() == 0)
			? CodeGenerationMessages.getString("AddJavaDocStubOperation.configure.message") //$NON-NLS-1$
			: comment;
	}
	
	private String createMethodTags(IDocument document, DocumentCommand command, String indentation, String lineDelimiter, IMethod method)
		throws BadLocationException, JavaModelException
	{
		IRegion partition= document.getPartition(command.offset);
		ISourceRange sourceRange= method.getSourceRange();
		if (sourceRange == null || sourceRange.getOffset() != partition.getOffset())
			return null;
				
		boolean isJavaDoc= partition.getLength() >= 3 && document.get(partition.getOffset(), 3).equals("/**"); //$NON-NLS-1$
			
		IMethod inheritedMethod= getInheritedMethod(method);
		if (inheritedMethod == null) {
			if (isJavaDoc)
				return createJavaDocMethodTags(method, lineDelimiter + indentation, ""); //$NON-NLS-1$
				
		} else {
			if (isJavaDoc || JavaPreferencesSettings.getCodeGenerationSettings().createNonJavadocComments)
				return createJavaDocInheritedMethodTags(inheritedMethod, lineDelimiter + indentation, ""); //$NON-NLS-1$
		}
		
		return null;
	}

	/**
	 * Returns the method inherited from, <code>null</code> if method is newly defined.
	 */
	private static IMethod getInheritedMethod(IMethod method) throws JavaModelException {
		IType declaringType= method.getDeclaringType();
		ITypeHierarchy typeHierarchy= declaringType.newSupertypeHierarchy(null);
		return JavaModelUtil.findMethodDeclarationInHierarchy(typeHierarchy, declaringType,
			method.getElementName(), method.getParameterTypes(), method.isConstructor());
	}
	
	protected void jdocIndentForCommentEnd(IDocument d, DocumentCommand c) {
		if (c.offset < 2 || d.getLength() == 0) {
			return;
		}
		try {
			if ("* ".equals(d.get(c.offset - 2, 2))) { //$NON-NLS-1$
				// modify document command
				c.length++;
				c.offset--;
			}					
		} catch (BadLocationException excp) {
			// stop work
		}
	}

	/**
	 * Guesses if the command operates within a newly created javadoc comment or not.
	 * If in doubt, it will assume that the javadoc is new.
	 */
	private static boolean isNewComment(IDocument document, int commandOffset) {

		try {
			int lineIndex= document.getLineOfOffset(commandOffset) + 1;
			if (lineIndex >= document.getNumberOfLines())
				return true;

			IRegion line= document.getLineInformation(lineIndex);
			ITypedRegion partition= document.getPartition(commandOffset);
			if (document.getLineOffset(lineIndex) >= partition.getOffset() + partition.getLength())
				return true;

			String string= document.get(line.getOffset(), line.getLength());				
			if (!string.trim().startsWith("*")) //$NON-NLS-1$
				return true;
			
			return false;
			
		} catch (BadLocationException e) {
			return false;
		}			
	}

	/*
	 * @see IAutoIndentStrategy#customizeDocumentCommand
	 */
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
		if (c.length == 0 && c.text != null && endsWithDelimiter(d, c.text))
			jdocIndentAfterNewLine(d, c);
		else if ("/".equals(c.text)) //$NON-NLS-1$
			jdocIndentForCommentEnd(d, c);			
	}

	/**
	 * Returns the compilation unit of the CompilationUnitEditor invoking the AutoIndentStrategy,
	 * might return <code>null</code> on error.
	 */
	private static ICompilationUnit getCompilationUnit() {

		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
			
		IWorkbenchPage page= window.getActivePage();
		if (page == null)	
			return null;

		IEditorPart editor= page.getActiveEditor();
		if (editor == null)
			return null;

		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();
		ICompilationUnit unit= manager.getWorkingCopy(editor.getEditorInput());
		if (unit == null)
			return null;

		return unit;
	}

	/**
	 * Creates tags for a newly declared or defined method.
	 */
	private static String createJavaDocMethodTags(IMethod method, String preFix, String postFix) throws JavaModelException {

		final StringBuffer buffer= new StringBuffer();

		final String[] parameterNames= method.getParameterNames();
		for (int i= 0; i < parameterNames.length; i++)
			appendJavaDocLine(buffer, preFix, "param", parameterNames[i], postFix); //$NON-NLS-1$

		final String returnType= method.getReturnType();
		if (returnType != null && !returnType.equals(Signature.SIG_VOID)) {
			String name= Signature.getSimpleName(Signature.toString(returnType));
			appendJavaDocLine(buffer, preFix, "return", name, postFix); //$NON-NLS-1$
		}

		final String[] exceptionTypes= method.getExceptionTypes();
		if (exceptionTypes != null) {
			for (int i= 0; i < exceptionTypes.length; i++) {
				String name= Signature.getSimpleName(Signature.toString(exceptionTypes[i]));
				appendJavaDocLine(buffer, preFix, "throws", name, postFix); //$NON-NLS-1$
			}
		}
		
		return buffer.toString();
	}

	/**
	 * Creates tags for an inherited method.
	 * 
	 * @param method the method it was inherited from.
	 */
	private static String createJavaDocInheritedMethodTags(IMethod method, String preFix, String postFix) throws JavaModelException {

		final StringBuffer buffer= new StringBuffer();
		
		appendJavaDocLine(buffer, preFix, "see", createSeeTagLink(method), postFix); //$NON-NLS-1$

		if (Flags.isDeprecated(method.getFlags()))
			appendJavaDocLine(buffer, preFix, "deprecated", "", postFix); //$NON-NLS-1$ //$NON-NLS-2$
			
		return buffer.toString();
	}
	
	/**
	 * Creates a see tag link string of the form type#method(arguments).
	 */
	private static String createSeeTagLink(IMethod method) {

		final StringBuffer buffer= new StringBuffer();	
		
		buffer.append(JavaModelUtil.getFullyQualifiedName(method.getDeclaringType()));
		buffer.append('#');
		buffer.append(method.getElementName());
		buffer.append('(');
		String[] parameterTypes= method.getParameterTypes();
		for (int i= 0; i < parameterTypes.length; i++) {
			if (i > 0)
				buffer.append(", "); //$NON-NLS-1$
			buffer.append(Signature.getSimpleName(Signature.toString(parameterTypes[i])));
		}
		buffer.append(')');

		return buffer.toString();
	}

	/**
	 * Appends a single javadoc tag line to the string buffer.
	 */
	private static void appendJavaDocLine(StringBuffer buffer, String preFix, String token, String name, String postFix) {
		buffer.append(preFix);
		buffer.append(" * "); //$NON-NLS-1$
		buffer.append('@');
		buffer.append(token);
		buffer.append(' ');
		buffer.append(name);
		buffer.append(postFix);		
	}
		
}
