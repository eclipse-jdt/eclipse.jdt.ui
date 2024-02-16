/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.internal.ui.text.correction.JavadocTagsSubProcessorCore;

public class JavadocUtil {

	private static List<String> tagOrder= Arrays.asList(
			TagElement.TAG_AUTHOR,
			TagElement.TAG_VERSION,
			TagElement.TAG_PARAM,
			TagElement.TAG_RETURN,
			TagElement.TAG_THROWS,
			TagElement.TAG_EXCEPTION,
			TagElement.TAG_SEE,
			TagElement.TAG_SINCE,
			TagElement.TAG_SERIAL,
			TagElement.TAG_SERIALFIELD,
			TagElement.TAG_SERIALDATA,
			TagElement.TAG_DEPRECATED,
			TagElement.TAG_VALUE
	);

	private JavadocUtil() {
		// static-only
	}

	public static TagElement createParamTag(String parameterName, AST ast, IJavaProject javaProject) {
		TagElement paramNode= ast.newTagElement();
		paramNode.setTagName(TagElement.TAG_PARAM);

		SimpleName simpleName= ast.newSimpleName(parameterName);
		paramNode.fragments().add(simpleName);

		TextElement textElement= ast.newTextElement();
		String text= StubUtility.getTodoTaskTag(javaProject);
		if (text != null)
			textElement.setText(text); //TODO: use template with {@todo} ...
		paramNode.fragments().add(textElement);

		return paramNode;
	}

	/**
	 * Decide whether to add a "param" javadoc tag or not.
	 * @param methodDeclaration the method declaration
	 * @return method has javadoc {@code &&} (method had no parameter before || there is already an {@code @param} tag)
	 */
	public static boolean shouldAddParamJavadoc(MethodDeclaration methodDeclaration) {
		Javadoc javadoc= methodDeclaration.getJavadoc();
		if (javadoc == null)
			return false;
		if (methodDeclaration.parameters().size() == 0)
			return true;
		List<TagElement> tags= javadoc.tags();
		for (TagElement element : tags) {
			if (TagElement.TAG_PARAM.equals(element.getTagName()))
				return true;
		}
		return false;
	}


	/**
	 * Adds a "param" javadoc tag for a new last parameter if necessary.
	 */
	public static void addParamJavadoc(String parameterName, MethodDeclaration methodDeclaration,
			ASTRewrite astRewrite, IJavaProject javaProject, TextEditGroup groupDescription) {
		if (! shouldAddParamJavadoc(methodDeclaration))
			return;

		ListRewrite tagsRewrite= astRewrite.getListRewrite(methodDeclaration.getJavadoc(), Javadoc.TAGS_PROPERTY);
		HashSet<String> leadingNames= new HashSet<>();
		for (Iterator<SingleVariableDeclaration> iter= methodDeclaration.parameters().iterator(); iter.hasNext();) {
			SingleVariableDeclaration curr= iter.next();
			leadingNames.add(curr.getName().getIdentifier());
		}
		TagElement parameterTag= createParamTag(parameterName, astRewrite.getAST(), javaProject);
		JavadocTagsSubProcessorCore.insertTag(tagsRewrite, parameterTag, leadingNames, groupDescription);
	}

	/**
	 * In a given list of tags, finds the one after which a first tag of type {@code tagName} should
	 * be inserted.
	 *
	 * @param tags existing tags
	 * @param tagName name of tag to add
	 * @return the <code>TagElement</code> just before the positions where a new
	 *         <code>TagElement</code> with name <code>tagName</code> should be inserted, or
	 *         <code>null</code> if it should be inserted as first.
	 */
	public static TagElement findTagElementToInsertAfter(List<TagElement> tags, String tagName) {
		int goalOrdinal= tagOrder.indexOf(tagName);
		if (goalOrdinal == -1) // unknown tag -> to end
			return (tags.isEmpty()) ? null : (TagElement) tags.get(tags.size());
		for (int i= 0; i < tags.size(); i++) {
			int tagOrdinal= tagOrder.indexOf(tags.get(i).getTagName());
			if (tagOrdinal >= goalOrdinal)
				return (i == 0) ? null : (TagElement) tags.get(i - 1);
		}
		return (tags.isEmpty()) ? null : (TagElement) tags.get(tags.size() - 1);
	}

	/**
	 * In a given list of tags, finds the one before which a last tag of type {@code tagName} should
	 * be inserted.
	 *
	 * @param tags existing tags
	 * @param tagName name of tag to add
	 * @return the <code>TagElement</code> just after the positions where a new
	 *         <code>TagElement</code> with name <code>tagName</code> should be inserted, or
	 *         <code>null</code> if it should be inserted as last.
	 */
	public static TagElement findTagElementToInsertBefore(List<TagElement> tags, String tagName) {
		int goalOrdinal= tagOrder.indexOf(tagName);
		if (goalOrdinal == -1) // unknown tag -> to end
			return (tags.isEmpty()) ? null : (TagElement) tags.get(tags.size());
		for (int i= tags.size() - 1; i >= 0; i--) {
			int tagOrdinal= tagOrder.indexOf(tags.get(i).getTagName());
			if (tagOrdinal <= goalOrdinal)
				return (i == tags.size() - 1) ? null : (TagElement) tags.get(i + 1);
		}
		return null;
	}

}
