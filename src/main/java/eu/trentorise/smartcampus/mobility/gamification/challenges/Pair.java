package eu.trentorise.smartcampus.mobility.gamification.challenges;

public class Pair<T, U> {
    public final T t;
    public final U u;

    public Pair(T t, U u) {
        this.t= t;
        this.u= u;
    }

    public T getFirst() {
        return t;
    }

    public U getSecond() {
        return u;
    }
    
    @Override
    public String toString() {
    	return t + "." + u;
    }
    
}