/*  Copyright (C) 2020-2023 by Jon Taylor
    Copyright (C) 1999-2007 by Peter Eastman

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
import buoy.widget.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Vector;

/** CreateMirrorPlaneTool is an EditingTool used for creating Curve objects. */

public class CreateMirrorPlaneTool extends EditingTool
{
  static int counter = 1;
  private Vector<Vec3> clickPoint;
  private Vector<Float> smoothness;
  private int smoothing;
  private MirrorPlaneObject theCurve;
  private CoordinateSystem coords;
  public static final int HANDLE_SIZE = 3;
  private EditingWindow window;

  public CreateMirrorPlaneTool(EditingWindow fr)
  {
    super(fr);
      this.window = fr;
      initButton("mirrorPlane_48");
    smoothing = Mesh.APPROXIMATING;
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("createMirrorPlaneTool.helpText"));
  }

  public void deactivate()
  {
    super.deactivate();
    addToScene();
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
      return "Creaate mirror plane object."; // Translate.text("createCurveTool.tipText");
  }

  public boolean hilightSelection()
  {
    return (clickPoint == null);
  }
  
  public void drawOverlay(ViewerCanvas view)
  {
    //System.out.println(" drawOverlay ");
    Camera cam = view.getCamera();

    if (clickPoint == null)
      return;
    
    if (theCurve != null)
    {
        Mat4 trans = cam.getWorldToScreen().times(coords.fromLocal());
        WireframeMesh mesh = theCurve.getWireframeMesh();
        Point p[] = new Point [mesh.vert.length];
        for (int i = 0; i < p.length; i++)
        {
            Vec2 v = trans.timesXY(mesh.vert[i]);
            p[i] = new Point((int) v.x, (int) v.y);
        }
        for (int i = 0; i < mesh.from.length; i++){
            view.drawLine(p[mesh.from[i]], p[mesh.to[i]], ViewerCanvas.lineColor);
            
            //Point pointA = new Point( (int) (screenPos.x-HANDLE_SIZE/2) + 20, (int) (screenPos.y-HANDLE_SIZE/2) + 20 );
            //Point pointB = new Point( (int) (screenPos.x-HANDLE_SIZE/2) - 20, (int) (screenPos.y-HANDLE_SIZE/2) - 20 );
            //view.drawLine(pointA, pointB, new Color(255,0,0) );
        }
        System.out.println(" draw overlay   points: " + mesh.from.length);
        
        //view.drawLine(p[mesh.from[i]], p[mesh.to[i]], ViewerCanvas.lineColor);
        
        
    }
      
      // test
    for (int i = 0; i < clickPoint.size(); i++)
      {
        Vec3 pos = (Vec3) clickPoint.lastElement();
        Vec2 screenPos = view.getCamera().getWorldToScreen().timesXY(pos);
        view.drawBox((int) screenPos.x-HANDLE_SIZE/2, (int) screenPos.y-HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, ViewerCanvas.handleColor);
          
        
          //view.drawLine(p[mesh.from[i]], p[mesh.to[i]], ViewerCanvas.lineColor);
          Point pointA = new Point( (int) (screenPos.x-HANDLE_SIZE/2) + 20, (int) (screenPos.y-HANDLE_SIZE/2) + 20 );
          Point pointB = new Point( (int) (screenPos.x-HANDLE_SIZE/2) - 20, (int) (screenPos.y-HANDLE_SIZE/2) - 20 );
          view.drawLine(pointA, pointB, new Color(255,0,0) );
      }
  }
  
    
    /**
     * mousePressed
     *
     * Description:
     */
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (clickPoint == null)
    {
      clickPoint = new Vector<Vec3>();
      smoothness = new Vector<Float>();
      view.repaint();
    }
    else
    {
      Vec3 pos = (Vec3) clickPoint.lastElement(); // null pointer
        if(pos != null){
          Vec2 screenPos = view.getCamera().getWorldToScreen().timesXY(pos);
          view.drawDraggedShape(new Line2D.Float(new Point2D.Double(screenPos.x, screenPos.y), e.getPoint()));
        }
    }
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (clickPoint.size() == 0)
      return;
    Point dragPoint = e.getPoint();
    Vec3 pos = (Vec3) clickPoint.lastElement();
    Vec2 screenPos = view.getCamera().getWorldToScreen().timesXY(pos);
    view.drawDraggedShape(new Line2D.Float(new Point2D.Double(screenPos.x, screenPos.y), dragPoint));
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    Vec3 vertex[], orig, ydir, zdir;
    float s[];

      //System.out.println(" mousereleased clickPoint.size()"  + clickPoint.size() );
      
      // abort creation of dimension
      //if (clickPoint.size() > 1 && clickPoint.size() < 2 ){
          // abort, delete points
          //System.out.println(" *** Abort *** ");
      //}
        
    // Addpoint to dimension
    if (e.getClickCount() != 2 && clickPoint.size() <= 2 )
      {
        clickPoint.addElement(cam.convertScreenToWorld(dragPoint, Camera.DEFAULT_DISTANCE_TO_SCREEN));
        smoothness.addElement(new Float(e.isShiftDown() ? 0.0f : 1.0f));
      }
      
      System.out.println(" size: " +clickPoint.size() );
    // Create dimension
    if (clickPoint.size() >= 1)
      {
        // Create a new line object.  First, find all the points in world coordinates.
            
        vertex = new Vec3 [clickPoint.size()];
        s = new float [clickPoint.size()];
        orig = new Vec3();
        for (int i = 0; i < vertex.length; i++)
          {
            vertex[i] = (Vec3) clickPoint.elementAt(i);
            s[i] = ((Float) smoothness.elementAt(i)).floatValue();
            orig = orig.plus(vertex[i]);
          }
        orig = orig.times(1.0/vertex.length);   // ???

        // Find the object's coordinate system.

        ydir = cam.getViewToWorld().timesDirection(Vec3.vy());
        zdir = cam.getViewToWorld().timesDirection(new Vec3(0.0, 0.0, -1.0));
        coords = new CoordinateSystem(orig, zdir, ydir);
        if (view.getSnapToGrid())
        {
          double spacing = view.getGridSpacing()/view.getSnapToSubdivisions();
          Vec3 offset = coords.toLocal().times(vertex[0]);
          offset.x = Math.IEEEremainder(offset.x, spacing);
          offset.y = Math.IEEEremainder(offset.y, spacing);
          offset.z = Math.IEEEremainder(offset.z, spacing);
          coords.fromLocal().transformDirection(offset);
          coords.setOrigin(orig.plus(offset));
        }

        // Transform all of the vertices into the object's coordinate system.
            
        for (int i = 0; i < vertex.length; i++)
        {
            vertex[i] = coords.toLocal().times(vertex[i]);
        }
        theCurve = new MirrorPlaneObject(vertex, s, smoothing, false);
        
          if(clickPoint.size() >= 2)
          //if (e.getClickCount() == 2)
          {
            theCurve.setClosed(e.isControlDown());
            addToScene();
            return;
          }
          
        cam.setObjectTransform(coords.fromLocal()); // ??? what do? camera add transform.
      }
      
    theWindow.updateImage();
  }
  
  /** When the user presses Enter, add the curve to the scene. */
  
  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    if (e.getKeyCode() == KeyPressedEvent.VK_ENTER && theCurve != null)
      {
        theCurve.setClosed(e.isControlDown());
        addToScene();
        e.consume();
      } else {
          if (e.getKeyCode() == 37 ||
              e.getKeyCode() == 38 ||
              e.getKeyCode() == 39 ||
              e.getKeyCode() == 40){
              Sound.playSound("deny.wav");
          }
      }
  }

  /**
   * addToScene
   *
   * Description: Add the curve to the scene.
   *  If an existing object is selected, add this object as a child of it.
   */
  private void addToScene()
  {
    boolean addCurve = (theCurve != null);
    if (addCurve)
      {
        ObjectInfo info = new ObjectInfo(theCurve, coords, "Mirror Plane "+(counter++));
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
          
          UndoRecord undo = new UndoRecord(theWindow, false);
          int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
          
        //
        // Set object as child of selection
        //
        ObjectInfo existingSelectedInfo = null;
        if(sel.length == 1){
            existingSelectedInfo = ((LayoutWindow) theWindow).getScene().getObject( sel[0] );
        }
        if(existingSelectedInfo != null &&
           ( existingSelectedInfo.getObject() instanceof Mesh ||
            existingSelectedInfo.getObject() instanceof Curve)
           ){
            existingSelectedInfo.addChild(info, 0);
            info.setParent(existingSelectedInfo);
        }
            
        ((LayoutWindow) theWindow).addObject(info, undo);
          
          // Generate mirrored objects
          if(existingSelectedInfo != null){
                if(existingSelectedInfo.getObject() instanceof MirrorPlaneObject ||
                   existingSelectedInfo.getObject() instanceof SplineMesh){
                    MirrorMesh mirror = new MirrorMesh();
                    mirror.update(existingSelectedInfo);
                }
                if(
                //clickedObject.getObject() instanceof MirrorPlaneObject ||
                   existingSelectedInfo.getObject() instanceof Curve){

                    MirrorCurve mirror = new MirrorCurve();
                    mirror.update(existingSelectedInfo);
                }
          }
          
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        theWindow.setUndoRecord(undo);
        ((LayoutWindow) theWindow).setSelection(theWindow.getScene().getNumObjects()-1);
        ((LayoutWindow) theWindow).rebuildItemList(); // redraw item list.
      } // end addCurve
    clickPoint = null;
    smoothness = null;
    theCurve = null;
    coords = null;
      if (addCurve) {
          theWindow.updateImage();
          theWindow.updateMenus();
      }
  }

  /**
   *
   *
   */
  public void iconDoubleClicked()
  {
      UpdateSceneMirroredObjects();
      
    BComboBox smoothingChoice = new BComboBox(new String [] {
      Translate.text("Interpolating"),
      Translate.text("Approximating")
    });
    if (smoothing == Mesh.INTERPOLATING)
      smoothingChoice.setSelectedIndex(0);
    else
      smoothingChoice.setSelectedIndex(1);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("selectCurveSmoothing"),
                new Widget [] {smoothingChoice},
                new String [] {Translate.text("Smoothing Method")});
    if (!dlg.clickedOk())
      return;
    if (smoothingChoice.getSelectedIndex() == 0)
      smoothing = Mesh.INTERPOLATING;
    else
      smoothing = Mesh.APPROXIMATING;
      
      
      
  }
    
    /**
     * UpdateSceneMirroredObjects
     *
     * Description:
     */
    public void UpdateSceneMirroredObjects(){
        // Update mirrored objects (DEBUG TESTING ONLY)
        Scene theScene = window.getScene();
        Vector<ObjectInfo> sceneObjects = theScene.getObjects();
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo info = sceneObjects.elementAt(i);
            //System.out.println(" o " + info.getName() );
            
            boolean mirrorObject = false;
            ObjectInfo[] children = info.getChildren();
            for(int c = 0; c < children.length; c++){
                ObjectInfo childInfo = children[i];
                if(childInfo.getObject() instanceof MirrorPlaneObject){
                    mirrorObject = true;
                    c = children.length;
                }
            }
            
            // Mirror each type of object
            if(mirrorObject && info.getObject() instanceof ArcObject){
                System.out.println("Mirror an Arc Object. ");
                
                
            }
            
        }
        theWindow.updateImage();
    }
}
