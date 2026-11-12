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
    }

    public static Result run(String cmd) {
        StringBuilder sb = new StringBuilder();
        int code = -1;
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = out.readLine()) != null) {
                sb.append(line).append('\n');
            }
            while ((line = err.readLine()) != null) {
                sb.append(line).append('\n');
            }
            code = p.waitFor();
        } catch (Exception e) {
            sb.append("ERROR: ").append(e.getMessage());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        return new Result(code, sb.toString().trim());
    }

    public static boolean hasRoot() {
        Result r = run("id");
        return r.out.contains("uid=0");
    }
}
