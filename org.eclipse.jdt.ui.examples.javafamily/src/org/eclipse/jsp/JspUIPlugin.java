/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp;

import org.osgi.framework.BundleContext;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.indexsearch.IIndexQuery;
import org.eclipse.core.indexsearch.ISearchResultCollector;
import org.eclipse.core.indexsearch.SearchEngine;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.ui.editors.text.TextEditorPreferenceConstants;


/**
 */
public class JspUIPlugin extends AbstractUIPlugin implements IResourceChangeListener {

	/**
	 * The id of the JavaFamilyExample plugin (value <code>"org.eclipse.jdt.ui.examples.javafamily"</code>).
	 */
	public static final String ID_PLUGIN= "org.eclipse.jdt.ui.examples.javafamily"; //$NON-NLS-1$

	public static final String JSP_TYPE= "jsp"; //$NON-NLS-1$

	private static final boolean DEBUG= false;
	private static JspUIPlugin fgDefault;
	private static boolean fgJSPIndexingIsEnabled= false;

	private SearchEngine fSearchEngine;

	public JspUIPlugin() {
		super();
		fgDefault= this;
		fSearchEngine= SearchEngine.getSearchEngine();
	}

	public static JspUIPlugin getDefault() {
		return fgDefault;
	}

	void controlJSPIndexing(boolean enable) {
		if (fgJSPIndexingIsEnabled != enable) {
			fgJSPIndexingIsEnabled= enable;
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			if (enable) {

				IResourceProxyVisitor visitor=
					proxy -> {
					String name= proxy.getName();
					int pos= name.lastIndexOf('.');
					if (pos >= 0) {
						String extension= name.substring(pos+1);
						if (JSP_TYPE.equalsIgnoreCase(extension)) {
							IResource r= proxy.requestResource();
							if (r instanceof IFile)
								jspAdded((IFile)r);
						}
					}
					return true;
				};
				try {
					workspace.getRoot().accept(visitor, 0);
				} catch (CoreException e) {
					log("visiting jsp files", e); //$NON-NLS-1$
				}

				workspace.addResourceChangeListener(this,
	//					IResourceChangeEvent.PRE_AUTO_BUILD |
	//					IResourceChangeEvent.POST_AUTO_BUILD |
						IResourceChangeEvent.POST_CHANGE |
						IResourceChangeEvent.PRE_DELETE |
						IResourceChangeEvent.PRE_CLOSE
				);
			} else {
				workspace.removeResourceChangeListener(this);
			}
		}
	}

	boolean isJSPIndexingOn() {
		return fgJSPIndexingIsEnabled;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if ( !fgJSPIndexingIsEnabled || event == null)
			return;
		IResourceDelta d= event.getDelta();
		if (d == null)
			return;
		try {
			d.accept(
				delta -> {
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
			);
		} catch (CoreException e) {
			log("processing resource delta", e); //$NON-NLS-1$
		}
	}

	public static void log(String message, Throwable e) {
		getDefault().getLog().log(new Status(IStatus.ERROR, ID_PLUGIN, IStatus.ERROR, message, e));
	}

	void jspAdded(IFile jspFile) {
		if (DEBUG) System.out.println("Added: " + jspFile); //$NON-NLS-1$
		JspIndexParser indexer= new JspIndexParser(jspFile);
		fSearchEngine.add(jspFile.getProject().getFullPath(), indexer);
	}

	void jspRemoved(IFile jspFile) {
		if (DEBUG) System.out.println("Removed: " + jspFile); //$NON-NLS-1$
		fSearchEngine.remove(jspFile.getFullPath().toString(), jspFile.getProject().getFullPath());
	}

	public void search(IIndexQuery query, ISearchResultCollector resultCollector, IProgressMonitor pm) {
		fSearchEngine.search(query, resultCollector, pm, SearchEngine.WAIT_UNTIL_READY_TO_SEARCH);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initializeDefaultPreferences();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			controlJSPIndexing(false);
		} finally {
			super.stop(context);
		}
	}

	private void initializeDefaultPreferences() {
		IPreferenceStore prefs= getPreferenceStore();
		TextEditorPreferenceConstants.initializeDefaultValues(prefs);
	}

	/**
	 * Returns the standard display to be used. The method first checks, if
	 * the thread calling this method has an associated display. If so, this
	 * display is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;
	}

	@Override
	protected ImageRegistry createImageRegistry() {
		return JspPluginImages.initializeImageRegistry();
	}

}
