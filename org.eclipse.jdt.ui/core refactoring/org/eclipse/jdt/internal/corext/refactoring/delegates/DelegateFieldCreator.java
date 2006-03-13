/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.delegates;

import org.eclipse.ltk.core.refactoring.RefactoringSessionDescriptor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.deprecation.DeprecationRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDeprecationResolving;

/**
 * Delegate creator for static fields. Note that this implementation assumes a
 * field <strong>with only one fragment</strong>. See
 * {@link MoveStaticMembersProcessor#getASTMembers(org.eclipse.ltk.core.refactoring.RefactoringStatus)}
 * for more information.
 * 
 * @since 3.2
 */
public class DelegateFieldCreator extends DelegateCreator {

	private VariableDeclarationFragment fOldFieldFragment;

	protected void initialize() {
		
		Assert.isTrue(getDeclaration() instanceof FieldDeclaration);
		Assert.isTrue(((FieldDeclaration) getDeclaration()).fragments().size() == 1);
		
		fOldFieldFragment= (VariableDeclarationFragment) ((FieldDeclaration) getDeclaration()).fragments().get(0);
		if (getNewElementName() == null)
			setNewElementName(fOldFieldFragment.getName().getIdentifier());
		
		setInsertBefore(false); // delegate must be inserted after the original field that is referenced in the initializer
	}

	protected ASTNode createBody(BodyDeclaration fd) throws JavaModelException {
		FieldDeclaration result= (FieldDeclaration) fd;
		Expression initializer= createDelegateFieldInitializer(result);
		return initializer;
	}

	protected ASTNode createDocReference(BodyDeclaration declaration) {
		MemberRef ref= getAst().newMemberRef();
		ref.setName(getAst().newSimpleName(getNewElementName()));

		if (isMoveToAnotherFile())
			ref.setQualifier(createDestinationTypeName());
		return ref;
	}
	
	protected ASTNode getBodyHead(BodyDeclaration result) {
		return fOldFieldFragment;
	}
	
	protected ChildPropertyDescriptor getJavaDocProperty() {
		return FieldDeclaration.JAVADOC_PROPERTY;
	}

	protected ChildPropertyDescriptor getBodyProperty() {
		return VariableDeclarationFragment.INITIALIZER_PROPERTY;
	}

	protected IBinding getDeclarationBinding() {
		return fOldFieldFragment.resolveBinding();
	}

	protected RefactoringSessionDescriptor createRefactoringScript() {
		final IVariableBinding binding= fOldFieldFragment.resolveBinding();
		if (binding != null) {
			final IJavaElement element= binding.getJavaElement();
			if (element instanceof IField) {
				final IField field= (IField) element;
				final IDeprecationResolving resolving= new InlineConstantRefactoring(field);
				if (resolving.canEnableDeprecationResolving())
					return resolving.createDeprecationResolution();
			}
		}
		return null;
	}

	protected String getRefactoringScriptName() {
		final IVariableBinding binding= fOldFieldFragment.resolveBinding();
		if (binding != null)
			return DeprecationRefactorings.getRefactoringScriptName(binding);
		return null;
	}

	protected String getTextEditGroupLabel() {
		return RefactoringCoreMessages.DelegateFieldCreator_text_edit_group_label;
	}

	// ******************* INTERNAL HELPERS ***************************

	private Expression createDelegateFieldInitializer(final FieldDeclaration declaration) throws JavaModelException {
		Assert.isNotNull(declaration);

		Expression qualification= getAccess();
		if (qualification != null) {
			FieldAccess access= getAst().newFieldAccess();
			access.setExpression(qualification);
			access.setName(getAst().newSimpleName(getNewElementName()));
			return access;
		} else {
			SimpleName access= getAst().newSimpleName(getNewElementName());
			return access;
		}
	}
}