/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000,2001
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.*;

import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.util.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;


public class JarStructureCreator implements IStructureCreator {

	/**
	 * Common base class for JarFolder and JarFile
	 */
	static abstract class JarResource implements IStructureComparator, ITypedElement {

		private String fName;

		JarResource(String name) {
			fName= name;
		}

		public String getName() {
			return fName;
		}

		public Image getImage() {
			return CompareUI.getImage(getType());
		}

		/**
		 * Returns true if other is ITypedElement and names are equal.
		 * @see IComparator#equals
		 */
		public boolean equals(Object other) {
			if (other instanceof ITypedElement)
				return fName.equals(((ITypedElement) other).getName());
			return super.equals(other);
		}

		public int hashCode() {
			return fName.hashCode();
		}
	}

	static class JarFolder extends JarResource {

		private HashMap fChildren= new HashMap(10);

		JarFolder(String name) {
			super(name);
		}

		public String getType() {
			return ITypedElement.FOLDER_TYPE;
		}

		public Object[] getChildren() {
			Object[] children= new Object[fChildren.size()];
			Iterator iter= fChildren.values().iterator();
			for (int i= 0; iter.hasNext(); i++)
				children[i]= iter.next();
			return children;
		}

		JarFile createContainer(String path) {
			String entry= path;
			int pos= path.indexOf('/');
			if (pos < 0)
				pos= path.indexOf('\\');
			if (pos >= 0) {
				entry= path.substring(0, pos);
				path= path.substring(pos + 1);
			} else if (entry.length() > 0) {
				JarFile ze= new JarFile(entry);
				fChildren.put(entry, ze);
				return ze;
			} else
				return null;

			JarFolder folder= null;
			if (fChildren != null) {
				Object o= fChildren.get(entry);
				if (o instanceof JarFolder)
					folder= (JarFolder) o;
			}

			if (folder == null) {
				folder= new JarFolder(entry);
				fChildren.put(entry, folder);
			}

			return folder.createContainer(path);
		}
	}

	static class JarFile extends JarResource implements IStreamContentAccessor {

		private byte[] fContents;

		JarFile(String name) {
			super(name);
		}

		public String getType() {
			String s= this.getName();
			int pos= s.lastIndexOf('.');
			if (pos >= 0)
				return s.substring(pos + 1);
			return ITypedElement.UNKNOWN_TYPE;
		}

		public Object[] getChildren() {
			return null;
		}
		
		public InputStream getContents() {
			if (fContents == null)
				fContents= new byte[0];
			return new ByteArrayInputStream(fContents);
		}

		byte[] getBytes() {
			return fContents;
		}

		void setBytes(byte[] buffer) {
			fContents= buffer;
		}
	}
	
	private String fTitle;

	public JarStructureCreator() {
		this("Jar Archive Compare");
	}
	
	public JarStructureCreator(String title) {
		fTitle= title;
	}

	public String getName() {
		return fTitle;
	}

	public IStructureComparator getStructure(Object input) {

		InputStream is= null;
		
		if (input instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca= (IStreamContentAccessor) input;
			try {
				is= sca.getContents();
			} catch (CoreException ex) {
			}
		}

		if (is == null)
			return null;

		ZipInputStream zip= new ZipInputStream(is);
		JarFolder root= new JarFolder("");
		try {
			for (;;) {
				ZipEntry entry= zip.getNextEntry();
				if (entry == null)
					break;
				//System.out.println(entry.getName() + ": " + entry.getSize() + " " + entry.getCompressedSize());

				JarFile ze= root.createContainer(entry.getName());
				if (ze != null) {
					int length= (int) entry.getSize();
					if (length >= 0) {
						byte[] buffer= new byte[length];
						int offset= 0;
	
						do {
							int n= zip.read(buffer, offset, length);
							offset += n;
							length -= n;
						} while (length > 0);
	
						ze.setBytes(buffer);
					}
				}
				zip.closeEntry();
			}
		} catch (IOException ex) {
			return null;
		} finally {
			try {
				zip.close();
			} catch (IOException ex) {
			}
		}

		if (root.fChildren.size() == 1) {
			Iterator iter= root.fChildren.values().iterator();
			return (IStructureComparator) iter.next();
		}
		return root;
	}

	public String getContents(Object o, boolean ignoreWhitespace) {
		if (o instanceof JarFile) {
			byte[] bytes= ((JarFile)o).getBytes();
			if (bytes != null)
				return new String(bytes);
			return "";
		}
		return null;
	}

	/**
	 * Returns <code>false</code> since we cannot update a zip archive.
	 */
	public boolean canSave() {
		return false;
	}

	/**
	 * Throws <code>AssertionFailedException</code> since we cannot update a zip archive.
	 */
	public void save(IStructureComparator structure, Object input) {
		Assert.isTrue(false, "cannot update zip archive");
	}
	
	public IStructureComparator locate(Object path, Object source) {
		return null;
	}
	
	public boolean canRewriteTree() {
		return false;
	}
	
	public void rewriteTree(Differencer diff, IDiffContainer root) {
	}
}

