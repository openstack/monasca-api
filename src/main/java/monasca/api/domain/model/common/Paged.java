package monasca.api.domain.model.common;

import java.util.ArrayList;
import java.util.List;

public class Paged {

  public static final int LIMIT = 50;

  public List<Link> links = new ArrayList<>();

  public List<?> elements;

}
