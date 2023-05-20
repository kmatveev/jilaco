package kmatveev.jilaco;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

public class JSEvaluator implements MainWindowController.Evaluator {

    private ScriptEngine engine;

    public JSEvaluator() {
        // create a Nashorn script engine
        engine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    public void reset() {
        // probably a better way is to reset script context only
        engine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    @Override
    public String[] eval(String expression) {
        StringWriter outputWriter = new StringWriter(), errorWriter = new StringWriter();
        try {
            engine.getContext().setWriter(outputWriter);
            engine.getContext().setErrorWriter(errorWriter);
            String result = String.valueOf(engine.eval(expression));
            return new String[]{result, null, outputWriter.toString(), errorWriter.toString()};
        } catch (final ScriptException se) {
            return new String[]{null, se.getMessage(), outputWriter.toString(), errorWriter.toString()};
        }
    }

    @Override
    public Map<String, Object> getBindings() {
        return null;
    }

    public static void main(String[] args) throws Exception {

        MainWindowController.configureLF();

        MainWindowController.Evaluator evaluator = new JSEvaluator();
        MainWindowController controller = new MainWindowController(evaluator, "Jilaco-JS", MainWindowController.loadProperties("jilaco-js"), new MainWindowController.AppListener() {
            @Override
            public void appExiting(Properties props) {
                try {
                    MainWindowController.storeProperties(props, "jilaco-js");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                
            }
        });
    }

}
