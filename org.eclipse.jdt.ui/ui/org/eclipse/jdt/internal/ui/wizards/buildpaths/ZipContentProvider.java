/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.zip.ZipFile;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * An adapter for presenting a zip file in a tree viewer.
 */
class ZipContentProvider implements ITreeContentProvider {

	private ZipTreeNode fTree;
	private ZipFile fZipFile;
	
	ZipTreeNode getSelectedNode(String initialSelection) {
		ZipTreeNode node= null;
		if (initialSelection != null) {
			node= fTree.findNode(initialSelection);
		}
		if (node == null) {
			node= fTree.findNode(""); //$NON-NLS-1$
		}
		return node;
	}
	
	void setInitialInput(ZipFile file){
		fTree= createTree(file);
	}
	
	private ZipTreeNode createTree(ZipFile zipFile){
		if (zipFile.equals(fZipFile))
			return fTree;
		fZipFile= zipFile;
		return ZipTreeNode.newZipTree(zipFile);
	}
	
	/* non java-doc
	 * @see ITreeContentProvider#inputChanged
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof ZipFile)
			fTree= createTree((ZipFile)newInput);
		else{
			fTree= null;	
			fZipFile= null;
		}	
	}

	/* non java-doc
	  * @see ITreeContentProvider#getParent
	  */
	public Object getParent(Object element) {
		return ((ZipTreeNode) element).getParent();
	}

	/* non java-doc
	 * @see ITreeContentProvider#hasChildren
	 */
	public boolean hasChildren(Object element) {
		return ((ZipTreeNode) element).hasChildren();
	}

	/* non java-doc
	 * @see ITreeContentProvider#getChildren
	 */
	public Object[] getChildren(Object element) {
		return ((ZipTreeNode) element).getChildren();
	}

	/* non java-doc
	 * @see ITreeContentProvider#getElements
	 */
	public Object[] getElements(Object zipFile) {
		if (fTree == null && zipFile instanceof ZipFile){
			fTree= createTree((ZipFile)zipFile);
		}	
		return fTree.getChildren();
	}

	/* non java-doc
	 * @see IContentProvider#dispose
	 */	
	public void dispose() {
		fTree= null;
		fZipFile= null;
	}
}


