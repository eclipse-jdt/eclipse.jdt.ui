/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.nls;

import org.eclipse.jdt.internal.corext.refactoring.nls.KeyValuePair;
import org.eclipse.jdt.internal.corext.refactoring.nls.PropertyFileDocumentModell;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.InsertEdit;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PropertyFileDocumentModellTest extends TestCase {

    public PropertyFileDocumentModellTest(String name) {
        super(name);
    }
    
    public static TestSuite suite() {
    	return new TestSuite(PropertyFileDocumentModellTest.class);
    }
    
    public void testInsertIntoEmptyDoc() {        
        PropertyFileDocumentModell modell = 
            new PropertyFileDocumentModell(new Document());
        
        InsertEdit insertEdit = modell.insert("key", "value");
        
        assertEquals("key=value\n", insertEdit.getText());
        assertEquals(0, insertEdit.getOffset());
    }
    
    public void testInsertIntoDoc() {
        String props = 
            "org.eclipse.1=value\n" +
            "org.eclipse.3=value\n";
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document(props));
        
        InsertEdit insertEdit = modell.insert("org.eclipse.2", "value");
        
        assertEquals("org.eclipse.2=value\n", insertEdit.getText());
        assertEquals(20, insertEdit.getOffset());
    }
    
    public void testInsertIntoDocWithBlankLines1() {
        String props = 
            "org.eclipse=value\n" +
            "\n" +
            "org.eclipse.test=value\n";
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document(props));
        
        InsertEdit insertEdit = modell.insert("org.eclipse.test", "value2");
        
        assertEquals("org.eclipse.test=value2\n", insertEdit.getText());
        assertEquals(19, insertEdit.getOffset());
    }
    
    public void testInsertIntoDocWithBlankLines2() {
        String props = 
            "a.b=v\n" +
            "\n" +
            "org.eclipse.test=value\n";
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document(props));
        
        InsertEdit insertEdit = modell.insert("a.c", "v");
        
        assertEquals("a.c=v\n", insertEdit.getText());
        assertEquals(6, insertEdit.getOffset());
    }
    
    public void testInsertIntoDocWithKeyValuePair() {
        String props = 
            "org.eclipse.1=value\n" +
            "org.eclipse.3=value\n";
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document(props));
        
        InsertEdit insertEdit = modell.insert(new KeyValuePair("org.eclipse.2", "value"));
        
        assertEquals("org.eclipse.2=value\n", insertEdit.getText());
        assertEquals(20, insertEdit.getOffset());        
    }    
    
    public void testInsertIntoDocWithDifferentSeperationChar() {
        String props = 
            "org.eclipse.1:value\n" +
            "org.eclipse.3 value\n";
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document(props));
        
        InsertEdit insertEdit = modell.insert(new KeyValuePair("org.eclipse.2", "value"));
        
        assertEquals("org.eclipse.2=value\n", insertEdit.getText());
        assertEquals(20, insertEdit.getOffset());
        assertEquals("value\n", modell.getProperty("org.eclipse.3"));
    }
    
    public void testEscapingOfComments() {
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document());        
        InsertEdit insertEdit = modell.insert(new KeyValuePair("key", "value!please escape"));
        assertEquals("key=value\\!please escape\n", insertEdit.getText());
    }
}
