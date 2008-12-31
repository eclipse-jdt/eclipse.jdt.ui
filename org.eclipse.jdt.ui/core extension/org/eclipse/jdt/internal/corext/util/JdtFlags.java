/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;


/**
 * The methods of this utility class are implemented from a Java language model
 * point of view. They do not only take the declared flags from the source into account
 * but also the implicit properties as defined in the Java Language Specification, version 3 (JLS3).
 */
public class JdtFlags {
	private JdtFlags(){
	}

	public static final String VISIBILITY_STRING_PRIVATE= 	"private";		//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PACKAGE= 	"";				//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PROTECTED= 	"protected";	//$NON-NLS-1$
	public static final String VISIBILITY_STRING_PUBLIC= 	"public";		//$NON-NLS-1$


	public static final int VISIBILITY_CODE_INVALID= 	-1;

	public static boolean isAbstract(IMember member) throws JavaModelException{
		if (isInterfaceOrAnnotationMethod(member))
			return true;
		return Flags.isAbstract(member.getFlags());
	}

	public static boolean isAbstract(IMethodBinding member) {
		if (isInterfaceOrAnnotationMember(member))
			return true;
		return Modifier.isAbstract(member.getModifiers());
	}

	public static boolean isDeprecated(IMember member) throws JavaModelException{
		return Flags.isDeprecated(member.getFlags());
	}

	public static boolean isFinal(IMember member) throws JavaModelException{
		if (isInterfaceOrAnnotationField(member))
			return true;
		if (isAnonymousType(member))
			return true;
		if (isEnumConstant(member))
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
		if (isInterfaceOrAnnotationMember(member))
			return true;
		if (isEnumConstant(member))
			return true;
		return Flags.isPublic(member.getFlags());
	}

	public static boolean isPublic(IBinding binding) {
		if (isInterfaceOrAnnotationMember(binding))
			return true;
		return Modifier.isPublic(binding.getModifiers());
	}


	public static boolean isPublic(BodyDeclaration bodyDeclaration) {
		if (isInterfaceOrAnnotationMember(bodyDeclaration))
			return true;
		return Modifier.isPublic(bodyDeclaration.getModifiers());
	}

	public static boolean isStatic(IMember member) throws JavaModelException{
		if (isNestedInterfaceOrAnnotation(member))
			return true;
		if (member.getElementType() != IJavaElement.METHOD && isInterfaceOrAnnotationMember(member))
			return true;
		if (isEnumConstant(member))
			return true;
		return Flags.isStatic(member.getFlags());
	}

	public static boolean isStatic(IMethodBinding methodBinding){
		return Modifier.isStatic(methodBinding.getModifiers());
	}

	public static boolean isStatic(IVariableBinding variableBinding){
		if (isInterfaceOrAnnotationMember(variableBinding))
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

	public static boolean isAnnotation(IMember member) throws JavaModelException{
		return Flags.isAnnotation(member.getFlags());
	}

	public static boolean isEnum(IMember member) throws JavaModelException{
		return Flags.isEnum(member.getFlags());
	}

	public static boolean isVarargs(IMethod method) throws JavaModelException{
		return Flags.isVarargs(method.getFlags());
	}

	public static boolean isTransient(IMember member) throws JavaModelException{
		return Flags.isTransient(member.getFlags());
	}

	public static boolean isVolatile(IMember member) throws JavaModelException{
		return Flags.isVolatile(member.getFlags());
	}

	private static boolean isInterfaceOrAnnotationMethod(IMember member) throws JavaModelException {
		return member.getElementType() == IJavaElement.METHOD && isInterfaceOrAnnotationMember(member);
	}

	private static boolean isInterfaceOrAnnotationField(IMember member) throws JavaModelException {
		return member.getElementType() == IJavaElement.FIELD && isInterfaceOrAnnotationMember(member);
	}

	private static boolean isInterfaceOrAnnotationMember(IMember member) throws JavaModelException {
		return member.getDeclaringType() != null && JavaModelUtil.isInterfaceOrAnnotation(member.getDeclaringType());
	}

	private static boolean isInterfaceOrAnnotationMember(IBinding binding) {
		ITypeBinding declaringType= null;
		if (binding instanceof IVariableBinding) {
			declaringType= ((IVariableBinding) binding).getDeclaringClass();
		} else if (binding instanceof IMethodBinding) {
			declaringType= ((IMethodBinding) binding).getDeclaringClass();
		} else if (binding instanceof ITypeBinding) {
			declaringType= ((ITypeBinding) binding).getDeclaringClass();
		}
		return declaringType != null && (declaringType.isInterface() || declaringType.isAnnotation());
	}

	private static boolean isInterfaceOrAnnotationMember(BodyDeclaration bodyDeclaration) {
		boolean isInterface= (bodyDeclaration.getParent() instanceof TypeDeclaration) &&
				((TypeDeclaration)bodyDeclaration.getParent()).isInterface();
		boolean isAnnotation= bodyDeclaration.getParent() instanceof AnnotationTypeDeclaration;
		return 	isInterface || isAnnotation;
	}

	private static boolean isNestedInterfaceOrAnnotation(IMember member) throws JavaModelException{
		return member.getElementType() == IJavaElement.TYPE &&
				member.getDeclaringType() != null &&
				JavaModelUtil.isInterfaceOrAnnotation((IType)member);
	}

	private static boolean isEnumConstant(IMember member) throws JavaModelException {
		return member.getElementType() == IJavaElement.FIELD && isEnum(member);
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

	public static int getVisibilityCode(String visibilityString) {
		Assert.isNotNull(visibilityString);
		if (VISIBILITY_STRING_PACKAGE.equals(visibilityString))
			return 0;
		else if (VISIBILITY_STRING_PRIVATE.equals(visibilityString))
			return Modifier.PRIVATE;
		else if (VISIBILITY_STRING_PROTECTED.equals(visibilityString))
			return Modifier.PROTECTED;
		else if (VISIBILITY_STRING_PUBLIC.equals(visibilityString))
			return Modifier.PUBLIC;
		return VISIBILITY_CODE_INVALID;
	}

	public static void assertVisibility(int visibility){
		Assert.isTrue(	visibility == Modifier.PUBLIC ||
		            	visibility == Modifier.PROTECTED ||
		            	visibility == Modifier.NONE ||
		            	visibility == Modifier.PRIVATE);
	}

	/**
	 * Compares two visibilities.
	 *
	 * @param newVisibility the 'new' visibility
	 * @param oldVisibility the 'old' visibility
	 * @return <code>true</code> iff the 'new' visibility is strictly higher than the old visibility
	 *
	 * @see Modifier#PUBLIC
	 * @see Modifier#PROTECTED
	 * @see Modifier#NONE
	 * @see Modifier#PRIVATE
	 */
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
