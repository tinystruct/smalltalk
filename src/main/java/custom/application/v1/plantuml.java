package custom.application.v1;

import com.plantuml.api.cheerpj.Base64OutputStream;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.util.TextFileLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class plantuml extends AbstractApplication {
    /**
     * Initialize for an application once it's loaded.
     */
    @Override
    public void init() {

    }

    @Action("generate-uml")
    public List<String> generateUML() throws ApplicationException, IOException {
        if (getContext().getAttribute("--plantuml-script") == null)
            throw new ApplicationException("Missing --plantuml-script");

        String script = getContext().getAttribute("--plantuml-script").toString();
        TextFileLoader loader = new TextFileLoader(script);
        String sc = loader.getContent().toString();
        return generateUML(sc);
    }

    public List<String> generateUML(String script) throws IOException {
        script = script.replaceAll("\\\\n", "\n");
        script = script.replaceAll("\\\\\"", "\"");
        SourceStringReader reader = new SourceStringReader(script);
        List<BlockUml> blocks = reader.getBlocks();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            OutputStream os = new Base64OutputStream();
            blocks.get(i).getDiagram().exportDiagram(os, i, new FileFormatOption(FileFormat.PNG));
            list.add(os.toString());
        }

        return list;
    }


    /**
     * Return the version of the application.
     *
     * @return version
     */
    @Override
    public String version() {
        return "";
    }
}
