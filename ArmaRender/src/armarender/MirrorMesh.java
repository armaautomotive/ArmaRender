/* Copyright (C) 2022-2023 by Jon Taylor

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
import armarender.texture.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.Dimension;
import java.util.Vector;

/** The SplineMeshEditorWindow class represents the window for editing SplineMesh objects. */

public class MirrorMesh
{
    static LayoutWindow window = null;
    
    public MirrorMesh(){
    }
    
    public void setLayoutWindow(LayoutWindow w){
        this.window = w;
    }
    
    /**
     * update
     *
     * Description: update a given mesh object or mech child MirrorPlaneObject.
     *
     * @param ObjectInfo - the object being updated. Sometimes is the mesh and sometimes a mirrorplaneObject.
     */
    public void update(ObjectInfo info){
        //System.out.println("mirror object name: " + info.getName() + " id: " + info.getId() +  " class: " + info.getObject().getClass().getName() );
        
        if(info.getObject() instanceof MirrorPlaneObject){
            ObjectInfo meshOI = info.getParent();
            if(meshOI != null){
                //System.out.println(" mirror -> MESH " + meshOI.getName());
                updateSplineMesh(info, meshOI, meshOI);
            }
        }
        
        if(info.getObject() instanceof armarender.object.SplineMesh){
            //System.out.println(" MESH " + info.getName());
            
            // Find Mirror plane object
            Vec3 mirrorLocation = null;
            
            // Note: ObjectInfo may not be scene OI and not contain children.
            ObjectInfo[] children = info.getChildren();
            ObjectInfo sceneInfo = null;
            
            if(window.getScene() != null && children.length == 0){ // theScene
                // Find the real object.
                Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
                for(int i = 0; i < sceneObjects.size(); i++){
                    ObjectInfo currOI = sceneObjects.elementAt(i);
                    //System.out.println(" " + currOI.getId() + " -> " + info.getId());
                    if(currOI.getId() == info.getId()){
                        //info = currOI;
                        sceneInfo = currOI;
                        children = currOI.getChildren();
                    }
                }
            }
            
            for(int i = 0; i < children.length; i++){
                ObjectInfo childOI = (ObjectInfo)children[i];
                if(childOI.getObject() instanceof MirrorPlaneObject){
                    //System.out.println(" MESH found mirror plane. " + info.getName());
                    
                    Mesh mesh = (Mesh) childOI.getObject();
                    Vec3 vert[] = mesh.getVertexPositions();
                    //System.out.println(" mirror length " + vert.length );
                    
                    if(sceneInfo != null){
                        updateSplineMesh(childOI, info, sceneInfo);
                    } else {
                        updateSplineMesh(childOI, info, info);
                    }
                }
            }
        }
        
        if(info.getObject() instanceof Mesh){
            // Not implemented.
        }
    }
    
    
    /**
     * updateSplineMesh
     *
     *  Description: Update a mirror mesh to conform to a reference based on a mirror plane.
     * Note: If a scene has two splinemesh objects with the same name, this function will fail.
     *
     * @param: Mirror -
     * @param: Mesh - modified points
     * @param: Mesh - scene object with world Coordinatess if different.
     */
    public void updateSplineMesh(ObjectInfo mirrorPlaneOI, ObjectInfo meshOI, ObjectInfo sceneMeshOI){
        if(meshOI.getObject() instanceof armarender.object.SplineMesh &&
           mirrorPlaneOI.getObject() instanceof MirrorPlaneObject){
            //System.out.println("updateSplineMesh " + meshOI.getName() + " - " +  sceneMeshOI.getName());
            Vec3 mirror = getMirrorPlaneX(mirrorPlaneOI);
            Mesh mesh = (Mesh) meshOI.getObject();
            Vec3 vert[] = mesh.getVertexPositions();
            CoordinateSystem c;
            //c = meshOI.getCoords().duplicate();
            c = sceneMeshOI.getCoords().duplicate();
            Mat4 aMat4 = c.duplicate().fromLocal();
            for(int i = 0; i < vert.length; i++){
                Vec3 currVec = new Vec3((Vec3)vert[i]);
                aMat4.transform(currVec);
                double xDistance = currVec.x - mirror.x;
                currVec.x = mirror.x - (xDistance) ;
                currVec.z = currVec.z;
                vert[i] = currVec;
            }
            SplineMesh newMesh = (SplineMesh)meshOI.getObject().duplicate();
            newMesh.setVertexPositions(vert);
            CoordinateSystem newMirrorCS = new CoordinateSystem();
            ObjectInfo newMeshOI = new ObjectInfo(newMesh, newMirrorCS, "MIRROR " + meshOI.getName());
            newMeshOI.setReplicateId(sceneMeshOI.getId());
            
            // Add new mesh
            if(window.getScene() != null){
                // Remove existing
                Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
                boolean exists = false;
                for(int i = 0; i < sceneObjects.size(); i++){
                    ObjectInfo oi = sceneObjects.elementAt(i);
                    if(
                       oi.getObject() instanceof Mesh                       // Is mesh
                       &&
                       //oi.getName().equals(newMeshOI.getName())           // OLD - match by name
                       oi.getReplicateId() == newMeshOI.getReplicateId()    // match by ID
                       &&
                       oi.getId() != newMeshOI.getId()                      // Don't count self.
                       ){ //
                        ((SplineMesh)oi.getObject()).setVertexPositions(((SplineMesh) newMeshOI.getObject()).getVertexPositions());
                        oi.setCoords(newMirrorCS); // reset coordinate system.
                        oi.clearCachedMeshes();
                        
                        exists = true;
                        i = sceneObjects.size(); // break
                    }
                }
                
                if(exists == false){
                    window.getScene().addObject(newMeshOI, null); // add replicated object to scene.
                    
                    // Add replicated mesh as child of mirror plane.
                    newMeshOI.setParent(mirrorPlaneOI); // Add perferation object to selection.
                    mirrorPlaneOI.addChild(newMeshOI, mirrorPlaneOI.getChildren().length); // info.getChildren().length+1
                }
                
                //LayoutWindow window = (LayoutWindow)theScene.getLayoutWindow();
                window.rebuildItemList(); // redraw item list.
                window.updateImage();
                window.updateMenus();
            }
        }
    }
 
    
    /**
     * getMirrorPlaneX
     *
     * Description: Calculate average of points in mirror plane.
     */
    public Vec3 getMirrorPlaneX(ObjectInfo info){
        Vec3 result = new Vec3();
        Mesh mesh = (Mesh) info.getObject();
        Vec3 vert[] = mesh.getVertexPositions();
        CoordinateSystem c;
        c = info.getCoords().duplicate();
        Mat4 aMat4 = c.duplicate().fromLocal();
        for(int j = 0; j < vert.length; j++){
            Vec3 currPoint = (Vec3)vert[j];
            aMat4.transform(currPoint);
            result.add(currPoint);
        }
        result.x /= vert.length;
        result.y /= vert.length;
        result.z /= vert.length;
        return result;
    }
    
    
    /**
     * setVisibility
     *
     * Description: called when a mirrorplaneobject visibility is changed and toggles the mirror mesh visibility.
     *
     * Note: This may be no longer nessisary as replicated objects are now children of mirror planes.
     *
     * @param info, the object having its visibility being toggled.
     */
    public void updateVisibility(ObjectInfo info){
        //System.out.println(" MirrorMesh.updateVisibility() " );
        boolean cVisible = !info.isChildrenHiddenWhenHidden();
        boolean v = info.isVisible();
        if(info.getObject() instanceof MirrorPlaneObject){
            //System.out.println(" mirrorplane " + cVisible + " v: " + v );
            ObjectInfo parent = info.getParent();
            if(parent != null && parent.getObject() instanceof SplineMesh){
                // Find mirror object.
                String mirrorObjectName = "MIRROR " + parent.getName();
                Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
                for(int i = 0; i < sceneObjects.size(); i++){
                    ObjectInfo currOI = sceneObjects.elementAt(i);
                    //System.out.println(" " + currOI.getId() + " -> " + info.getId());
                    if(currOI.getName().equals(mirrorObjectName)){
                        if(cVisible && v){
                            currOI.setVisible(true);
                        } else {
                            currOI.setVisible(false);
                        }
                    }
                }
            }
        }
    }
}
