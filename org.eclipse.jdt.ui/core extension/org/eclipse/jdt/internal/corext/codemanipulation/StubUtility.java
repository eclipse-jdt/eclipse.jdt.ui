/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

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
	 */
	public static String genStub(String destTypeName, IMethod method, GenStubSettings settings, IImportsStructure imports) throws JavaModelException {
		IType declaringtype= method.getDeclaringType();	
		StringBuffer buf= new StringBuffer();
		String methodName= method.getElementName();
		String[] paramTypes= method.getParameterTypes();
		String[] paramNames= method.getParameterNames();
		String[] excTypes= method.getExceptionTypes();
		String retTypeSig= method.getReturnType();
		int flags= method.getFlags();
		boolean isConstructor= method.isConstructor();
		
		int lastParam= paramTypes.length -1;		
		
		
		if (settings.createComments) {
			if (isConstructor) {
				String desc= "Constructor for " + destTypeName; //$NON-NLS-1$
				genJavaDocStub(desc, paramNames, Signature.SIG_VOID, excTypes, buf);
			} else {			
				// java doc
				if (settings.methodOverwrites) {
					boolean isDeprecated= Flags.isDeprecated(flags);
					genJavaDocSeeTag(declaringtype, methodName, paramTypes, settings.createNonJavadocComments, isDeprecated, buf);
				} else {
					// generate a default java doc comment
					String desc= "Method " + methodName; //$NON-NLS-1$
					genJavaDocStub(desc, paramNames, retTypeSig, excTypes, buf);
				}
			}
			buf.append('\n');
		}
		
		if (Flags.isPublic(flags) || isConstructor || (declaringtype.isInterface() && !settings.noBody)) {
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
				retTypeFrm= resolveAndAdd(retTypeSig, declaringtype, imports);
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
				paramTypeFrm= resolveAndAdd(paramTypeSig, declaringtype, imports);
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
				String excTypeFrm= resolveAndAdd(excTypeSig, declaringtype, imports);
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
			if (!settings.callSuper) {
				if (retTypeSig != null && !retTypeSig.equals(Signature.SIG_VOID)) {
					buf.append('\t');
					if (!isPrimitiveType(retTypeSig) || Signature.getArrayCount(retTypeSig) > 0) {
						buf.append("return null;\n\t"); //$NON-NLS-1$
					} else if (retTypeSig.equals(Signature.SIG_BOOLEAN)) {
						buf.append("return false;\n\t"); //$NON-NLS-1$
					} else {
						buf.append("return 0;\n\t"); //$NON-NLS-1$
					}
				}
			} else {
				buf.append('\t');
				if (!isConstructor) {
					if (!Signature.SIG_VOID.equals(retTypeSig)) {
						buf.append("return "); //$NON-NLS-1$
					}
					buf.append("super."); //$NON-NLS-1$
					buf.append(methodName);
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
			buf.append("}\n");			 //$NON-NLS-1$
		}
		return buf.toString();
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
	 */
	public static void genJavaDocSeeTag(IType declaringType, String methodName, String[] paramTypes, boolean nonJavaDocComment, boolean isDeprecated, StringBuffer buf) throws JavaModelException {
		String[] fullParamNames= new String[paramTypes.length];
		for (int i= 0; i < paramTypes.length; i++) {
			fullParamNames[i]= JavaModelUtil.getResolvedTypeName(paramTypes[i], declaringType);
		}
		String fullTypeName= JavaModelUtil.getFullyQualifiedName(declaringType);
		
		genJavaDocSeeTag(fullTypeName, methodName, fullParamNames, nonJavaDocComment, isDeprecated, buf);
	}
	
	/**
	 * Generates a '@see' tag to the defined method.
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
	public static String[] evalConstructors(IType type, IType supertype, CodeGenerationSettings settings, IImportsStructure imports) throws JavaModelException {
		IMethod[] superMethods= supertype.getMethods();
		String typeName= type.getElementName();
		IMethod[] methods= type.getMethods();
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.callSuper= true;
		ArrayList newMethods= new ArrayList(superMethods.length);
		for (int i= 0; i < superMethods.length; i++) {
			IMethod curr= superMethods[i];
			if (curr.isConstructor() && (JavaModelUtil.isVisible(curr, type.getPackageFragment()) || Flags.isProtected(curr.getFlags()))) {
				if (JavaModelUtil.findMethod(typeName, curr.getParameterTypes(), true, methods) == null) {
					String newStub= genStub(typeName, superMethods[i], genStubSettings, imports);
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
				IOverrideMethodQuery selectionQuery, IImportsStructure imports) throws JavaModelException {
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
		String[] result= new String[toImplementArray.length];
		for (int i= 0; i < toImplementArray.length; i++) {
			IMethod curr= toImplementArray[i];
			IMethod overrides= JavaModelUtil.findMethodImplementationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			genStubSettings.callSuper= (overrides != null);
						
			IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, type, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
			if (desc != null) {
				curr= desc;
			}
			result[i]= genStub(type.getElementName(), curr, genStubSettings, imports);
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
