/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDelegateUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ISimilarDeclarationUpdating;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.util.Messages;

/**
 * Helper class to generate a refactoring descriptor comment.
 *
 * @since 3.2
 */
public final class JDTRefactoringDescriptorComment {

	/** The element delimiter */
	private static final String ELEMENT_DELIMITER= RefactoringCoreMessages.JavaRefactoringDescriptorComment_element_delimiter;

	/** The line delimiter */
	private static final String LINE_DELIMITER= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Creates a composite setting.
	 *
	 * @param caption
	 *            the caption
	 * @param settings
	 *            the settings
	 * @return the composite setting
	 */
	public static String createCompositeSetting(final String caption, final String[] settings) {
		Assert.isNotNull(caption);
		Assert.isNotNull(settings);
		final StringBuilder buffer= new StringBuilder(128);
		for (String setting : settings) {
			if (setting != null && !"".equals(setting)) { //$NON-NLS-1$
				buffer.append(LINE_DELIMITER);
				buffer.append(ELEMENT_DELIMITER);
				buffer.append(setting);
			} else {
				buffer.append(LINE_DELIMITER);
				buffer.append(ELEMENT_DELIMITER);
				buffer.append(RefactoringCoreMessages.JavaRefactoringDescriptor_not_available);
			}
		}
		if (buffer.length() > 0)
			buffer.insert(0, caption);
		return buffer.toString();
	}

	/** The header of the comment */
	private final String fHeader;

	/** The project name, or <code>null</code> */
	private final String fProject;

	/** The settings list */
	private final List<String> fSettings= new ArrayList<>(6);

	/**
	 * Creates a new JDT refactoring descriptor comment.
	 *
	 * @param project
	 *            the project name, or <code>null</code>
	 * @param object
	 *            the refactoring object to generate a comment for
	 * @param header
	 *            the header of the comment (typically the unique description of
	 *            the refactoring with fully qualified element names)
	 */
	public JDTRefactoringDescriptorComment(final String project, final Object object, final String header) {
		Assert.isNotNull(object);
		Assert.isNotNull(header);
		fProject= project;
		fHeader= header;
		initializeInferredSettings(object);
	}

	/**
	 * Adds the specified setting to this comment.
	 *
	 * @param index
	 *            the index
	 * @param setting
	 *            the setting to add
	 */
	public void addSetting(final int index, final String setting) {
		Assert.isTrue(index >= 0);
		Assert.isNotNull(setting);
		Assert.isTrue(!"".equals(setting)); //$NON-NLS-1$
		fSettings.add(index, setting);
	}

	/**
	 * Adds the specified setting to this comment.
	 *
	 * @param setting
	 *            the setting to add, or <code>null</code> for no setting
	 */
	public void addSetting(final String setting) {
		if (setting != null && !"".equals(setting)) //$NON-NLS-1$
			fSettings.add(setting);
	}

	/**
	 * Returns this comment in a human-readable string representation.
	 *
	 * @return this comment in string representation
	 */
	public String asString() {
		final StringBuilder buffer= new StringBuilder(256);
		buffer.append(fHeader);
		if (fProject != null && !"".equals(fProject)) { //$NON-NLS-1$
			buffer.append(LINE_DELIMITER);
			buffer.append(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptorComment_original_project, BasicElementLabels.getResourceName(fProject)));
		}
		for (String setting : fSettings) {
			buffer.append(LINE_DELIMITER);
			buffer.append(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_inferred_setting_pattern, setting));
		}
		return buffer.toString();
	}

	/**
	 * Returns the number of settings.
	 *
	 * @return the number of settings
	 */
	public int getCount() {
		return fSettings.size();
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
			fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_original_element_pattern, JavaElementLabelsCore.getTextLabel(updating.getElements()[0], JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
			try {
				final Object element= updating.getNewElement();
				if (element != null)
					fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_renamed_element_pattern, JavaElementLabelsCore.getTextLabel(element, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
				else {
					final String newLabel= BasicElementLabels.getJavaElementName(updating.getCurrentElementName());
					fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_renamed_element_pattern, newLabel));
				}
			} catch (CoreException exception) {
				JavaManipulationPlugin.log(exception);
			}
		} else if (object instanceof RefactoringProcessor) {
			final RefactoringProcessor processor= (RefactoringProcessor) object;
			final Object[] elements= processor.getElements();
			if (elements != null) {
				if (elements.length == 1 && elements[0] != null)
					fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_original_element_pattern, JavaElementLabelsCore.getTextLabel(elements[0], JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
				else if (elements.length > 1) {
					final StringBuilder buffer= new StringBuilder(128);
					buffer.append(RefactoringCoreMessages.JavaRefactoringDescriptor_original_elements);
					for (Object element : elements) {
						if (element != null) {
							buffer.append(LINE_DELIMITER);
							buffer.append(ELEMENT_DELIMITER);
							buffer.append(JavaElementLabelsCore.getTextLabel(element, JavaElementLabelsCore.ALL_FULLY_QUALIFIED));
						} else {
							buffer.append(LINE_DELIMITER);
							buffer.append(ELEMENT_DELIMITER);
							buffer.append(RefactoringCoreMessages.JavaRefactoringDescriptor_not_available);
						}
					}
					fSettings.add(buffer.toString());
				}
			}
		} else if (object instanceof IReorgPolicy) {
			final IReorgPolicy policy= (IReorgPolicy) object;
			Object destination= policy.getJavaElementDestination();
			if (destination != null)
				fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptorComment_destination_pattern, JavaElementLabelsCore.getTextLabel(destination, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
			else {
				destination= policy.getResourceDestination();
				if (destination != null)
					fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptorComment_destination_pattern, JavaElementLabelsCore.getTextLabel(destination, JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
			}
			final List<IAdaptable> list= new ArrayList<>();
			list.addAll(Arrays.asList(policy.getJavaElements()));
			list.addAll(Arrays.asList(policy.getResources()));
			final Object[] elements= list.toArray();
			if (elements != null) {
				if (elements.length == 1 && elements[0] != null)
					fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_original_element_pattern, JavaElementLabelsCore.getTextLabel(elements[0], JavaElementLabelsCore.ALL_FULLY_QUALIFIED)));
				else if (elements.length > 1) {
					final StringBuilder buffer= new StringBuilder(128);
					buffer.append(RefactoringCoreMessages.JavaRefactoringDescriptor_original_elements);
					for (Object element : elements) {
						if (element != null) {
							buffer.append(LINE_DELIMITER);
							buffer.append(ELEMENT_DELIMITER);
							buffer.append(JavaElementLabelsCore.getTextLabel(element, JavaElementLabelsCore.ALL_FULLY_QUALIFIED));
						} else {
							buffer.append(LINE_DELIMITER);
							buffer.append(ELEMENT_DELIMITER);
							buffer.append(RefactoringCoreMessages.JavaRefactoringDescriptor_not_available);
						}
					}
					fSettings.add(buffer.toString());
				}
			}
			if (object instanceof IMovePolicy) {
				final IMovePolicy extended= (IMovePolicy) object;
				if (extended.isTextualMove())
					fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptorComment_textual_move_only);
			}
		}
		if (object instanceof IReferenceUpdating) {
			final IReferenceUpdating updating= (IReferenceUpdating) object;
			if (updating.getUpdateReferences())
				fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_update_references);
		}
		if (object instanceof ISimilarDeclarationUpdating) {
			final ISimilarDeclarationUpdating updating= (ISimilarDeclarationUpdating) object;
			if (updating.canEnableSimilarDeclarationUpdating() && updating.getUpdateSimilarDeclarations()) {
				final int strategy= updating.getMatchStrategy();
				switch (strategy) {
				case RenamingNameSuggestor.STRATEGY_EXACT:
					fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_rename_similar);
					break;
				case RenamingNameSuggestor.STRATEGY_EMBEDDED:
					fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_rename_similar_embedded);
					break;
				case RenamingNameSuggestor.STRATEGY_SUFFIX:
					fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_rename_similar_suffix);
					break;
				default:
					break;
				}
			}
		}
		if (object instanceof IQualifiedNameUpdating) {
			final IQualifiedNameUpdating updating= (IQualifiedNameUpdating) object;
			if (updating.canEnableQualifiedNameUpdating() && updating.getUpdateQualifiedNames()) {
				final String patterns= updating.getFilePatterns();
				if (patterns != null && !"".equals(patterns)) //$NON-NLS-1$
					fSettings.add(Messages.format(RefactoringCoreMessages.JavaRefactoringDescriptor_qualified_names_pattern, BasicElementLabels.getFilePattern(patterns.trim())));
				else
					fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_qualified_names);
			}
		}
		if (object instanceof ITextUpdating) {
			final ITextUpdating updating= (ITextUpdating) object;
			if (updating.canEnableTextUpdating())
				fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_textual_occurrences);
		}
		if (object instanceof IDelegateUpdating) {
			final IDelegateUpdating updating= (IDelegateUpdating) object;
			if (updating.canEnableDelegateUpdating() && updating.getDelegateUpdating()) {
				if (updating.getDeprecateDelegates())
					fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_keep_original_deprecated);
				else
					fSettings.add(RefactoringCoreMessages.JavaRefactoringDescriptor_keep_original);
			}
		}
	}

	/**
	 * Removes the setting at the specified index.
	 *
	 * @param index
	 *            the index
	 */
	public void removeSetting(final int index) {
		Assert.isTrue(index >= 0);
		fSettings.remove(index);
	}
}
