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

package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;



public class ProfileStore {
	
	/**
	 * A SAX event handler to parse the xml format for profiles. 
	 */
	private final static class ProfileDefaultHandler extends DefaultHandler {
		
		private List fProfiles;
		private int fVersion;
		
		private String fName;
		private Map fSettings;


		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

			if (qName.equals(XML_NODE_SETTING)) {

				final String key= attributes.getValue(XML_ATTRIBUTE_ID);
				final String value= attributes.getValue(XML_ATTRIBUTE_VALUE);
				fSettings.put(key, value);

			} else if (qName.equals(XML_NODE_PROFILE)) {

				fName= attributes.getValue(XML_ATTRIBUTE_NAME);
				fSettings= new HashMap(200);

			}
			else if (qName.equals(XML_NODE_ROOT)) {

				fProfiles= new ArrayList();
				try {
					fVersion= Integer.parseInt(attributes.getValue(XML_ATTRIBUTE_VERSION));
				} catch (NumberFormatException ex) {
					throw new SAXException(ex);
				}

			}
		}
		
		public void endElement(String uri, String localName, String qName) {
			if (qName.equals(XML_NODE_PROFILE)) {
				fProfiles.add(new CustomProfile(fName, fSettings, fVersion));
				fName= null;
				fSettings= null;
			}
		}
		
		public List getProfiles() {
			return fProfiles;
		}
		
	}

	/**
	 * Identifiers for the XML file.
	 */
	private final static String XML_NODE_ROOT= "profiles"; //$NON-NLS-1$
	private final static String XML_NODE_PROFILE= "profile"; //$NON-NLS-1$
	private final static String XML_NODE_SETTING= "setting"; //$NON-NLS-1$
	
	private final static String XML_ATTRIBUTE_VERSION= "version"; //$NON-NLS-1$
	private final static String XML_ATTRIBUTE_ID= "id"; //$NON-NLS-1$
	private final static String XML_ATTRIBUTE_NAME= "name"; //$NON-NLS-1$
	private final static String XML_ATTRIBUTE_VALUE= "value"; //$NON-NLS-1$
	
	private final File fStoreFile;
	
	public ProfileStore(File storeFile) {
		fStoreFile= storeFile;
	}
	
	public List readProfiles() throws CoreException, IOException {
		return readProfilesFromFile(fStoreFile);
	}
	
	public void writeProfiles(Collection profiles) throws CoreException, IOException {
		writeProfilesToFile(profiles, fStoreFile);
	}
	

	/**
	 * Read the available profiles from the internal XML file and return them
	 * as collection.
	 */
	public static List readProfilesFromFile(File file) throws CoreException, IOException {
		
		if (!file.exists())
			return null;
		
		final InputStream stream= new FileInputStream(file);
		try {
			return readProfilesFromStream(stream);
		} finally {
			stream.close();
		}
	}
	
	
	/**
	 * Load profiles from a XML stream and add them to a map.
	 */
	private static List readProfilesFromStream(InputStream stream) throws CoreException {
		
		final ProfileDefaultHandler handler= new ProfileDefaultHandler();
		try {
		    final SAXParserFactory factory= SAXParserFactory.newInstance();
			final SAXParser parser= factory.newSAXParser();
			parser.parse(new InputSource(stream), handler);
		} catch (Exception ex) {
			throw createException(ex, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		}
		return handler.getProfiles();
	}
	

	
	/**
	 * Write the available profiles to the internal XML file.
	 */
	public static void writeProfilesToFile(Collection profiles, File file) throws IOException, CoreException {
		final OutputStream writer= new FileOutputStream(file);
		try {
			writeProfilesToStream(profiles, writer);
		} finally {
			writer.close();
		}
	}

	
	/**
	 * Save profiles to an XML stream
	 */
	private static void writeProfilesToStream(Collection profiles, OutputStream stream) throws CoreException {

		try {
			final DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder= factory.newDocumentBuilder();		
			final Document document= builder.newDocument();
			
			final Element rootElement = document.createElement(XML_NODE_ROOT);
			rootElement.setAttribute(XML_ATTRIBUTE_VERSION, Integer.toString(ProfileVersioner.CURRENT_VERSION));

			document.appendChild(rootElement);
			
			for(final Iterator iter= profiles.iterator(); iter.hasNext();) {
				final Profile profile= (Profile)iter.next();
				if (profile instanceof CustomProfile) {
					final Element profileElement= createProfileElement((CustomProfile)profile, document);
					rootElement.appendChild(profileElement);
				}
			}

			Transformer transformer=TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			transformer.transform(new DOMSource(document), new StreamResult(stream));
		} catch (Exception e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.serializing_xml.message"));  //$NON-NLS-1$
		}
	}

	
	/**
	 * Create a new profile element in the specified document. The profile is not added
	 * to the document by this method. 
	 */
	private static Element createProfileElement(CustomProfile profile, Document document) {
		final Element element= document.createElement(XML_NODE_PROFILE);
		element.setAttribute(XML_ATTRIBUTE_NAME, profile.getName());
		element.setAttribute(XML_ATTRIBUTE_VERSION, Integer.toString(profile.getVersion()));
		
		final Iterator keyIter= ProfileManager.getKeys().iterator();
		
		while (keyIter.hasNext()) {
			final String key= (String)keyIter.next();
			final String value= (String)profile.getSettings().get(key);
			final Element setting= document.createElement(XML_NODE_SETTING);
			setting.setAttribute(XML_ATTRIBUTE_ID, key);
			setting.setAttribute(XML_ATTRIBUTE_VALUE, value);
			element.appendChild(setting);
		}
		return element;
	}
	
		
	/**
	 * Creates a UI exception for logging purposes
	 */
	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}
}
