package org.eclipse.jdt.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/**
  */
public class JavaCapabilityConfigurationPage extends NewElementWizardPage {

	private static final String PAGE_NAME= "JavaCapabilityConfigurationPage"; //$NON-NLS-1$
	
	private IJavaProject fJavaProject;
	private BuildPathsBlock fBuildPathsBlock;
	private boolean fIsNewProject;
	
	/**
	 * Creates a wizard page that can be used in a Java project creation wizard.
	 * It contains controls to configure a the classpath and the output folder.
	 * 
	 * <p>
	 * After constructing, a call to <code>init</code> is required
	 * </p>
	 */	
	public JavaCapabilityConfigurationPage() {
		super(PAGE_NAME);
		fJavaProject= null;
		
		setTitle(NewWizardMessages.getString("JavaCapabilityConfigurationPage.title")); //$NON-NLS-1$
		setDescription(NewWizardMessages.getString("JavaCapabilityConfigurationPage.description")); //$NON-NLS-1$
		
		IStatusChangeListener listener= new IStatusChangeListener() {
			public void statusChanged(IStatus status) {
				updateStatus(status);
			}
		};

		fBuildPathsBlock= new BuildPathsBlock(ResourcesPlugin.getWorkspace().getRoot(), listener, true);
	}
	
	/**
	 * Initializes the page with the project and default classpaths.
	 * <p>
	 * The default classpath entries must correspond the the given project.
	 * </p>
	 * <p>
	 * The caller of this method is responsible for creating the underlying project and any new
	 * folders that might be mentioned on the default classpath. The wizard will create the output folder if required.
	 * </p>
	 * <p>
	 * The project does not have to exist at the time of initialization, but must exist when executing the runnable
	 * obtained by <code>getRunnable()</code>.
	 * </p>
	 * @param project The java project.
	 * @param entries The default classpath entries or <code>null</code> to take the default
	 * @param path The folder to be taken as the default output path or <code>null</code> to take the default
	 * @return overrideExistingClasspath If set to true, an existing '.classpath' file is ignored. If set to <code>false</code>
	 * the default classpath is only used if no '.classpath' exists.
	 */
	public void init(IJavaProject jproject, IPath defaultOutputLocation, IClasspathEntry[] defaultEntries, boolean defaultsOverrideExistingClasspath) {
		if (!defaultsOverrideExistingClasspath && jproject.exists() && jproject.getProject().getFile(".classpath").exists()) { //$NON-NLS-1$
			defaultOutputLocation= null;
			defaultOutputLocation= null;
		}
		fBuildPathsBlock.init(jproject, defaultOutputLocation, defaultEntries);
		fJavaProject= jproject;
		fIsNewProject= jproject.exists();
	}
	

	/* (non-Javadoc)
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		Control control= fBuildPathsBlock.createControl(parent);
		setControl(control);
		
		WorkbenchHelp.setHelp(control, IJavaHelpContextIds.NEW_JAVAPROJECT_WIZARD_PAGE);
	}
		
	/**
	 * Returns the currently configured output location. Note that the returned path must not be valid.
	 */
	public IPath getOutputLocation() {
		return fBuildPathsBlock.getOutputLocation();
	}

	/**
	 * Returns the currently configured class path. Note that the class path must not be valid.
	 */	
	public IClasspathEntry[] getRawClassPath() {
		return fBuildPathsBlock.getRawClassPath();
	}
	

	/**
	 * Returns the runnable that will create the Java project or <code>null</code> if the page has not been initialized.
	 * The runnable sets the project's classpath and output location to the values configured in the page.
	 *
	 * @return the runnable
	 */		
	public IRunnableWithProgress getRunnable() {
		if (fJavaProject != null) {
			return new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					int nSteps= 10;			
					monitor.beginTask(NewWizardMessages.getString("JavaCapabilityConfigurationPage.op_desc"), nSteps); //$NON-NLS-1$
					
					try {
						IProject project= fJavaProject.getProject();
						if (!project.hasNature(JavaCore.NATURE_ID)) {
							addNatureToProject(project, JavaCore.NATURE_ID, new SubProgressMonitor(monitor, 1));
							nSteps--;
						}
														
						// configure the build paths
						IRunnableWithProgress jrunnable= fBuildPathsBlock.getRunnable(null);
						jrunnable.run(new SubProgressMonitor(monitor, nSteps));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
						
					} finally {
						monitor.done();
					}
				}
			};
		}
		return null;	
	}
	
	/**
	 * Adds a nature to a project
	 */
	private static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}		

}
