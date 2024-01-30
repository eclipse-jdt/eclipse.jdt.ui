/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Automated Name Recommendation For The Extract Local Variable Refactoring. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/601
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Context-based Automated Name Recommendation For The Extract Local Variable Refactoring. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/655
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Recommend variable name for Extracted Local Variable Refactoring when the extracted expression is a method invocation. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/684
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.templates.TemplatePersistenceData;
import org.eclipse.text.templates.TemplateStoreCore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.IndentManipulation;
import org.eclipse.jdt.core.manipulation.CodeGeneration;
import org.eclipse.jdt.core.manipulation.CodeStyleConfiguration;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;

import org.eclipse.jdt.internal.ui.util.ASTHelper;

/**
 * Implementations for {@link CodeGeneration} APIs, and other helper methods
 * to create source code stubs based on {@link IJavaElement}s.
 *
 * See StubUtility2
 * See JDTUIHelperClasses
 */
public class StubUtility {

	private static final String[] EMPTY= new String[0];

	private static final Set<String> VALID_TYPE_BODY_TEMPLATES;
	static {
		VALID_TYPE_BODY_TEMPLATES= new HashSet<>();
		VALID_TYPE_BODY_TEMPLATES.add(CodeTemplateContextType.CLASSBODY_ID);
		VALID_TYPE_BODY_TEMPLATES.add(CodeTemplateContextType.INTERFACEBODY_ID);
		VALID_TYPE_BODY_TEMPLATES.add(CodeTemplateContextType.ENUMBODY_ID);
		VALID_TYPE_BODY_TEMPLATES.add(CodeTemplateContextType.ANNOTATIONBODY_ID);
		VALID_TYPE_BODY_TEMPLATES.add(CodeTemplateContextType.RECORDBODY_ID);
	}

	//COPIED from org.eclipse.jdt.ui.PreferenceConstants
	public static final String CODEGEN_KEYWORD_THIS= "org.eclipse.jdt.ui.keywordthis"; //$NON-NLS-1$
	public static final String CODEGEN_IS_FOR_GETTERS= "org.eclipse.jdt.ui.gettersetter.use.is"; //$NON-NLS-1$
	public static final String CODEGEN_EXCEPTION_VAR_NAME= "org.eclipse.jdt.ui.exception.name"; //$NON-NLS-1$
	public static final String CODEGEN_ADD_COMMENTS= "org.eclipse.jdt.ui.javadoc"; //$NON-NLS-1$

	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getMethodBodyContent(boolean isConstructor, IJavaProject project, String destTypeName, String methodName, String bodyStatement, String lineDelimiter) throws CoreException {
		return getMethodBodyContent(false, isConstructor, project, destTypeName, methodName, bodyStatement, lineDelimiter);
	}

	public static String getMethodBodyContent(boolean useAlternativeMethodBody, boolean isConstructor, IJavaProject project, String destTypeName, String methodName, String bodyStatement, String lineDelimiter) throws CoreException {
		String templateName= isConstructor ? CodeTemplateContextType.CONSTRUCTORSTUB_ID
							: useAlternativeMethodBody ? CodeTemplateContextType.METHODSTUB_ALTERNATIVE_ID
							: CodeTemplateContextType.METHODSTUB_ID;
		Template template= getCodeTemplate(templateName, project);
		if (template == null) {
			return bodyStatement;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.BODY_STATEMENT, bodyStatement);
		String str= evaluateTemplate(context, template, new String[] { CodeTemplateContextType.BODY_STATEMENT });
		if (str == null && !Strings.containsOnlyWhitespaces(bodyStatement)) {
			return bodyStatement;
		}
		return str;
	}

	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getGetterMethodBodyContent(IJavaProject project, String destTypeName, String methodName, String fieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.GETTERSTUB_ID;
		Template template= getCodeTemplate(templateName, project);
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.FIELD, fieldName);

		return evaluateTemplate(context, template);
	}

	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getSetterMethodBodyContent(IJavaProject project, String destTypeName, String methodName, String fieldName, String paramName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.SETTERSTUB_ID;
		Template template= getCodeTemplate(templateName, project);
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

	public static String getCatchBodyContent(ICompilationUnit cu, String exceptionType, String variableName, ASTNode locationInAST, String lineDelimiter) throws CoreException {
		String enclosingType= ""; //$NON-NLS-1$
		String enclosingMethod= ""; //$NON-NLS-1$

		if (locationInAST != null) {
			MethodDeclaration parentMethod= ASTResolving.findParentMethodDeclaration(locationInAST);
			if (parentMethod != null) {
				enclosingMethod= parentMethod.getName().getIdentifier();
				locationInAST= parentMethod;
			}
			ASTNode parentType= ASTResolving.findParentType(locationInAST);
			if (parentType instanceof AbstractTypeDeclaration) {
				enclosingType= ((AbstractTypeDeclaration)parentType).getName().getIdentifier();
			}
		}
		return getCatchBodyContent(cu, exceptionType, variableName, enclosingType, enclosingMethod, lineDelimiter);
	}


	public static String getCatchBodyContent(ICompilationUnit cu, String exceptionType, String variableName, String enclosingType, String enclosingMethod, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, cu.getJavaProject());
		if (template == null) {
			return null;
		}

		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, enclosingType);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, enclosingMethod);
		context.setVariable(CodeTemplateContextType.EXCEPTION_TYPE, exceptionType);
		context.setVariable(CodeTemplateContextType.EXCEPTION_VAR, variableName);
		return evaluateTemplate(context, template);
	}

	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see CodeGeneration#getCompilationUnitContent(ICompilationUnit, String, String, String, String)
	 */
	public static String getCompilationUnitContent(ICompilationUnit cu, String fileComment, String typeComment, String typeContent, String lineDelimiter) throws CoreException {
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String packDecl= pack.isDefaultPackage() ? "" : "package " + pack.getElementName() + ';'; //$NON-NLS-1$ //$NON-NLS-2$
		return getCompilationUnitContent(cu, packDecl, fileComment, typeComment, typeContent, lineDelimiter);
	}

	public static String getCompilationUnitContent(ICompilationUnit cu, String packDecl, String fileComment, String typeComment, String typeContent, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, cu.getJavaProject());
		if (template == null) {
			return null;
		}

		IJavaProject project= cu.getJavaProject();
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.PACKAGE_DECLARATION, packDecl);
		context.setVariable(CodeTemplateContextType.TYPE_COMMENT, typeComment != null ? typeComment : ""); //$NON-NLS-1$
		context.setVariable(CodeTemplateContextType.FILE_COMMENT, fileComment != null ? fileComment : ""); //$NON-NLS-1$
		context.setVariable(CodeTemplateContextType.TYPE_DECLARATION, typeContent);
		context.setVariable(CodeTemplateContextType.TYPENAME, JavaCore.removeJavaLikeExtension(cu.getElementName()));

		String[] fullLine= { CodeTemplateContextType.PACKAGE_DECLARATION, CodeTemplateContextType.FILE_COMMENT, CodeTemplateContextType.TYPE_COMMENT };
		return evaluateTemplate(context, template, fullLine);
	}


	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see CodeGeneration#getFileComment(ICompilationUnit, String)
	 */
	public static String getFileComment(ICompilationUnit cu, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, cu.getJavaProject());
		if (template == null) {
			return null;
		}

		IJavaProject project= cu.getJavaProject();
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), project, lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.TYPENAME, JavaCore.removeJavaLikeExtension(cu.getElementName()));
		return evaluateTemplate(context, template);
	}

	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see CodeGeneration#getTypeComment(ICompilationUnit, String, String[], String)
	 */
	public static String getTypeComment(ICompilationUnit cu, String typeQualifiedName, String[] typeParameterNames, String[] params, String lineDelim) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, cu.getJavaProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelim);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, Signature.getQualifier(typeQualifiedName));
		context.setVariable(CodeTemplateContextType.TYPENAME, Signature.getSimpleName(typeQualifiedName));

		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException | TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}

		TemplateVariable position= findVariable(buffer, CodeTemplateContextType.TAGS); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}

		IDocument document= new Document(str);
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(document, tagOffsets[i], position.getLength(), params, EMPTY, null, typeParameterNames, false, lineDelim);
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e));
			}
		}
		return document.get();
	}

	/*
	 * Returns the parameters type names used in see tags. Currently, these are always fully qualified.
	 */
	public static String[] getParameterTypeNamesForSeeTag(IMethodBinding binding) {
		ITypeBinding[] typeBindings= binding.getParameterTypes();
		String[] result= new String[typeBindings.length];
		for (int i= 0; i < result.length; i++) {
			ITypeBinding curr= typeBindings[i];
			curr= curr.getErasure(); // Javadoc references use erased type
			result[i]= curr.getQualifiedName();
		}
		return result;
	}

	/*
	 * Returns the parameters type names used in see tags. Currently, these are always fully qualified.
	 */
	private static String[] getParameterTypeNamesForSeeTag(IMethod overridden) {
		try {
			ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setProject(overridden.getJavaProject());
			IBinding[] bindings= parser.createBindings(new IJavaElement[] { overridden }, null);
			if (bindings.length == 1 && bindings[0] instanceof IMethodBinding) {
				return getParameterTypeNamesForSeeTag((IMethodBinding)bindings[0]);
			}
		} catch (IllegalStateException e) {
			// method does not exist
		}
		// fall back code. Not good for generic methods!
		String[] paramTypes= overridden.getParameterTypes();
		String[] paramTypeNames= new String[paramTypes.length];
		for (int i= 0; i < paramTypes.length; i++) {
			paramTypeNames[i]= Signature.toString(Signature.getTypeErasure(paramTypes[i]));
		}
		return paramTypeNames;
	}

	private static String getSeeTag(String declaringClassQualifiedName, String methodName, String[] parameterTypesQualifiedNames) {
		StringBuilder buf= new StringBuilder();
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

	public static String[] getTypeParameterNames(ITypeParameter[] typeParameters) {
		String[] typeParametersNames= new String[typeParameters.length];
		for (int i= 0; i < typeParameters.length; i++) {
			typeParametersNames[i]= typeParameters[i].getElementName();
		}
		return typeParametersNames;
	}

	/**
	 * Don't use this method directly, use CodeGeneration.
	 *
	 * @param templateID the template id of the type body to get. Valid id's are
	 *            {@link CodeTemplateContextType#CLASSBODY_ID},
	 *            {@link CodeTemplateContextType#INTERFACEBODY_ID},
	 *            {@link CodeTemplateContextType#ENUMBODY_ID},
	 *            {@link CodeTemplateContextType#ANNOTATIONBODY_ID},
	 * @param cu the compilation unit to which the template is added
	 * @param typeName the type name
	 * @param lineDelim the line delimiter to use
	 * @return return the type body template or <code>null</code>
	 * @throws CoreException thrown if the template could not be evaluated
	 * See CodeGeneration#getTypeBody(String, ICompilationUnit, String, String)
	 */
	public static String getTypeBody(String templateID, ICompilationUnit cu, String typeName, String lineDelim) throws CoreException {
		if (!VALID_TYPE_BODY_TEMPLATES.contains(templateID)) {
			throw new IllegalArgumentException("Invalid code template ID: " + templateID); //$NON-NLS-1$
		}

		Template template= getCodeTemplate(templateID, cu.getJavaProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelim);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.TYPENAME, typeName);

		return evaluateTemplate(context, template);
	}

	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see CodeGeneration#getMethodComment(ICompilationUnit, String, String, String[], String[], String, String[], IMethod, String)
	 */
	public static String getMethodComment(ICompilationUnit cu, String typeName, String methodName, String[] paramNames, String[] excTypeSig, String retTypeSig, String[] typeParameterNames,
			IMethod target, boolean delegate, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.METHODCOMMENT_ID;
		if (retTypeSig == null) {
			templateName= CodeTemplateContextType.CONSTRUCTORCOMMENT_ID;
		} else if (target != null) {
			if (delegate)
				templateName= CodeTemplateContextType.DELEGATECOMMENT_ID;
			else
				templateName= CodeTemplateContextType.OVERRIDECOMMENT_ID;
		}
		Template template= getCodeTemplate(templateName, cu.getJavaProject());
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
		if (target != null) {
			String targetTypeName= target.getDeclaringType().getFullyQualifiedName('.');
			String[] targetParamTypeNames= getParameterTypeNamesForSeeTag(target);
			if (delegate)
				context.setVariable(CodeTemplateContextType.SEE_TO_TARGET_TAG, getSeeTag(targetTypeName, methodName, targetParamTypeNames));
			else
				context.setVariable(CodeTemplateContextType.SEE_TO_OVERRIDDEN_TAG, getSeeTag(targetTypeName, methodName, targetParamTypeNames));
		}
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException | TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null) {
			return null;
		}

		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplateVariable position= findVariable(buffer, CodeTemplateContextType.TAGS); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}

		IDocument document= new Document(str);
		String[] exceptionNames= new String[excTypeSig.length];
		for (int i= 0; i < excTypeSig.length; i++) {
			exceptionNames[i]= Signature.toString(excTypeSig[i]);
		}
		String returnType= retTypeSig != null ? Signature.toString(retTypeSig) : null;
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(document, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, typeParameterNames, false, lineDelimiter);
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e));
			}
		}
		return document.get();
	}

	// remove lines for empty variables
	private static String fixEmptyVariables(TemplateBuffer buffer, String[] variables) throws MalformedTreeException, BadLocationException {
		IDocument doc= new Document(buffer.getString());
		int nLines= doc.getNumberOfLines();
		MultiTextEdit edit= new MultiTextEdit();
		HashSet<Integer> removedLines= new HashSet<>();
		for (String variable : variables) {
			TemplateVariable position= findVariable(buffer, variable); // look if Javadoc tags have to be added
			if (position == null || position.getLength() > 0) {
				continue;
			}
			for (int offset2 : position.getOffsets()) {
				int line= doc.getLineOfOffset(offset2);
				IRegion lineInfo= doc.getLineInformation(line);
				int offset= lineInfo.getOffset();
				String str= doc.get(offset, lineInfo.getLength());
				if (Strings.containsOnlyWhitespaces(str) && nLines > line + 1 && removedLines.add(line)) {
					int nextStart= doc.getLineOffset(line + 1);
					edit.addChild(new DeleteEdit(offset, nextStart - offset));
				}
			}
		}
		edit.apply(doc, 0);
		return doc.get();
	}

	/*
	 * Don't use this method directly, use CodeGeneration.
	 * @see CodeGeneration#getModuleComment(IJavaProject, String, String, String[], String[], String[], String[], String[], String)
	 */
	public static String getModuleComment(ICompilationUnit cu, String moduleName, String[] providesNames,
			String[] usesNames, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.MODULECOMMENT_ID;
		Template template= getCodeTemplate(templateName, cu.getJavaProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_MODULE, moduleName);
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException | TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}

		TemplateVariable position= findVariable(buffer, CodeTemplateContextType.TAGS); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}

		IDocument document= new Document(str);
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertModuleTags(document, tagOffsets[i], position.getLength(), providesNames, usesNames,
						lineDelimiter);
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e));
			}
		}
		return document.get();
	}

	private static void insertModuleTags(IDocument textBuffer, int offset, int length, String[] providesNames,
			String[] usesNames, String lineDelimiter) throws BadLocationException {
		IRegion region= textBuffer.getLineInformationOfOffset(offset);
		if (region == null) {
			return;
		}
		String lineStart= textBuffer.get(region.getOffset(), offset - region.getOffset());

		StringBuilder buf= new StringBuilder();
		for (String providesName : providesNames) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@provides ").append(providesName); //$NON-NLS-1$
		}
		for (String usesName : usesNames) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@uses ").append(usesName); //$NON-NLS-1$
		}
		if (buf.length() == 0 && isAllCommentWhitespace(lineStart)) {
			int prevLine= textBuffer.getLineOfOffset(offset) - 1;
			if (prevLine > 0) {
				IRegion prevRegion= textBuffer.getLineInformation(prevLine);
				int prevLineEnd= prevRegion.getOffset() + prevRegion.getLength();
				// clear full line
				textBuffer.replace(prevLineEnd, offset + length - prevLineEnd, ""); //$NON-NLS-1$
				return;
			}
		}
		textBuffer.replace(offset, length, buf.toString());
	}

	/*
	 * Don't use this method directly, use CodeGeneration.
	 */
	public static String getFieldComment(ICompilationUnit cu, String typeName, String fieldName, String lineDelimiter) throws CoreException {
		Template template= getCodeTemplate(CodeTemplateContextType.FIELDCOMMENT_ID, cu.getJavaProject());
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
	 * Don't use this method directly, use CodeGeneration.
	 * @see CodeGeneration#getSetterComment(ICompilationUnit, String, String, String, String, String, String, String)
	 */
	public static String getSetterComment(ICompilationUnit cu, String typeName, String methodName, String fieldName, String fieldType, String paramName, String bareFieldName, String lineDelimiter)
			throws CoreException {
		String templateName= CodeTemplateContextType.SETTERCOMMENT_ID;
		Template template= getCodeTemplate(templateName, cu.getJavaProject());
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
	 * Don't use this method directly, use CodeGeneration.
	 * @see CodeGeneration#getGetterComment(ICompilationUnit, String, String, String, String, String, String)
	 */
	public static String getGetterComment(ICompilationUnit cu, String typeName, String methodName, String fieldName, String fieldType, String bareFieldName, String lineDelimiter) throws CoreException {
		String templateName= CodeTemplateContextType.GETTERCOMMENT_ID;
		Template template= getCodeTemplate(templateName, cu.getJavaProject());
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
		} catch (BadLocationException | TemplateException e) {
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

	private static String evaluateTemplate(CodeTemplateContext context, Template template, String[] fullLineVariables) throws CoreException {
		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
			if (buffer == null)
				return null;
			String str= fixEmptyVariables(buffer, fullLineVariables);
			if (Strings.containsOnlyWhitespaces(str)) {
				return null;
			}
			return str;
		} catch (BadLocationException | TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
	}


	/*
	 * Don't use this method directly, use CodeGeneration.
	 * This method should work with all AST levels.
	 * @see CodeGeneration#getMethodComment(ICompilationUnit, String, MethodDeclaration, boolean, String, String[], String)
	 */
	public static String getMethodComment(ICompilationUnit cu, String typeName, MethodDeclaration decl, boolean isDeprecated, String targetName, String targetMethodDeclaringTypeName,
			String[] targetMethodParameterTypeNames, boolean delegate, String lineDelimiter) throws CoreException {
		boolean needsTarget= targetMethodDeclaringTypeName != null && targetMethodParameterTypeNames != null;
		String templateName= CodeTemplateContextType.METHODCOMMENT_ID;
		if (decl.isConstructor()) {
			templateName= CodeTemplateContextType.CONSTRUCTORCOMMENT_ID;
		} else if (needsTarget) {
			if (delegate)
				templateName= CodeTemplateContextType.DELEGATECOMMENT_ID;
			else
				templateName= CodeTemplateContextType.OVERRIDECOMMENT_ID;
		}
		Template template= getCodeTemplate(templateName, cu.getJavaProject());
		if (template == null) {
			return null;
		}
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), cu.getJavaProject(), lineDelimiter);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, decl.getName().getIdentifier());
		if (!decl.isConstructor()) {
			ASTNode retType= getReturnType(decl);
			if (retType != null) {
				context.setVariable(CodeTemplateContextType.RETURN_TYPE, ASTNodes.asString(retType));
			} else {
				context.setVariable(CodeTemplateContextType.RETURN_TYPE, "void"); //$NON-NLS-1$
			}
		}
		if (needsTarget) {
			if (delegate)
				context.setVariable(CodeTemplateContextType.SEE_TO_TARGET_TAG, getSeeTag(targetMethodDeclaringTypeName, targetName, targetMethodParameterTypeNames));
			else
				context.setVariable(CodeTemplateContextType.SEE_TO_OVERRIDDEN_TAG, getSeeTag(targetMethodDeclaringTypeName, targetName, targetMethodParameterTypeNames));
		}

		TemplateBuffer buffer;
		try {
			buffer= context.evaluate(template);
		} catch (BadLocationException | TemplateException e) {
			throw new CoreException(Status.CANCEL_STATUS);
		}
		if (buffer == null)
			return null;
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplateVariable position= findVariable(buffer, CodeTemplateContextType.TAGS); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}

		IDocument textBuffer= new Document(str);
		List<TypeParameter> typeParams= decl.typeParameters();
		String[] typeParamNames= new String[typeParams.size()];
		for (int i= 0; i < typeParamNames.length; i++) {
			TypeParameter elem= typeParams.get(i);
			typeParamNames[i]= elem.getName().getIdentifier();
		}
		List<SingleVariableDeclaration> params= decl.parameters();
		String[] paramNames= new String[params.size()];
		for (int i= 0; i < paramNames.length; i++) {
			SingleVariableDeclaration elem= params.get(i);
			paramNames[i]= elem.getName().getIdentifier();
		}
		String[] exceptionNames= getExceptionNames(decl);

		String returnType= null;
		if (!decl.isConstructor()) {
			ASTNode retType= getReturnType(decl);
			if (retType != null) {
				returnType= ASTNodes.asString(retType);
			} else {
				returnType= "void"; //$NON-NLS-1$
			}
		}
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			try {
				insertTag(textBuffer, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, typeParamNames, isDeprecated, lineDelimiter);
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e));
			}
		}
		return textBuffer.get();
	}

	/**
	 * @param decl the method declaration
	 * @return the exception names
	 * @deprecated to avoid deprecation warnings
	 */
	@Deprecated
	private static String[] getExceptionNames(MethodDeclaration decl) {
		String[] exceptionNames;
		if (decl.getAST().apiLevel() >= ASTHelper.JLS8) {
			List<Type> exceptions= decl.thrownExceptionTypes();
			exceptionNames= new String[exceptions.size()];
			for (int i= 0; i < exceptionNames.length; i++) {
				exceptionNames[i]= ASTNodes.getTypeName(exceptions.get(i));
			}
		} else {
			List<Name> exceptions= decl.thrownExceptions();
			exceptionNames= new String[exceptions.size()];
			for (int i= 0; i < exceptionNames.length; i++) {
				exceptionNames[i]= ASTNodes.getSimpleNameIdentifier(exceptions.get(i));
			}
		}
		return exceptionNames;
	}

	public static boolean shouldGenerateMethodTypeParameterTags(IJavaProject project) {
		return JavaCore.ENABLED.equals(project.getOption(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_METHOD_TYPE_PARAMETERS, true));
	}

	/**
	 * @param decl the method declaration
	 * @return the return type
	 * @deprecated Deprecated to avoid deprecated warnings
	 */
	@Deprecated
	private static ASTNode getReturnType(MethodDeclaration decl) {
		// used from API, can't eliminate
		return decl.getAST().apiLevel() == ASTHelper.JLS2 ? decl.getReturnType() : decl.getReturnType2();
	}


	private static TemplateVariable findVariable(TemplateBuffer buffer, String variable) {
		for (TemplateVariable curr : buffer.getVariables()) {
			if (variable.equals(curr.getType())) {
				return curr;
			}
		}
		return null;
	}

	private static void insertTag(IDocument textBuffer, int offset, int length, String[] paramNames, String[] exceptionNames, String returnType, String[] typeParameterNames, boolean isDeprecated,
			String lineDelimiter) throws BadLocationException {
		IRegion region= textBuffer.getLineInformationOfOffset(offset);
		if (region == null) {
			return;
		}
		String lineStart= textBuffer.get(region.getOffset(), offset - region.getOffset());

		StringBuilder buf= new StringBuilder();
		for (String typeParameterName : typeParameterNames) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@param <").append(typeParameterName).append('>'); //$NON-NLS-1$
		}
		for (String paramName : paramNames) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@param ").append(paramName); //$NON-NLS-1$
		}
		if (returnType != null && !"void".equals(returnType)) { //$NON-NLS-1$
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@return"); //$NON-NLS-1$
		}
		if (exceptionNames != null) {
			for (String exceptionName : exceptionNames) {
				if (buf.length() > 0) {
					buf.append(lineDelimiter).append(lineStart);
				}
				buf.append("@throws ").append(exceptionName); //$NON-NLS-1$
			}
		}
		if (isDeprecated) {
			if (buf.length() > 0) {
				buf.append(lineDelimiter).append(lineStart);
			}
			buf.append("@deprecated"); //$NON-NLS-1$
		}
		if (buf.length() == 0 && isAllCommentWhitespace(lineStart)) {
			int prevLine= textBuffer.getLineOfOffset(offset) - 1;
			if (prevLine > 0) {
				IRegion prevRegion= textBuffer.getLineInformation(prevLine);
				int prevLineEnd= prevRegion.getOffset() + prevRegion.getLength();
				// clear full line
				textBuffer.replace(prevLineEnd, offset + length - prevLineEnd, ""); //$NON-NLS-1$
				return;
			}
		}
		textBuffer.replace(offset, length, buf.toString());
	}

	private static boolean isAllCommentWhitespace(String lineStart) {
		for (char ch : lineStart.toCharArray()) {
			if (!Character.isWhitespace(ch) && ch != '*') {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the line delimiter which is used in the specified project.
	 *
	 * @param project the java project, or <code>null</code>
	 * @return the used line delimiter
	 */
	public static String getLineDelimiterUsed(IJavaProject project) {
		return getProjectLineDelimiter(project);
	}

	private static String getProjectLineDelimiter(IJavaProject javaProject) {
		IProject project= null;
		if (javaProject != null)
			project= javaProject.getProject();

		String lineDelimiter= getLineDelimiterPreference(project);
		if (lineDelimiter != null)
			return lineDelimiter;

		return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static String getLineDelimiterPreference(IProject project) {
		IScopeContext[] scopeContext;
		if (project != null) {
			// project preference
			scopeContext= new IScopeContext[] { new ProjectScope(project) };
			String lineDelimiter= Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
			if (lineDelimiter != null)
				return lineDelimiter;
		}
		// workspace preference
		scopeContext= new IScopeContext[] { InstanceScope.INSTANCE };
		String platformDefault= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, platformDefault, scopeContext);
	}

	/**
	 * @param elem a Java element (doesn't have to exist)
	 * @return the existing or default line delimiter for the element
	 */
	public static String getLineDelimiterUsed(IJavaElement elem) {
		IOpenable openable= elem.getOpenable();
		if (openable instanceof ITypeRoot) {
			try {
				return openable.findRecommendedLineSeparator();
			} catch (JavaModelException exception) {
				// Use project setting
			}
		}
		IJavaProject project= elem.getJavaProject();
		return getProjectLineDelimiter(project.exists() ? project : null);
	}

	/**
	 * Evaluates the indentation used by a Java element. (in tabulators)
	 *
	 * @param elem the element to get the indent of
	 * @return return the indent unit
	 * @throws JavaModelException thrown if the element could not be accessed
	 */
	public static int getIndentUsed(IJavaElement elem) throws JavaModelException {
		IOpenable openable= elem.getOpenable();
		if (openable instanceof ITypeRoot) {
			IBuffer buf= openable.getBuffer();
			if (buf != null) {
				int offset= ((ISourceReference)elem).getSourceRange().getOffset();
				return getIndentUsed(buf, offset, elem.getJavaProject());
			}
		}
		return 0;
	}

	public static int getIndentUsed(IBuffer buffer, int offset, IJavaProject project) {
		int i= offset;
		// find beginning of line
		while (i > 0 && !IndentManipulation.isLineDelimiterChar(buffer.getChar(i - 1))) {
			i--;
		}
		return Strings.computeIndentUnits(buffer.getText(i, offset - i), project);
	}



	/**
	 * Returns the element after the give element.
	 *
	 * @param member a Java element
	 * @return the next sibling of the given element or <code>null</code>
	 * @throws JavaModelException thrown if the element could not be accessed
	 */
	public static IJavaElement findNextSibling(IJavaElement member) throws JavaModelException {
		IJavaElement parent= member.getParent();
		if (parent instanceof IParent) {
			IJavaElement[] elements= ((IParent)parent).getChildren();
			for (int i= elements.length - 2; i >= 0; i--) {
				if (member.equals(elements[i])) {
					return elements[i + 1];
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

	private static String removeTypeArguments(String baseName) {
		int idx= baseName.indexOf('<');
		if (idx != -1) {
			return baseName.substring(0, idx);
		}
		return baseName;
	}


	// --------------------------- name suggestions --------------------------

	public static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, ITypeBinding expectedType, Expression assignedExpression, Collection<String> excluded) {
		LinkedHashSet<String> res= new LinkedHashSet<>(); // avoid duplicates but keep order

		if (assignedExpression != null) {
			String nameFromExpression= getBaseNameFromExpression(project, assignedExpression, variableKind);
			if (nameFromExpression != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromExpression, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}

			String nameFromParent= getBaseNameFromLocationInParent(assignedExpression);
			if (nameFromParent != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromParent, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}
		}
		if (expectedType != null) {
			expectedType= Bindings.normalizeTypeBinding(expectedType);
			if (expectedType != null) {
				int dim= 0;
				if (expectedType.isArray()) {
					dim= expectedType.getDimensions();
					expectedType= expectedType.getElementType();
				}
				if (expectedType.isParameterizedType()) {
					expectedType= expectedType.getTypeDeclaration();
				}
				String typeName= expectedType.getName();
				if (typeName.length() > 0) {
					add(getVariableNameSuggestions(variableKind, project, typeName, dim, excluded, false), res);
				}
			}
		}
		if (res.isEmpty()) {
			return getDefaultVariableNameSuggestions(variableKind, excluded);
		}
		return res.toArray(new String[res.size()]);
	}

	public static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, ITypeBinding expectedType, Expression assignedExpression, Collection<String> excluded,
			String usedNameForIdenticalExpressionInCu, Collection<String> usedNamesForIdenticalExpressionInMethod) {
		LinkedHashSet<String> res= new LinkedHashSet<>(); // avoid duplicates but keep order

		String typeName= null;
		int dim= 0;
		if (expectedType != null) {
			expectedType= Bindings.normalizeTypeBinding(expectedType);
			if (expectedType != null) {
				if (expectedType.isArray()) {
					dim= expectedType.getDimensions();
					expectedType= expectedType.getElementType();
				}
				if (expectedType.isParameterizedType()) {
					expectedType= expectedType.getTypeDeclaration();
				}
				typeName= expectedType.getName();
			}
		}

		boolean isTypeAvailable= typeName != null && typeName.length() > 0;
		if (assignedExpression != null) {
			if (isTypeAvailable) {
				if (assignedExpression instanceof MethodInvocation) {
					// if we have a method invocation, see if we can recycle some previously used variable name that is assigned with the same expression.
					String recycledName= recycleNames(typeName, assignedExpression, excluded, usedNameForIdenticalExpressionInCu, usedNamesForIdenticalExpressionInMethod);
					if (recycledName != null) {
						add(getVariableNameSuggestions(variableKind, project, recycledName, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
					}
				}
			}

			String nameFromExpression= getBaseNameFromExpression(project, assignedExpression, variableKind);
			if (nameFromExpression != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromExpression, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}

			String nameFromParent= getBaseNameFromLocationInParent(assignedExpression);
			if (nameFromParent != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromParent, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}
		}

		if (isTypeAvailable) {
			add(getVariableNameSuggestions(variableKind, project, typeName, dim, excluded, false), res);
		}


		if (res.isEmpty()) {
			return getDefaultVariableNameSuggestions(variableKind, excluded);
		}
		return res.toArray(new String[res.size()]);
	}

	private static String recycleNames(String typeName, Expression assignedExpression, Collection<String> excluded,
			String usedNameForIdenticalExpressionInCu, Collection<String> usedNamesForIdenticalExpressionInMethod) {

		MethodInvocation methodInvocation= (MethodInvocation)assignedExpression;
		String name= methodInvocation.getName().getIdentifier();
		if (!name.toLowerCase().contains(typeName.toLowerCase())) {
			List<Expression> arguments= methodInvocation.arguments();
			List<Integer> argumentTypes= new ArrayList<>();
			for (Expression argument : arguments)
				argumentTypes.add(Integer.valueOf(argument.getNodeType()));
			if (arguments.size() > 1 || argumentTypes.contains(Integer.valueOf(ASTNode.METHOD_INVOCATION))) {
				if (usedNameForIdenticalExpressionInCu != null) {
					if (excluded.contains(usedNameForIdenticalExpressionInCu) || usedNamesForIdenticalExpressionInMethod.contains(usedNameForIdenticalExpressionInCu))
						return null;
					else
						return usedNameForIdenticalExpressionInCu;
				}
			}
		}

		return null;
	}



	public static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, Type expectedType, Expression assignedExpression, Collection<String> excluded) {
		LinkedHashSet<String> res= new LinkedHashSet<>(); // avoid duplicates but keep order

		if (assignedExpression != null) {
			String nameFromExpression= getBaseNameFromExpression(project, assignedExpression, variableKind);
			if (nameFromExpression != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromExpression, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}

			String nameFromParent= getBaseNameFromLocationInParent(assignedExpression);
			if (nameFromParent != null) {
				add(getVariableNameSuggestions(variableKind, project, nameFromParent, 0, excluded, false), res); // pass 0 as dimension, base name already contains plural.
			}
		}
		if (expectedType != null) {
			String[] names= getVariableNameSuggestions(variableKind, project, expectedType, excluded, false);
			res.addAll(Arrays.asList(names));
		}
		if (res.isEmpty()) {
			return getDefaultVariableNameSuggestions(variableKind, excluded);
		}
		return res.toArray(new String[res.size()]);
	}

	private static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, Type expectedType, Collection<String> excluded, boolean evaluateDefault) {
		int dim= 0;
		if (expectedType.isArrayType()) {
			ArrayType arrayType= (ArrayType)expectedType;
			dim= arrayType.getDimensions();
			expectedType= arrayType.getElementType();
		}
		if (expectedType.isParameterizedType()) {
			expectedType= ((ParameterizedType)expectedType).getType();
		}
		String typeName= ASTNodes.getTypeName(expectedType);

		if (typeName.length() > 0) {
			return getVariableNameSuggestions(variableKind, project, typeName, dim, excluded, evaluateDefault);
		}
		return EMPTY;
	}

	private static String[] getDefaultVariableNameSuggestions(int variableKind, Collection<String> excluded) {
		String prop= variableKind == NamingConventions.VK_STATIC_FINAL_FIELD ? "X" : "x"; //$NON-NLS-1$//$NON-NLS-2$
		String name= prop;
		int i= 1;
		while (excluded.contains(name)) {
			name= prop + i++;
		}
		return new String[] { name };
	}

	/**
	 * Returns variable name suggestions for the given base name. This is a layer over the JDT.Core
	 * NamingConventions API to fix its shortcomings. JDT UI code should only use this API.
	 *
	 * @param variableKind specifies what type the variable is: {@link NamingConventions#VK_LOCAL},
	 *            {@link NamingConventions#VK_PARAMETER}, {@link NamingConventions#VK_STATIC_FIELD},
	 *            {@link NamingConventions#VK_INSTANCE_FIELD}, or
	 *            {@link NamingConventions#VK_STATIC_FINAL_FIELD}.
	 * @param project the current project
	 * @param baseName the base name to make a suggestion on. The base name is expected to be a name
	 *            without any pre- or suffixes in singular form. Type name are accepted as well.
	 * @param dimensions if greater than 0, the resulting name will be in plural form
	 * @param excluded a collection containing all excluded names or <code>null</code> if no names
	 *            are excluded
	 * @param evaluateDefault if set, the result is guaranteed to contain at least one result. If
	 *            not, the result can be an empty array.
	 *
	 * @return the name suggestions sorted by relevance (best proposal first). If
	 *         <code>evaluateDefault</code> is set to true, the returned array is never empty. If
	 *         <code>evaluateDefault</code> is set to false, an empty array is returned if there is
	 *         no good suggestion for the given base name.
	 */
	public static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, String baseName, int dimensions, Collection<String> excluded, boolean evaluateDefault) {
		return NamingConventions.suggestVariableNames(variableKind, NamingConventions.BK_TYPE_NAME, removeTypeArguments(baseName), project, dimensions, getExcludedArray(excluded), evaluateDefault);
	}

	private static String[] getExcludedArray(Collection<String> excluded) {
		if (excluded == null) {
			return null;
		} else if (excluded instanceof ExcludedCollection) {
			return ((ExcludedCollection)excluded).getExcludedArray();
		}
		return excluded.toArray(new String[excluded.size()]);
	}


	private static final String[] KNOWN_METHOD_NAME_PREFIXES= { "get", "is", "to", "create", "load", "find",  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$//$NON-NLS-6$
			"build", "generate", "prepare", "parse", "current", "read", "resolve", "retrieve", "make", "add", "extract" }; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$//$NON-NLS-9$//$NON-NLS-10$//$NON-NLS-11$


	private static void add(String[] names, Set<String> result) {
		result.addAll(Arrays.asList(names));
	}

	private static String getBaseNameFromExpression(IJavaProject project, Expression assignedExpression, int variableKind) {
		String name= null;
		if (assignedExpression instanceof CastExpression) {
			assignedExpression= ((CastExpression)assignedExpression).getExpression();
		}
		if (assignedExpression instanceof Name) {
			Name simpleNode= (Name)assignedExpression;
			IBinding binding= simpleNode.resolveBinding();
			if (binding instanceof IVariableBinding)
				return getBaseName((IVariableBinding)binding, project);

			return ASTNodes.getSimpleNameIdentifier(simpleNode);
		} else if (assignedExpression instanceof MethodInvocation) {
			MethodInvocation methodInvocation= (MethodInvocation)assignedExpression;
			name= methodInvocation.getName().getIdentifier();
			if (name.equals("next")) { //$NON-NLS-1$
				Expression receiver= methodInvocation.getExpression();
				String modifiedName= getBaseNameFromReceiver(receiver);
				if (!modifiedName.equals("element")) //$NON-NLS-1$
					return modifiedName;
			}

		} else if (assignedExpression instanceof SuperMethodInvocation) {
			name= ((SuperMethodInvocation)assignedExpression).getName().getIdentifier();
		} else if (assignedExpression instanceof FieldAccess) {
			return ((FieldAccess)assignedExpression).getName().getIdentifier();
		} else if (variableKind == NamingConventions.VK_STATIC_FINAL_FIELD && (assignedExpression instanceof StringLiteral || assignedExpression instanceof NumberLiteral)) {
			String string= assignedExpression instanceof StringLiteral ? ((StringLiteral)assignedExpression).getLiteralValue() : ((NumberLiteral)assignedExpression).getToken();
			StringBuilder res= new StringBuilder();
			boolean needsUnderscore= false;
			for (char ch : string.toCharArray()) {
				if (Character.isJavaIdentifierPart(ch)) {
					if (res.length() == 0 && !Character.isJavaIdentifierStart(ch) || needsUnderscore) {
						res.append('_');
					}
					res.append(ch);
					needsUnderscore= false;
				} else {
					needsUnderscore= res.length() > 0;
				}
			}
			if (res.length() > 0) {
				return res.toString();
			}
		}
		if (name != null) {
			for (String curr : KNOWN_METHOD_NAME_PREFIXES) {
				if (name.startsWith(curr)) {
					if (name.equals(curr)) {
						return null; // don't suggest 'get' as variable name
					} else if (Character.isUpperCase(name.charAt(curr.length()))) {
						return name.substring(curr.length());
					}
				}
			}
		}
		return name;
	}

	private static String getBaseNameFromReceiver(Expression receiver) {
		if (receiver != null) {
			if (receiver instanceof SimpleName) {
				String receiverStr= receiver.toString();
				return ConvertLoopOperation.modifyBaseName(receiverStr);
			}
		}
		return "element"; //$NON-NLS-1$
	}

	private static String getBaseNameFromLocationInParent(Expression assignedExpression, List<Expression> arguments, IMethodBinding binding) {
		if (binding == null)
			return null;

		ITypeBinding[] parameterTypes= binding.getParameterTypes();
		if (parameterTypes.length != arguments.size()) // beware of guessed method bindings
			return null;

		int index= arguments.indexOf(assignedExpression);
		if (index == -1)
			return null;

		ITypeBinding expressionBinding= assignedExpression.resolveTypeBinding();
		if (expressionBinding != null && !expressionBinding.isAssignmentCompatible(parameterTypes[index]))
			return null;

		try {
			IJavaElement javaElement= binding.getJavaElement();
			if (javaElement instanceof IMethod) {
				IMethod method= (IMethod)javaElement;
				if (method.getOpenable().getBuffer() != null) { // avoid dummy names and lookup from Javadoc
					String[] parameterNames= method.getParameterNames();
					if (index < parameterNames.length) {
						return NamingConventions.getBaseName(NamingConventions.VK_PARAMETER, parameterNames[index], method.getJavaProject());
					}
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return null;
	}


	private static String getBaseNameFromLocationInParent(Expression assignedExpression) {
		StructuralPropertyDescriptor location= assignedExpression.getLocationInParent();
		if (location == MethodInvocation.ARGUMENTS_PROPERTY) {
			MethodInvocation parent= (MethodInvocation)assignedExpression.getParent();
			return getBaseNameFromLocationInParent(assignedExpression, parent.arguments(), parent.resolveMethodBinding());
		} else if (location == ClassInstanceCreation.ARGUMENTS_PROPERTY) {
			ClassInstanceCreation parent= (ClassInstanceCreation)assignedExpression.getParent();
			return getBaseNameFromLocationInParent(assignedExpression, parent.arguments(), parent.resolveConstructorBinding());
		} else if (location == SuperMethodInvocation.ARGUMENTS_PROPERTY) {
			SuperMethodInvocation parent= (SuperMethodInvocation)assignedExpression.getParent();
			return getBaseNameFromLocationInParent(assignedExpression, parent.arguments(), parent.resolveMethodBinding());
		} else if (location == ConstructorInvocation.ARGUMENTS_PROPERTY) {
			ConstructorInvocation parent= (ConstructorInvocation)assignedExpression.getParent();
			return getBaseNameFromLocationInParent(assignedExpression, parent.arguments(), parent.resolveConstructorBinding());
		} else if (location == SuperConstructorInvocation.ARGUMENTS_PROPERTY) {
			SuperConstructorInvocation parent= (SuperConstructorInvocation)assignedExpression.getParent();
			return getBaseNameFromLocationInParent(assignedExpression, parent.arguments(), parent.resolveConstructorBinding());
		}
		return null;
	}

	public static String[] getArgumentNameSuggestions(IType type, String[] excluded) {
		return getVariableNameSuggestions(NamingConventions.VK_PARAMETER, type.getJavaProject(), type.getElementName(), 0, new ExcludedCollection(excluded), true);
	}

	public static String[] getArgumentNameSuggestions(IJavaProject project, Type type, String[] excluded) {
		return getVariableNameSuggestions(NamingConventions.VK_PARAMETER, project, type, new ExcludedCollection(excluded), true);
	}

	public static String[] getArgumentNameSuggestions(IJavaProject project, ITypeBinding binding, String[] excluded) {
		return getVariableNameSuggestions(NamingConventions.VK_PARAMETER, project, binding, null, new ExcludedCollection(excluded));
	}

	public static String[] getArgumentNameSuggestions(IJavaProject project, String baseName, int dimensions, String[] excluded) {
		return getVariableNameSuggestions(NamingConventions.VK_PARAMETER, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}

	public static String[] getFieldNameSuggestions(IType type, int fieldModifiers, String[] excluded) {
		return getFieldNameSuggestions(type.getJavaProject(), type.getElementName(), 0, fieldModifiers, excluded);
	}

	public static String[] getFieldNameSuggestions(IJavaProject project, String baseName, int dimensions, int modifiers, String[] excluded) {
		if (Flags.isFinal(modifiers) && Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FINAL_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		} else if (Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		}
		return getVariableNameSuggestions(NamingConventions.VK_INSTANCE_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}

	public static String[] getLocalNameSuggestions(IJavaProject project, String baseName, int dimensions, String[] excluded) {
		return getVariableNameSuggestions(NamingConventions.VK_LOCAL, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}

	public static String suggestArgumentName(IJavaProject project, String baseName, String[] excluded) {
		return suggestVariableName(NamingConventions.VK_PARAMETER, project, baseName, 0, excluded);
	}

	private static String suggestVariableName(int varKind, IJavaProject project, String baseName, int dimension, String[] excluded) {
		return getVariableNameSuggestions(varKind, project, baseName, dimension, new ExcludedCollection(excluded), true)[0];
	}


	public static String[][] suggestArgumentNamesWithProposals(IJavaProject project, String[] paramNames) {
		String[][] newNames= new String[paramNames.length][];
		ArrayList<String> takenNames= new ArrayList<>();

		// Ensure that the code generation preferences are respected
		for (int i= 0; i < paramNames.length; i++) {
			String curr= paramNames[i];
			String baseName= NamingConventions.getBaseName(NamingConventions.VK_PARAMETER, curr, project);

			String[] proposedNames= getVariableNameSuggestions(NamingConventions.VK_PARAMETER, project, curr, 0, takenNames, true);
			if (!curr.equals(baseName)) {
				// make the existing name to favorite
				LinkedHashSet<String> updatedNames= new LinkedHashSet<>();
				updatedNames.add(curr);
				updatedNames.addAll(Arrays.asList(proposedNames));
				proposedNames= updatedNames.toArray(new String[updatedNames.size()]);
			}
			newNames[i]= proposedNames;
			takenNames.add(proposedNames[0]);
		}
		return newNames;
	}

	public static String[][] suggestArgumentNamesWithProposals(IJavaProject project, IMethodBinding binding) {
		int nParams= binding.getParameterTypes().length;
		if (nParams > 0) {
			try {
				IMethod method= (IMethod)binding.getMethodDeclaration().getJavaElement();
				if (method != null) {
					String[] parameterNames= method.getParameterNames();
					if (parameterNames.length == nParams) {
						return suggestArgumentNamesWithProposals(project, parameterNames);
					}
				}
			} catch (JavaModelException e) {
				// ignore
			}
		}
		String[][] names= new String[nParams][];
		for (int i= 0; i < names.length; i++) {
			names[i]= new String[] { "arg" + i }; //$NON-NLS-1$
		}
		return names;
	}


	public static String[] suggestArgumentNames(IJavaProject project, IMethodBinding binding) {
		int nParams= binding.getParameterTypes().length;

		if (nParams > 0) {
			try {
				IMethod method= (IMethod)binding.getMethodDeclaration().getJavaElement();
				if (method != null) {
					String[] paramNames= method.getParameterNames();
					if (paramNames.length == nParams) {
						String[] namesArray= EMPTY;
						ArrayList<String> newNames= new ArrayList<>(paramNames.length);
						// Ensure that the code generation preferences are respected
						for (String curr : paramNames) {
							String baseName= NamingConventions.getBaseName(NamingConventions.VK_PARAMETER, curr, method.getJavaProject());
							if (!curr.equals(baseName)) {
								// make the existing name the favorite
								newNames.add(curr);
							} else {
								newNames.add(suggestArgumentName(project, curr, namesArray));
							}
							namesArray= newNames.toArray(new String[newNames.size()]);
						}
						return namesArray;
					}
				}
			} catch (JavaModelException e) {
				// ignore
			}
		}
		String[] names= new String[nParams];
		for (int i= 0; i < names.length; i++) {
			names[i]= "arg" + i; //$NON-NLS-1$
		}
		return names;
	}

	public static String getBaseName(IField field) throws JavaModelException {
		return NamingConventions.getBaseName(getFieldKind(field.getFlags()), field.getElementName(), field.getJavaProject());
	}

	public static String getBaseName(IVariableBinding binding, IJavaProject project) {
		return NamingConventions.getBaseName(getKind(binding), binding.getName(), project);
	}

	/**
	 * Returns the kind of the given binding.
	 *
	 * @param binding variable binding
	 * @return one of the <code>NamingConventions.VK_*</code> constants
	 * @since 3.5
	 */
	private static int getKind(IVariableBinding binding) {
		if (binding.isField())
			return getFieldKind(binding.getModifiers());

		if (binding.isParameter())
			return NamingConventions.VK_PARAMETER;

		return NamingConventions.VK_LOCAL;
	}

	private static int getFieldKind(int modifiers) {
		if (!Modifier.isStatic(modifiers))
			return NamingConventions.VK_INSTANCE_FIELD;

		if (!Modifier.isFinal(modifiers))
			return NamingConventions.VK_STATIC_FIELD;

		return NamingConventions.VK_STATIC_FINAL_FIELD;
	}

	private static class ExcludedCollection extends AbstractList<String> {
		private String[] fExcluded;

		public ExcludedCollection(String[] excluded) {
			fExcluded= excluded;
		}

		public String[] getExcludedArray() {
			return fExcluded;
		}

		@Override
		public int size() {
			return fExcluded.length;
		}

		@Override
		public String get(int index) {
			return fExcluded[index];
		}

		@Override
		public int indexOf(Object o) {
			if (o instanceof String) {
				for (int i= 0; i < fExcluded.length; i++) {
					if (o.equals(fExcluded[i]))
						return i;
				}
			}
			return -1;
		}

		@Override
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}
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

	public static boolean hasConstantName(IJavaProject project, String name) {
		if (Character.isUpperCase(name.charAt(0)))
			return true;
		String prefixes= project.getOption(JavaCore.CODEASSIST_STATIC_FINAL_FIELD_PREFIXES, true);
		String suffixes= project.getOption(JavaCore.CODEASSIST_STATIC_FINAL_FIELD_SUFFIXES, true);
		return hasPrefixOrSuffix(prefixes, suffixes, name);
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

	// -------------------- preference access -----------------------

	public static boolean useThisForFieldAccess(IJavaProject project) {
		return Boolean.parseBoolean(JavaManipulation.getPreference(CODEGEN_KEYWORD_THIS, project));
	}

	public static boolean useIsForBooleanGetters(IJavaProject project) {
		return Boolean.parseBoolean(JavaManipulation.getPreference(CODEGEN_IS_FOR_GETTERS, project));
	}

	public static String getExceptionVariableName(IJavaProject project) {
		return JavaManipulation.getPreference(CODEGEN_EXCEPTION_VAR_NAME, project);
	}

	public static boolean doAddComments(IJavaProject project) {
		return Boolean.parseBoolean(JavaManipulation.getPreference(CODEGEN_ADD_COMMENTS, project));
	}

	/**
	 * Only to be used by tests
	 *
	 * @param templateId the template id
	 * @param pattern the new pattern
	 * @param project not used
	 */
	public static void setCodeTemplate(String templateId, String pattern, IJavaProject project) {
		TemplateStoreCore codeTemplateStore= JavaManipulation.getCodeTemplateStore();
		TemplatePersistenceData data= codeTemplateStore.getTemplateData(templateId);
		Template orig= data.getTemplate();
		Template copy= new Template(orig.getName(), orig.getDescription(), orig.getContextTypeId(), pattern, true);
		data.setTemplate(copy);
	}

	public static Template getCodeTemplate(String id, IJavaProject project) {
		if (project == null)
			return JavaManipulation.getCodeTemplateStore().findTemplateById(id);
		ProjectTemplateStore projectStore= new ProjectTemplateStore(project.getProject());
		try {
			projectStore.load();
		} catch (IOException e) {
			JavaManipulationPlugin.log(e);
		}
		return projectStore.findTemplateById(id);
	}


	public static ImportRewrite createImportRewrite(ICompilationUnit cu, boolean restoreExistingImports) throws JavaModelException {
		return CodeStyleConfiguration.createImportRewrite(cu, restoreExistingImports);
	}

	/**
	 * Returns a {@link ImportRewrite} using {@link ImportRewrite#create(CompilationUnit, boolean)} and
	 * configures the rewriter with the settings as specified in the JDT UI preferences.
	 * <p>
	 * This method sets {@link ImportRewrite#setUseContextToFilterImplicitImports(boolean)} to <code>true</code>
	 * iff the given AST has been resolved with bindings. Clients should always supply a context
	 * when they call one of the <code>addImport(...)</code> methods.
	 * </p>
	 *
	 * @param astRoot the AST root to create the rewriter on
	 * @param restoreExistingImports specifies if the existing imports should be kept or removed.
	 * @return the new rewriter configured with the settings as specified in the JDT UI preferences.
	 *
	 * @see ImportRewrite#create(CompilationUnit, boolean)
	 */
	public static ImportRewrite createImportRewrite(CompilationUnit astRoot, boolean restoreExistingImports) {
		ImportRewrite rewrite= CodeStyleConfiguration.createImportRewrite(astRoot, restoreExistingImports);
		if (astRoot.getAST().hasResolvedBindings()) {
			rewrite.setUseContextToFilterImplicitImports(true);
		}
		return rewrite;
	}

	private StubUtility() {
	}

}


