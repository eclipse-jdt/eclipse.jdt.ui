/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ISimilarDeclarationUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Helper class to generate a refactoring descriptor comment.
 * 
 * @since 3.2
 */
public final class JavaRefactoringDescriptorComment {

	/** The line delimiter */
	private static final String LINE_DELIMITER= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	/** The header of the comment */
	private final String fHeader;

	/** The inferred settings list */
	private final List fInferredSettings= new ArrayList(6);

	/** The user-defined settings list */
	private final List fUserSettings= new ArrayList(4);

	/**
	 * Creates a new java refactoring descriptor comment.
	 * 
	 * @param object
	 *            the refactoring object to generate a comment for
	 * @param header
	 *            the header of the comment (typically the unique description of
	 *            the refactoring with fully qualified element names)
	 */
	public JavaRefactoringDescriptorComment(final Object object, final String header) {
		Assert.isNotNull(object);
		Assert.isNotNull(header);
		fHeader= header;
		initializeInferredSettings(object);
	}

	/**
	 * Adds the specified setting to this comment.
	 * 
	 * @param setting
	 *            the setting to add
	 */
	public void addSetting(final String setting) {
		Assert.isNotNull(setting);
		Assert.isTrue(!"".equals(setting)); //$NON-NLS-1$
		fUserSettings.add(setting);
	}

	/**
	 * Returns this comment in a human-readable string representation.
	 * 
	 * @return this comment in string representation
	 */
	public String asString() {
		final StringBuffer buffer= new StringBuffer(256);
		buffer.append(fHeader);
		if (fUserSettings.size() + fInferredSettings.size() > 0) {
			for (final Iterator iterator= fUserSettings.iterator(); iterator.hasNext();) {
				final String setting= (String) iterator.next();
				buffer.append(LINE_DELIMITER);
				buffer.append(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_user_setting_pattern, setting));
			}
			for (final Iterator iterator= fInferredSettings.iterator(); iterator.hasNext();) {
				final String setting= (String) iterator.next();
				buffer.append(LINE_DELIMITER);
				buffer.append(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_inferred_setting_pattern, setting));
			}
		}
		return buffer.toString();
	}

	/**
	 * Initializes the inferred settings.
	 * 
	 * @param object
	 *            the refactoring object
	 */
	private void initializeInferredSettings(final Object object) {
		if (object instanceof INameUpdating) {
			final INameUpdating updating= (INameUpdating) object;
			fInferredSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_original_element_pattern, JavaElementLabels.getTextLabel(updating.getElements()[0], JavaElementLabels.ALL_FULLY_QUALIFIED)));
			try {
				final Object element= updating.getNewElement();
				if (element != null)
					fInferredSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_renamed_element_pattern, JavaElementLabels.getTextLabel(element, JavaElementLabels.ALL_FULLY_QUALIFIED)));
				else {
					final String oldLabel= JavaElementLabels.getTextLabel(updating.getElements()[0], JavaElementLabels.ALL_FULLY_QUALIFIED);
					final String newName= updating.getCurrentElementName();
					if (newName.length() < oldLabel.length()) {
						final String newLabel= oldLabel.substring(0, oldLabel.length() - newName.length());
						fInferredSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_renamed_element_pattern, newLabel + updating.getNewElementName()));
					}
				}
			} catch (CoreException exception) {
				JavaPlugin.log(exception);
			}
		} else if (object instanceof RefactoringProcessor) {
			final RefactoringProcessor processor= (RefactoringProcessor) object;
			final Object[] elements= processor.getElements();
			if (elements != null) {
				if (elements.length == 1 && elements[0] != null)
					fInferredSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_original_element_pattern, JavaElementLabels.getTextLabel(elements[0], JavaElementLabels.ALL_FULLY_QUALIFIED)));
				else if (elements.length > 1) {
					final StringBuffer buffer= new StringBuffer(128);
					buffer.append(RefactoringCoreMessages.JavaRefactoringDescriptor_original_elements);
					for (int index= 0; index < elements.length; index++) {
						if (elements[index] != null) {
							buffer.append(LINE_DELIMITER);
							buffer.append(JavaElementLabels.getTextLabel(elements[index], JavaElementLabels.ALL_FULLY_QUALIFIED));
						} else {
							buffer.append(LINE_DELIMITER);
							buffer.append(RefactoringCoreMessages.JavaRefactoringDescriptor_not_available);
						}
					}
					fInferredSettings.add(buffer.toString());
				}
			}
		}
		if (object instanceof IReferenceUpdating) {
			final IReferenceUpdating updating= (IReferenceUpdating) object;
			if (updating.canEnableUpdateReferences() && updating.getUpdateReferences())
				fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_update_references);
		}
		if (object instanceof ISimilarDeclarationUpdating) {
			final ISimilarDeclarationUpdating updating= (ISimilarDeclarationUpdating) object;
			if (updating.canEnableSimilarDeclarationUpdating() && updating.getUpdateSimilarDeclarations()) {
				final int strategy= updating.getMatchStrategy();
				if (strategy == RenamingNameSuggestor.STRATEGY_EXACT)
					fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_rename_similar);
				else if (strategy == RenamingNameSuggestor.STRATEGY_EMBEDDED)
					fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_rename_similar_embedded);
				else if (strategy == RenamingNameSuggestor.STRATEGY_SUFFIX)
					fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_rename_similar_suffix);
			}
		}
		if (object instanceof IQualifiedNameUpdating) {
			final IQualifiedNameUpdating updating= (IQualifiedNameUpdating) object;
			if (updating.canEnableQualifiedNameUpdating() && updating.getUpdateQualifiedNames()) {
				final String patterns= updating.getFilePatterns();
				if (patterns != null && !"".equals(patterns)) //$NON-NLS-1$
					fInferredSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_qualified_names_pattern, patterns.trim()));
				else
					fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_qualified_names);
			}
		}
		if (object instanceof ITextUpdating) {
			final ITextUpdating updating= (ITextUpdating) object;
			if (updating.canEnableTextUpdating())
				fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_textual_occurrences);
		}
		if (object instanceof IDelegateUpdating) {
			final IDelegateUpdating updating= (IDelegateUpdating) object;
			if (updating.canEnableDelegateUpdating() && updating.getDelegateUpdating()) {
				if (updating.getDeprecateDelegates())
					fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_keep_original_deprecated);
				else
					fInferredSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_keep_original);
			}
		}
	}
}
