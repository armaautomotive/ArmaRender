/* Based on OBJExporter, which is Copyright (C) 2002-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armadesignstudio.translators;

import armadesignstudio.*;
import armadesignstudio.math.*;
import armadesignstudio.object.*;
import armadesignstudio.texture.*;
import armadesignstudio.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;

import net.n3.nanoxml.*;

/** ColladaExporter contains the actual routines for exporting collada .dae files. */

public class ColladaExporter
{
  /** Display a dialog which allows the user to export a scene to an OBJ file. */
  
  public static void exportFile(BFrame parent, Scene theScene)
  {
    // Display a dialog box with options on how to export the scene.
    
    ValueField errorField = new ValueField(0.05, ValueField.POSITIVE);
    final ValueField widthField = new ValueField(200.0, ValueField.INTEGER+ValueField.POSITIVE);
    final ValueField heightField = new ValueField(200.0, ValueField.INTEGER+ValueField.POSITIVE);
    final ValueSlider qualitySlider = new ValueSlider(0.0, 1.0, 100, 0.5);
    final BCheckBox smoothBox = new BCheckBox(Translate.text("subdivideSmoothMeshes"), true);
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
	{
		dlg = new ComponentsDialog(parent, Translate.text("Collada:exportToDAE"), 
		new Widget [] {exportChoice, errorField, smoothBox, mtlBox, Translate.label("imageSizeForTextures"), widthField, heightField, qualitySlider},
		new String [] {null, Translate.text("maxSurfaceError"), null, null, null, Translate.text("Width"), Translate.text("Height"), Translate.text("imageQuality")});
	}
    else
	{
		// present the dialog for the case where there is no selection (doesn't have the exportChoice field)
		dlg = new ComponentsDialog(parent, Translate.text("Collada:exportToDAE"), 
		new Widget [] {errorField, smoothBox, mtlBox, Translate.label("imageSizeForTextures"), widthField, heightField, qualitySlider},
		new String [] {Translate.text("maxSurfaceError"), null, null, null, Translate.text("Width"), Translate.text("Height"), Translate.text("imageQuality")});
	}

	if (!dlg.clickedOk())	// user cancelled
	{
		return;
	}

    // Ask the user to select the output file.

    //BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("exportToOBJ"));
    BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("Collada:exportToDAE"));

    fc.setSelectedFile(new File("Untitled.dae"));

	if (ArmaDesignStudio.getCurrentDirectory() != null)
	{
		fc.setDirectory(new File(ArmaDesignStudio.getCurrentDirectory()));
	}

    if (!fc.showDialog(parent))	// failed to show the dialog
    {
    	return;
    }

    File dir = fc.getDirectory();
    File f = fc.getSelectedFile();
    String name = f.getName();
    String baseName = (name.toLowerCase().endsWith(".obj") ? name.substring(0, name.length()-4) : name);
      ArmaDesignStudio.setCurrentDirectory(dir.getAbsolutePath());
    
    // Create the output files.

    try
    {


/* TEXTURES
      TextureImageExporter textureExporter = null;

      String mtlFilename = null;
      if (mtlBox.getState())
      {

        textureExporter = new TextureImageExporter(dir,
        	baseName, (int) (100*qualitySlider.getValue()),
            TextureImageExporter.DIFFUSE + TextureImageExporter.HILIGHT+TextureImageExporter.EMISSIVE,
            (int) widthField.getValue(),
            (int) heightField.getValue());

        mtlFilename = baseName.replace(' ', '_')+".mtl";
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, mtlFilename))));

        writeTextures(theScene, out, exportChoice.getSelectedIndex() == 0, textureExporter);

        out.close();
        textureExporter.saveImages();
      }
*/

      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));

      //writeScene(theScene, out, exportChoice.getSelectedIndex() == 0, errorField.getValue(), smoothBox.getState(), textureExporter, mtlFilename);
      writeScene(theScene, out, exportChoice.getSelectedIndex() == 0, errorField.getValue(), smoothBox.getState(), null, null);      

      out.close();
    }
    catch (Exception ex)
	{
        ex.printStackTrace();
        new BStandardDialog("", new String [] {Translate.text("errorExportingScene"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(parent);
	}
  }


  /** Write out the scene in Collada format to the specified PrintWriter.  The other parameters
      correspond to the options in the dialog box displayed by exportFile(). */

  public static void writeScene(Scene theScene, PrintWriter out, boolean wholeScene, double tol, boolean smooth, TextureImageExporter textureExporter, String mtlFilename)
	throws Exception
  {
  
    // Write the header information.
	// <?xml version="1.0" encoding="utf-8"?>
    out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    
    
	// create the root COLLADA element
	IXMLElement root = new XMLElement("COLLADA");
	root.setAttribute("xmlns", "http://www.collada.org/2005/11/COLLADASchema");
	root.setAttribute("version", "1.4.1");

    // add the asset tag
	/*
	  <asset>
	    <contributor>
	      <authoring_tool>FBX COLLADA exporter</authoring_tool>
	    </contributor>
	    <created>2008-01-22T14:18:02Z</created>
	    <modified>2008-01-22T14:18:02Z</modified>
	    <unit meter="1.000000"/>
	    <up_axis>Y_UP</up_axis>
	  </asset>
	*/
	IXMLElement asset = root.createElement("asset");
	root.addChild(asset);

	IXMLElement contributor = asset.createElement("contributor");
	asset.addChild(contributor);
	
	IXMLElement authoringTool = contributor.createElement("authoringTool");
	contributor.addChild(authoringTool);
	
	IXMLElement comments = contributor.createElement("comments");
	contributor.addChild(comments);

	authoringTool.setContent("Art Of Illusion Collada Exporter");
	
	IXMLElement created = asset.createElement("created");
	asset.addChild(created);
	created.setContent(getISO8601Date());

	IXMLElement modified = asset.createElement("modified");
	asset.addChild(modified);
	modified.setContent(getISO8601Date());

	IXMLElement unit = asset.createElement("unit");
	asset.addChild(unit);
	unit.setAttribute("meter", "1.000000");

	IXMLElement upaxis = asset.createElement("up_axis");
	asset.addChild(upaxis);
	upaxis.setContent("Y_UP");
	

	// set up the main library nodes
	IXMLElement libraryCameras = root.createElement("library_cameras");
	root.addChild(libraryCameras);
	IXMLElement libraryLights = root.createElement("library_lights");
	root.addChild(libraryLights);
	IXMLElement libraryImages = root.createElement("library_images");
	root.addChild(libraryImages);
	IXMLElement libraryMaterials = root.createElement("library_materials");
	root.addChild(libraryMaterials);
	IXMLElement libraryEffects = root.createElement("library_effects");
	root.addChild(libraryEffects);
	IXMLElement libraryGeometries = root.createElement("library_geometries");
	root.addChild(libraryGeometries);
	IXMLElement libraryVisualScenes = root.createElement("library_visual_scenes");
	root.addChild(libraryVisualScenes);
	IXMLElement sceneNode = root.createElement("scene");
	root.addChild(sceneNode);
	
	// additional setup for scene and visual_scenes nodes
	IXMLElement visualSceneNode = libraryVisualScenes.createElement("visual_scene");
	libraryVisualScenes.addChild(visualSceneNode);
	visualSceneNode.setAttribute("id", "VisualSceneNode");
	visualSceneNode.setAttribute("name", "VisualScene");
	IXMLElement nodeNode = visualSceneNode.createElement("node");
	visualSceneNode.addChild(nodeNode);
	nodeNode.setAttribute("id", "node");
	nodeNode.setAttribute("name", "node");

	IXMLElement instanceVisualSceneNode = sceneNode.createElement("instance_visual_scene");
	sceneNode.addChild(instanceVisualSceneNode);
	instanceVisualSceneNode.setAttribute("url", "#VisualSceneNode");




	int numVert = 0, numNorm = 0, numTexVert = 0;
	//Hashtable<String, String> groupNames = new Hashtable<String, String>();
	NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
	nf.setMaximumFractionDigits(6);
	//nf.setGroupingUsed(false);
	int numObjs = theScene.getNumObjects();
	for (int i = 0; i < numObjs; i++)
	{
		// Get a rendering mesh for the object.
		
		ObjectInfo info = theScene.getObject(i);

		if (!wholeScene && !info.selected)
			continue;

		if (info.getObject().getTexture() == null)
			continue;

		FacetedMesh mesh;
		if (!smooth && info.getObject() instanceof FacetedMesh)
		{
			mesh = (FacetedMesh) info.getObject();
		}
		else
		{
			mesh = info.getObject().convertToTriangleMesh(tol);
		}

		// skip objects with no mesh for the time being
		if (mesh == null){continue;}


		String objectName = info.name.replace(" ", "_");	// remove spaces from object names - not sure if needed
		
		// add the geometry node
		IXMLElement geometryNode = libraryGeometries.createElement("geometry");
		libraryGeometries.addChild(geometryNode);
		geometryNode.setAttribute("id", objectName + "-lib");
		geometryNode.setAttribute("name", objectName);

		// add the mesh node
		IXMLElement meshNode = geometryNode.createElement("mesh");
		geometryNode.addChild(meshNode);

		// add the mesh/source nodes
		IXMLElement sourcePositionsNode = meshNode.createElement("source");
		meshNode.addChild(sourcePositionsNode);
		sourcePositionsNode.setAttribute("id", objectName + "-lib-positions");

		IXMLElement sourceNormalsNode = meshNode.createElement("source");
		meshNode.addChild(sourceNormalsNode);
		sourceNormalsNode.setAttribute("id", objectName + "-lib-normals");

		// add the float_array nodes
		IXMLElement arrayPositionsNode = sourcePositionsNode.createElement("float_array");
		sourcePositionsNode.addChild(arrayPositionsNode);
		arrayPositionsNode.setAttribute("id", objectName + "-lib-positions-array");

		IXMLElement arrayNormalsNode = sourceNormalsNode.createElement("float_array");
		sourceNormalsNode.addChild(arrayNormalsNode);
		arrayNormalsNode.setAttribute("id", objectName + "-lib-normals-array");
		
		// add the vertices node
		IXMLElement verticesNode = meshNode.createElement("vertices");
		meshNode.addChild(verticesNode);
		verticesNode.setAttribute("id", objectName + "-lib-vertices");

		IXMLElement vertexInputNode = verticesNode.createElement("input");
		verticesNode.addChild(vertexInputNode);
		vertexInputNode.setAttribute("semantic", "POSITION");
		vertexInputNode.setAttribute("source", "#" + objectName + "-lib-positions");



		StringBuilder commentStr = new StringBuilder();

        // Find the normals.

        Vec3 norm[];
        int normIndex[][] = new int[mesh.getFaceCount()][];
        if (mesh instanceof TriangleMesh)
        {
        	
        commentStr.append("trianglemesh");

          RenderingMesh rm = ((TriangleMesh) mesh).getRenderingMesh(Double.MAX_VALUE, false, info);
          norm = rm.norm;
          for (int j = 0; j < normIndex.length; j++)
            normIndex[j] = new int[] {rm.triangle[j].n1, rm.triangle[j].n2, rm.triangle[j].n3};
        }
        else
        {

        commentStr.append("other");

          norm = mesh.getNormals();
          for (int j = 0; j < normIndex.length; j++)
          {
            normIndex[j] = new int[mesh.getFaceVertexCount(j)];
            for (int k = 0; k < normIndex[j].length; k++)
              normIndex[j][k] = mesh.getFaceVertexIndex(j, k);
          }
        }
        
        
		// Write out the object.

		StringBuilder vertexArrayStr = new StringBuilder();
		StringBuilder normalsArrayStr = new StringBuilder();
		
/*
		TextureImageInfo ti = null;
		if (textureExporter != null)
		{
			ti = textureExporter.getTextureInfo(info.getObject().getTexture());
			if (ti != null)
			{
				out.println("usemtl "+ti.name);
			}
		}
*/

        Mat4 trans = info.getCoords().fromLocal();
        MeshVertex vert[] = mesh.getVertices();
		for (int j = 0; j < vert.length; j++)
		{
			Vec3 v = trans.times(vert[j].r);
			vertexArrayStr.append(nf.format(v.x) + " " + nf.format(v.y) + " " + nf.format(v.z));
			if(j < vert.length-1){vertexArrayStr.append(" ");}
		}
		for (int j = 0; j < norm.length; j++)
		{
			if (norm[j] == null)
			{
				if(j != 0)	// skip null value at zero - don't know why
				{
					normalsArrayStr.append("1 0 0");
				}
			}
			else
			{
				Vec3 v = trans.timesDirection(norm[j]);
				normalsArrayStr.append(nf.format(v.x) + " " + nf.format(v.y) + " " + nf.format(v.z));
			}
			if(j < norm.length-1){normalsArrayStr.append(" ");}
		}
		
		int XYZStride = 3;

		int vertexPositionCount = vert.length * XYZStride;	// 3 positions per vertex
		arrayPositionsNode.setAttribute("count", Integer.toString(vertexPositionCount));
		arrayPositionsNode.setContent(vertexArrayStr.toString());

		InsertTechniqueNode(sourcePositionsNode, objectName + "-lib-positions-array", vert.length, XYZStride);

		int normalsCount = (norm.length - 1) * XYZStride;	// 3 positions per normal
		arrayNormalsNode.setAttribute("count", Integer.toString(normalsCount));
		arrayNormalsNode.setContent(normalsArrayStr.toString());

		InsertTechniqueNode(sourceNormalsNode, objectName + "-lib-normals-array", norm.length - 1, XYZStride);

		IXMLElement polyOrTriNode;
/*
        if (mesh instanceof TriangleMesh)
        {
			// add the triangles node
			polyOrTriNode = meshNode.createElement("triangles");
			//meshNode.addChild(polyOrTriNode);
			//polyOrTriNode.setAttribute("count", Integer.toString(vert.length/XYZStride));
		}
		else
		{
			// add the polygons node
			polyOrTriNode = meshNode.createElement("polygons");
			//meshNode.addChild(polyOrTriNode);
			//polyOrTriNode.setAttribute("count", Integer.toString(rm.triangle.length));
		}
*/
		polyOrTriNode = meshNode.createElement("triangles");
		//polyOrTriNode = meshNode.createElement("polygons");
		//polyOrTriNode = meshNode.createElement("polylist");

		meshNode.addChild(polyOrTriNode);
		polyOrTriNode.setAttribute("count", Integer.toString(mesh.getFaceCount()));

		// add the vertices input node
		IXMLElement vertInputNode = meshNode.createElement("input");
		polyOrTriNode.addChild(vertInputNode);
		vertInputNode.setAttribute("offset", "0");
		vertInputNode.setAttribute("semantic", "VERTEX");	//semantic="VERTEX"
		vertInputNode.setAttribute("source", "#" + objectName + "-lib-vertices");

		// add the normals input node
		IXMLElement normInputNode = meshNode.createElement("input");
		polyOrTriNode.addChild(normInputNode);
		normInputNode.setAttribute("offset", "1");
		normInputNode.setAttribute("semantic", "NORMAL");	//semantic="NORMAL"
		normInputNode.setAttribute("source", "#" + objectName + "-lib-normals");

/*
		// add the vcount node for polygons
		IXMLElement vcountNode = meshNode.createElement("vcount");
		polyOrTriNode.addChild(vcountNode);
*/

/*
		if( ti != null && 
			((Object3D) mesh).getTextureMapping() instanceof UVMapping && 
			((UVMapping) ((Object3D) mesh).getTextureMapping()).isPerFaceVertex(mesh))
		{
			// A per-face-vertex texture mapping.
			
			Vec2 coords[][] = ((UVMapping) ((Object3D) mesh).getTextureMapping()).findFaceTextureCoordinates(mesh);
			double uscale = (ti.maxu == ti.minu ? 1.0 : 1.0/(ti.maxu-ti.minu));
			double vscale = (ti.maxv == ti.minv ? 1.0 : 1.0/(ti.maxv-ti.minv));
			for (int j = 0; j < coords.length; j++)
			{
				for (int k = 0; k < coords[j].length; k++)
				{
					double u = (coords[j][k].x-ti.minu)*uscale;
					double v = (coords[j][k].y-ti.minv)*vscale;
					out.println("vt "+nf.format(u)+" "+nf.format(v));
				}
			}

			for (int j = 0; j < mesh.getFaceCount(); j++)
			{
				out.print("f ");

				for (int k = 0; k < mesh.getFaceVertexCount(j); k++)
				{
					int vertIndex = mesh.getFaceVertexIndex(j, k)+1;
					if (k > 0)
					{
						out.print(' ');
					}
					out.print(vertIndex+numVert);
					out.print('/');
					out.print(k+1+numTexVert);
					out.print('/');
					out.print(normIndex[j][k]+numNorm+1);
				}

				out.println();
				numTexVert += coords[j].length;
			}
		}
        else if (ti != null && 
        		((Object3D) mesh).getTextureMapping() instanceof Mapping2D)
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
					{
						out.print(' ');
					}
					out.print(vertIndex+numVert);
					out.print('/');
					out.print(vertIndex+numTexVert);
					out.print('/');
					out.print(normIndex[j][k]+numNorm+1);
				}
				out.println();
			}
			numTexVert += coords.length;
		}
        else
        {
*/
			// No texture coordinates.
			StringBuilder fullPrimitivesStr = new StringBuilder();
			StringBuilder vertexCountStr = new StringBuilder();

			// each face
			int faceCount = mesh.getFaceCount();
			for (int j = 0; j < faceCount; j++)
			{

				StringBuilder primitiveStr = new StringBuilder();

				int numFaceVertices = mesh.getFaceVertexCount(j);
				
				vertexCountStr.append(Integer.toString(numFaceVertices));
				if(j < faceCount - 1)
				{
					vertexCountStr.append(" ");
				}

				// each vertex
				for (int k = 0; k < numFaceVertices; k++)
				{
					//int vertIndex = mesh.getFaceVertexIndex(j, k)+1;
					int vertIndex = mesh.getFaceVertexIndex(j, k);

					primitiveStr.append(Integer.toString(vertIndex));
					primitiveStr.append(" ");
					int normalIndex = normIndex[j][k];
					primitiveStr.append(Integer.toString(normalIndex - 1));	// indexes are 1 based, subtract 1 for zero based
					if(k < numFaceVertices - 1)
					{
						primitiveStr.append(" ");
					}
				}
				
				fullPrimitivesStr.append(primitiveStr.toString());
				if(j < faceCount - 1)
				{
					fullPrimitivesStr.append(" ");
				}
				
/*
					// add the primitives node
					IXMLElement primitivesNode = meshNode.createElement("p");
					polyOrTriNode.addChild(primitivesNode);
					primitivesNode.setContent(primitiveStr.toString());

*/
			}	// end for each face
			
			comments.setContent(commentStr.toString());

/*
			// set the content of the vcount node
			vcountNode.setContent(vertexCountStr.toString());
*/


			// add the primitives node
			IXMLElement primitivesNode = meshNode.createElement("p");
			polyOrTriNode.addChild(primitivesNode);
			primitivesNode.setContent(fullPrimitivesStr.toString());


//        }

			// create the instance_geometry node and add it to the visueal scene node
			IXMLElement instanceGeometryNode = meshNode.createElement("instance_geometry");
			nodeNode.addChild(instanceGeometryNode);
			instanceGeometryNode.setAttribute("url", "#" + objectName + "-lib" );
			
			// bind the materials (add the bind_material node)
			IXMLElement bindMaterialNode = meshNode.createElement("bind_material");
			instanceGeometryNode.addChild(bindMaterialNode);

			// add a dummy technique_common node for now
			IXMLElement dummyTechniqueNode = meshNode.createElement("technique_common");
			bindMaterialNode.addChild(dummyTechniqueNode);

		}	// end for each object in scene

		// write out the entire xml tree    
		XMLWriter writer = new XMLWriter(out);
		writer.write(root);

	}


	private static void InsertTechniqueNode(IXMLElement parentNode, String arrayReference, int count, int stride)
	{

		IXMLElement techniqueNode = parentNode.createElement("technique_common");
		parentNode.addChild(techniqueNode);

  		IXMLElement accessorNode = techniqueNode.createElement("accessor");
		techniqueNode.addChild(accessorNode);
		accessorNode.setAttribute("source", "#" + arrayReference);
		accessorNode.setAttribute("count", Integer.toString(count));
		accessorNode.setAttribute("stride", Integer.toString(stride));

  		IXMLElement param1 = accessorNode.createElement("param");
		accessorNode.addChild(param1);
		param1.setAttribute("name", "X");
		param1.setAttribute("type", "float");

  		IXMLElement param2 = accessorNode.createElement("param");
		accessorNode.addChild(param2);
		param2.setAttribute("name", "Y");
		param2.setAttribute("type", "float");

  		IXMLElement param3 = accessorNode.createElement("param");
		accessorNode.addChild(param3);
		param3.setAttribute("name", "Z");
		param3.setAttribute("type", "float");

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
    
    out.println("#Produced by Arma Design Studio "+ArmaDesignStudio.getVersion()+", "+(new Date()).toString());
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
  
	
	public static String getISO8601Date()
	{

		Date date = new Date();

		// From: http://www.dynamicobjects.com/d2r/archives/003057.html
		SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String result = ISO8601FORMAT.format(date);
		//convert YYYYMMDDTHH:mm:ss+HH00 into YYYYMMDDTHH:mm:ss+HH:00 
		//- note the added colon for the Timezone
		result = result.substring(0, result.length()-2) 
		+ ":" + result.substring(result.length()-2);
		return result;
	}


}
