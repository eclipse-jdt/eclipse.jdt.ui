package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.SingleCharReader;



public class JavaDocAccess {
	
	private static QualifiedName getQualifiedName(IPackageFragmentRoot root) {
		return new QualifiedName(JavaUI.ID_PLUGIN, "jdocattachment: " + root.getPath().toString()); //$NON-NLS-1$
	}	
	
	/**
	 * Gets the attached JavaDoc documentation from a packagefragment root.
	 * Returns null if not documentation has been attached to this root
	 */
	public static URL getJavaDocLocation(IPackageFragmentRoot root) throws CoreException {
		if (!root.isArchive()) {
			return null;
		}
		
		QualifiedName qualifiedName= getQualifiedName(root);
		IResource resource= root.getUnderlyingResource();
		if (resource == null) {
			resource= JavaPlugin.getWorkspace().getRoot();
		}
		String urlDesc= resource.getPersistentProperty(qualifiedName);
		if (urlDesc != null) {
			try {
				return new URL(urlDesc);
			} catch (MalformedURLException e) {
				// corrupted persistent properties?
				throw new JavaModelException(e, IStatus.ERROR);
			}
		}
		return null;
	}

	/**
	 * Attaches the location of the JavaDoc documentation to a packagefragment root.
	 * Documentation can only be attached to libraries.
	 */	
	public static void setJavaDocLocation(IPackageFragmentRoot root, URL url) throws CoreException {
		if (!root.isArchive()) {
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, JavaDocMessages.getString("JavaDocAccess.error.no_library"), null)); //$NON-NLS-1$
		}
		
		String urlDesc;
		if (url == null) {
			urlDesc= null;
		} else {
			urlDesc= url.toExternalForm();
		}
		QualifiedName qualifiedName= getQualifiedName(root);
		
		IResource resource= root.getUnderlyingResource();
		if (resource == null) {
			resource= JavaPlugin.getWorkspace().getRoot();
		}
		resource.setPersistentProperty(qualifiedName, urlDesc);	
	}
	
	/**
	 * Gets a reader for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 */
	public static SingleCharReader getJavaDoc(IMember member) throws JavaModelException {
		IBuffer buf= member.isBinary() ? member.getClassFile().getBuffer() : member.getCompilationUnit().getBuffer();
		if (buf == null) {
			// no source attachment found
			return null;
		}
		ISourceRange range= member.getSourceRange();
		int start= range.getOffset();
		int length= range.getLength();
		if (length >= 5 && buf.getChar(start) == '/'
			&& buf.getChar(start + 1) == '*' && buf.getChar(start + 2) == '*') {

			int end= findCommentEnd(buf, start + 3, start + length);
			if (end != -1) {
				return new JavaDoc2HTMLTextReader(new JavaDocCommentReader(buf, start, end));
			}
		}
		return null;
	}
	
	/**
	 * Gets a text content for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 */
	public static String getJavaDocText(IMember member) throws JavaModelException {
		try {
			SingleCharReader rd= getJavaDoc(member);
			if (rd != null)
				return rd.getString();
				
		} catch (IOException e) {
			throw new JavaModelException(e, IStatus.ERROR);
		}
		
		return null;
	}
		
	private static int findCommentEnd(IBuffer buffer, int start, int end) {
		for (int i= start; i < end; i++) {
			char ch= buffer.getChar(i);
			if (ch == '*' && (i + 1 < end) && buffer.getChar(i + 1) == '/') {
				return i + 2;
			}
		}
		return -1;
	}
}