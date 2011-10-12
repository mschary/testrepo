package com.kony.nativecodegen.actions;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.kony.nativecodegen.NativeCodeGenPlugin;
import com.kony.nativecodegen.ui.VariableTypeInferenceEditor;
import com.pat.tool.keditor.KEditorPlugin;
import com.pat.tool.keditor.ant.AntRunner;
import com.pat.tool.keditor.build.BuildConstants;
import com.pat.tool.keditor.console.ConsoleDisplayManager;
import com.pat.tool.keditor.preferences.IPreferenceConstants;
import com.pat.tool.keditor.splash.SplashData;
import com.pat.tool.keditor.splash.SplashUtils;
import com.pat.tool.keditor.tasks.TAndroidBuild;
import com.pat.tool.keditor.tasks.TJetty;
import com.pat.tool.keditor.tasks.Task;
import com.pat.tool.keditor.utils.CustomLibrariesUtils;
import com.pat.tool.keditor.utils.FileUtilities;
import com.pat.tool.keditor.utils.ImageUtils;
import com.pat.tool.keditor.utils.KConstants;
import com.pat.tool.keditor.utils.KUtils;
import com.pat.tool.keditor.utils.ProjectProperties;
import com.pat.tool.keditor.utils.ValidationUtil;
import com.pat.tool.keditor.widgets.IMobileChannel;
import com.pat.tool.keditor.widgets.MobileChannels;

public class NativeCodeGenerationJob extends Job {

	private String inferenceFileName;
	private String projectName;
	private String locales;
	
	//Ant property names
	public static final String CUSTOM_LIBS_DIR_KEY = "customlibs.dir";
	public static final String CUSTOM_LIBS_SRC_DIR_KEY = "customlibs.src.dir";
	public static final String CUSTOM_LIBS_XML_DIR_KEY = "customlibs.xml.dir";
	
	
	private String imgMagicHome = KUtils.EMPTY_STRING;
	private String androidHome = KUtils.EMPTY_STRING;
	private String buildOption = BuildConstants.DEFAULT_BUILD_OPTION;
	private String appID =  KUtils.EMPTY_STRING;
	private String appName =  KUtils.EMPTY_STRING;
	private String appVersion = "1.0.0";
	private String vendorName =  KUtils.EMPTY_STRING;
	private String appLogo = KUtils.EMPTY_STRING;
	private String httpPort = "80";
	private String httpsPort = "443";
	private String tcWebContext = KUtils.EMPTY_STRING;
	
	
	private String pluginLoc;
	private String projLoc;
	private String tempLocation;

	private SplashData splashData;
	private String ipadSupportedOrientations = "";

	//splash properties
	private String splashLogo;
	private String splashPBImg1;
	private String splashPBImg2;
	private String splashPBImg3;
	private String splashPBImg4;
	private String splashFGColor;
	private String splashBGColor;


	private int jettyPortNum = 0;
	private int jettyHttpsPortNum = 0;

	private boolean bbBuildOption = true;
	private boolean bbuseMDS = false;
	private boolean removeComments = false;
	private boolean enablei18n = false;
	private String defaultlocale = "";
	private String appPKGName = null;
	
	private boolean andLoadIndctr = false;

	private String win_guid = "";
	
	boolean iphoneSelected = false;
	boolean androidSelected = false;
	boolean bbSelected = false;
	boolean winSelected = false;
	private String iphoneBundleIdentifier;
	private String iphoneBundleVersion;
	private boolean iphoneipadGlossyEffect = false;
	
	private boolean mainresult;
	
	long time;

    // ant constants
	private static final String SOURCE_XML_DIR = "sourcexml.dir";
	private static final String SOURCE_DIR = "source.dir";
	private static final String BUILD_STATUS_FILE = "buildstatus.file";
	private static final String TYPE_INFERENCE_FILE = "typeinfer.file";
	private static final String DESTINATION_DIR = "destination.dir";
	private static final String RESOURCE_DIR = "resource.dir";
	private static final String CLASS_PATH = "classpath";
	private static final String CLASS_NAME = "classname";

	// Update com.pat.tool.keditor.navigation.model.ResourceRoot.testAttribute(Object, String, String) when file names are modified
	private static final String IPHONE_STATUS_FILE = "iphone_buildstatus.properties";
	private static final String IPAD_STATUS_FILE = "ipad_buildstatus.properties";
	private static final String ANDROID_STATUS_FILE = "android_buildstatus.properties";
	private static final String BB_STATUS_FILE = "bb_buildstatus.properties";
	private static final char FILE_SEPARATOR = '/';
	private static final String LUASRC_ANDROID = "/luasrc/android";
	private static final String LUASRC_BB = "/luasrc/bb";
	private static final String LUASRC_IPHONE = "/luasrc/iphone";
	private static final String LUASRC_IPAD = "/luasrc/ipad";
	private static final String GENERATED_FOLDER = "/generated";
	private static final String BUILD_SERVER_PATH = "/build/server";
	private static final String IPHONE_NATIVE_PATH = "/iphonenative";
	private static final String IPAD_NATIVE_PATH = "/ipadnative";
	private static final String ANDROID_NATIVE_PATH = "/androidnative";
	private static final String BB_NATIVE_PATH = "/bbnative";


	
    private IFile typeInferenceFile;
	private boolean ipadSelected;
	private boolean success;
	private boolean iphoneLoadIndctr;	
	
	public NativeCodeGenerationJob(String name, String projectName, String inferenceFileName) {
		super(name);
		this.projectName = projectName;
		this.inferenceFileName = inferenceFileName;
		iphoneSelected = inferenceFileName.equals(KConstants.IPHONE_TYPE_INFERENCE_FILE);
		ipadSelected = inferenceFileName.equals(KConstants.IPAD_TYPE_INFERENCE_FILE);
		androidSelected = inferenceFileName.equals(KConstants.ANDROID_TYPE_INFERENCE_FILE);
		bbSelected = inferenceFileName.equals(KConstants.BB_TYPE_INFERENCE_FILE);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		typeInferenceFile = project.getFile(inferenceFileName);
	    setRule(project);
	}


	
	public IStatus run(IProgressMonitor monitor) {
		success = true;
		String platform = getPlatformName(inferenceFileName);
		projLoc = KUtils.getProjectLocation(projectName);
		pluginLoc = NativeCodeGenPlugin.getDefault().getInstallLocation();
		tempLocation = KUtils.getTempLocation(projectName);
		
		readPeferences();
		loadProjProperties();

		time = System.currentTimeMillis();
		try {
			HashMap<String, String> antNativeProperties = new HashMap<String, String>();
			antNativeProperties.put("iphone", iphoneSelected + "");
			antNativeProperties.put("bb", bbSelected + "");
			antNativeProperties.put("android", androidSelected + "");
			antNativeProperties.put("winmobile", winSelected + "");
			antNativeProperties.put(Task.APP_FOLDER_KEY, tempLocation);
			antNativeProperties.put(Task.PLUGIN_LOC_KEY, pluginLoc);

			AntRunner nativeAnt = new AntRunner();
			nativeAnt.setBuildFile(pluginLoc + "nativefiles/nativebuild.xml");
			nativeAnt.setTarget("all");
			nativeAnt.setProps(antNativeProperties);
			mainresult = nativeAnt.execute();
			if (!mainresult) {
				Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation failed. Please build the project and try again");
				return status;
			}

			String serverLoc = tempLocation + BUILD_SERVER_PATH;

			String srcXmlDir = tempLocation + "/output";

			HashMap<String, String> antRCProperties = getAntRCProperties();
			antRCProperties.put(SOURCE_XML_DIR, srcXmlDir);

			String srcDir = projLoc + (iphoneSelected ? LUASRC_IPHONE : LUASRC_IPAD);
			String resourceDir = pluginLoc + (iphoneSelected ? "nativefiles/iphone" : "nativefiles/ipad");
			String typeInferenceFile = projLoc + FILE_SEPARATOR + inferenceFileName;
			String buildStatusFile = tempLocation + FILE_SEPARATOR + (iphoneSelected ? IPHONE_STATUS_FILE : IPAD_STATUS_FILE);
			String destDir = serverLoc + (iphoneSelected ? IPHONE_NATIVE_PATH : IPAD_NATIVE_PATH);
			String className = "com.konylabs.iphone.transformer.Transform";
			String classpath = "iphone.classpath";

			antRCProperties.put(SOURCE_DIR, srcDir);
			antRCProperties.put(RESOURCE_DIR, resourceDir);
			antRCProperties.put(TYPE_INFERENCE_FILE, typeInferenceFile);
			antRCProperties.put(BUILD_STATUS_FILE, buildStatusFile);
			antRCProperties.put(DESTINATION_DIR, destDir);
			antRCProperties.put(CLASS_NAME, className);
			antRCProperties.put(CLASS_PATH, classpath);

			// variable properties

			// com.konylabs.transformer.CodeGen.main(new String[] {source.dir, sourcexml.dir, resource.dir, typeinfer.file, buildstatus.file, destination.dir});
			if (iphoneSelected || ipadSelected) {
				if (executeNativeBuild(buildStatusFile, antRCProperties, platform)) {
					// Handle KAR File, this may require restarting Jetty server
					AntRunner antRunner = new AntRunner();
					antRCProperties.put(Task.PLUGIN_LOC_KEY, KUtils.getPluginLocation());
					antRunner.setBuildFile(tempLocation + "/build/server/build.xml");
					antRunner.setProps(antRCProperties);
					antRunner.setTarget(iphoneSelected ? "nativeiphone" : "nativeipad");
					success = antRunner.execute();

					if (success) {
						TJetty jetty = new TJetty();
						jetty.setClean(false);
						success = jetty.execute();
					}
				} else {
					success = false;
				}
			}

			if (bbSelected) {
				srcDir = projLoc + LUASRC_BB;
				resourceDir = pluginLoc + "nativefiles/blackberry";
				buildStatusFile = tempLocation + FILE_SEPARATOR + BB_STATUS_FILE;
				destDir = serverLoc + BB_NATIVE_PATH;
				className = "com.konylabs.transformer.Transform";
				classpath = "bb.classpath";
				antRCProperties.put(SOURCE_DIR, srcDir);
				antRCProperties.put(RESOURCE_DIR, resourceDir);
				antRCProperties.put(BUILD_STATUS_FILE, buildStatusFile);
				antRCProperties.put(DESTINATION_DIR, destDir);
				antRCProperties.put(CLASS_NAME, className);
				antRCProperties.put(CLASS_PATH, classpath);
			
				if (executeNativeBuild(buildStatusFile, antRCProperties, platform)) {
					String rel = BuildConstants.DEFAULT_BUILD_OPTION.equals(buildOption) ? "" : "-rel";
					String bbrel = bbBuildOption ? "" : "-lau";
					bbrel = bbrel + rel;
					antRCProperties.put("prefix", bbrel);
					antRCProperties.put("usemds", bbuseMDS + "");
					AntRunner antRunner = new AntRunner();
					antRunner.setBuildFile(tempLocation + "/build/luaj2me/native/build.xml");
					antRunner.setProps(antRCProperties);
					antRunner.setTarget("copy");
					success = antRunner.execute();
				}  else {
					success = false;
				}
			}

			if (androidSelected) {
				srcDir = projLoc + LUASRC_ANDROID;
				resourceDir = pluginLoc + "nativefiles/android";
				buildStatusFile = tempLocation + FILE_SEPARATOR + ANDROID_STATUS_FILE;
				destDir = serverLoc + ANDROID_NATIVE_PATH;
				className = "com.konylabs.transformer.Transform";
				classpath = "android.classpath";
				antRCProperties.put(SOURCE_DIR, srcDir);
				antRCProperties.put(RESOURCE_DIR, resourceDir);
				antRCProperties.put(BUILD_STATUS_FILE, buildStatusFile);
				antRCProperties.put(DESTINATION_DIR, destDir);
				antRCProperties.put(CLASS_NAME, className);
				antRCProperties.put(CLASS_PATH, classpath);

				if (executeNativeBuild(buildStatusFile, antRCProperties, platform)) {
					antRCProperties.put("nativecodegen", "true");
					antRCProperties.put("android.nativedir", serverLoc + ANDROID_NATIVE_PATH);

					TAndroidBuild androidBuild = new TAndroidBuild();
					androidBuild.setProjectName(projectName);
					androidBuild.setAndriodHome(androidHome);
					androidBuild.setAntProperties(antRCProperties);
					success = androidBuild.execute();
				} else {
					success = false;
				}
			}
			if (monitor.isCanceled()) {
				this.cancel();
				ConsoleDisplayManager.getDefault().println("Native code generation is cancelled.", ConsoleDisplayManager.MSG_INFORMATION);
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		} catch (Exception e) {
			success = false;
			ConsoleDisplayManager.getDefault().printException(e.getCause(), ConsoleDisplayManager.MSG_ERROR);
			e.printStackTrace();
			Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation has failed", e);
			return status;
		} finally {
			if (!success) {
				ConsoleDisplayManager.getDefault().println("Native code generation has failed for " + platform, ConsoleDisplayManager.MSG_ERROR);
			} else if (!mainresult) {
				ConsoleDisplayManager.getDefault().println("Native code generation has failed", ConsoleDisplayManager.MSG_ERROR);
			} else {
				ConsoleDisplayManager.getDefault().println("Native code generation is successful for " + platform, ConsoleDisplayManager.MSG_SUCCESS);
			}
			ConsoleDisplayManager.getDefault().println("Time taken for generating native code: " + (System.currentTimeMillis() - time));
		}
	}
	
	private static final String STATUS_KEY = "Status";
	private static final String STATUS_FAILED_TYPE_INFERENCE = "FAILED_TYPE_INFERENCE";
	private static final String STATUS_SUCCEEDED = "SUCCEEDED";
	public static final String STATUS_FAILED_GENERAL = "FAILED_GENERAL";
	public static final String STATUS_FAILED_CRASHED = "FAILED_CRASHED";


	private boolean executeNativeBuild(String buildStatusFile, Map<String, String> antRCProperties, String platform) {
		boolean success = false;
		AntRunner antRunner = new AntRunner();
		antRunner.setBuildFile(pluginLoc + "nativefiles/nativebuild.xml");
		antRunner.setProps(antRCProperties);
		antRunner.setTarget("nativerun");
		success = antRunner.execute();
		if (new File(buildStatusFile).exists()) {
			Properties properties = new Properties();
			FileReader reader = null;
			try {
				reader = new FileReader(buildStatusFile);
				properties.load(reader);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
			Object status = properties.get(STATUS_KEY);
			if (status.equals(STATUS_FAILED_TYPE_INFERENCE)) {
				try {
					typeInferenceFile.refreshLocal(IResource.DEPTH_ZERO, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
				openInferenceditor();
				ConsoleDisplayManager.getDefault().println("Unable to resolve types for variables.", ConsoleDisplayManager.MSG_ERROR);
			}
			return status.equals(STATUS_SUCCEEDED);
		}
		return success;
	}

	public void openInferenceditor() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					VariableTypeInferenceEditor editor = (VariableTypeInferenceEditor) IDE.openEditor(activePage, typeInferenceFile, VariableTypeInferenceEditor.ID);
					editor.setExecutionJob(NativeCodeGenerationJob.this);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void loadProjProperties() {
		
		HashMap<String, String> projPropMap = KUtils.loadProjProperties(projectName);
		splashData = SplashUtils.loadSplashData(projectName);

		if (!projPropMap.isEmpty()) {
			if (projPropMap.get(ProjectProperties.BUILD_OPTION_KEY) != null) {
				buildOption = projPropMap.get(ProjectProperties.BUILD_OPTION_KEY);
			}

			if (projPropMap.get(ProjectProperties.APPNAME_KEY) != null) {
				appName = projPropMap.get(ProjectProperties.APPNAME_KEY);
			}

			if (projPropMap.get(ProjectProperties.APPVERSION_KEY) != null) {
				appVersion = projPropMap.get(ProjectProperties.APPVERSION_KEY);
			} 

			if (projPropMap.containsKey(ProjectProperties.APPID_KEY)) {
				appID = projPropMap.get(ProjectProperties.APPID_KEY);
			}

			if (projPropMap.containsKey(ProjectProperties.SPLASHLOGO_KEY)) {
				splashLogo = KUtils.parseImgNameFrmFilePath(projPropMap.get(ProjectProperties.SPLASHLOGO_KEY));
			}

			if (projPropMap.containsKey(ProjectProperties.PROGBARINDLOGO1_KEY)) {
				splashPBImg1 = KUtils.parseImgNameFrmFilePath(projPropMap.get(ProjectProperties.PROGBARINDLOGO1_KEY));
			}

			if (projPropMap.containsKey(ProjectProperties.PROGBARINDLOGO2_KEY)) {
				splashPBImg2 = KUtils.parseImgNameFrmFilePath(projPropMap.get(ProjectProperties.PROGBARINDLOGO2_KEY));
			}

			if (projPropMap.containsKey(ProjectProperties.PROGBARINDLOGO3_KEY)) {
				splashPBImg3 = KUtils.parseImgNameFrmFilePath(projPropMap.get(ProjectProperties.PROGBARINDLOGO3_KEY));
			}

			if (projPropMap.containsKey(ProjectProperties.PROGBARINDLOGO4_KEY)) {
				splashPBImg4 = KUtils.parseImgNameFrmFilePath(projPropMap.get(ProjectProperties.PROGBARINDLOGO4_KEY));
			}

			if (projPropMap.containsKey(ProjectProperties.SPLASH_FGCOLOR_KEY)) {
				RGB rgb = KUtils.getRGB(projPropMap.get(ProjectProperties.SPLASH_FGCOLOR_KEY));
				splashFGColor = KUtils.getColorString(rgb)+"00";
			} else {
				splashFGColor = "00000000";
			}

			if (projPropMap.containsKey(ProjectProperties.SPLASH_BGCOLOR_KEY)) {
				RGB rgb = KUtils.getRGB(projPropMap.get(ProjectProperties.SPLASH_BGCOLOR_KEY));
				splashBGColor = KUtils.getColorString(rgb)+"00";
			} else {
				splashBGColor = "ffffff00";
			}
			

			iphoneBundleIdentifier = projPropMap.get(ProjectProperties.IPHONE_BUNDLE_IDENTIFIER_KEY);
			if(!ValidationUtil.isNonEmptyString(iphoneBundleIdentifier)) {
				iphoneBundleIdentifier = "com.kony."+appID;
			}
			iphoneBundleVersion = projPropMap.get(ProjectProperties.IPHONE_BUNDLE_VERSION_KEY);
			if(!ValidationUtil.isNonEmptyString(iphoneBundleVersion)) {
				iphoneBundleVersion = "1.0";
			}
			
			String glossyEffect = projPropMap.get(ProjectProperties.IPHONE_IPAD_GLOSSYEFFECT);
			if(glossyEffect != null && "true".equals(glossyEffect)) {
				iphoneipadGlossyEffect = true;
			}
			
			String launchMode = splashData.getLaunchMode();
			String portraitOrientation = projPropMap.get(ProjectProperties.IPAD_PORTRAIT_MODEKEY);
			String landscapeOrientation = projPropMap.get(ProjectProperties.IPAD_LANDSCAPE_MODEKEY);
			
			if (portraitOrientation == null) {
				portraitOrientation = Boolean.TRUE.toString();
			}
			if (landscapeOrientation == null) {
				landscapeOrientation = Boolean.FALSE.toString();
			}
			StringBuilder buffer = new StringBuilder();

			if (Boolean.TRUE.toString().equals(portraitOrientation) || SplashData.PORTRAIT_MODE.equals(launchMode)
					|| SplashData.BOTH_MODE.equals(launchMode)) {
				buffer.append(SplashData.PORTRAIT_MODE.toLowerCase());
				buffer.append(",");
			}

			if (Boolean.TRUE.toString().equals(landscapeOrientation) || SplashData.LANDSCAPE_MODE.equals(launchMode)
					|| SplashData.BOTH_MODE.equals(launchMode)) {
				buffer.append(SplashData.LANDSCAPE_MODE.toLowerCase());
				buffer.append(",");
			}

			KUtils.removeCommaAtEndofBuffer(buffer);
			ipadSupportedOrientations = buffer.toString();
			splashLogo = splashData.getPortrait_splash_image();

			
			String andLoad = projPropMap.get(ProjectProperties.ANDROID_LOAD_INDICATOR_KEY);
			if (andLoad != null && andLoad.trim().length() > 0 ) {
				if("false".equals(andLoad))  {
					andLoadIndctr = false;
				} else {
					andLoadIndctr = true;
				}					
			} else {
				andLoadIndctr = true;
			}
			
			String iphoneLoad = projPropMap.get(ProjectProperties.IPHONE_LOAD_INDICATOR_KEY);
			if ((iphoneLoad != null) && (iphoneLoad.trim().length() > 0) ) {
				if(Boolean.FALSE.toString().equals(iphoneLoad))  {
					iphoneLoadIndctr = false;
				} else {
					iphoneLoadIndctr = true;
				}
			} else {
				iphoneLoadIndctr = true;
			}

			if (projPropMap.containsKey(ProjectProperties.VENDORNAME_KEY)) {
				vendorName = projPropMap.get(ProjectProperties.VENDORNAME_KEY);
			}

			if (projPropMap.containsKey(ProjectProperties.APPLOGO_KEY)) {
				appLogo = KUtils.parseImgNameFrmFilePath(projPropMap.get(ProjectProperties.APPLOGO_KEY));
			}

			if (projPropMap.containsKey(ProjectProperties.MIDDLEWARE_SVC_PORT_KEY)) {
				httpPort = projPropMap.get(ProjectProperties.MIDDLEWARE_SVC_PORT_KEY);
				if (httpPort == null || httpPort.trim().length() == 0) httpPort = "80";
			}

			if (projPropMap.containsKey(ProjectProperties.MIDDLEWARE_HTTPS_PORT_KEY)) {
				httpsPort = projPropMap.get(ProjectProperties.MIDDLEWARE_HTTPS_PORT_KEY);
				if (httpsPort == null || httpsPort.trim().length() == 0) httpsPort = "443";
			}

			if (projPropMap.containsKey(ProjectProperties.THINWEBCONTEXT_KEY)) {
				tcWebContext = projPropMap.get(ProjectProperties.THINWEBCONTEXT_KEY);
				if (tcWebContext == null || tcWebContext.trim().length() == 0) tcWebContext = appID;
			}



			if (projPropMap.containsKey(ProjectProperties.BB_BUILD_OPTION_KEY)) {
				String bbBuildOptionStr = projPropMap.get(ProjectProperties.BB_BUILD_OPTION_KEY);
				if (bbBuildOptionStr != null && bbBuildOptionStr.trim().length() > 0 && "false".equals(bbBuildOptionStr))  {
					bbBuildOption = false;
				}
			}

			if (projPropMap.containsKey(ProjectProperties.BB_USE_MDS_KEY)) {
				String bbuseMDSStr = projPropMap.get(ProjectProperties.BB_USE_MDS_KEY);
				if (bbuseMDSStr != null && bbuseMDSStr.trim().length() > 0 ) {
					if("false".equals(bbuseMDSStr))  {
						bbuseMDS = false;
					} else {
						bbuseMDS = true;
					}					
				}
			}

			if (projPropMap.containsKey(ProjectProperties.COMMENT_MODULE_KEY)) {
				String removeCommentStr = projPropMap.get(ProjectProperties.COMMENT_MODULE_KEY);
				if (removeCommentStr != null && removeCommentStr.trim().length() > 0) {
					if("true".equals(removeCommentStr))  {
						removeComments = true;
					} else {
						removeComments = false;
					}					
				}
			}

			if(projPropMap.containsKey(ProjectProperties.IS_I18N_KEY)) {
				String isi18n = projPropMap.get(ProjectProperties.IS_I18N_KEY);
				if(isi18n != null && "true".equals(isi18n)) {
					enablei18n = true;
					String deflocale = projPropMap.get(ProjectProperties.DEFAULT_LOCALE_KEY);
					if(deflocale != null) {
						defaultlocale = deflocale;
					}
				}
			}

			if(projPropMap.containsKey(ProjectProperties.APPPCKGNAME_KEY)) {
				appPKGName = projPropMap.get(ProjectProperties.APPPCKGNAME_KEY);
			}

		}
		
	}
	
	private HashMap<String, String> getAntRCProperties() {
		HashMap<String, String> dlServerProp = new HashMap<String, String>();
		HashMap<String, String> projPropMap = KUtils.loadProjProperties(projectName);
		
		if (!projPropMap.isEmpty()) {
			if (projPropMap.get(ProjectProperties.BUILD_OPTION_KEY) != null) {
				buildOption = projPropMap.get(ProjectProperties.BUILD_OPTION_KEY);
			}
		}
		String webappsLoc = KEditorPlugin.getWorkspaceLoc()+"/webapps";
		dlServerProp.put(Task.APP_FOLDER_KEY, tempLocation);
		dlServerProp.put(Task.APP_ID_KEY, appID);
		dlServerProp.put(Task.APP_NAME_KEY, appName);
		dlServerProp.put(Task.PROJ_NAME_KEY, projectName);
		dlServerProp.put(Task.WEB_APPS_LOC, webappsLoc);
		dlServerProp.put("logo", appLogo);
		dlServerProp.put("appversion", appVersion);
		dlServerProp.put(Task.VENDOR_NAME_KEY, vendorName);
		dlServerProp.put("w.context", tcWebContext);
		dlServerProp.put(Task.PROPERTIES_FOLDER_KEY, projLoc+"/resources/i18n/properties");
		dlServerProp.put(Task.XML_FOLDER_KEY, projLoc+"/resources/i18n/xml");
		dlServerProp.put(Task.IPHONE_PROPERTIES_FOLDER_KEY, projLoc+"/resources/i18n/iphone_properties");
		dlServerProp.put("i18n.android.properties", projLoc+"/resources/i18n/android_properties");
		dlServerProp.put(CUSTOM_LIBS_DIR_KEY, projLoc + File.separator + CustomLibrariesUtils.RESOURCES_LIBRARIES_FOLDER);
		dlServerProp.put(CUSTOM_LIBS_SRC_DIR_KEY, projLoc + File.separator + CustomLibrariesUtils.SRC_OUTPUT_FOLDER);
		dlServerProp.put(CUSTOM_LIBS_XML_DIR_KEY, projLoc + File.separator + CustomLibrariesUtils.XML_OUTPUT_FOLDER);
		
		Properties i18nprop = KUtils.geti18nProperties(projectName);
		
		locales = KUtils.getAppLocales(projectName, i18nprop);
		
		if(locales != null && locales.trim().length()>0) {
			dlServerProp.put("locales", locales);
			String reslocale[] = locales.split(",");
			StringBuilder buf = new StringBuilder();
			for(int i=0; i< reslocale.length; i++) {
				buf.append(reslocale[i]+"/*");
				buf.append(",");
			}
			KUtils.removeCommaAtEndofBuffer(buf);
			dlServerProp.put("reslocale.str", buf.toString());
		} else {
			dlServerProp.put("locales", "");
			dlServerProp.put("reslocale.str", "");
		}
		
		String iphonelocales = KUtils.getiPhoneAppLocales(projectName, i18nprop);
		if(iphonelocales != null && iphonelocales.trim().length()>0) {
			dlServerProp.put("iphonelocales", iphonelocales);
		} else {
			dlServerProp.put("iphonelocales", "");
		}
		
		String wmlocales = locales.replaceAll(",", "\n");
		dlServerProp.put("wmlocales", wmlocales.trim());
		
		if(enablei18n) {
			dlServerProp.put("defaultlocale", defaultlocale+"");
		}
		dlServerProp.put("guid", win_guid);
		dlServerProp.put(Task.PROJ_NAME_KEY, projectName);
		dlServerProp.put("imagemagick", imgMagicHome);
		dlServerProp.put("system.path",System.getenv("Path"));
		dlServerProp.put(Task.PLUGIN_LOC_KEY, pluginLoc);
		dlServerProp.put(Task.BUILD_OPTION_KEY, buildOption);
		dlServerProp.put(Task.REMOVE_COMMENT_KEY, String.valueOf(removeComments));
		dlServerProp.put(Task.MACHINE_IP, com.pat.tool.keditor.KEditorPlugin.getMachineIP());
		dlServerProp.put("jetty.portnum", jettyPortNum+"");
		dlServerProp.put("jetty.httpsportnum", jettyHttpsPortNum+"");
		
		if (!isEmpty(splashLogo)) {
			dlServerProp.put(Task.SPLASH_IMAGE, splashLogo);
		} 
		String splashVideo = splashData.getPortrait_splash_video();
		dlServerProp.put(Task.SPLASH_VIDEO, splashVideo);
		if (!isEmpty(splashBGColor)) dlServerProp.put("splash.bgcolor", splashBGColor);
		if (!isEmpty(splashFGColor)) dlServerProp.put("splash.fgcolor", splashFGColor);
		
		String launchMode = splashData.getLaunchMode().toLowerCase();
		if("portait".equals(launchMode)) launchMode = "portrait";
		dlServerProp.put(Task.APP_LAUNCH_MODE, launchMode);

		dlServerProp.put(Task.SPLASH_ANDROID_LOAD_INDICATOR, andLoadIndctr+"");
		dlServerProp.put(Task.SPLASH_IPHONE_LOAD_INDICATOR, iphoneLoadIndctr+"");
		dlServerProp.put(Task.FONTS_WORKSPACE_KEY, ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString()+"/"+ FileUtilities.METADATA_KONYSTUDIO_FONTS);

		String splashAnimationImages = SplashUtils.getSplashAnimationImages(splashData, SplashData.PORTRAIT_MODE);
		dlServerProp.put(Task.SPLASH_ANIMATION_LIST, splashAnimationImages);
		String landscape_splashAnimationImages = SplashUtils.getSplashAnimationImages(splashData, SplashData.LANDSCAPE_MODE);
		dlServerProp.put(Task.SPLASH_LANDSCAPE_ANIMATION_LIST, landscape_splashAnimationImages);
		
		dlServerProp.put(Task.SPLASH_VIDEO_INTERRUPTABLE, splashData.isPotrait_animation_interruptable()+"");
		dlServerProp.put(Task.SPLASH_LANDSCAPE_VIDEO_INTERRUPTABLE, splashData.isLandscape_animation_interruptable()+"");
		
		dlServerProp.put(Task.SPLASH_ANIMATION_DURATION, splashData.getPortrait_animationDuration());
		dlServerProp.put(Task.SPLASH_LANDSCAPE_ANIMATION_DURATION, splashData.getLandscape_animationDuration());
		
		
		if (appPKGName != null && appPKGName.trim().length() > 0) {
			dlServerProp.put("packagename",appPKGName);
		}

		dlServerProp.put(Task.BUNDLE_IDENTIFIER, iphoneBundleIdentifier);
		dlServerProp.put(Task.BUNDLE_VERSION, iphoneBundleVersion);
		dlServerProp.put(Task.APPICON_GLOSSYEFFECT, iphoneipadGlossyEffect+"");
		dlServerProp.put(Task.IPAD_SUPPORTED_ORIENTATIONS, ipadSupportedOrientations);
		
		appendPlaformSpecificLogos(dlServerProp);
		
		Properties properties = new Properties();
		properties.putAll(dlServerProp);
		try {
			String fileName = "application.properties";
			properties.store(new FileWriter(fileName), "");
			System.out.println("------------------------------Test----------------------" + new File(fileName).getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dlServerProp;
	}
	
	private void appendPlaformSpecificLogos(HashMap<String, String> dlServerProp) {
		ArrayList<IMobileChannel> mobileChannels = new ArrayList<IMobileChannel>();
		mobileChannels.addAll(MobileChannels.getRichClientChannels());
		mobileChannels.addAll(MobileChannels.getTabletRichClientChannels());
		for (IMobileChannel mobileChannel : mobileChannels) {
			String xmlName = MobileChannels.getXMLName(mobileChannel);
			String logoKey = Task.APP_LOGO + "_" + xmlName;
			if (appLogo == null) {
				appLogo = KUtils.EMPTY_STRING;
			}
			dlServerProp.put(logoKey, ImageUtils.getPlatformSpecificImage(appLogo, mobileChannel));
			
			String splashKey = Task.SPLASH_IMAGE + "_" + xmlName;
			if (splashLogo == null) {
				splashLogo = KUtils.EMPTY_STRING;
			}
			dlServerProp.put(splashKey, ImageUtils.getPlatformSpecificImage(splashLogo, mobileChannel));
			
			String splashVideoKey = Task.SPLASH_VIDEO + "_" + xmlName;
			String splashVideo = splashData.getPortrait_splash_video();
			if (splashVideo == null) {
				splashVideo = KUtils.EMPTY_STRING;
			}
			dlServerProp.put(splashVideoKey, ImageUtils.getPlatformSpecificImage(splashVideo, mobileChannel));
			
			String splashLandscapeKey = Task.SPLASH_LANDSCAPE_IMAGE + "_" + xmlName;
			String splashLandscapeLogo = splashData.getLandscape_splash_image();
			if (splashLandscapeLogo == null) {
				splashLandscapeLogo = KUtils.EMPTY_STRING;
			}
			dlServerProp.put(splashLandscapeKey, ImageUtils.getPlatformSpecificImage(splashLandscapeLogo, mobileChannel));
			
			String splashLandscapeVideoKey = Task.SPLASH_LANDSCAPE_VIDEO + "_" + xmlName;
			String splashLandscapeVideo = splashData.getLandscape_splash_video();
			if (splashLandscapeVideo == null) {
				splashLandscapeVideo = KUtils.EMPTY_STRING;
			}
			dlServerProp.put(splashLandscapeVideoKey, ImageUtils.getPlatformSpecificImage(splashLandscapeVideo, mobileChannel));
		}
	}
	
	private void readPeferences() {
		imgMagicHome = KEditorPlugin.getDefault().getPreferenceStore().getString(IPreferenceConstants.IMGMAGIC_HOME);
		androidHome = KEditorPlugin.getDefault().getPreferenceStore().getString(IPreferenceConstants.ANDROID_HOME);
		jettyPortNum = KEditorPlugin.getDefault().getPreferenceStore().getInt(IPreferenceConstants.JETTY_SERVER_HTTP_PORT);
		jettyHttpsPortNum = KEditorPlugin.getDefault().getPreferenceStore().getInt(IPreferenceConstants.JETTY_SERVER_HTTPS_PORT);
		if (jettyPortNum == 0) jettyPortNum = 8888;
		if (jettyHttpsPortNum == 0) jettyHttpsPortNum = 8443;
	}
	
	private boolean isEmpty(String value) {
		boolean valid = false;
		if (value == null || value.trim().length() == 0) {
			valid = true;
		}
		return valid;
	}
	
	@Override
	public boolean belongsTo(Object family) {
		return projectName.equals(family);
	}
	
	
	public static String getPlatformName(String inferenceFileName) {
		if (inferenceFileName.equals(KConstants.IPHONE_TYPE_INFERENCE_FILE)) {
			return "iPhone";
		} if (inferenceFileName.equals(KConstants.IPAD_TYPE_INFERENCE_FILE)) {
			return "ipad";
		}else if (inferenceFileName.equals(KConstants.ANDROID_TYPE_INFERENCE_FILE)) {
			return "Android";
		} else {
			return "Blackberry";
		}
	}
	
	
	public static IFile getSourceFile(String fileName, String inferenceFileName, IProject project) throws CoreException {
		String folderPath = null;
		if (inferenceFileName.equals(KConstants.IPHONE_TYPE_INFERENCE_FILE)) {
			folderPath = LUASRC_IPHONE + GENERATED_FOLDER;
		} else if (inferenceFileName.equals(KConstants.IPAD_TYPE_INFERENCE_FILE)) {
			folderPath = LUASRC_IPAD + GENERATED_FOLDER;
		} else if (inferenceFileName.equals(KConstants.ANDROID_TYPE_INFERENCE_FILE)) {
			folderPath = LUASRC_ANDROID + GENERATED_FOLDER;
		} else {
			folderPath = LUASRC_BB + GENERATED_FOLDER;
		}
		IFolder genratedFolder = project.getFolder(new Path(folderPath));
		if (genratedFolder.exists()) {
			for (IResource resource : genratedFolder.members()) {
				if (resource.getType() == IResource.FILE) {
					if (resource.getName().equalsIgnoreCase(fileName)) {
						return genratedFolder.getFile(resource.getName());
					}
				}
			}
		}
		return null;
	}
	
	
	private static final String JAVA_EXTENSION = ".java";
	private static final String C_EXTENSION = ".m";
	
	public static File getNativeSourceFile(String fileName, String inferenceFileName, String project) {
		String serverLoc = KUtils.getTempLocation(project) + BUILD_SERVER_PATH;
		String folderPath = null;
		if (inferenceFileName.equals(KConstants.IPHONE_TYPE_INFERENCE_FILE)) {
			folderPath = serverLoc + IPHONE_NATIVE_PATH;
			fileName = fileName + C_EXTENSION;
		} else if (inferenceFileName.equals(KConstants.IPAD_TYPE_INFERENCE_FILE)) {
			folderPath = serverLoc + IPAD_NATIVE_PATH;
			fileName = fileName + C_EXTENSION;
		} else if (inferenceFileName.equals(KConstants.ANDROID_TYPE_INFERENCE_FILE)) {
			folderPath = serverLoc + ANDROID_NATIVE_PATH;
			fileName = fileName + JAVA_EXTENSION;
		} else {
			folderPath = serverLoc + BB_NATIVE_PATH;
			fileName = fileName + JAVA_EXTENSION;
		}
		File dir = new File(folderPath);
		if (dir.exists()) {
			for (File child : dir.listFiles()) {
				if (child.isFile()) {
					if (child.getName().equalsIgnoreCase(fileName)) {
						return child;
					}
				}
			}
		}
		return null;
	}
	

	public boolean isSuccess() {
		return success;
	}
	
}
