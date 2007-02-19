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

package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Measures the time to determine the content type of a file buffer.
 * 
 * @since 3.1
 */
public class ContentTypeTest extends TextPerformanceTestCase {
	
	private static final Class THIS= ContentTypeTest.class;


	private static final IContentType TEXT_CONTENT_TYPE= Platform.getContentTypeManager().getContentType("org.eclipse.core.runtime.text"); //$NON-NLS-1$

	private static final String TEXT_FILE= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/common/version.txt";


	private static final IContentType JAVA_CONTENT_TYPE= Platform.getContentTypeManager().getContentType("org.eclipse.jdt.core.javaSource"); //$NON-NLS-1$

	private static final String JAVA_FILE= PerformanceTestSetup.TEXT_LAYOUT;

	
	private static final IContentType PROPERTIES_CONTENT_TYPE= Platform.getContentTypeManager().getContentType("org.eclipse.jdt.core.javaProperties"); //$NON-NLS-1$

	private static final String PROPERTIES_FILE= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/common_j2se/org/eclipse/swt/internal/SWTMessages.properties";


	private static final IContentType PLUGIN_XML_CONTENT_TYPE= Platform.getContentTypeManager().getContentType("org.eclipse.pde.pluginManifest"); //$NON-NLS-1$

	private static final String PLUGIN_XML_FILE= "/" + PerformanceTestSetup.PROJECT + "/plugin.xml";


	private static final IContentType PLUGIN_PROPERTIES_CONTENT_TYPE= Platform.getContentTypeManager().getContentType("org.eclipse.pde.pluginProperties"); //$NON-NLS-1$

	private static final String PLUGIN_PROPERTIES_FILE= "/" + PerformanceTestSetup.PROJECT + "/plugin.properties";


	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;
	
	private static final int ITERATIONS= 10000;

	private static final int PLUGIN_XML_ITERATIONS= 1000;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.joinBackgroundActivities();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	/**
	 * Measures the time to retrieve the content type for a modified text
	 * file buffer.
	 * 
	 * @throws Exception
	 */
	public void testTextDirty() throws Exception {
		measure(TEXT_FILE, TEXT_CONTENT_TYPE, true, getNullPerformanceMeter(), getWarmUpRuns());
		measure(TEXT_FILE, TEXT_CONTENT_TYPE, true, createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	/**
	 * Measures the time to retrieve the content type for a modified Java
	 * file buffer.
	 * 
	 * @throws Exception
	 */
	public void testJavaDirty() throws Exception {
		measure(JAVA_FILE, JAVA_CONTENT_TYPE, true, getNullPerformanceMeter(), getWarmUpRuns());
		measure(JAVA_FILE, JAVA_CONTENT_TYPE, true, createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	/**
	 * Measures the time to retrieve the content type for a modified properties
	 * file buffer.
	 * 
	 * @throws Exception
	 */
	public void testPropertiesDirty() throws Exception {
		measure(PROPERTIES_FILE, PROPERTIES_CONTENT_TYPE, true, getNullPerformanceMeter(), getWarmUpRuns());
		measure(PROPERTIES_FILE, PROPERTIES_CONTENT_TYPE, true, createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	/**
	 * Measures the time to retrieve the content type for a modified plugin.xml
	 * file buffer.
	 * 
	 * @throws Exception
	 */
	public void testPluginXMLDirty() throws Exception {
		measure(PLUGIN_XML_FILE, PLUGIN_XML_CONTENT_TYPE, true, getNullPerformanceMeter(), getWarmUpRuns(), PLUGIN_XML_ITERATIONS);
		measure(PLUGIN_XML_FILE, PLUGIN_XML_CONTENT_TYPE, true, createPerformanceMeter(), getMeasuredRuns(), PLUGIN_XML_ITERATIONS);
		commitAllMeasurements();
		assertAllPerformance();
	}

	/**
	 * Measures the time to retrieve the content type for a modified
	 * plugin properties file buffer.
	 * 
	 * @throws Exception
	 */
	public void testPluginPropertiesDirty() throws Exception {
		measure(PLUGIN_PROPERTIES_FILE, PLUGIN_PROPERTIES_CONTENT_TYPE, true, getNullPerformanceMeter(), getWarmUpRuns());
		measure(PLUGIN_PROPERTIES_FILE, PLUGIN_PROPERTIES_CONTENT_TYPE, true, createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measure(String filename, IContentType expectedContentType, boolean dirty, PerformanceMeter performanceMeter, int runs) throws Exception {
		measure(filename, expectedContentType, dirty, performanceMeter, runs, ITERATIONS);
	}

	private void measure(String filename, IContentType expectedContentType, boolean dirty, PerformanceMeter performanceMeter, int runs, int iterations) throws CoreException, BadLocationException {
		IFile file= ResourceTestHelper.findFile(filename);
		IPath path= file.getFullPath();
		ITextFileBufferManager fileBufferManager= FileBuffers.getTextFileBufferManager();
		try {
			fileBufferManager.connect(path, LocationKind.IFILE, null);
			ITextFileBuffer fileBuffer= fileBufferManager.getTextFileBuffer(path, LocationKind.IFILE);
			if (dirty) {
				IDocument document= fileBuffer.getDocument();
				document.replace(document.getLength(), 0, " ");
			}
			for (int i= 0; i < runs; i++) {
				IContentType contentType= null;
				performanceMeter.start();
				for (int j= 0; j < iterations; j++) {
					contentType= fileBuffer.getContentType();
					assertNotNull(contentType);
				}
				performanceMeter.stop();
				assertEquals(expectedContentType, contentType);
			}
		} finally {
			fileBufferManager.disconnect(path, LocationKind.IFILE, null);
		}
	}
}
