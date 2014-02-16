package com.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input.Keys;

public class ValentinesDayHearts implements ApplicationListener, InputProcessor{
	final static float gravityCoef = 250f;
	final static int initialHeartCount = 10;
	final static float PhysWorldWidth = 80;
	final static float PhysWorldHeight = 48;
	
	final static float HeartMinSize = 12;
	final static float HeartMaxSize = 20;
	final static float HeartHeightWidthCoef = 1.0f;
	
	final static float[] HeartHues = new float[] 
			{ 0, 8.0f / 360.0f, 16.0f / 360.0f, 320.0f / 360.0f, 333.0f / 360.0f  };
	
	private int WorldWidth;
	private int WorldHeight;
	private int RenderCoef = 20;
	
	List<String> heartWords;
	
	final static Vector2 CenterDisplacement = new Vector2(128.0f / 256.0f, 150.0f / 256.0f * HeartHeightWidthCoef);
	
	public final static Vector2 FontSizeHeartSizeCoef = new Vector2(0.046f, 0.05f);
			//new Vector2(0.13f, 0.13f);
	
	private OrthographicCamera camera;
	private World world;
	FixtureAtlas fixtureAtlas;
	private SpriteBatch spriteBatch;
	private BitmapFont font;
	
	private Texture backgroundTexture;
	
	private List<Texture> heartTextures;
	
	private Map<Body, Heart> hearts;
	
	private List<Heart> heartsToRemove;
	
	private Sound createHeartSound;
	private List<Sound> breakingSounds;
	
	private Vector3 testPoint = new Vector3();
	private Body hitBody = null;
	
	private Vector2 gravity;
	
	private int bgAngleStepCount;
	private int bgPosUpdateCount;
	
	private Vector2 bgPos;
	private float bgVel;
	private float bgDirCurrentAngle;	
	private float bgDirBeginAngle;	
	private float bgDirEndAngle;
	private int currentBgAngleStep;
	private int currentBgUpdate;
	
	private Vector2 tmpVector1 = new Vector2();
	private List<Heart> diedHearts = new ArrayList<Heart>();
	
	private int renderFreq = 0;
	
	@Override
	public void create() {		
		WorldWidth = Gdx.graphics.getWidth();
		WorldHeight = Gdx.graphics.getHeight();
		
		loadWords("data/words.txt");
		
		camera = new OrthographicCamera(PhysWorldWidth, PhysWorldHeight);
		camera.position.set(0, 0, 0);
	
		spriteBatch = new SpriteBatch();
		font = new BitmapFont(
				Gdx.files.internal("data/Jura-Medium.fnt"), 
				Gdx.files.internal("data/Jura-Medium.png"), false);
		font.setColor(Color.WHITE);
		
		fixtureAtlas = new FixtureAtlas(Gdx.files.internal("data/hearts.bin"));
		
		backgroundTexture = new Texture("data/background.png");

		hearts = new HashMap<Body, Heart>(initialHeartCount);
		
		prepareHeartsTextures(Gdx.files.internal("data/heart.png"));
		
		createHeartSound = Gdx.audio.newSound(Gdx.files.internal("data/beating.ogg"));
		breakingSounds = new ArrayList<Sound>();
		for (int i = 1; i <= 9; i++)
			breakingSounds.add(Gdx.audio.newSound(Gdx.files.internal("data/sound (" + i + ").ogg")));
		
		gravity = Gdx.app.getType() == ApplicationType.Android ? 
				new Vector2() : new Vector2(0, -gravityCoef / 2.5f);
		createPhysicsWorld();
		createHearts();

		heartsToRemove = new ArrayList<Heart>();
		
		bgPos = new Vector2(0.5f, 0.5f);
		currentBgAngleStep = 0;
		bgDirBeginAngle = (float)(Math.random() * Math.PI * 2);
		bgDirEndAngle = bgDirBeginAngle + (float)(Math.random() * Math.PI + Math.PI / 4); 
		bgDirCurrentAngle = bgDirBeginAngle;
		
		if (Gdx.app.getType() == ApplicationType.Android) {
			bgAngleStepCount = 20;
			bgPosUpdateCount = 4;
			bgVel = 0.003f;
		} else {
			bgAngleStepCount = 40;
			bgPosUpdateCount = 16;
			bgVel = 0.0005f;
		}
		
		Gdx.input.setInputProcessor(this);
	}
	
	protected void loadWords(String fileName)  {
		if ((fileName == null) || (fileName == ""))
            throw new IllegalArgumentException();
        
        String line;
        heartWords = new ArrayList<String>();
        try
        {    
            BufferedReader in = new BufferedReader(
            		new InputStreamReader(Gdx.files.internal(fileName).read()));
            if (!in.ready())
                throw new IOException();
            while ((line = in.readLine()) != null) 
            	heartWords.add(line);
            in.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
	}

	protected void prepareHeartsTextures(FileHandle fileHandle) {
		heartTextures = new ArrayList<Texture>(1);
		heartTextures.add(new Texture(fileHandle));
		float[][][] hslData = prepareHslData(fileHandle);
		heartTextures = new ArrayList<Texture>(HeartHues.length);
		
		for (int i = 0; i < HeartHues.length; i++) {
			heartTextures.add(generateHeartTexture(hslData, HeartHues[i]));
			heartTextures.get(i).setFilter(TextureFilter.Linear, TextureFilter.Linear);		
		}
	}
	
	protected float[][][] prepareHslData(FileHandle fileHandle) {
		Pixmap pixmap = new Pixmap(fileHandle);
		float[][][] result = new float[pixmap.getWidth()][pixmap.getHeight()][4];
		
		for (int i = 0; i < pixmap.getWidth(); i++)
			for (int j = 0; j < pixmap.getHeight(); j++) {
				int color = pixmap.getPixel(i, j);
				float r = (float)((color >> 24) & 0xFF) / 255.0f;
				float g = (float)((color >> 16) & 0xFF) / 255.0f;
				float b = (float)((color >> 8) & 0xFF) / 255.0f;
				float a = (float)(color & 0xFF) / 255.0f;
				result[i][j] = ColorUtils.RgbToHsl(r, g, b, a);
			}
		
		return result;
	}
	
	protected Texture generateHeartTexture(float[][][] hslData, float newHue) {
		Pixmap pixmap = new Pixmap(hslData.length, hslData[0].length, Format.RGBA8888);

		float[] rgba;
		for (int i = 0; i < hslData.length; i++)
			for (int j = 0; j < hslData[0].length; j++) {
				rgba = ColorUtils.HslToRgb(newHue, hslData[i][j][1], hslData[i][j][2], hslData[i][j][3]);
				pixmap.setColor(rgba[0], rgba[1], rgba[2], rgba[3]);
				pixmap.drawPixel(i, j);
			}
		
		Texture result = new Texture(pixmap);
		return result;
	}
	
	@Override
	public void dispose() {
		for (int i = 0; i < heartTextures.size(); i++)
			heartTextures.get(i).dispose();
		spriteBatch.dispose();
		world.dispose();
	}

	@Override
	public void pause() {

	}	
	
	@Override
	public void render() {
		updatePhysics();
		updateBackground();		
		updateSprites();			
		updateParticles();
		if (renderFreq++ % RenderCoef == 0) {
			refresh();		
			renderBackground();
			renderHearts();		
			renderFonts();
			renderParticles();
			renderFreq = 0;
		}
	}
	
	protected void updatePhysics() {
		Body heartBody;
		
		for (Heart heart : hearts.values())	 {
			heartBody = heart.Body;
			
			if (Gdx.input.isKeyPressed(Keys.UP))
				heartBody.applyForceToCenter(0, 20000);
			
			if (Gdx.input.isKeyPressed(Keys.DOWN))
				heartBody.applyForceToCenter(0, -20000);
			
			if (Gdx.input.isKeyPressed(Keys.LEFT))
				heartBody.applyForceToCenter(-20000, 0);
			
			if (Gdx.input.isKeyPressed(Keys.RIGHT))
				heartBody.applyForceToCenter(20000, 0);
			
			if (Gdx.input.isKeyPressed(Keys.Q))
				heartBody.applyTorque(10000);
			
			if (Gdx.input.isKeyPressed(Keys.W))
				heartBody.applyTorque(-10000);
			
			break;
		}
		
		if (Gdx.app.getType() == ApplicationType.Android) {
			gravity.x = -Gdx.input.getPitch() / 90.0f;
			gravity.y = Gdx.input.getRoll() / 90.0f;
			gravity.mul(gravityCoef);
			world.setGravity(gravity);			
		}
		world.step(Gdx.app.getGraphics().getDeltaTime(), 6, 6);
		//world.step(Gdx.app.getGraphics().getDeltaTime(), 8, 3);
	}
	
	protected void updateBackground() {
		if (currentBgUpdate++ == bgPosUpdateCount) {
			
			bgPos.add(
				bgVel * (float)Math.cos(bgDirCurrentAngle), 
				bgVel * (float)Math.sin(bgDirCurrentAngle));
			
			bgDirCurrentAngle += (bgDirEndAngle - bgDirBeginAngle) / bgAngleStepCount;
			currentBgAngleStep++;
			
			if (currentBgAngleStep == bgAngleStepCount) {
				currentBgAngleStep = 0;
				bgDirBeginAngle = (float)(Math.random() * Math.PI * 2);
				bgDirEndAngle = bgDirBeginAngle + (float)(Math.random() * Math.PI * 2 - Math.PI); 
			}
			
			float textureWidth = (float)WorldWidth / backgroundTexture.getWidth();
			float textureHeight = (float)WorldHeight / backgroundTexture.getHeight();
			
			if (bgPos.x - textureWidth / 2 < 0 || bgPos.y - textureHeight / 2 < 0 ||
				bgPos.x + textureWidth / 2 > 1 || bgPos.y + textureHeight / 2 > 1)
					bgDirCurrentAngle -= (float)Math.PI;
			
			currentBgUpdate = 0;
		}
	}
	
	protected void updateSprites() {
		Body heartBody;
		
		for (Heart heart : hearts.values())	 {
			heartBody = heart.Body;
			Vector2 pos = heartBody.getPosition();
			float angleDeg = heartBody.getAngle() * MathUtils.radiansToDegrees;
	
			heart.Sprite.setPosition(pos.x, pos.y);
			heart.Sprite.setRotation(angleDeg);
		}
	}

	protected void updateParticles() {	
		for (Heart heart : heartsToRemove) {
			if (heart.ParticleEffect.isComplete())
				diedHearts.add(heart);
		}
		for (Heart heart : diedHearts)
			heartsToRemove.remove(heart);
		diedHearts.clear();
	}
	
	protected void refresh() {
		GL10 gl = Gdx.app.getGraphics().getGL10();
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
	}
	
	protected void renderBackground() {
		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0,
				WorldWidth, WorldHeight);
		spriteBatch.begin();
		
		float textureWidth = (float)WorldWidth / backgroundTexture.getWidth();
		float textureHeight = (float)WorldHeight / backgroundTexture.getHeight();
		
		spriteBatch.draw(backgroundTexture, 0, 0, WorldWidth, WorldHeight, 
				bgPos.x - textureWidth / 2, bgPos.y - textureHeight / 2,
				bgPos.x + textureWidth / 2, bgPos.y + textureHeight / 2);
		spriteBatch.end();
		/*
		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0,
				WorldWidth, WorldHeight);
		spriteBatch.begin();
		spriteBatch.draw(backgroundTexture, 0, 0, 0, 0, 800, 480);
		spriteBatch.end();*/
	}
	
	protected void renderHearts() {
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();		
		for (Heart heart : hearts.values())			
			heart.Sprite.draw(spriteBatch);
		spriteBatch.end();
	}
	
	protected void renderFonts() {	
		for (Heart heart : hearts.values())	 {
			Body body = heart.Body;
			Vector2 position = body.getPosition();
			tmpVector1.set((position.x / PhysWorldWidth + 0.5f) * WorldWidth, 
						   (position.y / PhysWorldHeight + 0.5f) * WorldHeight);
			
			Matrix4 projection = spriteBatch.getProjectionMatrix();
			projection.setToOrtho2D(0, 0, WorldWidth, WorldHeight);
			projection.translate(tmpVector1.x, tmpVector1.y, 0);
			projection.rotate(0, 0, 1, body.getAngle() / (float)Math.PI * 180);
			projection.translate(-tmpVector1.x, -tmpVector1.y, 0);
			
			Vector2 stringSize = heart.getStringSize();
			tmpVector1.add(heart.Size.x / PhysWorldWidth * WorldWidth * CenterDisplacement.x 
							   - stringSize.x * 0.5f, 
							   heart.Size.y /  PhysWorldHeight * WorldHeight * CenterDisplacement.y
							   + stringSize.y);	
			
			spriteBatch.begin();
			font.setScale(heart.Size.x * FontSizeHeartSizeCoef.x, heart.Size.y * FontSizeHeartSizeCoef.y);
			font.draw(spriteBatch, heart.String, tmpVector1.x, tmpVector1.y);
			font.setScale(1, 1);
			spriteBatch.end();
		}
	}

	protected void renderParticles() {	
		float delta = Gdx.graphics.getDeltaTime();
		for (Heart heart : heartsToRemove) {
			
			tmpVector1.set(heart.ParticleEffect.getEmitters().get(0).getX(), 
						   heart.ParticleEffect.getEmitters().get(0).getY());
			
			tmpVector1.sub(heart.Size.x / PhysWorldWidth * WorldWidth * CenterDisplacement.x,
							   heart.Size.y / PhysWorldHeight * WorldHeight * CenterDisplacement.y);
			
			Matrix4 projection = spriteBatch.getProjectionMatrix();
			projection.setToOrtho2D(0, 0, WorldWidth, WorldHeight);
			
			projection.translate(tmpVector1.x, tmpVector1.y, 0);
			projection.rotate(0, 0, 1, heart.getDeathAngle()  / (float)Math.PI * 180);
			projection.translate(-tmpVector1.x, -tmpVector1.y, 0);
			
			spriteBatch.begin();
			
			heart.ParticleEffect.draw(spriteBatch, delta);	
			
			spriteBatch.end();
		}
	}
	
	@Override
	public void resize(int arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}
	
	private void createHearts() {		
		for (int i = 0; i < initialHeartCount; i++)
			addHeart();
	}
	
	private void createPhysicsWorld () {
		world = new World(gravity, true);
		
		float wallWidth = 10;
		
		PolygonShape groundPoly;
		BodyDef groundBodyDef;
		Body groundBody;
		
		groundPoly = new PolygonShape();
		groundPoly.setAsBox(PhysWorldWidth + wallWidth, wallWidth, new Vector2(0, -PhysWorldHeight / 2 - wallWidth), 0);
		groundBodyDef = new BodyDef();
		groundBodyDef.type = BodyType.StaticBody;
		groundBody = world.createBody(groundBodyDef);		
		groundBody.createFixture(groundPoly, 10);
		groundPoly.dispose();
		
		groundPoly = new PolygonShape();
		groundPoly.setAsBox(PhysWorldWidth + wallWidth, wallWidth, new Vector2(0, +PhysWorldHeight / 2 + wallWidth), 0);
		groundBodyDef = new BodyDef();
		groundBodyDef.type = BodyType.StaticBody;
		groundBody = world.createBody(groundBodyDef);		
		groundBody.createFixture(groundPoly, 10);
		groundPoly.dispose();
		
		groundPoly = new PolygonShape();
		groundPoly.setAsBox(wallWidth, PhysWorldHeight + wallWidth, new Vector2(-PhysWorldWidth / 2 - wallWidth, 0), 0);
		groundBodyDef = new BodyDef();
		groundBodyDef.type = BodyType.StaticBody;
		groundBody = world.createBody(groundBodyDef);		
		groundBody.createFixture(groundPoly, 10);
		groundPoly.dispose();
		
		groundPoly = new PolygonShape();
		groundPoly.setAsBox(wallWidth, PhysWorldHeight + wallWidth, new Vector2(+PhysWorldWidth / 2 + wallWidth, 0), 0);
		groundBodyDef = new BodyDef();
		groundBodyDef.type = BodyType.StaticBody;
		groundBody = world.createBody(groundBodyDef);		
		groundBody.createFixture(groundPoly, 10);
		groundPoly.dispose();
	}
	
	@Override
	public boolean keyDown(int arg0) {
		return false;
	}

	@Override
	public boolean keyTyped(char arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyUp(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	QueryCallback callback = new QueryCallback() {
		@Override
		public boolean reportFixture (Fixture fixture) {
			// if the hit point is inside the fixture of the body
			// we report it
			if (fixture.testPoint(testPoint.x, testPoint.y)) {
				hitBody = fixture.getBody();
				return false;
			} else
				return true;
		}
	};
	
	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		camera.unproject(testPoint.set(x, y, 0));
		
		hitBody = null;
		world.QueryAABB(callback, testPoint.x - 0.0001f, testPoint.y - 0.0001f, testPoint.x + 0.0001f, testPoint.y + 0.0001f);

		// TODO: Repair;
		// if (hitBody == groundBody) hitBody = null;

		if (hitBody != null && hitBody.getType() == BodyType.KinematicBody) 
			return false;

		if (hitBody != null) {
			// Remove heart.
			if (hearts.containsKey(hitBody)) {
				Heart heart = hearts.get(hitBody);
				heart.destroy();
				
				heart.BreakSound.play(1);
				
				ParticleEffect effect = heart.ParticleEffect;
				Vector2 bodyPosition = heart.getDeathPos();
				effect.setPosition(((bodyPosition.x + heart.Size.x * 0.5f) / PhysWorldWidth  + 0.5f) * WorldWidth, 
								   ((bodyPosition.y + heart.Size.y * 0.5f) / PhysWorldHeight + 0.5f) * WorldHeight);
				effect.start();
				heartsToRemove.add(heart);
				
				world.destroyBody(hitBody);				
				hearts.remove(hitBody);		

				Gdx.input.vibrate(200);

			}
		} else {
			// Add heart.			
			addHeart(((float)x / WorldWidth - 0.5f) * PhysWorldWidth, 
					(-(float)y / WorldHeight + 0.5f) * PhysWorldHeight);
			
			createHeartSound.play(1);
		}
		
		return false;
	}
	
	private void addHeart() {
		addHeart(-1, -1);
	}
	
	private void addHeart(float x, float y) {
		BodyDef heartBodyDef = new BodyDef();
		heartBodyDef.type = BodyType.DynamicBody;
		
		Filter filter = new Filter();
		filter.groupIndex = 0;
		
		float newWidth = (float)Math.random() * (HeartMaxSize - HeartMinSize) + HeartMinSize;
		float newHeight = newWidth * HeartHeightWidthCoef;
			
		Vector2 size = new Vector2(newWidth, newHeight);
		
		heartBodyDef.angle = (float)(Math.atan2(gravity.y, gravity.x) + Math.PI / 2);
		float cosa = (float)Math.cos(heartBodyDef.angle);
		float sina = (float)Math.sin(heartBodyDef.angle);
		float dx = newWidth / 2;
		float dy = newHeight / 2;
		float x1 = dx * cosa - dy * sina;
		float y1 = dx * sina + dy * cosa;
		
		if (x == -1) {
			heartBodyDef.position.x = 
				(float)Math.random() * (PhysWorldWidth - newWidth) - (PhysWorldWidth / 2 - newHeight / 2);
			heartBodyDef.position.y = 
				(float)Math.random() * (PhysWorldHeight - newHeight) - (PhysWorldHeight / 2 - newHeight / 2);
		} else {
			heartBodyDef.position.x = x;
			heartBodyDef.position.y = y;
		}		
		
		heartBodyDef.position.x -= x1;
		heartBodyDef.position.y -= y1;
		
		Body body = world.createBody(heartBodyDef);
		
		fixtureAtlas.createFixtures(body, "heart.png", newWidth, newHeight);
			
		for (int j = 0; j < body.getFixtureList().size(); j++) {
			Fixture fixture = body.getFixtureList().get(j);
				
			fixture.setFilterData(filter);
			fixture.setFriction(0.75f);
			fixture.setDensity(1.0f);
			fixture.setRestitution(0.4f);
		}
			
		body.resetMassData();
		
		Sprite sprite = new Sprite(heartTextures.get((int)(Math.random() * heartTextures.size())));			
		sprite.setSize(newWidth, newHeight);
		sprite.setOrigin(0, 0);
		Vector2 heartPosition = body.getPosition();
		sprite.setPosition(heartPosition.x, heartPosition.y);
		float angleDeg = body.getAngle() * MathUtils.radiansToDegrees;
		sprite.setRotation(angleDeg);
		
		ParticleEffect effect = new ParticleEffect();
		effect.load(Gdx.files.internal("data/destroy.p"), Gdx.files.internal("data"));
		
		Sound breakingSound = breakingSounds.get((int)(Math.random() * breakingSounds.size()));
		
		hearts.put(body, new Heart(body, sprite, size, heartWords.get((int)(Math.random() * heartWords.size())), 
				font, effect, breakingSound));
	}
	
	@Override
	public boolean touchDragged(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchMoved(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		return false;
	}

}
