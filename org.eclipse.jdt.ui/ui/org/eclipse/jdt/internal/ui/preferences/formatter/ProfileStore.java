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

package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;



public class ProfileStore {
	

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
		
		final Reader reader= new FileReader(file);
		try {
			return readProfilesFromStream(reader);
		} finally {
			reader.close();
		}
	}
	
	
	/**
	 * Load profiles from a XML stream and add them to a map.
	 */
	private static List readProfilesFromStream(Reader reader) throws CoreException {
		
		Element element;
		try {
		    final DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			final DocumentBuilder parser = factory.newDocumentBuilder();
			element = parser.parse(new InputSource(reader)).getDocumentElement();
		} catch (Exception ex) {
			throw createException(ex, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		}

		if (element == null || !element.getNodeName().equalsIgnoreCase(XML_NODE_ROOT)) {
		    throw createException(new Exception(), FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message")); //$NON-NLS-1$
		}
		
		int version;
		try {
			version= Integer.parseInt(element.getAttribute(XML_ATTRIBUTE_VERSION));
		} catch (NumberFormatException ex) {
			throw createException(ex, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message")); //$NON-NLS-1$
		}
		
		final NodeList list= element.getChildNodes();
		
		final int length= list.getLength();
		
		final List profiles= new ArrayList();

		for (int i= 0; i < length; i++) {
			final Node node= list.item(i);
			final short type= node.getNodeType();
			if (type != Node.ELEMENT_NODE)
			    continue; // white space 
			final Element profileElement= (Element) node;
			if (!profileElement.getNodeName().equalsIgnoreCase(XML_NODE_PROFILE)) {
			    throw createException(null, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message")); //$NON-NLS-1$
			}	
			final Map settings= getSettingsFromElement(profileElement);
			final String name= profileElement.getAttribute(XML_ATTRIBUTE_NAME);
			final CustomProfile profile= new CustomProfile(name, settings, version);
			profiles.add(profile);
		}
		return profiles;
	}
	


	/**
	 * Create a new custom profile from its XML description.
	 */
	private static Map getSettingsFromElement(final Element element) throws CoreException {

		final Map settings= new HashMap();
		final NodeList list= element.getChildNodes();
		
		for (int i= 0; i < list.getLength(); i++) {
		    
			final Node node= list.item(i);
			
			if (node.getNodeType() != Node.ELEMENT_NODE)
			    continue; // white space
			
			final Element setting= (Element) node;
			
			if (!setting.getNodeName().equalsIgnoreCase(XML_NODE_SETTING)) {
			    throw createException(null, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message")); //$NON-NLS-1$
			}	
			
			final String id= setting.getAttribute(XML_ATTRIBUTE_ID);
			final String value= setting.getAttribute(XML_ATTRIBUTE_VALUE);
			
			settings.put(id, value);
		}
		return settings;
	}
	
	
	
	
	
	/**
	 * Write the available profiles to the internal XML file.
	 */
	public static void writeProfilesToFile(Collection profiles, File file) throws IOException, CoreException {
		final Writer writer= new FileWriter(file);
		try {
			writeProfilesToStream(profiles, writer);
		} finally {
			writer.close();
		}
	}

	
	/**
	 * Save profiles to an XML stream
	 */
	private static void writeProfilesToStream(Collection profiles, Writer writer) throws CoreException {
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
			transformer.transform(new DOMSource(document), new StreamResult(writer));
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
