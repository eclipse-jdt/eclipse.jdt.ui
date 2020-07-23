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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;

public class JavaPartitionerTest {
	private JavaTextTools fTextTools;
	private Document fDocument;
	protected boolean fDocumentPartitioningChanged;

	@Before
	public void setUp() {

		fTextTools= new JavaTextTools(new PreferenceStore());

		fDocument= new Document();
		IDocumentPartitioner partitioner= fTextTools.createDocumentPartitioner();
		partitioner.connect(fDocument);
		fDocument.setDocumentPartitioner(partitioner);
		fDocument.set("xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx");

		fDocumentPartitioningChanged= false;
		fDocument.addDocumentPartitioningListener(document -> fDocumentPartitioningChanged= true);
	}

	@After
	public void tearDown () {
		fTextTools.dispose();
		fTextTools= null;

		IDocumentPartitioner partitioner= fDocument.getDocumentPartitioner();
		partitioner.disconnect();
		fDocument= null;
	}

	protected String print(ITypedRegion r) {
		return "[" + r.getOffset() + "," + r.getLength() + "," + r.getType() + "]";
	}

	protected void checkPartitioning(ITypedRegion[] expectation, ITypedRegion[] result) {

		assertEquals("invalid number of partitions", expectation.length, result.length);

		for (int i= 0; i < expectation.length; i++) {
			ITypedRegion e= expectation[i];
			ITypedRegion r= result[i];
			assertEquals(print(r) + " != " + print(e), r, e);
		}

	}

	@Test
	public void testInitialPartitioning() {
		try {

			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx"

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(33, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(38, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(43, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testIntraPartitionChange() {
		try {

			fDocument.replace(34, 3, "y");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\ny\n/***/\nxxx");

			assertFalse(fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(33, 3, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(36, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(41, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testIntraPartitionChange2() {
		try {

			fDocument.replace(41, 0, "yyy");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/**yyy*/\nxxx");

			// assertTrue(!fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(33, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(38, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(46, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}
	@Test
	public void testInsertNewPartition() {
		try {

			fDocument.replace(35, 1, "/***/");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nx/***/x\n/***/\nxxx");

			assertTrue(fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(33, 2, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(35, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(40, 2, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(42, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(47, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testRemovePartition1() {
		try {

			fDocument.replace(13, 16, "");
			//	"xxx\n/*xxx*/\nx/**/\nxxx\n/***/\nxxx");

			assertTrue(fDocumentPartitioningChanged);


			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 2, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(13, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(17, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(22, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(27, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testRemovePartition2() {

		testJoinPartition3();
		fDocumentPartitioningChanged= false;

		try {

			fDocument.replace(5, 2, "");
			//	"xxx\nxxx\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx"

			assertTrue(fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  12, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(12,  8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(20, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(25, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(29, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(34, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(39, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}


	@Test
	public void testJoinPartitions1() {
		try {

			fDocument.replace(31, 1, "");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/*/\nxxx\n/***/\nxxx"

			assertTrue(fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 13, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(42, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testJoinPartitions2() {
		try {

			fDocument.replace(32, 1, "");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**\nxxx\n/***/\nxxx"

			assertTrue(fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 13, IJavaPartitions.JAVA_DOC),
				new TypedRegion(42, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testJoinPartition3() {
		try {

			fDocument.replace(9, 2, "");
			//	"xxx\n/*xxx\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx"

			assertTrue(fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  18, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(22, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(27, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(31, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(36, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(41, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}


	@Test
	public void testSplitPartition1() {

		testJoinPartitions1();
		fDocumentPartitioningChanged= false;


		try {

			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/*/\nxxx\n/***/\nxxx"
			fDocument.replace(31, 0, "*");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx"

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

			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**\nxxx\n/***/\nxxx"
			fDocument.replace(32, 0, "/");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx"

			assertTrue(fDocumentPartitioningChanged);

		} catch (BadLocationException x) {
			fail();
		}

		testInitialPartitioning();
	}

	@Test
	public void testSplitPartition3() {

		fDocumentPartitioningChanged= false;

		try {

			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx"
			fDocument.replace(12, 9, "");
			//	"xxx\n/*xxx*/\nx*/\nxxx\n/**/\nxxx\n/***/\nxxx"

			assertTrue(fDocumentPartitioningChanged);

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 9, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(20, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(34, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testCorruptPartitioning1() {
		try {

			fDocument.replace(0, fDocument.getLength(), "/***/\n/***/");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(5, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(6, 5, IJavaPartitions.JAVA_DOC)
			};

			checkPartitioning(expectation, result);

			fDocument.replace(6, 0, "*/\n/***/\n/*");
			// "/***/\n*/\n/***/\n/*/***/"

			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(5, 4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(9, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(14, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(15, 7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testCorruptPartitioning2() {
		try {

			fDocument.replace(0, fDocument.getLength(), "/***/\n/***/\n/***/");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(5, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(6, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(11, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(12, 5, IJavaPartitions.JAVA_DOC)
			};

			checkPartitioning(expectation, result);

			fDocument.replace(6, 0, "*/\n/***/\n/*");
			// "/***/\n*/\n/***/\n/*/***/\n/***/"

			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(5, 4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(9, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(14, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(15, 7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(22, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(23, 5, IJavaPartitions.JAVA_DOC)
			};

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testCorruptPartitioning3() {
		try {

			fDocument.replace(0, fDocument.getLength(), "/***/\n/**/");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(5, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(6, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};

			checkPartitioning(expectation, result);

			fDocument.replace(0, 9, "/***/\n/***/\n/***/\n/**");
			// "/***/\n/***/\n/***/\n/***/"

			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(5, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(6, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(11, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(12, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(17, 1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(18, 5, IJavaPartitions.JAVA_DOC)
			};

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testOpenPartition1() {
		try {

			fDocument.replace(42, 1, "");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***\nxxx"

			assertTrue(fDocumentPartitioningChanged);


			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(33, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(38, 8, IJavaPartitions.JAVA_DOC)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testOpenPartition2() {
		try {

			fDocument.replace(47, 0, "/*");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/***/\nxxx/*"

			assertTrue(fDocumentPartitioningChanged);


			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(33, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(38, 5, IJavaPartitions.JAVA_DOC),
				new TypedRegion(43, 4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(47, 2, IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}


	@Test
	public void testChangeContentTypeOfPartition() {
		try {

			fDocument.replace(39, 1, "");
			//	"xxx\n/*xxx*/\nxxx\n/**xxx*/\nxxx\n/**/\nxxx\n/**/\nxxx"

			assertTrue(fDocumentPartitioningChanged);


			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(4,  7, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(16, 8, IJavaPartitions.JAVA_DOC),
				new TypedRegion(24, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(29, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(33, 5, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(38, 4, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(42, 4, IDocument.DEFAULT_CONTENT_TYPE)
			};

			checkPartitioning(expectation, result);
		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testPartitionFinder() {
		try {

			for (ITypedRegion expected : fDocument.computePartitioning(0, fDocument.getLength())) {
				for (int j= 0; j < expected.getLength(); j++) {
					ITypedRegion result= fDocument.getPartition(expected.getOffset() + j);
					assertEquals(expected, result);
				}
			}

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testExtendPartition() {
		try {

			fDocument.replace(0, fDocument.getLength(), "/*");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  2, IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};

			checkPartitioning(expectation, result);

			fDocument.replace(2, 0, " ");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  3, IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};

			checkPartitioning(expectation, result);

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testTransformPartition() {
		try {

			fDocument.replace(0, fDocument.getLength(), "/*");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  2, IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};

			checkPartitioning(expectation, result);

			fDocument.replace(2, 0, "*");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  3, IJavaPartitions.JAVA_DOC)
			};

			checkPartitioning(expectation, result);

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testTogglePartition() {
		try {

			fDocument.replace(0, fDocument.getLength(), "\t/*\n\tx\n\t/*/\n\ty\n//\t*/");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation1= {
				new TypedRegion(0,  1, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(1,  10, IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(11, 4, IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(15, 5, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT)
			};
			checkPartitioning(expectation1, result);

			fDocumentPartitioningChanged= false;
			fDocument.replace(0, 0, "//"); // "//\t/*\n\tx\n\t/*/\n\ty\n//\t*/"
			assertTrue(fDocumentPartitioningChanged);

			result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation2= {
				new TypedRegion(0,  6, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT),
				new TypedRegion(6,  4,  IDocument.DEFAULT_CONTENT_TYPE),
				new TypedRegion(10,  12, IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};
			checkPartitioning(expectation2, result);

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testEditing1() {
		try {

			fDocument.replace(0, fDocument.getLength(), "");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  0,  IDocument.DEFAULT_CONTENT_TYPE)
			};
			checkPartitioning(expectation, result);

			fDocument.replace(fDocument.getLength(), 0, "/");
			fDocument.replace(fDocument.getLength(), 0, "*");
			fDocument.replace(fDocument.getLength(), 0, "*");
			fDocument.replace(fDocument.getLength(), 0, "/");

			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  fDocument.getLength(),  IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};
			checkPartitioning(expectation, result);


			fDocument.replace(fDocument.getLength(), 0, "\r\n");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  4,  IJavaPartitions.JAVA_MULTI_LINE_COMMENT),
				new TypedRegion(4, 2, IDocument.DEFAULT_CONTENT_TYPE)
			};
			checkPartitioning(expectation, result);


		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testEditing2() {
		try {

			fDocument.replace(0, fDocument.getLength(), "");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  0,  IDocument.DEFAULT_CONTENT_TYPE)
			};
			checkPartitioning(expectation, result);

			fDocument.replace(fDocument.getLength(), 0, "/");
			fDocument.replace(fDocument.getLength(), 0, "*");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  fDocument.getLength(),  IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};
			checkPartitioning(expectation, result);

			fDocument.replace(fDocument.getLength(), 0, "\r\n");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  fDocument.getLength(),  IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};
			checkPartitioning(expectation, result);

			fDocument.replace(fDocument.getLength(), 0, "*");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  fDocument.getLength(),  IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};
			checkPartitioning(expectation, result);

			fDocument.replace(fDocument.getLength(), 0, "*");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  fDocument.getLength(),  IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
			};
			checkPartitioning(expectation, result);

		} catch (BadLocationException x) {
			fail();
		}
	}

	@Test
	public void testEditing3() {
		try {

			fDocument.replace(0, fDocument.getLength(), "");

			ITypedRegion[] result= fDocument.computePartitioning(0, fDocument.getLength());
			TypedRegion[] expectation= {
				new TypedRegion(0,  0,  IDocument.DEFAULT_CONTENT_TYPE)
			};
			checkPartitioning(expectation, result);

			fDocument.replace(fDocument.getLength(), 0, "/");
			fDocument.replace(fDocument.getLength(), 0, "*");
			fDocument.replace(fDocument.getLength(), 0, "*");
			fDocument.replace(fDocument.getLength(), 0, "\r\n *");
			fDocument.replace(fDocument.getLength(), 0, "/");


			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  fDocument.getLength(),  IJavaPartitions.JAVA_DOC)
			};
			checkPartitioning(expectation, result);

			fDocument.replace(fDocument.getLength(), 0, "*");
			result= fDocument.computePartitioning(0, fDocument.getLength());
			expectation= new TypedRegion[] {
				new TypedRegion(0,  8,  IJavaPartitions.JAVA_DOC),
				new TypedRegion(8, 1, IDocument.DEFAULT_CONTENT_TYPE)
			};
			checkPartitioning(expectation, result);

		} catch (BadLocationException x) {
			fail();
		}
	}
}
