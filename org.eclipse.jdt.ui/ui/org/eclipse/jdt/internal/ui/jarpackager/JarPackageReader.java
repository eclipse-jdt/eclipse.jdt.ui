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

	private final static String BAD_FORMAT= "Bad format";
	
	protected InputStream fInputStream;

	private MultiStatus fWarnings;
	
	/**
	 * Reads a Jar Package from the underlying stream.
	 **/
	public JarPackageReader(InputStream inputStream) {
		Assert.isNotNull(inputStream);
		fInputStream= new BufferedInputStream(inputStream);
		fWarnings= new MultiStatus(JavaPlugin.getPluginId(), 0, "JAR Package Reader Warnings", null);
	}
	/**
	 * Hook for possible subclasses
	 **/
	protected JarPackageReader() {
	}
	/**
     * Reads the JAR specification from the underlying stream.
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
	 * @exception IOException
     */
    public void close() throws IOException {
		fInputStream.close();
	}

	public JarPackage readXML() throws IOException, SAXException {
		Element xmlJarDesc= null;
		JarPackage jarPackage= new JarPackage();
	  	DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
    	factory.setValidating(true);
		DocumentBuilder parser= null;
		try {
			parser= factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IOException(ex.getMessage());
		} finally {
			// Note: Above code is ok since clients are responsible to close the stream
		}
		xmlJarDesc= parser.parse(new InputSource(fInputStream)).getDocumentElement();
		if (!xmlJarDesc.getNodeName().equals("jardesc")) {
			throw new IOException(BAD_FORMAT);
		}
		NodeList topLevelElements= xmlJarDesc.getChildNodes();
		for (int i= 0; i < topLevelElements.getLength(); i++) {
			Node node= topLevelElements.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element element= (Element)node;
			if (element.getNodeName().equals("jar")) {
				jarPackage.setJarLocation(new Path(element.getAttribute("path")));
			}		
			if (element.getNodeName().equals("options")) {
				jarPackage.setOverwrite(getBooleanAttribute(element, "overwrite"));
				jarPackage.setCompress(getBooleanAttribute(element, "compress"));
				jarPackage.setExportErrors(getBooleanAttribute(element, "exportErrors"));
				jarPackage.setExportWarnings(getBooleanAttribute(element, "exportWarnings"));
				jarPackage.setLogErrors(getBooleanAttribute(element, "logErrors"));
				jarPackage.setLogWarnings(getBooleanAttribute(element, "logWarnings"));
				jarPackage.setSaveDescription(getBooleanAttribute(element, "saveDescription"));
				jarPackage.setDescriptionLocation(new Path(element.getAttribute("descriptionLocation")));
			}
			if (element.getNodeName().equals("manifest")) {
				jarPackage.setManifestVersion(element.getAttribute("manifestVersion"));
				jarPackage.setUsesManifest(getBooleanAttribute(element, "usesManifest"));
				jarPackage.setReuseManifest(getBooleanAttribute(element, "reuseManifest"));
				jarPackage.setSaveManifest(getBooleanAttribute(element,"saveManifest"));
				jarPackage.setGenerateManifest(getBooleanAttribute(element, "generateManifest"));
				jarPackage.setManifestLocation(new Path(element.getAttribute("manifestLocation")));
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
						&& sealingNode.getNodeName().equals("sealing")) {
						// Sealing
						Element sealingElement= (Element)sealingNode;
						jarPackage.setSealJar(getBooleanAttribute(sealingElement, "sealJar"));
						jarPackage.setPackagesToSeal(getPackages(sealingElement.getElementsByTagName("packagesToSeal")));
						jarPackage.setPackagesToUnseal(getPackages(sealingElement.getElementsByTagName("packagesToUnSeal")));
					}		
				}
			}
			if (element.getNodeName().equals("selectedElements")) {
				jarPackage.setExportClassFiles(getBooleanAttribute(element, "exportClassFiles"));
				jarPackage.setExportJavaFiles(getBooleanAttribute(element, "exportJavaFiles"));
				NodeList selectedElements= element.getChildNodes();
				for (int j= 0; j < selectedElements.getLength(); j++) {
					Node selectedNode= selectedElements.item(j);
					if (selectedNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					Element selectedElement= (Element)selectedNode;
					if (selectedElement.getNodeName().equals("file"))
						addFile(jarPackage.getSelectedElements(), selectedElement);
					else if (selectedElement.getNodeName().equals("folder"))
						addFolder(jarPackage.getSelectedElements() ,selectedElement);
					else if (selectedElement.getNodeName().equals("project"))
						addProject(jarPackage.getSelectedElements() ,selectedElement);
					else if (selectedElement.getNodeName().equals("javaElement"))
						addJavaElement(jarPackage.getSelectedElements() ,selectedElement);
					// Note: Other file types are not handled by this writer
				}
			}
		}
		return jarPackage;
	}

	protected boolean getBooleanAttribute(Element element, String name) throws IOException {
		String value= element.getAttribute(name);
		if (value != null && value.equalsIgnoreCase("true"))
			return true;
		if (value != null && value.equalsIgnoreCase("false"))
			return false;
		throw new IOException(BAD_FORMAT + ": Illegal value for boolean attribute");
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
		String name= element.getAttribute("name");
		if (name.equals(""))
			throw new IOException(BAD_FORMAT + ": Tag 'name' not found");
		IProject project= JavaPlugin.getWorkspace().getRoot().getProject(name);
		if (project != null)
			selectedElements.add(project);
	}

	private IPath getPath(Element element) throws IOException {
		String pathString= element.getAttribute("path");
		if (pathString.equals(""))
			throw new IOException(BAD_FORMAT + ": Tag 'path' not found");
		return new Path(element.getAttribute("path"));
	}
	
	private void addJavaElement(List selectedElements, Element element) throws IOException {
		String handleId= element.getAttribute("handleIdentifier");
		if (handleId.equals(""))
			throw new IOException(BAD_FORMAT + ": Tag 'handleIdentifier' not found or empty");
		IJavaElement je= JavaCore.create(handleId);
		if (je == null)
			addWarning("Warning: Java element does not exist in workspace", null);
		else
			selectedElements.add(je);
	}

	private IPackageFragment[] getPackages(NodeList list) throws IOException {
		if (list.getLength() > 1)
			throw new IOException(BAD_FORMAT + ": Duplicate Tag: " + list.item(0).getNodeName());
		if (list.getLength() == 0)
			return null; // optional entry is not present
		NodeList packageNodes= list.item(0).getChildNodes();
		List packages= new ArrayList(packageNodes.getLength());
		for (int i= 0; i < packageNodes.getLength(); i++) {
			Node packageNode= packageNodes.item(i);
			if (packageNode.getNodeType() == Node.ELEMENT_NODE && packageNode.getNodeName().equals("package")) {
				String handleId= ((Element)packageNode).getAttribute("handleIdentifier");
				if (handleId.equals(""))
					throw new IOException(BAD_FORMAT + ": Tag 'handleIdentifier' not found or empty");
				IJavaElement je= JavaCore.create(handleId);
				if (je != null && je.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
					packages.add(je);
				else
					addWarning("Warning: Java element does not exist in workspace", null);
			}					
		}
		return (IPackageFragment[])packages.toArray(new IPackageFragment[packages.size()]);
	}

	private IType getMainClass(Element element) {
		String handleId= element.getAttribute("mainClassHandleIdentifier");
		if (handleId.equals(""))
			return null;	// Main-Class entry is optional or can be empty
		IJavaElement je= JavaCore.create(handleId);
		if (je != null && je.getElementType() == IJavaElement.TYPE)
			return (IType)je;
		addWarning("Warning: Main Class type does not exist in workspace", null);
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
			return new Status(IStatus.OK, JavaPlugin.getPluginId(), 0, "", null);
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
