/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import org.eclipse.core.indexsearch.IIndexQuery;
import org.eclipse.core.indexsearch.ISearchResultCollector;
import org.eclipse.core.indexsearch.SearchEngine;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.jdt.internal.core.JavaModelManager;

/**
 */
public class JspCorePlugin extends AbstractUIPlugin implements IResourceChangeListener, IStartup {
		
	public static final String JSP_TYPE= "jsp";
	
	private static JspCorePlugin fgDefault;
	
	private SearchEngine fSearchEngine;
	
	/**
	 * @param descriptor
	 */
	public JspCorePlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgDefault= this;
		fSearchEngine= SearchEngine.getSearchEngine();
	}
	
	public static JspCorePlugin getDefault() {
		return fgDefault;
	}

	/**
	 * Startup of the JspCore plug-in.
	 * <p>
	 * Registers a resource changed listener.
	 * Starts the background indexing.
	 * <p>
	 * @see org.eclipse.core.runtime.Plugin#startup()
	 */
	public void startup() {
		System.out.println("JspCorePlugin: startup");
		
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		
		workspace.addResourceChangeListener(this,
//				IResourceChangeEvent.PRE_AUTO_BUILD |
//				IResourceChangeEvent.POST_AUTO_BUILD |
				IResourceChangeEvent.POST_CHANGE |
				IResourceChangeEvent.PRE_DELETE |
				IResourceChangeEvent.PRE_CLOSE
		);
	}
	
	public void resourceChanged(IResourceChangeEvent event) {
		if (event == null)
			return;
		IResourceDelta d= event.getDelta();
		if (d == null)
			return;
		try {
			d.accept(
				new IResourceDeltaVisitor() {
					public boolean visit(IResourceDelta delta) {
						if (delta != null) {
							IResource r= delta.getResource();
							if (r instanceof IFile) {
								IFile file= (IFile) r;
								String type= file.getFileExtension();
								if (JSP_TYPE.equalsIgnoreCase(type)) {
									switch (delta.getKind()) {
									case IResourceDelta.ADDED:
										jspAdded(file);
										break;
									case IResourceDelta.REMOVED:
										jspRemoved(file);
										break;
									case IResourceDelta.CHANGED:
										// no need to index if the content has not changed
										if ((delta.getFlags() & IResourceDelta.CONTENT) != 0)
											jspAdded(file);
										break;
									}					
								}
							}
						}
						return true;
					}
				}
			);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	void jspAdded(IFile jspFile) {
		System.out.println("Added: " + jspFile);
		AddJspFileToIndex job= new AddJspFileToIndex(jspFile, jspFile.getProject().getProject().getFullPath(), fSearchEngine.getIndexManager());
		fSearchEngine.add(job);

	}
	
	void jspRemoved(IFile jspFile) {
		System.out.println("Removed: " + jspFile);
		fSearchEngine.remove(jspFile.getFullPath().toString(), jspFile.getProject().getProject().getFullPath());
	}
	
	public void search(IIndexQuery query, ISearchResultCollector resultCollector, IProgressMonitor pm) {
		fSearchEngine.search(query, resultCollector, pm, SearchEngine.WAIT_UNTIL_READY_TO_SEARCH);
	}
		
	/**
	 * Shutdown the JspCore plug-in.
	 * <p>
	 * De-registers the resource changed listener.
	 * <p>
	 * @see org.eclipse.core.runtime.Plugin#shutdown()
	 */
	public void shutdown() {
		System.out.println("JspCorePlugin: shutdown");
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(JavaModelManager.getJavaModelManager().deltaProcessor);
	}

	public static void triggerLoad() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	public void earlyStartup() {
	}
}
