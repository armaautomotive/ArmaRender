/* Copyright (C) 2021-2022 Jon Taylor
   Copyright (C) 2002-2012 by Peter Eastman
 
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.translators;

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

import java.net.URI;

/** STLImporter implements the importing of OBJ files. */

public class STLImporter
{
    static byte[]  stlBytes = null;
    public static float maxX;
        public static float maxY;
        public static float maxZ;
        public static float minX;
        public static float minY;
        public static float minZ;
    
    
    
  public static void importFile(BFrame parent, Scene theScene)
  {
    BFileChooser bfc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("importSTL"));
    if (ArmaRender.getCurrentDirectory() != null)
      bfc.setDirectory(new File(ArmaRender.getCurrentDirectory()));
    if (!bfc.showDialog(parent))
      return;
    File f = bfc.getSelectedFile();
    ArmaRender.setCurrentDirectory(bfc.getDirectory().getAbsolutePath());
    String objName = f.getName();
    if (objName.lastIndexOf('.') > 0)
      objName = objName.substring(0, objName.lastIndexOf('.'));
    
      //System.out.println("Import name: (2) " + objName);
      importSTL(f);
  }
    
    
    /**
     * importSTL
     *
     * Description: Import an STL file into the scene.
     *
     * @param File, file of stl to read and import data from.
     */
    public static void importSTL(File file) {
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        maxZ = Float.MIN_VALUE;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        minZ = Float.MAX_VALUE;
        
        Scene theScene = new Scene();
        CoordinateSystem coords = new CoordinateSystem(new Vec3(0.0, 0.0, Camera.DEFAULT_DISTANCE_TO_SCREEN), new Vec3(0.0, 0.0, -1.0), Vec3.vy());
        ObjectInfo info = new ObjectInfo(new SceneCamera(), coords, "Camera 1");
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
        theScene.addObject(info, null);
        info = new ObjectInfo(new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 0.8f), coords.duplicate(), "Light 1");
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
        theScene.addObject(info, null);

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            
            int n = 0;
            byte[] buffer = new byte[4];
            inputStream.skip(80);
            n = inputStream.read(buffer);
            System.out.println("n=="+n);
            int size=getIntWithLittleEndian(buffer,0);
            System.out.println("size=="+size);
            List<Float> normalList=new ArrayList<Float>();
            List<Float> vertexList = new ArrayList<Float>();
            
            Vec3[] allVectors = new Vec3[size * 3];
            
            //float[] all
            //Vector faces = new Vector();
            int importedFaces[][] = new int [size][3];
            
            for (int i = 0; i < size; i++) {
                
                //normal
                Vec3[] normals = new Vec3[3];
                
                for(int k=0;k<3;k++){
                    inputStream.read(buffer);
                    normalList.add(Float.intBitsToFloat(getIntWithLittleEndian(buffer, 0)));
                }

                Vec3[] polygon = new Vec3[3];

                for(int j=0;j<3;j++){
                    inputStream.read(buffer);
                    float x = Float.intBitsToFloat(getIntWithLittleEndian(buffer, 0));
                    inputStream.read(buffer);
                    float y = Float.intBitsToFloat(getIntWithLittleEndian(buffer, 0));
                    inputStream.read(buffer);
                    float z = Float.intBitsToFloat(getIntWithLittleEndian(buffer, 0));
                    adjustMaxMin(x, y, z);
                    vertexList.add(x);
                    vertexList.add(y);
                    vertexList.add(z);
                    //System.out.println(" j " + j + " x " + x + " y  " + y + " z " + z);
                    Vec3 vec = new Vec3(x, y, z);
                    polygon[j] = vec; // testing method
                    
                    allVectors[(i*3) + j] = vec; // TODO: optimize this by only adding unique vec points and indexing them from multiple polygons.
                    
                }
                inputStream.skip(2);
                
                //int fc[][] = new int [groupFaces.size()][], numVert = 0;
                int fc[][] = new int [1][], numVert = 0;
                //Vec3 vert[] = new Vec3 [numVert];
                int faces[][] = new int [1][3];
                for(int m = 0; m < 1; m++){ // face.size()
                    //int[] faceIndexes = (int[])face.elementAt(i);
                    faces[m][0] = 0; // faceIndexes[0];
                    faces[m][1] = 1; // faceIndexes[1];
                    faces[m][2] = 2; // faceIndexes[2];
                }
                
                for(int m = 0; m < 1; m++){ // face.size()
                    importedFaces[i+m][0] = (i*3);
                    importedFaces[i+m][1] = (i*3)+1;
                    importedFaces[i+m][2] = (i*3)+2;
                }
                
                //info = new ObjectInfo(new TriangleMesh(polygon, faces), coords, file.getName()); //
                //theScene.addObject(info, null); // This object is only one polygin from the imported file.
            }
            
            ObjectInfo importedInfo = new ObjectInfo(new TriangleMesh(allVectors, importedFaces), coords, "-"+file.getName());
            theScene.addObject(importedInfo, null);
            
            System.out.println("normalList size== "+normalList.size());
            System.out.println("vertexList size== "+vertexList.size());
        } catch (FileNotFoundException e) {
            System.out.println("FileNotFoundException");
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("IOException");
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if(inputStream!=null)
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
        //System.out.println("done");
        ArmaRender.newWindow(theScene);
    }
    
    private static int getIntWithLittleEndian(byte[] bytes, int offset) {
        //return (0xff & stlBytes[offset]) | ((0xff & stlBytes[offset + 1]) << 8) | ((0xff & stlBytes[offset + 2]) << 16) | ((0xff & stlBytes[offset + 3]) << 24);
        return (0xff & bytes[offset]) | ((0xff & bytes[offset + 1]) << 8) | ((0xff & bytes[offset + 2]) << 16) | ((0xff & bytes[offset + 3]) << 24);
    }
    
    private static void adjustMaxMin(float x, float y, float z) {
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z > maxZ) {
                maxZ = z;
            }
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
        }
    
    
  
  /** Separate a line into pieces divided by whitespace. */
  
  private static String [] breakLine(String line)
  {
    StringTokenizer st = new StringTokenizer(line);
    Vector<String> v = new Vector<String>();
    
    while (st.hasMoreTokens())
      v.addElement(st.nextToken());
    String result[] = new String [v.size()];
    v.copyInto(result);
    return result;
  }
  
  /** Parse the specification for a vertex and return the index of the vertex
      to use. */
  
  private static VertexInfo parseVertexSpec(String spec, Vector vertex, Vector texture, Vector normal, int lineno) throws Exception
  {
    VertexInfo info = new VertexInfo();
    StringTokenizer st = new StringTokenizer(spec, "/", true);
    info.tex = info.norm = Integer.MAX_VALUE;
    int i = 0;
    while (st.hasMoreTokens())
      {
        String value = st.nextToken();
        if ("/".equals(value))
          {
            i++;
            continue;
          }
        try
          {
            int index = Integer.parseInt(value);
            int total;
            if (i == 0)
              total = vertex.size();
            else if (i == 1)
              total = texture.size();
            else
              total = normal.size();
            if (index < 0)
              index += total;
            else
              index--;
            if (i == 0)
              info.vert = index;
            else if (i == 1)
              info.tex = index;
            else
              info.norm = index;
          }
        catch (NumberFormatException ex)
          {
            throw new Exception("Illegal value '"+spec+"' found in line "+lineno+".");
          }
      }
    if (info.tex == Integer.MAX_VALUE)
      info.tex = info.vert;
    if (info.norm == Integer.MAX_VALUE)
      info.norm = info.vert;
    return info;
  }
  
  /** Parse the contents of a .mtl file and add TextureInfo object to a hashtable. */
  
  private static void parseTextures(String file, File baseDir, Hashtable<String, TextureInfo> textures) throws Exception
  {
    File f = new File(baseDir, file);
    if (!f.isFile())
      f = new File(file);
    if (!f.isFile())
    {
      new BStandardDialog("Error Importing File", "Cannot locate material file '"+file+"'.", BStandardDialog.ERROR).showMessageDialog(null);
      return;
    }
    BufferedReader in = new BufferedReader(new FileReader(f));
    String line;
    TextureInfo currentTexture = null;
    while ((line = in.readLine()) != null)
      {
        try
          {
            if (line.startsWith("#"))
              continue;
            String fields[] = breakLine(line);
            if (fields.length == 0)
              continue;
            if ("newmtl".equals(fields[0]))
              {
                // This is the start of a new texture.
                
                currentTexture = null;
                if (fields.length == 1 || textures.get(fields[1]) != null)
                  continue;
                currentTexture = new TextureInfo();
                textures.put(fields[1], currentTexture);
              }
            if (currentTexture == null || fields.length < 2)
              continue;
            if ("Kd".equals(fields[0]))
              currentTexture.diffuse = parseColor(fields);
            else if ("Ka".equals(fields[0]))
              currentTexture.ambient = parseColor(fields);
            else if ("Ks".equals(fields[0]))
              currentTexture.specular = parseColor(fields);
            else if ("d".equals(fields[0]) || "Tr".equals(fields[0]))
              currentTexture.transparency = 1.0-Double.parseDouble(fields[1]);
            else if ("Ns".equals(fields[0]))
              currentTexture.shininess = Double.parseDouble(fields[1]);
            else if ("map_Kd".equals(fields[0]))
              currentTexture.diffuseMap = fields[1];
            else if ("map_Ka".equals(fields[0]))
              currentTexture.ambientMap = fields[1];
            else if ("map_Ks".equals(fields[0]))
              currentTexture.specularMap = fields[1];
            else if ("map_d".equals(fields[0]))
              currentTexture.transparentMap = fields[1];
            else if ("map_Bump".equals(fields[0]))
              currentTexture.bumpMap = fields[1];
          }
        catch (Exception ex)
          {
            in.close();
            throw new Exception("Illegal line '"+line+"' found in file '"+file+"'.");
          }
      }
    in.close();
  }
  
  /** Create a texture from a TextureInfo and add it to the scene. */
  
  private static Texture createTexture(TextureInfo info, String name, Scene scene, File baseDir, Hashtable<String, ImageMap> imageMaps) throws Exception
  {
    if (info == null)
    {
      // This texture was not defined in an MTL file.  Create an empty image mapped texture
      // so that texture coordinates will be preserved and the user can specify the images
      // later.

      ImageMapTexture tex = new ImageMapTexture();
      tex.setName(name);
      scene.addTexture(tex);
      return tex;
    }
    info.resolveColors();
    ImageMap diffuseMap = loadMap(info.diffuseMap, scene, baseDir, imageMaps);
    ImageMap specularMap = loadMap(info.specularMap, scene, baseDir, imageMaps);
    ImageMap transparentMap = loadMap(info.transparentMap, scene, baseDir, imageMaps);
    ImageMap bumpMap = loadMap(info.bumpMap, scene, baseDir, imageMaps);
    RGBColor transparentColor =  new RGBColor(info.transparency, info.transparency, info.transparency);
    if (diffuseMap == null && specularMap == null && transparentMap == null && bumpMap == null)
      {
        // Create a uniform texture.
        
        UniformTexture tex = new UniformTexture();
        tex.diffuseColor = info.diffuse.duplicate();
        tex.specularColor = info.specular.duplicate();
        tex.transparentColor = transparentColor;
        tex.shininess = (float) info.specularity;
        tex.specularity = 0.0f;
        tex.roughness = info.roughness;
        tex.setName(name);
        scene.addTexture(tex);
        return tex;
      }
    else
      {
        // Create an image mapped texture.

        ImageMapTexture tex = new ImageMapTexture();
        tex.diffuseColor = (diffuseMap == null ? new ImageOrColor(info.diffuse) : new ImageOrColor(info.diffuse, diffuseMap));
        tex.specularColor = (specularMap == null ? new ImageOrColor(info.specular) : new ImageOrColor(info.specular, specularMap));
        tex.transparentColor = (transparentMap == null ? new ImageOrColor(transparentColor) : new ImageOrColor(transparentColor, transparentMap));
        if (transparentMap == null && info.transparency == 0.0 && diffuseMap != null && diffuseMap.getComponentCount() == 4)
        {
          // Use the diffuse map's alpha channel channel for transparency.

          tex.transparentColor = new ImageOrColor(new RGBColor(1.0, 1.0, 1.0));
          tex.transparency = new ImageOrValue(1.0f, diffuseMap, 3);
        }
        if (bumpMap != null)
          tex.bump = new ImageOrValue(1.0f, bumpMap, 0);
        tex.shininess = new ImageOrValue((float) info.specularity);
        tex.specularity = new ImageOrValue(0.0f);
        tex.roughness = new ImageOrValue((float) info.roughness);
        tex.tileX = tex.tileY = true;
        tex.mirrorX = tex.mirrorY = false;
        tex.setName(name);
        scene.addTexture(tex);
        return tex;
      }
  }
  
  /** Return the image map corresponding to the specified filename, and add it to the scene. */
  
  private static ImageMap loadMap(String name, Scene scene, File baseDir, Hashtable<String, ImageMap> imageMaps) throws Exception
  {
    if (name == null)
      return null;
    ImageMap map = imageMaps.get(name);
    if (map != null)
      return map;
    File f = new File(baseDir, name);
    if (!f.isFile())
      f = new File(name);
    if (!f.isFile())
      throw new Exception("Cannot locate image map file '"+name+"'.");
    try
      {
        map = ImageMap.loadImage(f);
      }
    catch (InterruptedException ex)
      {
        throw new Exception("Unable to load image map file '"+f.getAbsolutePath()+"'.");
      }
    scene.addImage(map);
    imageMaps.put(name, map);
    return map;
  }
  
  /** Parse the specification for a color. */
  
  private static RGBColor parseColor(String fields[]) throws NumberFormatException
  {
    if (fields.length < 4)
      return null;
    return new RGBColor(Double.parseDouble(fields[1]),
        Double.parseDouble(fields[2]),
        Double.parseDouble(fields[3]));
  }
  
  /** Inner class for storing information about a vertex of a face. */
  
  private static class VertexInfo
  {
    public int vert, norm, tex;
  }
  
  /** Inner class for storing information about a face. */
  
  private static class FaceInfo
  {
    public VertexInfo v1, v2, v3;
    public int smoothingGroup;
    public String texture;
    
    public FaceInfo(VertexInfo v1, VertexInfo v2, VertexInfo v3, int smoothingGroup, String texture)
    {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.smoothingGroup = smoothingGroup;
      this.texture = texture;
    }
    
    public VertexInfo getVertex(int which)
    {
      switch (which)
        {
          case 0: return v1;
          case 1: return v2;
          default: return v3;
        }
    }
  }
  
  /** Inner class for storing information about a texture in a .mtl file. */
  
  private static class TextureInfo
  {
    public RGBColor ambient, diffuse, specular;
    public double shininess, transparency, specularity, roughness;
    public String ambientMap, diffuseMap, specularMap, transparentMap, bumpMap;
    
    /** This should be called once, after the TextureInfo is created but before it is actually used.  It converts from the
        representation used by .obj files to the one used by Art of Illusion. */
        
    public void resolveColors()
    {
      if (diffuse == null)
      {
        if (diffuseMap == null)
          diffuse = new RGBColor(0.0, 0.0, 0.0);
        else
          diffuse = new RGBColor(1.0, 1.0, 1.0);
      }
      if (ambient == null)
        ambient = new RGBColor(0.0, 0.0, 0.0);
      if (specular == null)
        specular = new RGBColor(0.0, 0.0, 0.0);
      else
        specularity = 1.0;
      diffuse.scale(1.0-transparency);
      specular.scale(1.0-transparency);
      roughness = 1.0-(shininess-1.0)/128.0;
      if (roughness > 1.0)
        roughness = 1.0;
      checkColorRange(ambient);
      checkColorRange(diffuse);
      checkColorRange(specular);
    }
    
    /** Make sure that the components of a color are all between 0 and 1. */
    
    private void checkColorRange(RGBColor c)
    {
      float r = c.getRed(), g = c.getGreen(), b = c.getBlue();
      if (r < 0.0f) r = 0.0f;
      if (r > 1.0f) r = 1.0f;
      if (g < 0.0f) g = 0.0f;
      if (g > 1.0f) g = 1.0f;
      if (b < 0.0f) b = 0.0f;
      if (b > 1.0f) b = 1.0f;
      c.setRGB(r, g, b);
    }
  }
}
