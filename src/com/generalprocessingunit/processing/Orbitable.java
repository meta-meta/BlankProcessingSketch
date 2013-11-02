package com.generalprocessingunit.processing;

import processing.core.PApplet;
import processing.core.PConstants;

import java.util.ArrayList;
import java.util.List;

public class Orbitable
{
    float size;
    float phase;
    float orbitalRadius;
    float r, g, b;
    int rotAxis;
    List<Orbitable> satellites = new ArrayList<Orbitable>();

    Orbitable(float size, float phase, float orbitalRadius, float r, float g, float b, int rotAxis){
        this.size = size;
        this.phase = phase;
        this.orbitalRadius = orbitalRadius;
        this.r = r;
        this.g = g;
        this.b = b;
        this.rotAxis = rotAxis;
    }

    void draw(PApplet p5){
        spawn(p5);

        p5.pushMatrix();

        if(true){
            p5.stroke(r,g,b,30);
            p5.noFill();

        }else{
            p5.fill(r,g,b,1);
            p5.noStroke();
        }

        p5.sphere(size);
//        p5.ellipse(0,0,size,size);
//        p5.rect(0,0,size,size);


        for(Orbitable satellite : satellites){
            float direction1 = satellite.orbitalRadius * PApplet.sin((PApplet.TWO_PI + satellite.phase) * (p5.millis()/(1000f * satellite.orbitalRadius)));
            float direction2 = satellite.orbitalRadius * PApplet.cos((PApplet.TWO_PI + satellite.phase) * (p5.millis()/(1000f * satellite.orbitalRadius)));

            p5.translate(
                    rotAxis == 0 ? direction1 : rotAxis == 1 ? direction2 : 0,
                    rotAxis == 0 ? 0          : rotAxis == 1 ? direction1 : direction2,
                    rotAxis == 0 ? direction2 : rotAxis == 1 ? 0          : direction1);

            satellite.draw(p5);
        }

        p5.popMatrix();
    }

    void spawn(PApplet p5){
        int chance = PApplet.round(p5.random(500));
        if( chance == 8 && p5.frameRate > 20){
            float satelliteSize = p5.random(size / 4, size * 0.7f);
            satellites.add(new Orbitable(satelliteSize, p5.random(PConstants.TWO_PI), p5.random(size + satelliteSize, (satelliteSize/10) * (size + satelliteSize) ), p5.random(r-50, r+50), p5.random(g-50, g+50), p5.random(b-50, b+50), PApplet.round(p5.random(2)) ));
        }
    }
}
