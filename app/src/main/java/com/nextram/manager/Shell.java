package com.nextram.manager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class Shell {

    private Shell() {
    }

    public static final class Result {
        public final int code;
        public final String out;

        Result(int code, String out) {
            this.code = code;
            this.out = out;
        }

        public boolean ok() {
            return code == 0;
        }
    }

    public static Result run(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            r.close();
            int code = p.waitFor();
            return new Result(code, sb.toString());
        } catch (Exception e) {
            return new Result(-1, e.toString());
        }
    }

    public static boolean hasRoot() {
        Result r = run("id");
        return r.out != null && r.out.contains("uid=0");
    }
}
