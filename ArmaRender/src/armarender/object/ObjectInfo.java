/* Copyright (C) 2019-2023 by Jon Taylor
   Copyright (C) 1999-2013 by Peter Eastman
   
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.object;

import armarender.*;
import armarender.animation.*;
import armarender.animation.distortion.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import java.lang.ref.*;
import java.util.*;

/** ObjectInfo represents information about an object within a Scene: its position,
    orientation, name, visibility, etc.  The internal properties (i.e. geometry) of
    the object are defined by the "object" property.
    <p>
    There may be several ObjectInfos in a scene which all reference
    the same Object3D.  In that case, they are live duplicates of each other. */

public class ObjectInfo implements Comparable<ObjectInfo>
{
  public Object3D object;
  public CoordinateSystem coords;
  public CoordinateSystem coords_layout = null; // is cache object

  public String name;
  public boolean visible, selected, parentSelected;
  public ObjectInfo parent, children[];
  public Track tracks[];
  public Keyframe pose;
  public int id;
  private boolean locked;
  private Distortion distortion, prevDistortion;
  private SoftReference<RenderingMesh> cachedMesh;
  private SoftReference<WireframeMesh> cachedWire;
  private BoundingBox cachedBounds;
  private boolean lastPreviewWasWireframe;
  private boolean layoutViewModel = true;
  private boolean tubeLayoutViewModel = false;
    
  private boolean childrenHiddenWhenHidden = false;
  private int displayModeOverride = 0;       // [wireframe, solid, textured, ...]
  private String groupName = "";
  private double layout_origin_x = 0;
  private double layout_origin_y = 0;
  private double layout_origin_z = 0;
  private double layout_zDir_x = 0;
  private double layout_zDir_y = 0;
  private double layout_zDir_z = 0;
  private double layout_upDir_x = 0;
  private double layout_upDir_y = 0;
  private double layout_upDir_z = 0;
    
  // CAM properties
    private boolean cncDisabled = false;
    private int cncPointOffset = 0;
    private int cncPolyOrder = 0;               // Cant set default by size here as the geometry hasn't been defined yet.
    private double cncPolyDepth = 0;
    private boolean cncReversePointOrder = false;
    
  // FEA properties
    private double feaMPa = 0;
    
  // Pair align move highlight object to be moved.
  private boolean renderMoveHighlight = false;
    
    private int replicateId = -1;               // If set, this object uses the replicate id object for mirroring or copy.
    private boolean replicateEnabled = true;    // toggle enable disable mirror plane replication on this object.
    
    private int materialId = -1;                // Physical material [1026 Steel, 4030, Steel, 6061 Aluminum]
    
    private Vector<String> debugInfo = new Vector<String>();
    
  /** Create a new ObjectInfo. */

  public ObjectInfo(Object3D obj, CoordinateSystem c, String name)
  {
    setObject(obj);
    setCoords(c);
    this.setName(name);
    setVisible(true);
    children = new ObjectInfo [0];
    setId(-1);
      
    //obj.name = name;
    //if(cncPolyOrder == 0){
    //    cncPolyOrder = getDefaultPolyOrder(); // set default based on size once object geometry is available.
    //}
      //setCncPolyOrder(obj.getCncPolyOrder());
  }

  /** Create a new ObjectInfo which is identical to this one.  It will still reference the
      same Object3D object, but all other fields will be cloned. */

  public ObjectInfo duplicate()
  {
    return duplicate(getObject());
  }

  /** Create a new ObjectInfo which is identical to this one, but references a new Object3D. */

  public ObjectInfo duplicate(Object3D obj)
  {
    ObjectInfo info = new ObjectInfo(obj, getCoords().duplicate(), getName());

    info.setVisible(isVisible());
    info.setChildrenHiddenWhenHidden(isChildrenHiddenWhenHidden());
    info.setLocked(isLocked());
    info.setId(id);
    if (getTracks() != null)
      {
        info.tracks = new Track [getTracks().length];
        for (int i = 0; i < getTracks().length; i++)
          info.getTracks()[i] = getTracks()[i].duplicate(info);
      }
    if (distortion != null)
      info.distortion = distortion.duplicate();
    return info;
  }

  /** Given an array of ObjectInfos, duplicate all of them (including the objects they
      point to), keeping parent-child relationships intact. */

  public static ObjectInfo [] duplicateAll(ObjectInfo info[])
  {
    ObjectInfo newobj[] = new ObjectInfo [info.length];
    HashMap<ObjectInfo, ObjectInfo> objectMap = new HashMap<ObjectInfo, ObjectInfo>();
    for (int i = 0; i < newobj.length; i++)
    {
      newobj[i] = info[i].duplicate(info[i].getObject().duplicate());
      objectMap.put(info[i], newobj[i]);
    }
    for (int i = 0; i < info.length; i++)
      for (int k = info[i].getChildren().length-1; k >= 0; k--)
        {
          int j;
          for (j = 0; j < info.length && info[j] != info[i].getChildren()[k]; j++);
          if (j < info.length)
            newobj[i].addChild(newobj[j], 0);
        }
    for (int i = 0; i < newobj.length; i++)
      if (newobj[i].tracks != null)
        for (int j = 0; j < newobj[i].tracks.length; j++)
          newobj[i].tracks[j].updateObjectReferences(objectMap);
    return newobj;
  }

  /** Make this ObjectInfo identical to another one.  Both ObjectInfos will reference the
      same Object3D object, but all other fields will be cloned. */

  public void copyInfo(ObjectInfo info)
  {
    setObject(info.getObject());
    getCoords().copyCoords(info.getCoords());
    setName(info.name);
    setVisible(info.visible);
    setChildrenHiddenWhenHidden(info.isChildrenHiddenWhenHidden());
    setLocked(info.locked);
    setId(info.id);
    cachedMesh = info.cachedMesh;
    cachedWire = info.cachedWire;
    cachedBounds = info.cachedBounds;
      
    cncPolyOrder = info.getCncPolyOrder();
      
    if (info.getTracks() == null)
      tracks = null;
    else
      {
        tracks = new Track [info.getTracks().length];
        for (int i = 0; i < getTracks().length; i++)
          getTracks()[i] = info.getTracks()[i].duplicate(this);
      }
    if (info.distortion != null)
      distortion = info.distortion.duplicate();
    if (info.prevDistortion != null)
      prevDistortion = info.prevDistortion.duplicate();
  }

  /** Add a child to this object. */

  public void addChild(ObjectInfo info, int position)
  {
      
      // Check if child allready exists, Don't add duplicates.
      for(int i = 0; i < getChildren().length; i++){
          ObjectInfo child = children[i];
          if(info == child){
              System.out.println("Error: ObjectInfo.addChild() adding duplicate child: " + getName() + " -> " + info.getName());
              return;
          }
      }
      
      // Bounds check indexes.
      if(position >= getChildren().length){
          //System.out.println("OH OH 1");
          position = getChildren().length -1;
      }
      if(position < 0){
          //System.out.println("OH OH 2");
          position = 0;
      }
      
    ObjectInfo newChildren[] = new ObjectInfo [getChildren().length+1];
    int i;

    for (i = 0; i < position; i++){
      newChildren[i] = getChildren()[i];
    }
      
    newChildren[position] = info;
    for (; i < getChildren().length; i++){
      newChildren[i+1] = getChildren()[i];
    }
      
    children = newChildren;
    info.setParent(this);
  }

  /** Remove a child from this object. */

  public void removeChild(ObjectInfo info)
  {
    for (int i = 0; i < getChildren().length; i++)
      if (getChildren()[i] == info)
        {
          removeChild(i);
          return;
        }
  }

  /** Remove a child from this object. */

  public void removeChild(int which)
  {
    ObjectInfo newChildren[] = new ObjectInfo [getChildren().length-1];
    int i;

    getChildren()[which].setParent(null);
    for (i = 0; i < which; i++)
      newChildren[i] = getChildren()[i];
    for (i++; i < getChildren().length; i++)
      newChildren[i-1] = getChildren()[i];
    children = newChildren;
  }

  /** Add a track to this object. */

  public void addTrack(Track tr, int position)
  {
    if (getTracks() == null)
      {
        tracks = new Track [] {tr};
        return;
      }
    Track newTracks[] = new Track [getTracks().length+1];
    int i;

    for (i = 0; i < position; i++)
      newTracks[i] = getTracks()[i];
    newTracks[position] = tr;
    for (; i < getTracks().length; i++)
      newTracks[i+1] = getTracks()[i];
    tracks = newTracks;
  }

  /** Remove a track from this object. */

  public void removeTrack(Track tr)
  {
    for (int i = 0; i < getTracks().length; i++)
      if (getTracks()[i] == tr)
        {
          removeTrack(i);
          return;
        }
  }

  /** Remove a track from this object. */

  public void removeTrack(int which)
  {
    Track newTracks[] = new Track [getTracks().length-1];
    int i;

    for (i = 0; i < which; i++)
      newTracks[i] = getTracks()[i];
    for (i++; i < getTracks().length; i++)
      newTracks[i-1] = getTracks()[i];
    tracks = newTracks;
  }

  /** Set the texture and texture mapping for this object. */

  public void setTexture(Texture tex, TextureMapping map)
  {
    getObject().setTexture(tex, map);
    clearCachedMeshes();

    // Update any texture tracks.

    if (getTracks() != null)
      for (int i = 0; i < getTracks().length; i++)
        if (getTracks()[i] instanceof TextureTrack)
          ((TextureTrack) getTracks()[i]).parametersChanged();
  }

  /** Set the material and material mapping for this object. */

  public void setMaterial(Material mat, MaterialMapping map)
  {
    getObject().setMaterial(mat, map);
    clearCachedMeshes();
  }

  /** Remove any Distortions from the object. */

  public void clearDistortion()
  {
    distortion = null;
  }

  /** Add a Distortion to apply to the object.  Any other Distortions which
      have previously been added will be applied before this one. */

  public void addDistortion(Distortion d)
  {
    d.setPreviousDistortion(distortion);
    distortion = d;
  }

  /** Get the current Distortion applied to this object. */

  public Distortion getDistortion()
  {
    return distortion;
  }

  /** Returns true if a Distortion has been applied to this object. */

  public boolean isDistorted()
  {
    return (distortion != null);
  }

  /** Set the current Distortion applied to this object.  Any previously applied Distortion is discarded. */

  public void setDistortion(Distortion d)
  {
    distortion = d;
  }

  /** See if the Distortion has changed, and clear the cached meshes if it has. */

  private void checkDistortionChanged()
  {
    if ((prevDistortion == distortion) ||
        (distortion != null && distortion.isIdenticalTo(prevDistortion)))
      return;
    prevDistortion = distortion;
    clearCachedMeshes();
  }

  /** Get a new object which has had the distortion applied to it.  If there is no distortion,
      this simply returns the original object. */

  public Object3D getDistortedObject(double tol)
  {
    if (distortion == null)
      return getObject();
    Object3D obj = getObject();
    while (obj instanceof ObjectWrapper)
      obj = ((ObjectWrapper) obj).getWrappedObject();
    if (!(obj instanceof Mesh) && getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT)
      obj = obj.convertToTriangleMesh(tol);
    if (obj instanceof Mesh)
      obj = (Object3D) distortion.transform((Mesh) obj);
    return obj;
  }

  /** Get a rendering mesh for this object. */

  public RenderingMesh getRenderingMesh(double tol)
  {
    return getDistortedObject(tol).getRenderingMesh(tol, false, this);
  }

  /** Get a rendering mesh for interactive previews. */

  public RenderingMesh getPreviewMesh()
  {
    checkDistortionChanged();
    RenderingMesh cached = null;
    if (cachedMesh != null)
      cached = cachedMesh.get();
    if (cached == null)
      {
        if (getPose() != null && !getPose().equals(getObject().getPoseKeyframe()))
          getObject().applyPoseKeyframe(getPose());
        double tol = ArmaRender.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cached = obj.getRenderingMesh(tol, true, this);
        cachedMesh = new SoftReference<RenderingMesh>(cached);
        if (cachedBounds == null)
          cachedBounds = obj.getBounds();
      }
    lastPreviewWasWireframe = false;
    return cached;
  }

  /** Get a wireframe mesh for interactive previews. */

  public WireframeMesh getWireframePreview()
  {
    checkDistortionChanged();
    WireframeMesh cached = null;
    if (cachedWire != null)
      cached = cachedWire.get();
    if (cached == null)
      {
          if (getPose() != null && !getPose().equals(getObject().getPoseKeyframe())){
              getObject().applyPoseKeyframe(getPose());
          }
        double tol = ArmaRender.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cached = obj.getWireframeMesh();
        cachedWire = new SoftReference<WireframeMesh>(cached);
          if (cachedBounds == null){
              cachedBounds = obj.getBounds();
          }
      }
    lastPreviewWasWireframe = true;
    return cached;
  }

  /** Get a bounding box for the object.  The bounding box is defined in the object's local coordinate system. */

  public BoundingBox getBounds()
  {
    checkDistortionChanged();
    if (cachedBounds == null)
      {
        if (getPose() != null && !getPose().equals(getObject().getPoseKeyframe()))
          getObject().applyPoseKeyframe(getPose());
        double tol = ArmaRender.getPreferences().getInteractiveSurfaceError();
        Object3D obj = getDistortedObject(tol);
        cachedBounds = obj.getBounds();
        Object3D realObject = getObject();
        while (realObject instanceof ObjectWrapper)
          realObject = ((ObjectWrapper) realObject).getWrappedObject();
        if (!(realObject instanceof ObjectCollection))
        {
          if (lastPreviewWasWireframe && cachedWire == null)
            cachedWire = new SoftReference<WireframeMesh>(obj.getWireframeMesh());
          else if (!lastPreviewWasWireframe && cachedMesh == null)
            cachedMesh = new SoftReference<RenderingMesh>(obj.getRenderingMesh(tol, true, this));
        }
      }
    return cachedBounds;
  }

  /** Clear the cached preview meshes.  This should be called whenever the object is changed. */

  public void clearCachedMeshes()
  {
    cachedMesh = null;
    cachedWire = null;
    cachedBounds = null;
  }

  /** Get the skeleton for this object, or null if it does not have one. */

  public Skeleton getSkeleton()
  {
    return getObject().getSkeleton();
  }

  /** Get the Object3D defining the geometry for this ObjectInfo. */

  public Object3D getObject()
  {
    return object;
  }

  /** Set the Object3D defining the geometry for this ObjectInfo. */

  public void setObject(Object3D object)
  {
    this.object = object;
  }



  /**
   * getCoords
   *
   * Get the CoordinateSystem for this object. Different coord values are returned based on the layout/modeling view.
   *
   */
  public CoordinateSystem getCoords()
  {
	  LayoutModeling layout = new LayoutModeling();
	  
	  //Scene theScene = getObject().canvas.getScene();
	  //layout.setBaseDir(theScene.getDirectory() + "\\" + theScene.getName() + "_layout_data" );


	  // JDT return layout or modeling coords...
      
      if(tubeLayoutViewModel == true){
          // Load transform from parent.
          // work in progress
          //System.out.println(" object tubeLayoutViewModel ");
          
          // Read cutting layout coords.
          if(coords_layout == null){
              //System.out.println("read object coords.");
              coords_layout = new CoordinateSystem();
              coords_layout = layout.getCoords(this);
              
              
              // coords_layout = coords_layout * coords
              
              // transformAxes
              // transformOrigin
              // transformCoordinates
              
              // rotMatrix = Mat4.translation(origin.x, origin.y, origin.z).times(Mat4.axisRotation(rotAxis, angle)).times(Mat4.translation(-origin.x, -origin.y, -origin.z));
              
              //c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
              //coords_layout.transformCoordinates( coords.fromLocal() );
              
              //CoordinateSystem coords_temp = coords.duplicate();
              //coords_temp.transformCoordinates( coords_layout.fromLocal() );
              //coords_layout = coords_temp;
              
              //CoordinateSystem coords_temp = coords.duplicate();
              //coords_temp.transformCoordinates( coords_layout.toLocal() );
              //coords_layout = coords_temp;
              
          }
          return coords_layout;
      } if( layoutViewModel == false){ // Layout view
		
		return getLayoutCoords();
        
	} else {	// Modeling view
		coords_layout = null;
	    return coords;
	}
  }


  public void resetLayoutCoords(CoordinateSystem coords){
  	coords_layout = coords;
  }
    

    public CoordinateSystem getLayoutCoords()
    {
        LayoutModeling layout = new LayoutModeling();
        // Read cutting layout coords.
        if(coords_layout == null){
            //System.out.println("read object coords.");
            coords_layout = new CoordinateSystem();
            coords_layout = layout.getCoords(this);
        }
        return coords_layout;
    }
    
    public CoordinateSystem getModelingCoords()
    {
        return coords;
    }

  /** Set the CoordinateSystem for this object. */

  public void setCoords(CoordinateSystem coords)
  {
    this.coords = coords;
  }

  /** Get the name of this object. */

  public String getName()
  {
    return name;
  }

  /** Set the name of this object. */

  public void setName(String name)
  {
    this.name = name;
  }

  /** Get whether this object is visible. */

  public boolean isVisible()
  {
    return visible;
  }

  /** Set whether this object is visible. */

  public void setVisible(boolean visible)
  {
    this.visible = visible;
      // mirrorMesh.updateVisibility(this);
  }

    
    // rename to: isHiddenByParent  ???
    public boolean isHiddenByParent(){
        return childrenHiddenWhenHidden;
    }
    public boolean isChildrenHiddenWhenHidden() // isChildrenHidden
    {
      return childrenHiddenWhenHidden;
    }
    
    public void setChildrenHiddenWhenHidden(boolean c)
    {
      this.childrenHiddenWhenHidden = c;
        // mirrorMesh.updateVisibility(this);
    }
    
    public void toggleChildrenHidden(){
        
    }
    
  /** Get whether this object is locked. */

  public boolean isLocked()
  {
    return locked;
  }

  /** Set whether this object is locked. */

  public void setLocked(boolean locked)
  {
    this.locked = locked;
  }

  /** Get this object's parent, or null if it is a top level object. */

  public ObjectInfo getParent()
  {
    return parent;
  }

  /** Set this object's parent. */

  public void setParent(ObjectInfo parent)
  {
      
      // Debug find out if this is cause of errors.
      String existingParentName = "null";
      String replacedParentName = "null";
      if(this.parent != null){
          existingParentName = this.parent.getName();
      }
      if(parent != null){
          replacedParentName = parent.getName();
      }
      if(this.parent != null){
          System.out.println(" *** ObjectInfo: " + this.getName() + "(" + this.getId() + ")"  +
                             "    replace parent: " + existingParentName +
                             " with: -> " + replacedParentName );
      
          if(this.parent != null && parent != null &&  this.parent.getId() != parent.getId() ){
              System.out.println("     --- ObjectInfo.setParent changed parent ---  ");
          }
      }
      
    this.parent = parent;
  }

  /** Get the current pose for this object (may be null). */

  public Keyframe getPose()
  {
    return pose;
  }

  /** Set the current pose for this object (may be null). */

  public void setPose(Keyframe pose)
  {
    this.pose = pose;
  }

  /** Get this object's ID. */

  public int getId()
  {
    return id;
  }

  /** Set this object's ID. */

  public void setId(int id)
  {
    this.id = id;
  }
    
    /**
     * getReplicateId
     *
     * Description: Replicate id (default -1) is used to reference another object for geometry to copy or mirror.
     */
    public int getReplicateId(){
        return replicateId;
    }
    
    /**
     * setReplicateId
     *
     * Description: Replicate id (default -1) is used to reference another object for geometry to copy or mirror.
     */
    public void setReplicateId(int id){
        this.replicateId = id;
    }
    
    public boolean isReplicateEnabled(){
        return replicateEnabled;
    }
    
    public void setReplicateEnabled(boolean enabled){
        this.replicateEnabled = enabled;
    }

  /**
   * getChildren()
   *
   * Get the list of children for this object.
   *
   * @return array of child objects.
   */
  public ObjectInfo[] getChildren()
  {
    return children;
  }
    
    /**
     * getRecursiveChildren
     *
     *  Descriotion: return a list of all children in child objects.
     *
     */
    public Vector<ObjectInfo> getRecursiveChildren(){
        Vector<ObjectInfo> returnChildren = new Vector<ObjectInfo>();
        ObjectInfo[] c = getChildren();
        for(int i = 0; i < c.length; i++){
            
            returnChildren.addElement(c[i]);
            
            returnChildren.addAll(c[i].getRecursiveChildren());
        }
        return returnChildren;
    }

  /** Get the list of Tracks for this object. */

  public Track[] getTracks()
  {
    return tracks;
  }


    /**
     * setLayoutView - rename to mode?
     *
     * Description:
     */
	public void setLayoutView(boolean layout){
		layoutViewModel = layout;
        
        // JDT initalize layout
        if( layout == false ){ // Set to layout view
            //System.out.println("Object setLayoutView . ");
            LayoutModeling layoutModeling = new LayoutModeling();
            CoordinateSystem layoutCoords = layoutModeling.getCoords(this);
            if(layoutCoords == null){
                //System.out.println("Object setLayoutView SAVE DEFAULT ");
                //CoordinateSystem cx = coords; // this.getCoords();
                layoutModeling.saveLayout(this, coords);
            }
        }
	}

	public boolean getLayoutView(){
		return layoutViewModel;
	}
    
    public void setTubeLayoutView(boolean layout){
        tubeLayoutViewModel = layout;
        
        // ***
        // Initalize if not set
        //LayoutModeling layoutModeling = new LayoutModeling();
        //CoordinateSystem c = layoutModeling.getCoords(this);
        //CoordinateSystem cx = this.getCoords();
        
        //System.out.println("ObjectInfo "+this.getName()+"  x " + c.getOrigin().x );
        
        if(tubeLayoutViewModel == true || layoutViewModel == true){
            
        }
        
    }
    
    public boolean getTubeLayoutView(){
        return tubeLayoutViewModel;
    }

    public void setGroupName(String name){
        groupName = name;
    }
    
    public String getGroupName(){
        return groupName;
    }
    
    public double getLayoutOriginX(){
        return layout_origin_x;
    }
    public void setLayoutOriginX(double x){
        this.layout_origin_x = x;
    }
    
    public double getLayoutOriginY(){
        return layout_origin_y;
    }
    public void setLayoutOriginY(double y){
        this.layout_origin_y = y;
    }
    
    public double getLayoutOriginZ(){
        return layout_origin_z;
    }
    public void setLayoutOriginZ(double z){
        this.layout_origin_z = z;
    }
    
   
    public double getLayoutZDirX(){
        return layout_zDir_x;
    }
    public void setLayoutZDirX(double x){
        this.layout_zDir_x = x;
    }
    
    public double getLayoutZDirY(){
        return layout_zDir_y;
    }
    public void setLayoutZDirY(double y){
        this.layout_zDir_y = y;
    }
    
    public double getLayoutZDirZ(){
        return layout_zDir_z;
    }
    public void setLayoutZDirZ(double z){
        this.layout_zDir_z = z;
    }
    
    
    public double getLayoutUpDirX(){
        return layout_upDir_x;
    }
    public void setLayoutUpDirX(double x){
        this.layout_upDir_x = x;
    }
    
    public double getLayoutUpDirY(){
        return layout_upDir_y;
    }
    public void setLayoutUpDirY(double y){
        this.layout_upDir_y = y;
    }
    
    public double getLayoutUpDirZ(){
        return layout_upDir_z;
    }
    public void setLayoutUpDirZ(double z){
        this.layout_upDir_z = z;
    }
    
    
    
    
    public boolean getCncDisabled(){
        return cncDisabled;
    }
    public void setCncDisabled(boolean z){
        this.cncDisabled = z;
    }
    
    public int getCncPointOffset(){
        return cncPointOffset;
    }
    public void setCncPointOffset(int o){
        this.cncPointOffset = o;
    }
    
    public int getCncPolyOrder(){
        return cncPolyOrder;
    }
    public void setCncPolyOrder(int o){
        this.cncPolyOrder = o;
    }
      
      public double getCncPolyDepth(){
          return cncPolyDepth;
      }
      public void setCncPolyDepth(double d){
          this.cncPolyDepth = d;
      }
    
    public boolean getCncReversePointOrder(){
        return cncReversePointOrder;
    }
    public void setCncReversePointOrder(boolean d){
        this.cncReversePointOrder = d;
    }
    
    public double getFeaMPa(){
        return feaMPa;
    }
    public void setFeaMPa(double d){
        this.feaMPa = d;
    }
    
    public int getPhysicalMaterialId(){
        return materialId;
    }
    
    public void setPhysicalMaterialId(int id){
        this.materialId = id;
    }
    
    public int getDisplayModeOverride(){
        return displayModeOverride;
    }
    public void setDisplayModeOverride(int m){
        this.displayModeOverride = m;
    }
    
    
    
    public boolean getRenderMoveHighlight(){
        return renderMoveHighlight;
    }
    public void setRenderMoveHighlight(boolean d){
        this.renderMoveHighlight = d;
    }
    
    /**
    * getBounds
    *
    * Description: ObjectInfo.getBounds doesn't apply transfomations making its results inaccurate.
     @ return: BoundingBox - with values having ObjectInfo CoordinateSystem transform applied to make it world coordinates.
    */
    public BoundingBox getTranslatedBounds(){ // ObjectInfo object
        BoundingBox bounds = new BoundingBox(0,0,0,0,0,0); //  objectBoundsCache.get(object);
        //if(bounds != null){
        //    return bounds;
        //}
        LayoutModeling layout = new LayoutModeling();
        Object3D o3d = this.getObject().duplicate();
        bounds = o3d.getBounds();           // THIS DOES NOT WORK
        
        // Resolves issues with MoveObjectTool rotate on these objects breaks bounds.
        if(getObject() instanceof DirectionalLight || getObject() instanceof SceneCamera ){
            return bounds;
        }
        
        //bounds.minx = Float.MAX_VALUE; bounds.maxx = Float.MIN_VALUE;
        //bounds.miny = Float.MAX_VALUE; bounds.maxy = Float.MIN_VALUE;
        //bounds.minz = Float.MAX_VALUE; bounds.maxz = Float.MIN_VALUE;
        
        if(bounds == null){
            bounds = new BoundingBox(0,0,0,0,0,0);
        }
        bounds.minx = 999; bounds.maxx = -999;  // TODO: use max values.
        bounds.miny = 999; bounds.maxy = -999;
        bounds.minz = 999; bounds.maxz = -999;
        
        CoordinateSystem c;
        c = layout.getCoords(this);
        //Vec3 objOrigin = c.getOrigin();
        
        if((o3d instanceof Mesh) == false){ // Not mesh but geometric object.
            if( o3d.canConvertToTriangleMesh() != Object3D.CANT_CONVERT){
                o3d = o3d.convertToTriangleMesh(0.01); //
            }
        }
        
        if((o3d instanceof Mesh) == true){
            Mesh mesh = (Mesh) o3d; // obj.getObject(); // Object3D
            Vec3 [] verts = mesh.getVertexPositions();
            for(int i = 0; i < verts.length; i++){
                Vec3 point = verts[i];
                Mat4 mat4 = c.duplicate().fromLocal();
                mat4.transform(point);
                if(point.x < bounds.minx){
                    bounds.minx = point.x;
                }
                if(point.x > bounds.maxx){
                    bounds.maxx = point.x;
                }
                if(point.y < bounds.miny){
                    bounds.miny = point.y;
                }
                if(point.y > bounds.maxy){
                    bounds.maxy = point.y;
                }
                if(point.z < bounds.minz){
                    bounds.minz = point.z;
                }
                if(point.z > bounds.maxz){
                    bounds.maxz = point.z;
                }
            }
        } else if( o3d instanceof SceneCamera || o3d instanceof DirectionalLight ){  // BUG????
            //System.out.println("ObjectInfo.getTranslatedBounds() SceneCamera or DirectionalLight:  ");
            //bounds = new BoundingBox(0,0,0,0,0,0);
            bounds = o3d.getBounds();
        } else {
            System.out.println("Error: ObjectInfo.getTranslatedBounds(): " + o3d.getClass().getName() );
        }
        //objectBoundsCache.put(object, bounds);
        return bounds;
    }
    
    
    /**
     * getTop
     * Description: Get the top most vec point for this object.
     */
    public Vec3 getTopVec3(){
        LayoutModeling layout = new LayoutModeling();
        CoordinateSystem coords = this.getCoords();
        Vec3 topVec = new Vec3(coords.getOrigin());
        Object3D o3d = this.getObject().duplicate();
        double maxValue = -9999;
        CoordinateSystem c;
        c = layout.getCoords(this);
        Vec3 objOrigin = c.getOrigin();
        if((o3d instanceof Mesh) == true){
            Mesh mesh = (Mesh) o3d; // obj.getObject(); // Object3D
            Vec3 [] verts = mesh.getVertexPositions();
            for(int i = 0; i < verts.length; i++){
                Vec3 point = verts[i];
                Mat4 mat4 = c.duplicate().fromLocal();
                mat4.transform(point);
                if(point.y > maxValue){
                    maxValue = point.y;
                    
                    topVec = new Vec3(point);
                    System.out.println("+ " + maxValue);
                }
            }
        }
        //System.out.println(" ");
        return topVec;
    }
    
    public Vec3 getBottomVec3(){
        LayoutModeling layout = new LayoutModeling();
        CoordinateSystem coords = this.getCoords();
        Vec3 botVec = new Vec3(coords.getOrigin());
        Object3D o3d = this.getObject().duplicate();
        double minValue = 9999;
        CoordinateSystem c;
        c = layout.getCoords(this);
        Vec3 objOrigin = c.getOrigin();
        if((o3d instanceof Mesh) == true){
            Mesh mesh = (Mesh) o3d; // obj.getObject(); // Object3D
            Vec3 [] verts = mesh.getVertexPositions();
            for(int i = 0; i < verts.length; i++){
                Vec3 point = verts[i];
                Mat4 mat4 = c.duplicate().fromLocal();
                mat4.transform(point);
                if(point.y < minValue){
                    minValue = point.y;
                    
                    botVec = new Vec3(point);
                    System.out.println("- " + minValue);
                }
            }
        }
        //System.out.println(" ");
        return botVec;
    }
    
    
    /**
     * getDefaultPolyOrder
     *
     * Description: Get the size of the object bounds for a general default cut order.
     *  We want smaller objects cut first in case they exist within larger objects as once a part has
     *  been cut free from the material it is no longer fixed in place by bracing.
     *
     *  NOTE: method replaced by menu based for setting all object poly order by size.
     *
     *  @return poly order. order to cut parts.
     */
    public int getDefaultPolyOrder(){
        int order = 0;
        
        // get bounds
        if(this.getObject() != null){
            Object3D o3d = this.getObject().duplicate();
            BoundingBox bounds = o3d.getBounds();
            // create an integer based on the object size.
            order = (int)(
                          (bounds.maxx - bounds.minx) * (bounds.maxy - bounds.miny) * (bounds.maxz - bounds.minz)
                         );
            
            //System.out.println("getDefaultPolyOrder " + (bounds.maxx - bounds.minx) );
        }
        return order;
    }
    
    
    // add to debugInfo
    public void addDebugInfo(String line){
        debugInfo.addElement(line);
        if(debugInfo.size() > 1000){
            debugInfo.removeElementAt(0);
        }
    }
    
    public Vector<String> getDebugInfo(){
        return debugInfo;
    }
    
    
    @Override
      public int compareTo(ObjectInfo u) {
          Integer a = getCncPolyOrder();
          Integer b = u.getCncPolyOrder();
          if(a == 0){
              a = getDefaultPolyOrder();
          }
          if(b == 0){
              b = u.getDefaultPolyOrder();
          }
          return a.compareTo(b);
      }


	/**
     * getDecendantChildren
     * Description: Returns all of the children under this info.
     */
    public Vector<ObjectInfo> getDecendantChildren(ObjectInfo info){
        Vector<ObjectInfo> results = new Vector<ObjectInfo>();
        for(int i = 0; i < info.getChildren().length; i++){
            ObjectInfo child = info.getChildren()[i];
            results.addElement(child);
            Vector<ObjectInfo> decendants = getDecendantChildren(child);
            for(int d = 0; d < decendants.size(); d++){
                ObjectInfo decendant = decendants.elementAt(d);
                if(results.contains(decendant) == false){
                    results.addElement(decendant);
                }
            }
            //results.addAll();
        }
        return results;
    }

    
}
