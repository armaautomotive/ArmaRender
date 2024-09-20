/* Copyright (C) 2019-2023 by Jon Taylor
   Copyright (C) 1999-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.animation.*;
import armarender.image.*;
import armarender.material.*;
import armarender.math.*;
import armarender.object.*;
import armarender.texture.*;
import armarender.ui.*;
import armarender.util.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.beans.*;
import armarender.object.TriangleMesh.*;
import javax.swing.*; // For JOptionPane
import buoy.event.*;
import buoy.widget.*; // runCrashSimulation
import java.math.BigDecimal;
import java.text.DecimalFormat;
import com.jsevy.jdxf.*;
//import eu.mihosoft.jcsg.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.RoundingMode;

/** The Scene class describes a collection of objects, arranged relative to each other to
    form a scene, as well as the available textures and materials, environment options, etc. */

public class Scene
{
  private Vector<ObjectInfo> objects;
  private Vector<Material> materials;
  private Vector<Texture> textures;
  private Vector<ImageMap> images;
  private Vector<Integer> selection;
  private Vector<ListChangeListener> textureListeners, materialListeners;
  private HashMap<String, Object> metadataMap;
  private HashMap<ObjectInfo, Integer> objectIndexMap;
  private RGBColor ambientColor, environColor, fogColor;
  private Texture environTexture;
  private TextureMapping environMapping;
  private int gridSubdivisions, environMode, framesPerSecond, nextID;
  private double fogDist, gridSpacing, time;
  private boolean fog, showGrid, snapToGrid, errorsLoading;
  private String name, directory;
  private TexturesAndMaterialsDialog texDlg;
  private ParameterValue environParamValue[];
  private StringBuffer loadingErrors;
  private double scale;
  private int units; // design units
    private int exportUnits;

  public static final int HANDLE_SIZE = 4;
  public static final int ENVIRON_SOLID = 0;
  public static final int ENVIRON_DIFFUSE = 1;
  public static final int ENVIRON_EMISSIVE = 2;

  private static final byte FILE_PREFIX[] = {'A', 'o', 'I', 'S', 'c', 'e', 'n', 'e'}; // ***
  private static final byte ADS_FILE_PREFIX[] = {'A', 'D', 'S', ' ', ' ', ' ', ' ', ' '}; // or just not use the file prefix any more.

  ComputationalFluidDynamics cfd;
  public PointJoinObject createPointJoin;
  Mill mill;
  Mill2 mill2;
    
    public static final int fileVersion = 15; //
    
  //LayoutWindow theWindow;   //  TODO: Depricate, we don't want LayoutWindow and Scene to have referenes to each other. Add static LW variable to classes that need access.
  //Object layoutWindowObject; // TODO: use interface to remove circular reference. EditingWindow
    
  public Scene()
  {
    UniformTexture defTex = new UniformTexture();

    objects = new Vector<ObjectInfo>();
    materials = new Vector<Material>();
    textures = new Vector<Texture>();
    images = new Vector<ImageMap>();
    selection = new Vector<Integer>();
    metadataMap = new HashMap<String, Object>();
    textureListeners = new Vector<ListChangeListener>();
    materialListeners = new Vector<ListChangeListener>();
    defTex.setName("Default Texture");
    textures.addElement(defTex);
    ambientColor = new RGBColor(0.3f, 0.3f, 0.3f);
    environColor = new RGBColor(0.0f, 0.0f, 0.0f);
    environTexture = defTex;
    environMapping = defTex.getDefaultMapping(new Sphere(1.0, 1.0, 1.0));
    environParamValue = new ParameterValue [0];
    environMode = ENVIRON_SOLID;
    fogColor = new RGBColor(0.3f, 0.3f, 0.3f);
    fogDist = 20.0;
    fog = false;
    framesPerSecond = 30;
    nextID = 1;
    scale = 1.0;
      units = 3; // 3 == inches
      exportUnits = 3;

    // Grids are off by default.

    showGrid = snapToGrid = false;
    gridSpacing = 1.0;
    gridSubdivisions = 10;
      
      createPointJoin = new PointJoinObject();
      //layoutWindowObject = null;
  }
    
    /**
     * getCreatePointJoinObject
     *
     * Description: This object is used to store data while connecting two curve/mesh points.
     *   shared between SceneViewer for click and Curve for drawing.
     */
    public PointJoinObject getCreatePointJoinObject(){
        if(createPointJoin == null){
            this.createPointJoin = new PointJoinObject();
        }
        return this.createPointJoin;
    }
    
    public void setPointJoinObject(PointJoinObject pointJoinObject){
        this.createPointJoin = pointJoinObject;
    }

  /** Get the name of this scene. */

  public String getName()
  {
    return name;
  }

  /** Set the name of this scene. */

  public void setName(String newName)
  {
    name = newName;
  }

  /** Get the directory on disk in which this scene is saved. */

  public String getDirectory()
  {
    return directory;
  }

  /** Set the directory on disk in which this scene is saved. */

  public void setDirectory(String newDir)
  {
    directory = newDir;
  }

  /** Get the current time. */

  public double getTime()
  {
    return time;
  }

  /** Set the current time. */

  public void setTime(double t)
  {
    time = t;
    boolean processed[] = new boolean [objects.size()];
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = objects.elementAt(i);
        applyTracksToObject(info, processed, null, i);
      }
    for (ObjectInfo obj : objects)
      obj.getObject().sceneChanged(obj, this);
  }

  /** Modify an object (and any objects that depend on it) based on its tracks at the current time. */

  public void applyTracksToObject(ObjectInfo info)
  {
    applyTracksToObject(info, new boolean[objects.size()], null, 0);
    for (ObjectInfo obj : objects)
      obj.getObject().sceneChanged(obj, this);
  }

  /** This should be called after one or more objects have been modified by the user.
      It applies the animation tracks of all other objects which depend on the modified
      ones.  It also applies a subset of the animation tracks on the modified objects
      themselves to reflect their dependencies on other parts of the scene. */

  public void applyTracksAfterModification(Collection<ObjectInfo> changedObjects)
  {
    boolean changed[] = new boolean[objects.size()];
    boolean processed[] = new boolean[objects.size()];

    // First apply a subset of the tracks of the modified objects.

    for (ObjectInfo info : changedObjects)
    {
      int index = indexOf(info);
      changed[index] = processed[index] = true;

      // Find Constraint and IK tracks at the top of the list and apply them.

      int i;
      for (i = 0; i < info.getTracks().length && (info.getTracks()[i] instanceof ConstraintTrack ||
          info.getTracks()[i] instanceof IKTrack || info.getTracks()[i].isNullTrack()); i++);
      for (int j = i-1; j >= 0; j--)
        if (info.getTracks()[j].isEnabled())
          info.getTracks()[j].apply(time);
      if (info.getPose() != null)
        info.getObject().applyPoseKeyframe(info.getPose());
    }

    // Now apply tracks to all dependent objects.

    for (ObjectInfo info : objects)
      applyTracksToObject(info, processed, changed, indexOf(info));
    for (ObjectInfo info : objects)
      info.getObject().sceneChanged(info, this);
  }

  private void applyTracksToObject(ObjectInfo info, boolean processed[], boolean changed[], int index)
  {
    if (processed[index])
    {
      // This object has already been updated.

      info.getObject().sceneChanged(info, this);
      return;
    }
    processed[index] = true;

    // Determine whether this object possesses a Position or Rotation track, and update any
    // tracks it is dependent on.

    boolean hasPos = false, hasRot = false, hasPose = false;
    for (Track track : info.getTracks())
    {
      if (track.isNullTrack() || !track.isEnabled())
        continue;
      ObjectInfo depends[] = track.getDependencies();
      for (int i = 0; i < depends.length; i++)
      {
        int k = indexOf(depends[i]);
        if (k > -1 && !processed[k])
          applyTracksToObject(depends[i], processed, changed, k);
        if (k > -1 && changed != null && changed[k])
          changed[index] = true;
      }
      if (track instanceof PositionTrack || track instanceof ProceduralPositionTrack)
        hasPos = true;
      else if (track instanceof RotationTrack || track instanceof ProceduralRotationTrack)
        hasRot = true;
      else if (track instanceof PoseTrack || track instanceof IKTrack)
        hasPose = true;
    }
    if (changed != null && !changed[index])
      return;
    if (hasPos)
    {
      Vec3 orig = info.getCoords().getOrigin();
      orig.set(0.0, 0.0, 0.0);
      info.getCoords().setOrigin(orig);
    }
    if (hasRot)
      info.getCoords().setOrientation(0.0, 0.0, 0.0);
    if (hasPose)
      info.clearCachedMeshes();
    info.setPose(null);

    // Apply the tracks.

    info.clearDistortion();
    for (int j = info.getTracks().length-1; j >= 0; j--)
      if (info.getTracks()[j].isEnabled())
        info.getTracks()[j].apply(time);
    if (info.getPose() != null)
      info.getObject().applyPoseKeyframe(info.getPose());
  }

  /** Get the number of frames per second. */

  public int getFramesPerSecond()
  {
    return framesPerSecond;
  }

  /** Set the number of frames per second. */

  public void setFramesPerSecond(int n)
  {
    framesPerSecond = n;
  }

  /** Get the scene's ambient light color. */

  public RGBColor getAmbientColor()
  {
    return ambientColor;
  }

  /** Set the scene's ambient light color. */

  public void setAmbientColor(RGBColor color)
  {
    ambientColor = color;
  }

  /** Get the Scene's environment mapping mode.  This will be either ENVIRON_SOLID, ENVIRON_DIFFUSE, or
      ENVIRON_EMISSIVE. */

  public int getEnvironmentMode()
  {
    return environMode;
  }

  /** Set the Scene's environment mapping mode.  This should be either ENVIRON_SOLID, ENVIRON_DIFFUSE, or
      ENVIRON_EMISSIVE. */

  public void setEnvironmentMode(int mode)
  {
    environMode = mode;
  }

  /** Get the texture being used as an environment mapping. */

  public Texture getEnvironmentTexture()
  {
    return environTexture;
  }

  /** Set the texture being used as an environment mapping. */

  public void setEnvironmentTexture(Texture tex)
  {
    environTexture = tex;
  }

  /** Get the TextureMapping being used to map the environment map texture to the environment sphere. */

  public TextureMapping getEnvironmentMapping()
  {
    return environMapping;
  }

  /** Set the TextureMapping to use for mapping the environment map texture to the environment sphere. */

  public void setEnvironmentMapping(TextureMapping map)
  {
    environMapping = map;
  }

  /** Get the parameter values used for the environment map. */

  public ParameterValue [] getEnvironmentParameterValues()
  {
    return environParamValue;
  }

  /** Set the parameter values used for the environment map. */

  public void setEnvironmentParameterValues(ParameterValue value[])
  {
    environParamValue = value;
  }

  /** Get the environment color. */

  public RGBColor getEnvironmentColor()
  {
    return environColor;
  }

  /** Set the environment color. */

  public void setEnvironmentColor(RGBColor color)
  {
    environColor = color;
  }

  /** Get the fog color. */

  public RGBColor getFogColor()
  {
    return fogColor;
  }

  /** Set the fog color. */

  public void setFogColor(RGBColor color)
  {
    fogColor = color;
  }

  /** Determine whether fog is enabled. */

  public boolean getFogState()
  {
    return fog;
  }

  /** Get the length constant for exponential fog. */

  public double getFogDistance()
  {
    return fogDist;
  }

  /** Set the state of fog in the scene.
      @param state    sets whether fog is enabled
      @param dist     the length constant for exponential fog.
  */

  public void setFog(boolean state, double dist)
  {
    fog = state;
    fogDist = dist;
  }

  /** Get whether the grid is displayed. */

  public boolean getShowGrid()
  {
    return showGrid;
  }

  /** Set whether the grid is displayed. */

  public void setShowGrid(boolean show)
  {
    showGrid = show;
  }

  /** Get whether snap-to-grid is enabled. */

  public boolean getSnapToGrid()
  {
    return snapToGrid;
  }

  /** Set whether snap-to-grid is enabled. */

  public void setSnapToGrid(boolean snap)
  {
    snapToGrid = snap;
  }

  /** Get the grid spacing. */

  public double getGridSpacing()
  {
    return gridSpacing;
  }

  /** Set the grid spacing. */

  public void setGridSpacing(double spacing)
  {
    gridSpacing = spacing;
  }

  /** Get the number of grid snap-to subdivisions. */

  public int getGridSubdivisions()
  {
    return gridSubdivisions;
  }

  /** Set the number of grid snap-to subdivisions. */

  public void setGridSubdivisions(int subdivisions)
  {
    gridSubdivisions = subdivisions;
  }

  /** Add a new object to the scene.  If undo is not null, appropriate commands will be
      added to it to undo this operation. */

  public void addObject(Object3D obj, CoordinateSystem coords, String name, UndoRecord undo)
  {
    addObject(new ObjectInfo(obj, coords, name), undo);
    updateSelectionInfo();
    //return id;
  }

  /** Add a new object to the scene.  If undo is not null, appropriate commands will be
      added to it to undo this operation. */

  public int addObjectI(ObjectInfo info, UndoRecord undo)
  {
    int id = addObject(info, objects.size(), undo);
    updateSelectionInfo();
      
    return id;
  }
    
    public void addObject(ObjectInfo info, UndoRecord undo)
    {
        addObject(info, objects.size(), undo);
        updateSelectionInfo();
        
        
    }

  /** Add a new object to the scene in the specified position.  If undo is not null,
      appropriate commands will be added to it to undo this operation. */

  public int addObject(ObjectInfo info, int index, UndoRecord undo)
  {
    info.setId(nextID++);
    if (info.getTracks() == null)
      {
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
      }
    if (info.getObject().canSetTexture() && info.getObject().getTextureMapping() == null)
      info.setTexture(getDefaultTexture(), getDefaultTexture().getDefaultMapping(info.getObject()));
    info.getObject().sceneChanged(info, this);
    objects.insertElementAt(info, index);
    objectIndexMap = null;
    if (undo != null)
      undo.addCommandAtBeginning(UndoRecord.DELETE_OBJECT, new Object [] {index});
    updateSelectionInfo();
      
    return info.getId();
  }
    
    // JDT
    public void removeObjectL(Object3D obj){
        for (int i = 0; i < objects.size(); i++)
        {
            ObjectInfo info = objects.elementAt(i);
            if( obj == info.getObject()  ){
                removeObject(i, null);
            }
        }
    }
    
    /**
     * removeObjectInfo
     * Description:
     */
    public void removeObjectInfo(ObjectInfo oi){
        for (int i = 0; i < objects.size(); i++)
        {
            ObjectInfo info = objects.elementAt(i);
            if(oi == info){
                removeObject(i, null);
            }
        }
    }

  /** Delete an object from the scene.  If undo is not null, appropriate commands will be
      added to it to undo this operation. */

  public void removeObject(int which, UndoRecord undo)
  {
    ObjectInfo info = objects.elementAt(which);
    objects.removeElementAt(which);
    objectIndexMap = null;
    if (undo != null)
      undo.addCommandAtBeginning(UndoRecord.ADD_OBJECT, new Object [] {info, which});
    if (info.getParent() != null)
      {
        int j;
        for (j = 0; info.getParent().getChildren()[j] != info; j++);
        if (undo != null)
          undo.addCommandAtBeginning(UndoRecord.ADD_TO_GROUP, new Object [] {info.getParent(), info, j});
        info.getParent().removeChild(j);
      }
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = objects.elementAt(i);
        for (int j = 0; j < obj.getTracks().length; j++)
          {
            Track tr = obj.getTracks()[j];
            ObjectInfo depends[] = tr.getDependencies();
            for (int k = 0; k < depends.length; k++)
              if (depends[k] == info)
                {
                  if (undo != null)
                    undo.addCommandAtBeginning(UndoRecord.COPY_TRACK, new Object [] {tr, tr.duplicate(tr.getParent())});
                  obj.getTracks()[j].deleteDependencies(info);
                }
          }
      }
    clearSelection();
  }

  /** Add a new Material to the scene. */

  public void addMaterial(Material mat)
  {
    addMaterial(mat, materials.size());
  }

  /**
   * Add a new Material to the scene.
   *
   * @param mat    the Material to add
   * @param index  the position in the list to add it at
   */

  public void addMaterial(Material mat, int index)
  {
    materials.add(index, mat);
    for (int i = 0; i < materialListeners.size(); i++)
      materialListeners.elementAt(i).itemAdded(materials.size()-1, mat);
  }

  /** Remove a Material from the scene. */

  public void removeMaterial(int which)
  {
    Material mat = materials.elementAt(which);

    materials.removeElementAt(which);
    for (int i = 0; i < materialListeners.size(); i++)
      materialListeners.elementAt(i).itemRemoved(which, mat);
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = objects.elementAt(i);
        if (obj.getObject().getMaterial() == mat)
          obj.setMaterial(null, null);
      }
  }

  /**
   * Reorder the list of Materials by moving a Material to a new position in the list.
   *
   * @param oldIndex    the index of the Material to move
   * @param newIndex    the new position to move it to
   */

  public void reorderMaterial(int oldIndex, int newIndex)
  {
    if (newIndex < 0 || newIndex >= materials.size())
      throw new IllegalArgumentException("Illegal value for newIndex: "+newIndex);
    Material mat = materials.remove(oldIndex);
    materials.add(newIndex, mat);
  }

  /** Add a new Texture to the scene. */

  public void addTexture(Texture tex)
  {
    addTexture(tex, textures.size());
  }

  /**
   * Add a new Texture to the scene.
   *
   * @param tex    the Texture to add
   * @param index  the position in the list to add it at
   */

  public void addTexture(Texture tex, int index)
  {
    textures.add(index, tex);
    for (int i = 0; i < textureListeners.size(); i++)
      textureListeners.elementAt(i).itemAdded(textures.size()-1, tex);
  }

  /** Remove a Texture from the scene. */

  public void removeTexture(int which)
  {
    Texture tex = textures.elementAt(which);

    textures.removeElementAt(which);
    for (int i = 0; i < textureListeners.size(); i++)
      textureListeners.elementAt(i).itemRemoved(which, tex);
    if (textures.size() == 0)
      {
        UniformTexture defTex = new UniformTexture();
        defTex.setName("Default Texture");
        textures.addElement(defTex);
        for (int i = 0; i < textureListeners.size(); i++)
          textureListeners.elementAt(i).itemAdded(0, defTex);
      }
    Texture def = textures.elementAt(0);
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = objects.elementAt(i);
        if (obj.getObject().getTexture() == tex)
          obj.setTexture(def, def.getDefaultMapping(obj.getObject()));
        if (obj.getObject().getTextureMapping() instanceof LayeredMapping)
        {
          LayeredMapping map = (LayeredMapping) obj.getObject().getTextureMapping();
          for (int j = map.getNumLayers()-1; j >= 0; j--)
            if (map.getLayer(j) == tex)
              map.deleteLayer(j);
          obj.setTexture(obj.getObject().getTexture(), map);
        }
      }
    if (environTexture == tex)
    {
      environTexture = def;
      environMapping = def.getDefaultMapping(new Sphere(1.0, 1.0, 1.0));
    }
    if (environMapping instanceof LayeredMapping)
    {
      Sphere tempObject = new Sphere(1, 1, 1);
      tempObject.setTexture(environTexture, environMapping);
      tempObject.setParameterValues(environParamValue);
      LayeredMapping map = (LayeredMapping) environMapping;
      for (int j = map.getNumLayers()-1; j >= 0; j--)
        if (map.getLayer(j) == tex)
          map.deleteLayer(j);
      tempObject.setTexture(environTexture, environMapping);
      environParamValue = tempObject.getParameterValues();
    }
  }

  /**
   * Reorder the list of Textures by moving a Texture to a new position in the list.
   *
   * @param oldIndex    the index of the Texture to move
   * @param newIndex    the new position to move it to
   */

  public void reorderTexture(int oldIndex, int newIndex)
  {
    if (newIndex < 0 || newIndex >= textures.size())
      throw new IllegalArgumentException("Illegal value for newIndex: "+newIndex);
    Texture tex = textures.remove(oldIndex);
    textures.add(newIndex, tex);
  }

  /** This method should be called after a Material has been edited.  It notifies
      any objects using the Material that it has changed. */

  public void changeMaterial(int which)
  {
    Material mat = materials.elementAt(which);
    Object3D obj;

    for (int i = 0; i < objects.size(); i++)
      {
        obj = objects.elementAt(i).getObject();
        if (obj.getMaterial() == mat)
          obj.setMaterial(mat, obj.getMaterialMapping());
      }
    for (int i = 0; i < materialListeners.size(); i++)
      materialListeners.elementAt(i).itemChanged(which, mat);
  }

  /** This method should be called after a Texture has been edited.  It notifies
      any objects using the Texture that it has changed. */

  public void changeTexture(int which)
  {
    Texture tex = textures.elementAt(which);

    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo obj = objects.elementAt(i);
        if (obj.getObject().getTexture() == tex)
          obj.setTexture(tex, obj.getObject().getTextureMapping());
        else if (obj.getObject().getTexture() instanceof LayeredTexture)
          for (Texture layer : ((LayeredMapping) obj.getObject().getTextureMapping()).getLayers())
            if (layer == tex)
            {
              obj.setTexture(tex, obj.getObject().getTextureMapping());
              break;
            }
      }
    for (int i = 0; i < textureListeners.size(); i++)
      textureListeners.elementAt(i).itemChanged(which, tex);
  }

  /** Add an object which wants to be notified when the list of Materials in the Scene changes. */

  public void addMaterialListener(ListChangeListener ls)
  {
    materialListeners.addElement(ls);
  }

  /** Remove an object from the set to be notified when the list of Materials changes. */

  public void removeMaterialListener(ListChangeListener ls)
  {
    materialListeners.removeElement(ls);
  }

  /** Add an object which wants to be notified when the list of Textures in the Scene changes. */

  public void addTextureListener(ListChangeListener ls)
  {
    textureListeners.addElement(ls);
  }

  /** Remove an object from the set to be notified when the list of Textures changes. */

  public void removeTextureListener(ListChangeListener ls)
  {
    textureListeners.removeElement(ls);
  }

  /**
   * Get a piece of metadata stored in this scene.
   *
   * @param name   the name of the piece of metadata to get
   * @return the value associated with that name, or null if there is none
   */

  public Object getMetadata(String name)
  {
    return metadataMap.get(name);
  }

  /**
   * Store a piece of metadata in this scene.  This may be an arbitrary object which
   * you want to store as part of the scene.  When the scene is saved to disk, metadata
   * objects are stored using the java.beans.XMLEncoder class.  This means that if the
   * object is not a bean, you must register a PersistenceDelegate for it before calling
   * this method.  Otherwise, it will fail to be saved.
   *
   * @param name   the name of the piece of metadata to set
   * @param value  the value to store
   */

  public void setMetadata(String name, Object value)
  {
    metadataMap.put(name, value);
  }

  /**
   * Get the names of all metadata objects stored in this scene.
   */

  public Set<String> getAllMetadataNames()
  {
    return metadataMap.keySet();
  }

  /** Show the dialog for editing textures and materials. */

  public void showTexturesDialog(EditingWindow parent)

  {
    if (texDlg == null)
      texDlg = new TexturesAndMaterialsDialog(parent, this);
    else
    {
      Rectangle r = texDlg.getBounds();
      texDlg.dispose();
      texDlg = new TexturesAndMaterialsDialog(parent, this);
      texDlg.setBounds(r);
    }
    texDlg.setVisible(true);
  }

  /** Add an image map to the scene. */

  public void addImage(ImageMap im)
  {
    images.addElement(im);
  }

  /** Remove an image map from the scene. */

  public boolean removeImage(int which)
  {
    ImageMap image = images.elementAt(which);

    for (int i = 0; i < textures.size(); i++)
      if (textures.elementAt(i).usesImage(image))
        return false;
    for (int i = 0; i < materials.size(); i++)
      if (materials.elementAt(i).usesImage(image))
        return false;
    images.removeElementAt(which);
    return true;
  }

  /** Replace every instance of one object in the scene with another one.  If undo is not
      null, commands will be added to it to undo this operation. */

  public void replaceObject(Object3D original, Object3D replaceWith, UndoRecord undo)
  {
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = objects.elementAt(i);
        if (info.getObject() != original)
          continue;
        if (undo != null)
          undo.addCommand(UndoRecord.SET_OBJECT, new Object [] {info, original});
        info.setObject(replaceWith);
        info.clearCachedMeshes();
      }
  }

  /** This should be called whenever an object changes.  It clears any cached meshes for
      any instances of the object. */

  public void objectModified(Object3D obj)
  {
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = objects.elementAt(i);
        if (info.getObject() == obj)
          {
            info.clearCachedMeshes();
            info.setPose(null);
          }
      }
  }

  /**
   * Set one object to be selected, deselecting all other objects.
   * @deprecated Call setSelection() on the LayoutWindow instead.
   */

  public void setSelection(int which)
  {
    clearSelection();
    addToSelection(which);
    updateSelectionInfo();
  }

  /**
   * Set a list of objects to be selected, deselecting all other objects.
   * @deprecated Call setSelection() on the LayoutWindow instead.
   */

  public void setSelection(int which[])
  {
    clearSelection();
    for (int i = 0; i < which.length; i++)
      addToSelection(which[i]);
    updateSelectionInfo();
      
      //System.out.println("Scene.setSelection() ");
  }

  /**
   * Add an object to the list of selected objects.
   * @deprecated Call addToSelection() on the LayoutWindow instead.
   */

  public void addToSelection(int which)
  {
      if(which >= objects.size()){
          System.out.println("Error: out of range Scene.addToSelection("+which+")");
          return;
      }
      if(which < 0){
          System.out.println("Error: out of range Scene.addToSelection("+which+")");
          return;
      }
      
    ObjectInfo info = objects.elementAt(which);
    if (!info.selected)
      selection.addElement(which);
    info.selected = true;
    updateSelectionInfo();
      
      //System.out.println("Scene.addToSelection() ");
  }

  /**
   * Deselect all objects.
   * @deprecated Call clearSelection() on the LayoutWindow instead.
   */

  public void clearSelection()
  {
    selection.removeAllElements();
    for (int i = 0; i < objects.size(); i++)
      objects.elementAt(i).selected = false;
    updateSelectionInfo();
      
      // Dismiss pair distance align dialog
      //
      //System.out.println("* Scene.clearSelection() ");
  }

  /**
   * Deselect a particular object.
   * @deprecated Call removeFromSelection() on the LayoutWindow instead.
   */

  public void removeFromSelection(int which)
  {
    ObjectInfo info = objects.elementAt(which);
    selection.removeElement(which);
    info.selected = false;
    updateSelectionInfo();
  }

  /** Calculate the list of which objects are children of selected objects. */

  private void updateSelectionInfo()
  {
    for (int i = objects.size()-1; i >= 0; i--)
      objects.elementAt(i).parentSelected = false;
    for (int i = objects.size()-1; i >= 0; i--)
      {
        ObjectInfo info = objects.elementAt(i);
        ObjectInfo parent = info.getParent();
        while (parent != null)
          {
            if (parent.selected || parent.parentSelected)
              {
                info.parentSelected = true;
                break;
              }
            parent = parent.getParent();
          }
      }
      
      //System.out.println("* Scene.updateSelectionInfo() ");
  }

  /** Get the number of objects in this scene. */

  public int getNumObjects()
  {
    return objects.size();
  }

  /** Get the i'th object. */

  public ObjectInfo getObject(int i)
  {
      if(i < 0){
          return null;
      }
    return objects.elementAt(i);
  }

  /** Get the object with the specified name, or null if there is none.  If
      more than one object has the same name, this will return the first one. */

  public ObjectInfo getObject(String name)
  {
    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = objects.elementAt(i);
        if (info.getName().equals(name))
          return info;
      }
    return null;
  }

  /** Get the object with the specified ID, or null if there is none. */

  public ObjectInfo getObjectById(int id)
  {
    for (int i = 0; i < objects.size(); i++)
    {
      ObjectInfo info = objects.elementAt(i);
      if (info.getId() == id)
        return info;
    }
    return null;
  }

  /** Get all objects in the Scene in the form of a List. */

  public List<ObjectInfo> getAllObjects()
  {
    return Collections.unmodifiableList(objects);
  }

  /** Get the index of the specified object. */

  public int indexOf(ObjectInfo info)
  {
    if (objectIndexMap == null)
    {
      // Build an index for fast lookup.

      objectIndexMap = new HashMap<ObjectInfo, Integer>();
      for (int i = 0; i < objects.size(); i++)
        objectIndexMap.put(objects.get(i), i);
    }
    Integer index = objectIndexMap.get(info);
    return (index == null ? -1 : index);
  }

  /** Get the number of textures in this scene. */

  public int getNumTextures()
  {
    return textures.size();
  }

  /** Get the index of the specified texture. */

  public int indexOf(Texture tex)
  {
    return textures.indexOf(tex);
  }

  /** Get the i'th texture. */

  public Texture getTexture(int i)
  {
    return textures.elementAt(i);
  }

  /** Get the texture with the specified name, or null if there is none.  If
      more than one texture has the same name, this will return the first one. */

  public Texture getTexture(String name)
  {
    for (int i = 0; i < textures.size(); i++)
      {
        Texture tex = textures.elementAt(i);
        if (tex.getName().equals(name))
          return tex;
      }
    return null;
  }

  /** Get the number of materials in this scene. */

  public int getNumMaterials()
  {
    return materials.size();
  }

  /** Get the i'th material. */

  public Material getMaterial(int i)
  {
    return materials.elementAt(i);
  }

  /** Get the material with the specified name, or null if there is none.  If
      more than one material has the same name, this will return the first one. */

  public Material getMaterial(String name)
  {
    for (int i = 0; i < materials.size(); i++)
      {
        Material mat = materials.elementAt(i);
        if (mat.getName().equals(name))
          return mat;
      }
    return null;
  }

  /** Get the index of the specified material. */

  public int indexOf(Material mat)
  {
    return materials.indexOf(mat);
  }

  /** Get the number of image maps in this scene. */

  public int getNumImages()
  {
    return images.size();
  }

  /** Get the i'th image map. */

  public ImageMap getImage(int i)
  {
    return images.elementAt(i);
  }

  /** Get the index of the specified image map. */

  public int indexOf(ImageMap im)
  {
    return images.indexOf(im);
  }

  /** Get the default Texture for newly created objects. */

  public Texture getDefaultTexture()
  {
    return textures.elementAt(0);
  }

  /**
   * Get a list of the indices of all selected objects.
   * @deprecated Call getSelectedIndices() or getSelectedObjects() on the LayoutWindow instead.
   */

  public int [] getSelection()
  {
    int sel[] = new int [selection.size()];

    for (int i = 0; i < sel.length; i++)
      sel[i] = selection.elementAt(i);
    return sel;
  }

  /**
   * Get the indices of all objects which are either selected, or are children of
   * selected objects.
   * @deprecated Call getSelectionWithChildren() on the LayoutWindow instead.
   */

  public int [] getSelectionWithChildren()
  {
    int count = 0;
    for (int i = objects.size()-1; i >= 0; i--)
      {
        ObjectInfo info = objects.elementAt(i);
        if (info.selected || info.parentSelected)
          count++;
      }
    int sel[] = new int [count];
    count = 0;
    for (int i = objects.size()-1; i >= 0; i--)
      {
        ObjectInfo info = objects.elementAt(i);
        if (info.selected || info.parentSelected)
          sel[count++] = i;
      }
    return sel;
  }

  /** Return true if any errors occurred while loading the scene.  The scene is still valid
      and usable, but some objects in it were not loaded correctly. */

  public boolean errorsOccurredInLoading()
  {
    return errorsLoading;
  }

  /** Get a description of any errors which occurred while loading the scene. */

  public String getLoadingErrors()
  {
    return (loadingErrors == null ? "" : loadingErrors.toString());
  }

  /** The following constructor is used for reading files.  If fullScene is false, only the
      Textures and Materials are read. */

  public Scene(File f, boolean fullScene) throws IOException, InvalidObjectException
  {
    setName(f.getName());
    setDirectory(f.getParent());
    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f));
    buf.mark(ADS_FILE_PREFIX.length);

    // See if the file begins with the expected prefix.

    boolean hasPrefix = true;
    for (int i = 0; hasPrefix && i < ADS_FILE_PREFIX.length; i++){
        int c = buf.read();
        hasPrefix &= ((c == ADS_FILE_PREFIX[i]) || (c == FILE_PREFIX[i]));
        //System.out.println(" hasPrefix " + hasPrefix + "  ADS_FILE_PREFIX[i] " + ADS_FILE_PREFIX[i] + " c: " + c );
    }
      
    if (!hasPrefix)
      buf.reset(); // This is an old file that doesn't start with the prefix.

    // We expect the data to be gzipped, but if it's somehow gotten decompressed we should accept that to.

    DataInputStream in;
    try
    {
      in = new DataInputStream(new GZIPInputStream(buf));
    }
    catch (IOException ex)
    {
      buf.close();
      buf = new BufferedInputStream(new FileInputStream(f));
      in = new DataInputStream(buf);
    }
    initFromStream(in, fullScene);
    in.close();


    // JDT terrible hack
    // ObjectInfo needs a base dir set so we make it abstract and set it on scene open.
    // This means only one scene can be used at a time...
    LayoutModeling layout = new LayoutModeling();
    layout.setBaseDir(this.getDirectory() + System.getProperty("file.separator") + this.getName() + "_layout_data" );

  }

  /** The following constructor is used for reading from arbitrary input streams.  If fullScene
      is false, only the Textures and Materials are read. */

  public Scene(DataInputStream in, boolean fullScene) throws IOException, InvalidObjectException
  {
    initFromStream(in, fullScene);
  }

  /** Initialize the scene based on information read from an input stream. */

  private void initFromStream(DataInputStream in, boolean fullScene) throws IOException, InvalidObjectException
  {
    int count;
    short version = in.readShort();
    System.out.println("Reading file version: " + version);
      
    Hashtable<Integer, Object3D> table;
    Class cls;
    Constructor con;

    if (version < 0 || version > fileVersion) // set version in file.  
      throw new InvalidObjectException("");
      
    if(version >= 10){          // Version 10 and up stores scale in main file.
        scale = in.readDouble();
    }
      
      if(version >= 12){        // Read units
          units = in.readInt();
      }
      
      if(version >= 15){
          exportUnits = in.readInt();
      }
      
    loadingErrors = new StringBuffer();
    ambientColor = new RGBColor(in);
    fogColor = new RGBColor(in);
    fog = in.readBoolean();
    fogDist = in.readDouble();
    showGrid = in.readBoolean();
    snapToGrid = in.readBoolean();
    gridSpacing = in.readDouble();
    gridSubdivisions = in.readInt();
    framesPerSecond = in.readInt();
    nextID = 1;

    // Read the image maps.

    count = in.readInt();
    images = new Vector<ImageMap>(count);
    for (int i = 0; i < count; i++)
      {
        if (version == 0)
          {
            images.addElement(new MIPMappedImage(in, (short) 0));
            continue;
          }
        String classname = in.readUTF();
          
          if(classname.contains("artofillusion.")){
            classname = "armarender." + classname.substring(14);
          }
            if(classname.contains("armadesignstudio.")){
              classname = "armarender." + classname.substring(17);
            }
          
          System.out.println(" 4 " + classname);
          
        try
          {
            cls = ArmaRender.getClass(classname);
            if (cls == null)
              throw new IOException("Unknown class: "+classname);
            con = cls.getConstructor(DataInputStream.class);
            images.addElement((ImageMap) con.newInstance(in));
          }
        catch (Exception ex)
          {
              System.out.println(" Scene.initFromStream()  Error   classname: " + classname);
              
            throw new IOException("Error loading image: "+ex.getMessage());
              
          }
      }

    // Read the materials.

    count = in.readInt();
    materials = new Vector<Material>(count);
    for (int i = 0; i < count; i++)
      {
        try
          {
            String classname = in.readUTF();
              
              if(classname.contains("artofillusion.")){
                classname = "armarender." + classname.substring(14);
              }
                if(classname.contains("armadesignstudio.")){
                  classname = "armarender." + classname.substring(17);
                }
              
              System.out.println("8 " + classname);
              
            int len = in.readInt();
            byte bytes[] = new byte [len];
            in.readFully(bytes);
            cls = ArmaRender.getClass(classname);
            try
              {
                if (cls == null)
                  throw new IOException("Unknown class: " + classname);
                con = cls.getConstructor(DataInputStream.class, Scene.class);
                materials.addElement((Material) con.newInstance(new DataInputStream(new ByteArrayInputStream(bytes)), this));
              }
            catch (Exception ex)
              {
                ex.printStackTrace();
                if (ex instanceof ClassNotFoundException)
                  loadingErrors.append(Translate.text("errorFindingClass", classname)).append('\n');
                else
                  loadingErrors.append(Translate.text("errorInstantiatingClass", classname)).append('\n');
                UniformMaterial m = new UniformMaterial();
                m.setName("<unreadable>");
                materials.addElement(m);
                errorsLoading = true;
              }
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new IOException();
          }
      }

    // Read the textures.

    count = in.readInt();
    textures = new Vector<Texture>(count);
    for (int i = 0; i < count; i++)
      {
        try
          {
            String classname = in.readUTF();
              
              if(classname.contains("artofillusion.")){
                classname = "armarender." + classname.substring(14);
              }
                if(classname.contains("armadesignstudio.")){
                  classname = "armarender." + classname.substring(17);
                }
              
              System.out.println(" classname   " + classname ); // armadesignstudio.texture.UniformTexture
              
            int len = in.readInt();
            byte bytes[] = new byte [len];
            in.readFully(bytes);
            cls = ArmaRender.getClass(classname);
            try
              {
                if (cls == null)
                  throw new IOException("Unknown class: "+classname);
                  
                  System.out.println(" - " + DataInputStream.class.getName() );
                  
                con = cls.getConstructor(DataInputStream.class, Scene.class);
                  
                  
                textures.addElement((Texture) con.newInstance(new DataInputStream(new ByteArrayInputStream(bytes)), this));
              }
            catch (Exception ex)
              {
                ex.printStackTrace();
                if (ex instanceof ClassNotFoundException)
                  loadingErrors.append(Translate.text("errorFindingClass", classname)).append('\n');
                else
                  loadingErrors.append(Translate.text("errorInstantiatingClass", classname)).append('\n');
                UniformTexture t = new UniformTexture();
                t.setName("<unreadable>");
                textures.addElement(t);
                errorsLoading = true;
              }
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new IOException();
          }
      }

    // Read the objects.

    count = in.readInt();
    objects = new Vector<ObjectInfo>(count);
    table = new Hashtable<Integer, Object3D>(count);
    for (int i = 0; i < count; i++){
      objects.addElement(readObjectFromFile(in, table, version));
    }
    objectIndexMap = null;
    selection = new Vector<Integer>();

    // Read the list of children for each object.

    for (int i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = objects.elementAt(i);
        int num = in.readInt();
        for (int j = 0; j < num; j++)
          {
              try {
                  ObjectInfo child = objects.elementAt(in.readInt());
                  info.addChild(child, j);
              } catch(Exception e){
                  System.out.println("initFromStream. Error reading children.");
                  System.out.println("Error: " + info.getName() + " id: " + info.getId());
                  e.printStackTrace();
              }
          }
      }

    // Read in the environment mapping information.

    environMode = (int) in.readShort();
    if (environMode == ENVIRON_SOLID)
      {
        environColor = new RGBColor(in);
        environTexture = textures.elementAt(0);
        environMapping = environTexture.getDefaultMapping(new Sphere(1.0, 1.0, 1.0));
        environParamValue = new ParameterValue [0];
      }
    else
      {
        int texIndex = in.readInt();
        if (texIndex == -1)
          {
            // This is a layered texture.

            Object3D sphere = new Sphere(1.0, 1.0, 1.0);
            environTexture = new LayeredTexture(sphere);
            String mapClassName = in.readUTF();
              
              System.out.println("mapClassName: " + mapClassName);
              
            if (!LayeredMapping.class.getName().equals(mapClassName))
              throw new InvalidObjectException("");
            environMapping = environTexture.getDefaultMapping(sphere);
            ((LayeredMapping) environMapping).readFromFile(in, this);
          }
        else
          {
            environTexture = textures.elementAt(texIndex);
            try
              {
                  String val = in.readUTF();
                  
                  System.out.println("val: " + val);
                  
                Class mapClass = ArmaRender.getClass(val);
                con = mapClass.getConstructor(DataInputStream.class, Object3D.class, Texture.class);
                environMapping = (TextureMapping) con.newInstance(in, new Sphere(1.0, 1.0, 1.0), environTexture);
              }
            catch (Exception ex)
              {
                throw new IOException();
              }
          }
        environColor = new RGBColor(0.0f, 0.0f, 0.0f);
        environParamValue = new ParameterValue [environMapping.getParameters().length];
        if (version > 2)
          for (int i = 0; i < environParamValue.length; i++)
            environParamValue[i] = Object3D.readParameterValue(in);
      }

    // Read the metadata.

    metadataMap = new HashMap<String, Object>();
    if (version > 3)
    {
      count = in.readInt();
      SearchlistClassLoader loader = new SearchlistClassLoader(getClass().getClassLoader());
      for (ClassLoader cl : PluginRegistry.getPluginClassLoaders())
        loader.add(cl);
      for (int i = 0; i < count; i++)
      {
        try
        {
          String name = in.readUTF();
            
            System.out.println("name: " + name);
            
          byte data[] = new byte[in.readInt()];
          in.readFully(data);
          XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(data), null, null, loader);
          metadataMap.put(name, decoder.readObject());
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
          // Nothing more we can do about it.
        }
      }
    }
    textureListeners = new Vector<ListChangeListener>();
    materialListeners = new Vector<ListChangeListener>();
    setTime(0.0);
  }

    /**
     * readObjectFromFile
     *
     * Description: Read objectInfo from file
     */
  private ObjectInfo readObjectFromFile(DataInputStream in, Hashtable<Integer, Object3D> table, int version) throws IOException, InvalidObjectException
  {
      System.out.println(" *** readObjectFromFile: " );
      
    ObjectInfo info = new ObjectInfo(null, new CoordinateSystem(in), in.readUTF());
    Class cls;
    Constructor con;
    Object3D obj;

    info.setId(in.readInt());
    if (info.getId() >= nextID)
      nextID = info.getId() +1;
    info.setVisible(in.readBoolean());
    Integer key = in.readInt();
    obj = table.get(key);
    if (obj == null)
      {
        try
          {
              
            String classname = in.readUTF();
           
              System.out.println(" *** classname: " + classname);
              
            // ** Still not working ** 
            if(classname.contains("artofillusion.")){
              classname = "armarender." + classname.substring(14);
            }
              if(classname.contains("armadesignstudio.")){
                classname = "armarender." + classname.substring(17);
              }
              
              System.out.println(" 2 classname: " + classname);

            int len = in.readInt();
            byte bytes[] = new byte [len];
            in.readFully(bytes);
            try
              {
                cls = ArmaRender.getClass(classname);
                con = cls.getConstructor(DataInputStream.class, Scene.class);
                obj = (Object3D) con.newInstance(new DataInputStream(new ByteArrayInputStream(bytes)), this);
              }
            catch (Exception ex)
              {
                  System.out.println(" Scene exception: " + classname);

                if (ex instanceof InvocationTargetException)
                  ((InvocationTargetException) ex).getTargetException().printStackTrace();
                else
                  ex.printStackTrace();
                if (ex instanceof ClassNotFoundException)
                  loadingErrors.append(info.getName()).append(": ").append(Translate.text("errorFindingClass", classname)).append('\n');
                else
                  loadingErrors.append(info.getName()).append(": ").append(Translate.text("errorInstantiatingClass", classname)).append('\n');
                obj = new NullObject();
                info.setName("<unreadable> "+ info.getName());
                errorsLoading = true;
              }
            table.put(key, obj);
          }
        catch (Exception ex)
          {
            ex.printStackTrace();
            throw new IOException();
          }
      }
    info.setObject(obj);

    if (version < 2 && obj.getTexture() != null)
      {
        // Initialize the texture parameters.

        TextureParameter texParam[] = obj.getTextureMapping().getParameters();
        ParameterValue paramValue[] = obj.getParameterValues();
        double val[] = new double [paramValue.length];
        boolean perVertex[] = new boolean [paramValue.length];
        for (int i = 0; i < val.length; i++)
          val[i] = in.readDouble();
        for (int i = 0; i < perVertex.length; i++)
          perVertex[i] = in.readBoolean();
        for (int i = 0; i < paramValue.length; i++)
          if (paramValue[i] == null)
          {
            if (perVertex[i])
              paramValue[i] = new VertexParameterValue((Mesh) obj, texParam[i]);
            else
              paramValue[i] = new ConstantParameterValue(val[i]);
          }
        obj.setParameterValues(paramValue);
      }

    // Read the tracks for this object.

    int tracks = in.readInt();
    try
      {
        for (int i = 0; i < tracks; i++)
          {
              String classname = in.readUTF();
             
                System.out.println(" *** classname: " + classname);
                
              // ** Still not working **
              if(classname.contains("artofillusion.")){
                classname = "armarender." + classname.substring(14);
              }
                if(classname.contains("armadesignstudio.")){
                  classname = "armarender." + classname.substring(17);
                }
              
              System.out.println(" --3 classname: " + classname);
              
            cls = ArmaRender.getClass(classname);
              
           
              
            con = cls.getConstructor(ObjectInfo.class);
            Track tr = (Track) con.newInstance(info);
            tr.initFromStream(in, this);
            info.addTrack(tr, i);
          }
        if (info.getTracks() == null)
          info.tracks = new Track [0];
      }
    catch (Exception ex)
      {
        ex.printStackTrace();
        throw new IOException();
      }
      
    // read objectInfo group
      
      
      try {
          String groupName = "";
          if (version >= 5){
              if(in.available() > 0) {
                  groupName = in.readUTF();
              }
          }
          if(groupName != null){
              info.setGroupName(groupName);
              //System.out.println("Reading group name: " + groupName);
          }
      //} catch (UTFDataFormatException dfex){
      //    System.out.println(" ok ");
      } catch (Exception ex)
      {
          ex.printStackTrace();
          
          info.setGroupName("");
          System.out.println("Unable to read object group name information. This can happen when reading files from previous versions of ADS." );
        //
        //throw new IOException();
      }
      
      // Layout data
      if(version >= 6){
          try {
              info.setLayoutView(in.readBoolean());
              info.setLayoutOriginX(in.readDouble());
              info.setLayoutOriginY(in.readDouble());
              info.setLayoutOriginZ(in.readDouble());
              info.setLayoutZDirX(in.readDouble());
              info.setLayoutZDirY(in.readDouble());
              info.setLayoutZDirZ(in.readDouble());
              info.setLayoutUpDirX(in.readDouble());
              info.setLayoutUpDirY(in.readDouble());
              info.setLayoutUpDirZ(in.readDouble());
              
              info.setCncDisabled(in.readBoolean());
              info.setCncPointOffset(in.readInt());
              info.setCncPolyOrder(in.readInt());
              info.setCncPolyDepth(in.readDouble());
              info.setCncReversePointOrder(in.readBoolean());
              
              info.setFeaMPa(in.readDouble());
              
              info.setDisplayModeOverride(in.readInt());
              
          } catch (Exception ex)
          {
              
          }
      }
      
      if(version >= 9){
          try {
              info.setChildrenHiddenWhenHidden(in.readBoolean());
          } catch (Exception ex)
          {
              
          }
      }
      
      if(version >= 11){ // Added Feb 15 2023.
          try {
              info.setReplicateId(in.readInt());
          } catch (Exception ex)
          {
              
          }
      }
      
      if(version >= 13){ // Added Feb 28 2023
          try {
              info.setReplicateEnabled(in.readBoolean());
          } catch (Exception ex)
          {
              
          }
      }
      
      if(version >= 14){ // Added March 1 2023
          try {
              info.setPhysicalMaterialId(in.readInt());
          } catch (Exception ex)
          {
              
          }
      }
      
    return info;
  }

  /** Save the Scene to a file. */

  public void writeToFile(File f) throws IOException
  {
    int mode = (ArmaRender.getPreferences().getKeepBackupFiles() ? SafeFileOutputStream.OVERWRITE+SafeFileOutputStream.KEEP_BACKUP : SafeFileOutputStream.OVERWRITE);
    SafeFileOutputStream safeOut = new SafeFileOutputStream(f, mode);
    BufferedOutputStream bout = new BufferedOutputStream(safeOut);
    bout.write(ADS_FILE_PREFIX); // change this
      
      
      
    DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bout));
    writeToStream(out);
    out.close();
  }

  /** Write the Scene's representation to an output stream. */

  public void writeToStream(DataOutputStream out) throws IOException
  {
      
      // Progress dialog. (Doesn't work until save is in new thread)
      final JDialog progressDialog = new JDialog(); //  parentFrame , "Progress Dialog", true ); // parentFrame , "Progress Dialog", true); // Frame owner
      JProgressBar dpb = new JProgressBar(0, 100);
      progressDialog.add(BorderLayout.CENTER, dpb);
      JLabel progressLabel = new JLabel("Saving...");
      progressDialog.add(BorderLayout.NORTH, progressLabel);
      progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      progressDialog.setSize(300, 75);
      progressDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      // progressDialog.setLocationRelativeTo(parentFrame);
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      progressDialog.setLocation((int)(screenSize.getWidth() / 2) - (300/2), (int) ((screenSize.getHeight()/(float)2) - ((float)75/(float)2.0)));
      progressDialog.addWindowListener(new WindowAdapter()
      {
          public void windowClosed(WindowEvent e)
          {
              System.out.println("jdialog window closed event received");
              //running = false;
          }
          public void windowClosing(WindowEvent e)
          {
              System.out.println("jdialog window closing event received");
              //running = false;
          }
      });
      //progressDialog.setVisible(true);
      
      Scene scene = this;
     
      /*
      Thread thread = new Thread() {
          public void run() {
            try {
       */
            //
      int totalItems = images.size() + materials.size() + textures.size() + objects.size();
      int currProgress = 0;
      
      Material mat;
      Texture tex;
      int i, j, index = 0;
      Hashtable<Object3D, Integer> table = new Hashtable<Object3D, Integer>(objects.size());

      // File format version
      //out.writeShort(8); // Version 4=old, 5=with object group name, 6= layout view, cam, fea data, 7 = curve support mode, 8 = curve length, perpendicular
      out.writeShort(fileVersion); // 10 = scene scale. 11 = replicateId.
      
      
      // Scene scale (Version 10)
      out.writeDouble(scale);
      
      // Write units        (fileVersion: Version 12)
      out.writeInt(units);
      
      // Write export units (fileVersion: 13)
      out.writeInt(exportUnits);
      
      ambientColor.writeToFile(out);
      fogColor.writeToFile(out);
      out.writeBoolean(fog);
      out.writeDouble(fogDist);
      out.writeBoolean(showGrid);
      out.writeBoolean(snapToGrid);
      out.writeDouble(gridSpacing);
      out.writeInt(gridSubdivisions);
      out.writeInt(framesPerSecond);

    // Save the image maps.

    out.writeInt(images.size());
    for (i = 0; i < images.size(); i++)
      {
        ImageMap img = images.elementAt(i);
        out.writeUTF(img.getClass().getName());
        img.writeToStream(out);
          
          currProgress++;
          int progress = (int) (((float)(currProgress) / (float)(totalItems)) * (float)100);
          dpb.setValue(progress);
      }

    // Save the materials.

    out.writeInt(materials.size());
    for (i = 0; i < materials.size(); i++)
      {
        mat = materials.elementAt(i);
        out.writeUTF(mat.getClass().getName());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        mat.writeToFile(new DataOutputStream(bos), scene);
        byte bytes[] = bos.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes, 0, bytes.length);
              
        currProgress++;
        int progress = (int) (((float)(currProgress) / (float)(totalItems)) * (float)100);
        dpb.setValue(progress);
      }

    // Save the textures.

    out.writeInt(textures.size());
    for (i = 0; i < textures.size(); i++)
      {
        tex = textures.elementAt(i);
        out.writeUTF(tex.getClass().getName());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        tex.writeToFile(new DataOutputStream(bos), scene);
        byte bytes[] = bos.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes, 0, bytes.length);
              
        currProgress++;
        int progress = (int) (((float)(currProgress) / (float)(totalItems)) * (float)100);
        dpb.setValue(progress);
      }

    // Save the objects.

    out.writeInt(objects.size());
    for (i = 0; i < objects.size(); i++){
        // table is a hash table of object3d
        // index is the last object index used.
      index = writeObjectToFile(out, objects.elementAt(i), table, index);
              
        currProgress++;
        int progress = (int) (((float)(currProgress) / (float)(totalItems)) * (float)100);
        dpb.setValue(progress);
    }

    // Record the children of each object.  The format of this will be changed in the
    // next version.

    for (i = 0; i < objects.size(); i++)
      {
        ObjectInfo info = objects.elementAt(i);
        out.writeInt(info.getChildren().length);
        for (j = 0; j < info.getChildren().length; j++)
          out.writeInt(indexOf(info.getChildren()[j]));
      }

    // Save the environment mapping information.

    out.writeShort((short) environMode);
    if (environMode == ENVIRON_SOLID)
      environColor.writeToFile(out);
    else
      {
        out.writeInt(textures.lastIndexOf(environTexture));
        out.writeUTF(environMapping.getClass().getName());
        if (environMapping instanceof LayeredMapping)
          ((LayeredMapping) environMapping).writeToFile(out, scene);
        else
          environMapping.writeToFile(out);
        for (i = 0; i < environParamValue.length; i++)
        {
          out.writeUTF(environParamValue[i].getClass().getName());
          environParamValue[i].writeToStream(out);
        }
      }

    // Save metadata.

    out.writeInt(metadataMap.size());
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    SearchlistClassLoader loader = new SearchlistClassLoader(getClass().getClassLoader());
    for (ClassLoader cl : PluginRegistry.getPluginClassLoaders())
      loader.add(cl);
    Thread.currentThread().setContextClassLoader(loader); // So that plugin classes can be saved correctly.
    ExceptionListener exceptionListener = new ExceptionListener()
      {
        public void exceptionThrown(Exception e)
        {
          e.printStackTrace();
        }
      };
    for (Map.Entry<String, Object> entry : metadataMap.entrySet())
    {
      ByteArrayOutputStream value = new ByteArrayOutputStream();
      XMLEncoder encoder = new XMLEncoder(value);
      encoder.setExceptionListener(exceptionListener);
      encoder.writeObject(entry.getValue());
      encoder.close();
      out.writeUTF(entry.getKey());
      out.writeInt(value.size());
      out.write(value.toByteArray());
    }
    Thread.currentThread().setContextClassLoader(contextClassLoader);
      
     
            /*
            } catch (Exception e){
                
            }
          }
      };
      thread.start();
      System.out.println("Saved.");
             */
      
      // Done
      progressDialog.setVisible(false);
  }

  /**
   * writeObjectToFile
   *
   * Description: Write the information about a single object to a file.
   */
  private int writeObjectToFile(DataOutputStream out, ObjectInfo info, Hashtable<Object3D, Integer> table, int index) throws IOException
  {
    Integer key;

    info.getCoords().writeToFile(out);
    out.writeUTF(info.getName());
    out.writeInt(info.getId());
    out.writeBoolean(info.isVisible());
    key = table.get(info.getObject());
    if (key == null)
      {
        out.writeInt(index);
        out.writeUTF(info.getObject().getClass().getName());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        info.getObject().writeToFile(new DataOutputStream(bos), this);
        byte bytes[] = bos.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes, 0, bytes.length);
        key = index++;
        table.put(info.getObject(), key);
      }
    else
      out.writeInt(key.intValue());

    // Write the tracks for this object.

    out.writeInt(info.getTracks().length);
    for (int i = 0; i < info.getTracks().length; i++)
      {
        out.writeUTF(info.getTracks()[i].getClass().getName());
        info.getTracks()[i].writeToStream(out, this);
      }
    
    // Write the object group name
    out.writeUTF(info.getGroupName());
      
    // write layout data
      out.writeBoolean(info.getLayoutView());
      out.writeDouble(info.getLayoutOriginX());
      out.writeDouble(info.getLayoutOriginY());
      out.writeDouble(info.getLayoutOriginZ());
      out.writeDouble(info.getLayoutZDirX());
      out.writeDouble(info.getLayoutZDirY());
      out.writeDouble(info.getLayoutZDirZ());
      out.writeDouble(info.getLayoutUpDirX());
      out.writeDouble(info.getLayoutUpDirY());
      out.writeDouble(info.getLayoutUpDirZ());
      
    // CAM data
      out.writeBoolean(info.getCncDisabled());
      out.writeInt(info.getCncPointOffset());
      out.writeInt(info.getCncPolyOrder());
      out.writeDouble(info.getCncPolyDepth());
      out.writeBoolean(info.getCncReversePointOrder());
      
      // FEA data
      out.writeDouble(info.getFeaMPa());
      
      // Display mode overide
      out.writeInt(info.getDisplayModeOverride());
      
      // Children hidden when hidden    (File version 9)
      out.writeBoolean(info.isChildrenHiddenWhenHidden());
      
      // Write replicateId property
      out.writeInt(info.getReplicateId());
      
      // Write replicateEnabled property
      out.writeBoolean(info.isReplicateEnabled());
      
      // Wite Physical Material ID property
      out.writeInt(info.getPhysicalMaterialId());
      
    return index;
  }


    /**
    * autoSkin
    *
    * Description:
    * Skin curves into a mesh.
    */
    public void autoSkin(LayoutWindow layoutWindow){
        SplineSkin skin = new SplineSkin();
        skin.autoSkin(this, layoutWindow, objects);
        
        /*
        // 1) Find the maximum number of vertecies from all selected curves.
        int maxPoints = 0;

        for (ObjectInfo obj : objects){
          if(obj.selected == true){
            System.out.println("Object Info: ");
            Object co = (Object)obj.getObject();
              if((co instanceof Curve) == true){
                System.out.println("Curve");

                Mesh mesh = (Mesh) obj.getObject(); // Object3D
                Vec3 [] verts = mesh.getVertexPositions();
                for (Vec3 vert : verts){
                    //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                }

                if(verts.length > maxPoints){
                    maxPoints = verts.length;
                }

              }
          }
        }
        System.out.println("max points: " + maxPoints);

        // 2) subdivide all curves with fewer points to meet the max.
        for (ObjectInfo obj : objects){
          if(obj.selected == true){
            System.out.println("Object Info: ");
            Object co = (Object)obj.getObject();
              if((co instanceof Curve) == true){
                System.out.println("Curve");

                Mesh mesh = (Mesh) obj.getObject(); // Object3D
                Vec3 [] verts = mesh.getVertexPositions();
                for (Vec3 vert : verts){
                    //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                }

                if(verts.length < maxPoints){

                    // Subdivide
                    int addPoints = maxPoints - verts.length;




                }

              }
          }
        }

         */
    }
    
    
    /**
     * connectedCurvesToMesh
     *  Description: Called by LayoutWIndow.
     *  @param LayoutWindow
     */
    public void connectedCurvesToMesh(LayoutWindow layoutWindow){
        SplineSkin skin = new SplineSkin();
        skin.connectedCurvesToMesh(this, layoutWindow, objects);
    }
    
    /**
     * connectedCurvesToQuadMesh
     * Description: 
     */
    public void connectedCurvesToQuadMesh(LayoutWindow layoutWindow, boolean debug, int subdivisions, int mode){
        SplineSkin skin = new SplineSkin();
        skin.removeSplineMesh(this);
        skin.connectedCurvesToQuadMesh(this, layoutWindow, objects, debug, subdivisions, mode);
        //skin.connectedCurvesToTriangleMesh(this, layoutWindow, objects, debug, subdivisions);
    }
    
    public void connectedCurvesToTriangleMesh(LayoutWindow layoutWindow, boolean debug, int subdivisions){
        SplineSkin skin = new SplineSkin();
        //skin.removeSplineMesh(this);
        //skin.connectedCurvesToTriangleMesh(this, layoutWindow, objects, debug, subdivisions);
    }
    
    public void autoSkinByVoids(LayoutWindow layoutWindow){
        SplineSkin skin = new SplineSkin();
        skin.autoSkinByVoids(this, layoutWindow, objects);
    }

    /**
     * splineGridSkin
     *
     * Description: Create mesh from spline mesh in grid form contained as shildren of selected mesh.
     *  Or create mesh object given selected splines?
     */
    public void splineGridSkin(LayoutWindow layoutWindow){
        SplineSkin skin = new SplineSkin();
        
        skin.splineGridSkin(this, layoutWindow, objects);
        
        /*
        Vector curves = new Vector();
        
        for (ObjectInfo obj : objects){
          if(obj.selected == true){
            System.out.println("Object Info: ");
            Object co = (Object)obj.getObject();
              if((co instanceof Curve) == true){
                System.out.println("Curve");

                Mesh mesh = (Mesh) obj.getObject(); // Object3D
                Vec3 [] verts = mesh.getVertexPositions();
                
                for (Vec3 vert : verts){
                    //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                }
                  
                  curves.addElement(verts);

                //if(verts.length > maxPoints){
                    //maxPoints = verts.length;
                //}

              }
          }
        }
        System.out.println("XXX: " );
        */
    }
    
    /**
     * joinMultipleSplines
     *
     * Description:
     */
    public void joinMultipleSplines(LayoutWindow layoutWindow){
        JoinSplines join = new JoinSplines();
        join.joinSplines(this, layoutWindow, objects);
    }
    
    
    /**
     * straightenSpline
     *
     * Desciption:
     */
    public void straightenSpline(LayoutWindow layoutWindow){
        System.out.println("Straighten Spline ");
        StraightenSpline straighten = new StraightenSpline(layoutWindow);
        straighten.straightenSpline(objects);
    }
    
    /**
     * getSplineLength
     *
     * Description: Calculate linear length of a spline object.
     */
    public void getSplineLength(LayoutWindow layoutWindow){
        System.out.println("Get spline length");
        double linearLength = 0;
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                //System.out.println("Object Info: ");
                Object co = (Object)obj.getObject();
                if((co instanceof Curve) == true){
                    CoordinateSystem cs = ((ObjectInfo)obj).getCoords();
                    Mesh mesh = (Mesh) obj.getObject(); // Object3D
                    Vec3 [] verts = mesh.getVertexPositions();
                    Vec3 vecPoints[] = new Vec3[verts.length];
                    for(int i = 0; i < verts.length; i++){
                        vecPoints[i] = verts[i];
                    }
                    Vector ignoreChildren = new Vector();
                    for(int i = 1; i < verts.length; i++){
                        Vec3 vertA = verts[i - 1];
                        Vec3 vertB = verts[i];
                        Vec3 worldVertA = new Vec3(vertA);
                        Vec3 worldVertB = new Vec3(vertB);
                        cs = ((ObjectInfo)obj).getCoords();
                        Mat4 mat4 = cs.duplicate().fromLocal();
                        mat4.transform(worldVertA);
                        mat4.transform(worldVertB);
                        double distance = Math.sqrt(Math.pow(vertA.x - vertB.x, 2) + Math.pow(vertA.y - vertB.y, 2) + Math.pow(vertA.z - vertB.z, 2));
                        linearLength += distance;
                    }
                    if(((Curve)co).isClosed()){ // If spline is closed loop add distance from verticies on the ends.
                        Vec3 vertA = verts[verts.length - 1];
                        Vec3 vertB = verts[0];
                        Vec3 worldVertA = new Vec3(vertA);
                        Vec3 worldVertB = new Vec3(vertB);
                        cs = ((ObjectInfo)obj).getCoords();
                        Mat4 mat4 = cs.duplicate().fromLocal();
                        mat4.transform(worldVertA);
                        mat4.transform(worldVertB);
                        double distance = Math.sqrt(Math.pow(vertA.x - vertB.x, 2) + Math.pow(vertA.y - vertB.y, 2) + Math.pow(vertA.z - vertB.z, 2));
                        linearLength += distance;
                    }
                }
                
                // If object is ArcObject then get length
                
                
            }
        }
        
        double scale = getScale();
        linearLength = linearLength * scale;
        
        System.out.println(" spline length " + linearLength);
        
        JOptionPane.showMessageDialog(null, "Spline length: " + linearLength);
    }

    /**
     * getMeshLength
     *
     * Description: calculate the length of a mesh object by treating is as a bent tube.
     */
    public void getMeshLength(LayoutWindow layoutWindow){
        /*
         1 Start with the first virtex.
         2 for each connected vertex select the furthest away and calculate the distance.
         3 Only evaluate each vertex once.
         4 repeat process (1-3 with furthest vertex, ignoring verticies allready reviewed.)
         5 sum and return the total distances from all processes.
         
         // This wonn't work for some geometry.
         */
    }
    
  /**
  * getObjectInfo
  *
  * Description: Get object details.
  */
  public void getObjectInfo(){
      StringBuilder text = new StringBuilder();
      double totalArcLength = 0;
      int arcObjects = 0;
      
      // Hashmap arc tube thicknesses.
      HashMap<Double, Double> hm = new HashMap<Double, Double>();
      HashMap<String, Double> hm_ = new HashMap<String, Double>();
      
      for (ObjectInfo obj : objects){
        if(obj.selected == true){
          System.out.println("Object Info: ");
          Object co = (Object)obj.getObject();
            System.out.println(" Type: " + co.getClass().getName());
            System.out.println(" ID: " + obj.getId());
            System.out.println(" Replicate ID: " + obj.getReplicateId());
            
            if(obj.selected == true){
                String field = new String("Selected Object Type: " + co.getClass().getName() + "\n");
                text.append(field);
                
                field = new String("Selected Object ID: " + obj.getId() + "\n");
                text.append(field);
                
                field = new String("Selected Object Replicate ID: " + obj.getReplicateId() + "\n");
                text.append(field);
                
                // Parent ID
                ObjectInfo parent = obj.getParent();
                if(parent != null){
                    field = new String("Selected Object Parent ID: " + parent.getId() + "\n");
                    text.append(field);
                } else {
                    field = new String("Selected Object Parent: None" +  "\n");
                }
                
                // Children
                ObjectInfo[] children = obj.getChildren();
                if(children != null){
                    field = new String("Selected Object Children: [ ");
                    for(int c = 0; c < children.length; c++){
                        ObjectInfo child = (ObjectInfo)children[c];
                        if(c > 0){
                            field += ",";
                        }
                        field += " " + child.getId();
                    }
                    field += " ] \n";
                    text.append(field);
                }
            }
            
            
            // Children
            ObjectInfo[] children = obj.getChildren();
            String childrenField = new String("Children: " + children.length + "\n");
            text.append(childrenField);
            
            // Parent tree
            ObjectInfo parent = obj.getParent();
            if(parent != null){
                System.out.println("Parent: " + parent.getName());
                parent = parent.getParent();
                if(parent != null){
                    System.out.println(" -> Parent: " + parent.getName());
                    
                    String field = new String("Parent: " + parent.getName() + "\n");
                    text.append(field);
                }
            } else {
                System.out.println("No Parent. ");
            }
            
            
            String materialId = new String("Material ID: " + obj.getPhysicalMaterialId() + "\n");
            text.append(materialId);
            
            // Curve
            if((co instanceof Curve) == true){
              System.out.println("Curve");
              int points = 0;

              //CoordinateSystem c;

              //c = layout.getCoords(obj); // Read cutting coord from file

              //childClone.setCoords(c);
              Mesh mesh = (Mesh) obj.getObject(); // Object3D
              Vec3 [] verts = mesh.getVertexPositions();
              for (Vec3 vert : verts){
                      // Transform vertex points around object loc/rot.
                      //Mat4 mat4 = c.duplicate().fromLocal();
                      //mat4.transform(vert);

                      // Apply scale
                      //vert.x = vert.x * scale;
                      //vert.y = vert.y * scale;
                      //vert.z = vert.z * scale;

                      points++;
               }

                System.out.println("Points " + points);
                //JOptionPane.showMessageDialog(window, "Object Info.","Points", JOptionPane.WARNING_MESSAGE);
                //new BStandardDialog("", Translate.text("cannotTriangulate"), BStandardDialog.ERROR).showMessageDialog(this);
                //JOptionPane.showMessageDialog(null, "Curve Points: " + points, "Object Info", JOptionPane.WARNING_MESSAGE);

                if(points > 0){
                    String field = new String("Curve points: " + points + "\n");
                    System.out.println(field);
                    text.append(field);
                }
                
                //int smooth = ((Curve)co).getSmoothingMethod();
                //String smoothField = new String("Smooth method: " + smooth + "\n");
                //text.append(smoothField);
                //float[] curveSmoothness = ((Curve)co).getSmoothness();
                //for(int i = 0; i < curveSmoothness.length; i++){
                //    System.out.println(" sm: " + curveSmoothness[i]);
                //}
          }
            
            if((co instanceof Mesh) == true){
                System.out.println("Mesh");
                int points = 0;
                
                //CoordinateSystem c;
                
                //c = layout.getCoords(obj); // Read cutting coord from file
                
                //childClone.setCoords(c);
                Mesh mesh = (Mesh) obj.getObject(); // Object3D
                Vec3 [] verts = mesh.getVertexPositions();
                for (Vec3 vert : verts){
                    // Transform vertex points around object loc/rot.
                    //Mat4 mat4 = c.duplicate().fromLocal();
                    //mat4.transform(vert);
                    
                    // Apply scale
                    //vert.x = vert.x * scale;
                    //vert.y = vert.y * scale;
                    //vert.z = vert.z * scale;
                    
                    points++;
                }
                
                System.out.println("Points " + points);
                //JOptionPane.showMessageDialog(window, "Object Info.","Points", JOptionPane.WARNING_MESSAGE);
                //new BStandardDialog("", Translate.text("cannotTriangulate"), BStandardDialog.ERROR).showMessageDialog(this);
                //JOptionPane.showMessageDialog(null, "Mesh Points: " + points, "Object Info", JOptionPane.WARNING_MESSAGE);
                
                if(points > 0){
                    String field = new String("Mesh points: " + points + "\n");
                    System.out.println(field);
                    text.append(field);
                }
            }
            
            if((co instanceof SplineMesh) == true){
                System.out.println("SplineMesh *** ");
                
                SplineMesh sm = (SplineMesh)co;
                text.append("SplineMesh: " + sm.getObjectInfo(getScale()));
                
            } else {
                System.out.println("no");
            }
            
            if((co instanceof PointJoinObject) == true){
                System.out.println("PointJoinObject  ");
                
                PointJoinObject join = (PointJoinObject) obj.getObject(); // Object3D
                Vec3 [] verts = join.getVertexPositions();
                for (Vec3 vert : verts){
                    System.out.println("  vec  " + vert.x + " " + vert.y + " " + vert.z);
                }
                
            }
        }
          
        // Arc
          if(obj.getObject() instanceof ArcObject && obj.selected == true ){
              ArcObject arc = (ArcObject)obj.getObject();
              if(obj.isVisible() && obj.isChildrenHiddenWhenHidden() == false){
                  
                  String info = arc.getObjectInfo();
                  if(info.length() > 0){
                      text.append("Arc Object: " + info + "");
                  }
                  
                  double diameter_ = arc.getTubeDiameter();
                  double thickness_ = arc.getTubeWall();
                  
                  DecimalFormat df = new DecimalFormat("#.####");
                  df.setRoundingMode(RoundingMode.CEILING);
                  diameter_ = new Double(df.format(diameter_) ).doubleValue();
                  thickness_= new Double(df.format(thickness_) ).doubleValue();
                  
                  String key = diameter_ + "_" + thickness_;
                  
                  //System.out.println("diameter: " + diameter_);
                  
                  Mesh mesh = (Mesh) obj.getObject(); // Object3D
                  Vec3 [] verts = mesh.getVertexPositions();
                  double segmentLength = 0;
                  for(int i = 1; i < verts.length; i++){
                      double d = ((Vec3)verts[i - 1]).distance(((Vec3)verts[i - 0]));
                      totalArcLength += d;
                      segmentLength += d;
                  }
                  
                  Double existing = hm.get(diameter_);
                  if(existing == null){
                      existing = new Double(0);
                  }
                  existing += segmentLength;
                  hm.put(diameter_, existing);
                    
                  
                  Double existing_ = hm_.get(key);
                  if(existing_ == null){
                      existing_ = 0.0;
                  }
                  existing_ += segmentLength;
                  hm_.put(key, existing_);
                  
                  arcObjects++;
                  
                  Vector<Double> arcLengths = arc.getArcSegmentLength(); // calculates bend length.
                  double arcLength = 0;
                  for(int i = 0; i < arcLengths.size(); i++){
                      double segLength = arcLengths.elementAt(i);
                      arcLength += segLength;
                  }
                  //text.append("Arc profile length: " + round(arcLength) + "\n");
              }
          }
          
      } // objects
      
      
      
      if(totalArcLength > 0){
          DecimalFormat df = new DecimalFormat("#.##");
          df.setRoundingMode(RoundingMode.CEILING);
          
          text.append("Arc Objects: " + arcObjects + "\n");
          
          
          String field = new String("Total Arc length: " + round(totalArcLength) + " \n");
          text.append(field);
          
          double scale = getScale();
          if(scale == 0){
              scale = 1.0;
          }
          
          field = new String("Total arc length Scaled: " + round( (totalArcLength * scale) / 12 ) + " feet\n");
          text.append(field);
          
          double totalCost = 0;
          double totalWeight = 0;
          double totalLength = 0;
          /*
          for (Double key : hm.keySet()) {
              System.out.println("  " + key + " : " + hm.get(key));
              
              double diameterInches = (key * scale);
              double lengthFeet = ( (hm.get(key) * scale) /12 );
              diameterInches = round(diameterInches); //  new Double(df.format(diameterInches) ).doubleValue() ;
              lengthFeet = round(lengthFeet); // new Double(df.format(lengthFeet) ).doubleValue() ;
              
              double weight = 0;
              double cost = 0;
              if(diameterInches == 2.0){  // .095   .083
                  cost += 9.0 * lengthFeet; // 15
                  //weight += 1.9308 * lengthFeet; // 095
                  weight += 1.6975 * lengthFeet; // 083
                  totalCost += cost;
                  totalWeight += weight;
              }
              if(diameterInches == 1.75){  // .120
                  cost += 11.8 * lengthFeet;
                  weight += 1.6774 * lengthFeet; //  1.6774      2.08
                  totalCost += cost;
                  totalWeight += weight;
              }
              if(diameterInches == 1.5){  // .120  .095
                  cost += 12.7 * lengthFeet;
                  weight += 1.4240 * lengthFeet;
                  totalCost += cost;
                  totalWeight += weight;
              }
              if(diameterInches == 1.0){  // .120
                  cost += 4.05 * lengthFeet;
                  weight += 0.8120 * lengthFeet; // 0.8120   0.91
                  totalCost += cost;
                  totalWeight += weight;
              }
              
              text.append(  "Diameter: " + diameterInches + " Length: " + lengthFeet + " Feet. $" +df.format(cost)+ " lbs: " +df.format(weight)+" \n");
          }
          text.append("Total Cost : $" + df.format(totalCost) + " Total Weight: " + df.format(totalWeight) + "\n");
          */
          text.append("\n");
          
          totalCost = 0;
          totalWeight = 0;
          totalLength = 0;
          for (String key : hm_.keySet()) {
              //System.out.println("2  " + key + " : " + hm_.get(key));
              double diameterInches = 0;
              double wallInches = 0;
              int index = key.indexOf("_");
              if(index != -1){
                  diameterInches = new Double(key.substring(0, index)).doubleValue();
                  
                  wallInches = new Double(key.substring(index + 1)).doubleValue();
                  diameterInches = round(diameterInches * scale);
                  wallInches = roundK(wallInches * scale);
                  double lengthFeet = ( (hm_.get(key) * scale) /12 );
                  
                  //System.out.println( " ---- " + diameterInches + " " + wallInches);
                  
                  double weight = 0;
                  double cost = 0;
                  
                  
                  double solidArea = Math.PI * ((diameterInches/2) * (diameterInches/2));
                  double wr = (diameterInches - (wallInches*2))/2;
                  double holeArea = Math.PI * (wr * wr);
                  double tubeVolume = (solidArea - holeArea) * (lengthFeet * 12);
                  double tubeWeight = tubeVolume * 0.2833;
                  //text.append(  "    Tube volume: " + round(tubeVolume) + " tubeWeight: " + round(tubeWeight) + "\n");
                  weight += tubeWeight;
                  totalLength += lengthFeet;
                  
                  if(isClose(diameterInches, 2.0, 0.01) && isClose(wallInches, 0.095, 0.002)){  // diameterInches == 2.0 && wallInches == 0.095
                      cost += 16 * lengthFeet;
                      //weight += 1.9308 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  //if(diameterInches == 2.0 && wallInches == 0.083) { //
                  if(isClose(diameterInches, 2.0, 0.01) && isClose(wallInches, 0.083, 0.002)){
                      cost += 16 * lengthFeet;
                      //weight += 1.6975 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  
                  //if(diameterInches == 1.75 && wallInches == 0.120){  // .120
                  if(isClose(diameterInches, 1.75, 0.01) && isClose(wallInches, 0.120, 0.002)){
                      cost += 11.8 * lengthFeet;
                      //weight += 2.08 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  //if(diameterInches == 1.75 && wallInches == 0.095){  // .095
                  if(isClose(diameterInches, 1.75, 0.01) && isClose(wallInches, 0.095, 0.002)){
                      cost += 11.8 * lengthFeet;
                      //weight += 1.6774 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  //if(diameterInches == 1.75 && wallInches == 0.083){  // .083
                  if(isClose(diameterInches, 1.75, 0.01) && isClose(wallInches, 0.083, 0.002)){
                      cost += 11.8 * lengthFeet;
                      //weight += 1.4761 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  
                  //if(diameterInches == 1.5 && wallInches == 0.120){  // .120
                  if(isClose(diameterInches, 1.5, 0.01) && isClose(wallInches, 0.120, 0.002)){
                      cost += 12.7 * lengthFeet;
                      //weight += 1.7668 * lengthFeet;  // 1.7668
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  //if(diameterInches == 1.5 && wallInches == 0.095){  // .095
                  if(isClose(diameterInches, 1.5, 0.01) && isClose(wallInches, 0.095, 0.002)){
                      cost += 12.7 * lengthFeet;
                      //weight += 1.4240 * lengthFeet;  //
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  //if(diameterInches == 1.5 && wallInches == 0.083){  // .083
                  if(isClose(diameterInches, 1.5, 0.01) && isClose(wallInches, 0.083, 0.002)){
                      cost += 10 * lengthFeet;
                      //weight += 1.2548 * lengthFeet;  //
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  
                  
                  //if(diameterInches == 1.0 && wallInches == 0.095){
                  if(isClose(diameterInches, 1.0, 0.01) && isClose(wallInches, 0.095, 0.002)){
                      cost += 4.05 * lengthFeet;
                      //weight += 0.91 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  //if(diameterInches == 1.0 && wallInches == 0.083){  // .120
                  if(isClose(diameterInches, 1.0, 0.01) && isClose(wallInches, 0.083, 0.002)){
                      cost += 4.05 * lengthFeet;
                      //weight += 0.8120 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  //if(diameterInches == 1.0 && wallInches == 0.064){  // .064
                  if(isClose(diameterInches, 1.0, 0.01) && isClose(wallInches, 0.064, 0.002)){
                      cost += 4.05 * lengthFeet;
                      //weight += 0.6391 * lengthFeet;
                      totalCost += cost;
                      totalWeight += weight;
                  }
                  
                  
                  text.append(  "Diameter: " + diameterInches + " Wall: " + wallInches + " Length: " + round(lengthFeet) + " Feet. $" +df.format(cost)+ " lbs: " +df.format(weight)+" \n");
                  
              }
              
          }
          text.append("Total Cost : $" + df.format(totalCost) +
                      " Total Length: " + df.format(totalLength) +
                      " Total Weight: " + df.format(totalWeight) + "\n");
          
          
      }
      
      TextViewer textViewer = new TextViewer();
      textViewer.setText(text.toString());
      
      
    }
    
    public double round(double value){
        int i = (int) (value * 100);
        value = (double)i / 100;
        return value;
    }
    public double roundK(double value){
        int i = (int) (value * 1000);
        value = (double)i / 1000;
        return value;
    }
	
    
    /**
     * isClose
     * isCLose(2.0, 1.99, 0.01);
     */
    public boolean isClose(double a, double b, double range){
        if( a + (range/2) > b  && a - (range/2) < b ){
            return true;
        }
        return false;
    }
    
    
    /**
     * exportTubeBendGCode
     *
     * Description: Generate GCode that operates a bending machine.
     *  The bend profile is defined by a Curve object. Segment angles and the distance between them
     *  are used to create feed and press commends.
     */
    public void exportTubeBendGCode(){
        System.out.println("Export Tube Bend GCode.");
        
        LayoutModeling layout = new LayoutModeling();
        
        int fastRate = 1200;
        int slowRate = 50;
        
        String dir = getDirectory() + System.getProperty("file.separator") + getName() + "_gCode_tb";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        double scale = getScale();
        if(scale == 0){
            scale = 1.0;
        }
        
        // Calculate bounds of object to calculate centre for unrolling points around tube circumfrance.
        for (ObjectInfo obj : objects){
            boolean enabled = layout.isObjectEnabled(obj);
            ObjectInfo[] children = obj.getChildren();
            
            if(children.length > 0 && enabled){
                
                //System.out.println("   --- Object: " + obj.getName() + " count: " + children.length);
                //
                try {
                    boolean writeFile = false;
                    String gcode2 = "";
                    gcode2 += "; Arma Automotive\n";
                    gcode2 += "; Tube Bend\n";
                    gcode2 += "; Part: " + obj.getName() + "\n";
                    gcode2 += "G1\n";
                    Vector polygons = new Vector();
                    
                    double centreX = 0;
                    double centreY = 0;
                    double centreZ = 0;
                    
                    double boundsMinX = 9999;
                    double boundsMaxX = -9999;
                    double boundsMinY = 9999;
                    double boundsMaxY = -9999;
                    double boundsMinZ = 9999;
                    double boundsMaxZ = -9999;
                    
                    double minX = 9999;
                    double minY = 9999;
                    double minZ = 9999;
                    double maxX = -9999;
                    double maxY = -9999;
                    double maxZ = -9999;
                    
                    HashMap<Vector, Integer> polygonOrder = new HashMap<Vector, Integer>();
                    
                    //BoundingBox bounds = obj.getBounds();
                    //System.out.println(" bounds: " +
                    //                   " x: " + bounds.minx + " x " + bounds.maxx +
                    //                   " y" + bounds.miny + " maxy: " + bounds.maxy +
                    //                   " minz: " + bounds.minz + " maxz: " + bounds.maxz );
                    
                    ObjectInfo objClone = obj.duplicate();
                    objClone.setLayoutView(false);
                    Object co = (Object)objClone.getObject();
                    if(co instanceof Curve && objClone.isVisible() == true){ // && child_enabled
                        CoordinateSystem c;
                        c = layout.getCoords(objClone); // Read cutting coord from file
                        objClone.setCoords(c);
                        Mesh mesh = (Mesh) objClone.getObject(); // Object3D
                        Vec3 [] verts = mesh.getVertexPositions();
                        for (Vec3 vert : verts){
                            // Transform vertex points around object loc/rot.
                            Mat4 mat4 = c.duplicate().fromLocal();
                            mat4.transform(vert);
                            
                            if(vert.x > boundsMaxX){
                                boundsMaxX = vert.x;
                            }
                            if(vert.x < boundsMinX){
                                boundsMinX = vert.x;
                            }
                            if(vert.y > boundsMaxY){
                                boundsMaxY = vert.y;
                            }
                            if(vert.y < boundsMinY){
                                boundsMinY = vert.y;
                            }
                            if(vert.z > boundsMaxZ){
                                boundsMaxZ = vert.z;
                            }
                            if(vert.z < boundsMinZ){
                                boundsMinZ = vert.z;
                            }
                        }
                        double partLength = 0;
                        
                        String partGCode = "";
                        for (int i = 0; i < verts.length - 2; i++ ){
                            Vec3 vert = verts[i];
                            Vec3 vert2 = verts[i+1];
                            Vec3 vert3 = verts[i+2];
                            
                            float firstPair = getAngle( vert.x - vert2.x, vert.z - vert2.z );
                            float secondPair = getAngle( vert2.x - vert3.x, vert2.z - vert3.z );
                            float angle = Math.abs(secondPair - firstPair);
                            
                            double distance = Math.sqrt(Math.pow(vert.x - vert2.x, 2) + Math.pow(vert.y - vert2.y, 2) + Math.pow(vert.z - vert2.z, 2));
                            distance = distance * scale;
                            
                            double bendDistance = angle; // TODO: calculate this based on bender geometry.
                            // As the bend angle increases the distance is non linear becuase the bend arc is circular and the
                            // actuator is likely linear.
                            
                            //System.out.println(" " + i + " f " + firstPair + " -> " + secondPair);
                            //System.out.println(" " + i + " d: " + distance + "  angle: " + angle  );
                            
                            partGCode += "G1 X" +
                            roundThree(distance) +
                            " Y" +
                            roundThree(bendDistance) +
                            " Z" +
                            roundThree(0.0);
                            partGCode += " F"+10+"";
                            partGCode += ";\n"; // End line
                            
                            partLength += distance;
                        }
                        
                        
                        if(verts.length > 2){
                            Vec3 vert = verts[verts.length - 2];
                            Vec3 vert2 = verts[verts.length - 1];
                            double distance = Math.sqrt(Math.pow(vert.x - vert2.x, 2) + Math.pow(vert.y - vert2.y, 2) + Math.pow(vert.z - vert2.z, 2));
                            distance = distance * scale;
                            partLength += distance;
                        }
                        
                        //System.out.println("Bend   --- Object: " + obj.getName() + " count: " + children.length + " length: "+ partLength  );
                        gcode2 += "; Tube Length: " + roundThree(partLength) + "\n";
                        
                        gcode2 += partGCode;
                    }
                    
                    
                    try {
                        String gcodeFile = dir + System.getProperty("file.separator") + obj.getName() + ".gcode";
                        //gcodeFile += ".gcode";
                        System.out.println("Writing g code file: " + gcodeFile);
                        PrintWriter writer2 = new PrintWriter(gcodeFile, "UTF-8");
                        writer2.println(gcode2);
                        writer2.close();
                    } catch (Exception e){
                        System.out.println("Error: " + e.toString());
                    }
                    
                } catch (Exception e){
                    System.out.println("Error: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    /**
     * export3dCode
     *
     * Description: export mill gcode. find path.
     */
    public void export3dCode(LayoutWindow window){
       
        mill = new Mill();
        mill.setObjects(objects);
        mill.setScene(this);
        mill.setLayoutWindow(window);
       
        // Dialog
        if(mill.getUserInput()){
            //mill.exportGCode();
            mill.start();
        }
        
        //if(mill.isRunning() == false){
        //    mill.start();
        //}
    }
    
    public void export3dCode2(LayoutWindow window){
        
        mill2 = new Mill2();
        mill2.setObjects(objects);
        mill2.setScene(this);
        mill2.setLayoutWindow(window);
        
        // Dialog
        if(mill2.getUserInput()){
            //mill.exportGCode();
            mill2.start();
        }
        
        //if(mill.isRunning() == false){
        //    mill.start();
        //}
    }
    
    
    public float getAngle(double x, double y) {
        float angle = (float) Math.toDegrees(Math.atan2(y, x));
        if(angle < 0){
            angle += 360;
        }
        return angle;
    }
    
    public double getAngle3(Vec3 a, Vec3 b){
        double result = 0;
        
        // Temp not accurate
        result += Math.max(a.y, b.y) - Math.min(a.y, b.y);
        result += Math.max(a.z, b.z) - Math.min(a.z, b.z);

        double distance = a.distance(b);
        
        result = result / distance;
        
        return result;
    }
    
    // ---
    public void exportGCodeMesh(){
        LayoutModeling layout = new LayoutModeling();
        
        //layout.setBaseDir(this.getDirectory() + System.getProperty("file.separator") + this.getName() + "_layout_data" );
        
        //String dir = System.getProperty("user.dir") + System.getProperty("file.separator") + "gcode";
        String dir = getDirectory() + System.getProperty("file.separator") + getName() + "_gCode";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        double scale = 1.0;
        try {
            // Read current scale for this project.
            Properties prop = new Properties();
            InputStream input = null;
            OutputStream output = null;
            
            String dir2 = getDirectory() + System.getProperty("file.separator") + getName() + "_layout_data";
            File d2 = new File(dir2);
            if(d2.exists() == false){
                d2.mkdir();
            }
            dir2 = dir2 + System.getProperty("file.separator") + "scale.properties";
            d2 = new File(dir2);
            if(d2.exists() == false){
                //(works for both Windows and Linux)
                d2.getParentFile().mkdirs();
                d2.createNewFile();
            }
            
            input = new FileInputStream(dir2);
            // load a properties file
            prop.load(input);
            
            String v = prop.getProperty("export_scale");
            if(v != null){
                scale = Double.parseDouble(v);
            }
        } catch (Exception e){
            System.out.println("Error " + e);
            e.printStackTrace();
        }
        
        
        for (ObjectInfo obj : objects){
            String name = obj.getName();
            boolean enabled = layout.isObjectEnabled(obj);
            //System.out.println("   - name: " + name);
            ObjectInfo[] children = obj.getChildren();
            if(children.length > 0 && enabled){
                System.out.println("   - Group: " + name + " count: " + children.length);
                try {
                    // Create gcode file for group.
                    String gCodeFile = dir + System.getProperty("file.separator") + name + ".gcode";
                    //PrintWriter writer = new PrintWriter(gCodeFile, "UTF-8");
                    System.out.println("      File: " + gCodeFile);
                    
                    Vector polygons = new Vector();
                    // gcode
                    String gcode = "G1\n";
                    String gcode2 = "G1\n";
                    
                    double minX = 9999;
                    double minY = 9999;
                    double maxX = -9999;
                    double maxY = -9999;
                    double width = 0;
                    double depth = 0;
                    
                    double minZ = 9999;
                    double maxZ = -9999;
                    
                    boolean writeFile = false;
                    
                    for (ObjectInfo child : children){
                        ObjectInfo childClone = child.duplicate();
                        childClone.setLayoutView(false);
                        
                        String child_name = child.getName();
                        //System.out.println("      - child name: " + child_name);
                        Object co = (Object)child.getObject();
                        boolean child_enabled = layout.isObjectEnabled(child);
                        
                        //
                        // Polygon Flat (2d)
                        //
                        /*
                        if(co instanceof Mesh &&
                           child.isVisible() == true &&
                           co instanceof Curve &&
                           child_enabled){  // Is mesh and visible.
                            
                            System.out.println("      - Poly " + child_name);
                            writeFile = true;
                            //writer.println("# " + child_name );
                            Vector polygon = new Vector();
                            
                            // Apply layout coordinates
                            CoordinateSystem c;
                            //c = child.getCoords();
                            c = layout.getCoords(childClone); // Read cutting coord from file
                            //System.out.println(" coords " + c.getOrigin().x);
                            childClone.setCoords(c);
                            //c.setOrigin( );
                            
                            Vec3 origin = c.getOrigin(); // childClone.getCoords().getOrigin();
                            double angles[] = c.getRotationAngles(); //  childClone.getCoords().getRotationAngles();
                            //System.out.println(" " + origin.x + " " + c.getOrigin().x);
                            //System.out.println("         angles:  x " + angles[0] + " y " + angles[1] + " z " + angles[2]);
                            
                            Mesh mesh = (Mesh) childClone.getObject(); // Object3D
                            Vec3 [] verts = mesh.getVertexPositions();
                            for (Vec3 vert : verts){
                                // Transform vertex points around object loc/rot.
                                Mat4 mat4 = c.duplicate().fromLocal();
                                mat4.transform(vert);
                                
                                // Apply scale
                                vert.x = vert.x * scale;
                                vert.y = vert.y * scale;
                                vert.z = vert.z * scale;
                                
                                double x = vert.x; // + origin.x;  // works
                                double y = vert.y; // + origin.y;
                                double z = vert.z; // + origin.z;
                                
                                //System.out.println("         x " + x + " y: " + y + " z: " + z );
                                
                                //writer.println(" x " + x + " y: " + y + " z: " + z);
                                
                                polygon.addElement(vert);
                                
                                if(x < minX){
                                    minX = x;
                                }
                                if(x > maxX){
                                    maxX = x;
                                }
                                
                                if(y < minY){
                                    minY = y;
                                }
                                if(y > maxY){
                                    maxY = y;
                                }
                                
                                if(z < minZ){
                                    minZ = z;
                                }
                                if(z > maxZ){
                                    maxZ = z;
                                }
                            }
                            
                            // Reverse order
                            if(layout.getReverseOrder(child.getId() + "") == 1){
                                Collections.reverse(polygon);
                            }
                            
                            // Cycle polygon start.
                            // Allows cutting faccit deteail before long lines.
                            // polygons (Vector)
                            int start_offset = layout.getPointOffset(child.getId() + "");
                            System.out.println(" *** start_offset: " + start_offset);
                            //for(int pt = 0; pt < polygon.size(); pt++){
                            //}
                            while(start_offset > polygon.size() - 1){
                                start_offset = start_offset - polygon.size();
                            }
                            while(start_offset < 0){
                                start_offset = start_offset + polygon.size();
                            }
                            if(start_offset != 0 && polygon.size() > 0){
                                for(int i = 0; i < start_offset; i++){
                                    Vec3 vert = (Vec3)polygon.elementAt(0);
                                    polygon.remove(0);
                                    polygon.add(vert);
                                }
                            }
                            
                            polygons.addElement(polygon);
                        }
                        */
                        
                        //
                        // Mesh 3d
                        //   (Raise on each row...) Don't want to cut through surface to get to next row...
                        //
                        
                        if(co instanceof Mesh &&
                         child.isVisible() == true &&
                         (co instanceof Curve) == false &&
                         child_enabled){  // Is mesh and visible.
                         
                         System.out.println("      - Mesh " + child_name);
                         writeFile = true;
                         //writer.println("# " + child_name );
                         Vector polygon = new Vector();
                         
                         // Apply layout coordinates
                         CoordinateSystem c;
                         //c = child.getCoords();
                         c = layout.getCoords(childClone); // Read cutting coord from file
                         //System.out.println(" coords " + c.getOrigin().x);
                         childClone.setCoords(c);
                         //c.setOrigin( );
                         
                         Vec3 origin = c.getOrigin(); // childClone.getCoords().getOrigin();
                         double angles[] = c.getRotationAngles(); //  childClone.getCoords().getRotationAngles();
                         //System.out.println(" " + origin.x + " " + c.getOrigin().x);
                         //System.out.println("         angles:  x " + angles[0] + " y " + angles[1] + " z " + angles[2]);
                         
                         Mesh mesh = (Mesh) childClone.getObject(); // Object3D
                         Vec3 [] verts = mesh.getVertexPositions();
                         
                         int i = 0;
                         
                         for (Vec3 vert : verts){
                         // Transform vertex points around object loc/rot.
                         Mat4 mat4 = c.duplicate().fromLocal();
                         mat4.transform(vert);
                         
                         // Apply scale
                         vert.x = vert.x * scale;
                         vert.y = vert.y * scale;
                         vert.z = vert.z * scale;
                         
                         double x = vert.x; // + origin.x;  // works
                         double y = vert.y; // + origin.y;
                         double z = vert.z; // + origin.z;
                         
                         //System.out.println("         x " + x + " y: " + y + " z: " + z );
                         
                         //writer.println(" x " + x + " y: " + y + " z: " + z);
                         
                         polygon.addElement(vert);
                         
                         if(x < minX){
                         minX = x;
                         }
                         if(z < minY){
                         minY = z;
                         }
                         if(x > maxX){
                         maxX = x;
                         }
                         if(z > maxY){
                         maxY = z;
                         }
                         
                         if(y < minZ){
                         minZ = y;
                         }
                         if(y > maxZ){
                         maxZ = y;
                         }
                         }
                         
                         // Cycle polygon start.
                         // Allows cutting faccit deteail before long lines.
                         // polygons (Vector)
                         int start_offset = child.getCncPointOffset(); // layout.getPointOffset(child.getId() + "");
                         System.out.println(" *** start_offset: " + start_offset);
                         //for(int pt = 0; pt < polygon.size(); pt++){
                         //}
                         while(start_offset > polygon.size() - 1){
                             start_offset = start_offset - polygon.size();
                         }
                         while(start_offset < 0){
                             start_offset = start_offset + polygon.size();
                         }
                         if(start_offset != 0 && polygon.size() > 0){
                             for(int ii = 0; ii < start_offset; ii++){
                                 Vec3 vert = (Vec3)polygon.elementAt(0);
                                 polygon.remove(0);
                                 polygon.add(vert);
                             }
                         }
                         
                         polygons.addElement(polygon);
                         }
                        
                        
                        
                    }
                    // Close file
                    //writer.close();
                    
                    // Add boundary points (so you don't cut outside of the material or the clamps)
                    gcode2 += "G1 X" + roundThree(0) + " Y" + roundThree(0) + "\n"; // G90
                    gcode2 += "G1 X" + roundThree(maxX - minX) + " Y" + roundThree(0) + "\n"; // G90
                    gcode2 += "G1 X" + roundThree(maxX - minX) + " Y" + roundThree(maxZ - minZ) + "\n"; // G90
                    gcode2 += "G1 X" + roundThree(0) + " Y" + roundThree(maxZ - minZ) + "\n"; // G90
                    gcode2 += "G1 X" + roundThree(0) + " Y" + roundThree(0) + "\n"; // G90
                    gcode2 += "G1\n";
                    
                    // Sort polygons by order attribute
                    
                    
                    
                    //
                    // Write gcode file
                    // Iterate data
                    for(int p = 0; p < polygons.size(); p++){
                        System.out.println(" POLYGON ***");
                        
                        Vector polygon = (Vector)polygons.elementAt(p);
                        boolean lowered = false;
                        Vec3 firstPoint = null;
                        for(int pt = 0; pt < polygon.size(); pt++){
                            Vec3 point = (Vec3)polygon.elementAt(pt);
                            //System.out.println("  Point *** " + point.getX() + " " + point.getY());
                            
                            
                            point.x = (point.x + -minX); // shift to align all geometry to 0,0
                            point.z = (point.z + -minZ); //
                            
                            //point.z = (point.z + -minZ);
                            
                            gcode2 += "G1 X" +
                            roundThree(point.x) +
                            " Y" +
                            roundThree(point.z) +
                            //     " Z" +
                            //     roundThree(point.y) +
                            "\n"; // G90
                            if(!lowered){
                                gcode2 += "G00 Z-0.5\n"; // Lower router head for cutting.
                                lowered = true;
                                firstPoint = point;
                            }
                            
                            polygon.setElementAt(point, pt);
                        }
                        
                        // Connect last point to first point
                        if(firstPoint != null){
                            gcode2 += "G1 X" +
                            roundThree(firstPoint.x) +
                            " Y" +
                            roundThree(firstPoint.z) + "\n"; // G90
                        }
                        
                        gcode2 += "G00 Z0.5\n"; // Raise router head
                    }
                    
                    System.out.println("Width: " + (maxX - minX) + " Height: " + (maxZ - minZ));
                    System.out.println("Align: x: " + -minX + " y: " + -minZ);
                    
                    
                    // Write gcode to file
                    if(writeFile){
                        try {
                            
                            String gcodeFile = dir + System.getProperty("file.separator") + name + "";
                            gcodeFile += ".gcode";
                            System.out.println("Writing g code file: " + gcodeFile);
                            PrintWriter writer2 = new PrintWriter(gcodeFile, "UTF-8");
                            writer2.println(gcode2);
                            writer2.close();
                        } catch (Exception e){
                            System.out.println("Error: " + e.toString());
                        }
                        
                        // Multi part file
                        String gcode3 = gcode2;
                        int lines = 0; // StringUtils.countMatches(gcode2, "\n");
                        for(int i = 0; i < gcode3.length(); i++){
                            if(gcode3.charAt(i) == '\n'){
                                lines++;
                            }
                        }
                        if(lines > 499){
                            int lineNumber = 0;
                            int fileNumber = 1;
                            lines = 0;
                            for(int i = 0; i < gcode3.length(); i++){
                                if(gcode3.charAt(i) == '\n'){
                                    lines++;
                                    if(lines > 480){
                                        String gCodeSection = gcode3.substring(0, i);
                                        
                                        String gcodeFile = dir + System.getProperty("file.separator") + name + "_" + fileNumber;
                                        gcodeFile += ".gcode";
                                        System.out.println("Writing g code file: " + gcodeFile);
                                        PrintWriter writer2 = new PrintWriter(gcodeFile, "UTF-8");
                                        writer2.println(gCodeSection);
                                        writer2.close();
                                        
                                        fileNumber++;
                                        gcode3 = gcode3.substring(i+1, gcode3.length());
                                    }
                                }
                            }
                            String gcodeFile = dir + System.getProperty("file.separator") + name + "_" + fileNumber;
                            gcodeFile += ".gcode";
                            System.out.println("Writing g code file: " + gcodeFile);
                            PrintWriter writer2 = new PrintWriter(gcodeFile, "UTF-8");
                            writer2.println(gcode3);
                            writer2.close();
                            System.out.println(" Lines *** " + lines);
                        }
                        
                    }
                    
                } catch (Exception e){
                    System.out.println("Error: " + e);
                }
            }
        }
        System.out.println("Export mesh done. ");
    }
    // ---

    /**
    * exportDXF
    *
    * Description: Export all supported scene elements to DXF.
    */    
    public void exportDXF(){
        LayoutModeling layout = new LayoutModeling();
        
        //
        // Detect orientation of curve geometry. (X/Y or X/Z)
        //
        double sceneDepth = 0;
        double sceneHeight = 0;
        double minY_ = 9999;
        double maxY_ = -999;
        double minZ_ = 9999;
        double maxZ_ = -999;
        for (ObjectInfo obj : getObjects()){
            Object co = (Object)obj.getObject();
            boolean enabled = layout.isObjectEnabled(obj);
            if(enabled && co instanceof Curve){
                BoundingBox bounds = obj.getTranslatedBounds();
                if(bounds.miny < minY_){
                    minY_ = bounds.miny;
                }
                if(bounds.maxy > maxY_){
                    maxY_ = bounds.maxy;
                }
                if(bounds.minz < minZ_){
                    minZ_ = bounds.minz;
                }
                if(bounds.maxz > maxZ_){
                    maxZ_ = bounds.maxz;
                }
            }
        }
        sceneDepth = maxZ_ - minZ_;
        sceneHeight = maxY_ - minY_;
        boolean frontView = false;
        if(sceneDepth < sceneHeight){ // Front
            frontView = true;
        }
        
        
        
        DXFDocument dxfDocument = new DXFDocument("Arma Design Studio DXF Export");
        DXFGraphics dxfGraphics = dxfDocument.getGraphics();
        dxfGraphics.setColor(Color.BLACK);
        double scale = getScale();
        Vector polygons = new Vector();
        HashMap<Vector, Integer> polygonOrder = new HashMap<Vector, Integer>();
        for (ObjectInfo obj : objects){
            String name = obj.getName();
            ObjectInfo objClone = obj.duplicate(); 
            Object co = (Object)obj.getObject();
            if(co instanceof Mesh &&
                obj.isVisible() == true &&
                obj.isChildrenHiddenWhenHidden() == false
               //&& ((Mesh)obj) instanceof Curve
               ){  // Is mesh and visible.
                //System.out.println(".");
                Vector polygon = new Vector();

                CoordinateSystem objCoords = obj.getCoords();
                
                //CoordinateSystem c;
                //c = layout.getCoords(objClone);
                //objClone.setCoords(c);
                //Vec3 origin = c.getOrigin();
                
                Mesh mesh = (Mesh) objClone.getObject(); // Object3D
                Vec3 [] verts = mesh.getVertexPositions();
                for (Vec3 vert : verts){
                    // Transform vertex points around object loc/rot.
                    Mat4 mat4 = objCoords.duplicate().fromLocal(); // Apply object coordinate system (location, rotation, scale).
                    mat4.transform(vert);

                  // Apply scale
                  vert.x = vert.x * scale;
                  vert.y = vert.y * scale;
                  vert.z = vert.z * scale;

                  double x = vert.x; // + origin.x;  // works
                  double y = vert.y; // + origin.y;
                  double z = vert.z; // + origin.z;
                  //System.out.println("         x " + x + " y: " + y + " z: " + z );
                  //writer.println(" x " + x + " y: " + y + " z: " + z);
                  polygon.addElement(vert);
                }
                polygons.addElement(polygon);
	    } else {
              System.out.println(" no " + name + " " + obj);
            }
        }

        for(int p = 0; p < polygons.size(); p++){
          Vector polygon = (Vector)polygons.elementAt(p);
          Vector<RealPoint> realPoints = new Vector<RealPoint>();
          for(int pt = 0; pt < polygon.size(); pt++){
            Vec3 point = (Vec3)polygon.elementAt(pt);
            RealPoint realPoint = new RealPoint(point.x, point.y, point.z);
            
              // If front view swap y with z
              if(frontView == false){
                  realPoint = new RealPoint(point.x, -point.z, point.y);
              }
              
              realPoints.addElement(realPoint);
            //polygon.setElementAt(point, pt);
          }
          //System.out.println(" add polygon ");
          DXFLWPolyline polyline = new DXFLWPolyline( polygon.size(), realPoints, true, dxfGraphics); // int numVertices, Vector<RealPoint> vertices, boolean closed, Graphics2D graphics
          dxfDocument.addEntity(polyline);
        } 
        try {
          String stringOutput = dxfDocument.toDXFString();
          String projectName = getName();
          if(projectName.indexOf(".ads") != -1){
            projectName = projectName.substring(0, projectName.indexOf(".ads"));
          }
          String dxfFile = getDirectory() + System.getProperty("file.separator") + projectName + ".dxf";
          System.out.println("Writing dxf file: " + dxfFile);
          PrintWriter writer2 = new PrintWriter(dxfFile, "UTF-8");
          writer2.print(stringOutput);
          writer2.close();

          // Notify dialog.
          JOptionPane.showMessageDialog(null, "DXF export complete: " + dxfFile,  "Complete" , JOptionPane.ERROR_MESSAGE );

        } catch (Exception e){
          System.out.println("Error: " + e.toString());
        }

    }

    /**
    * exportLayoutDXF
    * Description: Export layout geometry to DXF. 
    */ 
    public void exportLayoutDXF(){
        LayoutModeling layout = new LayoutModeling();
        
        //layout.setBaseDir(this.getDirectory() + System.getProperty("file.separator") + this.getName() + "_layout_data" );
       
        DXFDocument dxfDocument = new DXFDocument("Arma Design Studion DXF Export");
        DXFGraphics dxfGraphics = dxfDocument.getGraphics();
        
        dxfGraphics.setColor(Color.BLUE);
        
        //Line2D line = new Line2D.Double(0, 0, 1.1, 1.1);
        //dxfGraphics.draw(line);

        /*
        dxfGraphics.setColor(Color.GREEN);
        RoundRectangle2D roundRect = new RoundRectangle2D.Double(211.3, 400, 110, 210, 10, 20);
        dxfGraphics.draw(roundRect);
        roundRect = new RoundRectangle2D.Double(211.3, 400, 110, 210, 20, 30);
        dxfGraphics.draw(roundRect);
        roundRect = new RoundRectangle2D.Double(211.3, 400, 110, 210, 30, 40);
        dxfGraphics.draw(roundRect);
        roundRect = new RoundRectangle2D.Double(211.3, 400, 110, 210, 40, 60);
        dxfGraphics.draw(roundRect);
        roundRect = new RoundRectangle2D.Double(211.3, 400, 110, 210, 60, 80);
        dxfGraphics.draw(roundRect);
        */
        //String stringOutput = dxfDocument.toDXFString();
        //System.out.println(stringOutput);

 
        //String dir = System.getProperty("user.dir") + System.getProperty("file.separator") + "gcode";
        String dir = getDirectory() + System.getProperty("file.separator") + getName() + "_dxf";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        double scale = getScale();
        for (ObjectInfo obj : objects){
            String name = obj.getName();
            boolean enabled = layout.isObjectEnabled(obj);
            //System.out.println("   - name: " + name);
            ObjectInfo[] children = obj.getChildren();
            if(children.length > 0 && enabled){
                System.out.println("   - Group: " + name + " count: " + children.length);
                try {
                    // Create gcode file for group.
                    String gCodeFile = dir + System.getProperty("file.separator") + name + ".dxf";
                    //PrintWriter writer = new PrintWriter(gCodeFile, "UTF-8");
                    System.out.println("      File: " + gCodeFile);
                    
                    Vector polygons = new Vector();
                    // gcode
                    String gcode = "";
                    String gcode2 = "";
                    
                    double minX = 9999;
                    double minY = 9999;
                    double maxX = -9999;
                    double maxY = -9999;
                    double width = 0;
                    double depth = 0;
                    
                    double minZ = 9999;
                    double maxZ = -9999;
                    
                    boolean writeFile = false;
                    
                    HashMap<Vector, Integer> polygonOrder = new HashMap<Vector, Integer>();
                    
                    for (ObjectInfo child : children){
                        ObjectInfo childClone = child.duplicate();
                        childClone.setLayoutView(false);
                        
                        String child_name = child.getName();
                        //System.out.println("      - child name: " + child_name);
                        Object co = (Object)child.getObject();
                        boolean child_enabled = layout.isObjectEnabled(child);
                        
                        
                        //
                        // Polygon Flat (2d)
                        //
                        if(co instanceof Mesh &&
                           child.isVisible() == true &&
                           co instanceof Curve &&
                           child_enabled){  // Is mesh and visible.
                            
                            //System.out.println("      - Poly " + child_name);
                            writeFile = true;
                            //writer.println("# " + child_name );
                            Vector polygon = new Vector();
                            
                            // Apply layout coordinates
                            CoordinateSystem c;
                            //c = child.getCoords();
                            c = layout.getCoords(childClone); // Read cutting coord from file
                            //System.out.println(" coords " + c.getOrigin().x);
                            childClone.setCoords(c);
                            //c.setOrigin( );
                            
                            Vec3 origin = c.getOrigin(); // childClone.getCoords().getOrigin();
                            double angles[] = c.getRotationAngles(); //  childClone.getCoords().getRotationAngles();
                            //System.out.println(" " + origin.x + " " + c.getOrigin().x);
                            //System.out.println("         angles:  x " + angles[0] + " y " + angles[1] + " z " + angles[2]);
                            
                            Mesh mesh = (Mesh) obj.getObject(); // Object3D
                            Vec3 [] verts = mesh.getVertexPositions();
                            for (Vec3 vert : verts){
                                // Transform vertex points around object loc/rot.
                                Mat4 mat4 = c.duplicate().fromLocal();
                                mat4.transform(vert);
                                
                                // Apply scale
                                vert.x = vert.x * scale;
                                vert.y = vert.y * scale;
                                vert.z = vert.z * scale;
                                
                                double x = vert.x; // + origin.x;  // works
                                double y = vert.y; // + origin.y;
                                double z = vert.z; // + origin.z;
                                
                                //System.out.println("         x " + x + " y: " + y + " z: " + z );
                                
                                //writer.println(" x " + x + " y: " + y + " z: " + z);
                                
                                polygon.addElement(vert);
                                
                                if(x < minX){
                                    minX = x;
                                }
                                if(x > maxX){
                                    maxX = x;
                                }
                                
                                if(y < minY){
                                    minY = y;
                                }
                                if(y > maxY){
                                    maxY = y;
                                }
                                
                                if(z < minZ){
                                    minZ = z;
                                }
                                if(z > maxZ){
                                    maxZ = z;
                                }
                            }
                            
                            // Reverse order
                            if(layout.getReverseOrder(child.getId() + "") == 1){
                                Collections.reverse(polygon);
                            }
                            
                            // Cycle polygon start.
                            // Allows cutting faccit deteail before long lines.
                            // polygons (Vector)
                            int start_offset = child.getCncPointOffset(); // layout.getPointOffset(child.getId() + "");
                            //System.out.println(" *** start_offset: " + start_offset);
                            //for(int pt = 0; pt < polygon.size(); pt++){
                            //}
                            while(start_offset > polygon.size() - 1){
                                start_offset = start_offset - polygon.size();
                            }
                            while(start_offset < 0){
                                start_offset = start_offset + polygon.size();
                            }
                            if(start_offset != 0 && polygon.size() > 0){
                                for(int i = 0; i < start_offset; i++){
                                    Vec3 vert = (Vec3)polygon.elementAt(0);
                                    polygon.remove(0);
                                    polygon.add(vert);
                                }
                            }
                            
                            // Insert this polygon into the correct sorted order.
                            
                            int polyOrder = child.getCncPolyOrder(); // layout.getPolyOrder( "" + child.getId() );
                            polygonOrder.put(polygon, polyOrder);
                            //System.out.println("  ****  " + child.getId() + "  polyOrder: " + polyOrder );
                            
                            int insertAtPos = 0;
                            boolean insertAtEnd = false;
                            //if(name.equals("Door")){
                            
                            
                            for(int pos = 0; pos < polygons.size(); pos++){
                                Vector poly = (Vector)polygons.elementAt(pos);
                                if(poly != null && polygonOrder != null){
                                    Object currPolyPos =  (Object)polygonOrder.get(poly);
                                    if(currPolyPos != null){
                                        int currPolyPosInt = ((Integer)currPolyPos).intValue();
                                        
                                        // If current poly order is less than the current in the existing list insert it before
                                        if( polyOrder < currPolyPosInt ){
                                            insertAtPos = pos - 0;
                                            pos = polygons.size() + 1; // break
                                            //break;
                                        }
                                    }
                                }
                            }
                            
                            if(polygons.size() > 0){
                                Vector lastPoly = (Vector)polygons.elementAt(polygons.size()-1);
                                Object lastPolyPos =  (Object)polygonOrder.get(lastPoly);
                                if(lastPolyPos != null){
                                    int lastPolyPosInt = ((Integer)lastPolyPos).intValue();
                                    if(polyOrder > lastPolyPosInt){
                                        insertAtEnd = true;
                                    }
                                }
                            }
                            
                            //System.out.println("  **** !!!!!!!!  " + child.getId() + "  insertAtPos: " + insertAtPos );
                            
                            if(insertAtEnd){
                                polygons.addElement(polygon);
                            } else {
                                //if(insertAtPos > 0){
                                //    System.out.println("insert at " + insertAtPos);
                                polygons.insertElementAt(polygon, insertAtPos);
                                //} else {
                                //    System.out.println("add element");
                                //    polygons.addElement(polygon);
                                //}
                            }
                        }
                    }
                    
                    // Add boundary points (so you don't cut outside of the material or the clamps)
                    
                  
                    
                    // Sort polygons by order attribute
                    
                    
                    
                    //
                    // Write dxf file
                    // Iterate data
                    for(int p = 0; p < polygons.size(); p++){
                        //System.out.println(" POLYGON ***");
                        
                        Vector polygon = (Vector)polygons.elementAt(p);
                        boolean lowered = false;
                        Vec3 firstPoint = null;
                   
                        Vector<RealPoint> realPoints = new Vector<RealPoint>();
 
                        for(int pt = 0; pt < polygon.size(); pt++){
                            Vec3 point = (Vec3)polygon.elementAt(pt);
                            
                            point.x = (point.x + -minX); // shift to align all geometry to 0,0
                            point.z = (point.z + -minZ); //
                            
                            RealPoint realPoint = new RealPoint(point.x, point.y, point.z);
                            realPoints.addElement(realPoint); 
                            
                            polygon.setElementAt(point, pt);
                        }

                        DXFLWPolyline polyline = new DXFLWPolyline( polygon.size(), realPoints, true, dxfGraphics); // int numVertices, Vector<RealPoint> vertices, boolean closed, Graphics2D graphics
                        dxfDocument.addEntity(polyline);                         

                        
                        // Connect last point to first point
                        //if(firstPoint != null){
                            //gcode2 += "G1 X" +
                            //roundThree(firstPoint.x) +
                            //" Y" +
                            //roundThree(firstPoint.z) + "\n"; // G90
                        //}
                    }
                    
                    System.out.println("Width: " + (maxX - minX) + " Height: " + (maxZ - minZ));
                    System.out.println("Align: x: " + -minX + " y: " + -minZ);
                    
                    
                    // Write gcode to file
                    if(writeFile){
                        try {
                            String stringOutput = dxfDocument.toDXFString();
 
                            String dxfFile = dir + System.getProperty("file.separator") + name + ".dxf";

                            System.out.println("Writing dxf file: " + dxfFile);

                            PrintWriter writer2 = new PrintWriter(dxfFile, "UTF-8");
                            writer2.print(stringOutput);
                            writer2.close();
                        } catch (Exception e){
                            System.out.println("Error: " + e.toString());
                        }
                    } else {
                        System.out.println("No geometry supported by DXF export.");
                    }
                    
                } catch (Exception e){
                    System.out.println("Error: " + e);
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Export done. ");
        // Notify dialog.
        JOptionPane.showMessageDialog(null, "DXF layout export complete. ",  "Complete" , JOptionPane.ERROR_MESSAGE );
    }
    
    
    /**
     * exportSTL
     *
     * Description: Export scene to STL format file.
     * TODO: Move this to a seperate file.
     */
    public void exportSTL(){
        LayoutModeling layout = new LayoutModeling();
        String dir = getDirectory() + System.getProperty("file.separator") + getName() + "_dxf";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        // Uses JCSG library
        //STL stl = new STL();
        /*
        CSG cube = new Cube(2).toCSG().color(Color.RED);
        CSG sphere = new Sphere(1.25).toCSG().color(Color.BLUE);
        CSG cyl = new Cylinder(0.5,3,16).toCSG().transformed(Transform.unity().translateZ(-1.5)).color(Color.GREEN);
        CSG cyl2 = cyl.transformed(Transform.unity().rotY(90));
        CSG cyl3 = cyl.transformed(Transform.unity().rotX(90));
         
        // perform union, difference and intersection
        CSG cubePlusSphere = cube.union(sphere);
        CSG cubeMinusSphere = cube.difference(sphere);
        CSG cubeIntersectSphere = cube.intersect(sphere);
        CSG cubeIntersectSphereCyl = cubeIntersectSphere.difference(cyl).difference(cyl2).difference(cyl3);
         
        // translate geometries to prevent overlapping
        CSG union = cube.
         union(sphere.transformed(Transform.unity().translateX(3))).
         union(cyl.transformed(Transform.unity().translateX(6))).
         union(cubePlusSphere.transformed(Transform.unity().translateX(9))).
         union(cubeMinusSphere.transformed(Transform.unity().translateX(12))).
         union(cubeIntersectSphere.transformed(Transform.unity().translateX(15))).
         union(cubeIntersectSphereCyl.transformed(Transform.unity().translateX(18)));
         
         
        String stlFile = dir + System.getProperty("file.separator") + name + ".stl";
        
        FileUtil.write(Paths.get("sample.stl"), union.toStlString());
        */
    }
    
    /**
     * getScale
     *
     * Description: Don't store scale in seperate file because they can't be sent easily.
     */
    public double getScale(){
        /*
        double scale = 1.0;
        try {
            // Read current scale for this project.
            Properties prop = new Properties();
            InputStream input = null;
            OutputStream output = null;
            
            String dir2 = getDirectory() + System.getProperty("file.separator") + getName() + "_layout_data";
            File d2 = new File(dir2);
            if(d2.exists() == false){
                d2.mkdir();
            }
            dir2 = dir2 + System.getProperty("file.separator") + "scale.properties";
            d2 = new File(dir2);
            if(d2.exists() == false){
                //(works for both Windows and Linux)
                d2.getParentFile().mkdirs();
                d2.createNewFile();
            }
            
            input = new FileInputStream(dir2);
            // load a properties file
            prop.load(input);
            
            String v = prop.getProperty("export_scale");
            if(v != null){
                scale = Double.parseDouble(v);
            }
        } catch (Exception e){
            System.out.println("Error " + e);
            e.printStackTrace();
        }
         */
        return scale;
    }
    
    public void setScale(double s){
        this.scale = s;
    }
    
    public void setUnits(int u){
        this.units = u;
    }
    
    public int getUnits(){
        return units;
    }
    
    public int getExportUnits(){
        return exportUnits;
    }
    
    public void setExportUnits(int u){
        this.exportUnits = u;
    }
    
    // not implemented.
    public void exportOBJ(){
        LayoutModeling layout = new LayoutModeling();
        String dir = getDirectory() + System.getProperty("file.separator") + getName() + "_gCode";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        double scale = getScale();
        
        // *** todo
    }

	String roundThree(double x){
		//double rounded = ((double)Math.round(x * 100000) / 100000);
		
		DecimalFormat df = new DecimalFormat("#");
        	df.setMaximumFractionDigits(3);

		return df.format(x);
	}
    
    String roundTwo(double x){
        //double rounded = ((double)Math.round(x * 100000) / 100000);
        
        DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(2);

        return df.format(x);
    }

    /**
     * setLayoutViewModeling
     *
     * Description: Called by LayoutWindow (main menu)
     */
	public void setLayoutViewModeling(){
		String dir = System.getProperty("user.dir") + System.getProperty("file.separator") + "layout_settings";
		File d = new File(dir);
		if(d.exists() == false){
			d.mkdir();
		}

		//
		String name = this.getName();
		dir = dir + System.getProperty("file.separator") + name;
		d = new File(dir);
		if(d.exists() == false){
			d.mkdir();
		}

		//selection
		/*
		ObjectInfo info = objects.elementAt(which);
		    if (!info.selected)
		      selection.addElement(which);
    		info.selected = true;
    	*/
    	for (ObjectInfo obj : objects){
			if(obj.selected == true){
                setChildObjectViewMode(obj, true);
                //setChildObjectViewModeTube(obj, false);
                
                /*
                String n = obj.getName();
				System.out.println("   - selected name: " + n);

				obj.setLayoutView(true);

				ObjectInfo[] children = obj.getChildren();
				if(children.length > 0){
					for (ObjectInfo child : children){
						child.setLayoutView(true);
					}
				}
                */
			}
		}
	}

    /**
     * setLayoutViewCutting
     * Description: Set Layout View
     */
	public void setLayoutViewCutting(){
		String dir = System.getProperty("user.dir") + System.getProperty("file.separator") + "layout_settings";
		File d = new File(dir);
		if(d.exists() == false){
			d.mkdir();
		}
		//
		String name = this.getName();
		dir = dir + System.getProperty("file.separator") + name;
		d = new File(dir);
		if(d.exists() == false){
			d.mkdir();
		}

		for (ObjectInfo obj : objects){
			if(obj.selected == true){
                setChildObjectViewMode(obj, false);
                
                /*
                 //String n = obj.getName();
                 //System.out.println("   - selected name: " + n);
				obj.setLayoutView(false);
				ObjectInfo[] children = obj.getChildren();
				if(children.length > 0){
					for (ObjectInfo child : children){
						child.setLayoutView(false);
					}
				}
                */
			}
		}
	}
    
    /**
     * resetLayoutView
     *
     * Description:
     */
    public void resetLayoutView(){
        LayoutModeling layoutModeling = new LayoutModeling();
        
        String dir = System.getProperty("user.dir") + System.getProperty("file.separator") + "layout_settings";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        //
        String name = this.getName();
        dir = dir + System.getProperty("file.separator") + name;
        d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                // TODO: remove layout file for object id
                //
                //
                System.out.println(" reset " + obj.getId() );
                layoutModeling.deleteLayout(obj);
                
                // Recrse through child objects
                ObjectInfo[] children = obj.getChildren();
                if(children.length > 0){
                    for(ObjectInfo child : children){
                        
                        layoutModeling.deleteLayout(child);
                        
                    }
                }
                
            }
        }
    }
    
    // DEPRICATE
    public void setLayoutViewTube(){
        //System.out.println("setLayoutViewTube");
        String dir = System.getProperty("user.dir") + System.getProperty("file.separator") + "layout_settings";
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        //
        String name = this.getName();
        dir = dir + System.getProperty("file.separator") + name;
        d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
            
                // setChildObjectViewMode(obj, false);
                //obj.setLayoutView(false);
                //obj.setTubeLayoutView(true);
                
                setChildObjectViewModeTube(obj, true);
                
            }
        }
    }
    
    public void debug(){
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
            
                LayoutModeling layoutModeling = new LayoutModeling();
                CoordinateSystem c = layoutModeling.getCoords(obj);  // BREAKS !!!!!!!!
                
                CoordinateSystem cx = obj.getCoords();
                
                System.out.println("Scene.debug: " + obj.getName() +
                                   "  x " + c.getOrigin().x + " y " + c.getOrigin().y +
                                   " z " + c.getOrigin().z);
                System.out.println("Scene.debug: cx "  +
                                   "  x " + cx.getOrigin().x + " y " + cx.getOrigin().y +
                                   " z " + cx.getOrigin().z);
                
            }
        }
    }
    
    
    /**
     * setChildObjectViewMode
     *
     * Recursivly set all object and childred view mode to given value.
     */
    public void setChildObjectViewMode(ObjectInfo obj, boolean layout){
        String n = obj.getName();
        //System.out.println("   - selected name: " + n);
        
        // Set object view mode
        obj.setLayoutView(layout);
        
        // Recrse through child objects
        ObjectInfo[] children = obj.getChildren();
        if(children.length > 0){
            for(ObjectInfo child : children){
                setChildObjectViewMode(child, layout);
            }
        }
    }
    
    // Tube
    public void setChildObjectViewModeTube(ObjectInfo obj, boolean layout){
        String n = obj.getName();
        //System.out.println("   - selected name: " + n);
        
        // Set object view mode
        //obj.setLayoutView(layout);
        obj.setTubeLayoutView(layout);
        
        // Recrse through child objects
        ObjectInfo[] children = obj.getChildren();
        if(children.length > 0){
            for(ObjectInfo child : children){
                setChildObjectViewModeTube(child, layout);
            }
        }
    }


	public void gcodeDisablePoly(){
		LayoutModeling layout = new LayoutModeling();
		for (ObjectInfo obj : objects){
			if(obj.selected == true){
				layout.disableObject( obj );
			}
		}
	}

	public void gcodeEnablePoly(){
		LayoutModeling layout = new LayoutModeling();
		for (ObjectInfo obj : objects){
			if(obj.selected == true){
				layout.enableObject( obj );
			}
		}
	}


	public void setGCodePolyOrder(){
		LayoutModeling layout = new LayoutModeling();
		for (ObjectInfo obj : objects){
			if(obj.selected == true){
				layout.setGCodePolyOrder( obj );
			}
		}
	}
    
    /**
     * setGCodePolyOrdersBySize
     * Description: Used to set object poly order by size as to create GCode that cuts smaller objects first.
     *  Once objects are cut any objects within are no longer fixed in place.
     */
    public void setGCodePolyOrdersBySize(){
        // Scale. Find min and max bound size values and scale to a reasonable integer range.
        double min = Double.MAX_VALUE;
        double max = 0;
         
        //LayoutModeling layout = new LayoutModeling();
        for (ObjectInfo obj : objects){
            // Get object bounds.
            BoundingBox bounds = obj.getBounds();
            
            double size = (double)(
                          (bounds.maxx - bounds.minx) * (bounds.maxy - bounds.miny) * (bounds.maxz - bounds.minz)
                         );
            if(size > max){
                    max = size;
            }
            if(size < min){
                min = size;
            }
            
            System.out.println(" --- " + obj.getName() + " size " + size + " " +  (bounds.maxx - bounds.minx)   );
            //if(obj.selected == true){
                //layout.setGCodePolyOrdersBySize();
            //}
        }
        
        // scale
        double scaleFactor = 1;
        if(min < 1){
            scaleFactor = 2;
        }
        
        // Set poly order based on object size * scaleFactor.
        for (ObjectInfo obj : objects){
            
        }
        
        /*
        if(this.getObject() != null){
            Object3D o3d = this.getObject().duplicate();
            BoundingBox bounds = o3d.getBounds();
            // create an integer based on the object size.
            order = (int)(
                          (bounds.maxx - bounds.minx) * (bounds.maxy - bounds.miny) * (bounds.maxz - bounds.minz)
                         );
            
            //System.out.println("getDefaultPolyOrder " + (bounds.maxx - bounds.minx) );
        }
        */
    }
    
    
    public void setGCodePointOffset(){
        LayoutModeling layout = new LayoutModeling();
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                layout.setGCodePointOffset( obj );
            }
        }
    }


	public void setGCodePolyDepth(){
		LayoutModeling layout = new LayoutModeling();
		for (ObjectInfo obj : objects){
			if(obj.selected == true){
				layout.setGCodePolyDepth( obj );
			}
		}
	}

    
    public void setGCodePolyReverseOrder(){
        LayoutModeling layout = new LayoutModeling();
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                layout.setGCodeReverseOrder( obj );
            }
        }
    }
   
    /**
    * runCrashSimulation
    *
    * Description.
    */
    public void runCrashSimulation( BFrame frame ){
        CrashSimulation crash = new CrashSimulation(frame);
        crash.setObjects(objects);
        
        ObjectInfo wall = null;
        ObjectInfo meshObj = null;
        LayoutModeling layout = new LayoutModeling();
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                //layout.setObjectStructure( obj );
                meshObj = obj;
            }

            //System.out.println(" object: " + obj.getName() );
            if( obj.getName().equals("wall") ){
                System.out.println("found fall");
                wall = obj;
            }     
        }
        
        //if(meshObj == null){
        //   JOptionPane.showMessageDialog(null, "Select an object to crash.",  "alert" , JOptionPane.ERROR_MESSAGE );
        //   return;
        //}

        if (crash.clickedOk()){
            //crash.runImpact( meshObj );
            
            //CrashSimulation.ImpactThread impactThread;
           
            crash.impactThread.setObject(meshObj);
            crash.impactThread.start();
            
            //}
        }
    }
    
    
    //public Vector traverseObject( MeshVertex[] verts   ){
    //    Vector result = new Vector();
    //    return result;
    //}
   
 
    public void setObjectStructure(){
        LayoutModeling layout = new LayoutModeling();
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                layout.setObjectStructure( obj );
            }
        }
    }
    
    
    /**
     * copyStructureObjects
     *
     * Description: Select structure objects
     */
    public void copyStructureObjects(LayoutWindow window){
        //LayoutModeling layout = new LayoutModeling();
        clearSelection();
        for (int i = 0; i < objects.size(); i++)
        {
            ObjectInfo info = objects.elementAt(i);
            String structName = info.getGroupName(); //  layout.getObjectStructure( info.getId() + "" );
            
            if( structName.length() > 0 && !structName.equals("0") ){
                //if( structName.equals("chassis") || structName.equals("roof") ){
                //structObjects.addElement(info);
                System.out.println("Select obj id: " +   info.getId() + " i: "+i+"  " + info.getName() +"  struct: " + structName);
                
                //addToSelection(info.getId());
                //window.addToSelection(info.getId());
                window.addToSelection(i);
            }
        }
        updateSelectionInfo();
        window.updateImage();
        //theWindow = window; // depricate
    }
    
    /**
     * generateStructureMesh
     *
     * Description: Take defined objects and combine them into a new object by Boolean union,
     *    convert to triangle mesh and subdivide all of the edges to increase the number of polygons.
     *
     */
    public void generateStructureMesh(LayoutWindow window){
        //theWindow = window;
        LayoutModeling layout = new LayoutModeling();
        
        // Get list of objects to include in the structure
        Vector<ObjectInfo> structObjects = new Vector<ObjectInfo>();
        
        HashMap<String,Vector<ObjectInfo>> structureMap = new HashMap<String, Vector<ObjectInfo>>();
        
        for (int i = 0; i < objects.size(); i++)
        {
            ObjectInfo info = objects.elementAt(i);
            //applyTracksToObject(info, processed, null, i);
            String structName = layout.getObjectStructure( info.getId() + "" );
            
            if( structName.length() > 0 && !structName.equals("0") ){
            //if( structName.equals("chassis") || structName.equals("roof") ){
                structObjects.addElement(info);
                System.out.println(" obj id: " +   info.getId() + "  " + info.getName() +"  struct: " + structName);
            }
        }
        System.out.println("Parts: " + structObjects.size() );
        
        // Use Boolean Modeling to construct the mesh
        ObjectInfo unionInfo = null;
        Vec3 center = null;
        
        //ObjectInfo firstUnion = null;
        
        //Vector unions = new Vector();
        
        for (int i = 1; i < structObjects.size() && i < 200; i++)
        {
            if(i == 1){ // Join first two objects
                ObjectInfo info_1 = structObjects.elementAt(i - 1);
                ObjectInfo info_2 = structObjects.elementAt(i);
                //System.out.println(" struct obj id: " +   info_1.getId() + " 2 " + info_2.getId() );
                
                CSGObject newobj = new CSGObject(structObjects.elementAt(i-1), structObjects.elementAt(1), CSGObject.UNION);
                center = newobj.centerObjects();
                //CSGDialog dial = new CSGDialog(window /*LayoutWindow*/, newobj);
                //if (!dial.clickedOk())
                //    return;
                
                unionInfo = new ObjectInfo(newobj, new CoordinateSystem(center, Vec3.vz(), Vec3.vy()), "Boolean "+(i));
                //unionInfo = new ObjectInfo(newobj, new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy()), "Boolean "+(i));
                unionInfo.addTrack(new PositionTrack(unionInfo), 0);
                unionInfo.addTrack(new RotationTrack(unionInfo), 1);
                
                
                //firstUnion = unionInfo;
                //unions.addElement(unionInfo);
            
            } else { // Join next object with latest merged set (unionInfo)
                ObjectInfo info_2 = structObjects.elementAt(i);
                
                //System.out.println(" 2nd union  " + unionInfo);
                
                CSGObject newobj = new CSGObject(structObjects.elementAt(i), unionInfo.duplicate()   , CSGObject.UNION);
                //CSGObject newobj = new CSGObject(structObjects.elementAt(i), (ObjectInfo)unions.elementAt(unions.size()-1), CSGObject.UNION);
                //System.out.println(" - " + structObjects.elementAt(i) + " " + unionInfo);
                
                //try {
                //center = newobj.centerObjects();  // Null pointer
                //} catch (Exception e){ System.out.println("ERROR "); }
                
                // CoordinateSystem(Vec3 orig, Vec3 zdir, Vec3 updir)
                //ObjectInfo newUnionInfo = new ObjectInfo(newobj, new CoordinateSystem(center, Vec3.vz(), Vec3.vy()), "Boolean "+(i));
                ObjectInfo newUnionInfo = new ObjectInfo(newobj, new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy()), "Boolean "+(i));
                
                
                newUnionInfo.addTrack(new PositionTrack(newUnionInfo), 0);
                newUnionInfo.addTrack(new RotationTrack(newUnionInfo), 1);
                
                //unions.addElement(newUnionInfo);
                
                unionInfo = newUnionInfo;
            }
        }
        
        window.addObject(unionInfo, null);
        //window.addObject(firstUnion, null);
        
        //window.addObject((ObjectInfo)unions.elementAt(unions.size()-1), null);
        
        
        
        window.setSelection(this.getNumObjects()-1);
        //window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(this.getNumObjects()-1)}));
        window.updateImage();
        
        System.out.println("Completed union between objects.");
        
        //
        // Convert the union object to triangle mesh
        //
        
        int sel[] = this.getSelection();
        Object3D obj, mesh;
        ObjectInfo info;
        
        if (sel.length != 1)
            return;
        info = this.getObject(sel[0]);
        obj = info.getObject();
        if (obj.canConvertToTriangleMesh() == Object3D.CANT_CONVERT){
            System.out.println("Can't convert this object. ");
            return;
        }
        
        System.out.println(" --- " + obj.getClass().getName() );
        
        //setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()}));
        if (obj.canConvertToTriangleMesh() == Object3D.EXACTLY){
            System.out.println(" convertToTriangleMesh(0.0) ");
            mesh = obj.convertToTriangleMesh(0.0);              // may be too expensive
            //mesh = obj.convertToTriangleMesh(0.1);
        }
        else
        {
            ValueField errorField = new ValueField(0.2, ValueField.POSITIVE);
            //ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("selectToleranceForMesh"),
            //                                            new Widget [] {errorField}, new String [] {Translate.text("maxError")});
            //if (!dlg.clickedOk())
            //    return;
            System.out.println("convertToTriangleMesh(" + errorField.getValue() + ")");
            mesh = obj.convertToTriangleMesh(errorField.getValue());
        }
        if (mesh == null)
        {
            System.out.println("Error converting to mesh");
            //new BStandardDialog("", Translate.text("cannotTriangulate"), BStandardDialog.ERROR).showMessageDialog(this);
            //return;
        }
        mesh.setTexture(obj.getTexture(), obj.getTextureMapping());
        mesh.setMaterial(obj.getMaterial(), obj.getMaterialMapping());
        this.getObject(sel[0]).setObject(mesh);
        //updateImage();
        //updateMenus();
        System.out.println("Completed convert to mesh.");
        
        
        // ***
        // Subdivide mesh to increase number of polygons
        // ***
        /*
        TriangleMesh mesh2 = (TriangleMesh) info.getObject();
        
        //Edge edge[] = mesh2.edge;
        // edge.length
        
        int verticies = mesh2.getVertices().length;
        System.out.println("verticies " + verticies);
        
        boolean selected[] = new boolean [verticies * 4];  // how many actually????
        
        for (int i = 0; i < selected.length; i++)
            selected[i] = true; // Select all
        //for (int i = 0; i < selected.length; i++)
        //    if (isEdgeHidden(i))
        //        selected[i] = false;
        
        
         int i, j;
         TriangleMesh theMesh = (TriangleMesh) info.getObject(), newmesh;
         boolean newselection[];
         Edge edges[];
         Face faces[];
         
         //if (selectMode != EDGE_MODE && selectMode != FACE_MODE)
         //    return;
         for (i = 0; !selected[i] && i < selected.length; i++);  // WTF ???
         if (i == selected.length)
             return;
        
         System.out.println(".");
        
         //if (selectMode == EDGE_MODE)
         //{
             // Subdivide selected edges, using the appropriate method.
             
             i = theMesh.getVertices().length;
             if (theMesh.getSmoothingMethod() == TriangleMesh.APPROXIMATING)
                 newmesh = TriangleMesh.subdivideLoop(theMesh, selected, Double.MAX_VALUE);
             else if (theMesh.getSmoothingMethod() == TriangleMesh.INTERPOLATING)
                 newmesh = TriangleMesh.subdivideButterfly(theMesh, selected, Double.MAX_VALUE);
             else
                 newmesh = TriangleMesh.subdivideLinear(theMesh, selected);          // OUT OF BOUNDS
             info.setObject(newmesh);
        
        
             // Update the selection.
             // Needed??? Probably No.
             edges = newmesh.getEdges();
             newselection = new boolean [edges.length];
             for (j = 0; j < edges.length; j++)
                 newselection[j] = (edges[j].v1 >= i || edges[j].v2 >= i);
             //setSelection(newselection);
             selected = newselection;
        
         //}
         //else
         //{
        
             // Subdivide selected faces.
             
             //i = theMesh.getVertices().length;
             //newmesh = TriangleMesh.subdivideFaces(theMesh, selected);
             //setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {newmesh, theMesh}));
             //setMesh(newmesh);
             
             // Update the selection.
             
             //faces = newmesh.getFaces();
             //newselection = new boolean [faces.length];
             //for (j = 0; j < faces.length; j++)
             //    newselection[j] = (faces[j].v1 >= i || faces[j].v2 >= i || faces[j].v3 >= i);
             //setSelection(newselection);
         
         //}
        System.out.println("Completed subdivide mesh.");
        */
        
    }
    
    /**
     * evenMesh
     *
     * Description: Subdivide long edges and simplify clustered edges.
     */
    public void evenMesh(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        System.out.println("even Mesh ");
        
        double max_edge_length = 0.047;
        String len = JOptionPane.showInputDialog("Enter maximum edge length ", max_edge_length);
        if(len != null){
            max_edge_length = Double.parseDouble(len);
        }
        
        double min_edge_length = 0.004;
        String min_len = JOptionPane.showInputDialog("Enter minimum edge length ", min_edge_length);
        if(min_len != null){
            min_edge_length = Double.parseDouble(min_len);
        }
        
        int sel[] = this.getSelection();
        Object3D obj, mesh;
        ObjectInfo info;
        if (sel.length != 1)
            return;
        info = this.getObject(sel[0]);
        
        TriangleMesh mesh2 = (TriangleMesh) info.getObject();
        
        MeshVertex[] verts = mesh2.getVertices();
        int verticies = mesh2.getVertices().length;
        System.out.println("verticies " + verticies);
        
        Edge edges[] = mesh2.getEdges();
        System.out.println("edges " + edges.length );
        
        /*
        double min_vertex = 999999;
        double max_vertex = 0;
        for(int k = 1; k < edges.length; k++){
            Edge edge = edges[k];
            // edge   int  v1, v2, f1, f2   (v vertex???)
            //System.out.println( " edge  " + edge.v1 + " " + edge.v2 );
            MeshVertex a = verts[edge.v1];
            MeshVertex b = verts[edge.v2];
            double dx = a.r.x - b.r.x;
            double dy = a.r.y - b.r.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            //System.out.println("   " + k + " dist: " + dist );
            if(dist < min_vertex){
                min_vertex = dist;
            }
            if(dist > max_vertex){
                max_vertex = dist;
            }
        }
         */
        
        //double avg_vertex = (max_vertex - min_vertex) / 2;
        
        // Process
        
        boolean working = true;
        int iteration = 0;
        
        while(working && iteration < 3){ // && iteration < 3
            iteration++;
            boolean allShortEnough = true;
            System.out.println("Subdivide itertaion. ");
            
            mesh2 = (TriangleMesh) info.getObject();
            verts = mesh2.getVertices();
            edges = mesh2.getEdges();
            
            
            for(int k = 1; k < edges.length; k++){
                Edge edge = edges[k];
                MeshVertex a = verts[edge.v1];
                MeshVertex b = verts[edge.v2];
                
                double dx = a.r.x - b.r.x;
                double dy = a.r.y - b.r.y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                
                if(dist > max_edge_length){
                    allShortEnough = false;
                }
                
                //if( dist > (max_vertex/5) ){ // Split edges greater than 1/4 of the largest edge
                if( dist > max_edge_length ){
                
                    System.out.println(" SUBDIVIDE  " + k + "   len: " + dist  + "    iteration: " + iteration );
                    
                    boolean selected[] = new boolean [edges.length * 6];  // how many actually????
                    for (int i = 0; i < selected.length; i++){
                        if(
                           i == k //|| i == k - 1
                           ){
                            selected[i] = true;
                        } else {
                            selected[i] = false;
                        }
                    }
                    
                    int i, j;
                    TriangleMesh theMesh = (TriangleMesh) info.getObject(), newmesh;
                    boolean newselection[];
                    //Edge edges[];
                    Face faces[];
                    // Subdivide selected edges, using the appropriate method.
                    //System.out.println("Edges: " + theMesh.getVertices().length);
                    i = theMesh.getVertices().length;
                    if (theMesh.getSmoothingMethod() == TriangleMesh.APPROXIMATING){
                        newmesh = TriangleMesh.subdivideLoop(theMesh, selected, Double.MAX_VALUE);
                    } else if (theMesh.getSmoothingMethod() == TriangleMesh.INTERPOLATING){
                        newmesh = TriangleMesh.subdivideButterfly(theMesh, selected, Double.MAX_VALUE);
                    } else {
                        // this one is used
                        newmesh = TriangleMesh.subdivideLinear(theMesh, selected);
                    }
                    
                    info.setObject(newmesh);
                }
                
                // min
                //
                if( dist < min_edge_length ){
                    //System.out.println(" SIMPLIFY  " + k );
                    
                    
                }
                
            }
            
            if( allShortEnough ){
                working = false;
            }
        }
        

        
        // Update the selection.
        // Needed??? Probably No.
//        edges = newmesh.getEdges();
//        newselection = new boolean [edges.length];
//        for (j = 0; j < edges.length; j++)
//            newselection[j] = (edges[j].v1 >= i || edges[j].v2 >= i);
        //setSelection(newselection);
//        selected = newselection;
        
        //}
        //else
        //{
        
        // Subdivide selected faces.
        
        //i = theMesh.getVertices().length;
        //newmesh = TriangleMesh.subdivideFaces(theMesh, selected);
        //setUndoRecord(new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {newmesh, theMesh}));
        //setMesh(newmesh);
        
        // Update the selection.
        
        //faces = newmesh.getFaces();
        //newselection = new boolean [faces.length];
        //for (j = 0; j < faces.length; j++)
        //    newselection[j] = (faces[j].v1 >= i || faces[j].v2 >= i || faces[j].v3 >= i);
        //setSelection(newselection);
        
        //}
        System.out.println("Completed subdivide mesh.");
        
        
    }
    
    
    /**
     * beamNGCreateBeam
     *
     *
     */
    public void beamNGCreateBeam(LayoutWindow window){
        ExportBeamNGDrive export = new ExportBeamNGDrive(window);
    }
    
    
    /**
     * exportBeamNGVehicle
     *
     */
    public void exportBeamNGVehicle(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        System.out.println("exportBeamNGVehicle");
        
        // 1) Look for object named 'BeamNG'
        // 2) extract nodes for children.
        
        // Write file
        /*
         {"Tug":
         
         {
         "information":{
         "authors":"B25Mitch",
         "name":"Tug",
         }
         
         "slotType" : "main",
         
         "nodes": [
         ["id", "posX", "posY", "posZ"],
         {"nodeWeight":10},
         {"frictionCoef":0.7},
         {"nodeMaterial":"|NM_METAL"},
         {"collision":true},
         {"selfCollision":true},
         
         ],
         
         "beams": [
         
         ["id1:", "id2:"],
         {"beamSpring":2000000,"beamDamp":200},
         {"beamDeform":80000,"beamStrength":"800000"},
         
         ],
         
         }
         }
         */
        
    }
    
    
    /**
     * exportObjectCSV
     *
     * Drescription: Export curve and mesh edge points to CSV point data files.
     * @param LayoutWindow
     */
    public void exportObjectCSV(LayoutWindow window){
        LayoutModeling layout = new LayoutModeling();
        System.out.println("exportObjectCSV");
        
        double scale = getScale();
        System.out.println(" export csv scale: " + scale);
        /*
        int sel[] = this.getSelection();
        Object3D obj;
        ObjectInfo info;
        for(int i = 0; i < sel.length; i++){
            info = this.getObject(sel[i]);
            obj = info.getObject();
         
            //String structName = layout.getObjectStructure( info.getId() + "" );
            System.out.println("name " + info.getId());
            //obj = info.getObject();
         
        }
        */
        
        boolean selection = false;
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                selection = true;
            }
        }
        
        String dir = this.getDirectory() + System.getProperty("file.separator") + this.getName() + "_exports";
        File d2 = new File(dir);
        if(d2.exists() == false){
            d2.mkdir();
        }
        //System.out.println(" File:  " + dir);
        
        //
        // clear old files
        //
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
                if(listOfFiles[i].getName().contains(".csv")  ){
                    System.out.println(" Delete file " + listOfFiles[i].getAbsolutePath());
                    listOfFiles[i].delete();
                }
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
        
        /*
        for (ObjectInfo obj : objects){
            if(obj.selected == true || selection == false){
                Object co = (Object)obj.getObject();
                if((co instanceof Curve) == true){
                    String dir = this.getDirectory() + System.getProperty("file.separator") + this.getName() + "_exports";
                    File d2 = new File(dir);
                    if(d2.exists() == false){
                        d2.mkdir();
                    }
                    String objectFile = dir + System.getProperty("file.separator") + group + ".csv";
                    d2 = new File(objectFile);
                    if(d2.exists() == true){
                        d2.delete();
                    }
                }
            }
        }
         */
        
        try {
            String projectName = this.getName();
            if(projectName.indexOf(".") != -1){
                projectName = projectName.substring(0, projectName.indexOf("."));
            }
            String singleObjectFile = dir + System.getProperty("file.separator") + projectName + ".csv"; // *** find file name
            PrintWriter singleWriter = new PrintWriter(new FileOutputStream(new File(singleObjectFile), false /* append = true */));
        
            for (ObjectInfo obj : objects){
                if(obj.selected == true || selection == false){
                    System.out.println("Object Info: ");
                    Object co = (Object)obj.getObject();
                    if((co instanceof Curve) == true && obj.isVisible() && obj.isHiddenByParent() == false){
                        //info = this.getObject(sel[i]);
                        String group = layout.getObjectGroup(obj.getId() + "");
                        
                        //System.out.println("Curve " + obj.getId() + " group: " + group );
                        
                        CoordinateSystem cs = ((ObjectInfo)obj).getCoords();
                        Vec3 origin = cs.getOrigin();
                        
                        Mesh mesh = (Mesh) obj.getObject(); // Object3D
                        //System.out.println(" m esh " +   obj.getId() );
                        //Vec3 [] verts = mesh.getVertexPositions();
                        //for (Vec3 vert : verts){
                        //    System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                        //}
                        
                        //if(group.length() > 0){
                            String objectFile = dir + System.getProperty("file.separator") + obj.getName() + ".csv";
                            
                            System.out.println("Export CSV file: " + objectFile);
                            try {
                                //PrintWriter writer = new PrintWriter(new FileOutputStream(new File(objectFile),true /* append = true */));
                                
                                //writer.println ("# " + obj.getName());
                                singleWriter.println ("# " + obj.getName());
                                Vec3 [] verts = mesh.getVertexPositions();
                                for (Vec3 vert : verts){
                                    
                                    Mat4 mat4 = cs.duplicate().fromLocal();
                                    mat4.transform(vert);
                                    
                                    //System.out.println("    vert: " + vert.x + " " + vert.y + "  " + vert.z );
                                    //double x = vert.x + origin.x;
                                    //double y = vert.y + origin.y;
                                    //double z = vert.z + origin.z;
                                    
                                    double x = vert.x * scale;
                                    double y = vert.y * scale;
                                    double z = vert.z * scale;
                                    
                                    //writer.println ("" + x + "," + y + "," + z);
                                    
                                    singleWriter.println ("" + x + "," + y + "," + z);
                                    
                                }
                                
                                singleWriter.println("");
                                
                                //writer.close();
                                
                            } catch(Exception ex){
                                //System.out.println("Error: " + dir);
                                System.out.println("Error: " + ex);
                            }
                        //}
                        
                        //if(verts.length > maxPoints){
                            //maxPoints = verts.length;
                        //}
                        
                    }
                    
                    /*
                    if((co instanceof Mesh) == true){
                        // TODO
                        String group = layout.getObjectGroup(obj.getId() + "");
                        
                        System.out.println("Mesh " + obj.getId() + " group: " + group );
                        
                        if(group.length() > 0){
                            String objectFile = dir + System.getProperty("file.separator") + group + ".csv";
                            
                            System.out.println("Export CSV file: " + objectFile);
                            try {
                                PrintWriter writer = new PrintWriter(new FileOutputStream(new File(objectFile),true ));
                                
                                writer.println ("# " + obj.getName());
                                Mesh mesh = (Mesh) obj.getObject(); // Object3D
                        
                                // get points
                                // ***
                                Vec3 vert[] = mesh.getVertexPositions();
                                //CoordinateSystem c;
                                CoordinateSystem cs = ((ObjectInfo)obj).getCoords();
                                Vec3 origin = cs.getOrigin();
                                
                                Mat4 aMat4 = cs.duplicate().fromLocal();
                                for(int i = 0; i < vert.length; i++){
                                    Vec3 currVec = new Vec3((Vec3)vert[i]);
                                    aMat4.transform(currVec);
                                    //double xDistance = currVec.x - mirror.x;
                                    //currVec.x = mirror.x - (xDistance) ;
                                    //currVec.z = currVec.z;
                                    //vert[i] = currVec;
                                    
                                    double x = currVec.x * scale;
                                    double y = currVec.y * scale;
                                    double z = currVec.z * scale;
                                    
                                    writer.println ("" + x + "," + y + "," + z);
                                }
                        
                            writer.close();
                            
                        } catch(Exception ex){
                            //System.out.println("Error: " + dir);
                            System.out.println("Error: " + ex);
                        }
                        
                        } // group
                    } // Mesh
                    */
                }
            } // Objects
            
            
            
            singleWriter.close();
            
        } catch(Exception ex){
            //System.out.println("Error: " + dir);
            System.out.println("Error: " + ex);
        }
    }
    
    
    /**
     * runCFD
     *
     * desc
     */
    public void runCFD(LayoutWindow window){
        //if(cfd == null){
            cfd = new ComputationalFluidDynamics();
        //}
        cfd.setObjects(objects);
        cfd.setLayoutWindow(window);
        //cfd.run();
        if(cfd.isRunning() == false){
            boolean ok = cfd.getUserInput();
            if(ok){
                cfd.start();
            }
        } else {
            //cfd.stopCFD();
        }
    }
    
    /**
     * stopCFD
     *
     */
    public void stopCFD(LayoutWindow window){
        if(cfd != null){
            if(cfd.isRunning() == false){
           
            } else {
                System.out.println("stop cfd");
                cfd.stopCFD();
                
                cfd = null;
            }
        }
    }
    
    public void calibrateCFD(LayoutWindow window){
        if(cfd != null){
            if(cfd.isRunning() == false){
                cfd.calibrate();
            }
        }
    }
    
    /**
     * frontalArea
     * Description: Menu item frontal area called. Pass request to CFD class.
     */
    public void frontalArea(LayoutWindow window){
        if(cfd == null){
            cfd = new ComputationalFluidDynamics();
            cfd.setObjects(objects);
        }
        cfd.frontalArea();
    }
    
    public void setObjectGroup(){
        LayoutModeling layout = new LayoutModeling();
        for (ObjectInfo obj : objects){
            if(obj.selected == true){
                layout.setObjectGroup( obj );
            }
        }
    }
    
    // 
    public Vector<ObjectInfo> getObjects(){
        return objects;
    }
    
    
    /**
     * DEPRICATE - place static variables for LayoutWindow->Scene in classes that need them.
     *  LayoutWindow and Scene can't both have references to each other.
     */
    /*
    public void setLayoutWindow(Object lw){ // LayoutWindow
        System.out.println("Depricate: Scene.setLayoutWindow()");
        layoutWindowObject = lw;
    }
    
    public Object getLayoutWindow(){
        System.out.println("Depricate: Scene.getLayoutWindow()");
        return layoutWindowObject;
    }
     */

// Faster way to add list of objects. - experimental
    public void addObjects(Vector <ObjectInfo> infos, UndoRecord undo)
    {
        for(int i = 0; i < infos.size(); i++){
            ObjectInfo info = infos.elementAt(i);
            addObject(info, objects.size(), undo);
        }
        updateSelectionInfo();
    }
    

}
