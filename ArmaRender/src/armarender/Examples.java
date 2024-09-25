
package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;


public class Examples {
    
    public Examples(){
        
    }
    
    /**
     * demo
     * Description: Example of common tasks.
     */
    public void demo(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        
        
        // list objects in the scene.
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo currInfo = sceneObjects.elementAt(i);
            System.out.println(" iterate scene objects: " + currInfo.getName() );
            Object3D currObj = currInfo.getObject();
            
            //
            // Is the object a TriangleMesh?
            //
            TriangleMesh triangleMesh = null;
            if(currObj instanceof TriangleMesh){
                triangleMesh = (TriangleMesh)currObj;
            } else if(currObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                triangleMesh = currObj.convertToTriangleMesh(0.1);
            }
            if(triangleMesh != null){
                CoordinateSystem c;
                c = layout.getCoords(currInfo);
                
                // Convert object coordinates to world (absolute) coordinates.
                // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                Mat4 mat4 = c.duplicate().fromLocal();
                
                MeshVertex[] verts = triangleMesh.getVertices();
                Vector<Vec3> worldVerts = new Vector<Vec3>();
                for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                    Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                    mat4.transform(vert);
                    worldVerts.addElement(vert); // add the translated vert to our list.
                    //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                }
                
                TriangleMesh.Edge[] edges = ((TriangleMesh)triangleMesh).getEdges();
                TriangleMesh.Face[] faces = triangleMesh.getFaces();
                
                for(int f = 0; f < faces.length; f++ ){
                    TriangleMesh.Face face = faces[f];
                    
                    System.out.println("  Face: A: " + worldVerts.elementAt(face.v1) + " B: " + worldVerts.elementAt(face.v2) + " C: " + worldVerts.elementAt(face.v3)  );
                    
                    // Calculate the center point and its face normal vector.
                    // ...
                    
                    
                    // Add a line object to the scene to represent the normal vector.
                    
                    
                    
                }
                
                
            }
        }
        
        
        
        // Add a cube to the scene.
        
        
        
        
        
    }
    
}

