/* Copyright (C) 2021-2023 by Jon Taylor
   Copyright (C) 1999-2007 by Peter Eastman

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
import java.awt.*;
import java.util.Vector;

/** ReshapeMeshTool is an EditingTool used for moving the vertices of TriangleMesh objects. */

public class ReshapeMeshTool extends MeshEditingTool
{
  private Point clickPoint;
  private Vec3 clickPos, baseVertPos[];
  private UndoRecord undo;

  public ReshapeMeshTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("movePoints_48");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("reshapeMeshTool.helpText"));
  }

  public int whichClicks()
  {
    return HANDLE_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return true;
  }

  public String getToolTipText()
  {
    return Translate.text("reshapeMeshTool.tipText");
  }

  public void mousePressedOnHandle(WidgetMouseEvent e, ViewerCanvas view, int obj, int handle)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    MeshVertex v[] = mesh.getVertices();
    
    clickPoint = e.getPoint();
    clickPos = v[handle].r;
    baseVertPos = mesh.getVertexPositions();
      
    //System.out.println("mousePressedOnHandle ");
  }
  
    /**
     * mouseDragged
     *
     * Description: Mesh editor, a selection is moved.
     */
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
      //System.out.println("ReshapeMeshTool.mouseDragged ");
      
    MeshViewer mv = (MeshViewer) view;
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = e.getPoint();
    Vec3 v[], drag;
    int dx, dy;

    if (undo == null)
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    if (e.isShiftDown())
    {
      if (Math.abs(dx) > Math.abs(dy))
        dy = 0;
      else
        dx = 0;
    }
    v = findDraggedPositions(clickPos, baseVertPos, dx, dy, mv, e.isControlDown(), controller.getSelectionDistance());
      //System.out.println("findDraggedPositions " + v.length );
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
    if (e.isControlDown())
      drag = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
    else
      drag = view.getCamera().findDragVector(clickPos, dx, dy);
    theWindow.setHelpText(Translate.text("reshapeMeshTool.dragText",
        Math.round(drag.x*1e5)/1e5+", "+Math.round(drag.y*1e5)/1e5+", "+Math.round(drag.z*1e5)/1e5));
      
      
      //
        // if the mesh has changed, update any mirror mesh objects to reflect changes.
        //
        if(v.length > 0){
            MirrorMesh mirror = new MirrorMesh();
            mirror.update(controller.getObject());
            
            MirrorCurve mirrorCurve = new MirrorCurve();
            mirrorCurve.update(controller.getObject());
        }
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = e.getPoint();
    int dx, dy;
    Vec3 v[];

    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    if (e.isShiftDown())
    {
      if (Math.abs(dx) > Math.abs(dy))
        dy = 0;
      else
        dx = 0;
    }
    if (dx != 0 || dy != 0)
    {
      if (undo != null)
        theWindow.setUndoRecord(undo);
      v = findDraggedPositions(clickPos, baseVertPos, dx, dy, (MeshViewer) view, e.isControlDown(), controller.getSelectionDistance());
      mesh.setVertexPositions(v);
    }
    controller.objectChanged();
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("reshapeMeshTool.helpText"));
    undo = null;
    baseVertPos = null;
  }

    /**
     * keyPressed
     * Description: Called by ObjectEditorWindow.keyPressed() when moving verts in editor window.
     */
  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    //System.out.println("ReshapeMeshTool.keyPressed() " + controller.getObject().getObject().getClass().getName() );
      
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Vec3 vert[] = mesh.getVertexPositions();
    int i, selectDist[] = controller.getSelectionDistance();
    int key = e.getKeyCode();
    double dx, dy;
    Vec3 v[];
    
    // Pressing an arrow key is equivalent to dragging the first selected point by one pixel.
 
    if (key == KeyPressedEvent.VK_UP)
    {
      dx = 0;
      dy = -1;
    }
    else if (key == KeyPressedEvent.VK_DOWN)
    {
      dx = 0;
      dy = 1;
    }
    else if (key == KeyPressedEvent.VK_LEFT)
    {
      dx = -1;
      dy = 0;
    }
    else if (key == KeyPressedEvent.VK_RIGHT)
    {
      dx = 1;
      dy = 0;
    }
    else
      return;
    e.consume();
    for (i = 0; i < vert.length && selectDist[i] != 0; i++);
    if (i == vert.length)
      return;
    if (view.getSnapToGrid())
    {
      double scale = view.getGridSpacing()*view.getScale();
      if (!e.isAltDown())
        scale /= view.getSnapToSubdivisions();
      dx *= scale;
      dy *= scale;
    }
    else if (e.isAltDown())
    {
      dx *= 10;
      dy *= 10;
    }
    theWindow.setUndoRecord(new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, vert}));
    v = findDraggedPositions(vert[i], vert, dx, dy, (MeshViewer) view, e.isControlDown(), selectDist);
    mesh.setVertexPositions(v);
      
      //
      // if the mesh has changed, update any mirror mesh objects to reflect changes.
      //
      if(v.length > 0){
          MirrorMesh mirror = new MirrorMesh();
          
          mirror.update(controller.getObject());
      }
      
      // If Curve has fixed Length, detect change and recalculate point.
      if(controller.getObject().getObject() instanceof armarender.object.Curve){
          ObjectInfo info = (ObjectInfo)controller.getObject();
          Curve curve = ((Curve)controller.getObject().getObject());
          double fixedLength = curve.getFixedLength();
          //System.out.println("   curve: " + fixedLength);
          if(fixedLength > 0){
              //Mesh mesh = (Mesh) info.getObject();
              //Vec3 [] verts = mesh.getVertexPositions();
              if(vert.length > 0){
                  Vec3 vertA = vert[0];
                  Vec3 vertB = vert[vert.length - 1];
                  double currentDistance = vertA.distance(vertB);
                  System.out.println("   currentDistance: " + currentDistance);
                  //System.out.println("   i: " + i);
                  if(currentDistance > fixedLength){
                      // selected vert, move vert[i] such that length is fixedLength
                      System.out.println("   Long: " + vert[i].x + " " + vert[i].y + " " + vert[i].z );
                  }
              }
          }
      }
      
      //
      // If this selected object is connected to another via a PJO then update the connected point.
      // Used for mesh tools.
      //
      ObjectInfo selectedCurveOI = null;
      if(controller.getObject().getObject() instanceof armarender.object.Curve){
          selectedCurveOI = (ObjectInfo)controller.getObject();
          
          Scene theScene = theWindow.getScene();
          for (ObjectInfo obj : theScene.getObjects()){
              //Object co = (Object)obj.getObject();
              //System.out.println(" -> " + obj.getName());
              
              // Get selected object point ID.
              Vector currObjectSelectedPoints = new Vector(); // indexes
              for (i = 0; i < vert.length; i++){
                  if(selectDist[i] == 0){
                      currObjectSelectedPoints.addElement(i);
                      //System.out.println(" curr selected point: " + i);
                  }
              }
              
              if(obj.getObject() instanceof armarender.object.PointJoinObject){
                  PointJoinObject pjo = (PointJoinObject)obj.getObject();
                  //System.out.println(" * "  + obj.getId());
                  // objectA objectB
                  int connectedObjectId = -1;
                  int connectedVertId = -1;
                  //int connectedSubVertId = -1;
                  int currVertId = -1;
                  //System.out.println(" pjo.objectA "  + pjo.objectA + "  selectedCurveOI.getId(): " + selectedCurveOI.getId());
                  if(pjo.objectA == selectedCurveOI.getId()){
                      //System.out.println(" B ");
                      connectedObjectId = pjo.objectB;
                      if(pjo.objectBPoint == -1){ // objectBSubPoint
                          pjo.updateLocation();
                      }
                      boolean pointSelected = false;
                      for(int ii = 0; ii < currObjectSelectedPoints.size(); ii++){
                          int selectedObjectVert = (int)currObjectSelectedPoints.elementAt(ii);
                          if(selectedObjectVert == pjo.objectAPoint){
                              pointSelected = true;
                          }
                      }
                      if(pointSelected){
                          //System.out.println("objectBPoint: " + pjo.objectBPoint);
                          connectedVertId = pjo.objectBPoint;
                      }
                      currVertId = pjo.objectAPoint;
                  }
                  //System.out.println(" pjo.objectB "  + pjo.objectB + "  selectedCurveOI.getId(): " + selectedCurveOI.getId());
                  if(pjo.objectB == selectedCurveOI.getId()){
                      //System.out.println(" A ");
                      connectedObjectId = pjo.objectA;
                      if(pjo.objectAPoint == -1){
                          pjo.updateLocation();
                      }
                      boolean pointSelected = false;
                      for(int ii = 0; ii < currObjectSelectedPoints.size(); ii++){
                          int selectedObjectVert = (int)currObjectSelectedPoints.elementAt(ii);
                          if( selectedObjectVert == pjo.objectBPoint){
                              pointSelected = true;
                          }
                      }
                      if(pointSelected){
                          connectedVertId = pjo.objectAPoint;
                      }
                      currVertId = pjo.objectBPoint;
                  }
                  
                  // currVertId is wrong
                  // connectedVertId is wrong ****
                  //System.out.println(" connectedObjectId " + connectedObjectId + "  connectedVertId: " + connectedVertId);
                  //System.out.println(" curr obj " + selectedCurveOI.getName() + " curr vertId " + currVertId);
                  
                  if(connectedVertId != -1 && currVertId != -1){
                      //System.out.println(" connectedObjectId " + connectedObjectId + "  connectedVertId: " + connectedVertId);
                      //System.out.println(" curr obj " + selectedCurveOI.getName() + " curr vertId " + currVertId);
                      
                      // Move Object: connectedObjectId
                      // Move Point: connectedVertId
                      // Target point location: selectedCurveOI - currVertId (translated through the connected object coordinate system.)
                      
                      ObjectInfo connectedCurveOI = null;
                      connectedCurveOI = theScene.getObjectById(connectedObjectId);
                      if(connectedCurveOI != null){
                          //System.out.println(" connected object name: " + connectedCurveOI.getName());
                          
                          CoordinateSystem cs = ((ObjectInfo)connectedCurveOI).getCoords();
                          Curve connectedCurve = (Curve)connectedCurveOI.getObject().duplicate();
                          Vec3 [] connectedVerts = connectedCurve.getVertexPositions();
                          if(connectedVertId < connectedVerts.length){
                              Vec3 connectedVec = connectedVerts[connectedVertId];
                              //System.out.println("Modify: " + connectedVec.x + " " + connectedVec.y + " " + connectedVec.z +  " to: " );
                              
                              
                              Vec3 connectedVert[] = connectedCurve.getVertexPositions();
                              int connectedSelectDist[] = new int[connectedVert.length];
                              for(int j = 0; j < connectedSelectDist.length; j++){
                                  if(j == connectedVertId){
                                      connectedSelectDist[j] = 0;
                                  } else {
                                      connectedSelectDist[j] = 1;
                                  }
                              }
                              
                              v = findDraggedPositions(connectedVert[connectedVertId], connectedVert, dx, dy, (MeshViewer)view, e.isControlDown(), connectedSelectDist);
                              
                              //System.out.println("v length " + v.length);
                              connectedCurve.setVertexPositions(v);
                              //connectedCurve.objectChanged();
                              connectedCurveOI.clearCachedMeshes();
                              connectedCurveOI.setObject(connectedCurve);
                              
                              // Update the PJO
                              // *** NOT WORKING ***
                              //PointJoinObject pjo = (PointJoinObject)obj.getObject();
                              //pjo.setVertexPositions();
                              obj.setObject(pjo);
                              obj.clearCachedMeshes();
                              
                          }
                          
                      } else {
                          System.out.println(" error getting object : " + connectedVertId);
                      }
                  }
              }
          }
      }
      
    controller.objectChanged();
    theWindow.updateImage();
  }
  
    /**
     * findDraggedPositions
     *
     * Description:
     */
  private Vec3 [] findDraggedPositions(Vec3 pos, Vec3 vert[], double dx, double dy, MeshViewer view, boolean controlDown, int selectDist[])
  {
    int maxDistance = view.getController().getTensionDistance();
    double tension = view.getController().getMeshTension();
    Vec3 drag[] = new Vec3 [maxDistance+1], v[] = new Vec3 [vert.length];
    
      //System.out.println("maxDistance " + maxDistance + " tension " +tension );
      
    if (controlDown)
      drag[0] = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
    else
      drag[0] = view.getCamera().findDragVector(pos, dx, dy);
    for (int i = 1; i <= maxDistance; i++)
      drag[i] = drag[0].times(Math.pow((maxDistance-i+1.0)/(maxDistance+1.0), tension));
    if (view.getUseWorldCoords())
    {
      Mat4 trans = view.getDisplayCoordinates().toLocal();
      for (int i = 0; i < drag.length; i++)
        trans.transformDirection(drag[i]);
    }
    for (int i = 0; i < vert.length; i++)
    {
        // JDT Not sure why this started pulling adjacent points all of a sudden
        // selDist 0 == selected
      //if (selectDist[i] > -1 ) { // (OLD) 0=sel, 1 =
      if (selectDist[i] == 0) { // JDT new only move selected points.
          //System.out.println("A selectDist[i] " + selectDist[i]);
          v[i] = vert[i].plus(drag[selectDist[i]]);
          //v[i] = new Vec3(vert[i]);
      } else {
          //System.out.println("B selectDist[i] " + selectDist[i]);
        v[i] = new Vec3(vert[i]);
          
      }
    }
    return v;
  }
   
}
