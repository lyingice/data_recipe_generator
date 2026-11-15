package net.lyingice.datarecipegenerator.common;

/**
 * The result of building one recipe: pretty-printed JSON plus the information
 * needed for the success message and automatic file naming.
 */
public record RecipeBuildResult(
        String json,
        String fileName,
        String autoName,
        String outputItemId,
        String baseItemId,
        int outputCount
) {
}
