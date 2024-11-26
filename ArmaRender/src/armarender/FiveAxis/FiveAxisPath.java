/* Copyright (C) 2021-2022 by Jon Taylor
 
 This program is free software; you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more details.*/

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
import java.awt.Color;
import java.util.Vector;
import armarender.object.Cylinder;

/**
  A Five Axis Path defines a pattern of router orientation to cut out a specific prifile at a specific AB angle.
 
 */

public class FiveAxisPath extends Curve implements Mesh
{
    protected MeshVertex vertex[];  // Path location vertex points. These points don't define the fector of the tool at the point.
    //protected MeshVertex previewVertex[]; // Many verts. Represents the curved geometry for representation.
    
    protected MeshVertex vertexAB[]; // Verticies that define AB vectors for each of the location vertex points.
    
    protected float smoothness[];
    protected boolean closed;
    protected int smoothingMethod;
    protected WireframeMesh cachedWire;
    protected BoundingBox bounds;
    
    protected double bendRadius = 0.1441;
    protected double bendAngle = 0;
    protected double tubeDiameter = 0.01;
    protected double tubeWall = 0;
    protected double minBendSpan = 0;       // min distance bender can do between seperate bends. Will only warn if bends exceed min.
    
    protected Vector cylendarRender = new Vector();
    private Scene scene;
    
    public static boolean OUTER = true;
    public static boolean INNER = false;
    
    private static final Property PROPERTIES[] = new Property [] {
        new Property(Translate.text("menu.smoothingMethod"),
                     new Object[] {
                        Translate.text("menu.none"),
                        Translate.text("menu.interpolating"),
                        Translate.text("menu.approximating")
                        }, Translate.text("menu.shading")),
        new Property(Translate.text("menu.closedEnds"), true)
        //new Property("Bend Radius",  0, 6.2, 0.1441 ),
        //new Property("Tube Diameter", 0, 1000.0, 2.0 ),
        //new Property("Tube Wall", 0, 1000.0, 0.095 ),
        //new Property("Min Bend Span", 0, 1000.0, 0.095 )
        //new Property("Mirrored", false)
    };
    
    public FiveAxisPath(Vec3 v[], float smoothness[], int smoothingMethod, boolean isClosed)
    {
        super(v, smoothness, smoothingMethod, isClosed);
        
        int i;
        
        vertex = new MeshVertex [v.length];
        for (i = 0; i < v.length; i++)
            vertex[i] = new MeshVertex(v[i]);
        this.smoothness = smoothness;
        this.smoothingMethod = smoothingMethod;
        closed = isClosed;
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
        
        FiveAxisPath result = new FiveAxisPath(v, s, smoothingMethod, closed);
        //result.setBendRadius(getBendRadius());
        //result.setTubeDiameter(getTubeDiameter());
        //result.setTubeWall(getTubeWall());
        //result.setMinBendSpan(getMinBendSpan());
        result.setScene(getScene());
        
        return result;
    }
    
    public void copyObject(Object3D obj)
    {
        FiveAxisPath cv = (FiveAxisPath) obj;
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
        
        setBendRadius(cv.getBendRadius());
        setTubeDiameter(cv.getTubeDiameter());
        setTubeWall(cv.getTubeWall());
        setScene(cv.getScene());
        
        clearCachedMesh();
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
    
    public BoundingBox getBounds()
    {
        if (bounds == null)
            findBounds();
        return bounds;
    }
    
    public MeshVertex[] getVertices()
    {
        return vertex;
    }
    
    public float[] getSmoothness()
    {
        return smoothness;
    }
    
    
    /** Get the smoothing method being used for this mesh. */
    
    public int getSmoothingMethod()
    {
        return smoothingMethod;
    }
    
    /** Move a single control vertex. */
    
    public void movePoint(int which, Vec3 pos)
    {
        vertex[which].r = pos;
        clearCachedMesh();
    }
    
    /** Get a list of the positions of all vertices which define the mesh. */
    
    public Vec3 [] getVertexPositions()
    {
        Vec3 v[] = new Vec3 [vertex.length];
        for (int i = 0; i < v.length; i++)
            v[i] = new Vec3(vertex[i].r);
        return v;
    }
    
    /** Set new positions for all vertices. */
    
    public void setVertexPositions(Vec3 v[])
    {
        if(vertex.length != v.length){
            vertex = new MeshVertex[v.length];
            for (int i = 0; i < v.length; i++)
                vertex[i] = new MeshVertex(v[i]);
        }
        for (int i = 0; i < v.length; i++)
            vertex[i].r = v[i];
        clearCachedMesh();
    }
    
    /** Set the smoothing method. */
    
    public void setSmoothingMethod(int method)
    {
        smoothingMethod = method;
        clearCachedMesh();
    }
    
    /** Set the smoothness values for all vertices. */
    
    public void setSmoothness(float s[])
    {
        for (int i = 0; i < s.length; i++)
            smoothness[i] = s[i];
        clearCachedMesh();
    }
    
    /** Set both the positions and smoothness values for all points. */
    
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
    
    /** Clear the cached mesh. */
    
    protected void clearCachedMesh()
    {
        cachedWire = null;
        bounds = null;
    }
    
    public WireframeMesh getWireframeMesh()
    {
        int i, from[], to[];
        FiveAxisPath subdiv;
        Vec3 vert[];
        
        if (cachedWire != null)
            return cachedWire;
        if (smoothingMethod == NO_SMOOTHING)
            subdiv = this;
        else
            subdiv = subdividePath().subdividePath();
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
    
    /** Return a new Curve object which has been subdivided once to give a finer approximation of the curve shape. */
    
    public FiveAxisPath subdividePath()
    {
        //System.out.println("  DimensionLinearObject.subdivideArc() ");
        if (vertex.length < 2)
            return (FiveAxisPath) duplicate();
        if (vertex.length == 2)
        {
            Vec3 newpos[] = new Vec3 [] {new Vec3(vertex[0].r), vertex[0].r.plus(vertex[1].r).times(0.5), new Vec3(vertex[1].r)};
            float news[] = new float [] {smoothness[0], (smoothness[0]+smoothness[1])*0.5f, smoothness[1]};
            return new FiveAxisPath(newpos, news, smoothingMethod, closed);
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
            if (smoothingMethod == INTERPOLATING)
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
            if (smoothingMethod == INTERPOLATING)
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
        return new FiveAxisPath(newpos, news, smoothingMethod, closed);
    }
     
    
    /** Return a new Dimension object which has been subdivided the specified number of times to give a finer approximation of
     the curve shape. */
    
    
    public FiveAxisPath subdividePath(int times)
    {
        FiveAxisPath c = this;
        for (int i = 0; i < times; i++)
            c = c.subdividePath();
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
    
    public boolean canSetTexture()
    {
        return false;
    }
    
    public int canConvertToTriangleMesh()
    {
        if (closed)
            return EXACTLY;
        return CANT_CONVERT;
    }
    
    public TriangleMesh convertToTriangleMesh(double tol)
    {
        TriangleMesh mesh = triangulateArc();
        if (mesh != null)
            mesh = TriangleMesh.optimizeMesh(mesh);
        return mesh;
    }
    
    private TriangleMesh triangulateArc() // triangulateDimension
    {
        //System.out.println("  DimensionObject.triangulateDimension() ");
        
        Vec3 v[] = new Vec3 [vertex.length], size = getBounds().getSize();
        Vec2 v2[] = new Vec2 [vertex.length];
        int i, j, current, count, min;
        int index[] = new int [vertex.length], faces[][] = new int [vertex.length-2][3];
        double dir, dir2;
        boolean inside;
        
        // Find the largest dimension of the line, and project the vertices onto the other two.
        
        if (size.x > size.y)
        {
            if (size.y > size.z)
                j = 2;
            else
                j = 1;
        }
        else
        {
            if (size.x > size.z)
                j = 2;
            else
                j = 0;
        }
        for (i = 0; i < vertex.length; i++)
        {
            v[i] = vertex[i].r;
            v2[i] = vertex[i].r.dropAxis(j);
        }
        
        // Select the vertex to start from.
        
        min = 0;
        for (i = 1; i < v2.length; i++)
        {
            if (v2[i].x < v2[min].x)
                min = i;
        }
        for (i = 0; i < index.length; i++)
            index[i] = i;
        current = min;
        do
        {
            dir = triangleDirection(v2, index, v2.length, current);
            if (dir == 0.0)
            {
                current = (current+1)%index.length;
                if (current == min)
                    return null;  // All of the points lie on a straight line.
            }
        } while (dir == 0.0);
        
        // Now add the triangles one at a time.
        
        count = index.length;
        for (i = 0; i < vertex.length-2; i++)
        {
            // Determine whether a triangle centered at the current vertex will face in the
            // correct direction.  If not, or if the triangle will contain another vertex,
            // then try the next vertex.
            
            j = current;
            do
            {
                dir2 = triangleDirection(v2, index, count, current);
                inside = containsPoints(v2, index, count, current);
                if (dir2*dir < 0.0 || inside)
                {
                    current = (current+1)%count;
                    if (current == j)
                        return null;  // Cannot triangulate the projected curve.
                }
            } while (dir2*dir < 0.0 || inside);
            
            // Add the face, and remove the vertex from the list.
            
            if (current == 0)
                faces[i][0] = index[count-1];
            else
                faces[i][0] = index[current-1];
            faces[i][1] = index[current];
            if (current == count-1)
                faces[i][2] = index[0];
            else
                faces[i][2] = index[current+1];
            for (j = current; j < count-1; j++)
                index[j] = index[j+1];
            count--;
            current = (current+1)%count;
        }
        TriangleMesh mesh = new TriangleMesh(v, faces);
        TriangleMesh.Vertex vert[] = (TriangleMesh.Vertex []) mesh.getVertices();
        for (i = 0; i < vert.length; i++)
            vert[i].smoothness = smoothness[i];
        mesh.setSmoothingMethod(smoothingMethod);
        return mesh;
    }
    
    /** This is used by the above method.  Given the list of remaining vertices, it finds the
     edges to either size of the specified vertex and returns their cross product.  This tells
     which way a triangle centered at the vertex will face. */
    
    double triangleDirection(Vec2 v2[], int index[], int count, int which)
    {
        Vec2 va, vb;
        
        if (which == 0)
            va = v2[index[which]].minus(v2[index[count-1]]);
        else
            va = v2[index[which]].minus(v2[index[which-1]]);
        if (which == count-1)
            vb = v2[index[which]].minus(v2[index[0]]);
        else
            vb = v2[index[which]].minus(v2[index[which+1]]);
        return va.cross(vb);
    }
    
    /** This is used by convertToTriangleMesh.  Given the list of remaining vertices, it
     determines whether a triangle centered at the specified vertex would contain any
     other vertices. */
    
    boolean containsPoints(Vec2 v2[], int index[], int count, int which)
    {
        Vec2 va, vb, v;
        double a, b, c;
        int i, prev, next;
        
        if (which == 0)
            prev = count-1;
        else
            prev = which-1;
        if (which == count-1)
            next = 0;
        else
            next = which+1;
        va = v2[index[which]].minus(v2[index[prev]]);
        vb = v2[index[which]].minus(v2[index[next]]);
        a = va.cross(vb);
        va.scale(1.0/a);
        vb.scale(1.0/a);
        for (i = 0; i < count; i++)
            if (i != prev && i != which && i != next)
            {
                v = v2[index[i]].minus(v2[index[which]]);
                b = vb.cross(v);
                c = v.cross(va);
                a = 1 - b - c;
                if (a >= 0.0 && a <= 1.0 && b >= 0.0 && b <= 1.0 && c >= 0.0 && c <= 1.0)
                    return true;
            }
        return false;
    }
    
    /** Normal vectors do not make sense for a curve, since it does not define a surface. */
    
    public Vec3 [] getNormals()
    {
        Vec3 norm[] = new Vec3[vertex.length];
        for (int i = 0; i < norm.length; i++)
            norm[i] = Vec3.vz();
        return norm;
    }
    
    public boolean isEditable()
    {
        return true;
    }
    
    /** Get the skeleton.  This returns null, since Curves cannot have skeletons. */
    
    public Skeleton getSkeleton()
    {
        return null;
    }
    
    /** Set the skeleton.  This does nothing, since Curves cannot have skeletons. */
    
    public void setSkeleton(Skeleton s)
    {
    }
    
    public void edit(EditingWindow parent, ObjectInfo info, Runnable cb)
    {
        CurveEditorWindow ed = new CurveEditorWindow(parent, "Five Axis Path object '"+ info.getName() +"'", info, cb, true);
        ed.setVisible(true);
    }
    
    public void editGesture(final EditingWindow parent, ObjectInfo info, Runnable cb, ObjectInfo realObject)
    {
        CurveEditorWindow ed = new CurveEditorWindow(parent, "Gesture '"+ info.getName() +"'", info, cb, false);
        ViewerCanvas views[] = ed.getAllViews();
        for (int i = 0; i < views.length; i++)
            ((MeshViewer) views[i]).setScene(parent.getScene(), realObject);
        ed.setVisible(true);
    }
    
    /** Get a MeshViewer which can be used for viewing this mesh. */
    
    public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options)
    {
        return new CurveViewer(controller, options);
    }
    
    /** The following two methods are used for reading and writing files.  The first is a
     constructor which reads the necessary data from an input stream.  The other writes
     the object's representation to an output stream. */
    
    public FiveAxisPath(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException // DimensionLinearObject
    {
        super(in, theScene);
        
        this.scene = theScene;
        //System.out.println("  DimensionObject( instream, scene ) ");
        
        int i;
        short version = in.readShort();
        
        if (version != 0)
            throw new InvalidObjectException("");
        vertex = new MeshVertex [in.readInt()];
        smoothness = new float [vertex.length];
        for (i = 0; i < vertex.length; i++)
        {
            vertex[i] = new MeshVertex(new Vec3(in));
            smoothness[i] = in.readFloat();
        }
        closed = in.readBoolean();
        smoothingMethod = in.readInt();
        bendRadius = in.readDouble();    // Bend die radius.
        bendAngle = in.readDouble();     // Bend angle
        tubeDiameter = in.readDouble();  // Tube diameter
        tubeWall = in.readDouble();      // Tube wall thickness
        try{
            minBendSpan = in.readDouble();   // min bend span.
        } catch (Exception e){
            
        }
    }
    
    
    
    /**
     * getArcSegmentLength
     *
     * Desxription: Calculates the given segment length given the arc circumfrances.
     *  Used by renderObject and CAM GCode bend and notch functions.
     *    - Ever segment will have three components, start arc circumfrance, straight length, end arc circumfrance.
     */
    public Vector<Double> getArcSegmentLength(){
        System.out.println("getArcSegmentLength()");
        LayoutModeling layout = new LayoutModeling();
        Mesh mesh = (Mesh)this;
        Vec3 [] verts = mesh.getVertexPositions();
        Vector<Double> lengthVec = new Vector<Double>();
        
        double total = 0;
        //double nonArcTotal = 0;
        
        //double[] lengths = new double[verts.length];
        //for(int i = 0; i < verts.length; i++){
        //    lengths[i] = 0;
        //}
        
        Vec3 previousAPoint = null;
        Vec3 previousCPoint = null;
        double previousArcCircumfrance = 0;
        
        for(int i = 2; i < verts.length; i++){ // && segment < verts.length
            Vec3 vertA = new Vec3(verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
            Vec3 vertB = new Vec3(verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
            Vec3 vertC = new Vec3(verts[i - 0].x, verts[i - 0].y, verts[i - 0].z);
            //System.out.println("vertA " +vertA  + " vertB.x " + vertB.x);
            vertA.subtract(vertB);  // align to vertB
            vertC.subtract(vertB);
            double lineSegmentLength = vertA.distance(vertB);
            //System.out.println(" lineSegmentLength: " + lineSegmentLength);
            
            //System.out.println(" vertA " +vertA );
            //System.out.println("vertC " +vertC );
            double angle = vertA.getAngle(vertC);
            //System.out.println(" Bend angle " + Math.toDegrees(angle));
            double halfAngle = angle / 2;
            
            
            double arcCircumfrance = (2 * Math.PI * bendRadius) * (((180 - Math.toDegrees(angle)) / 360));
            
            //System.out.println(" arc circumfrance length: " + arcCircumfrance );
            
            // Calculate line segment length
            double triangleAngleA = 90; // 180 - 90 - (angle / 2);
            double triangleAngleB = (Math.toDegrees(angle) / 2); //
            double triangleAngleC = 180 - 90 - (Math.toDegrees(angle) / 2);
            double triangleLengthB = bendRadius;
            double triangleLengthC = triangleLengthB *
                                        Math.sin( Math.toRadians(triangleAngleC) )  /
                                        Math.sin( Math.toRadians(triangleAngleB) );
            
            double triangleLengthA = triangleLengthB *
                                        Math.sin( Math.toRadians(triangleAngleA) ) /
                                        Math.sin( Math.toRadians(triangleAngleB) ); // a = b·sin(A)/sin(B)
            
            //System.out.println(" triangleAngleA: " + triangleAngleA);
            //System.out.println(" triangleAngleB: " + triangleAngleB);
            //System.out.println(" triangleAngleC: " + triangleAngleC);
            //System.out.println(" triangleLengthA: " + triangleLengthA);
            //System.out.println(" triangleLengthB: " + triangleLengthB);
            
            //System.out.println(" Triangle length: " + triangleLengthC);
            //double bendStartDistance = lineSegmentLength - triangleLengthC;
            //System.out.println(" Line segment to bend start: " + bendStartDistance);
            
            if(i == verts.length - 1){
                double lastSegmentLength = vertB.distance(vertC);
                //System.out.println(" lastSegmentLength: " + lastSegmentLength);
                //double lastSegmentLengthWithBend = triangleLengthC;
                //System.out.println(" lastSegmentLength minus bend: " + (lastSegmentLength - triangleLengthC) );
            }
            
            // bendRadius
            // Calculate the length offset to start the arc.
            
            Vec3 arcCentre = new Vec3(vertB);
            
            //
            //Vec3 vertA = new Vec3(verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
            //vertB = new Vec3(verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
            
            // Get Vector Pitch Yaw between A and C
            Vec3 vertAX = new Vec3(verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
            Vec3 vertCX = new Vec3(verts[i - 0].x, verts[i - 0].y, verts[i - 0].z);
            //System.out.println("   vertAX " +vertAX  +  "   vertCX " +vertCX  );
            vertAX.subtract(vertCX);
            //System.out.println("      vertAX " +vertAX );
            vertCX = new Vec3(0, 0.1, 0);
            double acAngleX = vertCX.getAngleX(vertAX);
            double acAngleY = vertCX.getAngleY(vertAX);
            double acAngleZ = vertCX.getAngleZ(vertAX);
            //System.out.println("   acAngleX " + Math.toDegrees(acAngleX) +  " acAngleY " + Math.toDegrees(acAngleY)  + " acAngleZ " + Math.toDegrees(acAngleZ));
            double halfACAngleZ = acAngleZ / 2;
            
            double pitch = halfAngle + halfACAngleZ;
            double yaw = halfAngle + halfACAngleZ;
            arcCentre.x = vertB.x + (bendRadius * Math.sin(yaw) * Math.cos(pitch));
            arcCentre.y = vertB.y + (bendRadius * Math.sin(pitch));
            arcCentre.z = vertB.z + (bendRadius * Math.cos(yaw) * Math.cos(pitch));
        
            
            // angle
            double invertedAngle = (Math.PI / 2) - angle;
            //System.out.println(" inverted angle: " + invertedAngle);
            
            // Calculate length of arc.
            double arcLength = 2 * Math.PI * bendRadius * (Math.toDegrees(angle) / 360);
            //System.out.println(" --- arcLength: " + arcLength);
            
            double arcLengthInverted = 2 * Math.PI * bendRadius * (Math.toDegrees(invertedAngle) / 360);
            if(arcLengthInverted < 0){
                arcLengthInverted = -arcLengthInverted;
            }
            //System.out.println(" --- arcLength Inverted: " + arcLengthInverted);
            
            
            double arcDistance = (2 * bendRadius) * Math.sin( Math.toDegrees(angle) /  (2 * bendRadius)  );
            if(arcDistance < 0){
                arcDistance = - arcDistance;
            }
            //System.out.println("arcDistance: " + arcDistance);
            
            // Transfer length of line by distance.
            Vec3 vertAL = new Vec3(verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
            Vec3 vertBL = new Vec3(verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
            Vec3 vertCL = new Vec3(verts[i - 0].x, verts[i - 0].y, verts[i - 0].z);
            //vertAL.subtract(vertBL);
            // vertAL is a vector
            //System.out.println("  XXX " + vertAL.x + " " + vertAL.y + " " + vertAL.z );
            
            // transform point from zerod vertBL of length
            Vec3 aLenSlide = new Vec3(0, 0.5, 0); // (arcDistance/2)
            CoordinateSystem cs = new CoordinateSystem();
            //cs = layout.getCoords(this);
            cs.setOrigin(new Vec3(0, 0, 0));
            //double rot[] = cs.getRotationAngles();
            //System.out.println("  XXX " +rot[0] + " " + rot[1] + " " + rot[2] );
            //c.setOrientation(-rot[0], -rot[1], -rot[2]);
            
            
            
            double distanceShift = triangleLengthC; //  arcLength / 2; //  / 2; arcLength / 2; //  arcLength    arcLengthInverted
            
            // New approach
            // AB distance
            double abDistance = vertAL.distance(vertBL);
            double abScaleFactor = distanceShift / abDistance; // (length along line)
            Vec3 abScaledSegment = new Vec3(vertAL);
            abScaledSegment.subtract(vertBL);
            abScaledSegment = abScaledSegment.times(abScaleFactor);
            Vec3 aPoint = new Vec3(abScaledSegment);
            aPoint.add(vertBL);
            
            
            double cbDistance = vertCL.distance(vertBL);
            double cbScaleFactor = distanceShift / cbDistance; // (length along line)
            Vec3 cbScaledSegment = new Vec3(vertCL);
            cbScaledSegment.subtract(vertBL);
            cbScaledSegment = cbScaledSegment.times(cbScaleFactor);
            Vec3 cPoint = new Vec3(cbScaledSegment);
            cPoint.add(vertBL);
            
            //canvas.renderLine(new Vec3(aPoint.x, aPoint.y, aPoint.z), new Vec3(cPoint.x, cPoint.y + 0.0, cPoint.z), theCamera, red); // line between curve start - and
            
            // Draw straight line segments
            Vec3 vertA_ = new Vec3(verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
            
            if(previousCPoint != null && previousAPoint != null){
                //canvas.renderLine(previousCPoint, aPoint, theCamera, lightBlue); // a -> b-offset //  vertA_
                if(tubeDiameter > 0){
                    //renderTubeCylendar(canvas, verts, previousCPoint, aPoint);
                    
                    lengthVec.addElement(previousArcCircumfrance/2);
                    total += previousArcCircumfrance / 2;
                    
                    //lengths[i-2] += previousCPoint.distance(aPoint);
                    lengthVec.addElement(previousCPoint.distance(aPoint));
                    System.out.println("mid " + previousCPoint.distance(aPoint));
                    total += previousCPoint.distance(aPoint);                               // Tally bend length
                    
                    
                    lengthVec.addElement(arcCircumfrance / 2);
                    total += arcCircumfrance / 2;
                }
            } else {
                //canvas.renderLine(vertA_, aPoint, theCamera, lightBlue); // a -> b-offset //  vertA_
                if(tubeDiameter > 0){
                    //renderTubeCylendar(canvas, verts, vertA_, aPoint);
                    
                    lengthVec.addElement(0.0);
                    
                    //lengths[i-2] += vertA_.distance(aPoint);
                    lengthVec.addElement(vertA_.distance(aPoint));
                    System.out.println("first " + vertA_.distance(aPoint));
                    total += vertA_.distance(aPoint);                               // Tally bend length
                    
                    
                    lengthVec.addElement(arcCircumfrance / 2);
                    total += arcCircumfrance / 2;
                }
            }
            
            // Add arc bend circumfrance length to totals.
            //lengths[i-2] += arcCircumfrance; // Arc length
            //lengthVec.addElement(arcCircumfrance);
            //System.out.println("Circumfrance:  " + arcCircumfrance );
            //total += arcCircumfrance;                                               // Tally bend length
             
            if(i == verts.length - 1){  // last segment
                Vec3 vertC_ = new Vec3(verts[i - 0].x, verts[i - 0].y, verts[i - 0].z);
                //canvas.renderLine(cPoint, vertC_, theCamera, lightBlue);
                if(tubeDiameter > 0){
                    //renderTubeCylendar(canvas, verts, cPoint, vertC_);
                    
                    if(previousArcCircumfrance > 0){ //
                        lengthVec.addElement(previousArcCircumfrance/2);
                        total += previousArcCircumfrance/2;
                    } else {                                                        // If no prev arc, use arcCirc
                        lengthVec.addElement(arcCircumfrance / 2);
                        total += arcCircumfrance / 2;
                    }
                    
                    //lengths[i-2] += cPoint.distance(vertC_);
                    lengthVec.addElement(cPoint.distance(vertC_));
                    System.out.println("Last " + cPoint.distance(vertC_));
                    total += cPoint.distance(vertC_);                               // Tally bend length
                    
                    lengthVec.addElement(0.0); // end has no ending arc
                }
            }
            
            
            // render arc profile (low resolution)
            // triangleLengthA (distance centre to pointB)
            // vertBL (non arc )
            // bendRadius  (length from centre point to desired location)
            
            double vertBOffset = triangleLengthA - bendRadius; // desired distance from vertB to curve
            Vec3 arcMidpoint = aPoint.midPoint(cPoint); // used with vertBL to create vector (direction) on which render arc lies.
            double dist = arcMidpoint.distance(vertBL);
            double scale = vertBOffset/  dist   ; // distanceShift
            //arcMidpoint = arcMidpoint.minus( vertB );  // create vector
            Vec3 arcPoint = new Vec3( arcMidpoint  ); // arcMidpoint
            arcPoint = arcPoint.minus( vertB );  //  vertB  create vector
            arcPoint = arcPoint.times(scale);
            arcPoint.add(vertB);
            //canvas.renderLine(arcPoint, aPoint, theCamera, blue); // low res curve profile A
            //canvas.renderLine(arcPoint, cPoint, theCamera, blue); // low res curve profile C
            
            //total += arcPoint.distance(aPoint);     // tally curved length
            //total += arcPoint.distance(cPoint);     // tally curved length
            
            //
            //double bCentreDistance = vertBL.distance(vertBL);
            
            previousAPoint = aPoint;
            previousCPoint = cPoint;
            previousArcCircumfrance = arcCircumfrance;
        } // verts loop
        
        if(verts.length == 2){
            Vec3 vertA = new Vec3(verts[0].x, verts[0].y, verts[0].z);
            Vec3 vertB = new Vec3(verts[1].x, verts[1].y, verts[1].z);
            //lengths[0] = vertA.distance(vertB);
            lengthVec.addElement(vertA.distance(vertB));
            
            total = vertA.distance(vertB);
        }
        
        System.out.println("Total: " + total);
        
        return lengthVec;
    }
    
    
    public Vector<Double> getArcSegmentLength2( ){
        System.out.println("getArcSegmentLength2() ****** " );
        
        Vector<Double> result = new Vector<Double>();
        
        Vector<Double> arcSegmentLengths = getArcSegmentLength();
        
        for(int i = 0; i < arcSegmentLengths.size(); i++){
            result.addElement(arcSegmentLengths.elementAt(i));
        }
        
        //
        Vector<Double> bendAngles = getSegmentBendAngles();
        
        int segments = result.size() / 3;
        System.out.println("  segments " + segments);
        
        for(int s = 0; s < segments; s++){ // && i < j * 3
            
            //double midLen = arcSegmentLengths.elementAt( (s * 3) + 1  );
            System.out.println("     s: " + s );
            
            
            if(s > 0){
                double angle2 = bendAngles.elementAt(s - 1);
                //System.out.println("*** angle2 to deg: " + Math.toDegrees(angle2) );
                //angle2 = Math.toDegrees(angle2);
                angle2 = (Math.PI ) - angle2;
                //System.out.println("*** angle2 to deg: " + Math.toDegrees(angle2) );
                //System.out.println("*** angle2: " + angle2 );
                
            
                double radius = getBendRadius();
                //System.out.println("radius: " + radius); // 0.1441
                double circumference = Math.PI * 2 * radius;
                //System.out.println("circumference: " + circumference );
                double bendCirc = circumference * ( angle2 /  (Math.PI * 2)  );
                //System.out.println("*** bendCirc: " + bendCirc );
                //result += bendCirc;
                //midLen += bendCirc;
                
                
                
                int indexA = ( (s - 1) * 3 ) + 2;
                int indexB = ( (s + 0) * 3 ) + 0;
                
                System.out.println( "  arc " + (bendCirc / 2) + " " + arcSegmentLengths.elementAt(indexA)    );
                
                result.setElementAt( (bendCirc / 2) , indexA);
                result.setElementAt( (bendCirc / 2) , indexB);
                
                System.out.println(" ****  add arc circ length:  " + bendCirc + "   indexA " + indexA + " indexB " +  indexB );
                
            }
        }
        
        return result;
    }
    
    
    /**
     * getSegmentRotationAngles
     *
     * Description: Get rotation values between segment sections.
     *  - Extract from ExportTubeBendGCode
     */
    public Vector<Double> getSegmentRotationAngles(ObjectInfo info){
        Vector<Double> result = new Vector<Double>();
        LayoutModeling layout = new LayoutModeling();
        
        MeshVertex[] verts = getVertices();
        for(int i = 2; i < verts.length; i++){
            Vec3 vertA = new Vec3(verts[i - 2].r.x, verts[i - 2].r.y, verts[i - 2].r.z);
            Vec3 vertB = new Vec3(verts[i - 1].r.x, verts[i - 1].r.y, verts[i - 1].r.z);
            Vec3 vertC = new Vec3(verts[i - 0].r.x, verts[i - 0].r.y, verts[i - 0].r.z);
          
            vertA.subtract(vertB);  // align to vertB
            vertC.subtract(vertB);  // align to vertB

            Vec3 vertA_ = new Vec3(verts[i - 2].r.x, verts[i - 2].r.y, verts[i - 2].r.z);

            Vec3 aTobNormal = new Vec3(vertB); // ba
            aTobNormal.subtract(vertA_);
            aTobNormal.normalize();

            Vec3 vertC_ = new Vec3(verts[i - 0].r.x, verts[i - 0].r.y, verts[i - 0].r.z);
            Vec3 bTocNormal = new Vec3(vertC_);
            bTocNormal.subtract(vertB);
            bTocNormal.normalize();

            CoordinateSystem cs1;
            cs1 = layout.getCoords(info);
            cs1.setOrigin(new Vec3(0, 0, 0));
            cs1.setOrientation(aTobNormal.x, aTobNormal.y, aTobNormal.z); // orient vector of a->b
            Mat4 mat41 = cs1.duplicate().fromLocal();
            Vec3 ccT = new Vec3(bTocNormal);
            mat41.transform(ccT); //


            double x = new Vec3(0,0,0).getAngleX(aTobNormal);
            double y = new Vec3(0,0,0).getAngleY(aTobNormal);
            double z = new Vec3(0,0,0).getAngleZ(aTobNormal);

            Vec3 bTocRotated = rotatePointX(bTocNormal, new Vec3(0,0,0), -z);
            bTocRotated = rotatePointTemp(bTocRotated, new Vec3(0,0,0), -x);

            double torsionAngle = new Vec3(0,0,0).getAngleY(bTocRotated);
            result.addElement( Math.toDegrees(torsionAngle) );
        }
        
        return result;
    }
    
    public Vec3 rotatePointX(Vec3 point, Vec3 origin, double angle){
        Vec3 rotatedPoint = new Vec3();
        rotatedPoint.x = origin.x + (point.x-origin.x) * Math.cos(angle) - (point.y - origin.y) * Math.sin(angle);
        rotatedPoint.y = origin.y + (point.x-origin.x) * Math.sin(angle) + (point.y - origin.y) * Math.cos(angle);
        rotatedPoint.z = point.z;
        return rotatedPoint;
    }
    
    public Vec3 rotatePointTemp(Vec3 point, Vec3 origin, double angle){
        double temp = point.x;
        point.x = point.z;
        point.z = temp;
        
        Vec3 rotatedPoint = new Vec3();
        rotatedPoint.x = origin.x + (point.x-origin.x) * Math.cos(angle) - (point.y - origin.y) * Math.sin(angle);
        rotatedPoint.y = origin.y + (point.x-origin.x) * Math.sin(angle) + (point.y - origin.y) * Math.cos(angle);
        rotatedPoint.z = point.z;
        
        temp = rotatedPoint.x;
        rotatedPoint.x = rotatedPoint.z;
        rotatedPoint.z = temp;
        return rotatedPoint;
    }
    
    
    /**
     * getSegmentBendAngles
     *
     * Description: Get bend angles between segment sections.
     */
    public Vector<Double> getSegmentBendAngles(){
        Vector<Double> result = new Vector<Double>();
        MeshVertex[] verts = getVertices();
        for(int i = 2; i < verts.length; i++){
            Vec3 a = verts[i - 2].r;
            Vec3 b = verts[i - 1].r;
            Vec3 c = verts[i - 0].r;
            a = a.minus(b);
            c = c.minus(b);
            double angle = a.getAngle(c);
            result.addElement(angle);
        }
        return result;
    }
    
    
    
    /**
     * OVERRIDE
     * Render this object into a ViewerCanvas.  The default implementation is sufficient for most
     * objects, but subclasses may override this to customize how they are displayed.
     *
     * @param obj      the ObjectInfo for this object
     * @param canvas   the canvas in which to render this object
     * @param viewDir  the direction from which this object is being viewed
     */
    //
    // TODO: display wireframe on morerender modes.
    //
    public void renderObject(ObjectInfo obj, ViewerCanvas canvas, Vec3 viewDir)
    {
        if(bendRadius <= 0.0){
            bendRadius = 0.01;
        }
        //System.out.println(" FiveAxisPath renderObject.  Radius: " + bendRadius);
        Color red = new Color(255, 0, 0);
        Color green = new Color(0, 255, 0);
        Color blue = new Color(0, 0, 190);
        Color lightBlue = new Color(100, 100, 255);
        Color lightGreen = new Color(50, 205, 50);
        LayoutModeling layout = new LayoutModeling();
        
        if (!obj.isVisible())
            return;
        Camera theCamera = canvas.getCamera();
        if (theCamera.visibility(obj.getBounds()) == Camera.NOT_VISIBLE)
            return;
        int renderMode = canvas.getRenderMode();
        /*
        if (renderMode == ViewerCanvas.RENDER_WIREFRAME)
        {
            //System.out.println(" RENDER_WIREFRAME ");
            canvas.renderWireframe(obj.getWireframePreview(), theCamera, ViewerCanvas.lineColor);
            return;
        }
        */
        
        Camera cam = canvas.getCamera();
        
        //canvas.renderWireframe(obj.getWireframePreview(), theCamera, ViewerCanvas.lineColor);
        
        // Tell Curve object to draw edit verticies markers if enabled.
        //((Curve)obj.object).drawEditObject(canvas);
        
        ((FiveAxisPath)obj.object).drawFirstPoint(canvas);
        
        // Draw dimension mark and annotation.
        //canvas.renderArcObject( obj, theCamera ); // renderDimensionLinearObject
        
        // renderLine(Vec3 p1, Vec3 p2, Camera cam, Color color)
        
        Mesh mesh = (Mesh) obj.getObject(); // Object3D
        Vec3 [] verts = mesh.getVertexPositions();
        //System.out.println("Mesh " + verts.length);
        
        Vec3 previousAPoint = null;
        Vec3 previousCPoint = null;
        
        for(int i = 1; i < verts.length; i++){
            Vec3 vertA = new Vec3(verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
            
            Vec3 vertB = new Vec3(verts[i - 0].x, verts[i - 0].y, verts[i - 0].z);
            
            
            Color lineColor = lightGreen;
           
            canvas.renderLine(vertA, vertB, theCamera, lineColor); // a -> b-offset //  vertA_
            
            
            
            //canvas.renderLine(new Vec3(aLenSlide3.x, aLenSlide3.y, aLenSlide3.z), new Vec3(aLenSlide3.x, aLenSlide3.y + 0.5, aLenSlide3.z), theCamera, green); // seperate Z
            //canvas.renderLine(aLenSlide, new Vec3(aLenSlide.x, aLenSlide.y + 0.4, aLenSlide.z), theCamera, XX); // old
            // x and y are correct, Z is not
        }
        
        /*
        if(verts.length == 2){
            Vec3 vertA = new Vec3(verts[0].x, verts[0].y, verts[0].z);
            Vec3 vertB = new Vec3(verts[1].x, verts[1].y, verts[1].z);
            
            Vec3 [] verts_ = new Vec3[2];
            verts_[0] = vertA;
            verts_[1] = vertB;
            renderTubeCylinder(canvas, verts_, vertA, vertB);
            canvas.renderLine(vertA, vertB, theCamera, blue);
        }
         */
    }
    
    
    /**
     * renderTubeCylendar
     *
     * Description: render illustration tube for arc profile.
     *  The scene object needs to be set specifically in this object.
     *
     *  @param canvas - Used for Camera mesh transparent render property.
     *  @param verts -
     *  @param Vec3 a -
     *  @param Vec3 b -
     */
    public void renderTubeCylinder(ViewerCanvas canvas, Vec3 [] verts, Vec3 a, Vec3 b){
        if(scene != null){
            Camera cam = canvas.getCamera();
            
            double height = a.distance(b);
            Cylinder cyl = new Cylinder(height, tubeDiameter/2, tubeDiameter/2, 1); // double height, double xradius, double yradius, double ratio
            cyl.setTexture(scene.getEnvironmentTexture(), scene.getEnvironmentMapping());
            ObjectInfo cylOI = new ObjectInfo(cyl, new CoordinateSystem(), "Render");
            
            CoordinateSystem cylCS = cylOI.getCoords();
            CoordinateSystem innerCylCS = cylOI.getCoords();
            
            
            Cylinder innerCyl = new Cylinder(height, (tubeDiameter/2) - tubeWall, (tubeDiameter/2) - tubeWall, 1);
            innerCyl.setTexture(scene.getEnvironmentTexture(), scene.getEnvironmentMapping());
            ObjectInfo innerCylOI = new ObjectInfo(innerCyl, new CoordinateSystem(), "RenderInner");
            
            
            Vec3 cylLocation = a.midPoint(b);
            cylCS.setOrigin( cylLocation );
            
            Vec3 vertAX_ = new Vec3(a); //  verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
            Vec3 vertBX_ = new Vec3(b); // verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
            vertAX_.subtract(vertBX_);
            vertAX_.normalize();
            //System.out.println("      vertAX " +vertAX );
            Vec3 zero = new Vec3(0.0, 0.0, 0.0);
            
           
            // The vector between a and b is perpendicular to the desured orientation. So we need to rotate then calculate the perp Up Dir.
            // To find a perpendicular vector for vertAX_, rotate it on the vector, then find the cross product of vertAX_ and the rotated vector.
            
            // Dot product of zero and vertAX_
            
            Vec3 rotatedNormal = new Vec3(vertAX_);                      // vertAX_ is normal vector segment direction to point
            //CoordinateSystem cs2 = new CoordinateSystem();
            
          
            // Given arbitrary x, y, then z = z2 = (-x1 * x2 - y1 * y2) / z1
            Vec3 perp1 = new Vec3(1,1,0); // X and Y are inputs, equation solves for Z.
            if(vertAX_.z == 0){
                vertAX_.z = vertAX_.z + 0.00001;
            }
            double z2 = (-vertAX_.x * perp1.x - vertAX_.y * perp1.y) / vertAX_.z;
            perp1.z = z2;
            
            rotatedNormal = perp1;
            
            //Vec3 perpendicularNormal = new Vec3(rotatedNormal);
            //double z2_ = (-rotatedNormal.x * perpendicularNormal.x - rotatedNormal.y * perpendicularNormal.y) / rotatedNormal.z;
            //perpendicularNormal.z = z2_;
            //perpendicularNormal.normalize();
            
            cylCS.setOrientation(perp1, vertAX_);
            innerCylCS.setOrientation(perp1, vertAX_);
            
            RenderingMesh rm = cyl.getRenderingMesh(0.1, true, cylOI);
            RenderingMesh innerRm = innerCyl.getRenderingMesh(0.1, true, cylOI);
            //WireframeMesh wm = cyl.getWireframeMesh();
            
            Mat4 mat4 = cylCS.duplicate().fromLocal();
            rm.transformMesh(mat4);
            innerRm.transformMesh(mat4);
            
            //wm.transformMesh(mat4);
            
            ConstantVertexShader shader = new ConstantVertexShader(ViewerCanvas.transparentColor);
            canvas.renderMeshTransparent(rm, shader, cam, vertAX_, null);
            
            if(tubeWall > 0.0){
                canvas.renderMeshTransparent(innerRm, shader, cam, vertAX_, null);
            }
            
            //VertexShader shader = new FlatVertexShader(mesh, obj.getObject(), time, obj.getCoords().toLocal().timesDirection(viewDir));
            
            //canvas.renderWireframe(wm, cam, new Color(100, 100, 100));
            
        } else {
            //System.out.println("Scene is null ");
        }
    }
    
    
    
    /**
     * getTubeCylinder
     *
     * Description: Used for notch intersection.
     *
     * @param:
     */
    public ObjectInfo getTubeCylinder(Vec3 [] verts, Vec3 a, Vec3 b, boolean outer){
        double height = a.distance(b);
        
        double radius = tubeDiameter/2;
        if(outer == false){
            radius = (tubeDiameter/2) - tubeWall;
        }
        
        Cylinder cyl = new Cylinder(height, radius, radius, 1); // double height, double xradius, double yradius, double ratio
        cyl.setTexture(scene.getEnvironmentTexture(), scene.getEnvironmentMapping());
        ObjectInfo cylOI = new ObjectInfo(cyl, new CoordinateSystem(), "Render");
        
        CoordinateSystem cylCS = cylOI.getCoords();
        
        Vec3 cylLocation = a.midPoint(b);
        cylCS.setOrigin(cylLocation);
        
        Vec3 vertAX_ = new Vec3(a); //  verts[i - 2].x, verts[i - 2].y, verts[i - 2].z);
        Vec3 vertBX_ = new Vec3(b); // verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
        vertAX_.subtract(vertBX_);
        vertAX_.normalize();
        //System.out.println("      vertAX " +vertAX );
        Vec3 zero = new Vec3(0.0, 0.0, 0.0);
        
        // The vector between a and b is perpendicular to the desured orientation. So we need to rotate then calculate the perp Up Dir.
        // To find a perpendicular vector for vertAX_, rotate it on the vector, then find the cross product of vertAX_ and the rotated vector.
        
        // Dot product of zero and vertAX_
        
        Vec3 rotatedNormal = new Vec3(vertAX_);                      // vertAX_ is normal vector segment direction to point
        //CoordinateSystem cs2 = new CoordinateSystem();
        
        // Given arbitrary x, y, then z = z2 = (-x1 * x2 - y1 * y2) / z1
        Vec3 perp1 = new Vec3(1,1,0); // X and Y are inputs, equation solves for Z.
        if(vertAX_.z == 0){
            vertAX_.z = vertAX_.z + 0.00001;
        }
        double z2 = (-vertAX_.x * perp1.x - vertAX_.y * perp1.y) / vertAX_.z;
        perp1.z = z2;
        
        rotatedNormal = perp1;
        
        //Vec3 perpendicularNormal = new Vec3(rotatedNormal);
        //double z2_ = (-rotatedNormal.x * perpendicularNormal.x - rotatedNormal.y * perpendicularNormal.y) / rotatedNormal.z;
        //perpendicularNormal.z = z2_;
        //perpendicularNormal.normalize();
        
        cylCS.setOrientation(perp1, vertAX_);
        
        RenderingMesh rm = cyl.getRenderingMesh(0.1, true, cylOI);
        //WireframeMesh wm = cyl.getWireframeMesh();
        
        Mat4 mat4 = cylCS.duplicate().fromLocal();
   //     rm.transformMesh(mat4);
        
        //wm.transformMesh(mat4);
        
        //ConstantVertexShader shader = new ConstantVertexShader(ViewerCanvas.transparentColor);
        //canvas.renderMeshTransparent(rm, shader, cam, vertAX_, null);
        
        return cylOI;
    }
    
    
    /**
     * getTubeGeometry
     *
     */
    public Vector<ObjectInfo> getTubeGeometry(ObjectInfo info){
            return getTubeGeometry(info, true);
    }
    
    
    /**
     * getTubeGeometry
     *
     * Description: Get a list of tube cylendar geometry, List of cylendars for straight segments.
     *  Used with boolean modelling to generate automatic notch profiles of intersecting tubes.
     *
     *  @param: ObjectInfo info - Coordinate system values are required for absolute location.
     *  @return Vector<ObjectInfo> - List of geometry cylendar objects representing arc shape.
     */
    public Vector<ObjectInfo> getTubeGeometry(ObjectInfo info, boolean outer){
        LayoutModeling layout = new LayoutModeling();
        Vector<ObjectInfo> result = new Vector<ObjectInfo>();
        if(bendRadius <= 0.0){
            bendRadius = 0.01;
        }
        // Vec3 [] verts
        Mesh mesh = (Mesh)info.getObject(); // Object3D ;
        Vec3 [] verts = mesh.getVertexPositions();
        //System.out.println("Mesh " + verts.length);
        CoordinateSystem c;
        c = layout.getCoords(info);
        Mat4 mat4 = c.duplicate().fromLocal();
        for(int i = 0; i < verts.length; i++){
            Vec3 vec1 = verts[i];
            mat4.transform(vec1);
        }
        Vec3 previousAPoint = null;
        Vec3 previousCPoint = null;
        for(int i = 1; i < verts.length; i++){
            Vec3 vertA = new Vec3(verts[i - 1].x, verts[i - 1].y, verts[i - 1].z);
            Vec3 vertB = new Vec3(verts[i - 0].x, verts[i - 0].y, verts[i - 0].z);
            
            ObjectInfo oi = getTubeCylinder(verts, vertA, vertB, outer);
            result.addElement(oi);
        }
        return result;
    }
    
    
    /**
     *
     */
    public double reverseAngle(double angle){
        angle = (Math.PI * 2) - angle;
        while(angle < 0){
            angle += (Math.PI * 2);
        }
        while(angle > (Math.PI * 2)){
            angle -= (Math.PI * 2);
        }
        return angle;
    }
    
    
    /**
     * writeToFile
     *
     */
    public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
    {
        super.writeToFile(out, theScene);
        int i;
        
        out.writeShort(0); // is this a delimiter or object id
        out.writeInt(vertex.length); // should always be 3
        for (i = 0; i < vertex.length; i++)
        {
            vertex[i].r.writeToFile(out);
            out.writeFloat(smoothness[i]);
        }
        out.writeBoolean(closed);
        out.writeInt(smoothingMethod);  // maybe use this method ID as a designation for dimension object
        
        out.writeDouble(bendRadius);    // Bend die radius.
        out.writeDouble(bendAngle);     // Bend angle
        out.writeDouble(tubeDiameter);  // Tube diameter
        out.writeDouble(tubeWall);      // Tube wall thickness
        out.writeDouble(minBendSpan);   // Min bend span.
        
    }
    
    public Property[] getProperties()
    {
        return (Property []) PROPERTIES.clone();
    }
    
    public Object getPropertyValue(int index)
    {
        //System.out.println("index " + index);
        if (index == 0)
        {
            if (smoothingMethod == 0)
                return PROPERTIES[0].getAllowedValues()[0];
            else
                return PROPERTIES[0].getAllowedValues()[smoothingMethod-1];
        }
        if(index == 1){
            return Boolean.valueOf(closed); //
        }
        if(index == 2){
            return bendRadius; //  Boolean.valueOf(supportMode);
        }
        if(index == 3){
            return tubeDiameter; //  Boolean.valueOf(supportMode);
        }
        if(index == 4){
            return tubeWall; //  Boolean.valueOf(supportMode);
        }
        if(index == 5){
            return minBendSpan;
        }
        return Boolean.valueOf(closed);
    }
    
    public void setPropertyValue(int index, Object value)
    {
        if (index == 0)
        {
            Object values[] = PROPERTIES[0].getAllowedValues();
            for (int i = 0; i < values.length; i++)
                if (values[i].equals(value))
                    setSmoothingMethod(i == 0 ? 0 : i+1);
        }
        else if(index == 1){
            setClosed(((Boolean) value).booleanValue());
        }
        else if(index == 2){
            bendRadius = (((Double) value).doubleValue());
        }
        else if(index == 3){
            tubeDiameter = (((Double) value).doubleValue());
        }
        else if(index == 4){
            tubeWall = (((Double) value).doubleValue());
        } else if(index == 5){
            minBendSpan = (((Double) value).doubleValue());;
        }
    }
    
    
    
    
    /** Return a Keyframe which describes the current pose of this object. */
    
    public Keyframe getPoseKeyframe()
    {
        return null; // new DimensionKeyframe(this);
    }
    
    /** Modify this object based on a pose keyframe. */
    
    public void applyPoseKeyframe(Keyframe k)
    {
        DimensionKeyframe key = (DimensionKeyframe) k;
        
        for (int i = 0; i < vertex.length; i++)
        {
            vertex[i].r.set(key.vertPos[i]);
            smoothness[i] = key.vertSmoothness[i];
        }
        cachedWire = null;
        bounds = null;
    }
    
    public boolean canConvertToActor()
    {
        return true;
    }
    
    /** Curves cannot be keyframed directly, since any change to mesh topology would
     cause all keyframes to become invalid.  Return an actor for this mesh. */
    
    public Object3D getPosableObject()
    {
        Curve m = (Curve) duplicate();
        return new Actor(m);
    }
    
    /** This class represents a pose of a Curve. */
    
    public static class DimensionKeyframe extends MeshGesture
    {
        Vec3 vertPos[];
        float vertSmoothness[];
        DimensionObject curve;
        
        public DimensionKeyframe(DimensionObject curve)
        {
            this.curve = curve;
            vertPos = new Vec3 [curve.vertex.length];
            vertSmoothness = new float [curve.vertex.length];
            for (int i = 0; i < vertPos.length; i++)
            {
                vertPos[i] = new Vec3(curve.vertex[i].r);
                vertSmoothness[i] = curve.smoothness[i];
            }
        }
        
        private DimensionKeyframe()
        {
        }
        
        /** Get the Mesh this Gesture belongs to. */
        
        protected Mesh getMesh()
        {
            return curve;
        }
        
        /** Get the positions of all vertices in this Gesture. */
        
        protected Vec3 [] getVertexPositions()
        {
            return vertPos;
        }
        
        /** Set the positions of all vertices in this Gesture. */
        
        protected void setVertexPositions(Vec3 pos[])
        {
            vertPos = pos;
        }
        
        /** Get the skeleton for this pose (or null if it doesn't have one). */
        
        public Skeleton getSkeleton()
        {
            return null;
        }
        
        /** Set the skeleton for this pose. */
        
        public void setSkeleton(Skeleton s)
        {
        }
        
        /** Create a duplicate of this keyframe. */
        
        public Keyframe duplicate()
        {
            return duplicate(curve);
        }
        
        public Keyframe duplicate(Object owner)
        {
            DimensionKeyframe k = new DimensionKeyframe();
            if (owner instanceof DimensionObject)
                k.curve = (DimensionObject) owner;
            else
                k.curve = (DimensionObject) ((ObjectInfo) owner).getObject();
            k.vertPos = new Vec3 [vertPos.length];
            k.vertSmoothness = new float [vertSmoothness.length];
            for (int i = 0; i < vertPos.length; i++)
            {
                k.vertPos[i] = new Vec3(vertPos[i]);
                k.vertSmoothness[i] = vertSmoothness[i];
            }
            return k;
        }
        
        /** Get the list of graphable values for this keyframe. */
        
        public double [] getGraphValues()
        {
            return new double [0];
        }
        
        /** Set the list of graphable values for this keyframe. */
        
        public void setGraphValues(double values[])
        {
        }
        
        /** These methods return a new Keyframe which is a weighted average of this one and one,
         two, or three others.  These methods should never be called, since Curves
         can only be keyframed by converting them to Actors. */
        
        public Keyframe blend(Keyframe o2, double weight1, double weight2)
        {
            return null;
        }
        
        public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
        {
            return null;
        }
        
        public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
        {
            return null;
        }
        
        /** Modify the mesh surface of a Gesture to be a weighted average of an arbitrary list of Gestures,
         averaged about this pose.  This method only modifies the vertex positions and texture parameters,
         not the skeleton, and all vertex positions are based on the offsets from the joints they are
         bound to.
         @param average   the Gesture to modify to be an average of other Gestures
         @param p         the list of Gestures to average
         @param weight    the weights for the different Gestures
         */
        
        public void blendSurface(MeshGesture average, MeshGesture p[], double weight[])
        {
            super.blendSurface(average, p, weight);
            DimensionKeyframe avg = (DimensionKeyframe) average;
            for (int i = 0; i < weight.length; i++)
            {
                DimensionKeyframe key = (DimensionKeyframe) p[i];
                for (int j = 0; j < vertSmoothness.length; j++)
                    avg.vertSmoothness[j] += (float) (weight[i]*(key.vertSmoothness[j]-vertSmoothness[j]));
            }
            
            // Make sure all smoothness values are within legal bounds.
            
            for (int i = 0; i < vertSmoothness.length; i++)
            {
                if (avg.vertSmoothness[i] < 0.0)
                    avg.vertSmoothness[i] = 0.0f;
                if (avg.vertSmoothness[i] > 1.0)
                    avg.vertSmoothness[i] = 1.0f;
            }
        }
        
        /** Determine whether this keyframe is identical to another one. */
        
        public boolean equals(Keyframe k)
        {
            if (!(k instanceof DimensionKeyframe))
                return false;
            DimensionKeyframe key = (DimensionKeyframe) k;
            for (int i = 0; i < vertPos.length; i++)
            {
                if (!vertPos[i].equals(key.vertPos[i]))
                    return false;
                if (vertSmoothness[i] != key.vertSmoothness[i])
                    return false;
            }
            return true;
        }
        
        /** Update the texture parameter values when the texture is changed. */
        
        public void textureChanged(TextureParameter oldParams[], TextureParameter newParams[])
        {
        }
        
        /** Get the value of a per-vertex texture parameter. */
        
        public ParameterValue getTextureParameter(TextureParameter p)
        {
            return null;
        }
        
        /** Set the value of a per-vertex texture parameter. */
        
        public void setTextureParameter(TextureParameter p, ParameterValue value)
        {
        }
        
        /** Write out a representation of this keyframe to a stream. */
        
        public void writeToStream(DataOutputStream out) throws IOException
        {
            out.writeShort(0); // version
            out.writeInt(vertPos.length);
            for (int i = 0; i < vertPos.length; i++)
            {
                vertPos[i].writeToFile(out);
                out.writeFloat(vertSmoothness[i]);
            }
        }
        
        /** Reconstructs the keyframe from its serialized representation. */
        
        public DimensionKeyframe(DataInputStream in, Object parent) throws IOException, InvalidObjectException
        {
            this();
            short version = in.readShort();
            if (version != 0)
                throw new InvalidObjectException("");
            curve = (DimensionObject) parent;
            int numVert = in.readInt();
            vertPos = new Vec3 [numVert];
            vertSmoothness = new float [numVert];
            for (int i = 0; i < numVert; i++)
            {
                vertPos[i] = new Vec3(in);
                vertSmoothness[i] = in.readFloat();
            }
        }
    }
    
    
    /**
     * drawFirstPoint
     *
     */
    public void drawFirstPoint(ViewerCanvas canvas){
        Camera theCamera = canvas.getCamera();
        MeshVertex v[] = ((Mesh) this).getVertices();
        Color col = new Color(0, 200, 200);
        Color selected_col = new Color(180, 80, 80);
        Color unselected_col = new Color(0, 255, 0);
        int HANDLE_SIZE = 5;
        boolean isSelected = false;
        int sel[];
        sel = canvas.getScene().getSelection();
        for (int i = 0; i < sel.length; i++)
        {
            ObjectInfo info = canvas.getScene().getObject(sel[i]);
            if(info.getObject() == this){
                isSelected = true;
                //System.out.println(" Selected " + info.getName() );
            }
        }
        
        if(isSelected ){ // If selected object
        
            // Iterate through curve vertecies.
            for (int i = 0; i < v.length && i < 1; i++){
                //if (selected[i] && theCamera.getObjectToView().timesZ(v[i].r) > theCamera.getClipDistance())
                //{
                if(isSelected ){ // if object selected or point in object selected.
                    Vec2 p = theCamera.getObjectToScreen().timesXY(v[i].r);
                    double z = theCamera.getObjectToView().timesZ(v[i].r);
                    
                    // if point selected, use different color.
                    //
                    //if(isObjectA && i == createPointJoin.objectAPoint){
                    //    col = selected_col;
                    //}
                    //if(isObjectB && i == createPointJoin.objectBPoint){
                    //    col = selected_col;
                    //}
                    
                    canvas.renderBox(((int) p.x) - (HANDLE_SIZE/2) - 1, ((int) p.y) - (HANDLE_SIZE/2) - 1, HANDLE_SIZE + 2, HANDLE_SIZE + 2, z, col);
                    
                    col = unselected_col;
                }
            }
        
            /*
            // Draw non movable subdivided verticies.
            // Subdivided verticies can be attached to by other object points.
            Vec3[] subdividedVerticies = getSubdividedVertices();
            if(subdividedVerticies != null && isSelected){
                //System.out.println( " draw "  );
                for (int j = 0; j < subdividedVerticies.length; j++){
                    Vec3 vec = subdividedVerticies[j];
                    Vec2 p = theCamera.getObjectToScreen().timesXY(vec);
                    double z = theCamera.getObjectToView().timesZ(vec);
                    //canvas.renderBox(((int) p.x) - HANDLE_SIZE/2, ((int) p.y) - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE, z, col);
                    canvas.drawLine(new Point((int)p.x - HANDLE_SIZE/2, (int)p.y - HANDLE_SIZE/2), new Point((int) p.x + HANDLE_SIZE/2, (int)p.y - HANDLE_SIZE/2), unselected_col); // bot
                    canvas.drawLine(new Point((int)p.x - HANDLE_SIZE/2, (int)p.y - HANDLE_SIZE/2), new Point((int) p.x - HANDLE_SIZE/2, (int)p.y + HANDLE_SIZE/2), unselected_col); // left
                    canvas.drawLine(new Point((int)p.x + HANDLE_SIZE/2, (int)p.y + HANDLE_SIZE/2), new Point((int) p.x + HANDLE_SIZE/2, (int)p.y - HANDLE_SIZE/2), unselected_col); // right
                    canvas.drawLine(new Point((int)p.x - HANDLE_SIZE/2, (int)p.y + HANDLE_SIZE/2), new Point((int) p.x + HANDLE_SIZE/2, (int)p.y + HANDLE_SIZE/2), unselected_col); // top
                }
            }
             */
        }
    }
    
    public void setBendRadius(double v){
        bendRadius = v;
    }
    
    public double getBendRadius(){
        return bendRadius;
    }
    
    public void setTubeDiameter(double v){
        tubeDiameter = v;
    }
    
    public double getTubeDiameter(){
        return tubeDiameter;
    }
    
    public void setTubeWall(double v){
        tubeWall = v;
    }
    
    public double getTubeWall(){
        return tubeWall;
    }
    
    public void setMinBendSpan(double v){
        minBendSpan = v;
    }
    
    public double getMinBendSpan(){
        return minBendSpan;
    }
    
    public void setScene(Scene s){
        this.scene = s;
    }
    
    public Scene getScene(){
        return scene;
    }
}
