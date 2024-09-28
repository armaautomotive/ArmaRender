
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
                    
                    //System.out.println("  Face: A: " + worldVerts.elementAt(face.v1) + " B: " + worldVerts.elementAt(face.v2) + " C: " + worldVerts.elementAt(face.v3)  );
                    
                    // Calculate the center point and its face normal vector.
                    // ...
                    
                    
                    // Add a line object to the scene to represent the normal vector.
                    
                }
                
            }
        }
        
        
        
        // Add a cube to the scene.
        
        
        
        //
        // Detect if a selected object is colliding with other objects in the scene.
        // Note: This is only one possible implementation of collision detection.
        // For a section, ...
        boolean selectionCollides = false;
        int sel[] = window.getSelectedIndices();
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>(); // TriangleMesh.Edge
        if(sel.length > 0){
            Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>();
            for(int s = 0; s < sel.length; s++){
                ObjectInfo selectedInfo = window.getScene().getObject( sel[s] );
                if(selectedInfo != null){
                    CoordinateSystem c;
                    c = layout.getCoords(selectedInfo);
                    Mat4 mat4 = c.duplicate().fromLocal();
                    
                    Object3D selectedObj = selectedInfo.getObject();
                    TriangleMesh selectedTM = null;
                    if(selectedObj instanceof TriangleMesh){
                        selectedTM = (TriangleMesh)selectedObj;
                    } else if(selectedObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                        selectedTM = selectedObj.convertToTriangleMesh(0.1);
                    }
                    if(selectedTM != null){
                        selectedObjects.addElement(selectedInfo);   // Save for exclusion later.
                        
                        MeshVertex[] verts = selectedTM.getVertices();
                        
                        Vector<Vec3> worldVerts = new Vector<Vec3>();
                        for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                            Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                            mat4.transform(vert);
                            worldVerts.addElement(vert); // add the translated vert to our list.
                            //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                        }
                        
                        TriangleMesh.Edge[] edges = selectedTM.getEdges();
                        for(int e = 0; e < edges.length; e++){
                            TriangleMesh.Edge edge = edges[e];
                            Vec3 a = worldVerts.elementAt( edge.v1 );
                            Vec3 b = worldVerts.elementAt( edge.v2 );
                            EdgePoints ep = new EdgePoints(a, b);
                            selectedEdgePoints.addElement(ep);
                        }
                    }
                }
            } // for each selection
            
            if(selectedEdgePoints.size() > 0){
                System.out.println(" edges " + selectedEdgePoints.size() );
                
                // Scan through objects in the scene to see if there is a collistion.
                
                for(int i = 0; i < sceneObjects.size(); i++){
                    ObjectInfo currInfo = sceneObjects.elementAt(i);
                    if( selectedObjects.contains(currInfo) ){          // Don't compare with self
                        continue;
                    }
                    System.out.println("   Compare with  scene object: " + currInfo.getName() );
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
                            Vec3 faceA = worldVerts.elementAt(face.v1);
                            Vec3 faceB = worldVerts.elementAt(face.v2);
                            Vec3 faceC = worldVerts.elementAt(face.v3);
                            
                            for(int ep = 0; ep < selectedEdgePoints.size(); ep++){
                                EdgePoints edgePoint = selectedEdgePoints.elementAt(ep);
                                if(Intersect2.intersects(edgePoint.a, edgePoint.b, faceA, faceB, faceC)){
                                    selectionCollides = true;
                                    System.out.println("Collision  object: " + currInfo.getName()   );
                                }
                            
                            }
                            
                            // selectionCollides
                            
                        }
                    }
                } // for each object in scene
                
                
                
            }
            
            System.out.println("Selection collides: " + selectionCollides ); // Print result
            
        } else {
            System.out.println("No objects selected to check for collisions.");
        }
    
        
        
    }
    
    
    
    
    /**
     * addLineToScene
     * Description: Add a line to the scene. Good for debugging 3d scene corrdinates.
     */
    public void addLineToScene(Scene scene, Vec3 a, Vec3 b){
        
    }
    
}

