package kmatveev.jilaco;

import java.util.Properties;

public class TinySchemeEvaluator extends GenericStdIOEvaluator {

    public TinySchemeEvaluator(String executable) {
        super(executable);
    }

    @Override
    protected String[] parseOutput(String s) {
        String delim;
        if (s.indexOf("\r\n") >= 0) {
            delim = "\r\n";
        } else if (s.indexOf("\r") > 0) {
            delim = "\r";
        } else {
            delim = "\n";
        }
        String[] lines = s.split(delim);
        StringBuilder result = new StringBuilder();
        StringBuilder error = new StringBuilder();
        StringBuilder output = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("Error:")) {
                error.append(line).append("\n");
            } else if (line.startsWith("ts>")){
                // output.append(line).append("\n");
                // ignore
            } else if (line.length() > 0){
                result.append(line).append("\n");
            }
        }

        return new String[]{getString(result), getString(error), getString(output), null};
    }

    private static String getString(StringBuilder sb) {
        if (sb.length() > 0) {
            if (sb.charAt(sb.length() - 1) == '\n') {
                sb.deleteCharAt(sb.length() - 1);
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {

        MainWindowController.configureLF();

        MainWindowController.Evaluator evaluator = new TinySchemeEvaluator("tinyscheme.exe");
        MainWindowController controller = new MainWindowController(evaluator, "Jilaco-TinyScheme", MainWindowController.loadProperties("jilaco-tinyscheme"), new MainWindowController.AppListener() {
            @Override
            public void appExiting(Properties props) {
                try {
                    MainWindowController.storeProperties(props, "jilaco-tinyscheme");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }

}
