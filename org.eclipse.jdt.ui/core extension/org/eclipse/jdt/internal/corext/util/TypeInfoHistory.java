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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

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
	
	private static class TypeHistoryDeltaListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			if (processDelta(event.getDelta())) {
				TypeInfoHistory.getInstance().markAsInconsistent();
			}
		}
		
		/**
		 * Computes whether the history needs a consistency check or not.
		 * 
		 * @param delta the Java element delta
		 * 
		 * @return <code>true</code> if consistency must be checked 
		 *  <code>false</code> otherwise.
		 */
		private boolean processDelta(IJavaElementDelta delta) {
			IJavaElement elem= delta.getElement();
			
			boolean isChanged= delta.getKind() == IJavaElementDelta.CHANGED;
			boolean isRemoved= delta.getKind() == IJavaElementDelta.REMOVED;
						
			switch (elem.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					if (isRemoved || (isChanged && 
							(delta.getFlags() & IJavaElementDelta.F_CLOSED) != 0)) {
						return true;
					}
					return processChildrenDelta(delta);
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					if (isRemoved || (isChanged && (
							(delta.getFlags() & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0 ||
							(delta.getFlags() & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0))) {
						return true;
					}
					return processChildrenDelta(delta);
				case IJavaElement.TYPE:
					if (isChanged && (delta.getFlags() & IJavaElementDelta.F_MODIFIERS) != 0) {
						return true;
					}
					// type children can be inner classes: fall through
				case IJavaElement.JAVA_MODEL:
				case IJavaElement.PACKAGE_FRAGMENT:
				case IJavaElement.CLASS_FILE:
					if (isRemoved) {
						return true;
					}				
					return processChildrenDelta(delta);
				case IJavaElement.COMPILATION_UNIT:
					// Not the primary compilation unit. Ignore it 
					if (!JavaModelUtil.isPrimary((ICompilationUnit) elem)) {
						return false;
					}

					if (isRemoved || (isChanged && isUnknownStructuralChange(delta.getFlags()))) {
						return true;
					}
					return processChildrenDelta(delta);
				default:
					// fields, methods, imports ect
					return false;
			}	
		}
		
		private boolean isUnknownStructuralChange(int flags) {
			if ((flags & IJavaElementDelta.F_CONTENT) == 0)
				return false;
			return (flags & IJavaElementDelta.F_FINE_GRAINED) == 0; 
		}

		/*
		private boolean isPossibleStructuralChange(int flags) {
			return (flags & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_FINE_GRAINED)) == IJavaElementDelta.F_CONTENT;
		}
		*/		
		
		private boolean processChildrenDelta(IJavaElementDelta delta) {
			IJavaElementDelta[] children= delta.getAffectedChildren();
			for (int i= 0; i < children.length; i++) {
				if (processDelta(children[i])) {
					return true;
				}
			}
			return false;
		}
	}
	
	private static final String NODE_ROOT= "typeInfoHistroy"; //$NON-NLS-1$
	private static final String NODE_TYPE_INFO= "typeInfo"; //$NON-NLS-1$
	private static final String NODE_NAME= "name"; //$NON-NLS-1$
	private static final String NODE_PACKAGE= "package"; //$NON-NLS-1$
	private static final String NODE_ENCLOSING_NAMES= "enclosingTypes"; //$NON-NLS-1$
	private static final String NODE_PATH= "path"; //$NON-NLS-1$
	private static final String NODE_MODIFIERS= "modifiers";  //$NON-NLS-1$
	
	private static final char[][] EMPTY_ENCLOSING_NAMES= new char[0][0];
	
	private Map fHistory= new LinkedHashMap(80, 0.75f, true) {
		private static final long serialVersionUID= 1L;
		protected boolean removeEldestEntry(Map.Entry eldest) {
			return size() > 60;
		}
	};
	
	private boolean fNeedsConsistencyCheck;
	private IElementChangedListener fDeltaListener;
	
	private static final String FILENAME= "TypeInfoHistory.xml"; //$NON-NLS-1$
	private static TypeInfoHistory fgInstance;
	
	public static synchronized TypeInfoHistory getInstance() {
		if (fgInstance == null)
			fgInstance= new TypeInfoHistory();
		return fgInstance;
	}
	
	public static void shutdown() {
		if (fgInstance == null)
			return;
		fgInstance.doShutdown();
		
	}
	
	private TypeInfoHistory() {
		fNeedsConsistencyCheck= true;
		fDeltaListener= new TypeHistoryDeltaListener();
		JavaCore.addElementChangedListener(fDeltaListener);
		load();
	}
	
	public synchronized void markAsInconsistent() {
		fNeedsConsistencyCheck= true;
	}
	
	public synchronized boolean needConsistencyCheck() {
		return fNeedsConsistencyCheck;
	}
	
	public synchronized boolean isEmpty() {
		return fHistory.isEmpty();
	}
	
	public synchronized boolean contains(TypeInfo type) {
		return fHistory.get(type) != null;
	}

	public synchronized void checkConsistency(IProgressMonitor monitor) {
		IJavaSearchScope scope= SearchEngine.createWorkspaceScope();
		List keys= new ArrayList(fHistory.keySet());
		monitor.beginTask(CorextMessages.TypeInfoHistory_consistency_check, keys.size());
		monitor.setTaskName(CorextMessages.TypeInfoHistory_consistency_check);
		for (Iterator iter= keys.iterator(); iter.hasNext();) {
			TypeInfo type= (TypeInfo)iter.next();
			try {
				IType jType= type.resolveType(scope);
				if (jType == null || !jType.exists()) {
					fHistory.remove(type);
				} else {
					// copy over the modifiers since they may have changed
					type.setModifiers(jType.getFlags());
				}
			} catch (JavaModelException e) {
				fHistory.remove(type);
			}
			monitor.worked(1);
		}
		monitor.done();
		fNeedsConsistencyCheck= false;
	}
	
	public synchronized void accessed(TypeInfo info) {
		fHistory.put(info, info);
	}
	
	public synchronized TypeInfo remove(TypeInfo info) {
		return (TypeInfo)fHistory.remove(info);
	}
	
	public synchronized TypeInfo[] getTypeInfos() {
		Collection values= fHistory.values();
		int size= values.size();
		TypeInfo[] result= new TypeInfo[size];
		int i= size - 1;
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			result[i]= (TypeInfo)iter.next();
			i--;
		}
		return result;
	}
	
	public synchronized TypeInfo[] getFilteredTypeInfos(TypeInfoFilter filter) {
		Collection values= fHistory.values();
		List result= new ArrayList();
		for (Iterator iter= values.iterator(); iter.hasNext();) {
			TypeInfo type= (TypeInfo)iter.next();
			if ((filter == null || filter.matchesHistoryElement(type)) && !TypeFilter.isFiltered(type.getFullyQualifiedName()))
				result.add(type);
		}
		Collections.reverse(result);
		return (TypeInfo[])result.toArray(new TypeInfo[result.size()]);
		
	}
	
	private void load() {
		IPath stateLocation= JavaPlugin.getDefault().getStateLocation().append(FILENAME);
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
	
	private void load(InputSource inputSource) throws CoreException {
		TypeInfoFactory factory= new TypeInfoFactory();
		Element root;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			root = parser.parse(inputSource).getDocumentElement();
		} catch (SAXException e) {
			throw createException(e, CorextMessages.TypeInfoHistory_error_read);  
		} catch (ParserConfigurationException e) {
			throw createException(e, CorextMessages.TypeInfoHistory_error_read); 
		} catch (IOException e) {
			throw createException(e, CorextMessages.TypeInfoHistory_error_read); 
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
					fHistory.put(info, info);
				}
			}
		}
	}
	
	public synchronized void save() {
		IPath stateLocation= JavaPlugin.getDefault().getStateLocation().append(FILENAME);
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
	
	private void save(OutputStream stream) throws CoreException {
		try {
			DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			DocumentBuilder builder= factory.newDocumentBuilder();		
			Document document= builder.newDocument();
			
			Element rootElement = document.createElement(NODE_ROOT);
			document.appendChild(rootElement);
	
			Iterator values= fHistory.values().iterator();
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
			throw createException(e, CorextMessages.TypeInfoHistory_error_serialize);
		} catch (ParserConfigurationException e) {
			throw createException(e, CorextMessages.TypeInfoHistory_error_serialize);
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
	
	private void doShutdown() {
		JavaCore.removeElementChangedListener(fDeltaListener);
	}
}