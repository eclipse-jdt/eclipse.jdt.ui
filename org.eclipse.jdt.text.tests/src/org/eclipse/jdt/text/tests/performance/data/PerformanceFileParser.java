/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance.data;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * @since 3.1
 */
public class PerformanceFileParser {
	private static final String RUN= "run"; //$NON-NLS-1$
	
	private static final String DRIVER= "driver"; //$NON-NLS-1$
	private static final String UUID= "uuid"; //$NON-NLS-1$
	private static final String DRIVERDATE= "driverdate"; //$NON-NLS-1$
	private static final String DRIVERLABEL= "driverlabel"; //$NON-NLS-1$
	private static final String DRIVERSTREAM= "driverstream"; //$NON-NLS-1$
	private static final String JVM= "jvm"; //$NON-NLS-1$
	private static final String HOST= "host"; //$NON-NLS-1$
	private static final String RUN_TS= "runTS"; //$NON-NLS-1$
	private static final String DISPLAY_RUN_TS= "displayRunTS"; //$NON-NLS-1$
	private static final String VERSION= "version"; //$NON-NLS-1$
	private static final String TESTNAME= "testname"; //$NON-NLS-1$
	private static final String CMDARGS= "cmdArgs"; //$NON-NLS-1$
	
	private static final String STEP= "step"; //$NON-NLS-1$
	private static final String ID= "id"; //$NON-NLS-1$
	private static final String VALUE= "value"; //$NON-NLS-1$
	private static final String WHAT= "what"; //$NON-NLS-1$
	private static final String RESULT= "result"; //$NON-NLS-1$
	
	
	private static final String[] PROPERTIES= new String[] { UUID, DRIVER, DRIVERDATE, DRIVERLABEL, DRIVERSTREAM, JVM, HOST, RUN_TS, DISPLAY_RUN_TS, VERSION, TESTNAME, CMDARGS};
	
	public Sample parse(InputSource source) throws IOException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			DocumentBuilder parser= factory.newDocumentBuilder();
			parser.setErrorHandler(new ErrorHandler() {
				public void warning(SAXParseException exception) throws SAXException {
				}
				public void error(SAXParseException exception) throws SAXException {
				}
				public void fatalError(SAXParseException exception) throws SAXException {
				}
			});
			Document document= parser.parse(source);
			
			NodeList elements= document.getElementsByTagName(RUN);
			if (elements.getLength() != 1)
				return malformedInputResult();
			
			Map properties= new HashMap();
			Node node= elements.item(0);
			NamedNodeMap attributes= node.getAttributes();
			for (int i= 0; i < PROPERTIES.length; i++) {
				properties.put(PROPERTIES[i], getStringValue(attributes, PROPERTIES[i], null));
			}
			
			List datapoints= new ArrayList();
			elements= document.getElementsByTagName(STEP);
			int length= elements.getLength();
			for (int i= 0; i < length; i++) {
				node= elements.item(i);
				attributes= node.getAttributes();
				String id= getStringValue(attributes, ID, null);
				if (id == null)
					return malformedInputResult();
				
				Map scalars= new HashMap();
				for (Node value= node.getFirstChild(); value != null; value= value.getNextSibling()) {
					if (value.getNodeType() != Node.ELEMENT_NODE || !value.getNodeName().equals(VALUE))
						continue;
					attributes= value.getAttributes();
					String dimension= getStringValue(attributes, WHAT);
					long magnitude= getLongValue(attributes, RESULT);
					scalars.put(dimension, new Scalar(dimension, magnitude));
				}
				
				datapoints.add(new DataPoint(id, scalars));
			}
			
			Sample session= new Sample(properties, (DataPoint[]) datapoints.toArray(new DataPoint[datapoints.size()]));
			return session;
			
		} catch (ParserConfigurationException e) {
			Assert.isTrue(false);
		} catch (SAXException e) {
			Throwable t= e.getCause();
			if (t instanceof IOException)
				throw (IOException) t;
			else
				throw new IOException(t == null ? e.getMessage() : t.getMessage());
		}
		
		
		return null;
	}
	
	public Sample parse(InputStream stream) throws IOException {
		InputStream header= new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<dummy>".getBytes()); //$NON-NLS-1$
		stream= new SequenceInputStream(header, stream); 
		InputStream footer= new ByteArrayInputStream("</dummy>".getBytes()); //$NON-NLS-1$
		stream= new SequenceInputStream(stream, footer);
		return parse(new InputSource(stream));
	}

	public Sample[] parseLocation(String path) {
		List result= new ArrayList(); 
		File dir= new File(path);
		if (dir.isDirectory()) {
			String[] files= dir.list();
			for (int i= 0; i < files.length; i++) {
				parseOne(new File(dir, files[i]), result);
			}
		} else {
			parseOne(dir, result);
		}
		
		return (Sample[]) result.toArray(new Sample[result.size()]);
	}

	private void parseOne(File file, List result) {
		try {
			InputStream stream= new BufferedInputStream(new FileInputStream(file));
			Sample parsed= parse(stream);
			parsed.fId= file.getCanonicalPath();
			result.add(parsed);
		} catch (FileNotFoundException e) {
			// continue
		} catch (IOException e) {
			// continue
		}
	}

	private Sample malformedInputResult() throws IOException {
		throw new IOException("malformed input"); //$NON-NLS-1$
	}
	
	private String getStringValue(NamedNodeMap attributes, String name, String defaultValue) {
		Node node= attributes.getNamedItem(name);
		return node == null	? defaultValue : node.getNodeValue();
	}
	
	private String getStringValue(NamedNodeMap attributes, String attribute) throws SAXException {
		String val= getStringValue(attributes, attribute, null);
		if (val == null)
			throw new SAXException("missing attribute:" + attribute); //$NON-NLS-1$
		return val;
	}

	private long getLongValue(NamedNodeMap attributes, String attribute) throws SAXException {
		String val= getStringValue(attributes, attribute, null);
		if (val == null)
			throw new SAXException("missing attribute:" + attribute); //$NON-NLS-1$
		else {
			try {
				return Long.parseLong(val);
			} catch (NumberFormatException e) {
				throw new SAXException("not integer input for attribute: '" + attribute + "' was: '" + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

}
