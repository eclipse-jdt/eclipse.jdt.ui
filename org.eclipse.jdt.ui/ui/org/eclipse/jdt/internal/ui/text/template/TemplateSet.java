/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.SerializerFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A user defined template set.
 */
public class TemplateSet {
	
	private static class TemplateComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			if (arg0 == arg1)
				return 0;
			
			if (arg0 == null)
				return -1;
				
			Template template0= (Template) arg0;
			Template template1= (Template) arg1;
			
			return template0.getName().compareTo(template1.getName());
		}
	}

	private static final String DEFAULT_FILE= "default-templates.xml"; //$NON-NLS-1$
	private static final String TEMPLATE_FILE= "templates.xml"; //$NON-NLS-1$
	private static final String TEMPLATE_TAG= "template"; //$NON-NLS-1$
	private static final String NAME_ATTRIBUTE= "name"; //$NON-NLS-1$
	private static final String DESCRIPTION_ATTRIBUTE= "description"; //$NON-NLS-1$
	private static final String CONTEXT_ATTRIBUTE= "context"; //$NON-NLS-1$

	private List fTemplates= new ArrayList();
	private Comparator fTemplateComparator= new TemplateComparator();
	private Template[] fSortedTemplates= new Template[0];
	
	private static TemplateSet fgTemplateSet;

	public static TemplateSet getInstance() {
		if (fgTemplateSet == null)
			fgTemplateSet= create();
		
		return fgTemplateSet;
	}

	private static InputStream getDefaultsAsStream() {
		return TemplateSet.class.getResourceAsStream(DEFAULT_FILE);
	}

	private static File getTemplateFile() {
		IPath path= JavaPlugin.getDefault().getStateLocation();
		path= path.append(TEMPLATE_FILE);
		
		return path.toFile();
	}
	
	private TemplateSet() {
	}

	private static TemplateSet create() {
		try {			
			File templateFile= getTemplateFile();			

			if (!templateFile.exists()) {
				InputStream inputStream= getDefaultsAsStream();
				
				if (inputStream == null)
					return new TemplateSet();					

				if (!templateFile.createNewFile())
					return new TemplateSet();

				OutputStream outputStream= new FileOutputStream(templateFile);
			
				// copy over default templates				
				byte buffer[]= new byte[65536];
				while (true) {
					int bytes= inputStream.read(buffer);
					if (bytes == -1)
						break;
					outputStream.write(buffer, 0, bytes);
				}
			
				inputStream.close();
				outputStream.close();
			}
			
			Assert.isTrue(templateFile.exists());

			TemplateSet templateSet= new TemplateSet();
			templateSet.addFromStream(new FileInputStream(templateFile));
			return templateSet;

		} catch (IOException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	/**
	 * Resets the template set with the default templates.
	 */
	public void restoreDefaults() {
		clear();
		addFromStream(getDefaultsAsStream());
	}
	
	/**
	 * Resets (reloads) the template set.
	 */
	public void reset() {
		clear();
		try {
			addFromStream(new FileInputStream(getTemplateFile()));
		} catch (FileNotFoundException e) {
			JavaPlugin.log(e);
		}
	}

	private boolean addFromStream(InputStream stream) {
		try {
			TemplateSet templateSet= new TemplateSet();
			
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder parser= factory.newDocumentBuilder();		
			Document document= parser.parse(new InputSource(stream));
			NodeList elements= document.getElementsByTagName(TEMPLATE_TAG);
			
			int count= elements.getLength();
			for (int i= 0; i != count; i++) {
				Node node= elements.item(i);					
				NamedNodeMap attributes= node.getAttributes();

				if (attributes == null)
					continue;

				String name= attributes.getNamedItem(NAME_ATTRIBUTE).getNodeValue();
				String description= attributes.getNamedItem(DESCRIPTION_ATTRIBUTE).getNodeValue();
				String context= attributes.getNamedItem(CONTEXT_ATTRIBUTE).getNodeValue();

				StringBuffer buffer= new StringBuffer();
				NodeList children= node.getChildNodes();
				for (int j= 0; j != children.getLength(); j++) {
					String value= children.item(j).getNodeValue();
					if (value != null)
						buffer.append(value);
				}
				String pattern= buffer.toString().trim();
					
				add(new Template(name, description, context, pattern));
			}

			return true;
			
		} catch (ParserConfigurationException e) {
			JavaPlugin.log(e);
		} catch (IOException e) {
			JavaPlugin.log(e);
		} catch (SAXException e) {
			JavaPlugin.log(e);
		}
		
		sort();
		
		return false;
	}

	/**
	 * Saves the template set.
	 */
	public boolean save() {					
		try {
			fgTemplateSet.save(new FileOutputStream(getTemplateFile()));
			return true;
		} catch (FileNotFoundException e) {
			JavaPlugin.log(e);
			return false;
		}
	}

	/**
	 * Saves the template set as XML.
	 */
	private boolean save(OutputStream stream) {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();		
			Document document= builder.newDocument();

			Node root= document.createElement("templates"); // $NON-NLS-1$
			document.appendChild(root);
			
			for (int i= 0; i != fTemplates.size(); i++) {
				Template template= (Template) fTemplates.get(i);
				
				Node node= document.createElement("template"); // $NON-NLS-1$
				root.appendChild(node);
				
				NamedNodeMap attributes= node.getAttributes();
				
				Attr name= document.createAttribute(NAME_ATTRIBUTE);
				name.setValue(template.getName());
				attributes.setNamedItem(name);
	
				Attr description= document.createAttribute(DESCRIPTION_ATTRIBUTE);
				description.setValue(template.getDescription());
				attributes.setNamedItem(description);
	
				Attr context= document.createAttribute(CONTEXT_ATTRIBUTE);
				context.setValue(template.getContext());
				attributes.setNamedItem(context);			
				
				Text pattern= document.createTextNode(template.getPattern());
				node.appendChild(pattern);			
			}		
			
			OutputFormat format = new OutputFormat();
			format.setPreserveSpace(true);
			Serializer serializer = SerializerFactory.getSerializerFactory("xml").makeSerializer(stream, format); //$NON-NLS-1$
			serializer.asDOMSerializer().serialize(document);
			
			return true;
			
		} catch (ParserConfigurationException e) {
			JavaPlugin.log(e);
		} catch (IOException e) {
			JavaPlugin.log(e);
		}		

		return false;
	}

	/**
	 * Adds a template to the set.
	 */
	public void add(Template template) {
		fTemplates.add(template);
		sort();
	}
	
	/**
	 * Removes a template to the set.
	 */	
	public void remove(Template template) {
		fTemplates.remove(template);
		sort();
	}

	/**
	 * Empties the set.
	 */		
	public void clear() {
		fTemplates.clear();
		sort();
	}
	
	/**
	 * Returns all templates.
	 */
	public Template[] getTemplates() {
		return (Template[]) fTemplates.toArray(new Template[fTemplates.size()]);
	}
	
	/**
	 * Returns templates matching a prefix.
	 */
	public Template[] getMatchingTemplates(String prefix, String partitionType) {
		Assert.isNotNull(prefix);
		Assert.isNotNull(partitionType);

		List results= new ArrayList(fSortedTemplates.length);

		for (int i= 0; i != fSortedTemplates.length; i++) {
			Template template= fSortedTemplates[i];

			if (template.matches(prefix, partitionType))
				results.add(template);							
		}

		return (Template[]) results.toArray(new Template[results.size()]);
	}

	private void sort() {
		fSortedTemplates= (Template[]) fTemplates.toArray(new Template[fTemplates.size()]);
		Arrays.sort(fSortedTemplates, fTemplateComparator);
	}
	
}

