/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Contribution for Bug 403917 - [1.8] Render TYPE_USE annotations in Javadoc hover/view
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;

public abstract class AbstractBindingLabelsTest extends CoreTests {

	public AbstractBindingLabelsTest(String name) {
		super(name);
	}

	protected IJavaProject fJProject1;
	protected boolean fHaveSource= true;

	@Override
	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	protected String getBindingLabel(IJavaElement elem, long flags) {
		ASTParser parser= ASTParser.newParser(AST.JLS12);
		parser.setResolveBindings(true);
		parser.setProject(fJProject1);
		IBinding binding= parser.createBindings(new IJavaElement[]{elem}, null)[0];
		return JavaElementLinks.getBindingLabel(binding, elem, flags, fHaveSource);
	}

	protected void assertLink(String lab, String display) {
		Pattern pattern= Pattern.compile("<a class='header' href='eclipse-javadoc:.*'>(.*)</a>");
		Matcher matcher= pattern.matcher(lab);
		assertEquals("number of match groups", 1, matcher.groupCount());
		assertTrue("Label doesn't match against expected pattern: "+lab, matcher.matches());
		assertEquals("display label", display, matcher.group(1));
	}

	protected void assertLink(String lab, String display, String title) {
		Pattern pattern= Pattern.compile("<a class='header' href='eclipse-javadoc:.*' title='(.*)'>(.*)</a>");
		Matcher matcher= pattern.matcher(lab);
		assertEquals("number of match groups", 2, matcher.groupCount());
		assertTrue("Label doesn't match against expected pattern: "+lab, matcher.matches());
		assertEquals("title", title, matcher.group(1));
		assertEquals("display label", display, matcher.group(2));
	}

	/*
	 * expectedMarkup may contain any number of occurrences of "{{qualifier|name}}",
	 * which will be matched as a link with link text "name" and a link title "in qualifier".
	 * Optionally, {{name}} will be matched as a link with a link text but no title.
	 */
	protected void assertLinkMatch(String label, String expectedMarkup) {
		// to avoid matching the entire unreadable label in one step we co-parse expected and actual value:
		int patternPos= 0;
		int labelPos= 0;
		int fragmentCount= 0;
		while (patternPos < expectedMarkup.length()) {
			// analyze expected mark-up:
			int open= expectedMarkup.indexOf("{{", patternPos);
			if (open == -1)
				break;
			int pipe= expectedMarkup.indexOf('|', open);
			if (pipe > -1) {
				int nextOpen= expectedMarkup.indexOf("{{", open+2);
				if (nextOpen > -1 && nextOpen < pipe)
					pipe= -1; // pipe belongs to next link
			}
			boolean hasTitle= pipe != -1;
			int close= expectedMarkup.indexOf("}}", hasTitle ? pipe : open);
			if (close + 2 < expectedMarkup.length() && expectedMarkup.charAt(close+2) == '}')
				close++; // position to the end of "}}}"

			if (open > patternPos) {
				// matching plain text:
				String expected= expectedMarkup.substring(patternPos, open);
				int end= label.indexOf("<a class", labelPos);
				assertTrue("next anchor not found ("+fragmentCount+")", end != -1);
				assertEquals("plain text ("+fragmentCount+")", escape(expected), label.substring(labelPos, end));
				fragmentCount++;

				labelPos= end;
			}
			
			if (close != -1) {
				// matching a link "<a class='header' href='eclipse-javadoc:IGNORE' title='LINK_TITLE'>LINK_TEXT</a>"
				assertTrue("link found ("+fragmentCount+")", label.substring(labelPos).startsWith( "<a class='header' href='eclipse-javadoc:"));
				String linkText= expectedMarkup.substring(hasTitle ? pipe+1 : open+2, close);
				String linkTitle= hasTitle ? "in "+expectedMarkup.substring(open+2, pipe) : null;
				if (linkTitle != null) {
					// match linkTitle & linkText:
					int start= label.indexOf("' title='", labelPos);
					assertTrue("title start not found", start != -1);
					start += "' title='".length();
					int end= label.indexOf('\'', start);
					assertTrue("title end not found", end != -1);
					assertEquals("title ("+fragmentCount+")", linkTitle, label.substring(start, end));
					fragmentCount++;

					start= label.indexOf("'>", end) + 2;
					assertTrue("link text start not found", start != -1);
					end= label.indexOf("</a>", start);
					assertTrue("link text end not found", end != -1);
					assertEquals("link text ("+fragmentCount+")", escape(linkText), label.substring(start, end));
					fragmentCount++;

					labelPos= end + "</a>".length();
				} else {
					// match only linkText
					int start= label.indexOf("'>", labelPos) + 2;
					assertTrue("link text start not found", start != -1);
					int end= label.indexOf("</a>", start+1);
					assertTrue("link text end not found", end != -1);
					assertEquals("link text ("+fragmentCount+")", escape(linkText), label.substring(start, end));
					fragmentCount++;

					labelPos= end + "</a>".length();
				}
			}
			patternPos= close+2;
		}
		if (patternPos < expectedMarkup.length()) {
			// matching tailing plain text:
			String expected= expectedMarkup.substring(patternPos);
			assertEquals("plain text ("+(fragmentCount)+")", escape(expected), label.substring(labelPos));
		}
	}

	protected String escape(String element) {
		return element.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	protected URI extractURI(String label) throws URISyntaxException {
		String anchor= "href='";
		int start= label.indexOf(anchor)+anchor.length();
		int end= label.indexOf('\'', start+1);
		return new URI(label.substring(start+1, end));
	}

}
