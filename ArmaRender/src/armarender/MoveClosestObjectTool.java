/* Copyright (C) 2020-2022 by Jon Taylor
   Copyright (C) 1999-2009 by Peter Eastman

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

/** MoveObjectTool is an EditingTool used for moving objects in a scene. */

public class MoveClosestObjectTool extends EditingTool
{
  Point clickPoint;
  Vec3 objectPos[];
  Vector<ObjectInfo> toMove;
  ObjectInfo clickedObject;
  boolean dragged, applyToChildren = true;

  public MoveClosestObjectTool(EditingWindow fr)
  {
    super(fr);
    //initButton("move");
      initButton("move_2_48");
      
      //System.out.println(" *** " + button. );
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("moveClosestObjectTool.helpText"));
  }

  public int whichClicks()
  {
    return OBJECT_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return true;
  }

  public String getToolTipText()
  {
    return Translate.text("moveClosestObjectTool.tipText");
  }

  public void mousePressedOnObject(WidgetMouseEvent e, ViewerCanvas view, int obj)
  {
	  //System.out.println("MoveClosestObjectTool.mousePressedOnObject()"); // JDT

    Scene theScene = theWindow.getScene();
    int i, sel[];

    toMove = new Vector<ObjectInfo>();
    clickedObject = theScene.getObject(obj);
    if (applyToChildren)
      sel = theScene.getSelectionWithChildren();
    else
      sel = theScene.getSelection();
    for (i = 0; i < sel.length; i++)
      toMove.addElement(theScene.getObject(sel[i]));
    objectPos = new Vec3 [toMove.size()];
    for (i = 0; i < objectPos.length; i++)
      {
        ObjectInfo info = toMove.elementAt(i);
        objectPos[i] = info.getCoords().getOrigin();
      }
    clickPoint = e.getPoint();
    dragged = false;
  }

/**
* mouseDragged
*
*
*/
  public void mouseDragged(final WidgetMouseEvent e, final ViewerCanvas view)
  {
	  //System.out.println("MoveObjectTool.mouseDragged()"); // JDT
	LayoutModeling layout = new LayoutModeling();
	Scene theScene = theWindow.getScene();
	//layout.setBaseDir(theScene.getDirectory() + "\\" + theScene.getName() + "_layout_data" );


    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    CoordinateSystem c;
    int i, dx, dy;
    Vec3 v;

    if (!dragged)
      {
        UndoRecord undo;
        theWindow.setUndoRecord(undo = new UndoRecord(theWindow, false));
        for (i = 0; i < toMove.size(); i++)
          {
            ObjectInfo info = toMove.elementAt(i);
            c = info.getCoords();
            undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {c, c.duplicate()});
          }
        dragged = true;
      }
    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    if (e.isShiftDown() && !e.isControlDown())
      {
        if (Math.abs(dx) > Math.abs(dy))
          dy = 0;
        else
          dx = 0;
      }
    if (e.isControlDown())
      v = cam.getCameraCoordinates().getZDirection().times(-dy*0.01);
    else
      v = cam.findDragVector(clickedObject.getCoords().getOrigin(), dx, dy);

      //System.out.println(" To move size: " + toMove.size());
    for (i = 0; i < toMove.size(); i++)
      {
        ObjectInfo info = toMove.elementAt(i);

        // If LayoutView == cutting use seperate coordinates.
        c = info.getCoords();
        // JDT
        if(info.getLayoutView() == false){
		c = layout.getCoords(info); // Read cutting coord from file
	}
	if(info.getTubeLayoutView() == true){
		c = layout.getCoords(info); // Read cutting coord from file 	
	}

        c.setOrigin(objectPos[i].plus(v));

		//System.out.println("   -- " + info.name  );

		// JDT
		if(info.getLayoutView() == false){
			layout.saveLayout(info, c);
			info.resetLayoutCoords(c);
		}
		if(info.getTubeLayoutView() == true){
			layout.saveLayout(info, c);
                        info.resetLayoutCoords(c);	
		}
      }
    theWindow.setModified();
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("moveClosestObjectTool.dragText",
      Math.round(v.x*1e5)/1e5+", "+Math.round(v.y*1e5)/1e5+", "+Math.round(v.z*1e5)/1e5));

      //System.out.println(" --- " + v.x + " " + v.y + " " + v.z);
      
      //
      // if a mirror plane object has changed, update any mirror mesh objects to reflect changes.
      //
      if(clickedObject != null){
          if(clickedObject.getObject() instanceof MirrorPlaneObject ||
             clickedObject.getObject() instanceof SplineMesh){
              MirrorMesh mirror = new MirrorMesh();
              mirror.update(clickedObject);
          }
          
          if(
             //clickedObject.getObject() instanceof MirrorPlaneObject ||
             clickedObject.getObject() instanceof Curve){
              
              MirrorCurve mirror = new MirrorCurve();
              mirror.update(clickedObject);
          }
      }
      
      //
      // Update boolean geometry
      //
      ObjectInfo parent = clickedObject.getParent();
      if(parent != null){
          Object3D parentObj = parent.getObject();
          if(parentObj instanceof Folder){
              Folder f = (Folder)parentObj;
              if(f.getAction() == Folder.UNION || f.getAction() == Folder.SUBTRACT || f.getAction() == Folder.INTERSECTION){
                  // TODO only regenerate this object tree
                  if(theWindow instanceof LayoutWindow){
                      ((LayoutWindow)theWindow).booleanSceneCommand();
                  }
              }
          }
      }
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
	//System.out.println("MoveObjectTool.mouseReleased()"); // JDT
    theWindow.getScene().applyTracksAfterModification(toMove);
    theWindow.setHelpText(Translate.text("moveClosestObjectTool.helpText"));
    toMove = null;
    objectPos = null;
    theWindow.updateImage();
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

    // Pressing an arrow key is equivalent to dragging the first selected object by one pixel.

      dx = 0;
      dy = 0;
      
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
    else {
        
      return;
    }
    e.consume();
    if (applyToChildren)
      sel = theScene.getSelectionWithChildren();
    else
      sel = theScene.getSelection();
      if (sel.length == 0){
          //return;  // No objects are selected.
      }
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
    CoordinateSystem cameraCoords = cam.getCameraCoordinates();
    if (e.isControlDown()) {
      v = cameraCoords.getZDirection().times(-dy*0.01);
    } else if( sel.length > 0 )
    {
      Vec3 origin = theScene.getObject(sel[0]).getCoords().getOrigin();
      if (Math.abs(origin.minus(cameraCoords.getOrigin()).dot(cameraCoords.getZDirection())) < 1e-10)
      {
        // The object being moved is in the plane of the camera, so use a slightly
        // different point to avoid dividing by zero.

        origin = origin.plus(cameraCoords.getZDirection().times(cam.getClipDistance()));
      }
      v = cam.findDragVector(origin, dx, dy);
    }
    theWindow.setUndoRecord(undo = new UndoRecord(theWindow, false));
    toMove = new Vector<ObjectInfo>();
    for (i = 0; i < sel.length; i++){
      toMove.addElement(theScene.getObject(sel[i]));
    }
      
      
      //System.out.println(" to move " + toMove.size());
    for (i = 0; i < toMove.size(); i++)
    {
      ObjectInfo info = toMove.elementAt(i);
      c = info.getCoords();
      if(info.getLayoutView() == false){ // JDT
        c = layout.getCoords(info); // Read cutting coord from file
      }
      if(info.getTubeLayoutView() == true){
        c = layout.getCoords(info); // Read cutting coord from file
      }

      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {c, c.duplicate()});

      c.setOrigin(c.getOrigin().plus(v));
        
        
        // If object (info) has points connected with PointJoinObject then update the object they connect with.
        int count = theScene.getNumObjects();
        HashMap<Integer, ObjectInfo> attachedObjectInfos = new HashMap<Integer, ObjectInfo>(); // Track attached objects.
        for(int ii = 0; ii < count; ii++){                         // Each object in the scene
            ObjectInfo obj = theScene.getObject(ii);
            if(obj.getObject() instanceof PointJoinObject){
                PointJoinObject pointJoin = (PointJoinObject)obj.getObject();
                // Find object point that is connected.
                if(pointJoin.objectA == info.getId()   ){
                    // Get Object B and move it too
                    int objId = pointJoin.objectB;
                    ObjectInfo joinedObject = theScene.getObjectById(objId);
                    if(joinedObject != null && (joinedObject.getObject() instanceof Curve ||
                                                joinedObject.getObject() instanceof SplineMesh) &&
                       !attachedObjectInfos.containsKey(objId)){
                        // update B
                        CoordinateSystem cc;
                        cc = joinedObject.getCoords();
                        cc.setOrigin(cc.getOrigin().plus(v));
                        //System.out.println(" B ");
                        attachedObjectInfos.put(objId, joinedObject);
                    }
                } else if(pointJoin.objectB == info.getId()){
                    // Get Object A
                    int objId = pointJoin.objectA;
                    ObjectInfo joinedObject = theScene.getObjectById(objId);
                    if(joinedObject != null && (joinedObject.getObject() instanceof Curve ||
                                                joinedObject.getObject() instanceof SplineMesh) &&
                       !attachedObjectInfos.containsKey(objId)){
                        // update A
                        CoordinateSystem cc;
                        cc = joinedObject.getCoords();
                        cc.setOrigin(cc.getOrigin().plus(v));
                        //System.out.println(" A ");
                        attachedObjectInfos.put(objId, joinedObject);
                    }
                }
                // Update pointJoin location.
                pointJoin.setVertexPositions(); // This isnt working. ***
                
            }
        }
        

      // JDT
      if(info.getLayoutView() == false){
        layout.saveLayout(info, c);
        info.resetLayoutCoords(c);
      }
      if(info.getTubeLayoutView() == true){
        layout.saveLayout(info, c);
        info.resetLayoutCoords(c);
      }
      // getWindow().getScene().objectModified(objects.get(i).getObject());

    }
      
    // If an object vertex is selected, apply move transform.
    PointJoinObject pointJoin = theScene.getCreatePointJoinObject(); // This is used as the point selection variable.
      
    // If one vertex point is selected it will be stored in the objectA structure.
    int selectedObject = pointJoin.objectA;
    int selectedObjectPoint = pointJoin.objectAPoint;
      
    if(selectedObject > 0){
        //System.out.println(" objectA " + pointJoin.objectA );
        
        int count = theScene.getNumObjects();
        for(i = 0; i < count; i++){                         // Each object in the scene
            ObjectInfo obj = theScene.getObject(i);
            if(obj.getId() == pointJoin.objectA){           // object in scene matches the selection.
                Mesh o3d = (Mesh)obj.getObject();
                MeshVertex[] verts = o3d.getVertices();
                if(pointJoin.objectAPoint < verts.length){  // if selected vertex is in bounds.
                    
                    if(v == null){
                        Vec3 origin = obj.getCoords().getOrigin();
                        if (Math.abs(origin.minus(cameraCoords.getOrigin()).dot(cameraCoords.getZDirection())) < 1e-10)
                        {
                            // The object being moved is in the plane of the camera, so use a slightly
                            // different point to avoid dividing by zero.
                            
                            origin = origin.plus(cameraCoords.getZDirection().times(cam.getClipDistance()));
                        }
                        v = cam.findDragVector(origin, dx, dy);
                    }
                    
                    Vec3 vr[] = new Vec3[verts.length];
                    for(int vrx = 0; vrx < verts.length; vrx++){
                        vr[vrx] = verts[vrx].r;
                    }
                    
                    MeshVertex vm = verts[pointJoin.objectAPoint];
                    Vec3 vec = vm.r;
                    //Vec3 vx[] = new Vec3[1];
                    
                    vr[pointJoin.objectAPoint] = new Vec3(vec.x + v.x, vec.y + v.y, vec.z + v.z);
                    
                    // Update object
                    o3d.setVertexPositions(vr); // clears cache mesh
                    obj.setObject((Object3D)o3d);
                    obj.clearCachedMeshes();
                    
                    //System.out.println("move selected point in object.");
                    
                    // Update any other object verticies connected to this object using a PointJoinObject.
                    
                    //System.out.println("count: " + count);
                    for(int ci = 0; ci < count; ci++){ // && ci != i
                        if(ci != i){                                // dont ...
                            ObjectInfo c_obj = theScene.getObject(ci);
                            //System.out.println("obj " + c_obj.getObject().getClass().getName() );
                            if(c_obj.getObject() instanceof PointJoinObject){
                                //System.out.println(" PJ " + c_obj.getId() );
                                PointJoinObject join = (PointJoinObject)c_obj.getObject();
                                
                                if(join.objectA == selectedObject && pointJoin.objectAPoint == join.objectAPoint){
                                    System.out.println(" FOUND POINT JOIN A");
                                    
                                    ObjectInfo joinedObject = theScene.getObjectById(join.objectB);
                                    if(joinedObject != null && (
                                                            joinedObject.getObject() instanceof Curve ||
                                                                joinedObject.getObject() instanceof SplineMesh
                                                                )
                                       ){
                                        
                                        Mesh joinedO3d = (Mesh)joinedObject.getObject();
                                        MeshVertex[] joinedVerts = joinedO3d.getVertices();
                                        if(join.objectBPoint < joinedVerts.length){
                                            System.out.println(" FOUND joined object "+ join.objectB +
                                                               " " + joinedObject.getName() +
                                                               " and point " + join.objectBPoint );
                                            
                                            
                                            Vec3 jvr[] = new Vec3[joinedVerts.length];
                                            for(int vrx = 0; vrx < joinedVerts.length; vrx++){
                                                jvr[vrx] = joinedVerts[vrx].r;
                                            }
                                            
                                            MeshVertex jvm = joinedVerts[join.objectBPoint];
                                            vec = jvm.r;
                                            //Vec3 vx[] = new Vec3[1];
                                            
                                            jvr[join.objectBPoint] = new Vec3(vec.x + v.x, vec.y + v.y, vec.z + v.z);
                                            
                                            // Update joined object
                                            joinedO3d.setVertexPositions(jvr); // clears cache mesh
                                            joinedObject.setObject((Object3D)joinedO3d);
                                            joinedObject.clearCachedMeshes();
                                            
                                            // Tell the point join object to update its verticies with the new modified objects.
                                            join.setVertexPositions();
                                            
                                            // TODO: now that point can have other joins to more objects...
                                        }
                                    }
                                }
                                if(join.objectB == selectedObject && pointJoin.objectAPoint == join.objectBPoint){
                                    System.out.println(" FOUND POINT JOIN B");
                                    // Find object A and it's vertex to move along with this point.
                                    
                                    ObjectInfo joinedObject = theScene.getObjectById(join.objectA);
                                    if(joinedObject != null &&
                                        (joinedObject.getObject() instanceof Curve ||
                                         joinedObject.getObject() instanceof SplineMesh)){
                                        
                                        Mesh joinedO3d = (Mesh)joinedObject.getObject();
                                        MeshVertex[] joinedVerts = joinedO3d.getVertices();
                                        if(join.objectAPoint < joinedVerts.length){
                                            System.out.println(" FOUND joined object "+ join.objectA +
                                                               " " + joinedObject.getName() +
                                                               " and point " + join.objectAPoint );
                                            
                                            Vec3 jvr[] = new Vec3[joinedVerts.length];
                                            for(int vrx = 0; vrx < joinedVerts.length; vrx++){
                                                jvr[vrx] = joinedVerts[vrx].r;
                                            }
                                            
                                            MeshVertex jvm = joinedVerts[join.objectAPoint];
                                            vec = jvm.r;
                                            //Vec3 vx[] = new Vec3[1];
                                            
                                            jvr[join.objectAPoint] = new Vec3(vec.x + v.x, vec.y + v.y, vec.z + v.z);
                                            
                                            // Update joined object
                                            joinedO3d.setVertexPositions(jvr); // clears cache mesh
                                            joinedObject.setObject((Object3D)joinedO3d);
                                            joinedObject.clearCachedMeshes();
                                            
                                            // Tell the point join object to update its verticies with the new modified objects.
                                            join.setVertexPositions();
                                            
                                            // TODO: now that point can have other joins to more objects...
                                        }
                                    }
                                }
                            }
                        }
                    }
                  
                    // LayoutWindow.updateImage();
                    //theWindow.updateImage();
                    
                }
            }
          }
        }
      
      // TODO: If vertex point selected, break and don't move any selected objects.
      // ***
      
      // Object B *** DEPRICATE
      //if(pointJoin.objectB > 0){
      //    System.out.println(" objectB " + pointJoin.objectB );
          
      //}
      
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
    BCheckBox childrenBox = new BCheckBox(Translate.text("applyToUnselectedChildren"), applyToChildren);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("moveToolTitle"),
		new Widget [] {childrenBox}, new String [] {null});
    if (!dlg.clickedOk())
      return;
    applyToChildren = childrenBox.getState();
  }
}
