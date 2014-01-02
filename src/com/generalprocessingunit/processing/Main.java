package com.generalprocessingunit.processing;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;


public class Main extends PApplet{
	public static void main(String[] args){
		PApplet.main(new String[] { "--present", Main.class.getCanonicalName() });
	}


    PGraphics[] pg = new PGraphics[7];
	@Override
	public void setup() {		
		size(1920, 1080, PApplet.OPENGL);

        // create the PGraphics buffers
        for (int i = 0; i < pg.length; i++)
        {
            pg[i] = createGraphics(300 + i * 10, 300 + i * 10, P3D);
            pg[i].background(0);
        }
    }
	
	@Override
	public void draw(){

        for(int i = 0; i < pg.length; i++){

            pg[i].beginDraw();

            // reset camera and perspective
            pg[i].camera();
            pg[i].perspective();

            // fade the background
            pg[i].blendMode(BLEND);
            pg[i].colorMode(RGB);
            pg[i].fill(0, 10 * (i+1));
            pg[i].rect(0,0,pg[i].width,pg[i].height);

            if (pg.length - 1 == i){
                background(0);
            }

            // oscillate camera back and forth
            pg[i].camera(0, 0, 0,
                    sin(frameCount / 200f * (i + 1)) * 0.1f, sin(frameCount / 300f * (i + 1)) * 0.1f, 1,
                    0, 1, 0);

            // this isn't working
//            pg[i].perspective(PI / 2.8f, pg[i].width / pg[i].height, 0.0001f, 100000f);


            pg[i].colorMode(HSB);
            pg[i].blendMode(ADD);
            pg[i].stroke(   127 + sin((i + 1) * (frameCount / 50f)) * 127,
                            200,
                            255,
                            1 * (i + 1));   // alpha is a delicate balance with the black rect above
            pg[i].strokeWeight(1 + i/2);

            if(i > 0){
                pg[i].translate(0, 0, 100);
                pg[i].rotateZ(i * (i%2)* frameCount / 400f);
                pg[i].rotateY(i * (i%2)* frameCount / 500f);

                pg[i].fill(2);
                pg[i].textureMode(REPEAT);
                texturedCube(pg[i - 1].get(), pg[i], 36);

            }

            pg[i].endDraw();
        }

        background(255);
        image(pg[pg.length-1], 0, 0, width, height);
    }

    void texturedCube(PImage tex, PGraphics pg, float size) {
        pg.beginShape(QUADS);
        pg.texture(tex);

        // http://processing.org/examples/texturecube.html
        // Given one texture and six faces, we can easily set up the uv coordinates
        // such that four of the faces tile "perfectly" along either u or v, but the other
        // two faces cannot be so aligned.  This code tiles "along" u, "around" the X/Z faces
        // and fudges the Y faces - the Y faces are arbitrarily aligned such that a
        // rotation along the X axis will put the "top" of either texture at the "top"
        // of the screen, but is not otherwised aligned with the X/Z faces. (This
        // just affects what type of symmetry is required if you need seamless
        // tiling all the way around the cube)

        // +Z "front" face
        pg.vertex(-size, -size,  size, 0, 0);
        pg.vertex( size, -size,  size, 1, 0);
        pg.vertex( size,  size,  size, 1, 1);
        pg.vertex(-size,  size,  size, 0, 1);

        // -Z "back" face
        pg.vertex( size, -size, -size, 0, 0);
        pg.vertex(-size, -size, -size, 1, 0);
        pg.vertex(-size,  size, -size, 1, 1);
        pg.vertex( size,  size, -size, 0, 1);

        // +Y "bottom" face
        pg.vertex(-size,  size,  size, 0, 0);
        pg.vertex( size,  size,  size, 1, 0);
        pg.vertex( size,  size, -size, 1, 1);
        pg.vertex(-size,  size, -size, 0, 1);

        // -Y "top" face
        pg.vertex(-size, -size, -size, 0, 0);
        pg.vertex( size, -size, -size, 1, 0);
        pg.vertex( size, -size,  size, 1, 1);
        pg.vertex(-size, -size,  size, 0, 1);

        // +X "right" face
        pg.vertex( size, -size,  size, 0, 0);
        pg.vertex( size, -size, -size, 1, 0);
        pg.vertex( size,  size, -size, 1, 1);
        pg.vertex( size,  size,  size, 0, 1);

        // -X "left" face
        pg.vertex(-size, -size, -size, 0, 0);
        pg.vertex(-size, -size,  size, 1, 0);
        pg.vertex(-size,  size,  size, 1, 1);
        pg.vertex(-size,  size, -size, 0, 1);

        pg.endShape();
    }
}
