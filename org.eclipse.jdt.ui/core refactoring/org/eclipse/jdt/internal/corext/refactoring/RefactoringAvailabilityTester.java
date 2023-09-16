/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
 *     Microsoft Corporation - moved some methods to RefactoringAvailabilityTesterCore for jdt.core.manipulation use
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.PrimitiveType;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtilsCore;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.javaeditor.JavaTextSelection;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringActions;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;

/**
 * Helper class to detect whether a certain refactoring can be enabled on a
 * selection.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code,
 * in order not to eagerly load refactoring classes during action
 * initialization.
 * </p>
 *
 * @since 3.1
 */
public final class RefactoringAvailabilityTester {

	public static IType getDeclaringType(IJavaElement element) {
		return RefactoringAvailabilityTesterCore.getDeclaringType(element);
	}

	public static IJavaElement[] getJavaElements(final Object[] elements) {
		return RefactoringAvailabilityTesterCore.getJavaElements(elements);
	}

	public static IMember[] getPullUpMembers(final IType type) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.getPullUpMembers(type);
	}

	public static IMember[] getPushDownMembers(final IType type) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.getPushDownMembers(type);
	}

	public static IResource[] getResources(final Object[] elements) {
		return RefactoringAvailabilityTesterCore.getResources(elements);
	}

	public static IType getSingleSelectedType(IStructuredSelection selection) throws JavaModelException {
		Object first= selection.getFirstElement();
		if (first instanceof IType)
			return (IType) first;
		if (first instanceof ICompilationUnit) {
			final ICompilationUnit unit= (ICompilationUnit) first;
			if (unit.exists())
				return  JavaElementUtil.getMainType(unit);
		}
		return null;
	}

	public static IType getTopLevelType(final IMember[] members) {
		return RefactoringAvailabilityTesterCore.getTopLevelType(members);
	}

	public static boolean isChangeSignatureAvailable(final IMethod method) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isChangeSignatureAvailable(method);
	}

	public static boolean isChangeSignatureAvailable(final IStructuredSelection selection) throws JavaModelException {
		final IMethod method= getSelectedMethod(selection);
		return isChangeSignatureAvailable(method);
	}

	public static boolean isChangeSignatureAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IMethod method= getSelectedMethod(selection);
		return isChangeSignatureAvailable(method);
	}

	public static IMethod getSelectedMethod(final IStructuredSelection selection) {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IMethod) {
				return (IMethod) selection.getFirstElement();
			}
		}
		return null;
	}

	public static IMethod getSelectedMethod(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length == 1 && (elements[0] instanceof IMethod))
			return ((IMethod) elements[0]);
		final IJavaElement element= selection.resolveEnclosingElement();
		return (element instanceof IMethod) ? (IMethod)element : null;
	}

	public static boolean isCanonicalConstructor(IMethod method) {
		return RefactoringAvailabilityTesterCore.isCanonicalConstructor(method);
	}

	public static boolean isCommonDeclaringType(final IMember[] members) {
		return RefactoringAvailabilityTesterCore.isCommonDeclaringType(members);
	}

	public static boolean isConvertAnonymousAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IType) {
				return isConvertAnonymousAvailable((IType) selection.getFirstElement());
			}
		}
		return false;
	}

	public static boolean isConvertAnonymousAvailable(final IType type) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isConvertAnonymousAvailable(type);
	}

	public static boolean isConvertAnonymousAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IType type= RefactoringActions.getEnclosingType(selection);
		if (type != null)
			return RefactoringAvailabilityTester.isConvertAnonymousAvailable(type);
		return false;
	}

	public static boolean isCopyAvailable(final IResource[] resources, final IJavaElement[] elements) throws JavaModelException {
		return ReorgPolicyFactory.createCopyPolicy(resources, elements).canEnable();
	}

	public static boolean isDelegateCreationAvailable(final IField field) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isDelegateCreationAvailable(field);
	}

	public static boolean isDeleteAvailable(final IJavaElement element) {
		return RefactoringAvailabilityTesterCore.isDeleteAvailable(element);
	}

	public static boolean isDeleteAvailable(final IResource resource) {
		return RefactoringAvailabilityTesterCore.isDeleteAvailable(resource);
	}

	public static boolean isDeleteAvailable(final IStructuredSelection selection) {
		if (!selection.isEmpty())
			return isDeleteAvailable(selection.toArray());
		return false;
	}

	public static boolean isDeleteAvailable(final Object[] objects) {
		if (objects.length != 0) {
			if (ReorgUtils.containsOnlyWorkingSets(Arrays.asList(objects)))
				return true;
			final IResource[] resources= RefactoringAvailabilityTester.getResources(objects);
			final IJavaElement[] elements= RefactoringAvailabilityTester.getJavaElements(objects);

			if (objects.length != resources.length + elements.length)
				return false;
			for (IResource resource : resources) {
				if (!isDeleteAvailable(resource)) {
					return false;
				}
			}
			for (IJavaElement element : elements) {
				if (!isDeleteAvailable(element)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static boolean isExternalizeStringsAvailable(final IStructuredSelection selection) throws JavaModelException {
		for (Object element : selection) {
			if (element instanceof IJavaElement) {
				IJavaElement javaElement= (IJavaElement)element;
				if (javaElement.exists() && !javaElement.isReadOnly()) {
					int elementType= javaElement.getElementType();
					switch (elementType) {
						case IJavaElement.PACKAGE_FRAGMENT:
						case IJavaElement.JAVA_PROJECT:
							return true;
						case IJavaElement.PACKAGE_FRAGMENT_ROOT:
							IPackageFragmentRoot root= (IPackageFragmentRoot)javaElement;
							if (!root.isExternal() && !ReorgUtilsCore.isClassFolder(root))
								return true;
							break;
						case IJavaElement.COMPILATION_UNIT:
							ICompilationUnit cu= (ICompilationUnit)javaElement;
							if (cu.exists())
								return true;
							break;
						case IJavaElement.TYPE:
							IJavaElement parent= ((IType) element).getParent();
							if (parent instanceof ICompilationUnit && parent.exists())
								return true;
							break;
						default:
							break;
					}
				}
			} else if (element instanceof IWorkingSet) {
				IWorkingSet workingSet= (IWorkingSet) element;
				return IWorkingSetIDs.JAVA.equals(workingSet.getId());
			}
		}
		return false;
	}

	public static boolean isExtractConstantAvailable(final JavaTextSelection selection) {
		return (selection.resolveInClassInitializer() || selection.resolveInMethodBody() || selection.resolveInVariableInitializer() || selection.resolveInAnnotation())
				&& Checks.isExtractableExpression(selection.resolveSelectedNodes(), selection.resolveCoveringNode());
	}

	public static boolean isExtractInterfaceAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			Object first= selection.getFirstElement();
			if (first instanceof IType) {
				return isExtractInterfaceAvailable((IType) first);
			} else if (first instanceof ICompilationUnit) {
				ICompilationUnit unit= (ICompilationUnit) first;
				if (!unit.exists() || unit.isReadOnly())
					return false;

				return true;
			}
		}
		return false;
	}

	public static boolean isExtractInterfaceAvailable(final IType type) throws JavaModelException {
		return Checks.isAvailable(type) && !type.isBinary() && !type.isReadOnly() && !type.isAnnotation() && !type.isAnonymous() && !type.isLambda();
	}

	public static boolean isExtractInterfaceAvailable(final JavaTextSelection selection) throws JavaModelException {
		return isExtractInterfaceAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
	}

	public static boolean isExtractMethodAvailable(final ASTNode[] nodes) {
		return RefactoringAvailabilityTesterCore.isExtractMethodAvailable(nodes);
	}

	public static boolean isExtractMethodAvailable(final JavaTextSelection selection) {
		return (selection.resolveInMethodBody() || selection.resolveInClassInitializer() || selection.resolveInVariableInitializer())
			&& !selection.resolveInAnnotation()
			&& RefactoringAvailabilityTester.isExtractMethodAvailable(selection.resolveSelectedNodes());
	}

	public static boolean isExtractSupertypeAvailable(IMember member) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isExtractSupertypeAvailable(member);
	}

	public static boolean isExtractSupertypeAvailable(final IMember[] members) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isExtractSupertypeAvailable(members);
	}

	public static boolean isExtractSupertypeAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				if (selection.getFirstElement() instanceof ICompilationUnit)
					return true; // Do not force opening
				final IType type= getSingleSelectedType(selection);
				if (type != null)
					return Checks.isAvailable(type) && isExtractSupertypeAvailable(new IType[] { type});
			}
			for (Object member : selection) {
				if (!(member instanceof IMember))
					return false;
			}
			final Set<IMember> members= new HashSet<>();
			@SuppressWarnings("unchecked")
			List<IMember> selectionList= (List<IMember>) (List<?>) Arrays.asList(selection.toArray());
			members.addAll(selectionList);
			return isExtractSupertypeAvailable(members.toArray(new IMember[members.size()]));
		}
		return false;
	}

	public static boolean isExtractSupertypeAvailable(final JavaTextSelection selection) throws JavaModelException {
		IJavaElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return isExtractSupertypeAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isExtractTempAvailable(final JavaTextSelection selection) {
		final ASTNode[] nodes= selection.resolveSelectedNodes();
		return (selection.resolveInMethodBody() || selection.resolveInClassInitializer())
				&& !selection.resolveInAnnotation()
				&& (Checks.isExtractableExpression(nodes, selection.resolveCoveringNode()) || (nodes != null && nodes.length == 1 && nodes[0] instanceof ExpressionStatement));
	}

	public static boolean isGeneralizeTypeAvailable(final IJavaElement element) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isGeneralizeTypeAvailable(element);
	}

	public static boolean isGeneralizeTypeAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			final Object element= selection.getFirstElement();
			if (element instanceof IMethod) {
				final IMethod method= (IMethod) element;
				if (!method.exists())
					return false;
				final String type= method.getReturnType();
				if (PrimitiveType.toCode(Signature.toString(type)) == null)
					return Checks.isAvailable(method);
			} else if (element instanceof IField) {
				final IField field= (IField) element;
				if (!field.exists())
					return false;
				if (!JdtFlags.isEnum(field))
					return Checks.isAvailable(field);
			}
		}
		return false;
	}

	public static boolean isGeneralizeTypeAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return isGeneralizeTypeAvailable(elements[0]);
	}

	public static boolean isInferTypeArgumentsAvailable(final IJavaElement element) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isInferTypeArgumentsAvailable(element);
	}

	public static boolean isInferTypeArgumentsAvailable(final IJavaElement[] elements) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isInferTypeArgumentsAvailable(elements);
	}

	public static boolean isInferTypeArgumentsAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty())
			return false;

		for (Object element : selection) {
			if (!(element instanceof IJavaElement))
				return false;
			if (element instanceof ICompilationUnit) {
				ICompilationUnit unit= (ICompilationUnit) element;
				if (!unit.exists() || unit.isReadOnly())
					return false;

				return true;
			}
			if (!isInferTypeArgumentsAvailable((IJavaElement) element))
				return false;
		}
		return true;
	}

	public static boolean isInlineConstantAvailable(final IField field) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isInlineConstantAvailable(field);
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
		return RefactoringAvailabilityTesterCore.isInlineMethodAvailable(method);
	}

	public static boolean isInlineMethodAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IMethod) && isInlineMethodAvailable(((IMethod) first));
	}

	public static boolean isInlineMethodAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1) {
			IJavaElement enclosingElement= selection.resolveEnclosingElement();
			if (!(enclosingElement instanceof IMember))
				return false;
			ITypeRoot typeRoot= ((IMember)enclosingElement).getTypeRoot();
			CompilationUnit compilationUnit= selection.resolvePartialAstAtOffset();
			if (compilationUnit == null)
				return false;
			return getInlineableMethodNode(typeRoot, compilationUnit, selection.getOffset(), selection.getLength()) != null;
		}
		IJavaElement element= elements[0];
		if (!(element instanceof IMethod))
			return false;
		IMethod method= (IMethod) element;
		if (!isInlineMethodAvailable((method)))
			return false;

		// in binary class, only activate for method declarations
		IJavaElement enclosingElement= selection.resolveEnclosingElement();
		if (enclosingElement == null || enclosingElement.getAncestor(IJavaElement.CLASS_FILE) == null)
			return true;
		if (!(enclosingElement instanceof IMethod))
			return false;
		IMethod enclosingMethod= (IMethod) enclosingElement;
		if (enclosingMethod.isConstructor())
			return false;
		int nameOffset= enclosingMethod.getNameRange().getOffset();
		int nameLength= enclosingMethod.getNameRange().getLength();
		return (nameOffset <= selection.getOffset()) && (selection.getOffset() + selection.getLength() <= nameOffset + nameLength);
	}

	public static ASTNode getInlineableMethodNode(ITypeRoot typeRoot, CompilationUnit root, int offset, int length) {
		return RefactoringAvailabilityTesterCore.getInlineableMethodNode(typeRoot, root, offset, length);
	}

	public static boolean isInlineTempAvailable(final ILocalVariable variable) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isInlineTempAvailable(variable);
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

		// there's no IMethod for the default constructor
		if (!Checks.isAvailable(selection.resolveEnclosingElement()))
			return false;
		ASTNode node= selection.resolveCoveringNode();
		if (node == null) {
			ASTNode[] selectedNodes= selection.resolveSelectedNodes();
			if (selectedNodes != null && selectedNodes.length == 1) {
				node= selectedNodes[0];
				if (node == null)
					return false;
			} else {
				return false;
			}
		}

		if (node.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION)
			return true;

		node= ASTNodes.getNormalizedNode(node);
		if (node.getLocationInParent() == ClassInstanceCreation.TYPE_PROPERTY)
			return true;

		return false;
	}

	public static boolean isIntroduceIndirectionAvailable(IMethod method) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isIntroduceIndirectionAvailable(method);
	}

	public static boolean isIntroduceIndirectionAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IMethod) && isIntroduceIndirectionAvailable(((IMethod) first));
	}

	public static boolean isIntroduceIndirectionAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length == 1)
			return (elements[0] instanceof IMethod) && isIntroduceIndirectionAvailable(((IMethod) elements[0]));
		ASTNode[] selectedNodes= selection.resolveSelectedNodes();
		if (selectedNodes == null || selectedNodes.length != 1)
			return false;
		switch (selectedNodes[0].getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
				return true;
			default:
				return false;
		}
	}

	public static boolean isIntroduceParameterAvailable(final ASTNode[] selectedNodes, ASTNode coveringNode) {
		return Checks.isExtractableExpression(selectedNodes, coveringNode);
	}

	public static boolean isIntroduceParameterAvailable(final JavaTextSelection selection) {
		return selection.resolveInMethodBody()
				&& !selection.resolveInAnnotation()
				&& isIntroduceParameterAvailable(selection.resolveSelectedNodes(), selection.resolveCoveringNode());
	}

	public static boolean isMoveAvailable(final IResource[] resources, final IJavaElement[] elements) throws JavaModelException {
		if (elements != null) {
			for (IJavaElement element : elements) {
				if (element == null || !element.exists())
					return false;
				if ((element instanceof IType) && ((IType) element).isLocal())
					return false;
				if ((element instanceof IPackageDeclaration))
					return false;
				if (element instanceof IField
						&& (JdtFlags.isEnum((IMember) element)
								|| ((IField) element).isRecordComponent()))
					return false;
				if ((element instanceof IMethod) && ((IMethod)element).isConstructor())
					return false;
			}
		}
		return ReorgPolicyFactory.createMovePolicy(resources, elements).canEnable();
	}

	public static boolean isMoveAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement element= selection.resolveEnclosingElement();
		if (element == null)
			return false;
		return isMoveAvailable(new IResource[0], new IJavaElement[] { element});
	}

	public static boolean isMoveInnerAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			Object first= selection.getFirstElement();
			if (first instanceof IType) {
				return isMoveInnerAvailable((IType) first);
			}
		}
		return false;
	}

	public static boolean isMoveInnerAvailable(final IType type) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isMoveInnerAvailable(type);
	}

	public static boolean isMoveInnerAvailable(final JavaTextSelection selection) throws JavaModelException {
		IType type= RefactoringAvailabilityTester.getDeclaringType(selection.resolveEnclosingElement());
		if (type == null)
			return false;
		return isMoveInnerAvailable(type);
	}

	public static boolean isMoveMethodAvailable(final IMethod method) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isMoveMethodAvailable(method);
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

	public static boolean isMoveStaticAvailable(final IMember member) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isMoveStaticAvailable(member);
	}

	public static boolean isMoveStaticAvailable(final IMember[] members) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isMoveStaticAvailable(members);
	}

	public static boolean isMoveStaticAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return RefactoringAvailabilityTester.isMoveStaticMembersAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isMoveStaticMembersAvailable(final IMember[] members) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isMoveStaticMembersAvailable(members);
	}

	public static boolean isPromoteTempAvailable(final ILocalVariable variable) throws JavaModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isPromoteTempAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof ILocalVariable) && isPromoteTempAvailable((ILocalVariable) elements[0]);
	}

	public static boolean isPullUpAvailable(IMember member) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isPullUpAvailable(member);
	}

	public static boolean isPullUpAvailable(final IMember[] members) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isPullUpAvailable(members);
	}

	public static boolean isPullUpAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				if (selection.getFirstElement() instanceof ICompilationUnit)
					return true; // Do not force opening
				final IType type= getSingleSelectedType(selection);
				if (type != null)
					return Checks.isAvailable(type) && isPullUpAvailable(new IType[] { type});
			}
			for (Object member : selection) {
				if (!(member instanceof IMember))
					return false;
			}
			final Set<IMember> members= new HashSet<>();
			@SuppressWarnings("unchecked")
			List<IMember> selectionList= (List<IMember>) (List<?>) Arrays.asList(selection.toArray());
			members.addAll(selectionList);
			return isPullUpAvailable(members.toArray(new IMember[members.size()]));
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
		return RefactoringAvailabilityTesterCore.isPushDownAvailable(member);
	}

	public static boolean isPushDownAvailable(final IMember[] members) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isPushDownAvailable(members);
	}

	public static boolean isPushDownAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				if (selection.getFirstElement() instanceof ICompilationUnit)
					return true; // Do not force opening
				final IType type= getSingleSelectedType(selection);
				if (type != null)
					return isPushDownAvailable(new IType[] { type});
			}
			for (Object member : selection) {
				if (!(member instanceof IMember))
					return false;
			}
			final Set<IMember> members= new HashSet<>();
			@SuppressWarnings("unchecked")
			List<IMember> selectionList= (List<IMember>) (List<?>) Arrays.asList(selection.toArray());
			members.addAll(selectionList);
			return isPushDownAvailable(members.toArray(new IMember[members.size()]));
		}
		return false;
	}

	public static boolean isPushDownAvailable(final JavaTextSelection selection) throws JavaModelException {
		IJavaElement element= selection.resolveEnclosingElement();
		if (!(element instanceof IMember))
			return false;
		return isPullUpAvailable(new IMember[] { (IMember) element});
	}

	public static boolean isRenameAvailable(final ICompilationUnit unit) {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(unit);
	}

	public static boolean isRenameAvailable(final IJavaProject project) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(project);
	}

	public static boolean isRenameAvailable(final IModuleDescription module) throws JavaModelException {
		return Checks.isAvailable(module);
	}

	public static boolean isRenameAvailable(final ILocalVariable variable) throws JavaModelException {
		return Checks.isAvailable(variable);
	}

	public static boolean isRenameAvailable(final IMethod method) throws CoreException {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(method);
	}

	public static boolean isRenameAvailable(final IPackageFragment fragment) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(fragment);
	}

	public static boolean isRenameAvailable(final IPackageFragmentRoot root) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(root);
	}

	public static boolean isRenameAvailable(final IResource resource) {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(resource);
	}

	public static boolean isRenameAvailable(final IType type) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isRenameAvailable(type);
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
		return RefactoringAvailabilityTesterCore.isRenameProhibited(method);
	}

	public static boolean isRenameProhibited(final IType type) {
		return RefactoringAvailabilityTesterCore.isRenameProhibited(type);
	}

	public static boolean isRenameVirtualMethodAvailable(final IMethod method) throws CoreException {
		return isRenameAvailable(method) && MethodChecks.isVirtual(method);
	}

	public static boolean isRenameElementAvailable(IJavaElement element) throws CoreException {
		return isRenameElementAvailable(element, false);
	}

	public static boolean isRenameElementAvailable(IJavaElement element, boolean isTextSelection) throws CoreException {
		return RefactoringAvailabilityTesterCore.isRenameElementAvailable(element, isTextSelection);
	}

	public static boolean isReplaceInvocationsAvailable(IMethod method) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isReplaceInvocationsAvailable(method);
	}

	public static boolean isReplaceInvocationsAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty() || selection.size() != 1)
			return false;
		final Object first= selection.getFirstElement();
		return (first instanceof IMethod) && isReplaceInvocationsAvailable(((IMethod) first));
	}

	public static boolean isReplaceInvocationsAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		IJavaElement element= elements[0];
		return (element instanceof IMethod) && isReplaceInvocationsAvailable(((IMethod) element));
	}

	public static boolean isSelfEncapsulateAvailable(IField field) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isSelfEncapsulateAvailable(field);
	}

	public static boolean isSelfEncapsulateAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			if (selection.getFirstElement() instanceof IField) {
				final IField field= (IField) selection.getFirstElement();
				return isSelfEncapsulateAvailable(field);
			}
		}
		return false;
	}

	public static boolean isSelfEncapsulateAvailable(final JavaTextSelection selection) throws JavaModelException {
		final IJavaElement[] elements= selection.resolveElementAtOffset();
		if (elements.length != 1)
			return false;
		return (elements[0] instanceof IField) && isSelfEncapsulateAvailable((IField) elements[0]);
	}

	public static boolean isUseSuperTypeAvailable(final IStructuredSelection selection) throws JavaModelException {
		if (selection.size() == 1) {
			final Object first= selection.getFirstElement();
			if (first instanceof IType) {
				return isUseSuperTypeAvailable((IType) first);
			} else if (first instanceof ICompilationUnit) {
				ICompilationUnit unit= (ICompilationUnit) first;
				if (!unit.exists() || unit.isReadOnly())
					return false;

				return true;
			}
		}
		return false;
	}

	public static boolean isUseSuperTypeAvailable(final IType type) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isUseSuperTypeAvailable(type);
	}

	public static boolean isUseSuperTypeAvailable(final JavaTextSelection selection) throws JavaModelException {
		return isUseSuperTypeAvailable(RefactoringActions.getEnclosingOrPrimaryType(selection));
	}

	public static boolean isWorkingCopyElement(final IJavaElement element) {
		return RefactoringAvailabilityTesterCore.isWorkingCopyElement(element);
	}


	public static boolean isIntroduceParameterObjectAvailable(IStructuredSelection selection) throws JavaModelException{
		IMethod method= getSelectedMethod(selection); //TODO test selected element for more than 1 parameter?
		return isChangeSignatureAvailable(method) && !isCanonicalConstructor(method);
	}

	public static boolean isIntroduceParameterObjectAvailable(JavaTextSelection selection) throws JavaModelException{
		IMethod method= getSelectedMethod(selection); //TODO test selected element for more than 1 parameter?
		return isChangeSignatureAvailable(method) && !isCanonicalConstructor(method);
	}



	public static boolean isExtractClassAvailable(IType type) throws JavaModelException {
		return RefactoringAvailabilityTesterCore.isExtractClassAvailable(type);
	}

	private RefactoringAvailabilityTester() {
		// Not for instantiation
	}
}
