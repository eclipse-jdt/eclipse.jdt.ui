/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Statement;

import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringActions;

public final class RefactoringAvailabilityTester {

	public static IType getDeclaringType(IJavaElement element) {
		if (element == null)
			return null;
		if (!(element instanceof IType))
			element= element.getAncestor(IJavaElement.TYPE);
		return (IType) element;
	}

	public static IMember[] getPullUpMembers(final IType type) throws JavaModelException {
		final List list= new ArrayList(3);
		IMember[] members= type.getFields();
		for (int index= 0; index < members.length; index++) {
			if (isPullUpAvailable(members[index]))
				list.add(members[index]);
		}
		members= type.getMethods();
		for (int index= 0; index < members.length; index++) {
			if (isPullUpAvailable(members[index]))
				list.add(members[index]);
		}
		members= type.getTypes();
		for (int index= 0; index < members.length; index++) {
			if (isPullUpAvailable(members[index]))
				list.add(members[index]);
		}
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	public static IMember[] getPushDownMembers(final IType type) throws JavaModelException {
		final List list= new ArrayList(3);
		IMember[] members= type.getFields();
		for (int index= 0; index < members.length; index++) {
			if (isPushDownAvailable(members[index]))
				list.add(members[index]);
		}
		members= type.getMethods();
		for (int index= 0; index < members.length; index++) {
			if (isPushDownAvailable(members[index]))
				list.add(members[index]);
		}
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	public static IType getTopLevelType(final IMember[] members) {
		if (members != null && members.length == 1 && Checks.isTopLevelType(members[0]))
			return (IType) members[0];
		return null;
	}

	public static boolean isChangeSignatureAvailable(final IMethod method) throws JavaModelException {
		return Checks.isAvailable(method) && !Flags.isAnnotation(method.getDeclaringType().getFlags());
	}

	public static boolean isChangeSignatureAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IMethod) {
				final IMethod method= (IMethod) selection.getFirstElement();
				return isChangeSignatureAvailable(method);
			}
		}
		return false;
	}

	public static boolean isChangeSignatureAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length == 1 && (elements[0] instanceof IMethod))
			return isChangeSignatureAvailable((IMethod) elements[0]);
		final IJavaElement element= selection.resolveEnclosingElement();
		return (element instanceof IMethod) && isChangeSignatureAvailable((IMethod) element);
	}

	public static boolean isCommonDeclaringType(final IMember[] members) {
		if (members.length == 0)
			return false;
		final IType type= members[0].getDeclaringType();
		if (type == null)
			return false;
		for (int index= 0; index < members.length; index++) {
			if (!type.equals(members[index].getDeclaringType()))
				return false;
		}
		return true;
	}

	public static boolean isConvertAnonymousAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IType) {
				final IType type= (IType) selection.getFirstElement();
				return type.isAnonymous();
			}
		}
		return false;
	}

	public static boolean isConvertAnonymousAvailable(final IType type) throws JavaModelException {
		return Checks.isAvailable(type) && type.isAnonymous();
	}

	public static boolean isConvertAnonymousAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IType type= RefactoringActions.getEnclosingType(selection);
		if (type != null)
			return RefactoringAvailabilityTester.isConvertAnonymousAvailable(type);
		return false;
	}

	public static boolean isExternalizeStringsAvailable(final IStructuredSelection selection) {
		if (selection.size() == 1) {
			ICompilationUnit unit= null;
			final Object first= selection.getFirstElement();
			if (first instanceof ICompilationUnit)
				unit= (ICompilationUnit) first;
			else if (first instanceof IType)
				unit= ((IType) first).getCompilationUnit();
			return unit != null && unit.exists();
		}
		return false;
	}

	public static boolean isExtractConstantAvailable(final JavaTextSelection selection) {
		return (selection.resolveInClassInitializer() || selection.resolveInMethodBody() || selection.resolveInVariableInitializer()) && Checks.isExtractableExpression(selection.resolveSelectedNodes(), selection.resolveCoveringNode());
	}

	public static boolean isExtractInterfaceAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			IType type= null;
			Object first= selection.getFirstElement();
			if (first instanceof IType)
				type= (IType) first;
			if (first instanceof ICompilationUnit)
				type= JavaElementUtil.getMainType((ICompilationUnit) first);
			return type != null && isExtractInterfaceAvailable(type);
		}
		return false;
	}

	public static boolean isExtractInterfaceAvailable(final IType type) throws JavaModelException {
		return Checks.isAvailable(type) && !type.isBinary() && !type.isReadOnly() && !type.isAnnotation() && !type.isAnonymous();
	}

	public static boolean isExtractInterfaceAvailable(final JavaTextSelection selection) throws JavaModelException {
		return isExtractInterfaceAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
	}

	public static boolean isExtractMethodAvailable(final ASTNode[] nodes) {
		if (nodes != null && nodes.length != 0) {
			if (nodes.length == 1)
				return nodes[0] instanceof Statement || Checks.isExtractableExpression(nodes[0]);
			else {
				for (int index= 0; index < nodes.length; index++) {
					if (!(nodes[index] instanceof Statement))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isExtractMethodAvailable(final JavaTextSelection selection) {
		return (selection.resolveInMethodBody() || selection.resolveInClassInitializer()) && RefactoringAvailabilityTester.isExtractMethodAvailable(selection.resolveSelectedNodes());
	}

	public static boolean isExtractTempAvailable(final JavaTextSelection selection) {
		final ASTNode[] nodes= selection.resolveSelectedNodes();
		return (selection.resolveInMethodBody() || selection.resolveInClassInitializer()) && (Checks.isExtractableExpression(nodes, selection.resolveCoveringNode()) || (nodes.length == 1 && nodes[0] instanceof ExpressionStatement));
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

	public static boolean isGeneralizeTypeAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			final Object element= selection.getFirstElement();
			if (element instanceof IMethod) {
				final IMethod method= (IMethod) element;
				final String type= method.getReturnType();
				if (PrimitiveType.toCode(Signature.toString(type)) == null)
					return true;
			} else if (element instanceof IField && !JdtFlags.isEnum((IMember) element))
				return true;
		}
		return false;
	}

	public static boolean isGeneralizeTypeAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return isGeneralizeTypeAvailable(elements[0]);
	}

	public static boolean isInferTypeArgumentsAvailable(final IJavaElement[] elements) {
		return elements.length > 0;
	}

	public static boolean isInferTypeArgumentsAvailable(final IStructuredSelection selection) {
		final List list= selection.toList();
		if (list.size() > 0) {
			for (int index= 0; index < list.size(); index++) {
				if (!(list.get(index) instanceof IJavaElement))
					return false;
			}
			return true;
		}
		return false;
	}

	public static boolean isInlineConstantAvailable(final IField field) throws JavaModelException {
		return Checks.isAvailable(field) && JdtFlags.isStatic(field) && JdtFlags.isFinal(field) && !JdtFlags.isEnum(field);
	}

	public static boolean isInlineConstantAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IField) && isInlineConstantAvailable(((IField) first));
	}

	public static boolean isInlineConstantAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof IField) && isInlineConstantAvailable(((IField) elements[0]));
	}

	public static boolean isInlineMethodAvailable(IMethod method) throws JavaModelException {
		return Checks.isAvailable(method);
	}

	public static boolean isInlineMethodAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IMethod) && isInlineMethodAvailable(((IMethod) first));
	}

	public static boolean isInlineMethodAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof IMethod) && isInlineMethodAvailable(((IMethod) elements[0]));
	}

	public static boolean isInlineTempAvailable(final ILocalVariable variable) throws JavaModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isInlineTempAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof ILocalVariable) && isInlineTempAvailable((ILocalVariable) elements[0]);
	}

	public static boolean isIntroduceFactoryAvailable(final IMethod method) throws JavaModelException {
		return Checks.isAvailable(method) && method.isConstructor();
	}

	public static boolean isIntroduceFactoryAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1 && selection.getFirstElement() instanceof IMethod)
			return isIntroduceFactoryAvailable((IMethod) selection.getFirstElement());
		return false;
	}

	public static boolean isIntroduceFactoryAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length == 1 && elements[0] instanceof IMethod)
			return isIntroduceFactoryAvailable((IMethod) elements[0]);
		return false;
	}

	public static boolean isIntroduceParameterAvailable(final ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static boolean isIntroduceParameterAvailable(final JavaTextSelection selection) {
		return selection.resolveInMethodBody() && isIntroduceParameterAvailable(selection.resolveSelectedNodes(), selection.resolveCoveringNode());
	}

	public static boolean isMoveInnerAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			IType type= null;
			Object first= selection.getFirstElement();
			if (first instanceof IType)
				type= (IType) first;
			if (first instanceof ICompilationUnit)
				type= JavaElementUtil.getMainType((ICompilationUnit) first);
			return type != null && isMoveInnerAvailable(type);
		}
		return false;
	}

	public static boolean isMoveInnerAvailable(final IType type) throws JavaModelException {
		return Checks.isAvailable(type) && !Checks.isAnonymous(type) && !Checks.isTopLevel(type) && !Checks.isInsideLocalType(type);
	}

	public static boolean isMoveInnerAvailable(final JavaTextSelection selection) throws JavaModelException {
		IType type= RefactoringAvailabilityTester.getDeclaringType(selection.resolveEnclosingElement());
		if (type == null)
			return false;
		return isMoveInnerAvailable(type);
	}

	public static boolean isMoveMethodAvailable(final IMethod method) throws JavaModelException {
		return method.exists() && !method.isConstructor() && !method.isBinary() && !method.getDeclaringType().isLocal() && !method.getDeclaringType().isAnnotation() && !method.isReadOnly() && !JdtFlags.isStatic(method);
	}

	public static boolean isMoveMethodAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			final Object first= selection.getFirstElement();
			return first instanceof IMethod && isMoveMethodAvailable((IMethod) first);
		}
		return false;
	}

	public static boolean isMoveMethodAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement method= selection.resolveEnclosingElement();
		if (!(method instanceof IMethod))
			return false;
		return isMoveMethodAvailable((IMethod) method);
	}

	public static boolean isPullUpAvailable(IMember member) throws JavaModelException {
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
		}
		return true;
	}

	public static boolean isPullUpAvailable(final IMember[] members) throws JavaModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null && getPullUpMembers(type).length != 0)
				return true;
			for (int index= 0; index < members.length; index++) {
				if (!isPullUpAvailable(members[index]))
					return false;
			}
			return isCommonDeclaringType(members);
		}
		return false;
	}

	public static boolean isPullUpAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (!selection.isEmpty()) {
			for (final Iterator iterator= selection.iterator(); iterator.hasNext();) {
				if (!(iterator.next() instanceof IMember))
					return false;
			}
			final Set members= new HashSet();
			members.addAll(Arrays.asList(selection.toArray()));
			return isPullUpAvailable((IMember[]) members.toArray(new IMember[members.size()]));
		}
		return false;
	}

	public static boolean isPullUpAvailable(final JavaTextSelection selection) throws JavaModelException {
		IJavaElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return isPullUpAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isPushDownAvailable(final IMember member) throws JavaModelException {
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
		}
		return true;
	}

	public static boolean isPushDownAvailable(final IMember[] members) throws JavaModelException {
		if (members != null && members.length != 0) {
			final IType type= getTopLevelType(members);
			if (type != null && RefactoringAvailabilityTester.getPushDownMembers(type).length != 0)
				return true;
			if (type != null && JdtFlags.isEnum(type))
				return false;
			for (int index= 0; index < members.length; index++) {
				if (!isPushDownAvailable(members[index]))
					return false;
			}
			return isCommonDeclaringType(members);
		}
		return false;
	}

	public static boolean isPushDownAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (!selection.isEmpty()) {
			for (final Iterator iterator= selection.iterator(); iterator.hasNext();) {
				if (!(iterator.next() instanceof IMember))
					return false;
			}
			final Set members= new HashSet();
			members.addAll(Arrays.asList(selection.toArray()));
			return isPushDownAvailable((IMember[]) members.toArray(new IMember[members.size()]));
		}
		return false;
	}

	public static boolean isPushDownAvailable(final JavaTextSelection selection) throws JavaModelException {
		IJavaElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return isPullUpAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isUseSuperTypeAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			IType type= null;
			final Object first= selection.getFirstElement();
			if (first instanceof IType)
				type= (IType) first;
			if (first instanceof ICompilationUnit)
				type= JavaElementUtil.getMainType((ICompilationUnit) first);
			return type != null && isUseSuperTypeAvailable(type);
		}
		return false;
	}

	public static boolean isUseSuperTypeAvailable(final IType type) throws JavaModelException {
		return Checks.isAvailable(type) && !type.isAnnotation() && !type.isAnonymous();
	}

	public static boolean isUseSuperTypeAvailable(final JavaTextSelection selection) throws JavaModelException {
		return isUseSuperTypeAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
	}

	private RefactoringAvailabilityTester() {
		// Not for instantiation
	}

	public static boolean isPromoteTempAvailable(final ILocalVariable variable) throws JavaModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isPromoteTempAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof ILocalVariable) && 
			isPromoteTempAvailable((ILocalVariable)elements[0]);
	}

	public static boolean isSelfEncapsulateAvailable(IField field) throws JavaModelException {
		return Checks.isAvailable(field) && !JdtFlags.isEnum(field) && !field.getDeclaringType().isAnnotation();
	}

	public static boolean isSelfEncapsulateAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof IField) && 
			isSelfEncapsulateAvailable((IField)elements[0]);
	}

	public static boolean isSelfEncapsulateAvailable(final IStructuredSelection selection) {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IField)
				return true;
		}
		return false;
	}
}