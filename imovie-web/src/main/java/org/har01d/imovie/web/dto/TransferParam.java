package org.har01d.imovie.web.dto;

import java.util.Set;
import lombok.Data;

@Data
public class TransferParam {

    private Set<Integer> resourceIds;
    private Integer movieId;
}
