package com.generalprocessingunit.processing;
import processing.core.PApplet;


public class Main extends PApplet{
	Orbitable sun = new Orbitable(10, 0, 20, 127, 127, 127, 0);
    float camR = 100;
    int sphereDetail = 1;

    public static void main(String[] args){
		PApplet.main(new String[] { /*"--present",*/ Main.class.getCanonicalName() });
	}
		
	@Override
	public void setup() {		
		size(1920, 1080, PApplet.OPENGL);
        background(50);
	}
	
	@Override
	public void draw(){
        blendMode(BLEND);
        camera();
        perspective();
        hint(DISABLE_DEPTH_MASK);
        fill(0,10);
        rect(0,0,width,height);
        hint(ENABLE_DEPTH_MASK);
//        background(50);


        perspective(PI/2.0f, width/height, 0.001f, 10000f);

        doCamera();


        if(keyPressed && keyCode == CONTROL){
            sphereDetail += (mouseY - (height/2)) * 0.01f;
        }
        sphereDetail(sphereDetail);
        blendMode(ADD);
        sun.draw(this);
	}

    private void doCamera()
    {
        float camX = (mouseX + millis() - (width/2)) * (PI/width);
        float camY = (mouseY - (height/2)) * (PI/height);
        if(keyPressed && keyCode == SHIFT){
            camR += (mouseY - (height/2)) * 0.01f;
        }

        camera(camR * sin(camX), camR * sin(camY), camR * cos(camX), 0, 0, 0, 0, 1, 0);
    }


}
