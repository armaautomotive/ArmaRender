/* Copyright (C) 2022 by Jon Taylor - Arma Automotive Inc.
 
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.Vector;
import java.util.HashMap;
import armarender.LayoutModeling; // JDT
import javax.swing.JOptionPane; 

/** MoveObjectTool is an EditingTool used for moving objects in a scene. */

public class CreateFolderTool extends EditingTool
{
  Point clickPoint;
  Vec3 objectPos[];
  Vector<ObjectInfo> toMove;
  ObjectInfo clickedObject;
  boolean dragged, applyToChildren = true;
  private static int counter = 1;

  public CreateFolderTool(EditingWindow fr)
  {
    super(fr);
    initButton("folder_48");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("Create Folder"));
  }

  public int whichClicks()
  {
    return OBJECT_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return false;
  }

  public String getToolTipText()
  {
    return Translate.text("folderTool.tipText"); // moveObjectTool.tipText
  }

    /**
     * addFolder
     *
     * Description: Add a folder.
     */
    public void addFolder(String name){
        Folder folderObj = new Folder();
        CoordinateSystem coords;
        coords = new CoordinateSystem();
        
        ObjectInfo info = new ObjectInfo(folderObj, coords, name);
        //info.addTrack(new PositionTrack(info), 0);
        //info.addTrack(new RotationTrack(info), 1);
        UndoRecord undo = new UndoRecord(theWindow, false);
        int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
        ((LayoutWindow) theWindow).addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        theWindow.setUndoRecord(undo);
        ((LayoutWindow) theWindow).setSelection(((LayoutWindow) theWindow).getScene().getNumObjects()-1);
        //points = null;
        theWindow.updateImage();
    }
    
    
   public void addFolderCommand(){
        addFolder("Folder");
    }
    
  public void mousePressedOnObject(WidgetMouseEvent e, ViewerCanvas view, int obj)
  {
    System.out.println("ADD FOLDER 1 "); // JDT

      addFolder("Folder");
  }
    
    public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
    {
        //clickPoint = e.getPoint();
        //shiftDown = e.isShiftDown();
        
        System.out.println("ADD FOLDER 2 ");
        addFolder("Folder");
    }

/**
* mouseDragged
*
*
*/
  public void mouseDragged(final WidgetMouseEvent e, final ViewerCanvas view)
  {
    
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
	//System.out.println("MoveObjectTool.mouseReleased()"); // JDT
    //theWindow.getScene().applyTracksAfterModification(toMove);
    //theWindow.setHelpText(Translate.text("moveObjectTool.helpText"));
    //toMove = null;
    //objectPos = null;
    //theWindow.updateImage();
  }

    /**
     * keyPressed
     *
     * Description: key pressed, move when object selected in main canvas views.
     */
  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
	LayoutModeling layout = new LayoutModeling();
    Scene theScene = theWindow.getScene();
    //layout.setBaseDir(theScene.getDirectory() + "\\" + theScene.getName() + "_layout_data" );
    Camera cam = view.getCamera();
    CoordinateSystem c;
    UndoRecord undo;
    int i, sel[];
    double dx, dy;
    Vec3 v = null;
    int key = e.getKeyCode();
      
      //System.out.println("MoveObjectTool.keyPressed() " + key);
      
      // TODO:
      // If there are any PJO objects update the connected points.
      // Oct 21 2021
      // See ReshapeMeshTool.java for code that moves connected curve points.
      // Possibly refactor all code to modify connected curve class.

    // Pressing an arrow key is equivalent to dragging the first selected object by one pixel.

     

      
    theWindow.getScene().applyTracksAfterModification(toMove);
    theWindow.updateImage();
  }
    
    
    public void moveVertex(int objectId){
        Scene theScene = theWindow.getScene();
        
        int count = theScene.getNumObjects();
        for(int i = 0; i < count; i++){
            ObjectInfo obj = theScene.getObject(i);
            //if( obj.getId() == this.objectA ){
                
            //}
        }
    }

  /* Allow the user to set options. */

  public void iconDoubleClicked()
  {
      /*
    BCheckBox childrenBox = new BCheckBox(Translate.text("applyToUnselectedChildren"), applyToChildren);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("moveToolTitle"),
		new Widget [] {childrenBox}, new String [] {null});
    if (!dlg.clickedOk())
      return;
    applyToChildren = childrenBox.getState();
       */
      
      String folderName = "Folder";
      // Get the user's name.
      folderName = JOptionPane.showInputDialog("Folder Name", "Folder");

            // Display message
            //JOptionPane.showMessageDialog(null, "Hello " + name);
      
      addFolder(folderName);
  }
}
