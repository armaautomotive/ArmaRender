
package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;


public class Examples {
    
    public Examples(){
        
    }
    
    // Container for information representing the router machine geometry.
    public class RouterElementContainer {
        public ObjectInfo element;
        public double location; // Location along axis of B/C cut tip to pivot.
        public double size;
        public boolean affixedToB = true;
        public boolean affixedToC = true;
        public boolean enabled = true;
        public RouterElementContainer(ObjectInfo info, double location, double size){
            this.element = info;
            this.location = location;
            this.size = size;
        }
        public RouterElementContainer(ObjectInfo info, double location, double size, boolean enabled){
            this.element = info;
            this.location = location;
            this.size = size;
            this.enabled = enabled;
        }
    }
    
    public class SurfacePointContainer {
        public Vec3 point;
        public Vec3 normal;
        public int passNumber = 0;
        SurfacePointContainer(Vec3 point, int pasNumber){
            this.point = point;
            this.passNumber = passNumber;
        }
        SurfacePointContainer(Vec3 point, Vec3 normal, int pasNumber){
            this.point = point;
            this.normal = normal;
            this.passNumber = passNumber;
        }
    }
    
    /**
     * This example will move a selected object along a orientation vector by the length of detected collision.
     * This should result in the object resting outside the collision.
     */
    public void intersectDistanceDemo(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        
        Vec3 orientation = new Vec3(-.7, .7, 0);
        orientation.normalize();
        //addLineToScene(window, new Vec3(0,0,0), orientation, "Orientation", true ); // CORRECT
        
        boolean selectionCollides = false;
        int sel[] = window.getSelectedIndices();
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>(); // TriangleMesh.Edge
        if(sel.length > 0){
            Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>();
            Vector<Vec3> selectionWorldVerts = new Vector<Vec3>();
            
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
                        
                        for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                            Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                            mat4.transform(vert);
                            selectionWorldVerts.addElement(vert); // add the translated vert to our list.
                            //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
                        }
                        TriangleMesh.Edge[] edges = selectedTM.getEdges();
                        for(int e = 0; e < edges.length; e++){
                            TriangleMesh.Edge edge = edges[e];
                            Vec3 a = selectionWorldVerts.elementAt( edge.v1 );
                            Vec3 b = selectionWorldVerts.elementAt( edge.v2 );
                            EdgePoints ep = new EdgePoints(a, b);
                            selectedEdgePoints.addElement(ep);
                        }
                    }
                }
            } // for each selection
            if(selectedEdgePoints.size() > 0){
                //System.out.println(" edges " + selectedEdgePoints.size() );
                // Scan through objects in the scene to see if there is a collistion.
                for(int i = 0; i < sceneObjects.size(); i++){
                    ObjectInfo currInfo = sceneObjects.elementAt(i);
                    if( selectedObjects.contains(currInfo) ){          // Don't compare with self
                        continue;
                    }
                    //System.out.println("   Compare with  scene object: " + currInfo.getName() );
                    Object3D currObj = currInfo.getObject();
                    
                    double longestCollisionDist = 0;
                    
                    //
                    // Is the object a TriangleMesh or can it be converted?
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
                                
                                Vec3 samplePointCollision = Intersect2.getIntersection(edgePoint.a, edgePoint.b, faceA, faceB, faceC );
                                if(samplePointCollision != null && samplePointCollision.x != 0  ){
                                    //System.out.println("Collsion ");
                                    
                                    //addLineToScene(window, samplePointCollision,  samplePointCollision.minus( new Vec3(0,1,0) ), "Collision", true ); // OK
                                    //addLineToScene(window, samplePointCollision, samplePointCollision.plus(new Vec3(0,2,0)), "Collision", true );
                                    
                                    double collisionDistance = getCollisionDistance( samplePointCollision, edgePoint.a, edgePoint.b, orientation );
                                    //double collisionDistance = Vec3.distanceOnAxis(edgePoint.a, edgePoint.b, orientation);
                                    
                                    if(collisionDistance > longestCollisionDist){
                                        longestCollisionDist = collisionDistance;
                                    }
                                    
                                    
                                    //System.out.println("collisionDistance " + collisionDistance);
                                    
                                    Vec3 showPointer = new Vec3(orientation);
                                    showPointer.normalize();
                                    showPointer = showPointer.times(collisionDistance);
                                    
                                    //System.out.println(" test " +  showPointer.distance(new Vec3())  );
                                    //addLineToScene(window, collidedObjectPoint, collidedObjectPoint.plus( showPointer )  , "-TEST: " + " " + collisionDistance, true );
                                }
                                //if(Intersect2.intersects(edgePoint.a, edgePoint.b, faceA, faceB, faceC)){
                                //    selectionCollides = true;
                                //    System.out.println("Collision  object: " + currInfo.getName());
                                //}
                            }
                        }
                    }
                    if(longestCollisionDist > 0){
                        Vec3 showPointer = new Vec3(orientation);
                        showPointer.normalize();
                        showPointer = showPointer.times(longestCollisionDist);
                        
                        for(int s = 0; s < selectedObjects.size(); s++){
                            ObjectInfo sInfo = selectedObjects.elementAt(s);

                            CoordinateSystem reCS = sInfo.getModelingCoords();
                            Vec3 currOrigin = reCS.getOrigin();
                            
                            reCS.setOrigin( currOrigin.plus(  showPointer  ) );  // showPointer
                            sInfo.clearCachedMeshes();
                            
                            System.out.println("Moving object. distance: " + longestCollisionDist);
                        }
                        window.updateImage();
                    }
                } // for each object in scene
            }
            //System.out.println("Selection collides: " + selectionCollides ); // Print result
        } else {
            System.out.println("No objects selected to check for collisions.");
        }
    }
    
    /**
     * demo
     * Description: Example of common tasks.
     */
    public void demo(LayoutWindow window){
        (new Thread() {
            public void run() {
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
              
                
                
                //
                // Detect if a selected object is colliding with other objects in the scene.
                // Note: This is only one possible implementation of collision detection.
                // For a section, capture the locations of the edges, then scan through the objects in the scene,
                // and check if the edges intersect with any of the faces.
                // Collitions on multiple objects constructed to represent the router, collete and machine could be used to detect collisions with
                // The mould part.
                //
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
                        //System.out.println(" edges " + selectedEdgePoints.size() );
                        // Scan through objects in the scene to see if there is a collistion.
                        for(int i = 0; i < sceneObjects.size(); i++){
                            ObjectInfo currInfo = sceneObjects.elementAt(i);
                            if( selectedObjects.contains(currInfo) ){          // Don't compare with self
                                continue;
                            }
                            //System.out.println("   Compare with  scene object: " + currInfo.getName() );
                            Object3D currObj = currInfo.getObject();
                            
                            //
                            // Is the object a TriangleMesh or can it be converted?
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
                                            System.out.println("Collision  object: " + currInfo.getName());
                                        }
                                    }
                                }
                            }
                        } // for each object in scene
                    }
                    System.out.println("Selection collides: " + selectionCollides ); // Print result
                } else {
                    System.out.println("No objects selected to check for collisions.");
                }
            
                
                
                //
                // Calculate a working / cutting region based on:
                // A) the objects in the scene representing the shape to create.
                // B) The projection vector or direction of the B/C axis pointing the drill in a direction
                //
                double c = 15; // rotation, 0-360.
                double b = 45; // angle 45 degrees.
                {   // test each of the approach directions. 
                    //c = 0;
                    //c = 90;
                    //c = 180;
                    //c = 270;
                }
                
                double accuracy = 0.2;
                boolean restMachiningEnabled = true;    // Will only cut regions that have not been cut allready by a previous pass.
                
                Vector<SurfacePointContainer> scanedSurfacePoints = new Vector<SurfacePointContainer>(); // used to define surface features, and avoid duplicate routing paths over areas allready cut.
                Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();  // : Make list of objects that construct the tool
                
                String gCode1 = calculateRoutingPassWithBC( window, 45, 15, accuracy, restMachiningEnabled, scanedSurfacePoints, 1 ); // First Pass
                gCode1 += calculateRoutingPassWithBC( window, 45, 15 + 180, accuracy, restMachiningEnabled, scanedSurfacePoints, 2 ); // Second Pass -> Rotated N degrees
                
                
                
                // More demos to come
                
                
                window.updateImage(); // Update scene
            }
        }).start();
    } // end demo function
    
    
    
    /**
     * calculateRoutingPassWithBC
     * Desscription:
     * // , Vector<RouterElementContainer> routerElements
     */
    public String calculateRoutingPassWithBC( LayoutWindow window, double b, double c, double accuracy, boolean restMachiningEnabled, Vector<SurfacePointContainer> scanedSurfacePoints, int passNumber ){
        LayoutModeling layout = new LayoutModeling();
        Scene scene = window.getScene();
        Vector<ObjectInfo> sceneObjects = scene.getObjects();
        
        
        // Router Size information.
        double routerHousingPosition = 1.25;
        double routerHousingSize = 0.75;
        double bitTipPosition = 0.12;
        double bitTipSize = 0.08;
        
        // Collision Properties
        double retractionValue = 0.50; // Hhigher means more change, more pull out, Lower means smoother finish, longer processing time.
        
        // Note: This concept could be used by running the following code example 4 times with the
        // following configurations (C=0, B=45), (C=90, B=45), (C=180, B=45), (C=270, B=45)
        // This way each of the sides are covered by at leas one pass.
        
        Vector<Vec3> debugMappingGrid = new Vector<Vec3>(); // pattern of cutting to be projected onto the scene.
        Vector<SurfacePointContainer> regionSurfacePoints = new Vector<SurfacePointContainer>(); // accumulated surface points projected
        Vector<Vec3> generatedCuttingPath = new Vector<Vec3>(); // GCode cutting path
        Vector<RouterElementContainer> routerElements = new Vector<RouterElementContainer>();  // : Make list of objects that construct the tool
        
        ObjectInfo surfaceMapInfo = null; // Object represents surface map.
        ObjectInfo toolPath = null;
        
        Vec3 toolVector = new Vec3(0, 1, 0); //
        Mat4 zRotationMat = Mat4.zrotation(Math.toRadians(b)); // Will be used to orient the inital position of the B axis.
        Mat4 yRotationMat = Mat4.yrotation(Math.toRadians(c)); // Will be used to orient the inital position of the C axis.
        zRotationMat.transform(toolVector); // Apply the B axis transform.
        yRotationMat.transform(toolVector); // Apply the C axis rotation
        toolVector.normalize(); // Normalize to scale the vector to a length of 1.
        System.out.println("toolVector " + toolVector);
        // Calculate the bounds of the scene objects,
        // for each object, get bounds.
        BoundingBox sceneBounds = null ; //new BoundingBox(); // Vec3 p1, Vec3 p2
        for(int i = 0; i < sceneObjects.size(); i++){
            ObjectInfo currInfo = sceneObjects.elementAt(i);
            BoundingBox currBounds = currInfo.getTranslatedBounds();
            if(sceneBounds == null){
                sceneBounds = currBounds;
            } else {
                sceneBounds.extend(currBounds);
            }
        }
        if(sceneBounds != null){
            double sceneSize = Math.max(sceneBounds.maxx - sceneBounds.minx, Math.max(sceneBounds.maxy - sceneBounds.miny, sceneBounds.maxz - sceneBounds.minz));
            //System.out.println("sceneSize " + sceneSize);
            Vec3 sceneCenter = sceneBounds.getCenter();
            
            Vec3 raySubtract = new Vec3(toolVector.times( sceneSize * 2) );
            
            // construct a grid and iterate each coordinate and translate it to the toolVector
        
            // Translate to point in space to project region mapping.
            
            Vec3 regionScan = new Vec3(sceneCenter);
            regionScan.add( toolVector.times(sceneSize) );
            
            // DEBUG Show
            ObjectInfo bcLineInfo = addLineToScene(window, regionScan,  regionScan.minus(raySubtract), "B/C Axis Ray (" + b + "-" + c + ")", false ); // debug show ray cast line
            bcLineInfo.setPhysicalMaterialId(500);
            
            int width = 80;
            int height = 80;
            
            // Loop through grid
            // TODO: take user or config data on accuracy units, and calibrate grid spacing to that size.
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    // xy offset to coords, Translate based on 'toolVector'
                    
                    Vec3 samplePoint = new Vec3(regionScan);
                    
                    //Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * 0.2 , (double)(y-(height/2)) * 0.2, (double)(y-(height/2)) * 0.2 ); // First try
                    Vec3 currGridPoint = new Vec3( (double)(x-(width/2)) * accuracy, 0, (double)(y-(height/2)) * accuracy );
                    
                    if(x % 2 == 0){ // Alternate scan direction on Y pass every other X. This is more effecient as it cuts the travel distance in half.
                        currGridPoint = new Vec3( (double)(x-(width/2)) * accuracy, 0, (double)((height-y-1) - (height/2)) * accuracy ); // reversed
                    }
                    
                    zRotationMat.transform(currGridPoint); // Apply the B axis transform.
                    yRotationMat.transform(currGridPoint); // Apply the C axis rotation
                    
                    samplePoint.add( currGridPoint ); // shift
                    
                    debugMappingGrid.addElement(currGridPoint);
                    
                    Vec3 samplePointB = samplePoint.minus(raySubtract); // Second point in ray cast
                    // Find collision location
                    Vec3 intersectPoint = null;
                    Vec3 intersectNormal = null; // experimental
                    for(int i = 0; i < sceneObjects.size(); i++){
                        ObjectInfo currInfo = sceneObjects.elementAt(i);
                        Object3D currObj = currInfo.getObject();
                        //System.out.println("Checking for intersect in: " + currInfo.getName());
                        
                        if( currInfo.getPhysicalMaterialId() == 500 ){
                            continue;
                        }
                        
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
                            CoordinateSystem cs;
                            cs = layout.getCoords(currInfo);
                            
                            // Convert object coordinates to world (absolute) coordinates.
                            // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                            Mat4 mat4 = cs.duplicate().fromLocal();
                            
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
                                
                                Vec3 normal = faceB.minus(faceA).cross(faceC.minus(faceA));
                                double length = normal.length();
                                if (length > 0.0){
                                    normal.scale(1.0/length);
                                }
                                
                                Vec3 samplePointCollision = Intersect2.getIntersection(samplePoint, samplePointB, faceA, faceB, faceC );
                                if(samplePointCollision != null){ // found intersection.
                                    //System.out.println(" *** ");
                                    if(intersectPoint != null){   // existing intersection exists, check if the new one is closer
                                        double existingDist = regionScan.distance(intersectPoint);
                                        double currrentDist = regionScan.distance(samplePointCollision);
                                        if(currrentDist < existingDist){
                                            intersectPoint = samplePointCollision;
                                            intersectNormal = normal;
                                        }
                                    } else {
                                        intersectPoint = samplePointCollision;
                                        intersectNormal = normal;
                                    }
                                }
                            }
                        }
                    }
                    if(intersectPoint != null){
                        //System.out.println(" Colision " + intersectPoint );
                        
                        // TODO: If a close point in scanedSurfacePoints exists, then don't include it in the surface to cut as it has allready been done.
                        boolean skipPointAsDuplicate = false; // only skip point if it was added from a different pass!
                        if(restMachiningEnabled){
                            double closestExistingSufacePoint = 9999999;
                            int existingPointCount = 0;
                            for(int pi = 0; pi < scanedSurfacePoints.size(); pi++){
                                SurfacePointContainer spc = scanedSurfacePoints.elementAt(pi);
                                Vec3 currExistingPoint = spc.point;
                                double currDist = intersectPoint.distance(currExistingPoint);
                                if( currDist < (accuracy * 1.5) && passNumber != spc.passNumber ){ // 1.2 // experiment with threshold
                                    skipPointAsDuplicate = true; // old method, doesn't do any overlap
                                    existingPointCount++;
                                }
                            }
                            //System.out.println("existingPointCount " + existingPointCount);
                            if(existingPointCount >= 3){ // we want overlap - needs work
                                skipPointAsDuplicate = true;
                            }
                        }
                        if(skipPointAsDuplicate == false){
                            
                            SurfacePointContainer spc = new SurfacePointContainer(intersectPoint, intersectNormal, passNumber);
                            
                            regionSurfacePoints.addElement(spc); // local intersectPoint
                            
                            scanedSurfacePoints.addElement(spc); // external
                        }
                        //addLineToScene(window, intersectPoint,  intersectPoint.plus(new Vec3(0,1,0)) );
                    }
                } // Y
            } // X
            
            if(debugMappingGrid.size() > 1){
                ObjectInfo line = addLineToScene(window, debugMappingGrid, "Projection Grid (" + b + "-" + c + ")", false);
                line.setPhysicalMaterialId(500);
            }
            
            if(regionSurfacePoints.size() > 0){
                SurfacePointContainer firstSpc = regionSurfacePoints.elementAt(0);
                SurfacePointContainer lastSpc = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1);
                
                // Add entry and exit paths from start position.
                // Note if this entry or exit collide they would beed to be rerouted.
                double maxMachineHeight = 0; // TODO calculate entry and exit points based on the capacity of the machine.
                Vec3 firstRegionSurfacePoint = firstSpc.point;
                Vec3 lastRegionSurfacePoint = lastSpc.point;
                
                
                SurfacePointContainer insertFirstSpc = new SurfacePointContainer( new Vec3(firstRegionSurfacePoint.x, firstRegionSurfacePoint.y + (sceneSize/4), firstRegionSurfacePoint.z), 0 );
                SurfacePointContainer insertLastSpc = new SurfacePointContainer(  new Vec3(lastRegionSurfacePoint.x, lastRegionSurfacePoint.y + (sceneSize/4), lastRegionSurfacePoint.z) , 0);
                
                regionSurfacePoints.add(0, insertFirstSpc); // insert entry
                regionSurfacePoints.add(regionSurfacePoints.size(), insertLastSpc );
                
                // Insert/fill points in gaps. Since the router travels in a straight line between points, we need to check each segment for collisions.
                regionSurfacePoints = fillGapsInPointPathSPC(regionSurfacePoints);
                
                // Draw line showing mapped surface
                if(regionSurfacePoints.size() > 1){
                    surfaceMapInfo = addLineToSceneSPC(window, regionSurfacePoints, "Surface Map (" + b + "-" + c + ")", true);
                    surfaceMapInfo.setPhysicalMaterialId(500);
                }
            }
        } // bounds
        
        if(regionSurfacePoints.size() == 0){
            return "";
        }
        
        //
        // Simulate cutting toolpath using regionSurfacePoints calculated from a particular B/C angle.
        //
        System.out.println("Scanning surface points using B/C Tool angle. ");
        Vec3 firstRegionSurfacePoint = regionSurfacePoints.elementAt(0).point;
        Vec3 lastRegionSurfacePoint = regionSurfacePoints.elementAt(regionSurfacePoints.size() - 1).point;
        ObjectInfo avatarCutterLine = addLineToScene(window, firstRegionSurfacePoint, firstRegionSurfacePoint.plus(toolVector.times(4) ), "Cutter (" + b + "-" + c + ")", true );
        avatarCutterLine.setPhysicalMaterialId(500);
        Curve currCurve = (Curve)avatarCutterLine.getObject();
        
        
        // Router Z height base
        ObjectInfo routerZBaseCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 4 ) ), 0.75, "Router Base (" + b + "-" + c + ")" );
        routerZBaseCubeInfo.setPhysicalMaterialId(500);
        //routerZBaseCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(routerZBaseCubeInfo, c,  0); // only C is applied
        routerElements.addElement(  new  RouterElementContainer( routerZBaseCubeInfo, 4, 0.75) );
        
        //
        // Add motor housing
        //
        // This includes multiple objects that represent the router machine.
        // Use to detect collisions.
        ObjectInfo drillBodyCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 2.2) ), 0.8, "Router Housing Base (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCubeInfo, 2.2, 0.8 ) );
        
        ObjectInfo drillBodyBackEndCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 5.0) ), 0.8, "Router Back End (" + b + "-" + c + ")" ); // Cube represents a part of the machine
        drillBodyBackEndCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyBackEndCubeInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyBackEndCubeInfo, 5.0, 0.8 ) );
        
        ObjectInfo drillBodyCilynderInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times(routerHousingPosition) ), routerHousingSize, routerHousingSize,  "Router Housing (" + b + "-" + c + ")" );
        drillBodyCilynderInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillBodyCilynderInfo, c,  b); // Set orientation
        routerElements.addElement(  new  RouterElementContainer( drillBodyCilynderInfo, routerHousingPosition, routerHousingSize) );
        
        // add Collet
        ObjectInfo drillColletInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.5 ) ), 0.2, 0.2,  "Collet (" + b + "-" + c + ")" );
        drillColletInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(drillColletInfo, c,  b);
        routerElements.addElement(  new  RouterElementContainer( drillColletInfo, 0.5, 0.2) );
        
        // Add tool tip
        //ObjectInfo toolPitCubeInfo = addCubeToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, "Bit Tip" ); // Cube represents tip of bit
        ObjectInfo toolPitCubeInfo = addCylinderToScene(window, firstRegionSurfacePoint.plus(toolVector.times( bitTipPosition ) ), bitTipSize, bitTipSize, "Bit Tip (" + b + "-" + c + ")" );
        toolPitCubeInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolPitCubeInfo, c,  b);
        routerElements.addElement( new  RouterElementContainer( toolPitCubeInfo, bitTipPosition, bitTipSize)  );
        
        // Add tool tip ball nose
        ObjectInfo toolBallNoseInfo = addSphereToScene(window, firstRegionSurfacePoint.plus(toolVector.times( 0.0001 ) ), bitTipSize, "Bit Ball Nose (" + b + "-" + c + ")" );
        toolBallNoseInfo.setPhysicalMaterialId(500);
        setObjectBCOrientation(toolBallNoseInfo, c,  b);
        RouterElementContainer ballNoseREC = new  RouterElementContainer( toolBallNoseInfo, 0.0001, bitTipSize, false); // Last parameter is enable
        routerElements.addElement(  ballNoseREC ); // Disabled collisions because BUGGY
        //System.out.println(" Ball size " + ballNoseREC.size + "  loc " + ballNoseREC.location );
       
        
        //
        // Scan surface mesh to create tool path.
        //
        for(int i = 0; i < regionSurfacePoints.size(); i++){
            Vec3 surfacePoint = regionSurfacePoints.elementAt(i).point;
            generatedCuttingPath.addElement(surfacePoint);
        }
        
        
        //
        // Retract tool based on drill bit tip and angle delta between BC and the surface.
        // This should be more effecient than using the drill tip geometry to collide with the scene for retraction.
        //
       
        
        
        
        if(surfaceMapInfo != null){
            surfaceMapInfo.setVisible(false); // Hide surface map because we want to show the GCode cut path now.
        }
        
        // If we have a GCode tool path add it to the scene.
        if(generatedCuttingPath.size() > 1){
            toolPath = addLineToScene(window, generatedCuttingPath, "GCode Tool Path (" + b + "-" + c + ")", true);
            toolPath.setPhysicalMaterialId(500);
        }
        
        
        //
        // Resolve collisions. Repeat until all collisions resolved.
        //
        boolean running = true;
        while(running){
            int collisionCount = 0;
            System.out.println("Modifying tool path to remove collisions.");
            //Vector<Vec3> updatedCuttingPath = new Vector<Vec3>();
            for(int i = 0; i < generatedCuttingPath.size(); i++){
                Vec3 currPoint = generatedCuttingPath.elementAt(i);
                
                //  calculate where the cutter would be to when fit to the current region surface point.
                Vector<Vec3> updatedPoints = new Vector<Vec3>();
                updatedPoints.addElement(currPoint);
                updatedPoints.addElement(currPoint.plus(toolVector.times(4))  ); // Make the length of the avatar arbitrary, scale later on.
                
                // Update the avatar object to show to the user where it is in space.
                currCurve.setVertexPositions(vectorToArray(updatedPoints)); // represents cutter
                avatarCutterLine.clearCachedMeshes();
                
                // Update router location
                for(int re = 0; re < routerElements.size(); re++){
                    RouterElementContainer rec = routerElements.elementAt(re);
                    ObjectInfo routerElement = rec.element;
                    CoordinateSystem reCS = routerElement.getModelingCoords();
                    reCS.setOrigin(currPoint.plus(toolVector.times(  rec.location + (rec.size / 2)  )));
                    routerElement.clearCachedMeshes();
                }
                
                // Check to see if the avatar cutter collides with any object in the scene.
                
                // Collision detection
                // The generatedCuttingPath can still collide with the scene. Perhaps keep filtering the
                // generatedCuttingPath pulling out more points that collide until there are no more collisions?
                // It appear the path along a side still collides.
                //boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
                //boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
                
                boolean collides = false;
                double retractDistance = retractionValue;
                retractDistance = 0;
                double maxLocation = 0;
                double maxCollision = 0;
                for(int re = 0; re < routerElements.size(); re++){
                    RouterElementContainer rec = routerElements.elementAt(re);
                    ObjectInfo routerElement = rec.element;
                    if(rec.enabled == false){
                        continue;
                    }
                    // rec.location
                    if(objectCollidesWithScene(routerElement, sceneObjects, routerElements)){
                        collides = true;
                        double currLocationDistance = rec.location - (rec.size / 2);
                        //retractDistance += (rec.location - (rec.size / 2)); // minus the size of the object?
                        if(currLocationDistance > maxLocation){
                            maxLocation = currLocationDistance;
                        }
                    }
                    
                    // new method - performance penalty
                    
                    if(collides){ // Only check collision distance if we know there is a collision.
                        double collideDist = objectSceneCollisionOffset( toolVector, routerElement, sceneObjects, routerElements );
                        if(collideDist > 0){
                            //System.out.println("Collide dist found " + collideDist);
                            //retractDistance += collideDist;
                            maxCollision = collideDist;
                        }
                    }
                     
                }
                retractDistance = Math.max(maxLocation, maxCollision); // Max or addition. Possible ideas.
                
                //retractDistance = 0.5;
                
                if(collides){
                //if(housingCollides || tipCollides){
                    collisionCount++;
                    //System.out.println("  Collide point " );
                    // This point will collide so:
                    // 1) We can't add the point to the GCode file, and
                    // 2) We must pull the router out along the B/C axis because we can't be sure the next point travel will collide with the scene.
                    
                    // create a void region that is a long channel angled along toolVector, and remove all generatedCuttingPath
                    // points that fall in that zone.
                    
                    // Add pull out point
                    //Vec3 retractPoint = new Vec3(currPoint);
                    //retractPoint.add(toolVector.times(retractDistance)); // was 3  retractDistance  retractionValue
                    
                    // NOTE: an optimization would be to take into account which machine object collided and retract the length needed not just the full length.
                    
                    
                    //
                    // Pull all points in channel.
                    //
                    Vec3 voidRegionStart = new Vec3(currPoint.plus(toolVector.times( 100 ) ));
                    Vec3 voidRegionEnd = new Vec3(currPoint.minus(toolVector.times( 100 ) ));
                    //int removedCount = 0;
                    for(int p = generatedCuttingPath.size() -1; p >= 0; p--){
                        Vec3 currP = generatedCuttingPath.elementAt(p);
                        
                        Vec3 closestPoint = Intersect2.closestPointToLineSegment( voidRegionStart, voidRegionEnd, currP);
                        double dist = closestPoint.distance(  currP );
                        //System.out.println("  dist " + dist );
                        double getAvgSpan = getAverageSpan(generatedCuttingPath);
                        //System.out.println("  dist " + dist + " getAvgSpan " + getAvgSpan );
                        if(dist < getAvgSpan / 2){
                            
                            Vec3 currRetractPoint = new Vec3(currP);
                            currRetractPoint.add(toolVector.times(retractDistance));
                            
                            //generatedCuttingPath.setElementAt(retractPoint, p); // NO This causes points to go off course.
                            generatedCuttingPath.setElementAt(currRetractPoint, p);
                        }
                    }
                    
                    
                    
                    //
                    // Fill in gaps created by changes
                    //
                    generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // Fill in gaps created by moving points.
                    generatedCuttingPath = removeDuplicatePoints(generatedCuttingPath); // Remove duplicates if we move multiple points to the same location.
                    //System.out.println("generatedCuttingPath size " + generatedCuttingPath.size()  );
                    
                    
                    // Update line representing toolpath
                    Curve tpCurve = (Curve)toolPath.getObject();
                    tpCurve.setVertexPositions(vectorToArray( generatedCuttingPath ));
                    toolPath.clearCachedMeshes();
                    
                    
                    //try { Thread.sleep(30); } catch(Exception e){} // Wait to show collision
                    // Note: This method of retracting to avoid collisions is simple but moves the machine excessivly in some cases.
                    
                } else {
                    //updatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                }
                
                // Update the scene
                window.updateImage();
                try { Thread.sleep(1); } catch(Exception e){} // Wait
            } // end loop generatedCuttingPath
            
            //System.out.println("size " + generatedCuttingPath.size()  + " collisionCount: " + collisionCount );
            if(collisionCount == 0){
                running = false; // we are done.
            }
        }
        
        
        
        // Now simulate the generated tool path to be written to a file.
        try { Thread.sleep(200); } catch(Exception e){}
        System.out.println("Simulating Tool Path.");
        
        
        
        //
        // Now simulate cutting of the new GCode which should result in no collisions.
        //
        String gCodeExport = "";
        int collisions = 0;
        //generatedCuttingPath = fillGapsInPointPath(generatedCuttingPath ); // We don't need to do this for the GCode, This is only for demonstration in the simulator.
        for(int i = 0; i < generatedCuttingPath.size(); i++){
            Vec3 currPoint = generatedCuttingPath.elementAt(i);
            
            //  calculate where the cutter would be to when fit to the current region surface point.
            Vector<Vec3> updatedPoints = new Vector<Vec3>();
            updatedPoints.addElement(currPoint);
            updatedPoints.addElement(currPoint.plus(toolVector.times(4))  ); // Make the length of the avatar arbitrary, scale later on.
            // Update the avatar object to show to the user where it is in space.
            currCurve.setVertexPositions(vectorToArray(updatedPoints));
            avatarCutterLine.clearCachedMeshes();
            
            // Update router location.
            for(int re = 0; re < routerElements.size(); re++){
                RouterElementContainer rec = routerElements.elementAt(re);
                ObjectInfo routerElement = rec.element;
                CoordinateSystem reCS = routerElement.getModelingCoords();
                reCS.setOrigin(currPoint.plus(toolVector.times(  rec.location + (rec.size / 2) )));
                routerElement.clearCachedMeshes();
            }
            
            
            // Check to see if the avatar cutter collides with any object in the scene.
            
            // TODO
            //boolean housingCollides = cubeCollidesWithScene( drillBodyCubeInfo, sceneObjects );
            //boolean tipCollides = cubeCollidesWithScene( toolPitCubeInfo, sceneObjects );
            
            boolean collides = false;
            for(int re = 0; re < routerElements.size(); re++){
                RouterElementContainer rec = routerElements.elementAt(re);
                if(rec.enabled == false){
                    continue;
                }
                ObjectInfo routerElement = rec.element;
                if(objectCollidesWithScene(routerElement, sceneObjects, routerElements)){
                    collides = true;
                }
            }
            
            
            //if(housingCollides || tipCollides){
            if(collides){
                collisions++;
                System.out.println("ERROR: GCode collision. ");
                
                try { Thread.sleep(12); } catch(Exception e){} // Wait to show collision, This shouldn't happen
            } else {
                //generatedCuttingPath.addElement(currPoint); // No collision, This point can be safely cut on the machine / GCode.
                
                // NOTE: XYZ need to be translated off of surface or cutting point.
                Vec3 xyzPoint = new Vec3(currPoint);
                xyzPoint.plus(toolVector.times(2.4)); // Note this value needs to be calculated based on the BC point to tip length.
                gCodeExport += "x" + xyzPoint.x + " y"+xyzPoint.y +" z"+xyzPoint.z+" b"+b+" c"+c+" f50;\n";
            }
            
            
            
            // Update the scene
            window.updateImage();
            try { Thread.sleep(5); } catch(Exception e){} // Wait
        } // end simulate GCode toolpoath
        System.out.println("Collsisions: " + collisions);
        
        String shortGCodeExport = gCodeExport.substring(0, Math.min(gCodeExport.length(), 3000));
        System.out.println("GCode: (Trimmed) " +   shortGCodeExport);
        
        
        
        //scene.removeObjectInfo(avatarCutterLine); // remove line
        
        
        return gCodeExport;
    }
    
    /**
     * addCubeToScene
     * Description:
     */
    public ObjectInfo addCubeToScene(LayoutWindow window, Vec3 location, double size, String name){
        Cube cube = new Cube(size, size, size);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(cube, coords, name);
        
        UndoRecord undo = new UndoRecord(window, false);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    /**
     * addLineToScene
     * Description: Add a line to the scene. Good for debugging 3d scene corrdinates.
     */
    public ObjectInfo addLineToScene(LayoutWindow window, Vec3 start, Vec3 end, String name, boolean visible){
        float[] s_ = new float[2];
        for(int i = 0; i < 2; i++){
            s_[i] = 0;
        }
        Vec3 [] vertex = new Vec3[2];
        vertex[0] = start;
        vertex[1] = end;
        Curve theCurve = new Curve(vertex, s_, 0, false);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(theCurve, coords, name);
        info.setVisible(visible);
        UndoRecord undo = new UndoRecord(window, false);
        //existingSelectedInfo.addChild(info, 0);
        //            info.setParent(existingSelectedInfo);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    
    /**
     * addLineToScene
     * Description:
     */
    
    public ObjectInfo addLineToScene(LayoutWindow window, Vector<Vec3> points, String name, boolean visible){
        float[] s_ = new float[points.size()];
        for(int i = 0; i < points.size(); i++){
            s_[i] = 0;
        }
        Vec3 [] vertex = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            
            vertex[i] = points.elementAt(i);
        }
        Curve theCurve = new Curve(vertex, s_, 0, false);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(theCurve, coords, name);
        info.setVisible(visible);
        UndoRecord undo = new UndoRecord(window, false);
        //existingSelectedInfo.addChild(info, 0);
        //            info.setParent(existingSelectedInfo);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    public ObjectInfo addLineToSceneSPC(LayoutWindow window, Vector<SurfacePointContainer> points, String name, boolean visible){
        float[] s_ = new float[points.size()];
        for(int i = 0; i < points.size(); i++){
            s_[i] = 0;
        }
        Vec3 [] vertex = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            SurfacePointContainer spc = points.elementAt(i);
            vertex[i] = spc.point;
        }
        Curve theCurve = new Curve(vertex, s_, 0, false);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(theCurve, coords, name);
        info.setVisible(visible);
        UndoRecord undo = new UndoRecord(window, false);
        //existingSelectedInfo.addChild(info, 0);
        //            info.setParent(existingSelectedInfo);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    
    /**
     * addCylinderToScene
     * Description:
     */
    public ObjectInfo addCylinderToScene(LayoutWindow window, Vec3 location, double width, double height, String name){
        Cylinder cylinder = new Cylinder(height, width/2, width/2, 1.0);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(cylinder, coords, name);
        
        UndoRecord undo = new UndoRecord(window, false);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    
    /**
     * addSphereToScene
     * Description: Add a Sphere object to the scene and return a reference.
     * @param: WIndow - access to scene where the object will be added.
     * @param: Vec3 location - location in space object is to be placed.
     * @param: double size - initally - xyz width.
     * @param: String name - name to display in object tree.
     */
    public ObjectInfo addSphereToScene(LayoutWindow window, Vec3 location, double size, String name){
        Sphere sphere = new Sphere(size/2, size/2, size/2);
        CoordinateSystem coords = new CoordinateSystem();
        ObjectInfo info = new ObjectInfo(sphere, coords, name);
        UndoRecord undo = new UndoRecord(window, false);
        ((LayoutWindow)window).addObject(info, undo);
        return info;
    }
    
    
    public Vector<SurfacePointContainer> fillGapsInPointPathSPC(Vector<SurfacePointContainer> regionSurfacePoints){
        double minSpan = 999999; // TODO fix this.
        double avgSpan = 0;
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1).point;
            Vec3 b = regionSurfacePoints.elementAt(i).point;
            double distance = a.distance(b);
            if(distance < minSpan){
                minSpan = distance;
            }
            avgSpan += distance;
        }
        if(regionSurfacePoints.size() > 1){
            avgSpan = avgSpan / regionSurfacePoints.size();
        }
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            
            Vec3 a = regionSurfacePoints.elementAt(i-1).point;
            Vec3 b = regionSurfacePoints.elementAt(i).point;
            double distance = a.distance(b);
            while(distance > avgSpan * 1.5){ // minSpan
                Vec3 insertMid = a.midPoint(b);
                
                SurfacePointContainer insertSPC = new SurfacePointContainer(  insertMid  , regionSurfacePoints.elementAt(i-1).passNumber );
                
                regionSurfacePoints.add( i, insertSPC );
                
                a = regionSurfacePoints.elementAt(i-1).point;
                b = regionSurfacePoints.elementAt(i).point;
                distance = a.distance(b);
            }
        }
        return regionSurfacePoints;
    }
    
    /**
     * fillGapsInPointPath
     * Description: Evaluate point list, insert mid points in areas where there are gaps.
     * Used to ensure all paths traveled are processed for collisions.
     * Note: This is demonstration code. It does not check for infinite loop conditions.
     */
    public Vector<Vec3> fillGapsInPointPath(Vector<Vec3> regionSurfacePoints){
        double minSpan = 999999; // TODO fix this.
        double avgSpan = 0;
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1);
            Vec3 b = regionSurfacePoints.elementAt(i);
            double distance = a.distance(b);
            if(distance < minSpan){
                minSpan = distance;
            }
            avgSpan += distance;
        }
        if(regionSurfacePoints.size() > 1){
            avgSpan = avgSpan / regionSurfacePoints.size();
        }
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1);
            Vec3 b = regionSurfacePoints.elementAt(i);
            double distance = a.distance(b);
            while(distance > avgSpan * 1.5){ // minSpan
                Vec3 insertMid = a.midPoint(b);
                regionSurfacePoints.add( i, insertMid );
                a = regionSurfacePoints.elementAt(i-1);
                b = regionSurfacePoints.elementAt(i);
                distance = a.distance(b);
            }
        }
        return regionSurfacePoints;
    }
    
    
    public double getAverageSpan(Vector<Vec3> regionSurfacePoints){
        double avgSpan = 0;
        for(int i = 1; i < regionSurfacePoints.size(); i++){
            Vec3 a = regionSurfacePoints.elementAt(i-1);
            Vec3 b = regionSurfacePoints.elementAt(i);
            double distance = a.distance(b);
            //if(distance < minSpan){
            //    minSpan = distance;
            //}
            avgSpan += distance;
        }
        if(regionSurfacePoints.size() > 1){
            avgSpan = avgSpan / regionSurfacePoints.size();
        }
        return avgSpan;
    }
    
    /**
     * removeDuplicatePoints
     * Description:
     */
    public Vector<Vec3> removeDuplicatePoints(Vector<Vec3> points){
        double avgSpan = getAverageSpan(points);
        for(int i = points.size() - 1; i > 0 ; i--){
            Vec3 a = points.elementAt(i-1);
            Vec3 b = points.elementAt(i);
            double distance = a.distance(b);
            if(distance < avgSpan / 100){
                points.removeElementAt(i);
            }
        }
        return points;
    }
    
    
    /**
     * vectorToArray
     */
    public Vec3[] vectorToArray(Vector<Vec3> points){
        Vec3[] array = new Vec3[points.size()];
        for(int i = 0; i < points.size(); i++){
            array[i] = (Vec3)points.elementAt(i);
        }
        return array;
    }
    
    
    /**
     * objectCollidesWithScene
     * Description: Detect if a given object collides with the scene.
     * Note: This function needs to be updated to support multiple objects representing the machine cutter.
     * @param: ObjectInfo info - object which is part of the machine used to test collisions with objects in the scene.
     * @param: Vector<ObjectInfo> - all objects in the scene. Also includes the machine objects which need to be excluded from collision detection.
     * TODO: passing in a list of geometry for the router would be better than tagging with an attribute?
     */
    public boolean objectCollidesWithScene( ObjectInfo detectInfo, Vector<ObjectInfo> sceneObjects, Vector<RouterElementContainer> routerElements ){
        boolean collides = false;
        LayoutModeling layout = new LayoutModeling();
        
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>();
        Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>(); // TEMP this won't work with multiple objects
        
        CoordinateSystem c;
        c = layout.getCoords(detectInfo);
        Mat4 mat4 = c.duplicate().fromLocal();
        
        Object3D selectedObj = detectInfo.getObject();
        TriangleMesh selectedTM = null;
        if(selectedObj instanceof TriangleMesh){
            selectedTM = (TriangleMesh)selectedObj;
        } else if(selectedObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
            selectedTM = selectedObj.convertToTriangleMesh(0.1);
        }
        if(selectedTM != null){
            selectedObjects.addElement(detectInfo);   // Save for exclusion later.
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
        
        if(selectedEdgePoints.size() > 0){
            //System.out.println(" edges " + selectedEdgePoints.size() );
            // Scan through objects in the scene to see if there is a collistion.
            for(int i = 0; i < sceneObjects.size(); i++){
                ObjectInfo currInfo = sceneObjects.elementAt(i);
                if( selectedObjects.contains(currInfo) ){          // Don't compare with self
                    continue;
                }
                if(currInfo.getPhysicalMaterialId() == 500){ // We designate these object types to be non colliding. i.e. we don't care if the
                    continue;
                }
                if( routerElements.contains(currInfo) ){ // Don't check if this router object is colliding with another router object.
                    continue;
                }
                //System.out.println("   Compare with  scene object: " + currInfo.getName() );
                Object3D currObj = currInfo.getObject();
                
                //
                // Is the object a TriangleMesh or can it be converted?
                //
                TriangleMesh triangleMesh = null;
                if(currObj instanceof TriangleMesh){
                    triangleMesh = (TriangleMesh)currObj;
                } else if(currObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                    triangleMesh = currObj.convertToTriangleMesh(0.1);
                }
                if(triangleMesh != null){
                    CoordinateSystem cc;
                    cc = layout.getCoords(currInfo);
                    
                    // Convert object coordinates to world (absolute) coordinates.
                    // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                    mat4 = cc.duplicate().fromLocal();
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
                                collides = true;
                                //System.out.println("Collision  object: " + currInfo.getName());
                                return true;
                            }
                        }
                    }
                }
            } // for each object in scene
        }
        return collides;
    }
    
    
    /**
     * objectSceneCollisionOffset
     * Description: A more advanced version of objectCollidesWithScene but returns information to communicate the extent of the collision.
     *  I.e. how far does the object need to be moved to resolve the collision.
     */
    public double objectSceneCollisionOffset( Vec3 orientation, ObjectInfo detectInfo, Vector<ObjectInfo> sceneObjects, Vector<RouterElementContainer> routerElements ){
        double result = 0;
        LayoutModeling layout = new LayoutModeling();
        
        Vector<EdgePoints> selectedEdgePoints = new Vector<EdgePoints>();
        Vector<ObjectInfo> selectedObjects = new Vector<ObjectInfo>(); // TEMP this won't work with multiple objects
        
        CoordinateSystem c;
        c = layout.getCoords(detectInfo);
        Mat4 mat4 = c.duplicate().fromLocal();
        
        
        Object3D selectedObj = detectInfo.getObject();
        TriangleMesh selectedTM = null;
        if(selectedObj instanceof TriangleMesh){
            selectedTM = (TriangleMesh)selectedObj;
        } else if(selectedObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
            selectedTM = selectedObj.convertToTriangleMesh(0.1);
        }
        Vector<Vec3> detectWorldVerts = new Vector<Vec3>();
        if(selectedTM != null){
            selectedObjects.addElement(detectInfo);   // Save for exclusion later.
            MeshVertex[] verts = selectedTM.getVertices();
            for(int v = 0; v < verts.length; v++){  // These translated verts will have the same indexes as the object array.
                Vec3 vert = new Vec3(verts[v].r); // Make a new Vec3 as we don't want to modify the geometry of the object.
                mat4.transform(vert);
                detectWorldVerts.addElement(vert); // add the translated vert to our list.
                //System.out.println("  Vert index: " + v + " - " + vert); // Print vert location XYZ data.
            }
            TriangleMesh.Edge[] edges = selectedTM.getEdges();
            for(int e = 0; e < edges.length; e++){
                TriangleMesh.Edge edge = edges[e];
                Vec3 a = detectWorldVerts.elementAt( edge.v1 );
                Vec3 b = detectWorldVerts.elementAt( edge.v2 );
                EdgePoints ep = new EdgePoints(a, b);
                selectedEdgePoints.addElement(ep);
            }
        }
        
        // Calculate the point furthest according to the orientation.
        Vec3 furthestPoint = null;
        
        
        double intersectDistance = 0;
        
        
        if(selectedEdgePoints.size() > 0){
            //System.out.println(" edges " + selectedEdgePoints.size() );
            // Scan through objects in the scene to see if there is a collistion.
            for(int i = 0; i < sceneObjects.size(); i++){
                ObjectInfo currInfo = sceneObjects.elementAt(i);
                if( selectedObjects.contains(currInfo) ){          // Don't compare with self
                    continue;
                }
                if(currInfo.getPhysicalMaterialId() == 500){ // We designate these object types to be non colliding. i.e. we don't care if the
                    continue;
                }
                if( routerElements.contains(currInfo) ){ // Don't check if this router object is colliding with another router object.
                    continue;
                }
                //System.out.println("   Compare with  scene object: " + currInfo.getName() );
                Object3D currObj = currInfo.getObject();
                
                double longestCollisionDist = 0;
                
                //
                // Is the object a TriangleMesh or can it be converted?
                //
                TriangleMesh triangleMesh = null;
                if(currObj instanceof TriangleMesh){
                    triangleMesh = (TriangleMesh)currObj;
                } else if(currObj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                    triangleMesh = currObj.convertToTriangleMesh(0.1);
                }
                if(triangleMesh != null){
                    CoordinateSystem cc;
                    cc = layout.getCoords(currInfo);
                    
                    // Convert object coordinates to world (absolute) coordinates.
                    // The object has its own coordinate system with transformations of location, orientation, scale ect. To see them in absolute world coordinates we need to convert.
                    mat4 = cc.duplicate().fromLocal();
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
                            
                            
                            Vec3 samplePointCollision = Intersect2.getIntersection(edgePoint.a, edgePoint.b, faceA, faceB, faceC );
                            if(samplePointCollision != null && samplePointCollision.x != 0  ){
                                //System.out.println("Collsion ");
                                
                                
                                double collisionDistance = getCollisionDistance( samplePointCollision, edgePoint.a, edgePoint.b, orientation );
                                //double collisionDistance = Vec3.distanceOnAxis(edgePoint.a, edgePoint.b, orientation);
                                
                                if(collisionDistance > longestCollisionDist){
                                    longestCollisionDist = collisionDistance;
                                    result = longestCollisionDist;
                                }
                                
                            }
                        }
                    }
                }
            } // for each object in scene
        }
        return result;
    }
    
    
    /**
     * setObjectBCOrientation
     * Description:
     */
    public void setObjectBCOrientation(ObjectInfo info, double c, double b){
        CoordinateSystem bcCS;
        bcCS = info.getCoords();
        CoordinateSystem zeroCS = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
        Mat4 mat4 = zeroCS.fromLocal();
        mat4 = mat4.zrotation( Math.toRadians(b) );
        bcCS.transformAxes(mat4);
        mat4 = mat4.yrotation( Math.toRadians(c) );
        bcCS.transformAxes(mat4);
        info.setCoords(bcCS);
    }
    
    
    /**
     *
     *
     */
    public double getCollisionDistance( Vec3 samplePointCollision, Vec3 a, Vec3 b, Vec3 orientation ){
        // Find the side we want to move from
        Vec3 targetDirection = new Vec3(samplePointCollision);
        targetDirection.add(orientation);
        double aDist = a.distance( targetDirection );
        double bDist = b.distance( targetDirection );
        Vec3 collidedObjectPoint = null;
        if(aDist < bDist){
            collidedObjectPoint = new Vec3(b);
        } else {
            collidedObjectPoint = new Vec3(a);
        }
        // samplePointCollision and collidedObjectPoint
        
        Vec3 temp = new Vec3(collidedObjectPoint); //
        temp.subtract(samplePointCollision );
        
        
        Vec3 perpendicularOrientation = new Vec3(orientation);
        perpendicularOrientation = perpendicularOrientation.cross(new Vec3(0,1,0));
        perpendicularOrientation.normalize();
        double angle = Vec3.getAngle( orientation, new Vec3(0, 0, 0), new Vec3(0, 1, 0));
        
        Mat4 orientationMat4 = Mat4.axisRotation(perpendicularOrientation, Math.toRadians(angle));

        orientationMat4.transform(temp);
        
        double collisionDistance = -temp.y;
    
        return collisionDistance;
    }
}

