/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;


public class VariableDeclarationRewrite {

	public static void rewriteModifiers(final SingleVariableDeclaration declarationNode, final int includedModifiers, final int excludedModifiers, final ASTRewrite rewrite, final TextEditGroup group) {
		ModifierRewrite listRewrite= ModifierRewrite.create(rewrite, declarationNode);
		listRewrite.setModifiers(includedModifiers, excludedModifiers, group);
	}

	public static void rewriteModifiers(final VariableDeclarationExpression declarationNode, final int includedModifiers, final int excludedModifiers, final ASTRewrite rewrite, final TextEditGroup group) {
		ModifierRewrite listRewrite= ModifierRewrite.create(rewrite, declarationNode);
		listRewrite.setModifiers(includedModifiers, excludedModifiers, group);
	}

	public static void rewriteModifiers(final FieldDeclaration declarationNode, final VariableDeclarationFragment[] toChange, final int includedModifiers, final int excludedModifiers, final ASTRewrite rewrite, final TextEditGroup group) {
		final List fragmentsToChange= Arrays.asList(toChange);
		AST ast= declarationNode.getAST();

		List fragments= declarationNode.fragments();
		Iterator iter= fragments.iterator();

		ListRewrite blockRewrite;
		if (declarationNode.getParent() instanceof AbstractTypeDeclaration) {
			blockRewrite= rewrite.getListRewrite(declarationNode.getParent(), ((AbstractTypeDeclaration)declarationNode.getParent()).getBodyDeclarationsProperty());
		} else {
			blockRewrite= rewrite.getListRewrite(declarationNode.getParent(), AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
		}

		VariableDeclarationFragment lastFragment= (VariableDeclarationFragment)iter.next();
		ASTNode lastStatement= declarationNode;

		boolean modifiersModified= false;
		if (fragmentsToChange.contains(lastFragment)) {
			ModifierRewrite modifierRewrite= ModifierRewrite.create(rewrite, declarationNode);
			modifierRewrite.setModifiers(includedModifiers, excludedModifiers, group);
			modifiersModified= true;
		}

		ListRewrite fragmentsRewrite= null;
		while (iter.hasNext()) {
			VariableDeclarationFragment currentFragment= (VariableDeclarationFragment)iter.next();

			if (fragmentsToChange.contains(lastFragment) != fragmentsToChange.contains(currentFragment)) {

					FieldDeclaration newStatement= ast.newFieldDeclaration((VariableDeclarationFragment)rewrite.createMoveTarget(currentFragment));
					newStatement.setType((Type)rewrite.createCopyTarget(declarationNode.getType()));

					ModifierRewrite modifierRewrite= ModifierRewrite.create(rewrite, newStatement);
					if (fragmentsToChange.contains(currentFragment)) {
						modifierRewrite.copyAllAnnotations(declarationNode, group);
						int newModifiers= (declarationNode.getModifiers() & ~excludedModifiers) | includedModifiers;
						modifierRewrite.setModifiers(newModifiers, excludedModifiers, group);
					} else {
						modifierRewrite.copyAllModifiers(declarationNode, group, modifiersModified);
					}
					blockRewrite.insertAfter(newStatement, lastStatement, group);

					fragmentsRewrite= rewrite.getListRewrite(newStatement, FieldDeclaration.FRAGMENTS_PROPERTY);
					lastStatement= newStatement;
			} else if (fragmentsRewrite != null) {
				ASTNode fragment0= rewrite.createMoveTarget(currentFragment);
				fragmentsRewrite.insertLast(fragment0, group);
			}
			lastFragment= currentFragment;
		}
	}

	public static void rewriteModifiers(final VariableDeclarationStatement declarationNode, final VariableDeclarationFragment[] toChange, final int includedModifiers, final int excludedModifiers, ASTRewrite rewrite, final TextEditGroup group) {
		final List fragmentsToChange= Arrays.asList(toChange);
		AST ast= declarationNode.getAST();

		List fragments= declarationNode.fragments();
		Iterator iter= fragments.iterator();

		ListRewrite blockRewrite= null;
		ASTNode parentStatement= declarationNode.getParent();
		if (parentStatement instanceof SwitchStatement) {
			blockRewrite= rewrite.getListRewrite(parentStatement, SwitchStatement.STATEMENTS_PROPERTY);
		} else if (parentStatement instanceof Block) {
			blockRewrite= rewrite.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
		} else {
			// should not happen. VariableDeclaration's can not be in a control statement body
			Assert.isTrue(false);
		}

		VariableDeclarationFragment lastFragment= (VariableDeclarationFragment)iter.next();
		ASTNode lastStatement= declarationNode;

		boolean modifiersModified= false;
		if (fragmentsToChange.contains(lastFragment)) {
			ModifierRewrite modifierRewrite= ModifierRewrite.create(rewrite, declarationNode);
			modifierRewrite.setModifiers(includedModifiers, excludedModifiers, group);
			modifiersModified= true;
		}

		ListRewrite fragmentsRewrite= null;
		while (iter.hasNext()) {
			VariableDeclarationFragment currentFragment= (VariableDeclarationFragment)iter.next();

			if (fragmentsToChange.contains(lastFragment) != fragmentsToChange.contains(currentFragment)) {

					VariableDeclarationStatement newStatement= ast.newVariableDeclarationStatement((VariableDeclarationFragment)rewrite.createMoveTarget(currentFragment));
					newStatement.setType((Type)rewrite.createCopyTarget(declarationNode.getType()));

					ModifierRewrite modifierRewrite= ModifierRewrite.create(rewrite, newStatement);
					if (fragmentsToChange.contains(currentFragment)) {
						modifierRewrite.copyAllAnnotations(declarationNode, group);
						int newModifiers= (declarationNode.getModifiers() & ~excludedModifiers) | includedModifiers;
						modifierRewrite.setModifiers(newModifiers, excludedModifiers, group);
					} else {
						modifierRewrite.copyAllModifiers(declarationNode, group, modifiersModified);
					}
					blockRewrite.insertAfter(newStatement, lastStatement, group);

					fragmentsRewrite= rewrite.getListRewrite(newStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY);
					lastStatement= newStatement;
			} else if (fragmentsRewrite != null) {
				ASTNode fragment0= rewrite.createMoveTarget(currentFragment);
				fragmentsRewrite.insertLast(fragment0, group);
			}
			lastFragment= currentFragment;
		}
	}
}
