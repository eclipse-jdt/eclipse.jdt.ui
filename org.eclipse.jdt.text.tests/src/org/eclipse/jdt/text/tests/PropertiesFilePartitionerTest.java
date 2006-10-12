/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.PreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.propertiesfileeditor.IPropertiesFilePartitions;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileDocumentSetupParticipant;



public class PropertiesFilePartitionerTest extends TestCase {
	
	private JavaTextTools fTextTools;
	private Document fDocument;
	protected boolean fDocumentPartitioningChanged;
	
	
	public PropertiesFilePartitionerTest(String name) {
		super(name);
	}
	
	protected void setUp() {

		fTextTools= new JavaTextTools(new PreferenceStore());
		
		fDocument= new Document();
		PropertiesFileDocumentSetupParticipant.setupDocument(fDocument);
		fDocument.set("###Comment\nkey=value\nkey value\nkey:value");
		
		fDocumentPartitioningChanged= false;
		fDocument.addDocumentPartitioningListener(new IDocumentPartitioningListener() {
			public void documentPartitioningChanged(IDocument document) {
				fDocumentPartitioningChanged= true;
			}
		});	
	}
	
	public static Test suite() {
		return new TestSuite(PropertiesFilePartitionerTest.class); 
	}
	
	protected void tearDown () {
		fTextTools.dispose();
		fTextTools= null;
		
		IDocumentPartitioner partitioner= fDocument.getDocumentPartitioner(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING);
		partitioner.disconnect();
		fDocument= null;
	}

	protected String print(ITypedRegion r) {
		return "[" + r.getOffset() + "," + r.getLength() + "," + r.getType() + "]";
	}
	
	protected void checkPartitioning(ITypedRegion[] expectation, ITypedRegion[] result) {
		
		assertEquals("invalid number of partitions:", expectation.length, result.length);
		
		for (int i= 0; i < expectation.length; i++) {
			ITypedRegion e= expectation[i];
			ITypedRegion r= result[i];
			assertTrue("was: "+ print(r) + ", expected: " + print(e), r.equals(e));
		}
				
	}
	
	public void testInitialPartitioning() {
		try {
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
				new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
				new TypedRegion(11, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(14, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(34, 6, IPropertiesFilePartitions.PROPERTY_VALUE)
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
	
	public void testPartitioningWithEndingEscape() {
		try {
			fDocument.replace(40, 0, "\n key value\\n\nkey value\n");
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
				new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
				new TypedRegion(11, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(14, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(34, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(41, 4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(45, 9, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(54, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(57, 7, IPropertiesFilePartitions.PROPERTY_VALUE)
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
	
	public void testPartitioningWithLeadingWhitespace() {
		try {
			fDocument.replace(40, 0, "\n key value\n  key value\n\tkey value\n\t\tkey value");
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
					new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
					new TypedRegion(11, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(14, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(34, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(41, 4, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(45, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(52, 5, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(57, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(64, 4, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(68, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(75, 5, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(80, 6, IPropertiesFilePartitions.PROPERTY_VALUE),
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
		
	public void testIntraPartitionChange1() {
		try {
			
			fDocument.replace(1, 3, "ttt");
			
			assertTrue(!fDocumentPartitioningChanged);
			
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
				new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
				new TypedRegion(11, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(14, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(34, 6, IPropertiesFilePartitions.PROPERTY_VALUE)
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}

	public void testIntraPartitionChange2() {
		try {
			
			fDocument.replace(14, 1, " ");
			
			 assertTrue(!fDocumentPartitioningChanged);
			
				ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
				TypedRegion[] expectation= {
					new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
					new TypedRegion(11, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(14, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(34, 6, IPropertiesFilePartitions.PROPERTY_VALUE)
				};
				
				checkPartitioning(expectation, result);
			} catch (BadLocationException x) {
				fail();
			} catch (BadPartitioningException x) {
				fail();
			}
	}
	
	public void testInsertNewPartition() {
		try {
			
			fDocument.replace(31, 0, "key:value\n");
			
			assertTrue(fDocumentPartitioningChanged);
			
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
				new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
				new TypedRegion(11, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(14, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(34, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(41, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(44, 6, IPropertiesFilePartitions.PROPERTY_VALUE)
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}	

	public void testRemoveCommentPartition() {
		try {
			
			fDocument.replace(0, 11, "");
			
			assertTrue(fDocumentPartitioningChanged);
			
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
				new TypedRegion(0, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(3, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(10, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(13, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
				new TypedRegion(20, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(23, 6, IPropertiesFilePartitions.PROPERTY_VALUE)
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
	
	public void testRemoveValuePartition() {
		
		fDocumentPartitioningChanged= false;
		
		try {
			
			fDocument.replace(34, 6, "");
			
			assertTrue(fDocumentPartitioningChanged);
			
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
					new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
					new TypedRegion(11, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(14, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE)
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}

	
	public void testJoinPartitions1() {
		try {
			
			fDocument.replace(14, 1, "x");
			
			assertTrue(fDocumentPartitioningChanged);
			
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
					new TypedRegion(0, 11, IPropertiesFilePartitions.COMMENT),
					new TypedRegion(11, 13, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(34, 6, IPropertiesFilePartitions.PROPERTY_VALUE)
				};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
	
	public void testJoinPartitions2() {
		try {
			
			fDocument.replace(10, 1, " ");
			
			assertTrue(fDocumentPartitioningChanged);
			
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
					new TypedRegion(0, 21, IPropertiesFilePartitions.COMMENT),
					new TypedRegion(21, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(24, 7, IPropertiesFilePartitions.PROPERTY_VALUE),
					new TypedRegion(31, 3, IDocument.DEFAULT_CONTENT_TYPE),
					new TypedRegion(34, 6, IPropertiesFilePartitions.PROPERTY_VALUE)
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
	
	
	public void testSplitPartition1() {
		
		testJoinPartitions1();
		fDocumentPartitioningChanged= false;
		
		
		try {
			
			fDocument.replace(14, 1, "=");
			
			assertTrue(fDocumentPartitioningChanged);
			
			
		} catch (BadLocationException x) {
			assertTrue(false);
		}
		
		testInitialPartitioning();
	}
	
	public void testSplitPartition2() {
		
		testJoinPartitions2();
		fDocumentPartitioningChanged= false;
		
		try {
			
			fDocument.replace(10, 1, "\n");
			
			assertTrue(fDocumentPartitioningChanged);
			
		} catch (BadLocationException x) {
			assertTrue(false);
		}
			
		testInitialPartitioning();
	}
	
	public void testPartitionFinder() {
		try {
			
			ITypedRegion[] partitioning= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			
			for (int i= 0; i < partitioning.length; i++) {
				ITypedRegion expected= partitioning[i];
				for (int j= 0; j < expected.getLength(); j++) {
					ITypedRegion result= fDocument.getPartition(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, expected.getOffset() + j, false);
					assertTrue(expected.equals(result));
				}
			}
			
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
	
	public void testReplaceWithCommentPartition() {
		try {
			
			fDocument.replace(0, fDocument.getLength(), "#Comment");
						
			ITypedRegion[] result= fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false);
			TypedRegion[] expectation= {
				new TypedRegion(0, 8, IPropertiesFilePartitions.COMMENT),
			};
			
			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}
}
