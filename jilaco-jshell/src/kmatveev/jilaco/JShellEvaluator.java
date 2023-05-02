package kmatveev.jilaco;

import jdk.jshell.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

public class JShellEvaluator implements MainWindowController.Evaluator {

    private JShell jshell;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    public JShellEvaluator() {
        jshell = JShell.builder().out(new PrintStream(output, true)).err(new PrintStream(err, true)).build();
    }

    @Override
    public String[] eval(String expression) {
        output.reset();
        err.reset();

        SourceCodeAnalysis.CompletionInfo an = jshell.sourceCodeAnalysis().analyzeCompletion(expression);
        List<SnippetEvent> events = jshell.eval(an.source());
        StringBuilder resultsStr = new StringBuilder();
        for (SnippetEvent event : events) {
            if (event.status() == Snippet.Status.VALID) {
                resultsStr.append(event.value()).append("\n");
            }
        }
        String outputStr = output.toString(Charset.forName("UTF-8"));
        String errStr = err.toString(Charset.forName("UTF-8"));
        output.reset();
        err.reset();
        return new String[]{resultsStr.toString(), errStr, outputStr};
    }

    @Override
    public void reset() {
        jshell = JShell.builder().out(new PrintStream(output, true)).err(new PrintStream(err, true)).build();
    }

    public static void main(String[] args) throws Exception {

        MainWindowController.configureLF();

        MainWindowController.Evaluator evaluator = new JShellEvaluator();
        MainWindowController controller = new MainWindowController(evaluator, "Jilaco-JShell", MainWindowController.loadProperties("jilaco-jshell"), new MainWindowController.AppListener() {
            @Override
            public void appExiting(Properties props) {
                try {
                    MainWindowController.storeProperties(props, "jilaco-jshell");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }


}
