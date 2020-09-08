/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Hashtable;
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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.corext.CorextMessages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

/**
 * History stores a list of key, object pairs. The list is bounded at size
 * MAX_HISTORY_SIZE. If the list exceeds this size the eldest element is removed
 * from the list. An element can be added/renewed with a call to <code>accessed(Object)</code>.
 *
 * The history can be stored to/loaded from an xml file.
 *
 * @param <K> key type
 * @param <V> value type
 */
public abstract class History<K, V> {

	private static final String DEFAULT_ROOT_NODE_NAME= "histroyRootNode"; //$NON-NLS-1$
	private static final String DEFAULT_INFO_NODE_NAME= "infoNode"; //$NON-NLS-1$
	private static final int MAX_HISTORY_SIZE= 60;

	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}

	private final Map<K, V> fHistory;
	private final Hashtable<K, Integer> fPositions;
	private final String fFileName;
	private final String fRootNodeName;
	private final String fInfoNodeName;

	public History(String fileName, String rootNodeName, String infoNodeName) {
		fHistory= new LinkedHashMap<K, V>(80, 0.75f, true) {
			private static final long serialVersionUID= 1L;
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > MAX_HISTORY_SIZE;
			}
		};
		fFileName= fileName;
		fRootNodeName= rootNodeName;
		fInfoNodeName= infoNodeName;
		fPositions= new Hashtable<>(MAX_HISTORY_SIZE);
	}

	public History(String fileName) {
		this(fileName, DEFAULT_ROOT_NODE_NAME, DEFAULT_INFO_NODE_NAME);
	}

	public synchronized void accessed(V object) {
		fHistory.put(getKey(object), object);
		rebuildPositions();
	}

	public synchronized boolean contains(V object) {
		return fHistory.containsKey(getKey(object));
	}

	public synchronized boolean containsKey(K key) {
		return fHistory.containsKey(key);
	}

	public synchronized boolean isEmpty() {
		return fHistory.isEmpty();
	}

	public synchronized Object remove(V object) {
		Object removed= fHistory.remove(getKey(object));
		rebuildPositions();
		return removed;
	}

	public synchronized Object removeKey(Object key) {
		Object removed= fHistory.remove(key);
		rebuildPositions();
		return removed;
	}

	/**
	 * Normalized position in history of object denoted by key.
	 * The position is a value between zero and one where zero
	 * means not contained in history and one means newest element
	 * in history. The lower the value the older the element.
	 *
	 * @param key The key of the object to inspect
	 * @return value in [0.0, 1.0] the lower the older the element
	 */
	public synchronized float getNormalizedPosition(K key) {
		if (!containsKey(key))
			return 0.0f;

		int pos= fPositions.get(key).intValue() + 1;

		//containsKey(key) implies fHistory.size()>0
		return (float)pos / (float)fHistory.size();
	}

	/**
	 * Absolute position of object denoted by key in the
	 * history or -1 if !containsKey(key). The higher the
	 * newer.
	 *
	 * @param key The key of the object to inspect
	 * @return value between 0 and MAX_HISTORY_SIZE - 1, or -1
	 */
	public synchronized int getPosition(K key) {
		if (!containsKey(key))
			return -1;

		return fPositions.get(key);
	}

	public synchronized void load() {
		IPath stateLocation= JavaPlugin.getDefault().getStateLocation().append(fFileName);
		File file= stateLocation.toFile();
		if (file.exists()) {
			InputStreamReader reader= null;
	        try {
				reader = new InputStreamReader(new FileInputStream(file), "utf-8");//$NON-NLS-1$
				load(new InputSource(reader));
			} catch (IOException | CoreException e) {
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
		File file= stateLocation.toFile();
		OutputStream out= null;
		try {
			out= new FileOutputStream(file);
			save(out);
		} catch (IOException | CoreException | TransformerFactoryConfigurationError e) {
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

	protected Set<K> getKeys() {
		return fHistory.keySet();
	}

	protected Collection<V> getValues() {
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
	 * @return return a new instance of an Object given <code>element</code>
	 */
	protected abstract V createFromElement(Element element);

	/**
	 * Get key for object
	 *
	 * @param object The object to calculate a key for, not null
	 * @return The key for object, not null
	 */
	protected abstract K getKey(V object);

	private void rebuildPositions() {
		fPositions.clear();
		int pos=0;
		for (V element : fHistory.values()) {
			fPositions.put(getKey(element), pos);
			pos++;
		}
	}

	private void load(InputSource inputSource) throws CoreException {
		Element root;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			root = parser.parse(inputSource).getDocumentElement();
		} catch (SAXException | ParserConfigurationException | IOException e) {
			throw createException(e, Messages.format(CorextMessages.History_error_read, BasicElementLabels.getResourceName(fFileName)));
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
					V object= createFromElement(type);
					if (object != null) {
						fHistory.put(getKey(object), object);
					}
				}
			}
		}
		rebuildPositions();
	}

	private void save(OutputStream stream) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();
			Document document= builder.newDocument();

			Element rootElement = document.createElement(fRootNodeName);
			document.appendChild(rootElement);

			Iterator<V> values= getValues().iterator();
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
		} catch (TransformerException | ParserConfigurationException e) {
			throw createException(e, Messages.format(CorextMessages.History_error_serialize, BasicElementLabels.getResourceName(fFileName)));
		}
	}

}
