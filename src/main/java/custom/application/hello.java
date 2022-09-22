package custom.application;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.ApplicationManager;

import java.util.Date;

public class hello extends AbstractApplication {

    @Override
    public void init() {
        // TODO Auto-generated method stub
        this.setAction("praise", "praise");
        this.setAction("say", "say");
        this.setAction("smile", "smile");
        this.setAction("date", "currentDate");
        this.setAction("setdate", "setCurrentDate");
        this.setAction("stable", "stable");
    }

    @Override
    public String version() {
        return "1.0";
    }

    public String praise() {
        return "Praise to the Lord!";
    }

    public String say() {
        if(null != this.context.getAttribute("--words"))
            return this.context.getAttribute("--words").toString();

        return "Invalid parameter(s).";
    }

    public Date setCurrentDate(Date date) {
        return date;
    }

    public Date currentDate(){
        return new Date();
    }

    public boolean stable(boolean x) {
        return x;
    }

    public String say(String words) {
        return words;
    }

    public String smile() {
        return ":)";
    }

    public static void main(String[] args) throws ApplicationException {
        ApplicationManager.install(new hello());
        System.out.println(ApplicationManager.call("praise", null));

        ApplicationContext ctx = new ApplicationContext();
        ctx.setAttribute("--words", "Praise to the Lord!");

        System.out.println(ApplicationManager.call("say", ctx));
        System.out.println(ApplicationManager.call("setdate/2022-01-01 00:00:00", ctx));
        System.out.println(ApplicationManager.call("stable/true", ctx));
    }
}