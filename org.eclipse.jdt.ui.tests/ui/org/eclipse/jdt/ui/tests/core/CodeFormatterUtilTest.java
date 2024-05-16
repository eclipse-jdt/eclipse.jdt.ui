/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Assert;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CodeFormatterUtilTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	@Before
	public void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRequiredProject(fJProject1, pts.getProject());

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	private static String evaluateFormatterEdit(String string, TextEdit edit, Position[] positions) {
		try {
			Document doc= createDocument(string, positions);
			edit.apply(doc, 0);
			if (positions != null) {
				for (Position position : positions) {
					Assert.isTrue(!position.isDeleted, "Position got deleted"); //$NON-NLS-1$
				}
			}
			return doc.get();
		} catch (BadLocationException e) {
			JavaPlugin.log(e); // bug in the formatter
			Assert.isTrue(false, "Formatter created edits with wrong positions: " + e.getMessage()); //$NON-NLS-1$
		}
		return null;
	}

	private static Document createDocument(String string, Position[] positions) throws IllegalArgumentException {
		Document doc= new Document(string);
		try {
			if (positions != null) {
				final String POS_CATEGORY= "myCategory"; //$NON-NLS-1$

				doc.addPositionCategory(POS_CATEGORY);
				doc.addPositionUpdater(new DefaultPositionUpdater(POS_CATEGORY) {
					@Override
					protected boolean notDeleted() {
						if (fOffset < fPosition.offset && (fPosition.offset + fPosition.length < fOffset + fLength)) {
							fPosition.offset= fOffset + fLength; // deleted positions: set to end of remove
							return false;
						}
						return true;
					}
				});
				for (Position position : positions) {
					try {
						doc.addPosition(POS_CATEGORY, position);
					} catch (BadLocationException e) {
						throw new IllegalArgumentException("Position outside of string. offset: " + position.offset + ", length: " + position.length + ", string size: " + string.length()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					}
				}
			}
		} catch (BadPositionCategoryException cannotHappen) {
			// can not happen: category is correctly set up
		}
		return doc;
	}


	@Test
	public void testCU() throws Exception {
		String contents= """
			package test1;
			public class A {
			    public void foo() {
			    Runnable run= new Runnable() {};
			    }
			}
			""";


		String formatted= CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, contents, 0, "\n", fJProject1);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
			        Runnable run = new Runnable() {
			        };
			    }
			}
			""";
		assertEqualString(formatted, expected);
	}

	@Test
	public void testCUIndented() throws Exception {
		String contents= """
			package test1;
			public class A {
			    public void foo() {
			    Runnable run= new Runnable() {};
			    }
			}
			""";


		String formatted= CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, contents, 1, "\n", fJProject1);

		String expected= """
			    package test1;
			    public class A {
			        public void foo() {
			            Runnable run = new Runnable() {
			            };
			        }
			    }
			""";
		assertEqualString(formatted, expected);
	}

	@Test
	public void testCUNewAPI() throws Exception {
		String contents= """
			package test1;
			public class A {
			    public void foo() {
			    Runnable run= new Runnable() {};
			    }
			}
			""";

		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, contents, 0, "\n", null);
		Document doc= new Document(contents);
		edit.apply(doc);
		String formatted= doc.get();


		String expected= """
			package test1;
			public class A {
			    public void foo() {
			        Runnable run = new Runnable() {
			        };
			    }
			}
			""";
		assertEqualString(formatted, expected);
	}

	@Test
	public void testCUNewAPI2() throws Exception {
		String contents= """
			package test1;
			public class A {
			/**
			 * comment
			 */
			    public void foo() {
			    Runnable run= new Runnable() {};
			    }
			}
			""";

		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, contents, 0, "\n", null);
		Document doc= new Document(contents);
		edit.apply(doc);
		String formatted= doc.get();


		String expected= """
			package test1;
			public class A {
			    /**
			     * comment
			     */
			    public void foo() {
			        Runnable run = new Runnable() {
			        };
			    }
			}
			""";
		assertEqualString(formatted, expected);
	}

	@Test
	public void testCUWithPos() throws Exception {
		String contents= """
			package test1;
			public class A {
			    public void foo() {
			    Runnable run= new Runnable() {};
			    }
			}
			""";

		String word1= "new";
		int start1= contents.indexOf(word1);

		Position pos1= new Position(start1, word1.length());

		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, contents, 0, "\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1});

		String expected= """
			package test1;
			public class A {
			    public void foo() {
			        Runnable run = new Runnable() {
			        };
			    }
			}
			""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.offset + pos1.length);
		assertEqualString(curr1, word1);
	}

	@Test
	public void testPackage() throws Exception {
		String contents= """
			  package   com . test1;\
			""";

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		PackageDeclaration decl= ast.newPackageDeclaration();

		TextEdit edit= CodeFormatterUtil.format2(decl, contents, 0, "\n", null);
		assertNotNull(edit);
		Document document= new Document(contents);
		edit.apply(document);

		String str= """
			package com.test1;""";
		assertEqualString(document.get(), str);
	}

	@Test
	public void testPackageWithPos() throws Exception {
		String contents= """
			package   com . test1;""";

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		PackageDeclaration node= ast.newPackageDeclaration();

		String word1= "com";
		int start1= contents.indexOf(word1);

		String word2= ";";
		int start2= contents.indexOf(word2);

		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());

		TextEdit edit= CodeFormatterUtil.format2(node, contents, 0, "\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2});

		String expected= """
			package com.test1;""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.offset + pos1.length);
		assertEqualString(curr1, word1);

		String curr2= formatted.substring(pos2.offset, pos2.offset + pos2.length);
		assertEqualString(curr2, word2);

	}

	@Test
	public void testVarDeclStatemenetWithPos() throws Exception {
		String contents= """
			x[ ]=
			new  int[ offset]""";

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		VariableDeclarationFragment node= ast.newVariableDeclarationFragment();

		String word1= "new";
		int start1= contents.indexOf(word1);

		String word2= "offset";
		int start2= contents.indexOf(word2);

		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());

		TextEdit edit= CodeFormatterUtil.format2(node, contents, 0, "\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2});

		String expected= """
			x[] = new int[offset]""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.offset + pos1.length);
		assertEqualString(curr1, word1);

		String curr2= formatted.substring(pos2.offset, pos2.offset + pos2.length);
		assertEqualString(curr2, word2);

	}

	@Test
	public void testJavadoc() throws Exception {
		String contents= """
			/** bar
			 * foo
			 */
			""";

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		Javadoc node= ast.newJavadoc();

		String word1= "bar";
		int start1= contents.indexOf(word1);

		String word2= "foo";
		int start2= contents.indexOf(word2);

		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());

		TextEdit edit= CodeFormatterUtil.format2(node, contents, 0, "\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2});

		String expected= """
			/** bar
			 * foo
			 */
			""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.offset + pos1.length);
		assertEqualString(curr1, word1);

		String curr2= formatted.substring(pos2.offset, pos2.offset + pos2.length);
		assertEqualString(curr2, word2);

	}

	@Test
	public void testJavadoc2() throws Exception {
		String contents= """
			/** bar
			 * foo
			 */""";

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		Javadoc node= ast.newJavadoc();

		String word1= "bar";
		int start1= contents.indexOf(word1);

		String word2= "foo";
		int start2= contents.indexOf(word2);

		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());

		TextEdit edit= CodeFormatterUtil.format2(node, contents, 1, "\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2});

		String expected= """
			    /** bar
			     * foo
			     */\
			""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.offset + pos1.length);
		assertEqualString(curr1, word1);

		String curr2= formatted.substring(pos2.offset, pos2.offset + pos2.length);
		assertEqualString(curr2, word2);

	}

	@Test
	public void testJavadoc3() throws Exception {
		String contents= """
			/** bar
			 * foo
			 */""";

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		Javadoc node= ast.newJavadoc();

		String word1= "bar";
		int start1= contents.indexOf(word1);

		String word2= "foo";
		int start2= contents.indexOf(word2);


		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());

		TextEdit edit= CodeFormatterUtil.format2(node, contents, 0, "\r\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2});

		String expected= """
			/** bar\r
			 * foo\r
			 */""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.offset + pos1.length);
		assertEqualString(curr1, word1);

		String curr2= formatted.substring(pos2.offset, pos2.offset + pos2.length);
		assertEqualString(curr2, word2);

	}

	@Test
	public void testCatchClause() throws Exception {
		String contents= """
			catch
			(Exception e) {
			}""";

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		CatchClause node= ast.newCatchClause();

		String word1= "catch";
		int start1= contents.indexOf(word1);

		String word2= "Exception";
		int start2= contents.indexOf(word2);

		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());

		TextEdit edit= CodeFormatterUtil.format2(node, contents, 0, "\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2});

		String expected= """
			 catch (Exception e) {
			}""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.offset + pos1.length);
		assertEqualString(curr1, word1);

		String curr2= formatted.substring(pos2.offset, pos2.offset + pos2.length);
		assertEqualString(curr2, word2);

	}

	@Test
	public void testCatchStringLiteral() throws Exception {
		String contents= """
			"Hello" """;

		AST ast= AST.newAST(IASTSharedValues.SHARED_AST_LEVEL, false);
		StringLiteral node= ast.newStringLiteral();

		TextEdit edit= CodeFormatterUtil.format2(node, contents, 0, "\n", null);
		assertNotNull(edit);
		Document document= new Document(contents);
		edit.apply(document);

		String expected= """
			"Hello\"""";
		assertEqualString(document.get(), expected);

	}

	@Test
	public void testFormatSubstring() throws Exception {
		String contents= """
			package test1;
			
			import java.util.Vector;
			
			public class A {
			    public void foo() {
			    Runnable runnable= new Runnable() {};
			    runnable.toString();
			    }
			}
			""";

		String formatString= "Runnable runnable= new Runnable() {};";
		int formatStart= contents.indexOf(formatString);
		int formatLength= formatString.length();

		String word1= "import";
		int start1= contents.indexOf(word1);

		String word2= "new";
		int start2= contents.indexOf(word2);

		String word3= "toString";
		int start3= contents.indexOf(word3);

		Position pos1= new Position(start1, word1.length());
		Position pos2= new Position(start2, word2.length());
		Position pos3= new Position(start3, word3.length());

		TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, contents, formatStart, formatLength, 0, "\n", null);
		assertNotNull(edit);
		String formatted= evaluateFormatterEdit(contents, edit, new Position[] { pos1, pos2, pos3});

		String expected= """
			package test1;
			
			import java.util.Vector;
			
			public class A {
			    public void foo() {
			        Runnable runnable = new Runnable() {
			        };
			    runnable.toString();
			    }
			}
			""";
		assertEqualString(formatted, expected);

		String curr1= formatted.substring(pos1.offset, pos1.getOffset() + pos1.getLength());
		assertEqualString(curr1, word1);

		String curr2= formatted.substring(pos2.offset, pos2.getOffset() + pos2.getLength());
		assertEqualString(curr2, word2);

		String curr3= formatted.substring(pos3.offset, pos3.getOffset() + pos3.getLength());
		assertEqualString(curr3, word3);

	}



}
