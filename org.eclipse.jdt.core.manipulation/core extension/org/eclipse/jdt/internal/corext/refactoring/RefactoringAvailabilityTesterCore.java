/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

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

	private RefactoringAvailabilityTesterCore() {
	}
}
