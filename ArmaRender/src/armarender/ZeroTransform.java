/* Copyright (C) 2022 by Jon Taylor

   
 */

package armarender;

import armarender.*;
import armarender.animation.*;
import armarender.image.*;
import armarender.math.*;
import armarender.object.*;
import armarender.texture.*;
import armarender.ui.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;


public class ZeroTransform
{
    public ZeroTransform(){
        
    }
    
    /**
     * zero
     *
     * Description: Translate selected object points such that they remain stationary relative to the scene but the Object transform is zero.
     * 3D scanned objects will have a random rotation, Its convienient to rotate the object with object transform to the desired orientation and zero
     * so that mirroring or adjusting is simple on single axies.
     */
    public void zero(Scene theScene){
        LayoutModeling layout = new LayoutModeling();
        int [] selection = theScene.getSelection();
        for(int i = 0; i < selection.length; i++){
            ObjectInfo obj = theScene.getObject(selection[i]);
            Object co = (Object)obj.getObject();
            CoordinateSystem c;
            c = layout.getCoords(obj);
            if( obj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT){ // (co instanceof Mesh) == true &&
                
                //
                System.out.println(" ZERO Mesh " + obj.getName() );
                
                TriangleMesh triangleMesh = obj.getObject().convertToTriangleMesh(0.05);
                //triangleMesh = ((TriangleMesh)obj.getObject()).duplicate()  .convertToTriangleMesh(0.05);
                MeshVertex[] verts = triangleMesh.getVertices();
                Vec3[] vecs = triangleMesh.getVertexPositions();
                
                for(int e = 0; e < verts.length; e++){
                    Vec3 v = vecs[e];// ((MeshVertex)verts[e]).r;
                   
                    
                    System.out.println("    v " + v);
                    Mat4 mat4 = c.duplicate().fromLocal();
                    
                    mat4.transform(v);
                    
                 
                    verts[e].r = v;
                }
                triangleMesh.setVertexPositions(vecs);
                
                CoordinateSystem c2 = new CoordinateSystem();
                //obj.setCoords(c2);
                //c.
                
                //TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                //for(int e = 0; e < edges.length; e++){
                //    TriangleMesh.Edge edge = edges[e];
                //    Vec3 vec1 = new Vec3(verts[edge.v1].r); // duplicate
                //    Vec3 vec2 = new Vec3(verts[edge.v2].r);
                //    System.out.println(" x: " + vec1.x + " y: "+ vec1.y + " z: " + vec1.z  + " ->  " + " x: " + vec2.x + " y: "+ vec2.y + " z: " + vec2.z  );
                //}
                
                //TriangleMesh.Face[] faces = triangleMesh.getFaces();
                
                
                
                
            }
            
        }
        //theScene.
    }
    
}
