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
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
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
	
	public static boolean isAbstract(IMethodBinding member) {
		if (isInterfaceMember(member))
			return true;
		return Modifier.isAbstract(member.getModifiers());	
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
	
	public static boolean isPackageVisible(IBinding binding) {
		return (! isPrivate(binding) && ! isProtected(binding) && ! isPublic(binding));
	}
	
	public static boolean isPrivate(IMember member) throws JavaModelException{
		return Flags.isPrivate(member.getFlags());
	}

	public static boolean isPrivate(BodyDeclaration bodyDeclaration) {
		return Modifier.isPrivate(bodyDeclaration.getModifiers());
	}
	
	public static boolean isPrivate(IBinding binding) {
		return Modifier.isPrivate(binding.getModifiers());
	}

	public static boolean isProtected(IMember member) throws JavaModelException{
		return Flags.isProtected(member.getFlags());
	}

	public static boolean isProtected(BodyDeclaration bodyDeclaration) {
		return Modifier.isProtected(bodyDeclaration.getModifiers());
	}
	
	public static boolean isProtected(IBinding binding) {
		return Modifier.isProtected(binding.getModifiers());
	}

	public static boolean isPublic(IMember member) throws JavaModelException{
		if (isInterfaceMember(member))
			return true;
		return Flags.isPublic(member.getFlags());
	}
	
	public static boolean isPublic(IBinding binding) {
		if (isInterfaceMember(binding))
			return true;
		return Modifier.isPublic(binding.getModifiers());
	}
	

	public static boolean isPublic(BodyDeclaration bodyDeclaration) {
		if (isInterfaceMember(bodyDeclaration))
			return true;
		return Modifier.isPublic(bodyDeclaration.getModifiers());
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

	public static boolean isStatic(IVariableBinding variableBinding){
		if (isInterfaceMember(variableBinding))
			return true;
		return Modifier.isStatic(variableBinding.getModifiers());
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
	
	private static boolean isInterfaceMember(IBinding binding) {
		ITypeBinding declaringType= null;
		if (binding instanceof IVariableBinding) {
			declaringType= ((IVariableBinding) binding).getDeclaringClass();
		} else if (binding instanceof IMethodBinding) {
			declaringType= ((IMethodBinding) binding).getDeclaringClass();
		} else if (binding instanceof ITypeBinding) {
			declaringType= ((ITypeBinding) binding).getDeclaringClass();
		}
		return declaringType != null && declaringType.isInterface();
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

	public static int getVisibilityCode(IMember member) throws JavaModelException {
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
	
	public static int getVisibilityCode(BodyDeclaration bodyDeclaration) {
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
	
	public static int getVisibilityCode(IBinding binding) {
		if (isPublic(binding))
			return Modifier.PUBLIC;
		else if (isProtected(binding))
			return Modifier.PROTECTED;
		else if (isPackageVisible(binding))
			return Modifier.NONE;
		else if (isPrivate(binding))
			return Modifier.PRIVATE;
		Assert.isTrue(false);
		return VISIBILITY_CODE_INVALID;
	}
	
	
	public static String getVisibilityString(int visibilityCode){
		if (Modifier.isPublic(visibilityCode))
			return VISIBILITY_STRING_PUBLIC;
		if (Modifier.isProtected(visibilityCode))
			return VISIBILITY_STRING_PROTECTED;
		if (Modifier.isPrivate(visibilityCode))
			return VISIBILITY_STRING_PRIVATE;
		return VISIBILITY_STRING_PACKAGE;
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
	
	public static int getLowerVisibility(int visibility1, int visibility2) {
		if (isHigherVisibility(visibility1, visibility2))
			return visibility2;
		else
			return visibility1;
	}
	
	public static int clearAccessModifiers(int flags) {
		return clearFlag(Modifier.PROTECTED | Modifier.PUBLIC | Modifier.PRIVATE, flags);
	}

	public static int clearFlag(int flag, int flags){
		return flags & ~ flag;
	}
}
