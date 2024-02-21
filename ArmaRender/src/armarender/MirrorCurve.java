/* Copyright (C) 2022-2023 by Jon Taylor

  
 Note: CurveEditorWindow has a replicate function conformMirror() that could be moved here.
 
 Check for parent child break.
 
 */

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

public class MirrorCurve
{
    static LayoutWindow window = null;
    
    public MirrorCurve(){
    }
    
    public void setLayoutWindow(LayoutWindow w){
        this.window = w;
    }

    /**
     * update
     *
     * Description: Update
     */
    public void update(ObjectInfo info){
        //System.out.println("MirrorCurve.update(): " + info.getName() + " " + info.getId() + " cls: " +  info.getObject().getClass().getName()  );
        
        if(info.getObject() instanceof Curve){                      // Curve / ArcTube selected.
            //System.out.println(" Object is curve ");
            Curve curve = (Curve)info.getObject();
            mirrorNotchAcrossParent(info);
            mirrorNotchAcrossChildren(info);
            
            // TODO: Update replicated curve.
            // Find Mirror plane object
            Vec3 mirrorLocation = null;
            
            // Note: ObjectInfo may not be scene OI and not contain children.
            ObjectInfo[] children = info.getChildren();
            ObjectInfo sceneInfo = null;
            
            // Get scene children in event object is a copy from the editor window.
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
                    
                    Curve mesh = (Curve) childOI.getObject();
                    Vec3 vert[] = mesh.getVertexPositions();
                    //System.out.println(" mirror length " + vert.length );
                    
                    if(sceneInfo != null){
                        updateCurve(childOI, info, sceneInfo);
                    } else {
                        updateCurve(childOI, info, info);
                    }
                }
            }
        } // end instance of Curve
        
        // If Mirror plane selected, update replicated curve
        if(info.getObject() instanceof MirrorPlaneObject){          // Mirror Plane object selected.
            ObjectInfo meshOI = info.getParent();
            if(meshOI != null){
                //System.out.println("MirrorCurve.update() Selected Object is Mirror Plane. ");
                updateCurve(info, meshOI, meshOI);
            }
        }
    }
    
    /**
     * mirrorNotch
     *
     * Description: Update mirror geometry for objects having the matching replcateId() property.
     *  Used to replicate notches on curve/arcs that are replicated.
     *
     * @param: ObjectInfo info - Object  to mirror.
     */
    public void mirrorNotchAcrossChildren(ObjectInfo info){
        ObjectInfo parentOI = info.getParent();
        //System.out.println(" info " + info + " window " + window + " parentOI " + parentOI  );
        if(info != null && window != null && parentOI != null){
            //Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
            ObjectInfo[] children = parentOI.getChildren(); // parent is origional arc
            for(int i = 0; i < children.length; i++){
                ObjectInfo child = (ObjectInfo)children[i];
                //System.out.println("ch " + child.getName());
                if(child.getObject() instanceof MirrorPlaneObject){
                    ObjectInfo mirrorOI = child;
                    ObjectInfo[] mirrorChildren = mirrorOI.getChildren();
                    for(int j = 0; j < mirrorChildren.length; j++){
                        ObjectInfo mirrorChild = (ObjectInfo)mirrorChildren[j];
                        if(mirrorChild.getObject() instanceof Curve ){
                            ObjectInfo replicatedArcOI = mirrorChild;
                            ObjectInfo[] replicatedNotchChildren = replicatedArcOI.getChildren();
                            for(int k = 0; k < replicatedNotchChildren.length; k++){
                                ObjectInfo oldNotchOI = (ObjectInfo)replicatedNotchChildren[k];
                                //System.out.println("oldNotchOI " + oldNotchOI.getName());
                                //System.out.println("oldNotchOI replicateId " + oldNotchOI.getReplicateId() + " = id: " + info.getId());
                                if(oldNotchOI.getReplicateId() == info.getId()){
                                    // Replicate: info  to  oldNotchOI  across mirrorOI
                                    Vec3 mirror = getMirrorPlaneX(mirrorOI);
                                    Mesh mesh = (Mesh) info.getObject();
                                    Vec3 vert[] = mesh.getVertexPositions();
                                    CoordinateSystem c;
                                    //c = meshOI.getCoords().duplicate();
                                    c = info.getCoords().duplicate();
                                    Mat4 aMat4 = c.duplicate().fromLocal();
                                    for(int v = 0; v < vert.length; v++){
                                        Vec3 currVec = new Vec3((Vec3)vert[v]);
                                        aMat4.transform(currVec);
                                        double xDistance = currVec.x - mirror.x;
                                        currVec.x = mirror.x - (xDistance);
                                        currVec.z = currVec.z;
                                        vert[v] = currVec;
                                    }
                                    Curve newMesh = (Curve)oldNotchOI.getObject().duplicate();
                                    newMesh.setVertexPositions(vert);
                                    oldNotchOI.setObject(newMesh);
                                    oldNotchOI.clearCachedMeshes();
                                    window.updateImage();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    /**
     * mirrorNotchAcrossParent
     *
     * Description: Mirror a notch, Used to replicate notches across mirrored parts.
     *
     *
     * @param: ObjectInfo info to be replicated across parent.
     */
    public void mirrorNotchAcrossParent(ObjectInfo info){
        //System.out.println("MirrorCurve.mirrorNotchAcrossParent: ");
        // Find the scene object, this editing object is only a copy without parent references.
        ObjectInfo sceneObjectInfo = info;
        /*
        Vector<ObjectInfo> objects = scene.getObjects();
        for(int i = 0; i < objects.size(); i++){
            ObjectInfo oi = (ObjectInfo)objects.elementAt(i);
            if(oi.getId() == getObject().getId()){
                //System.out.println("FOUND");
                sceneObjectInfo = oi;
                i = objects.size();
            }
        }
         */
        if(info.isReplicateEnabled() == false){
            //System.out.println("MirrorCurve mirrorNotchAcrossParent disabled. ");
            return;
        }
        
        if(sceneObjectInfo != null){
            ObjectInfo parentOI = sceneObjectInfo.getParent();
            if(parentOI != null){
                //System.out.println("parentOI: " + parentOI.getName());
                ObjectInfo[] parentChildren = parentOI.getChildren();
                for(int c = 0; c < parentChildren.length; c++){
                    ObjectInfo pcOi = (ObjectInfo)parentChildren[c];
                    if(pcOi.getId() != sceneObjectInfo.getId() &&
                       pcOi.getObject() instanceof MirrorPlaneObject &&
                       pcOi.isVisible()
                       
                       )
                    {
                        //System.out.println("Yes parent is a mirrored object. ");
                        // Find out the parent mirror point, where is the mirror plane location.
                        
                        int parentMirrorIndex = getMirrorIndex(parentOI);
                        //System.out.println("parentMirrorIndex: " + parentMirrorIndex);
                        
                        // Get the parent mirror location.
                        if(parentOI.getObject() instanceof ArcObject){
                            ArcObject arc = (ArcObject)parentOI.getObject();
                            MeshVertex v[] = arc.getVertices();
                            if(v.length > parentMirrorIndex && parentMirrorIndex > -1){
                                Vec3 mirrorPoint = new Vec3(v[parentMirrorIndex].r);
                                CoordinateSystem coords = parentOI.getCoords();
                                Mat4 mat4 = coords.duplicate().fromLocal();
                                mat4.transform(mirrorPoint);    // Translate to scene world coordinates.
                                if(mirrorPoint != null){
                                    //System.out.println("Mirror Point " + mirrorPoint);
                                    // X is the value we want.
                                    
                                    // get curve verts
                                    
                                    //Curve editCurve = (Curve)sceneObjectInfo.getObject();
                                    //MeshVertex editVerts[] = editCurve.getVertices();
                                    BoundingBox editingCurveBounds = sceneObjectInfo.getTranslatedBounds();
                                    Vec3 editingCurveCentre = editingCurveBounds.getCenter();
                                    
                                    // Is this object on the source side?
                                    //if( editingCurveCentre.x <  mirrorPoint.x ){
                                   
                                        // Does a mirrored object allready exist? if no create one in parent.
                                        boolean found = false;
                                        ObjectInfo existingMirrorCurve = null;
                                        for(int c2 = 0; c2 < parentChildren.length; c2++){
                                            ObjectInfo pcOi_2 = (ObjectInfo)parentChildren[c2];
                                            if(
                                               //pcOi_2.getName().equals(sceneObjectInfo.getName() + " REPLICATED " + sceneObjectInfo.getId())
                                               //pcOi_2.getReplicateId() == sceneObjectInfo.getId()
                                               //pcOi_2.getId() == sceneObjectInfo.getReplicateId()
                                               pcOi_2.getReplicateId() == sceneObjectInfo.getId()
                                               &&
                                               pcOi_2.getId() != sceneObjectInfo.getId()
                                               )
                                            {
                                                found = true;
                                                existingMirrorCurve = pcOi_2;
                                            }
                                        }
                                        //System.out.println("is replicated: " + found);
                                        
                                        // Calculate Mirror points
                                        Vector<Vec3> mirrorPoints = new Vector<Vec3>();
                                        Curve curve = (Curve)sceneObjectInfo.getObject(); // don't use this one. ****  info
                                        //Curve curve = (Curve)getObject().getObject(); // use the editing object version for changes.
                                        MeshVertex curveVerts[] = curve.getVertices();
                                        for(int i = 0; i < curveVerts.length; i++){
                                            Vec3 mirrorVec = new Vec3( curveVerts[i].r );
                                            CoordinateSystem curveCoords = sceneObjectInfo.getCoords(); // editing object world coordinates
                                            Mat4 curveMat4 = curveCoords.duplicate().fromLocal();
                                            curveMat4.transform(mirrorVec);    // Translate to scene world coordinates.
                                            mirrorVec.x += (mirrorPoint.x - mirrorVec.x) * 2;
                                            //mirrorVec.y += .1;
                                            mirrorPoints.addElement(mirrorVec);
                                        }
                                        
                                        // Add or update mirror object with points.
                                        if(found == false){ // Add replicated object
                                            //System.out.println("Add ");
                                            Curve cur = (Curve)sceneObjectInfo.getObject(); // used for isClosed attribute
                                            
                                            if(sceneObjectInfo.getReplicateId() == -1){ // Only replicate origional objects.
                                                addCurveFromPoints(mirrorPoints,
                                                               parentOI,
                                                               sceneObjectInfo.getName() + " REPLICATED " + sceneObjectInfo.getId(),
                                                               cur.isClosed(),
                                                               sceneObjectInfo.getId());
                                            }
                                        } else if(existingMirrorCurve != null){
                                            //System.out.println("Update ");
                                            updateCurvePoints(existingMirrorCurve, mirrorPoints);
                                        }
                                    //}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    /**
     * addCurveFromPoints
     *
     * Description: Add a curve to the scene given points.
     *
     *  @param: Vector<Vec3> points to corn curve.
     *  @param: ObjectInfo parent to add curve object to.
     *  @param: String name label.
     *  @param: boolean closed.
     *   @param: int object id of object being replicated.
     */
    public ObjectInfo addCurveFromPoints(Vector<Vec3> pointChain, ObjectInfo parentObjectInfo, String name, boolean closed, int replicateId){
        float[] s_ = new float[pointChain.size()]; // s_[0] = 0; s_[1] = 0; s_[2] = 0;
        for(int i = 0; i < pointChain.size(); i++){
            s_[i] = 1; // 1 means apply selected smothing
        }
        Vec3[] vertex = new Vec3[pointChain.size()]; // constructed curve geometry.
        for(int i = 0; i < pointChain.size(); i++){
            Vec3 v = pointChain.elementAt(i);
            vertex[i] = new Vec3(v);
        }
        Curve perferationCurve = new Curve(vertex, s_, 0, /* closed */closed ); // false
        CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
        ObjectInfo perferationInfo = new ObjectInfo(perferationCurve, coords, name);
        perferationInfo.setReplicateId( replicateId );
        perferationInfo.setParent(parentObjectInfo); // Add perferation object to selection.
        parentObjectInfo.addChild(perferationInfo, parentObjectInfo.getChildren().length); // info.getChildren().length+1
        if(window != null){
            window.addObject(perferationInfo, null);
            window.updateImage();
            window.updateTree(); // window update tree
        } else {
            System.out.println(" NO WINDOW ");
        }
        return perferationInfo;
    }
    
    /**
     * updateCurvePoints
     *
     * Description:
     */
    public void updateCurvePoints(ObjectInfo objectInfo, Vector<Vec3> pointChain){
        Curve curve = (Curve)objectInfo.getObject();
        //int smoothingMethod = curve.getSmoothingMethod();
        //System.out.println("updateCurvePoints " + objectInfo.getName() );
        
        float[] s_ = new float[pointChain.size()]; // s_[0] = 0; s_[1] = 0; s_[2] = 0;
        for(int i = 0; i < pointChain.size(); i++){
            s_[i] = 1; // 1 means apply selected smothing
        }
        Vec3[] vertex = new Vec3[pointChain.size()]; // constructed curve geometry.
        for(int i = 0; i < pointChain.size(); i++){
            Vec3 v = pointChain.elementAt(i);
            vertex[i] = new Vec3(v);
        }
        curve.setVertexPositions(vertex);
        objectInfo.setObject(curve);
        objectInfo.clearCachedMeshes();
        if(window != null){
            window.updateImage();
            //window.updateTree(); // window update tree. This is a very expensive operation.
        }
    }
    
    /**
     * getMirrorIndex
     *
     * Description: if there is a mirror plane object child, get the location information in order to copy geometry across plane.
     *
     * @param sel[]: Selection array. ??? is this parameter needed?
     * @return int: Index in editing object verts.
     */
    public int getMirrorIndex(ObjectInfo info){ // boolean sel[]
        int index = -1;
        // does the editing object contain a child object for a mirror plane.
        ObjectInfo[] children = info.getChildren();
        for(int i = 0; i < children.length; i++){
            ObjectInfo child = (ObjectInfo)children[i];
            CoordinateSystem childCS = child.getCoords();
            if(child.getObject() instanceof armarender.object.MirrorPlaneObject && child.isVisible()){
              // Get location and size, then see if it is close to a point on the arc/curve.
                Mesh mirrorMesh = (Mesh) child.getObject();
                Vec3 [] mirrorVerts = mirrorMesh.getVertexPositions();
                if(mirrorVerts.length > 1){
                    Vec3 vertA = new Vec3(mirrorVerts[0].x, mirrorVerts[0].y, mirrorVerts[0].z); // MirrorPlane Point
                    Vec3 vertB = new Vec3(mirrorVerts[1].x, mirrorVerts[1].y, mirrorVerts[1].z);
                    Mat4 childMat4 = childCS.duplicate().fromLocal();
                    childMat4.transform(vertA);
                    childMat4.transform(vertB);
                    double mirrorDistance = vertA.distance(vertB);      // span of mirror plane to apply mirror function.
                    Vec3 mirrorCentre = vertA.midPoint(vertB);          // Mid point
                    CoordinateSystem curveCS = info.getCoords();
                    Vec3 [] curveVerts = ((Mesh)info.getObject()).getVertexPositions();
                    for(int p = 0; p < curveVerts.length; p++){
                        Vec3 curveVec = (Vec3)curveVerts[p];
                        Mat4 curveMat4 = curveCS.duplicate().fromLocal();
                        curveMat4.transform(curveVec);
                        double dist = curveVec.distance(mirrorCentre);
                        if(dist < mirrorDistance * 2){
                            index = p;
                        }
                    }
                }
            }
        }
        return index;
    }
    
    
    /**
     * updateCurve
     *
     * Description: Replicate curve to mirror another, only if the mirror plane is left or right on the X axis.
     *  If the mirror plane is with the X bounds the mirror causes the geometry to be mirrored.
     *
     * @param: ObjectInfo
     * @param: ObjectInfo
     * @param:
     */
    public void updateCurve(ObjectInfo mirrorPlaneOI, ObjectInfo meshOI, ObjectInfo sceneMeshOI){
        if(
           meshOI.getObject() instanceof armarender.object.Curve &&
           mirrorPlaneOI.getObject() instanceof MirrorPlaneObject
           )
        {
            //System.out.println("updateCurve   Mirror  " + mirrorPlaneOI.getId() + "  Curve: " + meshOI.getId() + " Scene Curve: " + sceneMeshOI.getId());
            
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
            
            BoundingBox bb = meshOI.getTranslatedBounds();
            BoundingBox bbs = sceneMeshOI.getTranslatedBounds();
            Vec3 bbCenter = bb.getCenter();
            if(mirror.x > bb.minx && mirror.x < bb.maxx && mirror.x > bbs.minx && mirror.x < bbs.maxx){ 
                System.out.println("Abort curve replicate. Mirror Plane must be outside X boundary of curve.");
                return; // abort, don't replicate curve as seperate copy because the mirror plane indicates the mirror represents symetry across itself.
            }
            
            Curve newMesh = (Curve)meshOI.getObject().duplicate();
            newMesh.setVertexPositions(vert);
            CoordinateSystem newMirrorCS = new CoordinateSystem();
            ObjectInfo newMeshOI = new ObjectInfo(newMesh, newMirrorCS, "MIRROR " + meshOI.getName());
            newMeshOI.setReplicateId(sceneMeshOI.getId());
            // Add new mesh
            if(window.getScene() != null){
                // Remove existing
                Vector<ObjectInfo> sceneObjects = window.getScene().getObjects();
                
                boolean exists = false;
                ObjectInfo existingReplicatedCurve = null;
                
                for(int i = 0; i < sceneObjects.size(); i++){
                    ObjectInfo oi = sceneObjects.elementAt(i);
                    if(
                       oi.getReplicateId() == sceneMeshOI.getId()
                       &&
                       oi.getId() != sceneMeshOI.getId()
                    )
                    {
                        //
                        // Update existing coordinates
                        //
                        ((Curve)oi.getObject()).setVertexPositions(((Curve)newMeshOI.getObject()).getVertexPositions());
                        oi.setCoords(newMirrorCS); // reset coordinate system.
                        oi.clearCachedMeshes();
                        
                        //System.out.println(" found existing " + oi.getId() + " " + oi.getName() );
                        
                        existingReplicatedCurve = oi;
                        
                        exists = true;
                        i = sceneObjects.size(); // break
                    }
                }
                
                if(exists == false){            // Add replicated curve to the scene for first time.
                    
                    // Set new replicated object to be child of source object. Or child of mirror plane
                    newMeshOI.setParent(mirrorPlaneOI);
                    mirrorPlaneOI.addChild(newMeshOI, mirrorPlaneOI.getChildren().length);
                    
                    // Add replicated curve to scene
                    window.getScene().addObject(newMeshOI, null);
                    
                    //
                    // Replicate any Notch profiles in curve being replicated.
                    //
                    ObjectInfo [] curveChildren = sceneMeshOI.getChildren();
                    for(int ci = 0; ci < curveChildren.length; ci++){
                        ObjectInfo child = (ObjectInfo)curveChildren[ci];
                        if(child.getObject() instanceof Curve &&
                           child.getObject() instanceof MirrorPlaneObject == false &&
                           child.isVisible() &&
                           window != null
                           ){
                            // Replicate notch into new replicated curve
                            //System.out.println(" Notch to copy: " + child.getName() );
                            Vec3 notchMirror = getMirrorPlaneX(mirrorPlaneOI);
                            Vec3 notchVert[] = ((Mesh)child.getObject()).getVertexPositions();
                            CoordinateSystem cs;
                            cs = child.getCoords().duplicate();
                            Mat4 notchMat4 = cs.duplicate().fromLocal();
                            for(int i = 0; i < notchVert.length; i++){
                                Vec3 currVec = new Vec3((Vec3)notchVert[i]);
                                notchMat4.transform(currVec);
                                double xDistance = currVec.x - notchMirror.x;
                                currVec.x = notchMirror.x - (xDistance);
                                currVec.z = currVec.z;
                                notchVert[i] = currVec;
                            }
                            Curve newNotchMesh = (Curve)child.getObject().duplicate();
                            newNotchMesh.setVertexPositions(notchVert);
                            CoordinateSystem newNotchMirrorCS = new CoordinateSystem();
                            ObjectInfo newNotchMeshOI = new ObjectInfo(newNotchMesh, newNotchMirrorCS, "COPY " + meshOI.getName());
                            newNotchMeshOI.setReplicateId(child.getId());
                            newMeshOI.addChild( newNotchMeshOI, 0 );
                            newNotchMeshOI.setParent( newMeshOI ); // newMeshOI
                            window.addObject(newNotchMeshOI, null);
                            window.updateImage();
                            window.updateTree(); // window update tree
                        }
                    }
                    
                    // Update object List
                    window.rebuildItemList(); // redraw item list with new item added. (Slow operation.)
                    
                } else if(existingReplicatedCurve != null) {
                    // Update existing object points
                    
                    //System.out.println(" Update replicated curve points ");
                    //Curve existingMesh = (Curve)existingReplicatedCurve.getObject();
                    //existingMesh.setVertexPositions(vert);
                    //existingReplicatedCurve.clearCachedMeshes();
                    
                    // Update notches across replicated curve
                    
                    // TODO:
                }
                
                window.updateImage();
                window.updateMenus();
            }
             
        } // Parameters are correct types.
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
    
}
