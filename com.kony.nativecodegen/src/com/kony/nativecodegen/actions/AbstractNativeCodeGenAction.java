package com.kony.nativecodegen.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import com.pat.tool.keditor.navigation.model.ResourceLeaf;

public abstract class AbstractNativeCodeGenAction implements IObjectActionDelegate {

	protected ResourceLeaf selectedElement;
	protected IViewPart view;

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

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		view = (IViewPart) targetPart;
	}

}
