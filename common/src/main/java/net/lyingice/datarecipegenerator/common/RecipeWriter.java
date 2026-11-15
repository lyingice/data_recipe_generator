package net.lyingice.datarecipegenerator.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a generated recipe into a ready-to-use datapack folder:
 *
 * <pre>
 * data_recipe_generator_output/
 *   pack.mcmeta
 *   data/&lt;namespace&gt;/&lt;recipe|recipes&gt;/&lt;file&gt;.json
 * </pre>
 *
 * The output path is always relative to the output root and is checked for
 * traversal. Clients cannot ask the server to write arbitrary files.
 */
public final class RecipeWriter {
    public static final String OUTPUT_ROOT_NAME = "data_recipe_generator_output";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private RecipeWriter() {
    }

    public static Path write(Path serverDirectory, GeneratorConfig config, PlatformEdition edition,
                             String fileName, String json)
            throws IOException, RecipeValidationException {
        Path outputRoot = serverDirectory.resolve(OUTPUT_ROOT_NAME).toAbsolutePath().normalize();
        Path packRoot = resolvePackRoot(outputRoot, config.outputPath);

        String namespace = config.namespace == null ? "" : config.namespace.trim();
        if (!ResourceIds.isValidNamespace(namespace)) {
            throw new RecipeValidationException(RecipeError.INVALID_NAMESPACE, namespace);
        }

        String safeName = ResourceIds.sanitizeFileName(fileName);
        if (safeName.contains("/") || safeName.contains("\\") || safeName.contains("..")) {
            throw new RecipeValidationException(RecipeError.INVALID_OUTPUT_PATH, fileName);
        }

        Files.createDirectories(packRoot);
        writePackMetadata(packRoot, edition);

        Path recipeDirectory = packRoot.resolve("data")
                .resolve(namespace)
                .resolve(edition.recipeFolder())
                .normalize();
        if (!recipeDirectory.startsWith(outputRoot)) {
            throw new RecipeValidationException(RecipeError.INVALID_OUTPUT_PATH, config.outputPath);
        }
        Files.createDirectories(recipeDirectory);

        Path recipeFile = recipeDirectory.resolve(safeName + ".json").normalize();
        if (!recipeFile.startsWith(outputRoot)) {
            throw new RecipeValidationException(RecipeError.INVALID_OUTPUT_PATH, config.outputPath);
        }
        Files.writeString(recipeFile, json + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return recipeFile;
    }

    private static Path resolvePackRoot(Path outputRoot, String outputPath)
            throws RecipeValidationException {
        if (outputPath == null || outputPath.isBlank()) {
            return outputRoot;
        }
        String relative = outputPath.trim().replace('\\', '/');
        if (relative.startsWith("/") || relative.contains(":")) {
            throw new RecipeValidationException(RecipeError.INVALID_OUTPUT_PATH, outputPath);
        }
        List<String> segments = new ArrayList<>();
        for (String segment : relative.split("/")) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                throw new RecipeValidationException(RecipeError.INVALID_OUTPUT_PATH, outputPath);
            }
            segments.add(segment);
        }
        if (segments.isEmpty()) {
            return outputRoot;
        }
        Path resolved = outputRoot;
        for (String segment : segments) {
            resolved = resolved.resolve(segment);
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(outputRoot) || resolved.equals(outputRoot.getParent())) {
            throw new RecipeValidationException(RecipeError.INVALID_OUTPUT_PATH, outputPath);
        }
        return resolved;
    }

    private static void writePackMetadata(Path packRoot, PlatformEdition edition) throws IOException {
        Path metadata = packRoot.resolve("pack.mcmeta");
        if (Files.exists(metadata)) {
            return;
        }
        JsonObject pack = new JsonObject();
        if (edition == PlatformEdition.NEOFORGE_26_1_2) {
            JsonObject description = new JsonObject();
            description.addProperty("text", "Data Recipe Generator output");
            pack.add("description", description);
            // 26.1's PackFormat codec rejects a bare pack_format above the
            // pre-minor-version cutoff; newer packs must declare min/max.
            pack.addProperty("min_format", edition.dataPackFormat());
            pack.addProperty("max_format", edition.dataPackFormat());
        } else {
            pack.addProperty("pack_format", edition.dataPackFormat());
            pack.addProperty("description", "Data Recipe Generator output");
        }

        JsonObject root = new JsonObject();
        root.add("pack", pack);
        try {
            Files.writeString(metadata, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException ignored) {
            // Another request created the pack metadata concurrently.
        }
    }
}
