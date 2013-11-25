package com.generalprocessingunit.processing;

import processing.core.PApplet;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


public class Main extends PApplet /*implements OSCListener */{
    List<Note> notes = new ArrayList<Note>();
    List<Monstar> monstars = new ArrayList<Monstar>();

    // show notes recieved from 10 seconds ago
    static final int EVENT_HORIZON = 5000;
    int millisAtLastMonstar = 0;
    int life = 100;
    int millisAtLastdamage=0;
    int score = 0;
//    OSCPortIn oscPortIn;

	public static void main(String[] args){
		PApplet.main(new String[] { /*"--present",*/ "com.generalprocessingunit.processing.Main" });
	}

    public Main(){
        super();

//        try{
//            oscPortIn = new OSCPortIn(7400);
//            oscPortIn.addListener("/note", this);
//            oscPortIn.startListening();
//        }
//        catch (Exception e){
//            System.out.print(e);
//        }
    }
		
	@Override
	public void setup() {		
		size(1000, 1000, PApplet.OPENGL);
	}
	
	@Override
	public void draw(){
        background(0);

        stroke(40);
        for(float theta = TWO_PI/24; theta < TWO_PI; theta += TWO_PI/12){
            line(width/2, height/2, width/2+cos(theta)*600, height/2+sin(theta)*600);
        }

        for(int i = monstars.size() - 1; i > -1; i--){
            Monstar monstar = monstars.get(i);

            if(!monstar.active){
                continue;
            }

            int t =  millis()-monstar.millis;
            int d = monstar.distance - t;


            pushMatrix();
            translate(width / 2, height / 2, (5000f / EVENT_HORIZON) * -d);

            float theta = TWO_PI / 12 * (monstar.note % 12);
            rotate(theta);

            stroke(255);
            noFill();
            if(d < 100){
                rect(-10,-400,10,-380);
                if(d<50){
                    monstar.active = false;
                    life-= 10;
                    millisAtLastdamage = millis();
                }
            }
            else
            {
                monstar.draw(0, -400, 50);
            }

            for(int j = notes.size() - 1; j > -1; j--){
                Note note = notes.get(j);
                if(!note.active){
                    continue;
                }

                if(note.note%12 != monstar.note%12){
                    continue;
                }

                if(abs(d - (millis() - note.millis)) < 50){
                    monstar.active = false;
                    note.active = false;
                    score++;
                }

                // no need to check any previous notes since they are all behind this monstar
                if(millis() - note.millis > d){
                    break;
                }

            }

            popMatrix();

        }

        for(int i = notes.size() - 1; i > -1; i--){
            Note note = notes.get(i);
            int d = millis() - note.millis;

            if(d > EVENT_HORIZON){
                if(note.active){
                    life -= 5;
                    note.active=false;
                }

                break;
            }

            if(!note.active){
                continue;
            }

            float hue = (255f/12) * (note.note%12);
            colorMode(HSB);
            fill(hue, 255, d < 50 ? 255 :  150 - 150f/EVENT_HORIZON * d);
            colorMode(RGB);
            noStroke();

            pushMatrix();
            translate(width/2, height/2, (5000f/EVENT_HORIZON) * -d);
            float theta = TWO_PI/12 * (note.note%12);
            rotate(theta);

            int octave = note.note/12;
            ellipse(0 , -400 , 200 - 25*octave, 1 + 25*octave );
//            ellipse(-300 + note.note%12 * 70, note.note/12 * -100 +  d*0.1f, 50, 50);
            popMatrix();

        }

        if((millis() - millisAtLastMonstar > 500)){
            if((int)random(2) == 0){
                monstars.add(new Monstar((int)random(12), 3000));

                //TODO: pick notes according to randomly picked sequences of deltas
            }
            millisAtLastMonstar = millis();
        }

        life = min(100, life+ (millis() - millisAtLastdamage < 2000 ? 0 : 1 ));
        if(life <= 0){
            score = 0;
            life = 100;
        }

        noStroke();
        fill(255,0,0);
        rect(width - (width/10), 0, ((width/10f)/100f) * life, 20);
        System.out.println(life);

        noFill();
        strokeWeight(2);
        stroke(255,255,255);
        rect(width - (width/10), 0, width, 20);

        for(int i=0; i < score/5; i++){
            ellipse(10 + 5*i, 10, 5, 5);
        }
	}

//
//    @Override
//    public void acceptMessage(Date time, OSCMessage message) {
//        message.getAddress();
//        message.getArguments();
//
//
//        int note = (Integer) message.getArguments()[0];
//
//        notes.add(new Note(note, millis()));
////        System.out.print(message.getArguments());
//
//    }
//
//    @Override
//    public void destroy() {
//        super.destroy();
//        oscPortIn.close();
//    }


    @Override
    public void keyPressed(KeyEvent e)
    {
        super.keyPressed(e);    //To change body of overridden methods use File | Settings | File Templates.

        System.out.print(e.getKeyCode());
        notes.add(new Note(e.getKeyCode(), millis()));
    }

    private class Note {
        public Note(int note, int millis){
            this.note=note;
            this.millis=millis;
        }

        int note;
        int millis;
        boolean active = true;
    }

    private class Monstar {
        public Monstar(int note, int distance){
            this.note=note;
            this.distance=distance;
            this.millis = millis();
        }

        void draw(float x, float y, float r){
            pushMatrix();
            translate(x,y);
            rotate(millis()/1000f);
            for(float theta=0; theta<TWO_PI; theta+=TWO_PI/5){
                line(cos(theta) * r, sin(theta)*r, cos(theta + 2*TWO_PI/5)*r, sin(theta + 2*TWO_PI/5)*r);
            }
            popMatrix();
        }

        int note;
        int distance;
        int millis;
        boolean active = true;
    }
}
