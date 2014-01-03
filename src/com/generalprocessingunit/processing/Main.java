package com.generalprocessingunit.processing;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.awt.event.KeyEvent;
import java.util.*;


public class Main extends PApplet implements OSCListener {

/*Spawn Speed
* */
    int timeBetweenSpawns = 1000;
    int hitStreak = 0;
    int missStreak = 0;

    // Faster
    public static final int HIT_STREAK_TO_SPEED_UP = 5;
    public static final int TIME_BETWEEN_SPAWNS_DEC = 50;
    public static final int MIN_TIME_BETWEEN_SPAWNS = 100;

    // Slower
    public static final int MISS_STREAK_TO_SLOW_DOWN = 3;
    public static final int TIME_BETWEEN_SPAWNS_INC = 200;
    public static final int MAX_TIME_BETWEEN_SPAWNS = 2000;

/*Stats
* */
    // Life
    float life = 100;
    public static final int MILLIS_WITHOUT_DAMAGE_UNTIL_LIFE_REGEN = 2000;
    public static final float LIFE_REGEN_PER_SEC = 10f;

    // Stats
    int score = 0;
    int millisAtLastdamage = 0;
    private int millisAtLastDraw = 0;

/*Entities
* */
    // Notes
    List<Note> notes = new ArrayList<>();
    // 34 - 98 full range
    // 48 - 83 middle 3 octaves
    public static final int LOWEST_NOTE = 48;
    public static final int HIGHEST_NOTE = 83;

    List<Monstar> monstars = new ArrayList<>();
    int millisAtLastMonstar = 0;

    static final int EVENT_HORIZON = 5000;
    public static final float Z_DEPTH = 5000f;

/*Boilerplate
* */
    OSCPortIn oscPortIn;
    OSCPortOut oscPortOut;

    PGraphics background;

    public static void main(String[] args){
		PApplet.main(new String[] { /*"--present",*/ "com.generalprocessingunit.processing.Main" });
	}

    public Main(){
        super();

        try{
            oscPortIn = new OSCPortIn(7400);
            oscPortIn.addListener("/noteOn", this);
            oscPortIn.addListener("/noteOff", this);
            oscPortIn.startListening();
            oscPortOut = new OSCPortOut();
        }
        catch (Exception e){
            System.out.print(e);
        }
    }
		
	@Override
	public void setup() {		
		size(1000, 1000, PApplet.OPENGL);

        background = createGraphics(width, height, P3D);
        drawBackground(background);
	}
	
	@Override
	public void draw(){
        hint(DISABLE_DEPTH_MASK);
        image(background.get(), 0, 0);
        hint(ENABLE_DEPTH_MASK);

        detectCollisions();

        drawMonstars();

        drawNotes();

        spawnMonstar();

        // let life increase slowly when damage has been avoided for more than x millis
        int millis = millis();
        life = min(100f, life + (millis - millisAtLastdamage < MILLIS_WITHOUT_DAMAGE_UNTIL_LIFE_REGEN ? 0f : (millis - millisAtLastDraw)*(LIFE_REGEN_PER_SEC /1000f)));
        millisAtLastDraw = millis;

        if(life <= 0){
            score = 0;
            life = 100;
        }

        if(hitStreak > HIT_STREAK_TO_SPEED_UP){
            hitStreak = 0;
            timeBetweenSpawns = max(MIN_TIME_BETWEEN_SPAWNS, timeBetweenSpawns - TIME_BETWEEN_SPAWNS_DEC);
//            System.out.println("TimeBetweenSpawns: " + timeBetweenSpawns);
        }

        if(missStreak > MISS_STREAK_TO_SLOW_DOWN){
            missStreak = 0;
            timeBetweenSpawns = min(MAX_TIME_BETWEEN_SPAWNS, timeBetweenSpawns + TIME_BETWEEN_SPAWNS_INC);
//            System.out.println("TimeBetweenSpawns: " + timeBetweenSpawns);

        }

        drawStats();
	}

    private void drawStats() {
        // life meter
        noStroke();
        fill(255, 0, 0);
        rect(width - (width / 10), 0, ((width / 10f) / 100f) * life, 20);

        //frame around life meter
        noFill();
        strokeWeight(2);
        stroke(255, 255, 255);
        rect(width - (width / 10), 0, width, 20);

        // score pellets
        for (int i = 0; i < score / 5; i++) {
            ellipse(10 + 5 * i, 10, 5, 5);
        }
    }

    private void spawnMonstar() {
        if((millis() - millisAtLastMonstar > timeBetweenSpawns)){
            int nextNote = getNextNote();
            monstars.add(new Monstar(nextNote, 3000));
            playSound("/spawn", Arrays.asList((Object)nextNote));
            millisAtLastMonstar = millis();
        }
    }

    int currentNote = 60;
    String currentSequence = "";
    Iterator<Integer> sequenceIterator;

    private int getNextNote() {
        if(currentSequence.isEmpty() || !sequenceIterator.hasNext()){
            currentSequence = Drills.getSequenceIdInBounds(currentNote, LOWEST_NOTE, HIGHEST_NOTE, Drills.Sequences.sequenceIds());
            sequenceIterator = Drills.Sequences.get(currentSequence).iterator();
        }

        currentNote += sequenceIterator.next();
        System.out.println("seq: " + currentSequence + " note: " + currentNote);
        return currentNote;
    }


    private int getRandomNote() {
        return (int)random(LOWEST_NOTE, HIGHEST_NOTE);
    }

    private void drawNotes() {
        for(int i = notes.size() - 1; i > -1; i--){
            Note note = notes.get(i);
            int d = millis() - note.millis;
            int d2 = note.noteOffReceived ? millis() - note.millisAtNoteOff : 0;

            if(d > EVENT_HORIZON){
                if (note.active) {
                    life -= 5;
                    millisAtLastdamage = millis();
                    missStreak++;
                    hitStreak = 0;
                    note.active = false;

                    playSound("/miss", Arrays.asList((Object) note.note));
                }

                break;
            }

            if(!note.active){
                continue;
            }

            // color the note
            float hue = (255f / 12) * (note.note % 12);
            colorMode(HSB);
            stroke(hue, 255, 255 - 150f / EVENT_HORIZON * d);
            colorMode(RGB);
            noFill();

            pushMatrix();
            translate(width / 2, height / 2);
            float theta = TWO_PI / 12 * (note.note % 12);
            rotate(theta);

            float   z1 = (Z_DEPTH / EVENT_HORIZON) * -d,
                    z2 = (Z_DEPTH / EVENT_HORIZON) * -d2;

            int octave = note.note / 12 - 2;
            strokeWeight(30 / (1.5f * octave));
            for (int o = 0; o < octave; o++) {
                float x = 20 * o - (10 * octave) + 10;
                line(x, -width / 2, z1, x, -width / 2, z2);
            }

            popMatrix();

        }
    }

    private void drawMonstars() {
        List<Monstar> monstarsToRemove = new ArrayList<>();

        for(Monstar monstar : monstars){
            int t = millis() - monstar.millis;
            int d = monstar.distanceAtSpawn - t;

            pushMatrix();
            translate(width / 2, height / 2);

            float theta = TWO_PI / 12 * (monstar.note % 12);
            rotate(theta);

            stroke(255);
            noFill();
            if (d < 50) {
                strokeWeight(20);
                arc(0, 0, width - 25, width - 25, -TWO_PI / 24 - HALF_PI, TWO_PI / 24 - HALF_PI);

                if (d < 50) {
                    monstarsToRemove.add(monstar);
                    life -= 10;
                    missStreak++;
                    hitStreak = 0;
                    millisAtLastdamage = millis();
                }
            } else {
                strokeWeight(1);
                translate(0, 0, (Z_DEPTH / EVENT_HORIZON) * -d);
                monstar.draw(0, -width / 2, 50);
            }

            popMatrix();
        }

        for (Monstar monstar : monstarsToRemove) {
            monstars.remove(monstar);
        }
    }

    private void detectCollisions() {
        List<Monstar> monstarsToRemove = new ArrayList<>();

        for(Monstar monstar : monstars){
            int millis = millis();
            int t = millis - monstar.millis;
            int monstarDistance = monstar.distanceAtSpawn - t;


            for (int i = notes.size() - 1; i > -1; i--) {
                Note note = notes.get(i);
                if (!note.active) {
                    continue;
                }

                if (note.note /*% 12 */!= monstar.note /*% 12*/) {
                    continue;
                }

                int noteDistance = millis - note.millis;
                int noteTailDistance = note.noteOffReceived ? millis - note.millisAtNoteOff : 0;

                // if collision
                if (abs(noteTailDistance - monstarDistance) <= 2*(millis - millisAtLastDraw) ||
                        abs(noteDistance - monstarDistance) <= 2*(millis - millisAtLastDraw)) {
                    monstarsToRemove.add(monstar);
                    note.active = false;
                    score++;
                    hitStreak++;
                    missStreak = 0;

                    playSound("/collision");
                }

                // no need to check any previous notes since they are all behind this monstar
                if (noteDistance > monstarDistance) {
                    break;
                }
            }
        }

        for(Monstar monstar : monstarsToRemove){
            monstars.remove(monstar);
        }
    }

    private void playSound(String s, Collection<Object> args) {
        OSCMessage msg = new OSCMessage(s, args);
        try {
            oscPortOut.send(msg);
        } catch (Exception e) {
            System.out.println("Couldn't send");
        }
    }

    private void playSound(String s) {
        playSound(s, null);
    }

    private void drawBackground(PGraphics buffer) {
        buffer.beginDraw();
        buffer.background(30,0,40);
        buffer.fill(0);
        buffer.noStroke();
        buffer.ellipse(width / 2f, height / 2f, width, width);
        buffer.colorMode(HSB);

        for(int r = width-50; r > 50; r -= r/30){
            buffer.strokeWeight(4f * r / (float) width);
            buffer.stroke(240, 200, 0.4f * (r * (255f / width)));
            buffer.ellipse(width / 2f, height / 2f, r, r);

            buffer.pushMatrix();
            buffer.translate(width / 2, height / 2);

            for (float theta = TWO_PI / 24; theta < TWO_PI; theta += TWO_PI / 12) {

                float   x1 = cos(theta) * (r),
                        y1 = sin(theta) * (r),
                        x2 = cos(theta) * (r - r/30),
                        y2 = sin(theta) * (r - r/30);

                buffer.line(x1 / 2, y1 / 2, x2 / 2, y2 / 2);
            }

            buffer.popMatrix();
        }
        buffer.endDraw();
    }

    @Override
    public void acceptMessage(Date time, OSCMessage message) {
        message.getAddress();
        message.getArguments();

        if(message.getAddress().equals("/noteOn")){
            int note = (Integer) message.getArguments()[0];
            notes.add(new Note(note, millis()));
//            System.out.println("note received: " + note);
        } else if (message.getAddress().equals("/noteOff")){
            notes.get(notes.size()-1).noteOff();
//            System.out.println("note OFF: " + notes.get(notes.size()-1).note);
        }
    }

    @Override
    public void destroy() {
        oscPortIn.close();
        super.destroy();
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        super.keyPressed(e);

        System.out.print(e.getKeyCode());
        notes.add(new Note(e.getKeyCode(), millis()));
    }

    private class Note {
        int note;
        int millis;
        boolean active = true;

        public Note(int note, int millis) {
            this.note = note;
            this.millis = millis;
        }

        boolean noteOffReceived = false;
        int millisAtNoteOff;

        public void noteOff(){
            noteOffReceived = true;
            millisAtNoteOff = millis();
        }
    }

    private class Monstar {
        int note;
        int distanceAtSpawn;
        int millis;

        public Monstar(int note, int distanceAtSpawn){
            this.note = note;
            this.distanceAtSpawn = distanceAtSpawn;
            this.millis = millis();
        }

        void draw(float x, float y, float r){
            pushMatrix();
            translate(x, y);
            rotate(millis() / 1000f);
            int points = 11 - note / 12;

            if (points == 7) {
                stroke(255, 100, 100);
            } else if (points == 6) {
                stroke(100, 255, 100);
            } else {
                stroke(100, 100, 255);
            }

            for (float theta = 0; theta < TWO_PI; theta += TWO_PI / points) {
                line(cos(theta) * r, sin(theta) * r, cos(theta + 2 * TWO_PI / points) * r, sin(theta + 2 * TWO_PI / points) * r);
            }
            popMatrix();
        }
    }
}
