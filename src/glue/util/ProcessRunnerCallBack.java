package glue.util;

public interface ProcessRunnerCallBack {
    public void processTerminated(int index, Object info, int status, long time, String message);
}
