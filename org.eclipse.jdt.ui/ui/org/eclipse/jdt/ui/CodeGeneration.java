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
package org.eclipse.jdt.ui;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;

/**
 * Class that offers access to the templates contained in the 'code generation' preference page.
 * 
 * @since 2.1
 */
public class CodeGeneration {
	
	private CodeGeneration() {
	}
	
	/**
	 * Returns the content for a new compilation unit using the 'new Java file' code template.
	 * @param cu The compilation to create the source for. The compilation unit does not need to exist.
	 * @param typeComment The comment for the type to created. Used when the code template contains a ${typecomment} variable. Can be <code>null</code> if
	 * no comment should be added.
	 * @param typeContent The code of the type, including type declaration and body.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the template is undefined or empty.
	 * @throws CoreException
	 */
	public static String getCompilationUnitContent(ICompilationUnit cu, String typeComment, String typeContent, String lineDelimiter) throws CoreException {	
		return StubUtility.getCompilationUnitContent(cu, typeComment, typeContent, lineDelimiter);
	}
	
	/**
	 * Returns the content for a new type comment using the 'typecomment' code template. The returned content is unformatted and is not indented.
	 * @param cu The compilation where the type is contained. The compilation unit does not need to exist.
	 * @param typeQualifiedName The name of the type to which the comment is added. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.jdt.core.IType#getTypeQualifiedName(char)}.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the code template is undefined or empty. The returned content is unformatted and is not indented.
	 * @throws CoreException
	 */	
	public static String getTypeComment(ICompilationUnit cu, String typeQualifiedName, String lineDelimiter) throws CoreException {
		return StubUtility.getTypeComment(cu, typeQualifiedName, lineDelimiter);
	}

	/**
	 * Returns the content for a new field comment using the 'fieldcomment' code template. The returned content is unformatted and is not indented.
	 * @param cu The compilation where the type is contained. The compilation unit does not need to exist.
	 * @param typeName The name of the type of the field to which the comment is added.
	 * @param fieldName The name of the field to which the comment is added.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the new content or <code>null</code> if the code template is undefined or empty. The returned content is unformatted and is not indented.
	 * @throws CoreException
	 */	
	public static String getFieldComment(ICompilationUnit cu, String typeName, String fieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getFieldComment(cu, typeName, fieldName, lineDelimiter);
	}	
	
	/**
	 * Returns the comment for a method or constructor using the comment code templates (constructor / method / overriding method).
	 * <code>null</code> is returned if the template is empty.
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.jdt.core.IType#getTypeQualifiedName(char)}
	 * @param decl The MethodDeclaration AST node that will be added as new
	 * method. The node does not need to exist in an AST (no parent needed) and does not need to resolve.
	 * See {@link org.eclipse.jdt.core.dom.AST#newMethodDeclaration()} fo how to create such a node.
	 * @param overridden The binding of the method that will be overridden by the created
	 * method or <code>null</code> if no method is overridden.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the generated method comment or <code>null</code> if the
	 * code template is empty. The returned content is unformatted and not indented (formatting required).
	 * @throws CoreException
	 */
	public static String getMethodComment(ICompilationUnit cu, String declaringTypeName, MethodDeclaration decl, IMethodBinding overridden, String lineDelimiter) throws CoreException {
		return StubUtility.getMethodComment(cu, declaringTypeName, decl, overridden, lineDelimiter);
	}

	/**
	 * Returns the comment for a method or constructor using the comment code templates (constructor / method / overriding method).
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * <p>Exception types and return type are in signature notation. e.g. a source method declared as <code>public void foo(String text, int length)</code>
	 * would return the array <code>{"QString;","I"}</code> as parameter types. See {@link org.eclipse.jdt.core.Signature}
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated).
	 * @param methodName Name of the method.
	 * @param paramNames Names of the parameters for the method.
	 * @param excTypeSig Throwns exceptions (Signature notation)
	 * @param retTypeSig Return type (Signature notation) or <code>null</code>
	 * for constructors.
	 * @param overridden The method that will be overridden by the created method or
	 * <code>null</code> for non-overriding methods. If not <code>null</code>, the method must exist.
	 * @param lineDelimiter The line delimiter to be used
	 * @return Returns the constructed comment or <code>null</code> if
	 * the comment code template is empty. The returned content is unformatted and not indented (formatting required).
	 * @throws CoreException
	 */
	public static String getMethodComment(ICompilationUnit cu, String declaringTypeName, String methodName, String[] paramNames, String[] excTypeSig, String retTypeSig, IMethod overridden, String lineDelimiter) throws CoreException {
		return StubUtility.getMethodComment(cu, declaringTypeName, methodName, paramNames, excTypeSig, retTypeSig, overridden, lineDelimiter);
	}
		
	/**
	 * Returns the comment for a method or constructor using the comment code templates (constructor / method / overriding method).
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param method The method to be documented. The method must exist.
	 * @param overridden The method that will be overridden by the created method or
	 * <code>null</code> for non-overriding methods. If not <code>null</code>, the method must exist.
	 * @param lineDelimiter The line delimiter to be used
	 * @return Returns the constructed comment or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException 
	 */
	public static String getMethodComment(IMethod method, IMethod overridden, String lineDelimiter) throws CoreException {
		return StubUtility.getMethodComment(method, overridden, lineDelimiter);
	}	

	/**
	 * Returns the content of body for a method or constructor using the method body templates.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated).
	 * @param methodName Name of the method.
	 * @param isConstructor Defines if the created body is for a constructor
	 * @param bodyStatement The code to be entered at the place of the variable ${body_statement}. 
	 * @return Returns the constructed body contnet or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException
	 */	
	public static String getMethodBodyContent(ICompilationUnit cu, String declaringTypeName, String methodName, boolean isConstructor, String bodyStatement, String lineDelimiter) throws CoreException {
		return StubUtility.getMethodBodyContent(isConstructor, cu.getJavaProject(), declaringTypeName, methodName, bodyStatement, lineDelimiter);
	}
	
	/**
	 * Returns the content of body for a getter method using the getter method body template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated).
	 * @param methodName The name of the getter method.
	 * @param fieldName The name of the field to be set in the setter method, corresponding to the template variable for ${field}. 
	 * @return Returns the constructed body content or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException
	 */	
	public static String getGetterMethodBodyContent(ICompilationUnit cu, String declaringTypeName, String methodName, String fieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getGetterMethodBodyContent(cu.getJavaProject(), declaringTypeName, methodName, fieldName, lineDelimiter);
	}
	
	/**
	 * Returns the content of body for a setter method using the setter method body template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated).
	 * @param methodName The name of the setter method.
	 * @param fieldName The name of the field to be set in the setter method, corresponding to the template variable for ${field}.
	 * @param paramName The parameter passed to the setter method, corresponding to the template variable for $(param).
	 * @return Returns the constructed body content or <code>null</code> if
	 * the comment code template is empty. The returned string is unformatted and and has no indent (formatting required).
	 * @throws CoreException
	 */	
	public static String getSetterMethodBodyContent(ICompilationUnit cu, String declaringTypeName, String methodName, String fieldName, String paramName, String lineDelimiter) throws CoreException {
		return StubUtility.getSetterMethodBodyContent(cu.getJavaProject(), declaringTypeName, methodName, fieldName, paramName, lineDelimiter);
	}
	
	/**
	 * Returns the comment for a getter method using the getter comment template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.jdt.core.IType#getTypeQualifiedName(char)}
	 * @param methodName Name of the method.
	 * @param fieldName name of the field that is get.
	 * @param fieldType The type of the field that is to get.
	 * @param bareFieldName The field name without prefix or suffix.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the generated getter comment or <code>null</code> if the
	 * code template is empty. The returned content is not indented.
	 * @throws CoreException
	 * @since 3.0
	 */
	public static String getGetterComment(ICompilationUnit cu, String declaringTypeName, String methodName, String fieldName, String fieldType, String bareFieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getGetterComment(cu, declaringTypeName, methodName, fieldName, fieldType, bareFieldName, lineDelimiter);
	}
	
	/**
	 * Returns the comment for a setter method using the setter method body template.
	 * <code>null</code> is returned if the template is empty.
	 * <p>The returned string is unformatted and not indented.
	 * 
	 * @param cu The compilation unit to which the method belongs. The compilation unit does not need to exist.
	 * @param declaringTypeName Name of the type to which the method belongs. For inner types the name must be qualified and include the outer
	 * types names (dot separated). See {@link org.eclipse.jdt.core.IType#getTypeQualifiedName(char)}
	 * @param methodName Name of the method.
	 * @param fieldName name of the field that is set.
	 * @param fieldType The type of the field that is to set.
	 * @param paramName The name of the parameter that is set.
	 * @param bareFieldName The field name without prefix or suffix.
	 * @param lineDelimiter The line delimiter to be used.
	 * @return Returns the generated setter comment or <code>null</code> if the
	 * code template is empty. The returned comment is not indented.
	 * @throws CoreException
	 * @since 3.0
	 */
	public static String getSetterComment(ICompilationUnit cu, String declaringTypeName, String methodName, String fieldName, String fieldType, String paramName, String bareFieldName, String lineDelimiter) throws CoreException {
		return StubUtility.getSetterComment(cu, declaringTypeName, methodName, fieldName, fieldType, paramName, bareFieldName, lineDelimiter);
	}		
}
