/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.Assert;

public class JdtFlags {
	private JdtFlags(){
	}
	
	public static final String VISIBILITY_STRING_PRIVATE= 	"private";		//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PACKAGE= 	"";				//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PROTECTED= 	"protected";	//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PUBLIC= 	"public";		//$NON-NLS-1$
	

	public static final int VISIBILITY_CODE_INVALID= 	-1;

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

	public static boolean isPackageVisible(BodyDeclaration bodyDeclaration) {
		return (! isPrivate(bodyDeclaration) && ! isProtected(bodyDeclaration) && ! isPublic(bodyDeclaration));
	}
	
	public static boolean isPrivate(IMember member) throws JavaModelException{
		return Flags.isPrivate(member.getFlags());
	}

	public static boolean isPrivate(BodyDeclaration bodyDeclaration) {
		return Modifier.isPrivate(getModifiers(bodyDeclaration));
	}

	public static boolean isProtected(IMember member) throws JavaModelException{
		return Flags.isProtected(member.getFlags());
	}

	public static boolean isProtected(BodyDeclaration bodyDeclaration) {
		return Modifier.isProtected(getModifiers(bodyDeclaration));
	}

	public static boolean isPublic(IMember member) throws JavaModelException{
		if (isInterfaceMember(member))
			return true;
		return Flags.isPublic(member.getFlags());
	}

	public static boolean isPublic(BodyDeclaration bodyDeclaration) {
		if (isInterfaceMember(bodyDeclaration))
			return true;
		return Modifier.isPublic(getModifiers(bodyDeclaration));
	}

	private static int getModifiers(BodyDeclaration bodyDeclaration) {
		if (bodyDeclaration instanceof MethodDeclaration)
			return ((MethodDeclaration)bodyDeclaration).getModifiers();
		else if (bodyDeclaration instanceof FieldDeclaration)
			return ((FieldDeclaration)bodyDeclaration).getModifiers();
		else if (bodyDeclaration instanceof TypeDeclaration)
			return ((TypeDeclaration)bodyDeclaration).getModifiers();
		else if (bodyDeclaration instanceof Initializer)
			return ((Initializer)bodyDeclaration).getModifiers();
		Assert.isTrue(false, "unexpected type of BodyDeclaration");
		return -1;
	}

	public static boolean isStatic(IMember member) throws JavaModelException{
		if (isNestedInterface(member))
			return true;
		if (member.getElementType() != IJavaElement.METHOD && isInterfaceMember(member))
			return true;
		return Flags.isStatic(member.getFlags());
	}

	public static boolean isStatic(IMethodBinding methodBinding){
		return Modifier.isStatic(methodBinding.getModifiers());
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
	
	private static boolean isInterfaceMember(BodyDeclaration bodyDeclaration) {
		return 	(bodyDeclaration.getParent() instanceof TypeDeclaration) &&
				((TypeDeclaration)bodyDeclaration.getParent()).isInterface();
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
			return Modifier.PUBLIC;
		else if (isProtected(member))
			return Modifier.PROTECTED;
		else if (isPackageVisible(member))
			return Modifier.NONE;
		else if (isPrivate(member))
			return Modifier.PRIVATE;
		Assert.isTrue(false);
		return VISIBILITY_CODE_INVALID;
	}
	
	public static int getVisibilityCode(BodyDeclaration bodyDeclaration) throws JavaModelException{
		if (isPublic(bodyDeclaration))
			return Modifier.PUBLIC;
		else if (isProtected(bodyDeclaration))
			return Modifier.PROTECTED;
		else if (isPackageVisible(bodyDeclaration))
			return Modifier.NONE;
		else if (isPrivate(bodyDeclaration))
			return Modifier.PRIVATE;
		Assert.isTrue(false);
		return VISIBILITY_CODE_INVALID;
	}
	
	public static String getVisibilityString(int visibilityCode){
		switch(visibilityCode){
			case Modifier.PUBLIC: return VISIBILITY_STRING_PUBLIC;
			case Modifier.PROTECTED: return VISIBILITY_STRING_PROTECTED;
			case Modifier.NONE: return VISIBILITY_STRING_PACKAGE;
			case Modifier.PRIVATE: return VISIBILITY_STRING_PRIVATE;
			default:
				Assert.isTrue(false);
				return null;
		}
	}
	
	public static void assertVisibility(int visibility){
		Assert.isTrue(	visibility == Modifier.PUBLIC ||
		            	visibility == Modifier.PROTECTED ||
		            	visibility == Modifier.NONE ||
		            	visibility == Modifier.PRIVATE);  
	}
	
	public static boolean isHigherVisibility(int newVisibility, int oldVisibility){
		assertVisibility(oldVisibility);
		assertVisibility(newVisibility);
		switch (oldVisibility) {
			case Modifier.PRIVATE :
				return 	newVisibility == Modifier.NONE
						||	newVisibility == Modifier.PUBLIC
						||  newVisibility == Modifier.PROTECTED;
			case Modifier.NONE :
				return 	newVisibility == Modifier.PUBLIC
						||  newVisibility == Modifier.PROTECTED;

			case Modifier.PROTECTED :
				return newVisibility == Modifier.PUBLIC;

			case Modifier.PUBLIC :
				return false;
			default: 
				Assert.isTrue(false);
				return false;	
		}
	}

	public static int clearAccessModifiers(int flags) {
		return clearFlag(Modifier.PROTECTED | Modifier.PUBLIC | Modifier.PRIVATE, flags);
	}

	public static int clearFlag(int flag, int flags){
		return flags & ~ flag;
	}
}
