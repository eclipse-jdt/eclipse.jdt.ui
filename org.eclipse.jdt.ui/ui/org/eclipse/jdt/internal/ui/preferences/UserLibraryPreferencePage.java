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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementSorter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListLabelProvider;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPUserLibraryElement;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.*;

public class UserLibraryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID= "org.eclipse.jdt.ui.preferences.UserLibraryPreferencePage"; //$NON-NLS-1$

	public static class LibraryNameDialog extends StatusDialog implements IDialogFieldListener {

		private StringDialogField fNameField;
		private SelectionButtonDialogField fIsSystemField;

		private CPUserLibraryElement fElementToEdit;
		private List fExistingLibraries;

		public LibraryNameDialog(Shell parent, CPUserLibraryElement elementToEdit, List existingLibraries) {
			super(parent);
			if (elementToEdit == null) { 
				setTitle(PreferencesMessages.getString("UserLibraryPreferencePage.LibraryNameDialog.new.title")); //$NON-NLS-1$
			} else {
				setTitle(PreferencesMessages.getString("UserLibraryPreferencePage.LibraryNameDialog.edit.title")); //$NON-NLS-1$
			}
			
			fElementToEdit= elementToEdit;
			fExistingLibraries= existingLibraries;
			
			fNameField= new StringDialogField();
			fNameField.setDialogFieldListener(this);
			fNameField.setLabelText(PreferencesMessages.getString("UserLibraryPreferencePage.LibraryNameDialog.name.label")); //$NON-NLS-1$
			
			fIsSystemField= new SelectionButtonDialogField(SWT.CHECK);
			fIsSystemField.setLabelText(PreferencesMessages.getString("UserLibraryPreferencePage.LibraryNameDialog.issystem.label")); //$NON-NLS-1$
			
			if (elementToEdit != null) {
				fNameField.setText(elementToEdit.getName());
				fIsSystemField.setSelection(elementToEdit.isSystemLibrary());
			} else {
				fNameField.setText(""); //$NON-NLS-1$
				fIsSystemField.setSelection(false);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			LayoutUtil.doDefaultLayout(composite, new DialogField[] { fNameField, fIsSystemField }, true, SWT.DEFAULT, SWT.DEFAULT);
			fNameField.postSetFocusOnDialogField(parent.getDisplay());
			
			Dialog.applyDialogFont(composite);
			return composite;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fNameField) {
				updateStatus(validateSettings());
			}
		}
		
		private IStatus validateSettings() {
			String name= fNameField.getText();
			if (name.length() == 0) {
				return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LibraryNameDialog.name.error.entername")); //$NON-NLS-1$
			}
			for (int i= 0; i < fExistingLibraries.size(); i++) {
				CPUserLibraryElement curr= (CPUserLibraryElement) fExistingLibraries.get(i);
				if (curr != fElementToEdit && name.equals(curr.getName())) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getFormattedString("UserLibraryPreferencePage.LibraryNameDialog.name.error.exists", name)); //$NON-NLS-1$
				}
			}
			IStatus status= ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE);
			if (status.matches(IStatus.ERROR)) {
				return new StatusInfo(IStatus.ERROR, "Name contains invalid characters."); //$NON-NLS-1$
			}
			return StatusInfo.OK_STATUS;
		}
		
		public CPUserLibraryElement getNewLibrary() {
			CPListElement[] entries= null;
			if (fElementToEdit != null) {
				entries= fElementToEdit.getChildren();
			}
			return new CPUserLibraryElement(fNameField.getText(), fIsSystemField.isSelected(), entries);
		}
		
	}
	
	public static class LoadSaveDialog extends StatusDialog implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		
		private static final String CURRENT_VERSION= "1"; //$NON-NLS-1$

		private static final String TAG_ROOT= "eclipse-userlibraries"; //$NON-NLS-1$
		private static final String TAG_VERSION= "version"; //$NON-NLS-1$
		private static final String TAG_LIBRARY= "library"; //$NON-NLS-1$
		private static final String TAG_SOURCEATTACHMENT= "source"; //$NON-NLS-1$
		private static final String TAG_ARCHIVE_PATH= "path"; //$NON-NLS-1$
		private static final String TAG_ARCHIVE= "archive"; //$NON-NLS-1$
		private static final String TAG_SYSTEMLIBRARY= "systemlibrary"; //$NON-NLS-1$
		private static final String TAG_NAME= "name"; //$NON-NLS-1$
		private static final String TAG_JAVADOC= "javadoc"; //$NON-NLS-1$

		private static final String PREF_LASTPATH= JavaUI.ID_PLUGIN + ".lastuserlibrary"; //$NON-NLS-1$
		private static final String PREF_USER_LIBRARY_LOADSAVE_SIZE= "UserLibraryLoadSaveDialog.size"; //$NON-NLS-1$
		
		private List fExistingLibraries;
		private IDialogSettings fSettings;
		
		private File fLastFile;
		
		private StringButtonDialogField fLocationField;
		private CheckedListDialogField fExportImportList;
		private Point fInitialSize;
		private final boolean fIsSave;

		public LoadSaveDialog(Shell shell, boolean isSave, List existingLibraries, IDialogSettings dialogSettings) {
			super(shell);
			setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
			
			PixelConverter converter= new PixelConverter(shell);
			
			fExistingLibraries= existingLibraries;
			fSettings= dialogSettings;
			fLastFile= null;
			fIsSave= isSave;
			
			int defaultWidth= converter.convertWidthInCharsToPixels(80);
			int defaultHeigth= converter.convertHeightInCharsToPixels(34);
			String lastSize= fSettings.get(PREF_USER_LIBRARY_LOADSAVE_SIZE);
			if (lastSize != null) {
				fInitialSize= StringConverter.asPoint(lastSize, new Point(defaultWidth, defaultHeigth));
			} else {
				fInitialSize= new Point(defaultWidth, defaultHeigth);
			}
		
			if (isSave()) {
				setTitle(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.save.title")); //$NON-NLS-1$
			} else {
				setTitle(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.title"));				 //$NON-NLS-1$
			}
			
			fLocationField= new StringButtonDialogField(this);
			fLocationField.setLabelText(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.location.label")); //$NON-NLS-1$
			fLocationField.setButtonLabel(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.location.button")); //$NON-NLS-1$
			fLocationField.setDialogFieldListener(this);
			
			String[] buttonNames= new String[] {
					PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.list.selectall.button"), //$NON-NLS-1$
					PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.list.deselectall.button") //$NON-NLS-1$
			};
			fExportImportList= new CheckedListDialogField(this, buttonNames, new CPListLabelProvider());
			fExportImportList.setCheckAllButtonIndex(0);
			fExportImportList.setUncheckAllButtonIndex(1);
			fExportImportList.setViewerSorter(new CPListElementSorter());
			if (isSave()) {
				fExportImportList.setLabelText(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.list.save.label")); //$NON-NLS-1$
				fExportImportList.setElements(fExistingLibraries);
				fExportImportList.checkAll(true);
			} else {
				fExportImportList.setLabelText(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.list.load.label")); //$NON-NLS-1$
			}
			String lastPath= fSettings.get(PREF_LASTPATH);
			if (lastPath != null) {
				fLocationField.setText(lastPath);
			} else {
				fLocationField.setText(""); //$NON-NLS-1$
			}
		}
				
		protected Point getInitialSize() {
			return fInitialSize;
		}
		
		private boolean isSave() {
			return fIsSave;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			DialogField[] fields;
			if (isSave()) {
				fields= new DialogField[] { fExportImportList, fLocationField };
			} else {
				fields= new DialogField[] { fLocationField, fExportImportList };
			}
			LayoutUtil.doDefaultLayout(composite, fields, true, SWT.DEFAULT, SWT.DEFAULT);
			fExportImportList.getListControl(null).setLayoutData(new GridData(GridData.FILL_BOTH));
			
			fLocationField.postSetFocusOnDialogField(parent.getDisplay());
			
			Dialog.applyDialogFont(composite);
			return composite;
		}
		

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void changeControlPressed(DialogField field) {
			String label= isSave() ? PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.filedialog.save.title") : PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.filedialog.load.title"); //$NON-NLS-1$ //$NON-NLS-2$
			FileDialog dialog= new FileDialog(getShell(), isSave() ? SWT.SAVE : SWT.OPEN);
			dialog.setText(label);
			dialog.setFilterExtensions(new String[] {"*.userlibraries", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			String lastPath= fLocationField.getText();
			if (lastPath.length() == 0 || !new File(lastPath).exists()) {
				lastPath= fSettings.get(PREF_LASTPATH);
			}
			if (lastPath != null) {
				dialog.setFileName(lastPath);
			}
			String fileName= dialog.open();
			if (fileName != null) {
				fSettings.put(PREF_LASTPATH, fileName);
				fLocationField.setText(fileName);
			}
		}

		private IStatus updateShownLibraries(IStatus status) {
			if (!status.isOK()) {
				fExportImportList.removeAllElements();
				fExportImportList.setEnabled(false);
				fLastFile= null;
			} else {
				File file= new File(fLocationField.getText());
				if (!file.equals(fLastFile)) {
					fLastFile= file;
					try {
						List elements= loadLibraries(file);
						fExportImportList.setElements(elements);
						fExportImportList.checkAll(true);
						fExportImportList.setEnabled(true);
						if (elements.isEmpty()) {
							return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.error.empty")); //$NON-NLS-1$
						}
					} catch (IOException e) {
						fExportImportList.removeAllElements();
						fExportImportList.setEnabled(false);
						return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.error.invalidfile")); //$NON-NLS-1$
					}
				}
			}
			return status;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
		 */
		public void dialogFieldChanged(DialogField field) {
			if (field == fLocationField) {
				IStatus status= validateSettings();
				if (!isSave()) {
					status= updateShownLibraries(status);
				}
				updateStatus(status);
			} else if (field == fExportImportList) {
				updateStatus(validateSettings());
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#customButtonPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField, int)
		 */
		public void customButtonPressed(ListDialogField field, int index) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#selectionChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void selectionChanged(ListDialogField field) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void doubleClicked(ListDialogField field) {
			List selectedElements= fExportImportList.getSelectedElements();
			if (selectedElements.size() == 1) {
				Object elem= selectedElements.get(0);
				fExportImportList.setChecked(elem, !fExportImportList.isChecked(elem));
			}
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
		 */
		protected void okPressed() {
			if (isSave()) {
				final File file= new File(fLocationField.getText());
				if (file.exists()) {
					String title= PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.overwrite.title"); //$NON-NLS-1$
					String message= PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.overwrite.message"); //$NON-NLS-1$
					if (!MessageDialog.openQuestion(getShell(), title, message)) {
						return;
					}
				}
				final List elements= fExportImportList.getCheckedElements();

				IRunnableContext context= PlatformUI.getWorkbench().getProgressService();
				try {
					context.run(true, true, new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							try {
								saveLibraries(elements, file, monitor);
							} catch (IOException e) {
								throw new InvocationTargetException(e);
							}
						}
					});
					fSettings.put(PREF_LASTPATH, file.getPath());
				} catch (InvocationTargetException e) {
					String errorTitle= PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.save.errordialog.title"); //$NON-NLS-1$
					String errorMessage= PreferencesMessages.getFormattedString("UserLibraryPreferencePage.LoadSaveDialog.save.errordialog.message", e.getMessage()); //$NON-NLS-1$
					ExceptionHandler.handle(e, getShell(), errorTitle, errorMessage);
					return;
				} catch (InterruptedException e) {
					// cancelled
					return;
				}
				String savedTitle= PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.save.ok.title"); //$NON-NLS-1$
				String savedMessage= PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.save.ok.message"); //$NON-NLS-1$
				MessageDialog.openInformation(getShell(), savedTitle, savedMessage);
			} else {
				HashSet map= new HashSet(fExistingLibraries.size());
				for (int k= 0; k < fExistingLibraries.size(); k++) {
					CPUserLibraryElement elem= (CPUserLibraryElement) fExistingLibraries.get(k);
					map.add(elem.getName());
				}
				int nReplaced= 0;
				List elements= getLoadedLibraries();
				for (int i= 0; i < elements.size(); i++) {
					CPUserLibraryElement curr= (CPUserLibraryElement) elements.get(i);
					if (map.contains(curr.getName())) {
						nReplaced++;
					}
				}
				if (nReplaced > 0) {
					String replaceTitle= PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.replace.title"); //$NON-NLS-1$
					String replaceMessage;
					if (nReplaced == 1) {
						replaceMessage= PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.replace.message"); //$NON-NLS-1$
					} else {
						replaceMessage= PreferencesMessages.getFormattedString("UserLibraryPreferencePage.LoadSaveDialog.load.replace.multiple.message", String.valueOf(nReplaced)); //$NON-NLS-1$
					}
					if (!MessageDialog.openConfirm(getShell(), replaceTitle, replaceMessage)) {
						return;
					}
				}
			}
			super.okPressed();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#close()
		 */
		public boolean close() {
			Point point= getShell().getSize();
			fSettings.put(PREF_USER_LIBRARY_LOADSAVE_SIZE, StringConverter.asString(point));
			return super.close();
		}
		
		private IStatus validateSettings() {
			String name= fLocationField.getText();
			fLastFile= null;
			if (isSave()) {
				if (name.length() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.location.error.save.enterlocation")); //$NON-NLS-1$
				}
				File file= new File(name);
				if (file.isDirectory()) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.location.error.save.invalid")); //$NON-NLS-1$
				}
				if (fExportImportList.getCheckedSize() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.list.error.save.nothingselected")); //$NON-NLS-1$
				}
				fLastFile= file;
			} else {
				if (name.length() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.location.error.load.enterlocation")); //$NON-NLS-1$
				}
				if (!new File(name).isFile()) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.location.error.load.invalid")); //$NON-NLS-1$
				}
				if (fExportImportList.getSize() > 0 && fExportImportList.getCheckedSize() == 0) {
					return new StatusInfo(IStatus.ERROR, PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.list.error.load.nothingselected")); //$NON-NLS-1$
				}
			}
			return new StatusInfo();
		}
		
		protected static void saveLibraries(List libraries, File file, IProgressMonitor monitor) throws IOException {
			OutputStream stream= new FileOutputStream(file);
			try {
				DocumentBuilder docBuilder= null;
				DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				docBuilder= factory.newDocumentBuilder();
				Document document= docBuilder.newDocument();
	
				// Create the document
				Element rootElement= document.createElement(TAG_ROOT);
				document.appendChild(rootElement);
	
				rootElement.setAttribute(TAG_VERSION, CURRENT_VERSION); 
				
				for (int i= 0; i < libraries.size(); i++) {
					Element libraryElement= document.createElement(TAG_LIBRARY);
					rootElement.appendChild(libraryElement);
					
					CPUserLibraryElement curr= (CPUserLibraryElement) libraries.get(i);
					libraryElement.setAttribute(TAG_NAME, curr.getName());
					libraryElement.setAttribute(TAG_SYSTEMLIBRARY, String.valueOf(curr.isSystemLibrary()));
					
					CPListElement[] children= curr.getChildren();
					for (int k= 0; k < children.length; k++) {
						CPListElement child= children[k];
						
						Element childElement= document.createElement(TAG_ARCHIVE); 
						libraryElement.appendChild(childElement);
						
						childElement.setAttribute(TAG_ARCHIVE_PATH, child.getPath().toOSString());
						IPath sourceAttachment= (IPath) child.getAttribute(CPListElement.SOURCEATTACHMENT);
						if (sourceAttachment != null) {
							childElement.setAttribute(TAG_SOURCEATTACHMENT, sourceAttachment.toOSString());
	
						}
						URL javadocLocation= (URL) child.getAttribute(CPListElement.JAVADOC);
						if (javadocLocation != null) {
							childElement.setAttribute(TAG_JAVADOC, javadocLocation.toExternalForm());
						}					
					}
				}
	
				// Write the document to the stream
				Transformer transformer=TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","4"); //$NON-NLS-1$ //$NON-NLS-2$

				DOMSource source = new DOMSource(document);
				StreamResult result = new StreamResult(stream);
				transformer.transform(source, result);	
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} catch (TransformerException e) {
				throw new IOException(e.getMessage());			
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
				if (monitor != null) {
					monitor.done();
				}
			}
		}
				
		private static List loadLibraries(File file) throws IOException {
			InputStream stream= new FileInputStream(file);
			Element cpElement;
			try {
				DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				cpElement = parser.parse(new InputSource(stream)).getDocumentElement();
			} catch (SAXException e) {
				throw new IOException(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.badformat")); //$NON-NLS-1$
			} catch (ParserConfigurationException e) {
				throw new IOException(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.badformat")); //$NON-NLS-1$
			} finally {
				stream.close();
			}
			
			if (!cpElement.getNodeName().equalsIgnoreCase(TAG_ROOT)) {
				throw new IOException(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.badformat")); //$NON-NLS-1$
			}
			NodeList libList= cpElement.getElementsByTagName(TAG_LIBRARY);
			int length = libList.getLength();
			
			ArrayList result= new ArrayList(length);
			for (int i= 0; i < length; i++) {
				Node lib= libList.item(i);
				if (!(lib instanceof Element)) {
					continue;
				}
				Element libElement= (Element) lib;
				String name= libElement.getAttribute(TAG_NAME);
				boolean isSystem= Boolean.valueOf(libElement.getAttribute(TAG_SYSTEMLIBRARY)).booleanValue();
				
				CPUserLibraryElement newLibrary= new CPUserLibraryElement(name, isSystem, null);
				result.add(newLibrary);
				
				NodeList archiveList= libElement.getElementsByTagName(TAG_ARCHIVE);
				for (int k= 0; k < archiveList.getLength(); k++) {
					Node archiveNode= archiveList.item(k);
					if (!(archiveNode instanceof Element)) {
						continue;
					}
					Element archiveElement= (Element) archiveNode;
					
					String path= archiveElement.getAttribute(TAG_ARCHIVE_PATH);
					CPListElement newArchive= new CPListElement(newLibrary, null, IClasspathEntry.CPE_LIBRARY, new Path(path), null);
					newLibrary.add(newArchive);
					
					if (archiveElement.hasAttribute(TAG_SOURCEATTACHMENT)) {
						IPath sourceAttach= new Path(archiveElement.getAttribute(TAG_SOURCEATTACHMENT));
						newArchive.setAttribute(CPListElement.SOURCEATTACHMENT, sourceAttach);
					}
					if (archiveElement.hasAttribute(TAG_JAVADOC)) {
						try {
							URL javadoc= new URL(archiveElement.getAttribute(TAG_JAVADOC));
							newArchive.setAttribute(CPListElement.JAVADOC, javadoc);
						} catch (MalformedURLException e) {
							// ignore
						}
					}
				}
			}
			return result;
		}

		public List getLoadedLibraries() {
			return fExportImportList.getCheckedElements();
		}
	}
	
	private IDialogSettings fDialogSettings;
	private TreeListDialogField fLibraryList;
	private IJavaProject fDummyProject;
		
	private static final int IDX_NEW= 0;
	private static final int IDX_EDIT= 1;
	private static final int IDX_ADD= 2;
	private static final int IDX_REMOVE= 3;
	private static final int IDX_LOAD= 5;
	private static final int IDX_SAVE= 6;
	
	/**
	 * Constructor for ClasspathVariablesPreferencePage
	 */
	public UserLibraryPreferencePage() {
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		fDummyProject= getNonExistingProject();
	
		// title only used when page is shown programatically
		setTitle(PreferencesMessages.getString("UserLibraryPreferencePage.title")); //$NON-NLS-1$
		setDescription(PreferencesMessages.getString("UserLibraryPreferencePage.description")); //$NON-NLS-1$
		noDefaultAndApplyButton();

		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
		
		UserLibraryAdapter adapter= new UserLibraryAdapter();
		String[] buttonLabels= new String[] {
				PreferencesMessages.getString("UserLibraryPreferencePage.libraries.new.button"), //$NON-NLS-1$
				PreferencesMessages.getString("UserLibraryPreferencePage.libraries.edit.button"), //$NON-NLS-1$
				PreferencesMessages.getString("UserLibraryPreferencePage.libraries.addjar.button"), //$NON-NLS-1$
				PreferencesMessages.getString("UserLibraryPreferencePage.libraries.remove.button"), //$NON-NLS-1$
				null,
				PreferencesMessages.getString("UserLibraryPreferencePage.libraries.load.button"), //$NON-NLS-1$
				PreferencesMessages.getString("UserLibraryPreferencePage.libraries.save.button") //$NON-NLS-1$
		};
		
		fLibraryList= new TreeListDialogField(adapter, buttonLabels, new CPListLabelProvider());
		fLibraryList.setLabelText(PreferencesMessages.getString("UserLibraryPreferencePage.libraries.label")); //$NON-NLS-1$
		
		String[] names= JavaCore.getUserLibraryNames();
		ArrayList elements= new ArrayList();
		
		for (int i= 0; i < names.length; i++) {
			IPath path= new Path(JavaCore.USER_LIBRARY_CONTAINER_ID).append(names[i]);
			try {
				IClasspathContainer container= JavaCore.getClasspathContainer(path, fDummyProject);
				elements.add(new CPUserLibraryElement(names[i], container));
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				// ignore
			}
		}
		fLibraryList.setElements(elements);
		fLibraryList.setViewerSorter(new CPListElementSorter());
		
		doSelectionChanged(fLibraryList); //update button enable state 
	}
	
	private IJavaProject getNonExistingProject() {
		String name= "####internal"; //$NON-NLS-1$
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		while (true) {
			IProject project= root.getProject(name);
			if (!project.exists()) {
				return JavaCore.create(project);
			}
			name += '1';
		}		
	}
	
	
	/**
	 * Constructor to be used when programatically showing the page
	 * @param selectedLibrary The entry to be selected by default. 
	 */
	public UserLibraryPreferencePage(String selectedLibrary, boolean createIfNotFound) {
		this();
		if (selectedLibrary != null) {
			int nElements= fLibraryList.getSize();
			for (int i= 0; i < nElements; i++) {
				CPUserLibraryElement curr= (CPUserLibraryElement) fLibraryList.getElement(i);
				if (curr.getName().equals(selectedLibrary)) {
					fLibraryList.selectElements(new StructuredSelection(curr));
					fLibraryList.expandElement(curr, AbstractTreeViewer.ALL_LEVELS);
					break;
				}
			}
			if (createIfNotFound) {
				CPUserLibraryElement elem= new CPUserLibraryElement(selectedLibrary, null);
				fLibraryList.addElement(elem);
				fLibraryList.selectElements(new StructuredSelection(elem));
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.CP_USERLIBRARIES_PREFERENCE_PAGE);
	}	

	/*
	 * @see PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fLibraryList }, true);
		LayoutUtil.setHorizontalGrabbing(fLibraryList.getTreeControl(null));
		Dialog.applyDialogFont(composite);
		return composite;
	}
	
	/*
	 * @see IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		super.performDefaults();
	}

	/*
	 * @see PreferencePage#performOk()
	 */
	public boolean performOk() {
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() { 
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						if (monitor != null) {
							monitor= new NullProgressMonitor();
						}
						
						updateUserLibararies(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			String title= PreferencesMessages.getString("UserLibraryPreferencePage.config.error.title"); //$NON-NLS-1$
			String message= PreferencesMessages.getString("UserLibraryPreferencePage.config.error.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, getShell(), title, message);
		}
		return true;
	}
	
	
	private void updateUserLibararies(IProgressMonitor monitor) throws CoreException {
		List list= fLibraryList.getElements();
		HashSet oldNames= new HashSet(Arrays.asList(JavaCore.getUserLibraryNames()));
		int nExisting= list.size();
		
		HashSet newEntries= new HashSet(list.size());
		for (int i= 0; i < nExisting; i++) {
			CPUserLibraryElement element= (CPUserLibraryElement) list.get(i);
			boolean contained= oldNames.remove(element.getName());
			if (!contained) {
				newEntries.add(element);
			}
		}	
		
		int len= nExisting + oldNames.size();
		monitor.beginTask(PreferencesMessages.getString("UserLibraryPreferencePage.operation"), len); //$NON-NLS-1$
		MultiStatus multiStatus= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, PreferencesMessages.getString("UserLibraryPreferencePage.operation.error"), null); //$NON-NLS-1$
		
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(JavaCore.USER_LIBRARY_CONTAINER_ID);
		IJavaProject jproject= fDummyProject;
		
		IJavaProject[] projectInit= new IJavaProject[] { jproject };
		IClasspathContainer[] containerInit= new IClasspathContainer[] { null };
		
		ArrayList paths= new ArrayList();
		ArrayList urls= new ArrayList();
		
		for (int i= 0; i < nExisting; i++) {
			CPUserLibraryElement element= (CPUserLibraryElement) list.get(i);
			IPath path= element.getPath();
			if (newEntries.contains(element) || element.hasChanges(JavaCore.getClasspathContainer(path, jproject))) {
				IClasspathContainer updatedContainer= element.getUpdatedContainer();
				try {
					initializer.requestClasspathContainerUpdate(path, jproject, updatedContainer);
					containerInit[0]= updatedContainer;
					JavaCore.setClasspathContainer(path, projectInit, containerInit, null); // force updating of containers, bug 62250 
				} catch (CoreException e) {
					multiStatus.add(e.getStatus());
				}
			}
			element.collectJavaDocLocations(paths, urls);
			monitor.worked(1);
		}
		
		containerInit[0]= null;
		Iterator iter= oldNames.iterator();
		while (iter.hasNext()) {
			String name= (String) iter.next();
			
			IPath path= new Path(JavaCore.USER_LIBRARY_CONTAINER_ID).append(name);
			try {
				initializer.requestClasspathContainerUpdate(path, jproject, null);
				JavaCore.setClasspathContainer(path, projectInit, containerInit, null); // force updating of containers, bug 62250 
			} catch (CoreException e) {
				multiStatus.add(e.getStatus());
			}
			monitor.worked(1);
		}
		// save javadoc locations
		JavaUI.setLibraryJavadocLocations((IPath[]) paths.toArray(new IPath[paths.size()]), (URL[]) urls.toArray(new URL[paths.size()]));
		
		if (!multiStatus.isOK()) {
			throw new CoreException(multiStatus);
		}
	}
	
	private CPUserLibraryElement getSingleSelectedLibrary(List selected) {
		if (selected.size() == 1 && selected.get(0) instanceof CPUserLibraryElement) {
			return (CPUserLibraryElement) selected.get(0);
		}
		return null;
	}
	
	private void editAttributeEntry(CPListElementAttribute elem) {
		String key= elem.getKey();
		if (key.equals(CPListElement.SOURCEATTACHMENT)) {
			CPListElement selElement= elem.getParent();
			
			IClasspathEntry result= BuildPathDialogAccess.configureSourceAttachment(getShell(), selElement.getClasspathEntry());
			if (result != null) {
				selElement.setAttribute(CPListElement.SOURCEATTACHMENT, result.getSourceAttachmentPath());
				fLibraryList.refresh();
			}
		} else if (key.equals(CPListElement.JAVADOC)) {
			CPListElement selElement= elem.getParent();
			URL initialLocation= (URL) selElement.getAttribute(CPListElement.JAVADOC);
			String elementName= new CPListLabelProvider().getText(selElement);
			
			URL[] result= BuildPathDialogAccess.configureJavadocLocation(getShell(), elementName, initialLocation);
			if (result != null) {
				selElement.setAttribute(CPListElement.JAVADOC, result[0]);
				fLibraryList.refresh();
			}
		}
	}
	
	protected void doSelectionChanged(TreeListDialogField field) {
		List list= field.getSelectedElements();
		field.enableButton(IDX_REMOVE, canRemove(list));
		field.enableButton(IDX_EDIT, canEdit(list));
		field.enableButton(IDX_ADD, canAdd(list));
		field.enableButton(IDX_SAVE, field.getSize() > 0);	
	}
	
	protected void doCustomButtonPressed(TreeListDialogField field, int index) {
		if (index == IDX_NEW) {
			editUserLibraryElement(null);
		} else if (index == IDX_ADD) {
			doAdd(field.getSelectedElements());
		} else if (index == IDX_REMOVE) {
			doRemove(field.getSelectedElements());
		} else if (index == IDX_EDIT) {
			doEdit(field.getSelectedElements());
		} else if (index == IDX_SAVE) {
			doSave();
		} else if (index == IDX_LOAD) {
			doLoad();
		}
	}
	
	protected void doDoubleClicked(TreeListDialogField field) {
		List selected= field.getSelectedElements();
		if (canEdit(selected)) {
			doEdit(field.getSelectedElements());
		}
	}
	
	protected void doKeyPressed(TreeListDialogField field, KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {
			List selection= field.getSelectedElements();
			if (canRemove(selection)) {
				doRemove(selection);
			}
		}
	}
	
	private void doEdit(List selected) {
		if (selected.size() == 1) {
			Object curr= selected.get(0);
			if (curr instanceof CPListElementAttribute) {
				editAttributeEntry((CPListElementAttribute) curr);
			} else if (curr instanceof CPUserLibraryElement) {
				editUserLibraryElement((CPUserLibraryElement) curr);
			} else if (curr instanceof CPListElement) {
				CPListElement elem= (CPListElement) curr;
				openExtJarFileDialog(elem, elem.getParentContainer());
			}
		}
	}

	private void editUserLibraryElement(CPUserLibraryElement element) {
		LibraryNameDialog dialog= new LibraryNameDialog(getShell(), element, fLibraryList.getElements());
		if (dialog.open() == Window.OK) {
			CPUserLibraryElement newLibrary= dialog.getNewLibrary();
			if (element != null) {
				fLibraryList.replaceElement(element, newLibrary);
			} else {
				fLibraryList.addElement(newLibrary);
			}
			fLibraryList.expandElement(newLibrary, AbstractTreeViewer.ALL_LEVELS);
			fLibraryList.selectElements(new StructuredSelection(newLibrary));
		}
	}
	
	private void editArchiveElement(CPListElement existingElement, CPUserLibraryElement parent) {
		CPListElement[] elements= openExtJarFileDialog(existingElement, parent);
		if (elements != null) {
			for (int i= 0; i < elements.length; i++) {
				if (existingElement != null) {
					parent.replace(existingElement, elements[i]);
				} else {
					parent.add(elements[i]);
				}
			}
			fLibraryList.refresh(parent);
			fLibraryList.expandElement(parent, AbstractTreeViewer.ALL_LEVELS);
			fLibraryList.selectElements(new StructuredSelection(parent));
		}
	}


	private void doRemove(List selected) {
		Object selectionAfter= null;
		for (int i= 0; i < selected.size(); i++) {
			Object curr= selected.get(i);
			if (curr instanceof CPUserLibraryElement) {
				fLibraryList.removeElement(curr);
			} else if (curr instanceof CPListElement) {
				Object parent= ((CPListElement) curr).getParentContainer();
				if (parent instanceof CPUserLibraryElement) {
					CPUserLibraryElement elem= (CPUserLibraryElement) parent;
					elem.remove((CPListElement) curr);
					fLibraryList.refresh(elem);
					selectionAfter= parent;
				}
			} else if (curr instanceof CPListElementAttribute) {
				CPListElementAttribute attrib= (CPListElementAttribute) curr;
				attrib.getParent().setAttribute(attrib.getKey(), null);
				fLibraryList.refresh(attrib);
			}
		}
		if (fLibraryList.getSelectedElements().isEmpty()) {
			if (selectionAfter != null) {
				fLibraryList.selectElements(new StructuredSelection(selectionAfter));
			} else {
				fLibraryList.selectFirstElement();	
			}
		}
	}

	private void doAdd(List list) {
		if (canAdd(list)) {
			CPUserLibraryElement element= getSingleSelectedLibrary(list);
			editArchiveElement(null, element);
		}
	}
	
	private void doLoad() {
		List existing= fLibraryList.getElements();
		LoadSaveDialog dialog= new LoadSaveDialog(getShell(), false, existing, fDialogSettings);
		if (dialog.open() == Window.OK) {
			HashMap map= new HashMap(existing.size());
			for (int k= 0; k < existing.size(); k++) {
				CPUserLibraryElement elem= (CPUserLibraryElement) existing.get(k);
				map.put(elem.getName(), elem);
			}
			
			List list= dialog.getLoadedLibraries();
			for (int i= 0; i < list.size(); i++) {
				CPUserLibraryElement elem= (CPUserLibraryElement) list.get(i);
				CPUserLibraryElement found= (CPUserLibraryElement) map.get(elem.getName());
				if (found == null) {
					existing.add(elem);
					map.put(elem.getName(), elem);
				} else {
					existing.set(existing.indexOf(found), elem); // replace
				}
			}
			fLibraryList.setElements(existing);
			fLibraryList.selectElements(new StructuredSelection(list));
		}
	}
	
	private void doSave() {
		LoadSaveDialog dialog= new LoadSaveDialog(getShell(), true, fLibraryList.getElements(), fDialogSettings);
		dialog.open();
	}
	
	private boolean canAdd(List list) {
		return getSingleSelectedLibrary(list) != null;
	}

	private boolean canEdit(List list) {
		if (list.size() == 1) {
			return true;
		}
		return false;
	}

	private boolean canRemove(List list) { 
		return !list.isEmpty();
	}

	private CPListElement[] openExtJarFileDialog(CPListElement existing, Object parent) {
		String lastUsedPath;
		if (existing != null) {
			lastUsedPath= existing.getPath().removeLastSegments(1).toOSString();
		} else {
			lastUsedPath= fDialogSettings.get(IUIConstants.DIALOGSTORE_LASTEXTJAR);
			if (lastUsedPath == null) {
				lastUsedPath= ""; //$NON-NLS-1$
			}
		}
		String title= (existing == null) ? PreferencesMessages.getString("UserLibraryPreferencePage.browsejar.new.title") : PreferencesMessages.getString("UserLibraryPreferencePage.browsejar.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
		
		FileDialog dialog= new FileDialog(getShell(), existing == null ? SWT.MULTI : SWT.SINGLE);
		dialog.setText(title);
		dialog.setFilterExtensions(new String[] {"*.jar;*.zip"}); //$NON-NLS-1$
		dialog.setFilterPath(lastUsedPath);
		if (existing != null) {
			dialog.setFileName(existing.getPath().lastSegment());
		}
		
		String res= dialog.open();
		if (res == null) {
			return null;
		}
		String[] fileNames= dialog.getFileNames();
		int nChosen= fileNames.length;
			
		IPath filterPath= new Path(dialog.getFilterPath());
		CPListElement[] elems= new CPListElement[nChosen];
		for (int i= 0; i < nChosen; i++) {
			IPath path= filterPath.append(fileNames[i]).makeAbsolute();	
			CPListElement curr= new CPListElement(parent, null, IClasspathEntry.CPE_LIBRARY, path, null);
			curr.setAttribute(CPListElement.SOURCEATTACHMENT, BuildPathSupport.guessSourceAttachment(curr));
			curr.setAttribute(CPListElement.JAVADOC, JavaUI.getLibraryJavadocLocation(curr.getPath()));
			elems[i]= curr;
		}
		fDialogSettings.put(IUIConstants.DIALOGSTORE_LASTEXTJAR, filterPath.toOSString());
		
		return elems;
	}

	
	private class UserLibraryAdapter implements ITreeListAdapter {
		
		private final Object[] EMPTY= new Object[0];

		public void customButtonPressed(TreeListDialogField field, int index) {
			doCustomButtonPressed(field, index);
		}

		public void selectionChanged(TreeListDialogField field) {
			doSelectionChanged(field);
		}
		
		public void doubleClicked(TreeListDialogField field) {
			doDoubleClicked(field);
		}

		public void keyPressed(TreeListDialogField field, KeyEvent event) {
			doKeyPressed(field, event);
		}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element instanceof CPUserLibraryElement) {
				CPUserLibraryElement elem= (CPUserLibraryElement) element;
				return elem.getChildren();
			} else if (element instanceof CPListElement) {
				return ((CPListElement)element).getChildren(false);
			}
			return EMPTY;
		}

		public Object getParent(TreeListDialogField field, Object element) {
			if (element instanceof CPListElementAttribute) {
				return ((CPListElementAttribute) element).getParent();
			} else if (element instanceof CPListElement) {
				return ((CPListElement) element).getParentContainer();
			} 
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return getChildren(field, element).length > 0;
		}
				
	}

	
}
