package custom.application;

import org.tinystruct.AbstractApplication;

public class hello extends AbstractApplication {

    @Override
    public void init() {
        // TODO Auto-generated method stub
        this.setAction("praise", "praise");
        this.setAction("say", "say");
        this.setAction("smile", "smile");
    }

    @Override
    public String version() {
        return "1.0";
    }

    public String praise() {
        return "Praise to the Lord!";
    }

    public String say() {
        if(null != this.context.getParameter("words"))
            return this.context.getParameter("words");

        return "Invalid parameter(s).";
    }

    public String say(String words) {
        return words;
    }

    public String smile() {
        return ":)";
    }

}