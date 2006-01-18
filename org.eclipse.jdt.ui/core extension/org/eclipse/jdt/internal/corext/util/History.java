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
package org.eclipse.jdt.internal.corext.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.corext.CorextMessages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * History stores a list of key, object pairs. The list is bounded at size
 * MAX_HISTORY_SIZE. If the list exceeds this size the eldest element is removed
 * from the list. An element can be added/renewed with a call to <code>accessed(Object)</code>. 
 * 
 * The history can be stored to/loaded from an xml file.
 */
public abstract class History {

	private static final String DEFAULT_ROOT_NODE_NAME= "histroyRootNode"; //$NON-NLS-1$
	private static final String DEFAULT_INFO_NODE_NAME= "infoNode"; //$NON-NLS-1$
	private static final int MAX_HISTORY_SIZE= 60;

	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}

	private final Map fHistory;
	private final String fFileName;
	private final String fRootNodeName;
	private final String fInfoNodeName;
		
	public History(String fileName, String rootNodeName, String infoNodeName) {
		fHistory= new LinkedHashMap(80, 0.75f, true) {
			private static final long serialVersionUID= 1L;
			protected boolean removeEldestEntry(Map.Entry eldest) {
				return size() > MAX_HISTORY_SIZE;
			}
		};
		fFileName= fileName;
		fRootNodeName= rootNodeName;
		fInfoNodeName= infoNodeName;
	}
	
	public History(String fileName) {
		this(fileName, DEFAULT_ROOT_NODE_NAME, DEFAULT_INFO_NODE_NAME);
	}
	
	public synchronized void accessed(Object object) {
		fHistory.put(getKey(object), object);
	}
	
	public synchronized boolean contains(Object object) {
		return fHistory.containsKey(getKey(object));
	}
	
	public synchronized boolean containsKey(Object key) {
		return fHistory.containsKey(key);
	}
	
	public synchronized boolean isEmpty() {
		return fHistory.isEmpty();
	}
	
	public synchronized Object remove(Object object) {
		return fHistory.remove(getKey(object));
	}
	
	public synchronized Object removeKey(Object key) {
		return fHistory.remove(key);
	}
	
	/**
	 * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer if o1 is newer then o2
     * 		   a positive integer if o1 is older then o2
     * 		   zero if o1 is equal to o2 or neither o1 nor o2 is 
     *         element of the history
	 */
	public synchronized int compare(Object o1, Object o2) {
		if (o1 == o2 || o1.equals(o2))
			return 0;
		
		if (contains(o1)) {
			if (contains(o2)) {
				Collection collection= getValues();
				for (Iterator iter= collection.iterator(); iter.hasNext();) {
					Object element= iter.next();
					if (element.equals(o1)) {
						return 1;
					} else if (element.equals(o2)) {
						return -1;
					}
				}
				return 0;
			} else {
				return -1;
			}
		} else if (contains(o2)) {
			return 1;
		} else {
			return 0;
		}
	}
	
	/**
	 * @param key1 the first object to be compared.
     * @param key2 the second object to be compared.
     * @return a negative integer if object denoted by key1 is newer then object denoted by key2
     * 		   a positive integer if object denoted by key1 is older then object denoted by key2
     * 		   zero if key is equal to key2 or neither object denoted by key1 nor object denoted 
     *         by key2 is element of the history
	 */
	public synchronized int compareByKeys(Object key1, Object key2) {
		if (key1 == key2 || key1.equals(key2))
			return 0;
		
		if (containsKey(key1)) {
			if (containsKey(key2)) {
				Collection collection= getValues();
				for (Iterator iter= collection.iterator(); iter.hasNext();) {
					Object element= getKey(iter.next());
					if (element.equals(key1)) {
						return 1;
					} else if (element.equals(key2)) {
						return -1;
					}
				}
				return 0;
			} else {
				return -1;
			}
		} else if (containsKey(key2)) {
			return 1;
		} else {
			return 0;
		}
	}

	public synchronized void load() {
		IPath stateLocation= JavaPlugin.getDefault().getStateLocation().append(fFileName);
		File file= new File(stateLocation.toOSString());
		if (file.exists()) {
			InputStreamReader reader= null;
	        try {
				reader = new InputStreamReader(new FileInputStream(file), "utf-8");//$NON-NLS-1$
				load(new InputSource(reader));
			} catch (IOException e) {
				JavaPlugin.log(e);
			} catch (CoreException e) {
				JavaPlugin.log(e);
			} finally {
				try {
					if (reader != null)
						reader.close();
				} catch (IOException e) {
					JavaPlugin.log(e);
				}
			}
		}
	}
	
	public synchronized void save() {
		IPath stateLocation= JavaPlugin.getDefault().getStateLocation().append(fFileName);
		File file= new File(stateLocation.toOSString());
		OutputStream out= null;
		try {
			out= new FileOutputStream(file); 
			save(out);
		} catch (IOException e) {
			JavaPlugin.log(e);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} catch (TransformerFactoryConfigurationError e) {
			// The XML library can be misconficgured (e.g. via 
			// -Djava.endorsed.dirs=C:\notExisting\xerces-2_7_1)
			JavaPlugin.log(e);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				JavaPlugin.log(e);
			}
		}
	}
	
	protected Set getKeys() {
		return fHistory.keySet();
	}
	
	protected Collection getValues() {
		return fHistory.values();
	}
	
	/**
	 * Store <code>Object</code> in <code>Element</code>
	 * 
	 * @param object The object to store
	 * @param element The Element to store to
	 */
	protected abstract void setAttributes(Object object, Element element);
	
	/**
	 * Return a new instance of an Object given <code>element</code>
	 * 
	 * @param element The element containing required information to create the Object
	 */
	protected abstract Object createFromElement(Element element);
	
	/**
	 * Get key for object
	 * 
	 * @param object The object to calculate a key for, not null
	 * @return The key for object, not null
	 */
	protected abstract Object getKey(Object object);

	private void load(InputSource inputSource) throws CoreException {
		Element root;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			root = parser.parse(inputSource).getDocumentElement();
		} catch (SAXException e) {
			throw createException(e, Messages.format(CorextMessages.History_error_read, fFileName));  
		} catch (ParserConfigurationException e) {
			throw createException(e, Messages.format(CorextMessages.History_error_read, fFileName)); 
		} catch (IOException e) {
			throw createException(e, Messages.format(CorextMessages.History_error_read, fFileName)); 
		}
		
		if (root == null) return;
		if (!root.getNodeName().equalsIgnoreCase(fRootNodeName)) {
			return;
		}
		NodeList list= root.getChildNodes();
		int length= list.getLength();
		for (int i= 0; i < length; ++i) {
			Node node= list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element type= (Element) node;
				if (type.getNodeName().equalsIgnoreCase(fInfoNodeName)) {
					Object object= createFromElement(type);
					accessed(object);
				}
			}
		}
	}
	
	private void save(OutputStream stream) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();		
			Document document= builder.newDocument();
			
			Element rootElement = document.createElement(fRootNodeName);
			document.appendChild(rootElement);
	
			Iterator values= getValues().iterator();
			while (values.hasNext()) {
				Object object= values.next();
				Element element= document.createElement(fInfoNodeName);
				setAttributes(object, element);
				rootElement.appendChild(element);
			}
			
			Transformer transformer=TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(stream);

			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw createException(e, Messages.format(CorextMessages.History_error_serialize, fFileName));
		} catch (ParserConfigurationException e) {
			throw createException(e, Messages.format(CorextMessages.History_error_serialize, fFileName));
		}
	}

}
