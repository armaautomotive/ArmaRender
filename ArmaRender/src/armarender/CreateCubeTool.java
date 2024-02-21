/* Copyright (C) 2022-2023 by Jon Taylor
   Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.animation.*;
import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;

import java.awt.*;

/** CreateCubeTool is an EditingTool used for creating Cube objects. */

public class CreateCubeTool extends EditingTool
{
  static int counter = 1;
  private boolean shiftDown;
  private Point clickPoint;
  private ObjectInfo objInfo;
  private boolean dragOccured = false;
    
  public CreateCubeTool(LayoutWindow fr)
  {
    super(fr);
      initButton("cube_48");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("createCubeTool.helpText"));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("createCubeTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint = e.getPoint();
    shiftDown = e.isShiftDown();
      
      // if no drag, deselect existing object. TODO
      
  }
  
    /**
     * mouseDragged
     *
     * Description:
     */
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
      dragOccured = true;
      boolean rebuildBooleanScene = false;
      
    if (objInfo == null)
    {
      // Create the cube.
      
      Scene theScene = ((LayoutWindow) theWindow).getScene();
      objInfo = new ObjectInfo(new Cube(1.0, 1.0, 1.0), new CoordinateSystem(), "Cube "+(counter++));
      objInfo.addTrack(new PositionTrack(objInfo), 0);
      objInfo.addTrack(new RotationTrack(objInfo), 1);
      UndoRecord undo = new UndoRecord(theWindow, false);
      int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
      
        ObjectInfo existingSelectedInfo = null;
        if(sel.length == 1){
            existingSelectedInfo = ((LayoutWindow) theWindow).getScene().getObject( sel[0] );
            if(existingSelectedInfo != null
               &&
               existingSelectedInfo.getObject() instanceof SceneCamera == false &&
               existingSelectedInfo.getObject() instanceof DirectionalLight == false
               //( existingSelectedInfo.getObject() instanceof Mesh ||
               // existingSelectedInfo.getObject() instanceof Curve)
               ){
                existingSelectedInfo.addChild(objInfo, 0);
                objInfo.setParent(existingSelectedInfo);
                
                // If selected object is Folder with action type of Subtract or intersection, make object display mode transparent.
                if(existingSelectedInfo.getObject() instanceof Folder){
                    Folder f = (Folder)existingSelectedInfo.getObject();
                    if(f.getAction() == Folder.SUBTRACT || f.getAction() == Folder.INTERSECTION || f.getAction() == Folder.UNION){
                        objInfo.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                        objInfo.clearCachedMeshes();
                        ObjectInfo parent = existingSelectedInfo.getParent();
                        if(parent != null){
                            parent.setDisplayModeOverride( ViewerCanvas.RENDER_TRANSPARENT + 1);
                            parent.clearCachedMeshes();
                            // Regenerate boolean objects. (Cant generate here, object not exist)
                            rebuildBooleanScene = true;
                        }
                        
                    } // folder is subtract or intersection.
                } // selection is folder
            } // existingSelectedInfo
        } // end sel.length == 1
        
        ((LayoutWindow) theWindow).addObject(objInfo, undo);
      undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
      theWindow.setUndoRecord(undo);
      ((LayoutWindow) theWindow).setSelection(theScene.getNumObjects()-1);
        
      ((LayoutWindow) theWindow).rebuildItemList(); // redraw item list.
    }
    
    // Determine the size and position for the cube.
    
    Scene theScene = ((LayoutWindow) theWindow).getScene();
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    Vec3 v1, v2, v3, orig, xdir, ydir, zdir;
    double xsize, ysize, zsize;
    
    if (shiftDown)
    {
      if (Math.abs(dragPoint.x-clickPoint.x) > Math.abs(dragPoint.y-clickPoint.y))
      {
        if (dragPoint.y < clickPoint.y)
          dragPoint.y = clickPoint.y - Math.abs(dragPoint.x-clickPoint.x);
        else
          dragPoint.y = clickPoint.y + Math.abs(dragPoint.x-clickPoint.x);
      }
      else
      {
        if (dragPoint.x < clickPoint.x)
          dragPoint.x = clickPoint.x - Math.abs(dragPoint.y-clickPoint.y);
        else
          dragPoint.x = clickPoint.x + Math.abs(dragPoint.y-clickPoint.y);
      }
    }
    v1 = cam.convertScreenToWorld(clickPoint, Camera.DEFAULT_DISTANCE_TO_SCREEN);
    v2 = cam.convertScreenToWorld(new Point(dragPoint.x, clickPoint.y), Camera.DEFAULT_DISTANCE_TO_SCREEN);
    v3 = cam.convertScreenToWorld(dragPoint, Camera.DEFAULT_DISTANCE_TO_SCREEN);
    orig = v1.plus(v3).times(0.5);
    if (dragPoint.x < clickPoint.x)
      xdir = v1.minus(v2);
    else
      xdir = v2.minus(v1);
    if (dragPoint.y < clickPoint.y)
      ydir = v3.minus(v2);
    else
      ydir = v2.minus(v3);
    xsize = xdir.length();
    ysize = ydir.length();
    xdir = xdir.times(1.0/xsize);
    ydir = ydir.times(1.0/ysize);
    zdir = xdir.cross(ydir);
    zsize = Math.min(xsize, ysize);

    // Update the size and position, and redraw the display.
    
    ((Cube) objInfo.getObject()).setSize(xsize, ysize, zsize);
    objInfo.getCoords().setOrigin(orig);
    objInfo.getCoords().setOrientation(zdir, ydir);
    objInfo.clearCachedMeshes();
    theWindow.setModified();
    theWindow.updateImage();
  }
  
    /**
     * mouseReleased
     *
     */
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
      boolean rebuildBooleanScene = false;
      if(objInfo != null){
          ObjectInfo parent = objInfo.getParent();
          if(parent != null){
                //System.out.println("Parent  " + parent.getName());
              if(parent.getObject() instanceof Folder ){
                  Folder f = (Folder)parent.getObject();
                  if(f.getAction() == Folder.SUBTRACT || f.getAction() == Folder.INTERSECTION || f.getAction() == Folder.UNION){
                      rebuildBooleanScene = true;
                  }
              }
          }
      }
      
      //
      // If we click (no drag) on the selected object, then leave it selected.
      //
      int i, sel[];
      sel = ((LayoutWindow) theWindow).getScene().getSelection();
      for (i = 0; i < sel.length; i++)
      {
        ObjectInfo info = ((LayoutWindow) theWindow).getScene().getObject(sel[i]);
        view.getCamera().setObjectTransform(info.getCoords().fromLocal());
        Rectangle bounds;
        bounds = view.getCamera().findScreenBounds(info.getBounds());
        if (!info.isLocked() && bounds != null && !info.isChildrenHiddenWhenHidden() && pointInRectangle(clickPoint, bounds))
        {
            dragOccured = true; // Casuses selection to remain
        }
      }
      
      
    // If no cube was added then deselect any scene objects.
    // This allows easy adding new cubes to existing children.
    if(dragOccured == false){
        System.out.println("Deselect existing scene object. ");
        ((LayoutWindow) theWindow).clearSelection();
        ((LayoutWindow) theWindow).updateImage();
    }
      if(dragOccured == true){
          //Sound.playSound("success.wav");
          
          if(rebuildBooleanScene){
              if(theWindow instanceof LayoutWindow){
                  //((LayoutWindow)theWindow).booleanSceneCommand(); // rebuilds entire scene. Time consuming.
                  // Update boolean object
                  ObjectInfo parent = objInfo.getParent();
                  if(parent != null){
                      if(parent.getObject() instanceof Folder){
                          ObjectInfo rootObject = parent.getParent();
                          if(rootObject != null){
                              ((LayoutWindow)theWindow).updateBooleanObject( rootObject );
                          }
                      }
                  }
              }
          }
      }
          
      objInfo = null;
    dragOccured = false;
  }
    
    // From SceneViewer
    private boolean pointInRectangle(Point p, Rectangle r)
    {
      return (r.x-1 <= p.x && r.y-1 <= p.y && r.x+r.width+1 >= p.x && r.y+r.height+1 >= p.y);
    }
    
    
    public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
    {
        if (e.getKeyCode() == 37 ||
            e.getKeyCode() == 38 ||
            e.getKeyCode() == 39 ||
            e.getKeyCode() == 40){
            Sound.playSound("deny.wav");
        }
    }
}
