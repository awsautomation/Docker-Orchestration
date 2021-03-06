

package com.codeabovelab.dm.cluman.cluster.compose.model;

import lombok.Builder;
import lombok.Data;

import java.io.File;

@Data
@Builder
public class ComposeArg {

    private final File file;
    private final String appName;
    private final boolean runUpdate;
    private final boolean checkContainersUpDuringStart;
    private final String clusterName;

}
