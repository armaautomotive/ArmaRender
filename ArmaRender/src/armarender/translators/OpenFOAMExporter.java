/* Copyright (C) 2024, 2025 by Jon Taylor
   
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
import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.net.URL;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;

/** OpenFOAMExporter contains the  routines for exporting OpenFOAM files. */

public class OpenFOAMExporter
{
    
    /** 
     *
     * Description:
     *
     */
    public static void exportFile(BFrame parent, Scene theScene)
    {
        // Prompt user for cell size in meters.
        String input = JOptionPane.showInputDialog(null, "Enter cell size in meters:");
        
        
        // format file name
        String fileName = theScene.getName();
        if(fileName == null){
            System.out.println("Error: File must be saved first.");
            
            // Dialog
            
            return;
        }
        int extensionPos = fileName.indexOf(".ads");
        if(extensionPos != -1){
            fileName = fileName.substring(0, extensionPos);
        }
        
        String dir = theScene.getDirectory() + System.getProperty("file.separator") + fileName;
        File d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        // folder for tube bends
        dir = theScene.getDirectory() + System.getProperty("file.separator") + fileName + System.getProperty("file.separator") + "OpenFOAM";
        d = new File(dir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        String contantDir = dir + System.getProperty("file.separator") + "constant";
        d = new File(contantDir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        String polyMeshDir = contantDir + System.getProperty("file.separator") + "polyMesh";
        d = new File(polyMeshDir);
        if(d.exists() == false){
            d.mkdir();
        }
        
        String path = ""; // dir
        Vector<String> objFileList = new Vector<String>();
        Vector<ObjectInfo> writeObjects = new Vector<ObjectInfo>();
        Vector<ObjectInfo> objects = theScene.getObjects();
        
        Vector<Vec3> scenePoints = new Vector<Vec3>();
        Vector<TriangleMesh.Face> sceneFaces = new Vector<TriangleMesh.Face>();
        
        for(int a = 0; a < objects.size(); a++){
            ObjectInfo info = (ObjectInfo)objects.elementAt(a);
            if(info.getParent() == null){   // Only process root level objects.
                Object3D obj = info.getObject();
                //if( obj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT ){
                
                
                //System.out.println(" XXX " + path + " " +info.getName() );
                
                //if( info.getExport() > 0 ){
                    //System.out.println("Export Root Object "  + " " +info.getName() );
                    //writeObjectsToFile(theScene, info, path, errorField.getValue(), objFileList, true);
                //}
                
                TriangleMesh tm = null;
                if(obj instanceof TriangleMesh){
                    tm = (TriangleMesh)obj;
                } else if ( obj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT &&
                           obj instanceof Curve == false ){
                    tm = obj.convertToTriangleMesh(0.05);
                }
                if(tm != null){
                 
                    int pointIndexOffset = scenePoints.size();
                    
                    Mat4 mat4 = info.getCoords().fromLocal();
                    
                    MeshVertex [] verts = tm.getVertices();
                    for(int i = 0; i < verts.length; i++){
                        Vec3 p = new Vec3(verts[i].r);
                        mat4.transform(p);  // Convert to world coordinates.
                        
                        scenePoints.addElement( p );
                    }
                    
                    
                    TriangleMesh.Face[] faces = tm.getFaces();
                    for(int i = 0; i < faces.length; i++){
                        TriangleMesh.Face face = faces[i];
                        
                        // Modify point indicies, which point to verts in the current object list to an index in all scene points.
                        
                        face.v1 += pointIndexOffset;
                        face.v2 += pointIndexOffset;
                        face.v3 += pointIndexOffset;
                        
                        sceneFaces.addElement(face);
                    }
                    
                }
                
                
                //int progress = (int) (((float)(a) / (float)(objects.size())) * (float)100);
                //progressDialog.setProgress(progress);
            }
        }
        
        
        //
        // Point File
        //
        String pointFilePath = polyMeshDir + System.getProperty("file.separator") + "points";
        try (FileWriter writer = new FileWriter(pointFilePath)) {
            String content = "/*--------------------------------*- C++ -*----------------------------------*\\ \n"+
            "| =========                 |                                                 |\n"+
            "| \\      /  F ield         | OpenFOAM: The Open Source CFD Toolbox           |\n"+
            "|  \\    /   O peration     | Version:  9                                     |\n"+
            "|   \\  /    A nd           | Website:  www.OpenFOAM.org                      |\n"+
            "|    \\/     M anipulation  |                                                 |\n"+
            "\\*---------------------------------------------------------------------------*/\n"+
            "\n" +
            "FoamFile\n"+
            "{\n"+
            "    version     2.0;\n"+
            "    format      ascii;\n"+
            "    class       vectorField;\n"+
            "    location    \"constant/polyMesh\";\n"+
            "    object      points;\n"+
            "}\n"+
            "// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * //\n";
            
            content += "\n";
            content += "<"+scenePoints.size()+">\n" +
            "(\n";
            for(int i = 0; i < scenePoints.size(); i++){
                Vec3 point = scenePoints.elementAt(i);
                content += "    (x"+ point.x+" "+point.y+" "+point.z+");\n";
            }
            content += ")\n";
            
            writer.write(content);
            
            System.out.println("File written successfully.");
            
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
        
        
        //
        // Faces file
        //
        String facesFilePath = polyMeshDir + System.getProperty("file.separator") + "faces";
        try (FileWriter writer = new FileWriter(facesFilePath)) {
        
            
            // NOTE: we need to swap the face point indicies from object reference to scene reference.
            
            String content = "/*--------------------------------*- C++ -*----------------------------------*\\ \n"+
            "| =========                 |                                                 |\n"+
            "| \\      /  F ield         | OpenFOAM: The Open Source CFD Toolbox           |\n"+
            "|  \\    /   O peration     | Version:  9                                     |\n"+
            "|   \\  /    A nd           | Website:  www.OpenFOAM.org                      |\n"+
            "|    \\/     M anipulation  |                                                 |\n"+
            "\\*---------------------------------------------------------------------------*/\n"+
            "\n" +
            "FoamFile\n"+
            "{\n"+
            "    version     2.0;\n"+
            "    format      ascii;\n"+
            "    class       vectorField;\n"+
            "    location    \"constant/polyMesh\";\n"+
            "    object      faces;\n"+
            "}\n"+
            "// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * //\n";
            
            content += "\n";
            content += "<"+sceneFaces.size()+">\n" +
            "(\n";
            for(int i = 0; i < sceneFaces.size(); i++){
                TriangleMesh.Face face = sceneFaces.elementAt(i);
                
                content += "    (x"+ face.v1+" "+face.v2+" "+face.v3+");\n"; //
            }
            content += ")\n";
            
            writer.write(content);
            
            System.out.println("File written successfully.");
            
        
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
        
        
        
        //
        // Owner file
        //
        String ownerFilePath = polyMeshDir + System.getProperty("file.separator") + "owner";
        try (FileWriter writer = new FileWriter(ownerFilePath)) {
        
            String content = "/*--------------------------------*- C++ -*----------------------------------*\\ \n"+
            "| =========                 |                                                 |\n"+
            "| \\      /  F ield         | OpenFOAM: The Open Source CFD Toolbox           |\n"+
            "|  \\    /   O peration     | Version:  9                                     |\n"+
            "|   \\  /    A nd           | Website:  www.OpenFOAM.org                      |\n"+
            "|    \\/     M anipulation  |                                                 |\n"+
            "\\*---------------------------------------------------------------------------*/\n"+
            "\n" +
            "FoamFile\n"+
            "{\n"+
            "    version     2.0;\n"+
            "    format      ascii;\n"+
            "    class       vectorField;\n"+
            "    location    \"constant/polyMesh\";\n"+
            "    object      owner;\n"+
            "}\n"+
            "// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * //\n";
            
            content += "\n";
            content += "<"+sceneFaces.size()+">\n" +
            "(\n";
            for(int i = 0; i < sceneFaces.size(); i++){
                TriangleMesh.Face face = sceneFaces.elementAt(i);
                
                
                
                content += "    0;\n"; // NOTE this needs work
            }
            content += ")\n";
            
            writer.write(content);
            
            System.out.println("File written successfully.");
            
        
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
        
        //
        // neighbour
        //
        String neighbourFilePath = polyMeshDir + System.getProperty("file.separator") + "neighbour";
        try (FileWriter writer = new FileWriter(neighbourFilePath)) {
        
            String content = "/*--------------------------------*- C++ -*----------------------------------*\\ \n"+
            "| =========                 |                                                 |\n"+
            "| \\      /  F ield         | OpenFOAM: The Open Source CFD Toolbox           |\n"+
            "|  \\    /   O peration     | Version:  9                                     |\n"+
            "|   \\  /    A nd           | Website:  www.OpenFOAM.org                      |\n"+
            "|    \\/     M anipulation  |                                                 |\n"+
            "\\*---------------------------------------------------------------------------*/\n"+
            "\n" +
            "FoamFile\n"+
            "{\n"+
            "    version     2.0;\n"+
            "    format      ascii;\n"+
            "    class       vectorField;\n"+
            "    location    \"constant/polyMesh\";\n"+
            "    object      neighbour;\n"+
            "}\n"+
            "// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * //\n";
            
            content += "\n";
            content += "<"+1+">\n" +
            "(\n";
            //for(int i = 0; i < sceneFaces.size(); i++){
            //    TriangleMesh.Face face = sceneFaces.elementAt(i);
                content += "    1;\n"; // NOTE this needs work
            //}
            content += ")\n";
            
            writer.write(content);
            
            System.out.println("File written successfully.");
            
        
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
        
        
        if(parent instanceof LayoutWindow){
            ((LayoutWindow)parent).loadExportFolder();
        }
        
        
    }

    
    
    
}
