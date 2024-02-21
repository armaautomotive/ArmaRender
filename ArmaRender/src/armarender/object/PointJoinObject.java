/**
 Copyright (C) 2019, 2021 by Jon Taylor
 PointJoinObject
 
 This program is free software; you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 
 Relevent sections:
 - Create: SceneViewer.mousePressed()
 - Draw: SoftwareCanvasDrawer.renderPointJoinObject()
 - Draw Joined Objects: Curve.drawEditObject()
   Draw mesh points?
 - Move: MoveObjectTool.keyPressed()
 - Key Point Move: ReshapeMeshTool.keyPressed()
 
 */

package armarender.object;

import armarender.*;
import armarender.texture.*;
import armarender.animation.*;
import armarender.math.*;
import armarender.ui.*;
import buoy.widget.*;
import java.io.*;

import armarender.view.*;
import armarender.texture.*;


// java.lang.NoSuchMethodException: armarender.object.PointJoinObject.<init>(java.io.DataInputStream, armarender.Scene)

// Curve extends Object3D

public class PointJoinObject extends Object3D implements Mesh // extends Curve implements Mesh
{
    Scene theScene;
    public int objectA;         // Object ID
    public int objectB;
    public int objectAPoint;    // Point Vertex in object.
    public int objectBPoint;
    public int objectASubPoint; // point of subdivided object.
    public int objectBSubPoint;
    
    protected MeshVertex vertex[];  // 2 vertex max
    protected float smoothness[];
    protected boolean closed;
    protected int smoothingMethod;
    protected WireframeMesh cachedWire;
    protected BoundingBox bounds;
    double halfx, halfy, halfz;
    
    private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("menu.smoothingMethod"),
                 new Object[] {
                     Translate.text("menu.none"),
                     Translate.text("menu.interpolating"),
                     Translate.text("menu.approximating")
                 }, Translate.text("menu.shading")),
    new Property(Translate.text("menu.closedEnds"), true)
    };
    
    
    public PointJoinObject(Vec3 v[], float smoothness[], int smoothingMethod, boolean isClosed)
    {
        //super(v, smoothness, smoothingMethod, isClosed);
        
        int i;
        theScene = null;
        
        vertex = new MeshVertex [v.length];
        for (i = 0; i < v.length; i++)
            vertex[i] = new MeshVertex(v[i]);
        this.smoothness = smoothness;
        this.smoothingMethod = smoothingMethod;
        closed = isClosed;
        
        //ObjectAID = 0;
        //ObjectBID = 0;
        objectA = 0;
        objectB = 0;
        objectAPoint = -1;
        objectBPoint = -1;
        objectASubPoint = -1;
        objectBSubPoint = -1;
        //System.out.println(" constructor ");
    }
    
    
    public PointJoinObject()
    {
        theScene = null;
        //ObjectAID = 0;
        //ObjectBID = 0;
        objectA = 0;
        objectB = 0;
        objectAPoint = -1;
        objectBPoint = -1;
        objectASubPoint = -1;
        objectBSubPoint = -1;
        //System.out.println(" constructor ");
    }
    
    /**
     * setScene
     *
     * Description: This object needs a reference to the scene in order to access the other objects this point connects.
     */
    public void setScene(Scene scene){
        this.theScene = scene;
    }
    
    public void setVertex(Vec3 v[])
    {
        vertex = new MeshVertex [v.length];
        for (int i = 0; i < v.length; i++){
            vertex[i] = new MeshVertex(v[i]);
        }
    }
    
    public Object3D duplicate()
    {
        Vec3 v[] = new Vec3 [vertex.length];
        float s[] = new float [vertex.length];
        
        for (int i = 0; i < vertex.length; i++)
        {
            v[i] = new Vec3(vertex[i].r);
            s[i] = smoothness[i];
        }
        return new PointJoinObject(v, s, smoothingMethod, closed);
    }
    
    public void copyObject(Object3D obj)
    {
        PointJoinObject cv = (PointJoinObject) obj;
        MeshVertex v[] = cv.getVertices();
        
        vertex = new MeshVertex [v.length];
        smoothness = new float [v.length];
        for (int i = 0; i < vertex.length; i++)
        {
            vertex[i] = new MeshVertex(new Vec3(v[i].r));
            smoothness[i] = cv.smoothness[i];
        }
        smoothingMethod = cv.smoothingMethod;
        setClosed(cv.closed);
        clearCachedMesh();
    }
    
    
    
    public MeshVertex[] getVertices()
    {
        return vertex;
    }
    
    public float[] getSmoothness()
    {
        return smoothness;
    }
    
    
    
    
    public void movePoint(int which, Vec3 pos)
    {
        vertex[which].r = pos;
        clearCachedMesh();
    }
    
    public Vec3 [] getVertexPositions()
    {
        Vec3 v[] = new Vec3 [vertex.length];
        for (int i = 0; i < v.length; i++)
            v[i] = new Vec3(vertex[i].r);
        return v;
    }
    
    //
    public void setVertexPositions(Vec3 v[])
    {
        //System.out.println("setVertexPositions");
        for (int i = 0; i < v.length; i++)
            vertex[i].r = v[i];
        
        clearCachedMesh();
    }
    
    /**
     * updateLocation
     *
     * Description: Set the join object based on the current curve positions.
     */
    public void updateLocation()
    {
        //System.out.println("updateLocation() ");
        if(theScene == null){
            System.out.println("PointJoinObject.updateLocation() no scene.");
            return;
        }
        LayoutModeling layout = new LayoutModeling();
        
        if(true){
        //return;
        }
        
        Vec3 v[] = new Vec3[2];
        v[0] = new Vec3(-2.0, 0.0, 0.0);
        v[1] = new Vec3(-2.0, 0.0, 0.0);
        
        // Get curve objects
        ObjectInfo objA = theScene.getObjectById(objectA).duplicate();
        ObjectInfo objB = theScene.getObjectById(objectB).duplicate();
        
        System.out.println(" pointJoin - " +  objA.getName() + " - " + objB.getName() );
        
        // Subdivided points: objectASubPoint, objectBSubPoint.
        Object co = (Object)objA.getObject();
        if((co instanceof Curve) == true){
            CoordinateSystem cs = ((ObjectInfo)objA).getCoords();
            //System.out.println("Curve");

            Curve curveA = (Curve)objA.getObject().duplicate();
            Curve curveB = (Curve)objB.getObject().duplicate();
            Vec3 [] vertsA = curveA.getVertexPositions();
            Vec3 [] vertsB = curveB.getVertexPositions();
            // Subdivide
            Vec3[] vertsASubdivided = curveA.getSubdividedVertices();
            Vec3[] vertsBSubdivided = curveB.getSubdividedVertices();
            
            // Translate to world coordinates
            for(int i = 0; i < vertsASubdivided.length; i++){
                Vec3 vertA = (Vec3)vertsASubdivided[i];
                //cs = ((ObjectInfo)objA).getCoords();
                cs = layout.getCoords(objA);
                
                Mat4 mat4 = cs.duplicate().fromLocal();
                mat4.transform(vertA);
            }
            for(int i = 0; i < vertsBSubdivided.length; i++){
                Vec3 vertB = (Vec3)vertsBSubdivided[i];
                //cs = ((ObjectInfo)objB).getCoords();
                cs = layout.getCoords(objB);
                Mat4 mat4 = cs.duplicate().fromLocal();
                mat4.transform(vertB);
            }
          
            double closestDistance = Double.MAX_VALUE;
            int closestAIndex = -1;
            int closestBIndex = -1;
            Vec3 closestAVec = new Vec3();
            Vec3 closestBVec = new Vec3();
            
            for (int a = 0; a < vertsASubdivided.length; a++){
                Vec3 vertA = vertsASubdivided[a];
                for (int b = 0; b < vertsBSubdivided.length; b++){
                    Vec3 vertB = vertsBSubdivided[b];
                    double distance = Math.sqrt(Math.pow(vertA.x - vertB.x, 2) + Math.pow(vertA.y - vertB.y, 2) + Math.pow(vertA.z - vertB.z, 2));
                    //System.out.println("distance: " + distance);
                    if(distance < closestDistance){
                        closestDistance = distance;
                        closestAIndex = a;
                        closestBIndex = b;
                        closestAVec = vertA;
                        closestBVec = vertB;
                    }
                }
            }
            if(closestAIndex > -1 && closestBIndex > -1){
                v[0] = new Vec3(closestAVec.x, closestAVec.y, closestAVec.z);
                v[1] = new Vec3(closestBVec.x, closestBVec.y, closestBVec.z);
                setVertex(v);
                clearCachedMesh();
                
                System.out.println("    vertA: " + v[0].x + " " + v[0].y + " " + v[0].z );
                System.out.println("    vertB: " + v[1].x + " " + v[1].y + " " + v[1].z );
                
                objectASubPoint = closestAIndex;
                objectBSubPoint = closestBIndex;
                
                
                System.out.println("  - a " + objectASubPoint+ " len: " + vertsASubdivided.length + " " + objA.getName());
                System.out.println("  - b " + objectBSubPoint+ " len: " + vertsBSubdivided.length + " " + objB.getName());
            }
        }
        
        // Non subdivided points: objectAPoint, objectBPoint.
        // Used for meshing when two curves are connected together, one point can update the other object.
        if((co instanceof Curve) == true){
            CoordinateSystem cs = ((ObjectInfo)objA).getCoords();
            Curve curveA = (Curve)objA.getObject().duplicate();
            Curve curveB = (Curve)objB.getObject().duplicate();
            Vec3 [] vertsA = curveA.getVertexPositions();
            Vec3 [] vertsB = curveB.getVertexPositions();
            
            // Translate to world coordinates
            for(int i = 0; i < vertsA.length; i++){
                Vec3 vertA = (Vec3)vertsA[i];
                //cs = ((ObjectInfo)objA).getCoords();
                cs = layout.getCoords(objA);
                
                Mat4 mat4 = cs.duplicate().fromLocal();
                mat4.transform(vertA);
            }
            for(int i = 0; i < vertsB.length; i++){
                Vec3 vertB = (Vec3)vertsB[i];
                //cs = ((ObjectInfo)objB).getCoords();
                cs = layout.getCoords(objB);
                Mat4 mat4 = cs.duplicate().fromLocal();
                mat4.transform(vertB);
            }
            double closestDistance = Double.MAX_VALUE;
            int closestAIndex = -1;
            int closestBIndex = -1;
            Vec3 closestAVec = new Vec3();
            Vec3 closestBVec = new Vec3();
            for (int a = 0; a < vertsA.length; a++){
                Vec3 vertA = vertsA[a];
                for (int b = 0; b < vertsB.length; b++){
                    Vec3 vertB = vertsB[b];
                    double distance = Math.sqrt(Math.pow(vertA.x - vertB.x, 2) + Math.pow(vertA.y - vertB.y, 2) + Math.pow(vertA.z - vertB.z, 2));
                    //System.out.println("distance: " + distance);
                    if(distance < closestDistance){
                        closestDistance = distance;
                        closestAIndex = a;
                        closestBIndex = b;
                        closestAVec = vertA;
                        closestBVec = vertB;
                    }
                }
            }
            if(closestAIndex > -1 && closestBIndex > -1){
                v[0] = new Vec3(closestAVec.x, closestAVec.y, closestAVec.z);
                v[1] = new Vec3(closestBVec.x, closestBVec.y, closestBVec.z);
                setVertex(v);
                clearCachedMesh();
                
                System.out.println("    _vertA: " + v[0].x + " " + v[0].y + " " + v[0].z );
                System.out.println("    _vertB: " + v[1].x + " " + v[1].y + " " + v[1].z );
                
                objectAPoint = closestAIndex;
                objectBPoint = closestBIndex;
                
                System.out.println("  _- a " + objectAPoint+ " len: " + vertsA.length + " " + objA.getName());
                System.out.println("  _- b " + objectBPoint+ " len: " + vertsB.length + " " + objB.getName());
            }
        }
        
    }
    
    /**
     * setVertexPositions     DEPRICATE
     * Description: Set vertex positions based on other objects.
     */
    public void setVertexPositions()
    {
        System.out.println("setVertexPositions. ");
        if(theScene == null){
            System.out.println("PointJoinObject no scene. ");
            return;
        }
        
        LayoutModeling layout = new LayoutModeling();
        
        Vec3 v[] = new Vec3[2];
        v[0] = new Vec3(0.0, 0.0, 0.0);
        v[1] = new Vec3(0.0, 0.0, 0.0);
        
        int count = theScene.getNumObjects();
        for(int i = 0; i < count; i++){
            ObjectInfo obj = theScene.getObject(i);
            if(obj.getId() == this.objectA){
                //System.out.println(" FOUND A " + obj.getName());
                Mesh o3d = (Mesh)obj.getObject();
                MeshVertex[] verts = o3d.getVertices();
                if(this.objectAPoint < verts.length && this.objectAPoint >= 0){
                    
                    MeshVertex vm = verts[this.objectAPoint];
                    Vec3 vec = vm.r;
                    v[0] = new Vec3(vec.x, vec.y, vec.z);
                    
                    if(obj.getObject() instanceof Curve || obj.getObject() instanceof SplineMesh){
                    
                        // update location
                        ObjectInfo childClone = obj.duplicate();
                        childClone.setLayoutView(false);
                        CoordinateSystem c;
                        c = layout.getCoords(childClone);
                        
                        Vec3 origin = c.getOrigin();
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform( v[0] );
                    }
                    
                }
                
                // if point not set but subpoint is.
                
                if( obj.getObject() instanceof Curve ){
                    //System.out.println(" CURVE " );
                    Curve curve = (Curve)obj.getObject();
                    Vec3[] subdividedPoints = curve.getSubdividedVertices();
                    
                    //if(this.objectASubPoint >= 0 &&   ){
                        
                    //}
                }
                
            }
            if(obj.getId() == this.objectB){
                //System.out.println(" FOUND B " + obj.getName());
                Mesh o3d = (Mesh)obj.getObject();
                MeshVertex[] verts = o3d.getVertices();
                if(this.objectBPoint < verts.length && this.objectBPoint >= 0){
                    MeshVertex vm = verts[this.objectBPoint];
                    Vec3 vec = vm.r;
                    v[1] = new Vec3(vec.x, vec.y, vec.z);
                    
                    if(obj.getObject() instanceof Curve || obj.getObject() instanceof SplineMesh){
                        // update location
                        ObjectInfo childClone = obj.duplicate();
                        childClone.setLayoutView(false);
                        CoordinateSystem c;
                        c = layout.getCoords(childClone);
                        
                        Vec3 origin = c.getOrigin();
                        Mat4 mat4 = c.duplicate().fromLocal();
                        mat4.transform( v[1] );
                    }
                }
            }
        }
        
        //System.out.println(" Objects in scene: " + count);
        //System.out.println(" objectA: " + objectA);
        //System.out.println(" objectB: " + objectB);
        
        //System.out.println("    A: " + v[0].x + " " + v[0].y  + " " + v[0].z );
        //System.out.println("    B: " + v[1].x + " " + v[1].y  + " " + v[1].z );
        //setVertex(v);
        
        //clearCachedMesh();
    }
    
    
    /**
     * renderObject
     *
     * Description:
     */
    public void renderObject(ObjectInfo obj, ViewerCanvas canvas, Vec3 viewDir)
    {
        if (!obj.isVisible())
            return;
        Camera theCamera = canvas.getCamera();
        if (theCamera.visibility(obj.getBounds()) == Camera.NOT_VISIBLE)
            return;
        int renderMode = canvas.getRenderMode();
        if (renderMode == ViewerCanvas.RENDER_WIREFRAME)
        {
            //System.out.println(" RENDER_WIREFRAME ");
            canvas.renderWireframe(obj.getWireframePreview(), theCamera, ViewerCanvas.lineColor);
            return;
        }
        RenderingMesh mesh = obj.getPreviewMesh();
        if (mesh != null)
        {
            
            if (parametersChanged)
            {
                TexturedVertexShader.clearCachedShaders(mesh);
                parametersChanged = false;
            }
            VertexShader shader;
            if (renderMode == ViewerCanvas.RENDER_TRANSPARENT)
            {
                //System.out.println(" RENDER_TRANSPARENT ");
                shader = new ConstantVertexShader(ViewerCanvas.transparentColor);
                canvas.renderMeshTransparent(mesh, shader, theCamera, obj.getCoords().toLocal().timesDirection(viewDir), null);
            }
            else
            {
                //System.out.println(" RENDER_MESH ");
                double time = 0.0;
                if (canvas.getScene() != null)
                    time = canvas.getScene().getTime();
                if (renderMode == ViewerCanvas.RENDER_FLAT)
                    shader = new FlatVertexShader(mesh, obj.getObject(), time, obj.getCoords().toLocal().timesDirection(viewDir));
                else if (renderMode == ViewerCanvas.RENDER_SMOOTH)
                    shader = new SmoothVertexShader(mesh, obj.getObject(), time, obj.getCoords().toLocal().timesDirection(viewDir));
                else
                    shader = new TexturedVertexShader(mesh, obj.getObject(), time, obj.getCoords().toLocal().timesDirection(viewDir)).optimize();
                
                canvas.renderMesh(mesh, shader, theCamera, obj.getObject().isClosed(), null);
            }
        }
        else {
            //System.out.println(" wireframe ");
            // always Call
            
            //Draw curve
            //canvas.renderWireframe(obj.getWireframePreview(), theCamera, ViewerCanvas.lineColor);
            
            // Draw JoinPoint.
            canvas.renderPointJoinObject( obj, theCamera );
        }
    }
    
    public void setShape(Vec3 v[], float smoothness[])
    {
        if (v.length != vertex.length)
            vertex = new MeshVertex [v.length];
        for (int i = 0; i < v.length; i++)
            vertex[i] = new MeshVertex(v[i]);
        this.smoothness = smoothness;
        clearCachedMesh();
    }
    
    public void setClosed(boolean isClosed)
    {
        closed = isClosed;
        clearCachedMesh();
    }
    
    public boolean isClosed()
    {
        return closed;
    }
    
    
    
    /**
     * writeToFile
     *
     * Description: write pointObject to file.
     */
    public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
    {
        this.theScene = theScene;
        super.writeToFile(out, theScene);
        //System.out.println("  DimensionObject writeToFile ( outstream, scene ) ");
        //System.out.println(" writeToFile  A: " + objectA + " B: " + objectB);
        
        int i;
        
        out.writeShort(0); // is this a delimiter or object id
        //out.writeInt(vertex.length); // should always be 3
        
        out.writeInt(objectA);
        out.writeInt(objectB);
        out.writeInt(objectAPoint);
        out.writeInt(objectBPoint);
        out.writeInt(objectASubPoint);
        out.writeInt(objectBSubPoint);
        
        //for (i = 0; i < vertex.length; i++)
        //{
            //vertex[i].r.writeToFile(out);
            //out.writeFloat(smoothness[i]);
        //}
        //out.writeBoolean(closed);
        //out.writeInt(smoothingMethod); // maybe use this method ID as a designation for dimension object
    }
    
    /**
     * PointJoinObject
     *
     * Descirption: Read from file.
     */
    public PointJoinObject(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
    {
        super(in, theScene);
        this.theScene = theScene;
        //System.out.println("  PointJoinObject( instream, scene ) ");
        int i;
        short version = in.readShort();
        
        if (version != 0){
            throw new InvalidObjectException("");
        }
        
        // Temporary. Actual values will be calculated by the render function when all file objects have been loaded.
        Vec3 v[] = new Vec3[2];
        v[0] = new Vec3(0.0, 0.0, 0.0);
        v[1] = new Vec3(0.0, 0.5, 0.0);
        setVertex(v);
        
        objectA = in.readInt();
        objectB = in.readInt();
        objectAPoint = in.readInt();
        objectBPoint = in.readInt();
        objectASubPoint = in.readInt();
        objectBSubPoint = in.readInt();
        
        //System.out.println(" read joint " + objectA + " " + objectB + " " + objectAPoint + " " + objectBPoint);
        
        setVertexPositions();
        
        updateLocation();
        
        //vertex = new MeshVertex [in.readInt()];
        //smoothness = new float [vertex.length];
        //for (i = 0; i < vertex.length; i++)
        //{
            //vertex[i] = new MeshVertex(new Vec3(in));
            //smoothness[i] = in.readFloat();
        //}
        //closed = in.readBoolean();
        //smoothingMethod = in.readInt();
    }
    
   
    
    
    
    /* Return a Keyframe which describes the current pose of this object. */
    
    public Keyframe getPoseKeyframe()
    {
        return new VectorKeyframe(2.0, 2.0, 2.0);
    }
    
    public void applyPoseKeyframe(Keyframe k)
    {
        VectorKeyframe key = (VectorKeyframe) k;
        
        setSize(key.x, key.y, key.z);
    }
    
    public WireframeMesh getWireframeMesh()
    {
        //System.out.println(" getWireframeMesh "  );
        
        // Calculate actual coordinates between the object points that are connected.
        setVertexPositions();
        
        int i, from[], to[];
        PointJoinObject subdiv;
        Vec3 vert[];
        
        if (cachedWire != null)
            return cachedWire;
        if (true) //smoothingMethod == NO_SMOOTHING)
            subdiv = this;
        else
            subdiv = subdividePointJoin().subdividePointJoin();
        vert = new Vec3 [subdiv.vertex.length];
        for (i = 0; i < vert.length; i++)
            vert[i] = subdiv.vertex[i].r;
        if (closed)
        {
            from = new int [vert.length];
            to = new int [vert.length];
            from[vert.length-1] = vert.length-1;
            to[vert.length-1] = 0;
        }
        else
        {
            from = new int [vert.length-1];
            to = new int [vert.length-1];
        }
        for (i = 0; i < vert.length-1; i++)
        {
            from[i] = i;
            to[i] = i+1;
        }
        return (cachedWire = new WireframeMesh(vert, from, to));
    }
    
    public PointJoinObject subdividePointJoin()
    {
        //System.out.println("  DimensionObject.subdivideDimension() ");
        if (vertex.length < 2)
            return (PointJoinObject) duplicate();
        if (vertex.length == 2)
        {
            Vec3 newpos[] = new Vec3 [] {new Vec3(vertex[0].r), vertex[0].r.plus(vertex[1].r).times(0.5), new Vec3(vertex[1].r)};
            float news[] = new float [] {smoothness[0], (smoothness[0]+smoothness[1])*0.5f, smoothness[1]};
            return new PointJoinObject(newpos, news, smoothingMethod, closed);
        }
        Vec3 v[] = new Vec3 [vertex.length];
        for (int i = 0; i < v.length; i++)
            v[i] = new Vec3(vertex[i].r);
        Vec3 newpos[];
        float news[];
        int i, j;
        if (closed)
        {
            newpos = new Vec3 [v.length*2];
            news = new float [smoothness.length*2];
            if (true) // smoothingMethod == INTERPOLATING)
            {
                newpos[0] = v[0];
                newpos[1] = calcInterpPoint(v, smoothness, v.length-1, 0, 1, 2);
                for (i = 2, j = 1; i < newpos.length; i++)
                {
                    if (i%2 == 0)
                        newpos[i] = v[j];
                    else
                    {
                        newpos[i] = calcInterpPoint(v, smoothness, j-1, j, (j+1)%v.length, (j+2)%v.length);
                        j++;
                    }
                }
            }
            else
            {
                newpos[0] = calcApproxPoint(v, smoothness, v.length-1, 0, 1);
                for (i = 1; i < v.length-1; i++)
                {
                    newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
                    newpos[i*2] = calcApproxPoint(v, smoothness, i-1, i, i+1);
                }
                newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
                newpos[i*2] = calcApproxPoint(v, smoothness, i-1, i, 0);
                newpos[i*2+1] = v[0].plus(v[i]).times(0.5);
            }
            for (i = 0; i < smoothness.length; i++)
            {
                news[i*2] = Math.min(smoothness[i]*2.0f, 1.0f);
                news[i*2+1] = 1.0f;
            }
        }
        else
        {
            newpos = new Vec3 [v.length*2-1];
            news = new float [smoothness.length*2-1];
            if (true ) // smoothingMethod == INTERPOLATING)
            {
                newpos[0] = v[0];
                newpos[1] = calcInterpPoint(v, smoothness, 0, 0, 1, 2);
                for (i = 2, j = 1; i < newpos.length-2; i++)
                {
                    if (i%2 == 0)
                        newpos[i] = v[j];
                    else
                    {
                        newpos[i] = calcInterpPoint(v, smoothness, j-1, j, j+1, j+2);
                        j++;
                    }
                }
                newpos[i] = calcInterpPoint(v, smoothness, j-1, j, j+1, j+1);
                newpos[i+1] = v[j+1];
            }
            else
            {
                newpos[0] = v[0];
                for (i = 1; i < v.length-1; i++)
                {
                    newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
                    newpos[i*2] = calcApproxPoint(v, smoothness, i-1, i, i+1);
                }
                newpos[i*2-1] = v[i].plus(v[i-1]).times(0.5);
                newpos[i*2] = v[i];
            }
            for (i = 0; i < smoothness.length-1; i++)
            {
                news[i*2] = Math.min(smoothness[i]*2.0f, 1.0f);
                news[i*2+1] = 1.0f;
            }
            news[i*2] = Math.min(smoothness[i]*2.0f, 1.0f);
        }
        return new PointJoinObject(newpos, news, smoothingMethod, closed);
    }
    
    /** Return a new Dimension object which has been subdivided the specified number of times to give a finer approximation of
     the curve shape. */
    
    public PointJoinObject subdividePointJoin(int times)
    {
        PointJoinObject c = this;
        for (int i = 0; i < times; i++)
            c = c.subdividePointJoin();
        return c;
    }
    
    /** The following two routines are used by subdivideDimension to calculate new point positions
     for interpolating and approximating subdivision.  v is the array of current points, s is
     the array of smoothness values for them, and i, j, k, and m are the indices of the points
     from which the new point will be calculated. */
    
    public static Vec3 calcInterpPoint(Vec3 v[], float s[], int i, int j, int k, int m)
    {
        double w1, w2, w3, w4;
        
        w1 = -0.0625*s[j];
        w2 = 0.5-w1;
        w4 = -0.0625*s[k];
        w3 = 0.5-w4;
        
        return new Vec3 (w1*v[i].x + w2*v[j].x + w3*v[k].x + w4*v[m].x,
                         w1*v[i].y + w2*v[j].y + w3*v[k].y + w4*v[m].y,
                         w1*v[i].z + w2*v[j].z + w3*v[k].z + w4*v[m].z);
    }
    
    public static Vec3 calcApproxPoint(Vec3 v[], float s[], int i, int j, int k)
    {
        double w1 = 0.125*s[j], w2 = 1.0-2.0*w1;
        
        return new Vec3 (w1*v[i].x + w2*v[j].x + w1*v[k].x,
                         w1*v[i].y + w2*v[j].y + w1*v[k].y,
                         w1*v[i].z + w2*v[j].z + w1*v[k].z);
    }
    
    public void setSize(double xsize, double ysize, double zsize)
    {
        Vec3 size = getBounds().getSize();
        double xscale, yscale, zscale;
        
        if (size.x == 0.0)
            xscale = 1.0;
        else
            xscale = xsize / size.x;
        if (size.y == 0.0)
            yscale = 1.0;
        else
            yscale = ysize / size.y;
        if (size.z == 0.0)
            zscale = 1.0;
        else
            zscale = zsize / size.z;
        for (int i = 0; i < vertex.length; i++)
        {
            vertex[i].r.x *= xscale;
            vertex[i].r.y *= yscale;
            vertex[i].r.z *= zscale;
        }
        clearCachedMesh();
    }
    
    protected void clearCachedMesh()
    {
        cachedWire = null;
        bounds = null;
    }
    
    public BoundingBox getBounds()
    {
        if (bounds == null)
            findBounds();
        return bounds;
    }
    
    protected void findBounds()
    {
        double minx, miny, minz, maxx, maxy, maxz;
        Vec3 v, points[];
        int i;
        
        getWireframeMesh();
        points = cachedWire.vert;
        minx = maxx = points[0].x;
        miny = maxy = points[0].y;
        minz = maxz = points[0].z;
        for (i = 1; i < points.length; i++)
        {
            v = points[i];
            if (v.x < minx) minx = v.x;
            if (v.x > maxx) maxx = v.x;
            if (v.y < miny) miny = v.y;
            if (v.y > maxy) maxy = v.y;
            if (v.z < minz) minz = v.z;
            if (v.z > maxz) maxz = v.z;
        }
        bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
    }
    
    
    public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options)
    {
        return new CurveViewer(controller, options);
    }
    
    public void setSkeleton(Skeleton s)
    {
    }
    
    public Vec3 [] getNormals()
    {
        Vec3 norm[] = new Vec3[vertex.length];
        for (int i = 0; i < norm.length; i++)
            norm[i] = Vec3.vz();
        return norm;
    }
    
    /** Get the skeleton for this pose (or null if it doesn't have one). */
    
    public Skeleton getSkeleton()
    {
        return null;
    }
    
}
