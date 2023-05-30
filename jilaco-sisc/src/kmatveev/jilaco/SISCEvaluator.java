package kmatveev.jilaco;

import sisc.env.DynamicEnvironment;
import sisc.env.MemorySymEnv;
import sisc.env.SymbolicEnvironment;
import sisc.interpreter.AppContext;
import sisc.interpreter.Context;
import sisc.interpreter.Interpreter;
import sisc.interpreter.SchemeException;
import sisc.util.ExpressionVisitee;
import sisc.util.ExpressionVisitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

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

            URL heap = AppContext.findHeap(SISCEvaluator.class.getResource("/sisc.shp"));
            ctx.addHeap(AppContext.openHeap(heap));

            interpreter = Context.enter(new DynamicEnvironment(ctx, System.in, output));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getBindings() {
        Map<String, Object> result = new TreeMap<String, Object>();
        SymbolicEnvironment topLevelEnv = interpreter.dynenv.ctx.toplevel_env;
        populateBindingsRecursively(topLevelEnv, result);
        return result;
    }

    private static Map<String, Object> populateBindingsRecursively(SymbolicEnvironment symEnv, Map<String, Object> result) {
        Map<String, Object> parentNode;
        if (symEnv.getParent() != null) {
            parentNode = populateBindingsRecursively(symEnv.getParent(), result);
        } else {
            parentNode = result;
        }
        Map<String, Object> thisNode = new TreeMap<String, Object>();
        parentNode.put(symEnv.getName().toString(), thisNode);

        symEnv.visit(new ExpressionVisitor() {
            private Object name = null;
            @Override
            public boolean visit(ExpressionVisitee expressionVisitee) {
                if (name == null) {
                    name = expressionVisitee;
                } else {
                    thisNode.put(name.toString(), expressionVisitee);
                    name = null;
                }
                return true;
            }
        });
        return thisNode;
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
