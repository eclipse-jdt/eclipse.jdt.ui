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

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.refactoring.nls.KeyValuePair;
import org.eclipse.jdt.internal.corext.refactoring.nls.PropertyFileDocumentModell;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;

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
    
    public void testInsertIntoDocWithDifferentSeperationChar() throws Exception {
        Document props = new Document( 
            "org.eclipse.1:value\n" +
            "org.eclipse.3 value\n");
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(props);
        
        InsertEdit insertEdit = modell.insert(new KeyValuePair("org.eclipse.2", "value"));
        insertEdit.apply(props);
        
        assertEquals(
    			"org.eclipse.1:value\n" +
                "org.eclipse.2=value\n" +
                "org.eclipse.3 value\n",
				props.get());
    }
    
    public void testInserAfterUniCode() throws Exception {
        Document doc = new Document( 
            "org.eclipse.1=\\u00ea\n" +
            "org.eclipse.3=value3\n");
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(doc);
        
        InsertEdit insertEdit = modell.insert(new KeyValuePair("org.eclipse.2", "value2"));
        
        insertEdit.apply(doc);
        
        assertEquals(
        		"org.eclipse.1=\\u00ea\n" +
        		"org.eclipse.2=value2\n" +
            	"org.eclipse.3=value3\n",
				doc.get());
    }  
    
    public void testRemovingOfKey() throws Exception {
    	Document props = new Document(
            "org.eclipse.1=value1\n" +
            "org.eclipse.2=value2\n" +
            "org.eclipse.3=value3\n");
    	PropertyFileDocumentModell modell = new PropertyFileDocumentModell(props);
    	
    	DeleteEdit deleteEdit = modell.remove("org.eclipse.2");
    	deleteEdit.apply(props);
    	
    	assertEquals(
    			"org.eclipse.1=value1\n" +
                "org.eclipse.3=value3\n",
				props.get());
    }
    
    public void testRemovingOfLastKey() throws Exception {
    	Document props = new Document(
            "org.eclipse.1=value1\n" +
            "org.eclipse.2=value2\n" +
            "org.eclipse.3=value3\n");
    	PropertyFileDocumentModell modell = new PropertyFileDocumentModell(props);
    	
    	DeleteEdit deleteEdit = modell.remove("org.eclipse.3");
    	deleteEdit.apply(props);
    	
    	assertEquals(
    			"org.eclipse.1=value1\n" +
                "org.eclipse.2=value2\n",
				props.get());
    }    
    
    public void testEscapingOfComments() {
        PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document());        
        InsertEdit insertEdit = modell.insert(new KeyValuePair("key", "value!please escape"));
        assertEquals("key=value\\!please escape\n", insertEdit.getText());
    }
    
    public void testEscapingOfLineBreaks() {
    	PropertyFileDocumentModell modell = 
    		new PropertyFileDocumentModell(new Document());
    	InsertEdit insertEdit = modell.insert(new KeyValuePair("key", "value1\nvalue2"));
    	assertEquals("key=value1\\nvalue2\n", insertEdit.getText());    	
    }
    
    public void testEscapingOfUniCode() {
    	PropertyFileDocumentModell modell = new PropertyFileDocumentModell(new Document());        
        InsertEdit insertEdit = modell.insert(new KeyValuePair("key", "\u00ea"));
        assertEquals("key=\\u00EA\n", insertEdit.getText());
    }
}
