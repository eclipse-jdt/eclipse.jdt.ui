package org.eclipse.jdt.internal.ui.preferences;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
 

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *  Tree representing the structure of a zip file
 */
/*package*/ class ZipTreeNode {
	private ZipTreeNode fParent;
	private List fChildren;
	private String fName;
	private boolean fRepresentsZip= false;
	private static final String SEGMENT_SEPARATOR= "/";

	private ZipTreeNode(ZipTreeNode parent, List children, String name) {
		fParent= parent;
		fChildren= children;
		fName= name;
	}

	/*package*/ String getName() {
		return fName;
	}

	/*package*/ boolean representsZipFile(){
		return fRepresentsZip;
	}

	/*package*/ boolean hasChildren() {
		return fChildren != null;
	}

	/*package*/ Object[] getChildren() {
		if (fChildren == null)
			return null;
		else
			return fChildren.toArray();
	}

	/*package*/ Object getParent() {
		return fParent;
	}

	private int separatorIndex(String name){
		return name.indexOf(SEGMENT_SEPARATOR);
	}
	
	private String getFirstSegment(String name){
		if (separatorIndex(name) == -1)
			return name;
		else 
			return name.substring(0, separatorIndex(name));
	}
	
	private String getTail(String name){
		return name.substring(separatorIndex(name) + 1);
	}
	
	private boolean isRoot(){
		return fName == null;
	}
	
	/*
	 * finds the node (in the tree )
	 * whose name corresponds to the parameter
	 */
	/*package*/ ZipTreeNode findNode(String name){
		if (isRoot()){
			return ((ZipTreeNode)fChildren.get(0)).findNode(name);
		}
		if (representsZipFile() && "".equals(name)){
			return this;
		} 
	
		if (!representsZipFile() && separatorIndex(name) == -1){
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
		
		for (int i=0; i < kids.length; i++) {
			ZipTreeNode found;
			if (representsZipFile())
				found= ((ZipTreeNode)kids[i]).findNode(name);
			else 
				found= ((ZipTreeNode)kids[i]).findNode(tail);
			if (found != null)
				return found;
		}
		
		return null;
	}
	
	/*package*/ void insert(String name) {
		if ("".equals(name) || separatorIndex(name) == -1)
			return;
		String firstSegment= getFirstSegment(name);
		if (fChildren == null)
			fChildren= new ArrayList(5);
		Object[] kids= getChildren();
		String tail= getTail(name);
		for (int i= 0; i < kids.length; i++) {
			ZipTreeNode each= (ZipTreeNode) kids[i];
			if (each.getName().equals(firstSegment)) {
				each.insert(tail);
				return;
			}
		}
		ZipTreeNode newKid= new ZipTreeNode(this, null, firstSegment);
		fChildren.add(newKid);
		newKid.insert(tail);
	}

	private StringBuffer toStringBuffer() {
		if (fName == null || representsZipFile()) //top level
			return new StringBuffer();
		else
			return fParent.toStringBuffer().append(fName).append(SEGMENT_SEPARATOR);
	}

	public String toString() {
		//empty package root
		if (representsZipFile())
			return "";
		StringBuffer sb= toStringBuffer();
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	public static ZipTreeNode newZipTree(ZipFile file) {
		//the top level node is the only one with fName == null
		ZipTreeNode topLevel= new ZipTreeNode(null, null, null);
		ZipTreeNode zipNode= new ZipTreeNode(topLevel, null, file.getName());
		zipNode.fRepresentsZip= true;
		Enumeration all= file.entries();
		while (all.hasMoreElements()) {
			ZipEntry each= (ZipEntry) all.nextElement();
			zipNode.insert(each.getName().substring(0, each.getName().lastIndexOf(SEGMENT_SEPARATOR) + 1));
		}
		List l= new ArrayList(1);
		l.add(zipNode);
		topLevel.fChildren= l;
		
		return topLevel;
	}
}


