package monoton.utils.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Utility class for file and stream operations.
 *
 * @author Jefferson
 * @since 30/11/2022
 */
public class FileUtil {
    private static final Logger LOGGER = Logger.getLogger(FileUtil.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Reads the contents of an InputStream into a String using a StringBuilder.
     * Uses BufferedReader for efficient reading and ensures proper resource cleanup.
     *
     * @param inputStream the input stream to read from
     * @return the string representation of the input stream's contents
     * @throws IllegalArgumentException if inputStream is null
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public static String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        StringBuilder stringBuilder = new StringBuilder(DEFAULT_BUFFER_SIZE);

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        } catch (IOException e) {
            LOGGER.severe("Error reading input stream: " + e.getMessage());
            throw e;
        }

        return stringBuilder.toString();
    }
}