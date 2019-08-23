package info.ragozin.loadscript;

import java.util.Map;
import java.util.Random;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebResponse;

public class RandomLoopProcessor implements InteractionProcessor {

    private Random random = new Random(1);
    private double chance = 0;

    public RandomLoopProcessor() {
    }

    public void chance(String chance) {
        this.chance = Double.parseDouble(chance);
    }

    @Override
    public void preprocess(Map<String, String> variables) {
        // do nothing
    }

    @Override
    public synchronized void processResponse(WebConnection connection, WebResponse response, Map<String, String> variables) {
        if (random.nextDouble() < chance) {
            variables.put("RESTART", "");
        }
    }
}
