package com.generalprocessingunit.processing;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;
import processing.core.PApplet;

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

    public static void main(String[] args){
		PApplet.main(new String[] { /*"--present",*/ "com.generalprocessingunit.processing.Main" });
	}

    public Main(){
        super();

        try{
            oscPortIn = new OSCPortIn(7400);
            oscPortIn.addListener("/note", this);
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
	}
	
	@Override
	public void draw(){
        background(0);

        drawRadialGrid();

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
            System.out.println("TimeBetweenSpawns: " + timeBetweenSpawns);
        }

        if(missStreak > MISS_STREAK_TO_SLOW_DOWN){
            missStreak = 0;
            timeBetweenSpawns = min(MAX_TIME_BETWEEN_SPAWNS, timeBetweenSpawns + TIME_BETWEEN_SPAWNS_INC);
            System.out.println("TimeBetweenSpawns: " + timeBetweenSpawns);

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

    private List<Integer> invert(List<Integer> oldList){
        List<Integer> newList = new ArrayList<Integer>(oldList.size());
        for(int i=oldList.size()-1; i >= 0; i--){
            newList.add(-oldList.get(i));
        }
        return  newList;
    }

    List<List<Integer>> spawnSequence = Arrays.asList(
            Arrays.asList(2,2,1,2,2,2,1),
            invert(Arrays.asList(2,2,1,2,2,2,1)),
//            Arrays.asList(2,1,2,2,1,2,2),
            Arrays.asList(4,-2,3,-1,3,-2,4,-2,4,-2,3,-1,3),
            invert(Arrays.asList(4,-2,3,-1,3,-2,4,-2,4,-2,3,-1,3))
//            Arrays.asList(2,2,2,2,2,2,-2,-2,-2,-2,-2,-2),
//            Arrays.asList(7),
//            Arrays.asList(-7),
//            Arrays.asList(5),
//            Arrays.asList(-5)

//            Arrays.asList(-1) // the above are all ascending. if there is no descending option we can get stuck trying to force a in bounds new sequence
//            TODO: perhaps label some of the more important ones
//            build this into a class and have them show the labels or symbol when a sequence starts
//            the player can then either ignore the cue and sight read or take the cue to know in advance
//            what is coming
    );

    int currentNote = 60;
    int currentSequence = -1;
    Iterator<Integer> sequenceIterator;

    private int getNextNote() {
        if(currentSequence == -1 || !sequenceIterator.hasNext()){
//            if((int)random(5) == 0){
//                currentSequence = -1;
//                currentNote = getRandomNote();
//                return currentNote;
//            }

            int i = -1;
            // pick a sequence that will leave us in bounds
            for(Boolean inBounds = false; !inBounds; ){
                i = (int)random(spawnSequence.size());
                int nextNote = currentNote;
                for(int x: spawnSequence.get(i)){
                    nextNote += x;
                }
                inBounds = (nextNote >= LOWEST_NOTE && nextNote <= HIGHEST_NOTE);
            }
            currentSequence = i;

            sequenceIterator = spawnSequence.get(currentSequence).iterator();
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
            fill(hue, 255, d < 50 ? 255 : 150 - 150f / EVENT_HORIZON * d);
            colorMode(RGB);
            noStroke();

            pushMatrix();
            translate(width / 2, height / 2, (Z_DEPTH / EVENT_HORIZON) * -d);
            float theta = TWO_PI / 12 * (note.note % 12);
            rotate(theta);

            int octave = note.note / 12;
            ellipse(0, -400, 200 - 25 * octave, 0 + 25 * octave);
//            ellipse(-300 + note.note%12 * 70, note.note/12 * -100 +  d*0.1f, 50, 50);
            popMatrix();

        }
    }

    private void drawMonstars() {
        List<Monstar> monstarsToRemove = new ArrayList<>();

        for(Monstar monstar : monstars){
            int t = millis() - monstar.millis;
            int d = monstar.distanceAtSpawn - t;

            pushMatrix();
            translate(width / 2, height / 2, (Z_DEPTH / EVENT_HORIZON) * -d);

            float theta = TWO_PI / 12 * (monstar.note % 12);
            rotate(theta);

            stroke(255);
            noFill();
            if (d < 100) {
                rect(-10, -400, 10, -380);
                if (d < 50) {
                    monstarsToRemove.add(monstar);
                    life -= 10;
                    missStreak++;
                    hitStreak = 0;
                    millisAtLastdamage = millis();
                }
            } else {
                monstar.draw(0, -400, 50);
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
            int t = millis() - monstar.millis;
            int d = monstar.distanceAtSpawn - t;

            for (int i = notes.size() - 1; i > -1; i--) {
                Note note = notes.get(i);
                if (!note.active) {
                    continue;
                }

                if (note.note /*% 12 */!= monstar.note /*% 12*/) {
                    continue;
                }

                // if collision
                if (abs(d - (millis() - note.millis)) < 50) {
                    monstarsToRemove.add(monstar);
                    note.active = false;
                    score++;
                    hitStreak ++;
                    missStreak = 0;

                    playSound("/collision");
                }

                // no need to check any previous notes since they are all behind this monstar
                if (millis() - note.millis > d) {
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

    private void drawRadialGrid() {
        stroke(40);

        pushMatrix();
        translate(width / 2, height / 2);

        for (float theta = TWO_PI / 24; theta < TWO_PI; theta += TWO_PI / 12) {

            float   x1 = cos(theta) * (width / 2),
                    y1 = sin(theta) * (width / 2),
                    x2 = cos(theta) * (width / 20),
                    y2 = sin(theta) * (width / 20);

            line(x1, y1, x2, y2);
        }

        ellipse(0, 0, 100, 100);

        popMatrix();
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
        public Note(int note, int millis) {
            this.note = note;
            this.millis = millis;
        }

        int note;
        int millis;
        boolean active = true;
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
