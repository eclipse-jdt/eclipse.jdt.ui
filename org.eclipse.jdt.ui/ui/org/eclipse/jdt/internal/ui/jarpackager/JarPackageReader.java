/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.io.BufferedInputStream;import java.io.IOException;import java.io.InputStream;import java.io.ObjectInputStream;import java.util.ArrayList;import java.util.List;import javax.xml.parsers.DocumentBuilder;import javax.xml.parsers.DocumentBuilderFactory;import javax.xml.parsers.ParserConfigurationException;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IFolder;import org.eclipse.core.resources.IProject;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.MultiStatus;import org.eclipse.core.runtime.Path;import org.eclipse.core.runtime.Status;import org.eclipse.jface.util.Assert;import org.w3c.dom.Element;import org.w3c.dom.Node;import org.w3c.dom.NodeList;import org.xml.sax.InputSource;import org.xml.sax.SAXException;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.internal.ui.JavaPlugin;
/**
 * Reads data from an InputStream and returns a JarPackage
 */
public class JarPackageReader extends Object {

	protected InputStream fInputStream;

	private MultiStatus fWarnings;
	
	/**
	 * Reads a Jar Package from the underlying stream.
	 * It is the clients responsiblity to close the stream.
	 **/
	public JarPackageReader(InputStream inputStream) {
		Assert.isNotNull(inputStream);
		fInputStream= new BufferedInputStream(inputStream);
		fWarnings= new MultiStatus(JavaPlugin.getPluginId(), 0, JarPackagerMessages.getString("JarPackageReader.jarPackageReaderWarnings"), null); //$NON-NLS-1$
	}
	/**
	 * Hook for possible subclasses
	 **/
	protected JarPackageReader() {
	}
	/**
     * Reads the JAR specification from the underlying stream.
     * 
     * @exception IOException				if writing to the underlying stream fails
     * @exception ClassNotFoundException	if one of the classes in the stream is not found
     * @deprecated As of 0.114, replaced by readXML - will be removed
     */
    public JarPackage readObject() throws IOException, ClassNotFoundException {
		JarPackage jarPackage= null;
		ObjectInputStream objectInput= new ObjectInputStream(fInputStream);
		jarPackage= (JarPackage)objectInput.readObject();
		return jarPackage;
    }
	/**
     * Closes this stream.
	 * It is the clients responsiblity to close the stream.
	 * 
	 * @exception IOException
     */
    public void close() throws IOException {
    	if (fInputStream != null)
			fInputStream.close();
	}

	public JarPackage readXML() throws IOException, SAXException {
		Element xmlJarDesc= null;
		JarPackage jarPackage= new JarPackage();
	  	DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
    	factory.setValidating(false);
		DocumentBuilder parser= null;
		try {
			parser= factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			// Note: Above code is ok since clients are responsible to close the stream
		}
		xmlJarDesc= parser.parse(new InputSource(fInputStream)).getDocumentElement();
		if (!xmlJarDesc.getNodeName().equals(JarPackage.DESCRIPTION_EXTENSION)) {
			throw new IOException(JarPackagerMessages.getString("JarPackageReader.error.badFormat")); //$NON-NLS-1$
		}
		NodeList topLevelElements= xmlJarDesc.getChildNodes();
		for (int i= 0; i < topLevelElements.getLength(); i++) {
			Node node= topLevelElements.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element element= (Element)node;
			if (element.getNodeName().equals(JarPackage.EXTENSION)) {
				jarPackage.setJarLocation(new Path(element.getAttribute("path"))); //$NON-NLS-1$
			}		
			if (element.getNodeName().equals("options")) { //$NON-NLS-1$
				jarPackage.setOverwrite(getBooleanAttribute(element, "overwrite")); //$NON-NLS-1$
				jarPackage.setCompress(getBooleanAttribute(element, "compress")); //$NON-NLS-1$
				jarPackage.setExportErrors(getBooleanAttribute(element, "exportErrors")); //$NON-NLS-1$
				jarPackage.setExportWarnings(getBooleanAttribute(element, "exportWarnings")); //$NON-NLS-1$
				jarPackage.setLogErrors(getBooleanAttribute(element, "logErrors")); //$NON-NLS-1$
				jarPackage.setLogWarnings(getBooleanAttribute(element, "logWarnings")); //$NON-NLS-1$
				jarPackage.setSaveDescription(getBooleanAttribute(element, "saveDescription")); //$NON-NLS-1$
				jarPackage.setDescriptionLocation(new Path(element.getAttribute("descriptionLocation"))); //$NON-NLS-1$
			}
			if (element.getNodeName().equals("manifest")) { //$NON-NLS-1$
				jarPackage.setManifestVersion(element.getAttribute("manifestVersion")); //$NON-NLS-1$
				jarPackage.setUsesManifest(getBooleanAttribute(element, "usesManifest")); //$NON-NLS-1$
				jarPackage.setReuseManifest(getBooleanAttribute(element, "reuseManifest")); //$NON-NLS-1$
				jarPackage.setSaveManifest(getBooleanAttribute(element,"saveManifest")); //$NON-NLS-1$
				jarPackage.setGenerateManifest(getBooleanAttribute(element, "generateManifest")); //$NON-NLS-1$
				jarPackage.setManifestLocation(new Path(element.getAttribute("manifestLocation"))); //$NON-NLS-1$
				jarPackage.setMainClass(getMainClass(element));
				/*
				 * Try to find sealing info. Could ask for single child node
				 * but this would stop others from adding more child nodes to
				 * the manifest node.
				 */
				NodeList sealingElementContainer= element.getChildNodes();
				for (int j= 0; j < sealingElementContainer.getLength(); j++) {
					Node sealingNode= sealingElementContainer.item(j);
					if (sealingNode.getNodeType() == Node.ELEMENT_NODE
						&& sealingNode.getNodeName().equals("sealing")) { //$NON-NLS-1$
						// Sealing
						Element sealingElement= (Element)sealingNode;
						jarPackage.setSealJar(getBooleanAttribute(sealingElement, "sealJar")); //$NON-NLS-1$
						jarPackage.setPackagesToSeal(getPackages(sealingElement.getElementsByTagName("packagesToSeal"))); //$NON-NLS-1$
						jarPackage.setPackagesToUnseal(getPackages(sealingElement.getElementsByTagName("packagesToUnSeal"))); //$NON-NLS-1$
					}		
				}
			}
			if (element.getNodeName().equals("selectedElements")) { //$NON-NLS-1$
				jarPackage.setExportClassFiles(getBooleanAttribute(element, "exportClassFiles")); //$NON-NLS-1$
				jarPackage.setExportJavaFiles(getBooleanAttribute(element, "exportJavaFiles")); //$NON-NLS-1$
				NodeList selectedElements= element.getChildNodes();
				for (int j= 0; j < selectedElements.getLength(); j++) {
					Node selectedNode= selectedElements.item(j);
					if (selectedNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					Element selectedElement= (Element)selectedNode;
					if (selectedElement.getNodeName().equals("file")) //$NON-NLS-1$
						addFile(jarPackage.getSelectedElements(), selectedElement);
					else if (selectedElement.getNodeName().equals("folder")) //$NON-NLS-1$
						addFolder(jarPackage.getSelectedElements() ,selectedElement);
					else if (selectedElement.getNodeName().equals("project")) //$NON-NLS-1$
						addProject(jarPackage.getSelectedElements() ,selectedElement);
					else if (selectedElement.getNodeName().equals("javaElement")) //$NON-NLS-1$
						addJavaElement(jarPackage.getSelectedElements() ,selectedElement);
					// Note: Other file types are not handled by this writer
				}
			}
		}
		return jarPackage;
	}

	protected boolean getBooleanAttribute(Element element, String name) throws IOException {
		String value= element.getAttribute(name);
		if (value != null && value.equalsIgnoreCase("true")) //$NON-NLS-1$
			return true;
		if (value != null && value.equalsIgnoreCase("false")) //$NON-NLS-1$
			return false;
		throw new IOException(JarPackagerMessages.getString("JarPackageReader.error.illegalValueForBooleanAttribute")); //$NON-NLS-1$
	}

	private void addFile(List selectedElements, Element element) throws IOException {
		IPath path= getPath(element);
		if (path != null) {
			IFile file= JavaPlugin.getWorkspace().getRoot().getFile(path);
			if (file != null)
				selectedElements.add(file);
		}
	}

	private void addFolder(List selectedElements, Element element) throws IOException {
		IPath path= getPath(element);
		if (path != null) {
			IFolder folder= JavaPlugin.getWorkspace().getRoot().getFolder(path);
			if (folder != null)
				selectedElements.add(folder);
		}
	}

	private void addProject(List selectedElements, Element element) throws IOException {
		String name= element.getAttribute("name"); //$NON-NLS-1$
		if (name.equals("")) //$NON-NLS-1$
			throw new IOException(JarPackagerMessages.getString("JarPackageReader.error.tagNameNotFound")); //$NON-NLS-1$
		IProject project= JavaPlugin.getWorkspace().getRoot().getProject(name);
		if (project != null)
			selectedElements.add(project);
	}

	private IPath getPath(Element element) throws IOException {
		String pathString= element.getAttribute("path"); //$NON-NLS-1$
		if (pathString.equals("")) //$NON-NLS-1$
			throw new IOException(JarPackagerMessages.getString("JarPackageReader.error.tagPathNotFound")); //$NON-NLS-1$
		return new Path(element.getAttribute("path")); //$NON-NLS-1$
	}
	
	private void addJavaElement(List selectedElements, Element element) throws IOException {
		String handleId= element.getAttribute("handleIdentifier"); //$NON-NLS-1$
		if (handleId.equals("")) //$NON-NLS-1$
			throw new IOException(JarPackagerMessages.getString("JarPackageReader.error.tagHandleIdentifierNotFoundOrEmpty")); //$NON-NLS-1$
		IJavaElement je= JavaCore.create(handleId);
		if (je == null)
			addWarning(JarPackagerMessages.getString("JarPackageReader.warning.javaElementDoesNotExist"), null); //$NON-NLS-1$
		else
			selectedElements.add(je);
	}

	private IPackageFragment[] getPackages(NodeList list) throws IOException {
		if (list.getLength() > 1)
			throw new IOException(JarPackagerMessages.getFormattedString("JarPackageReader.error.duplicateTag", list.item(0).getNodeName())); //$NON-NLS-1$
		if (list.getLength() == 0)
			return null; // optional entry is not present
		NodeList packageNodes= list.item(0).getChildNodes();
		List packages= new ArrayList(packageNodes.getLength());
		for (int i= 0; i < packageNodes.getLength(); i++) {
			Node packageNode= packageNodes.item(i);
			if (packageNode.getNodeType() == Node.ELEMENT_NODE && packageNode.getNodeName().equals("package")) { //$NON-NLS-1$
				String handleId= ((Element)packageNode).getAttribute("handleIdentifier"); //$NON-NLS-1$
				if (handleId.equals("")) //$NON-NLS-1$
					throw new IOException(JarPackagerMessages.getString("JarPackageReader.error.tagHandleIdentifierNotFoundOrEmpty")); //$NON-NLS-1$
				IJavaElement je= JavaCore.create(handleId);
				if (je != null && je.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
					packages.add(je);
				else
					addWarning(JarPackagerMessages.getString("JarPackageReader.warning.javaElementDoesNotExist"), null); //$NON-NLS-1$
			}					
		}
		return (IPackageFragment[])packages.toArray(new IPackageFragment[packages.size()]);
	}

	private IType getMainClass(Element element) {
		String handleId= element.getAttribute("mainClassHandleIdentifier"); //$NON-NLS-1$
		if (handleId.equals("")) //$NON-NLS-1$
			return null;	// Main-Class entry is optional or can be empty
		IJavaElement je= JavaCore.create(handleId);
		if (je != null && je.getElementType() == IJavaElement.TYPE)
			return (IType)je;
		addWarning(JarPackagerMessages.getString("JarPackageReader.warning.mainClassDoesNotExist"), null); //$NON-NLS-1$
		return null;
	}
	/**
	 * Returns the warnings of this operation. If there are no
	 * warnings, a status object with IStatus.OK is returned.
	 *
	 * @return the status of this operation
	 */
	public IStatus getWarnings() {
		if (fWarnings.getChildren().length == 0)
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null); //$NON-NLS-1$
		else
			return fWarnings;
	}
	/**
	 * Adds a new warning to the list with the passed information.
	 * Normally the export operation continues after a warning.
	 * @param	message		the message
	 * @param	exception	the throwable that caused the warning, or <code>null</code>
	 */
	protected void addWarning(String message, Throwable error) {
		fWarnings.add(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), 0, message, error));
	}
}
