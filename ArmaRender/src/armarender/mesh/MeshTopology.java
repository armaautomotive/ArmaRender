/*
   Copyright (C) 2025 Jon Taylor
 */

package armarender.mesh;

import armarender.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import armarender.ui.*;
import armarender.object.*;
import buoy.widget.*;
import java.io.*;
import java.lang.ref.*;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

public class MeshTopology {
    
    
    public MeshTopology(){
        
    }
    
    
    /**
     * isWatertight
     * Description: determine if a mesh object is air tight.
     * @param: ObjectInfo info - object with type of mesh or can convert to mesh.
     */
    public static boolean isWatertight(ObjectInfo info){
        boolean tight = true;
        
        TriangleMesh mesh = null;
        
        if(info.getObject() instanceof TriangleMesh){
            mesh = (TriangleMesh)info.getObject();
        } else if(((Object3D)info.getObject()).canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
            mesh = ((Object3D)info.getObject()).convertToTriangleMesh(0.05);
        }
        
        if(mesh != null){
            MeshVertex[] verts = mesh.getVertices();
            TriangleMesh.Face[] faces = mesh.getFaces();
            TriangleMesh.Edge[] edges = mesh.getEdges();
            
            for(int i = 0; i < edges.length; i++){
                
                int e1 = edges[i].v1;
                int e2 = edges[i].v2;
                
                boolean edgeIsValid = false;
                int connectedFaceCount = 0;
                
                for(int j = 0; j < faces.length; j++){
                    int matchCount = 0;
                    
                    int f1 = faces[j].v1;
                    int f2 = faces[j].v2;
                    int f3 = faces[j].v3;
                    
                    if(e1 == f1 || e1 == f2 || e1 == f3){
                        matchCount++;
                    }
                    if(e2 == f1 || e2 == f2 || e2 == f3){
                        matchCount++;
                    }
                    
                    if(matchCount > 1){ // A connected face must have two shared vertecies with the edge.
                        edgeIsValid = true;
                        connectedFaceCount++;
                    }
                }
                
                if(connectedFaceCount < 2){ // All edges must have two connected faces or it's not water tight.
                    tight = false;
                }
            }
        }
        
        return tight;
    }

}
