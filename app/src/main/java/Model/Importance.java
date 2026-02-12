package Model;

public enum Importance {
    NORMALAN(1), VAZAN(3), EKSTREMNO_VAZAN(10), SPECIJALAN(100);
    public final int xp;
    Importance(int xp) { this.xp = xp; }
}