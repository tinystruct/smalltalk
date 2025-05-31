package custom.application.v1;

import com.plantuml.api.cheerpj.Base64OutputStream;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.http.Request;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.util.TextFileLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class plantuml extends AbstractApplication {
    /**
     * Initialize for an application once it's loaded.
     */
    @Override
    public void init() {

    }

    @Action(value = "generate-uml", mode = org.tinystruct.application.Action.Mode.CLI)
    public List<String> generateUML() throws ApplicationException, IOException {
        if (getContext().getAttribute("--plantuml-script") == null)
            throw new ApplicationException("Missing --plantuml-script");

        String script = getContext().getAttribute("--plantuml-script").toString();
        TextFileLoader loader = new TextFileLoader(script);
        String sc = loader.getContent().toString();
        return generateUML(sc);
    }

    @Action(value = "plantuml2png")
    public List<String> generateUMLFromEncoded(Request request) throws ApplicationException, IOException {
        if (request.getParameter("plantuml-script") == null)
            throw new ApplicationException("Missing plantuml-script");

        byte[] bytes = Base64.getDecoder().decode(request.getParameter("plantuml-script"));
        return generateUML(new String(bytes, StandardCharsets.UTF_8));
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
