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
package org.eclipse.jdt.ui.tests.hover;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;

import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.core.rules.Java23ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;

public class MarkdownCommentTests extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new Java23ProjectTestSetup("TestSetupProject", false);

	private IJavaProject fJProject1;

	// copies from CoreJavaElementLinks
	private static final char LINK_SEPARATOR= '\u2602';
	private static final char LINK_BRACKET_REPLACEMENT= '\u2603';

	// copies from JavaElement:
	static final char JEM_JAVAPROJECT= '=';
	static final char JEM_PACKAGEFRAGMENTROOT= '/';
	static final char JEM_PACKAGEFRAGMENT= '<';
	static final char JEM_FIELD= '^';
	static final char JEM_METHOD= '~';
	static final char JEM_COMPILATIONUNIT= '{';
	static final char JEM_TYPE= LINK_BRACKET_REPLACEMENT; // replacement for '['

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	protected ICompilationUnit getWorkingCopy(String path, String source, WorkingCopyOwner owner) throws JavaModelException {
		ICompilationUnit workingCopy= (ICompilationUnit) JavaCore.create(getFile(path));
		if (owner != null)
			workingCopy= workingCopy.getWorkingCopy(owner, null/*no progress monitor*/);
		else
			workingCopy.becomeWorkingCopy(null/*no progress monitor*/);
		workingCopy.getBuffer().setContents(source);
		workingCopy.makeConsistent(null/*no progress monitor*/);
		return workingCopy;
	}

	protected IFile getFile(String path) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		return root.getFile(new Path(path));
	}

	private String makeEncodedClassUri(String pack, String cuName, String clazz) {
		try {
			StringBuilder buf= new StringBuilder();
			buf.append(LINK_SEPARATOR);
			buf.append(JEM_JAVAPROJECT).append("TestSetupProject");
			buf.append(JEM_PACKAGEFRAGMENTROOT).append("src");
			buf.append(JEM_PACKAGEFRAGMENT).append(pack);
			buf.append(JEM_COMPILATIONUNIT).append(cuName).append(".java");
			buf.append(JEM_TYPE).append(clazz);
			URI uri= new URI("eclipse-javadoc", buf.toString(), null);
			return uri.toASCIIString();
		} catch (URISyntaxException e) {
			fail(e);
			return null;
		}
	}

	/** Create a javadoc link to the specified method. */
	private String makeEncodedMethodUri(String pack, String cuName, String clazz, String selector, String... parameters) {
		return makeEncodedMethodUri(false, pack, cuName, clazz, selector, parameters);
	}
	/** Create a javadoc link for the specified method.
	 * @param asScopeURI when {@code true} the link is intended as a scope prefix for {@link #makeEncodedRelativeUri(String, String...)}.
	 */
	private String makeEncodedMethodUri(boolean asScopeURI, String pack, String cuName, String clazz, String selector, String... parameters) {
		try {
			StringBuilder buf= new StringBuilder();
			buf.append(LINK_SEPARATOR);
			buf.append(JEM_JAVAPROJECT).append("TestSetupProject");
			buf.append(JEM_PACKAGEFRAGMENTROOT).append("src");
			buf.append(JEM_PACKAGEFRAGMENT).append(pack);
			buf.append(JEM_COMPILATIONUNIT).append(cuName).append(".java");
			buf.append(JEM_TYPE).append(clazz);

			if (asScopeURI) {
				buf.append(JEM_METHOD);
			} else {
				buf.append(LINK_SEPARATOR);
				buf.append(LINK_SEPARATOR);
			}
			buf.append(selector);
			for (String parameter : parameters) {
				if (asScopeURI) {
					buf.append(JEM_METHOD);
				} else {
					buf.append(LINK_SEPARATOR);
				}
				buf.append(parameter);
			}
			URI uri= new URI("eclipse-javadoc", buf.toString(), null);
			return uri.toASCIIString();
		} catch (URISyntaxException e) {
			fail(e);
			return null;
		}
	}

	/** Create a javadoc link to the element specified by moreWords as seen from the scope specified by scopeURI. */
	private String makeEncodedRelativeUri(String scopeURI, String... moreWords) {
		try {
			StringBuilder buf= new StringBuilder(scopeURI);
			for (String parameter : moreWords) {
				buf.append(LINK_SEPARATOR).append(parameter);
			}
			URI uri= new URI(buf.toString());
			return uri.toASCIIString();
		} catch (URISyntaxException e) {
			fail(e);
			return null;
		}
	}

	private <T extends ISourceReference & IJavaElement> String getHoverHtmlContent(ICompilationUnit cu, T element) throws JavaModelException {
		ISourceRange range= element.getNameRange();
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(
				new IJavaElement[] { element },
				cu,
				new Region(range.getOffset(), range.getLength()),
				null);
		return hoverInfo.getHtml();
	}

	/** Strips standard head and tail of actualContent for convenient assertEquals() comparison. */
	private void assertHtmlContent(String expectedContent, String actualContent) {
		int start= actualContent.indexOf("<a class='header'");
		String headerTail= "</div></h5><br><p>";
		start= actualContent.indexOf(headerTail, start);
		int end= actualContent.lastIndexOf("</body></html>");
		assertEquals(expectedContent, actualContent.substring(start+headerTail.length(), end));
	}

	@Test
	public void testBasicFormatting() throws Exception {
		String source= """
				package p;
				/// ## TestClass
				///
				/// Paragraph
				///
				/// - item 1
				/// - _item 2_
				public class TestClass {
					/// ### m()
					///
					/// Paragraph with _emphasis_
					/// - item 1
					/// - item 2
					/// @param i an _integer_ !
					void m(int i) {
					}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/TestClass.java", source, null);
		assertNotNull("TestClass.java", cu);

		IType type= cu.getType("TestClass");
		String actualHtmlContent= getHoverHtmlContent(cu, type);

		assertHtmlContent("""
				<h2>TestClass</h2>
				<p>Paragraph</p>
				<ul>
				<li>item 1</li>
				<li><em>item 2</em></li>
				</ul>
				""",
				actualHtmlContent);

		IMethod elem= type.getMethods()[0];
		actualHtmlContent= getHoverHtmlContent(cu, elem);

		assertHtmlContent("""
				<h3>m()</h3>
				<p>Paragraph with <em>emphasis</em></p>
				<ul>
				<li>item 1</li>
				<li>item 2</li>
				</ul>
				<dl><dt>Parameters:</dt><dd><b>i</b> an <em>integer</em> !</dd></dl>
				""",
				actualHtmlContent);
	}

	@Test
	public void testItemAtEnd() throws Exception {
		String source= """
				package p;
				/// - item 1
				/// - item 2
				public class TestClass {
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/TestClass.java", source, null);
		assertNotNull("TestClass.java", cu);

		IType type= cu.getType("TestClass");
		String actualHtmlContent= getHoverHtmlContent(cu, type);
		assertHtmlContent("""
				<ul>
				<li>item 1</li>
				<li>item 2</li>
				</ul>
				""",
				actualHtmlContent);
	}

	@Test
	public void testMarkdownLink1() throws Exception {
		String source= """
				package p;
				/// ## TestClass
				///
				/// Please have a _look at [#m1(int)]_ if you like.
				public class TestClass {
					public void m1(int i) {}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/TestClass.java", source, null);
		assertNotNull("TestClass.java", cu);

		String expectedURI= makeEncodedMethodUri("p", "TestClass", "TestClass", "m1", "int");
		String expectedHtmlContent= """
				<h2>TestClass</h2>
				<p>Please have a <em>look at <code><a href='URI'>m1(int)</a></code></em> if you like.</p>
				"""
				.replace("URI", expectedURI);

		IType type= cu.getType("TestClass");
		String actualHtmlContent= getHoverHtmlContent(cu, type);
		assertHtmlContent(expectedHtmlContent, actualHtmlContent);
	}

	@Test
	public void testMarkdownLink2() throws Exception {
		String source= """
				package p;
				/// ## TestClass
				///
				/// Please have a _look at [method 1][#m1(int)]_ if you like.
				public class TestClass {
					public void m1(int i) {}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/TestClass.java", source, null);
		assertNotNull("TestClass.java", cu);

		String expectedURI= makeEncodedMethodUri("p", "TestClass", "TestClass", "m1", "int");
		String expectedHtmlContent= """
				<h2>TestClass</h2>
				<p>Please have a <em>look at <code><a href='URI'>method 1</a></code></em> if you like.</p>
				"""
				.replace("URI", expectedURI);

		IType type= cu.getType("TestClass");
		String actualHtmlContent= getHoverHtmlContent(cu, type);
		assertHtmlContent(expectedHtmlContent, actualHtmlContent);
	}

	@Test
	public void testMarkdownLink3() throws Exception {
		// reference mentions an array type: array-[] must be escaped
		String source= """
				package p;
				/// ## TestClass
				///
				/// Simple link [#m1(int\\[\\])].
				/// Link with custom text [method 1][#m1(int\\[\\])].
				public class TestClass {
					public void m1(int[] i) {}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/TestClass.java", source, null);
		assertNotNull("TestClass.java", cu);

		String expectedURI= makeEncodedMethodUri("p", "TestClass", "TestClass", "m1", "int []");
		String expectedHtmlContent= """
				<h2>TestClass</h2>
				<p>Simple link <code><a href='URI'>m1(int [])</a></code>.
				Link with custom text <code><a href='URI'>method 1</a></code>.</p>
				"""
				.replaceAll("URI", expectedURI);

		IType type= cu.getType("TestClass");
		String actualHtmlContent= getHoverHtmlContent(cu, type);
		assertHtmlContent(expectedHtmlContent, actualHtmlContent);
	}

	@Test
	public void testSpec01Links() throws CoreException {
		String source= """
				package p;
				public class Spec01Links {

					/// - a module [java.base/]
					/// - a package [java.util]
					/// - a class [String]
					/// - a field [String#CASE_INSENSITIVE_ORDER]
					/// - a method [String#chars()]
					public void plainLinks() {

					}

					/// - [the `java.base` module][java.base/]
					/// - [the `java.util` package][java.util]
					/// - [a class][String]
					/// - [a field][String#CASE_INSENSITIVE_ORDER]
					/// - [a method][String#chars()]
					public void linksDisplayString() {

					}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/Spec01Links.java", source, null);
		assertNotNull("Spec01Links.java", cu);
		IType type= cu.getType("Spec01Links");

		String plainLinksURI= makeEncodedMethodUri(true, "p", "Spec01Links", "Spec01Links", "plainLinks");
		String moduleURI= makeEncodedRelativeUri(plainLinksURI, "java.base/");
		String packageURI= makeEncodedRelativeUri(plainLinksURI, "java.util");
		String classURI= makeEncodedRelativeUri(plainLinksURI, "String");
		String fieldURI= makeEncodedRelativeUri(plainLinksURI, "String", "CASE_INSENSITIVE_ORDER");
		String methodURI= makeEncodedRelativeUri(plainLinksURI, "String", "chars", "");
		String expectedHtmlContent= """
				<ul>
				<li>a module <code><a href='MODULE_URI'>java.base/</a></code></li>
				<li>a package <code><a href='PACKAGE_URI'>java.util</a></code></li>
				<li>a class <code><a href='CLASS_URI'>String</a></code></li>
				<li>a field <code><a href='FIELD_URI'>String.CASE_INSENSITIVE_ORDER</a></code></li>
				<li>a method <code><a href='METHOD_URI'>String.chars()</a></code></li>
				</ul>
				"""
				.replace("MODULE_URI", moduleURI)
				.replace("PACKAGE_URI", packageURI)
				.replace("CLASS_URI", classURI)
				.replace("FIELD_URI", fieldURI)
				.replace("METHOD_URI", methodURI);

		IMethod method= type.getMethod("plainLinks", new String[0]);
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent(expectedHtmlContent, actualHtmlContent);

		expectedHtmlContent= """
				<ul>
				<li><code><a href='MODULE_URI'>the <code>java.base</code> module</a></code></li>
				<li><code><a href='PACKAGE_URI'>the <code>java.util</code> package</a></code></li>
				<li><code><a href='CLASS_URI'>a class</a></code></li>
				<li><code><a href='FIELD_URI'>a field</a></code></li>
				<li><code><a href='METHOD_URI'>a method</a></code></li>
				</ul>
				"""
				.replace("MODULE_URI", moduleURI)
				.replace("PACKAGE_URI", packageURI)
				.replace("CLASS_URI", classURI)
				.replace("FIELD_URI", fieldURI)
				.replace("METHOD_URI", methodURI)
				.replaceAll("plainLinks", "linksDisplayString"); // rebase all links to be relative to linksDisplayString
		method= type.getMethod("linksDisplayString", new String[0]);
		actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent(expectedHtmlContent, actualHtmlContent);
	}

	@Test
	public void testSpec02Table() throws CoreException {
		String source= """
				package p;

				/// | Latin | Greek |
				/// |-------|-------|
				/// | a     | alpha |
				/// | b     | beta  |
				/// | c     | gamma |
				public class Spec02Table {
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/Spec02Table.java", source, null);
		assertNotNull("Spec02Table.java", cu);

		IType type= cu.getType("Spec02Table");
		String actualHtmlContent= getHoverHtmlContent(cu, type);
		assertHtmlContent("""
				<table>
				<thead>
				<tr>
				<th>Latin</th>
				<th>Greek</th>
				</tr>
				</thead>
				<tbody>
				<tr>
				<td>a</td>
				<td>alpha</td>
				</tr>
				<tr>
				<td>b</td>
				<td>beta</td>
				</tr>
				<tr>
				<td>c</td>
				<td>gamma</td>
				</tr>
				</tbody>
				</table>
				""",
				actualHtmlContent);
	}

	@Test
	public void testSpec03Tags() throws CoreException {
		String source= """
				package p;
				class Super {
					/// super doc
					public void m(int i) {}
				}
				public class Spec03Tags extends Super {

					/// {@inheritDoc}
					/// In addition, this methods calls [#wait()].
					///
					/// @param i the index
					public void m(int i) {}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/Spec03Tags.java", source, null);
		assertNotNull("Spec03Tags.java", cu);

		String mURI= makeEncodedMethodUri(true, "p", "Spec03Tags", "Spec03Tags","m", "I");
		String waitURI= makeEncodedRelativeUri(mURI, "", "wait", "");
		String superURI= makeEncodedClassUri("p", "Spec03Tags", "Super");
		String superMURI= makeEncodedMethodUri(true, "p", "Spec03Tags", "Super","m", "I");
		String expectedContent= """
				<p>super doc
				In addition, this methods calls <code><a href='METHOD_URI'>wait()</a></code>.<div><b>Overrides:</b> <a href='SUPER_M_URI'>m(...)</a> in <a href='SUPER_URI'>Super</a></div></p>
				<dl><dt>Parameters:</dt><dd><b>i</b> the index</dd></dl>
				"""
				.replace("METHOD_URI", waitURI)
				.replace("SUPER_M_URI", superMURI)
				.replace("SUPER_URI", superURI);

		IType type= cu.getType("Spec03Tags");
		IMethod method= type.getMethod("m", new String[] { "I" });
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent(expectedContent, actualHtmlContent);
	}

	@Test
	public void testSpec04Code() throws CoreException {
		String source= """
				/// The following code span contains literal text, and not a JavaDoc tag:
				/// `{@inheritDoc}`
				///
				/// In the following indented code block, `@Override` is an annotation,
				/// and not a JavaDoc tag:
				///
				///     @Override
				///     public void m() ...
				///
				/// Likewise, in the following fenced code block, `@Override` is an annotation,
				/// and not a JavaDoc tag:
				///
				/// ```
				/// @Override
				/// public void m() ...
				/// ```
				public class Spec04Code {
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/Spec04Code.java", source, null);
		assertNotNull("Spec04Code.java", cu);

		String expectedContent= """
				<p>The following code span contains literal text, and not a JavaDoc tag:
				<code>{@inheritDoc}</code></p>
				<p>In the following indented code block, <code>@Override</code> is an annotation,
				and not a JavaDoc tag:</p>
				<pre><code>@Override
				public void m() ...
				</code></pre>
				<p>Likewise, in the following fenced code block, <code>@Override</code> is an annotation,
				and not a JavaDoc tag:</p>
				<pre><code>@Override
				public void m() ...
				</code></pre>
				""";
		IType type= cu.getType("Spec04Code");
		String actualHtmlContent= getHoverHtmlContent(cu, type);
		assertHtmlContent(expectedContent, actualHtmlContent);
	}

	@Test
	public void testSpec05TextInTag() throws CoreException {
		String source= """
				import java.util.List;

				public class Spec05TextInTag {
					/// @param l   the list, or `null` if no list is available
					public void m(List<String> l) {}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/Spec05TextInTag.java", source, null);
		assertNotNull("Spec05TextInTag.java", cu);

		IType type= cu.getType("Spec05TextInTag");
		IMethod method= type.getMethods()[0];
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent("""
				<dl><dt>Parameters:</dt><dd><b>l</b> the list, or <code>null</code> if no list is available</dd></dl>
				""",
				actualHtmlContent);
	}

	@Test
	public void testCodeAtEdge() throws CoreException {
		String source= """
				package p;

				public class CodeAtEdge {
				    ///     @Override
				    ///     void m() {}
				    ///
				    /// Plain text
				    ///
				    ///     @Override
				    ///     void m() {}
				    public void m() {}
				}

				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/CodeAtEdge.java", source, null);
		assertNotNull("CodeAtEdge.java", cu);

		IType type= cu.getType("CodeAtEdge");

		IMethod method= type.getMethods()[0];
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent("""
				<pre><code>@Override
				void m() {}
				</code></pre>
				<p>Plain text</p>
				<pre><code>@Override
				void m() {}
				</code></pre>
				""",
				actualHtmlContent);
	}
	@Test
	public void testLineStarts() throws CoreException {
		String source= """
				package p;

				public class LineStarts {
					/// Three
					//// Four - show one slash
					///// Five - show two slashes
					/// Drei
					void numberOfSlashes() { }

					///  two
					///
					///none - all leadings spaces will be significant
					///
					/// public void one()
					///
					///    public void four() // four spaces suffice for code
					///
					void numberOfSpaces1() { }

					///  two
					///
					/// public void one()
					///
					///  public void two()
					///
					///    public void four()
					///
					///     public void five() // shown correctly by both
					///
					/// public void one()
					///
					///    public void four()
					///
					///     public void five() // dropped by javadoc
					void numberOfSpaces2() { }
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/LineStarts.java", source, null);
		assertNotNull("LineStarts.java", cu);

		IType type= cu.getType("LineStarts");

		IMethod method= type.getMethods()[0];
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent("""
				<p>Three
				/ Four - show one slash
				// Five - show two slashes
				Drei</p>
				""",
				actualHtmlContent);

		method= type.getMethods()[1];
		actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent("""
				<p>two</p>
				<p>none - all leadings spaces will be significant</p>
				<p>public void one()</p>
				<pre><code>public void four() // four spaces suffice for code
				</code></pre>
				""",
				actualHtmlContent);

		method= type.getMethods()[2];
		actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent("""
				<p>two</p>
				<p>public void one()</p>
				<p>public void two()</p>
				<p>public void four()</p>
				<pre><code>public void five() // shown correctly by both
				</code></pre>
				<p>public void one()</p>
				<p>public void four()</p>
				<pre><code>public void five() // dropped by javadoc
				</code></pre>
				""",
				actualHtmlContent);
	}


	@Test
	public void testSeeTag() throws CoreException {
		String source= """
				package p;

				/// @see #m()
				/// @see <a href="https://www.eclipse.org">Eclipse.org</a>
				public class SeeTag {
					public void m() {}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/SeeTag.java", source, null);
		assertNotNull("SeeTag.java", cu);

		IType type= cu.getType("SeeTag");

		String actualHtmlContent= getHoverHtmlContent(cu, type);
		String expectedContent= """
				<dl><dt>See Also:</dt><dd><a href='METHOD_URI'>m()</a></dd><dd><a href="https://www.eclipse.org">Eclipse.org</a></dd></dl>
				"""
				.replace("METHOD_URI", makeEncodedMethodUri("p", "SeeTag", "SeeTag","m", ""));
		assertHtmlContent(expectedContent, actualHtmlContent);
	}

	@Test
	public void testGH2808_codeAfterPara() throws CoreException {
		String source= """
				package p;

				public class CodeAfterPara {
					/// Plain Text
					///     @Override public void four() // four significant spaces but no blank line
					void noBlankLine() { }

					/// Plain Text
					///  \s
					///     @Override public void four() // four significant spaces after blank line
					void withBlankLine() { }
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/CodeAfterPara.java", source, null);
		assertNotNull("CodeAfterPara.java", cu);

		IType type= cu.getType("CodeAfterPara");

		IMethod method= type.getMethods()[0];
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		String expectedContent= """
				<p>Plain Text</p>
				<dl><dt>@Override</dt><dd>public void four() // four significant spaces but no blank line</dd></dl>
				""";
		assertHtmlContent(expectedContent, actualHtmlContent);

		method= type.getMethods()[1];
		actualHtmlContent= getHoverHtmlContent(cu, method);
		expectedContent= """
				<p>Plain Text</p>
				<pre><code>@Override public void four() // four significant spaces after blank line
				</code></pre>
				""";
		assertHtmlContent(expectedContent, actualHtmlContent);
	}

	@Test
	public void testGH2808_terminatingAnIndentedCodeBlock() throws CoreException {
		String source= """
				package p;

				public class BlockEnding {
					/// Plain Text
					///
					///     @Override public void four()
					///     ```
					///     /// doc
					///     /// ```
					///     /// @Override Nested Code
					///     /// ```
					///     ```
					void indentedWithFence() { }

					/// Plain Text
					///
					///     @Override public void four()
					/// Plain again
					void paraAfterCode() { }
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/BlockEnding.java", source, null);
		assertNotNull("BlockEnding.java", cu);

		IType type= cu.getType("BlockEnding");

		IMethod method= type.getMethods()[0];
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		String expectedContent= """
				<p>Plain Text</p>
				<pre><code>@Override public void four()
				```
				/// doc
				/// ```
				/// @Override Nested Code
				/// ```
				```
				</code></pre>
				""";
		assertHtmlContent(expectedContent, actualHtmlContent);

		method= type.getMethods()[1];
		actualHtmlContent= getHoverHtmlContent(cu, method);
		expectedContent= """
				<p>Plain Text</p>
				<pre><code>@Override public void four()
				</code></pre>
				<p>Plain again</p>
				""";
		assertHtmlContent(expectedContent, actualHtmlContent);
	}
	@Test
	public void testGH2980() throws CoreException {
		String source= """
				package p;

				public class X {
					/// This is a test Javadoc for method test1()
					/// ```java
					/// test1(42);
					/// ```
					/// @param a some parameter for test1
					public void test1(int a) {}
					///
					/// This is a test Javadoc for method test2()
					/// 'test2(0)'
					/// @param b some parameter for test1
					public void test2(int b) {}
					/// This is a test Javadoc for method test3()
					/// ```java
					/// int r = test3();
					/// System.out.println(r);
					/// ```
					/// @return an int value
					public int test3() {
						return 0;
					}
					/// This is a test Javadoc for method test4()
					/// Invocation method 1:
					/// ```java
					/// int r = test4();
					/// System.out.println(r);
					/// ```
					/// Invocation method 2:
					/// ```java
					/// System.out.println(test4());
					/// ```
					/// @return an int value
					/// @param i an int param
					public int test4(int i) {
						return 0;
					}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/X.java", source, null);
		assertNotNull("X.java", cu);

		IType type= cu.getType("X");

		IMethod method= type.getMethods()[0];
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		String expectedContent= """
					<p>This is a test Javadoc for method test1()</p>
					<pre><code class="language-java">test1(42);
					</code></pre>
					<dl><dt>Parameters:</dt><dd><b>a</b> some parameter for test1</dd></dl>
					""";
		assertHtmlContent(expectedContent, actualHtmlContent);

		method= type.getMethods()[1];
		actualHtmlContent= getHoverHtmlContent(cu, method);
		expectedContent= """
					<p>This is a test Javadoc for method test2()
					'test2(0)'</p>
					<dl><dt>Parameters:</dt><dd><b>b</b> some parameter for test1</dd></dl>
					""";
		assertHtmlContent(expectedContent, actualHtmlContent);

		method= type.getMethods()[2];
		actualHtmlContent= getHoverHtmlContent(cu, method);
		expectedContent= """
					<p>This is a test Javadoc for method test3()</p>
					<pre><code class="language-java">int r = test3();
					System.out.println(r);
					</code></pre>
					<dl><dt>Returns:</dt><dd>an int value</dd></dl>
					""";
		method= type.getMethods()[3];
		actualHtmlContent= getHoverHtmlContent(cu, method);
		expectedContent= """
					<p>This is a test Javadoc for method test4()
					Invocation method 1:</p>
					<pre><code class="language-java">int r = test4();
					System.out.println(r);
					</code></pre>
					<p>Invocation method 2:</p>
					<pre><code class="language-java">System.out.println(test4());
					</code></pre>
					<dl><dt>Parameters:</dt><dd><b>i</b> an int param</dd><dt>Returns:</dt><dd>an int value</dd></dl>
					""";
		assertHtmlContent(expectedContent, actualHtmlContent);
	}
	@Test
	public void testFenceLenFour_1() throws CoreException {
		String source= """
				/// ````
				/// ```
				/// @param is not a tag here because this is nested literal *markdown*
				/// ```
				/// ````
				public class FenceLenFour {
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/FenceLenFour.java", source, null);
		assertNotNull("FenceLenFour.java", cu);

		String expectedContent= """
				<pre><code>```
				@param is not a tag here because this is nested literal *markdown*
				```
				</code></pre>
				""";
		IType type= cu.getType("FenceLenFour");
		String actualHtmlContent= getHoverHtmlContent(cu, type);
		assertHtmlContent(expectedContent, actualHtmlContent);
	}
	@Test
	public void testFenceLenFour_2() throws CoreException {
		String source= """
				public class FenceLenFour {
					/// `````
					/// ````
					/// ```
					/// @param is not a tag here because this is nested literal *markdown*
					/// ```
					/// ````
					/// `````
					/// @return an int value
					/// @param i real param
					public int foo(int i) {
						return 0;
					}
				}
				""";
		ICompilationUnit cu= getWorkingCopy("/TestSetupProject/src/p/FenceLenFour.java", source, null);
		assertNotNull("FenceLenFour.java", cu);

		String expectedContent= """
				<pre><code>````
				```
				@param is not a tag here because this is nested literal *markdown*
				```
				````
				</code></pre>
				<dl><dt>Parameters:</dt><dd><b>i</b> real param</dd><dt>Returns:</dt><dd>an int value</dd></dl>
				""";
		IType type= cu.getType("FenceLenFour");
		IMethod method= type.getMethods()[0];
		String actualHtmlContent= getHoverHtmlContent(cu, method);
		assertHtmlContent(expectedContent, actualHtmlContent);
	}
}
