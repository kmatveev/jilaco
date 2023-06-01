package kmatveev.jilaco;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GenericStdIOEvaluator implements MainWindowController.Evaluator{

    private Process proc;
    private String executable;

    public GenericStdIOEvaluator(String executable) {
        this.executable = executable;
        reset();
    }

    @Override
    public String[] eval(String expression) {

        try {
            InputStream fromProc = proc.getInputStream();
            BufferedInputStream fromUser = new BufferedInputStream(System.in);

            proc.getOutputStream().write(expression.getBytes(StandardCharsets.UTF_8));
            proc.getOutputStream().write((byte)'\n');
            proc.getOutputStream().flush();

            byte[] buffer = new byte[2000];
            StringBuilder result = new StringBuilder();
            CharsetDecoder decoder = StandardCharsets.US_ASCII.newDecoder();
            while (true) {
                int fromProcLen = fromProc.read(buffer);
                if (fromProcLen <= 0) break;
                result.append(decoder.decode(ByteBuffer.wrap(buffer, 0, fromProcLen)));
                if (fromProcLen < buffer.length) break;
            }
            return parseOutput(result.toString());
        } catch (IOException e) {
            return new String[] {null, e.getMessage(), null, null};
        }

    }

    protected String[] parseOutput(String s) {
        // by default all output is a result value
        return new String[]{s, null, null, null};
    }

    @Override
    public void reset() {

        try {
            if (proc != null) {
                proc.destroy();
            }

            ProcessBuilder pb = new ProcessBuilder(executable);
            proc = pb.start();

            InputStream fromProc = proc.getInputStream();
            int fromProcLen = fromProc.read(new byte[2000]); // read and discard

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Map<String, Object> getBindings() {
        return null;
    }

}
