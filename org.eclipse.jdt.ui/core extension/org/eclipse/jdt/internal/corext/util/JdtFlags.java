package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;

public class JdtFlags {
	private JdtFlags(){
	}
	
	public static final String VISIBILITY_STRING_PRIVATE= 	"private";		//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PACKAGE= 	"";				//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PROTECTED= 	"protected";	//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PUBLIC= 	"public";		//$NON-NLS-1$
	

	public static final int VISIBILITY_CODE_INVALID= 	-1;
	public static final int VISIBILITY_CODE_PRIVATE= 	0;
	public static final int VISIBILITY_CODE_PACKAGE=		1;
	public static final int VISIBILITY_CODE_PROTECTED=	2;
	public static final int VISIBILITY_CODE_PUBLIC= 		3;

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
		if (isAnonymousType(member))	
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
				member.getDeclaringType() != null &&
				((IType)member).isInterface();
	}

	private static boolean isAnonymousType(IMember member) throws JavaModelException {
		return member.getElementType() == IJavaElement.TYPE && 
				((IType)member).isAnonymous();
	}

	public static int getVisibilityCode(IMember member) throws JavaModelException{
		if (isPublic(member))
			return VISIBILITY_CODE_PUBLIC;
		else if (isProtected(member))
			return VISIBILITY_CODE_PROTECTED;
		else if (isPackageVisible(member))
			return VISIBILITY_CODE_PACKAGE;
		else if (isPrivate(member))
			return VISIBILITY_CODE_PRIVATE;
		Assert.isTrue(false);
		return VISIBILITY_CODE_INVALID;
	}
	
	public static String getVisibilityString(int visibilityCode){
		switch(visibilityCode){
			case VISIBILITY_CODE_PUBLIC: return VISIBILITY_STRING_PUBLIC;
			case VISIBILITY_CODE_PROTECTED: return VISIBILITY_STRING_PROTECTED;
			case VISIBILITY_CODE_PACKAGE: return VISIBILITY_STRING_PACKAGE;
			case VISIBILITY_CODE_PRIVATE: return VISIBILITY_STRING_PRIVATE;
			default:
				Assert.isTrue(false);
				return null;
		}
	}
	
	public static void assertVisibility(int visibility){
		Assert.isTrue(	visibility == JdtFlags.VISIBILITY_CODE_PUBLIC ||
		            	visibility == JdtFlags.VISIBILITY_CODE_PROTECTED ||
		            	visibility == JdtFlags.VISIBILITY_CODE_PACKAGE ||
		            	visibility == JdtFlags.VISIBILITY_CODE_PRIVATE);  
	}
	
	public static boolean isHigherVisibility(int newVisibility, int oldVisibility){
		assertVisibility(oldVisibility);
		assertVisibility(newVisibility);
		switch (oldVisibility) {
			case JdtFlags.VISIBILITY_CODE_PRIVATE :
				return 	newVisibility == JdtFlags.VISIBILITY_CODE_PACKAGE
						||	newVisibility == JdtFlags.VISIBILITY_CODE_PUBLIC
						||  newVisibility == JdtFlags.VISIBILITY_CODE_PROTECTED;
			case JdtFlags.VISIBILITY_CODE_PACKAGE :
				return 	newVisibility == JdtFlags.VISIBILITY_CODE_PUBLIC
						||  newVisibility == JdtFlags.VISIBILITY_CODE_PROTECTED;

			case JdtFlags.VISIBILITY_CODE_PROTECTED :
				return newVisibility == JdtFlags.VISIBILITY_CODE_PUBLIC;

			case JdtFlags.VISIBILITY_CODE_PUBLIC :
				return false;
			default: 
				Assert.isTrue(false);
				return false;	
		}
	}
}
