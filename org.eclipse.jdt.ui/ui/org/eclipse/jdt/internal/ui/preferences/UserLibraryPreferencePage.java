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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

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
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.userlibrary.UserLibrary;
import org.eclipse.jdt.internal.corext.userlibrary.UserLibraryManager;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementSorter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListLabelProvider;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPUserLibraryElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.JavadocPropertyDialog;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentDialog;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

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
			return Status.OK_STATUS;
		}
		
		public CPUserLibraryElement getNewLibrary() {
			CPListElement[] entries= null;
			if (fElementToEdit != null) {
				entries= fElementToEdit.getChildren();
			}
			return new CPUserLibraryElement(fNameField.getText(), fIsSystemField.isSelected(), entries);
		}
		
	}
	
	public static class LoadSaveDialog extends StatusDialog implements IStringButtonAdapter, IDialogFieldListener {
		
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

		private List fExistingLibraries;
		private IDialogSettings fSettings;
		
		private File fLastFile;
		
		private StringButtonDialogField fLocationField;
		private CheckedListDialogField fExportImportList;
		private Point fInitialSize;

		public LoadSaveDialog(Shell shell, List existingLibraries, IDialogSettings dialogSettings) {
			super(shell);
			setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
			
			PixelConverter converter= new PixelConverter(shell);
			fInitialSize= new Point(converter.convertWidthInCharsToPixels(80), converter.convertHeightInCharsToPixels(34));
			
			fExistingLibraries= existingLibraries;
			fSettings= dialogSettings;
			fLastFile= null;
			
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
			fExportImportList= new CheckedListDialogField(null, buttonNames, new CPListLabelProvider());
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
			return fExistingLibraries != null;
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

				BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
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
				} catch (InterruptedException e) {
					// cancelled
					return;
				}
			}
			super.okPressed();
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
			FileOutputStream outputStream= new FileOutputStream(file);
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
				StreamResult result = new StreamResult(outputStream);
				transformer.transform(source, result);
			} catch (ParserConfigurationException e) {
				throw new IOException(e.getMessage());
			} catch (TransformerException e) {
				throw new IOException(e.getMessage());			
			} finally {
				try {
					outputStream.close();
				} catch (IOException e) {
					// ignore
				}
				if (monitor != null) {
					monitor.done();
				}
			}
		}
				
		private static List loadLibraries(File file) throws IOException {
			Reader reader= new FileReader(file);
			Element cpElement;
			try {
				DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				cpElement = parser.parse(new InputSource(reader)).getDocumentElement();
			} catch (SAXException e) {
				throw new IOException(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.badformat")); //$NON-NLS-1$
			} catch (ParserConfigurationException e) {
				throw new IOException(PreferencesMessages.getString("UserLibraryPreferencePage.LoadSaveDialog.load.badformat")); //$NON-NLS-1$
			} finally {
				reader.close();
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
					CPListElement newArchive= new CPListElement(null, IClasspathEntry.CPE_LIBRARY, new Path(path), null);
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
		
		String[] names= UserLibraryManager.getUserLibraryNames();
		ArrayList elements= new ArrayList();
		for (int i= 0; i < names.length; i++) {
			elements.add(new CPUserLibraryElement(names[i]));
		}
		fLibraryList.setElements(elements);
		fLibraryList.setViewerSorter(new CPListElementSorter());
		
		doSelectionChanged(fLibraryList); //update button enable state 
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
				CPUserLibraryElement elem= new CPUserLibraryElement(selectedLibrary);
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
		ProgressMonitorDialog dialog= new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() { 
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						updateUserLibararies(monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
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
	
	
	private void updateUserLibararies(IProgressMonitor monitor) throws JavaModelException {
		List list= fLibraryList.getElements();
		HashSet oldNames= new HashSet(Arrays.asList(UserLibraryManager.getUserLibraryNames()));
		int nExisting= list.size();
		
		for (int i= 0; i < nExisting; i++) {
			CPUserLibraryElement element= (CPUserLibraryElement) list.get(i);
			oldNames.remove(element.getName());
		}	
		
		ArrayList paths= new ArrayList();
		ArrayList urls= new ArrayList();
		
		int len= nExisting + oldNames.size();
		String[] newNames= new String[len];
		UserLibrary[] newLibs= new UserLibrary[len];
		for (int i= 0; i < nExisting; i++) {
			CPUserLibraryElement element= (CPUserLibraryElement) list.get(i);
			newNames[i]= element.getName();
			newLibs[i]= element.getUserLibrary();			
			
			element.collectJavaDocLocations(paths, urls);
		}
		Iterator iter= oldNames.iterator();
		for (int i= nExisting; i < len; i++) {
			newNames[i]= (String) iter.next();
			newLibs[i]= null;
		}
		// save javadoc locations
		JavaUI.setLibraryJavadocLocations((IPath[]) paths.toArray(new IPath[paths.size()]), (URL[]) urls.toArray(new URL[paths.size()]));
		
		// save libararies
		UserLibraryManager.setUserLibraries(newNames, newLibs, monitor);
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
			
			IPath containerPath= null;
			SourceAttachmentDialog dialog= new SourceAttachmentDialog(getShell(), selElement.getClasspathEntry(), containerPath, null, false);
			if (dialog.open() == Window.OK) {
				selElement.setAttribute(CPListElement.SOURCEATTACHMENT, dialog.getSourceAttachmentPath());
				fLibraryList.refresh();
			}
		} else if (key.equals(CPListElement.JAVADOC)) {
			CPListElement selElement= elem.getParent();
			JavadocPropertyDialog dialog= new JavadocPropertyDialog(getShell(), selElement);
			if (dialog.open() == Window.OK) {
				selElement.setAttribute(CPListElement.JAVADOC, dialog.getJavaDocLocation());
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
		LoadSaveDialog dialog= new LoadSaveDialog(getShell(), null, fDialogSettings);
		if (dialog.open() == Window.OK) {

			List existing= fLibraryList.getElements();
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
		LoadSaveDialog dialog= new LoadSaveDialog(getShell(), fLibraryList.getElements(), fDialogSettings);
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
		return true;
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
		String title= (existing == null) ? PreferencesMessages.getString("UserLibraryPreferencePage.browsejar.new.title") : PreferencesMessages.getString("UserLibraryPreferencePage.browseJAR.edit.title"); //$NON-NLS-1$ //$NON-NLS-2$
		
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
