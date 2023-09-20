/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - based this file on RefactoringAvailabilityTester
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtilsCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

/**
 * Helper class to detect whether a certain refactoring can be enabled on a
 * selection.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code,
 * in order not to eagerly load refactoring classes during action
 * initialization.
 * </p>
 *
 * @since 1.12
 */
public final class RefactoringAvailabilityTesterCore  {


	public static boolean isDeleteAvailable(final IJavaElement element) {
		if (!element.exists())
			return false;
		if (element instanceof IJavaModel || element instanceof IJavaProject)
			return false;
		if (element.getParent() != null && element.getParent().isReadOnly())
			return false;
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) element;
			if (root.isExternal() || Checks.isClasspathDelete(root)) // TODO: rename isClasspathDelete
				return false;

			if (root.getResource().equals(root.getJavaProject().getProject()))
				return false;
		}
		if (element instanceof IPackageFragment && ((IPackageFragment) element).isDefaultPackage()) {
			return false;
		}
		if (element.getResource() == null && !RefactoringAvailabilityTesterCore.isWorkingCopyElement(element))
			return false;
		if (element instanceof IMember && ((IMember) element).isBinary())
			return false;
		return true;
	}

	public static boolean isMoveStaticMembersAvailable(final IMember[] members) throws JavaModelException {
		if (members == null)
			return false;
		if (members.length == 0)
			return false;
		if (!isMoveStaticAvailable(members))
			return false;
		if (!isCommonDeclaringType(members))
			return false;
		return true;
	}

	public static boolean isMoveStaticAvailable(final IMember[] members) throws JavaModelException {
		for (IMember member : members) {
			if (!isMoveStaticAvailable(member))
				return false;
		}
		return true;
	}

	public static boolean isMoveStaticAvailable(final IMember member) throws JavaModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaElement.METHOD && type != IJavaElement.FIELD && type != IJavaElement.TYPE)
			return false;
		if (JdtFlags.isEnum(member) && type != IJavaElement.TYPE)
			return false;
		final IType declaring= member.getDeclaringType();
		if (declaring == null)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (type == IJavaElement.METHOD && declaring.isInterface()) {
			boolean is18OrHigher= JavaModelUtil.is1d8OrHigher(member.getJavaProject());
			if (!is18OrHigher || !Flags.isStatic(member.getFlags()))
				return false;
		}
		if (type == IJavaElement.METHOD && !JdtFlags.isStatic(member))
			return false;
		if (type == IJavaElement.METHOD && ((IMethod) member).isConstructor())
			return false;
		if (type == IJavaElement.TYPE && !JdtFlags.isStatic(member))
			return false;
		if (!declaring.isInterface() && !JdtFlags.isStatic(member))
			return false;
		return true;
	}

	public static boolean isCommonDeclaringType(final IMember[] members) {
		if (members.length == 0)
			return false;
		final IType type= members[0].getDeclaringType();
		if (type == null)
			return false;
		for (IMember member : members) {
			if (!type.equals(member.getDeclaringType()))
				return false;
		}
		return true;
	}

	public static boolean isDelegateCreationAvailable(final IField field) throws JavaModelException {
		return field.exists() && (Flags.isStatic(field.getFlags()) && Flags.isFinal(field.getFlags()) /*
		 * &&
		 * hasInitializer(field)
		 */);
	}

	public static boolean isInlineTempAvailable(final ILocalVariable variable) throws JavaModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isInlineConstantAvailable(final IField field) throws JavaModelException {
		return Checks.isAvailable(field) && JdtFlags.isStatic(field) && JdtFlags.isFinal(field) && !JdtFlags.isEnum(field);
	}

	public static boolean isInlineMethodAvailable(IMethod method) throws JavaModelException {
		if (method == null)
			return false;
		if (!method.exists())
			return false;
		if (!method.isStructureKnown())
			return false;
		if (!method.isBinary())
			return true;
		if (method.isConstructor())
			return false;
		return SourceRange.isAvailable(method.getNameRange());
	}

	public static ASTNode getInlineableMethodNode(ITypeRoot typeRoot, CompilationUnit root, int offset, int length) {
		ASTNode node= null;
		try {
			node= getInlineableMethodNode(NodeFinder.perform(root, offset, length, typeRoot), typeRoot);
		} catch(JavaModelException e) {
			// Do nothing
		}
		if (node != null)
			return node;
		return getInlineableMethodNode(NodeFinder.perform(root, offset, length), typeRoot);
	}

	private static ASTNode getInlineableMethodNode(ASTNode node, IJavaElement unit) {
		if (node == null)
			return null;
		switch (node.getNodeType()) {
			case ASTNode.SIMPLE_NAME:
				StructuralPropertyDescriptor locationInParent= node.getLocationInParent();
				if (locationInParent == MethodDeclaration.NAME_PROPERTY) {
					return node.getParent();
				} else if (locationInParent == MethodInvocation.NAME_PROPERTY
						|| locationInParent == SuperMethodInvocation.NAME_PROPERTY) {
					return unit instanceof ICompilationUnit ? node.getParent() : null; // don't start on invocations in binary
				}
				return null;
			case ASTNode.EXPRESSION_STATEMENT:
				node= ((ExpressionStatement)node).getExpression();
		}
		switch (node.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				return node;
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return unit instanceof ICompilationUnit ? node : null; // don't start on invocations in binary
		}
		return null;
	}

	public static boolean isChangeSignatureAvailable(final IMethod method) throws JavaModelException {
		return (method != null) && Checks.isAvailable(method) && !Flags.isAnnotation(method.getDeclaringType().getFlags());
	}


	public static IType getDeclaringType(IJavaElement element) {
		if (element == null)
			return null;
		if (!(element instanceof IType))
			element= element.getAncestor(IJavaElement.TYPE);
		return (IType) element;
	}

	public static IJavaElement[] getJavaElements(final Object[] elements) {
		List<IJavaElement> result= new ArrayList<>();
		for (Object element : elements) {
			if (element instanceof IJavaElement) {
				result.add((IJavaElement) element);
			}
		}
		return result.toArray(new IJavaElement[result.size()]);
	}

	public static IMember[] getPullUpMembers(final IType type) throws JavaModelException {
		final List<IMember> list= new ArrayList<>(3);
		if (type.exists()) {
			for (IMember member : type.getFields()) {
				if (isPullUpAvailable(member)) {
					list.add(member);
				}
			}
			for (IMember member : type.getMethods()) {
				if (isPullUpAvailable(member)) {
					list.add(member);
				}
			}
			for (IMember member : type.getTypes()) {
				if (isPullUpAvailable(member)) {
					list.add(member);
				}
			}
		}
		return list.toArray(new IMember[list.size()]);
	}

	public static IMember[] getPushDownMembers(final IType type) throws JavaModelException {
		final List<IMember> list= new ArrayList<>(3);
		if (type.exists()) {
			for (IMember member : type.getFields()) {
				if (isPushDownAvailable(member)) {
					list.add(member);
				}
			}
			for (IMember member : type.getMethods()) {
				if (isPushDownAvailable(member)) {
					list.add(member);
				}
			}
		}
		return list.toArray(new IMember[list.size()]);
	}

	public static IResource[] getResources(final Object[] elements) {
		List<IResource> result= new ArrayList<>();
		for (Object element : elements) {
			if (element instanceof IResource) {
				result.add((IResource) element);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	public static IType getTopLevelType(final IMember[] members) {
		if (members != null && members.length == 1 && Checks.isTopLevelType(members[0]))
			return (IType) members[0];
		return null;
	}

	public static boolean isCanonicalConstructor(IMethod method) {
		boolean isCanonicalConstructor = false;
		try {
			if (method != null && method.isConstructor()) {
				CompilationUnit cUnit= SharedASTProviderCore.getAST(method.getCompilationUnit(), SharedASTProviderCore.WAIT_YES, null);
				if (cUnit != null) {
					MethodDeclaration mDecl= ASTNodeSearchUtil.getMethodDeclarationNode(method, cUnit);
					if (mDecl != null) {
						IMethodBinding mBinding= mDecl.resolveBinding();
						if (mBinding != null && mBinding.isCanonicalConstructor()) {
							isCanonicalConstructor= true;
						}
					}
				}
			}
		} catch (JavaModelException e) {
			//do nothing
		}
		return isCanonicalConstructor;
	}

	public static boolean isConvertAnonymousAvailable(final IType type) throws JavaModelException {
		if (Checks.isAvailable(type)) {
			final IJavaElement element= type.getParent();
			if (element instanceof IField && JdtFlags.isEnum((IMember) element))
				return false;
			return type.isAnonymous();
		}
		return false;
	}

	public static boolean isDeleteAvailable(final IResource resource) {
		if (!resource.exists() || resource.isPhantom())
			return false;
		if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT)
			return false;
		return true;
	}

	public static boolean isExtractInterfaceAvailable(final IType type) throws JavaModelException {
		return Checks.isAvailable(type) && !type.isBinary() && !type.isReadOnly() && !type.isAnnotation() && !type.isAnonymous() && !type.isLambda();
	}

	public static boolean isExtractMethodAvailable(final ASTNode[] nodes) {
		if (nodes != null && nodes.length != 0) {
			if (nodes.length == 1)
				return nodes[0] instanceof Statement || Checks.isExtractableExpression(nodes[0]);
			else {
				for (ASTNode node : nodes) {
					if (!(node instanceof Statement)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isExtractSupertypeAvailable(IMember member) throws JavaModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaElement.METHOD && type != IJavaElement.FIELD && type != IJavaElement.TYPE)
			return false;
		if (JdtFlags.isEnum(member) && type != IJavaElement.TYPE)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (member instanceof IMethod) {
			final IMethod method= (IMethod) member;
			if (method.isConstructor())
				return false;
			if (JdtFlags.isNative(method))
				return false;
			member= method.getDeclaringType();
		} else if (member instanceof IField) {
			member= member.getDeclaringType();
		}
		if (member instanceof IType) {
			if (JdtFlags.isEnum(member) || JdtFlags.isAnnotation(member))
				return false;
			if (member.getDeclaringType() != null && !JdtFlags.isStatic(member))
				return false;
			if (((IType)member).isAnonymous())
				return false; // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=253727
			if (((IType)member).isLambda())
				return false;
		}
		return true;
	}

	public static boolean isExtractSupertypeAvailable(final IMember[] members) throws JavaModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null && !type.isClass())
				return false;
			for (IMember member : members) {
				if (!isExtractSupertypeAvailable(member)) {
					return false;
				}
			}
			return members.length == 1 || isCommonDeclaringType(members);
		}
		return false;
	}

	public static boolean isGeneralizeTypeAvailable(final IJavaElement element) throws JavaModelException {
		if (element != null && element.exists()) {
			String type= null;
			if (element instanceof IMethod)
				type= ((IMethod) element).getReturnType();
			else if (element instanceof IField) {
				final IField field= (IField) element;
				if (JdtFlags.isEnum(field))
					return false;
				type= field.getTypeSignature();
			} else if (element instanceof ILocalVariable)
				return true;
			else if (element instanceof IType) {
				final IType clazz= (IType) element;
				if (JdtFlags.isEnum(clazz))
					return false;
				return true;
			}
			if (type == null || PrimitiveType.toCode(Signature.toString(type)) != null)
				return false;
			return true;
		}
		return false;
	}

	public static boolean isInferTypeArgumentsAvailable(final IJavaElement element) throws JavaModelException {
		if (!Checks.isAvailable(element)) {
			return false;
		} else if (element instanceof IJavaProject) {
			IJavaProject project= (IJavaProject) element;
			for (IClasspathEntry classpathEntry : project.getRawClasspath()) {
				if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					return true;
				}
			}
			return false;
		} else if (element instanceof IPackageFragmentRoot) {
			return ((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE;
		} else if (element instanceof IPackageFragment) {
			return ((IPackageFragment) element).getKind() == IPackageFragmentRoot.K_SOURCE;
		} else if (element instanceof ICompilationUnit
				|| element.getAncestor(IJavaElement.COMPILATION_UNIT) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isInferTypeArgumentsAvailable(final IJavaElement[] elements) throws JavaModelException {
		if (elements.length == 0)
			return false;

		for (IJavaElement element : elements) {
			if (!(isInferTypeArgumentsAvailable(element))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isIntroduceFactoryAvailable(final IMethod method) throws JavaModelException {
		return Checks.isAvailable(method) && method.isConstructor();
	}

	public static boolean isIntroduceIndirectionAvailable(IMethod method) throws JavaModelException {
		if (method == null)
			return false;
		if (!method.exists())
			return false;
		if (!method.isStructureKnown())
			return false;
		if (method.isConstructor())
			return false;
		if (method.getDeclaringType().isAnnotation())
			return false;
		if (JavaModelUtil.isPolymorphicSignature(method))
			return false;

		return true;
	}

	public static boolean isIntroduceParameterAvailable(final ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static boolean isMoveInnerAvailable(final IType type) throws JavaModelException {
		return Checks.isAvailable(type) && !Checks.isAnonymous(type) && !JavaElementUtil.isMainType(type) && !Checks.isInsideLocalType(type);
	}

	public static boolean isMoveMethodAvailable(final IMethod method) throws JavaModelException {
		return method.exists() && !method.isConstructor() && !method.isBinary() && !method.isReadOnly()
				&& !JdtFlags.isStatic(method) && (JdtFlags.isDefaultMethod(method) || !method.getDeclaringType().isInterface());
	}
	public static boolean isPromoteTempAvailable(final ILocalVariable variable) throws JavaModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isPullUpAvailable(IMember member) throws JavaModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaElement.METHOD && type != IJavaElement.FIELD && type != IJavaElement.TYPE)
			return false;
		if (JdtFlags.isEnum(member) && type != IJavaElement.TYPE)
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (member instanceof IType) {
			if (!JdtFlags.isStatic(member) && !JdtFlags.isEnum(member) && !JdtFlags.isAnnotation(member))
				return false;
		}
		if (member instanceof IMethod) {
			final IMethod method= (IMethod) member;
			if (method.isConstructor())
				return false;
			if (JdtFlags.isNative(method))
				return false;
			final IType declaring= method.getDeclaringType();
			if (declaring != null && declaring.isAnnotation())
				return false;
		}
		return true;
	}

	public static boolean isPullUpAvailable(final IMember[] members) throws JavaModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null && getPullUpMembers(type).length != 0)
				return true;
			for (IMember member : members) {
				if (!isPullUpAvailable(member)) {
					return false;
				}
			}
			return isCommonDeclaringType(members);
		}
		return false;
	}

	public static boolean isPushDownAvailable(final IMember member) throws JavaModelException {
		if (!member.exists())
			return false;
		final int type= member.getElementType();
		if (type != IJavaElement.METHOD && type != IJavaElement.FIELD)
			return false;
		if (JdtFlags.isEnum(member))
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (JdtFlags.isStatic(member))
			return false;
		if (type == IJavaElement.METHOD) {
			final IMethod method= (IMethod) member;
			if (method.isConstructor())
				return false;
			if (JdtFlags.isNative(method))
				return false;
			final IType declaring= method.getDeclaringType();
			if (declaring != null && declaring.isAnnotation())
				return false;
		}
		return true;
	}

	public static boolean isPushDownAvailable(final IMember[] members) throws JavaModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null && RefactoringAvailabilityTesterCore.getPushDownMembers(type).length != 0)
				return true;
			if (type != null && JdtFlags.isEnum(type))
				return false;
			for (IMember member : members) {
				if (!isPushDownAvailable(member)) {
					return false;
				}
			}
			return isCommonDeclaringType(members);
		}
		return false;
	}
	public static boolean isRenameAvailable(final ICompilationUnit unit) {
		if (unit == null)
			return false;
		if (!unit.exists())
			return false;
		if (!JavaModelUtil.isPrimary(unit))
			return false;
		if (unit.isReadOnly())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IJavaProject project) throws JavaModelException {
		if (project == null)
			return false;
		if (!Checks.isAvailable(project))
			return false;
		if (!project.isConsistent())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IModuleDescription module) throws JavaModelException {
		return Checks.isAvailable(module);
	}

	public static boolean isRenameAvailable(final ILocalVariable variable) throws JavaModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isRenameAvailable(final IMethod method) throws CoreException {
		if (method == null)
			return false;
		if (!Checks.isAvailable(method))
			return false;
		if (method.isConstructor())
			return false;
		if (isRenameProhibited(method))
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IPackageFragment fragment) throws JavaModelException {
		if (fragment == null)
			return false;
		if (!Checks.isAvailable(fragment))
			return false;
		if (fragment.isDefaultPackage())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IPackageFragmentRoot root) throws JavaModelException {
		if (root == null)
			return false;
		if (!Checks.isAvailable(root))
			return false;
		if (root.isArchive())
			return false;
		if (root.isExternal())
			return false;
		if (!root.isConsistent())
			return false;
		if (root.getResource() instanceof IProject)
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IResource resource) {
		if (resource == null)
			return false;
		if (!resource.exists())
			return false;
		if (!resource.isAccessible())
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final IType type) throws JavaModelException {
		if (type == null)
			return false;
		if (type.isAnonymous())
			return false;
		if (type.isLambda())
			return false;
		if (!Checks.isAvailable(type))
			return false;
		if (isRenameProhibited(type))
			return false;
		return true;
	}

	public static boolean isRenameAvailable(final ITypeParameter parameter) throws JavaModelException {
		return Checks.isAvailable(parameter);
	}

	public static boolean isRenameEnumConstAvailable(final IField field) throws JavaModelException {
		return Checks.isAvailable(field) && field.getDeclaringType().isEnum();
	}

	public static boolean isRenameFieldAvailable(final IField field) throws JavaModelException {
		return Checks.isAvailable(field) && !JdtFlags.isEnum(field);
	}

	public static boolean isRenameModuleAvailable(final IModuleDescription module) throws JavaModelException {
		return Checks.isAvailable(module);
	}

	public static boolean isRenameNonVirtualMethodAvailable(final IMethod method) throws JavaModelException, CoreException {
		return isRenameAvailable(method) && !MethodChecks.isVirtual(method);
	}

	public static boolean isRenameProhibited(final IMethod method) throws CoreException {
		if ("toString".equals(method.getElementName()) //$NON-NLS-1$
				&& (method.getNumberOfParameters() == 0) && ("Ljava.lang.String;".equals(method.getReturnType()) //$NON-NLS-1$
						|| "QString;".equals(method.getReturnType()) //$NON-NLS-1$
						|| "Qjava.lang.String;".equals(method.getReturnType()))) //$NON-NLS-1$
			return true;
		else
			return false;
	}

	public static boolean isRenameProhibited(final IType type) {
		return "java.lang".equals(type.getPackageFragment().getElementName()); //$NON-NLS-1$
	}

	public static boolean isRenameVirtualMethodAvailable(final IMethod method) throws CoreException {
		return isRenameAvailable(method) && MethodChecks.isVirtual(method);
	}

	public static boolean isRenameElementAvailable(IJavaElement element) throws CoreException {
		return isRenameElementAvailable(element, false);
	}

	public static boolean isRenameElementAvailable(IJavaElement element, boolean isTextSelection) throws CoreException {
		if (element != null) {
			switch (element.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					return isRenameAvailable((IJavaProject) element);
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					return isRenameAvailable((IPackageFragmentRoot) element);
				case IJavaElement.PACKAGE_FRAGMENT:
					return isRenameAvailable((IPackageFragment) element);
				case IJavaElement.COMPILATION_UNIT:
					return isRenameAvailable((ICompilationUnit) element);
				case IJavaElement.TYPE:
					return isRenameAvailable((IType) element);
				case IJavaElement.METHOD:
					final IMethod method= (IMethod) element;
					if (method.isConstructor())
						return isRenameAvailable(method.getDeclaringType());
					else
						return isRenameAvailable(method);
				case IJavaElement.FIELD:
					final IField field= (IField) element;
					if (Flags.isEnum(field.getFlags()))
						return isRenameEnumConstAvailable(field);
					else
						return isRenameFieldAvailable(field);
				case IJavaElement.TYPE_PARAMETER:
					return isRenameAvailable((ITypeParameter) element);
				case IJavaElement.LOCAL_VARIABLE:
					return isRenameAvailable((ILocalVariable) element);
				case IJavaElement.JAVA_MODULE: {
					return isRenameAvailable((IModuleDescription) element);
				}
				default:
					break;
			}
		}
		return false;
	}

	public static boolean isReplaceInvocationsAvailable(IMethod method) throws JavaModelException {
		if (method == null)
			return false;
		if (!method.exists())
			return false;
		if (method.isConstructor())
			return false;
		return true;
	}

	public static boolean isSelfEncapsulateAvailable(IField field) throws JavaModelException {
		return Checks.isAvailable(field) && !JdtFlags.isEnum(field) && !field.getDeclaringType().isInterface();
	}

	public static boolean isUseSuperTypeAvailable(final IType type) throws JavaModelException {
		return type != null && type.exists() && !type.isAnnotation() && !type.isAnonymous() && !type.isLambda();
	}

	public static boolean isWorkingCopyElement(final IJavaElement element) {
		if (element instanceof ICompilationUnit)
			return ((ICompilationUnit) element).isWorkingCopy();
		if (ReorgUtilsCore.isInsideCompilationUnit(element))
			return ReorgUtilsCore.getCompilationUnit(element).isWorkingCopy();
		return false;
	}

	public static boolean isExtractClassAvailable(IType type) throws JavaModelException {
		if (type == null)
			return false;
		if (!type.exists())
			return false;
		return ReorgUtilsCore.isInsideCompilationUnit(type) && type.isClass() && !type.isAnonymous()  && !type.isLambda();
	}


	private RefactoringAvailabilityTesterCore() {
	}
}
