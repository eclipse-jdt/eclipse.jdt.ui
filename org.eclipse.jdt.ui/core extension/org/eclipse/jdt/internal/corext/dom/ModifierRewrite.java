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
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
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
	
	private static final int VISIBILITY_MODIFIERS= Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;
	
	private ListRewrite fModifierRewrite;
	private AST fAst;


	public static ModifierRewrite create(ASTRewrite rewrite, ASTNode declNode) {
		return new ModifierRewrite(rewrite, declNode);
	}

	private ModifierRewrite(ASTRewrite rewrite, ASTNode declNode) {
		ListRewrite modifierRewrite= null;
		switch (declNode.getNodeType()) {
			case ASTNode.METHOD_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, MethodDeclaration.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.FIELD_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, FieldDeclaration.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				modifierRewrite= rewrite.getListRewrite(declNode, VariableDeclarationExpression.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				modifierRewrite= rewrite.getListRewrite(declNode, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.TYPE_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, TypeDeclaration.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.ENUM_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, EnumDeclaration.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, AnnotationTypeDeclaration.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, EnumConstantDeclaration.MODIFIERS2_PROPERTY);
				break;
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
				modifierRewrite= rewrite.getListRewrite(declNode, AnnotationTypeMemberDeclaration.MODIFIERS2_PROPERTY);
				break;
			default:
				throw new IllegalArgumentException("node has no modfiers: " + declNode.getClass().getName()); //$NON-NLS-1$
		}
		fModifierRewrite= modifierRewrite;
		fAst= declNode.getAST();
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
	
	
	private void internalSetModifiers(int modfiers, int consideredFlags, TextEditGroup editGroup) {
		// remove modfiers
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
		List newNodes= ASTNodeFactory.newModifiers(fAst, newModifiers);
		
		// add modifiers
		for (int i= 0; i < newNodes.size(); i++) {
			Modifier curr= (Modifier) newNodes.get(i);
			if ((curr.getKeyword().toFlagValue() & VISIBILITY_MODIFIERS) != 0) {
				fModifierRewrite.insertFirst(curr, editGroup);
			} else {
				fModifierRewrite.insertLast(curr, editGroup);
			}
		}
	}
	

}
