package Model;

public enum Difficulty {
    VEOMA_LAK(1), LAK(3), TEZAK(7), EKSTREMNO_TEZAK(20);
    public final int xp;
    Difficulty(int xp) { this.xp = xp; }
}