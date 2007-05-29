/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.jdt.internal.corext.refactoring.nls.KeyValuePair;
import org.eclipse.jdt.internal.corext.refactoring.nls.PropertyFileDocumentModel;
import org.eclipse.jface.text.Document;

import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public class PropertyFileDocumentModellTest extends TestCase {

	public PropertyFileDocumentModellTest(String name) {
		super(name);
	}

	public static TestSuite suite() {
		return new TestSuite(PropertyFileDocumentModellTest.class);
	}

	public void testInsertIntoEmptyDoc() throws Exception {
		Document props= new Document();

		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		InsertEdit insertEdit= modell.insert("key", "value");
		insertEdit.apply(props);

		RefactoringTest.assertEqualLines("key=value\n", props.get());
	}

	public void testInsertIntoDoc() throws Exception {
		Document props= new Document("org.eclipse.nls.1=value\n" + "org.eclipse=value\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		InsertEdit insertEdit= modell.insert("org.eclipse.nls.2", "value");
		insertEdit.apply(props);

		assertEquals("org.eclipse.nls.1=value\n" + "org.eclipse.nls.2=value\n" + "org.eclipse=value\n", props.get());
	}

	public void testInsertIntoDoc2() throws Exception {
		Document props= new Document("org.1=value\n" + "org.2=value\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		InsertEdit insertEdit= modell.insert("arg.1", "value");
		insertEdit.apply(props);

		assertEquals("org.1=value\n" + "org.2=value\n" + "arg.1=value\n", props.get());
	}

	public void testManyInsertsIntoDoc() throws Exception {
		Document props= new Document("org.eclipse.nls.1=value\n" + "\n" + "org.eclipse=value\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		KeyValuePair[] keyValuePairs= {new KeyValuePair("org.eclipse.nls.2", "value"), new KeyValuePair("org.eclipse.2", "value")};

		InsertEdit[] insertEdit= modell.insert(keyValuePairs);
		MultiTextEdit multiEdit= new MultiTextEdit();
		for (int i= 0; i < insertEdit.length; i++) {
			multiEdit.addChild(insertEdit[i]);
		}
		multiEdit.apply(props);

		assertEquals("org.eclipse.nls.1=value\n" + "org.eclipse.nls.2=value\n" + "\n" + "org.eclipse=value\n" + "org.eclipse.2=value\n", props.get());
	}

	public void testBlockInsertsIntoDoc() throws Exception {
		Document props= new Document("org.eclipse.1=value\n" + "org.eclipse.2=value\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		KeyValuePair[] keyValuePairs= {new KeyValuePair("org.eclipse.nls.1", "value"), new KeyValuePair("org.eclipse.nls.2", "value")};

		InsertEdit[] insertEdit= modell.insert(keyValuePairs);
		MultiTextEdit multiEdit= new MultiTextEdit();
		for (int i= 0; i < insertEdit.length; i++) {
			multiEdit.addChild(insertEdit[i]);
		}
		multiEdit.apply(props);

		assertEquals("org.eclipse.1=value\n" + "org.eclipse.2=value\n" + "org.eclipse.nls.1=value\n" + "org.eclipse.nls.2=value\n", props.get());
	}

	public void testInsertIntoDocWithBlankLines1() throws Exception {
		Document props= new Document("org.eclipse=value\n" + "\n" + "org.eclipse.test=value\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		InsertEdit insertEdit= modell.insert("org.eclipse.test", "value2");
		insertEdit.apply(props);

		assertEquals("org.eclipse=value\n" + "\n" + "org.eclipse.test=value\n" + "org.eclipse.test=value2\n", props.get());
	}

	public void testInsertIntoDocWithBlankLines2() throws Exception {
		Document props= new Document("a.b=v\n" + "\n" + "org.eclipse.test=value\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		InsertEdit insertEdit= modell.insert("a.c", "v");
		insertEdit.apply(props);

		assertEquals("a.b=v\n" + "a.c=v\n" + "\n" + "org.eclipse.test=value\n", props.get());
	}

	public void testInsertIntoDocWithDifferentSeperationChar() throws Exception {
		Document props= new Document("org.eclipse.ok:value\n" + "org.eclipse.what value\n");
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(props);

		InsertEdit insertEdit= modell.insert(new KeyValuePair("org.eclipse.nix", "value"));
		insertEdit.apply(props);

		assertEquals("org.eclipse.ok:value\n" + "org.eclipse.what value\n" + "org.eclipse.nix=value\n", props.get());
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

		ReplaceEdit replaceEdit= modell.replace(new KeyValuePair("org.eclipse.2", "value"), new KeyValuePair("org.1", "value"));
		replaceEdit.apply(props);

		assertEquals("org.eclipse.1=value1\n" + "org.1=value\n" + "org.eclipse.3=value3\n", props.get());
	}

	// Escaping stuff
	public void testEscapingOfComments() {
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(new Document());
		InsertEdit insertEdit= modell.insert(new KeyValuePair("key", "value!please escape"));
		RefactoringTest.assertEqualLines("key=value\\!please escape\n", insertEdit.getText());
	}

	public void testEscapingOfLineBreaks() {
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(new Document());
		InsertEdit insertEdit= modell.insert(new KeyValuePair("key", "value1\nvalue2\r"));
		RefactoringTest.assertEqualLines("key=value1\\nvalue2\\r\n", insertEdit.getText());
	}

	public void testEscapingOfUniCode() {
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(new Document());
		InsertEdit insertEdit= modell.insert(new KeyValuePair("key", "\u00ea"));
		RefactoringTest.assertEqualLines("key=\\u00EA\n", insertEdit.getText());
	}
	
	public void testEscapingOfLeadingWhiteSpaces() {
		PropertyFileDocumentModel modell= new PropertyFileDocumentModel(new Document());
		InsertEdit insertEdit= modell.insert(new KeyValuePair("key", "  test"));
		RefactoringTest.assertEqualLines("key=\\ \\ test\n", insertEdit.getText());
	}
}
