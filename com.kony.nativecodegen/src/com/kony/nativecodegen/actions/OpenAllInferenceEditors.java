package com.kony.nativecodegen.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.pat.tool.keditor.navigation.model.ResourceLeaf;
import com.pat.tool.keditor.utils.KConstants;

public class OpenAllInferenceEditors  extends AbstractNativeCodeGenAction {

	@Override
	public void run(IAction action) {
		openEditor(KConstants.IPHONE_TYPE_INFERENCE_FILE, selectedElement);
		openEditor(KConstants.ANDROID_TYPE_INFERENCE_FILE, selectedElement);
		openEditor(KConstants.BB_TYPE_INFERENCE_FILE, selectedElement);
		
	}
	
	public static void openEditor(String fileName, ResourceLeaf resourceLeaf) {
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getProject(resourceLeaf.getName()).getFile(fileName);
	    if (file.exists()) {
	    	IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	    	try {
	    		IDE.openEditor(activePage, file);
	    	} catch (PartInitException e) {
	    		e.printStackTrace();
	    	}
	    }
	}

}