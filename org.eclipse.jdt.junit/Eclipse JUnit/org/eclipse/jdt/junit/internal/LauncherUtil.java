/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.model.Factory;
import org.eclipse.core.runtime.model.PluginDescriptorModel;
import org.eclipse.core.runtime.model.PluginPrerequisiteModel;
import org.eclipse.core.runtime.model.PluginRegistryModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.junit.internal.BaseLauncherUtil;
import org.eclipse.junit.internal.JUnitPlugin;
import sun.security.action.GetPropertyAction;


public class LauncherUtil { 
	
	private LauncherUtil() {
	}
	
	private static long fNumber;		

	protected static String[] createClassPath(IType type) throws InvocationTargetException {
		String[] cp= new String[0];		
		try {
			cp = JavaRuntime.computeDefaultRuntimeClassPath(type.getJavaProject());
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
		String[] classPath= new String[cp.length + 1];
		System.arraycopy(cp, 0, classPath, 0, cp.length);
		boolean startupOnClassPath= true;
		for(int i= 0; i < cp.length; i++) {
			if (cp[i].indexOf("startup.jar") > 0) {
				startupOnClassPath= true;
				break;
			}
		}
		try {
			classPath[classPath.length - 1]= getStartupJarDir() + File.separator + "startup.jar";
		} catch (InvocationTargetException e) {
			if (startupOnClassPath) {
				classPath[classPath.length - 1]= "";
				BaseLauncherUtil.logException("Information starting UIMain: no 'startup.jar' found relative to plugins directories, "
					+ "but there was one on the projects classPath.", e);
			}
			else
				throw e;
		}
		return classPath;
	}

	protected static String createPluginsPathFile(IType type, String pluginID) throws InvocationTargetException{
		fNumber= System.currentTimeMillis();
		PluginRegistryModel[] pluginRegistryModels = createPluginRegistryModels(type);
		ArrayList required = collectRequiredPluginIDs(pluginRegistryModels, pluginID);
		File file = writePluginsPathFile(required, pluginRegistryModels);
		try {
			URL url= file.toURL();
			return url.toExternalForm();
		} catch (MalformedURLException e) {
			throw new InvocationTargetException(e);
		}
	}
	protected static PluginRegistryModel[] createPluginRegistryModels(IType type) throws InvocationTargetException {
		String[] pluginsDirectories = collectPluginsDirectories();
		
		PluginRegistryModel[] pluginRegistryModels= new PluginRegistryModel[pluginsDirectories.length + 1];
		pluginRegistryModels[0]= getPluginRegistryModel(type.getJavaProject());
		
		MultiStatus status= new MultiStatus("JUnit plugin.xml scann", 1010, "error scanning plugin.xml", null);
		Factory factory= new Factory(status);		
		for (int i= 0; i < pluginsDirectories.length; i++) {
			if (pluginsDirectories[i].equals(getDefaultPluginsLocation()) && Platform.getPluginRegistry() instanceof PluginRegistryModel) {
				pluginRegistryModels[i + 1]= (PluginRegistryModel) Platform.getPluginRegistry();
			}
			else {
				URL[] urls= createPluginURLs(new File(pluginsDirectories[i]));
				pluginRegistryModels[i + 1]= Platform.parsePlugins(urls, factory);
			}
		}
		return pluginRegistryModels;
	}

	protected static String[] collectPluginsDirectories() {
		String workspace= ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		String defaultPluginsLocation= getDefaultPluginsLocation();		
		
		int nOfPluginsDirs= countnOfPluginsDirs();
		int index= 0;
		
		String[] pluginsDirectories = new String[nOfPluginsDirs];
		IPreferenceStore store= JUnitUIPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(PreferencePage.PLUGINS_DIR_WS_CHECKED))
			pluginsDirectories[index++]= workspace;
		if (store.getBoolean(PreferencePage.PLUGINS_DIR_STARTING_CHECKED))
			pluginsDirectories[index++]= defaultPluginsLocation;
		for (int i= 0; i < store.getInt(PreferencePage.NOF_PLUGINS_DIRS); i++) {
			if (store.getBoolean(PreferencePage.PLUGINS_DIR_IS_CHECKED_ + i))
				pluginsDirectories[index++]= store.getString(PreferencePage.PLUGINS_DIR_ + i);
		}
		return pluginsDirectories;
	}

	protected static File writePluginsPathFile(ArrayList required, PluginRegistryModel[] pluginRegistryModels) 
			throws InvocationTargetException {
		StringBuffer stringBuffer= new StringBuffer();
		int count= 0;
		
		// pluginRegistryModels[0] includes only the descriptor for the tested plugin
		// the location of this plugin is in the workspace
		// if the preference is set to TESTPLUGIN_FROM_WS == true then the tested plugin
		// will allways be taken from there
		IPreferenceStore store= JUnitUIPlugin.getDefault().getPreferenceStore();
		int start= 1;
		if (store.getBoolean(PreferencePage.TESTPLUGIN_FROM_WS) || !store.getBoolean(PreferencePage.PLUGIN_INIT_DONE))
			start= 0;
		
		for(int i= 0; i < required.size(); i++) {
			PluginDescriptorModel pluginDescriptorModel;
			for( int j= start; j < pluginRegistryModels.length; j++) {
				pluginDescriptorModel= pluginRegistryModels[j].getPlugin((String) required.get(i));
				if (pluginDescriptorModel != null) {
					stringBuffer.append(escapeSpaces(pluginDescriptorModel.getId()) 
								+ " = " + escapeSpaces(pluginDescriptorModel.getLocation()) + "plugin.xml\n");					
					count++;
					break;
				}
			}
		}

		try {
			File file= new File(getTempLocation(), ".plugins_path_" + fNumber);
			FileWriter fileWriter= new FileWriter(file);
			fileWriter.write(new String(stringBuffer));
			fileWriter.flush();
			fileWriter.close();

		if (required.size() != count) {
			String msg= "can not find all required plugins.\n"
				+ "for more information have a look at: " + getTempLocation() + ".plugins_path_" + fNumber;
			throw new InvocationTargetException(new Exception(msg));
		}
		
			return file;
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
	}

	protected static ArrayList collectRequiredPluginIDs(PluginRegistryModel[] pluginRegistryModels, String pluginID) {
		ArrayList required= new ArrayList();
		required.add(pluginRegistryModels[0].getPlugins()[0].getId());
		required.add(pluginID);
		required.add(JUnitPlugin.getPluginID());
		required.add("org.eclipse.sdk");
		
		int arraySize= 0;		
		while (arraySize != required.size()) {
			arraySize= required.size();
			for (int i= 0; i < required.size(); i++) {
				for(int j= 0; j < pluginRegistryModels.length ; j++) {
					PluginDescriptorModel pluginDescriptorModel= pluginRegistryModels[j].getPlugin((String) required.get(i));
					if (pluginDescriptorModel != null) {
						PluginPrerequisiteModel[] pluginPrerequisiteModels= pluginDescriptorModel.getRequires();
						if(pluginPrerequisiteModels != null) {
							for(int k= 0; k < pluginPrerequisiteModels.length; k++) {
								if (!required.contains(pluginPrerequisiteModels[k].getPlugin()))
									required.add(pluginPrerequisiteModels[k].getPlugin());
							}
						}
						break;
					}
				}
			}
		}
		return required;
	}

	protected static PluginRegistryModel getPluginRegistryModel(IJavaProject project) throws InvocationTargetException {
		MultiStatus status= new MultiStatus("JUnit plugin.xml scann", 1010, "error scanning plugin.xml", null);
		Factory factory= new Factory(status);
		String testPluginName= null;	
		try {		
			URL url= project.getProject().getLocation().toFile().toURL();
			url= Platform.asLocalURL(url);
			url= new URL(url, "plugin.xml");
		
			PluginRegistryModel pluginRegistryModel= Platform.parsePlugins(new URL[] {url}, factory);
			if (pluginRegistryModel != null && pluginRegistryModel.getPlugins().length == 1)
				return pluginRegistryModel;
		} catch (IOException e) {
			//throw new InvocationTargetException(e);
		}
		throw new InvocationTargetException(
			new Exception("could not instantiate the PluginRegistryModel for " + project.getElementName())
		);
	}

	protected static String getStartupJarDir() throws InvocationTargetException {
		IPreferenceStore store= JUnitUIPlugin.getDefault().getPreferenceStore();
	
		if (!store.getBoolean(PreferencePage.PLUGIN_INIT_DONE))
			store.setValue(PreferencePage.CHECK_ALL_FOR_STARTUPJAR, true);
		boolean checkAll= store.getBoolean(PreferencePage.CHECK_ALL_FOR_STARTUPJAR);

		if (checkAll || store.getBoolean(PreferencePage.PLUGINS_DIR_WS_CHECKED))
			if (ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".." + File.separator + "startup.jar").toFile().exists())
				return ResourcesPlugin.getWorkspace().getRoot().getLocation().append("..").toOSString();
		if (checkAll || store.getBoolean(PreferencePage.PLUGINS_DIR_STARTING_CHECKED))
			if (new Path(getDefaultPluginsLocation()).append(".." + File.separator + "startup.jar").toFile().exists())
				return new Path(getDefaultPluginsLocation()).append("..").toOSString();
		for (int i= 0; i < store.getInt(PreferencePage.NOF_PLUGINS_DIRS); i++) {
			if (checkAll || store.getBoolean(PreferencePage.PLUGINS_DIR_IS_CHECKED_ + i))
				if (new Path(store.getString(PreferencePage.PLUGINS_DIR_ + i)).append(".." + File.separator + "startup.jar").toFile().exists())
					return new Path(store.getString(PreferencePage.PLUGINS_DIR_ + i)).append("..").toOSString();
		}
		throw new InvocationTargetException(
			new Exception("could not find 'startup.jar' relative to the directories specified in the JUnit preferences page.")
		);
	}
	
	protected static String getTempLocation() {
		return System.getProperty("java.io.tmpdir") + File.separator;
	}

	protected static String getTempWorkSpaceLocation() {	
 			return getTempLocation() + "workspace_" + fNumber;		
	}

	protected static int countnOfPluginsDirs() {
		IPreferenceStore store= JUnitUIPlugin.getDefault().getPreferenceStore();	
		boolean initDone= store.getBoolean(PreferencePage.PLUGIN_INIT_DONE);
		if (!initDone) {
			store.setValue(PreferencePage.PLUGINS_DIR_WS_CHECKED, true);
			store.setValue(PreferencePage.PLUGINS_DIR_STARTING_CHECKED, true);
			store.setValue(PreferencePage.NOF_PLUGINS_DIRS, 0);
			return 2;
		}		
		int count= 0;
		if (store.getBoolean(PreferencePage.PLUGINS_DIR_WS_CHECKED)) count++;
		if (store.getBoolean(PreferencePage.PLUGINS_DIR_STARTING_CHECKED)) count++;
		for (int i= 0; i < store.getInt(PreferencePage.NOF_PLUGINS_DIRS); i++)
			if (store.getBoolean(PreferencePage.PLUGINS_DIR_IS_CHECKED_ + i)) count++;	
		return count;
	}		

	protected static URL[] createPluginURLs(File directory) throws InvocationTargetException {
		ArrayList pluginURLs= new ArrayList();
		File pluginsDir[]= directory.listFiles();
		for (int i= 0; i < pluginsDir.length; i++) {
			File[] file= pluginsDir[i].listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.equals("plugin.xml");
				}
			});
			if (file != null && file.length == 1 && file[0].exists()) {
				try {
					pluginURLs.add(file[0].toURL());
				} catch (MalformedURLException e) {
					throw new InvocationTargetException(e);
				}
			}
		}
		return (URL[]) pluginURLs.toArray(new URL[pluginURLs.size()]);			
	}

	protected static Object escapeSpaces(String string){
		int index= 0;
		String tmp;
		while (index < string.length() && index != -1){
			index= string.indexOf(' ', index);
			if(index > 0) {
				tmp= string.substring(0, index) + '\\';
				string= tmp + string.substring(index);
				index = index + 2;
			}
		}
		return string;	
	}

	protected static String getDefaultPluginsLocation() {
		String className= LauncherUtil.class.getName();
		int index= className.lastIndexOf('.');
		if (index > 0 && index + 1 < className.length())
			className= className.substring(index + 1);
		URL url= LauncherUtil.class.getResource(className + ".class");
		String string= url.toExternalForm();
		index= string.indexOf("org.eclipse.jdt.junit");
		if (index > -1)
			string= string.substring(0, index);
		index= string.indexOf("jar:");
		if (index == 0)
			string= string.substring(4);			
		index= string.indexOf("file:");
		if (index == 0)
			string= string.substring(5);
		return string;
	}	
}

