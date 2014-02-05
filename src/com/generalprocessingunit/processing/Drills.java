package com.generalprocessingunit.processing;

import java.util.*;

public class Drills {

    public class SequenceAttempt {
        String sequenceId;
        int totalHits = 0, totalMisses = 0;

        SequenceAttempt(String sequenceId){
            this.sequenceId = sequenceId;
        }

        float percentage(){
            return (float)totalMisses / (totalHits + totalMisses);
        }

        boolean flawless(){
            return totalMisses == 0;
        }
    }

    private List<SequenceAttempt> sequenceAttempts = new ArrayList<>();
    private Map<String, List<SequenceAttempt>> sequenceAttemptsBySequence= new HashMap<>();

    private Set<String> sequencesThatNeedPractice = new HashSet<>();
    private static final float MIN_HIT_PERCENTAGE_TO_PASS = 0.8f;
    private static final int NUM_RECENT_ATTEMPTS_TO_TEST = 10;

    void recordSequenceAttempt(SequenceAttempt sequenceAttempt){
        sequenceAttempts.add(sequenceAttempt);

        if(!sequenceAttemptsBySequence.containsKey(sequenceAttempt.sequenceId)){
            sequenceAttemptsBySequence.put(sequenceAttempt.sequenceId, new ArrayList<SequenceAttempt>());
        }
        sequenceAttemptsBySequence.get(sequenceAttempt.sequenceId).add(sequenceAttempt);
    }

    float percentageRecentAttempts(String sequenceId, int numAttempts){
        List<SequenceAttempt> attempts = sequenceAttemptsBySequence.get(sequenceId);

        int totalHits = 0, totalMisses = 0;
        for(int i = attempts.size() - 1; i >= Math.max(attempts.size() - numAttempts, 0); --i){
            totalHits += attempts.get(i).totalHits;
            totalMisses += attempts.get(i).totalMisses;
        }

        float hitPercentage = (float)totalHits / (totalHits + totalMisses);

        if(hitPercentage < MIN_HIT_PERCENTAGE_TO_PASS){
            sequencesThatNeedPractice.add(sequenceId);
        } else {
            sequencesThatNeedPractice.remove(sequenceId);
        }

        return hitPercentage;
    }

    SequenceAttempt currentSequenceAttempt;
    int currentSequenceLength;
    String currentSequenceId;

    Iterator<Integer> currentSequenceIterator;
    int currentNote = 0;

    void playSequence(String sequenceId){
        currentSequenceAttempt = new SequenceAttempt(sequenceId);
        currentSequenceLength = Sequences.get(sequenceId).size() + 1; // +1 for the given first note
        currentSequenceId = sequenceId;

        List<Integer> currentSequence = Arrays.asList(0);
        currentSequence.addAll(Sequences.get(sequenceId));
        currentSequenceIterator = currentSequence.listIterator();
    }

    // call this when Monstar gets removed from list
    void noteResult(boolean hit){
        if(hit){
            currentSequenceAttempt.totalHits++;
        } else {
            currentSequenceAttempt.totalMisses++;
        }
    }

    int getNextNote(int lowestNote, int highestNote){
        if(currentSequenceIterator.hasNext()){
            currentNote += currentSequenceIterator.next();
            return currentNote;
        } else {
            chooseNewSequence(lowestNote, highestNote);
            return -1;
        }
    }

    void chooseNewSequence(int lowestNote, int highestNote){
        if (currentSequenceAttempt.totalHits + currentSequenceAttempt.totalMisses < currentSequenceLength) {
            return;  // only allow one sequence at a time
        }

        recordSequenceAttempt(currentSequenceAttempt);

        // how bad did we do?
        if (Math.random() > currentSequenceAttempt.percentage()){
            // if we've gotten here check how bad we've been doing with this one recently
            if(Math.random() > percentageRecentAttempts(currentSequenceId, NUM_RECENT_ATTEMPTS_TO_TEST)){
                // we've done bad enough in the last 10 attempts to need practice
                playSequence(currentSequenceId);
                return;
            }
        }

        if(Math.random() > 0.4){
            // TODO: get sequence that needs practice and is in bounds
            playSequence(getSequenceIdInBounds(currentNote, lowestNote, highestNote, sequencesThatNeedPractice));
            return;
        }


        // play any sequence
        playSequence(getSequenceIdInBounds(currentNote, lowestNote, highestNote, Sequences.sequenceIds()));
    }

    static String getSequenceIdInBounds(int startNote, int lowestNote, int highestNote, Collection<String> sequenceIds) {
        List<String> remaining = new ArrayList<>(sequenceIds);

        // pick a sequence that will leave us in bounds
        for(Boolean inBounds = false; !inBounds && remaining.size() > 0; ){
            int i = (int)(Math.random() * remaining.size());
            int nextNote = startNote;

            String sequenceId = remaining.get(i);
            List<Integer> seq = Sequences.get(sequenceId);
            for(int x: seq){
                nextNote += x;
            }

            inBounds = (nextNote >= lowestNote && nextNote <= highestNote);
            if(inBounds){
                return sequenceId;
            }

            remaining.remove(i);
        }

        // fallover. there will definitely be something in the master list
        Set<String> remainingSequences = new HashSet<>(Sequences.sequenceIds());
        remainingSequences.removeAll(sequenceIds);
        return getSequenceIdInBounds(startNote, lowestNote, highestNote, remainingSequences);
    }


    static class Sequences {
        private static Map<String, List<Integer>> sequences = new HashMap<>();

        static Set<String> sequenceIds(){
            return sequences.keySet();
        }

        static Integer size(){
            return sequences.size();
        }

        static {
            Sequences.putAllTransformations("Chromatic Octave", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
            Sequences.putAllTransformations("Whole Tone Scale", 2, 2, 2, 2, 2, 2);
            Sequences.putAllTransformations("Ionian Mode", 2, 2, 1, 2, 2, 2, 1);
            Sequences.putAllTransformations("Dorian Mode", 2, 1, 2, 2, 2, 1, 2);
            Sequences.putAllTransformations("Phrygian Mode ", 1, 2, 2, 2, 1, 2, 2);
            Sequences.putAllTransformations("Lydian Mode", 2, 2, 2, 1, 2, 2, 1);
            Sequences.putAllTransformations("Mixolydian Mode", 2, 2, 1, 2, 2, 1, 2);
            Sequences.putAllTransformations("Aeolian Mode", 2, 1, 2, 2, 1, 2, 2);
            Sequences.putAllTransformations("Locrian Mode", 1, 2, 2, 1, 2, 2, 2);

            // TODO: these should be next level and 1324 should be a level up from each of these groups
            // TODO: How about inversions
            Sequences.putSingle("Major", 4, 3, 5);
            Sequences.putSingle("Minor", 3, 4, 5);
            Sequences.putSingle("Diminished", 3, 3, 6);
            Sequences.putSingle("Augmented", 4, 4, 4);
            Sequences.putSingle("Major 7",4, 3, 4, 1);
            Sequences.putSingle("Minor 7", 3, 4, 3, 2);
            Sequences.putSingle("Dominant 7", 4, 3, 3, 2);
            Sequences.putSingle("Minor Major 7", 3, 4, 4, 1);
            Sequences.putSingle("Diminished 7", 3, 3, 3, 3);
            Sequences.putSingle("Half-diminished", 3, 3, 4, 2);
            Sequences.putSingle("Augmented 7", 4, 4, 2, 2);
        }

        static List<Integer> get(String sequenceId){
            return sequences.get(sequenceId);
        }

        private static void put(String title, List<Integer> sequence){
            // prepend a 0 to the sequence before putting it
            List<Integer> finalSequence = new ArrayList<>();
            finalSequence.add(0);
            finalSequence.addAll(sequence);
            sequences.put(title, finalSequence);
        }

        private static void putAllTransformations(String title, Integer... array){
            put(title, Arrays.asList(array));
            putInverse(title, array);
            put1324(title, array);
        }

        private static void putSingle(String title, Integer... array){
            put(title, Arrays.asList(array));
        }

        private static void putInverse(String title, Integer... array){
            put(title + " desc", invert(Arrays.asList(array)));
        }

        private static void put1324(String title, Integer... array){
            if(array.length < 2){
                return;
            }

            List<Integer> newList = new ArrayList<>();
            for(int i = 0; i < array.length; i++){
                newList.add(array[i] + array[(i+1) % array.length]);
                newList.add(-array[(i+1) % array.length]);
            }

            put(title + " 1324", newList);
        }

        private static List<Integer> invert(List<Integer> oldList) {
            List<Integer> newList = new ArrayList<>(oldList.size());
            for (int i = oldList.size() - 1; i >= 0; i--) {
                newList.add(-oldList.get(i));
            }
            return newList;
        }
    }



}


