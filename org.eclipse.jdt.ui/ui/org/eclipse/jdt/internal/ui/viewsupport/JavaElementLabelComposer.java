/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Guven Demir <guven.internet+eclipse@gmail.com> - [package explorer] Alternative package name shortening: abbreviation - https://bugs.eclipse.org/bugs/show_bug.cgi?id=299514
 *     Stefan Xenos <sxenos@gmail.com> (Google) - Bug 479286 - NullPointerException in JavaElementLabelComposer
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.Attributes.Name;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelComposerCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * Implementation of {@link JavaElementLabels}.
 *
 * @since 3.5
 */
public class JavaElementLabelComposer extends JavaElementLabelComposerCore {

	public static abstract class FlexibleBuffer extends FlexibleBufferCore {

		/**
		 * Sets a styler to use for the given source range. The range must be subrange of actual
		 * string of this buffer. Stylers previously set for that range will be overwritten.
		 *
		 * @param offset the start offset of the range
		 * @param length the length of the range
		 * @param styler the styler to set
		 *
		 * @throws StringIndexOutOfBoundsException if <code>start</code> is less than zero, or if
		 *             offset plus length is greater than the length of this object.
		 */
		public abstract void setStyle(int offset, int length, Styler styler);
	}

	public static class FlexibleStringBuffer extends FlexibleBuffer {
		private final StringBuffer fStringBuffer;

		public FlexibleStringBuffer(StringBuffer stringBuffer) {
			fStringBuffer= stringBuffer;
		}

		@Override
		public FlexibleBuffer append(char ch) {
			fStringBuffer.append(ch);
			return this;
		}

		@Override
		public FlexibleBuffer append(String string) {
			fStringBuffer.append(string);
			return this;
		}

		@Override
		public int length() {
			return fStringBuffer.length();
		}

		@Override
		public String toString() {
			return fStringBuffer.toString();
		}

		@Override
		public void setStyle(int offset, int length, Styler styler) {
			// no style
		}
	}


	public static class FlexibleStyledString extends FlexibleBuffer {
		private final StyledString fStyledString;

		public FlexibleStyledString(StyledString stringBuffer) {
			fStyledString= stringBuffer;
		}

		@Override
		public FlexibleBuffer append(char ch) {
			fStyledString.append(ch);
			return this;
		}

		@Override
		public FlexibleBuffer append(String string) {
			fStyledString.append(string);
			return this;
		}

		@Override
		public int length() {
			return fStyledString.length();
		}

		/**
		 * Sets a styler to use for the given source range. The range must be subrange of actual
		 * string of this buffer. Stylers previously set for that range will be overwritten.
		 *
		 * @param offset the start offset of the range
		 * @param length the length of the range
		 * @param styler the styler to set
		 *
		 * @throws StringIndexOutOfBoundsException if <code>start</code> is less than zero, or if
		 *             offset plus length is greater than the length of this object.
		 */
		@Override
		public void setStyle(int offset, int length, Styler styler) {
			fStyledString.setStyle(offset, length, styler);
		}

		@Override
		public String toString() {
			return fStyledString.toString();
		}
	}

	private static class PackageNameAbbreviation {
		private String fPackagePrefix;

		private String fAbbreviation;

		public PackageNameAbbreviation(String packagePrefix, String abbreviation) {
			fPackagePrefix= packagePrefix;
			fAbbreviation= abbreviation;
		}

		public String getPackagePrefix() {
			return fPackagePrefix;
		}

		public String getAbbreviation() {
			return fAbbreviation;
		}
	}


	final static long QUALIFIER_FLAGS= JavaElementLabels.P_COMPRESSED | JavaElementLabels.USE_RESOLVED;

	private static final Styler QUALIFIER_STYLE= StyledString.QUALIFIER_STYLER;
	private static final Styler COUNTER_STYLE= StyledString.COUNTER_STYLER;
	private static final Styler DECORATIONS_STYLE= StyledString.DECORATIONS_STYLER;

	/*
	 * Package name compression
	 */
	private static String fgPkgNamePattern= ""; //$NON-NLS-1$

	/*
	 * Package name abbreviation
	 */
	private static String fgPkgNameAbbreviationPattern= ""; //$NON-NLS-1$
	private static PackageNameAbbreviation[] fgPkgNameAbbreviation;

	/**
	 * Creates a new java element composer based on the given buffer.
	 *
	 * @param buffer the buffer
	 */
	public JavaElementLabelComposer(FlexibleBuffer buffer) {
		super(buffer);
	}

	/**
	 * Creates a new java element composer based on the given buffer.
	 *
	 * @param buffer the buffer
	 */
	public JavaElementLabelComposer(StyledString buffer) {
		this(new FlexibleStyledString(buffer));
	}

	/**
	 * Creates a new java element composer based on the given buffer.
	 *
	 * @param buffer the buffer
	 */
	public JavaElementLabelComposer(StringBuffer buffer) {
		this(new FlexibleStringBuffer(buffer));
	}



	@Override
	protected void setQualifierStyle(int offset) {
		if (fBuffer instanceof FlexibleBuffer)
			((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
	}

	@Override
	protected void setDecorationsStyle(int offset) {
		if (fBuffer instanceof FlexibleBuffer)
			((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, DECORATIONS_STYLE);
	}

	@Override
	protected void appendCategoryLabel(IMember member, long flags) throws JavaModelException {
		String[] categories= member.getCategories();
		if (categories.length > 0) {
			int offset= fBuffer.length();
			StringBuilder categoriesBuf= new StringBuilder();
			for (int i= 0; i < categories.length; i++) {
				if (i > 0)
					categoriesBuf.append(JavaElementLabels.CATEGORY_SEPARATOR_STRING);
				categoriesBuf.append(categories[i]);
			}
			fBuffer.append(JavaElementLabels.CONCAT_STRING);
			fBuffer.append(Messages.format(JavaUIMessages.JavaElementLabels_category, categoriesBuf.toString()));
			if (getFlag(flags, JavaElementLabels.COLORIZE) && fBuffer instanceof FlexibleBuffer) {
				((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, COUNTER_STYLE);
			}
		}
	}

	/**
	 * Appends the label for a package fragment root. Considers the ROOT_* flags.
	 *
	 * @param root the element to render
	 * @param flags the rendering flags. Flags with names starting with ROOT_' are considered.
	 */
	@Override
	public void appendPackageFragmentRootLabel(IPackageFragmentRoot root, long flags) {
		// Handle variables different
		if (getFlag(flags, JavaElementLabels.ROOT_VARIABLE) && appendVariableLabel(root, flags))
			return;
		if (root.isArchive())
			appendArchiveLabel(root, flags);
		else
			appendFolderLabel(root, flags);
	}

	private void appendArchiveLabel(IPackageFragmentRoot root, long flags) {
		boolean external= root.isExternal();
		if (external)
			appendExternalArchiveLabel(root, flags);
		else
			appendInternalArchiveLabel(root, flags);
	}

	private boolean appendVariableLabel(IPackageFragmentRoot root, long flags) {
		try {
			IClasspathEntry rawEntry= root.getRawClasspathEntry();
			if (rawEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
				IClasspathEntry entry= JavaModelUtil.getClasspathEntry(root);
				if (entry.getReferencingEntry() != null) {
					return false; // not the variable entry itself, but a referenced entry
				}
				IPath path= rawEntry.getPath().makeRelative();

				if (getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED)) {
					int segements= path.segmentCount();
					if (segements > 0) {
						fBuffer.append(path.segment(segements - 1));
						if (segements > 1) {
							int offset= fBuffer.length();
							fBuffer.append(JavaElementLabels.CONCAT_STRING);
							fBuffer.append(path.removeLastSegments(1).toOSString());
							if (getFlag(flags, JavaElementLabels.COLORIZE) && fBuffer instanceof FlexibleBuffer) {
								((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
							}
						}
					} else {
						fBuffer.append(path.toString());
					}
				} else {
					fBuffer.append(path.toString());
				}
				int offset= fBuffer.length();
				fBuffer.append(JavaElementLabels.CONCAT_STRING);
				if (root.isExternal())
					fBuffer.append(root.getPath().toOSString());
				else
					fBuffer.append(root.getPath().makeRelative().toString());

				if (getFlag(flags, JavaElementLabels.COLORIZE) && fBuffer instanceof FlexibleBuffer) {
					((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
				}
				return true;
			}
		} catch (JavaModelException e) {
			// problems with class path, ignore (bug 202792)
			return false;
		}
		return false;
	}

	private void appendExternalArchiveLabel(IPackageFragmentRoot root, long flags) {
		IPath path;
		IClasspathEntry classpathEntry= null;
		try {
			classpathEntry= JavaModelUtil.getClasspathEntry(root);
			IPath rawPath= classpathEntry.getPath();
			if (classpathEntry.getEntryKind() != IClasspathEntry.CPE_CONTAINER && !rawPath.isAbsolute())
				path= rawPath;
			else
				path= root.getPath();
		} catch (JavaModelException e) {
			path= root.getPath();
		}
		if (getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED)) {
			int segmentCount= path.segmentCount();
			if (segmentCount > 0) {
				String elementName= root.getElementName();
				fBuffer.append(elementName);
				int offset= fBuffer.length();

				boolean skipLastSegment= elementName.equals(path.lastSegment());
				if (segmentCount > 1 || path.getDevice() != null || !skipLastSegment) {
					fBuffer.append(JavaElementLabels.CONCAT_STRING);
					IPath postQualifier= skipLastSegment ? path.removeLastSegments(1) : path;
					fBuffer.append(postQualifier.toOSString());
				}
				if (classpathEntry != null) {
					IClasspathEntry referencingEntry= classpathEntry.getReferencingEntry();
					if (referencingEntry != null) {
						fBuffer.append(Messages.format(JavaUIMessages.JavaElementLabels_onClassPathOf, new Object[] { Name.CLASS_PATH.toString(), referencingEntry.getPath().lastSegment() }));
					}
				}
				if (getFlag(flags, JavaElementLabels.COLORIZE) && fBuffer instanceof FlexibleBuffer) {
					((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
				}
			} else {
				fBuffer.append(path.toOSString());
			}
		} else {
			fBuffer.append(path.toOSString());
		}
	}

	@Override
	protected void appendAbbreviatedPackageFragment(IPackageFragment pack) {
		refreshPackageNameAbbreviation();

		String pkgName= pack.getElementName();

		if (fgPkgNameAbbreviation != null && fgPkgNameAbbreviation.length != 0) {

			for (PackageNameAbbreviation abbr : fgPkgNameAbbreviation) {
				String abbrPrefix= abbr.getPackagePrefix();
				if (pkgName.startsWith(abbrPrefix)) {
					int abbrPrefixLength= abbrPrefix.length();
					int pkgLength= pkgName.length();
					if ((pkgLength != abbrPrefixLength)
							&& (pkgName.charAt(abbrPrefixLength) != '.')) {
						continue;
					}

					fBuffer.append(abbr.getAbbreviation());

					if (pkgLength > abbrPrefixLength) {
						fBuffer.append('.');

						String remaining= pkgName.substring(abbrPrefixLength + 1);

						if (isPackageNameCompressionEnabled())
							appendCompressedPackageFragment(remaining);
						else
							fBuffer.append(remaining);
					}

					return;
				}
			}
		}

		if (isPackageNameCompressionEnabled()) {
			appendCompressedPackageFragment(pkgName);
		} else {
			fBuffer.append(pkgName);
		}
	}

	private void appendInternalArchiveLabel(IPackageFragmentRoot root, long flags) {
		IResource resource= root.getResource();
		boolean rootQualified= getFlag(flags, JavaElementLabels.ROOT_QUALIFIED);
		if (rootQualified) {
			fBuffer.append(root.getPath().makeRelative().toString());
		} else {
			fBuffer.append(root.getElementName());
			int offset= fBuffer.length();
			boolean referencedPostQualified= getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED);
			if (referencedPostQualified && isReferenced(root)) {
				fBuffer.append(JavaElementLabels.CONCAT_STRING);
				fBuffer.append(resource.getParent().getFullPath().makeRelative().toString());
			} else if (getFlag(flags, JavaElementLabels.ROOT_POST_QUALIFIED)) {
				fBuffer.append(JavaElementLabels.CONCAT_STRING);
				fBuffer.append(root.getParent().getPath().makeRelative().toString());
			}
			if (referencedPostQualified) {
				try {
					IClasspathEntry referencingEntry= JavaModelUtil.getClasspathEntry(root).getReferencingEntry();
					if (referencingEntry != null) {
						fBuffer.append(Messages.format(JavaUIMessages.JavaElementLabels_onClassPathOf, new Object[] { Name.CLASS_PATH.toString(), referencingEntry.getPath().lastSegment() }));
					}
				} catch (JavaModelException e) {
					// ignore
				}
			}
			if (getFlag(flags, JavaElementLabels.COLORIZE) && fBuffer instanceof FlexibleBuffer) {
				((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	private void appendFolderLabel(IPackageFragmentRoot root, long flags) {
		IResource resource= root.getResource();
		if (resource == null) {
			appendExternalArchiveLabel(root, flags);
			return;
		}

		boolean rootQualified= getFlag(flags, JavaElementLabels.ROOT_QUALIFIED);
		boolean referencedQualified= getFlag(flags, JavaElementLabels.REFERENCED_ROOT_POST_QUALIFIED) && isReferenced(root);
		if (rootQualified) {
			fBuffer.append(root.getPath().makeRelative().toString());
		} else {
			IPath projectRelativePath= resource.getProjectRelativePath();
			if (projectRelativePath.segmentCount() == 0) {
				fBuffer.append(resource.getName());
				referencedQualified= false;
			} else {
				fBuffer.append(projectRelativePath.toString());
			}

			int offset= fBuffer.length();
			if (referencedQualified) {
				fBuffer.append(JavaElementLabels.CONCAT_STRING);
				fBuffer.append(resource.getProject().getName());
			} else if (getFlag(flags, JavaElementLabels.ROOT_POST_QUALIFIED)) {
				fBuffer.append(JavaElementLabels.CONCAT_STRING);
				fBuffer.append(root.getParent().getElementName());
			} else {
				return;
			}
			if (getFlag(flags, JavaElementLabels.COLORIZE) && fBuffer instanceof FlexibleBuffer) {
				((FlexibleBuffer)fBuffer).setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	/**
	 * Returns <code>true</code> if the given package fragment root is
	 * referenced. This means it is a descendant of a different project but is referenced
	 * by the root's parent. Returns <code>false</code> if the given root
	 * doesn't have an underlying resource.
	 *
	 * @param root the package fragment root
	 * @return returns <code>true</code> if the given package fragment root is referenced
	 */
	private boolean isReferenced(IPackageFragmentRoot root) {
		IResource resource= root.getResource();
		if (resource != null) {
			IProject jarProject= resource.getProject();
			IProject container= root.getJavaProject().getProject();
			return !container.equals(jarProject);
		}
		return false;
	}

	@Override
	protected void refreshPackageNamePattern() {
		String pattern= getPkgNamePatternForPackagesView();
		final String EMPTY_STRING= ""; //$NON-NLS-1$
		if (pattern.equals(fgPkgNamePattern))
			return;
		else if (pattern.length() == 0) {
			fgPkgNamePattern= EMPTY_STRING;
			fgPkgNameLength= -1;
			return;
		}
		fgPkgNamePattern= pattern;
		int i= 0;
		fgPkgNameChars= 0;
		fgPkgNamePrefix= EMPTY_STRING;
		fgPkgNamePostfix= EMPTY_STRING;
		while (i < pattern.length()) {
			char ch= pattern.charAt(i);
			if (Character.isDigit(ch)) {
				fgPkgNameChars= ch-48;
				if (i > 0)
					fgPkgNamePrefix= pattern.substring(0, i);
				if (i >= 0)
					fgPkgNamePostfix= pattern.substring(i+1);
				fgPkgNameLength= fgPkgNamePrefix.length() + fgPkgNameChars + fgPkgNamePostfix.length();
				return;
			}
			i++;
		}
		fgPkgNamePrefix= pattern;
		fgPkgNameLength= pattern.length();
	}

	private void refreshPackageNameAbbreviation() {
		String pattern= getPkgNameAbbreviationPatternForPackagesView();

		if (fgPkgNameAbbreviationPattern.equals(pattern))
			return;

		fgPkgNameAbbreviationPattern= pattern;

		if (pattern == null || pattern.length() == 0) {
			fgPkgNameAbbreviationPattern= ""; //$NON-NLS-1$
			fgPkgNameAbbreviation= null;
			return;
		}

		PackageNameAbbreviation[] abbrs= parseAbbreviationPattern(pattern);

		if (abbrs == null)
			abbrs= new PackageNameAbbreviation[0];

		fgPkgNameAbbreviation= abbrs;
	}

	public static PackageNameAbbreviation[] parseAbbreviationPattern(String pattern) {
		ArrayList<PackageNameAbbreviation> result= new ArrayList<>();

		for (String p : pattern.split("\\s*(?:\r\n?|\n)\\s*")) { //$NON-NLS-1$
			String part= p.trim();
			if (part.length() == 0)
				continue;
			String[] parts2= part.split("\\s*=\\s*", 2); //$NON-NLS-1$
			if (parts2.length != 2)
				return null;
			String prefix= parts2[0].trim();
			String abbr= parts2[1].trim();
			if (prefix.startsWith("#")) //$NON-NLS-1$
				continue;
			PackageNameAbbreviation pkgAbbr= new PackageNameAbbreviation(prefix, abbr);
			result.add(pkgAbbr);
		}

		Collections.sort(result, (a1, a2) -> a2.getPackagePrefix().length() - a1.getPackagePrefix().length());

		return result.toArray(new PackageNameAbbreviation[0]);
	}

	private boolean isPackageNameCompressionEnabled() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		return store.getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES);
	}

	private String getPkgNamePatternForPackagesView() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES))
			return ""; //$NON-NLS-1$
		return store.getString(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW);
	}

	@Override
	protected boolean isPackageNameAbbreviationEnabled() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		return store.getBoolean(PreferenceConstants.APPEARANCE_ABBREVIATE_PACKAGE_NAMES);
	}

	private String getPkgNameAbbreviationPatternForPackagesView() {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		if (!store.getBoolean(PreferenceConstants.APPEARANCE_ABBREVIATE_PACKAGE_NAMES))
			return ""; //$NON-NLS-1$
		return store.getString(PreferenceConstants.APPEARANCE_PKG_NAME_ABBREVIATION_PATTERN_FOR_PKG_VIEW);
	}

}
