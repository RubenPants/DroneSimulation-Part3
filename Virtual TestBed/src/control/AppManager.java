package control;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import entities.Airport;
import entities.Camera;
import entities.Drone;
import entities.Entity;
import entities.Model;
import entities.TexturedModel;
import guis.GuiRenderer;
import guis.GuiTexture;
import interfaces.AutopilotConfig;
import interfaces.AutopilotModule;
import objConverter.OBJFileLoader;
import rendering.CameraView;
import rendering.Loader;
import rendering.MasterRenderer;
import rendering.TextureFrameBuffer;
import shaders.ModelTexture;
import swing_components.AccuracyFrame;
import swing_components.CloseListener;
import swing_components.MainFrame;
import swing_components.SettingsEvent;
import swing_components.SettingsListener;
import swing_components.SimulationListener;
import swing_components.TesterFrame;
import terrain.ProceduralTerrainLoader;
import terrain.Terrain;
import terrain.TerrainTexture;
import terrain.TerrainTexturePack;
import worldSimulation.DroneStartSettings;

public class AppManager {
	
	public static final int START_DISPLAY_WIDTH = 800;
	public static final int START_DISPLAY_HEIGHT = 800;
	private static int FPS_CAP = 50;
	private static int autopilotCallsPerSecond = 50;
	private static int iterationsPerFrame = 10;
	private static final int INFO_REFRESH_RATE = 10;
	
	//private static final int DRONE_AMOUNT = 1;
	private static final int AIRPORT_LENGTH = 70;
	private static final int AIRPORT_WIDTH = 250;
	
	private static float lastFrameTime;
	private static long thisFrameTime;
	private static long lastUpdateTime;
	private static float delta;
	
	private static MasterRenderer masterRenderer;
	private static GuiRenderer guiRenderer;
	private static Loader loader;
	
	private static WorldManager worldManager;
	
	private static DroneStartSettings startSettings = new DroneStartSettings();
	private static TexturedModel texModel;
	private static TexturedModel shadowTexModel;
	
	private static int focussedDrone = 0;
	private static ProceduralTerrainLoader terrainLoader;
	private static float cameraDistance = 50f;
	private static float timeRelativeToReal = 1f;
	
	private static TextureFrameBuffer textureTopBuffer;
	private static TextureFrameBuffer textureSideBuffer;
	private static List<GuiTexture> guis = new ArrayList<GuiTexture>();	
	private static ByteBuffer imageBuffer = ByteBuffer.allocateDirect(START_DISPLAY_WIDTH*START_DISPLAY_HEIGHT*3);
	private static byte[] image;
	private static byte[] imageCopy;
	private static boolean imageCopyRequested = true;
	private static Camera customCamera;
	
	private static MainFrame mainFrame;
	private static JFileChooser fileChooser;
	
	private static TesterFrame testerFrame;
	private static boolean isTesting = false;
	private static int testAmount;
	private static int testsSoFar = 0;
	private static float timeToBeat;
	
	private static float deltaLastInfoUpdate = 0; 
	private static boolean shouldClose = false;
	private static CameraView cameraView = CameraView.Custom;
	private static SimulationStatus simStatus = SimulationStatus.Idle;
	
	private static float simulationTime = 0;
	
	private static AutopilotModule module;
	private static AutopilotConfig configs;
	
	/**
	 * Starts the app by creating and showing the configurations frame. The mainframe is made as well but is still invisible.
	 */
	public static void createApp() {
		//Initialiseer het swing main en tester paneel.
		createMainFrame(START_DISPLAY_WIDTH, START_DISPLAY_HEIGHT);
		createTesterFrame();
		
		//Initialiseer de loader en renderers en de buffers om naar te renderen
		loader = new Loader();
		masterRenderer = new MasterRenderer(loader);
		guiRenderer = new GuiRenderer(loader);
		
		textureTopBuffer = new TextureFrameBuffer(2048,2048);
		textureSideBuffer = new TextureFrameBuffer(2048,2048);

		//Initialiseer de filechooser om paden te lezen
		fileChooser = new JFileChooser();
		
		//Splits het scherm in 2 delen voor de orthogonale projecties
		GuiTexture guiTextureTop = new GuiTexture(textureTopBuffer.getTexture(), new Vector2f(0f,0.5f), new Vector2f(1f,-.5f));
		GuiTexture guiTextureSide = new GuiTexture(textureSideBuffer.getTexture(), new Vector2f(0f,-0.5f), new Vector2f(1f,-.5f));
		GuiTexture guiSplitter = new GuiTexture(loader.loadTexture("splitter"), new Vector2f(0,0), new Vector2f(1f,0.002f));
		
		guis.add(guiTextureTop);
		guis.add(guiTextureSide);
		guis.add(guiSplitter);
		
		//*****************TERRAIN TEXTURES**********************
		
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("ground3"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("ground4"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("ground1"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("ground2"));
				
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture,rTexture,gTexture,bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap2"));
		
		Model terrainModel = Terrain.generateTerrain(loader);

		
		//*******************DRONE EN AIRPORT MODELS*************************
		Model planeModel = OBJFileLoader.loadOBJ("plane2", loader);
		Model shadowModel = loader.loadTexturedQuad();
		ModelTexture planeTexture = new ModelTexture(loader.loadTexture("plane2"));
		planeTexture.setReflectivity(1);
		planeTexture.setShineDamper(6);
		ModelTexture shadowTexture = new ModelTexture(loader.loadTexture("shadowTexture"));
		shadowTexture.setHasTransparency(true);
		
		texModel = new TexturedModel(planeModel.getVaoID(),planeModel.getVertexCount(),planeTexture);
		shadowTexModel = new TexturedModel(shadowModel.getVaoID(), shadowModel.getVertexCount(), shadowTexture);
		Drone.addModels(texModel, shadowTexModel);
		
		ModelTexture tarmac2 = new ModelTexture(loader.loadTexture("tarmac2"));
		tarmac2.setReflectivity(0.3f);
		tarmac2.setShineDamper(2);
		tarmac2.setDepthMask(false);
		
		Model airportModel = OBJFileLoader.loadOBJ("finaln", loader);
		ModelTexture airportTexture = new ModelTexture(loader.loadTexture("betterbegood"));
		airportTexture.setReflectivity(0.1f);
		airportTexture.setShineDamper(2);
		Airport.setModel(airportModel, airportTexture);
		//Airport.setModel(loader);
		
		//******************WERELD*******************************
		worldManager = new WorldManager(new CrashHandler() {
			public void handleCrash(String message) {
				simStatus = SimulationStatus.ResetRequested;
				showErrorMessage(message);
			}
		});
		//TODO
		Airport.defineAirportParameters(AIRPORT_LENGTH, AIRPORT_WIDTH);
		Airport airport1 = new Airport(new Vector2f(0,0), 0);
		airport1.setId(0);
		Airport airport2 = new Airport(new Vector2f(-4000,0), (float) (Math.PI/6));
		airport2.setId(1);
		Airport airport3 = new Airport(new Vector2f(0,-4000), (float) (Math.PI/5));
		airport3.setId(2);
		Airport airport4 = new Airport(new Vector2f(-3000,3000), (float) (Math.PI*3/4));
		airport4.setId(3);
		Airport airport5 = new Airport(new Vector2f(4000,0), (float) (Math.PI*8/3));
		airport5.setId(4);
		Airport airport6 = new Airport(new Vector2f(-3000,-3000), (float) (Math.PI/6));
		airport6.setId(5);
		Airport airport7 = new Airport(new Vector2f(0,4000), (float) (Math.PI/2));
		airport7.setId(6);
		Airport airport8 = new Airport(new Vector2f(3000,-3000), (float) (Math.PI/3));
		airport8.setId(7);
		Airport airport9 = new Airport(new Vector2f(3000,3000), (float) (Math.PI/9));
		airport9.setId(8);
		worldManager.addAirport(airport1);
		worldManager.addAirport(airport2);
		worldManager.addAirport(airport3);
		worldManager.addAirport(airport4);
		worldManager.addAirport(airport5);
		worldManager.addAirport(airport6);
		worldManager.addAirport(airport7);
		worldManager.addAirport(airport8);
		worldManager.addAirport(airport9);
		
		configs = mainFrame.getConfigs();
		Drone drone1 = new Drone(configs);
		worldManager.addDroneToAirport(drone1, airport2, 0);
		worldManager.addDrone(drone1);
		customCamera = new Camera(drone1.getCameraPosition(), 0, (float)-Math.PI/6, 0);
		
		Drone drone2 = new Drone(configs);
		worldManager.addDrone(drone2);
		worldManager.addDroneToAirport(drone2, airport1, 0);
		
		Drone drone2_5 = new Drone(configs);
		worldManager.addDrone(drone2_5);
		worldManager.addDroneToAirport(drone2_5, airport1, 1);
		
		Drone drone3 = new Drone(configs);
		worldManager.addDrone(drone3);
		worldManager.addDroneToAirport(drone3, airport3, 0);
		
		Drone drone4 = new Drone(configs);
		worldManager.addDrone(drone4);
		worldManager.addDroneToAirport(drone4, airport3, 1);
		
		Drone drone5 = new Drone(configs);
		worldManager.addDrone(drone5);
		worldManager.addDroneToAirport(drone5, airport4, 0);
		
		Drone drone6 = new Drone(configs);
		worldManager.addDrone(drone6);
		worldManager.addDroneToAirport(drone6, airport4, 1);
		
		Drone drone7 = new Drone(configs);
		worldManager.addDrone(drone7);
		worldManager.addDroneToAirport(drone7, airport5, 0);
		
		Drone drone8 = new Drone(configs);
		worldManager.addDrone(drone8);
		worldManager.addDroneToAirport(drone8, airport5, 1);
		
		Drone drone9 = new Drone(configs);
		worldManager.addDrone(drone9);
		worldManager.addDroneToAirport(drone9, airport6, 0);
		
		Drone drone10 = new Drone(configs);
		worldManager.addDrone(drone10);
		worldManager.addDroneToAirport(drone10, airport6, 1);
		
		Drone drone11 = new Drone(configs);
		worldManager.addDrone(drone11);
		worldManager.addDroneToAirport(drone11, airport7, 0);
		
		Drone drone12 = new Drone(configs);
		worldManager.addDrone(drone12);
		worldManager.addDroneToAirport(drone12, airport7, 1);
		
		Drone drone13 = new Drone(configs);
		worldManager.addDrone(drone13);
		worldManager.addDroneToAirport(drone13, airport8, 0);
		
		Drone drone14 = new Drone(configs);
		worldManager.addDrone(drone14);
		worldManager.addDroneToAirport(drone14, airport8, 1);
		
		Drone drone15 = new Drone(configs);
		worldManager.addDrone(drone15);
		worldManager.addDroneToAirport(drone15, airport9, 0);
		
		Drone drone16 = new Drone(configs);
		worldManager.addDrone(drone16);
		worldManager.addDroneToAirport(drone16, airport9, 1);
		
		terrainLoader = new ProceduralTerrainLoader(drone1, terrainModel, 10, texturePack, blendMap);
		
		mainFrame.setWorldManager(worldManager);
		mainFrame.fireData(worldManager);
		
		//Initialiseer de frame time en maak mainFrame zichtbaar
		lastFrameTime = getCurrentTime();
		lastUpdateTime = getCurrentTime();
		mainFrame.setVisible(true);
	}
	
	/**
	 * Update the whole app. This includes rendering the image, then if the simulation isn't paused updating the drone.
	 * Then the display is updated so the drone can be evolved over the amount of time that it took to do the last frame.
	 */
	public static void updateApp() {
		long timeMilli = getCurrentTime();
		long time = System.nanoTime();
		//Haal de simulation status op. (Wordt ook aangepast door swing thread)
		SimulationStatus status = simStatus;
		
		/*
		 * Kijk naar de simulation status:
		 * ResetRequested: Herstart de drone en verwijder het pad
		 * RestartRequested: Herstart enkel de drone en het pad
		 * PathUpdateRequested: Herstart de drone en zet het nieuwe pad
		 */
		switch (status) {
		case ResetRequested:
			reset();
			break;
		case RestartRequested:
			reset();
			break;
		case ConfigRequested:
			configAutopilotModule();
			break;
		case Started:
			try {
				if(worldManager.checkDronesForPackagePickup()) mainFrame.refreshPackTable();
				worldManager.allDronesTimePassed((float) 1.0/autopilotCallsPerSecond, simulationTime, iterationsPerFrame);
			} catch (Exception e) {
				showErrorMessage(e.getMessage());
				e.printStackTrace();
			}
			simulationTime += (float) 1.0/autopilotCallsPerSecond;
			//checkTargetReached();
			break;
		default:
			break;
		}

		terrainLoader.updateTerrain();
		
		//Update het scherm (default FrameBuffer)

		updateDisplay();
		
		worldManager.waitForThreads();
		
		refreshInfo(focussedDrone);
		//Synchroniseer de update van de app naar het aantal autopilot calls.
		int loopsPerSecond = (int)(autopilotCallsPerSecond*timeRelativeToReal);
		Display.sync(loopsPerSecond);
		//System.out.println("TOTAL UPDATE TIME TESTBED:" + getCurrentTime() + "-" + timeMilli + "=" + (getCurrentTime()-timeMilli));
	}
	
	/**
	 * Create the mainframe, including all the openGL stuff. (Renderer en loader initialised as well)
	 */
	private static void createMainFrame(int width, int height) {
		mainFrame = new MainFrame(width, height);
		mainFrame.setCloseListener(new CloseListener() {
			public void requestClose() {
				shouldClose = true;
				mainFrame.dispose();
			}
		});
		mainFrame.setSettingsListener(new SettingsListener() {
			public void changeRenderSettings(SettingsEvent e) {
				masterRenderer.changeSettings(e.getFov(), e.getRed(), e.getGreen(), e.getBlue());
			}
			@Override
			public void setTime(float time) {
				timeRelativeToReal = time;
			}
			@Override
			public void setDroneForCamera(int id) {
				focussedDrone = id;
			}
		});
		mainFrame.setSimulationListener(new SimulationListener() {
			public void swapView(CameraView view) {
				cameraView = view;
			}
			public void swapSimulationStatus(SimulationStatus status) {
				simStatus = status;
				imageCopyRequested = true;
			}
			@Override
			public void testingRequested() {
				testerFrame.setVisible(true);
			}
		});

		mainFrame.addExportListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION){
					File selectedFile = fileChooser.getSelectedFile();
					if(((JMenuItem)e.getSource()).getText() == "Export Drone View"){
						selectedFile = new File(selectedFile + ".bmp");
						saveImage(selectedFile);
					}
				}
			}
		});
		mainFrame.addAccuracyListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    EventQueue.invokeLater(new Runnable() {
			        public void run() {
			            AccuracyFrame frame = new AccuracyFrame(autopilotCallsPerSecond, iterationsPerFrame);
			            frame.addButtonListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								int[] settings = frame.getSettings();
								AppManager.autopilotCallsPerSecond = settings[0];
								AppManager.iterationsPerFrame = settings[1];
								frame.dispose();
							}
						});
			        }
			    });
			}
		});
	}
	
	private static void createTesterFrame(){
		testerFrame = new TesterFrame(100, 8, 1200);
		testerFrame.addStartButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				simStatus = SimulationStatus.ResetRequested;
				timeRelativeToReal = testerFrame.getSpeed();
				isTesting = true;
				testAmount = testerFrame.getAmountOfTests();
				timeToBeat = testerFrame.getTimeToBeat();
				testsSoFar = 0;
				testerFrame.lockTesting(true);
				mainFrame.lockSimulation();
			}
		});
		testerFrame.addStopButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				simStatus = SimulationStatus.ResetRequested;
				timeRelativeToReal = 1;
				isTesting = false;
				testerFrame.reset();
				mainFrame.resetRunMenu();
				testerFrame.lockTesting(false);
				timeRelativeToReal = 1;
				testerFrame.setVisible(false);
			}
		});
		testerFrame.addPauseButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(simStatus == SimulationStatus.Paused) {
					simStatus = SimulationStatus.Started;
					testerFrame.paused(false);
				} else {
					simStatus = SimulationStatus.Paused;
					testerFrame.paused(true);
				}
			}
		});
		testerFrame.addClearButtonListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				simStatus = SimulationStatus.ResetRequested;
				timeRelativeToReal = 1;
				isTesting = false;
				testerFrame.reset();
				testerFrame.lockTesting(false);
				timeRelativeToReal = 1;
			}
		});
	}
	
	/**
	 * Update the virtual drone. (Ask the autopilot for outputs and change the settings of the plane.)
	 * If the drone is not yet configured for the autopilot, then it gets configured. 
	 */
	private static void configAutopilotModule(){
		module.defineAirportParams(AIRPORT_LENGTH, AIRPORT_WIDTH);
		for(Airport port: worldManager.getAirports()){
			module.defineAirport(port.getPosition2D().x, port.getPosition2D().y, port.getCenterRunway0Relative().x, port.getCenterRunway0Relative().y);
		}
		worldManager.defineDrones();
		simStatus = SimulationStatus.Started;
	}
	
	/**
	 * Update the display. The display is synchronised to FPS_CAP frames per second (if it can handle it). 
	 * The amount of time between the last frame and the current frame is saved in delta. 
	 */
	private static void updateDisplay() {
		long currentUpdateTime = getCurrentTime();
		delta = (currentUpdateTime - lastUpdateTime)/1000f;
		lastUpdateTime = currentUpdateTime;
		thisFrameTime += delta*1000;
		if(thisFrameTime >= 1000/FPS_CAP) {
			//System.out.println("RENDERING");
			renderTestbedView();
			lastFrameTime = thisFrameTime/1000f;
			thisFrameTime = 0;
			long time = getCurrentTime();
			Display.update();
			//System.out.println("UPDATE SCREEN TIME:" + AppManager.getCurrentTime() + "-" + time + "=" + (AppManager.getCurrentTime()-time));
		}
	}
	
	/**
	 * Get the last frame's time.
	 */
	public static float getUpdateTimeSeconds() {
		return delta;
	}
	
	public static float getFrameTimeSeconds(){
		return lastFrameTime;
	}
	/**
	 * Close the app. Cleans up all the data stored in the loader, renderer and buffers. Destroys the display.
	 */
	public static void closeApp() {
		loader.cleanUp();
		masterRenderer.cleanUp();
		guiRenderer.cleanUp();
		textureTopBuffer.cleanUp();
		textureSideBuffer.cleanUp();
		Display.destroy();
	}
	
	public static boolean closeRequested(){
		return shouldClose;
	}
	
	//Current time in milliseconds
	public static long getCurrentTime() {
		return Sys.getTime()*1000/Sys.getTimerResolution();
	}

//	private static void renderForAutopilot(){
//		buffer.bindFrameBuffer();
//		planeRenderer.renderCubes(world, camera, 1, true);
//		terrainRenderer.render(terrainLoader.getTerrains(), camera);
//		imageBuffer.clear();
//		GL11.glReadPixels(0, 0, RESOLUTION_WIDTH, RESOLUTION_HEIGHT, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, imageBuffer);
//		byte[] image_flipped = new byte[RESOLUTION_WIDTH*RESOLUTION_HEIGHT*3];
//		imageBuffer.get(image_flipped);
//		image = new byte[RESOLUTION_WIDTH*RESOLUTION_HEIGHT*3];
//		
//		for(int x = 0; x<RESOLUTION_WIDTH;x++){
//			for(int y = 0; y<RESOLUTION_HEIGHT;y++){
//				image[(x+RESOLUTION_WIDTH*y)*3] = image_flipped[(x+(RESOLUTION_HEIGHT-1-y)*RESOLUTION_WIDTH)*3];
//				image[(x+RESOLUTION_WIDTH*y)*3 + 1] = image_flipped[(x+(RESOLUTION_HEIGHT-1-y)*RESOLUTION_WIDTH)*3 + 1];
//				image[(x+RESOLUTION_WIDTH*y)*3 + 2] = image_flipped[(x+(RESOLUTION_HEIGHT-1-y)*RESOLUTION_WIDTH)*3 + 2];
//			}	
//		}
//		if(imageCopyRequested) {
//			imageCopy = image.clone();
//			imageCopyRequested = false;
//		}
//		
//		buffer.unbindCurrentFrameBuffer();
//	}
	
	private static void renderTestbedView() {
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        Drone drone = worldManager.getDrone(focussedDrone);
		switch(cameraView) {
		case DroneView:
//			buffer.bindFrameBuffer();
//			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
//			GL30.glBlitFramebuffer(
//				0, 0, RESOLUTION_WIDTH, RESOLUTION_HEIGHT,
//				0, 0, Display.getWidth(), Display.getHeight(),
//				GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
//			buffer.unbindCurrentFrameBuffer();
			Camera camera = new Camera(drone);
			masterRenderer.createPerspectiveProjectionMatrix();
			masterRenderer.renderScene(getAllEntities(false), terrainLoader.getTerrains(), camera);
			break;
		case OrthogonalViews:
			textureTopBuffer.bindFrameBuffer();
			Camera orthoTopCamera = new Camera(new Vector3f(drone.getPosition().x,drone.getPosition().y+100,drone.getPosition().z), (float)Math.PI/2,-(float)Math.PI/2,0);
			masterRenderer.createOrthogonalProjectionMatrix(400,200);

			masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), orthoTopCamera);	
			textureTopBuffer.unbindCurrentFrameBuffer();
			
			textureSideBuffer.bindFrameBuffer();
			Camera orthoSideCamera = new Camera(new Vector3f(drone.getPosition().x + 100,drone.getPosition().y,drone.getPosition().z), (float)Math.PI/2,0,0);
			masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), orthoSideCamera);	
			textureSideBuffer.unbindCurrentFrameBuffer();

			guiRenderer.render(guis);
			
			break;
		case ThirdPerson:
			Camera thirdPersonCamera = new Camera(
					Vector3f.add(drone.getPosition(),
					new Vector3f((float) (cameraDistance*Math.sin(drone.getHeading())),
							0,(float) (cameraDistance*Math.cos(drone.getHeading()))),null),
					drone.getHeading(), 0, 0);
			masterRenderer.createPerspectiveProjectionMatrix();

			masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), thirdPersonCamera);
			break;
		case Custom:
			customCamera.move();
			customCamera.moveAroundDrone(drone);
			masterRenderer.createPerspectiveProjectionMatrix();
			masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), customCamera);
			break;
		}
	}
	
	private static void showMessage(String message) {
		System.out.println();
		System.out.println("GAME OVER!");
	    EventQueue.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	            int i = JOptionPane.showConfirmDialog(null, message + " Press 'Yes' to quit the app, or press 'No' to restart.");
	            if(i == JOptionPane.OK_OPTION) {
		            shouldClose = true;
		            mainFrame.dispose();
	            }
	            if (i == JOptionPane.NO_OPTION) {
	            	simStatus = SimulationStatus.ResetRequested;
	            }
	        }
	    });
	}
	
	private static void showErrorMessage(String message) {
    	simStatus = SimulationStatus.ResetRequested;
		System.out.println();
		System.out.println("GAME OVER!");
	    EventQueue.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	        	JOptionPane.showMessageDialog(mainFrame,
	        		    message,
	        		    "ERROR",
	        		    JOptionPane.ERROR_MESSAGE);
	        }
	    });
	}

	private static void refreshInfo(int foccusedDrone) {
		deltaLastInfoUpdate += getUpdateTimeSeconds();
		Drone drone = worldManager.getDrone(focussedDrone);
		if(deltaLastInfoUpdate >= 1.0/INFO_REFRESH_RATE) {
			mainFrame.updateOrientationLabels(drone.getHeading(), drone.getPitch(), drone.getRoll());
			mainFrame.updateVelocityLabels(drone.getPosition(), drone.getVelocity(), drone.getRelativeAngularVelocity());
			mainFrame.updateTime(getFrameTimeSeconds(), simulationTime);
			deltaLastInfoUpdate = 0;
		}
	}
	
	private static void reset(){
		worldManager.reset(startSettings);
		simulationTime = 0;
		simStatus = SimulationStatus.Idle;
		Drone drone = worldManager.getDrone(0);
		mainFrame.updateOrientationLabels(drone.getHeading(), drone.getPitch(), drone.getRoll());
		mainFrame.updateVelocityLabels(drone.getPosition(), drone.getVelocity(), drone.getRelativeAngularVelocity());
		if(isTesting){
			testsSoFar++;
			if(testsSoFar > testAmount){
				testsSoFar = 0;
				isTesting = false;
			} else {
				simStatus = SimulationStatus.ConfigRequested;
			}
		} else {
			mainFrame.resetRunMenu();
		}
	}
	
	public static void saveImage(File file) {
		int width = 200, height = 200;
		
		// Convert to image
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		for (int i=0 ; i<height ; i++) {
			for (int j=0 ; j<width ; j++) {
				byte r = imageCopy[(i * width + j) * 3 + 0];
				byte g = imageCopy[(i * width + j) * 3 + 1];
				byte b = imageCopy[(i * width + j) * 3 + 2];
				int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
				bufferedImage.setRGB(j, i, rgb);
			}
		}
		try {
			ImageIO.write(bufferedImage, "bmp", file);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private static ArrayList<Entity> getAllEntities(boolean withDrones){
		ArrayList<Entity> entities = new ArrayList<>();
		for(Drone drone: worldManager.getDrones()){
			if(withDrones) entities.add(drone.getEntity());
			entities.add(drone.getShadowEntity());	
		}
		for(Airport airport: worldManager.getAirports()){
			entities.add(airport.getEntity());
		}
		return entities;
	}
	
	public static void setModule(AutopilotModule module){
		AppManager.module = module;
		worldManager.setAutopilotModule(module);
	}
}
