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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.core.*;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContext;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

public class StubUtility {
	
	
	public static class GenStubSettings extends CodeGenerationSettings {
	
		public boolean callSuper;
		public boolean methodOverwrites;
		public boolean noBody;
		public int methodModifiers;
		
		public GenStubSettings(CodeGenerationSettings settings) {
			settings.setSettings(this);
			methodModifiers= -1;	
		}
		
	}
		
	private static final String[] EMPTY= new String[0];
	
	/**
	 * Generates a method stub including the method comment. Given a template
	 * method, a stub with the same signature will be constructed so it can be
	 * added to a type. The method body will be empty or contain a return or
	 * super call.
	 * @param cu The cu to create the source for
	 * @param destTypeName The name of the type to which the method will be
	 * added to
	 * @param method A method template (method belongs to different type than the parent)
	 * @param definingType The type that defines the method.
	 * @param settings Options as defined above (<code>GenStubSettings</code>)
	 * @param imports Imports required by the stub are added to the imports structure. If imports structure is <code>null</code>
	 * all type names are qualified.
	 * @return The generated code
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	public static String genStub(ICompilationUnit cu, String destTypeName, IMethod method, IType definingType, GenStubSettings settings, IImportsStructure imports) throws CoreException {
		String methName= method.getElementName();
		String[] paramNames= suggestArgumentNames(method.getJavaProject(), method.getParameterNames());
		String returnType= method.isConstructor() ? null : method.getReturnType();
		String lineDelimiter= String.valueOf('\n'); // reformatting required
		
		
		StringBuffer buf= new StringBuffer();
		// add method comment
		if (settings.createComments && cu != null) {
			IMethod overridden= null;
			if (settings.methodOverwrites && returnType != null) {
				overridden= JavaModelUtil.findMethod(methName, method.getParameterTypes(), false, definingType.getMethods());
			}
			String comment= getMethodComment(cu, destTypeName, methName, paramNames, method.getExceptionTypes(), returnType, overridden, lineDelimiter);
			if (comment != null) {
				buf.append(comment);
			} else {
				buf.append("/**").append(lineDelimiter); //$NON-NLS-1$
				buf.append(" *").append(lineDelimiter); //$NON-NLS-1$
				buf.append(" */").append(lineDelimiter); //$NON-NLS-1$							
			}
			buf.append(lineDelimiter);
		}
		// add method declaration
		String bodyContent= null;
		if (!settings.noBody) {
			String bodyStatement= getDefaultMethodBodyStatement(methName, paramNames, returnType, settings.callSuper);
			bodyContent= getMethodBodyContent(returnType == null, method.getJavaProject(), destTypeName, methName, bodyStatement, lineDelimiter);
			if (bodyContent == null) {
				bodyContent= ""; //$NON-NLS-1$
			}
		}
		int flags= settings.methodModifiers;
		if (flags == -1) {
			flags= method.getFlags();
		}
		
		genMethodDeclaration(destTypeName, method, flags, bodyContent, imports, buf);
		return buf.toString();
	}

	/**
	 * Generates a method stub not including the method comment. Given a
	 * template method and the body content, a stub with the same signature will
	 * be constructed so it can be added to a type.
	 * @param destTypeName The name of the type to which the method will be
	 * added to
	 * @param method A method template (method belongs to different type than the parent)
	 * @param bodyContent Content of the body
	 * @param imports Imports required by the stub are added to the imports
	 * structure. If imports structure is <code>null</code> all type names are
	 * qualified.
	 * @param buf The buffer to append the gerenated code.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	public static void genMethodDeclaration(String destTypeName, IMethod method, String bodyContent, IImportsStructure imports, StringBuffer buf) throws CoreException {
		genMethodDeclaration(destTypeName, method, method.getFlags(), bodyContent, imports, buf);
	}
	
	/**
	 * Generates a method stub not including the method comment. Given a
	 * template method and the body content, a stub with the same signature will
	 * be constructed so it can be added to a type.
	 * @param destTypeName The name of the type to which the method will be
	 * added to
	 * @param method A method template (method belongs to different type than the parent)
	 * @param flags
	 * @param bodyContent Content of the body
	 * @param imports Imports required by the stub are added to the imports
	 * structure. If imports structure is <code>null</code> all type names are
	 * qualified.
	 * @param buf The buffer to append the gerenated code.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	public static void genMethodDeclaration(String destTypeName, IMethod method, int flags, String bodyContent, IImportsStructure imports, StringBuffer buf) throws CoreException {
		IType parentType= method.getDeclaringType();	
		String methodName= method.getElementName();
		String[] paramTypes= method.getParameterTypes();
		String[] paramNames= suggestArgumentNames(parentType.getJavaProject(), method.getParameterNames());

		String[] excTypes= method.getExceptionTypes();

		boolean isConstructor= method.isConstructor();
		String retTypeSig= isConstructor ? null : method.getReturnType();
		
		int lastParam= paramTypes.length -1;
		
		if (Flags.isPublic(flags) || (parentType.isInterface() && bodyContent != null)) {
			buf.append("public "); //$NON-NLS-1$
		} else if (Flags.isProtected(flags)) {
			buf.append("protected "); //$NON-NLS-1$
		} else if (Flags.isPrivate(flags)) {
			buf.append("private "); //$NON-NLS-1$
		}
		if (Flags.isSynchronized(flags)) {
			buf.append("synchronized "); //$NON-NLS-1$
		}		
		if (Flags.isVolatile(flags)) {
			buf.append("volatile "); //$NON-NLS-1$
		}
		if (Flags.isStrictfp(flags)) {
			buf.append("strictfp "); //$NON-NLS-1$
		}
		if (Flags.isStatic(flags)) {
			buf.append("static "); //$NON-NLS-1$
		}		
			
		if (isConstructor) {
			buf.append(destTypeName);
		} else {
			String retTypeFrm;
			if (!isPrimitiveType(retTypeSig)) {
				retTypeFrm= resolveAndAdd(retTypeSig, parentType, imports);
			} else {
				retTypeFrm= Signature.toString(retTypeSig);
			}
			buf.append(retTypeFrm);
			buf.append(' ');
			buf.append(methodName);
		}
		buf.append('(');
		for (int i= 0; i <= lastParam; i++) {
			String paramTypeSig= paramTypes[i];
			String paramTypeFrm;
			
			if (!isPrimitiveType(paramTypeSig)) {
				paramTypeFrm= resolveAndAdd(paramTypeSig, parentType, imports);
			} else {
				paramTypeFrm= Signature.toString(paramTypeSig);
			}
			buf.append(paramTypeFrm);
			buf.append(' ');
			buf.append(paramNames[i]);
			if (i < lastParam) {
				buf.append(", "); //$NON-NLS-1$
			}
		}
		buf.append(')');
		
		int lastExc= excTypes.length - 1;
		if (lastExc >= 0) {
			buf.append(" throws "); //$NON-NLS-1$
			for (int i= 0; i <= lastExc; i++) {
				String excTypeSig= excTypes[i];
				String excTypeFrm= resolveAndAdd(excTypeSig, parentType, imports);
				buf.append(excTypeFrm);
				if (i < lastExc) {
					buf.append(", "); //$NON-NLS-1$
				}
			}
		}
		if (bodyContent == null) {
			buf.append(";\n\n"); //$NON-NLS-1$
		} else {
			buf.append(" {\n\t"); //$NON-NLS-1$
			if ((bodyContent != null) && (bodyContent.length() > 0)) {
				buf.append(bodyContent);
				buf.append('\n');
			}
			buf.append("}\n");			 //$NON-NLS-1$
		}
	}
	
	public static String getDefaultMethodBodyStatement(String methodName, String[] paramNames, String retTypeSig, boolean callSuper) {
		StringBuffer buf= new StringBuffer();
		if (callSuper) {
			if (retTypeSig != null) {
				if (!Signature.SIG_VOID.equals(retTypeSig)) {
					buf.append("return "); //$NON-NLS-1$
				}
				buf.append("super."); //$NON-NLS-1$
				buf.append(methodName);
			} else {
				buf.append("super"); //$NON-NLS-1$
			}
			buf.append('(');			
			for (int i= 0; i < paramNames.length; i++) {
				if (i > 0) {
					buf.append(", "); //$NON-NLS-1$
				}
				buf.append(paramNames[i]);
			}
			buf.append(");"); //$NON-NLS-1$
		} else {
			if (retTypeSig != null && !retTypeSig.equals(Signature.SIG_VOID)) {
				if (!isPrimitiveType(retTypeSig) || Signature.getArrayCount(retTypeSig) > 0) {
					buf.append("return null;"); //$NON-NLS-1$
				} else if (retTypeSig.equals(Signature.SIG_BOOLEAN)) {
					buf.append("return false;"); //$NON-NLS-1$
				} else {
					buf.append("return 0;"); //$NON-NLS-1$
				}
			}			
		}
		return buf.toString();
	}	

	public static String getMethodBodyContent(boolean isConstructor, IJavaProject project, String destTypeName, String methodName, String bodyStatement, String lineDelimiter) throws CoreException {
		String templateName= isConstructor ? CodeTemplateContextType.CONSTRUCTORSTUB : CodeTemplateContextType.METHODSTUB;
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(templateName);
		if (template == null) {
			return bodyStatement;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.BODY_STATEMENT, bodyStatement);
		String str= evaluateTemplate(context, template);
		if (str == null && !Strings.containsOnlyWhitespaces(bodyStatement)) {
			return bodyStatement;
		}
		return str;
	}
	
	public static String getGetterMethodBodyContent(IJavaProject project, String destTypeName, String methodName, String fieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.GETTERSTUB;
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(templateName);
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		
		return evaluateTemplate(context, template);
	}
	
	public static String getSetterMethodBodyContent(IJavaProject project, String destTypeName, String methodName, String fieldName, String paramName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.SETTERSTUB;
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(templateName);
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, fieldName);
		context.setVariable(CodeTemplateContextType.PARAM, paramName);
		
		return evaluateTemplate(context, template);
	}
	
	public static String getCatchBodyContent(ICompilationUnit cu, String exceptionType, String variableName, String lineDelimiter) throws CoreException {
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.CATCHBLOCK);
		if (template == null) {
			return null;
		}

		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setVariable(CodeTemplateContextType.EXCEPTION_TYPE, exceptionType);
		context.setVariable(CodeTemplateContextType.EXCEPTION_VAR, variableName); //$NON-NLS-1$
		return evaluateTemplate(context, template);
	}		
	
	/*
	 * @see org.eclipse.jdt.ui.CodeGeneration#getTypeComment(ICompilationUnit, String, String)
	 */	
	public static String getCompilationUnitContent(ICompilationUnit cu, String typeComment, String typeContent, String lineDelimiter) throws CoreException {
		IPackageFragment pack= (IPackageFragment) cu.getParent();
		String packDecl= pack.isDefaultPackage() ? "" : "package " + pack.getElementName() + ';'; //$NON-NLS-1$ //$NON-NLS-2$

		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.NEWTYPE);
		if (template == null) {
			return null;
		}
		
		IJavaProject project= cu.getJavaProject();
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.PACKAGE_DECLARATION, packDecl);
		context.setVariable(CodeTemplateContextType.TYPE_COMMENT, typeComment != null ? typeComment : ""); //$NON-NLS-1$
		context.setVariable(CodeTemplateContextType.TYPE_DECLARATION, typeContent);
		context.setVariable(CodeTemplateContextType.TYPENAME, Signature.getQualifier(cu.getElementName()));
		return evaluateTemplate(context, template);
	}		

	/*
	 * @see org.eclipse.jdt.ui.CodeGeneration#getTypeComment(ICompilationUnit, String, String)
	 */		
	public static String getTypeComment(ICompilationUnit cu, String typeQualifiedName, String lineDelim) throws CoreException {
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.TYPECOMMENT);
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelim);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, Signature.getQualifier(typeQualifiedName));
		context.setVariable(CodeTemplateContextType.TYPENAME, Signature.getSimpleName(typeQualifiedName));
		return evaluateTemplate(context, template);
	}

	private static String[] getParameterTypesQualifiedNames(IMethodBinding binding) {
		ITypeBinding[] typeBindings= binding.getParameterTypes();
		String[] result= new String[typeBindings.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= typeBindings[i].getQualifiedName();
		}
		return result;
	}

	private static String getSeeTag(String declaringClassQualifiedName, String methodName, String[] parameterTypesQualifiedNames) {
		StringBuffer buf= new StringBuffer();
		buf.append("@see "); //$NON-NLS-1$
		buf.append(declaringClassQualifiedName);
		buf.append('#'); 
		buf.append(methodName);
		buf.append('(');
		for (int i= 0; i < parameterTypesQualifiedNames.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(parameterTypesQualifiedNames[i]);
		}
		buf.append(')');
		return buf.toString();
	}
	
	private static String getSeeTag(IMethod overridden) throws JavaModelException {
		IType declaringType= overridden.getDeclaringType();
		StringBuffer buf= new StringBuffer();
		buf.append("@see "); //$NON-NLS-1$
		buf.append(declaringType.getFullyQualifiedName('.'));
		buf.append('#'); 
		buf.append(overridden.getElementName());
		buf.append('(');
		String[] paramTypes= overridden.getParameterTypes();
		for (int i= 0; i < paramTypes.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			String curr= paramTypes[i];
			buf.append(JavaModelUtil.getResolvedTypeName(curr, declaringType));
			int arrayCount= Signature.getArrayCount(curr);
			while (arrayCount > 0) {
				buf.append("[]"); //$NON-NLS-1$
				arrayCount--;
			}
		}
		buf.append(')');
		return buf.toString();
	}
	
	/*
	 * @see org.eclipse.jdt.ui.CodeGeneration#getMethodComment(IMethod,IMethod,String)
	 */
	public static String getMethodComment(IMethod method, IMethod overridden, String lineDelimiter) throws CoreException {
		String retType= method.isConstructor() ? null : method.getReturnType();
		String[] paramNames= method.getParameterNames();
		
		return getMethodComment(method.getCompilationUnit(), method.getDeclaringType().getElementName(),
			method.getElementName(), paramNames, method.getExceptionTypes(), retType, overridden, lineDelimiter);
	}

	/*
	 * @see org.eclipse.jdt.ui.CodeGeneration#getMethodComment(ICompilationUnit, String, String, String[], String[], String, IMethod, String)
	 */
	public static String getMethodComment(ICompilationUnit cu, String typeName, String methodName, String[] paramNames, String[] excTypeSig, String retTypeSig, IMethod overridden, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.METHODCOMMENT;
		if (retTypeSig == null) {
			templateName= CodeTemplateContextType.CONSTRUCTORCOMMENT;
		} else if (overridden != null) {
			templateName= CodeTemplateContextType.OVERRIDECOMMENT;
		}
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(templateName);
		if (template == null) {
			return null;
		}		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
				
		if (retTypeSig != null) {
			context.setVariable(CodeTemplateContextType.RETURN_TYPE, Signature.toString(retTypeSig));
		}
		if (overridden != null) {
			context.setVariable(CodeTemplateContextType.SEE_TAG, getSeeTag(overridden));
		}
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null) {
			return null;
		}
		
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplateVariable position= findTagVariable(buffer); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}
			
		IDocument textBuffer= new Document(str);
		String[] exceptionNames= new String[excTypeSig.length];
		for (int i= 0; i < excTypeSig.length; i++) {
			exceptionNames[i]= Signature.toString(excTypeSig[i]);
		}
		String returnType= retTypeSig != null ? Signature.toString(retTypeSig) : null;
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(textBuffer, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, false, lineDelimiter);
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
		return textBuffer.get();
	}

	public static String getFieldComment(ICompilationUnit cu, String typeName, String fieldName, String lineDelimiter) throws CoreException {
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.FIELDCOMMENT);
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		
		return evaluateTemplate(context, template);
	}	
	
	
	/*
	 * @see org.eclipse.jdt.ui.CodeGeneration#getSetterComment(ICompilationUnit, String, String, String, String, String, String, String)
	 */
	public static String getSetterComment(ICompilationUnit cu, String typeName, String methodName, String fieldName, String fieldType, String paramName, String bareFieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.SETTERCOMMENT;
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(templateName);
		if (template == null) {
			return null;
		}
		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, fieldType);
		context.setVariable(CodeTemplateContextType.BARE_FIELD_NAME, bareFieldName);
		context.setVariable(CodeTemplateContextType.PARAM, paramName);

		return evaluateTemplate(context, template);
	}	
	
	/*
	 * @see org.eclipse.jdt.ui.CodeGeneration#getGetterComment(ICompilationUnit, String, String, String, String, String, String)
	 */
	public static String getGetterComment(ICompilationUnit cu, String typeName, String methodName, String fieldName, String fieldType, String bareFieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.GETTERCOMMENT;
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(templateName);
		if (template == null) {
			return null;
		}		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);
		context.setVariable(CodeTemplateContextType.FIELD_TYPE, fieldType);
		context.setVariable(CodeTemplateContextType.BARE_FIELD_NAME, bareFieldName);

		return evaluateTemplate(context, template);
	}
	
	private static String evaluateTemplate(CodeTemplateContext context, Template template) throws CoreException {
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null)
			return null;
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		return str;
	}

	/*
	 * @see org.eclipse.jdt.ui.CodeGeneration#getMethodComment(ICompilationUnit, String, MethodDeclaration, IMethodBinding, String)
	 */
	public static String getMethodComment(ICompilationUnit cu, String typeName, MethodDeclaration decl, IMethodBinding overridden, String lineDelimiter) throws CoreException {
		if (overridden != null) {
			String declaringClassQualifiedName= overridden.getDeclaringClass().getQualifiedName();
			String[] parameterTypesQualifiedNames= getParameterTypesQualifiedNames(overridden);			
			return getMethodComment(cu, typeName, decl, true, overridden.isDeprecated(), declaringClassQualifiedName, parameterTypesQualifiedNames, lineDelimiter);
		} else {
			return getMethodComment(cu, typeName, decl, false, false, null, null, lineDelimiter);
		}
	}
	
	/**
	 * Returns the comment for a method using the comment code templates.
	 * <code>null</code> is returned if the template is empty.
	 * @param cu The compilation unit to which the method belongs
	 * @param typeName Name of the type to which the method belongs.
	 * @param decl The AST MethodDeclaration node that will be added as new
	 * method.
	 * @param isOverridden <code>true</code> iff decl overrides another method
	 * @param isDeprecated <code>true</code> iff the method that decl overrides is deprecated.
	 * Note: it must not be <code>true</code> if isOverridden is <code>false</code>.
	 * @param declaringClassQualifiedName Fully qualified name of the type in which the overriddden 
	 * method (if any exists) in declared. If isOverridden is <code>false</code>, this is ignored.
	 * @param parameterTypesQualifiedNames Fully qualified names of parameter types of the type in which the overriddden 
	 * method (if any exists) in declared. If isOverridden is <code>false</code>, this is ignored.
	 * @param lineDelimiter The line delimiter to use
	 * @return String Returns the method comment or <code>null</code> if the
	 * configured template is empty. 
	 * (formatting required)
	 * @throws CoreException
	 */
	public static String getMethodComment(ICompilationUnit cu, String typeName, MethodDeclaration decl, boolean isOverridden, boolean isDeprecated, String declaringClassQualifiedName, String[] parameterTypesQualifiedNames, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.METHODCOMMENT;
		if (decl.isConstructor()) {
			templateName= CodeTemplateContextType.CONSTRUCTORCOMMENT;
		} else if (isOverridden) {
			templateName= CodeTemplateContextType.OVERRIDECOMMENT;
		}
		Template template= JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(templateName);
		if (template == null) {
			return null;
		}		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, decl.getName().getIdentifier());
		if (!decl.isConstructor()) {
			ASTNode returnType= (decl.getAST().apiLevel() == AST.JLS2) ? decl.getReturnType() : decl.getReturnType2();
			context.setVariable(CodeTemplateContextType.RETURN_TYPE, ASTNodes.asString(returnType));
		}
		if (isOverridden) {
			String methodName= decl.getName().getIdentifier();
			context.setVariable(CodeTemplateContextType.SEE_TAG, getSeeTag(declaringClassQualifiedName, methodName, parameterTypesQualifiedNames));
		}
		
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		} catch (TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null)
			return null;
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplateVariable position= findTagVariable(buffer);  // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}
			
		IDocument textBuffer= new Document(str);
		List params= decl.parameters();
		String[] paramNames= new String[params.size()];
		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= (SingleVariableDeclaration) params.get(i);
			paramNames[i]= elem.getName().getIdentifier();
		}
		List exceptions= decl.thrownExceptions();
		String[] exceptionNames= new String[exceptions.size()];
		for (int i= 0; i < exceptions.size(); i++) {
			exceptionNames[i]= ASTNodes.getSimpleNameIdentifier((Name) exceptions.get(i));
		}
		
		String returnType= null;
		if (!decl.isConstructor()) {
			returnType= ASTNodes.asString((decl.getAST().apiLevel() == AST.JLS2) ? decl.getReturnType() : decl.getReturnType2());
		}
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(textBuffer, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, isDeprecated, lineDelimiter);
			} catch (BadLocationException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
		return textBuffer.get();
	}
	
	private static TemplateVariable findTagVariable(TemplateBuffer buffer) {
		TemplateVariable[] positions= buffer.getVariables();
		for (int i= 0; i < positions.length; i++) {
			TemplateVariable curr= positions[i];
			if (CodeTemplateContextType.TAGS.equals(curr.getType())) {
				return curr;
			}
		}
		return null;		
	}	
	
	private static void insertTag(IDocument textBuffer, int offset, int length, String[] paramNames, String[] exceptionNames, String returnType, boolean isDeprecated, String lineDelimiter) throws BadLocationException {
		IRegion region= textBuffer.getLineInformationOfOffset(offset);
		if (region == null) {
			return;
		}
		String lineStart= textBuffer.get(region.getOffset(), offset - region.getOffset());
		
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < paramNames.length; i++) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter); buf.append(lineStart);
			}
			buf.append("@param "); buf.append(paramNames[i]); //$NON-NLS-1$
		}
		if (returnType != null && !returnType.equals("void")) { //$NON-NLS-1$
			if (buf.length() > 0) {
				buf.append(lineDelimiter); buf.append(lineStart);
			}			
			buf.append("@return"); //$NON-NLS-1$
		}
		if (exceptionNames != null) {
			for (int i= 0; i < exceptionNames.length; i++) {
				if (buf.length() > 0) {
					buf.append(lineDelimiter); buf.append(lineStart);
				}
				buf.append("@throws "); buf.append(exceptionNames[i]); //$NON-NLS-1$
			}
		}		
		if (isDeprecated) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter); buf.append(lineStart);
			}
			buf.append("@deprecated"); //$NON-NLS-1$
		}
		textBuffer.replace(offset, length, buf.toString());
	}
	
	private static boolean isPrimitiveType(String typeName) {
		char first= Signature.getElementType(typeName).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED && first != Signature.C_TYPE_VARIABLE);
	}

	private static String resolveAndAdd(String refTypeSig, IType declaringType, IImportsStructure imports) throws JavaModelException {
		int genericStart = refTypeSig.indexOf(Signature.C_GENERIC_START);
		if (genericStart != -1) {
			// generic type
			StringBuffer buf= new StringBuffer();
			
			String erasure= resolveAndAdd(Signature.getTypeErasure(refTypeSig), declaringType, imports);
			buf.append(erasure);
			buf.append('<');
			String[] typeArguments= Signature.getTypeArguments(refTypeSig);
			if (typeArguments.length > 0) {
				for (int i= 0; i < typeArguments.length; i++) {
					if (i > 0) {
						buf.append(", "); //$NON-NLS-1$
					}
					buf.append(resolveAndAdd(typeArguments[i], declaringType, imports));
				}
			}
			buf.append('>');
			return buf.toString();
		}
		if (refTypeSig.length() > 0) {
			switch (refTypeSig.charAt(0)) {
				case Signature.C_ARRAY :
					int arrayCount= Signature.getArrayCount(refTypeSig);
					StringBuffer buf= new StringBuffer();
					buf.append(resolveAndAdd(Signature.getElementType(refTypeSig), declaringType, imports));
					for (int i= 0; i < arrayCount; i++) {
						buf.append("[]"); //$NON-NLS-1$
					}
					return buf.toString();
				case Signature.C_RESOLVED:
				case Signature.C_UNRESOLVED:
					String resolvedTypeName= JavaModelUtil.getResolvedTypeName(refTypeSig, declaringType);
					if (resolvedTypeName != null) {
						if (imports == null) {
							return resolvedTypeName;
						} else {
							return imports.addImport(resolvedTypeName);
						}
					}
					break;
				case Signature.C_EXTENDS:
					return "? extends " + resolveAndAdd(refTypeSig.substring(1), declaringType, imports); //$NON-NLS-1$
				case Signature.C_SUPER:
					return "? super " + resolveAndAdd(refTypeSig.substring(1), declaringType, imports); //$NON-NLS-1$
			}
		}
		return Signature.toString(refTypeSig);
	}
		
	/**
	 * Finds a method in a list of methods.
	 * @param method
	 * @param allMethods
	 * @return The found method or null, if nothing found
	 * @throws JavaModelException
	 */
	private static IMethod findMethod(IMethod method, List allMethods) throws JavaModelException {
		String name= method.getElementName();
		String[] paramTypes= method.getParameterTypes();
		boolean isConstructor= method.isConstructor();

		for (int i= allMethods.size() - 1; i >= 0; i--) {
			IMethod curr= (IMethod) allMethods.get(i);
			if (JavaModelUtil.isSameMethodSignature(name, paramTypes, isConstructor, curr)) {
				return curr;
			}			
		}
		return null;
	}

	/**
	 * Creates needed constructors for a type.
	 * @param type The type to create constructors for
	 * @param supertype The type's super type
	 * @param settings Options for comment generation
	 * @param imports Required imports are added to the import structure. Structure can be <code>null</code>, types are qualified then.
	 * @return Returns the generated stubs or <code>null</code> if the creation has been canceled
	 * @throws CoreException
	 */
	public static String[] evalConstructors(IType type, IType supertype, CodeGenerationSettings settings, IImportsStructure imports) throws CoreException {
		IMethod[] superMethods= supertype.getMethods();
		String typeName= type.getElementName();
		ICompilationUnit cu= type.getCompilationUnit();
		IMethod[] methods= type.getMethods();
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.callSuper= true;
		
		ArrayList newMethods= new ArrayList(superMethods.length);
		for (int i= 0; i < superMethods.length; i++) {
			IMethod curr= superMethods[i];
			if (curr.isConstructor() && (JavaModelUtil.isVisibleInHierarchy(curr, type.getPackageFragment()))) {
				if (JavaModelUtil.findMethod(typeName, curr.getParameterTypes(), true, methods) == null) {
					genStubSettings.methodModifiers= Flags.AccPublic | JdtFlags.clearAccessModifiers(curr.getFlags());
					String newStub= genStub(cu, typeName, curr, curr.getDeclaringType(), genStubSettings, imports);
					newMethods.add(newStub);
				}
			}
		}
		return (String[]) newMethods.toArray(new String[newMethods.size()]);
	}
	
	/**
	 * Returns all unimplemented constructors of a type including root type default 
	 * constructors if there are no other superclass constructors unimplemented. 
	 * @param type The type to create constructors for
	 * @return Returns the generated stubs or <code>null</code> if the creation has been canceled
	 * @throws CoreException
	 */
	public static IMethod[] getOverridableConstructors(IType type) throws CoreException {
		List constructorMethods= new ArrayList();
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);				
		IType supertype= hierarchy.getSuperclass(type);
		if (supertype == null)
			return (new IMethod[0]);

		IMethod[] superMethods= supertype.getMethods();
		boolean constuctorFound= false;
		String typeName= type.getElementName();
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < superMethods.length; i++) {
				IMethod curr= superMethods[i];
				if (curr.isConstructor())  {
					constuctorFound= true;
					if (JavaModelUtil.isVisibleInHierarchy(curr, type.getPackageFragment()))
						if (JavaModelUtil.findMethod(typeName, curr.getParameterTypes(), true, methods) == null)
							constructorMethods.add(curr);
		
				}
		}
		
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=38487
		if (!constuctorFound)  {
			IType objectType= type.getJavaProject().findType("java.lang.Object"); //$NON-NLS-1$
			IMethod curr= objectType.getMethod("Object", EMPTY);  //$NON-NLS-1$
			if (JavaModelUtil.findMethod(typeName, curr.getParameterTypes(), true, methods) == null) {
				constructorMethods.add(curr);
			}
		}
		return (IMethod[]) constructorMethods.toArray(new IMethod[constructorMethods.size()]);
	}

	/**
	 * Returns all overridable methods of a type
	 * @param type The type to search the overridable methods for 
	 * @param hierarchy The type hierarchy of the type
 	 * @param isSubType If set, the result can include methods of the passed type, if not only methods from super
	 * types are considered
	 * @return Returns the all methods that can be overridden
	 * @throws JavaModelException
	 */
	public static IMethod[] getOverridableMethods(IType type, ITypeHierarchy hierarchy, boolean isSubType) throws JavaModelException {
		List allMethods= new ArrayList();

		IMethod[] typeMethods= type.getMethods();
		for (int i= 0; i < typeMethods.length; i++) {
			IMethod curr= typeMethods[i];
			if (!curr.isConstructor() && !Flags.isStatic(curr.getFlags()) && !Flags.isPrivate(curr.getFlags())) {
				allMethods.add(curr);
			}
		}

		IType[] superTypes= hierarchy.getAllSuperclasses(type);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod[] methods= superTypes[i].getMethods();
			for (int k= 0; k < methods.length; k++) {
				IMethod curr= methods[k];
				if (!curr.isConstructor() && !Flags.isStatic(curr.getFlags()) && !Flags.isPrivate(curr.getFlags())) {
					if (findMethod(curr, allMethods) == null) {
						allMethods.add(curr);
					}
				}
			}
		}

		IType[] superInterfaces= hierarchy.getAllSuperInterfaces(type);
		for (int i= 0; i < superInterfaces.length; i++) {
			IMethod[] methods= superInterfaces[i].getMethods();
			for (int k= 0; k < methods.length; k++) {
				IMethod curr= methods[k];

				// binary interfaces can contain static initializers (variable intializations)
				// 1G4CKUS
				if (!Flags.isStatic(curr.getFlags())) {
					IMethod impl= findMethod(curr, allMethods);
					if (impl == null || !JavaModelUtil.isVisibleInHierarchy(impl, type.getPackageFragment()) || prefereInterfaceMethod(hierarchy, curr, impl)) {
						if (impl != null) {
							allMethods.remove(impl);
						}
						// implement an interface method when it does not exist in the hierarchy
						// or when it throws less exceptions that the implemented
						allMethods.add(curr);
					}
				}
			}
		}
		if (!isSubType) {
			allMethods.removeAll(Arrays.asList(typeMethods));
		}
		// remove finals
		for (int i= allMethods.size() - 1; i >= 0; i--) {
			IMethod curr= (IMethod) allMethods.get(i);
			if (Flags.isFinal(curr.getFlags())) {
				allMethods.remove(i);
			}
		}
		return (IMethod[]) allMethods.toArray(new IMethod[allMethods.size()]);
	}
	
	private static boolean prefereInterfaceMethod(ITypeHierarchy hierarchy, IMethod interfaceMethod, IMethod curr) throws JavaModelException {
		if (Flags.isFinal(curr.getFlags())) {
			return false;
		}
		IType interfaceType= interfaceMethod.getDeclaringType();
		IType[] interfaces= hierarchy.getAllSuperInterfaces(curr.getDeclaringType());
		for (int i= 0; i < interfaces.length; i++) {
			if (interfaces[i] == interfaceType) {
				return false;
			}
		}
		return curr.getExceptionTypes().length > interfaceMethod.getExceptionTypes().length;
	}
	
	/**
	 * Generate method stubs for methods to overrride
	 * @param type The type to search the overridable methods for 
	 * @param hierarchy The type hierarchy of the type
	 * @param methodsToImplement Methods to override or implement
	 * @param settings Options for comment generation
	 * @param imports Required imports are added to the import structure. Structure can be <code>null</code>, types are qualified then.
	 * @return Returns the generated stubs
	 * @throws CoreException
	 */
	public static String[] genOverrideStubs(IMethod[] methodsToImplement, IType type, ITypeHierarchy hierarchy, CodeGenerationSettings settings, IImportsStructure imports) throws CoreException {
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.methodOverwrites= true;
		ICompilationUnit cu= type.getCompilationUnit();
		String[] result= new String[methodsToImplement.length];
		for (int i= 0; i < methodsToImplement.length; i++) {
			IMethod curr= methodsToImplement[i];
			IMethod overrides= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (overrides != null) {
				genStubSettings.callSuper= true;
				curr= overrides;
			}
			genStubSettings.methodModifiers= curr.getFlags();
			IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (desc == null) {
				desc= curr;
			}
			result[i]= genStub(cu, type.getElementName(), curr, desc.getDeclaringType(), genStubSettings, imports);
		}
		return result;
	}
	/**
	 * Searches for unimplemented methods of a type.
	 * @param type
	 * @param hierarchy
	 * @param isSubType If set, the evaluation is for a subtype of the given type. If not set, the
	 * evaluation is for the type itself.
	 * @param settings Options for comment generation
	 * @param imports Required imports are added to the import structure. Structure can be <code>null</code>, types are qualified then.
	 * @return Returns the generated stubs or <code>null</code> if the creation has been canceled
	 * @throws CoreException
	 */
	public static String[] evalUnimplementedMethods(IType type, ITypeHierarchy hierarchy, boolean isSubType, CodeGenerationSettings settings, 
			IImportsStructure imports) throws CoreException {
					
		IMethod[] inheritedMethods= getOverridableMethods(type, hierarchy, isSubType);
		
		List toImplement= new ArrayList();
		for (int i= 0; i < inheritedMethods.length; i++) {
			IMethod curr= inheritedMethods[i];
			if (JdtFlags.isAbstract(curr)) {
				toImplement.add(curr);
			}
		}
		IMethod[] toImplementArray= (IMethod[]) toImplement.toArray(new IMethod[toImplement.size()]);		
		return genOverrideStubs(toImplementArray, type, hierarchy, settings, imports);
	}

	/**
	 * Examines a string and returns the first line delimiter found.
	 * @param elem
	 * @return
	 * @throws JavaModelException
	 */
	public static String getLineDelimiterUsed(IJavaElement elem) throws JavaModelException {
		ICompilationUnit cu= (ICompilationUnit) elem.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (cu != null && cu.exists()) {
			IBuffer buf= cu.getBuffer();
			int length= buf.getLength();
			for (int i= 0; i < length; i++) {
				char ch= buf.getChar(i);
				if (ch == SWT.CR) {
					if (i + 1 < length) {
						if (buf.getChar(i + 1) == SWT.LF) {
							return "\r\n"; //$NON-NLS-1$
						}
					}
					return "\r"; //$NON-NLS-1$
				} else if (ch == SWT.LF) {
					return "\n"; //$NON-NLS-1$
				}
			}
		}
		return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Embodies the policy which line delimiter to use when inserting into
	 * a document.
	 * @param doc
	 * @return The line delimiter
	 */	
	public static String getLineDelimiterFor(IDocument doc) {
		// new for: 1GF5UU0: ITPJUI:WIN2000 - "Organize Imports" in java editor inserts lines in wrong format
		String lineDelim= null;
		try {
			lineDelim= doc.getLineDelimiter(0);
		} catch (BadLocationException e) {
		}
		if (lineDelim == null) {
			String systemDelimiter= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			String[] lineDelims= doc.getLegalLineDelimiters();
			for (int i= 0; i < lineDelims.length; i++) {
				if (lineDelims[i].equals(systemDelimiter)) {
					lineDelim= systemDelimiter;
					break;
				}
			}
			if (lineDelim == null) {
				lineDelim= lineDelims.length > 0 ? lineDelims[0] : systemDelimiter;
			}
		}
		return lineDelim;
	}


	/**
	 * Evaluates the indention used by a Java element. (in tabulators)
	 * @param elem
	 * @return
	 * @throws JavaModelException
	 */	
	public static int getIndentUsed(IJavaElement elem) throws JavaModelException {
		if (elem instanceof ISourceReference) {
			ICompilationUnit cu= (ICompilationUnit) elem.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				IBuffer buf= cu.getBuffer();
				int offset= ((ISourceReference)elem).getSourceRange().getOffset();
				int i= offset;
				// find beginning of line
				while (i > 0 && !Strings.isLineDelimiterChar(buf.getChar(i - 1)) ){
					i--;
				}
				return Strings.computeIndent(buf.getText(i, offset - i), CodeFormatterUtil.getTabWidth());
			}
		}
		return 0;
	}
		
	/**
	 * Returns the element after the give element.
	 * @param member
	 * @return
	 * @throws JavaModelException
	 */
	public static IJavaElement findNextSibling(IJavaElement member) throws JavaModelException {
		IJavaElement parent= member.getParent();
		if (parent instanceof IParent) {
			IJavaElement[] elements= ((IParent)parent).getChildren();
			for (int i= elements.length - 2; i >= 0 ; i--) {
				if (member.equals(elements[i])) {
					return elements[i+1];
				}
			}
		}
		return null;
	}
	
	public static String getTodoTaskTag(IJavaProject project) {
		String markers= null;
		if (project == null) {
			markers= JavaCore.getOption(JavaCore.COMPILER_TASK_TAGS);
		} else {
			markers= project.getOption(JavaCore.COMPILER_TASK_TAGS, true);
		}
		
		if (markers != null && markers.length() > 0) {
			int idx= markers.indexOf(',');
			if (idx == -1) {
				return markers;
			} else {
				return markers.substring(0, idx);
			}
		}
		return null;
	}
	
	/*
	 * Workarounds for bug 38111
	 */
	public static String[] getArgumentNameSuggestions(IJavaProject project, String baseName, int dimensions, String[] excluded) {
		String name= workaround38111(baseName);
		String[] res= NamingConventions.suggestArgumentNames(project, "", name, dimensions, excluded); //$NON-NLS-1$
		return sortByLength(res); // longest first
	}
		 
	public static String[] getFieldNameSuggestions(IJavaProject project, String baseName, int dimensions, int modifiers, String[] excluded) {
		String name= workaround38111(baseName);
		String[] res= NamingConventions.suggestFieldNames(project, "", name, dimensions, modifiers, excluded); //$NON-NLS-1$
		return sortByLength(res); // longest first
	}
	
	public static String[] getLocalNameSuggestions(IJavaProject project, String baseName, int dimensions, String[] excluded) {
		String name= workaround38111(baseName);
		String[] res= NamingConventions.suggestLocalVariableNames(project, "", name, dimensions, excluded); //$NON-NLS-1$
		return sortByLength(res); // longest first
	}
	
	private static String[] sortByLength(String[] proposals) {
		Arrays.sort(proposals, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((String) o2).length() - ((String) o1).length();
			}
		});
		return proposals;
	}
	
	private static String workaround38111(String baseName) {
		if (BASE_TYPES.contains(baseName))
			return baseName;
		return Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
	}
	
	private static final List BASE_TYPES= Arrays.asList(
			new String[] {"boolean", "byte", "char", "double", "float", "int", "long", "short"});  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	public static String suggestArgumentName(IJavaProject project, String baseName, String[] excluded) {
		String[] argnames= getArgumentNameSuggestions(project, baseName, 0, excluded);
		if (argnames.length > 0) {
			return argnames[0];
		}
		return baseName;
	}
	
	public static String[] suggestArgumentNames(IJavaProject project, String[] paramNames) {
		String prefixes= project.getOption(JavaCore.CODEASSIST_ARGUMENT_PREFIXES, true);
		if (prefixes == null) {
			prefixes= ""; //$NON-NLS-1$
		}
		String suffixes= project.getOption(JavaCore.CODEASSIST_ARGUMENT_SUFFIXES, true);
		if (suffixes == null) {
			suffixes= ""; //$NON-NLS-1$
		}
		if (prefixes.length() + suffixes.length() == 0) {
			return paramNames;
		}
		
		String[] newNames= new String[paramNames.length];
		// Ensure that the codegeneration preferences are respected
		for (int i= 0; i < paramNames.length; i++) {
			String curr= paramNames[i];
			if (!hasPrefixOrSuffix(prefixes, suffixes, curr)) {
				newNames[i]= suggestArgumentName(project, paramNames[i], null);
			} else {
				newNames[i]= curr;
			}
		}
		return newNames;
	}
	
	public static boolean hasFieldName(IJavaProject project, String name) {
		String prefixes= project.getOption(JavaCore.CODEASSIST_FIELD_PREFIXES, true);
		String suffixes= project.getOption(JavaCore.CODEASSIST_FIELD_SUFFIXES, true);
		String staticPrefixes= project.getOption(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, true);
		String staticSuffixes= project.getOption(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, true);
		
		
		return hasPrefixOrSuffix(prefixes, suffixes, name) 
			|| hasPrefixOrSuffix(staticPrefixes, staticSuffixes, name);
	}
	
	public static boolean hasParameterName(IJavaProject project, String name) {
		String prefixes= project.getOption(JavaCore.CODEASSIST_ARGUMENT_PREFIXES, true);
		String suffixes= project.getOption(JavaCore.CODEASSIST_ARGUMENT_SUFFIXES, true);
		return hasPrefixOrSuffix(prefixes, suffixes, name);
	}
	
	public static boolean hasLocalVariableName(IJavaProject project, String name) {
		String prefixes= project.getOption(JavaCore.CODEASSIST_LOCAL_PREFIXES, true);
		String suffixes= project.getOption(JavaCore.CODEASSIST_LOCAL_SUFFIXES, true);
		return hasPrefixOrSuffix(prefixes, suffixes, name);
	}
	
	public static boolean hasConstantName(String name) {
		return Character.isUpperCase(name.charAt(0));
	}
	
	
	private static boolean hasPrefixOrSuffix(String prefixes, String suffixes, String name) {
		final String listSeparartor= ","; //$NON-NLS-1$

		StringTokenizer tok= new StringTokenizer(prefixes, listSeparartor);
		while (tok.hasMoreTokens()) {
			String curr= tok.nextToken();
			if (name.startsWith(curr)) {
				return true;
			}
		}

		tok= new StringTokenizer(suffixes, listSeparartor);
		while (tok.hasMoreTokens()) {
			String curr= tok.nextToken();
			if (name.endsWith(curr)) {
				return true;
			}
		}
		return false;
	}
}
