/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.jdt.launching.ExecutionArguments;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;

public class JavadocOptionsManager {

	private IWorkspaceRoot fRoot;
	//consider making a List
	private List fProjects;
	private IFile fXmlfile;

	private StatusInfo fWizardStatus;

	private List fSourceElements;

	private List fSelectedElements;

	private String fAccess;
	private String fDocletpath;
	private String fDocletname;
	private boolean fFromStandard;
	private String fStylesheet;
	private String fAdditionalParams;
	private String fOverview;
	private String fTitle;

	private IPath[] fSourcepath;
	private IPath[] fClasspath;

	private boolean fNotree;
	private boolean fNoindex;
	private boolean fSplitindex;
	private boolean fNonavbar;
	private boolean fNodeprecated;
	private boolean fNoDeprecatedlist;
	private boolean fAuthor;
	private boolean fVersion;
	private boolean fUse;
	
	private boolean fJDK14Mode;

	private boolean fOpenInBrowser;

	//list of hrefs in string format
	private Map fLinks;

	//add-on for multi-project version
	private String fDestination;
	private String fDependencies;
	private String fAntpath;

	public final String PRIVATE= "private"; //$NON-NLS-1$
	public final String PROTECTED= "protected"; //$NON-NLS-1$
	public final String PACKAGE= "package"; //$NON-NLS-1$
	public final String PUBLIC= "public"; //$NON-NLS-1$

	public final String USE= "use"; //$NON-NLS-1$
	public final String NOTREE= "notree"; //$NON-NLS-1$
	public final String NOINDEX= "noindex"; //$NON-NLS-1$
	public final String NONAVBAR= "nonavbar"; //$NON-NLS-1$
	public final String NODEPRECATED= "nodeprecated"; //$NON-NLS-1$
	public final String NODEPRECATEDLIST= "nodeprecatedlist"; //$NON-NLS-1$
	public final String VERSION= "version"; //$NON-NLS-1$
	public final String AUTHOR= "author"; //$NON-NLS-1$
	public final String SPLITINDEX= "splitindex"; //$NON-NLS-1$
	public final String STYLESHEETFILE= "stylesheetfile"; //$NON-NLS-1$
	public final String OVERVIEW= "overview"; //$NON-NLS-1$
	public final String DOCLETNAME= "docletname"; //$NON-NLS-1$
	public final String DOCLETPATH= "docletpath"; //$NON-NLS-1$
	public final String SOURCEPATH= "sourcepath"; //$NON-NLS-1$
	public final String CLASSPATH= "classpath"; //$NON-NLS-1$
	public final String DESTINATION= "destdir"; //$NON-NLS-1$
	public final String OPENINBROWSER= "openinbrowser"; //$NON-NLS-1$	

	public final String VISIBILITY= "access"; //$NON-NLS-1$
	public final String PACKAGENAMES= "packagenames"; //$NON-NLS-1$
	public final String SOURCEFILES= "sourcefiles"; //$NON-NLS-1$
	public final String EXTRAOPTIONS= "additionalparam"; //$NON-NLS-1$
	public final String JAVADOCCOMMAND= "javadoccommand"; //$NON-NLS-1$
	public final String TITLE= "doctitle"; //$NON-NLS-1$
	public final String HREF= "href"; //$NON-NLS-1$

	public final String NAME= "name"; //$NON-NLS-1$
	public final String PATH= "path"; //$NON-NLS-1$
	private final String FROMSTANDARD= "fromStandard"; //$NON-NLS-1$
	private final String ANTPATH= "antpath"; //$NON-NLS-1$
	public final String SOURCE= "source"; //$NON-NLS-1$
	
	

	/**
	 * @param xmlJavadocFile The ant file to take initl values from or null, if not started from an ant file.
	 * @param setting Dialog settings for the Javadoc exporter.
	 */
	public JavadocOptionsManager(IFile xmlJavadocFile, IDialogSettings settings, ISelection currSelection) {
		fRoot= ResourcesPlugin.getWorkspace().getRoot();
		fXmlfile= xmlJavadocFile;
		fWizardStatus= new StatusInfo();
		fLinks= new HashMap();
		fProjects= new ArrayList();
		if (xmlJavadocFile != null) {
			try {
				JavadocReader reader= new JavadocReader(xmlJavadocFile.getContents());
				Element element= reader.readXML();
				IJavaProject p= reader.getProject();
				if (element != null && p != null) {
					fProjects.add(p);
					loadFromXML(element);
					return;
				}
				fWizardStatus.setWarning(JavadocExportMessages.getString("JavadocOptionsManager.antfileincorrectCE.warning")); //$NON-NLS-1$
			} catch (CoreException e) {
				JavaPlugin.log(e);
				fWizardStatus.setWarning(JavadocExportMessages.getString("JavadocOptionsManager.antfileincorrectCE.warning")); //$NON-NLS-1$
			} catch (IOException e) {
				JavaPlugin.log(e);
				fWizardStatus.setWarning(JavadocExportMessages.getString("JavadocOptionsManager.antfileincorrectIOE.warning")); //$NON-NLS-1$
			} catch (SAXException e) {
				fWizardStatus.setWarning(JavadocExportMessages.getString("JavadocOptionsManager.antfileincorrectSAXE.warning")); //$NON-NLS-1$
			}
		}
		if (settings != null) {
			loadFromDialogStore(settings, currSelection);
		} else {
			loadDefaults(currSelection);
		}
	}

	private void loadFromDialogStore(IDialogSettings settings, ISelection sel) {
		//getValidSelection will also find the project
		fSelectedElements= getValidSelection(sel);

		fAccess= settings.get(VISIBILITY);
		if (fAccess == null)
			fAccess= PROTECTED;

		//this is defaulted to false.
		fFromStandard= settings.getBoolean(FROMSTANDARD);

		//doclet is loaded even if the standard doclet is being used
		fDocletpath= settings.get(DOCLETPATH);
		fDocletname= settings.get(DOCLETNAME);
		if (fDocletpath == null || fDocletname == null) {
			fFromStandard= true;
			fDocletpath= ""; //$NON-NLS-1$
			fDocletname= ""; //$NON-NLS-1$
		}

		//load a default antpath
		fAntpath= settings.get(ANTPATH);
		if (fAntpath == null)
			fAntpath= ""; //$NON-NLS-1$
		//$NON-NLS-1$

		//load a default antpath
		fDestination= settings.get(DESTINATION);
		if (fDestination == null)
			fDestination= ""; //$NON-NLS-1$
		//$NON-NLS-1$

		fTitle= settings.get(TITLE);
		if (fTitle == null)
			fTitle= ""; //$NON-NLS-1$

		fStylesheet= settings.get(STYLESHEETFILE);
		if (fStylesheet == null)
			fStylesheet= ""; //$NON-NLS-1$

		fAdditionalParams= settings.get(EXTRAOPTIONS);
		if (fAdditionalParams == null)
			fAdditionalParams= ""; //$NON-NLS-1$

		fOverview= settings.get(OVERVIEW);
		if (fOverview == null)
			fOverview= ""; //$NON-NLS-1$

		fUse= loadbutton(settings.get(USE));
		fAuthor= loadbutton(settings.get(AUTHOR));
		fVersion= loadbutton(settings.get(VERSION));
		fNodeprecated= loadbutton(settings.get(NODEPRECATED));
		fNoDeprecatedlist= loadbutton(settings.get(NODEPRECATEDLIST));
		fNonavbar= loadbutton(settings.get(NONAVBAR));
		fNoindex= loadbutton(settings.get(NOINDEX));
		fNotree= loadbutton(settings.get(NOTREE));
		fSplitindex= loadbutton(settings.get(SPLITINDEX));
		fOpenInBrowser= loadbutton(settings.get(OPENINBROWSER));
		
		fJDK14Mode= loadbutton(settings.get(SOURCE));

		//get the set of project specific data
		loadLinksFromDialogSettings(settings);
	}

	private String getDefaultAntPath(IJavaProject project) {
		if (project != null) {
			IPath path= project.getProject().getLocation();
			if (path != null)
				return path.append("javadoc.xml").toOSString(); //$NON-NLS-1$
		}

		return ""; //$NON-NLS-1$
	}

	private String getDefaultDestination(IJavaProject project) {
		if (project != null) {
			URL url= JavaUI.getProjectJavadocLocation(project);
			//uses default if source is has http protocol
			if (url == null || !url.getProtocol().equals("file")) { //$NON-NLS-1$
				IPath path= project.getProject().getLocation();
				if (path != null)
					return path.append("doc").toOSString(); //$NON-NLS-1$
			} else {
				//must do this to remove leading "/"
				return (new File(url.getFile())).getPath();
			}
		}

		return ""; //$NON-NLS-1$

	}
	/**
	* Method creates a list of data structes that contain
	* The destination, antfile location and the list of library/project references for every
	* project in the workspace.Defaults are created for new project.
	*/
	private void loadLinksFromDialogSettings(IDialogSettings settings) {

		//sets data for projects if stored in the dialog settings
		if (settings != null) {
			IDialogSettings links= settings.getSection("projects"); //$NON-NLS-1$
			if (links != null) {
				IDialogSettings[] projs= links.getSections();
				for (int i= 0; i < projs.length; i++) {
					IDialogSettings iDialogSettings= projs[i];
					String projectName= iDialogSettings.getName();
					IProject project= fRoot.getProject(projectName);
					//make sure project has not been removed
					if (project.exists()) {
						IJavaProject javaProject= JavaCore.create(project);
						if (!fLinks.containsKey(javaProject)) {
							String hrefs= iDialogSettings.get(HREF);
							if (hrefs == null) {
								hrefs= ""; //$NON-NLS-1$
							}
							String destdir= iDialogSettings.get(DESTINATION);
							if (destdir == null || destdir.length() == 0) {
								destdir= getDefaultDestination(javaProject);
							}
							String antpath= iDialogSettings.get(ANTPATH);
							if (antpath == null || antpath.length() == 0) {
								antpath= getDefaultAntPath(javaProject);
							}
							ProjectData data= new ProjectData();
							data.setDestination(destdir);
							data.setAntpath(antpath);
							data.setLinks(hrefs);
							if (!fLinks.containsValue(javaProject))
								fLinks.put(javaProject, data);
						}
					}
				}
			}
		}
		//finds projects in the workspace that have been added since the
		//last time the wizard was run
		IProject[] projects= fRoot.getProjects();
		for (int i= 0; i < projects.length; i++) {
			IProject iProject= projects[i];
			IJavaProject javaProject= JavaCore.create(iProject);
			if (!fLinks.containsKey(javaProject)) {
				ProjectData data= new ProjectData();
				data.setDestination(getDefaultDestination(javaProject));
				data.setAntpath(getDefaultAntPath(javaProject));
				data.setLinks(""); //$NON-NLS-1$
				fLinks.put(javaProject, data);
			}
		}
	}
	//loads defaults for wizard (nothing is stored)
	private void loadDefaults(ISelection sel) {
		fSelectedElements= getValidSelection(sel);

		fAccess= PUBLIC;

		fDocletname= ""; //$NON-NLS-1$
		fDocletpath= ""; //$NON-NLS-1$
		fTitle= ""; //$NON-NLS-1$
		fStylesheet= ""; //$NON-NLS-1$
		fAdditionalParams= ""; //$NON-NLS-1$
		fOverview= ""; //$NON-NLS-1$
		fAntpath= ""; //$NON-NLS-1$
		fDestination= ""; //$NON-NLS-1$

		fUse= true;
		fAuthor= true;
		fVersion= true;
		fNodeprecated= false;
		fNoDeprecatedlist= false;
		fNonavbar= false;
		fNoindex= false;
		fNotree= false;
		fSplitindex= true;
		fOpenInBrowser= false;
		fJDK14Mode= false;

		//by default it is empty all project map to the empty string
		fFromStandard= true;
		loadLinksFromDialogSettings(null);
	}

	private void loadFromXML(Element element) {

		fAccess= element.getAttribute(VISIBILITY);
		if (!(fAccess.length() > 0))
			fAccess= PROTECTED;

		//Since the selected packages are stored we must locate the project
		String destination= element.getAttribute(DESTINATION);
		fDestination= destination;
		fFromStandard= true;
		fDocletname= ""; //$NON-NLS-1$
		fDocletpath= ""; //$NON-NLS-1$

		if (destination.equals("")) { //$NON-NLS-1$
			NodeList list= element.getChildNodes();
			for (int i= 0; i < list.getLength(); i++) {
				Node child= list.item(i);
				if (child.getNodeName().equals("doclet")) { //$NON-NLS-1$
					fDocletpath= ((Element) child).getAttribute(PATH);
					fDocletname= ((Element) child).getAttribute(NAME);
					if (!(fDocletpath.equals("") && !fDocletname.equals(""))) { //$NON-NLS-1$ //$NON-NLS-2$
						fFromStandard= false;
					} else {
						fDocletname= ""; //$NON-NLS-1$
						fDocletpath= ""; //$NON-NLS-1$
					}
					break;
				}
			}
		}

		//find all the links stored in the ant script
		boolean firstTime= true;
		StringBuffer buf= new StringBuffer();
		NodeList children= element.getChildNodes();
		for (int i= 0; i < children.getLength(); i++) {
			Node child= children.item(i);
			if (child.getNodeName().equals("link")) { //$NON-NLS-1$
				String href= ((Element) child).getAttribute(HREF);
				if (firstTime)
					firstTime= false;
				else
					buf.append(';');
				buf.append(href);
			}
		}

		//associate all those links with each project selected	
		for (Iterator iter= fProjects.iterator(); iter.hasNext();) {
			IJavaProject iJavaProject= (IJavaProject) iter.next();

			ProjectData data= new ProjectData();
			IPath path= fXmlfile.getLocation();
			if (path != null)
				data.setAntpath(path.toOSString());
			else
				data.setAntpath(""); //$NON-NLS-1$
			data.setLinks(buf.toString());
			data.setDestination(destination);
			fLinks.put(iJavaProject, data);
		}

		//get tree elements
		setSelectedElementsFromAnt(element, (IJavaProject) fProjects.get(0));
		IPath p= fXmlfile.getLocation();
		if (p != null)
			fAntpath= p.toOSString();
		else
			fAntpath= ""; //$NON-NLS-1$

		fStylesheet= element.getAttribute(STYLESHEETFILE);
		fTitle= element.getAttribute(TITLE);
		fAdditionalParams= element.getAttribute(EXTRAOPTIONS);
		fOverview= element.getAttribute(OVERVIEW);

		fUse= loadbutton(element.getAttribute(USE));
		fAuthor= loadbutton(element.getAttribute(AUTHOR));
		fVersion= loadbutton(element.getAttribute(VERSION));
		fNodeprecated= loadbutton(element.getAttribute(NODEPRECATED));
		fNoDeprecatedlist= loadbutton(element.getAttribute(NODEPRECATEDLIST));
		fNonavbar= loadbutton(element.getAttribute(NONAVBAR));
		fNoindex= loadbutton(element.getAttribute(NOINDEX));
		fNotree= loadbutton(element.getAttribute(NOTREE));
		fSplitindex= loadbutton(element.getAttribute(SPLITINDEX));
	}

	/*
	 * Method creates an absolute path to the project. If the path is already
	 * absolute it returns the path. If it encounters any difficulties in
	 * creating the absolute path, the method returns null.
	 * 
	 * @param pathStr
	 * @return IPath
	 */
	private IPath makeAbsolutePathFromRelative(String pathStr) {
		IPath path= new Path(pathStr);
		if (!path.isAbsolute()) {
			if (fXmlfile == null) {
				return null;
			}
			IPath basePath= fXmlfile.getParent().getLocation(); // relative to the ant file location
			if (basePath == null) {
				return null;
			}
			return basePath.append(pathStr);
		}
		return path;
	}

	private void setSelectedElementsFromAnt(Element element, IJavaProject iJavaProject) {
		fSelectedElements= new ArrayList();

		//get all the packages in side the project
		String name;
		String packagenames= element.getAttribute(PACKAGENAMES);
		if (packagenames != null) {
			StringTokenizer tokenizer= new StringTokenizer(packagenames, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				name= tokenizer.nextToken().trim();
				IJavaElement el;
				try {
					el= JavaModelUtil.findTypeContainer(iJavaProject, name);
				} catch (JavaModelException e) {
					JavaPlugin.log(e);
					continue;
				}
				if ((el != null) && (el instanceof IPackageFragment)) {
					fSelectedElements.add(el);
				}
			}
		}

		//get all CompilationUnites in side the project
		String sourcefiles= element.getAttribute(SOURCEFILES);
		if (sourcefiles != null) {
			StringTokenizer tokenizer= new StringTokenizer(sourcefiles, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				name= tokenizer.nextToken().trim();
				if (name.endsWith(".java")) { //$NON-NLS-1$
					IPath path= makeAbsolutePathFromRelative(name);

					//if unable to create an absolute path the the resource skip it
					if (path == null)
						continue;

					IFile[] files= fRoot.findFilesForLocation(path);
					for (int i= 0; i < files.length; i++) {
						IFile curr= files[i];
						if (curr.getProject().equals(iJavaProject.getProject())) {
							IJavaElement el= JavaCore.createCompilationUnitFrom(curr);
							if (el != null) {
								fSelectedElements.add(el);
							}
						}
					}
				}
			}
		}
	}

	//it is possible that the package list is empty
	public StatusInfo getWizardStatus() {
		return fWizardStatus;
	}

	public IJavaElement[] getSelectedElements() {
		return (IJavaElement[]) fSelectedElements.toArray(new IJavaElement[fSelectedElements.size()]);
	}

	public IJavaElement[] getSourceElements() {
		return (IJavaElement[]) fSourceElements.toArray(new IJavaElement[fSourceElements.size()]);
	}

	public String getAccess() {
		return fAccess;
	}

	public String getGeneralAntpath() {
		return fAntpath;
	}

	public String getSpecificAntpath(IJavaProject project) {
		ProjectData data= (ProjectData) fLinks.get(project);
		if (data != null)
			return data.getAntPath();
		else
			return ""; //$NON-NLS-1$
	}

	public boolean fromStandard() {
		return fFromStandard;
	}

	//for now if multiple projects are selected the destination
	//feild will be empty, 
	public String getDestination(IJavaProject project) {

		ProjectData data= (ProjectData) fLinks.get(project);
		if (data != null)
			return data.getDestination();
		else
			return ""; //$NON-NLS-1$
	}

	public String getDestination() {
		return fDestination;
	}

	public String getDocletPath() {
		return fDocletpath;
	}

	public String getDocletName() {
		return fDocletname;
	}

	public String getStyleSheet() {
		return fStylesheet;
	}

	public String getOverview() {
		return fOverview;
	}

	public String getAdditionalParams() {
		return fAdditionalParams;
	}

	public IPath[] getClasspath() {
		return fClasspath;
	}

	public IPath[] getSourcepath() {
		return fSourcepath;
	}

	public IWorkspaceRoot getRoot() {
		return fRoot;
	}

	//	public IJavaProject[] getJavaProjects() {
	//		return (IJavaProject[]) fProjects.toArray(new IJavaProject[fProjects.size()]);
	//	}

	//Use only this one later
	public List getJavaProjects() {
		return fProjects;
	}

	public String getTitle() {
		return fTitle;
	}

	public String getLinks(IJavaProject project) {
		ProjectData data= (ProjectData) fLinks.get(project);
		if (data != null)
			return data.getLinks();
		else
			return ""; //$NON-NLS-1$
	}

	public String getDependencies() {
		return fDependencies;
	}

	public Map getLinkMap() {
		return fLinks;
	}

	public boolean doOpenInBrowser() {
		return fOpenInBrowser;
	}

	public boolean getBoolean(String flag) {

		if (flag.equals(AUTHOR))
			return fAuthor;
		else if (flag.equals(VERSION))
			return fVersion;
		else if (flag.equals(USE))
			return fUse;
		else if (flag.equals(NODEPRECATED))
			return fNodeprecated;
		else if (flag.equals(NODEPRECATEDLIST))
			return fNoDeprecatedlist;
		else if (flag.equals(NOINDEX))
			return fNoindex;
		else if (flag.equals(NOTREE))
			return fNotree;
		else if (flag.equals(SPLITINDEX))
			return fSplitindex;
		else if (flag.equals(NONAVBAR))
			return fNonavbar;
		else
			return false;
	}

	private boolean loadbutton(String value) {

		if (value == null || value.equals("")) //$NON-NLS-1$
			return false;
		else {
			if (value.equals("true")) //$NON-NLS-1$
				return true;
			else
				return false;
		}
	}
	
	private String flatPathList(IPath[] paths) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < paths.length; i++) {
			if (i > 0) {
				buf.append(File.pathSeparatorChar);
			}
			buf.append(paths[i].toOSString());
		}
		return buf.toString();
	}
	

	public String[] createArgumentArray() {
		if (fProjects.isEmpty()) {
			return new String[0];
		}

		List args= new ArrayList();
		//bug 38692
		args.add(JavadocPreferencePage.getJavaDocCommand());
		if (fFromStandard) {
			args.add("-d"); //$NON-NLS-1$
			args.add(fDestination);
		} else {
			if (!fAdditionalParams.equals("")) { //$NON-NLS-1$
				ExecutionArguments tokens= new ExecutionArguments("", fAdditionalParams); //$NON-NLS-1$
				String[] argsArray= tokens.getProgramArgumentsArray();
				for (int i= 0; i < argsArray.length; i++) {
					args.add(argsArray[i]);
				}
			}
			args.add("-doclet"); //$NON-NLS-1$
			args.add(fDocletname);
			args.add("-docletpath"); //$NON-NLS-1$
			args.add(fDocletpath);
		}
		args.add("-sourcepath"); //$NON-NLS-1$
		args.add(flatPathList(fSourcepath));
		args.add("-classpath"); //$NON-NLS-1$
		args.add(flatPathList(fClasspath));
		args.add("-" + fAccess); //$NON-NLS-1$

		if (fFromStandard) {
			if (fJDK14Mode) {
				args.add("-source"); //$NON-NLS-1$
				args.add("1.4"); //$NON-NLS-1$
			}			
			
			if (fUse)
				args.add("-use"); //$NON-NLS-1$
			if (fVersion)
				args.add("-version"); //$NON-NLS-1$
			if (fAuthor)
				args.add("-author"); //$NON-NLS-1$
			if (fNonavbar)
				args.add("-nonavbar"); //$NON-NLS-1$
			if (fNoindex)
				args.add("-noindex"); //$NON-NLS-1$
			if (fNotree)
				args.add("-notree"); //$NON-NLS-1$
			if (fNodeprecated)
				args.add("-nodeprecated"); //$NON-NLS-1$
			if (fNoDeprecatedlist)
				args.add("-nodeprecatedlist"); //$NON-NLS-1$
			if (fSplitindex)
				args.add("-splitindex"); //$NON-NLS-1$

			if (!fTitle.equals("")) { //$NON-NLS-1$
				args.add("-doctitle"); //$NON-NLS-1$
				args.add(fTitle);
			}


			if (!fStylesheet.equals("")) { //$NON-NLS-1$
				args.add("-stylesheetfile"); //$NON-NLS-1$
				args.add(fStylesheet);
			}

			if (!fAdditionalParams.equals("")) { //$NON-NLS-1$
				ExecutionArguments tokens= new ExecutionArguments("", fAdditionalParams); //$NON-NLS-1$
				String[] argsArray= tokens.getProgramArgumentsArray();
				for (int i= 0; i < argsArray.length; i++) {
					args.add(argsArray[i]);
				}
			}

			String hrefs= fDependencies;
			StringTokenizer tokenizer= new StringTokenizer(hrefs, ";"); //$NON-NLS-1$
			while (tokenizer.hasMoreElements()) {
				String href= (String) tokenizer.nextElement();
				args.add("-link"); //$NON-NLS-1$
				args.add(href);
			}
		} //end standard options

		if (!fOverview.equals("")) { //$NON-NLS-1$
			args.add("-overview"); //$NON-NLS-1$
			args.add(fOverview);
		}

		for (int i= 0; i < fSourceElements.size(); i++) {
			IJavaElement curr= (IJavaElement) fSourceElements.get(i);
			if (curr instanceof IPackageFragment) {
				args.add(curr.getElementName());
			} else if (curr instanceof ICompilationUnit) {
				IPath p= curr.getResource().getLocation();
				if (p != null)
					args.add(p.toOSString());
			}
		}

		String[] res= (String[]) args.toArray(new String[args.size()]);
		return res;

	}

	public void createXML() throws CoreException {
		FileOutputStream objectStreamOutput= null;
		//@change
		//for now only writting ant files for single project selection
		String antpath= fAntpath;
		try {
			if (!antpath.equals("")) { //$NON-NLS-1$
				File file= new File(antpath);

				IPath antPath= new Path(antpath);
				IPath antDir= antPath.removeLastSegments(1);
				
				IPath basePath= null;
				
				Assert.isTrue(fProjects.size() == 1);
				IJavaProject jproject= (IJavaProject) fProjects.get(0);
				IWorkspaceRoot root= jproject.getProject().getWorkspace().getRoot();
				if (root.findFilesForLocation(antPath).length > 0) {
					basePath= antDir; // only do relative path if ant file is stored in the workspace
				}
				
				antDir.toFile().mkdirs();

				objectStreamOutput= new FileOutputStream(file);
				JavadocWriter writer= new JavadocWriter(objectStreamOutput, basePath, jproject);
				writer.writeXML(this);
			}
		} catch (IOException e) {
			String message= JavadocExportMessages.getString("JavadocOptionsManager.createXM.error"); //$NON-NLS-1$
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, message, e));
		} catch (ParserConfigurationException e) {
			String message= JavadocExportMessages.getString("JavadocOptionsManager.createXM.error"); //$NON-NLS-1$
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, message, e));
		} catch (TransformerException e) {
			String message= JavadocExportMessages.getString("JavadocOptionsManager.createXM.error"); //$NON-NLS-1$
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, message, e));
		} finally {
			if (objectStreamOutput != null) {
				try {
					objectStreamOutput.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public IDialogSettings createDialogSettings() {

		IDialogSettings settings= new DialogSettings("javadoc"); //$NON-NLS-1$

		settings.put(FROMSTANDARD, fFromStandard);

		settings.put(DOCLETNAME, fDocletname);
		settings.put(DOCLETPATH, fDocletpath);

		settings.put(VISIBILITY, fAccess);

		settings.put(USE, fUse);
		settings.put(AUTHOR, fAuthor);
		settings.put(VERSION, fVersion);
		settings.put(NODEPRECATED, fNodeprecated);
		settings.put(NODEPRECATEDLIST, fNoDeprecatedlist);
		settings.put(SPLITINDEX, fSplitindex);
		settings.put(NOINDEX, fNoindex);
		settings.put(NOTREE, fNotree);
		settings.put(NONAVBAR, fNonavbar);
		settings.put(OPENINBROWSER, fOpenInBrowser);
		settings.put(SOURCE, fJDK14Mode);

		if (!fAntpath.equals("")) //$NON-NLS-1$
			settings.put(ANTPATH, fAntpath);
		if (!fDestination.equals("")) //$NON-NLS-1$
			settings.put(DESTINATION, fDestination);
		if (!fAdditionalParams.equals("")) //$NON-NLS-1$
			settings.put(EXTRAOPTIONS, fAdditionalParams);
		if (!fOverview.equals("")) //$NON-NLS-1$
			settings.put(OVERVIEW, fOverview);
		if (!fStylesheet.equals("")) //$NON-NLS-1$
			settings.put(STYLESHEETFILE, fStylesheet);
		if (!fTitle.equals("")) //$NON-NLS-1$
			settings.put(TITLE, fTitle);

		IDialogSettings links= new DialogSettings("projects"); //$NON-NLS-1$

		//Write all project information to DialogSettings.
		Set keys= fLinks.keySet();
		for (Iterator iter= keys.iterator(); iter.hasNext();) {

			IJavaProject iJavaProject= (IJavaProject) iter.next();

			IDialogSettings proj= new DialogSettings(iJavaProject.getElementName());
			if (!keys.contains(iJavaProject)) {
				proj.put(HREF, ""); //$NON-NLS-1$
				proj.put(DESTINATION, ""); //$NON-NLS-1$
				proj.put(ANTPATH, ""); //$NON-NLS-1$
			} else {
				ProjectData data= (ProjectData) fLinks.get(iJavaProject);
				proj.put(HREF, data.getLinks());
				proj.put(DESTINATION, data.getDestination());
				proj.put(ANTPATH, data.getAntPath());
			}
			links.addSection(proj);
		}
		settings.addSection(links);

		return settings;
	}

	public void setAccess(String access) {
		this.fAccess= access;
	}

	public void setDestination(IJavaProject project, String destination) {
		ProjectData data= (ProjectData) fLinks.get(project);
		if (data != null)
			data.setDestination(destination);
	}

	public void setDestination(String destination) {
		fDestination= destination;
	}

	public void setDocletPath(String docletpath) {
		this.fDocletpath= docletpath;
	}

	public void setDocletName(String docletname) {
		this.fDocletname= docletname;
	}

	public void setStyleSheet(String stylesheet) {
		this.fStylesheet= stylesheet;
	}

	public void setOverview(String overview) {
		this.fOverview= overview;
	}

	public void setAdditionalParams(String params) {
		fAdditionalParams= params;
	}

	public void setSpecificAntpath(IJavaProject project, String antpath) {
		ProjectData data= (ProjectData) fLinks.get(project);
		if (data != null)
			data.setAntpath(antpath);
	}

	public void setGeneralAntpath(String antpath) {
		this.fAntpath= antpath;
	}
	public void setClasspath(IPath[] classpath) {
		this.fClasspath= classpath;
	}

	public void setSourcepath(IPath[] sourcepath) {
		this.fSourcepath= sourcepath;
	}

	public void setSourceElements(IJavaElement[] elements) {
		this.fSourceElements= new ArrayList(Arrays.asList(elements));
	}

	public void setProjects(IJavaProject[] projects, boolean clear) {
		if (clear)
			fProjects.clear();

		for (int i= 0; i < projects.length; i++) {
			IJavaProject iJavaProject= projects[i];
			if (!fProjects.contains(iJavaProject))
				this.fProjects.add(iJavaProject);
		}
	}

	public void setFromStandard(boolean fromStandard) {
		this.fFromStandard= fromStandard;
	}

	public void setTitle(String title) {
		this.fTitle= title;
	}

	public void setDependencies(String dependencies) {
		fDependencies= dependencies;
	}

	public void setLinks(IJavaProject project, String hrefs) {
		ProjectData data= (ProjectData) fLinks.get(project);
		if (data != null)
			data.setLinks(hrefs);
	}

	public void setOpenInBrowser(boolean openInBrowser) {
		this.fOpenInBrowser= openInBrowser;
	}

	public void setBoolean(String flag, boolean value) {

		if (flag.equals(AUTHOR))
			this.fAuthor= value;
		else if (flag.equals(USE))
			this.fUse= value;
		else if (flag.equals(VERSION))
			this.fVersion= value;
		else if (flag.equals(NODEPRECATED))
			this.fNodeprecated= value;
		else if (flag.equals(NODEPRECATEDLIST))
			this.fNoDeprecatedlist= value;
		else if (flag.equals(NOINDEX))
			this.fNoindex= value;
		else if (flag.equals(NOTREE))
			this.fNotree= value;
		else if (flag.equals(SPLITINDEX))
			this.fSplitindex= value;
		else if (flag.equals(NONAVBAR))
			this.fNonavbar= value;
	}
	
	public boolean isJDK14Mode() {
		return fJDK14Mode;
	}

	public void setJDK14Mode(boolean jdk14Mode) {
		fJDK14Mode= jdk14Mode;
	}	

	private List getValidSelection(ISelection currentSelection) {

		ArrayList res= new ArrayList();
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) currentSelection;

			if (structuredSelection.isEmpty()) {
				currentSelection= JavaPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();
				if (currentSelection instanceof IStructuredSelection)
					structuredSelection= (IStructuredSelection) currentSelection;
			}
			Iterator iter= structuredSelection.iterator();
			//this method will also find the project for default
			//destination and ant generation paths
			getProjects(res, iter);
		}
		return res;
	}

	private void getProjects(List selectedElements, Iterator iter) {

		while (iter.hasNext()) {
			Object selectedElement= iter.next();
			IJavaElement elem= getSelectableJavaElement(selectedElement);
			if (elem != null) {
				IJavaProject jproj= elem.getJavaProject();
				if (jproj != null) {
					//adding the project of the selected element in the list
					//now we can select and generate javadoc for two 
					//methods in different projects
					if (!fProjects.contains(jproj))
						fProjects.add(jproj);
					selectedElements.add(elem);
				}
			}
		}
		//if no projects selected add a default
		if (fProjects.isEmpty()) {
			try {
				IJavaProject[] jprojects= JavaCore.create(fRoot).getJavaProjects();

				for (int i= 0; i < jprojects.length; i++) {
					IJavaProject iJavaProject= jprojects[i];
					if (getValidProject(iJavaProject)) {
						fProjects.add(iJavaProject);
						break;
					}
				}				
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private IJavaElement getSelectableJavaElement(Object obj) {
		IJavaElement je= null;
		try {
			if (obj instanceof IAdaptable) {
				je= (IJavaElement) ((IAdaptable) obj).getAdapter(IJavaElement.class);
			}

			if (je == null) {
				return null;
			}

			switch (je.getElementType()) {
				case IJavaElement.JAVA_MODEL :
				case IJavaElement.JAVA_PROJECT :
				case IJavaElement.CLASS_FILE :
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT :
					if (containsCompilationUnits((IPackageFragmentRoot) je)) {
						return je;
					}
					break;
				case IJavaElement.PACKAGE_FRAGMENT :
					if (containsCompilationUnits((IPackageFragment) je)) {
						return je;
					}
					break;
				default :
					ICompilationUnit cu= (ICompilationUnit) je.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (cu != null) {
						return JavaModelUtil.toOriginal(cu);
					}
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		IJavaProject project= je.getJavaProject();
		if (getValidProject(project))
			return project;
		else
			return null;
	}

	private boolean getValidProject(IJavaProject project) {
		if (project != null && project.exists()) {
			try {
				IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
				for (int i= 0; i < roots.length; i++) {
					if (containsCompilationUnits(roots[i])) {
						return true;
					}
				}

			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return false;
	}

	private boolean containsCompilationUnits(IPackageFragmentRoot root) throws JavaModelException {
		if (root.getKind() != IPackageFragmentRoot.K_SOURCE) {
			return false;
		}

		IJavaElement[] elements= root.getChildren();
		for (int i= 0; i < elements.length; i++) {
			if (elements[i] instanceof IPackageFragment) {
				IPackageFragment fragment= (IPackageFragment) elements[i];
				if (containsCompilationUnits(fragment)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean containsCompilationUnits(IPackageFragment pack) throws JavaModelException {
		return pack.getCompilationUnits().length > 0;
	}
		

	private static class ProjectData {

		private String fHrefs;
		private String fDestinationDir;
		private String fAntPath;

		public void setLinks(String hrefs) {
			if (hrefs == null)
				fHrefs= ""; //$NON-NLS-1$
			else
				fHrefs= hrefs;
		}

		public void setDestination(String destination) {
			if (destination == null)
				fDestinationDir= ""; //$NON-NLS-1$
			else
				fDestinationDir= destination;
		}

		public void setAntpath(String antpath) {
			if (antpath == null)
				fAntPath= ""; //$NON-NLS-1$
			else
				fAntPath= antpath;
		}

		public String getLinks() {
			return fHrefs;
		}

		public String getDestination() {
			return fDestinationDir;
		}

		public String getAntPath() {
			return fAntPath;
		}

	}



}
