package net.lyingice.datarecipegenerator.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeWriterTest {

    @Test
    void writesReadyToUseDataPack(@TempDir Path temp) throws Exception {
        GeneratorConfig config = new GeneratorConfig();
        config.namespace = "test_pack";
        config.outputPath = "my_pack";

        Path written = RecipeWriter.write(temp, config, PlatformEdition.FORGE_1_20_1, "stone", "{\"type\":\"x\"}");

        Path expected = temp.resolve("data_recipe_generator_output")
                .resolve("my_pack")
                .resolve("data")
                .resolve("test_pack")
                .resolve("recipes")
                .resolve("stone.json");
        assertEquals(expected.toAbsolutePath().normalize(), written);
        assertTrue(Files.exists(written));

        Path metadata = temp.resolve("data_recipe_generator_output")
                .resolve("my_pack")
                .resolve("pack.mcmeta");
        assertTrue(Files.exists(metadata));
        JsonObject pack = JsonParser.parseString(Files.readString(metadata)).getAsJsonObject().getAsJsonObject("pack");
        assertEquals(15, pack.get("pack_format").getAsInt());
    }

    @Test
    void writes1211PackFormat(@TempDir Path temp) throws Exception {
        GeneratorConfig config = new GeneratorConfig();
        config.namespace = "test";

        Path written = RecipeWriter.write(temp, config, PlatformEdition.NEOFORGE_1_21_1, "stone", "{}");
        assertTrue(written.toString().contains("/data/test/recipe/stone.json".replace('/', java.io.File.separatorChar)));
        Path metadata = temp.resolve("data_recipe_generator_output").resolve("pack.mcmeta");
        JsonObject pack = JsonParser.parseString(Files.readString(metadata)).getAsJsonObject().getAsJsonObject("pack");
        assertEquals(48, pack.get("pack_format").getAsInt());
    }

    @Test
    void usesSingularRecipeFolderFor2612(@TempDir Path temp) throws Exception {
        GeneratorConfig config = new GeneratorConfig();
        config.namespace = "test";

        Path written = RecipeWriter.write(temp, config, PlatformEdition.NEOFORGE_26_1_2, "stone", "{}");
        assertTrue(written.toString().contains("/data/test/recipe/stone.json".replace('/', java.io.File.separatorChar)));
        Path metadata = temp.resolve("data_recipe_generator_output").resolve("pack.mcmeta");
        JsonObject pack = JsonParser.parseString(Files.readString(metadata)).getAsJsonObject().getAsJsonObject("pack");
        assertEquals(101, pack.get("min_format").getAsInt());
        assertEquals(101, pack.get("max_format").getAsInt());
    }

    @Test
    void rejectsTraversalAndInvalidNamespace(@TempDir Path temp) {
        GeneratorConfig traversal = new GeneratorConfig();
        traversal.namespace = "test";
        traversal.outputPath = "../evil";
        RecipeValidationException pathError = assertThrows(RecipeValidationException.class,
                () -> RecipeWriter.write(temp, traversal, PlatformEdition.FORGE_1_20_1, "x", "{}"));
        assertEquals(RecipeError.INVALID_OUTPUT_PATH, pathError.error());

        GeneratorConfig namespace = new GeneratorConfig();
        namespace.namespace = "Bad Namespace!";
        RecipeValidationException namespaceError = assertThrows(RecipeValidationException.class,
                () -> RecipeWriter.write(temp, namespace, PlatformEdition.FORGE_1_20_1, "x", "{}"));
        assertEquals(RecipeError.INVALID_NAMESPACE, namespaceError.error());
    }
}
