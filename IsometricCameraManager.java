package com.golden.game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.ActionListener;

public class IsometricCameraManager {
    private Vector3f position = new Vector3f(0, 35, 35);  // Starting position
    private float height = 35f;  // Fixed height
    private float angle = FastMath.PI / 4f;  // 45 degrees
    private float zoomLevel = 1.0f;
    private Camera camera;
    private InputManager inputManager;
    
    public IsometricCameraManager(Camera camera, InputManager inputManager) {
        this.camera = camera;
        this.inputManager = inputManager;
        initialize();
        setupControls();
    }
    
    public void initialize() {
        camera.setLocation(position);
        camera.lookAtDirection(new Vector3f(0, -1, -1).normalizeLocal(), Vector3f.UNIT_Y);
        camera.setFrustumPerspective(45f, camera.getAspect(), 0.1f, 1000f);
    }
    
    public void pan(float forward, float right) {
        // Use camera's direction vectors
        Vector3f forwardDir = camera.getDirection().clone();
        forwardDir.y = 0;  // Ignore vertical component
        forwardDir.normalizeLocal();

        Vector3f leftDir = camera.getLeft().clone();
        leftDir.y = 0;  // Ignore vertical component
        leftDir.normalizeLocal();

        position.addLocal(forwardDir.mult(forward));
        position.addLocal(leftDir.mult(right));

        // Maintain height
        position.y = height * zoomLevel;
        updateCamera();
    }
    
    public void zoom(float amount) {
        zoomLevel = FastMath.clamp(zoomLevel + amount, 0.5f, 2.0f);
        position.y = height * zoomLevel;
        updateCamera();
    }
    
    public void updateCamera() {
        // Set camera position
        camera.setLocation(position);
        
        // Set the tilt direction
        camera.lookAtDirection(new Vector3f(0, -1, -1).normalizeLocal(), Vector3f.UNIT_Y);
    }
    
    public Vector3f getPosition() {
        return position;
    }
    
    public void setPosition(Vector3f pos) {
        this.position = pos;
        updateCamera();
    }
    
    private void setupControls() {
        inputManager.addMapping("Pan_Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Pan_Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Pan_Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Pan_Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Zoom_In", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("Zoom_Out", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        
        inputManager.addListener(analogListener, "Pan_Forward", "Pan_Backward", "Pan_Left", "Pan_Right");
        inputManager.addListener(actionListener, "Zoom_In", "Zoom_Out");
    }
    
    private final AnalogListener analogListener = (String name, float value, float tpf) -> {
        float speed = 20f * tpf;
        
        switch (name) {
            case "Pan_Forward": pan(speed, 0); break;
            case "Pan_Backward": pan(-speed, 0); break;
            case "Pan_Left": pan(0, speed); break;
            case "Pan_Right": pan(0, -speed); break;
        }
    };

    private final ActionListener actionListener = (String name, boolean isPressed, float tpf) -> {
        if (isPressed) {
            if (name.equals("Zoom_In")) {
                zoom(-0.01f);
            } else if (name.equals("Zoom_Out")) {
                zoom(0.01f);
            }
        }
    };
}