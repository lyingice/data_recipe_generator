package net.lyingice.datarecipegenerator.common;

/**
 * Thrown when a draft recipe cannot be turned into valid JSON.
 */
public class RecipeValidationException extends Exception {
    private final RecipeError error;
    private final String detail;

    public RecipeValidationException(RecipeError error) {
        this(error, "");
    }

    public RecipeValidationException(RecipeError error, String detail) {
        super(error.name() + (detail == null || detail.isEmpty() ? "" : ": " + detail));
        this.error = error;
        this.detail = detail == null ? "" : detail;
    }

    public RecipeError error() {
        return this.error;
    }

    public String detail() {
        return this.detail;
    }

    public String messageKey() {
        return this.error.messageKey();
    }
}
