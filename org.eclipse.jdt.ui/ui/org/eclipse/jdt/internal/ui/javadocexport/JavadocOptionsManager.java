/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.ExecutionArguments;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocLocations;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;

public class JavadocOptionsManager {

	private IWorkspaceRoot fRoot;
	private IJavaProject fProject;
	private IFile fXmlfile;
	
	private StatusInfo fWizardStatus;

	private List fPackages;
	private String fAccess;
	private String fDocletpath;
	private String fDestination;
	private String fDocletname;
	private boolean fFromStandard;
	private String fStylesheet;
	private String fAdditionalParams;
	private String fOverview;
	private String fJDocCommand;

	private String fSourcepath;
	private String fClasspath;

	private String fAntpath;

	private boolean fNotree;
	private boolean fNoindex;
	private boolean fSplitindex;
	private boolean fNonavbar;
	private boolean fNodeprecated;
	private boolean fNoDeprecatedlist;
	private boolean fAuthor;
	private boolean fVersion;

	public final static String PRIVATE= "private";
	public final static String PROTECTED= "protected";
	public final static String PACKAGE= "package";
	public final static String PUBLIC= "public";

	public final static String NOTREE= "notree";
	public final static String NOINDEX= "noindex";
	public final static String NONAVBAR= "nonavbar";
	public final static String NODEPRECATED= "nodeprecated";
	public final static String NODEPRECATEDLIST= "nodeprecatedlist";
	public final static String VERSION= "version";
	public final static String AUTHOR= "author";
	public final static String SPLITINDEX= "splitindex";
	public final static String STYLESHEETFILE= "stylesheetfile";
	public final static String OVERVIEW= "overview";
	public final static String DOCLETNAME= "docletname";
	public final static String DOCLETPATH= "docletpath";
	public final static String SOURCEPATH= "sourcepath";
	public final static String CLASSPATH= "classpath";
	public final static String DESTINATION= "destdir";

	public final static String VISIBILITY= "access";
	public final static String PACKAGENAMES= "packagenames";
	public final static String EXTRAOPTIONS= "additionalparam";
	public final static String JAVADOCCOMMAND= "javadoccommand";

	public final String NAME= "name";
	public final String PATH= "path";
	
	public JavadocOptionsManager(IDialogSettings settings, IWorkspaceRoot root, ISelection selection) {
		this(null, settings, root, selection);	
	}


	public JavadocOptionsManager(IFile xmlJavadocFile, IDialogSettings settings, IWorkspaceRoot root, ISelection currSelection) {
		Element element;
		this.fRoot= root;
		fJDocCommand= JavadocPreferencePage.getJavaDocCommand();
		this.fXmlfile= xmlJavadocFile;
		this.fWizardStatus= new StatusInfo();
		
		if(xmlJavadocFile!= null) {
			try {
				JavadocReader reader= new JavadocReader(xmlJavadocFile.getContents());
				element= reader.readXML();
	
				if (element == null)
					loadStore(settings, currSelection);
				else
					loadStore(element);
			} catch(CoreException e) {
				JavaPlugin.log(e);
				fWizardStatus.setWarning("Unable to run wizard from Ant file, defaults used...");
				loadStore(settings, currSelection);
			} catch(IOException e) {
				JavaPlugin.log(e);
				fWizardStatus.setWarning("Error reading Ant file, defaults used...");
				loadStore(settings, currSelection);
			} catch(SAXException e) {
				JavaPlugin.log(e);
				fWizardStatus.setWarning("Error reading Ant file, defaults used...");
				loadStore(settings, currSelection);
			}		
		}else loadStore(settings, currSelection);		
	}

	private void loadStore(IDialogSettings settings, ISelection sel) {

	if(settings!= null){
		fPackages= new ArrayList();
		//getValidSelection will also find the project
		IStructuredSelection selection= getValidSelection(sel);
		fPackages= selection.toList();

		fAccess= settings.get(VISIBILITY);
		if (fAccess == null)
			fAccess= PRIVATE;

		fFromStandard= false;
		fDocletpath= settings.get(DOCLETPATH);
		fDocletname= settings.get(DOCLETNAME);
		if (fDocletpath == null || fDocletname == null) {
			fFromStandard= true;
			fDocletname= fDocletname= "";
		}

		//load a destination even if a custom doclet is being used
		IPath path= null;
		if (fProject != null) {
			path= fProject.getProject().getFullPath();

			URL url= JavaDocLocations.getJavadocLocation(path);
			//uses default if source is has http protocol
			if (url == null || !url.getProtocol().equals("file")) {
				fDestination= fProject.getProject().getLocation().addTrailingSeparator().append("doc").toOSString();
			} else {
				//must do this to remove leading "/"
				File file= new File(url.getFile());
				IPath tpath= new Path(file.getPath());
				fDestination= tpath.toOSString();
			}
		} else
			fDestination= "";

		if (fProject != null) {
			path= fProject.getProject().getLocation();
			fAntpath= path.addTrailingSeparator().append("javadoc.xml").toOSString();
		} else
			fAntpath= "";

		fStylesheet= settings.get(STYLESHEETFILE);
		if (fStylesheet == null)
			fStylesheet= "";

		fAdditionalParams= settings.get(EXTRAOPTIONS);
		if (fAdditionalParams == null)
			fAdditionalParams= "";

		fOverview= settings.get(OVERVIEW);
		if (fOverview == null)
			fOverview= "";

		fAuthor= loadbutton(settings.get(AUTHOR));
		fVersion= loadbutton(settings.get(VERSION));
		fNodeprecated= loadbutton(settings.get(NODEPRECATED));
		fNoDeprecatedlist= loadbutton(settings.get(NODEPRECATEDLIST));
		fNonavbar= loadbutton(settings.get(NONAVBAR));
		fNoindex= loadbutton(settings.get(NOINDEX));
		fNotree= loadbutton(settings.get(NOTREE));
		fSplitindex= loadbutton(settings.get(SPLITINDEX));

		}else loadDefaults(sel);
	}
	private void loadDefaults(ISelection sel) {
		fPackages= new ArrayList();
		IStructuredSelection selection= getValidSelection(sel);
		fPackages= selection.toList();
		fAccess= PRIVATE;

		
		if (fProject == null) {
			fAntpath= "";
		} else {
			IPath path= fProject.getProject().getLocation().addTrailingSeparator();
			fAntpath= path.append("javadoc.xml").toOSString();
		}

		//default destination
		fFromStandard= true;
		if (fProject != null) {
			IPath path= fProject.getProject().getFullPath();
			URL url= JavaDocLocations.getJavadocLocation(path);
			if (url != null && url.getProtocol().equals("file")) {
				File file= new File(url.getFile());
				IPath tpath= new Path(file.getPath());
				fDestination= tpath.toOSString();
			} else {
				fDestination= fProject.getProject().getLocation().addTrailingSeparator().append("doc").toOSString();
			}

		} else
			fDestination= "";
		
		fDocletname="";
		fDocletpath="";
		fStylesheet= "";
		fAdditionalParams= "";
		fOverview= "";

		fAuthor= true;
		fVersion= true;
		fNodeprecated= false;
		fNoDeprecatedlist= false;
		fNonavbar= false;
		fNoindex= false;
		fNotree= false;
		fSplitindex= true;
	}

	private void loadStore(Element element) {

		fAccess= element.getAttribute(VISIBILITY);
		if (!(fAccess.length() > 0))
			fAccess= PRIVATE;

		//locate the project, set global variable fProject
		fSourcepath= element.getAttribute(SOURCEPATH);
		String token;
		if (!fSourcepath.equals("")) {
			int index= fSourcepath.indexOf(";");
			if (index != -1)
				token= fSourcepath.substring(0, index);
			else
				token= fSourcepath;
			IContainer container= fRoot.getContainerForLocation(new Path(token));
			if (container != null) {
				IProject p= container.getProject();
				fProject= JavaCore.create(p);
			}
		}

		//Since the selected packages are stored we must locate the project
		fDestination= element.getAttribute(DESTINATION);
		fFromStandard= true;
		if (fDestination.equals("")) {
			NodeList list= element.getChildNodes();
			for (int i= 0; i < list.getLength(); i++) {
				Node child= list.item(i);
				if (child.getNodeName().equals("doclet")) {
					fDocletpath= ((Element) child).getAttribute(PATH);
					fDocletname= ((Element) child).getAttribute(NAME);
					if (!(fDocletpath.equals("") && !fDocletname.equals("")))
						fFromStandard= false;
					else fDocletname= fDocletpath ="";
					break;
				}
			}
		}

		//get all the package or type names
		//@Notice : Change settreechecked
		List names= new ArrayList();
		String packagenames= element.getAttribute(PACKAGENAMES);
		if (packagenames != null) {
			StringTokenizer tokenizer= new StringTokenizer(packagenames, ",");
			while (tokenizer.hasMoreElements()) {
				names.add(tokenizer.nextElement());
			}
		}

		//get tree elements
		fPackages= new ArrayList();
		if (fProject != null) {
			try {
				for (int i= 0; i < names.size(); i++) {
					String name= (String) names.get(i);
					IJavaElement el= JavaModelUtil.findTypeContainer(fProject, name);
					fPackages.add(el);
				}
			} catch (JavaModelException e) {
				JavaPlugin.logErrorMessage(e.getMessage());
			}
		}

		//set ant path
		fAntpath= fXmlfile.getLocation().toOSString();
		//		String antfilename= xmlfile.getName();
		//		try {
		//			if(fProject != null) {
		//				IPath path= fProject.getCorrespondingResource().getLocation();
		//				path= path.addTrailingSeparator().append(antfilename);
		//				antpath= path.toOSString();
		//			} else antpath= antfilename;
		//		} catch(JavaModelException e) {
		//			antpath= antfilename;
		//		} 

		fStylesheet= element.getAttribute(STYLESHEETFILE);
		fAdditionalParams= element.getAttribute(EXTRAOPTIONS);
		fOverview= element.getAttribute(OVERVIEW);

		fAuthor= loadbutton(element.getAttribute(AUTHOR));
		fVersion= loadbutton(element.getAttribute(VERSION));
		fNodeprecated= loadbutton(element.getAttribute(NODEPRECATED));
		fNoDeprecatedlist= loadbutton(element.getAttribute(NODEPRECATEDLIST));
		fNonavbar= loadbutton(element.getAttribute(NONAVBAR));
		fNoindex= loadbutton(element.getAttribute(NOINDEX));
		fNotree= loadbutton(element.getAttribute(NOTREE));
		fSplitindex= loadbutton(element.getAttribute(SPLITINDEX));
	}

	//it is possible that the package list is empty
	public StatusInfo getWizardStatus() {
		return fWizardStatus;
	}	

	public List getPackagenames() {
		return fPackages;
	}

	public String getAccess() {
		return fAccess;
	}

	public String getAntpath() {
		return fAntpath;
	}

	public boolean fromStandard() {
		return fFromStandard;
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

	public String getClasspath() {
		return fClasspath;
	}

	public String getSourcepath() {
		return fSourcepath;
	}

	public IWorkspaceRoot getRoot() {
		return fRoot;
	}

	public IJavaProject getJavaProject() {
		return this.fProject;
	}

	public boolean getBoolean(String flag) {

		if (flag.equals(AUTHOR))
			return fAuthor;
		else if (flag.equals(VERSION))
			return fVersion;
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

		if (value == null || value.equals(""))
			return false;
		else {
			if (value.equals("true"))
				return true;
			else
				return false;
		}

	}

	public String[] createArgumentArray() {
		List args= new ArrayList();

		args.add(fJDocCommand);
		if (fFromStandard) {
			args.add("-d");
			args.add(fDestination);
		} else {
			if (!fAdditionalParams.equals("")) {
				ExecutionArguments tokens= new ExecutionArguments("", fAdditionalParams);
				String[] argsArray= tokens.getProgramArgumentsArray();
				for (int i= 0; i < argsArray.length; i++) {
					args.add(argsArray[i]);
				}
			}
			args.add("-doclet");
			args.add(fDocletname);
			args.add("-docletpath");
			args.add(fDocletpath);
		}
		args.add("-sourcepath");
		args.add(fSourcepath);
		args.add("-classpath");
		args.add(fClasspath);
		args.add("-" + fAccess);

		if (fFromStandard) {
			if (fVersion)
				args.add("-version");
			if (fAuthor)
				args.add("-author");
			if (fNonavbar)
				args.add("-nonavbar");
			if (fNoindex)
				args.add("-noindex");
			if (fNotree)
				args.add("-notree");
			if (fNodeprecated)
				args.add("-nodeprecated");
			if (fNoDeprecatedlist)
				args.add("-nodeprecatedlist");
			if (fSplitindex)
				args.add("-splitindex");

			if (!fStylesheet.equals("")) {
				args.add("-stylsheet");
				args.add(fStylesheet);
			}
			if (!fAdditionalParams.equals("")) {
				ExecutionArguments tokens= new ExecutionArguments("", fAdditionalParams);
				String[] argsArray= tokens.getProgramArgumentsArray();
				for (int i= 0; i < argsArray.length; i++) {
					args.add(argsArray[i]);
				}
			}

		}

		if (!fOverview.equals("")) {
			args.add("-overview");
			args.add(fOverview);
		}

		Object[] str= fPackages.toArray();
		for (int i= 0; i < str.length; i++) {
			String object= (String) str[i];
			args.add(object);
		}

		return (String[]) args.toArray(new String[args.size()]);
	}

	public void createXML() {
		FileOutputStream objectStreamOutput= null;
		try {
			if (!fAntpath.equals("")) {
				File file= new File(fAntpath);

				IPath path= new Path(fAntpath);
				path= path.removeLastSegments(1);
				path.toFile().mkdirs();

				objectStreamOutput= new FileOutputStream(file);
				JavadocWriter writer= new JavadocWriter(objectStreamOutput);
				writer.writeXML(this);
											
			}
		} catch (IOException e) {
			JavaPlugin.logErrorMessage(e.getMessage());
		} finally {
			if (objectStreamOutput != null) {
				try { objectStreamOutput.close(); } catch (IOException e) {}
			}
		}
	}

	public IDialogSettings createDialogSettings() {

		IDialogSettings settings= new DialogSettings("javadoc");

		if (!fFromStandard) {
			settings.put(DOCLETNAME, fDocletname);
			settings.put(DOCLETPATH, fDocletpath);
		} else
			settings.put(DESTINATION, fDestination);
		settings.put(VISIBILITY, fAccess);

		settings.put(AUTHOR, fAuthor);
		settings.put(VERSION, fVersion);
		settings.put(NODEPRECATED, fNodeprecated);
		settings.put(NODEPRECATEDLIST, fNoDeprecatedlist);
		settings.put(SPLITINDEX, fSplitindex);
		settings.put(NOINDEX, fNoindex);
		settings.put(NOTREE, fNotree);
		settings.put(NONAVBAR, fNonavbar);

		if (!fAdditionalParams.equals(""))
			settings.put(EXTRAOPTIONS, fAdditionalParams);
		if (!fOverview.equals(""))
			settings.put(OVERVIEW, fOverview);
		if (!fStylesheet.equals(""))
			settings.put(STYLESHEETFILE, fStylesheet);

		return settings;
	}

	public void setAccess(String access) {
		this.fAccess= access;
	}

	public void setDestination(String destination) {
		this.fDestination= destination;
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

	public void setAntpath(String antpath) {
		this.fAntpath= antpath;
	}

	public void setClasspath(String classpath) {
		this.fClasspath= classpath;
	}

	public void setSourcepath(String sourcepath) {
		this.fSourcepath= sourcepath;
	}

	public void setPackagenames(List packagenames) {
		this.fPackages= packagenames;
	}

	public void setRoot(IWorkspaceRoot root) {
		this.fRoot= root;
	}

	public void setProject(IJavaProject project) {
		this.fProject= project;
	}

	public void setFromStandard(boolean fromStandard) {
		this.fFromStandard= fromStandard;
	}

	public void setBoolean(String flag, boolean value) {

		if (flag.equals(AUTHOR))
			this.fAuthor= value;
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

	private IStructuredSelection getValidSelection(ISelection currentSelection) {

		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) currentSelection;

			if (structuredSelection.isEmpty()) {
				currentSelection= JavaPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();
				if (currentSelection instanceof IStructuredSelection)
					structuredSelection= (IStructuredSelection) currentSelection;
			}
			List selectedElements= new ArrayList(structuredSelection.size());
			Iterator iter= structuredSelection.iterator();

			//this method will also find the project for default
			//destination and ant generation paths
			getProject(selectedElements, iter);
			if (!selectedElements.isEmpty()) {
				return new StructuredSelection(selectedElements);
			}
		}
		if (fProject != null)
			return new StructuredSelection(fProject);

		return StructuredSelection.EMPTY;
	}

	private void getProject(List selectedElements, Iterator iter) {
		fProject= null;
		while (iter.hasNext()) {
			Object selectedElement= iter.next();
			IJavaElement elem= getSelectableJavaElement(selectedElement);
			if (elem != null) {
				IJavaProject jproj= elem.getJavaProject();
				if (fProject == null || fProject.equals(jproj)) {
					selectedElements.add(elem);
					fProject= jproj;
					break;
				}
			}
		}
		if (fProject == null) {
			Object[] roots= fRoot.getProjects();

			for (int i= 0; i < roots.length; i++) {
				IProject p= (IProject) roots[i];
				IJavaProject iJavaProject= JavaCore.create(p);
				if (getValidProject(iJavaProject)) {
					fProject= iJavaProject;
					break;
				}
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
					ICompilationUnit cu= (ICompilationUnit) JavaModelUtil.findElementOfKind(je, IJavaElement.COMPILATION_UNIT);
					if (cu != null) {
						if (cu.isWorkingCopy()) {
							cu= (ICompilationUnit) cu.getOriginalElement();
						}
						IType primaryType= JavaModelUtil.findPrimaryType(cu);
						if (primaryType != null) {
							return primaryType;
						}
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
		if (project != null) {
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

}