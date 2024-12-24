/*******************************************************************************
 * Copyright (c) 2023, 2024 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.bcoview.BytecodeOutlinePlugin;

import org.eclipse.core.filesystem.URIUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

public class JdtUtils {
	/** package separator in bytecode notation */
	private static final char PACKAGE_SEPARATOR = '/';

	/** type name separator (for inner types) in bytecode notation */
	private static final char TYPE_SEPARATOR = '$';

	private JdtUtils() {
		// don't call
	}

	public static IJavaElement getMethod(IParent parent, String signature) {
		try {
			IJavaElement[] children = parent.getChildren();
			for (IJavaElement child : children) {
				IJavaElement javaElement = child;
				switch (javaElement.getElementType()) {
					case IJavaElement.INITIALIZER:
						// fall through
					case IJavaElement.METHOD:
						if (signature.equals(getMethodSignature(javaElement))) {
							return javaElement;
						}
						break;
					default:
						break;
				}
				if (javaElement instanceof IParent) {
					javaElement = getMethod((IParent) javaElement, signature);
					if (javaElement != null) {
						return javaElement;
					}
				}
			}
		} catch (JavaModelException e) {
			// just ignore it. Mostly caused by class files not on the class path
			// which is not a problem for us, but a big problem for JDT
		}
		return null;
	}

	/**
	 * @param childEl non null
	 * @return method signature, if given java element is either initializer or method, otherwise
	 *         returns null.
	 */
	public static String getMethodSignature(IJavaElement childEl) {
		String methodName = null;
		if (childEl.getElementType() == IJavaElement.INITIALIZER) {
			IInitializer ini = (IInitializer) childEl;
			try {
				if (Flags.isStatic(ini.getFlags())) {
					methodName = "<clinit>()V"; //$NON-NLS-1$
				} else {
					methodName = "<init>()"; //$NON-NLS-1$
				}
			} catch (JavaModelException e) {
				// this is compilation problem - don't show the message
				BytecodeOutlinePlugin.log(e, IStatus.WARNING);
			}
		} else if (childEl.getElementType() == IJavaElement.METHOD) {
			IMethod iMethod = (IMethod) childEl;
			try {
				methodName = createMethodSignature(iMethod);
			} catch (JavaModelException e) {
				// this is compilation problem - don't show the message
				BytecodeOutlinePlugin.log(e, IStatus.WARNING);
			}
		}
		return methodName;
	}

	public static String createMethodSignature(IMethod iMethod) throws JavaModelException {
		StringBuffer sb = new StringBuffer();

		// Eclipse put class name as constructor name - we change it!
		if (iMethod.isConstructor()) {
			sb.append("<init>"); //$NON-NLS-1$
		} else {
			sb.append(iMethod.getElementName());
		}

		if (iMethod.isBinary()) { // iMethod instanceof BinaryMember
			// binary info should be full qualified
			return sb.append(iMethod.getSignature()).toString();
		}

		// start method parameter descriptions list
		sb.append('(');
		IType declaringType = iMethod.getDeclaringType();
		String[] parameterTypes = iMethod.getParameterTypes();

		/*
		 * For non - static inner classes bytecode constructor should contain as first
		 * parameter the enclosing type instance, but in Eclipse AST there are no
		 * appropriated parameter. So we need to create enclosing type signature and
		 * add it as first parameter.
		 */
		if (iMethod.isConstructor() && isNonStaticInner(declaringType)) {
			// this is a very special case
			String typeSignature = getTypeSignature(getFirstAncestor(declaringType));
			if (typeSignature != null) {
				String[] newParams = new String[parameterTypes.length + 1];
				newParams[0] = typeSignature;
				System.arraycopy(parameterTypes, 0, newParams, 1, parameterTypes.length);
				parameterTypes = newParams;
			}
		}

		// doSomething(Lgenerics/DummyForAsmGenerics;)Lgenerics/DummyForAsmGenerics;
		for (String parameterType : parameterTypes) {
			String resolvedType = getResolvedType(parameterType, declaringType);
			if (resolvedType != null && resolvedType.length() > 0) {
				sb.append(resolvedType);
			} else {
				// this is a generic type
				appendGenericType(sb, iMethod, parameterType);
			}
		}
		sb.append(')');

		// continue here with adding resolved return type
		String returnType = iMethod.getReturnType();
		String resolvedType = getResolvedType(returnType, declaringType);
		if (resolvedType != null && resolvedType.length() > 0) {
			sb.append(resolvedType);
		} else {
			// this is a generic type
			appendGenericType(sb, iMethod, returnType);
		}

		return sb.toString();
	}

	/**
	 * @param type can be null
	 * @return full qualified, resolved type name in bytecode notation
	 */
	private static String getTypeSignature(IType type) {
		if (type == null) {
			return null;
		}
		/*
		 * getFullyQualifiedName() returns name, where package separator is '.',
		 * but we need '/' for bytecode. The hack with ',' is to use a character
		 * which is not allowed as Java char to be sure not to replace too much
		 */
		String name = type.getFullyQualifiedName(',');
		// replace package separators
		name = name.replace(Signature.C_DOT, PACKAGE_SEPARATOR);
		// replace class separators
		name = name.replace(',', TYPE_SEPARATOR);
		return Signature.C_RESOLVED + name + Signature.C_SEMICOLON;
	}

	private static void appendGenericType(StringBuffer sb, IMethod iMethod, String unresolvedType) throws JavaModelException {
		IType declaringType = iMethod.getDeclaringType();

		// unresolvedType is here like "QA;" => we remove "Q" and ";"
		if (unresolvedType.length() < 3) {
			// ???? something wrong here ....
			sb.append(unresolvedType);
			return;
		}
		unresolvedType = unresolvedType.substring(1, unresolvedType.length() - 1);

		ITypeParameter typeParameter = iMethod.getTypeParameter(unresolvedType);
		if (typeParameter == null || !typeParameter.exists()) {
			typeParameter = declaringType.getTypeParameter(unresolvedType);
		}

		String[] bounds = typeParameter.getBounds();
		if (bounds.length == 0) {
			sb.append("Ljava/lang/Object;"); //$NON-NLS-1$
		} else {
			for (String bound : bounds) {
				String simplyName = bound;
				simplyName = Signature.C_UNRESOLVED + simplyName + Signature.C_NAME_END;
				String resolvedType = getResolvedType(simplyName, declaringType);
				sb.append(resolvedType);
			}
		}
	}

	/**
	 * @param typeToResolve non null
	 * @param declaringType non null
	 * @return full qualified "bytecode formatted" type
	 * @throws JavaModelException on error
	 */
	private static String getResolvedType(String typeToResolve, IType declaringType) throws JavaModelException {
		StringBuffer sb = new StringBuffer();
		int arrayCount = Signature.getArrayCount(typeToResolve);
		// test which letter is following - Q or L are for reference types
		boolean isPrimitive = isPrimitiveType(typeToResolve.charAt(arrayCount));
		if (isPrimitive) {
			// simply add whole string (probably with array chars like [[I etc.)
			sb.append(typeToResolve);
		} else {
			boolean isUnresolvedType = isUnresolvedType(typeToResolve, arrayCount);
			if (!isUnresolvedType) {
				sb.append(typeToResolve);
			} else {
				// we need resolved types
				String resolved = getResolvedTypeName(typeToResolve, declaringType);
				if (resolved != null) {
					while (arrayCount > 0) {
						sb.append(Signature.C_ARRAY);
						arrayCount--;
					}
					sb.append(Signature.C_RESOLVED);
					sb.append(resolved);
					sb.append(Signature.C_SEMICOLON);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Copied and modified from JavaModelUtil. Resolves a type name in the context of the declaring
	 * type.
	 *
	 * @param refTypeSig the type name in signature notation (for example 'QVector') this can also
	 *            be an array type, but dimensions will be ignored.
	 * @param declaringType the context for resolving (type where the reference was made in)
	 * @return returns the fully qualified <b>bytecode </b> type name or build-in-type name. if a
	 *         unresoved type couldn't be resolved null is returned
	 * @throws JavaModelException on error
	 */
	private static String getResolvedTypeName(String refTypeSig, IType declaringType) throws JavaModelException {

		/* the whole method is copied from JavaModelUtil.getResolvedTypeName(...).
		 * The problem is, that JavaModelUtil uses '.' to separate package
		 * names, but we need '/' -> see JavaModelUtil.concatenateName() vs
		 * JdtUtils.concatenateName()
		 */
		int arrayCount = Signature.getArrayCount(refTypeSig);
		if (isUnresolvedType(refTypeSig, arrayCount)) {
			String name = ""; //$NON-NLS-1$
			int bracket = refTypeSig.indexOf(Signature.C_GENERIC_START, arrayCount + 1);
			if (bracket > 0) {
				name = refTypeSig.substring(arrayCount + 1, bracket);
			} else {
				int semi = refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
				if (semi == -1) {
					throw new IllegalArgumentException();
				}
				name = refTypeSig.substring(arrayCount + 1, semi);
			}
			String[][] resolvedNames = declaringType.resolveType(name);
			if (resolvedNames != null && resolvedNames.length > 0) {
				return concatenateName(resolvedNames[0][0], resolvedNames[0][1]);
			}
			return null;
		}
		return refTypeSig.substring(arrayCount);// Signature.toString(substring);
	}

	/**
	 * @param refTypeSig signature
	 * @param arrayCount expected array count in the signature
	 * @return true if the given string is an unresolved signature (Eclipse - internal
	 *         representation)
	 */
	private static boolean isUnresolvedType(String refTypeSig, int arrayCount) {
		char type = refTypeSig.charAt(arrayCount);
		return type == Signature.C_UNRESOLVED;
	}

	/**
	 * Concatenates package and class name. Both strings can be empty or <code>null</code>.
	 *
	 * @param packageName can be null
	 * @param className can be null
	 * @return result, non null, can be empty
	 */
	private static String concatenateName(String packageName, String className) {
		StringBuffer buf = new StringBuffer();
		if (packageName != null && packageName.length() > 0) {
			packageName = packageName.replace(Signature.C_DOT, PACKAGE_SEPARATOR);
			buf.append(packageName);
		}
		if (className != null && className.length() > 0) {
			if (buf.length() > 0) {
				buf.append(PACKAGE_SEPARATOR);
			}
			className = className.replace(Signature.C_DOT, TYPE_SEPARATOR);
			buf.append(className);
		}
		return buf.toString();
	}

	/**
	 * Test which letter is following - Q or L are for reference types
	 *
	 * @param first char
	 * @return true, if character is not a symbol for reference types
	 */
	private static boolean isPrimitiveType(char first) {
		return first != Signature.C_RESOLVED && first != Signature.C_UNRESOLVED;
	}

	/**
	 * @param childEl may be null
	 * @return first ancestor with IJavaElement.TYPE element type, or null
	 */
	public static IType getEnclosingType(IJavaElement childEl) {
		if (childEl == null) {
			return null;
		}
		return (IType) childEl.getAncestor(IJavaElement.TYPE);
	}

	/**
	 * Modified copy from org.eclipse.jdt.internal.ui.actions.SelectionConverter
	 *
	 * @param input can be null
	 * @param selection can be null
	 * @return null, if selection is null or could not be resolved to java element
	 * @throws JavaModelException on error
	 */
	public static IJavaElement getElementAtOffset(IJavaElement input, ITextSelection selection) throws JavaModelException {
		if (selection == null) {
			return null;
		}
		ICompilationUnit workingCopy = null;
		if (input instanceof ICompilationUnit) {
			workingCopy = (ICompilationUnit) input;
			// be in-sync with model
			// instead of using internal JavaModelUtil.reconcile(workingCopy);
			synchronized (workingCopy) {
				workingCopy.reconcile(
						ICompilationUnit.NO_AST,
						false /* don't force problem detection */,
						null /* use primary owner */,
						null /* no progress monitor */);
			}
			IJavaElement ref = workingCopy.getElementAt(selection.getOffset());
			if (ref != null) {
				return ref;
			}
		} else if (input instanceof IClassFile) {
			IClassFile iClass = (IClassFile) input;
			IJavaElement ref = iClass.getElementAt(selection.getOffset());
			if (ref != null) {
				// If we are in the inner class, try to refine search result now
				if (ref instanceof IType) {
					IType type = (IType) ref;
					IClassFile classFile = type.getClassFile();
					if (classFile != iClass) {
						/*
						 * WORKAROUND it seems that source range for constructors from
						 * bytecode with source attached from zip files is not computed
						 * in Eclipse (SourceMapper returns nothing useful).
						 * Example: HashMap$Entry class with constructor
						 * <init>(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Entry;)V
						 * We will get here at least the inner class...
						 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=137847
						 */
						ref = classFile.getElementAt(selection.getOffset());
					}
				}
				return ref;
			}
		}
		return null;
	}

	/**
	 * Cite: jdk1.1.8/docs/guide/innerclasses/spec/innerclasses.doc10.html: For the sake of tools,
	 * there are some additional requirements on the naming of an inaccessible class N. Its bytecode
	 * name must consist of the bytecode name of an enclosing class (the immediately enclosing
	 * class, if it is a member), followed either by `$' and a positive decimal numeral chosen by
	 * the compiler, or by `$' and the simple name of N, or else by both (in that order). Moreover,
	 * the bytecode name of a block-local N must consist of its enclosing package member T, the
	 * characters `$1$', and N, if the resulting name would be unique. <br>
	 * Note, that this rule was changed for static blocks after 1.5 jdk.
	 *
	 * @param javaElement non null
	 * @return simply element name
	 */
	public static String getElementName(IJavaElement javaElement) {
		if (isAnonymousType(javaElement)) {
			IType anonType = (IType) javaElement;
			List<IJavaElement> allAnonymous = new ArrayList<>();
			/*
			 * in order to resolve anon. class name we need to know about all other
			 * anonymous classes in declaring class, therefore we need to collect all here
			 */
			collectAllAnonymous(allAnonymous, anonType);
			int idx = getAnonimousIndex(anonType, allAnonymous.toArray(new IType[allAnonymous.size()]));
			return Integer.toString(idx);
		}
		String name = javaElement.getElementName();
		if (isLocal(javaElement)) {
			/*
			 * Compiler have different naming conventions for inner non-anon. classes in
			 * static blocks or any methods, this difference was introduced with 1.5 JDK.
			 * The problem is, that we could have projects with classes, generated
			 * with both 1.5 and earlier settings. One could not see on particular
			 * java element, for which jdk version the existing bytecode was generated.
			 * If we could have a *.class file, but we are just searching for one...
			 * So there could be still a chance, that this code fails, if java element
			 * is not compiled with comiler settings from project, but with different
			 */
			//ECJ mandates 1.8+ now
			name = "1" + name; // compiler output changed for > 1.5 code //$NON-NLS-1$
		}

		if (name.endsWith(".java")) { //$NON-NLS-1$
			name = name.substring(0, name.lastIndexOf(".java")); //$NON-NLS-1$
		} else if (name.endsWith(".class")) { //$NON-NLS-1$
			name = name.substring(0, name.lastIndexOf(".class")); //$NON-NLS-1$
		}
		return name;
	}

	/**
	 * @param javaElement non null
	 * @return null, if javaElement is top level class
	 */
	private static IType getFirstAncestor(IJavaElement javaElement) {
		IJavaElement parent = javaElement;
		if (javaElement.getElementType() == IJavaElement.TYPE) {
			parent = javaElement.getParent();
		}
		if (parent != null) {
			return (IType) parent.getAncestor(IJavaElement.TYPE);
		}
		return null;
	}

	private static IJavaElement getLastAncestor(IJavaElement javaElement, int elementType) {
		IJavaElement lastFound = null;
		if (elementType == javaElement.getElementType()) {
			lastFound = javaElement;
		}
		IJavaElement parent = javaElement.getParent();
		if (parent == null) {
			return lastFound;
		}
		IJavaElement ancestor = parent.getAncestor(elementType);
		if (ancestor != null) {
			return getLastAncestor(ancestor, elementType);
		}
		return lastFound;
	}

	/**
	 * @param javaElement can be null
	 * @return true, if given element is anonymous inner class
	 */
	private static boolean isAnonymousType(IJavaElement javaElement) {
		try {
			return javaElement instanceof IType && ((IType) javaElement).isAnonymous();
		} catch (JavaModelException e) {
			BytecodeOutlinePlugin.log(e, IStatus.ERROR);
		}
		return false;
	}

	/**
	 * @param innerType should be inner type.
	 * @return true, if given element is inner class from initializer block or method body
	 */
	private static boolean isLocal(IJavaElement innerType) {
		try {
			return innerType instanceof IType && ((IType) innerType).isLocal();
		} catch (JavaModelException e) {
			BytecodeOutlinePlugin.log(e, IStatus.ERROR);
		}
		return false;
	}

	/**
	 * @param type non null
	 * @return true, if given element is non static inner class
	 * @throws JavaModelException on error
	 */
	private static boolean isNonStaticInner(IType type) throws JavaModelException {
		if (type.isMember()) {
			return !Flags.isStatic(type.getFlags());
		}
		return false;
	}

	/**
	 * @param javaElement can be null
	 * @return absolute path of generated bytecode package for given element
	 * @throws JavaModelException on error
	 */
	private static String getPackageOutputPath(IJavaElement javaElement) throws JavaModelException {
		String dir = ""; //$NON-NLS-1$
		if (javaElement == null) {
			return dir;
		}

		IJavaProject project = javaElement.getJavaProject();

		if (project == null) {
			return dir;
		}
		// default bytecode location
		IPath path = project.getOutputLocation();

		IResource resource = javaElement.getUnderlyingResource();
		if (resource == null) {
			return dir;
		}
		// resolve multiple output locations here
		if (project.exists() && project.getProject().isOpen()) {
			IClasspathEntry entries[] = project.getRawClasspath();
			for (IClasspathEntry classpathEntry : entries) {
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IPath outputPath = classpathEntry.getOutputLocation();
					if (outputPath != null && classpathEntry.getPath().isPrefixOf(resource.getFullPath())) {
						path = outputPath;
						break;
					}
				}
			}
		}

		if (path == null) {
			// check the default location if not already included
			IPath def = project.getOutputLocation();
			if (def != null && def.isPrefixOf(resource.getFullPath())) {
				path = def;
			}
		}

		if (path == null) {
			return dir;
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		if (!project.getPath().equals(path)) {
			IFolder outputFolder = workspace.getRoot().getFolder(path);
			if (outputFolder != null) {
				// linked resources will be resolved here!
				IPath rawPath = outputFolder.getRawLocation();
				if (rawPath != null) {
					path = rawPath;
				}
			}
		} else {
			path = project.getProject().getLocation();
		}

		// here we should resolve path variables,
		// probably existing at first place of path
		IPathVariableManager pathManager = workspace.getPathVariableManager();
		URI resolvedURI = pathManager.resolveURI(URIUtil.toURI(path));
		if (resolvedURI != null) {
			path = URIUtil.toPath(resolvedURI);
		}

		if (path == null) {
			return dir;
		}

		if (isPackageRoot(project, resource)) {
			dir = path.toOSString();
		} else {
			String packPath = EclipseUtils.getJavaPackageName(javaElement).replace(Signature.C_DOT, PACKAGE_SEPARATOR);
			dir = path.append(packPath).toOSString();
		}
		return dir;
	}

	private static boolean isPackageRoot(IJavaProject project, IResource pack) throws JavaModelException {
		boolean isRoot = false;
		if (project == null || pack == null || !(pack instanceof IContainer)) {
			return isRoot;
		}
		IPackageFragmentRoot root = project.getPackageFragmentRoot(pack);
		IClasspathEntry clPathEntry = null;
		if (root != null) {
			clPathEntry = root.getRawClasspathEntry();
		}
		isRoot = clPathEntry != null;
		return isRoot;
	}

	/**
	 * Works only for eclipse - managed/generated bytecode, ergo not with imported classes/jars
	 *
	 * @param javaElement can be null
	 * @return full os-specific file path to .class resource, containing given element
	 */
	public static String getByteCodePath(IJavaElement javaElement) {
		if (javaElement == null) {
			return "";//$NON-NLS-1$
		}
		String packagePath = ""; //$NON-NLS-1$
		try {
			packagePath = getPackageOutputPath(javaElement);
		} catch (JavaModelException e) {
			BytecodeOutlinePlugin.log(e, IStatus.ERROR);
			return ""; //$NON-NLS-1$
		}
		IJavaElement ancestor = getLastAncestor(javaElement, IJavaElement.TYPE);
		StringBuffer sb = new StringBuffer(packagePath);
		sb.append(File.separator);
		sb.append(getClassName(javaElement, ancestor));
		sb.append(".class"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * @param javaElement non null
	 * @return new generated input stream for given element bytecode class file, or null if class
	 *         file cannot be found or this element is not from java source path
	 */
	public static byte[] readClassBytes(IJavaElement javaElement) {
		IClassFile classFile = (IClassFile) javaElement.getAncestor(IJavaElement.CLASS_FILE);

		// existing read-only class files
		if (classFile != null) {
			try {
				return classFile.getBytes();
			} catch (JavaModelException e) {
				BytecodeOutlinePlugin.log(e, IStatus.ERROR);
			}
		} else {
			// usual eclipse - generated bytecode
			boolean inJavaPath = isOnClasspath(javaElement);
			if (!inJavaPath) {
				return null;
			}
			String classPath = getByteCodePath(javaElement);
			if (classPath.isEmpty()) {
				return null;
			}
			try {
				return Files.readAllBytes(Paths.get(classPath));
			} catch (IOException e) {
				// if autobuild is disabled, we get tons of this errors.
				// but I think we cannot ignore them, therefore WARNING and not
				// ERROR status
				BytecodeOutlinePlugin.log(e, IStatus.WARNING);
			}
		}
		return null;
	}

	private static boolean isOnClasspath(IJavaElement javaElement) {
		IJavaProject project = javaElement.getJavaProject();
		if (project != null) {
			boolean result = project.isOnClasspath(javaElement);
			return result;
		}
		return false;
	}

	/**
	 * @param classFile non null
	 * @return full qualified bytecode name of given class
	 */
	public static String getFullBytecodeName(IClassFile classFile) {
		IPackageFragment packageFr = (IPackageFragment) classFile.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
		if (packageFr == null) {
			return null;
		}
		String packageName = packageFr.getElementName();
		// switch to java bytecode naming conventions
		packageName = packageName.replace(Signature.C_DOT, PACKAGE_SEPARATOR);

		String className = classFile.getElementName();
		if (packageName.length() > 0) {
			return packageName + PACKAGE_SEPARATOR + className;
		}
		return className;
	}

	private static String getClassName(IJavaElement javaElement, IJavaElement topAncestor) {
		StringBuffer sb = new StringBuffer();
		if (!javaElement.equals(topAncestor)) {
			int elementType = javaElement.getElementType();
			if (elementType == IJavaElement.FIELD
					|| elementType == IJavaElement.METHOD
					|| elementType == IJavaElement.INITIALIZER) {
				// it's field or method
				javaElement = getFirstAncestor(javaElement);
			} else {
				/*
				 * TODO there is an issue with < 1.5 compiler setting and with inner
				 * classes with the same name but defined in different methods in the same
				 * source file. Then compiler needs to generate *different* content for
				 *  A$1$B and A$1$B, which is not possible so therefore compiler generates
				 *  A$1$B and A$2$B. The naming order is the source range order of inner
				 *  classes, so the first inner B class will get A$1$B and the second
				 *  inner B class A$2$B etc.
				 */

				// override top ancestor with immediate ancestor
				topAncestor = getFirstAncestor(javaElement);
				while (topAncestor != null) {
					sb.insert(0, getElementName(topAncestor) + TYPE_SEPARATOR);
					topAncestor = getFirstAncestor(topAncestor);
				}
			}
		}
		sb.append(getElementName(javaElement));
		return sb.toString();
	}

	/**
	 * Collect all anonymous classes which are on the same "name shema level" as the given element
	 * for the compiler. The list could contain different set of elements for the same source code,
	 * depends on the compiler and jdk version
	 *
	 * @param list for the found anon. classes, elements instanceof IType.
	 * @param anonType the anon. type
	 */
	private static void collectAllAnonymous(List<IJavaElement> list, IType anonType) {
		/*
		 * For JDK >= 1.5 in Eclipse 3.1+ the naming shema for nested anonymous
		 * classes was changed from A$1, A$2, A$3, A$4, ..., A$n
		 * to A$1, A$1$1, A$1$2, A$1$2$1, ..., A$2, A$2$1, A$2$2, ..., A$x$y
		 */

		IParent declaringType = anonType.getDeclaringType();

		try {
			collectAllAnonymous(list, declaringType);
		} catch (JavaModelException e) {
			BytecodeOutlinePlugin.log(e, IStatus.ERROR);
		}
		sortAnonymous(list);
		return;
	}

	/**
	 * Traverses down the children tree of this parent and collect all child anon. classes
	 *
	 * @param list non null
	 * @param parent non null
	 * @throws JavaModelException on error
	 */
	private static void collectAllAnonymous(List<IJavaElement> list, IParent parent) throws JavaModelException {
		IJavaElement[] children = parent.getChildren();
		for (IJavaElement childElem : children) {
			if (isAnonymousType(childElem)) {
				list.add(childElem);
			}
			if (childElem instanceof IParent) {
				if (!(childElem instanceof IType)) {
					collectAllAnonymous(list, (IParent) childElem);
				}
			}
		}
	}

	/**
	 * @param anonType non null
	 * @param anonymous non null
	 * @return the index of given java element in the anon. classes list, which was used by compiler
	 *         to generate bytecode name for given element. If the given type is not in the list,
	 *         then return value is '-1'
	 */
	private static int getAnonimousIndex(IType anonType, IType[] anonymous) {
		for (int i = 0; i < anonymous.length; i++) {
			if (anonymous[i] == anonType) {
				// +1 because compiler starts generated classes always with 1
				return i + 1;
			}
		}
		return -1;
	}

	/**
	 * Sort given anonymous classes in order like java compiler would generate output classes, in
	 * context of given anonymous type
	 *
	 * @param anonymous non null
	 */
	private static void sortAnonymous(List<IJavaElement> anonymous) {
		SourceOffsetComparator sourceComparator = new SourceOffsetComparator();
		AnonymClassComparator classComparator = new AnonymClassComparator(sourceComparator);
		Collections.sort(anonymous, classComparator);

		if (BytecodeOutlinePlugin.DEBUG) {
			debugCompilePrio(classComparator);
		}
	}

	private static void debugCompilePrio(AnonymClassComparator classComparator) {
		final Map<IType, Integer> map = classComparator.map;
		Comparator<IType> prioComp = (e1, e2) -> {
			int result = map.get(e2).compareTo(map.get(e1));
			if (result == 0) {
				return e1.toString().compareTo(e2.toString());
			}
			return result;
		};

		List<IType> keys = new ArrayList<>(map.keySet());
		Collections.sort(keys, prioComp);
		for (IType key2 : keys) {
			Object key = key2;
			System.out.println(map.get(key) + " : " + key); //$NON-NLS-1$
		}
	}

	/**
	 * 1) from instance init 2) from deepest inner from instance init (deepest first) 3) from static
	 * init 4) from deepest inner from static init (deepest first) 5) from deepest inner (deepest
	 * first) 6) regular anon classes from main class
	 *
	 * <br>
	 * Note, that nested inner anon. classes which do not have different non-anon. inner class
	 * ancestors, are compiled in they nesting order, opposite to rule 2)
	 *
	 * @param javaElement non null
	 * @return priority - lesser mean will be compiled later, a value > 0
	 */
	private static int getAnonCompilePriority50(IJavaElement javaElement) {

		// search for initializer block
		IJavaElement initBlock = getLastAncestor(javaElement, IJavaElement.INITIALIZER);
		// test is for anon. classes from initializer blocks
		if (initBlock != null) {
			return 10; // from inner from class init
		}

		// test for anon. classes from "regular" code
		return 5;
	}

	private static int getAnonCompilePriority(IJavaElement elt) {
		return getAnonCompilePriority50(elt);
	}

	/**
	 * Check if java element is an interface or abstract method or a method from interface.
	 *
	 * @param javaEl can be null
	 * @return true if the given element is an interface or abstract method or a method from
	 *         interface.
	 */
	public static boolean isAbstractOrInterface(IJavaElement javaEl) {
		if (javaEl == null) {
			return true;
		}
		boolean abstractOrInterface = false;
		try {
			switch (javaEl.getElementType()) {
				case IJavaElement.CLASS_FILE:
					IClassFile classFile = (IClassFile) javaEl;
					if (isOnClasspath(javaEl)) {
						abstractOrInterface = classFile.isInterface();
					} /*else {
						 this is the case for eclipse-generated class files.
						 if we do not perform the check in if, then we will have java model
						 exception on classFile.isInterface() call.
						}*/
					break;
				case IJavaElement.COMPILATION_UNIT:
					ICompilationUnit cUnit = (ICompilationUnit) javaEl;
					IType type = cUnit.findPrimaryType();
					abstractOrInterface = type != null && type.isInterface();
					break;
				case IJavaElement.TYPE:
					abstractOrInterface = ((IType) javaEl).isInterface();
					break;
				case IJavaElement.METHOD:
					// test for "abstract" flag on method in a class
					abstractOrInterface = Flags.isAbstract(((IMethod) javaEl).getFlags());
					// "abstract" flags could be not exist on interface methods
					if (!abstractOrInterface) {
						IType ancestor = (IType) javaEl.getAncestor(IJavaElement.TYPE);
						abstractOrInterface = ancestor != null && ancestor.isInterface();
					}
					break;
				default:
					IType ancestor1 = (IType) javaEl.getAncestor(IJavaElement.TYPE);
					abstractOrInterface = ancestor1 != null && ancestor1.isInterface();
					break;
			}
		} catch (JavaModelException e) {
			// No point to log it here
			// BytecodeOutlinePlugin.log(e, IStatus.ERROR);
		}
		return abstractOrInterface;
	}

	static class SourceOffsetComparator implements Comparator<IType> {

		/**
		 * First source occurrence win.
		 *
		 * @param o1 should be IType
		 * @param o2 should be IType
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(IType o1, IType o2) {
			IType m1 = o1;
			IType m2 = o2;
			int idx1, idx2;
			try {
				ISourceRange sr1 = m1.getSourceRange();
				ISourceRange sr2 = m2.getSourceRange();
				if (sr1 == null || sr2 == null) {
					return 0;
				}
				idx1 = sr1.getOffset();
				idx2 = sr2.getOffset();
			} catch (JavaModelException e) {
				BytecodeOutlinePlugin.log(e, IStatus.ERROR);
				return 0;
			}
			return idx1 - idx2;
		}
	}

	static class AnonymClassComparator implements Comparator<IJavaElement> {

		private final SourceOffsetComparator sourceComparator;

		private final Map<IType, Integer> map;

		public AnonymClassComparator(SourceOffsetComparator sourceComparator) {
			this.sourceComparator = sourceComparator;
			map = new IdentityHashMap<>();
		}

		/**
		 * Very simple comparison based on init/not init block decision and then on the source code
		 * position
		 *
		 * @param m1 non null
		 * @param m2 non null
		 * @return -1, 0 or 1
		 */
		private int compare50(IType m1, IType m2) {

			int compilePrio1 = getCompilePrio(m1);
			int compilePrio2 = getCompilePrio(m2);

			if (compilePrio1 > compilePrio2) {
				return -1;
			} else if (compilePrio1 < compilePrio2) {
				return 1;
			} else {
				return sourceComparator.compare(m1, m2);
			}
		}

		/**
		 * If "deep" is the same, then source order win. 1) from instance init 2) from deepest inner
		 * from instance init (deepest first) 3) from static init 4) from deepest inner from static
		 * init (deepest first) 5) from deepest inner (deepest first) 7) regular anon classes from
		 * main class
		 *
		 * <br>
		 * Note, that nested inner anon. classes which do not have different non-anon. inner class
		 * ancestors, are compiled in they nesting order, opposite to rule 2)
		 *
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(IJavaElement o1, IJavaElement o2) {
			if (o1 == o2) {
				return 0;
			}
			IType m1 = (IType) o1;
			IType m2 = (IType) o2;
			return compare50(m1, m2);
		}

		private int getCompilePrio(IType anonType) {
			int compilePrio;
			Integer prio;
			if ((prio = map.get(anonType)) != null) {
				compilePrio = prio.intValue();
				if (BytecodeOutlinePlugin.DEBUG) {
					System.out.println("Using cache"); //$NON-NLS-1$
				}
			} else {
				compilePrio = getAnonCompilePriority(anonType);
				map.put(anonType, Integer.valueOf(compilePrio));
				if (BytecodeOutlinePlugin.DEBUG) {
					System.out.println("Calculating value!"); //$NON-NLS-1$
				}
			}
			return compilePrio;
		}
	}

	/**
	 * Finds a type by the simple name. see
	 * org.eclipse.jdt.internal.corext.codemanipulation.AddImportsOperation
	 *
	 * @param simpleTypeName non null
	 * @param searchScope non null
	 * @param monitor non null
	 * @return null, if no types was found, empty array if more then one type was found, or only one
	 *         element, if single match exists
	 * @throws JavaModelException on search
	 */
	public static IType[] getTypeForName(String simpleTypeName, IJavaSearchScope searchScope, IProgressMonitor monitor) throws JavaModelException {
		final List<IType> result = new ArrayList<>();
		final TypeFactory fFactory = new TypeFactory();
		TypeNameRequestor requestor = new TypeNameRequestor() {
			@Override
			public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName1, char[][] enclosingTypeNames, String path) {
				IType type = fFactory.create(packageName, simpleTypeName1, enclosingTypeNames, path, searchScope);
				if (type != null) {
					result.add(type);
				}
			}
		};
		int matchRule = SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
		new SearchEngine().searchAllTypeNames(
				null, matchRule,
				simpleTypeName.toCharArray(), matchRule,
				IJavaSearchConstants.TYPE, searchScope, requestor,
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, monitor);

		return result.toArray(new IType[result.size()]);
	}

	/**
	 * Selects the openable elements out of the given ones.
	 *
	 * @param elements the elements to filter
	 * @return the openable elements
	 */
	public static IJavaElement[] selectOpenableElements(IJavaElement[] elements) {
		List<IJavaElement> result = new ArrayList<>(elements.length);
		for (IJavaElement element : elements) {
			if (element == null) {
				continue;
			}
			switch (element.getElementType()) {
				case IJavaElement.PACKAGE_DECLARATION:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.JAVA_MODEL:
					break;
				default:
					result.add(element);
					break;
			}
		}
		return result.toArray(new IJavaElement[result.size()]);
	}
}
