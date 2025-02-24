package com.golden.game;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.renderer.queue.RenderQueue;


import java.util.logging.Logger;

import java.util.logging.Level;
import com.jme3.light.DirectionalLight;
import com.jme3.light.AmbientLight;
import com.jme3.asset.TextureKey;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TabbedPanel;
import com.simsilica.lemur.TabbedPanel.Tab;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.MouseListener;
import com.simsilica.lemur.focus.FocusNavigationFunctions;
import com.simsilica.lemur.input.InputMapper;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import static com.golden.game.Data.*;

public class Main extends SimpleApplication {
    private Generator gen = new Generator();
    private CollisionGrid collisionGrid = new CollisionGrid(1f);
    private TransportNetwork net = new TransportNetwork(gen,collisionGrid);
    private Simulation sim = new Simulation(gen, net);
    private IsometricCameraManager isoCam;
    private GameAssetManager load;
    private Random r = new Random();
    private static final ColorRGBA BTN_NORMAL = new ColorRGBA(0.3f, 0.3f, 0.3f, 0.9f);
    private static final ColorRGBA BTN_HOVER = new ColorRGBA(0.4f, 0.4f, 0.4f, 0.9f);
    private static final ColorRGBA BTN_SELECTED = new ColorRGBA(0.5f, 0.5f, 0.7f, 0.9f);

    private boolean processingSlice = false;
    private boolean runningTurn = false;
    private int inactiveSlices=0;
    private String turnType = "work";
    private int slice = 0;
    private int aCounter=0;
    private int needsAnimation=0;
    
    boolean debugMode = true;
    boolean debugPanelOpen = false;
    boolean debugVis = true;
    float boundingCheck = 1.5f;
    private Node gridDebugNode;
    
    // Game state
    private boolean removingBuilding = false;
    private boolean placingBuilding = false;
    private boolean buildingValid = true;
    private boolean placingRoad = false;
    private boolean removingRoad = false;
    private String selectedBuilding;
    private int turn = sim.turn;
    private Spatial roadPreview;
    private Spatial nodePreview;
    private Spatial guideCircle;
    private Spatial buildingPreview;
    private Data lastPlacedNode;
    private boolean isFirstClick = true;
    private RawInputListener roadPlacementListener;
    private RawInputListener buildingPlacementListener;

    private String selectedPathType = "wooden";
    private Map<Data, List<Vector3f>> buildingConnections = new HashMap<>();
    private Data currentPath;  
    private Data currentBuilding;
    private boolean isShiftDown = false;
    private Data onNode = null;
    private Data onPoint = null;
    
    private List<AnimationControl> activeAnimations = new ArrayList<>();
    
    private Container bottomBar;
    private Container middleBar;
    private Container topInfoPanel;
    private Container buildingPanel;
    private Container agentPanel;
    
    private Container debugPanel;
    
    private Button selectedCategoryButton = null;
    private Button selectedBuildingButton = null;

    
    // Scene organization
    private Node buildingsNode;
    private Node agentsNode;
    private Node roadsNode;
    private Node rNodeNode;
    
    private Map<String,List<Data>> workSpots = new HashMap<>();
    private Map<String,List<Data>> storageSpots = new HashMap<>();
    private Map<String,List<Data>> snapSpots = new HashMap<>();
  
    // Model cache
    private Map<String, Spatial> modelCache = new HashMap<>();
    
    public static void main(String[] args) {
    	 System.out.println("Starting main");
         Logger.getLogger("").setLevel(Level.SEVERE);
         
         Main app = new Main();
         

         try {
             AppSettings settings = new AppSettings(true);
             settings.setTitle("Golden - Test Setup");
             
             // Anti-aliasing settings
             settings.setSamples(4);  // Start with 4x MSAA
             settings.setGammaCorrection(true);
             
             settings.setFrequency(60); // Match your display refresh rate
             settings.setVSync(true);   // Enable vertical sync
             settings.setStencilBits(8);
             
             
             settings.setWidth(1280);  // Instead of 1300
             settings.setHeight(768);  // Instead of 800

          // Force high-quality rendering
          settings.setRenderer(AppSettings.LWJGL_OPENGL32);
          settings.setSamples(4);
          settings.setGammaCorrection(true);

          // Try explicitly setting the framebuffer size
          settings.putInteger("FrameBufferWidth", 2560);
          settings.putInteger("FrameBufferHeight", 1600);
             // Enable framebuffer anti-aliasing
             settings.setDepthBits(24);
             settings.setBitsPerPixel(32);
             
             app.setSettings(settings);
             app.start(JmeContext.Type.Display);

         } catch (Exception e) {
             System.out.println("Error in main:");
             e.printStackTrace();
         }
    }

    @Override
    public void simpleInitApp() {
        // Setup asset manager
    	
    	
    	 Logger.getLogger("com.jme3.scene.plugins.OBJLoader").setLevel(Level.SEVERE);
         inputManager.deleteMapping(INPUT_MAPPING_EXIT);
    	 viewPort.setClearFlags(true, true, true);
    	 viewPort.setBackgroundColor(ColorRGBA.Black);
    	 renderManager.setSinglePassLightBatchSize(8);
    	 
    	    renderer.setDefaultAnisotropicFilter(16);
    	    
    	    // Enable edge smoothing at the viewport level
    	    viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.6f, 0.7f, 1.0f));
    	    RenderState renderState = new RenderState();
    	    renderState.setBlendMode(RenderState.BlendMode.Alpha);
  
    	 setupAssetManager();
         setupEnvironment();
         setupVisuals();
         
         GuiGlobals.initialize(this);
         BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
         GuiGlobals.getInstance().getStyles().setDefault(guiFont);
         

         InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
         inputMapper.deactivateGroup(FocusNavigationFunctions.UI_NAV);

	      // Then reinitialize only the ones we want
	      inputMapper.map(FocusNavigationFunctions.F_NEXT, KeyInput.KEY_TAB);
	      inputMapper.map(FocusNavigationFunctions.F_PREV, KeyInput.KEY_TAB, KeyInput.KEY_LSHIFT);
	      inputMapper.map(FocusNavigationFunctions.F_PREV, KeyInput.KEY_TAB, KeyInput.KEY_RSHIFT);
	      inputMapper.map(FocusNavigationFunctions.F_ACTIVATE, KeyInput.KEY_RETURN);
	      inputMapper.map(FocusNavigationFunctions.F_ACTIVATE, KeyInput.KEY_NUMPADENTER);

      // Reactivate the group
      inputMapper.activateGroup(FocusNavigationFunctions.UI_NAV);

         // Initialize scene nodes
         setupSceneNodes();

         flyCam.setEnabled(false);
         isoCam = new IsometricCameraManager(cam, inputManager);
         inputManager.setCursorVisible(true);
         
         load = new GameAssetManager(gen, assetManager);
         load.loadModels(modelCache);
         workSpots = load.getWorkSpots();
         storageSpots = load.getStorageSpots();
         snapSpots = load.getSnapSpots();
         createRoadPlacementListener();
         createBuildingPlacementListener();
         setupInputHandling();
         createInitialEntities();
         UniversalupdateStorageVisual();
         setupUISystem();     
         
         if(debugVis)
         {
         gridDebugNode = collisionGrid.createDebugNode(assetManager);
         rootNode.attachChild(gridDebugNode);
         }
  
     }
    
    private void createInitialEntities() {
        createAgents(3);
        createBuildings(0);
        createRnodes(1);
        sim.refreshNameMap();
    }
    
    private void setupInputHandling() {
        // Building placement
        inputManager.addMapping("PlaceBuilding", new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping("RemoveBuilding", new KeyTrigger(KeyInput.KEY_X));
        // Road placement
        inputManager.addMapping("ToggleRoadPlacement", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("RemoveRoad", new KeyTrigger(KeyInput.KEY_DELETE));
        // Shift key handling
        inputManager.addMapping("ShiftDown", new KeyTrigger(KeyInput.KEY_LSHIFT));

        // Game controls
        inputManager.addMapping("NextTurn", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("DebugMenu", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("CloseAll", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("CloseWindow", new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping("AddAgent", new KeyTrigger(KeyInput.KEY_C));

        inputManager.addListener(new ActionListener() {
            public void onAction(String name, boolean isPressed, float tpf) {
                switch (name) {
                    case "ShiftDown":
                        isShiftDown = isPressed;  // Track if LSHIFT is held down
                        break;
                    case "PlaceBuilding":
                        if (isPressed) 
                        {
                        	toggleBuildingPlacement();
                        }
                        break;
                    case "RemoveBuilding":
                        if (isPressed) removingBuilding = !removingBuilding;
                        break;
                    case "ToggleRoadPlacement":
                        if (isPressed) {
                            if (!isShiftDown) {
                            	toggleRoadPlacement();
                            } else {
                                toggleRoadRemoval();
                            }
                        }
                        break;
                    case "RemoveRoad":
                        if (isPressed) {
                           toggleRoadRemoval();
                        }
                        break;
                    case "NextTurn":
                        if (isPressed&&!runningTurn) 
                        {
                        	turnType="work";
                        	inactiveSlices=0;
                        	slice=0;
                        	runTurn();
                        }
                        break;
                    case "DebugMenu":
                    	if (isPressed&&debugMode)
                    	{
                    		toggleDebugMenu();
                    		sim.processAll("bills");
                    	}
                    break;
                    case "CloseAll":
                    	if(isPressed)
                    	{
                    		closeAll();
                    	}
                    break;
                    case "CloseWindow":
                    	if(isPressed)
                    	{
                    		System.exit(0);
                    	}
                    break;
                    case "AddAgent":
                    	if(isPressed)
                    	{
                    		createAgents(1);
                    		sim.refreshNameMap();
                    	}
                    break;
                }
            }
        }, "PlaceBuilding", "RemoveBuilding", "ToggleRoadPlacement", "RemoveRoad", "ShiftDown", "NextTurn","DebugMenu","CloseAll","CloseWindow","AddAgent");
    }


    private void setupUISystem() {
        // Create our categories map with better naming
        Map<String, List<String>> categoryMap = new HashMap<>();
        
        // Handle paths separately
        categoryMap.put("Paths", Arrays.asList(gen.pathTypes));
        
        // Handle buildings by level
        Map<Integer, List<String>> buildingsByLevel = new HashMap<>();
        for (String buildingType : gen.buildingTypes) {
            Data building = gen.makeBuilding(buildingType);
            int level = building.getNote("level", I);
            buildingsByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(buildingType);
        }
        
        // Add buildings to category map
        categoryMap.put("Basic", buildingsByLevel.get(0));
        categoryMap.put("Storage", buildingsByLevel.get(1));
        categoryMap.put("Production", buildingsByLevel.get(2));

        // Bottom bar setup
        bottomBar = new Container();
        float bottomBarWidth = 400f;  // Made wider to accommodate horizontal buttons
        float bottomBarHeight = 60f;
        bottomBar.setPreferredSize(new Vector3f(bottomBarWidth, bottomBarHeight, 0));
        bottomBar.setLocalTranslation(
            (cam.getWidth() - bottomBarWidth) / 2,
            50,
            0
        );

        QuadBackgroundComponent bg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.85f));
        bottomBar.setBackground(bg);
        
        // Create a horizontal container
        Container buttonContainer = new Container();
        buttonContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Even, FillMode.None));
        
        // Add buttons in order: Paths first, then other levels
        List<Integer> orderedLevels = new ArrayList<>(buildingsByLevel.keySet());
        Collections.sort(orderedLevels);  // Sort levels
        
        for (String category : Arrays.asList("Paths", "Basic", "Storage", "Production")) {
            Button categoryBtn = createStyledButton(category);

            // Store level for the click handler
            categoryBtn.addClickCommands(source -> handleCategorySelection(categoryBtn,category,categoryMap));
            
            buttonContainer.addChild(categoryBtn);
        }
        
        bottomBar.addChild(buttonContainer);
        guiNode.attachChild(bottomBar);
    }

    private void showMiddleBar(String category, List<String> items) {
        
        if (middleBar != null) {
            middleBar.removeFromParent();
        }

        middleBar = new Container();
        float middleBarWidth = 800f;  // Match bottom bar width
        float middleBarHeight = 40f;
        middleBar.setPreferredSize(new Vector3f(middleBarWidth, middleBarHeight, 0));
        
        // Position above bottom bar
        middleBar.setLocalTranslation(
            (cam.getWidth() - middleBarWidth) / 2,
            bottomBar.getLocalTranslation().y + bottomBar.getPreferredSize().y + 2,
            0
        );

        QuadBackgroundComponent bg = new QuadBackgroundComponent(new ColorRGBA(0.25f, 0.25f, 0.25f, 0.85f));
        middleBar.setBackground(bg);
        
        // Create horizontal button container
        Container buildingButtons = new Container();
        buildingButtons.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Even, FillMode.None));

        if(category=="Paths") {
        for (String pathType : gen.pathTypes) {
            Data pathData = gen.generateRoadNode(new Vector3f(0,0,0), pathType);
            if (!pathData.hasNote("visible") || pathData.getNote("visible", B)) {
                Button pathBtn = createStyledButton(pathType);
              
                pathBtn.addClickCommands(source -> handlePathSelection(pathBtn, pathType));
                buildingButtons.addChild(pathBtn);
            }
        }
        }
        else
        {
        for (String item : items) {
        	Data buildingData = gen.makeBuilding(item);

        	if(sim.techs.containsAll(buildingData.getNote("techreq",SL)))
        	{
            Button itemBtn = createStyledButton(item);
            itemBtn.addClickCommands(source -> handleBuildingSelection(itemBtn, item));
            buildingButtons.addChild(itemBtn);
        	}
        }
        }
        
        middleBar.addChild(buildingButtons);
        guiNode.attachChild(middleBar);  // Make sure we actually attach it!
       
    }

    private void showBuildingInfo(String buildingType) {
        if (topInfoPanel != null) {
            topInfoPanel.removeFromParent();
        }

        Data building = gen.makeBuilding(buildingType);
        
        topInfoPanel = new Container();
        float infoPanelWidth = 300f;
        float infoPanelHeight = 150f;
        topInfoPanel.setPreferredSize(new Vector3f(infoPanelWidth, infoPanelHeight, 0));
        
        // Position above middle bar
        topInfoPanel.setLocalTranslation(
            (cam.getWidth() - infoPanelWidth) / 2,
            middleBar.getLocalTranslation().y + middleBar.getPreferredSize().y + 10,
            0
        );

        QuadBackgroundComponent bg = new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 0.95f));
        topInfoPanel.setBackground(bg);
        topInfoPanel.setInsets(new Insets3f(15, 15, 15, 15));

        // Create formatted info text
        Container infoContainer = new Container();
        infoContainer.setLayout(new SpringGridLayout(Axis.Y, Axis.X));
        
        Label titleLabel = new Label(buildingType.toUpperCase());
        titleLabel.setFontSize(20f);
        infoContainer.addChild(titleLabel);

        if (building.hasNote("produces")) {
            Label producesLabel = new Label("PRODUCES:");
            producesLabel.setFontSize(16f);
            infoContainer.addChild(producesLabel);
            
            ArrayList<Data> produces = building.getNote("produces", DL);
            ArrayList<Integer> prodAmt = building.getNote("prodamt", IL);
            for (int i = 0; i < produces.size(); i++) {
                Label itemLabel = new Label("  • " + produces.get(i).getNote("type", S) + ": " + prodAmt.get(i));
                itemLabel.setFontSize(14f);
                infoContainer.addChild(itemLabel);
            }
        }
        
        topInfoPanel.addChild(infoContainer);
    }

    private void hideInfoPanel() {
        if (topInfoPanel != null) {
            topInfoPanel.removeFromParent();
            topInfoPanel = null;
        }
    }
    
    private void toggleButtonSelection(Button button, boolean selected) {
        QuadBackgroundComponent selectedBg = new QuadBackgroundComponent(BTN_SELECTED);
        QuadBackgroundComponent normalBg = new QuadBackgroundComponent(BTN_NORMAL);
        button.setBackground(selected ? selectedBg.clone() : normalBg.clone());
    }
    
    private Button createStyledButton(String text) {
        Button button = new Button(text);
        
        button.setFontSize(16f);
        button.setColor(ColorRGBA.White);  // Normal text color
        button.setFocusColor(ColorRGBA.White);
        button.setPreferredSize(new Vector3f(120, 40, 0));
        button.setTextHAlignment(HAlignment.Center);
        button.setTextVAlignment(VAlignment.Center);
        
        // Create backgrounds for different states
        QuadBackgroundComponent normalBg = new QuadBackgroundComponent(BTN_NORMAL);
        QuadBackgroundComponent hoverBg = new QuadBackgroundComponent(BTN_HOVER);
        QuadBackgroundComponent selectedBg = new QuadBackgroundComponent(BTN_SELECTED);
        
        button.setBackground(normalBg.clone());
        
        // Add mouse listener with debounce to prevent flickering
        long[] lastEventTime = {0}; // Array to allow modification in lambda
        
        button.addMouseListener(new MouseListener() {
            @Override
            public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastEventTime[0] < 10) return; // Debounce
                lastEventTime[0] = currentTime;
                
                if (button != selectedBuildingButton && button != selectedCategoryButton) {
                    button.setBackground(hoverBg.clone());
                }
            }

            @Override
            public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastEventTime[0] < 10) return; // Debounce
                lastEventTime[0] = currentTime;
                
                if (button != selectedBuildingButton && button != selectedCategoryButton) {
                    button.setBackground(normalBg.clone());
                }
            }

            @Override public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {}
            @Override public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {}
        });
        
        return button;
    }

    private void handleCategorySelection(Button newSelection,String category ,Map<String, List<String>> categoryMap ) {
        // If clicking already selected button, deselect everything
        if (newSelection == selectedCategoryButton) {
            selectedCategoryButton = null;
            toggleButtonSelection(newSelection, false);
            if (middleBar != null) {
                middleBar.removeFromParent();
                middleBar = null;
            }
            // Also clear building selection if any
            if (selectedBuildingButton != null) {
                toggleButtonSelection(selectedBuildingButton, false);
                selectedBuildingButton = null;
            }
            placingBuilding = false;
            placingRoad = false;
        } else {
            // Deselect previous category if any
            if (selectedCategoryButton != null) {
                toggleButtonSelection(selectedCategoryButton, false);
            }
            // Select new category
            selectedCategoryButton = newSelection;
            toggleButtonSelection(newSelection, true);
            
            // Clear any building selection
            if (selectedBuildingButton != null) {
                toggleButtonSelection(selectedBuildingButton, false);
                selectedBuildingButton = null;
            }
            placingBuilding = false;
            placingRoad = false;
            
            // Show new category content
            showMiddleBar(category, categoryMap.get(category));
        }
    }

    private void handleBuildingSelection(Button newSelection, String buildingType) {
        // If clicking already selected building, deselect it
        if (newSelection == selectedBuildingButton) {
            selectedBuildingButton = null;
            toggleButtonSelection(newSelection, false);
            toggleBuildingPlacement();
        } else {
            // Deselect previous building if any
            if (selectedBuildingButton != null) {
                toggleButtonSelection(selectedBuildingButton, false);
                toggleBuildingPlacement();
            }
            // Select new building
            selectedBuildingButton = newSelection;
            toggleButtonSelection(newSelection, true);
            selectedBuilding = buildingType;
            toggleBuildingPlacement();
        }
    }

    private void handlePathSelection(Button newSelection, String pathType) {
        // If clicking already selected path, deselect it
        if (newSelection == selectedBuildingButton) {
            selectedBuildingButton = null;
            toggleButtonSelection(newSelection, false);
            toggleRoadPlacement();
        } else {
            // Deselect previous selection if any
            if (selectedBuildingButton != null) {
                toggleButtonSelection(selectedBuildingButton, false);
                toggleRoadPlacement();
            }
            // Select new path
            selectedBuildingButton = newSelection;
            toggleButtonSelection(newSelection, true);
            selectedPathType = pathType;
            toggleRoadPlacement();
        }
    }
    
    private void toggleDebugMenu()
    {
    	if(!debugPanelOpen)
    	{
    		showDebugMenu();
    		debugPanelOpen=true;
    	}
    	else
    	{
    		System.out.println("  [DEBUG] Hiding debug panel");
    		guiNode.detachChild(debugPanel);
    		debugPanelOpen=false;
    	}
    }
    
    private void closeAll()
    {
    	if(debugPanelOpen)
    		guiNode.detachChild(debugPanel);
    	if(buildingPanel!=null)
    		buildingPanel.removeFromParent();
    	if(agentPanel!=null)
    		agentPanel.removeFromParent();
    }
    
    private void showDebugMenu() {
        System.out.println(" [DEBUG] Showing debug menu");
        System.out.println(" [DEBUG] Cam width: " + cam.getWidth());
        System.out.println(" [DEBUG] Cam height: " + cam.getHeight());

        // Main debug panel
        debugPanel = new Container();
        float debugPanelWidth = 300f;
        float debugPanelHeight = 600f;
        debugPanel.setPreferredSize(new Vector3f(debugPanelWidth, debugPanelHeight, 0));
        debugPanel.setLocalTranslation(20, cam.getHeight() - 50, 0);

        // Background for main panel
        QuadBackgroundComponent bg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.85f));
        debugPanel.setBackground(bg);

        // Create the tabbed panel
        TabbedPanel tabbedPanel = new TabbedPanel();
        
        // Create content containers for each tab
        Container buildingsPanel = new Container();
        buildingsPanel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.5f, 0.2f, 0.2f, 0.85f)));
        buildingsPanel.setPreferredSize(new Vector3f(300f, 575f, 0));
        buildingsPanel.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None));
       
        Container buildingSelectorPanel = new Container();
        buildingSelectorPanel.setPreferredSize(new Vector3f(100f,400f,0));
        buildingSelectorPanel.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
        buildingSelectorPanel.setInsets(new Insets3f(4,4,4,4));
        QuadBackgroundComponent buildingSelectorPanelBg = new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.9f));
        buildingSelectorPanel.setBackground(buildingSelectorPanelBg);
        for(Data building : sim.buildings)
        {
        	Button buildingBtn = new Button(building.getNote("name", S));
        	buildingBtn.setPreferredSize(new Vector3f(80,20f,0f));
        	buildingBtn.setInsets(new Insets3f(1,1,1,1));
        	buildingBtn.setFontSize(10f);
        	buildingBtn.setTextHAlignment(HAlignment.Center);
        	buildingBtn.setTextVAlignment(VAlignment.Center);
        	QuadBackgroundComponent buildingBtnBg = new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.4f, 0.8f));
        	buildingBtn.setBackground(buildingBtnBg);
        	addEffectsToButton(buildingBtn);
        	buildingSelectorPanel.addChild(buildingBtn);
        }

        
        buildingsPanel.addChild(buildingSelectorPanel);
        
        
        Container agentsPanel = new Container();
        agentsPanel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.5f, 0.2f, 0.2f, 0.85f)));
        agentsPanel.setPreferredSize(new Vector3f(300f, 575f, 0));
        // Add agents-specific content here

        // Add tabs with their content
        tabbedPanel.addTab("Buildings", buildingsPanel);
        tabbedPanel.addTab("Agents", agentsPanel);
        
        for(Tab s : tabbedPanel.getTabs())
        {
        	Button tab = s.getTitleButton();
        	customizeTabButton(tab);
        }
        // Style the tab panel if needed
        tabbedPanel.setPreferredSize(new Vector3f(300f, 575f, 0));

        debugPanel.addChild(tabbedPanel);

        guiNode.attachChild(debugPanel);
    }

    // Optional: Custom tab button styling
    private void customizeTabButton(Button tab) {
        tab.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 0.85f)));
        tab.setInsets(new Insets3f(0,2,0,2));
        tab.setColor(ColorRGBA.White);  // Normal text color
        tab.setFocusColor(ColorRGBA.White);
        addEffectsToButton(tab);
    }
    
    private void addEffectsToButton(Button button) {
    	
        long[] lastEventTime = {0};
        QuadBackgroundComponent oldbg = (QuadBackgroundComponent) button.getBackground();
    	ColorRGBA oldcl = new ColorRGBA(oldbg.getColor());
    	
        button.addMouseListener( new MouseListener() {
            @Override
            public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastEventTime[0] < 10) return; // Debounce
                lastEventTime[0] = currentTime;
                
                QuadBackgroundComponent bg = new QuadBackgroundComponent(BTN_HOVER);
                button.setBackground(bg);
            }

            @Override
            public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastEventTime[0] < 10) return; // Debounce
                lastEventTime[0] = currentTime;
                
                QuadBackgroundComponent bg = new QuadBackgroundComponent(oldcl);
                button.setBackground(bg);
                
            }

			@Override
			public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
				
			}

			@Override
			public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {
				
			}
        });
    }


   
    private void createBuildingPlacementListener() {
    	buildingPlacementListener = new RawInputListener() {
            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                if (placingBuilding && buildingPreview != null) {
                    Vector3f currentPoint = getWorldMousePosition();
                    if (currentPoint != null) {
                        updateMouseMoveB(currentPoint);
                    }
                }
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
                if (evt.isPressed() && evt.getButtonIndex() == MouseInput.BUTTON_LEFT)
                {
                    Vector3f clickPoint = getWorldMousePosition();
                    if (clickPoint != null) 
                    {
                    	if(placingBuilding&&buildingValid)
                    	{
                    	placeBuilding(clickPoint,selectedBuilding);
                    	}
                    }
                }
            }

            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}
            @Override public void onTouchEvent(TouchEvent evt) {}
        };
    }
    
    private void createRoadPlacementListener() {
        roadPlacementListener = new RawInputListener() {
            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                if (placingRoad && roadPreview != null) {
                    Vector3f currentPoint = getWorldMousePosition();
                    if (currentPoint != null) {
                        updateMouseMove(currentPoint);
                    }
                }
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
                if (evt.isPressed() && evt.getButtonIndex() == MouseInput.BUTTON_LEFT)
                {
                    Vector3f clickPoint = getWorldMousePosition();
                    if (clickPoint != null) 
                    {
                    	if(placingRoad)
                    	{
                    	handleRoadPlacement(clickPoint);
                    	}
                    }
                }
            }

            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}
            @Override public void onTouchEvent(TouchEvent evt) {}
        };
    }


    private Vector3f getWorldMousePosition() {
        Vector2f click2d = inputManager.getCursorPosition();
        Vector3f click3d = cam.getWorldCoordinates(
            new Vector2f(click2d.x, click2d.y), 0f);
        Vector3f dir = cam.getWorldCoordinates(
            new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d);
        dir.normalizeLocal();

        Plane plane = new Plane(Vector3f.UNIT_Y, 0);
        Ray ray = new Ray(click3d, dir);
        Vector3f intersection = new Vector3f();
        
        if (ray.intersectsWherePlane(plane, intersection)) {
            return intersection.clone();
        }
        return null;
    }
  
    private void updateMouseMoveB(Vector3f mousePosition) 
    {
    	 if (!placingBuilding) return;
         currentBuilding = sim.buildingMap.get(selectedBuilding);
         if (currentBuilding == null) return;
         
         buildingPreview.setCullHint(CullHint.Never);
         
        String improves = currentBuilding.getNote("improves", S);
        int level = currentBuilding.getNote("level", I);
    	BoundingBox bBox = findTrueBoundingBox(selectedBuilding);
    	bBox.setCenter(mousePosition);
         
         TransportNetwork.SnapResult snapResult = 
                 net.findRnodeSnap(mousePosition, 2f, null, level>0 ? null : improves,sim.rNodes,bBox);
         if(!snapResult.legal)     
         {
             buildingValid=false;
        	 buildingPreview.setLocalTranslation(mousePosition);
        	 updateBuildingPreviewColor(ColorRGBA.Red);
        	 onNode=null;
             onPoint = null;
         }
         else if (snapResult.node != null && snapResult.snapPoint != null && !snapResult.snapPoint.getNote("occupied",B) && sim.buildingMap.get(selectedBuilding).hasNote("improves")) {
             buildingValid=true;
        	 buildingPreview.setLocalTranslation(snapResult.snapPoint.getPosition());
             updateBuildingPreviewColor(snapResult.isExisting ? ColorRGBA.Blue : ColorRGBA.Green);
             onNode = snapResult.node;
             onPoint = snapResult.snapPoint;
         } 
         else if(inBuilding(bBox)!=null)
         {
             buildingValid=true;
        	 buildingPreview.setLocalTranslation(mousePosition);
        	 updateBuildingPreviewColor(ColorRGBA.Green);
        	 onNode=null;
             onPoint = null;
         }
         else
         {
             buildingValid=true;
        	 buildingPreview.setLocalTranslation(mousePosition);
        	 updateBuildingPreviewColor(ColorRGBA.White);
        	 onNode=null;
             onPoint = null;
         }
         
    }
    
    private Data inBuilding(BoundingBox bBox)
    {
    	for(Data b : sim.buildings)
    	{
    		if(findTrueBoundingBox(b).intersects(bBox))
    		{
    		return b;
    		}
    	}
    	return null;
    }
    
    private void updateMouseMove(Vector3f mousePosition) {
        if (!placingRoad) return;
        currentPath = net.pathMap.get(selectedPathType);
        if (currentPath == null) return;
        
        guideCircle.setCullHint(CullHint.Dynamic);
        nodePreview.setCullHint(CullHint.Dynamic);
        roadPreview.setCullHint(CullHint.Dynamic);
        
        if (isFirstClick || lastPlacedNode == null) {  // Added null check
            // Update guide circle position
           
            TransportNetwork.SnapResult snapResult = 
                net.findSnapPoint(mousePosition, currentPath.getNote("spacing", L), null, buildingConnections);
                
            if (snapResult.node != null) {
                guideCircle.setLocalTranslation(snapResult.node.getPosition());
                nodePreview.setLocalTranslation(snapResult.node.getPosition());
                roadPreview.setLocalTranslation(snapResult.node.getPosition());
                Material mat = ((Geometry)nodePreview).getMaterial();
                mat.setColor("Color", snapResult.isExisting ? ColorRGBA.Blue : ColorRGBA.Green);
                updateGuideCircleColor(snapResult.isExisting ? ColorRGBA.Blue : ColorRGBA.Green);
            } 
            else if (snapResult.legal)
            {
                guideCircle.setLocalTranslation(mousePosition);
                nodePreview.setLocalTranslation(mousePosition);
                roadPreview.setLocalTranslation(mousePosition);
                Material mat = ((Geometry)nodePreview).getMaterial();
                mat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f));
                updateGuideCircleColor( new ColorRGBA(1, 1, 1, 0.5f));
            }
            else
            {
	    	   guideCircle.setLocalTranslation(mousePosition);
	           nodePreview.setLocalTranslation(mousePosition);
	           roadPreview.setLocalTranslation(mousePosition);
	           Material mat = ((Geometry)nodePreview).getMaterial();
	           mat.setColor("Color", new ColorRGBA(1f, 0.0f, 0.0f, 0.5f));
	           updateGuideCircleColor( new ColorRGBA(1, 1, 1, 0.5f));
            }
        } else {
            // Only do this part if we have a lastPlacedNode
            TransportNetwork.SnapResult snapResult = 
                net.findSnapPoint(mousePosition, currentPath.getNote("spacing", L), lastPlacedNode, buildingConnections);
                
            Vector3f endPoint = snapResult.node != null ? 
                snapResult.node.getPosition() : 
                constrainToCircle(lastPlacedNode.getPosition(), mousePosition, currentPath.getNote("spacing", L));
                
            guideCircle.setLocalTranslation(lastPlacedNode.getPosition());
            updateGuideCircleColor( new ColorRGBA(1, 1, 1, 0.5f));
            updateRoadPreview(lastPlacedNode.getPosition(), endPoint);
            updateNodePreview(endPoint, !snapResult.legal ? ColorRGBA.Red : (snapResult.node != null ? (snapResult.isExisting ? ColorRGBA.Blue : ColorRGBA.Green) : ColorRGBA.Gray));
        }
    }
    
    
   
    private void setupRoadPlacementSystem() {
        // Create guide circle
    	currentPath = net.pathMap.get(selectedPathType);
        Cylinder cylinder = new Cylinder(3, 32, currentPath != null ? currentPath.getNote("spacing", L) : 8f, 2f, false);

        Material guideMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        guideMat.setColor("Color", new ColorRGBA(1, 1, 1, 0.5f));
        guideMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        guideMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        
        
        guideCircle = new Geometry("guide_circle", cylinder);
        guideCircle.rotate(new Quaternion(0f,1f,1f,0f));
        guideCircle.setMaterial(guideMat);
        guideCircle.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        
        // Create node preview
        Sphere sphere = new Sphere(8, 16, 1f);
        Material nodeMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        nodeMat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f));
        
        nodePreview = new Geometry("node_preview", sphere);
        nodePreview.setMaterial(nodeMat);
        nodePreview.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        // Create road preview
        Box box = new Box(0.5f, 0.1f, 0.5f);
        Material roadMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        roadMat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 0.5f, 0.5f));
        
        roadPreview = new Geometry("road_preview", box);
        roadPreview.setMaterial(roadMat);
        roadPreview.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        guideCircle.setCullHint(CullHint.Always);
        nodePreview.setCullHint(CullHint.Always);
        roadPreview.setCullHint(CullHint.Always);
        
        rootNode.attachChild(guideCircle);
        rootNode.attachChild(roadPreview);
        rootNode.attachChild(nodePreview);
    }

    private void setupBuildingPlacement()
    {
    BoundingBox bBox = findTrueBoundingBox(selectedBuilding);
    float radius = Math.max(bBox.getXExtent(), bBox.getZExtent());
   
    Sphere sphere = new Sphere(16, 16, radius);
    Material guideMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    guideMat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 1.0f, 0.5f));
    //guideMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    
    buildingPreview = new Geometry("building_preview", sphere);
    buildingPreview.setMaterial(guideMat);
    buildingPreview.setQueueBucket(RenderQueue.Bucket.Transparent);
    
    buildingPreview.setCullHint(CullHint.Always);
    
    rootNode.attachChild(buildingPreview);
    }
    
   
    private void toggleBuildingPlacement() {
        placingBuilding = !placingBuilding;
        placingRoad=false;
        cleanPreviews();
        
        if (placingBuilding) {
        	removingBuilding = false;
           // isFirstClick = true;
            currentBuilding = sim.buildingMap.get(selectedBuilding);
            inputManager.addRawInputListener(buildingPlacementListener);
        	setupBuildingPlacement();
        } else {
           //Cancel building placment here!
            inputManager.removeRawInputListener(buildingPlacementListener);
            //lastPlacedNode = null;
        }
    }
    
    private void toggleRoadPlacement() {
        placingRoad = !placingRoad;
        placingBuilding=false;
        cleanPreviews();
        
        if (placingRoad) {
        	removingRoad = false;
            isFirstClick = true;
            inputManager.addRawInputListener(roadPlacementListener);
            setupRoadPlacementSystem();
        } else {
           cancelRoadPlacement();
            inputManager.removeRawInputListener(roadPlacementListener);
            lastPlacedNode = null;
        }
    }
    private void toggleRoadRemoval() {
        removingRoad = !removingRoad;
        placingBuilding=false;
        if(placingRoad)
        cancelRoadPlacement();
        cleanPreviews();
    }
    
    private void cleanPreviews()
    {
    if(roadPreview!=null) roadPreview.removeFromParent();
    if(nodePreview!=null) nodePreview.removeFromParent();
    if(guideCircle!=null) guideCircle.removeFromParent();
    if(buildingPreview!=null) buildingPreview.removeFromParent();

    }
    
    private void cancelRoadPlacement() {
        if (guideCircle != null) {
            rootNode.detachChild(guideCircle);
            guideCircle = null;
        }
        if (nodePreview != null) {
            rootNode.detachChild(nodePreview);
            nodePreview = null;
        }
        if (roadPreview != null) {
            rootNode.detachChild(roadPreview);
            roadPreview = null;
        }
        
        lastPlacedNode = null;
        isFirstClick = true;
        placingRoad = false;
        
        // Remove any mouse move handlers
        if (roadPlacementListener != null) {
            inputManager.removeRawInputListener(roadPlacementListener);
        }
    }
    
   
    private void handleRoadPlacement(Vector3f clickPoint) {
        if (clickPoint == null) return;
        
        Data path = net.pathMap.get(selectedPathType);
        float spacing = path.getNote("spacing", L);
        TransportNetwork.SnapResult snapResult = 
            net.findSnapPoint(clickPoint, spacing, isFirstClick ? null : lastPlacedNode, buildingConnections);

        Vector3f placementPoint;
        boolean useSnap = false;
        
        if (snapResult.node != null) {
            placementPoint = snapResult.node.getPosition();
            useSnap = true;
        } else if (!isFirstClick && lastPlacedNode != null) {
            // Constrain to spacing radius if no snap point
            placementPoint = constrainToCircle(lastPlacedNode.getPosition(), clickPoint, spacing);
        } else {
            placementPoint = clickPoint;
        }

        if (isFirstClick) {
            // Always place first node, snapped or not
            lastPlacedNode = useSnap && snapResult.isExisting ? snapResult.node :
                net.createNode(placementPoint, selectedPathType);
                
            updateGuideCircle(lastPlacedNode.getPosition(),
                useSnap ? (snapResult.isExisting ? ColorRGBA.Blue : ColorRGBA.Green) : ColorRGBA.White);
            isFirstClick = false;
        } else {
            // Place subsequent nodes
            Data endNode = useSnap && snapResult.isExisting ? snapResult.node :
                net.createNode(placementPoint, selectedPathType);
                
            net.connectNodes(lastPlacedNode, endNode);
            createRoadVisual(lastPlacedNode, endNode,path.getNote("color", RGBA),lastPlacedNode,endNode);
            lastPlacedNode = endNode;
        }
    }
   
    private void updateGuideCircleColor(ColorRGBA color) {
        Material mat = ((Geometry)guideCircle).getMaterial();
        mat.setColor("Color", color);
    }

    private void updateGuideCircle(Vector3f position, ColorRGBA color) {
        guideCircle.setLocalTranslation(position);
        updateGuideCircleColor(color);
    }

    private void updateNodePreview(Vector3f position, ColorRGBA color) {
        nodePreview.setLocalTranslation(position);
        Material mat = ((Geometry)nodePreview).getMaterial();
        mat.setColor("Color", color);
    }

    private void createRoadVisual(Data startNode, Data endNode,ColorRGBA color,Data firstNode,Data lastNode) {
        Box box = new Box(0.5f, 0.05f, 0.5f);
        Geometry roadGeom = new Geometry("road", box);
        
        Material roadMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        roadMat.setBoolean("UseMaterialColors", true);
        roadMat.setColor("Ambient", color);
        roadMat.setColor("Diffuse", color);
        
        roadGeom.setMaterial(roadMat);
    
        // Position and scale
        Vector3f start = startNode.getPosition();
        Vector3f end = endNode.getPosition();
        Vector3f direction = end.subtract(start);
        float length = direction.length();
        direction.normalizeLocal();
        
        Vector3f center = start.add(direction.mult(length * 0.5f));
        roadGeom.setLocalScale(length, 1, 1);
        roadGeom.setLocalTranslation(center);
        
        // Rotation
        Quaternion rotation = new Quaternion();
        rotation.lookAt(direction, Vector3f.UNIT_Y);
   
        
        Quaternion additionalRotation = new Quaternion();
        additionalRotation.fromAngleAxis(FastMath.PI / 2, Vector3f.UNIT_Y);
        rotation.multLocal(additionalRotation);
        roadGeom.setLocalRotation(rotation);
        
        addRoadClickHandler(roadGeom,firstNode,lastNode);
        rootNode.attachChild(roadGeom);
    }

    private void updateBuildingPreviewColor(ColorRGBA color)
    {
        Material mat = ((Geometry)buildingPreview).getMaterial();
        mat.setColor("Color", color);
    }
    
    private Vector3f constrainToCircle(Vector3f center, Vector3f point, float radius) {
        Vector3f direction = point.subtract(center);
        float distance = direction.length();
        
        if (distance > radius) {
            direction.normalizeLocal().multLocal(radius);
            return center.add(direction);
        }
        return point;
    }

    private void updateRoadPreview(Vector3f start, Vector3f end) {
        Vector3f direction = end.subtract(start);
        float length = direction.length();
        direction.normalizeLocal();
        
        Vector3f center = start.add(direction.mult(length * 0.5f));
        roadPreview.setLocalScale(length, 1, 1);
        roadPreview.setLocalTranslation(center);
        
        // Calculate rotation to point along direction
        Quaternion rotation = new Quaternion();
        rotation.lookAt(direction, Vector3f.UNIT_Y);
        
        Quaternion additionalRotation = new Quaternion();
        additionalRotation.fromAngleAxis(FastMath.PI / 2, Vector3f.UNIT_Y);
        rotation.multLocal(additionalRotation);
        roadPreview.setLocalRotation(rotation);
    }
    
    
  private void addRoadClickHandler(Spatial roadModel, Data firstNode,Data lastNode) {	
    	// Add click detection
        inputManager.addMapping("Selected", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("Selected") && isPressed) {
                    // Cast ray from mouse click
                    Vector2f click2d = inputManager.getCursorPosition();
                    Vector3f click3d = cam.getWorldCoordinates(
                        new Vector2f(click2d.x, click2d.y), 0f);
                    Vector3f dir = cam.getWorldCoordinates(
                        new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
                    Ray ray = new Ray(click3d, dir);
                    
                    CollisionResults results = new CollisionResults();
                    roadModel.collideWith(ray, results);
                    
                    if (results.size() > 0) {
                        float dist = results.getClosestCollision().getDistance();
                        if (dist <= 100f) { 
                            if (removingRoad && !placingRoad && !removingBuilding && !placingBuilding)
                            {
                            	if(net.getConnectedNodes(firstNode).size()<=1)
                            	{
                            		net.nodes.remove(firstNode);
                            	}
                            	if(net.getConnectedNodes(lastNode).size()<=1)
                            	{
                            		net.nodes.remove(lastNode);
                            	}
                            	
                            	net.getConnectedNodes(firstNode).remove(lastNode);
                            	net.getConnectedNodes(lastNode).remove(firstNode);
                            	roadModel.removeFromParent();
                            } 
                        }
                    }
                }
            }
        }, "Selected");
    }

    
    private void placeBuilding(Vector3f clickPoint, String buildingType) {
        if (buildingType == null) return;
        
        // Create the building data
        Data b = gen.makeBuilding(buildingType);
        if(onNode!=null&&!onPoint.getNote("occupied",B))
        {
        	onPoint.setNote("occupied", true);
        	onPoint.setNote("building", b);
        	onNode.getNote("buildings", DL).add(b);
        	b.setNote("on",onNode);
        	b.addNote("onpoint",onPoint);
        	Vector3f pointPos = onPoint.getPosition();
        	b.setPosition(new Vector3f(pointPos.getX(),0.1f,pointPos.getZ()));
        }
        else if(b.getNote("level", I)>0) b.setPosition(clickPoint);
        else 
        {
        	System.out.println("[PB] Invalid position or node");
        	return;
        }
        	

        addModelToScene(b, buildingsNode, buildingType);
        sim.store("buildings", b);


        updateBuildingConnectionPoints(b);
        
        onBuildingPlaced();

    }
    
    private void onBuildingPlaced() {
        if (selectedBuildingButton != null) {
            toggleButtonSelection(selectedBuildingButton, false);
            selectedBuildingButton = null;
        }
        buildingPreview.removeFromParent();
        onNode=null;
        placingBuilding = false;
    }
    
    private static final ColorRGBA BTN_WORKING_ELSEWHERE = new ColorRGBA(0.7f, 0.3f, 0.3f, 0.9f); // Red for working elsewhere
    Label recipeName;
    Label recipeContents;
    
    private void showBuildingPanel(Data building) {
        if (buildingPanel != null) {
            buildingPanel.removeFromParent();
        }
        buildingPanel = new Container();
        float panelWidth = 400f;
        float panelHeight = 520f;
        buildingPanel.setPreferredSize(new Vector3f(panelWidth, panelHeight, 0));
        buildingPanel.setLocalTranslation(
            cam.getWidth() - panelWidth - 20,
            cam.getHeight() / 2 + panelHeight / 2,
            0
        );

        // Main panel background
        QuadBackgroundComponent panelBg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.95f));
        buildingPanel.setBackground(panelBg);
        buildingPanel.setLayout(new SpringGridLayout(Axis.Y, Axis.X,FillMode.Last,FillMode.Proportional));

        // Title bar container
        Container titleBar = new Container();
        titleBar.setPreferredSize(new Vector3f(panelWidth, 40, 0));
        QuadBackgroundComponent titleBg = new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 1f));
        titleBar.setBackground(titleBg);
        titleBar.setInsets(new Insets3f(5, 10, 5, 10));

        // Title text and close button in horizontal layout
        Container titleContent = new Container();
        titleContent.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        
        Label titleLabel = new Label(building.getNote("name", S));
        titleLabel.setFontSize(22f);
        titleLabel.setColor(ColorRGBA.White);
        titleLabel.setTextHAlignment(HAlignment.Left);
        

        Button closeBtn = new Button("X");
        closeBtn.setFontSize(18f);
        closeBtn.setPreferredSize(new Vector3f(5, 5, 0));
        closeBtn.setInsets(new Insets3f(5,10,5,10));
        closeBtn.setColor(ColorRGBA.White);
        closeBtn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.7f, 0.3f, 0.3f, 0.0f))); // Transparent background
        closeBtn.setTextHAlignment(HAlignment.Right);
        closeBtn.addClickCommands(source -> {
            buildingPanel.removeFromParent();
        });

        titleContent.addChild(titleLabel);
        titleContent.addChild(closeBtn);
        titleBar.addChild(titleContent);

        
        Container workerPanel = new Container();
        workerPanel.setLayout(new SpringGridLayout(Axis.X, Axis.Y,FillMode.Even,FillMode.Proportional));
        workerPanel.setInsets(new Insets3f(5, 2, 2, 5));
        
        Container workerList1 = new Container();
        workerList1.setLayout(new SpringGridLayout(Axis.Y, Axis.X,FillMode.None,FillMode.Proportional));
        workerList1.setInsets(new Insets3f(5, 2, 2, 5));
        
        Container workerList2 = new Container();
        workerList2.setLayout(new SpringGridLayout(Axis.Y, Axis.X,FillMode.None,FillMode.Proportional));
        workerList2.setInsets(new Insets3f(5, 2, 2, 5));
     
        Container workerList3 = new Container();
        workerList3.setLayout(new SpringGridLayout(Axis.Y, Axis.X,FillMode.None,FillMode.Proportional));
        workerList3.setInsets(new Insets3f(5, 2, 2, 5));
     
        
        int agentCounter = 0;

        // Create worker buttons with consistent styling
        for (Data agent : sim.agents) {
            if (!agent.hasNote("owns")) {
            	agentCounter++;
                Container workerBtn = new Container();
                workerBtn.setInsets(new Insets3f(2,0,0,2));
                workerBtn.setPreferredSize(new Vector3f(40f, 25, 0));

                // Set initial background based on work state
                QuadBackgroundComponent initialBg;
                if (agent.hasNote("works at")) {
                    Data workplace = agent.getNote("works at", D);
                    if (workplace != null) {
                        if (workplace.equals(building)) {
                            initialBg = new QuadBackgroundComponent(BTN_SELECTED);
                        } else if (!workplace.getNote("type", S).equals("road")) {
                            initialBg = new QuadBackgroundComponent(BTN_WORKING_ELSEWHERE);
                        } else {
                            initialBg = new QuadBackgroundComponent(BTN_NORMAL);
                        }
                    } else {
                        initialBg = new QuadBackgroundComponent(BTN_NORMAL);
                    }
                } else {
                    initialBg = new QuadBackgroundComponent(BTN_NORMAL);
                }
                workerBtn.setBackground(initialBg);

                Label nameLabel = new Label(agent.getNote("name", S));
                nameLabel.setColor(ColorRGBA.White);
                nameLabel.setFontSize(15f);
                nameLabel.setTextHAlignment(HAlignment.Left);
                nameLabel.setTextVAlignment(VAlignment.Center);
                
                workerBtn.addChild(nameLabel);

                workerBtn.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
                        if (event.isPressed()) {
                        	if(isShiftDown)
				             {
				                isoCam.setPosition(new Vector3f(agent.getPosition().x,isoCam.getPosition().y,agent.getPosition().z+(isoCam.getPosition().y)));
				             }
                        	else 
                        	{
                            // Count current workers at this building
                            int currentWorkers = 0;
                            for (Data otherAgent : sim.agents) {
                                if (otherAgent.hasNote("works at") && 
                                    otherAgent.getNote("works at", D) != null && 
                                    otherAgent.getNote("works at", D).equals(building)) {
                                    currentWorkers++;
                                }
                            }

                            // Get max workers allowed
                            int maxWorkers = building.getNote("maxworkers", I);

                            if (!agent.hasNote("works at") || agent.getNote("works at", D) == null) {
                                // Only allow assignment if under max workers
                                if (currentWorkers < maxWorkers) {
                                    agent.setNote("works at", building);
                                    workerBtn.setBackground(new QuadBackgroundComponent(BTN_SELECTED));
                                }
                            } else if (agent.getNote("works at", D).equals(building)) {
                                // Always allow removal
                                agent.setNote("works at", gen.makeBuilding("road"));
                                workerBtn.setBackground(new QuadBackgroundComponent(BTN_NORMAL));
                            } else {
                                // Only allow reassignment if under max workers
                                if (currentWorkers < maxWorkers) {
                                    agent.setNote("works at", building);
                                    workerBtn.setBackground(new QuadBackgroundComponent(BTN_SELECTED));
                                }
                            }
                        }
                    }
                    }
                    @Override 
                    public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
                        workerBtn.setBackground(new QuadBackgroundComponent(BTN_HOVER));
                    }
                    
                    @Override 
                    public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
                        // Just revert to the current work state
                        if (agent.hasNote("works at")) {
                            Data workplace = agent.getNote("works at", D);
                            if (workplace != null) {
                                if (workplace.equals(building)) {
                                    workerBtn.setBackground(new QuadBackgroundComponent(BTN_SELECTED));
                                } else if (!workplace.getNote("type", S).equals("road")) {
                                    workerBtn.setBackground(new QuadBackgroundComponent(BTN_WORKING_ELSEWHERE));
                                } else {
                                    workerBtn.setBackground(new QuadBackgroundComponent(BTN_NORMAL));
                                }
                            } else {
                                workerBtn.setBackground(new QuadBackgroundComponent(BTN_NORMAL));
                            }
                        } else {
                            workerBtn.setBackground(new QuadBackgroundComponent(BTN_NORMAL));
                        }
                    }
                    
                    @Override 
                    public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {}
                });

                if(agentCounter<=17)
                workerList1.addChild(workerBtn);
                else if(agentCounter>17&&agentCounter<=34)
                workerList2.addChild(workerBtn);
                else if(agentCounter>34&&agentCounter<=51)
                workerList3.addChild(workerBtn);
                else
                System.out.println("[SBP] Ran out of space to display workers");
            }
        }
        
        workerPanel.addChild(workerList1);
        workerPanel.addChild(workerList2);
        workerPanel.addChild(workerList3);
        
        Container recipes = new Container();
        recipes.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.Proportional));
        recipes.setInsets(new Insets3f(5, 2, 2, 5));
        
        // Create references to keep track of recipe info containers
        Container recipeInfo = new Container();
        
        Container recipeSelector = new Container();
        recipeSelector.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Proportional));
        recipeSelector.setInsets(new Insets3f(5, 2, 2, 5));
        recipeSelector.setPreferredSize(new Vector3f(100, 25, 0));
        recipeSelector.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.9f)));
        
        for(Data recipe : building.getNote("recipes", DL)) {
            if(sim.techs.containsAll(recipe.getNote("techreq", SL))) {
                Button recipeBtn = new Button(recipe.getNote("type", S));
                recipeBtn.setInsets(new Insets3f(2,2,2,2));
                recipeBtn.setPreferredSize(new Vector3f(panelWidth - 20, 25, 0));
                recipeBtn.setColor(ColorRGBA.White);
                recipeBtn.setFontSize(15f);
                recipeBtn.setTextHAlignment(HAlignment.Left);
                recipeBtn.setTextVAlignment(VAlignment.Center);
                recipeBtn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 0.9f)));
                addEffectsToButton(recipeBtn);
                
                recipeBtn.addClickCommands(e -> {
                    building.setNote("selectedrecipe", recipe);
                    
                    // Update recipe info
                    StringBuilder info = new StringBuilder();
                    info.append("-----------------------\n Produces \n-----------------------\n ");
                    for(Data product : recipe.getNote("products", DL))
                        info.append(product.getNote("type", S).substring(0, 1).toUpperCase() + 
                                  product.getNote("type", S).substring(1));
                    for(Integer productamt : recipe.getNote("productamt", IL))
                        info.append(": " + productamt + "\n ");
                    
                    info.append("-----------------------\n Uses \n-----------------------\n");
                    for(Data use : recipe.getNote("uses", DL))
                        info.append(use.getNote("type", S).substring(0, 1).toUpperCase() + 
                                use.getNote("type", S).substring(1));
                    for(Integer useamt : recipe.getNote("useamt", IL))
                        info.append(": " + useamt + "\n");
                    
                    // Update the labels
                    recipeName.setText(recipe.getNote("name", S));
                    recipeContents.setText(info.toString());
                    
                    // Force update
                    recipes.updateLogicalState(0);
                });
                if(!recipe.hasNote("obsolete"))
                {
                    recipeSelector.addChild(recipeBtn);
                }
                else if(!sim.techs.containsAll(recipe.getNote("obsolete", SL)))
                	recipeSelector.addChild(recipeBtn);
            }
        }
        
        // Initial recipe setup
        Data initialRecipe = building.getNote("selectedrecipe", D);
        
        recipeInfo = new Container();
        recipeInfo.setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.Last, FillMode.None));
        recipeInfo.setInsets(new Insets3f(5, 2, 2, 5));
        recipeInfo.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.1f, 1f)));
        recipeInfo.setPreferredSize(new Vector3f(200, 300, 0));
        
        recipeName = new Label(initialRecipe.getNote("name", S));
        recipeName.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 1f)));
        recipeName.setPreferredSize(new Vector3f(270f,30f,0f));
        recipeName.setInsets(new Insets3f(5, 10, 5, 10));
        
        StringBuilder initialInfo = new StringBuilder();
        initialInfo.append(" Produces \n-----------------------\n ");
        for(Data product : initialRecipe.getNote("products", DL))
            initialInfo.append(product.getNote("type", S).substring(0, 1).toUpperCase() + 
                             product.getNote("type", S).substring(1));
        for(Integer productamt : initialRecipe.getNote("productamt", IL))
            initialInfo.append(": " + productamt + "\n ");
        
        initialInfo.append("-----------------------\n Uses \n-----------------------\n");
        for(Data use : initialRecipe.getNote("uses", DL))
            initialInfo.append(use.getNote("type", S));
        for(Integer useamt : initialRecipe.getNote("useamt", IL))
            initialInfo.append(": " + useamt + "\n");
        
        recipeContents = new Label(initialInfo.toString());
        recipeContents.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.4f, 1f)));
        recipeContents.setPreferredSize(new Vector3f(270f,230,0f));
        recipeContents.setInsets(new Insets3f(5, 8, 8, 10));
        
        recipeInfo.addChild(recipeName);
        recipeInfo.addChild(recipeContents);
        
        recipes.addChild(recipeSelector);
        recipes.addChild(recipeInfo);

        StringBuilder invInfo = new StringBuilder();
        invInfo.append(" Inventory \n----------------------\n ");
        for(Entry<String, Integer> item : building.getNote("has", DH).entrySet())
        	invInfo.append(item.getKey()+": "+item.getValue()+"\n");
        
        
        Container inventory = new Container();
        inventory.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Even, FillMode.Proportional));
        inventory.setInsets(new Insets3f(2, 2, 2, 2));
        
        Container inventoryInfo = new Container();
        inventoryInfo.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.Proportional));
        inventoryInfo.setInsets(new Insets3f(2, 2, 2, 2));
        inventoryInfo.setPreferredSize(new Vector3f(100f, 25, 0));
        inventoryInfo.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.9f)));
        
        Label inInv = new Label(invInfo.toString());
        inInv.setFontSize(14f);
        inInv.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.4f, 1f)));
        inInv.setPreferredSize(new Vector3f(80f,230,0f));
        inInv.setInsets(new Insets3f(5, 2, 2, 5));
        
        inventoryInfo.addChild(inInv);
        
        StringBuilder billInfo = new StringBuilder();
        List<Data> bills = sim.findBuildingBills(building);
        if(!bills.isEmpty())
        {
        	for(Data bill : bills)
        	{
        		billInfo.append("Bill for "+bill.getNote("amount",I)+" "+bill.getNote("product",S)+
        		" from "+bill.getNote("source", D).getNote("name", S)+" to "
        		+bill.getNote("destination", D).getNote("name",S)+"\nStatus: "+bill.getNote("status", S)+
        		" Urgency: "+bill.getNote("urgency", F)+"\n");
        	}
        }
        		
        
        Container billsInfo = new Container();
        billsInfo.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.Proportional));
        billsInfo.setInsets(new Insets3f(5, 2, 2, 5));
        billsInfo.setPreferredSize(new Vector3f(200f, 25, 0));
        billsInfo.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.9f)));
        
        Label currentBills = new Label(billInfo.toString());
        currentBills.setFontSize(15f);
        currentBills.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.4f, 1f)));
        currentBills.setPreferredSize(new Vector3f(180f,230,0f));
        currentBills.setInsets(new Insets3f(5, 2, 2, 5));
        
        billsInfo.addChild(currentBills);
        
        inventory.addChild(inventoryInfo);
        inventory.addChild(billsInfo);
        
        TabbedPanel tabs = new TabbedPanel();
        tabs.setInsets(new Insets3f(2, 10, 2, 10));
        // Add tabs with their content
        tabs.addTab("Workers", workerPanel);
        tabs.addTab("Recipes", recipes);
        tabs.addTab("Inventory", inventory);
        
        for(Tab s : tabs.getTabs())
        {
        	Button tab = s.getTitleButton();
        	customizeTabButton(tab);
        }
        

        buildingPanel.addChild(titleBar);
        buildingPanel.addChild(tabs);
        guiNode.attachChild(buildingPanel);
        
        
    }
    
    private void showAgentPanel(Data agent)
    {
    	 if (agentPanel != null) {
    		 agentPanel.removeFromParent();
         }
    	 agentPanel = new Container();
          float agentPanelWidth = 300f;
          float agentPanelHeight = 600f;
          agentPanel.setPreferredSize(new Vector3f(agentPanelWidth, agentPanelHeight, 0));
          agentPanel.setLocalTranslation(20, cam.getHeight() - 50, 0);

          // Background for main panel
          QuadBackgroundComponent bg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.85f));
          agentPanel.setBackground(bg);


         // Main panel background
         QuadBackgroundComponent panelBg = new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.95f));
         agentPanel.setBackground(panelBg);
         agentPanel.setLayout(new SpringGridLayout(Axis.Y, Axis.X,FillMode.Last,FillMode.Proportional));

         // Title bar container
         Container titleBar = new Container();
         titleBar.setPreferredSize(new Vector3f(agentPanelWidth, 40, 0));
         QuadBackgroundComponent titleBg = new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 1f));
         titleBar.setBackground(titleBg);
         titleBar.setInsets(new Insets3f(5, 10, 5, 10));

         // Title text and close button in horizontal layout
         Container titleContent = new Container();
         titleContent.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
         
         Label titleLabel = new Label(agent.getNote("name", S));
         titleLabel.setFontSize(22f);
         titleLabel.setColor(ColorRGBA.White);
         titleLabel.setTextHAlignment(HAlignment.Left);
         
         Button closeBtn = new Button("X");
         closeBtn.setFontSize(18f);
         closeBtn.setPreferredSize(new Vector3f(5, 5, 0));
         closeBtn.setInsets(new Insets3f(5,10,5,10));
         closeBtn.setColor(ColorRGBA.White);
         closeBtn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.7f, 0.3f, 0.3f, 0.0f))); // Transparent background
         closeBtn.setTextHAlignment(HAlignment.Right);
         closeBtn.addClickCommands(source -> {
        	 agentPanel.removeFromParent();
         });

         titleContent.addChild(titleLabel);
         titleContent.addChild(closeBtn);
         titleBar.addChild(titleContent);
         
         
         Container agentInfo = new Container();
         agentInfo.setLayout(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Last, FillMode.None));
         agentInfo.setInsets(new Insets3f(5, 2, 2, 5));
         agentInfo.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.1f, 1f)));
         agentInfo.setPreferredSize(new Vector3f(200, 300, 0));
         
         StringBuilder invInfo = new StringBuilder();
         invInfo.append(" Inventory \n----------------------\n ");
         for(Entry<String, Integer> item : agent.getNote("has", DH).entrySet())
        	 invInfo.append(item.getKey()+": "+item.getValue()+"\n");
         
         Label inInv = new Label(invInfo.toString());
         inInv.setFontSize(14f);
         inInv.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.4f, 1f)));
         inInv.setPreferredSize(new Vector3f(80f,230,0f));
         inInv.setInsets(new Insets3f(5, 2, 2, 5));
         
         StringBuilder bI = new StringBuilder();
         bI.append("Works at: "); bI.append(agent.getNote("works at", D).getNote("name", S)+"\n");
         bI.append("Speed: "); bI.append(agent.getNote("speed", I)+"\n");
         bI.append("Speed Left: "); bI.append(agent.getNote("speed_left", I)+"\n");
         bI.append("Mood: "); bI.append(agent.getNote("mood", I)+"\n");
         bI.append("Hunger: "); bI.append(agent.getNote("hunger", I)+"\n");
         
    	 bI.append("\nPrefrences\n--------------\n");
         if(!agent.getNote("pref", SLH).isEmpty())
         {
	         for(Entry<Integer,List<String>> pref : agent.getNote("pref", SLH).entrySet())
	         {
	        	 bI.append(pref.getKey()); bI.append(pref.getValue().toString());
	        	 bI.append("\n");
	         }
         }
         else bI.append("No prefrences");
         
         Label basicInfo = new Label(bI.toString());
         basicInfo.setFontSize(14f);
         basicInfo.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.4f, 1f)));
         basicInfo.setPreferredSize(new Vector3f(80f,230,0f));
         basicInfo.setInsets(new Insets3f(5, 2, 2, 5));
         
         agentInfo.addChild(inInv);
         agentInfo.addChild(basicInfo);
         
         agentPanel.addChild(titleBar);
         agentPanel.addChild(agentInfo);
         guiNode.attachChild(agentPanel);
    }
    
    private void updateBuildingConnectionPoints(Data building) {
        Vector3f center = building.getPosition();
        Vector3f buildingBox = building.getNote("dimensions",V3);
        int width = (int) buildingBox.getX();
        int height =(int) buildingBox.getZ();
        
        // Calculate corner position for connection points
        float x = center.x;
        float z = center.z;  

        
        List<Vector3f> points = new ArrayList<>();
        points.add(new Vector3f(x + width/boundingCheck, 0, z));   // Right
        points.add(new Vector3f(x - width/boundingCheck, 0, z));   // Left
        points.add(new Vector3f(x, 0, z + height/boundingCheck));  // Back
        points.add(new Vector3f(x, 0, z - height/boundingCheck));  // Front
        
        building.addNote("right", points.get(0));
        building.addNote("left", points.get(1));
        building.addNote("back", points.get(2));
        building.addNote("front", points.get(3));

        buildingConnections.put(building, points);
    }
  //Adds basic click handeling [ABC]
    private void addBuildingClickHandler(Spatial buildingModel, Data building) {	
    	
    	addBoundingBox(building,buildingModel);
    	// Add click detection
        inputManager.addMapping("Selected", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("Selected") && isPressed) {
                    // Cast ray from mouse click
                    Vector2f click2d = inputManager.getCursorPosition();
                    Vector3f click3d = cam.getWorldCoordinates(
                        new Vector2f(click2d.x, click2d.y), 0f);
                    Vector3f dir = cam.getWorldCoordinates(
                        new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
                    Ray ray = new Ray(click3d, dir);
                    
                    // Check for collision with building's bounding box
                    CollisionResults results = new CollisionResults();
                    buildingModel.collideWith(ray, results);
                    
                    if (results.size() > 0) {
                        float dist = results.getClosestCollision().getDistance();
                        if (dist <= 100f) { // Adjust this value based on your scene scale
                            // Building was clicked
                            if (!removingRoad && !placingRoad && !removingBuilding && !placingBuilding) {
                            	showBuildingPanel(building);
                            }
                            else if(removingBuilding)
                            {
                            	removeBuilding(buildingModel,building);
                            }
                        }
                    }
                }
            }
        }, "Selected");
    }
    
    private void removeBuilding(Spatial buildingModel, Data building)
    {
    	Data on = building.getNote("on",D);
    	if(on!=null) 
    		{
        	Data onPoint = building.getNote("onpoint",D);
    		on.setNote("improved",false);
    		onPoint.setNote("occupied", false);
    		}
    	for(Data agent : sim.agents)
    	{
    		if(agent.getNote("works at",D).equals(building))
    		{
    			agent.setNote("works at",gen.makeBuilding("road"));
    		}
    	}
    	sim.buildings.remove(building);
    	buildingModel.removeFromParent();
    }
    
 private void addAgentClickHandler(Spatial agentModel, Data agent) {
	 	addBoundingBox(agent,agentModel);
    	 
      	// Add click detection
        inputManager.addMapping("Click", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("Click") && isPressed) {
                    // Cast ray from mouse click
                    Vector2f click2d = inputManager.getCursorPosition();
                    Vector3f click3d = cam.getWorldCoordinates(
                        new Vector2f(click2d.x, click2d.y), 0f);
                    Vector3f dir = cam.getWorldCoordinates(
                        new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
                    Ray ray = new Ray(click3d, dir);
                    
                    // Check for collision with the agent
                    CollisionResults results = new CollisionResults();
                    agentModel.collideWith(ray, results);
                    
                    if (results.size() > 0) {
                        float dist = results.getClosestCollision().getDistance();
                        if (dist <= 100f) { // Scene scale adjuster
                        	//For any future checks we might want!
                            if (true) {
                            	showAgentPanel(agent);
                            }
                        }
                    }
                }
            }
        }, "Click");
    }
    
    private void setupEnvironment() {
        // Create sky
        setupSkyBox();
        
        // Create terrain
        setupTerrain();
        
        // Adjust camera settings for better viewing
        cam.setLocation(new Vector3f(0, 10, 20));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(15f);
    }
    
    private void setupSkyBox() {
        Texture west = assetManager.loadTexture("Textures/Sky/west.png");
        Texture east = assetManager.loadTexture("Textures/Sky/east.png");
        Texture north = assetManager.loadTexture("Textures/Sky/north.png");
        Texture south = assetManager.loadTexture("Textures/Sky/south.png");
        Texture up = assetManager.loadTexture("Textures/Sky/up.png");
        Texture down = assetManager.loadTexture("Textures/Sky/down.png");
        
        rootNode.attachChild(SkyFactory.createSky(assetManager, 
            west, east, north, south, up, down));
    }
    
    
    private void setupAssetManager() {
        String userDir = System.getProperty("user.dir");
        File assetsDir = new File(userDir, "assets");
        
        if (!assetsDir.exists()) {
            assetsDir.mkdir();
            new File(assetsDir, "Models").mkdir();
            System.out.println("Created assets directory at: " + assetsDir.getAbsolutePath());
        }
        
        assetManager.registerLocator(assetsDir.getAbsolutePath(), FileLocator.class);
    }
    
    private void setupSceneNodes() {
        buildingsNode = new Node("Buildings");
        agentsNode = new Node("Agents");
        roadsNode = new Node("Roads");
        rNodeNode = new Node("rNode");
        
        rootNode.attachChild(buildingsNode);
        rootNode.attachChild(agentsNode);
        rootNode.attachChild(roadsNode);
        rootNode.attachChild(rNodeNode);
    }
    
    private void setupVisuals() {
        // 1. Enhance basic lighting
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(sun);

        // Add ambient light for softer shadows
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(ambient);

        // 2. Setup shadows
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 4096, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.4f);
        viewPort.addProcessor(dlsr);

        // 3. Setup post-processing
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);

        // SSAO for better depth perception
        SSAOFilter ssao = new SSAOFilter(0.5f, 3f, 0.2f, 0.3f);
        fpp.addFilter(ssao);

        // Bloom for glowing effects
//        BloomFilter bloom = new BloomFilter();
//        bloom.setBloomIntensity(2.0f);
//        bloom.setExposurePower(2.0f);
//        fpp.addFilter(bloom);

        // FXAA for smoother edges
        FXAAFilter fxaa = new FXAAFilter();
        fxaa.setSubPixelShift(1.0f/4.0f);  // Adjust for quality vs performance
        fxaa.setVxOffset(1.0f);  // Adjust edge detection sensitivity
        fxaa.setSpanMax(4f);  // Maximum pixel span
        fpp.addFilter(fxaa);

//        // Color correction/tone mapping
//        ToneMapFilter tone = new ToneMapFilter();
//        fpp.addFilter(tone);
    }
    
    private void addBoundingBox(Data data, Spatial model)
    {
    	Vector3f dimensions = null;
    	System.out.println("Finding object bounding box");
    	 if (model.getWorldBound() instanceof BoundingBox) {
    	        BoundingBox boundingBox = (BoundingBox) model.getWorldBound();
    	        float width = boundingBox.getXExtent() * 2;  // Full width
    	        float height = boundingBox.getYExtent() * 2; // Full height
    	        float depth = boundingBox.getZExtent() * 2;  // Full depth
    	        dimensions = new Vector3f(width, height, depth);
    	    } else {
    	        System.out.println("[ABC] Spatial does not have a BoundingBox.");
    	    }
    	 if(dimensions!=null)
    	 data.addNote("dimensions", dimensions);
    	 
    	 data.addNote("bBox",findTrueBoundingBox(data));
    }
    
    private Vector3f findBoundingBox(String object)
    {
    	Vector3f dimensions = null;
    	Spatial model = modelCache.get(object);
    	System.out.println("Finding object bounding box");
    	 if (model.getWorldBound() instanceof BoundingBox) {
    	        BoundingBox boundingBox = (BoundingBox) model.getWorldBound();
    	        float width = boundingBox.getXExtent() * 2;  // Full width
    	        float height = boundingBox.getYExtent() * 2; // Full height
    	        float depth = boundingBox.getZExtent() * 2;  // Full depth
    	        dimensions = new Vector3f(width, height, depth);
    	    } else {
    	        System.out.println("[ABC] Spatial does not have a BoundingBox.");
    	    }
    	 return dimensions;
    }
    
    private BoundingBox findTrueBoundingBox(Data object)
    {
    	Spatial model = object.getNote("spatial", N);
    	 if (model.getWorldBound() instanceof BoundingBox) {
             	//model.getLocalTransform().transformVector(null,null);
    	        BoundingBox boundingBox = (BoundingBox) model.getWorldBound();
    	        boundingBox.transform(model.getLocalTransform());
    	        return boundingBox;
    	    } else {
    	        System.out.println("[ABC] Spatial does not have a BoundingBox.");
    	        return null;
    	    }
    }
    
    private BoundingBox findTrueBoundingBox(String object)
    {
        Spatial model = modelCache.get(object);
    	 if (model.getWorldBound() instanceof BoundingBox) {
             	//model.getLocalTransform().transformVector(null,null);
    	        BoundingBox boundingBox = (BoundingBox) model.getWorldBound();
    	        boundingBox.transform(model.getLocalTransform());
    	        return boundingBox;
    	    } else {
    	        System.out.println("[ABC] Spatial does not have a BoundingBox.");
    	        return null;
    	    }
    }
    
    
    private BoundingSphere findBoundingSphere(String object)
    {
    	Spatial model = modelCache.get(object);
    	 if (model.getWorldBound() instanceof BoundingSphere) {
             	//model.getLocalTransform().transformVector(null,null);
    	        BoundingSphere boundingSphere = (BoundingSphere) model.getWorldBound();
    	        boundingSphere.transform(model.getLocalTransform());
    	        return boundingSphere;
    	    } else {
    	        System.out.println("[ABC] Spatial does not have a BoundingSphere.");
    	        return null;
    	    }
    }
    
    private void updateStorageVisual(Data entity) {
        // Iterate through all geometries in the model
    	ArrayList<Data> spots = entity.getNote("storagespots", DL);
    	HashMap<String,Integer> has = entity.getNote("has", DH);
    	for(Data spot : spots)
    	{
    		if (!spot.getNote("has",DH).isEmpty())
    		{
    			has = spot.getNote("has", DH);
    		}
    			
    		if(has.get(spot.getNote("type", S))!=null)
    		{
    			int amount = has.get(spot.getNote("type", S));
    			for(Node visual : spot.getNote("visuals",NL))
    			{
    			      String name = visual.getName();
    	              String[] parts = name.split("_");
    	              int num = Integer.parseInt(parts[0].split("#")[1]);
   
    	                if(num<=amount)
    	                {
    	                	visual.setCullHint(CullHint.Never);
    	                	List<Vector3f> points = getVisualPoints(visual,true);
    	                	if(!points.isEmpty())
    	                	{
    	                	collisionGrid.addSolid(points, entity);
    	                	}
    	                }
    	                else 
    	                {
    	                    visual.setCullHint(CullHint.Always);  
    	                    List<Vector3f> points = getVisualPoints(visual,false);
    	                	if(!points.isEmpty())
    	                	{
    	                	collisionGrid.removeSolid(points, entity);
    	                	}
    	                }
    			}
    		}
    	}     
    }
    
    private void UniversalupdateStorageVisual() {
   
    for(Data building : sim.buildings)
    {
    	if(building.hasNote("storagespots")&&building.hasNote("has"))
    	updateStorageVisual(building);
    }
    
    for(Data agent : sim.agents)
    {
    	if(agent.hasNote("storagespots")&&agent.hasNote("has"))
    	updateStorageVisual(agent);
    }
    
    for(Data rNode : sim.rNodes)
    {
    	if(rNode.hasNote("storagespots")&&rNode.hasNote("has"))
    	updateStorageVisual(rNode);
    }

    }
    
    private List<Vector3f> getVisualPoints(Node visual, boolean adding) {
        List<Vector3f> points = new ArrayList<>();
        
        // First get all mesh vertices
        List<Vector3f> meshPoints = new ArrayList<>();
        for (Spatial child : visual.getChildren()) {
            if (child instanceof Geometry) {
                Geometry geom = (Geometry)child;
                FloatBuffer positions = geom.getMesh().getFloatBuffer(VertexBuffer.Type.Position);
                positions.rewind();
                
                while (positions.hasRemaining()) {
                    Vector3f point = new Vector3f(
                        positions.get(),
                        positions.get(),
                        positions.get()
                    );
                    meshPoints.add(visual.localToWorld(point, null));
                }
            }
        }
        
        // Get bounds of the mesh
        Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        for (Vector3f point : meshPoints) {
            min.x = Math.min(min.x, point.x);
            min.z = Math.min(min.z, point.z);
            max.x = Math.max(max.x, point.x);
            max.z = Math.max(max.z, point.z);
        }
        
        // Fill points at 0.5f intervals within bounds
        for (float x = min.x; x <= max.x; x += 0.5f) {
            for (float z = min.z; z <= max.z; z += 0.5f) {
                Vector3f point = new Vector3f(x, 0, z);
                if(adding&&!collisionGrid.isSolid(point))
                points.add(point);
                else if(!adding)
                points.add(point);
            }
        }
        
        return points;
    }
   
    private void createAgents(int count) {
        for (int i = 0; i < count; i++) {
        	String type =  gen.agentTypes[r.nextInt(gen.mouseTypes.length)];
            Data agent = sim.makeAgent(type);
            String path = type;
            System.out.println("[CA] Path: "+path);
            addModelToScene(agent, agentsNode, path);
        }
    }
    
    private void createPred(Data nest) 
    {
        	String type =  gen.predTypes[r.nextInt(gen.predTypes.length)];
            Data agent = sim.makePred(type);
            nest.setNote("nestfor", agent);
            agent.setPosition(nest.getPosition());
            agent.addNote("nest", nest);
            String path = type;
            System.out.println("[CP] Path: "+path);
            addModelToScene(agent, agentsNode, path);
    }
    
    private void createBuildings(int count) {
        for (int i = 0; i < count; i++) {
        	Data building = gen.makeBuilding("cuttery");
            sim.store("buildings", building);
            addModelToScene(building, buildingsNode, building.getNote("type", S));
        }
    }
    private void createRnodes(int count) {
        // Track which types we've used to ensure at least one of each
        Set<String> usedTypes = new HashSet<>();
        List<Data> placedNodes = new ArrayList<>();
        
        // First, place one of each type
        for (String type : gen.rNodeTypes) {
            Data rNode = gen.rNode(type);
            if (placeNode(rNode, placedNodes, true)) {
                usedTypes.add(type);
                placedNodes.add(rNode);
                sim.store("rNodes", rNode);
                addModelToScene(rNode, buildingsNode, rNode.getNote("type", S));
                count--;
            }
        }
        
        // Then place the remaining nodes randomly
        for (int i = 0; i < count; i++) {
            int selector = r.nextInt(1,gen.rNodeTypes.length);
            Data rNode = gen.rNode(gen.rNodeTypes[selector]);
            
            if (placeNode(rNode, placedNodes, false)) {
                placedNodes.add(rNode);
                sim.store("rNodes", rNode);
                addModelToScene(rNode, buildingsNode, rNode.getNote("type", S));
            } else {
                // If we failed to place, try again
                i--;
            }
        }
    }

    private boolean placeNode(Data node, List<Data> existingNodes, boolean isRequired) {
       if(node.getNote("type", S).equals("tree"))
       {
    	   node.setPosition(new Vector3f(0f,0f,-20f));
    	   return true;
       }
       
    	boolean isNestable = node.getNote("nestable", B).equals(true);
    	// Maximum attempts to place a node
        int maxAttempts = isRequired ? 100 : 20;
        float minDistance = 20f; // Minimum distance between nodes
        float centerClearRadius = isNestable ? 80f : 10f; // Radius of clear zone in center
        float maxAttemptRadius = isNestable ? 120f : 80f; // Maximum radius from center
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate position in a ring, avoiding the center
            float angle = r.nextFloat() * 2f * FastMath.PI;
            float radius = centerClearRadius + (r.nextFloat() * (maxAttemptRadius - centerClearRadius));
            
            Vector3f position = new Vector3f(
                FastMath.cos(angle) * radius,
                0,
                FastMath.sin(angle) * radius
            );
            
            // Check if position is valid
            boolean valid = true;
            
            // Check distance from other nodes
            for (Data existing : existingNodes) {
                Vector3f existingPos = existing.getPosition();
                if (position.distance(existingPos) < minDistance) {
                    valid = false;
                    break;
                }
            }
            
            if(debugMode)
            {
                Cylinder cylinder = new Cylinder(3,64,120f, 4f, false);

                Material guideMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                guideMat.setColor("Color", new ColorRGBA(1, 0.5f, 0.5f, 0.5f));
                guideMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                guideMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
                Geometry indicator = new Geometry("indicator",cylinder);
                indicator.setMaterial(guideMat);
                indicator.rotate(new Quaternion(0f,1f,1f,0f));

                rootNode.attachChild(indicator);
            }
            
            if (valid) {
                node.setPosition(position);
                if(isNestable) createPred(node);
                return true;
            }
        }
        
        
        return false;
    }
    
    private void setupTerrain() {
        // Create a large flat plane for the ground
    	int size = 400;
        Quad ground = new Quad(size, size);
        Geometry groundGeom = new Geometry("Ground", ground);
        groundGeom.rotate(-FastMath.HALF_PI, 0, 0); // Rotate to be flat
        groundGeom.setLocalTranslation(-size/2, 0, size/2); // Center the ground
        
        // Create material for the ground
        Material groundMat = new Material(assetManager, 
            "Common/MatDefs/Light/Lighting.j3md");
        
        // Load and apply ground texture
        TextureKey key = new TextureKey("Textures/Terrain/grass.png");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        tex.setWrap(Texture.WrapMode.Repeat);
        
        groundMat.setTexture("DiffuseMap", tex);
        groundMat.setBoolean("UseMaterialColors", true);
        groundMat.setColor("Ambient", ColorRGBA.White.mult(0.5f));
        groundMat.setColor("Diffuse", ColorRGBA.White);
        
        groundGeom.setMaterial(groundMat);
        rootNode.attachChild(groundGeom);
    }
    
    private void addModelToScene(Data entity, Node parentNode, String modelType) {
        try {
            Spatial model = modelCache.get(modelType).clone();
            
            if (model != null) {
                // Set position
                Vector3f position = entity.getPosition();
                Quaternion rotation = entity.getRotation();
                //model.setLocalRotation(rotation);
                model.setLocalTranslation(position);
                
                model.setCullHint(CullHint.Never);
                
                // Store the spatial in the entity
                entity.addNote("spatial", model);
                
                if (entity.hasNote("type") && Arrays.asList(gen.buildingTypes).contains(entity.getNote("type", S))) 
                {
                    addBuildingClickHandler(model, entity);
                    addSpots(entity,model);
                    
                    if(debugVis)
                    {
                    Vector3f buildingBox = entity.getNote("dimensions",V3);
                    float x = buildingBox.getX()/boundingCheck;
                    float z = buildingBox.getZ()/boundingCheck;
                    
                    Box box = new Box(x,0.5f,z);
                    Geometry marker = new Geometry("bounds_marker", box);
                    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", ColorRGBA.Red);  // Make it visible for debugging
                    marker.setMaterial(mat);
                   // marker.setLocalTranslation(position);
                    
                    // Add marker to model
                    ((Node)model).attachChild(marker);
                    }
                    
                }
                else if(entity.hasNote("speed"))
                {
                	System.out.println(" [AMS] Loading an agent model "+entity.getNote("name",S));
                	addAgentClickHandler(model,entity);
                	addSpots(entity,model);
                }
                else if(entity.hasNote("type") && Arrays.asList(gen.rNodeTypes).contains(entity.getNote("type", S)))
                {
                	System.out.println(" [AMS] Loading a rNode model "+entity.getNote("type",S));
                    addSpots(entity,model);
                }
                
                // Add to scene
                parentNode.attachChild(model);
            }
        } 
        catch (Exception e) {
            System.err.println("Error adding model to scene: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addSpots(Data entity, Spatial clonedModel) {
        String type = entity.getNote("type",S);
        System.out.println("[AS] Type: "+entity.getNote("type", S));
        // Handle work spots
        List<Data> wSpots = workSpots.get(type);
        if(wSpots != null&&!wSpots.isEmpty()) 
        {
            for(Data spot : wSpots) 
            {
                Data newSpot = new Data();
                // Get the original position relative to model origin
                Vector3f originalPos = spot.getPosition();
                Vector3f newPos = new Vector3f();
                
                // Transform the position based on building's new location
                clonedModel.getLocalTransform().transformVector(originalPos, newPos);
                System.out.println("[AS] Original spot position: " + originalPos);
                System.out.println("[AS] Transformed spot position: " + newPos);
                newSpot.setPosition(newPos);
                newSpot.addNote("type", spot.getNote("type", S));
                newSpot.addNote("allowed", spot.getNote("allowed", SL));
                newSpot.addNote("occupied", false);
                
                entity.getNote("workspots", DL).add(newSpot);
            }
        }
        
        // For storage spots, we need to find the actual nodes in the cloned model
        List<Data> sSpots = storageSpots.get(type);
        if(sSpots != null) {
            for(Data spot : sSpots) {
                Data newSpot = new Data();
                newSpot.setPosition(spot.getPosition());
                newSpot.addNote("type", spot.getNote("type", S));
                newSpot.addNote("capacity", spot.getNote("capacity", I));
                newSpot.addNote("viscode", spot.getNote("viscode", S));
                
                // Find the actual nodes in the cloned model
                List<Node> visuals = new ArrayList<>();
                List<String> visualNames = spot.getNote("visualnames", SL);
                for(String name : visualNames) {
                    Node node = findNodeByName(clonedModel, name);
                    if(node != null) {
                        node.setCullHint(CullHint.Always); // Start hidden
                        visuals.add(node);
                    }
                }
                newSpot.addNote("visuals", visuals);
                Map<String,Integer> has = new HashMap<>();
                if(entity.hasNote("improved"))
                has.put(spot.getNote("type", S),spot.getNote("capacity", I));
                newSpot.addNote("has", has);
                entity.getNote("storagespots", DL).add(newSpot);
            }
        }
        List<Data> nSpots = snapSpots.get(type);
        if(nSpots != null) {
            for(Data spot : nSpots) {
            	Data newSpot = new Data();
                Vector3f originalPos = spot.getPosition();
                Vector3f newPos = new Vector3f();
               clonedModel.getLocalTransform().transformVector(originalPos, newPos);
                newSpot.setPosition(newPos);
                newSpot.addNote("nodetype", spot.getNote("nodetype", S));
                newSpot.addNote("viscode", spot.getNote("viscode", S));
                newSpot.addNote("occupied", false);
                newSpot.addNote("building",null);
                entity.getNote("snapspots", DL).add(newSpot);
            }
        }
        
    }

    private Node findNodeByName(Spatial model, String name) {
        if(model instanceof Node) {
            if(name.equals(model.getName())) {
                return (Node)model;
            }
            Node node = (Node)model;
            for(Spatial child : node.getChildren()) {
                Node result = findNodeByName(child, name);
                if(result != null) {
                    return result;
                }
            }
        }
        return null;
    }
   
    
    private void runTurn() {
        turn++;
//        turnDisplay.setText("Turn: " + turn);
//        apDisplay.setText("AP: " + ap);
        System.out.println("\n*----------["+turn+"]----------* ------------\n");
        runningTurn=true;
        // Initialize agents for new turn
        for(Data agent : sim.agents) {
        	if(agent.getNote("works at", D).getNote("type", S).equalsIgnoreCase("road")&&agent.getNote("atype", S).equals("civl"))
        	agent.setNote("speed", 24);
        		
            agent.setNote("speed_left", agent.getNote("speed", I));
            agent.setNote("processed_turn", false);
            System.out.println("[RT] "+agent.getNote("name",S)+" set speed_left to "+agent.getNote("speed", I));
        }
        
       //while(inactiveSlices <= 2)
        runSlice();
        

    }
    
    private void runSlice()
    {
       	aCounter=0;
       	activeAnimations.clear();
       	net.agentMovment.clear();
        if(inactiveSlices > sim.agents.size()) 
        {
        	//System.out.println("[RS] Returning: "+inactiveSlices);
        	runningTurn=false;
        	return;
        }
        
            System.out.println("**************"+(slice+1)+"**************");
            slice++;
            
            
            boolean anyAgentActive = false;  // Track if any agent did anything this slice              
            // Process each agent
            
            for(Data trash : sim.trashCan)
            {
            	sim.agents.remove(trash);
            }
            for(Data agent : sim.agents) 
            {
                int beforeSpeed = agent.getNote("speed_left", I);
                sim.resolveTurn(agent);
                
                switch (turnType) {
                    case "work":
                        sim.runWorkTurn(agent);
                        break;
                    case "market":
                        sim.runMarketTurn();
                        break;
                    case "trade":
                        sim.runTradeTurn();
                        break;
                }
                
            	if(debugMode)
            	{
            		showPaths(agent);
            	}
            	if(debugVis)
            	{
                    collisionGrid.updateDebugNode(gridDebugNode,assetManager);
            	}
                
                // If this agent did something, mark the slice as active
                if(agent.getNote("speed_left", I) != beforeSpeed) {
                    anyAgentActive = true;
                }
            }
            
            UniversalupdateStorageVisual();
            // Update inactive slice counter
            if(!anyAgentActive) {
                inactiveSlices++;
                //System.out.println("[RT] No actions this slice. Inactive slices: " + inactiveSlices);
            } else {
                inactiveSlices=0;  // Reset counter if any agent was active
                //System.out.println("[RT] Actions performed this slice. Reset innactive slices");
            }
            
            needsAnimation=net.agentMovment.size();
            if(!net.agentMovment.isEmpty())
            {
            animateAgentMovements();
            }
            else
            {
//              	System.out.println("	[RS] No movment to animate");
//            	System.out.println("	[RS] "+needsAnimation+" : "+aCounter);
            	runSlice();
            }
       
    }

    private void animateAgentMovements() {
        // Clear any existing animations
        activeAnimations.clear();
//        System.out.println("	[AAM] Starting animate agent movment "+net.agentMovment.size());      
        for (Map.Entry<Data, List<Vector3f>> entry : net.agentMovment.entrySet()) 
        {
            Data agent = entry.getKey();
            List<Vector3f> movementPoints = entry.getValue();
//            System.out.println("	[AAM] Looking at "+agent.getNote("name", S)+" movment: "+movementPoints.size());

            if (movementPoints.size() < 2) 
            {
//            	System.out.println("	[AAM] Attempted to move 1 spot");
//            	System.out.println("	[AAM] "+needsAnimation+" : "+aCounter);
            	//if(aCounter>needsAnimation)
            	runSlice();
            	continue;
            }

            // Get the spatial associated with this agent
            Spatial agentSpatial = agent.getNote("spatial", Spatial.class);
            if (agentSpatial == null) continue;

            // Create an animation control for this movement sequence
            AnimationControl control = new AnimationControl(
                agentSpatial, 
                movementPoints,
                0.1f  // Duration for each movement segment
            );
            
            activeAnimations.add(control);
        }
//        System.out.println("	[AAM] Running a new slice");
//    	runSlice();
      
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        
        boolean shouldRunNextSlice = false;
        // Update animations
        Iterator<AnimationControl> it = activeAnimations.iterator();
        while (it.hasNext()) {
            AnimationControl control = it.next();
            control.update(tpf);
            if (control.isComplete()) {
            	aCounter++;
//            	System.out.println("	[SU] Control Complete");
//            	System.out.println("	[SU] "+needsAnimation+" : "+aCounter);
                it.remove();
                if(needsAnimation<=aCounter)
                {
                	shouldRunNextSlice=true;
                }
            }
        }
        
        if(shouldRunNextSlice) {
            net.agentMovment.clear();
            runSlice();
        }
        //Check slice
        if(activeAnimations.isEmpty()) 
        {
            processingSlice = false;
        }
    }

    // Animation control class to handle movement
    private class AnimationControl { 
        private Spatial spatial;
        private List<Vector3f> points;
        private float segmentDuration;
        private int currentPoint = 0;
        private float currentTime = 0;
        private boolean isComplete = false;
        private boolean debugPrinted = false;

        public AnimationControl(Spatial spatial, List<Vector3f> points, float segmentDuration) {
            this.spatial = spatial;
            this.points = points;
            this.segmentDuration = segmentDuration;
        }

        public void update(float tpf) {
            if (isComplete) 
            	{
            	return;
            	}

            currentTime += tpf;
            if (!debugPrinted) {
                debugPrinted = true;
            }

            if (currentTime >= segmentDuration) {
                // Move to next segment
                currentTime = 0;
                currentPoint++;
                debugPrinted = false;
                
                
                if (currentPoint >= points.size() - 1) {
                    isComplete = true;
                    return;
                }
            }

            // Interpolate position
            Vector3f start = points.get(currentPoint);
            Vector3f end = points.get(currentPoint + 1);
            float progress = currentTime / segmentDuration;
            
            Vector3f current = new Vector3f(
                start.x + (end.x - start.x) * progress,
                start.y + (end.y - start.y) * progress,
                start.z + (end.z - start.z) * progress
            );
            
            spatial.setLocalTranslation(current);
        }

        public boolean isComplete() {
            return isComplete;
        }
    }

//Additional debug classes
 Map<Data,Map<Data,Spatial>> debugPaths = new HashMap<>();
    
    private void showPaths(Data agent) {
        debugPaths.putIfAbsent(agent, new HashMap<>());
        List<Data> currentPath = agent.getNote("remainingpath", PL);
        
        // First, create markers for any new path points
        if(currentPath != null && !currentPath.isEmpty()) {
            for(Data point : currentPath) {
                // Only create new markers for points we haven't marked yet
                if(!debugPaths.get(agent).containsKey(point)) {
                    Box box = new Box(0.2f, 0.4f, 0.2f);
                    Geometry marker = new Geometry("bounds_marker", box);
                    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", ColorRGBA.Yellow);
                    marker.setMaterial(mat);
                    marker.setLocalTranslation(point.getPosition());

                    rootNode.attachChild(marker);
                    debugPaths.get(agent).put(point, marker);
                }
            }
        }

        // Then remove markers for points no longer in the path
        Iterator<Entry<Data, Spatial>> iterator = debugPaths.get(agent).entrySet().iterator();
        while(iterator.hasNext()) {
            Entry<Data, Spatial> entry = iterator.next();
            if(currentPath == null || !currentPath.contains(entry.getKey())) {
                entry.getValue().removeFromParent();  // Remove from scene
                iterator.remove();  // Remove from our tracking map
            }
        }
    }
    
    
}


