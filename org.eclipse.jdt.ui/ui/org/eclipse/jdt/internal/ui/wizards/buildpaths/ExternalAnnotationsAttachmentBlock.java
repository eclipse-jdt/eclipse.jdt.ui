/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Frits Jalvingh <jal@etc.to> - Contribution for Bug 459831 - [launching] Support attaching external annotations to a JRE container
 *     Stephan Herrmann - Contribution for Bug 463936 - ExternalAnnotationsAttachmentDialog should not allow virtual folders
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.icu.text.MessageFormat;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaPrecomputedNamesAssistProcessor;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ComboDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;


/**
 * UI to set the External Annotations path. Same implementation for both setting attachments for
 * libraries from variable entries and for normal (internal or external) jar.
 */
public class ExternalAnnotationsAttachmentBlock {

	private final IStatusChangeListener fContext;

	private StringButtonDialogField fWorkspaceFileNameField;
	private StringButtonDialogField fExternalFileNameField;
	private SelectionButtonDialogField fExternalFolderButton;
	private SelectionButtonDialogField fExternalRadio, fWorkspaceRadio, fContainerRelativeRadio;
	private ComboDialogField fContainerSelection;
	private StringDialogField fContainerRelative;

	private Map<String,String> fContainerDesc2ID;
	private Map<String,IClasspathContainer> fID2Container;
	private JavaPrecomputedNamesAssistProcessor fArtifactAssistprocessor;

	private IStatus fWorkspaceNameStatus;
	private IStatus fExternalNameStatus;
	private IStatus fContainerRelativeStatus;

	private final IWorkspaceRoot fWorkspaceRoot;

	private Control fSWTWidget;

	private final IPath fEntry;

	/**
	 * @param context listeners for status updates
	 * @param entry The entry to edit
	 * @param javaProject The enclosing java project
	 */
	public ExternalAnnotationsAttachmentBlock(IStatusChangeListener context, IPath entry, IJavaProject javaProject) {
		fContext= context;
		fEntry= entry == null ? Path.EMPTY : entry;

		fWorkspaceRoot= ResourcesPlugin.getWorkspace().getRoot();

		fWorkspaceNameStatus= new StatusInfo();
		fExternalNameStatus= new StatusInfo();

		AnnotationsAttachmentAdapter adapter= new AnnotationsAttachmentAdapter();

		// create the dialog fields (no widgets yet)
		fWorkspaceRadio= new SelectionButtonDialogField(SWT.RADIO);
		fWorkspaceRadio.setDialogFieldListener(adapter);
		fWorkspaceRadio.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_workspace_radiolabel);

		fWorkspaceFileNameField= new StringButtonDialogField(adapter);
		fWorkspaceFileNameField.setDialogFieldListener(adapter);
		fWorkspaceFileNameField.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_filename_workspace_label);
		fWorkspaceFileNameField.setButtonLabel(NewWizardMessages.AnnotationsAttachmentBlock_filename_workspace_browse);

		fExternalRadio= new SelectionButtonDialogField(SWT.RADIO);
		fExternalRadio.setDialogFieldListener(adapter);
		fExternalRadio.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_external_radiolabel);

		fExternalFileNameField= new StringButtonDialogField(adapter);
		fExternalFileNameField.setDialogFieldListener(adapter);
		fExternalFileNameField.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_filename_external_label);
		fExternalFileNameField.setButtonLabel(NewWizardMessages.AnnotationsAttachmentBlock_filename_externalfile_button);

		fExternalFolderButton= new SelectionButtonDialogField(SWT.PUSH);
		fExternalFolderButton.setDialogFieldListener(adapter);
		fExternalFolderButton.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_filename_externalfolder_button);

		fContainerRelativeRadio= new SelectionButtonDialogField(SWT.RADIO);
		fContainerRelativeRadio.setDialogFieldListener(adapter);
		fContainerRelativeRadio.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_containerRelative_radio);

		createContainerMaps(javaProject);
		fContainerSelection= new ComboDialogField(SWT.READ_ONLY);
		fContainerSelection.setDialogFieldListener(adapter);
		fContainerSelection.setItems(fContainerDesc2ID.keySet().toArray(String[]::new));
		fContainerSelection.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_container_label);

		fContainerRelative= new StringDialogField();
		fContainerRelative.setDialogFieldListener(adapter);
		fContainerRelative.setLabelText(NewWizardMessages.AnnotationsAttachmentBlock_artifact_label);

		setDefaults();
	}

	private void createContainerMaps(IJavaProject javaProject) {
		fContainerDesc2ID= new HashMap<>();
		fID2Container= new HashMap<>();
		if (javaProject != null) {
			try {
				for (IClasspathEntry entry : javaProject.getRawClasspath()) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						IPath path= entry.getPath();
						IClasspathContainer container= JavaCore.getClasspathContainer(path, javaProject);
						fContainerDesc2ID.put(container.getDescription(), path.toString());
						fID2Container.put(path.toString(), container);
					}
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
		} else {
			// workaround for missing java project:
			// fetch all registered containers and use ID also instead of readable description
			for (String containerId : IClasspathContainer.getRegisteredContainerIds()) {
				fContainerDesc2ID.put(containerId, containerId);
			}
		}
	}

	public void setDefaults() {
		IPath annotationsAttachmentPath= fEntry;
		String path= annotationsAttachmentPath == null ? null : annotationsAttachmentPath.toPortableString();

		if (isWorkspacePath(annotationsAttachmentPath)) {
			fWorkspaceRadio.setSelection(true);
			fWorkspaceFileNameField.setText(path);
		} else if (path != null && path.length() != 0) {
			Entry<String, String> container= getContainerPrefix(path);
			if (container != null) {
				fContainerRelativeRadio.setSelection(true);
				fContainerSelection.selectItem(container.getKey());
				fContainerRelative.setText(path.substring(container.getValue().length()+1));
			} else {
				fExternalRadio.setSelection(true);
				fExternalFileNameField.setText(path);
			}
		} else {
			fWorkspaceRadio.setSelection(true);
			fExternalRadio.setSelection(false);
		}
	}

	/* Answer description and ID of a container that is the prefix of path. */
	private Entry<String,String> getContainerPrefix(String path) {
		for (Entry<String, String> entry : fContainerDesc2ID.entrySet()) {
			if (path.startsWith(entry.getValue()+'/')) {
				return entry;
			}
		}
		return null;
	}

	private boolean isWorkspacePath(IPath path) {
		if (path == null || path.getDevice() != null)
			return false;
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		if (workspace == null)
			return false;
		return workspace.getRoot().findMember(path) != null;
	}

	/**
	 * Gets the external annotation path chosen by the user
	 * @return the external annotation path, or Path.EMPTY if no path is selected.
	 */
	public IPath getAnnotationsPath() {
		return getFilePath();
	}

	/**
	 * Creates the control
	 * @param parent the parent
	 * @return the created control
	 */
	public Control createControl(Composite parent) {
		PixelConverter converter= new PixelConverter(parent);

		fSWTWidget= parent;

		Composite composite= new Composite(parent, SWT.NONE);

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 3;
		composite.setLayout(layout);


		int widthHint= converter.convertWidthInCharsToPixels(60);

		GridData gd= new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3, 1);
		gd.widthHint= converter.convertWidthInCharsToPixels(50);

		Label message= new Label(composite, SWT.LEFT + SWT.WRAP);
		message.setLayoutData(gd);
		message.setText(NewWizardMessages.AnnotationsAttachmentBlock_message);

		fWorkspaceRadio.doFillIntoGrid(composite, 3);

		fWorkspaceFileNameField.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalIndent(fWorkspaceFileNameField.getLabelControl(null));
		Text workspaceFileNameField= fWorkspaceFileNameField.getTextControl(null);
		LayoutUtil.setWidthHint(workspaceFileNameField, widthHint);
		LayoutUtil.setHorizontalGrabbing(workspaceFileNameField);
		BidiUtils.applyBidiProcessing(workspaceFileNameField, StructuredTextTypeHandlerFactory.FILE);

		DialogField.createEmptySpace(composite, 3);

		fExternalRadio.doFillIntoGrid(composite, 3);
		fExternalFileNameField.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalIndent(fExternalFileNameField.getLabelControl(null));
		Text externalFileNameField= fExternalFileNameField.getTextControl(null);
		LayoutUtil.setWidthHint(externalFileNameField, widthHint);
		LayoutUtil.setHorizontalGrabbing(externalFileNameField);
		BidiUtils.applyBidiProcessing(externalFileNameField, StructuredTextTypeHandlerFactory.FILE);


		DialogField.createEmptySpace(composite, 2);

		fExternalFolderButton.doFillIntoGrid(composite, 1);

		fContainerRelativeRadio.doFillIntoGrid(composite, 3);

		fContainerSelection.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalIndent(fContainerSelection.getLabelControl(null));
		Combo containerSelection= fContainerSelection.getComboControl(null);
		LayoutUtil.setWidthHint(containerSelection, widthHint);
		LayoutUtil.setHorizontalGrabbing(containerSelection);

		fContainerRelative.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalIndent(fContainerRelative.getLabelControl(null));
		Text containerRelative= fContainerRelative.getTextControl(null);
		LayoutUtil.setWidthHint(containerRelative, widthHint);
		LayoutUtil.setHorizontalGrabbing(containerRelative);

		fWorkspaceRadio.attachDialogField(fWorkspaceFileNameField);
		fExternalRadio.attachDialogFields(new DialogField[] { fExternalFileNameField, fExternalFolderButton });
		fContainerRelativeRadio.attachDialogFields(new DialogField[] { fContainerSelection, fContainerRelative });

		Dialog.applyDialogFont(composite);

		if (fContainerRelativeRadio.isSelected()) {
			// continue initialization that started during setDefaults, but didn't have a textControl at that time
			configureArtifactAssist();
		}

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.EXTERNAL_ANNOTATIONS_ATTACHMENT_DIALOG);
		return composite;
	}


	private class AnnotationsAttachmentAdapter implements IStringButtonAdapter, IDialogFieldListener {

		// -------- IStringButtonAdapter --------
		@Override
		public void changeControlPressed(DialogField field) {
			attachmentChangeControlPressed(field);
		}

		// ---------- IDialogFieldListener --------
		@Override
		public void dialogFieldChanged(DialogField field) {
			attachmentDialogFieldChanged(field);
		}
	}

	private void attachmentChangeControlPressed(DialogField field) {
		if (field == fWorkspaceFileNameField) {
			IPath jarFilePath= chooseInternal();
			if (jarFilePath != null) {
				fWorkspaceFileNameField.setText(jarFilePath.toString());
			}
		} else if (field == fExternalFileNameField) {
			IPath jarFilePath= chooseExtJarFile();
			if (jarFilePath != null) {
				fExternalFileNameField.setText(jarFilePath.toString());
			}
		}
	}

	// ---------- IDialogFieldListener --------

	private void attachmentDialogFieldChanged(DialogField field) {
		if (field == fWorkspaceFileNameField) {
			fWorkspaceNameStatus= updateFileNameStatus(fWorkspaceFileNameField);
		} else if (field == fExternalFileNameField) {
			fExternalNameStatus= updateFileNameStatus(fExternalFileNameField);
		} else if (field == fExternalFolderButton) {
			IPath folderPath= chooseExtFolder();
			if (folderPath != null) {
				fExternalFileNameField.setText(folderPath.toString());
			}
			return;
		} else if (field == fContainerRelative || field == fContainerRelativeRadio || field == fContainerSelection) {
			if (field == fContainerSelection && fSWTWidget != null) {
				configureArtifactAssist();
			}
			StatusInfo status= new StatusInfo();
			if (fContainerSelection.getText().isEmpty()) {
				status.setError(NewWizardMessages.AnnotationsAttachmentBlock_missingContainer_error);
			} else if (fContainerRelative.getText().isEmpty()) {
				status.setError(NewWizardMessages.AnnotationsAttachmentBlock_missingArtifact_error);
			} else {
				String containerID= fContainerDesc2ID.get(fContainerSelection.getText());
				if (fID2Container.containsKey(containerID)) {
					IClasspathContainer container= fID2Container.get(containerID);
					boolean found= false;
					String relative= fContainerRelative.getText();
					for (IClasspathEntry classpathEntry : container.getClasspathEntries()) {
						String entryPath= classpathEntry.getPath().lastSegment().toString();
						// relative must be equal to entryPath (a) entirely, or (b) up-to a boundary char
						if (entryPath.equals(relative) ||
								(entryPath.startsWith(relative) && !Character.isLetterOrDigit(entryPath.charAt(relative.length())))) {
							found= true;
							break;
						}
					}
					if (!found) {
						status.setError(MessageFormat.format(NewWizardMessages.AnnotationsAttachmentBlock_artifactNotFound_error,
								fContainerRelative.getText(), fContainerSelection.getText()));
					}
				}
			}
			fContainerRelativeStatus= status;
		}
		doStatusLineUpdate();
	}

	private void doStatusLineUpdate() {
		IStatus status;
		boolean isWorkSpace= fWorkspaceRadio.isSelected();
		if (isWorkSpace) {
			fWorkspaceFileNameField.enableButton(canBrowseFileName());
			status= fWorkspaceNameStatus;
		} else if (fExternalRadio.isSelected()) {
			fExternalFileNameField.enableButton(canBrowseFileName());
			status= fExternalNameStatus;
		} else {
			status= fContainerRelativeStatus;
		}
		fContext.statusChanged(status);
	}

	private void configureArtifactAssist() {
		String id= fContainerDesc2ID.get(fContainerSelection.getText());
		if (id == null) return;
		IClasspathContainer container= fID2Container.get(id);
		if (container == null) return;
		List<String> names= new ArrayList<>();
		for (IClasspathEntry entry : container.getClasspathEntries()) {
			String path= guessArtifactName(entry.getPath().toString());
			names.add(path);
		}
		Text textControl= fContainerRelative.getTextControl(null);
		getArtifactAssistprocessor(textControl).setNames(names);
		if (names.size() == 1) {
			textControl.setText(names.iterator().next());
		}
	}

	private JavaPrecomputedNamesAssistProcessor getArtifactAssistprocessor(Text textControl) {
		if (fArtifactAssistprocessor == null) {
			fArtifactAssistprocessor= new JavaPrecomputedNamesAssistProcessor(Collections.emptyList(),
					JavaPlugin.getImageDescriptorRegistry().get(JavaPluginImages.DESC_OBJS_JAR));
			ControlContentAssistHelper.createTextContentAssistant(textControl, fArtifactAssistprocessor);
		}
		return fArtifactAssistprocessor;
	}

	private String guessArtifactName(String path) {
		int s= path.lastIndexOf('/')+1;
		for (int i=s; i<path.length(); i++) {
			if (!Character.isLetterOrDigit(path.charAt(i))) {
				if (i+1 < path.length()) {
					char next= path.charAt(i+1);
					if (Character.isLetter(next))
						continue;
				}
				return path.substring(s, i);
			}
		}
		return path.substring(s);
	}

	private boolean canBrowseFileName() { // FIXME remove
		return true;
	}

	private IStatus updateFileNameStatus(StringButtonDialogField field) {
		StatusInfo status= new StatusInfo();

		String fileName= field.getText();
		if (fileName.length() == 0) {
			// no external annotations file defined
			return status;
		} else {
			if (!Path.EMPTY.isValidPath(fileName)) {
				status.setError(NewWizardMessages.AnnotationsAttachmentBlock_filename_error_notvalid);
				return status;
			}
			IPath filePath= Path.fromOSString(fileName);
			File file= filePath.toFile();
			IResource res= fWorkspaceRoot.findMember(filePath);
			boolean exists;
			boolean isVirtual= false;
			if (res != null) {
				IPath location= res.getLocation();
				if (location != null) {
					exists= location.toFile().exists();
				} else {
					exists= res.exists();
				}
				isVirtual= res.isVirtual();
			} else {
				exists= file.exists();
			}
			if (!exists) {
				String message= Messages.format(NewWizardMessages.AnnotationsAttachmentBlock_filename_error_filenotexists, BasicElementLabels.getPathLabel(filePath, false));
				status.setError(message);
				return status;
			}
			if (isVirtual) {
				status.setError(NewWizardMessages.AnnotationsAttachmentBlock_filename_error_virtual);
				return status;
			}
			if (!filePath.isAbsolute()) {
				status.setError(NewWizardMessages.AnnotationsAttachmentBlock_filename_error_notabsolute);
				return status;
			}
		}
		return status;
	}

	private IPath getFilePath() {
		if (fContainerRelativeRadio.isSelected()) {
			String container= fContainerSelection.getText();
			return new Path(fContainerDesc2ID.get(container)).append(fContainerRelative.getText());
		}
		String filePath= fWorkspaceRadio.isSelected() ? fWorkspaceFileNameField.getText() : fExternalFileNameField.getText();
		return Path.fromOSString(filePath).makeAbsolute();
	}

	/*
	 * Opens a dialog to choose a jar from the file system.
	 */
	private IPath chooseExtJarFile() {
		IPath currPath= getFilePath();
		if (currPath.segmentCount() == 0) {
			currPath= fEntry;
		}

		if (ArchiveFileFilter.isArchivePath(currPath, true)) {
			currPath= currPath.removeLastSegments(1);
		}

		FileDialog dialog= new FileDialog(getShell(), SWT.SHEET);
		dialog.setText(NewWizardMessages.AnnotationsAttachmentBlock_extjardialog_text);
		dialog.setFilterExtensions(ArchiveFileFilter.JAR_ZIP_FILTER_EXTENSIONS);
		dialog.setFilterPath(currPath.toOSString());
		String res= dialog.open();
		if (res != null) {
			return Path.fromOSString(res).makeAbsolute();
		}
		return null;
	}

	private IPath chooseExtFolder() {
		IPath currPath= getFilePath();
		if (currPath.segmentCount() == 0) {
			currPath= fEntry;
		}
		if (ArchiveFileFilter.isArchivePath(currPath, true)) {
			currPath= currPath.removeLastSegments(1);
		}

		DirectoryDialog dialog= new DirectoryDialog(getShell(), SWT.SHEET);
		dialog.setMessage(NewWizardMessages.AnnotationsAttachmentBlock_extfolderdialog_message);
		dialog.setText(NewWizardMessages.AnnotationsAttachmentBlock_extfolderdialog_text);
		dialog.setFilterPath(currPath.toOSString());
		String res= dialog.open();
		if (res != null) {
			return Path.fromOSString(res).makeAbsolute();
		}
		return null;
	}

	/*
	 * Opens a dialog to choose an internal jar.
	 */
	private IPath chooseInternal() {
		String initSelection= fWorkspaceFileNameField.getText();

		ViewerFilter filter= new ArchiveFileFilter((List<IResource>) null, false, false);

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		IResource initSel= null;
		if (initSelection.length() > 0) {
			initSel= fWorkspaceRoot.findMember(new Path(initSelection));
		}
		if (initSel == null) {
			initSel= fWorkspaceRoot.findMember(fEntry);
		}

		FolderSelectionDialog dialog= new FolderSelectionDialog(getShell(), lp, cp);
		dialog.setAllowMultiple(false);
		dialog.addFilter(filter);
		dialog.setTitle(NewWizardMessages.AnnotationsAttachmentBlock_intjardialog_title);
		dialog.setMessage(NewWizardMessages.AnnotationsAttachmentBlock_intjardialog_message);
		dialog.setInput(fWorkspaceRoot);
		dialog.setInitialSelection(initSel);
		dialog.setValidator(selection -> {
			if (selection != null && selection.length == 1) {
				Object selectedObject= selection[0];
				if (selectedObject instanceof IResource && ((IResource) selectedObject).isVirtual())
					return new StatusInfo(IStatus.ERROR, NewWizardMessages.AnnotationsAttachmentBlock_filename_error_virtual);
			}
			return new Status(IStatus.OK, PlatformUI.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
		});
		if (dialog.open() == Window.OK) {
			IResource res= (IResource) dialog.getFirstResult();
			return res.getFullPath();
		}
		return null;
	}

	private Shell getShell() {
		if (fSWTWidget != null) {
			return fSWTWidget.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();
	}

	/**
	 * Creates a runnable that sets the annotations attachment by modifying the project's classpath.
	 * @param shell the shell
	 * @param newEntry the new entry
	 * @param jproject the Java project
	 * @param containerPath the path of the parent container or <code>null</code> if the element is not in a container
	 * @param isReferencedEntry <code>true</code> iff the entry has a {@link IClasspathEntry#getReferencingEntry() referencing entry}
	 * @return return the runnable
	 */
	public static IRunnableWithProgress getRunnable(final Shell shell, final IClasspathEntry newEntry, final IJavaProject jproject, final IPath containerPath, final boolean isReferencedEntry) {
		return monitor -> {
			try {
				String[] changedAttributes= { IClasspathAttribute.EXTERNAL_ANNOTATION_PATH };
				BuildPathSupport.modifyClasspathEntry(shell, newEntry, changedAttributes, jproject, containerPath, isReferencedEntry, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
	}
}
