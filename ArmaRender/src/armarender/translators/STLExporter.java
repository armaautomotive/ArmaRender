/* Copyright (C) 2025 Jon Taylor

    
 TODO: Add preogress bar
 */

package armarender.translators;

import armarender.*;
import armarender.math.*;
import armarender.object.*;
import armarender.texture.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;

/** STLExporter contains the actual routines for exporting OBJ files. */

public class STLExporter
{
  /** Display a dialog which allows the user to export a scene to an OBJ file. */
  
  public static void exportFile(BFrame parent, Scene theScene)
  {
      System.out.println("STL Export.");
      Thread worker = new Thread() {
          public void run() {
      
    // Display a dialog box with options on how to export the scene.
    
    ValueField errorField = new ValueField(0.05, ValueField.POSITIVE);
    final ValueField widthField = new ValueField(200.0, ValueField.INTEGER+ValueField.POSITIVE);
    final ValueField heightField = new ValueField(200.0, ValueField.INTEGER+ValueField.POSITIVE);
    final ValueSlider qualitySlider = new ValueSlider(0.0, 1.0, 100, 0.5);
    final BCheckBox smoothBox = new BCheckBox(Translate.text("subdivideSmoothMeshes"), true);
    final BCheckBox normalsBox = new BCheckBox(Translate.text("alwaysWriteNormals"), false);
    final BCheckBox mtlBox = new BCheckBox(Translate.text("writeTexToMTL"), false);
    BComboBox exportChoice = new BComboBox(new String [] {
      Translate.text("exportWholeScene"),
      Translate.text("selectedObjectsOnly")
    });
    mtlBox.addEventLink(ValueChangedEvent.class, new Object() {
      void processEvent()
      {
        widthField.setEnabled(mtlBox.getState());
        heightField.setEnabled(mtlBox.getState());
        qualitySlider.setEnabled(mtlBox.getState());
      }
    });
    mtlBox.dispatchEvent(new ValueChangedEvent(mtlBox));
    ComponentsDialog dlg;
    if (theScene.getSelection().length > 0)
      dlg = new ComponentsDialog(parent, Translate.text("exportToSTL"), 
	  new Widget [] {exportChoice, errorField, smoothBox, normalsBox, mtlBox, Translate.label("imageSizeForTextures"), widthField, heightField, qualitySlider},
	  new String [] {null, Translate.text("maxSurfaceError"), null, null, null, null, Translate.text("Width"), Translate.text("Height"), Translate.text("imageQuality")});
    else
      dlg = new ComponentsDialog(parent, Translate.text("exportToSTL"),
	  new Widget [] {errorField, smoothBox, normalsBox, mtlBox, Translate.label("imageSizeForTextures"), widthField, heightField, qualitySlider},
	  new String [] {Translate.text("maxSurfaceError"), null, null, null, null, Translate.text("Width"), Translate.text("Height"), Translate.text("imageQuality")});
    if (!dlg.clickedOk())
      return;

      
      
      // format file name
      String fileName = theScene.getName();
      int extensionPos = fileName.indexOf(".ads");
      if(extensionPos != -1){
          fileName = fileName.substring(0, extensionPos);
      }
      
      String exportDir = theScene.getDirectory() + System.getProperty("file.separator") + fileName;
      File d = new File(exportDir);
      if(d.exists() == false){
          d.mkdir();
      }
      
      // folder for tube bends
      exportDir = theScene.getDirectory() + System.getProperty("file.separator") + fileName + System.getProperty("file.separator") + "stl";
      d = new File(exportDir);
      if(d.exists() == false){
          d.mkdir();
      }
      
      
      
    // Ask the user to select the output file.
    BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("exportToSTL"), d); // SelectionMode mode, String title, File directory
      
      //fc.setDirectory(new File(exportDir));
      fc.setSelectedFile(new File(fileName + ".stl"));
      
    //fc.setSelectedFile(new File(fileName + ".stl"));
    if (ArmaRender.getCurrentDirectory() != null)
      fc.setDirectory(new File(ArmaRender.getCurrentDirectory()));
    if (!fc.showDialog(parent))
      return;
    File dir = fc.getDirectory();
    File f = fc.getSelectedFile();
    String name = f.getName();
    String baseName = (name.endsWith(".stl") ? name.substring(0, name.length()-4) : name);
              ArmaRender.setCurrentDirectory(dir.getAbsolutePath());
      
      
      
    
    // Create the output files.

    try
    {
      TextureImageExporter textureExporter = null;
      String mtlFilename = null;
      /*
        if (mtlBox.getState())
      {
        textureExporter = new TextureImageExporter(dir, baseName, (int) (100*qualitySlider.getValue()),
            TextureImageExporter.DIFFUSE+TextureImageExporter.HILIGHT+TextureImageExporter.EMISSIVE,
            (int) widthField.getValue(), (int) heightField.getValue());
        
          mtlFilename = baseName.replace(' ', '_')+".mtl";
        //PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, mtlFilename))));
        //writeTextures(theScene, out, exportChoice.getSelectedIndex() == 0, textureExporter);
        //out.close();
        textureExporter.saveImages();
      }
       */
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
      writeScene(theScene, out,
                 exportChoice.getSelectedIndex() == 0,
                 errorField.getValue(),
                 smoothBox.getState(),
                 normalsBox.getState(),
                 textureExporter, mtlFilename, fileName);
      out.close();
    }
    catch (Exception ex)
      {
        ex.printStackTrace();
        new BStandardDialog("", new String [] {Translate.text("errorExportingScene"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(parent);
      }
      
              
              
          }
      };
      worker.start();
      
  }

    
  /**
   * Write out the scene in STL format to the specified PrintWriter.  The other parameters
   *   correspond to the options in the dialog box displayed by exportFile().
   */
  public static void writeScene(Scene theScene,
                                PrintWriter out,
                                boolean wholeScene,
                                double tol,
                                boolean smooth,
                                boolean alwaysStoreNormals,
                                TextureImageExporter textureExporter,
                                String mtlFilename,
                                String fileName)
  {
      
      ProgressDialog progressDialog = new ProgressDialog("Exporting");
      progressDialog.start();
      progressDialog.setProgress(1);
      
    // Write the header information.

    out.println("comment Produced by Arma Automotive - Arma Design Studio "+ArmaRender.getVersion()+", "+(new Date()).toString());
    //if (mtlFilename != null)
    //  out.println("mtllib "+mtlFilename);

    // Write the objects in the scene.
      
      out.println("solid " + fileName);

    int numVert = 0, numNorm = 0, numTexVert = 0;
    Hashtable<String, String> groupNames = new Hashtable<String, String>();
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setMaximumFractionDigits(5);
    nf.setGroupingUsed(false);
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
          int progress = (int) (((float)(i / (float)(theScene.getNumObjects())) * (float)100));
          progressDialog.setProgress(progress);
          
        // Get a rendering mesh for the object.
        
        ObjectInfo info = theScene.getObject(i);
        if (!wholeScene && !info.selected)
          continue;
        if (info.getObject().getTexture() == null)
          continue;
          
          if(info.getObject() instanceof Curve){
              continue;
          }
          
          TriangleMesh mesh;
        if (!smooth && info.getObject() instanceof TriangleMesh)
          mesh = (TriangleMesh) info.getObject();
        else
          mesh = info.getObject().convertToTriangleMesh(tol);
        if (mesh == null)
          continue;

          
        // Find the normals.

        Vec3 norm[];
        int normIndex[][] = new int[mesh.getFaceCount()][];
          if (mesh instanceof TriangleMesh ) // && mesh instanceof Curve == false
        {
            System.out.println(" - " + info.getName()  + " " + mesh.getClass().getName() );
          RenderingMesh rm = ((TriangleMesh) mesh).getRenderingMesh(Double.MAX_VALUE, false, info);
          norm = rm.norm;
          for (int j = 0; j < normIndex.length; j++)
            normIndex[j] = new int[] {rm.triangle[j].n1, rm.triangle[j].n2, rm.triangle[j].n3};
        }
        else
        {
          norm = mesh.getNormals();
          for (int j = 0; j < normIndex.length; j++)
          {
            normIndex[j] = new int[mesh.getFaceVertexCount(j)];
            for (int k = 0; k < normIndex[j].length; k++)
              normIndex[j][k] = mesh.getFaceVertexIndex(j, k);
          }
        }

          
        // Determine whether normals are actually required.

        MeshVertex vert[] = mesh.getVertices();
        
          boolean needNormals = true;
          
          
          /*
          Mat4 trans = info.getCoords().fromLocal();
          for (int j = 0; j < vert.length; j++)
            {
              Vec3 v = trans.times(vert[j].r);
              out.println("v "+nf.format(v.x)+" "+nf.format(v.y)+" "+nf.format(v.z));
            }
          
          
          for (int j = 0; j < mesh.getFaceCount(); j++)
          {
            out.print("f ");
            for (int k = 0; k < mesh.getFaceVertexCount(j); k++)
            {
              int vertIndex = mesh.getFaceVertexIndex(j, k)+1;
              if (k > 0){
                out.print(' ');
              }
              out.print(vertIndex+numVert);
              //if (needNormals)
              //{
              //  out.print("//");
              //  out.print(normIndex[j][k]+numNorm+1);
              //}
            }
            out.println();
          }
           */
          
          //CoordinateSystem cs =
          Mat4 trans = info.getCoords().fromLocal();
          
          TriangleMesh.Face[] faces = mesh.getFaces();
          
          for(int f = 0; f < faces.length; f++){
              TriangleMesh.Face face = faces[f];
              Vec3 a = new Vec3(vert[ face.v1 ].r);
              Vec3 b = new Vec3(vert[ face.v2 ].r);
              Vec3 c = new Vec3(vert[ face.v3 ].r);
              trans.transform(a);
              trans.transform(b);
              trans.transform(c);
              
              
              
              Vec3 normal = Vec3.getFaceNormal(a, b, c);
              
              //System.out.println(" a " + normal);
              
              out.print("  facet normal " + theScene.roundThree(normal.x) + " " + theScene.roundThree(normal.y)+ " " + theScene.roundThree(normal.z) + "\n");
              out.print("    outer loop\n");
              out.print("      vertex " + theScene.roundThree(a.x) + " " + theScene.roundThree(a.y) + " " + theScene.roundThree(a.z) + "\n");
              out.print("      vertex " + theScene.roundThree(b.x) + " " + theScene.roundThree(b.y) + " " + theScene.roundThree(b.z) + "\n");
              out.print("      vertex " + theScene.roundThree(c.x) + " " + theScene.roundThree(c.y) + " " + theScene.roundThree(c.z) + "\n");
              out.print("    endloop\n");
              out.print("  endfacet\n");
          }
          
          
          
          
          /*
        if (alwaysStoreNormals)
          needNormals = true;
        else
        {
          for (int j = 0; j < normIndex.length && !needNormals; j++)
          {
            for (int k = 1; k < normIndex[j].length; k++)
              if (!norm[normIndex[j][k]].equals(norm[normIndex[j][0]]))
                needNormals = true;
          }
          if (!needNormals){
            out.println("s 0"); // The mesh is faceted, so we can simply disable smoothing
          }
          else
          {
            needNormals = false;
            Vec3 vertNormal[] = new Vec3[vert.length];
            for (int j = 0; j < mesh.getFaceCount() && !needNormals; j++)
            {
              for (int k = 0; k < mesh.getFaceVertexCount(j); k++)
              {
                Vec3 n = norm[normIndex[j][k]];
                int index = mesh.getFaceVertexIndex(j, k);
                if (vertNormal[index] == null)
                  vertNormal[index] = n;
                else if (!n.equals(vertNormal[index]))
                  needNormals = true;
              }
            }
            if (!needNormals)
              out.println("s 1"); // The mesh is fully smoothed, so we can simply use a smoothing group
          }
        }
           */
           
        
        // Select a name for the group.
        
        String baseName = info.getName().replace(' ', '_');
        String name = baseName;
        int append = 1;
        while (groupNames.get(name) != null)
          name = baseName+"_"+(append++);
        groupNames.put(name, "");
        
        // Write out the object.
        
        //out.println("g "+name);
        TextureImageInfo ti = null;
        if (textureExporter != null)
          {
            //ti = textureExporter.getTextureInfo(info.getObject().getTexture());
            //if (ti != null)
            //  out.println("usemtl "+ti.name);
          }
        
          
          /*
        if (needNormals)
          for (int j = 0; j < norm.length; j++)
            {
              if (norm[j] == null)
                out.println("vn 1 0 0");
              else
                {
                  Vec3 v = trans.timesDirection(norm[j]);
                  out.println("vn "+nf.format(v.x)+" "+nf.format(v.y)+" "+nf.format(v.z));
                }
            }
          */
          
          
          /*
        if (ti != null && ((Object3D) mesh).getTextureMapping() instanceof UVMapping && ((UVMapping) ((Object3D) mesh).getTextureMapping()).isPerFaceVertex(mesh))
        {
          // A per-face-vertex texture mapping.
          
          Vec2 coords[][] = ((UVMapping) ((Object3D) mesh).getTextureMapping()).findFaceTextureCoordinates(mesh);
          double uscale = (ti.maxu == ti.minu ? 1.0 : 1.0/(ti.maxu-ti.minu));
          double vscale = (ti.maxv == ti.minv ? 1.0 : 1.0/(ti.maxv-ti.minv));
          for (int j = 0; j < coords.length; j++)
            for (int k = 0; k < coords[j].length; k++)
            {
              double u = (coords[j][k].x-ti.minu)*uscale;
              double v = (coords[j][k].y-ti.minv)*vscale;
              out.println("vt "+nf.format(u)+" "+nf.format(v));
            }
          for (int j = 0; j < mesh.getFaceCount(); j++)
          {
            out.print("f ");
            for (int k = 0; k < mesh.getFaceVertexCount(j); k++)
            {
              int vertIndex = mesh.getFaceVertexIndex(j, k)+1;
              if (k > 0)
                out.print(' ');
              out.print(vertIndex+numVert);
              out.print('/');
              out.print(k+1+numTexVert);
              //if (needNormals)
              //{
              //  out.print('/');
              //  out.print(normIndex[j][k]+numNorm+1);
              //}
            }
            out.println();
            numTexVert += coords[j].length;
          }
        }
        else if (ti != null && ((Object3D) mesh).getTextureMapping() instanceof Mapping2D)
        {
          // A per-vertex texture mapping.
          
          Vec2 coords[] = ((Mapping2D) ((Object3D) mesh).getTextureMapping()).findTextureCoordinates(mesh);
          double uscale = (ti.maxu == ti.minu ? 1.0 : 1.0/(ti.maxu-ti.minu));
          double vscale = (ti.maxv == ti.minv ? 1.0 : 1.0/(ti.maxv-ti.minv));
          for (int j = 0; j < coords.length; j++)
          {
            double u = (coords[j].x-ti.minu)*uscale;
            double v = (coords[j].y-ti.minv)*vscale;
            out.println("vt "+nf.format(u)+" "+nf.format(v));
          }
          for (int j = 0; j < mesh.getFaceCount(); j++)
          {
            out.print("f ");
            for (int k = 0; k < mesh.getFaceVertexCount(j); k++)
            {
              int vertIndex = mesh.getFaceVertexIndex(j, k)+1;
              if (k > 0)
                out.print(' ');
              out.print(vertIndex+numVert);
              out.print('/');
              out.print(vertIndex+numTexVert);
              //if (needNormals)
              //{
              //  out.print('/');
              //  out.print(normIndex[j][k]+numNorm+1);
              //}
            }
            out.println();
          }
          numTexVert += coords.length;
        }
        else
        {
           */
          
          // No texture coordinates.
          
          
        //}
        numVert += vert.length;
        //if (needNormals)
        //  numNorm += norm.length;
      }
      
      out.println("endsolid " + fileName);
      
      progressDialog.close();
  }
  
  /** Write out the .mtl file describing the textures. */
  
  private static void writeTextures(Scene theScene, PrintWriter out, boolean wholeScene, TextureImageExporter textureExporter)
  {
    // Find all the textures.
    
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (!wholeScene && !info.selected)
          continue;
        textureExporter.addObject(info);
      }
    
    // Write out the .mtl file.
    
    out.println("#Produced by Arma Design Studio "+ArmaRender.getVersion()+", "+(new Date()).toString());
    Enumeration textures = textureExporter.getTextures();
    Hashtable<String, TextureImageInfo> names = new Hashtable<String, TextureImageInfo>();
    TextureSpec spec = new TextureSpec();
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setMaximumFractionDigits(5);
    while (textures.hasMoreElements())
      {
        TextureImageInfo info = (TextureImageInfo) textures.nextElement();
        
        // Select a name for the texture.
        
        String baseName = info.texture.getName().replace(' ', '_');
        if (names.get(baseName) == null)
          info.name = baseName;
        else
          {
            int i = 1;
            while (names.get(baseName+i) != null)
              i++;
            info.name = baseName+i;
          }
        names.put(info.name, info);
        
        // Write the texture.
        
        out.println("newmtl "+info.name);
        info.texture.getAverageSpec(spec, 0.0, info.paramValue);
        if (info.diffuseFilename == null)
          out.println("Kd "+nf.format(spec.diffuse.getRed())+" "+nf.format(spec.diffuse.getGreen())+" "+nf.format(spec.diffuse.getBlue()));
        else
          {
            out.println("Kd 1 1 1");
            out.println("map_Kd "+info.diffuseFilename);
          }
        if (info.hilightFilename == null)
          out.println("Ks "+nf.format(spec.hilight.getRed())+" "+nf.format(spec.hilight.getGreen())+" "+nf.format(spec.hilight.getBlue()));
        else
          {
            out.println("Ks 1 1 1");
            out.println("map_Ks "+info.hilightFilename);
          }
        if (info.emissiveFilename == null)
          out.println("Ka "+nf.format(spec.emissive.getRed())+" "+nf.format(spec.emissive.getGreen())+" "+nf.format(spec.emissive.getBlue()));
        else
          {
            out.println("Ka 1 1 1");
            out.println("map_Ka "+info.emissiveFilename);
          }
        if (info.hilightFilename == null && spec.hilight.getRed() == 0.0f && spec.hilight.getGreen() == 0.0f && spec.hilight.getBlue() == 0.0f)
          out.println("illum 1");
        else
          {
            out.println("illum 2");
            out.println("Ns "+(int) ((1.0-spec.roughness)*128.0+1.0));
          }
      }
  }
}
