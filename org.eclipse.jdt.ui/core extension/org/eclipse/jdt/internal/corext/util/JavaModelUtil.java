/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * Utility methods for the Java Model.
 */
public class JavaModelUtil {

	/** 
	 * Finds a type by its qualified type name (dot separated).
	 * @param jproject The java project to search in
	 * @param str The fully qualified name (type name with enclosing type names and package (all separated by dots))
	 * @return The type found, or null if not existing
	 * The method does not find inner types. Waiting for a Java Core solution
	 */	
	public static IType findType(IJavaProject jproject, String fullyQualifiedName) throws JavaModelException {
		String pathStr= fullyQualifiedName.replace('.', '/') + ".java"; //$NON-NLS-1$
		IJavaElement jelement= jproject.findElement(new Path(pathStr));
		if (jelement == null) {
			// try to find it as inner type
			String qualifier= Signature.getQualifier(fullyQualifiedName);
			if (qualifier.length() > 0) {
				IType type= findType(jproject, qualifier); // recursive!
				if (type != null) {
					IType res= type.getType(Signature.getSimpleName(fullyQualifiedName));
					if (res.exists()) {
						return res;
					}
				}
			}
		} else if (jelement.getElementType() == IJavaElement.COMPILATION_UNIT) {
			String simpleName= Signature.getSimpleName(fullyQualifiedName);
			return ((ICompilationUnit) jelement).getType(simpleName);
		} else if (jelement.getElementType() == IJavaElement.CLASS_FILE) {
			return ((IClassFile) jelement).getType();
		}
		return null;
	}

	/** 
	 * Finds a type by package and type name.
	 * @param jproject the java project to search in
	 * @param pack The package name
	 * @param typeQualifiedName the type qualified name (type name with enclosing type names (separated by dots))
	 * @return the type found, or null if not existing
	 */	
	public static IType findType(IJavaProject jproject, String pack, String typeQualifiedName) throws JavaModelException {
		// should be supplied from java core
		int dot= typeQualifiedName.indexOf('.');
		if (dot == -1) {
			return findType(jproject, concatenateName(pack, typeQualifiedName));
		}
		IPath packPath;
		if (pack.length() > 0) {
			packPath= new Path(pack.replace('.', '/'));
		} else {
			packPath= new Path(""); //$NON-NLS-1$
		}
		// fixed for 1GEXEI6: ITPJUI:ALL - Incorrect error message on class creation wizard
		IPath path= packPath.append(typeQualifiedName.substring(0, dot) + ".java"); //$NON-NLS-1$
		IJavaElement elem= jproject.findElement(path);
		if (elem instanceof ICompilationUnit) {
			return findTypeInCompilationUnit((ICompilationUnit)elem, typeQualifiedName);
		} else if (elem instanceof IClassFile) {
			path= packPath.append(typeQualifiedName.replace('.', '$') + ".class"); //$NON-NLS-1$
			elem= jproject.findElement(path);
			if (elem instanceof IClassFile) {
				return ((IClassFile)elem).getType();
			}
		}
		return null;
	}
	
	/**
	 * Finds a type container by container name.
	 * The returned element will be of type <code>IType</code> or a <code>IPackageFragment</code>.
	 * <code>null</code> is returned if the type container could not be found.
	 * @param jproject The Java project defining the context to search
	 * @param typeContainerName A dot separarted name of the type container
	 * @see #getTypeContainerName()
	 */
	public static IJavaElement findTypeContainer(IJavaProject jproject, String typeContainerName) throws JavaModelException {
		// try to find it as type
		IJavaElement result= findType(jproject, typeContainerName);
		if (result == null) {
			// find it as package
			IPath path= new Path(typeContainerName.replace('.', '/'));
			result= jproject.findElement(path);
			if (!(result instanceof IPackageFragment)) {
				result= null;
			}
			
		}
		return result;
	}	
	
	/** 
	 * Finds a type in a compilation unit. Typical usage is to find the corresponding
	 * type in a working copy.
	 * @param cu the compilation unit to search in
	 * @param typeQualifiedName the type qualified name (type name with enclosing type names (separated by dots))
	 * @return the type found, or null if not existing
	 */		
	public static IType findTypeInCompilationUnit(ICompilationUnit cu, String typeQualifiedName) throws JavaModelException {
		IType[] types= cu.getAllTypes();
		for (int i= 0; i < types.length; i++) {
			String currName= getTypeQualifiedName(types[i]);
			if (typeQualifiedName.equals(currName)) {
				return types[i];
			}
		}
		return null;
	}
	
	/** 
	 * Finds a a member in a compilation unit. Typical usage is to find the corresponding
	 * member in a working copy.
	 * @param cu the compilation unit (eg. working copy) to search in
	 * @param member the member (eg. from the original)
	 * @return the member found, or null if not existing
	 */		
	public static IMember findMemberInCompilationUnit(ICompilationUnit cu, IMember member) throws JavaModelException {
		if (member.getElementType() == IJavaElement.TYPE) {
			return findTypeInCompilationUnit(cu, getTypeQualifiedName((IType)member));
		} else {
			IType declaringType= findTypeInCompilationUnit(cu, getTypeQualifiedName(member.getDeclaringType()));
			if (declaringType != null) {
				IMember result= null;
				switch (member.getElementType()) {
				case IJavaElement.FIELD:
					result= declaringType.getField(member.getElementName());
					break;
				case IJavaElement.METHOD:
					IMethod meth= (IMethod) member;
					result= findMethod(meth.getElementName(), meth.getParameterTypes(), meth.isConstructor(), declaringType);
					break;
				case IJavaElement.INITIALIZER:
					result= declaringType.getInitializer(1);
					break;					
				}
				if (result != null && result.exists()) {
					return result;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Gets the primary type of a compilation unit (type with the same name as the
	 * compilation unit), or <code>null</code> if not existing.
	 */
	public static IType findPrimaryType(ICompilationUnit cu) {
		String typeName= Signature.getQualifier(cu.getElementName());
		IType primaryType= cu.getType(typeName);
		if (primaryType.exists()) {
			return primaryType;
		}
		return null;
	}
	
	
	/**
	 * Finds a JavaElement by name.
	 * 
	 * @param elements the array to choose from
	 * @param name Name of the element to find
	 * @return the first member of the given array with the name given
	 */
	private static IJavaElement find(IJavaElement[] elements, String name) {
		if (elements == null || name == null)
			return null;
			
		for (int i= 0; i < elements.length; i++) {
			if (name.equals(elements[i].getElementName()))
				return elements[i];
		}
		
		return null;
	}
	
	/** 
	 * Returns the element of the given compilation unit which is "equal" to the
	 * given element. Note that the given element usually has a parent different
	 * from the given compilation unit.
	 * 
	 * @param cu the cu to search in
	 * @param element the element to look for
	 * @return an element of the given cu "equal" to the given element
	 */		
	public static IJavaElement findInCompilationUnit(ICompilationUnit cu, IJavaElement element) throws JavaModelException {
		
		if (element instanceof IMember)
			return findMemberInCompilationUnit(cu, (IMember) element);
		
		int type= element.getElementType();
		switch (type) {
			case IJavaElement.IMPORT_CONTAINER:
				return cu.getImportContainer();
			
			case IJavaElement.PACKAGE_DECLARATION:
				return find(cu.getPackageDeclarations(), element.getElementName());
			
			case IJavaElement.IMPORT_DECLARATION:
				return find(cu.getImports(), element.getElementName());
			
			case IJavaElement.COMPILATION_UNIT:
				return cu;
		}
		
		return null;
	}
	
	/**
	 * Returns the qualified type name of the given type using '.' as separators.
	 * This is a replace for IType.getTypeQualifiedName()
	 * which uses '$' as separators. As '$' is also a valid character in an id
	 * this is ambiguous. JavaCore PR: 1GCFUNT
	 */
	public static String getTypeQualifiedName(IType type) {
		StringBuffer buf= new StringBuffer();
		getTypeQualifiedName(type, buf);
		return buf.toString();
	}
	
	private static void getTypeQualifiedName(IType type, StringBuffer buf) {
		IType outerType= type.getDeclaringType();
		if (outerType != null) {
			getTypeQualifiedName(outerType, buf);
			buf.append('.');
		}
		buf.append(type.getElementName());
	}	

	/**
	 * Returns the fully qualified name of the given type using '.' as separators.
	 * This is a replace for IType.getFullyQualifiedTypeName
	 * which uses '$' as separators. As '$' is also a valid character in an id
	 * this is ambiguous. JavaCore PR: 1GCFUNT
	 */
	public static String getFullyQualifiedName(IType type) {
		StringBuffer buf= new StringBuffer();
		String packName= type.getPackageFragment().getElementName();
		if (packName.length() > 0) {
			buf.append(packName);
			buf.append('.');
		}
		getTypeQualifiedName(type, buf);
		return buf.toString();
	}
	
	/**
	 * Returns the fully qualified name of a type's container. (package name or enclosing type name)
	 */
	public static String getTypeContainerName(IType type) {
		IType outerType= type.getDeclaringType();
		if (outerType != null) {
			return getFullyQualifiedName(outerType);
		} else {
			return type.getPackageFragment().getElementName();
		}
	}
		
	/**
	 * Returns the raw class path entry corresponding to a package fragment root
	 * or null if there isn't a corresponding entry.
	 */
	public static IClasspathEntry getRawClasspathEntry(IPackageFragmentRoot root) throws JavaModelException {
		IPath path= root.getPath();
		IClasspathEntry[] entries= root.getJavaProject().getRawClasspath();
		for (int i= 0; i < entries.length; i++) {
			IClasspathEntry curr= entries[i];
		
			if (curr.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				curr= JavaCore.getResolvedClasspathEntry(curr);
			}
			if (curr != null && curr.getContentKind() == root.getKind() && path.equals(curr.getPath())) {
				return entries[i];
			}
		}
		return null;
	}

	/**
	 * Concatenates two names. Uses a dot for separation.
	 * Both strings can be empty or <code>null</code>.
	 */
	public static String concatenateName(String name1, String name2) {
		StringBuffer buf= new StringBuffer();
		if (name1 != null && name1.length() > 0) {
			buf.append(name1);
			if (name2 != null && name2.length() > 0) {
				buf.append('.');
				buf.append(name2);
			}
		}
		return buf.toString();
	}
	
	/**
	 * Evaluates if a member (possible from another package) is visible from
	 * elements in a package.
	 * @param member The member to test the visibility for
	 * @param pack The package in focus
	 */
	public static boolean isVisible(IMember member, IPackageFragment pack) throws JavaModelException {
		int otherflags= member.getFlags();
		
		if (Flags.isPublic(otherflags) || Flags.isProtected(otherflags)) {
			return true;
		} else if (Flags.isPrivate(otherflags)) {
			return false;
		}		
		
		IPackageFragment otherpack= (IPackageFragment) findParentOfKind(member, IJavaElement.PACKAGE_FRAGMENT);
		return (pack != null && pack.equals(otherpack));
	}
		
	/**
	 * Returns true if the element is on the build path of the given project
	 */	
	public static boolean isOnBuildPath(IJavaProject jproject, IJavaElement element) throws JavaModelException {
		IPath rootPath;
		if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
			rootPath= ((IJavaProject)element).getProject().getFullPath();
		} else {
			IPackageFragmentRoot root= getPackageFragmentRoot(element);
			if (root == null) {
				return false;
			}
			rootPath= root.getPath();
		}
		return jproject.findPackageFragmentRoot(rootPath) != null;
	}
	
	
	/**
	 * Returns the package fragment root of <code>IJavaElement</code>. If the given
	 * element is already a package fragment root, the element itself is returned.
	 */
	public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
		return (IPackageFragmentRoot)findElementOfKind(element, IJavaElement.PACKAGE_FRAGMENT_ROOT);
	}

	/**
	 * Returns the first openable parent. If the given element is openable, the element
	 * itself is returned.
	 */
	public static IOpenable getOpenable(IJavaElement element) {
		while (element != null && !(element instanceof IOpenable)) {
			element= element.getParent();
		}
		return (IOpenable) element;	
	}		
	
	
	/**
	 * Returns the parent of the supplied java element that conforms to the given 
	 * parent type or <code>null</code>, if such a parent doesn't exit.
	 */
	public static IJavaElement findParentOfKind(IJavaElement element, int kind) {
		if (element == null)
			return null;
		return findElementOfKind(element.getParent(), kind);	
	}
	
	/**
	 * Returns the first java element that conforms to the given type walking the
	 * java element's parent relationship. If the given element alrady conforms to
	 * the given kind, the element is returned.
	 * Returns <code>null</code> if no such element exits.
	 */
	public static IJavaElement findElementOfKind(IJavaElement element, int kind) {
		while (element != null && element.getElementType() != kind)
			element= element.getParent();
		return element;				
	}

	/**
	 * Finds a method in a type.
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done.
	 * Constructors are only compared by parameters, not the name.
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @return The first found method or <code>null</code>, if nothing found
	 */
	public static IMethod findMethod(String name, String[] paramTypes, boolean isConstructor, IType type) throws JavaModelException {
		return findMethod(name, paramTypes, isConstructor, type.getMethods());
	}

	/**
	 * Finds a method by name.
	 * This searches for a method with a name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done.
	 * Constructors are only compared by parameters, not the name.
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @param methods The methods to search in
	 * @return The found method or <code>null</code>, if nothing found
	 */
	public static IMethod findMethod(String name, String[] paramTypes, boolean isConstructor, IMethod[] methods) throws JavaModelException {
		for (int i= methods.length - 1; i >= 0; i--) {
			if (isSameMethodSignature(name, paramTypes, isConstructor, methods[i])) {
				return methods[i];
			}
		}
		return null;
	}
	
	/**
	 * Finds a method declararion in a type's hierarchy. The search is top down, so this
	 * returns the first declaration of the method in the hierarchy.
	 * This searches for a method with a name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done.
	 * Constructors are only compared by parameters, not the name.
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @return The first method found or null, if nothing found
	 */
	public static IMethod findMethodDeclarationInHierarchy(ITypeHierarchy hierarchy, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		IType[] superTypes= hierarchy.getAllSupertypes(hierarchy.getType());
		for (int i= superTypes.length - 1; i >= 0; i--) {
			IMethod found= findMethod(name, paramTypes, isConstructor, superTypes[i]);
			if (found != null) {
				return found;
			}
		}
		return null;
	}
	
	/**
	 * Finds a method implementation in a type's hierarchy. The search is bottom-up, so this
	 * returns the overwritten method.
	 * This searches for a method with a name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done.
	 * Constructors are only compared by parameters, not the name.
	 * @param name The name of the method to find
	 * @param paramTypes The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param isConstructor If the method is a constructor
	 * @return The first method found or null, if nothing found
	 */
	public static IMethod findMethodImplementationInHierarchy(ITypeHierarchy hierarchy, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		IType[] superTypes= hierarchy.getAllSupertypes(hierarchy.getType());
		for (int i= 0; i < superTypes.length; i++) {
			IMethod found= findMethod(name, paramTypes, isConstructor, superTypes[i]);
			if (found != null) {
				return found;
			}
		}
		return null;
	}	
	
	/**
	 * Tests if a method equals to the given signature.
	 * Parameter types are only compared by the simple name, no resolving for
	 * the fully qualified type name is done. Constructors are only compared by
	 * parameters, not the name.
	 * @param Name of the method
	 * @param The type signatures of the parameters e.g. <code>{"QString;","I"}</code>
	 * @param Specifies if the method is a constructor
	 * @return Returns <code>true</code> if the method has the given name and parameter types and constructor state.
	 */
	public static boolean isSameMethodSignature(String name, String[] paramTypes, boolean isConstructor, IMethod curr) throws JavaModelException {
		if (isConstructor || name.equals(curr.getElementName())) {
			if (isConstructor == curr.isConstructor()) {
				String[] currParamTypes= curr.getParameterTypes();
				if (paramTypes.length == currParamTypes.length) {
					for (int i= 0; i < paramTypes.length; i++) {
						String t1= Signature.getSimpleName(Signature.toString(paramTypes[i]));
						String t2= Signature.getSimpleName(Signature.toString(currParamTypes[i]));
						if (!t1.equals(t2)) {
							return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks whether the given type has a valid main method or not.
	 */
	public static boolean hasMainMethod(IType type) throws JavaModelException {
		String[] paramSignature=  { Signature.createArraySignature(Signature.createTypeSignature("String", false), 1) };
		IMethod method= findMethod("main", paramSignature, false, type);
		if (method != null) {
			int flags= method.getFlags();
			return Flags.isStatic(flags) && Flags.isPublic(flags) && Signature.SIG_VOID.equals(method.getReturnType());
		}
		return false;		
	}
	
	/**
	 * Tests if a method is a main method. Does not resolve the parameter types.
	 * Method must exist.
	 */
	public static boolean isMainMethod(IMethod method) throws JavaModelException {
		if ("main".equals(method.getElementName()) && Signature.SIG_VOID.equals(method.getReturnType())) { //$NON-NLS-1$
			int flags= method.getFlags();
			if (Flags.isStatic(flags) && Flags.isPublic(flags)) {
				String[] paramTypes= method.getParameterTypes();
				if (paramTypes.length == 1) {
					String name=  Signature.toString(paramTypes[0]);
					return "String[]".equals(Signature.getSimpleName(name)); //$NON-NLS-1$
				}
			}
		}
		return false;
	}
	
	/**
	 * Resolves a type name in the context of the declaring type.
	 * @param refTypeSig the type name in signature notation (for example 'QVector')
	 *                   this can also be an array type, but dimensions will be ignored.
	 * @param declaringType the context for resolving (type where the reference was made in)
	 * @return returns the fully qualified type name or build-in-type name. 
	 *  			if a unresoved type couldn't be resolved null is returned
	 */
	public static String getResolvedTypeName(String refTypeSig, IType declaringType) throws JavaModelException {
		int arrayCount= Signature.getArrayCount(refTypeSig);
		char type= refTypeSig.charAt(arrayCount);
		if (type == Signature.C_UNRESOLVED) {
			int semi= refTypeSig.indexOf(Signature.C_SEMICOLON, arrayCount + 1);
			if (semi == -1) {
				throw new IllegalArgumentException();
			}
			String name= refTypeSig.substring(arrayCount + 1, semi);				
			
			String[][] resolvedNames= declaringType.resolveType(name);
			if (resolvedNames != null && resolvedNames.length > 0) {
				return JavaModelUtil.concatenateName(resolvedNames[0][0], resolvedNames[0][1]);
			}
			return null;
		} else {
			return Signature.toString(refTypeSig.substring(arrayCount));
		}
	}

	
}