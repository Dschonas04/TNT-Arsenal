// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.block;

/**
 * Die drei Bombengrößen. Radius und Lunte kommen aus der Konfiguration; die Stufe benennt sie nur
 * und sagt, welcher Konfigurationseintrag gilt.
 */
public enum Tier {
    MINI("mini_nuke"),
    NUKE("nuke"),
    TSAR("tsar_bomba");

    private final String id;

    Tier(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
