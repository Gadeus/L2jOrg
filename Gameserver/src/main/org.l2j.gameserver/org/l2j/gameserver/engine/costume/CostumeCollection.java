package org.l2j.gameserver.engine.costume;

import io.github.joealisson.primitive.IntSet;

/**
 * @author JoeAlisson
 */
public record CostumeCollection (int id, int skill, IntSet costumes) {
}
