package info.ragozin.loadscript;

import java.util.Map;
import java.util.Random;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebResponse;

public class RandomNumberGenerator implements InteractionProcessor {

    private Random random = new Random(1);
    private String name;
    private int min = 0;
    private int max = Integer.MAX_VALUE;

    public RandomNumberGenerator() {
    }

    public void name(String name) {
        this.name = name;
    }

    public void min(String min) {
        this.min = Integer.parseInt(min);
    }

    public void max(String max) {
        this.max = Integer.parseInt(max);
    }

    @Override
    public synchronized void preprocess(Map<String, String> variables) {
        int val = random.nextInt(max + 1 - min) + min;
        variables.put(name, String.valueOf(val));
    }

    @Override
    public void processResponse(WebConnection connection, WebResponse response, Map<String, String> variables) {
        // do nothing
    }
}
