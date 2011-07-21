package com.kony.nativecodegen.actions;

import org.eclipse.jface.action.IAction;

import com.pat.tool.keditor.utils.KConstants;

public class OpenIpadInferenceEditor  extends AbstractNativeCodeGenAction {

	@Override
	public void run(IAction action) {
		OpenAllInferenceEditors.openEditor(KConstants.IPAD_TYPE_INFERENCE_FILE, selectedElement);
	}

}