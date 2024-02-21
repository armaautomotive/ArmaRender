/* Copyright (C) 2021-2023 by Jon Taylor
   Copyright (C) 1999-2006 by Peter Eastman

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

/** The CurveViewer class is a component which displays a Curve object and 
    allows the user to edit it.
    This draws curves in the editor window.
 */

public class CurveViewer extends MeshViewer
{
  boolean draggingSelectionBox, dragging;
  int deselect;

  public CurveViewer(MeshEditController window, RowContainer p)
  {
    super(window, p);
  }

    /**
     * drawObject
     *
     * Description: Draw curve in editor window.
     */
  protected void drawObject()
  {
    if (!showMesh)
      return;
      
    MeshVertex v[] = ((Mesh) getController().getObject().getObject()).getVertices();
    boolean selected[] = controller.getSelection();

    WireframeMesh wireframe = getController().getObject().getObject().getWireframeMesh();
    //System.out.println("CurveViewer  drawObject " + wireframe.vert.length + " wireframe.from.length " +  wireframe.from.length);

    for (int i = 0; i < wireframe.from.length; i++){
      renderLine(wireframe.vert[wireframe.from[i]], wireframe.vert[wireframe.to[i]], theCamera,  lineColor);         // Draw lines
    }
    
    for (int i = 0; i < v.length; i++)
      if (!selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
        {
          Vec2 p = theCamera.getObjectToScreen().timesXY(v[i].r);
          double z = theCamera.getObjectToView().timesZ(v[i].r);
          renderBox(((int) p.x) - HANDLE_SIZE/2, ((int) p.y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, lineColor);
        }
      
      // Draw arc
      Color blue = new Color(0, 0, 190);
      Color lightBlue = new Color(100, 100, 255);
      Color red = new Color(255, 0, 0);
      
      Object o = getController().getObject().getObject();
      ObjectInfo oi = getController().getObject();
      if(o instanceof ArcObject){                               // Object is ArcObject type
          LayoutModeling layout = new LayoutModeling();
          
          ArcObject arc = (ArcObject)o;
          double bendRadius = arc.getBendRadius();
          double tubeDiameter = arc.getTubeDiameter();
          //arc.renderObject( oi,    , new Vec3(0,1,0));
          
          // Have the Arc Object draw istelf. Includes, segment lines, arcs and tube diameter.
          Vec3 viewDir = theCamera.getViewToWorld().timesDirection(Vec3.vz());
          arc.renderObject(oi, this, viewDir);
          
      } // end ArcObject
      
      //
      // Draw edit points - and selection color. Do last so it appears on top of other drawing.
      //
      Color col = (currentTool.hilightSelection() ? highlightColor : lineColor);
      //col = new Color(0, 255, 0);
      for (int i = 0; i < v.length; i++){
        if (selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
          {
            Vec2 p = theCamera.getObjectToScreen().timesXY(v[i].r);
            double z = theCamera.getObjectToView().timesZ(v[i].r);
            renderBox(((int) p.x) - HANDLE_SIZE/2, ((int) p.y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, col);
          }
      }
  }
    
    
    
    /**
     * renderTubeCylendar
     *
     * Description: render illustration tube for arc profile.
     *  The scene object needs to be set specifically in this object.
     */
    /*
    public void renderTubeCylendar(ViewerCanvas canvas, Vec3 [] verts, Vec3 a, Vec3 b){
        if(scene != null){
            Camera cam = canvas.getCamera();
            
            double height = a.distance(b);
            Cylinder cyl = new Cylinder(height, tubeDiameter, tubeDiameter, 1); // double height, double xradius, double yradius, double ratio
            cyl.setTexture(scene.getEnvironmentTexture(), scene.getEnvironmentMapping());
            ObjectInfo cylOI = new ObjectInfo(cyl, new CoordinateSystem(), "Render");
            
            CoordinateSystem cylCS = cylOI.getCoords();
            
            Vec3 cylLocation = a.midPoint(b);
            cylCS.setOrigin( cylLocation );
            
            Vec3 vertAX_ = new Vec3(a); //  verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
            Vec3 vertBX_ = new Vec3(b); // verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
            vertAX_.subtract(vertBX_);
            vertAX_.normalize();
            //System.out.println("      vertAX " +vertAX );
            Vec3 zero = new Vec3(0.0, 0.0, 0.0);
            
           
            // The vector between a and b is perpendicular to the desured orientation. So we need to rotate then calculate the perp Up Dir.
            // To find a perpendicular vector for vertAX_, rotate it on the vector, then find the cross product of vertAX_ and the rotated vector.
            
            // Dot product of zero and vertAX_
            
            Vec3 rotatedNormal = new Vec3(vertAX_);                      // vertAX_ is normal vector segment direction to point
            //CoordinateSystem cs2 = new CoordinateSystem();
            
          
            // Given arbitrary x, y, then z = z2 = (-x1 * x2 - y1 * y2) / z1
            Vec3 perp1 = new Vec3(1,1,0); // X and Y are inputs, equation solves for Z.
            if(vertAX_.z == 0){
                vertAX_.z = vertAX_.z + 0.00001;
            }
            double z2 = (-vertAX_.x * perp1.x - vertAX_.y * perp1.y) / vertAX_.z;
            perp1.z = z2;
            
            rotatedNormal = perp1;
            
            //Vec3 perpendicularNormal = new Vec3(rotatedNormal);
            //double z2_ = (-rotatedNormal.x * perpendicularNormal.x - rotatedNormal.y * perpendicularNormal.y) / rotatedNormal.z;
            //perpendicularNormal.z = z2_;
            //perpendicularNormal.normalize();
            
            cylCS.setOrientation(perp1, vertAX_);
            
            RenderingMesh rm = cyl.getRenderingMesh(0.1, true, cylOI);
            //WireframeMesh wm = cyl.getWireframeMesh();
            
            Mat4 mat4 = cylCS.duplicate().fromLocal();
            rm.transformMesh(mat4);
            
            //wm.transformMesh(mat4);
            
            ConstantVertexShader shader = new ConstantVertexShader(ViewerCanvas.transparentColor);
            canvas.renderMeshTransparent(rm, shader, cam, vertAX_, null);
            //canvas.renderWireframe(wm, cam, new Color(100, 100, 100));
            
        } else {
            //System.out.println("Scene is null ");
        }
    }
    */
    

  /** When the user presses the mouse, forward events to the current tool as appropriate.
      If this is a vertex based tool, allow them to select or deselect vertices.
   
     Editor Window.
   */

  protected void mousePressed(WidgetMouseEvent e)
  {
      //System.out.println("CurveViewer.mousePressed() " );
    int i, j, x, y;
    double z, nearest;
    MeshVertex v[] = ((Curve) getController().getObject().getObject()).getVertices();
    Vec2 pos;
    Point p;

    requestFocus();
    sentClick = false;
    deselect = -1;
    dragging = false;
    clickPoint = e.getPoint();
    
    // Determine which tool is active.
    
    if (metaTool != null && (e.isMetaDown() || e.getButton() == 3 ))
      activeTool = metaTool;
    else if (altTool != null && e.isAltDown())
      activeTool = altTool;
    else
      activeTool = currentTool;

    // If the current tool wants all clicks, just forward the event and return.

    if ((activeTool.whichClicks() & EditingTool.ALL_CLICKS) != 0)
      {
        activeTool.mousePressed(e, this);
        dragging = true;
        sentClick = true;
      }
    boolean allowSelectionChange = activeTool.allowSelectionChanges();
    boolean wantHandleClicks = ((activeTool.whichClicks() & EditingTool.HANDLE_CLICKS) != 0);
    if (!allowSelectionChange && !wantHandleClicks)
      return;

    // See whether the click was on a currently selected vertex.
    
    p = e.getPoint();
    boolean selected[] = controller.getSelection();
    for (i = 0; i < v.length; i++)
      if (selected[i])
        {
          pos = theCamera.getObjectToScreen().timesXY(v[i].r);
          x = (int) pos.x;
          y = (int) pos.y;
          if (x >= p.x-HANDLE_SIZE / 2 &&
              x <= p.x+HANDLE_SIZE/2 &&
              y >= p.y-HANDLE_SIZE/2 - (HANDLE_SIZE/2) - 1 &&
              y <= p.y+HANDLE_SIZE/2)
            break;
        }
    if (i < v.length)
      {
        // The click was on a selected vertex.  If it was a shift-click, the user may want
        // to deselect it, so set a flag.  Forward the event to the current tool.
        
        if (e.isShiftDown() && allowSelectionChange)
          deselect = i;
        if (wantHandleClicks)
        {
          activeTool.mousePressedOnHandle(e, this, 0, i);
          sentClick = true;
        }
        return;
      }

    // The click was not on a selected vertex.  See whether it was on an unselected one.
    // If so, select it and send an event to the current tool.
    
    j = -1;
    nearest = Double.MAX_VALUE;
    for (i = 0; i < v.length; i++)
      {
        pos = theCamera.getObjectToScreen().timesXY(v[i].r);
        x = (int) pos.x;
        y = (int) pos.y;
          
          // Debug
          //System.out.println("  click i  " + i + "  x: " + x + " y: " + y +  "  - p.x " + p.x + " p.y " + p.y + "  handle: " + HANDLE_SIZE);
          
        if (x >= p.x-HANDLE_SIZE/2 &&
            x <= p.x+HANDLE_SIZE/2 &&
            y >= p.y-HANDLE_SIZE/2 - (HANDLE_SIZE/2) - 1 && // modify bottom of bounds.
            y <= p.y+HANDLE_SIZE/2  )
          {
            z = theCamera.getObjectToView().timesZ(v[i].r);
              //System.out.println("  z: " + z + " < nearest " + nearest );
            if (
                //z > 0.0 &&                                             // For some reason z can be negative. Might be camera pased in from Scene.
                z < nearest)                                             //
              {
                nearest = z;
                j = i;
              }
          }
      }
      //System.out.println(" Click j: " + j);
      //System.out.println("nearest " + nearest);
      
      
    if (j > -1)
      {
        if (allowSelectionChange)
        {
          boolean oldSelection[] = (boolean []) selected.clone();
          if (!e.isShiftDown())
            for (i = 0; i < selected.length; i++)
              selected[i] = false;
          selected[j] = true;
          ((CurveEditorWindow) controller).findSelectionDistance();
          currentTool.getWindow().setUndoRecord(new UndoRecord(currentTool.getWindow(), false, UndoRecord.SET_MESH_SELECTION, new Object [] {controller, new Integer(controller.getSelectionMode()), oldSelection}));
          controller.setSelection(selected);
          activeTool.getWindow().updateMenus();
        }
        if (e.isShiftDown())
          repaint();
        else if (wantHandleClicks)
        {
          activeTool.mousePressedOnHandle(e, this, 0, j);
          sentClick = true;
        }
        return;
      }
    
    // The click was not on a handle.  Start dragging a selection box.

    if (allowSelectionChange)
    {
      draggingSelectionBox = true;
      beginDraggingSelection(p, false);
    }
  }

  protected void mouseDragged(WidgetMouseEvent e)
  {
    if (!dragging)
      {
        Point p = e.getPoint();
        if (Math.abs(p.x-clickPoint.x) < 2 && Math.abs(p.y-clickPoint.y) < 2)
          return;
      }
    dragging = true;
    deselect = -1;
    super.mouseDragged(e);
  }

  protected void mouseReleased(WidgetMouseEvent e)
  {
    int i, x, y;
    MeshVertex v[] = ((Curve) getController().getObject().getObject()).getVertices();
    Vec2 pos;

    moveToGrid(e);
    endDraggingSelection();
    boolean selected[] = controller.getSelection();
    boolean oldSelection[] = (boolean []) selected.clone();
    if (draggingSelectionBox && !e.isShiftDown() && !e.isControlDown())
      for (i = 0; i < selected.length; i++)
        selected[i] = false;

    // If the user was dragging a selection box, then select or deselect anything 
    // it intersects.
    
    if (selectBounds != null)
      {
        boolean newsel = !e.isControlDown();
        for (i = 0; i < v.length; i++)
          {
            pos = theCamera.getObjectToScreen().timesXY(v[i].r);
            x = (int) pos.x;
            y = (int) pos.y;
            if (selectionRegionContains(new Point(x, y)))
              selected[i] = newsel;
          }
      }
    draggingBox = draggingSelectionBox = false;

    // Send the event to the current tool, if appropriate.

    if (sentClick)
      {
        if (!dragging)
          {
            Point p = e.getPoint();
            e.translatePoint(clickPoint.x-p.x, clickPoint.y-p.y);
          }
        activeTool.mouseReleased(e, this);
      }

    // If the user shift-clicked a selected point and released the mouse without dragging,
    // then deselect the point.

    if (deselect > -1)
      selected[deselect] = false;
    ((CurveEditorWindow) controller).findSelectionDistance();
    for (int k = 0; k < selected.length; k++)
      if (selected[k] != oldSelection[k])
      {
        currentTool.getWindow().setUndoRecord(new UndoRecord(currentTool.getWindow(), false, UndoRecord.SET_MESH_SELECTION, new Object [] {controller, new Integer(controller.getSelectionMode()), oldSelection}));
        break;
      }
    controller.setSelection(selected);
    activeTool.getWindow().updateMenus();
  }
}
