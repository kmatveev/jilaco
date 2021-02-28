package kmatveev.jilaco;

import sisc.env.DynamicEnvironment;
import sisc.env.MemorySymEnv;
import sisc.interpreter.AppContext;
import sisc.interpreter.Context;
import sisc.interpreter.Interpreter;
import sisc.interpreter.SchemeException;
import sisc.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

public class SISCEvaluator implements MainWindowController.Evaluator {

    private Interpreter interpreter;
    private ByteArrayOutputStream output = new ByteArrayOutputStream();
    private AppContext ctx;

    public SISCEvaluator() {
        reset();
    }

    @Override
    public String[] eval(String expression) {
        try {
            output.reset();
            String result = String.valueOf(interpreter.eval(expression));
            String outputStr = output.toString(interpreter.dynenv.characterSet.getName());
            output.reset();
            return new String[]{result, null, outputStr};
            
        } catch (SchemeException e) {
            return new String[]{null, e.getMessageText() };  
        } catch (IOException e) {
            return new String[] {null, e.getMessage()};
        }
    }

    @Override
    public void reset() {
        try {
            output.reset();
            ctx = new AppContext(new MemorySymEnv());

            URL heap = AppContext.findHeap(Util.makeURL("resources/sisc.shp"));
            ctx.addHeap(AppContext.openHeap(heap));

            interpreter = Context.enter(new DynamicEnvironment(ctx, System.in, output));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {

        MainWindowController.configureLF();

        MainWindowController.Evaluator evaluator = new SISCEvaluator(); // new JSEvaluator();
        MainWindowController controller = new MainWindowController(evaluator, "Jilaco-SISC", MainWindowController.loadProperties("jilaco-sisc"), new MainWindowController.AppListener() {
            @Override
            public void appExiting(Properties props) {
                try {
                    MainWindowController.storeProperties(props, "jilaco-sisc");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    
}
