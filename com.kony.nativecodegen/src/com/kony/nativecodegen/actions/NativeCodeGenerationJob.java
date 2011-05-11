package com.kony.nativecodegen.actions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import com.pat.tool.keditor.console.ConsoleDisplayManager;
import com.pat.tool.keditor.preferences.IPreferenceConstants;
import com.pat.tool.keditor.tasks.TAndroidBuild;
import com.pat.tool.keditor.tasks.TJetty;
import com.pat.tool.keditor.tasks.Task;
import com.pat.tool.keditor.utils.CustomLibrariesUtils;
import com.pat.tool.keditor.utils.FileUtilities;
import com.pat.tool.keditor.utils.KUtils;
import com.pat.tool.keditor.utils.ProjectProperties;

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
	private String buildOption = KUtils.DEFAULT_BUILD_OPTION;
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

	public static final String IPHONE_TYPE_INFERENCE_FILE = "typeinference_iphone.npf";
	public static final String ANDROID_TYPE_INFERENCE_FILE = "typeinference_android.npf";
	public static final String BB_TYPE_INFERENCE_FILE = "typeinference_bb.npf";
	private static final String IPHONE_STATUS_FILE = "iphone_buildstatus.properties";
	private static final String ANDROID_STATUS_FILE = "android_buildstatus.properties";
	private static final String BB_STATUS_FILE = "bb_buildstatus.properties";
	private static final char FILE_SEPARATOR = '/';
	private static final String LUASRC_ANDROID = "/luasrc/android";
	private static final String LUASRC_BB = "/luasrc/bb";
	private static final String LUASRC_IPHONE = "/luasrc/iphone";
	private static final String GENERATED_FOLDER = "/generated";

	
    private IFile typeInferenceFile;	
	
	public NativeCodeGenerationJob(String name, String projectName, String inferenceFileName) {
		super(name);
		this.projectName = projectName;
		this.inferenceFileName = inferenceFileName;
		iphoneSelected = inferenceFileName.equals(IPHONE_TYPE_INFERENCE_FILE);
		androidSelected = inferenceFileName.equals(ANDROID_TYPE_INFERENCE_FILE);
		bbSelected = inferenceFileName.equals(BB_TYPE_INFERENCE_FILE);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		typeInferenceFile = project.getFile(inferenceFileName);
	    setRule(project);
	}


	
	public IStatus run(IProgressMonitor monitor) {
		boolean succeeded = true;
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

			String serverLoc = tempLocation + "/build/server";

			String srcXmlDir = tempLocation + "/output";

			HashMap<String, String> antRCProperties = getAntRCProperties();
			antRCProperties.put(SOURCE_XML_DIR, srcXmlDir);

			String srcDir = projLoc + LUASRC_IPHONE;
			String resourceDir = pluginLoc + "nativefiles/iphone";
			String typeInferenceFile = projLoc + FILE_SEPARATOR +  IPHONE_TYPE_INFERENCE_FILE;
			String buildStatusFile = tempLocation + FILE_SEPARATOR + IPHONE_STATUS_FILE;
			String destDir = serverLoc + "/iphonenative";
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
			if (iphoneSelected) {
				if (executeNativeBuild(buildStatusFile, antRCProperties, platform)) {
					// Handle KAR File, this may require restarting Jetty server
					AntRunner antRunner = new AntRunner();
					antRunner.setBuildFile(tempLocation + "/build/server/build.xml");
					antRunner.setProps(antRCProperties);
					antRunner.setTarget("nativeiphone");
					succeeded = antRunner.execute();

					TJetty jetty = new TJetty();
					jetty.setClean(false);
					succeeded = jetty.execute();
				} else {
					succeeded = false;
				}
			}

			if (bbSelected) {
				srcDir = projLoc + LUASRC_BB;
				resourceDir = pluginLoc + "nativefiles/blackberry";
				typeInferenceFile = projLoc + FILE_SEPARATOR + BB_TYPE_INFERENCE_FILE;
				buildStatusFile = tempLocation + FILE_SEPARATOR + BB_STATUS_FILE;
				destDir = serverLoc + "/bbnative";
				className = "com.konylabs.transformer.Transform";
				classpath = "bb.classpath";
				antRCProperties.put(SOURCE_DIR, srcDir);
				antRCProperties.put(RESOURCE_DIR, resourceDir);
				antRCProperties.put(TYPE_INFERENCE_FILE, typeInferenceFile);
				antRCProperties.put(BUILD_STATUS_FILE, buildStatusFile);
				antRCProperties.put(DESTINATION_DIR, destDir);
				antRCProperties.put(CLASS_NAME, className);
				antRCProperties.put(CLASS_PATH, classpath);
			
				if (executeNativeBuild(buildStatusFile, antRCProperties, platform)) {
					String rel = KUtils.DEFAULT_BUILD_OPTION.equals(buildOption) ? "" : "-rel";
					String bbrel = bbBuildOption ? "" : "-lau";
					bbrel = bbrel + rel;
					antRCProperties.put("prefix", bbrel);
					antRCProperties.put("usemds", bbuseMDS + "");
					AntRunner antRunner = new AntRunner();
					antRunner.setBuildFile(tempLocation + "/build/luaj2me/native/build.xml");
					antRunner.setProps(antRCProperties);
					antRunner.setTarget("copy");
					succeeded = antRunner.execute();
				}  else {
					succeeded = false;
				}
			}

			if (androidSelected) {
				srcDir = projLoc + LUASRC_ANDROID;
				resourceDir = pluginLoc + "nativefiles/android";
				typeInferenceFile = projLoc + FILE_SEPARATOR + ANDROID_TYPE_INFERENCE_FILE;
				buildStatusFile = tempLocation + FILE_SEPARATOR + ANDROID_STATUS_FILE;
				destDir = serverLoc + "/androidnative";
				className = "com.konylabs.transformer.Transform";
				classpath = "android.classpath";
				antRCProperties.put(SOURCE_DIR, srcDir);
				antRCProperties.put(RESOURCE_DIR, resourceDir);
				antRCProperties.put(TYPE_INFERENCE_FILE, typeInferenceFile);
				antRCProperties.put(BUILD_STATUS_FILE, buildStatusFile);
				antRCProperties.put(DESTINATION_DIR, destDir);
				antRCProperties.put(CLASS_NAME, className);
				antRCProperties.put(CLASS_PATH, classpath);

				if (executeNativeBuild(buildStatusFile, antRCProperties, platform)) {
					antRCProperties.put("nativecodegen", "true");
					antRCProperties.put("android.nativedir", serverLoc + "/androidnative");

					TAndroidBuild androidBuild = new TAndroidBuild();
					androidBuild.setProjectName(projectName);
					androidBuild.setAndriodHome(androidHome);
					androidBuild.setAntProperties(antRCProperties);
					succeeded = androidBuild.execute();
				} else {
					succeeded = false;
				}
			}
			if (monitor.isCanceled()) {
				this.cancel();
				ConsoleDisplayManager.getDefault().println("Native code generation is cancelled.", ConsoleDisplayManager.MSG_INFORMATION);
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		} catch (Exception e) {
			succeeded = false;
			ConsoleDisplayManager.getDefault().printException(e.getCause(), ConsoleDisplayManager.MSG_ERROR);
			e.printStackTrace();
			Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation is failed", e);
			return status;
		} finally {
			if (!succeeded) {
				ConsoleDisplayManager.getDefault().println("Native code generation failed for:-->" + platform, ConsoleDisplayManager.MSG_ERROR);
			} else if (!mainresult) {
				ConsoleDisplayManager.getDefault().println("Native code generation failed", ConsoleDisplayManager.MSG_ERROR);
			} else {
				ConsoleDisplayManager.getDefault().println("Native code generation is successfull for " + platform, ConsoleDisplayManager.MSG_WARNING);
			}
			ConsoleDisplayManager.getDefault().println("Time taken for generating native code: " + (System.currentTimeMillis() - time));
		}
	}
	
	private static final String STATUS_KEY = "Status";
	private static final String STATUS_FAILED_TYPE_INFERENCE = "FAILED_TYPE_INFERENCE";


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
							//ignore
						}
					}
				}
				if (properties.get(STATUS_KEY).equals(STATUS_FAILED_TYPE_INFERENCE)) {
					try {
						typeInferenceFile.refreshLocal(IResource.DEPTH_ZERO, null);
					} catch (CoreException e) {
						e.printStackTrace();
					}
					openInferenceditor();
					ConsoleDisplayManager.getDefault().println("Unable to resolve types for variables.", ConsoleDisplayManager.MSG_ERROR);
					return false;
				}
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

//			if (projPropMap.containsKey(ProjectProperties.WIN_BUILD_OPTION_KEY)) {
//				String wmbuildStr = projPropMap.get(ProjectProperties.WIN_BUILD_OPTION_KEY);
//				if (wmbuildStr != null && wmbuildStr.trim().length() > 0 ) {
//					if("false".equals(wmbuildStr))  {
//						wmBuildOption = false;
//					} else {
//						wmBuildOption = true;
//					}					
//				}
//			}
//
//			if (projPropMap.containsKey(ProjectProperties.WIN_SMOOTH_SCROLL_KEY)) {
//				String wmsmoothStr = projPropMap.get(ProjectProperties.WIN_SMOOTH_SCROLL_KEY);
//				if (wmsmoothStr != null && wmsmoothStr.trim().length() > 0 ) {
//					if("false".equals(wmsmoothStr))  {
//						wmsmoothScroll = false;
//					} else {
//						wmsmoothScroll = true;
//					}					
//				}
//			}

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
		
		dlServerProp.put(Task.APP_FOLDER_KEY, tempLocation);
		dlServerProp.put(Task.APP_ID_KEY, appID);
		dlServerProp.put(Task.APP_NAME_KEY, appName);
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
			StringBuffer buf = new StringBuffer();
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

		if (!isEmpty(splashLogo)) dlServerProp.put("splash.image", splashLogo);
		if (!isEmpty(splashPBImg1)) dlServerProp.put("splash.progress1", splashPBImg1);
		if (!isEmpty(splashPBImg2)) dlServerProp.put("splash.progress2", splashPBImg2);
		if (!isEmpty(splashPBImg3)) dlServerProp.put("splash.progress3", splashPBImg3);
		if (!isEmpty(splashPBImg4)) dlServerProp.put("splash.progress4", splashPBImg4);
		if (!isEmpty(splashBGColor)) dlServerProp.put("splash.bgcolor", splashBGColor);
		if (!isEmpty(splashFGColor)) dlServerProp.put("splash.fgcolor", splashFGColor);
		
		dlServerProp.put("splash.li", andLoadIndctr+"");
		dlServerProp.put("fonts_workspace", ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString()+"/"+ FileUtilities.METADATA_KONYSTUDIO_FONTS);

		if (appPKGName != null && appPKGName.trim().length() > 0) {
			dlServerProp.put("packagename",appPKGName);
		}

		return dlServerProp;
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
		if (inferenceFileName.equals(IPHONE_TYPE_INFERENCE_FILE)) {
			return "iPhone";
		} else if (inferenceFileName.equals(ANDROID_TYPE_INFERENCE_FILE)) {
			return "Android";
		} else {
			return "Blackberry";
		}
	}
	
	
	public static IFile getSourceFile(String fileName, String inferenceFileName, IProject project) throws CoreException {
		String folderPath = null;
		if (inferenceFileName.equals(IPHONE_TYPE_INFERENCE_FILE)) {
			folderPath = LUASRC_IPHONE + GENERATED_FOLDER;
		} else if (inferenceFileName.equals(ANDROID_TYPE_INFERENCE_FILE)) {
			folderPath = LUASRC_ANDROID + GENERATED_FOLDER;
		} else {
			folderPath = LUASRC_BB + GENERATED_FOLDER;
		}
		IFolder genratedFolder = project.getFolder(new Path(folderPath));
		for (IResource resource : genratedFolder.members()) {
			if (resource.getType() == IResource.FILE) {
				if (resource.getName().equalsIgnoreCase(fileName)) {
					return genratedFolder.getFile(resource.getName());
				}
			}
		}
		return null;
	}
	
}
