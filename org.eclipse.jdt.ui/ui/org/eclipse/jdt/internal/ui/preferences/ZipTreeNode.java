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
package org.eclipse.jdt.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *  Tree representing the structure of a zip file
 */
public class ZipTreeNode {
	private ZipTreeNode fParent;
	private List fChildren;
	private String fName;
	private boolean fRepresentsZip= false;
	private static final String SEGMENT_SEPARATOR= "/"; //$NON-NLS-1$

	private ZipTreeNode(ZipTreeNode parent, String name) {
		fParent= parent;
		fChildren= new ArrayList(0);
		fName= name;
	}

	public String getName() {
		return fName;
	}

	boolean representsZipFile() {
		return fRepresentsZip;
	}

	boolean hasChildren() {
		return fChildren != null && !fChildren.isEmpty();
	}

	Object[] getChildren() {
		return fChildren.toArray();
	}

	Object getParent() {
		return fParent;
	}

	private static int separatorIndex(String name) {
		return name.indexOf(SEGMENT_SEPARATOR);
	}

	private static String getFirstSegment(String name) {
		if (separatorIndex(name) == -1)
			return name;
		else
			return name.substring(0, separatorIndex(name));
	}

	private static String getTail(String name) {
		return name.substring(separatorIndex(name) + 1);
	}

	private boolean isRoot() {
		return fName == null;
	}

	/*
	 * finds the node (in the tree )
	 * whose name corresponds to the parameter
	 */
	ZipTreeNode findNode(String name) {
		if (isRoot())
			return ((ZipTreeNode) fChildren.get(0)).findNode(name);

		if (representsZipFile() && "".equals(name)) //$NON-NLS-1$
			return this;

		if (!representsZipFile() && separatorIndex(name) == -1) {
			if (name.equals(fName))
				return this;
			else
				return null;
		}

		String firstSegment= getFirstSegment(name);
		if (!representsZipFile() && !firstSegment.equals(fName))
			return null;

		Object[] kids= getChildren();
		String tail= getTail(name);

		for (int i= 0; i < kids.length; i++) {
			ZipTreeNode found;
			if (representsZipFile())
				found= ((ZipTreeNode) kids[i]).findNode(name);
			else
				found= ((ZipTreeNode) kids[i]).findNode(tail);
			if (found != null)
				return found;
		}

		return null;
	}

	void insert(String name) {
		if ("".equals(name) || separatorIndex(name) == -1) //$NON-NLS-1$
			return;
		String firstSegment= getFirstSegment(name);
		Object[] kids= getChildren();
		String tail= getTail(name);
		for (int i= 0; i < kids.length; i++) {
			ZipTreeNode each= (ZipTreeNode) kids[i];
			if (each.getName().equals(firstSegment)) {
				each.insert(tail);
				return;
			}
		}
		ZipTreeNode newKid= new ZipTreeNode(this, firstSegment);
		fChildren.add(newKid);
		newKid.insert(tail);
	}

	private StringBuffer toStringBuffer() {
		if (fName == null || representsZipFile()) //top level
			return new StringBuffer();
		else
			return fParent.toStringBuffer().append(fName).append(
				SEGMENT_SEPARATOR);
	}

	/* non java-doc
	  * debugging only
	  */
	public String toString() {
		//empty package root
		if (representsZipFile())
			return ""; //$NON-NLS-1$
		StringBuffer sb= toStringBuffer();
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	static ZipTreeNode newZipTree(ZipFile file) {
		//the top level node is the only one with fName == null
		ZipTreeNode topLevel= new ZipTreeNode(null, null);
		ZipTreeNode zipNode= new ZipTreeNode(topLevel, file.getName());
		zipNode.fRepresentsZip= true;
		Enumeration all= file.entries();
		while (all.hasMoreElements()) {
			ZipEntry each= (ZipEntry) all.nextElement();
			zipNode.insert(
				each.getName().substring(
					0,
					each.getName().lastIndexOf(SEGMENT_SEPARATOR) + 1));
		}
		List l= new ArrayList(1);
		l.add(zipNode);
		topLevel.fChildren= l;

		return topLevel;
	}
}
