/***********vv********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.util.SWTUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



/**
 * The code formatter preference page. 
 */

public class CodingStyleConfigurationBlock {
    
    private static final String PREF_LASTLOADPATH= JavaUI.ID_PLUGIN + ".codeformatter.loadpath"; //$NON-NLS-1$
	private static final String PREF_LASTSAVEPATH= JavaUI.ID_PLUGIN + ".codeformatter.savepath"; //$NON-NLS-1$
	
	
	private class XMLFileUpdater implements Observer {
		
		private final File fFile;
		
		public XMLFileUpdater() {
			fFile= getStoreFile();
			fProfileManager.addObserver(this);
		}

		public void update(Observable o, Object arg) {
			final int value= ((Integer)arg).intValue();
			switch (value) {
			case ProfileManager.PROFILE_DELETED_EVENT:
			case ProfileManager.PROFILE_RENAMED_EVENT:
			case ProfileManager.PROFILE_CREATED_EVENT:
			case ProfileManager.SETTINGS_CHANGED_EVENT:
				try {
					writeProfilesToFile(fProfileManager.getSortedProfiles(), fFile);
				} catch (Exception x) {
					JavaPlugin.log(x);
				}
			}
		}
	}
	
	

	private class ProfileComboController implements Observer, SelectionListener {
		private List fSortedProfiles;
		
		public ProfileComboController() {
			fProfileCombo.addSelectionListener(this);
			fProfileManager.addObserver(this);
			updateProfiles();
			updateSelection();
		}
		
		public void widgetSelected(SelectionEvent e) {
			final int index= fProfileCombo.getSelectionIndex();
			fProfileManager.setSelected((Profile)fSortedProfiles.get(index));
		}

		public void widgetDefaultSelected(SelectionEvent e) {}

		public void update(Observable o, Object arg) {
			if (arg == null) return;
			final int value= ((Integer)arg).intValue();
			switch (value) {
				case ProfileManager.PROFILE_CREATED_EVENT:
				case ProfileManager.PROFILE_DELETED_EVENT:
				case ProfileManager.PROFILE_RENAMED_EVENT:
					updateProfiles();
				case ProfileManager.SELECTION_CHANGED_EVENT:
					updateSelection();
			}
		}
		
		private void updateProfiles() {
			fSortedProfiles= fProfileManager.getSortedProfiles();
			fProfileCombo.setItems(fProfileManager.getSortedNames());
		}

		private void updateSelection() {
			fProfileCombo.setText(fProfileManager.getSelected().getName());
		}
	}
	
	private class ButtonController implements Observer, SelectionListener {
		
		public ButtonController() {
			fProfileManager.addObserver(this);
			fNewButton.addSelectionListener(this);
			fRenameButton.addSelectionListener(this);
			fEditButton.addSelectionListener(this);
			fDeleteButton.addSelectionListener(this);
			fSaveButton.addSelectionListener(this);
			fLoadButton.addSelectionListener(this);
			update(fProfileManager, null);
		}

		public void update(Observable o, Object arg) {
			final boolean state= ((ProfileManager)o).getSelected() instanceof CustomProfile;
			fEditButton.setEnabled(state);
			fDeleteButton.setEnabled(state);
			fSaveButton.setEnabled(state);
			fRenameButton.setEnabled(state);
		}

		public void widgetSelected(SelectionEvent e) {
			final Button button= (Button)e.widget;
			if (button == fEditButton)
				modifyButtonPressed();
			else if (button == fDeleteButton) 
				deleteButtonPressed();
			else if (button == fNewButton)
				newButtonPressed();
			else if (button == fLoadButton)
				loadButtonPressed();
			else if (button == fSaveButton)
				saveButtonPressed();
			else if (button == fRenameButton) 
				renameButtonPressed();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		
		private void renameButtonPressed() {
			if (!(fProfileManager.getSelected() instanceof CustomProfile)) return;
			final CustomProfile profile= (CustomProfile)fProfileManager.getSelected();
			final RenameProfileDialog renameDialog= new RenameProfileDialog(fComposite.getShell(), profile);
			renameDialog.open();
		}
		
		private void modifyButtonPressed() {
			final ModifyDialog modifyDialog= new ModifyDialog(fComposite.getShell(), fProfileManager.getSelected(), false);
			modifyDialog.open();
		}
		
		private void deleteButtonPressed() {
			if (MessageDialog.openQuestion(
				fComposite.getShell(), 
				FormatterMessages.getString("CodingStyleConfigurationBlock.delete_confirmation.title"), //$NON-NLS-1$
				FormatterMessages.getFormattedString("CodingStyleConfigurationBlock.delete_confirmation.question", fProfileManager.getSelected().getName()))) { //$NON-NLS-1$
				fProfileManager.deleteSelected();
			}
		}
		
		private void newButtonPressed() {
			final CreateProfileDialog p= new CreateProfileDialog(fComposite.getShell(), fProfileManager);
			if (p.open() != Window.OK) 
				return;
			if (!p.openEditDialog()) 
				return;
			final ModifyDialog modifyDialog= new ModifyDialog(fComposite.getShell(), p.getCreatedProfile(), true);
			modifyDialog.open();
		}
		
		private void saveButtonPressed() {
			final FileDialog dialog= new FileDialog(fComposite.getShell(), SWT.SAVE);
			dialog.setText(FormatterMessages.getString("CodingStyleConfigurationBlock.save_profile.dialog.title")); //$NON-NLS-1$
			dialog.setFilterExtensions(new String [] {"*.xml"}); //$NON-NLS-1$
			
			final String lastPath= JavaPlugin.getDefault().getDialogSettings().get(PREF_LASTSAVEPATH);
			if (lastPath != null) {
				dialog.setFilterPath(lastPath);
			}
			final String path= dialog.open();
			if (path == null) 
				return;
			
			JavaPlugin.getDefault().getDialogSettings().put(PREF_LASTSAVEPATH, dialog.getFilterPath());
			
			final File file= new File(path);
			final Collection profiles= new ArrayList();
			profiles.add(fProfileManager.getSelected());
			try {
				writeProfilesToFile(profiles, file);
			} catch (Exception x) {
				final String title= FormatterMessages.getString("CodingStyleConfigurationBlock.save_profile.error.title"); //$NON-NLS-1$
				final String message= FormatterMessages.getString("CodingStyleConfigurationBlock.save_profile.error.message"); //$NON-NLS-1$
				MessageDialog.openError(fComposite.getShell(), title, message);
			}
		}
		
		private void loadButtonPressed() {
			final FileDialog dialog= new FileDialog(fComposite.getShell(), SWT.OPEN);
			dialog.setText(FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.dialog.title")); //$NON-NLS-1$
			dialog.setFilterExtensions(new String [] {"*.xml"}); //$NON-NLS-1$
			final String lastPath= JavaPlugin.getDefault().getDialogSettings().get(PREF_LASTLOADPATH);
			if (lastPath != null) {
				dialog.setFilterPath(lastPath);
			}
			final String path= dialog.open();
			if (path == null) 
				return;
			JavaPlugin.getDefault().getDialogSettings().put(PREF_LASTLOADPATH, dialog.getFilterPath());
			
			final File file= new File(path);
			Collection profiles= null;
			try {
				profiles= readProfilesFromFile(file);
			} catch (Exception e) {
				final String title= FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.error.title"); //$NON-NLS-1$
				final String message= FormatterMessages.getString("CodingStyleConfigurationBlock.load_profile.error.message"); //$NON-NLS-1$
				MessageDialog.openError(fComposite.getShell(), title, message);
			}
			if (profiles == null || profiles.isEmpty())
				return;
			
			final CustomProfile profile= (CustomProfile)profiles.iterator().next();
			if (fProfileManager.containsName(profile.getName())) {
				final AlreadyExistsDialog aeDialog= new AlreadyExistsDialog(fComposite.getShell(), profile, fProfileManager);
				if (aeDialog.open() != Window.OK) 
					return;
			}
			fProfileManager.addProfile(profile);
		}
	}
	
	private class PreviewController implements Observer {

		public PreviewController() {
			fProfileManager.addObserver(this);
			fJavaPreview.setWorkingValues(fProfileManager.getSelected().getSettings());
			fJavaPreview.update();
		}
		
		public void update(Observable o, Object arg) {
			final int value= ((Integer)arg).intValue();
			switch (value) {
				case ProfileManager.PROFILE_CREATED_EVENT:
				case ProfileManager.PROFILE_DELETED_EVENT:
				case ProfileManager.SELECTION_CHANGED_EVENT:
				case ProfileManager.SETTINGS_CHANGED_EVENT:
					fJavaPreview.setWorkingValues(((ProfileManager)o).getSelected().getSettings());
					fJavaPreview.update();
			}
		}
		
	}

	
	/**
	 * Identifiers for the XML file.
	 */
	private final static String XML_NODE_ROOT= "profiles"; //$NON-NLS-1$
	private final static String XML_NODE_PROFILE= "profile"; //$NON-NLS-1$
	private final static String XML_NODE_SETTING= "setting"; //$NON-NLS-1$
	
	private final static String XML_ATTRIBUTE_VERSION= "version"; //$NON-NLS-1$
	private final static String XML_ATTRIBUTE_ID= "id"; //$NON-NLS-1$
	private final static String XML_ATTRIBUTE_NAME= "name"; //$NON-NLS-1$
	private final static String XML_ATTRIBUTE_VALUE= "value"; //$NON-NLS-1$

	
	/**
	 * The version of the XML file
	 */
	private final static int XML_VERSION= 1;
	
	
	/**
	 * The name of the store file.
	 */
	protected final static String STORE_FILE= "code_formatter_profiles.xml"; //$NON-NLS-1$
	
	
	/**
	 * Some Java source code used for preview.
	 */
	private final static String fPreview=
		"/**\n* " + //$NON-NLS-1$
		FormatterMessages.getString("CodingStyleConfigurationBlock.preview.title") + //$NON-NLS-1$
		"\n*/\n\n" + //$NON-NLS-1$
		"package mypackage; import java.util.LinkedList; public class MyIntStack {" + //$NON-NLS-1$
		"private final LinkedList fStack;" + //$NON-NLS-1$
		"public MyIntStack(){fStack= new LinkedList();}" + //$NON-NLS-1$
		"public int pop(){return ((Integer)fStack.removeFirst()).intValue();}" + //$NON-NLS-1$
		"public void push(int elem){fStack.addFirst(new Integer(elem));}" + //$NON-NLS-1$
		"public boolean isEmpty() {return fStack.isEmpty();}" + //$NON-NLS-1$
		"}"; //$NON-NLS-1$

	/**
	 * The GUI controls
	 */
	protected Composite fComposite;
	protected Combo fProfileCombo;
	protected Button fEditButton;
	protected Button fRenameButton;
	protected Button fDeleteButton;
	protected Button fNewButton;
	protected Button fLoadButton;
	protected Button fSaveButton;
	
	/**
	 * The ProfileManager, the model of this page.
	 */
	protected final ProfileManager fProfileManager;
	
	/**
	 * The JavaPreview.
	 */
	protected final JavaPreview fJavaPreview;
	private PixelConverter fPixConv;

	
	/**
	 * Create a new <code>CodeFormatterPreferencePage</code>.
	 */
	public CodingStyleConfigurationBlock() {
		Collection profiles= null;
		try {
			profiles= readProfilesFromFile(getStoreFile());
		} catch (Exception e) {
			JavaPlugin.log(e);
		}
		if (profiles == null)
			profiles= new ArrayList();
		fProfileManager= new ProfileManager(profiles);
		Profile selected= fProfileManager.getSelected();
		fJavaPreview= new JavaPreview(selected.getSettings());
		fJavaPreview.setPreviewText(fPreview);
		new XMLFileUpdater();
	}

	/**
	 * Create the contents
	 */
	public Composite createContents(Composite parent) {

		final int numColumns = 5;
		
		fPixConv = new PixelConverter(parent);
		fComposite = createComposite(parent, numColumns, false);

		createLabel(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.select_profile.text"), numColumns); //$NON-NLS-1$
		fProfileCombo= createProfileCombo(fComposite, numColumns - 3, fPixConv.convertWidthInCharsToPixels(20));
		fEditButton= createButton(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.edit_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$
		fRenameButton= createButton(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.rename_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$
		fDeleteButton= createButton(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.remove_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$

		final Composite group= createComposite(fComposite, 4, false);
		final GridData groupData= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		groupData.horizontalSpan= numColumns;
		group.setLayoutData(groupData);

		fNewButton= createButton(group, FormatterMessages.getString("CodingStyleConfigurationBlock.new_button.desc"), GridData.HORIZONTAL_ALIGN_BEGINNING); //$NON-NLS-1$
		((GridData)createLabel(group, "", 1).getLayoutData()).grabExcessHorizontalSpace= true; //$NON-NLS-1$
		fLoadButton= createButton(group, FormatterMessages.getString("CodingStyleConfigurationBlock.load_button.desc"), GridData.HORIZONTAL_ALIGN_END); //$NON-NLS-1$
		fSaveButton= createButton(group, FormatterMessages.getString("CodingStyleConfigurationBlock.save_button.desc"), GridData.HORIZONTAL_ALIGN_END); //$NON-NLS-1$

		createLabel(fComposite, FormatterMessages.getString("CodingStyleConfigurationBlock.preview_label.text"), numColumns); //$NON-NLS-1$
		configurePreview(fComposite, numColumns);
		
		new ButtonController();
		new ProfileComboController();
		new PreviewController();
		
		return fComposite;
	}

	
	private static Button createButton(Composite composite, String text, final int style) {
		final Button button= new Button(composite, SWT.PUSH);
		button.setText(text);

		final GridData gd= new GridData(style);
		gd.widthHint= SWTUtil.getButtonWidthHint(button);
		gd.heightHint= SWTUtil.getButtonHeightHint(button);
		button.setLayoutData(gd);
		return button;
	}
	
	private static Combo createProfileCombo(Composite composite, int span, int widthHint) {
		final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		gd.widthHint= widthHint;

		final Combo combo= new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY );
		combo.setLayoutData(gd);
		return combo;
	}
	
	private Label createLabel(Composite composite, String text, int numColumns) {
		final GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = numColumns;
		gd.widthHint= 0;

		final Label label = new Label(composite, SWT.WRAP);
		label.setText(text);
		label.setLayoutData(gd);
		return label;		
	}
	
	private Composite createComposite(Composite parent, int numColumns, boolean margins) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout(numColumns, false);
		if (margins) {
			layout.marginHeight= fPixConv.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			layout.marginWidth= fPixConv.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		} else {
			layout.marginHeight = 0;
			layout.marginWidth = 0;
		}
		layout.horizontalSpacing= fPixConv.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing= fPixConv.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);
		return composite;
	}
	
	private void configurePreview(Composite composite, int numColumns) {
		final GridData gd = new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = numColumns;
		gd.verticalSpan= 7;
		gd.widthHint = 0;
		gd.heightHint = 0;
		final Control control= fJavaPreview.createContents(composite);
		control.setLayoutData(gd);
		
	}

	protected IPreferenceStore doGetPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
	
	public void performOk() {
		fProfileManager.commitChanges();
	}	
	
	
	/**
	 * Read the available profiles from the internal XML file and return them
	 * as collection.
	 */
	public Collection readProfilesFromFile(File file) throws CoreException, IOException {
		
		if (!file.exists())
			return null;
		
		Reader reader= new FileReader(file);
		if (reader == null) 
			return null;
		try {
			return readProfilesFromStream(reader);
		} finally {
			if (reader != null)
				try { reader.close(); } catch (IOException e) {}
		}
	}
	
	
	/**
	 * Load profiles from a XML stream and add them to a map.
	 */
	private Collection readProfilesFromStream(Reader reader) throws CoreException {
		
		Element cpElement;
		try {
		    final DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			final DocumentBuilder parser = factory.newDocumentBuilder();
			cpElement = parser.parse(new InputSource(reader)).getDocumentElement();
			
		} catch (SAXException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		} catch (IOException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message"));  //$NON-NLS-1$
		}

		if (cpElement == null || !cpElement.getNodeName().equalsIgnoreCase(XML_NODE_ROOT)) {
		    throw createException(new Exception(), FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message")); //$NON-NLS-1$
		}
		
		final NodeList list= cpElement.getChildNodes();
		
		final int length= list.getLength();
		
		final Collection profiles= new ArrayList();

		for (int i= 0; i < length; i++) {
			final Node node= list.item(i);
			final short type= node.getNodeType();
			if (type != Node.ELEMENT_NODE)
			    continue; // white space 
			final Element element= (Element) node;
			if (!element.getNodeName().equalsIgnoreCase(XML_NODE_PROFILE)) {
			    throw createException(null, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message")); //$NON-NLS-1$
			}	
			profiles.add(createProfileFromElement(element));
		}
		return profiles;
	}
	

	/**
	 * Create a new custom profile from its XML description.
	 */
	private CustomProfile createProfileFromElement(final Element element) throws CoreException {

	    final Map settings= ProfileManager.getDefaultSettings();
		final String name= element.getAttribute(XML_ATTRIBUTE_NAME);
		final NodeList list= element.getChildNodes();
		
		for (int i= 0; i < list.getLength(); i++) {
		    
			final Node node= list.item(i);
			
			if (node.getNodeType() != Node.ELEMENT_NODE)
			    continue; // white space
			
			final Element setting= (Element) node;
			
			if (!setting.getNodeName().equalsIgnoreCase(XML_NODE_SETTING)) {
			    throw createException(null, FormatterMessages.getString("CodingStyleConfigurationBlock.error.reading_xml.message")); //$NON-NLS-1$
			}	
			
			final String id= setting.getAttribute(XML_ATTRIBUTE_ID);
			final String value= setting.getAttribute(XML_ATTRIBUTE_VALUE);
			
			if (settings.containsKey(id)) {
			    settings.put(id, value);
			}
		}
		return new CustomProfile(name, settings);
	}
	
	
	/**
	 * Write the available profiles to the internal XML file.
	 */
	public static void writeProfilesToFile(Collection profiles, File file) throws IOException, CoreException {
		Writer writer= new FileWriter(file);
		try {
			writeProfilesToStream(profiles, writer);
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	
	/**
	 * Save profiles to an XML stream
	 */
	private static void writeProfilesToStream(Collection profiles, Writer writer) throws CoreException {
		try {
			final DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder= factory.newDocumentBuilder();		
			final Document document= builder.newDocument();
			
			final Element rootElement = document.createElement(XML_NODE_ROOT);
			rootElement.setAttribute(XML_ATTRIBUTE_VERSION, Integer.toString(XML_VERSION));
			document.appendChild(rootElement);
			
			final Iterator iter= profiles.iterator();
			
			while (iter.hasNext()) {
				final Profile p= (Profile)iter.next();
				if (p instanceof CustomProfile) {
					final Element profile= createProfileElement((CustomProfile)p, document);
					rootElement.appendChild(profile);
				}
			}

			Transformer transformer=TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
			transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(writer);

			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.serializing_xml.message"));  //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw createException(e, FormatterMessages.getString("CodingStyleConfigurationBlock.error.serializing_xml.message")); //$NON-NLS-1$
		}
	}

	
	/**
	 * Create a new profile element in the specified document. The profile is not added
	 * to the document by this method. 
	 */
	private static Element createProfileElement(CustomProfile profile, Document document) {
		final Element element= document.createElement(XML_NODE_PROFILE);
		element.setAttribute(XML_ATTRIBUTE_NAME, profile.getName());
		
		final Iterator keyIter= profile.getSettings().keySet().iterator();
		
		while (keyIter.hasNext()) {
			final String key= (String)keyIter.next();
			final String value= (String)profile.getSettings().get(key);
			final Element setting= document.createElement(XML_NODE_SETTING);
			setting.setAttribute(XML_ATTRIBUTE_ID, key);
			setting.setAttribute(XML_ATTRIBUTE_VALUE, value);
			element.appendChild(setting);
		}
		return element;
	}
	
	/**
	 * Get a <code>File</code> object representing the internal store file.
	 */
	protected static File getStoreFile() {
		return JavaPlugin.getDefault().getStateLocation().append(STORE_FILE).toFile();
	}

	
	/**
	 * Creates a UI exception for logging purposes
	 */
	private static JavaUIException createException(Throwable t, String message) {
		return new JavaUIException(JavaUIStatus.createError(IStatus.ERROR, message, t));
	}
}
