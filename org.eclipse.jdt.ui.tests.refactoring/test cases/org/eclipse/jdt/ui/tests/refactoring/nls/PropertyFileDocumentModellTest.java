/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.DocumentChange;

import org.eclipse.jdt.internal.corext.refactoring.nls.KeyValuePair;
import org.eclipse.jdt.internal.corext.refactoring.nls.PropertyFileDocumentModel;

import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

public class PropertyFileDocumentModellTest extends TestCase {

	public PropertyFileDocumentModellTest(String name) {
		super(name);
	}

	public static TestSuite suite() {
		return new TestSuite(PropertyFileDocumentModellTest.class);
	}

	private static void insert(IDocument document, String key, String value) throws CoreException {
		insert(document, new KeyValuePair[] {new KeyValuePair(key, value)});
	}

	private static void insert(IDocument document, KeyValuePair[] pairs) throws CoreException {
		PropertyFileDocumentModel model= new PropertyFileDocumentModel(document);

		for (int i= 0; i < pairs.length; i++) {
			KeyValuePair pair= pairs[i];
			pair.setValue(PropertyFileDocumentModel.unwindValue(pair.getValue()) + model.getLineDelimiter());
			pair.setKey(PropertyFileDocumentModel.unwindEscapeChars(pair.getKey()));
		}

		DocumentChange change= new DocumentChange("", document);
		model.insert(pairs, change);
		change.perform(new NullProgressMonitor());
	}

	public void testInsertIntoEmptyDoc() throws Exception {
		Document props= new Document();

		insert(props, "key", "value");

		RefactoringTest.assertEqualLines(
				"key=value\n", props.get());
	}

	public void testInsertIntoDoc() throws Exception {
		Document props= new Document(
				"org.eclipse.nls.1=value\n" +
				"org.eclipse=value\n");

		insert(props, "org.eclipse.nls.2", "value");

		assertEquals(
				"org.eclipse.nls.1=value\n" +
				"org.eclipse.nls.2=value\n" +
				"org.eclipse=value\n", props.get());
	}

	public void testInsertIntoDoc2() throws Exception {
		Document props= new Document(
				"org.1=value\n" +
				"org.2=value\n");

		insert(props, "arg.1", "value");

		assertEquals(
				"arg.1=value\n" +
				"org.1=value\n" +
				"org.2=value\n", props.get());
	}

	public void testInsertIntoDoc3() throws Exception {
		Document props= new Document(
				"Test_B_1=value\n" +
				"Test_A_1=value\n");

		insert(props, "Test_B_2", "value");

		assertEquals(
				"Test_B_1=value\n" +
				"Test_B_2=value\n" +
				"Test_A_1=value\n", props.get());
	}

	public void testInsertIntoDoc4() throws Exception {
		Document props= new Document(
				"Test_Aa=value\n" +
				"Test_Ab=value\n" +
				"\n" +
				"Test_Bb=\n" +
				"Test_Bc=");

		insert(props, new KeyValuePair[] {new KeyValuePair("Test_Ba", ""), new KeyValuePair("Test_Az", "")});

		assertEquals("Test_Aa=value\n" +
				"Test_Ab=value\n" +
				"Test_Az=\n" +
				"\n" +
				"Test_Ba=\n" +
				"Test_Bb=\n" +
				"Test_Bc=", props.get());
	}

	public void testManyInsertsIntoDoc() throws Exception {
		Document props= new Document(
				"org.eclipse.nls.1=value\n" +
				"\n" +
				"org.eclipse.2=value\n");

		insert(props, new KeyValuePair[] {
				new KeyValuePair("org.eclipse.nls.2", "value"),
				new KeyValuePair("org.eclipse.nls", "value"),
				new KeyValuePair("org.apache", "value"),
				new KeyValuePair("org.xenon", "value"),
				new KeyValuePair("org.eclipse", "value"),
				new KeyValuePair("org.eclipse.xyzblabla.pipapo", "value")});

		assertEquals(
				"org.apache=value\n" +
				"org.eclipse.nls=value\n" +
				"org.eclipse.nls.1=value\n" +
				"org.eclipse.nls.2=value\n" +
				"\n" +
				"org.eclipse=value\n" +
				"org.eclipse.2=value\n" +
				"org.eclipse.xyzblabla.pipapo=value\n" +
				"org.xenon=value\n",
				props.get());
	}

	public void testManyInsertsIntoDoc2() throws Exception {
		Document props= new Document(
				"key_b=value\n" +
				"\n" +
				"key_y=value\n");

		insert(props, new KeyValuePair[] {new KeyValuePair("key_c", "value"), new KeyValuePair("key_a", "value"), new KeyValuePair("key_z", "value")});

		assertEquals(
				"key_a=value\n" +
				"key_b=value\n" +
				"key_c=value\n" +
				"\n" +
				"key_y=value\n" +
				"key_z=value\n", props.get());
	}

	public void testManyInsertsIntoDoc3() throws Exception {
		Document props= new Document(
				"key_a=value\n" +
				"\n" +
				"key_b_2=value\n");

		insert(props, new KeyValuePair[] {new KeyValuePair("key_b_1", "value"), new KeyValuePair("key_b_0", "value")});

		assertEquals(
				"key_a=value\n" +
				"\n" +
				"key_b_0=value\n" +
				"key_b_1=value\n" +
				"key_b_2=value\n", props.get());
	}

	public void testManyInsertsIntoDoc4() throws Exception {
		Document props= new Document(
				"Clazz.Pong=Pong\n" +
				"Clazz.Ping=Ping\n");

		insert(props, new KeyValuePair[] {
				new KeyValuePair("Clazz.Pizza", "value"),
				new KeyValuePair("Clazz.Posers", "value")});

		assertEquals(
				"Clazz.Pong=Pong\n" +
				"Clazz.Posers=value\n" +
				"Clazz.Ping=Ping\n" +
				"Clazz.Pizza=value\n", props.get());
	}

	public void testManyInsertsIntoDoc5() throws Exception {
		Document props= new Document(
				"Clazz.Pong=Pong\n" +
				"Clazz.Ping=Ping\n");

		insert(props, new KeyValuePair[] {
				new KeyValuePair("Clazz.P", "p"),
				new KeyValuePair("Clazz.PosersWithAVeryLongName", "p")});

		assertEquals(
				"Clazz.P=p\n" +
				"Clazz.Pong=Pong\n" +
				"Clazz.PosersWithAVeryLongName=p\n" +
				"Clazz.Ping=Ping\n", props.get());
	}

	public void testBlockInsertsIntoDoc() throws Exception {
		Document props= new Document(
				"org.eclipse.1=value\n" +
				"org.eclipse.2=value\n");

		insert(props, new KeyValuePair[] {new KeyValuePair("org.eclipse.nls.1", "value"), new KeyValuePair("org.eclipse.nls.2", "value")});

		assertEquals(
				"org.eclipse.1=value\n" +
				"org.eclipse.2=value\n" +
				"org.eclipse.nls.1=value\n" +
				"org.eclipse.nls.2=value\n", props.get());
	}

	public void testInsertIntoDocWithBlankLines1() throws Exception {
		Document props= new Document(
				"org.eclipse=value\n" +
				"\n" +
				"org.eclipse.test=value\n");

		insert(props, "org.eclipse.test", "value2");

		assertEquals(
				"org.eclipse=value\n" +
				"\n" +
				"org.eclipse.test=value\n" +
				"org.eclipse.test=value2\n", props.get());
	}

	public void testInsertIntoDocWithBlankLines2() throws Exception {
		Document props= new Document(
				"a.b=v\n" +
				"\n" +
				"org.eclipse.test=value\n");

		insert(props, "a.c", "v");

		assertEquals(
				"a.b=v\n" +
				"a.c=v\n" +
				"\n" +
				"org.eclipse.test=value\n", props.get());
	}

	public void testInsertIntoDocWithDifferentSeperationChar() throws Exception {
		Document props= new Document(
				"org.eclipse.ok:value\n" +
				"org.eclipse.what value\n");

		insert(props, "org.eclipse.nix", "value");

		assertEquals(
				"org.eclipse.nix=value\n" +
				"org.eclipse.ok:value\n" +
				"org.eclipse.what value\n", props.get());
	}

	public void testRemovingOfKey() throws Exception {
		Document props= new Document("org.eclipse.1=value1\n" + "org.eclipse.2=value2\n" + "org.eclipse.3=value3\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		DeleteEdit deleteEdit= modell.remove("org.eclipse.2");
		deleteEdit.apply(props);

		assertEquals("org.eclipse.1=value1\n" + "org.eclipse.3=value3\n", props.get());
	}

	public void testRemovingOfLastKey() throws Exception {
		Document props= new Document("org.eclipse.1=value1\n" + "org.eclipse.2=value2\n" + "org.eclipse.3=value3\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		DeleteEdit deleteEdit= modell.remove("org.eclipse.3");
		deleteEdit.apply(props);

		assertEquals("org.eclipse.1=value1\n" + "org.eclipse.2=value2\n", props.get());
	}

	public void testReplacementOfKeyValuePair() throws Exception {
		Document props= new Document("org.eclipse.1=value1\n" + "org.eclipse.2=value2\n" + "org.eclipse.3=value3\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		ReplaceEdit replaceEdit= modell.replace(new KeyValuePair("org.eclipse.2", "value\n"), new KeyValuePair("org.1", "value\n"));
		replaceEdit.apply(props);

		assertEquals("org.eclipse.1=value1\n" + "org.1=value\n" + "org.eclipse.3=value3\n", props.get());
	}

	// Escaping stuff
	public void testEscapingOfComments() throws Exception {
		Document props= new Document();

		insert(props, "key", "value!please escape");

		RefactoringTest.assertEqualLines(
				"key=value\\!please escape\n", props.get());
	}

	public void testEscapingOfLineBreaks() throws Exception {
		Document props= new Document();

		insert(props, "key", "value1\nvalue2\r");

		RefactoringTest.assertEqualLines(
				"key=value1\\nvalue2\\r\n", props.get());
	}

	public void testEscapingOfUniCode() throws Exception {
		Document props= new Document();

		insert(props, "key", "\u00ea");

		RefactoringTest.assertEqualLines("key=\\u00EA\n", props.get());
	}

	public void testEscapingOfLeadingWhiteSpaces() throws Exception {
		Document props= new Document();

		insert(props, "key", "  test");

		RefactoringTest.assertEqualLines("key=\\ \\ test\n", props.get());
	}
}
