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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
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
	 * Preference key where all profiles are stored
	 */
	private static final String PREF_FORMATTER_PROFILES= "org.eclipse.jdt.ui.formatterprofiles"; //$NON-NLS-1$
	
	/**
	 * Preference key where all profiles are stored
	 */
	private static final String PREF_FORMATTER_PROFILES_VERSION= "org.eclipse.jdt.ui.formatterprofiles.version"; //$NON-NLS-1$
	
	
	
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
		
	private ProfileStore() {
	}
	
	/**
	 * @return Returns the collection of profiles currently stored in the preference store or
	 * <code>null</code> if the loading failed. The elements are of type {@link CustomProfile}
	 * and are all updated to the latest version.
	 * @throws CoreException
	 */
	public static List readProfiles() throws CoreException {
		List res= readProfilesFromPreferences(PREF_FORMATTER_PROFILES);
		if (res == null) {
			return readOldForCompatibility();
		}
		return res;
	}
	
	public static void writeProfiles(Collection profiles) throws CoreException {
		ByteArrayOutputStream stream= new ByteArrayOutputStream(2000);
		try {
			writeProfilesToStream(profiles, stream);
			String val;
			try {
				val= stream.toString("UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				val= stream.toString(); //$NON-NLS-1$
			}
			IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
			preferenceStore.setValue(PREF_FORMATTER_PROFILES, val);
			preferenceStore.setValue(PREF_FORMATTER_PROFILES_VERSION, ProfileVersioner.CURRENT_VERSION);
			JavaPlugin.getDefault().savePluginPreferences();
		} finally {
			try { stream.close(); } catch (IOException e) { /* ignore */ }
		}
	}
	
	private static List readProfilesFromPreferences(String key) throws CoreException {
		String string= PreferenceConstants.getPreferenceStore().getString(key);
		if (string != null && string.length() > 0) {
			byte[] bytes;
			try {
				bytes= string.getBytes("UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				bytes= string.getBytes();
			}
			InputStream is= new ByteArrayInputStream(bytes);
			try {
				List res= readProfilesFromStream(new InputSource(is));
				if (res != null) {
					for (int i= 0; i < res.size(); i++) {
						ProfileVersioner.updateAndComplete((CustomProfile) res.get(i));
					}
				}
				return res;
			} finally {
				try { is.close(); } catch (IOException e) { /* ignore */ }
			}
		}
		return null;
	}	

	/**
	 * Read the available profiles from the internal XML file and return them
	 * as collection.
	 */
	private static List readOldForCompatibility() {
		
		// in 3.0 M9 and less the profiles were stored in a file in the plugin's meta data
		final String STORE_FILE= "code_formatter_profiles.xml"; //$NON-NLS-1$

		File file= JavaPlugin.getDefault().getStateLocation().append(STORE_FILE).toFile();
		if (!file.exists())
			return null;
		
		try {
			// note that it's wrong to use a file reader when XML declares UTF-8: Kept for compatibility
			final FileReader reader= new FileReader(file);
			try {
				List res= readProfilesFromStream(new InputSource(reader));
				if (res != null) {
					for (int i= 0; i < res.size(); i++) {
						ProfileVersioner.updateAndComplete((CustomProfile) res.get(i));
					}
					writeProfiles(res);
				}
				file.delete(); // remove after successful write
				return res;
			} finally {
				reader.close();
			}
		} catch (CoreException e) {
			JavaPlugin.log(e); // log but ignore
		} catch (IOException e) {
			JavaPlugin.log(e); // log but ignore
		}
		return null;
	}
	
	
	/**
	 * Read the available profiles from the internal XML file and return them
	 * as collection or <code>null</code> if the file is not a profile file.
	 */
	public static List readProfilesFromFile(File file) throws CoreException {
		try {
			final FileInputStream reader= new FileInputStream(file); 
			try {
				return readProfilesFromStream(new InputSource(reader));
			} finally {
				try { reader.close(); } catch (IOException e) { /* ignore */ }
			}
		} catch (IOException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		}
	}
	
	/**
	 * Load profiles from a XML stream and add them to a map or <code>null</code> if the source is not a profile store.
	 */
	private static List readProfilesFromStream(InputSource inputSource) throws CoreException {
		
		final ProfileDefaultHandler handler= new ProfileDefaultHandler();
		try {
		    final SAXParserFactory factory= SAXParserFactory.newInstance();
			final SAXParser parser= factory.newSAXParser();
			parser.parse(inputSource, handler);
		} catch (SAXException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		} catch (IOException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		}
		return handler.getProfiles();
	}
	
	/**
	 * Write the available profiles to the internal XML file.
	 */
	public static void writeProfilesToFile(Collection profiles, File file) throws CoreException {
		final OutputStream writer;
		try {
			writer= new FileOutputStream(file);
			try {
				writeProfilesToStream(profiles, writer);
			} finally {
				try { writer.close(); } catch (IOException e) { /* ignore */ }
			}
		} catch (IOException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.serializing_xml.message"));  //$NON-NLS-1$
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
		} catch (TransformerException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.serializing_xml.message"));  //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
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
			if (value != null) {
				final Element setting= document.createElement(XML_NODE_SETTING);
				setting.setAttribute(XML_ATTRIBUTE_ID, key);
				setting.setAttribute(XML_ATTRIBUTE_VALUE, value);
				element.appendChild(setting);
			} else {
				JavaPlugin.logErrorMessage("ProfileStore: Profile does not contain value for key " + key); //$NON-NLS-1$
			}
		}
		return element;
	}
	
	public static void checkCurrentOptionsVersion() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		int version= store.getInt(PREF_FORMATTER_PROFILES_VERSION);
		if (version >= ProfileVersioner.CURRENT_VERSION) {
			return; // is up to date
		}
		try {
			List profiles= ProfileStore.readProfiles();
			if (profiles != null && !profiles.isEmpty()) {
				ProfileManager manager= new ProfileManager(profiles);
				Profile selected= manager.getSelected();
				if (selected instanceof CustomProfile) {
					manager.commitChanges(); // updates JavaCore options
				}
			}
			store.setValue(PREF_FORMATTER_PROFILES_VERSION, ProfileVersioner.CURRENT_VERSION);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
	}
	
		
	/**
	 * Creates a UI exception for logging purposes
	 */
	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}
}
