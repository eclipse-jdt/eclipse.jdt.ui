/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codegeneration;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.TextUtilities;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class StubUtility {
	
	
	public static class GenStubSettings extends CodeGenerationSettings {
	
		public boolean callSuper;
		public boolean methodOverwrites;
		public boolean noBody;
		
		public GenStubSettings(CodeGenerationSettings settings) {
			this.createComments= settings.createComments;
			this.createNonJavadocComments= settings.createNonJavadocComments;		
		}
		
		public GenStubSettings() {
		}
			
	}
	

	/**
	 * Generates a stub. Given a template method, a stub with the same signature
	 * will be constructed so it can be added to a type.
	 * @param destTypeName The name of the type to which the method will be added to (Used for the constructor)
	 * @param method A method template (method belongs to different type than the parent)
	 * @param options Options as defined abouve (GENSTUB_*)
	 * @param imports Imports required by the sub are added to the imports structure
	 * @throws JavaModelException
	 */
	public static String genStub(String destTypeName, IMethod method, GenStubSettings settings, IImportsStructure imports) throws JavaModelException {
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
				if (settings.methodOverwrites) {
					genJavaDocSeeTag(declaringtype.getElementName(), method.getElementName(), paramTypes, settings.createNonJavadocComments, buf);
				} else {
					// generate a default java doc comment
					String desc= "Method " + method.getElementName(); //$NON-NLS-1$
					genJavaDocStub(desc, paramNames, retTypeSig, excTypes, buf);
				}
			}
		}
		int flags= method.getFlags();
		if (Flags.isPublic(flags) || (declaringtype.isInterface() && !settings.noBody)) {
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
		if (settings.noBody) {
			buf.append(";\n\n"); //$NON-NLS-1$
		} else {
			buf.append(" {\n\t"); //$NON-NLS-1$
			if (!settings.callSuper) {
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
	
	private static boolean isSet(int options, int flag) {
		return (options & flag) != 0;
	}	

	private static boolean isBuiltInType(String typeName) {
		char first= Signature.getElementType(typeName).charAt(0);
		return (first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED);
	}

	private static void resolveAndAdd(String refTypeSig, IType declaringType, IImportsStructure imports) throws JavaModelException {
		String resolvedTypeName= JavaModelUtil.getResolvedTypeName(refTypeSig, declaringType);
		if (resolvedTypeName != null) {
			imports.addImport(resolvedTypeName);		
		}
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
		for (int i= 0; i < excTypeSigs.length; i++) {
			String simpleName= Signature.getSimpleName(Signature.toString(excTypeSigs[i]));
			buf.append(" * @throws "); buf.append(simpleName); buf.append('\n'); //$NON-NLS-1$
		}		
		buf.append(" */\n"); //$NON-NLS-1$
	}
	
	/**
	 * Generates a '@see' tag to the defined method.
	 */
	public static void genJavaDocSeeTag(String declaringTypeName, String methodName, String[] paramTypes, boolean nonJavaDocComment, StringBuffer buf) {
		// create a @see link
		buf.append("/*");
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
		buf.append(" */\n"); //$NON-NLS-1$
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
	 * @param newMethods The resulting source for the created constructors (List of String)
	 * @param imports Type names for input declarations required (for example 'java.util.Vector')
	 */
	public static void evalConstructors(IType type, IType supertype, CodeGenerationSettings settings, List newMethods, IImportsStructure imports) throws JavaModelException {
		IMethod[] methods= supertype.getMethods();
		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.callSuper= true;
		for (int i= 0; i < methods.length; i++) {
			IMethod curr= methods[i];
			if (curr.isConstructor() && JavaModelUtil.isVisible(curr, type.getPackageFragment())) {
				String newStub= genStub(type.getElementName(), methods[i], genStubSettings, imports);
				newMethods.add(newStub);
			}
		}
	}


	/**
	 * Searches for unimplemented methods of a type.
	 * @param isSubType If set, the evaluation is for a subtype of the given type. If not set, the
	 * evaluation is for the type itself.
	 * @param commentOptions Options for comment generation (see above)
	 * @param newMethods The source for the created methods (List of String)
	 * @param imports Type names for input declarations required (for example 'java.util.Vector')
	 */
	public static void evalUnimplementedMethods(IType type, ITypeHierarchy hierarchy, boolean isSubType, CodeGenerationSettings settings, List newMethods, IImportsStructure imports) throws JavaModelException {
		List allMethods= new ArrayList();

		IMethod[] typeMethods= type.getMethods();
		for (int i= 0; i < typeMethods.length; i++) {
			IMethod curr= typeMethods[i];
			if (!curr.isConstructor() && !Flags.isStatic(curr.getFlags())) {
				allMethods.add(curr);
			}
		}

		IType[] superTypes= hierarchy.getAllSuperclasses(type);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod[] methods= superTypes[i].getMethods();
			for (int k= 0; k < methods.length; k++) {
				IMethod curr= methods[k];
				if (!curr.isConstructor() && !Flags.isStatic(curr.getFlags())) {
					if (findMethod(curr, allMethods) == null) {
						allMethods.add(curr);
					}
				}
			}
		}

		GenStubSettings genStubSettings= new GenStubSettings(settings);
		genStubSettings.methodOverwrites= true;
		// do not call super
		for (int i= 0; i < allMethods.size(); i++) {
			IMethod curr= (IMethod) allMethods.get(i);
			if ((Flags.isAbstract(curr.getFlags()) || curr.getDeclaringType().isInterface()) && (isSubType || !type.equals(curr.getDeclaringType()))) {
				// implement all abstract methods. See tag points to declaration
				IMethod desc= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, curr.getElementName(), curr.getParameterTypes(), curr.isConstructor());
				if (desc == null) {
					desc= curr;
				}
				
				String newStub= genStub(type.getElementName(), desc, genStubSettings, imports);
				newMethods.add(newStub);
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
					if (impl == null || curr.getExceptionTypes().length < impl.getExceptionTypes().length) {
						// implement an interface method when it does not exist in the hierarchy
						// or when it throws less exceptions that the implemented
						String newStub= genStub(type.getElementName(), curr, genStubSettings, imports);
						newMethods.add(newStub);
						allMethods.add(curr);
					}
				}
			}
		}
	}

	/**
	 * Examines a string and returns the first line delimiter found.
	 */
	public static String getLineDelimiterUsed(IJavaElement elem) throws JavaModelException {
		ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(elem, IJavaElement.COMPILATION_UNIT);
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
			ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(elem, IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				IBuffer buf= cu.getBuffer();
				int offset= ((ISourceReference)elem).getSourceRange().getOffset();
				int i= offset;
				// find beginning of line
				while (i > 0 && "\n\r".indexOf(buf.getChar(i - 1)) == -1) {
					i--;
				}
				int tabWidth= 4;
				String tabWidthString= (String) JavaCore.getOptions().get("org.eclipse.jdt.core.formatter.tabulation.size");
				if (tabWidthString != null) {
					try {
						tabWidth= Integer.parseInt(tabWidthString);
					} catch (NumberFormatException e) {
					}
				}
				
				return TextUtilities.getIndent(buf.getText(i, offset - i), tabWidth);
			}
		}
		return 0;
	}
	
	public static String codeFormat(String sourceString, int initialIndentationLevel, String lineDelim) {
		// code formatter does not offer all options we need
		
		CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
		formatter.options.setLineSeparator(lineDelim);
		formatter.setInitialIndentationLevel(initialIndentationLevel);
		return formatter.formatSourceString(sourceString) + lineDelim;
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

}
