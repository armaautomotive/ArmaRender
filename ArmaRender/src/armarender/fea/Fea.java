/* Copyright (C) 2020 by Jon Taylor

 This program is free software; you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 
 */

package armarender.fea;

import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;
import javax.swing.JComboBox;
import armarender.object.*;
import armarender.math.*;
import armarender.*;

public class Fea
{
    Vector<ObjectInfo> objects;
    ObjectInfo obj;
    
    Vector<FeaPoint> points;
    HashMap<Integer, FeaPoint> pointMap;
    HashMap<Integer, FeaConnection> connectionMap;
    
    public Fea()
    {
        
    }
    
    /**
    * impact
    *  Description: Create data structures used to manage calculations in impact deformation.
    */
    public void initalize(ObjectInfo meshObj){
        System.out.println("Fea initalize. ");
        
        Object3D triangleMesh = null;
        if(meshObj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
            triangleMesh = meshObj.getObject().convertToTriangleMesh(0.05);
            if(triangleMesh instanceof TriangleMesh){
                TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                MeshVertex[] edgeVerts = ((TriangleMesh)triangleMesh).getVertices();
                
                //
                //Object meshObject = (Object)meshObj.getObject();
                //Vec3 [] verts = mesh.getVertexPositions();
                
                // Get list of all verts connected to the currVec by edges.
                for(int v = 0; v < edges.length; v++){
                    TriangleMesh.Edge edge = edges[v];
                    
                    // edge.v1 edge.v2
                    
                    //connectionMap.put( , );
                }
                
                //int index = 0;
                //for (Vec3 vert : edgeVerts){ // iterate object points
                for(int i = 0; i < edgeVerts.length; i++){
                    Vec3 vertex = edgeVerts[i].r;
                    
                    FeaPoint feaPoint = new FeaPoint();
                //    feaPoint.pointId = index;
                //    feaPoint.preLocation = new Vec3(vertex.x, vertex.y, vertex.z);
                //    feaPoint.postLocation = new Vec3();
                //    feaPoint.inputForces
                    
                    
                //    feaPoint.connections
                    
                //    pointMap.put(i, feaPoint);
                    
                    
                    
                    //index++;
                }
            }
        }
            
            /*
            for (ObjectInfo info : objects){
                Object co = (Object)info.getObject();
                if((co instanceof ForceObject) == true){
             
                    System.out.println(" Force Vector ");
                    Mesh mesh = (Mesh) info.getObject(); // Object3D
                    Vec3 [] verts = mesh.getVertexPositions();
                    for (Vec3 vert : verts){
                        System.out.println("   - " + vert.x + " " + vert.y + " " + vert.z);
                        
                        
                        //points
                        
                    }
                }
            }
             */
    }
    
    public void orderPoints(){
        
    }
    
    /**
     * impact
     *  Description:
     */
    public void impact(ObjectInfo meshObj){
        LayoutModeling layout = new LayoutModeling();
        
        Object3D triangleMesh = null; // && triangleMesh != null
        if(meshObj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
            
            triangleMesh = meshObj.getObject().convertToTriangleMesh(0.05);
            
        
        }
    }
    
    
    public void precalculatePropogation(){
        
    }
    
    public void scalePropogationByEdgeChange(){
        
    }
    
    public void Propogate(){
        
    }
    
    public void setObjects(Vector<ObjectInfo> objects){
        this.objects = objects;
    }
    public void setObject(ObjectInfo obj){
        this.obj = obj;
    }
}
