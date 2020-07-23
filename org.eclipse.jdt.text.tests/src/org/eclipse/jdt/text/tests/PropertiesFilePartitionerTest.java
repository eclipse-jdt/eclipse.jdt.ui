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
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jface.preference.PreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.propertiesfileeditor.IPropertiesFilePartitions;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileDocumentSetupParticipant;

public class PropertiesFilePartitionerTest {
	private JavaTextTools fTextTools;
	private Document fDocument;
	protected boolean fDocumentPartitioningChanged;

	@Before
	public void setUp() {

		fTextTools= new JavaTextTools(new PreferenceStore());

		fDocument= new Document();
		PropertiesFileDocumentSetupParticipant.setupDocument(fDocument);
		fDocument.set("###Comment\nkey=value\nkey value\nkey:value");
		//             01234567890 1234567890 1234567890 123456789

		fDocumentPartitioningChanged= false;
		fDocument.addDocumentPartitioningListener(document -> fDocumentPartitioningChanged= true);
	}

	@After
	public void tearDown () {
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
			assertEquals("was: "+ print(r) + ", expected: " + print(e), r, e);
		}

	}

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testIntraPartitionChange1() {
		try {

			fDocument.replace(1, 3, "ttt");

			assertFalse(fDocumentPartitioningChanged);

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

	@Test
	public void testIntraPartitionChange2() {
		try {

			fDocument.replace(14, 1, " ");

			 assertFalse(fDocumentPartitioningChanged);

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

	@Test
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

	@Test
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

	@Test
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


	@Test
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

	@Test
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


	@Test
	public void testSplitPartition1() {

		testJoinPartitions1();
		fDocumentPartitioningChanged= false;


		try {

			fDocument.replace(14, 1, "=");

			assertTrue(fDocumentPartitioningChanged);


		} catch (BadLocationException x) {
			fail();
		}

		testInitialPartitioning();
	}

	@Test
	public void testSplitPartition2() {

		testJoinPartitions2();
		fDocumentPartitioningChanged= false;

		try {

			fDocument.replace(10, 1, "\n");

			assertTrue(fDocumentPartitioningChanged);

		} catch (BadLocationException x) {
			fail();
		}

		testInitialPartitioning();
	}

	@Test
	public void testPartitionFinder() {
		try {

			for (ITypedRegion expected : fDocument.computePartitioning(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, 0, fDocument.getLength(), false)) {
				for (int j= 0; j < expected.getLength(); j++) {
					ITypedRegion result= fDocument.getPartition(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, expected.getOffset() + j, false);
					assertEquals(expected, result);
				}
			}

		} catch (BadLocationException x) {
			fail();
		} catch (BadPartitioningException x) {
			fail();
		}
	}

	@Test
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
