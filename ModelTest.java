package com.golden.game;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;

public class ModelTest extends SimpleApplication {
    public static void main(String[] args) {
        System.out.println("Starting main");
        Logger.getLogger("").setLevel(Level.ALL);
        
        ModelTest app = new ModelTest();
        
        try {
            AppSettings settings = new AppSettings(true);
            settings.setTitle("Golden - Test Setup");
            settings.setResolution(800, 600);
            app.setSettings(settings);
            
            System.out.println("System Info:");
            System.out.println("OS Arch: " + System.getProperty("os.arch"));
            System.out.println("OS Name: " + System.getProperty("os.name"));
            System.out.println("Java Version: " + System.getProperty("java.version"));
            System.out.println("Working Directory: " + System.getProperty("user.dir"));
            
            app.start(JmeContext.Type.Display);
            
        } catch (Exception e) {
            System.out.println("Error in main:");
            e.printStackTrace();
        }
    }

    @Override
    public void simpleInitApp() {
        // Configure asset manager
        setupAssetManager();
        
        // Set up camera
        cam.setLocation(new Vector3f(0, 0, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // Create a box as a placeholder
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);

        // Try to load an OBJ file
        loadModel();
    }
    
    private void setupAssetManager() {
        // Get the project root directory
        String userDir = System.getProperty("user.dir");
        File assetsDir = new File(userDir, "assets");
        
        // Create assets directory if it doesn't exist
        if (!assetsDir.exists()) {
            assetsDir.mkdir();
            new File(assetsDir, "Models").mkdir();
            System.out.println("Created assets directory at: " + assetsDir.getAbsolutePath());
        }
        
        // Register the assets directory with the asset manager
        assetManager.registerLocator(assetsDir.getAbsolutePath(), FileLocator.class);
        

    }

    private void loadModel() {
        try {
            // First check if file exists
            String modelPath = "Models/TestModel.obj";
            if (checkModelFile(modelPath)) {
                System.out.println("Attempting to load model: " + modelPath);
                
                Spatial model = assetManager.loadModel(modelPath);
                if (model != null) {
                    // Add material to make sure it's visible
                    Material mat = new Material(assetManager, 
                        "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", ColorRGBA.Red);
                    model.setMaterial(mat);
                    
                    // Position and scale
                    model.setLocalTranslation(3, 0, 0);
                    model.scale(1.0f); // Adjust scale if needed
                    
                    // Add to scene
                    rootNode.attachChild(model);
                    System.out.println("Model loaded successfully");
                    
                    // Debug information
                    debugModel(model);
                }
            } else {
                System.out.println("Model file not found. Creating test cube...");
                createTestCube();
            }
        } catch (Exception e) {
            System.err.println("Error loading model:");
            e.printStackTrace();
        }
    }

    private boolean checkModelFile(String path) {
        File file = new File(System.getProperty("user.dir"), "assets/" + path);
        System.out.println("Checking for file: " + file.getAbsolutePath());
        boolean exists = file.exists();
        System.out.println("File exists: " + exists);
        return exists;
    }
    
    private void createTestCube() {
        // Create a red cube as a test model
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("TestCube", box);
        Material mat = new Material(assetManager, 
            "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        geom.setMaterial(mat);
        geom.setLocalTranslation(3, 0, 0);
        rootNode.attachChild(geom);
        System.out.println("Created test cube");
    }
    
    private void debugModel(Spatial model) {
        System.out.println("\nModel Debug Info:");
        System.out.println("Name: " + model.getName());
        System.out.println("Type: " + model.getClass().getSimpleName());
        System.out.println("Transform: " + model.getLocalTransform());
        System.out.println("Bounds: " + model.getWorldBound());
    }
}