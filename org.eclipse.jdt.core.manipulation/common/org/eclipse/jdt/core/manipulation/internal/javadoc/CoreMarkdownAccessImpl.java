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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Document;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;

public class CoreMarkdownAccessImpl extends CoreJavadocAccessImpl {

	private Parser fParser;
	private HtmlRenderer fRenderer;
	private int fBlockDepth= 0;

	public CoreMarkdownAccessImpl(IJavaElement element, Javadoc javadoc, String source) {
		super(element, javadoc, source);
		init();
	}
	public CoreMarkdownAccessImpl(IJavaElement element, Javadoc javadoc, String source, JavadocLookup lookup) {
		super(element, javadoc, source, lookup);
		init();
	}

	private void init() {
		List<Extension> extensions= List.of(TablesExtension.create());
		fParser= Parser.builder().extensions(extensions).build();
		fRenderer= HtmlRenderer.builder()
				.extensions(extensions)
				.nodeRendererFactory(JavaSyntaxHighlightingRenderer::new)
				.build();
	}

	private static final Set<String> JAVA_LANGUAGES= Set.of("java", "jav"); //$NON-NLS-1$ //$NON-NLS-2$

	private static volatile FencedCodeTokenizer fFencedCodeTokenizer;
	private static volatile boolean fTokenizerInitialized;
	private static volatile boolean fTokenizerFailed;

	public static void setFencedCodeTokenizer(FencedCodeTokenizer tokenizer) {
		fFencedCodeTokenizer= tokenizer;
		fTokenizerInitialized= true;
	}

	public static FencedCodeTokenizer getFencedCodeTokenizer() {
		if (!fTokenizerInitialized) {
			initTokenizer();
		}
		return fFencedCodeTokenizer;
	}

	private static synchronized void initTokenizer() {
		if (fTokenizerInitialized) {
			return;
		}
		try {
			org.osgi.framework.Bundle bundle= org.eclipse.core.runtime.Platform.getBundle("org.eclipse.jdt.ui.tm4e"); //$NON-NLS-1$
			if (bundle != null) {
				bundle.start();
			}
		} catch (Exception e) {
			// tm4e bundle not available or failed to start — fall back to plain text
		}
		fTokenizerInitialized= true;
	}

	static class JavaSyntaxHighlightingRenderer implements NodeRenderer {
		private final HtmlNodeRendererContext context;

		JavaSyntaxHighlightingRenderer(HtmlNodeRendererContext context) {
			this.context= context;
		}

		@Override
		public Set<Class<? extends Node>> getNodeTypes() {
			return Collections.singleton(FencedCodeBlock.class);
		}

		@Override
		public void render(Node node) {
			FencedCodeBlock codeBlock= (FencedCodeBlock) node;
			String info= codeBlock.getInfo();
			if (info != null) {
				info= info.trim().split("\\s+", 2)[0]; //$NON-NLS-1$
			}
			String literal= codeBlock.getLiteral();
			boolean isJava= info != null && JAVA_LANGUAGES.contains(info.trim().toLowerCase());

			HtmlWriter html= context.getWriter();
			html.line();
			if (isJava) {
				html.tag("pre"); //$NON-NLS-1$
				html.tag("code", Map.of("class", "language-java")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				html.raw(JavaCodeTokenizer.tokenize(literal));
			} else {
				html.tag("pre"); //$NON-NLS-1$
				Map<String, String> attrs= info != null && !info.isEmpty()
						? Map.of("class", "language-" + info.trim()) //$NON-NLS-1$ //$NON-NLS-2$
						: Collections.emptyMap();
				html.tag("code", attrs); //$NON-NLS-1$
				String tokenized= null;
				if (info != null && !info.isEmpty()) {
					try {
						FencedCodeTokenizer tokenizer= getFencedCodeTokenizer();
						if (tokenizer != null) {
							tokenized= tokenizer.tokenize(info.trim().toLowerCase(), literal);
						}
					} catch (Exception | LinkageError e) {
						if (!fTokenizerFailed) {
							fTokenizerFailed= true;
							org.eclipse.core.runtime.ILog.get(CoreMarkdownAccessImpl.class)
									.error("Fenced code tokenizer failed, falling back to plain text", e); //$NON-NLS-1$
						}
						fFencedCodeTokenizer= null;
						fTokenizerInitialized= false;
					}
				}
				if (tokenized != null) {
					html.raw(tokenized);
				} else {
					html.text(literal);
				}
			}
			html.tag("/code"); //$NON-NLS-1$
			html.tag("/pre"); //$NON-NLS-1$
			html.line();
		}
	}

	@Override
	protected String removeDocLineIntros(String textWithSlashes) {
		// handle unicode
		String content= textWithSlashes.replaceAll("\\\\u000[d,D]", "\r"); //$NON-NLS-1$ //$NON-NLS-2$
		content= content.replaceAll("\\\\u000[a,A]", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		String lineBreakGroup= "(\\r\\n?|\\n)"; //$NON-NLS-1$
		String noBreakSpace= "[^\r\n&&\\s]"; //$NON-NLS-1$
		// in the markdown case relevant leading whitespace is contained in TextElements, no need to preserve blanks *between* elements
		return content.replaceAll(lineBreakGroup + noBreakSpace + "*///" + noBreakSpace + '*', "$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected void handleLink(List<? extends ASTNode> fragments) {
		if (fragments.size() == 2 && fragments.get(0) instanceof TextElement && !(fragments.get(1) instanceof TextElement)) {
			// super method expects the reference as first fragment, optional label as second fragment
			fragments= Arrays.asList(fragments.get(1), fragments.get(0));
		}
		super.handleLink(fragments);
	}

	@Override
	protected String getBlockTagStart() {
		this.fBlockDepth++;
		return "\n"+super.getBlockTagStart(); //$NON-NLS-1$
	}

	@Override
	protected String getBlockTagEnd() {
		if (this.fBlockDepth > 0)
			this.fBlockDepth--;
		return super.getBlockTagEnd();
	}

	@Override
	protected void handleContentElements(List<? extends ASTNode> nodes, boolean skipLeadingWhitespace, TagElement tagElement) {
		int start= fBuf.length();
		super.handleContentElements(nodes, skipLeadingWhitespace, tagElement);
		if (this.fBlockDepth > 0) {
			// inside an HTML block the markdown content must be rendered now
			String generated= fBuf.substring(start); // extract new part of fBuf
			Node node= fParser.parse(generated);
			if (node.getFirstChild() instanceof Paragraph para && para.getNext() == null) {
				// inside block replace single paragraph with its children
				node= eliminateContainerNode(para);
			}
			String rendered= fRenderer.render(node);
			fBuf.replace(start, fBuf.length(), rendered); // replace new part with its rendered version
		}
	}

	/** Return a new Document containing all children of the given container node. */
	protected Node eliminateContainerNode(Node container) {
		List<Node> children= new ArrayList<>();
		for (Node child= container.getFirstChild(); child != null; child= child.getNext()) {
			children.add(child);
		}
		Document doc= new Document();
		for (Node child2 : children) {
			doc.appendChild(child2);
		}
		return doc;
	}

	@Override
	public String toHTML() {
		String content= super.toHTML();
		Node document= fParser.parse(content);
		return fRenderer.render(document);
	}
}
