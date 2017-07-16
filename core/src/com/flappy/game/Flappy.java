package com.flappy.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Flappy extends ApplicationAdapter  {

	SpriteBatch batch;
	Texture background;
	float w,h;
	Animation<TextureRegion> plane;
	OrthographicCamera camera;
	FPSLogger fpsLogger;
	TextureRegion terrainBelow;
	TextureRegion terrainAbove;
	float terrainOffset;
	float planeAnimTime;
	Vector2 planeVelocity = new Vector2();
	Vector2 scrollVelocity = new Vector2();
	Vector2 planePosition = new Vector2();
	Vector2 planeDefaultPosition = new Vector2();
	Vector2 gravity = new Vector2();
	private static final Vector2 damping = new Vector2(0.99f,0.99f);
	TextureAtlas.AtlasRegion bgRegion;
	Viewport viewport;
	Vector3 touchPosition = new Vector3(); // istanza per salvare il punto di touch
	Vector2 tmpVector = new Vector2();
	private static final int TOUCH_IMPULSE = 500;
	TextureRegion tapIndicator;
	TextureRegion gameOver;
	float tapDrawTime;
	private static final float TAP_DRAW_TIME_MAX = 1.0f;
	Array<Vector2> pillars = new Array<Vector2>();
	GameState gameState = GameState.INIT;
	Vector2 lastPillarPosition = new Vector2();
	TextureRegion pillarUp;
	TextureRegion pillarDown;
	float deltaPosition;
	Rectangle planeRect = new Rectangle();
	Rectangle obstacleRect = new Rectangle();

	InputAdapter inputAdapter= new InputAdapter()
	{
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button)
		{


			touchPosition.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			camera.unproject(touchPosition);
			tmpVector.set(planePosition.x, planePosition.y);
			tmpVector.sub(touchPosition.x, touchPosition.y).nor();
			planeVelocity.mulAdd(tmpVector,
					TOUCH_IMPULSE - MathUtils.clamp(Vector2.dst(touchPosition.x, touchPosition.y, planePosition.x, planePosition.y), 0, TOUCH_IMPULSE));
			tapDrawTime = TAP_DRAW_TIME_MAX;

			return true;
		}

		@Override
		public boolean touchUp(int x, int y, int pointer, int button) {

			return true;
		}
	};



	@Override
	public void create () {

		w = Gdx.graphics.getWidth(); // larghezza per la finestra
		h = Gdx.graphics.getHeight(); // altezza per la finestra
		fpsLogger = new FPSLogger();
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		//camera.setToOrtho(false,800,480);
		camera.position.set(400,240,0);

		viewport = new FitViewport(800,480,camera);

		TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("flappyassets.txt"));
		bgRegion = atlas.findRegion("background");
		terrainBelow = atlas.findRegion("groundGrass");
		terrainAbove = new TextureRegion (terrainBelow);
		terrainAbove.flip(true,true);

		plane = new Animation(0.05f,atlas.findRegion("planeRed1"),atlas.findRegion("planeRed2"),atlas.findRegion("planeRed3"),atlas.findRegion("planeRed2"));
		plane.setPlayMode(Animation.PlayMode.LOOP);
		touchPosition.set(Gdx.input.getX(),Gdx.input.getY(),0);

		camera.unproject(touchPosition);
		tapIndicator = atlas.findRegion("tap");
		gameOver = atlas.findRegion("textGameOver");
		pillarUp = atlas.findRegion("rockGrass");
		pillarDown = atlas.findRegion("rockGrassDown");


		resetScene();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		fpsLogger.log();
		updateScene();
		drawScene();

	}
	
	@Override
	public void dispose ()
	{
		batch.dispose();
		background.dispose();

	}

	private void drawScene()
	{
		// Ricorda che i draw vanno posti fra batch.begin ed end

		camera.update(); // aggiorna la camera
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.disableBlending();
		batch.draw(bgRegion, 0, 0);
		// aggiunge pixel traslucidi quando una texture viene disegnata su un'altra
		batch.enableBlending();
		for (Vector2 vec: pillars)
		{
			if (vec.y == 1)
				batch.draw(pillarUp, vec.x, 0);
			else
				batch.draw(pillarDown, vec.x, 480 - pillarDown.getRegionHeight());
		}
		batch.draw(terrainBelow,terrainOffset,0);
		batch.draw(terrainBelow,terrainOffset + terrainBelow.getRegionWidth(),0);
		batch.draw(terrainAbove,terrainOffset, 480 - terrainAbove.getRegionHeight());
		batch.draw(terrainAbove,terrainOffset + terrainAbove.getRegionWidth(), 480 - terrainAbove.getRegionHeight());
		batch.draw( plane.getKeyFrame(planeAnimTime),planePosition.x,planePosition.y);


		if(gameState == GameState.INIT)
			batch.draw(tapIndicator,planePosition.x,planePosition.y-80);
		else if(tapDrawTime>0)
			batch.draw(tapIndicator,touchPosition.x - 29.5f, touchPosition.y - 29.5f); //29.5 è metà fra altezza e larghezza schermo


		if(gameState == GameState.GAME_OVER)
			batch.draw(gameOver, 400-206 , 240-80);


		batch.end();

	}

	private void resetScene()
	{
		terrainOffset = 0;
		gameState = GameState.INIT;
		planeAnimTime = 0;
		planeVelocity.set(400,0);
		gravity.set(0,-4);
		planeDefaultPosition.set(400- 88/2, 240- 73/2);
		planePosition.set(planeDefaultPosition.x,planeDefaultPosition.y);
		scrollVelocity.set(4,0);
		pillars.clear();
		lastPillarPosition.setZero();
		addPillar();

	}

	private void updateScene() {
		if (Gdx.input.justTouched())
		{
			if (gameState == GameState.INIT)
			{
				gameState = GameState.ACTION;
				return;
			}
			if (gameState == GameState.GAME_OVER)
			{
				gameState = GameState.INIT;
				resetScene();
				return;
			}
		}

		Gdx.input.setInputProcessor(inputAdapter);
		if(gameState == GameState.INIT || gameState == GameState.GAME_OVER) return ;


		float deltaTime = Gdx.graphics.getDeltaTime();
		terrainOffset -= 150 * deltaTime;
		planeAnimTime += deltaTime;
		planeVelocity.scl(damping);
		planeVelocity.add(gravity);
		planeVelocity.add(scrollVelocity);

		planePosition.mulAdd(planeVelocity, deltaTime);
		terrainOffset -= planePosition.x - planeDefaultPosition.x;
		deltaPosition =  planePosition.x - planeDefaultPosition.x;
		planePosition.x = planeDefaultPosition.x;




		if (terrainOffset * -1 > terrainBelow.getRegionWidth())
			terrainOffset = 0;

		if (terrainOffset > 0)
			terrainOffset = -terrainBelow.getRegionWidth();

		planeRect.set(planePosition.x+16,planePosition.y,50,73);
		for (Vector2 vec : pillars)
		{
			vec.x -= deltaPosition;
			if (vec.x + pillarUp.getRegionWidth() < -10)
				pillars.removeValue(vec,false);
			if (vec.y ==1)
				obstacleRect.set(vec.x+10,0,pillarUp.getRegionWidth() - 20, pillarUp.getRegionHeight() - 10);
			else
				obstacleRect.set(vec.x + 10, 480 - pillarDown.getRegionHeight() + 10, pillarUp.getRegionWidth() - 20, pillarUp.getRegionHeight());
			if (planeRect.overlaps(obstacleRect))
				if (gameState != GameState.GAME_OVER)
					gameState = GameState.GAME_OVER;


		}

		if (lastPillarPosition.x < 400)
			addPillar();

		if (planePosition.y < terrainBelow.getRegionHeight() - 35 || planePosition.y + 73 > 480 - terrainBelow.getRegionHeight() + 35)
			if (gameState != GameState.GAME_OVER)
				gameState = GameState.GAME_OVER;



		tapDrawTime -= deltaTime;
	}


	@Override
	public void resize (int width, int height)
	{
		viewport.update(width,height);
	}

	private void addPillar()
	{
		Vector2 pillarPosition = new Vector2();

		if (pillars.size == 0)
			pillarPosition.x = (float) (800 + Math.random() *600 );
		else
			pillarPosition.x = lastPillarPosition.x + (float) (600 + Math.random() *600 );

		if (MathUtils.randomBoolean())
			pillarPosition.y = 1f;
		else
			pillarPosition.y = -1f; // upside down

		lastPillarPosition = pillarPosition;
		pillars.add(pillarPosition);
	}
}
