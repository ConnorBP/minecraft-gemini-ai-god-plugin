package net.bigyous.gptgodmc.loggables;

public class CommandLoggable extends BaseLoggable {
    protected String output;

    public CommandLoggable(String output) {
        super();
        this.output = output;
    }

    @Override
    public String getLog() {
        return String.format("command ran with output: %s", output);
    }

    @Override
    public boolean combine(Loggable l) {
        return false;
    }
}
