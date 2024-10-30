package net.bigyous.gptgodmc.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import net.bigyous.gptgodmc.WorldManager;
import net.bigyous.gptgodmc.GPT.Json.FunctionDeclaration;
import net.bigyous.gptgodmc.GPT.Json.Tool;

public class GPTUtils {
    private static EncodingRegistry registry = Encodings.newLazyEncodingRegistry();
    private static Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    public static int countTokens(String message) {
        if (message == null) {
            return 0;
        }
        return encoding.countTokens(message);
    }

    public static int countTokens(String[] messages) {
        int acumulator = 0;
        for (String msg : messages) {
            acumulator += countTokens(msg);
        }
        return acumulator;
    }

    public static int countTokens(List<String> messages) {
        int acumulator = 0;
        for (String msg : messages) {
            acumulator += countTokens(msg);
        }
        return acumulator;
    }

    public static Tool[] randomToolSubset(Tool[] tools, int size) {
        Random r = new Random();

        for (int i = tools.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);

            Tool temp = tools[i];
            tools[i] = tools[j];
            tools[j] = temp;
        }
        return Arrays.copyOfRange(tools, 0, size);
    }

    // calculates the sum tokens for all functions in a tool
    public static int calculateToolTokens(Tool tool) {
        int sum = 0;
        for (FunctionDeclaration func : tool.getFunctions()) {
            sum += func.calculateFunctionTokens();
        }
        return sum;
    }

    public static <T> T[] concatWithArrayCopy(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    // returns a formatted time stamp of current minecraft world time for GPT
    public static String getTimeStamp() {
        return getWorldTimeStamp(WorldManager.getCurrentWorld());
    }

    // returns a formatted time stamp of current players minecraft world time for
    // GPT
    public static String getPlayerTimeStamp(Player player) {
        return getWorldTimeStamp(player.getWorld());
    }

    // gets the time stamp for a specific world
    public static String getWorldTimeStamp(World world) {
        long time = world.getTime();
        long days = world.getFullTime() / (24 * 1000);
        long hours = time / 1000;
        long minutes = ((time % 1000) * 6) / 100;
        return String.format("Day%04d [%02d:%02d]", days, hours, minutes);
    }
}
