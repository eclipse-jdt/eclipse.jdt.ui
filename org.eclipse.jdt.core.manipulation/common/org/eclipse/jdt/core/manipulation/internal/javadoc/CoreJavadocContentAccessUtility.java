/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TextElement;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;

import org.eclipse.jdt.internal.ui.viewsupport.CoreJavaElementLinks;

public class CoreJavadocContentAccessUtility {

	public static String createMethodInTypeLinks(IMethod overridden) {
		CharSequence methodLink= createSimpleMemberLink(overridden, CoreJavaElementLinks.JAVADOC_SCHEME);
		CharSequence typeLink= createSimpleMemberLink(overridden.getDeclaringType(), CoreJavaElementLinks.JAVADOC_SCHEME);
		String methodInType= MessageFormat.format(JavaDocMessages.JavaDoc2HTMLTextReader_method_in_type, methodLink, typeLink);
		return methodInType;
	}

	public static boolean isWhitespaceTextElement(Object fragment) {
		if (!(fragment instanceof TextElement))
			return false;

		TextElement textElement= (TextElement) fragment;
		return textElement.getText().trim().length() == 0;
	}

	public static boolean canInheritJavadoc(IMember member) {
		if (member instanceof IMethod && member.getJavaProject().exists()) {
			/*
			 * Exists test catches ExternalJavaProject, in which case no hierarchy can be built.
			 */
			try {
				return !((IMethod) member).isConstructor();
			} catch (JavaModelException e) {
				JavaManipulationPlugin.log(e);
			}
		}
		return false;
	}

	/**
	 * Gets the reader content as a String
	 *
	 * @param reader the reader
	 * @return the reader content as string
	 */
	public static String getString(Reader reader) {
		StringBuilder buf= new StringBuilder();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}

	/**
	 * Reads the content of the IFile.
	 *
	 * @param file the file whose content has to be read
	 * @return the content of the file
	 * @throws CoreException if the file could not be successfully connected or disconnected
	 */
	public static String getIFileContent(IFile file) throws CoreException {
		String content= null;
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath fullPath= file.getFullPath();
		manager.connect(fullPath, LocationKind.IFILE, null);
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(fullPath, LocationKind.IFILE);
			if (buffer != null) {
				content= buffer.getDocument().get();
			}
		} finally {
			manager.disconnect(fullPath, LocationKind.IFILE, null);
		}

		return content;
	}


	@SuppressWarnings("resource")
	public static String getFileContentFromAttachedSource(IPackageFragmentRoot root, String filePath) throws CoreException {
		IPath sourceAttachmentPath= root.getSourceAttachmentPath();
		if (sourceAttachmentPath != null) {
			File file= null;
			String encoding= null;

			if (sourceAttachmentPath.getDevice() == null) {
				//the path could be a workspace relative path to a zip or to the source folder
				IWorkspaceRoot wsRoot= ResourcesPlugin.getWorkspace().getRoot();
				IResource res= wsRoot.findMember(sourceAttachmentPath);

				if (res instanceof IFile) {
					// zip in the workspace
					IPath location= res.getLocation();
					if (location == null)
						return null;
					file= location.toFile();
					encoding= ((IFile) res).getCharset(false);

				} else if (res instanceof IContainer) {
					// folder in the workspace
					res= ((IContainer) res).findMember(filePath);
					if (!(res instanceof IFile))
						return null;
					encoding= ((IFile) res).getCharset(false);
					if (encoding == null)
						encoding= getSourceAttachmentEncoding(root);
					return getContentsFromInputStream(((IFile) res).getContents(), encoding);
				}
			}

			if (file == null || !file.exists())
				file= sourceAttachmentPath.toFile();

			if (file.isDirectory()) {
				//the path is an absolute filesystem path to the source folder
				IPath packagedocPath= sourceAttachmentPath.append(filePath);
				if (packagedocPath.toFile().exists())
					return getFileContent(packagedocPath.toFile());

			} else if (file.exists()) {
				//the package documentation is in a Jar/Zip
				IPath sourceAttachmentRootPath= root.getSourceAttachmentRootPath();
				String packagedocPath;
				//consider the root path also in the search path if it exists
				if (sourceAttachmentRootPath != null) {
					packagedocPath= sourceAttachmentRootPath.append(filePath).toString();
				} else {
					packagedocPath= filePath;
				}
				ZipFile zipFile= null;
				InputStream in= null;
				try {
					zipFile= new ZipFile(file, ZipFile.OPEN_READ);
					ZipEntry packagedocFile= zipFile.getEntry(packagedocPath);
					if (packagedocFile != null) {
						in= zipFile.getInputStream(packagedocFile);
						if (encoding == null)
							encoding= getSourceAttachmentEncoding(root);
						return getContentsFromInputStream(in, encoding);
					}
				} catch (IOException e) {
					throw new CoreException(new Status(IStatus.ERROR, JavaManipulationPlugin.getPluginId(), e.getMessage(), e));
				} finally {
					try {
						if (in != null) {
							in.close();
						}
					} catch (IOException e) {
						//ignore
					}
					try {
						if (zipFile != null) {
							zipFile.close();//this will close the InputStream also
						}
					} catch (IOException e) {
						//ignore
					}
				}
			}
		}

		return null;
	}



	public static String getSourceAttachmentEncoding(IPackageFragmentRoot root) throws JavaModelException {
		String encoding= ResourcesPlugin.getEncoding();
		IClasspathEntry entry= root.getRawClasspathEntry();

		if (entry != null) {
			int kind= entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE) {
				for (IClasspathAttribute attrib : entry.getExtraAttributes()) {
					if (IClasspathAttribute.SOURCE_ATTACHMENT_ENCODING.equals(attrib.getName())) {
						return attrib.getValue();
					}
				}
			}
		}

		return encoding;
	}

	public static String getContentsFromInputStream(InputStream in, String encoding) throws CoreException {
		final int defaultFileSize= 15 * 1024;
		StringBuilder buffer= new StringBuilder(defaultFileSize);
		Reader reader= null;

		try {
			reader= new BufferedReader(new InputStreamReader(in, encoding), defaultFileSize);

			char[] readBuffer= new char[2048];
			int charCount= reader.read(readBuffer);

			while (charCount > 0) {
				buffer.append(readBuffer, 0, charCount);
				charCount= reader.read(readBuffer);
			}

		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaManipulationPlugin.getPluginId(), e.getMessage(), e));
		} finally {
			try {
				if (reader != null) {
					reader.close();//this will also close the InputStream wrapped in the reader
				}
			} catch (IOException e) {
				//ignore
			}
		}
		return buffer.toString();
	}

	/**
	 * Reads the content of the java.io.File.
	 *
	 * @param file the file whose content has to be read
	 * @return the content of the file
	 * @throws CoreException if the file could not be successfully connected or disconnected
	 */
	public static String getFileContent(File file) throws CoreException {
		String content= null;
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();

		IPath fullPath= new Path(file.getAbsolutePath());
		manager.connect(fullPath, LocationKind.LOCATION, null);
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(fullPath, LocationKind.LOCATION);
			if (buffer != null) {
				content= buffer.getDocument().get();
			}
		} finally {
			manager.disconnect(fullPath, LocationKind.LOCATION, null);
		}
		return content;
	}


	public static Javadoc getJavadocNode(IJavaElement element, String rawJavadoc) {
		//FIXME: take from SharedASTProvider if available
		//Caveat: Javadoc nodes are not available when Javadoc processing has been disabled!
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=212207

		String source= rawJavadoc + "class C{}"; //$NON-NLS-1$
		CompilationUnit root= createAST(element, source);
		if (root == null)
			return null;
		List<AbstractTypeDeclaration> types= root.types();
		if (types.size() != 1)
			return null;
		AbstractTypeDeclaration type= types.get(0);
		return type.getJavadoc();
	}

	public static Javadoc getPackageJavadocNode(IJavaElement element, String cuSource) {
		CompilationUnit cu= createAST(element, cuSource);
		if (cu != null) {
			PackageDeclaration packDecl= cu.getPackage();
			if (packDecl != null) {
				return packDecl.getJavadoc();
			}
		}
		return null;
	}

	public static CompilationUnit createAST(IJavaElement element, String cuSource) {
		Assert.isNotNull(element);
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);

		IJavaProject javaProject= element.getJavaProject();
		parser.setProject(javaProject);
		Map<String, String> options= javaProject.getOptions(true);
		options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED); // workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=212207
		parser.setCompilerOptions(options);

		parser.setSource(cuSource.toCharArray());
		return (CompilationUnit) parser.createAST(null);
	}

	public static String getHTMLContent(IJarEntryResource jarEntryResource, String encoding) throws CoreException {
		try (InputStream in= jarEntryResource.getContents()) {
			return CoreJavadocContentAccessUtility.getContentsFromInputStream(in, encoding);
		} catch (IOException e) {
			// Ignore
		}
		return null;
	}


	public static CharSequence createSimpleMemberLink(IMember member, String scheme) {
		StringBuffer buf= new StringBuffer();
		buf.append("<a href='"); //$NON-NLS-1$
		try {
			String uri= CoreJavaElementLinks.createURI(scheme, member);
			buf.append(uri);
		} catch (URISyntaxException e) {
			JavaManipulationPlugin.log(e);
		}
		buf.append("'>"); //$NON-NLS-1$
		JavaElementLabelsCore.getElementLabel(member, 0, buf);
		buf.append("</a>"); //$NON-NLS-1$
		return buf;
	}


	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment. The
	 * content does contain only the text from the comment without the Javadoc leading star
	 * characters. Returns <code>null</code> if the member does not contain a Javadoc comment or if
	 * no source is available.
	 *
	 * @param member The member to get the Javadoc of.
	 * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden
	 *            class is returned if <code>allowInherited</code> is <code>true</code>.
	 * @return Returns a reader for the Javadoc comment content or <code>null</code> if the member
	 *         does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements javadoc can not be accessed
	 */
	public static Reader getContentReader(IMember member, boolean allowInherited) throws JavaModelException {
		Reader contentReader= internalGetContentReader(member);
		if (contentReader != null
				|| !allowInherited
				|| (member.getElementType() != IJavaElement.METHOD))
			return contentReader;
		return findDocInHierarchy((IMethod) member, false, false);
	}

	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment. The
	 * content does contain only the text from the comment without the Javadoc leading star
	 * characters. Returns <code>null</code> if the member does not contain a Javadoc comment or if
	 * no source is available.
	 *
	 * @param member The member to get the Javadoc of.
	 * @return Returns a reader for the Javadoc comment content or <code>null</code> if the member
	 *         does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements javadoc can not be accessed
	 * @since 3.4
	 */
	private static Reader internalGetContentReader(IMember member) throws JavaModelException {
		IBuffer buf= member.getOpenable().getBuffer();
		if (buf == null) {
			return null; // no source attachment found
		}

		ISourceRange javadocRange= member.getJavadocRange();
		if (javadocRange != null) {
			CoreJavaDocCommentReader reader= new CoreJavaDocCommentReader(buf, javadocRange.getOffset(), javadocRange.getOffset() + javadocRange.getLength() - 1);
			if (!containsOnlyInheritDoc(reader, javadocRange.getLength())) {
				reader.reset();
				return reader;
			}
		}

		return null;
	}

	/**
	 * Checks whether the given reader only returns the inheritDoc tag.
	 *
	 * @param reader the reader
	 * @param length the length of the underlying content
	 * @return <code>true</code> if the reader only returns the inheritDoc tag
	 * @since 3.2
	 */
	private static boolean containsOnlyInheritDoc(Reader reader, int length) {
		char[] content= new char[length];
		try {
			reader.read(content, 0, length);
		} catch (IOException e) {
			return false;
		}
		return "{@inheritDoc}".equals(new String(content).trim()); //$NON-NLS-1$

	}

	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment. and
	 * renders the tags in HTML. Returns <code>null</code> if the member does not contain a Javadoc
	 * comment or if no source is available.
	 *
	 * @param member the member to get the Javadoc of.
	 * @param allowInherited for methods with no (Javadoc) comment, the comment of the overridden
	 *            class is returned if <code>allowInherited</code> is <code>true</code>
	 * @param useAttachedJavadoc if <code>true</code> Javadoc will be extracted from attached
	 *            Javadoc if there's no source
	 * @return a reader for the Javadoc comment content in HTML or <code>null</code> if the member
	 *         does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements Javadoc can not be accessed
	 * @since 3.2
	 */
	public static Reader getHTMLContentReader(IMember member, boolean allowInherited, boolean useAttachedJavadoc) throws JavaModelException {
		Reader contentReader= internalGetContentReader(member);
		if (contentReader != null) {
			CoreJavaDoc2HTMLTextReader r= new CoreJavaDoc2HTMLTextReader(contentReader);
			return r;
		}
		if (useAttachedJavadoc && member.getOpenable().getBuffer() == null) { // only if no source available
			String s= member.getAttachedJavadoc(null);
			if (s != null)
				return new StringReader(s);
		}

		if (allowInherited && (member.getElementType() == IJavaElement.METHOD))
			return findDocInHierarchy((IMethod) member, true, useAttachedJavadoc);

		return null;
	}

	private static Reader findDocInHierarchy(IMethod method, boolean isHTML, boolean useAttachedJavadoc) throws JavaModelException {
		/*
		 * Catch ExternalJavaProject in which case
		 * no hierarchy can be built.
		 */
		if (!method.getJavaProject().exists())
			return null;

		IType type= method.getDeclaringType();
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);

		MethodOverrideTester tester= new MethodOverrideTester(type, hierarchy);

		for (IType curr : hierarchy.getAllSupertypes(type)) {
			IMethod overridden= tester.findOverriddenMethodInType(curr, method);
			if (overridden != null) {
				if (isHTML) {
					Reader reader= getHTMLContentReader(overridden, false, useAttachedJavadoc);
					if (reader != null)
						return reader;
				} else {
					Reader reader= getContentReader(overridden, false);
					if (reader != null)
						return reader;
				}
			}
		}
		return null;
	}

}
