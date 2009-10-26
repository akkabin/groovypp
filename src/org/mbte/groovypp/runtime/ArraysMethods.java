package org.mbte.groovypp.runtime;

import java.util.Iterator;

public class ArraysMethods {
    private static int normaliseIndex(int i, int size) {
        int temp = i;
        if (i < 0) {
            i += size;
        }
        if (i < 0) {
            throw new ArrayIndexOutOfBoundsException("Negative array index [" + temp + "] too large for array size " + size);
        }
        return i;
    }

    public static <T> T getAt (T [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static <T> void putAt (T [] self, int i, T v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static byte getAt(byte [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (byte [] self, int i, byte v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static short getAt(short [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (short [] self, int i, short v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static int getAt(int [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (int [] self, int i, int v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static char getAt(char [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (char [] self, int i, char v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static float getAt(float [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (float [] self, int i, float v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static double getAt(double [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (double [] self, int i, double v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static boolean getAt(boolean [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (boolean [] self, int i, boolean v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static long getAt(long [] self, int i) {
        return self[normaliseIndex(i, self.length)];
    }

    public static  void putAt (long [] self, int i, long v) {
        self[normaliseIndex(i, self.length)] = v;
    }

    public static Iterator<Character> iterator (final char self []) {
        return new Iterator<Character> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Character next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Boolean> iterator (final boolean self []) {
        return new Iterator<Boolean> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Boolean next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Byte> iterator (final byte self []) {
        return new Iterator<Byte> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Byte next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Short> iterator (final short self []) {
        return new Iterator<Short> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Short next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Integer> iterator (final int self []) {
        return new Iterator<Integer> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Integer next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Long> iterator (final long self []) {
        return new Iterator<Long> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Long next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Float> iterator (final float self []) {
        return new Iterator<Float> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Float next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static Iterator<Double> iterator (final double self []) {
        return new Iterator<Double> () {
            int count = 0;

            public boolean hasNext() {
                return count != self.length;
            }

            public Double next() {
                return self[count++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}