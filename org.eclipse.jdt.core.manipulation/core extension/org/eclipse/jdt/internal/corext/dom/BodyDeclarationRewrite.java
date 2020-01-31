/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.MembersOrderPreferenceCacheCommon;

/**
 * Rewrite helper for body declarations.
 *
 * @see ASTNodes#getBodyDeclarationsProperty(ASTNode)
 *
 * @since 1.10
 */
public class BodyDeclarationRewrite {

	private ASTNode fTypeNode;
	private ListRewrite fListRewrite;

	public static BodyDeclarationRewrite create(ASTRewrite rewrite, ASTNode typeNode) {
		return new BodyDeclarationRewrite(rewrite, typeNode);
	}

	private BodyDeclarationRewrite(ASTRewrite rewrite, ASTNode typeNode) {
		ChildListPropertyDescriptor property= ASTNodes.getBodyDeclarationsProperty(typeNode);
		fTypeNode= typeNode;
		fListRewrite= rewrite.getListRewrite(typeNode, property);
	}

	public void insert(BodyDeclaration decl, TextEditGroup description) {
		List<BodyDeclaration> container= ASTNodes.getBodyDeclarations(fTypeNode);
		int index= getInsertionIndex(decl, container);
		fListRewrite.insertAt(decl, index, description);
	}

	/**
	 * Computes the insertion index to be used to add the given member to the
	 * the list <code>container</code>.
	 * @param member the member to add
	 * @param container a list containing objects of type <code>BodyDeclaration</code>
	 * @return the insertion index to be used
	 */
	public static int getInsertionIndex(BodyDeclaration member, List<? extends BodyDeclaration> container) {
		int containerSize= container.size();

		// use MembersOrderPreferenceCacheCommon which has no UI dependencies and
		// will get Preferences from the node specified by JavaManipulation.getPreferenceNodeId
		MembersOrderPreferenceCacheCommon orderStore= JavaManipulationPlugin.getDefault().getMembersOrderPreferenceCacheCommon();

		int orderIndex= getOrderPreference(member, orderStore);

		int insertPos= containerSize;
		int insertPosOrderIndex= -1;

		for (int i= containerSize - 1; i >= 0; i--) {
			int currOrderIndex= getOrderPreference(container.get(i), orderStore);
			if (orderIndex == currOrderIndex) {
				if (insertPosOrderIndex != orderIndex) { // no perfect match yet
					insertPos= i + 1; // after a same kind
					insertPosOrderIndex= orderIndex; // perfect match
				}
			} else if (insertPosOrderIndex != orderIndex) { // not yet a perfect match
				if (currOrderIndex < orderIndex) { // we are bigger
					if (insertPosOrderIndex == -1) {
						insertPos= i + 1; // after
						insertPosOrderIndex= currOrderIndex;
					}
				} else {
					insertPos= i; // before
					insertPosOrderIndex= currOrderIndex;
				}
			}
		}
		return insertPos;
	}

	private static int getOrderPreference(BodyDeclaration member, MembersOrderPreferenceCacheCommon store) {
		int memberType= member.getNodeType();
		int modifiers= member.getModifiers();

		switch (memberType) {
			case ASTNode.TYPE_DECLARATION:
			case ASTNode.ENUM_DECLARATION :
			case ASTNode.ANNOTATION_TYPE_DECLARATION :
				return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.TYPE_INDEX) * 2;
			case ASTNode.FIELD_DECLARATION:
				if (Modifier.isStatic(modifiers)) {
					int index= store.getCategoryIndex(MembersOrderPreferenceCacheCommon.STATIC_FIELDS_INDEX) * 2;
					if (Modifier.isFinal(modifiers)) {
						return index; // first final static, then static
					}
					return index + 1;
				}
				return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.FIELDS_INDEX) * 2;
			case ASTNode.INITIALIZER:
				if (Modifier.isStatic(modifiers)) {
					return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.STATIC_INIT_INDEX) * 2;
				}
				return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.INIT_INDEX) * 2;
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
				return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.METHOD_INDEX) * 2;
			case ASTNode.METHOD_DECLARATION:
				if (Modifier.isStatic(modifiers)) {
					return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.STATIC_METHODS_INDEX) * 2;
				}
				if (((MethodDeclaration) member).isConstructor()) {
					return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.CONSTRUCTORS_INDEX) * 2;
				}
				return store.getCategoryIndex(MembersOrderPreferenceCacheCommon.METHOD_INDEX) * 2;
			default:
				return 100;
		}
	}
}
