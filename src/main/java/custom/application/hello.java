package custom.application;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Settings;
import org.tinystruct.system.annotation.Action;

import java.util.Date;

public class hello extends AbstractApplication {

    @Override
    public void init() {
        // TODO Auto-generated method stub
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Action("praise")
    public String praise() {
        return "Praise to the Lord!";
    }

    @Action("say")
    public String say() {
        if (null != this.context.getAttribute("--words"))
            return this.context.getAttribute("--words").toString();

        return "Invalid parameter(s).";
    }

    @Action("setdate")
    public Date setCurrentDate(Date date) {
        return date;
    }

    @Action("date")
    public Date currentDate() {
        return new Date();
    }

    @Action("stable")
    public boolean stable(boolean x) {
        return x;
    }

    @Action("smile")
    public String smile() {
        return ":)";
    }

    public static void main(String[] args) throws ApplicationException {
        ApplicationManager.install(new hello(), new Settings());
        System.out.println(ApplicationManager.call("praise", null));

        ApplicationContext ctx = new ApplicationContext();
        ctx.setAttribute("--words", "Praise to the Lord!");

        System.out.println(ApplicationManager.call("say", ctx));
        System.out.println(ApplicationManager.call("setdate/2022-01-01 00:00:00", ctx));
        System.out.println(ApplicationManager.call("stable/true", ctx));
    }
}