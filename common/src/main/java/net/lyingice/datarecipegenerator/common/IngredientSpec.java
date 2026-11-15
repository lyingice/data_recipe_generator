package net.lyingice.datarecipegenerator.common;

import java.util.Objects;

/**
 * One resolved ingredient: either a concrete item id or a tag id (without the
 * leading '#').
 */
public final class IngredientSpec {
    private final String id;
    private final boolean tag;

    private IngredientSpec(String id, boolean tag) {
        this.id = id;
        this.tag = tag;
    }

    /**
     * Parses an override string. A leading '#' means "tag", otherwise the value
     * is an item id. Both forms must be valid resource locations.
     */
    public static IngredientSpec parse(String raw) throws RecipeValidationException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        boolean tag = value.startsWith("#");
        if (tag) {
            value = value.substring(1);
        }
        if (!ResourceIds.isValid(value)) {
            throw new RecipeValidationException(RecipeError.INVALID_RESOURCE_LOCATION, raw.trim());
        }
        return new IngredientSpec(value, tag);
    }

    public static IngredientSpec item(String id) throws RecipeValidationException {
        if (!ResourceIds.isValid(id)) {
            throw new RecipeValidationException(RecipeError.INVALID_RESOURCE_LOCATION, id);
        }
        return new IngredientSpec(id, false);
    }

    public String id() {
        return this.id;
    }

    public boolean tag() {
        return this.tag;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IngredientSpec other
                && this.tag == other.tag
                && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.tag);
    }

    @Override
    public String toString() {
        return this.tag ? "#" + this.id : this.id;
    }
}
