package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Checks;

public class JdtFlags {
	private JdtFlags(){
	}
	
	public static boolean isAbstract(IMember member) throws JavaModelException{
		if (isInterfaceMethod(member))
			return true;
		return Flags.isAbstract(member.getFlags());	
	}

	public static boolean isDeprecated(IMember member) throws JavaModelException{
		return Flags.isDeprecated(member.getFlags());
	}

	public static boolean isFinal(IMember member) throws JavaModelException{
		if (isInterfaceField(member))
			return true;
		return Flags.isFinal(member.getFlags());
	}

	public static boolean isNative(IMember member) throws JavaModelException{
		return Flags.isNative(member.getFlags());
	}

	public static boolean isPackageVisible(IMember member) throws JavaModelException{
		return (! isPrivate(member) && ! isProtected(member) && ! isPublic(member));
	}
	
	public static boolean isPrivate(IMember member) throws JavaModelException{
		return Flags.isPrivate(member.getFlags());
	}

	public static boolean isProtected(IMember member) throws JavaModelException{
		return Flags.isProtected(member.getFlags());
	}

	public static boolean isPublic(IMember member) throws JavaModelException{
		if (isInterfaceMember(member))
			return true;
		return Flags.isPublic(member.getFlags());
	}

	public static boolean isStatic(IMember member) throws JavaModelException{
		if (isNestedInterface(member))
			return true;
		if (isInterfaceField(member))
			return true;
		return Flags.isStatic(member.getFlags());
	}

	public static boolean isStrictfp(IMember member) throws JavaModelException{
		return Flags.isStrictfp(member.getFlags());
	}

	public static boolean isSynchronized(IMember member) throws JavaModelException{
		return Flags.isSynchronized(member.getFlags());
	}

	public static boolean isSynthetic(IMember member) throws JavaModelException{
		return Flags.isSynthetic(member.getFlags());
	}

	public static boolean isTransient(IMember member) throws JavaModelException{
		return Flags.isTransient(member.getFlags());
	}

	public static boolean isVolatile(IMember member) throws JavaModelException{
		return Flags.isVolatile(member.getFlags());
	}
	
	private static boolean isInterfaceMethod(IMember member) throws JavaModelException {
		return member.getElementType() == IJavaElement.METHOD && isInterfaceMember(member);
	}

	private static boolean isInterfaceField(IMember member) throws JavaModelException {
		return member.getElementType() == IJavaElement.FIELD && isInterfaceMember(member);
	}

	private static boolean isInterfaceMember(IMember member) throws JavaModelException {
		return member.getDeclaringType() != null && member.getDeclaringType().isInterface();
	}
	private static boolean isNestedInterface(IMember member) throws JavaModelException{
		return member.getElementType() == IJavaElement.TYPE && 
				!Checks.isTopLevel((IType)member) &&
				((IType)member).isInterface();
	}
	
}
