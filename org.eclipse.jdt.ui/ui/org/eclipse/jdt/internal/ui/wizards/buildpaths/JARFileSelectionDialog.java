/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.io.File;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.viewsupport.FilteredElementTreeSelectionDialog;

/**
 * Selection dialog to select a JAR on the file system.
 * Set input to a java.io.File that point to folder.
 */
public class JARFileSelectionDialog extends FilteredElementTreeSelectionDialog {

	/**
	 * Constructor for JARFileSelectionDialog.
	 * @param parent parent shell
	 * @param multiSelect specifies if selecting multiple elements is allowed
	 * @param acceptFolders specifies if folders can be selected as well
	 */
	public JARFileSelectionDialog(Shell parent, boolean multiSelect, boolean acceptFolders) {
		this(parent, multiSelect, acceptFolders, false);
	}

	/**
	 * Constructor for JARFileSelectionDialog.
	 * @param parent parent shell
	 * @param multiSelect specifies if selecting multiple elements is allowed
	 * @param acceptFolders specifies if folders can be selected as well
	 * @param acceptAllArchives specifies if all archives (not just jar and zip) can be selected
	 */
	public JARFileSelectionDialog(Shell parent, boolean multiSelect, boolean acceptFolders, boolean acceptAllArchives) {
		super(parent, new FileLabelProvider(), new FileContentProvider(), false);
		setComparator(new FileViewerComparator());
		if (!acceptAllArchives) {
			addFilter(new JARZipFileFilter(acceptFolders));
		} else {
			setInitialFilter(ArchiveFileFilter.JARZIP_FILTER_STRING);
		}
		setValidator(new FileSelectionValidator(multiSelect, acceptFolders));
		setHelpAvailable(false);
	}



	private static class FileLabelProvider extends LabelProvider {
		private final Image IMG_FOLDER= ISharedImages.get().getImage(ISharedImages.IMG_OBJ_FOLDER);
		private final Image IMG_JAR=  JavaPlugin.getDefault().getImageRegistry().get(JavaPluginImages.IMG_OBJS_EXTJAR);

		@Override
		public Image getImage(Object element) {
			if (element instanceof File) {
				File curr= (File) element;
				if (curr.isDirectory()) {
					return IMG_FOLDER;
				} else {
					return IMG_JAR;
				}
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof File) {
				return BasicElementLabels.getResourceName(((File) element).getName());
			}
			return super.getText(element);
		}
	}

	private static class FileContentProvider implements ITreeContentProvider {

		private final Object[] EMPTY= new Object[0];

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof File) {
				File[] children= ((File) parentElement).listFiles();
				if (children != null) {
					return children;
				}
			}
			return EMPTY;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof File) {
				return ((File) element).getParentFile();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object element) {
			return getChildren(element);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	private static class JARZipFileFilter extends ViewerFilter {
		private final boolean fAcceptFolders;

		public JARZipFileFilter(boolean acceptFolders) {
			fAcceptFolders= acceptFolders;
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (element instanceof File) {
				File file= (File) element;
				if (file.isFile()) {
					return isArchive(file);
				} else if (fAcceptFolders) {
					return true;
				} else {
					File[] listFiles= file.listFiles();
					if (listFiles != null) {
						for (File f : listFiles) {
							if (select(viewer, file, f)) {
								return true;
							}
						}
					}
				}
			}
			return false;
		}

		private static boolean isArchive(File file) {
			String name= file.getName();
			int detIndex= name.lastIndexOf('.');
			return (detIndex != -1 && ArchiveFileFilter.isArchiveFileExtension(name.substring(detIndex + 1)));
		}
	}

	private static class FileViewerComparator extends ViewerComparator {
		@Override
		public int category(Object element) {
			if (element instanceof File) {
				if (((File) element).isFile()) {
					return 1;
				}
			}
			return 0;
		}
	}

	private static class FileSelectionValidator implements ISelectionStatusValidator {
		private boolean fMultiSelect;
		private boolean fAcceptFolders;

		public FileSelectionValidator(boolean multiSelect, boolean acceptFolders) {
			fMultiSelect= multiSelect;
			fAcceptFolders= acceptFolders;
		}

		@Override
		public IStatus validate(Object[] selection) {
			int nSelected= selection.length;
			if (nSelected == 0 || (nSelected > 1 && !fMultiSelect)) {
				return new StatusInfo(IStatus.ERROR, "");  //$NON-NLS-1$
			}
			for (Object curr : selection) {
				if (curr instanceof File) {
					File file= (File) curr;
					if (!fAcceptFolders && !file.isFile()) {
						return new StatusInfo(IStatus.ERROR, "");  //$NON-NLS-1$
					}
				}
			}
			return new StatusInfo();
		}
	}


}
