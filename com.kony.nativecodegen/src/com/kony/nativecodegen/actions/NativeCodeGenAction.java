package com.kony.nativecodegen.actions;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.progress.IProgressConstants;

import com.kony.nativecodegen.NativeCodeGenPlugin;
import com.kony.nativecodegen.ui.NativeCodePlatformSelectionDialog;
import com.pat.tool.keditor.KEditorPlugin;
import com.pat.tool.keditor.ant.AntRunner;
import com.pat.tool.keditor.console.ConsoleDisplayManager;
import com.pat.tool.keditor.navigation.model.ResourceLeaf;
import com.pat.tool.keditor.preferences.IPreferenceConstants;
import com.pat.tool.keditor.tasks.TAndroidBuild;
import com.pat.tool.keditor.tasks.TJetty;
import com.pat.tool.keditor.tasks.Task;
import com.pat.tool.keditor.utils.CustomLibrariesUtils;
import com.pat.tool.keditor.utils.FileUtilities;
import com.pat.tool.keditor.utils.KUtils;
import com.pat.tool.keditor.utils.ProjectProperties;

/**
 * Native code generation is supported only IPhone,Windows Phone, Android and
 * BlackBerry. So for the first time allow to choose the platform from the
 * dialog and create the respective folder under <code>nativecode</code> folder.
 * 
 * @author Rakesh
 */

public class NativeCodeGenAction implements IObjectActionDelegate {


	private IViewPart view;

	private ResourceLeaf selectedElement;
	
	private String projectName;
	
	private String locales;
	
	//Ant property names
	public static final String CUSTOM_LIBS_DIR_KEY = "customlibs.dir";
	public static final String CUSTOM_LIBS_SRC_DIR_KEY = "customlibs.src.dir";
	public static final String CUSTOM_LIBS_XML_DIR_KEY = "customlibs.xml.dir";
	
	
	private String imgMagicHome = KUtils.EMPTY_STRING;
	private String androidHome = KUtils.EMPTY_STRING;
	private String publishURL = KUtils.EMPTY_STRING;
	private String buildOption = KUtils.DEFAULT_BUILD_OPTION;
	private String appID =  KUtils.EMPTY_STRING;
	private String appName =  KUtils.EMPTY_STRING;
	private String appVersion = "1.0.0";
	private String vendorName =  KUtils.EMPTY_STRING;
	private String appLogo = KUtils.EMPTY_STRING;
	private String httpPort = "80";
	private String httpsPort = "443";
	private String tcWebContext = KUtils.EMPTY_STRING;
	private boolean isWinSelected = false;
	
	
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
	private boolean wmBuildOption = true;
	private boolean wmsmoothScroll = true;
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
	
	private String errorStr = "";
	private boolean mainresult;
	
	long time;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		view = (IViewPart) targetPart;
	}

	@Override
	public void run(IAction action) {
		iphoneSelected = false;
		androidSelected = false;
		bbSelected = false;
		errorStr = "";
		projectName = selectedElement.getName();
		projLoc = KUtils.getProjectLocation(projectName);
		pluginLoc = NativeCodeGenPlugin.getDefault().getInstallLocation();
		
		tempLocation = KUtils.getTempLocation(projectName);
		view.getSite().getPage().saveAllEditors(true);
		NativeCodePlatformSelectionDialog dialog = new NativeCodePlatformSelectionDialog(
				view.getViewSite().getShell());
		dialog.setHelpAvailable(false);
		int option = dialog.open();
		if (option == Window.OK) {
			int selectedState = dialog.getSelectedState();
			
			if(selectedState == 0) return;
			
			readPeferences();
			loadProjProperties();
			
			int value = selectedState & NativeCodePlatformSelectionDialog.IPHONE_SELECTED;
			if (value == NativeCodePlatformSelectionDialog.IPHONE_SELECTED) {
				iphoneSelected = true;
			}
			
			value = selectedState & NativeCodePlatformSelectionDialog.ANDROID_SELECTED;
			if (value == NativeCodePlatformSelectionDialog.ANDROID_SELECTED) {
				androidSelected = true;
			}

			value = selectedState & NativeCodePlatformSelectionDialog.BB_SELECTED;
			if (value == NativeCodePlatformSelectionDialog.BB_SELECTED) {
				bbSelected = true;				
			}

			value = selectedState & NativeCodePlatformSelectionDialog.WINDOWS_SELECTED;
			if (value == NativeCodePlatformSelectionDialog.WINDOWS_SELECTED) {
				winSelected = true;
			}
			
			StringBuffer platString = new StringBuffer();
			if(iphoneSelected) platString.append("iPhone,");
			if(bbSelected) platString.append("Blackberry,");
			if(androidSelected) platString.append("Android,");
			
			if(platString.length() > 0) {
				KUtils.removeCommaAtEndofBuffer(platString);
			}
			
			time = System.currentTimeMillis();
			Job job = new Job("Generating native code for "+platString.toString()) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
			
				try {
					HashMap<String, String> antNativeProperties = new HashMap<String, String>();
					antNativeProperties.put("iphone", iphoneSelected+"");
					antNativeProperties.put("bb", bbSelected+"");
					antNativeProperties.put("android", androidSelected+"");
					antNativeProperties.put("winmobile", winSelected+"");
					antNativeProperties.put(Task.APP_FOLDER_KEY, tempLocation);
					antNativeProperties.put(Task.PLUGIN_LOC_KEY, pluginLoc);
					
					AntRunner nativeAnt = new AntRunner();
					nativeAnt.setBuildFile(pluginLoc+"/nativefiles/nativebuild.xml");
					nativeAnt.setTarget("all");
					nativeAnt.setProps(antNativeProperties);
					mainresult = nativeAnt.execute();
					if(!mainresult) {
						Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation failed. Please build the project and try again");
						return status;
					}
					
					String serverLoc = tempLocation+"/build/server";
					
					String inputDir = serverLoc+"/iphonelua";
					String outputDir = serverLoc+"/iphonenative";
					
					String apiDefLoc = pluginLoc+"/nativefiles/apiDef";
					String javaSTG = pluginLoc+"/nativefiles/JavaSTG.stg";
					
					String andapiDefLoc = pluginLoc+"/nativefiles/andriodApiDef";
					String andSTG = pluginLoc+"/nativefiles/AndriodSTG.stg";
					
					String resourcesDir = pluginLoc+"/nativefiles/iPhoneTrans/resources";
					
					HashMap<String, String> antRCProperties = getAntRCProperties();
					
					String platform = "iphone";
					
					if(iphoneSelected) {
						
						//com.konylabs.iphone.transformer.Transform.main(new String[] {inputDir, outputDir, resourcesDir});
						
						AntRunner iphoneantRunner = new AntRunner();
						iphoneantRunner.setBuildFile(pluginLoc+"/nativefiles/nativebuild.xml");
						antRCProperties.put("input.dir", inputDir);
						antRCProperties.put("output.dir", outputDir);
						antRCProperties.put("resource.dir", resourcesDir);
						iphoneantRunner.setProps(antRCProperties);
						iphoneantRunner.setTarget("iphonerun");
						boolean success = iphoneantRunner.execute();
						
						if (!success) {
							errorStr += "iPhone";
							Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation failed. See console for details");
							return status;
						}
						
						//Handle KAR File, this may require restarting Jetty server
						
						AntRunner antRunner = new AntRunner();
						antRunner.setBuildFile(tempLocation+"/build/server/build.xml");
						antRunner.setProps(antRCProperties);
						antRunner.setTarget("nativeiphone");
						antRunner.execute();
						
						TJetty jetty = new TJetty();
						jetty.setClean(false);
						jetty.execute();
						
					}
					
					if(bbSelected) {
						platform = "bb";
						inputDir = serverLoc+"/bblua";
						outputDir = serverLoc+"/bbnative";
						
						//com.konylabs.transformer.CodeGen.main(new String[] {inputDir, outputDir, platform, apiDefLoc, javaSTG});
						AntRunner bbantRunner = new AntRunner();
						bbantRunner.setBuildFile(pluginLoc+"/nativefiles/nativebuild.xml");
						antRCProperties.put("input.dir", inputDir);
						antRCProperties.put("output.dir", outputDir);
						antRCProperties.put("platform", platform);
						antRCProperties.put("apidef.loc", apiDefLoc);
						antRCProperties.put("stg.loc", javaSTG);
						bbantRunner.setProps(antRCProperties);
						bbantRunner.setTarget("nativerun");
						boolean success = bbantRunner.execute();
						
						if (!success) {
							if(errorStr.length() > 0) errorStr += ", ";
							errorStr += "Blackberry";
							Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation failed. See console for details");
							return status;
						}
						
						
						String rel =  KUtils.DEFAULT_BUILD_OPTION.equals(buildOption) ? "" : "-rel";
						String bbrel =  bbBuildOption ? "" : "-lau";
						bbrel = bbrel + rel;
						antRCProperties.put("prefix", bbrel);
						antRCProperties.put("usemds", bbuseMDS+"");
						AntRunner antRunner = new AntRunner();
						antRunner.setBuildFile(tempLocation+"/build/luaj2me/native/build.xml");
						antRunner.setProps(antRCProperties);
						antRunner.setTarget("copy");
						antRunner.execute();
					}
					
					if(androidSelected) {
						platform = "andriod";
						inputDir = serverLoc+"/androidlua";
						outputDir = serverLoc+"/androidnative";
						
						//com.konylabs.transformer.CodeGen.main(new String[] {inputDir, outputDir, platform, apiDefLoc, javaSTG});
						AntRunner andantRunner = new AntRunner();
						andantRunner.setBuildFile(pluginLoc+"/nativefiles/nativebuild.xml");
						antRCProperties.put("input.dir", inputDir);
						antRCProperties.put("output.dir", outputDir);
						antRCProperties.put("platform", platform);
						antRCProperties.put("apidef.loc", andapiDefLoc);
						antRCProperties.put("stg.loc", andSTG);
						andantRunner.setProps(antRCProperties);
						andantRunner.setTarget("nativerun");
						boolean success = andantRunner.execute();
						if (!success) {
							if(errorStr.length() > 0) errorStr += ", ";
							errorStr += "Android";
							Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation failed. See console for details");
							return status;
						}
						
						antRCProperties.put("nativecodegen", "true");
						antRCProperties.put("android.nativedir", serverLoc+"/androidnative");
						
						TAndroidBuild androidBuild = new TAndroidBuild();
						androidBuild.setProjectName(projectName);
						androidBuild.setAndriodHome(androidHome);
						androidBuild.setAntProperties(antRCProperties);
						androidBuild.execute();
					}
					if (monitor.isCanceled()) {
            			this.cancel();
            			ConsoleDisplayManager.getDefault().println("Native code generation is cancelled.", ConsoleDisplayManager.MSG_INFORMATION);
            			return Status.CANCEL_STATUS;
            		}
					return Status.OK_STATUS;
				} catch(Exception e) {
					ConsoleDisplayManager.getDefault().printException(e.getCause(), ConsoleDisplayManager.MSG_ERROR);
					e.printStackTrace();
					Status status = new Status(Status.ERROR, KEditorPlugin.PLUGIN_ID, "Native code generation is failed", e);
	            	return status;
				} finally {
					if(errorStr.length() > 1) {
						ConsoleDisplayManager.getDefault().println("Native code generation failed for:-->"+errorStr, ConsoleDisplayManager.MSG_ERROR);
					} else if (!mainresult){
						ConsoleDisplayManager.getDefault().println("Native code generation failed", ConsoleDisplayManager.MSG_ERROR);
					} else {
						ConsoleDisplayManager.getDefault().println("Native code generation is successfull", ConsoleDisplayManager.MSG_ERROR);
					}
	            	ConsoleDisplayManager.getDefault().println("Time taken for generating native code: "+(System.currentTimeMillis()-time));
	            }
			}			
		};
		job.setProperty(IProgressConstants.PROPERTY_IN_DIALOG, Boolean.FALSE);
	    job.setPriority(Job.BUILD);
	    job.setUser(true);
	    job.schedule();
		}
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

			if (projPropMap.containsKey(ProjectProperties.WIN_BUILD_OPTION_KEY)) {
				String wmbuildStr = projPropMap.get(ProjectProperties.WIN_BUILD_OPTION_KEY);
				if (wmbuildStr != null && wmbuildStr.trim().length() > 0 ) {
					if("false".equals(wmbuildStr))  {
						wmBuildOption = false;
					} else {
						wmBuildOption = true;
					}					
				}
			}

			if (projPropMap.containsKey(ProjectProperties.WIN_SMOOTH_SCROLL_KEY)) {
				String wmsmoothStr = projPropMap.get(ProjectProperties.WIN_SMOOTH_SCROLL_KEY);
				if (wmsmoothStr != null && wmsmoothStr.trim().length() > 0 ) {
					if("false".equals(wmsmoothStr))  {
						wmsmoothScroll = false;
					} else {
						wmsmoothScroll = true;
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
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				Object firstElement = ssel.getFirstElement();
				if (firstElement instanceof ResourceLeaf) {
					selectedElement = (ResourceLeaf) ssel.getFirstElement();
				}
			}
		}
	}

}
