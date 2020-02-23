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

	private JavadocUtil() {
		// static-only
	}

	//TODO: is a copy of ChangeSignatureRefactoring.DeclarationUpdate#createParamTag(..)
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
	 * @return method has javadoc && (method had no parameter before || there is already an @param tag)
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
	 * @param parameterName
	 * @param methodDeclaration
	 * @param astRewrite
	 * @param javaProject
	 * @param groupDescription
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
}
