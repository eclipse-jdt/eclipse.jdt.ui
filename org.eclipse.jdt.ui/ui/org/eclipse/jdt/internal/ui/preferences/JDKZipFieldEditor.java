/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.preference.FileFieldEditor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.PortingFinder;

public class JDKZipFieldEditor extends FileFieldEditor {

	private static final String JAVA_LANG_OBJECT= "java/lang/Object.java";
	public static final String PROP_PREFIX= "org.eclipse.jdt.ui.build.jdk.prefix";
	public static final String PROP_SOURCE= "org.eclipse.jdt.ui.build.jdk.source";
	public static final String KEY_SOURCE= "org.eclipse.jdt.ui.build.jdk.source.label";
	public static final String KEY_ERR_NO_PREFIX= "org.eclipse.jdt.ui.build.jdk.source.err.prefix";

	private String fSourcePrefix;
	private ZipFileFieldEditor fZipEditor;
	private String fOldValue;

	public JDKZipFieldEditor(Composite parent) {
		super(PROP_SOURCE, JavaPlugin.getResourceString(KEY_SOURCE), parent);
		org.eclipse.jdt.internal.ui.util.PortingFinder.toBeDone("PROP_PREFIX, PROP_SOURCE duplicated");
		org.eclipse.jdt.internal.ui.util.PortingFinder.toBeDone("KEY_SOURCE, KEY_ERR_NO_PREFIX reused. Use ClassName.key notation");	
	}
	
	//fix for: 1G840WG: ITPJUI:WINNT - 'source for binaries' - inconsistent order 
	public void setZipEditor(ZipFileFieldEditor zipEditor){
		fZipEditor= zipEditor;
	}
	
	private boolean isOK() {
		if (!super.checkState()) {
			fSourcePrefix= null;
			return false;
		}

		String value= getStringValue();
		if (value == null || "".equals(value)) {
			fSourcePrefix= null;
			return true;
		}
		fSourcePrefix= determinePackagePrefix(new File(getStringValue()));
		if (fSourcePrefix == null) {
			showErrorMessage(JavaPlugin.getResourceString(KEY_ERR_NO_PREFIX));
			return false;
		}
		return true;
	}

	/**
	 * @see StringFieldEditor#checkState
	 */
	protected boolean checkState() {
		boolean isOK= isOK();
		String value= getStringValue();
		boolean enable= isOK && value != null && !"".equals(value);
		fZipEditor.setEnabled(enable);
		if (enable){
			if (!value.equals(fOldValue)) {
				fZipEditor.setZipFileName(value);
				fZipEditor.setStringValue(determinePackagePrefix(new File(getStringValue())));
			} 
		} else {
			fZipEditor.setStringValue("");	
		}	
			
		fOldValue= value;
		return isOK;
	}
	
	public String getSourcePrefix() {
		return fSourcePrefix;
	}

	/**
	 * try finding the package prefix
	 */
	private String determinePackagePrefix(File f) {
		try {
			ZipFile zip= null;
			try {
				zip= new ZipFile(f);
				Enumeration zipEntries= zip.entries();
				while (zipEntries.hasMoreElements()) {
					ZipEntry entry= (ZipEntry) zipEntries.nextElement();
					String name= entry.getName();
					if (name.endsWith(JAVA_LANG_OBJECT)) {
						String prefix= name.substring(0, name.length() - JAVA_LANG_OBJECT.length());
						if (prefix.endsWith("/"))
							prefix= prefix.substring(0, prefix.length() - 1);
						return prefix;
					}
				}
			} catch (IOException e) {
			} finally {
				if (zip != null)
					zip.close();
			}
		} catch (IOException e) {
		}
		return null;
	}

	/**
	 * Implements <code>FieldEditor.doLoad()</code>.
	 * @see FieldEditor#doLoad()
	 * @private
	 */
	protected void doLoad() {
		super.doLoad();
		fSourcePrefix= getPreferenceStore().getString(PROP_PREFIX);
	}

}


