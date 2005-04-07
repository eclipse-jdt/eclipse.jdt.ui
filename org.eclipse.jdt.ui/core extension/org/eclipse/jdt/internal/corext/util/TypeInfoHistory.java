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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.IDialogSettings;

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

public class TypeInfoHistory {
	
	private static final String HISTORY_SETTINGS= "histroy";  //$NON-NLS-1$
	
	private static final String NODE_ROOT= "typeInfoHistroy"; //$NON-NLS-1$
	private static final String NODE_TYPE_INFO= "typeInfo"; //$NON-NLS-1$
	private static final String NODE_NAME= "name"; //$NON-NLS-1$
	private static final String NODE_PACKAGE= "package"; //$NON-NLS-1$
	private static final String NODE_ENCLOSING_NAMES= "enclosingTypes"; //$NON-NLS-1$
	private static final String NODE_PATH= "path"; //$NON-NLS-1$
	private static final String NODE_MODIFIERS= "modifiers";  //$NON-NLS-1$
	
	private static final char[][] EMPTY_ENCLOSING_NAMES= new char[0][0];
	
	private Map fHistroy= new LinkedHashMap(80, 0.75f, true) {
		private static final long serialVersionUID= 1L;
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > 60;
		}
	};
	
	public TypeInfoHistory(IDialogSettings settings) {
		load(settings);
	}
	
	public void accessed(TypeInfo info) {
		fHistroy.put(info, info);
	}
	
	public TypeInfo[] getTypeInfos() {
		Collection values= fHistroy.values();
		int size= values.size();
		TypeInfo[] result= new TypeInfo[size];
		int i= size - 1;
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			result[i]= (TypeInfo)iter.next();
			i--;
		}
		return result;
	}
	
	public void save(IDialogSettings settings) {
		ByteArrayOutputStream stream= new ByteArrayOutputStream(2000);
		try {
			save(stream);
			byte[] bytes= stream.toByteArray();
			String val;
			try {
				val= new String(bytes, "UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				val= new String(bytes);
			}
			settings.put(HISTORY_SETTINGS, val);
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				//	error closing reader: ignore
			}
		}
	}
	
	private void save(OutputStream stream) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();		
			Document document= builder.newDocument();
			
			Element rootElement = document.createElement(NODE_ROOT);
			document.appendChild(rootElement);
	
			Iterator values= fHistroy.values().iterator();
			while (values.hasNext()) {
				TypeInfo type= (TypeInfo)values.next();
				Element typeElement= document.createElement(NODE_TYPE_INFO);
				typeElement.setAttribute(NODE_NAME, type.getTypeName());
				typeElement.setAttribute(NODE_PACKAGE, type.getPackageName());
				typeElement.setAttribute(NODE_ENCLOSING_NAMES, type.getEnclosingName());
				typeElement.setAttribute(NODE_PATH, type.getPath());
				typeElement.setAttribute(NODE_MODIFIERS, Integer.toString(type.getModifiers()));
				rootElement.appendChild(typeElement);
			}
			
			Transformer transformer=TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(stream);

			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw createException(e, CorextMessages.getString("TypeInfoHistory.error.serialize")); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw createException(e, CorextMessages.getString("TypeInfoHistory.error.serialize")); //$NON-NLS-1$
		}
	}
	
	private void load(IDialogSettings settings) {
		String string= settings.get(HISTORY_SETTINGS);
		if (string != null && string.length() > 0) {
			byte[] bytes;
			try {
				bytes= string.getBytes("UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				bytes= string.getBytes();
			}
			InputStream is= new ByteArrayInputStream(bytes);
			try {
				load(new InputSource(is));
			} catch (CoreException e) {
				JavaPlugin.log(e);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	
	private void load(InputSource inputSource) throws CoreException {
		TypeInfoFactory factory= new TypeInfoFactory();
		Element root;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			root = parser.parse(inputSource).getDocumentElement();
		} catch (SAXException e) {
			throw createException(e, CorextMessages.getString("TypeInfoHistory.error.read"));  //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw createException(e, CorextMessages.getString("TypeInfoHistory.error.read")); //$NON-NLS-1$
		} catch (IOException e) {
			throw createException(e, CorextMessages.getString("TypeInfoHistory.error.read")); //$NON-NLS-1$
		}
		
		if (root == null) return;
		if (!root.getNodeName().equalsIgnoreCase(NODE_ROOT)) {
			return;
		}
		NodeList list= root.getChildNodes();
		int length= list.getLength();
		for (int i= 0; i < length; ++i) {
			Node node= list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element type= (Element) node;
				if (type.getNodeName().equalsIgnoreCase(NODE_TYPE_INFO)) {
					String name= type.getAttribute(NODE_NAME);
					String pack= type.getAttribute(NODE_PACKAGE);
					char[][] enclosingNames= getEnclosingNames(type);
					String path= type.getAttribute(NODE_PATH);
					int modifiers= 0;
					try {
						modifiers= Integer.parseInt(type.getAttribute(NODE_MODIFIERS));
					} catch (NumberFormatException e) {
						// take zero
					}
					TypeInfo info= factory.create(
						pack.toCharArray(), name.toCharArray(), enclosingNames, modifiers, path);
					fHistroy.put(info, info);
				}
			}
		}
	}
	
	private char[][] getEnclosingNames(Element type) {
		String enclosingNames= type.getAttribute(NODE_ENCLOSING_NAMES);
		if (enclosingNames.length() == 0)
			return EMPTY_ENCLOSING_NAMES;
		StringTokenizer tokenizer= new StringTokenizer(enclosingNames, "."); //$NON-NLS-1$
		List names= new ArrayList();
		while(tokenizer.hasMoreTokens()) {
			String name= tokenizer.nextToken();
			names.add(name.toCharArray());
		}
		return (char[][])names.toArray(new char[names.size()][]);
	}

	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}	
}