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

package org.eclipse.jdt.internal.junit.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage.ImportsManager;
import org.eclipse.swt.SWT;

/**
 * Utility methods for code generation.
 * TODO: some methods are duplicated from org.eclipse.jdt.ui
 */
public class JUnitStubUtility {

	public static class GenStubSettings extends CodeGenerationSettings {
	
		public boolean fCallSuper;
		public boolean fMethodOverwrites;
		public boolean fNoBody;
		
		public GenStubSettings(CodeGenerationSettings settings) {
			this.createComments= settings.createComments;
		}
	}

	/**
	 * Examines a string and returns the first line delimiter found.
	 */
	public static String getLineDelimiterUsed(IJavaElement elem) {
		try {
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
		} catch (JavaModelException e) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public static String codeFormat(String sourceString, int initialIndentationLevel, String lineDelim) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		return formatter.format(sourceString, initialIndentationLevel, null, lineDelim);
	}

	/**
	 * Generates a stub. Given a template method, a stub with the same signature
	 * will be constructed so it can be added to a type.
	 * @param destTypeName The name of the type to which the method will be added to (Used for the constructor)
	 * @param method A method template (method belongs to different type than the parent)
	 * @param settings Options as defined above (GENSTUB_*)
	 * @param imports Imports required by the sub are added to the imports structure
	 * @throws JavaModelException
	 */
	public static String genStub(String destTypeName, IMethod method, GenStubSettings settings, ImportsManager imports) throws JavaModelException {
		IType declaringtype= method.getDeclaringType();	
		StringBuffer buf= new StringBuffer();
		String[] paramTypes= method.getParameterTypes();
		String[] paramNames= method.getParameterNames();
		String[] excTypes= method.getExceptionTypes();
		String retTypeSig= method.getReturnType();
		
		int lastParam= paramTypes.length -1;		
		
		if (settings.createComments) {
			if (method.isConstructor()) {
				String desc= "Constructor for " + destTypeName; //$NON-NLS-1$
				genJavaDocStub(desc, paramNames, Signature.SIG_VOID, excTypes, buf);
			} else {			
				// java doc
				if (settings.fMethodOverwrites) {
					boolean isDeprecated= Flags.isDeprecated(method.getFlags());
					genJavaDocSeeTag(declaringtype.getElementName(), method.getElementName(), paramTypes, settings.createNonJavadocComments, isDeprecated, buf);
				} else {
					// generate a default java doc comment
					String desc= "Method " + method.getElementName(); //$NON-NLS-1$
					genJavaDocStub(desc, paramNames, retTypeSig, excTypes, buf);
				}
			}
		}
		int flags= method.getFlags();
		if (Flags.isPublic(flags) || (declaringtype.isInterface() && !settings.fNoBody)) {
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
			
		if (method.isConstructor()) {
			buf.append(destTypeName);
		} else {
			String retTypeFrm= Signature.toString(retTypeSig);
			if (!isBuiltInType(retTypeSig)) {
				resolveAndAdd(retTypeSig, declaringtype, imports);
			}
			buf.append(Signature.getSimpleName(retTypeFrm));
			buf.append(' ');
			buf.append(method.getElementName());
		}
		buf.append('(');
		for (int i= 0; i <= lastParam; i++) {
			String paramTypeSig= paramTypes[i];
			String paramTypeFrm= Signature.toString(paramTypeSig);
			if (!isBuiltInType(paramTypeSig)) {
				resolveAndAdd(paramTypeSig, declaringtype, imports);
			}
			buf.append(Signature.getSimpleName(paramTypeFrm));
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
				String excTypeFrm= Signature.toString(excTypeSig);
				resolveAndAdd(excTypeSig, declaringtype, imports);
				buf.append(Signature.getSimpleName(excTypeFrm));
				if (i < lastExc) {
					buf.append(", "); //$NON-NLS-1$
				}
			}
		}
		if (settings.fNoBody) {
			buf.append(";\n\n"); //$NON-NLS-1$
		} else {
			buf.append(" {\n\t"); //$NON-NLS-1$
			if (!settings.fCallSuper) {
				if (retTypeSig != null && !retTypeSig.equals(Signature.SIG_VOID)) {
					buf.append('\t');
					if (!isBuiltInType(retTypeSig) || Signature.getArrayCount(retTypeSig) > 0) {
						buf.append("return null;\n\t"); //$NON-NLS-1$
					} else if (retTypeSig.equals(Signature.SIG_BOOLEAN)) {
						buf.append("return false;\n\t"); //$NON-NLS-1$
					} else {
						buf.append("return 0;\n\t"); //$NON-NLS-1$
					}
				}
			} else {
				buf.append('\t');
				if (!method.isConstructor()) {
					if (!Signature.SIG_VOID.equals(retTypeSig)) {
						buf.append("return "); //$NON-NLS-1$
					}
					buf.append("super."); //$NON-NLS-1$
					buf.append(method.getElementName());
				} else {
					buf.append("super"); //$NON-NLS-1$
				}
				buf.append('(');			
				for (int i= 0; i <= lastParam; i++) {
					buf.append(paramNames[i]);
					if (i < lastParam) {
						buf.append(", "); //$NON-NLS-1$
					}
				}
				buf.append(");\n\t"); //$NON-NLS-1$
			}
			buf.append("}\n\n");			 //$NON-NLS-1$
		}
		return buf.toString();
	}

	/**
	 * Generates a default JavaDoc comment stub for a method.
	 */
	private static void genJavaDocStub(String descr, String[] paramNames, String retTypeSig, String[] excTypeSigs, StringBuffer buf) {
		buf.append("/**\n"); //$NON-NLS-1$
		buf.append(" * "); buf.append(descr); buf.append(".\n"); //$NON-NLS-2$ //$NON-NLS-1$
		for (int i= 0; i < paramNames.length; i++) {
			buf.append(" * @param "); buf.append(paramNames[i]); buf.append('\n'); //$NON-NLS-1$
		}
		if (retTypeSig != null && !retTypeSig.equals(Signature.SIG_VOID)) {
			String simpleName= Signature.getSimpleName(Signature.toString(retTypeSig));
			buf.append(" * @return "); buf.append(simpleName); buf.append('\n'); //$NON-NLS-1$
		}
		for (int i= 0; i < excTypeSigs.length; i++) {
			String simpleName= Signature.getSimpleName(Signature.toString(excTypeSigs[i]));
			buf.append(" * @throws "); buf.append(simpleName); buf.append('\n'); //$NON-NLS-1$
		}		
		buf.append(" */\n"); //$NON-NLS-1$
	}
	
	/**
	 * Generates a '@see' tag to the defined method.
	 */
	public static void genJavaDocSeeTag(String declaringTypeName, String methodName, String[] paramTypes, boolean nonJavaDocComment, boolean isDeprecated, StringBuffer buf) {
		// create a @see link
		buf.append("/*"); //$NON-NLS-1$
		if (!nonJavaDocComment) {
			buf.append('*');
		}
		buf.append("\n * @see "); //$NON-NLS-1$
		buf.append(declaringTypeName);
		buf.append('#'); 
		buf.append(methodName);
		buf.append('(');
		for (int i= 0; i < paramTypes.length; i++) {
			if (i > 0) {
				buf.append(", "); //$NON-NLS-1$
			}
			buf.append(Signature.getSimpleName(Signature.toString(paramTypes[i])));
		}
		buf.append(")\n"); //$NON-NLS-1$
		if (isDeprecated) {
			buf.append(" * @deprecated\n"); //$NON-NLS-1$
		}
		buf.append(" */\n"); //$NON-NLS-1$
	}	

	private static boolean isBuiltInType(String typeName) {
		char first= Signature.getElementType(typeName).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
	}

	private static void resolveAndAdd(String refTypeSig, IType declaringType, ImportsManager imports) throws JavaModelException {
		String resolvedTypeName= JavaModelUtil.getResolvedTypeName(refTypeSig, declaringType);
		if (resolvedTypeName != null) {
			imports.addImport(resolvedTypeName);		
		}
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
