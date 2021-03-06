package com.jayfella.jme.vehicle;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.simsilica.lemur.Label;

public class SpeedometerState extends BaseAppState {

    private final Node node;
    private final Vehicle vehicle;

    private Node guiNode;

    private Node speedoNeedleNode;
    private Label speedLabel;

    private Label gearLabel;

    private final Vehicle.SpeedUnit outputType;

    public SpeedometerState(Vehicle vehicle, Vehicle.SpeedUnit outputType) {

        this.node = new Node("Speedometer: " + vehicle.getName());
        this.node.setQueueBucket(RenderQueue.Bucket.Gui);
        this.vehicle = vehicle;

        this.outputType = outputType;
    }

    private void setProjectionHeight(Camera camera, float factor) {
        float bottom = camera.getFrustumBottom();
        camera.setFrustumBottom(bottom * factor);
        float left = camera.getFrustumLeft();
        camera.setFrustumLeft(left * factor);
        float right = camera.getFrustumRight();
        camera.setFrustumRight(right * factor);
        float top = camera.getFrustumTop();
        camera.setFrustumTop(top * factor);
    }

    private Texture2D generateImpostor(Node scene, int size) {

        Camera newCam = new Camera(size, size);
        newCam.setFrustumPerspective(45f, 1f, 1f, 2f);
        newCam.setParallelProjection(true);
        setProjectionHeight(newCam, size + 40);
        newCam.lookAtDirection(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);

        ViewPort vp = getApplication().getRenderManager().createPreView("Offscreen View", newCam);
        vp.setClearFlags(true, true, true);
        vp.setBackgroundColor(ColorRGBA.BlackNoAlpha);

        FrameBuffer offBuffer = new FrameBuffer(size, size, 1);

        Texture2D offTex = new Texture2D(size, size, Image.Format.ABGR8);
        offTex.setMinFilter(Texture.MinFilter.Trilinear);
        offTex.setMagFilter(Texture.MagFilter.Bilinear);

        offBuffer.setDepthBuffer(Image.Format.Depth);
        offBuffer.setColorTexture(offTex);

        vp.setOutputFrameBuffer(offBuffer);

        //scene.updateLogicalState(0);
        scene.updateGeometricState();

        vp.attachScene(scene);

        getApplication().getRenderManager().removeMainView(vp);

        return offTex;

    }

    private Node buildRadialNumbers(int max, int step, float radius, float border) {

        int count = (max / step) + 1;

        Node node = new Node("Numbers Node");
        node.setLocalTranslation(radius, radius, 1);

        float reducedRad = radius - border;

        int num = 0;

        int startAngle = 245;

        float angleStep = ((155 * 2f) / count) * FastMath.DEG_TO_RAD;

        float theta = startAngle * FastMath.DEG_TO_RAD;

        for (int i = 0; i <= count; i++) {

            float x = reducedRad * FastMath.cos(theta);
            float y = reducedRad * FastMath.sin(theta);

            Label label = new Label("" + num);
            label.setColor(ColorRGBA.White);

            label.setLocalTranslation(
                    x - (label.getPreferredSize().x * .5f),
                    y + (label.getPreferredSize().y * .5f),
                    0);

            node.attachChild(label);

            num += step;
            theta -= angleStep;

        }

        node.setLocalTranslation(0, 0, -1);

        return node;
    }

    private Geometry createSpeedoGeom(AssetManager assetManager) {


        Texture speedoBgTex = assetManager.loadTexture("Textures/Vehicles/Speedometer/speedo_bg_2.png");

        Geometry speedoBgGeom = new Geometry("Speedometer Background Geometry",
                new Quad(speedoBgTex.getImage().getWidth(), speedoBgTex.getImage().getHeight()));

        speedoBgGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        speedoBgGeom.getMaterial().setTexture("ColorMap", speedoBgTex);
        speedoBgGeom.getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        Node numbers = buildRadialNumbers((int) vehicle.getGearBox().getMaxSpeed(outputType), 10, speedoBgTex.getImage().getWidth() / 2f, 20);

        speedoBgGeom.setLocalTranslation(
                -speedoBgTex.getImage().getWidth() / 2f,
                -speedoBgTex.getImage().getHeight() / 2f,
                -1

        );

        numbers.attachChild(speedoBgGeom);

        Texture2D numberTexture = generateImpostor(numbers, speedoBgTex.getImage().getWidth());

        Geometry numbersGeom = new Geometry("Speedo Numbers",
                new Quad(speedoBgTex.getImage().getWidth(), speedoBgTex.getImage().getHeight()));

        numbersGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        numbersGeom.getMaterial().setTexture("ColorMap", numberTexture);
        numbersGeom.getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        return numbersGeom;
    }

    @Override
    protected void initialize(Application app) {

        this.guiNode = ((SimpleApplication)app).getGuiNode();

        Geometry speedoGeometry = createSpeedoGeom(app.getAssetManager());

        node.attachChild(speedoGeometry);

        speedoNeedleNode = new Node("Speedo Needle Node");

        Texture speedoNeedleTex = app.getAssetManager().loadTexture("Textures/Vehicles/Speedometer/speedo_needle_2.png");
        Geometry speedoNeedleGeom = new Geometry("Speedometer Needle Geometry",
                new Quad(speedoNeedleTex.getImage().getWidth(), speedoNeedleTex.getImage().getHeight()));

        speedoNeedleGeom.setMaterial(new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md"));
        speedoNeedleGeom.getMaterial().setTexture("ColorMap", speedoNeedleTex);
        speedoNeedleGeom.getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        speedoNeedleGeom.setLocalTranslation(
                -(speedoNeedleTex.getImage().getWidth() * 0.5f),
                -(speedoNeedleTex.getImage().getWidth() * 0.5f) - 7,
                0);

        speedoNeedleNode.setLocalTranslation(100, 100, 1);
        speedoNeedleNode.attachChild(speedoNeedleGeom);

        node.attachChild(speedoNeedleNode);

        speedLabel = new Label("888");
        speedLabel.setColor(new ColorRGBA(66 / 255f, 244 / 255f, 241 / 255f, 1.0f));

        speedLabel.setLocalTranslation(
                100 - (speedLabel.getPreferredSize().x * 0.5f),
                speedLabel.getPreferredSize().y + 15,
                1
        );
        node.attachChild(speedLabel);

        node.setLocalTranslation(
                app.getCamera().getWidth() - 200 - 20,
                20, 0
        );

        this.gearLabel = new Label("N");
        gearLabel.setColor(new ColorRGBA(66 / 255f, 244 / 255f, 241 / 255f, 1.0f));
        gearLabel.setLocalTranslation(
                100 - (gearLabel.getPreferredSize().x * 0.5f),
                speedLabel.getPreferredSize().y + 45,
                1
        );
        node.attachChild(gearLabel);

    }



    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        guiNode.attachChild(node);
    }

    @Override
    protected void onDisable() {
        node.removeFromParent();
    }

    private final Quaternion speedoRot = new Quaternion();
    private float[] speedoAngles = new float[3];


    private String speedFormatMph = "%03.0f";

    @Override
    public void update(float tpf) {

        float startStopAngle = 155;

        // if we just deal with speed based on a positive integer from the start, everything works the same if we are reversing.

        float speed = Math.abs(vehicle.getSpeed(outputType));
        float speedUnit = speed / vehicle.getGearBox().getMaxSpeed(outputType);

        float rot = startStopAngle - ((startStopAngle * 2) * speedUnit);
        rot = FastMath.clamp(rot, -startStopAngle, startStopAngle);
        rot = rot * FastMath.DEG_TO_RAD;

        speedoAngles[2] = rot;
        speedoRot.fromAngles(speedoAngles);
        speedoNeedleNode.setLocalRotation(speedoRot);
        speedLabel.setText(String.format(speedFormatMph, speed));
        gearLabel.setText( "" + (vehicle.getGearBox().getActiveGearNum() + 1) );

    }

}
