/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.util.JdtHackFinder;

/* non java-doc
 * Utility methods for the Java Model. These methods should be part of
 * <code>IJavaModel</code> but unfortunately they aren't.
 * 
 * NOTE: This code has been directly copied from Java UI
 */
public class JavaModelUtility {
	
	public static IType findType(IJavaProject jproject, String str) {
		String pathStr= str.replace('.', '/') + ".java";
		IJavaElement jelement= null;
		try {
			jelement= jproject.findElement(new Path(pathStr));
		} catch (JavaModelException e) {
			// an illegal path -> no element found
		}
		IType resType= null;
		if (jelement instanceof ICompilationUnit) {
			String simpleName= Signature.getSimpleName(str);
			resType= ((ICompilationUnit)jelement).getType(simpleName);
		} else if (jelement instanceof IClassFile) {
			try {
				resType= ((IClassFile)jelement).getType();
			} catch (JavaModelException e) {
				// Fall through and return null.
			}
		}
		return resType;
	}
	
	/**
	 * Convert an import declaration into the java element the import declaration
	 * stands for. An on demand import declaration is converted into a package
	 * fragement or type depending of the kind of import statement (e.g. p1.p2.*
	 * versus p1.p2.T1.*). A normal import declaration is converted into the 
	 * corresponding <code>IType</code>.
	 */
	public static IJavaElement convertFromImportDeclaration(IImportDeclaration declaration) {
		if (declaration.isOnDemand()) {
			String pattern= declaration.getElementName();
			pattern= pattern.substring(0, pattern.length() - 2);
			IJavaProject project= declaration.getJavaProject();
			
			JdtHackFinder.fixme("1GBRLSV: ITPJCORE:WIN2000 - Question: how to I find an inner type");
			// First try if the import statement is of form p1.p2.T1.* which would lead
			// to a type not to a package.
			IJavaElement result= findType(project, pattern);
			if (result != null)
				return result;
			
			return convertToPackageFragment(pattern, project);
		} else {
			return convertToType(declaration);	
		}
	}
	
	private static IPackageFragment convertToPackageFragment(IImportDeclaration declaration) {
		String pattern= declaration.getElementName();
		pattern= pattern.substring(0, pattern.length() - 2);
		return convertToPackageFragment(pattern, declaration.getJavaProject());
	}	
		
	private static IPackageFragment convertToPackageFragment(String pattern, IJavaProject project) {
		
		try {
			// Check the project itself.
			JdtHackFinder.fixme("1GAOLWQ: ITPJCORE:WIN2000 - IJavaProject.findPackageFragment strange semantic");
			IPackageFragment[] packages= project.getPackageFragments();
			for (int i= 0; i < packages.length; i++) {
				if (pattern.equals(packages[i].getElementName()))
					return packages[i];
			}
			
			// Convert to a path and search on the class path.
			pattern= pattern.replace('.', IPath.SEPARATOR);
			IPath path= new Path(pattern).makeAbsolute();
			return project.findPackageFragment(path);
		} catch (JavaModelException e) {
			return null;
		}	
	}
	
	private static IType convertToType(IImportDeclaration declaration) {
		IJavaProject project= declaration.getJavaProject();
		return findType(project, declaration.getElementName());
	}
	
	
	private static final String SIG1= Signature.createArraySignature(Signature.createTypeSignature("String", false), 1);
	private static final String SIG2= Signature.createArraySignature(Signature.createTypeSignature("java.lang.String", false), 1);
	private static final String SIG3= Signature.createArraySignature(Signature.createTypeSignature("java.lang.String", true), 1);

	/**
	 * Checks whether the given IType has a main method or not.
	 */
	public static boolean hasMainMethod(IType type) {
		if (isStaticPublicVoidMethod(type.getMethod("main", new String[] { SIG1 })) || 
			isStaticPublicVoidMethod(type.getMethod("main", new String[] { SIG2 })) || 
			isStaticPublicVoidMethod(type.getMethod("main", new String[] { SIG3 }))) {
				return true;
		}
		
		return false;
	}
	
	public static boolean isMainMethod(IMethod method) {
		try {
			if (!isStaticPublicVoidMethod(method))
				return false;
			String signature= method.getSignature();
			if ("([Qjava.lang.String;)V".equals(signature) ||
				"([Ljava/lang/String;)V".equals(signature))
				return true;
			if ("([QString;)V".equals(signature)) {
				String[] resolvedName= StubUtility.getResolvedTypeName("QString;", method.getDeclaringType());
				if (resolvedName != null && "java.lang".equals(resolvedName[0]))
					return true;
			}
			return false;
		} catch (JavaModelException e) {
		}
		return false;
	}
	
	private static boolean isStaticPublicVoidMethod(IMethod m) {
		try {
			return "V".equals(m.getReturnType()) && Flags.isStatic(m.getFlags()) && Flags.isPublic(m.getFlags());
		} catch (JavaModelException e) {
			return false;
		}
	}
	
	
}