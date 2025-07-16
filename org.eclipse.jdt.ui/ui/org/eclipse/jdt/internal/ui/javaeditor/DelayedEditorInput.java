package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This EditorInput implementation is delaying the access to the real {@link IEditorInput}.
 * The delay will end if all of the {@link IClasspathContainer} are resolved.
 *
 * @see #delayIsFinished()
 * @see #executeDelayed(Runnable)
 */
public class DelayedEditorInput implements IEditorInput {

	private Supplier<IEditorInput> fDelayedIAdaptableRetriever;
	private final ArrayList<Runnable> runnables;
	private final IJavaProject fProject;

	public DelayedEditorInput(IJavaProject project, Supplier<IEditorInput> delayedIAdaptableRetriever) {
		runnables = new ArrayList<>();
		fProject= project;
		fDelayedIAdaptableRetriever= delayedIAdaptableRetriever;
		var job = new Job(JavaEditorMessages.DelayedEditorInput_Waiting_for_classpath_containers_initialization +  project.getElementName()) {
			@Override
			public IStatus run(IProgressMonitor mon) {
				while (!delayIsFinished() && !mon.isCanceled()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						ExceptionHandler.log(e, e.getMessage());
					}
				}
				if(mon.isCanceled()) {
					return Status.CANCEL_STATUS;
				} else {
					runnables.forEach(Runnable::run);
					return Status.OK_STATUS;
				}
			}
			@Override
			public boolean belongsTo(Object family) {
				return DelayedEditorInput.class == family;
			}
		};
		job.schedule();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return JavaEditorMessages.DelayedEditorInput_delayed_editor_input;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

	/**
	 * Retrieves the original {@link IEditorInput} which wasn't possible because
	 * of the {@link IClasspathContainer}s were not loaded fully.
	 *
	 * @return the original {@link IEditorInput}
	 */
	public IEditorInput getDelayedIEditorInput() {
		return fDelayedIAdaptableRetriever.get();
	}

	/**
	 * Method to add functionality to delay. Will be delayed if the {@link IClasspathContainer}
	 * are not resolved, otherwise executes this directly.
	 *
	 * @param runnable the runnable which should be executed
	 */
	public void executeDelayed(Runnable runnable) {
		if (delayIsFinished()) {
			runnable.run();
		} else {
			runnables.add(runnable);
		}
	}

	/**
	 * @return true, if all classpath containers are resolved.
	 */
	public boolean delayIsFinished() {
		return JavaCore.hasResolvedAllClasspathContainers(fProject);
	}

}
