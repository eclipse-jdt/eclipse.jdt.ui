/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.template.CodeTemplates;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplatePosition;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContext;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

public class StubUtility {
	
	
	public static class GenStubSettings extends CodeGenerationSettings {
	
		public boolean callSuper;
		public boolean methodOverwrites;
		public boolean noBody;
		
		public GenStubSettings(CodeGenerationSettings settings) {
			settings.setSettings(this);	
		}
		
		public GenStubSettings() {
		}
			
	}


	/**
	 * Generates a stub. Given a template method, a stub with the same signature
	 * will be constructed so it can be added to a type.
	 * @param destTypeName The name of the type to which the method will be added to (Used for the constructor)
	 * @param method A method template (method belongs to different type than the parent)
	 * @param options Options as defined above (<code>GenStubSettings</code>)
	 * @param imports Imports required by the stub are added to the imports structure. If imports structure is <code>null</code>
	 * all type names are qualified.
	 * @throws JavaModelException
	 * @deprecated
     */
	public static String genStub(String destTypeName, IMethod method, GenStubSettings settings, IImportsStructure imports) throws CoreException {
		return genStub(destTypeName, method, method.getDeclaringType(), settings, imports);
	}
	
	/**
	 * Generates a stub. Given a template method, a stub with the same signature
	 * will be constructed so it can be added to a type.
	 * @param destTypeName The name of the type to which the method will be added to (Used for the constructor)
	 * @param method A method template (method belongs to different type than the parent)
	 * @param definingType The type that defines the method.
	 * @param options Options as defined above (<code>GenStubSettings</code>)
	 * @param imports Imports required by the stub are added to the imports structure. If imports structure is <code>null</code>
	 * all type names are qualified.
	 * @throws JavaModelException
	 */
	public static String genStub(ICompilationUnit cu, String destTypeName, IMethod method, IType definingType, GenStubSettings settings, IImportsStructure imports) throws CoreException {
		StringBuffer buf= new StringBuffer();
		if (settings.createComments && cu != null) {
			String comment;
			if (method.isConstructor()) {
				comment= getMethodComment(cu, destTypeName, method.getElementName(), method.getParameterNames(), method.getExceptionTypes(), null);
			} else {
				if (settings.methodOverwrites) {
					comment= getOverridingMethodComment(cu, destTypeName, definingType, method.getElementName(), method.getParameterTypes(), method.getParameterNames(), method.getExceptionTypes());
				} else {
					comment= getMethodComment(cu, destTypeName, method.getElementName(), method.getParameterNames(), method.getExceptionTypes(), method.getReturnType());
				}		
			}
			if (comment != null) {
				buf.append(comment);
			} else {
				buf.append("/**\n *\n **/");
			}
			buf.append('\n');
		}
		buf.append(genStub(destTypeName, method, method.getDeclaringType(), settings, imports));
		return buf.toString();
	}

	private static String genStub(String destTypeName, IMethod method, IType definingType, GenStubSettings settings, IImportsStructure imports) throws CoreException {
		IType parentType= method.getDeclaringType();	
		StringBuffer buf= new StringBuffer();
		String methodName= method.getElementName();
		String[] paramTypes= method.getParameterTypes();
		String[] paramNames= method.getParameterNames();
		String[] excTypes= method.getExceptionTypes();
		
		int flags= method.getFlags();
		boolean isConstructor= method.isConstructor();
		String retTypeSig= isConstructor ? null : method.getReturnType();
		
		int lastParam= paramTypes.length -1;
		
		if (Flags.isPublic(flags) || isConstructor || (parentType.isInterface() && !settings.noBody)) {
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
		if (settings.noBody) {
			buf.append(";\n\n"); //$NON-NLS-1$
		} else {
			buf.append(" {\n\t"); //$NON-NLS-1$
					
			String body= createMethodBody(settings.callSuper, methodName, paramNames, retTypeSig);
			String template= getBodyStubTemplate(isConstructor, method.getJavaProject(), destTypeName, methodName, body);
			if (template != null) {
				buf.append(template);
				buf.append('\n');
			}
			buf.append("}\n");			 //$NON-NLS-1$
		}
		return buf.toString();
	}
	
	public static String createMethodBody(boolean callSuper, String methodName, String[] paramNames, String retTypeSig) {
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
			return buf.toString();
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
			return "";
		}
	}	

	public static String getBodyStubTemplate(boolean isConstructor, IJavaProject project, String destTypeName, String methodName, String bodyStatement) throws CoreException {
		String templateName= isConstructor ? CodeTemplates.CONSTRUCTORSTUB : CodeTemplates.METHODSTUB;
		Template template= CodeTemplates.getCodeTemplate(templateName);
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeName(), project, String.valueOf('\n'), 0);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		context.setVariable(CodeTemplateContextType.BODY_STATEMENT, bodyStatement);
		String str= context.evaluate(template).getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			if (Strings.containsOnlyWhitespaces(bodyStatement)) {
				return null;
			}
			return bodyStatement;
		}
		return str;
	}
	
	public static String getTypeComment(ICompilationUnit cu, String destTypeName) throws CoreException {
		Template template= CodeTemplates.getCodeTemplate(CodeTemplates.TYPECOMMENT);
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeName(), cu.getJavaProject(), String.valueOf('\n'), 0);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, destTypeName);
		String str= context.evaluate(template).getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		return str;
	}
	
	/**
	 * Returns the comment for a non-overriding method. <code>null</code> is
	 * returned if the configured template is empty.
	 * @param cu The compilation unit to which the method belongs
	 * @param typeName Name of the type to which the method belongs.
	 * @param methodName Name of the method.
	 * @param paramNames Names of the parameters for the method.
	 * @param excTypeSig Throwns exceptions (Signature notation)
	 * @param retTypeSig Return type (Signature notation)
	 * @return String Returns the contructed coment or <code>null</code> if the
	 * the configured template is empty. The string uses \\n as line delimiter
	 * (formatting required)
	 * @throws CoreException
	 */
	public static String getMethodComment(ICompilationUnit cu, String typeName, String methodName, String[] paramNames, String[] excTypeSig, String retTypeSig) throws CoreException {
		String templateName= retTypeSig != null ? CodeTemplates.METHODCOMMENT : CodeTemplates.CONSTRUCTORCOMMENT;
		Template template= CodeTemplates.getCodeTemplate(templateName);
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeName(), cu.getJavaProject(), String.valueOf('\n'), 0);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		if (retTypeSig != null) {
			context.setVariable(CodeTemplateContextType.RETURN_TYPE, Signature.toString(retTypeSig));
		}
		
		TemplateBuffer buffer= context.evaluate(template);
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplatePosition position= findTagPosition(buffer); // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}
			
		TextBuffer textBuffer= TextBuffer.create(str);
		String[] exceptionNames= new String[excTypeSig.length];
		for (int i= 0; i < excTypeSig.length; i++) {
			exceptionNames[i]= Signature.toString(excTypeSig[i]);
		}
		String returnType= retTypeSig != null ? Signature.toString(retTypeSig) : null;
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			insertTag(textBuffer, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, false);
		}
		return textBuffer.getContent();
	}
			
	private static TemplatePosition findTagPosition(TemplateBuffer buffer) {
		TemplatePosition[] positions= buffer.getVariables();
		for (int i= 0; i < positions.length; i++) {
			TemplatePosition curr= positions[i];
			if (CodeTemplateContextType.TAGS.equals(curr.getName())) {
				return curr;
			}
		}
		return null;		
	}

	public static String getMethodComment(ICompilationUnit cu, String typeName, MethodDeclaration decl) throws CoreException {
		String templateName= decl.isConstructor() ? CodeTemplates.CONSTRUCTORCOMMENT : CodeTemplates.METHODCOMMENT;
		Template template= CodeTemplates.getCodeTemplate(templateName);
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeName(), cu.getJavaProject(), String.valueOf('\n'), 0);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, decl.getName().getIdentifier());
		if (!decl.isConstructor()) {
			context.setVariable(CodeTemplateContextType.RETURN_TYPE, ASTNodes.asString(decl.getReturnType()));
		}
		TemplateBuffer buffer= context.evaluate(template);
		String str= buffer.getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		TemplatePosition position= findTagPosition(buffer);  // look if Javadoc tags have to be added
		if (position == null) {
			return str;
		}
			
		TextBuffer textBuffer= TextBuffer.create(str);
		List params= decl.parameters();
		String[] paramNames= new String[params.size()];
		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= (SingleVariableDeclaration) params.get(i);
			paramNames[i]= elem.getName().getIdentifier();
		}
		List exceptions= decl.thrownExceptions();
		String[] exceptionNames= new String[exceptions.size()];
		for (int i= 0; i < exceptions.size(); i++) {
			exceptionNames[i]= ASTResolving.getSimpleName((Name) exceptions.get(i));
		}
		String returnType= !decl.isConstructor() ? ASTNodes.asString(decl.getReturnType()) : null;
		int[] tagOffsets= position.getOffsets();
		for (int i= tagOffsets.length - 1; i >= 0; i--) { // from last to first
			insertTag(textBuffer, tagOffsets[i], position.getLength(), paramNames, exceptionNames, returnType, false);
		}
		return textBuffer.getContent();
	}	
	
	private static void insertTag(TextBuffer textBuffer, int offset, int length, String[] paramNames, String[] exceptionNames, String returnType, boolean isDeprecated) throws CoreException {
		TextRegion region= textBuffer.getLineInformationOfOffset(offset);
		if (region == null) {
			return;
		}
		String lineStart= textBuffer.getContent(region.getOffset(), offset - region.getOffset());
		
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < paramNames.length; i++) {
			if (buf.length() > 0) {
				buf.append('\n'); buf.append(lineStart);
			}
			buf.append("@param "); buf.append(paramNames[i]); //$NON-NLS-1$
		}
		if (returnType != null && !returnType.equals("void")) {
			if (buf.length() > 0) {
				buf.append('\n'); buf.append(lineStart);
			}			
			buf.append("@return "); buf.append(returnType); //$NON-NLS-1$
		}
		if (exceptionNames != null) {
			for (int i= 0; i < exceptionNames.length; i++) {
				if (buf.length() > 0) {
					buf.append('\n'); buf.append(lineStart);
				}
				buf.append("@throws "); buf.append(exceptionNames[i]); //$NON-NLS-1$
			}
		}		
		if (isDeprecated) {
			if (buf.length() > 0) {
				buf.append('\n'); buf.append(lineStart);
			}
			buf.append("@deprecated");
		}
		textBuffer.replace(offset, length, buf.toString());
	}

	public static String getOverridingMethodComment(ICompilationUnit cu, String typeName, IType definingType, String methodName, String[] paramTypeRefs, String[] paramNames, String[] excTypeSig) throws CoreException {
		IMethod overridden= JavaModelUtil.findMethod(methodName, paramTypeRefs, false, definingType.getMethods());
		String retTypeSig= Signature.SIG_VOID;
		boolean isDeprecated= false;
		if (overridden != null) {
			retTypeSig= overridden.getReturnType();
			isDeprecated= Flags.isDeprecated(overridden.getFlags());
		}
		
		Template template= CodeTemplates.getCodeTemplate(CodeTemplates.OVERRIDECOMMENT);
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeName(), cu.getJavaProject(), String.valueOf('\n'), 0);
		context.setCompilationUnitVariables(cu);
		context.setVariable(CodeTemplateContextType.ENCLOSING_METHOD, methodName);
		context.setVariable(CodeTemplateContextType.ENCLOSING_TYPE, typeName);
		context.setVariable(CodeTemplateContextType.RETURN_TYPE, Signature.toString(retTypeSig));
		
		String str= context.evaluate(template).getString();
		if (Strings.containsOnlyWhitespaces(str)) {
			return null;
		}
		return str;
	}		
	
	private static boolean isSet(int options, int flag) {
		return (options & flag) != 0;
	}	

	private static boolean isPrimitiveType(String typeName) {
		char first= Signature.getElementType(typeName).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
	}

	private static String resolveAndAdd(String refTypeSig, IType declaringType, IImportsStructure imports) throws JavaModelException {
		String resolvedTypeName= JavaModelUtil.getResolvedTypeName(refTypeSig, declaringType);
		if (resolvedTypeName != null) {
			StringBuffer buf= new StringBuffer();
			if (imports != null) {
				buf.append(imports.addImport(resolvedTypeName));
			} else {
				buf.append(resolvedTypeName);
			}
			int arrayCount= Signature.getArrayCount(refTypeSig);
			for (int i= 0; i < arrayCount; i++) {
				buf.append("[]"); //$NON-NLS-1$
			}
			return buf.toString();
		}
		return Signature.toString(refTypeSig);
	}
	
	/**
	 * Generates a default JavaDoc comment stub for a method.
	 */
	public static void genJavaDocStub(String descr, String[] paramNames, String retTypeSig, String[] excTypeSigs, StringBuffer buf) {
		buf.append("/**\n"); //$NON-NLS-1$
		buf.append(" * "); buf.append(descr); buf.append(".\n"); //$NON-NLS-2$ //$NON-NLS-1$
		for (int i= 0; i < paramNames.length; i++) {
			buf.append(" * @param "); buf.append(paramNames[i]); buf.append('\n'); //$NON-NLS-1$
		}
		if (retTypeSig != null && !retTypeSig.equals(Signature.SIG_VOID)) {
			String simpleName= Signature.getSimpleName(Signature.toString(retTypeSig));
			buf.append(" * @return "); buf.append(simpleName); buf.append('\n'); //$NON-NLS-1$
		}
		if (excTypeSigs != null) {
			for (int i= 0; i < excTypeSigs.length; i++) {
				String simpleName= Signature.getSimpleName(Signature.toString(excTypeSigs[i]));
				buf.append(" * @throws "); buf.append(simpleName); buf.append('\n'); //$NON-NLS-1$
			}
		}
		buf.append(" */"); //$NON-NLS-1$
	}
		
	/**
	 * Generates a '@see' tag to the defined method.
	 * @deprecated Use getMethodComment or getOverridingMethodComment
	 */
	public static void genJavaDocSeeTag(String fullyQualifiedTypeName, String methodName, String[] fullParamTypeNames, boolean nonJavaDocComment, boolean isDeprecated, StringBuffer buf) throws JavaModelException {
		// create a @see link
		buf.append("/*"); //$NON-NLS-1$
		if (!nonJavaDocComment) {
			buf.append('*');
		} else {
			buf.append(" (non-Javadoc)"); //$NON-NLS-1$
		}
		buf.append("\n * @see "); //$NON-NLS-1$
		buf.append(fullyQualifiedTypeName);
		buf.append('#'); 
		buf.append(methodName);
		buf.append('(');
		for (int i= 0; i < fullParamTypeNames.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(fullParamTypeNames[i]);
		}
		buf.append(")\n"); //$NON-NLS-1$
		if (isDeprecated) {
			buf.append(" * @deprecated\n"); //$NON-NLS-1$
		}
		buf.append(" */"); //$NON-NLS-1$
	}	
	
	

	/**
	 * Finds a method in a list of methods.
	 * @return The found method or null, if nothing found
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
			if (curr.isConstructor() && (JavaModelUtil.isVisible(curr, type.getPackageFragment()) || Flags.isProtected(curr.getFlags()))) {
				if (JavaModelUtil.findMethod(typeName, curr.getParameterTypes(), true, methods) == null) {
					String newStub= genStub(cu, typeName, curr, curr.getDeclaringType(), genStubSettings, imports);
					newMethods.add(newStub);
				}
			}
		}
		return (String[]) newMethods.toArray(new String[newMethods.size()]);
	}

	/**
	 * Searches for unimplemented methods of a type.
	 * @param isSubType If set, the evaluation is for a subtype of the given type. If not set, the
	 * evaluation is for the type itself.
	 * @param settings Options for comment generation
	 * @param selectionQuery If not null will select the methods to implement.
	 * @param imports Required imports are added to the import structure. Structure can be <code>null</code>, types are qualified then.
	 * @return Returns the generated stubs or <code>null</code> if the creation has been canceled
	 */
	public static String[] evalUnimplementedMethods(IType type, ITypeHierarchy hierarchy, boolean isSubType, CodeGenerationSettings settings, 
				IOverrideMethodQuery selectionQuery, IImportsStructure imports) throws CoreException {
		List allMethods= new ArrayList();
		List toImplement= new ArrayList();

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

		// do not call super
		for (int i= 0; i < allMethods.size(); i++) {
			IMethod curr= (IMethod) allMethods.get(i);
			if ((Flags.isAbstract(curr.getFlags()) || curr.getDeclaringType().isInterface()) && (isSubType || !type.equals(curr.getDeclaringType()))) {
				// implement all abstract methods
				toImplement.add(curr);
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
					if (impl == null || ((curr.getExceptionTypes().length < impl.getExceptionTypes().length) && !Flags.isFinal(impl.getFlags()))) {
						if (impl != null) {
							allMethods.remove(impl);
						}
						// implement an interface method when it does not exist in the hierarchy
						// or when it throws less exceptions that the implemented
						toImplement.add(curr);
						allMethods.add(curr);
					}
				}
			}
		}
		IMethod[] toImplementArray= (IMethod[]) toImplement.toArray(new IMethod[toImplement.size()]);
		if (selectionQuery != null) {
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
			IMethod[] choice= (IMethod[]) allMethods.toArray(new IMethod[allMethods.size()]);
			toImplementArray= selectionQuery.select(choice, toImplementArray, hierarchy);
			if (toImplementArray == null) {
				//cancel pressed
				return null;
			}
		}
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.methodOverwrites= true;
		ICompilationUnit cu= type.getCompilationUnit();
		String[] result= new String[toImplementArray.length];
		for (int i= 0; i < toImplementArray.length; i++) {
			IMethod curr= toImplementArray[i];
			IMethod overrides= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (overrides != null) {
				genStubSettings.callSuper= true;
				curr= overrides;
			}
			IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (desc == null) {
				desc= curr;
			}
			result[i]= genStub(cu, type.getElementName(), curr, desc.getDeclaringType(), genStubSettings, imports);
		}
		return result;
	}

	/**
	 * Examines a string and returns the first line delimiter found.
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
	
	public static String codeFormat(String sourceString, int initialIndentationLevel, String lineDelim) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		return formatter.format(sourceString, initialIndentationLevel, null, lineDelim);
	}
	
	/**
	 * Returns the element after the give element.
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

}
