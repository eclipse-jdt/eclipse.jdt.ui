/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

/**
 *
 */
public class ModifierRewrite {

	public static final int VISIBILITY_MODIFIERS= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;

	private ListRewrite fModifierRewrite;
	private AST fAst;


	public static ModifierRewrite create(ASTRewrite rewrite, ASTNode declNode) {
		return new ModifierRewrite(rewrite, declNode);
	}

	private ModifierRewrite(ASTRewrite rewrite, ASTNode declNode) {
		fModifierRewrite= evaluateListRewrite(rewrite, declNode);
		fAst= declNode.getAST();
	}

	private ListRewrite evaluateListRewrite(ASTRewrite rewrite, ASTNode declNode) {
		switch (declNode.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				return rewrite.getListRewrite(declNode, MethodDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.FIELD_DECLARATION:
				return rewrite.getListRewrite(declNode, FieldDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				return rewrite.getListRewrite(declNode, VariableDeclarationExpression.MODIFIERS2_PROPERTY);
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				return rewrite.getListRewrite(declNode, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				return rewrite.getListRewrite(declNode, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.TYPE_DECLARATION:
				return rewrite.getListRewrite(declNode, TypeDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.ENUM_DECLARATION:
				return rewrite.getListRewrite(declNode, EnumDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
				return rewrite.getListRewrite(declNode, AnnotationTypeDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return rewrite.getListRewrite(declNode, EnumConstantDeclaration.MODIFIERS2_PROPERTY);
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
				return rewrite.getListRewrite(declNode, AnnotationTypeMemberDeclaration.MODIFIERS2_PROPERTY);
			default:
				throw new IllegalArgumentException("node has no modifiers: " + declNode.getClass().getName()); //$NON-NLS-1$
		}
	}

	public ListRewrite getModifierRewrite() {
		return fModifierRewrite;
	}

	public void setModifiers(int modfiers, TextEditGroup editGroup) {
		internalSetModifiers(modfiers, -1, editGroup);
	}

	public void setModifiers(int included, int excluded, TextEditGroup editGroup) {
		internalSetModifiers(included, included | excluded, editGroup);
	}

	public void setVisibility(int visibilityFlags, TextEditGroup editGroup) {
		internalSetModifiers(visibilityFlags, VISIBILITY_MODIFIERS, editGroup);
	}

	public void copyAllModifiers(ASTNode otherDecl, TextEditGroup editGroup) {
		ListRewrite modifierList= evaluateListRewrite(fModifierRewrite.getASTRewrite(), otherDecl);
		List originalList= modifierList.getOriginalList();
		if (originalList.isEmpty()) {
			return;
		}

		ASTNode copy= modifierList.createCopyTarget((ASTNode) originalList.get(0), (ASTNode) originalList.get(originalList.size() - 1));
		if (copy != null) {
			fModifierRewrite.insertLast(copy, editGroup);
		}
	}

	public void copyAllAnnotations(ASTNode otherDecl, TextEditGroup editGroup) {
		ListRewrite modifierList= evaluateListRewrite(fModifierRewrite.getASTRewrite(), otherDecl);
		List originalList= modifierList.getOriginalList();

		for (Iterator iterator= originalList.iterator(); iterator.hasNext();) {
			IExtendedModifier modifier= (IExtendedModifier) iterator.next();
			if (modifier.isAnnotation()) {
				fModifierRewrite.insertLast(fModifierRewrite.getASTRewrite().createCopyTarget((Annotation) modifier), editGroup);
			}
		}
	}

	private void internalSetModifiers(int modfiers, int consideredFlags, TextEditGroup editGroup) {
		// remove modifiers
		int newModifiers= modfiers & consideredFlags;

		List originalList= fModifierRewrite.getOriginalList();
		for (int i= 0; i < originalList.size(); i++) {
			ASTNode curr= (ASTNode) originalList.get(i);
			if (curr instanceof Modifier) {
				int flag= ((Modifier)curr).getKeyword().toFlagValue();
				if ((consideredFlags & flag) != 0) {
					if ((newModifiers & flag) == 0) {
						fModifierRewrite.remove(curr, editGroup);
					}
					newModifiers &= ~flag;
				}
			}
		}

		// find last annotation
		IExtendedModifier lastAnnotation= null;
		List extendedList= fModifierRewrite.getRewrittenList();
		for (int i= 0; i < extendedList.size(); i++) {
			IExtendedModifier curr= (IExtendedModifier) extendedList.get(i);
			if (curr.isAnnotation())
				lastAnnotation= curr;
		}

		// add modifiers
		List newNodes= ASTNodeFactory.newModifiers(fAst, newModifiers);
		for (int i= 0; i < newNodes.size(); i++) {
			Modifier curr= (Modifier) newNodes.get(i);
			if ((curr.getKeyword().toFlagValue() & VISIBILITY_MODIFIERS) != 0) {
				if (lastAnnotation != null)
					fModifierRewrite.insertAfter(curr, (ASTNode) lastAnnotation, editGroup);
				else
					fModifierRewrite.insertFirst(curr, editGroup);
			} else {
				fModifierRewrite.insertLast(curr, editGroup);
			}
		}
	}
}
