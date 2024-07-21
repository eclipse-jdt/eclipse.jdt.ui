/*******************************************************************************
 * Copyright (c) 2024 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.Javadoc;

public class CoreMarkdownAccessImpl extends CoreJavadocAccessImpl {

	public CoreMarkdownAccessImpl(IJavaElement element, Javadoc javadoc, String source) {
		super(element, javadoc, source);
	}

	public CoreMarkdownAccessImpl(IJavaElement element, Javadoc javadoc, String source, JavadocLookup lookup) {
		super(element, javadoc, source, lookup);
	}

	@Override
	protected String removeDocLineIntros(String textWithStars) {
		String lineBreakGroup= "(\\r\\n?|\\n)"; //$NON-NLS-1$
		String noBreakSpace= "[^\r\n&&\\s]"; //$NON-NLS-1$
		return textWithStars.replaceAll(lineBreakGroup + noBreakSpace + "///" /*+ noBreakSpace + '?'*/, "$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected String getBlockTagStart() {
		return "\n"+super.getBlockTagStart(); //$NON-NLS-1$
	}

	@Override
	public String toHTML() {
		String content = super.toHTML();
		Parser parser = Parser.builder().build();
		Node document = parser.parse(content);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document);
	}

}
