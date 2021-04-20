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
 *     Red Hat Inc. - subset of JavadocTagsSubProcessor moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

/**
 *
 */
public class JavadocTagsSubProcessorCore {


	public static Set<String> getPreviousTypeParamNames(List<TypeParameter> typeParams, ASTNode missingNode) {
		Set<String> previousNames=  new HashSet<>();
		for (TypeParameter curr : typeParams) {
			if (curr == missingNode) {
				return previousNames;
			}
			previousNames.add('<' + curr.getName().getIdentifier() + '>');
		}
		return previousNames;
	}

	public static TagElement findTag(Javadoc javadoc, String name, String arg) {
		List<TagElement> tags= javadoc.tags();
		for (TagElement curr : tags) {
			if (name.equals(curr.getTagName())) {
				if (arg != null) {
					String argument= getArgument(curr);
					if (arg.equals(argument)) {
						return curr;
					}
				} else {
					return curr;
				}
			}
		}
		return null;
	}

	public static TagElement findParamTag(Javadoc javadoc, String arg) {
		List<TagElement> tags= javadoc.tags();
		for (TagElement curr : tags) {
			String currName= curr.getTagName();
			if (TagElement.TAG_PARAM.equals(currName)) {
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}


	public static TagElement findThrowsTag(Javadoc javadoc, String arg) {
		List<TagElement> tags= javadoc.tags();
		for (TagElement curr : tags) {
			String currName= curr.getTagName();
			if (TagElement.TAG_THROWS.equals(currName) || TagElement.TAG_EXCEPTION.equals(currName)) {
				String argument= getArgument(curr);
				if (arg.equals(argument)) {
					return curr;
				}
			}
		}
		return null;
	}

	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set<String> sameKindLeadingNames) {
		insertTag(rewriter, newElement, sameKindLeadingNames, null);
	}

	public static void insertTag(ListRewrite rewriter, TagElement newElement, Set<String> sameKindLeadingNames, TextEditGroup groupDescription) {
		List<? extends ASTNode> tags= rewriter.getRewrittenList();

		String insertedTagName= newElement.getTagName();

		ASTNode after= null;
		int tagRanking= getTagRanking(insertedTagName);
		for (int i= tags.size() - 1; i >= 0; i--) {
			TagElement curr= (TagElement) tags.get(i);
			String tagName= curr.getTagName();
			if (tagName == null || tagRanking > getTagRanking(tagName)) {
				after= curr;
				break;
			}
			if (sameKindLeadingNames != null && isSameTag(insertedTagName, tagName)) {
				String arg= getArgument(curr);
				if (arg != null && sameKindLeadingNames.contains(arg)) {
					after= curr;
					break;
				}
			}
		}
		if (after != null) {
			rewriter.insertAfter(newElement, after, groupDescription);
		} else {
			rewriter.insertFirst(newElement, groupDescription);
		}
	}

	private static boolean isSameTag(String insertedTagName, String tagName) {
		if (insertedTagName.equals(tagName)) {
			return true;
		}
		if (TagElement.TAG_EXCEPTION.equals(tagName)) {
			return TagElement.TAG_THROWS.equals(insertedTagName);
		}
		return false;
	}

	private static String[] TAG_ORDER= { // see http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#orderoftags
		TagElement.TAG_AUTHOR,
		TagElement.TAG_VERSION,
		TagElement.TAG_PARAM,
		TagElement.TAG_RETURN,
		TagElement.TAG_THROWS, // synonym to TAG_EXCEPTION
		TagElement.TAG_SEE,
		TagElement.TAG_SINCE,
		TagElement.TAG_SERIAL,
		TagElement.TAG_DEPRECATED
	};

	private static int getTagRanking(String tagName) {
		if (TagElement.TAG_EXCEPTION.equals(tagName)) {
			tagName= TagElement.TAG_THROWS;
		}
		for (int i= 0; i < TAG_ORDER.length; i++) {
			if (tagName.equals(TAG_ORDER[i])) {
				return i;
			}
		}
		return TAG_ORDER.length;
	}

	private static String getArgument(TagElement curr) {
		List<? extends ASTNode> fragments= curr.fragments();
		if (!fragments.isEmpty()) {
			Object first= fragments.get(0);
			if (first instanceof Name) {
				return ASTNodes.getSimpleNameIdentifier((Name) first);
			} else if (first instanceof TextElement && TagElement.TAG_PARAM.equals(curr.getTagName())) {
				String text= ((TextElement) first).getText();
				if ("<".equals(text) && fragments.size() >= 3) { //$NON-NLS-1$
					Object second= fragments.get(1);
					Object third= fragments.get(2);
					if (second instanceof Name && third instanceof TextElement && ">".equals(((TextElement) third).getText())) { //$NON-NLS-1$
						return '<' + ASTNodes.getSimpleNameIdentifier((Name) second) + '>';
					}
				} else if (text.startsWith(String.valueOf('<')) && text.endsWith(String.valueOf('>')) && text.length() > 2) {
					return text.substring(1, text.length() - 1);
				}
			}
		}
		return null;
	}

	private JavadocTagsSubProcessorCore() {
	}

}
