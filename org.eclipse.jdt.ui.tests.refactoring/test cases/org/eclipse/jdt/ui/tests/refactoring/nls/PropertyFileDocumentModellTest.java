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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.DocumentChange;

import org.eclipse.jdt.internal.corext.refactoring.nls.KeyValuePair;
import org.eclipse.jdt.internal.corext.refactoring.nls.PropertyFileDocumentModel;

import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;

public class PropertyFileDocumentModellTest {
	private static void insert(IDocument document, String key, String value) throws CoreException {
		insert(document, new KeyValuePair[] {new KeyValuePair(key, value)});
	}

	private static void insert(IDocument document, KeyValuePair[] pairs) throws CoreException {
		PropertyFileDocumentModel model= new PropertyFileDocumentModel(document);

		for (KeyValuePair pair : pairs) {
			pair.setValue(PropertyFileDocumentModel.escape(pair.getValue(), true) + model.getLineDelimiter());
			pair.setKey(PropertyFileDocumentModel.escape(pair.getKey(), false));
		}

		DocumentChange change= new DocumentChange("", document);
		model.insert(pairs, change);
		change.perform(new NullProgressMonitor());
	}

	@Test
	public void insertIntoEmptyDoc() throws Exception {
		Document props= new Document();

		insert(props, "key", "value");

		GenericRefactoringTest.assertEqualLines(
				"key=value\n", props.get());
	}

	@Test
	public void insertIntoDoc() throws Exception {
		Document props= new Document(
				"org.eclipse.nls.1=value\n" +
				"org.eclipse=value\n");

		insert(props, "org.eclipse.nls.2", "value");

		assertEquals(
				"""
					org.eclipse.nls.1=value
					org.eclipse.nls.2=value
					org.eclipse=value
					""", props.get());
	}

	@Test
	public void insertIntoDoc2() throws Exception {
		Document props= new Document(
				"org.1=value\n" +
				"org.2=value\n");

		insert(props, "arg.1", "value");

		assertEquals(
				"""
					arg.1=value
					org.1=value
					org.2=value
					""", props.get());
	}

	@Test
	public void insertIntoDoc3() throws Exception {
		Document props= new Document(
				"Test_B_1=value\n" +
				"Test_A_1=value\n");

		insert(props, "Test_B_2", "value");

		assertEquals(
				"""
					Test_B_1=value
					Test_B_2=value
					Test_A_1=value
					""", props.get());
	}

	@Test
	public void insertIntoDoc4() throws Exception {
		Document props= new Document(
				"""
					Test_Aa=value
					Test_Ab=value
					
					Test_Bb=
					Test_Bc=""");

		insert(props, new KeyValuePair[] {new KeyValuePair("Test_Ba", ""), new KeyValuePair("Test_Az", "")});

		assertEquals("""
			Test_Aa=value
			Test_Ab=value
			Test_Az=
			
			Test_Ba=
			Test_Bb=
			Test_Bc=""", props.get());
	}

	@Test
	public void manyInsertsIntoDoc() throws Exception {
		Document props= new Document(
				"""
					org.eclipse.nls.1=value
					
					org.eclipse.2=value
					""");

		insert(props, new KeyValuePair[] {
				new KeyValuePair("org.eclipse.nls.2", "value"),
				new KeyValuePair("org.eclipse.nls", "value"),
				new KeyValuePair("org.apache", "value"),
				new KeyValuePair("org.xenon", "value"),
				new KeyValuePair("org.eclipse", "value"),
				new KeyValuePair("org.eclipse.xyzblabla.pipapo", "value")});

		assertEquals(
				"""
					org.apache=value
					org.eclipse.nls=value
					org.eclipse.nls.1=value
					org.eclipse.nls.2=value
					
					org.eclipse=value
					org.eclipse.2=value
					org.eclipse.xyzblabla.pipapo=value
					org.xenon=value
					""",
				props.get());
	}

	@Test
	public void manyInsertsIntoDoc2() throws Exception {
		Document props= new Document(
				"""
					key_b=value
					
					key_y=value
					""");

		insert(props, new KeyValuePair[] {new KeyValuePair("key_c", "value"), new KeyValuePair("key_a", "value"), new KeyValuePair("key_z", "value")});

		assertEquals(
				"""
					key_a=value
					key_b=value
					key_c=value
					
					key_y=value
					key_z=value
					""", props.get());
	}

	@Test
	public void manyInsertsIntoDoc3() throws Exception {
		Document props= new Document(
				"""
					key_a=value
					
					key_b_2=value
					""");

		insert(props, new KeyValuePair[] {new KeyValuePair("key_b_1", "value"), new KeyValuePair("key_b_0", "value")});

		assertEquals(
				"""
					key_a=value
					
					key_b_0=value
					key_b_1=value
					key_b_2=value
					""", props.get());
	}

	@Test
	public void manyInsertsIntoDoc4() throws Exception {
		Document props= new Document(
				"Clazz.Pong=Pong\n" +
				"Clazz.Ping=Ping\n");

		insert(props, new KeyValuePair[] {
				new KeyValuePair("Clazz.Pizza", "value"),
				new KeyValuePair("Clazz.Posers", "value")});

		assertEquals(
				"""
					Clazz.Pong=Pong
					Clazz.Posers=value
					Clazz.Ping=Ping
					Clazz.Pizza=value
					""", props.get());
	}

	@Test
	public void manyInsertsIntoDoc5() throws Exception {
		Document props= new Document(
				"Clazz.Pong=Pong\n" +
				"Clazz.Ping=Ping\n");

		insert(props, new KeyValuePair[] {
				new KeyValuePair("Clazz.P", "p"),
				new KeyValuePair("Clazz.PosersWithAVeryLongName", "p")});

		assertEquals(
				"""
					Clazz.P=p
					Clazz.Pong=Pong
					Clazz.PosersWithAVeryLongName=p
					Clazz.Ping=Ping
					""", props.get());
	}

	@Test
	public void blockInsertsIntoDoc() throws Exception {
		Document props= new Document(
				"org.eclipse.1=value\n" +
				"org.eclipse.2=value\n");

		insert(props, new KeyValuePair[] {new KeyValuePair("org.eclipse.nls.1", "value"), new KeyValuePair("org.eclipse.nls.2", "value")});

		assertEquals(
				"""
					org.eclipse.1=value
					org.eclipse.2=value
					org.eclipse.nls.1=value
					org.eclipse.nls.2=value
					""", props.get());
	}

	@Test
	public void insertIntoDocWithBlankLines1() throws Exception {
		Document props= new Document(
				"""
					org.eclipse=value
					
					org.eclipse.test=value
					""");

		insert(props, "org.eclipse.test", "value2");

		assertEquals(
				"""
					org.eclipse=value
					
					org.eclipse.test=value
					org.eclipse.test=value2
					""", props.get());
	}

	@Test
	public void insertIntoDocWithBlankLines2() throws Exception {
		Document props= new Document(
				"""
					a.b=v
					
					org.eclipse.test=value
					""");

		insert(props, "a.c", "v");

		assertEquals(
				"""
					a.b=v
					a.c=v
					
					org.eclipse.test=value
					""", props.get());
	}

	@Test
	public void insertIntoDocWithDifferentSeperationChar() throws Exception {
		Document props= new Document(
				"org.eclipse.ok:value\n" +
				"org.eclipse.what value\n");

		insert(props, "org.eclipse.nix", "value");

		assertEquals(
				"""
					org.eclipse.nix=value
					org.eclipse.ok:value
					org.eclipse.what value
					""", props.get());
	}

	@Test
	public void removingOfKey() throws Exception {
		Document props= new Document("""
			org.eclipse.1=value1
			org.eclipse.2=value2
			org.eclipse.3=value3
			""");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		DeleteEdit deleteEdit= modell.remove("org.eclipse.2");
		deleteEdit.apply(props);

		assertEquals("org.eclipse.1=value1\n" + "org.eclipse.3=value3\n", props.get());
	}

	@Test
	public void removingOfKey2() throws Exception {
		Document props= new Document("""
			org.eclipse.1=value1
			 org.eclipse.2           =  value2  \s
			org.eclipse.3=value3
			""");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		DeleteEdit deleteEdit= modell.remove("org.eclipse.2");
		deleteEdit.apply(props);

		assertEquals("org.eclipse.1=value1\n" + "org.eclipse.3=value3\n", props.get());
	}

	@Test
	public void removingOfLastKey() throws Exception {
		Document props= new Document("""
			org.eclipse.1=value1
			org.eclipse.2=value2
			org.eclipse.3=value3
			""");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		DeleteEdit deleteEdit= modell.remove("org.eclipse.3");
		deleteEdit.apply(props);

		assertEquals("org.eclipse.1=value1\n" + "org.eclipse.2=value2\n", props.get());
	}

	@Test
	public void replacementOfKeyValuePair() throws Exception {
		Document props= new Document("""
			org.eclipse.1=value1
			org.eclipse.2=value2
			org.eclipse.3=value3
			""");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		ReplaceEdit replaceEdit= modell.replace(new KeyValuePair("org.eclipse.2", "value\n"), new KeyValuePair("org.1", "value\n"));
		replaceEdit.apply(props);

		assertEquals("""
			org.eclipse.1=value1
			org.1=value
			org.eclipse.3=value3
			""", props.get());
	}

	// Escaping stuff
	@Test
	public void escapingOfComments() throws Exception {
		Document props= new Document();

		insert(props, "key", "value!please escape");

		GenericRefactoringTest.assertEqualLines(
				"key=value\\!please escape\n", props.get());
	}

	@Test
	public void escapingOfLineBreaks() throws Exception {
		Document props= new Document();

		insert(props, "key", "value1\nvalue2\r");

		GenericRefactoringTest.assertEqualLines(
				"key=value1\\nvalue2\\r\n", props.get());
	}

	@Test
	public void escapingOfTab() throws Exception {
		Document props= new Document();

		insert(props, "key", "value1\tvalue2");

		GenericRefactoringTest.assertEqualLines(
				"key=value1\\tvalue2\n", props.get());
	}

	@Test
	public void escapingOfFormFeed() throws Exception {
		Document props= new Document();

		insert(props, "key", "value1\fvalue2");

		GenericRefactoringTest.assertEqualLines(
				"key=value1\\fvalue2\n", props.get());
	}

	@Test
	public void escapingOfBackspace() throws Exception {
		Document props= new Document();

		insert(props, "key", "value1\bvalue2");

		GenericRefactoringTest.assertEqualLines(
				"key=value1\\u0008value2\n", props.get());
	}

	@Test
	public void escapingOfEscapes() throws Exception {
		Document props= new Document();

		insert(props, "key", "c:\\demo\\demo.java");

		GenericRefactoringTest.assertEqualLines(
				"key=c:\\\\demo\\\\demo.java\n", props.get());
	}

	@Test
	public void escapingOfISO8859() throws Exception {
		Document props= new Document();

		insert(props, "key", "\u00e4");

		GenericRefactoringTest.assertEqualLines("key=ä\n", props.get());
	}

	@Test
	public void escapingOfUniCode() throws Exception {
		Document props= new Document();

		insert(props, "key", "\u0926");

		GenericRefactoringTest.assertEqualLines("key=\\u0926\n", props.get());
	}

	@Test
	public void escapingOfLeadingWhiteSpaces() throws Exception {
		Document props= new Document();

		insert(props, "key", "  test");

		GenericRefactoringTest.assertEqualLines("key=\\ \\ test\n", props.get());
	}
}
