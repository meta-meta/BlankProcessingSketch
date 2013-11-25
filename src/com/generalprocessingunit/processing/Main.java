package com.generalprocessingunit.processing;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Main extends PApplet implements OSCListener {
    List<Note> notes = new ArrayList<Note>();

    // show notes recieved from 10 seconds ago
    static final int EVENT_HORIZON = 10000;
    OSCPortIn oscPortIn;

	public static void main(String[] args){
		PApplet.main(new String[] { /*"--present",*/ "com.generalprocessingunit.processing.Main" });
	}

    public Main(){
        super();

        try{
            oscPortIn = new OSCPortIn(7400);
            oscPortIn.addListener("/note", this);
            oscPortIn.startListening();
        }
        catch (Exception e){
            System.out.print(e);
        }
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

        for(int i = notes.size() - 1; i > -1; i--){
            Note note = notes.get(i);
            int d = millis() - note.millis;

            if(d > EVENT_HORIZON){
                break;
            }

            float hue = (255f/12) * (note.note%12);
            colorMode(HSB);
            fill(hue, 255, d < 50 ? 255 :  150 - 150f/EVENT_HORIZON * d);
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
	}


    @Override
    public void acceptMessage(Date time, OSCMessage message) {
        message.getAddress();
        message.getArguments();


        int note = (Integer) message.getArguments()[0];

        notes.add(new Note(note, millis()));
//        System.out.print(message.getArguments());

    }

    @Override
    public void destroy() {
        super.destroy();
        oscPortIn.close();
    }

    private class Note {
        public Note(int note, int millis){
            this.note=note;
            this.millis=millis;
        }

        int note;
        int millis;
    }
}
