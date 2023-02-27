package custom.ai;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;

public interface Provider {
    Builder call() throws ApplicationException;
}
