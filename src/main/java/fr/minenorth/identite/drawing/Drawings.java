package fr.minenorth.identite.drawing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/** Tous les dessins d'un personnage. Immuable ; un dessin absent est entièrement transparent. */
public final class Drawings {
    public static final Drawings EMPTY = new Drawings(new EnumMap<>(DrawingKind.class));

    private final Map<DrawingKind, byte[]> grids;

    private Drawings(Map<DrawingKind, byte[]> grids) { this.grids = grids; }

    /** Copie de la grille (largeur × hauteur numéros de couleur, ligne par ligne). */
    public byte[] get(DrawingKind kind) {
        byte[] g = grids.get(kind);
        return g == null ? new byte[kind.width() * kind.height()] : g.clone();
    }

    public boolean isEmpty() { return grids.isEmpty(); }

    /** Lève IllegalArgumentException si la taille est mauvaise ou si un numéro sort de la palette. */
    public Drawings with(DrawingKind kind, byte[] grid) {
        check(kind, grid);
        Map<DrawingKind, byte[]> copy = new EnumMap<>(DrawingKind.class);
        copy.putAll(grids);
        if (blank(grid)) copy.remove(kind);
        else copy.put(kind, grid.clone());
        return new Drawings(copy);
    }

    private static boolean blank(byte[] grid) {
        for (byte b : grid) if (b != 0) return false;
        return true;
    }

    private static void check(DrawingKind kind, byte[] grid) {
        if (grid == null || grid.length != kind.width() * kind.height()) {
            throw new IllegalArgumentException(kind + " : taille incorrecte");
        }
        for (byte b : grid) {
            if (b < 0 || b > kind.paletteSize()) throw new IllegalArgumentException(kind + " : couleur hors palette");
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<DrawingKind, byte[]> e : grids.entrySet()) tag.putByteArray(e.getKey().name(), e.getValue());
        return tag;
    }

    /** Lecture tolérante d'une sauvegarde : un dessin illisible est simplement ignoré. */
    public static Drawings fromTag(CompoundTag tag) {
        Drawings out = EMPTY;
        for (DrawingKind kind : DrawingKind.values()) {
            if (!tag.contains(kind.name())) continue;
            try {
                out = out.with(kind, tag.getByteArray(kind.name()));
            } catch (IllegalArgumentException ignored) {
                // dessin abîmé ou d'une ancienne palette : on repart d'une grille vide
            }
        }
        return out;
    }

    public void write(FriendlyByteBuf buf) {
        for (DrawingKind kind : DrawingKind.values()) buf.writeBytes(get(kind));
    }

    /** Lève IllegalArgumentException (ou une exception de lecture) si les données sont invalides. */
    public static Drawings read(FriendlyByteBuf buf) {
        Drawings out = EMPTY;
        for (DrawingKind kind : DrawingKind.values()) {
            byte[] grid = new byte[kind.width() * kind.height()];
            buf.readBytes(grid);
            out = out.with(kind, grid);
        }
        return out;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Drawings d)) return false;
        for (DrawingKind k : DrawingKind.values()) if (!Arrays.equals(grids.get(k), d.grids.get(k))) return false;
        return true;
    }

    @Override public int hashCode() {
        int h = 0;
        for (DrawingKind k : DrawingKind.values()) h = 31 * h + Arrays.hashCode(grids.get(k));
        return h;
    }
}
