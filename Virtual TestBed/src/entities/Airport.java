package entities;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import rendering.Loader;
import shaders.ModelTexture;
import tools.Tools;

public class Airport {
	private static float w = 250;
	private static float l = 70;
	
	private final Vector2f position;
	private final float rotation;
	
	private final Matrix4f toAirport;
	private final Matrix4f toWorld;
	
	private static ModelTexture texture;
	private static Model model;
	
	private Entity entity;
	
	private Drone droneGate0;
	private Drone droneGate1;
	
	private final float HEIGHT = -4.0f;
	//private static final float HEIGHT = 0.05f;
	
	private int id;
	
	public Airport(Vector2f position, float rotation){
		this.position = position;
		this.rotation = rotation;
		this.toWorld = Tools.createTransformationMatrix(getPosition3D(), getRotation(), 0, 0, 1);
		this.toAirport = Matrix4f.invert(toWorld, null);
		TexturedModel tModel = new TexturedModel(model.getVaoID(), model.getVertexCount(), texture);
		this.entity = new Entity(tModel, new Vector3f(position.x, HEIGHT, position.y), rotation, 0, 0, 35);
	}
	
	public static void defineAirportParameters(float length, float width) {
		w = width;
		l = length;
	}
	
	public Vector2f getPosition2D(){
		return this.position;
	}
	
	public Vector3f getPosition3D(){
		return new Vector3f(position.x, 0, position.y);
	}
	
	public float getRotation(){
		return this.rotation;
	}
	
	public Vector3f getRelativeCoord(Vector3f position){
		return Tools.transformVector(this.toAirport, position);
	}
	
	public Vector3f getAbsolulateCoordinate(Vector3f relativeCoord) {
		return Tools.transformVector(this.toWorld, relativeCoord);
	}
	
	public Vector2f getAbsolulateCoordinate(Vector2f relativeCoord) {
		Vector3f vector3 = Tools.transformVector(this.toWorld, new Vector3f(relativeCoord.x, 0, relativeCoord.y));
		return new Vector2f(vector3.x, vector3.z);
	}
	
	public boolean droneInAirport(Drone drone){
		Vector3f relDronePos = getRelativeCoord(drone.getPosition());
		return Math.abs(relDronePos.x)<w/2 && Math.abs(relDronePos.z)<l/2;
		
	}

	public Entity getEntity(){
		return entity;
	}
	
	public static void setModel(Model model, ModelTexture texture) {
		Airport.model = model;
		Airport.texture = texture;
	}
	
	public Vector2f getCenterRunway0Relative() {
		return (new Vector2f(-(float)Math.cos(rotation), (float)Math.sin(rotation))).normalise(null);
	}

	public Vector3f getPlaneSpawnGate0(float droneHeight) {
		Vector2f relSpawn = new Vector2f(0, -w+35);
		Vector2f groundCoord = getAbsolulateCoordinate(relSpawn);
		return new Vector3f(groundCoord.x, droneHeight, groundCoord.y);
	}
	
	public Vector3f getPlaneSpawnGate1(float droneHeight) {
		Vector2f relSpawn = new Vector2f(0, w-35);
		Vector2f groundCoord = getAbsolulateCoordinate(relSpawn);
		return new Vector3f(groundCoord.x, droneHeight, groundCoord.y);
	}

	public boolean isGateZeroOccupied() {
		return droneGate0!=null;
	}

	public void setDroneGate0(Drone drone) {
		droneGate0 = drone;
		if(drone==null) return;
		drone.setAirport(this);
		drone.setGate(0);
	}

	public boolean isGateOneOccupied() {
		return droneGate1!=null;
	}
	
	public void setDroneGate1(Drone drone) {
		droneGate1 = drone;
		if(drone==null) return;
		drone.setAirport(this);
		drone.setGate(1);
	}
	
	public boolean isAirportOccupied(){
		return isGateOneOccupied()&&isGateZeroOccupied();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String toString(){
		return "Airport "+id;
	}
	
	public int isInGate(Drone drone) {
		Vector3f position = getRelativeCoord(drone.getPosition());
		if (Math.abs(position.x) < w/2 && Math.abs(position.z) < w) {
			if(position.z > 0) return 1;
			else return 0;
		} else {
			return -1;
		}
	}
	
	public static void setModel(Loader loader){
		float[] positions = {-w/2-l, 0, -w,
							-w/2-l, 0,  w,
							w/2+l , 0,  w,
							w/2+l , 0, -w};
		float[] textureCoords = { 0, 0,
								1, 0,
								1, 1,
								0, 1};
		float[] normals = {0,1,0,0,1,0,0,1,0,0,1,0};
		int[] indices = {0,1,3,	3,1,2};
		model = loader.loadToVAO(positions, textureCoords, normals, indices);
	}
}
